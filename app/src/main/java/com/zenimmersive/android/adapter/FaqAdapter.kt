package com.zenimmersive.android.adapter

import android.content.Context
import android.os.Build
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.zenimmersive.android.R
import com.zenimmersive.android.apiresponsemodel.FaqResModel
import com.zenimmersive.android.databinding.ItemFaqBinding

class FaqAdapter(val context: Context, private var faqList: ArrayList<FaqResModel.Result?>) : RecyclerView.Adapter<FaqAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemFaqBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return  ViewHolder(ItemFaqBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.tvFaqTitle.text = Html.fromHtml(faqList.get(position)?.question ?: "", Html.FROM_HTML_MODE_COMPACT)
        holder.binding.tvDetails.text = Html.fromHtml(faqList.get(position)?.description ?: "", Html.FROM_HTML_MODE_COMPACT)

        holder.binding.root.setOnClickListener {
//            listener.itemClick(callLogsList[position], holder.binding)
            if (holder.binding.infoFullView.visibility == View.VISIBLE) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//                    TransitionManager.beginDelayedTransition(binding.expandableCardDiagnoses, AutoTransition())
                }
                holder.binding.infoFullView.visibility = View.GONE
                holder.binding.ivExpandCollapse.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.chevron_right))

            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//                    TransitionManager.beginDelayedTransition(binding.expandableCardDiagnoses, AutoTransition())
                }
                holder.binding.infoFullView.visibility = View.VISIBLE
                holder.binding.ivExpandCollapse.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.chevron_right))
//                arrow.setImageResource(R.drawable.ic_baseline_expand_less_24)
            }
        }

    }

    override fun getItemCount(): Int {
        return faqList.size
    }

}