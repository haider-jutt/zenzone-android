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
import com.zenimmersive.android.databinding.SearchMusicItemBinding
import com.zenimmersive.android.helper.Constants
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.helper.KeyStorage.Companion.APP_SELECTED_LANGUAGE

class PlaylistOnlySongsAdapter(
    val context: Context,
    val songsList: ArrayList<AlbumMusic?>,
    val listener: PlayListOnlySongsAdapterListener
) : RecyclerView.Adapter<PlaylistOnlySongsAdapter.ViewHolder>() {
    val lan = KeyStorage.getInstance(context).getString(APP_SELECTED_LANGUAGE)
    var userJsonString = KeyStorage.getInstance(context).getString(Constants.USER_DATA, "")

    class ViewHolder(val binding: SearchMusicItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            SearchMusicItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = songsList[position] ?: return
        val binding = holder.binding

        // Load image
        Glide.with(context)
            .load(item.getBackgroundVertical(lan))
            .into(binding.ivMusic)

        // Determine language and related fields
        val isEnglish = lan.equals("en", ignoreCase = true)
        val songName = if (isEnglish) item.songName else item.songNameFrench
        val isGuided = if (isEnglish) item.isGuided else item.isFGuided

        // Set title
        binding.tvTitle.text = songName

        // Guided / Unguided visibility
        binding.llGuided.visibility = if (isGuided == 1) VISIBLE else GONE
        binding.llUnGuided.visibility = if (isGuided == 0) VISIBLE else GONE

        // Duration
        binding.tvDuration.text = "${item.getMusicDurationString()} ${context.getString(R.string.min)}"

        // New tag
        binding.tvNew.visibility = if (item.isNew == 1) VISIBLE else GONE

        // Paid status
        if (KeyStorage.getInstance(context).shouldShowPaidStatus(item, userJsonString)) {
            binding.ivPlay.visibility = GONE
            binding.viewPaidContainer.visibility = VISIBLE
            binding.viewPaidText.text = "${context.getString(R.string.buy).uppercase()} ${item.cost ?: "0"} €"
        } else {
            binding.viewPaidContainer.visibility = GONE
            binding.ivPlay.visibility = VISIBLE
        }

        // Click listener
        binding.root.setOnClickListener { listener.onPlayListOnlySongsAdapterClick(item) }
    }


    override fun getItemCount(): Int {
        return songsList.size
    }

    interface PlayListOnlySongsAdapterListener {
        fun onPlayListOnlySongsAdapterClick(songData: AlbumMusic)
    }
}