package com.zenimmersive.android.ui.payment

import android.app.Activity
import android.content.Context
import android.util.Log
import android.os.Handler
import android.os.Looper
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.QueryProductDetailsResult
import com.zenimmersive.android.helper.LogSystem
import com.zenimmersive.android.helper.KeyStorage
import java.text.NumberFormat
import java.util.Currency
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class PurchaseHelper(var context: Context) : PurchasesUpdatedListener {
    val TAG = PurchaseHelper::class.java.simpleName

    companion object {
        //for subscription plan ids
        val PREMIUM_MONTHLY = "monthly_premium_model"
        val PREMIUM_YEARLY = "yearly_premium_model"
        val INFINITE_MONTHLY = "infinite_model_monthly"
        val INFINITE_YEARLY = "infinite_model_yearly"
        private var instance: PurchaseHelper? = null
        fun getInstance(context: Context): PurchaseHelper {
            if (instance == null) instance = PurchaseHelper(context)
            return instance!!
        }

        fun toDuration(productId: String?) : Long {
            if(productId.isNullOrEmpty()) return 0L
            return when(productId) {
                PREMIUM_MONTHLY-> TimeUnit.DAYS.toMillis(28)
                PREMIUM_YEARLY-> TimeUnit.DAYS.toMillis(365)
                INFINITE_MONTHLY-> TimeUnit.DAYS.toMillis(28)
                INFINITE_YEARLY-> TimeUnit.DAYS.toMillis(365)
                else -> 0L
            }
        }
    }

    var billingClient: BillingClient
    private val pendingBillingActions = mutableListOf<() -> Unit>()
    private val mainHandler = Handler(Looper.getMainLooper())

    private data class SubscriptionPlan(
        val productId: String,
        val subscriptionType: Int,
        val durationMonths: Int
    )

    private val subscriptionPlans = mapOf(
        PREMIUM_MONTHLY to SubscriptionPlan(PREMIUM_MONTHLY, 2, 1),
        PREMIUM_YEARLY to SubscriptionPlan(PREMIUM_YEARLY, 2, 12),
        INFINITE_MONTHLY to SubscriptionPlan(INFINITE_MONTHLY, 3, 1),
        INFINITE_YEARLY to SubscriptionPlan(INFINITE_YEARLY, 3, 12)
    )

    private val billingPeriodPattern =
        Pattern.compile("P(?:(\\d+)Y)?(?:(\\d+)M)?(?:(\\d+)W)?(?:(\\d+)D)?", Pattern.CASE_INSENSITIVE)

    init {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                LogSystem.e(TAG, "onBillingSetupFinished: ${billingResult.responseCode}")
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    fetchSubscriptions()
                    flushPendingBillingActions()
                    fetchPurchasesConsume()
                }
            }

            override fun onBillingServiceDisconnected() {
                billingClient.startConnection(this)
            }
        })
    }

    private fun fetchPurchasesConsume() {
        // Query all purchases
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            onPurchasesUpdated(billingResult, purchases)
        }
    }


    private var subscriptionSkuList = arrayListOf<ProductDetails>()
    private var purchaseListener: PurchaseListener? = null

    fun fetchSubscriptions(listener: FetchSubscriptionListener? = null) {
        LogSystem.e(TAG, "fetchSubscriptions Invoked")
        var queryItems = arrayOf(PREMIUM_MONTHLY, PREMIUM_YEARLY, INFINITE_MONTHLY, INFINITE_YEARLY)

        var productList: MutableList<QueryProductDetailsParams.Product> = ArrayList()
        queryItems.forEach {
            productList.add(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(it)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )
        }

        var params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params, object : ProductDetailsResponseListener {
            override fun onProductDetailsResponse(
                p0: BillingResult,
                queryProductDetailsResult: QueryProductDetailsResult
            ) {
                LogSystem.e(
                    TAG,
                    "onProductDetailsResponse: ${p0.responseCode} - ${p0.debugMessage} Size : ${queryProductDetailsResult.productDetailsList?.size}"
                )
                if (p0.responseCode == BillingClient.BillingResponseCode.OK) {
                    subscriptionSkuList.clear()
                    subscriptionSkuList.addAll(queryProductDetailsResult.productDetailsList)
                    listener?.onSubscriptionFetched(subscriptionSkuList)
                } else {
                    LogSystem.e(TAG, "onProductDetailsResponse: ${p0.debugMessage}")
                    listener?.onSubscriptionFetchError(p0.debugMessage)
                }
            }
        })

    }

    private fun purchaseSkuIsInApp(id: String): Boolean {
        return id != PREMIUM_MONTHLY &&
                id != PREMIUM_YEARLY &&
                id != INFINITE_MONTHLY &&
                id != INFINITE_YEARLY
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        LogSystem.e(
            TAG,
            "onPurchasesUpdated: ${billingResult.responseCode} - ${billingResult.debugMessage}, Purchases : ${purchases?.size ?: 0}"
        )

        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                purchaseListener?.onPurchaseFailed("Item already owned by another account!")
            }

            BillingClient.BillingResponseCode.OK -> {
                LogSystem.e(TAG,"Result OK, Purchase List : ${purchases?.size}")
                if (!purchases.isNullOrEmpty()) {
                    var handled = false

                    for (purchase in purchases) {
                        LogSystem.d(TAG, "Purchase details: $purchase")
                        LogSystem.d(TAG, "Product IDs: ${purchase.products}")
                        LogSystem.d(TAG, "Purchase Token: ${purchase.purchaseToken}")
                        LogSystem.d(TAG, "Purchase Time: ${purchase.purchaseTime} DateTime : ${Date(purchase.purchaseTime)}")
                        LogSystem.d(TAG, "Order ID: ${purchase.orderId}")
                        LogSystem.d(TAG, "Purchase State: ${purchase.purchaseState}")
                        LogSystem.d(TAG, "Is Acknowledged: ${purchase.isAcknowledged}")

                        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                            val productId = purchase.products.firstOrNull()
                            // 1. Handle INAPP Products (Coins)
                            if (productId != null && purchaseSkuIsInApp(productId)) {
                                LogSystem.d(TAG, "In-App Purchase: $productId Consuming Product")
                                val params = ConsumeParams.newBuilder()
                                    .setPurchaseToken(purchase.purchaseToken)
                                    .build()

                                billingClient.consumeAsync(params) { billingResult, _ ->
                                    LogSystem.d(TAG, "Consumption result: ${billingResult.responseCode}")
                                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                        LogSystem.d(TAG, "Product successfully consumed.")
                                        // Coins successfully consumed → user can buy again
                                        purchaseListener?.onPurchaseSuccess(purchase)
                                    } else {
                                        LogSystem.d(TAG, "Consumption failed: ${billingResult.debugMessage}")
                                        purchaseListener?.onPurchaseFailed("Consumption failed: ${billingResult.debugMessage}")
                                    }
                                }
                            }
                            else if (!purchase.isAcknowledged) {
                                val acknowledgePurchaseParams =
                                    AcknowledgePurchaseParams.newBuilder()
                                        .setPurchaseToken(purchase.purchaseToken)
                                        .build()
                                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { ackResult ->
                                    if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                        LogSystem.d(TAG, "Purchase acknowledged successfully.")
                                        purchaseListener?.onPurchaseSuccess(purchase)
                                    } else {
                                        LogSystem.e(
                                            TAG,
                                            "Purchase acknowledgement failed: ${ackResult.debugMessage}"
                                        )
                                        purchaseListener?.onPurchaseFailed("Purchase acknowledgement failed: ${ackResult.debugMessage}")
                                    }
                                }
                            } else {
                                // Already acknowledged
                                purchaseListener?.onPurchaseSuccess(purchase)
                            }
                            handled = true
                        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                            LogSystem.d(TAG, "Purchase pending: ${purchase.orderId}")
                            purchaseListener?.onPurchaseFailed("Purchase is pending.")
                        }
                    }

                    if (!handled) {
                        purchaseListener?.onPurchaseFailed("No valid purchases processed.")
                    }
                } else {
                    purchaseListener?.onPurchaseFailed("Empty purchase list.")
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                purchaseListener?.onPurchaseCancelled()
            }

            else -> {
                purchaseListener?.onPurchaseFailed("Error ${billingResult.responseCode}: ${billingResult.debugMessage}")
            }
        }
    }


    //queryPurchases
    private fun queryPurchases() {

    }

    fun getCachedSubscriptionSkuList() = subscriptionSkuList

    //2 = PREMIUM, 3 = INFINITE
    fun findMonthlySubscriptionPlanByType(musicPackType: Int): ProductDetails? {
        var skuPlan: ProductDetails? = null
        subscriptionSkuList.forEach {
            var skuId = it.productId
            if (skuId.equals(PREMIUM_MONTHLY) && musicPackType == 2) {
                skuPlan = it
                return@forEach
            }
            if (skuId.equals(INFINITE_MONTHLY) && musicPackType == 3) {
                skuPlan = it
                return@forEach
            }
        }
        return skuPlan
    }

    fun findYearlySubscriptionPlanByType(musicPackType: Int): ProductDetails? {
        var skuPlan: ProductDetails? = null
        subscriptionSkuList.forEach {
            var skuId = it.productId
            if (skuId.equals(PREMIUM_YEARLY) && musicPackType == 2) {
                skuPlan = it
                return@forEach
            }
            if (skuId.equals(INFINITE_YEARLY) && musicPackType == 3) {
                skuPlan = it
                return@forEach
            }
        }
        return skuPlan
    }


    fun calculateMonthlyFromYearly(planYearly: ProductDetails?): String {
        if (planYearly == null) return "Only -- / Month"
        val offer = planYearly.subscriptionOfferDetails?.firstOrNull() ?: return "--"
        val pricingPhase = offer.pricingPhases.pricingPhaseList.firstOrNull() ?: return "--"

        val yearlyMicros = pricingPhase.priceAmountMicros
        val currencyCode = pricingPhase.priceCurrencyCode
        val yearlyPrice = yearlyMicros / 1_000_000.0
        val monthlyPrice = yearlyPrice / 12.0

        val currencyFormatter = NumberFormat.getCurrencyInstance().apply {
            currency = Currency.getInstance(currencyCode)
        }

        return "Only ${currencyFormatter.format(monthlyPrice) ?: "--"} / Month"
    }


    fun fetchProductById(productSku: String, listener: FetchProductListener? = null) {
        var productList: MutableList<QueryProductDetailsParams.Product> = ArrayList()
        productList.add(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productSku)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params, object : ProductDetailsResponseListener {
            override fun onProductDetailsResponse(
                p0: BillingResult,
                queryProductDetailsResult: QueryProductDetailsResult
            ) {
                LogSystem.e(
                    TAG,
                    "onProductDetailsResponse: ${p0.responseCode} - ${p0.debugMessage} Size : ${queryProductDetailsResult.productDetailsList?.size}"
                )
                if (p0.responseCode == BillingClient.BillingResponseCode.OK) {
                    if (queryProductDetailsResult.productDetailsList.isNotEmpty()) {
                        listener?.onProductFetched(
                            queryProductDetailsResult.productDetailsList.get(
                                0
                            )
                        )
                    } else {
                        listener?.onProductFetchError("No Product Found")
                    }
                } else {
                    LogSystem.e(TAG, "onProductDetailsResponse: ${p0.debugMessage}")
                    listener?.onProductFetchError(p0.debugMessage)
                }
            }

        })
    }

    fun launchPurchaseFlow(context: Activity, purchaseSku: ProductDetails) {
        val builder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(purchaseSku)

        // Only set offerToken for subscriptions
        val offerToken = purchaseSku.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (offerToken != null) {
            builder.setOfferToken(offerToken)
        }

        val productDetailsParamsList = listOf(builder.build())

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        // Proceed with billing flow
        val result = billingClient.launchBillingFlow(context, billingFlowParams)
        LogSystem.e(TAG, "launchBillingFlow: ${result.responseCode} - ${result.debugMessage}")
    }

    fun bindPurchaseListener(listener: PurchaseListener) {
        this.purchaseListener = listener
    }

    //Not using, It will be now could based, Backend will control the Expiry from the Developer APIs
    fun refreshSubscriptionStatus(
        force: Boolean = false,
        callback: ((Boolean) -> Unit)? = null
    ) {
        val keyStorage = KeyStorage.getInstance(context)
        val orderId = keyStorage.getEffectiveUserSubscriptionOrderId()
        val hasStoredSubscription = keyStorage.getStoredSubscriptionType() > 1
        if (!force && !hasStoredSubscription) {
            notifyCallback(callback, false)
            return
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        runOrQueueBillingAction {
            fetchSubscriptions(object : FetchSubscriptionListener{
                override fun onSubscriptionFetched(subscriptionSkuList: ArrayList<ProductDetails>) {
                    billingClient.queryPurchasesAsync(params) { result, purchases ->
                        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                            handleSubscriptionPurchases(purchases, callback, orderId)
                        } else {
//                            keyStorage.clearSubscriptionInfo()
//                            keyStorage.updateUserSubscriptionType(1)
                            notifyCallback(callback, false)
                        }
                    }
                }

                override fun onSubscriptionFetchError(debugMessage: String) {
                    notifyCallback(callback, false)
                }

            })
        }
    }

    fun persistSubscriptionPurchase(
        productId: String?,
        subscriptionType: Int,
        expiryMillis: Long
    ) {
        if (productId.isNullOrEmpty() || expiryMillis <= 0L) return
        val keyStorage = KeyStorage.getInstance(context)
        keyStorage.storeSubscriptionInfo(productId, subscriptionType, expiryMillis)
        keyStorage.updateUserSubscriptionType(subscriptionType)
    }

    fun calculateSubscriptionExpiry(
        productDetails: ProductDetails?,
        purchaseTime: Long,
        fallbackProductId: String? = null
    ): Long? {
        val billingPeriod = productDetails?.subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.firstOrNull()
            ?.billingPeriod

        if (!billingPeriod.isNullOrEmpty()) {
            parseBillingPeriod(purchaseTime, billingPeriod)?.let { return it }
        }

        val productId = productDetails?.productId ?: fallbackProductId
        val plan = findPlan(productId)
        return plan?.let { computeExpiryMillis(it.durationMonths, purchaseTime) }
    }

    private fun handleSubscriptionPurchases(
        records: List<Purchase>?,
        callback: ((Boolean) -> Unit)?,
        orderId: String?
    ) {
        LogSystem.e(TAG, "handleSubscriptionPurchases: ${records?.size}, orderId: $orderId")
        val keyStorage = KeyStorage.getInstance(context)
        if (records.isNullOrEmpty()) {
            //keyStorage.clearSubscriptionInfo()
            //keyStorage.updateUserSubscriptionType(1)
            notifyCallback(callback, false)
            return
        }
        if (orderId.isNullOrEmpty()) {
            notifyCallback(callback, false)
            return
        }
        records.find { it.orderId == orderId }?.let { record ->
            val productId = record.products.firstOrNull()
                ?: record.skus?.firstOrNull()
            if (productId != null) {
                LogSystem.e(TAG, "handleSubscriptionPurchases: Found matching orderId=$orderId, productId=$productId")
                val plan = findPlan(productId)
                if(plan != null) {
                    val expiry = computeExpiryMillis(plan.durationMonths, record.purchaseTime)
                    LogSystem.e(TAG, "handleSubscriptionPurchases: purchaseTime=${record.purchaseTime}, Date : ${Date(record.purchaseTime)}")
                    LogSystem.e(TAG, "handleSubscriptionPurchases: expiry=$expiry Date : ${Date(expiry)}")
                    keyStorage.storeSubscriptionInfo(productId, plan.subscriptionType, expiry)
                    keyStorage.updateUserSubscriptionType(plan.subscriptionType)
                    notifyCallback(callback, true)
                    return@let
                }
            }
            notifyCallback(callback, false)
        } ?: run {
            LogSystem.e(TAG, "handleSubscriptionPurchases: No matching purchase found for orderId=$orderId")
            notifyCallback(callback, false)
        }
    }

    private fun runOrQueueBillingAction(action: () -> Unit) {
        if (billingClient.isReady) {
            action()
        } else {
            synchronized(pendingBillingActions) {
                pendingBillingActions.add(action)
            }
        }
    }

    private fun flushPendingBillingActions() {
        val actions: List<() -> Unit>
        synchronized(pendingBillingActions) {
            if (pendingBillingActions.isEmpty()) return
            actions = pendingBillingActions.toList()
            pendingBillingActions.clear()
        }
        actions.forEach { it.invoke() }
    }

    private fun findPlan(productId: String?): SubscriptionPlan? {
        if (productId.isNullOrEmpty()) return null
        return subscriptionPlans[productId]
    }

    private fun computeExpiryMillis(durationMonths: Int, purchaseTime: Long): Long {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.timeInMillis = purchaseTime
        calendar.add(Calendar.MONTH, durationMonths)
        return calendar.timeInMillis
    }

    private fun parseBillingPeriod(purchaseTime: Long, billingPeriod: String): Long? {
        val matcher = billingPeriodPattern.matcher(billingPeriod)
        if (!matcher.matches()) return null
        val years = matcher.group(1)?.toIntOrNull() ?: 0
        val months = matcher.group(2)?.toIntOrNull() ?: 0
        val weeks = matcher.group(3)?.toIntOrNull() ?: 0
        val days = matcher.group(4)?.toIntOrNull() ?: 0

        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.timeInMillis = purchaseTime
        if (years > 0) calendar.add(Calendar.YEAR, years)
        if (months > 0) calendar.add(Calendar.MONTH, months)
        if (weeks > 0) calendar.add(Calendar.DAY_OF_YEAR, weeks * 7)
        if (days > 0) calendar.add(Calendar.DAY_OF_YEAR, days)
        return calendar.timeInMillis
    }

    private fun notifyCallback(callback: ((Boolean) -> Unit)?, value: Boolean) {
        if (callback == null) return
        mainHandler.post {
            callback(value)
        }
    }


    //FetchSubscriptionListener
    interface FetchSubscriptionListener {
        fun onSubscriptionFetched(subscriptionSkuList: ArrayList<ProductDetails>)
        fun onSubscriptionFetchError(debugMessage: String)
    }

    interface FetchProductListener {
        fun onProductFetched(productSku: ProductDetails)
        fun onProductFetchError(debugMessage: String)
    }

    interface PurchaseListener {
        fun onPurchaseSuccess(purchase: Purchase)
        fun onPurchaseCancelled()
        fun onPurchaseFailed(errorMessage: String)
    }
}

