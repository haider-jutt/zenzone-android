package com.zenimmersive.android.ui.payment

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.zenimmersive.android.R
import com.zenimmersive.android.apiresponsemodel.AlbumMusic
import com.zenimmersive.android.base.ViewModelFactory
import com.zenimmersive.android.base.ViewState
import com.zenimmersive.android.databinding.FragmentSubscriptionBSBinding
import com.zenimmersive.android.helper.Constants.SONG_DATA
import com.zenimmersive.android.repository.SubscriptionRepository
import com.zenimmersive.android.viewmodel.SubscriptionViewModel
import com.zenimmersive.android.helper.KeyStorage
import org.json.JSONObject
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.content.DialogInterface
import com.zenimmersive.android.helper.hide

class SubscriptionBSFragment : BottomSheetDialogFragment(), PurchaseHelper.PurchaseListener {
    private lateinit var binding: FragmentSubscriptionBSBinding
    private var bottomSheetDismissListener: BottomSheetDismissListener? = null
    private lateinit var subscriptionViewModel: SubscriptionViewModel
    private var loadingDialog: Dialog? = null
    private var pendingSubscriptionExpiryMillis: Long = 0L
    private var pendingSubscriptionProductId: String? = null

    fun bindBottomSheetDismissListener(listener: BottomSheetDismissListener) {
        bottomSheetDismissListener = listener
    }

    lateinit var subscriptionHandler: PurchaseHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSubscriptionBSBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet =
                bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                it.setBackgroundColor(Color.TRANSPARENT)
                val behavior = BottomSheetBehavior.from(it)
                it.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

       /* dialog.setOnDismissListener {
            bottomSheetDismissListener?.onBottomSheetDismissed()
        }*/

        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.let { dlg ->
            val bottomSheet =
                dlg.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.layoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        bottomSheetDismissListener?.onBottomSheetDismissed(musicPack)
    }

    interface BottomSheetDismissListener {
        fun onBottomSheetDismissed(musicPack: AlbumMusic?)
    }

    var singleProductSku: ProductDetails? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        parseArguments()
        subscriptionHandler = PurchaseHelper.getInstance(requireContext())

        // ViewModel initialization
        val repository = activity?.let { SubscriptionRepository(it) }
        val factory = ViewModelFactory(repository)
        subscriptionViewModel = ViewModelProvider(this, factory)[SubscriptionViewModel::class.java]

        observeSubscriptionVerification()

        binding.ivClose.setOnClickListener {
            dismiss()
        }

        // Bind purchase listener only once
        subscriptionHandler.bindPurchaseListener(this)

