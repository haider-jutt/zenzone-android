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
import com.zenimmersive.android.databinding.SmallMusicItemBinding
import com.zenimmersive.android.helper.Constants
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.helper.KeyStorage.Companion.APP_SELECTED_LANGUAGE

class MusicPlackListAdapter (val context: Context, val popularList: ArrayList<AlbumMusic?>, val listener: MusicPackItemListener): RecyclerView.Adapter<MusicPlackListAdapter.ViewHolder>() {

    class ViewHolder(val binding: SmallMusicItemBinding) : RecyclerView.ViewHolder(binding.root)
    val lan = KeyStorage.getInstance(context).getString(APP_SELECTED_LANGUAGE)
    var userJsonString = KeyStorage.getInstance(context).getString(Constants.USER_DATA, "")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(SmallMusicItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = popularList[position] ?: return
        val binding = holder.binding

        // Load image
        Glide.with(context)
            .load(item.getBackgroundVertical(lan))
            .into(binding.ivMusic)

        // Paid / Free visibility
        val isPaidVisible = KeyStorage.getInstance(context)
            .shouldShowPaidStatus(item, userJsonString)

        binding.apply {
            viewPaidContainer.visibility = if (isPaidVisible) VISIBLE else GONE
            ivPlay.visibility = if (isPaidVisible) GONE else VISIBLE

            if (isPaidVisible) {
                viewPaidText.text = "${context.getString(R.string.buy).uppercase()} ${item.cost ?: "0"} €"
                viewPaidContainer.setOnClickListener {
                    listener.onMusicPackPurchaseButtonPressed(item)
                }
            }
        }

        // Language-based title
        val isEnglish = lan.equals("en", ignoreCase = true)
        binding.tvTitle.text = if (isEnglish) item.songName else item.songNameFrench ?: item.songName

        // Duration
        binding.tvDuration.text =
            "${item.getMusicDurationString()} ${context.getString(R.string.min)}"

        // Guided / UnGuided
        val isGuided = item.isGuided == 1
        binding.llGuided.visibility = if (isGuided) VISIBLE else GONE
        binding.llUnGuided.visibility = if (isGuided) GONE else VISIBLE

        // Click listener
        binding.root.setOnClickListener {
            listener.onMusicPackPressed(item, popularList)
        }
    }


    override fun getItemCount(): Int {
        return popularList.size
    }
}