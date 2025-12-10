package com.zenimmersive.android.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
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

class FavoritesAdapter (val context: Context, val favoritesList: ArrayList<AlbumMusic?>, val listener: FavoriteAdapterLister): RecyclerView.Adapter<FavoritesAdapter.ViewHolder>() {
    val lan = KeyStorage.getInstance(context).getString(APP_SELECTED_LANGUAGE)
    var userJsonString = KeyStorage.getInstance(context).getString(Constants.USER_DATA, "")

    class ViewHolder(val binding: RecentlyPlayedItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(RecentlyPlayedItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val musicPack = favoritesList[position] ?: return
        val binding = holder.binding

        // Duration text
        binding.tvDuration.text = "${musicPack.getMusicDurationString()} ${context.getString(R.string.min)}"

        // Load image
        Glide.with(context)
            .load(musicPack.getBackgroundVertical(lan))
            .into(binding.ivImage)

        // Determine if language is English
        val isEnglish = lan.equals("en", ignoreCase = true)

        // Set title based on language
        binding.tvTitle.text = if (isEnglish) musicPack.songName else musicPack.songNameFrench

        // Guided/UnGuided visibility logic
        val isGuided = if (isEnglish) musicPack.isGuided else musicPack.isFGuided
        if (isGuided == 0) {
            binding.llGuided.visibility = View.GONE
            binding.llUnGuided.visibility = View.VISIBLE
        } else {
            binding.llGuided.visibility = View.VISIBLE
            binding.llUnGuided.visibility = View.GONE
        }

        // Click listener
        binding.root.setOnClickListener {
            listener.favoriteSongClick(musicPack)
        }

        // Paid/Free visibility logic
        if (KeyStorage.getInstance(context).shouldShowPaidStatus(musicPack, userJsonString)) {
            binding.ivPlay.visibility = View.GONE
            binding.viewPaidContainer.visibility = View.VISIBLE
            binding.viewPaidText.text = "${context.getString(R.string.buy).uppercase()} ${musicPack.cost ?: "0"} €"
            binding.tvPurchaseAndFreeTag.visibility = View.GONE
        } else {
            binding.viewPaidContainer.visibility = View.GONE
            binding.tvPurchaseAndFreeTag.visibility = View.GONE
            binding.ivPlay.visibility = View.VISIBLE
        }
    }


    override fun getItemCount(): Int {
        return favoritesList.size
    }

    interface FavoriteAdapterLister{
        fun favoriteSongClick(songData: AlbumMusic)
    }
}