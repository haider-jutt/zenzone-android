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

class HomeRecentlyPlayedAdapter (val context: Context, val recentlyList: ArrayList<AlbumMusic?>, val listener: RecentlyPlayedAdapterLister): RecyclerView.Adapter<HomeRecentlyPlayedAdapter.ViewHolder>() {
    val lan = KeyStorage.getInstance(context).getString(APP_SELECTED_LANGUAGE)

    class ViewHolder(val binding: RecentlyPlayedItemBinding) : RecyclerView.ViewHolder(binding.root)
    var userJsonString = KeyStorage.getInstance(context).getString(Constants.USER_DATA, "")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(RecentlyPlayedItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Glide.with(context).load(recentlyList[position]?.getBackgroundVertical(lan)).into(holder.binding.ivImage)
        val item = recentlyList[position] ?: return
        val binding = holder.binding

        // Load background image
        Glide.with(context)
            .load(item.getBackgroundVertical(lan))
            .into(binding.ivImage)

        // Duration text
        binding.tvDuration.text = "${item.getMusicDurationString()} ${context.getString(R.string.min)}"

        // Language & Title
        val isEnglish = lan.equals("en", ignoreCase = true)
        binding.tvTitle.text = if (isEnglish) item.songName else item.songNameFrench ?: item.songName

        // Guided / Unguided visibility
        val guidedValue = if (isEnglish) item.isGuided else item.isFGuided
        binding.llGuided.visibility = if (guidedValue == 1) VISIBLE else GONE
        binding.llUnGuided.visibility = if (guidedValue == 0) VISIBLE else GONE


        // Clicks
        binding.root.setOnClickListener {
            listener.recentlyPlayedSongClick(item)
        }

        // Paid/free visibility handling
        if (KeyStorage.getInstance(context).shouldShowPaidStatus(item, userJsonString)) {
            binding.ivPlay.visibility = GONE
            binding.viewPaidContainer.visibility = VISIBLE
            binding.viewPaidText.text = "${context.getString(R.string.buy).uppercase()} ${item.cost ?: "0"} €"

            // Purchase click listeners
            binding.viewPaidText.setOnClickListener {
                listener.onMusicPackPurchaseButtonPressed(item)
            }
            binding.tvPurchaseAndFreeTag.setOnClickListener {
                listener.onMusicPackPurchaseButtonPressed(item)
            }
        } else {
            binding.viewPaidContainer.visibility = GONE
            binding.tvPurchaseAndFreeTag.visibility = GONE
            binding.ivPlay.visibility = VISIBLE
        }
    }

    override fun getItemCount(): Int {
        return recentlyList.size
    }

    interface RecentlyPlayedAdapterLister : SongBuyListener {
        fun recentlyPlayedSongClick(songData: AlbumMusic)
    }
}