package com.zenimmersive.android.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zenimmersive.android.R
import com.zenimmersive.android.apiresponsemodel.OrderListResModel
import com.zenimmersive.android.databinding.MyOrdersItemBinding
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.helper.KeyStorage.Companion.APP_SELECTED_LANGUAGE
import com.zenimmersive.android.helper.hide
import com.zenimmersive.android.helper.show
import com.zenimmersive.android.ui.payment.PurchaseHelper

class MyOrdersAdapter(val context: Context, val orderList: ArrayList<OrderListResModel.Result?>) :
    RecyclerView.Adapter<MyOrdersAdapter.ViewHolder>() {
    val lan = KeyStorage.getInstance(context).getString(APP_SELECTED_LANGUAGE)

    class ViewHolder(val binding: MyOrdersItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            MyOrdersItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        if (orderList[position]?.orderNumber?.endsWith("model") == true) {
            var imageId = findModelImage(orderList[position]?.orderNumber)
            holder.binding.ivImage.setImageResource(imageId)
        } else Glide.with(context).load(orderList[position]?.getBackgroundVertical(lan))
            .error(R.drawable.placeholder_error)
            .into(holder.binding.ivImage)

        holder.binding.tvSongName.text = orderList[position]?.getSongNameByLan(lan)
        holder.binding.tvOrderNumber.text = orderList[position]?.orderNumber
        holder.binding.tvOrderDate.text = orderList[position]?.orderDate
        holder.binding.tvTransactionCode.text = orderList[position]?.transactionCode
        holder.binding.tvAmount.text = orderList[position]?.amount

        var item = orderList[position]
        if ((item?.purchasedPackType?.equals("1") == false) || orderList[position]?.amount.isNullOrEmpty()) {
            holder.binding.amountContainer.hide()
        } else {
            holder.binding.amountContainer.show()
        }
    }

    private fun findModelImage(orderNumber: String?): Int {
        val lan = KeyStorage.getInstance(context).getString(APP_SELECTED_LANGUAGE)
        if (lan.equals("fr")) {
            if (orderNumber.equals(PurchaseHelper.PREMIUM_MONTHLY)) {
                return R.drawable.premium_monthly_fr
            } else if (orderNumber.equals(PurchaseHelper.PREMIUM_YEARLY)) {
                return R.drawable.premium_yearly_fr
            } else if (orderNumber.equals(PurchaseHelper.INFINITE_MONTHLY)) {
                return R.drawable.infinite_monthly_fr
            } else if (orderNumber.equals(PurchaseHelper.INFINITE_YEARLY)) {
                return R.drawable.infinite_yearly_fr
            }
        } else {
            if (orderNumber.equals(PurchaseHelper.PREMIUM_MONTHLY)) {
                return R.drawable.premium_monthly_en
            } else if (orderNumber.equals(PurchaseHelper.PREMIUM_YEARLY)) {
                return R.drawable.premium_yearly_en
            } else if (orderNumber.equals(PurchaseHelper.INFINITE_MONTHLY)) {
                return R.drawable.infinite_monthly_en
            } else if (orderNumber.equals(PurchaseHelper.INFINITE_YEARLY)) {
                return R.drawable.infinite_yearly_en
            }
        }

        return R.drawable.placeholder_error
    }

    override fun getItemCount(): Int {
        return orderList.size
    }
}