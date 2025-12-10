package com.zenimmersive.android.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zenimmersive.android.R
import com.zenimmersive.android.apiresponsemodel.AlbumMusic
import com.zenimmersive.android.databinding.RecentlyPlayedItemBinding
import com.zenimmersive.android.helper.Constants
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.helper.KeyStorage.Companion.APP_SELECTED_LANGUAGE

class VipMusicAdapter (val context: Context, val vipList: ArrayList<AlbumMusic?>, val listener: MusicPackItemListener): RecyclerView.Adapter<VipMusicAdapter.ViewHolder>() {
    val lan = KeyStorage.getInstance(context).getString(APP_SELECTED_LANGUAGE)
    var userJsonString = KeyStorage.getInstance(context).getString(Constants.USER_DATA, "")

    class ViewHolder(val binding: RecentlyPlayedItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(RecentlyPlayedItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = vipList[position] ?: return
        val binding = holder.binding

        Glide.with(context)
            .load(item.getBackgroundVertical(lan))
            .into(binding.ivImage)

        binding.tvDuration.text = "${item.getMusicDurationString()} ${context.getString(R.string.min)}"

        // Determine language properties
        val isEnglish = lan.equals("en", ignoreCase = true)
        val currencySymbol = if (isEnglish) "$" else "€"
        val songName = if (isEnglish) item.songName else item.songNameFrench
        val isGuided = if (isEnglish) item.isGuided else item.isFGuided

        // Set title
        binding.tvTitle.text = songName

        // Guided / Unguided visibility
        binding.llGuided.visibility = if (isGuided == 1) VISIBLE else GONE
        binding.llUnGuided.visibility = if (isGuided == 0) VISIBLE else GONE

        // Price / Free tag
        binding.tvPurchaseAndFreeTag.apply {
            text = if (item.isPaid == 1)
                "${context.getString(R.string.buy)} ${item.cost} $currencySymbol"
            else context.getString(R.string.free)
            visibility = VISIBLE
        }

        // Default hide play button
        binding.ivPlay.visibility = GONE

        // Item click
        binding.root.setOnClickListener {
            listener.onMusicPackPressed(item, arrayListOf(item))
        }

        // Paid logic
        if (KeyStorage.getInstance(context).shouldShowPaidStatus(item, userJsonString)) {
            binding.ivPlay.visibility = GONE
            binding.viewPaidContainer.visibility = VISIBLE
            binding.viewPaidText.text = "${context.getString(R.string.buy).uppercase()} ${item.cost ?: "0"} $currencySymbol"

            val purchaseClick = { listener.onMusicPackPurchaseButtonPressed(item) }
            binding.viewPaidText.setOnClickListener { purchaseClick() }
            binding.tvPurchaseAndFreeTag.setOnClickListener { purchaseClick() }

        } else {
            binding.viewPaidContainer.visibility = GONE
            binding.tvPurchaseAndFreeTag.visibility = GONE
            binding.ivPlay.visibility = VISIBLE
        }
    }


    override fun getItemCount(): Int {
        return vipList.size
    }

}