        subscriptionHandler.fetchProductById(
            musicPack?.productId ?: "",
            object : PurchaseHelper.FetchProductListener {
                override fun onProductFetched(productSku: ProductDetails) {
                    singleProductSku = productSku
                    binding.root.post {
                        binding.progressBarProductPrice.hide()
                        bindPurchaseModelUI()
                    }
                }

                override fun onProductFetchError(debugMessage: String) {
                    showToast(debugMessage)
                    dismiss()
                }

            })
        if (subscriptionHandler.getCachedSubscriptionSkuList().isEmpty()) {
            subscriptionHandler.fetchSubscriptions(object :
                PurchaseHelper.FetchSubscriptionListener {
                override fun onSubscriptionFetched(subscriptionSkuList: ArrayList<ProductDetails>) {
                    binding.root.post {
                        binding.progressBarMonthly.hide()
                        binding.progressBarYearly.hide()
                        bindPurchaseModelUI()
                    }
                }

                override fun onSubscriptionFetchError(debugMessage: String) {
                    showToast(debugMessage)
                    dismiss()
                }
            });
        } else {
            binding.root.post {
                binding.progressBarMonthly.hide()
                binding.progressBarYearly.hide()
                bindPurchaseModelUI()
            }
        }
    }

    var musicPack: AlbumMusic? = null
    var musicPackType = 2 // 1 = Free, 2 = Premium, 3 = Infinite (default: Premium)
    var musicPackDuration = 1

    private fun parseArguments() {
        if (arguments != null) {
            if (arguments?.getSerializable(SONG_DATA) != null) {
                musicPack = arguments?.getSerializable(SONG_DATA) as AlbumMusic
                musicPackType = musicPack?.musicPackType ?: 2 // Default to Premium
            }
        } else {
            dismiss()
        }
    }

    private var purchaseSku: ProductDetails? = null

    private fun bindPurchaseModelUI() {
        if (!isSafe()) return

//        val purchaseHelper = PurchaseHelper.getInstance(requireContext())

        when (musicPackType) {
            2 -> { // Premium
                binding.txt4.visibility = View.GONE
                binding.txtChangePlan.setText(R.string.you_are_a_pro)
                binding.txtPlan.setText(R.string.premium)
                binding.txt2.setText(R.string.plan_premium_lights)
                binding.tvTitle.setText(R.string.unlock_premium)
            }
            3 -> { // Infinite
                binding.txtPlan.setText(R.string.plan_infinite)
                binding.txt2.setText(R.string.plan_infinite_lights)
                binding.txt4.visibility = View.VISIBLE
                binding.txtChangePlan.setText(R.string.go_back_to_premium)
                binding.tvTitle.setText(R.string.unlock_infinite)
            }
            else -> { // Free or unknown
                binding.txtPlan.setText(R.string.plan_free)
                binding.txt2.setText(R.string.plan_free_lights)
                binding.txt4.visibility = View.GONE
                binding.txtChangePlan.setText("")
                binding.tvTitle.setText(R.string.unlock_free)
            }
        }

        val planMonthly: ProductDetails? =
            subscriptionHandler.findMonthlySubscriptionPlanByType(musicPackType)
        val planYearly: ProductDetails? =
            subscriptionHandler.findYearlySubscriptionPlanByType(musicPackType)

        // UI Price Texts
        val txtMonthlyPrice =
            "${planMonthly?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "--"} / Month"
        val txtYearlyPrice =
            "${planYearly?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "--"} / Year"
        val txtSinglePrice = singleProductSku?.oneTimePurchaseOfferDetails?.formattedPrice ?: "--"

        binding.txtMonthPrice.text = txtMonthlyPrice
        binding.txtYearPrice.text = txtYearlyPrice
        binding.txtSinglePrice.text = txtSinglePrice

        // Show calculated "per month" price from yearly
        binding.txtPerMonthPrice.text = subscriptionHandler.calculateMonthlyFromYearly(planYearly)

        // Handle plan selection
        binding.txtMonthPriceRoot.setOnClickListener {
            displaySkuSelection(0)
            purchaseSku = planMonthly
        }

        binding.txtYearPriceRoot.setOnClickListener {
            displaySkuSelection(1)
            purchaseSku = planYearly
        }

        binding.layoutSinglePurchase.setOnClickListener {
            displaySkuSelection(2)
            purchaseSku = singleProductSku
        }

        // Purchase Button
        binding.txtPurchase.setOnClickListener {
            if (purchaseSku != null) {
                showLoading()
                //Todo uncomment after testing
                subscriptionHandler.launchPurchaseFlow(requireActivity(), purchaseSku!!)
                //Todo comment after testing
//                dummySubscriptionUpdate()
            } else {
                showToast(getString(R.string.error_select_plan))
            }
        }

        binding.txtChangePlan.setOnClickListener {
            // Toggle between Premium and Infinite only
            musicPackType = when (musicPackType) {
                2 -> 3 // Premium -> Infinite
                3 -> 2 // Infinite -> Premium
                else -> 2 // Default to Premium
            }
            bindPurchaseModelUI()
        }
    }


    private fun displaySkuSelection(selectionModelIndex: Int) {
        musicPackDuration = selectionModelIndex+1
        var selectableViews = listOf(
            binding.txtMonthPriceRoot,
            binding.txtYearPriceRoot,
            binding.layoutSinglePurchase
        )
        var purchaseTextViews =
            listOf(binding.txtMonthPrice, binding.txtYearPrice, binding.txtSinglePrice)

        selectableViews.forEach { viewItem -> viewItem.setBackgroundResource(R.drawable.rounded_corner_light_gray) }
        purchaseTextViews.forEach { viewItem -> viewItem.setTextColor(resources.getColor(R.color.black)) }

        selectableViews[selectionModelIndex].setBackgroundResource(R.drawable.rounded_corner_light_yellow)
        purchaseTextViews[selectionModelIndex].setTextColor(resources.getColor(R.color.sky_blue))
    }


    fun isSafe() = !isRemoving && !isDetached && view != null && context != null
    fun showToast(msg: String) {
        if (!isSafe()) return
        activity?.runOnUiThread {
            try {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                // Optionally log the error
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun observeSubscriptionVerification() {
        subscriptionViewModel.verifySubscriptionRes.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ViewState.Loading -> showLoading()
                is ViewState.Data -> {
                    hideLoading()
                    if (purchaseSku?.productId == singleProductSku?.productId) {
                        // Single song purchased
                        musicPack?.isPurchased = 1
                        KeyStorage.getInstance(requireContext()).storePurchasePackId(musicPack?.songId)
                        showToast(getString(R.string.success_product_purchase_stored))
                        dismiss()
                    } else {
                        // Subscription pack purchased
                        // Update userSubscriptionType in preferences for subscription pack
                        val userJson = KeyStorage.getInstance(requireContext()).getString(com.zenimmersive.android.helper.Constants.USER_DATA, "")
                        val userObj = try { JSONObject(userJson) } catch (e: Exception) { JSONObject() }
                        var data = userObj.optJSONObject("data")
                        var result = data?.optJSONObject("result")

                        val newType = when (musicPackType) {
                            2 -> 2 // Premium
                            3 -> 3 // Infinite
                            else -> 1 // Free
                        }
                        result?.put("userSubscriptionType", newType)
                        data?.put("result", result)
                        userObj.put("data", data)
                        KeyStorage.getInstance(requireContext()).setString(com.zenimmersive.android.helper.Constants.USER_DATA, userObj.toString())
                        subscriptionHandler.persistSubscriptionPurchase(
                            pendingSubscriptionProductId ?: purchaseSku?.productId,
                            newType,
                            pendingSubscriptionExpiryMillis
                        )
                        pendingSubscriptionExpiryMillis = 0L
                        pendingSubscriptionProductId = null
                        showToast(getString(R.string.success_subscription_stored))
                        dismiss()
                    }
                }
                is ViewState.Error -> {
                    hideLoading()
                    showToast(getString(R.string.error_verification_failed, state.error))
                }
                else -> hideLoading()
            }
        }
    }

    private fun showLoading() {
        if (loadingDialog == null) {
            loadingDialog = Dialog(requireContext()).apply {
                setContentView(R.layout.progress_dialog) // Use existing progress_dialog.xml
                setCancelable(false)
            }
        }
        loadingDialog?.show()
    }

    private fun hideLoading() {
        loadingDialog?.dismiss()
    }

    override fun onPurchaseSuccess(purchase: Purchase) {
        if (isSafe()) {
            activity?.runOnUiThread {
                showToast(context?.getString(R.string.purchase_completed_successfully)!!)
                val songId = musicPack?.songId ?: 0
                val productId = purchaseSku?.productId ?: ""
                val purchaseToken = purchase.purchaseToken
                val orderId = purchase.orderId ?: ""
                if (orderId.isEmpty()) {
                    showToast("Purchase order ID is missing")
                    hideLoading()
                    return@runOnUiThread
                }
                if (purchaseSku?.productId == singleProductSku?.productId) {
                    val price = purchaseSku?.oneTimePurchaseOfferDetails?.formattedPrice?:""
                    val currency = purchaseSku?.oneTimePurchaseOfferDetails?.priceCurrencyCode
                    val amountMicros = purchaseSku?.oneTimePurchaseOfferDetails?.priceAmountMicros
                    //Single Product Purchase
                    subscriptionViewModel.verifyMusicPackPurchase(songId, productId, purchaseToken,price,orderId)
                } else {
                    val price = purchaseSku?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice?:""
                    val currency = purchaseSku?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.priceCurrencyCode
                    val amountMicros = purchaseSku?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.priceAmountMicros
                    pendingSubscriptionProductId = purchaseSku?.productId
                    pendingSubscriptionExpiryMillis = subscriptionHandler.calculateSubscriptionExpiry(
                        purchaseSku,
                        purchase.purchaseTime,
                        purchaseSku?.productId
                    ) ?: 0L
                    //Subscription Purchase
                    subscriptionViewModel.verifySubscriptionPackPurchase(productId, musicPackType, musicPackDuration ,purchaseToken,price,orderId)
                }
            }
        }
    }

    override fun onPurchaseCancelled() {
        hideLoading()
        showToast(getString(R.string.purchase_was_cancelled_by_you))
    }

    override fun onPurchaseFailed(errorMessage: String) {
        hideLoading()
        showToast(getString(R.string.purchase_failed, errorMessage))
    }

}


