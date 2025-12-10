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
import com.zenimmersive.android.databinding.LargeMusicItemBinding
import com.zenimmersive.android.helper.Constants
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.helper.KeyStorage.Companion.APP_SELECTED_LANGUAGE

class HomePopularAdapter (val context: Context, val popularList: ArrayList<AlbumMusic?>, val listener: MusicPackItemListener): RecyclerView.Adapter<HomePopularAdapter.ViewHolder>() {

    class ViewHolder(val binding: LargeMusicItemBinding) : RecyclerView.ViewHolder(binding.root)
    val lan = KeyStorage.getInstance(context).getString(APP_SELECTED_LANGUAGE)
    var userJsonString = KeyStorage.getInstance(context).getString(Constants.USER_DATA, "")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LargeMusicItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = popularList[position] ?: return
        val binding = holder.binding

        // Load image
        Glide.with(context)
            .load(item.getBackgroundVertical(lan))
            .into(binding.ivMusic)

        // --- Paid status handling ---
        val shouldShowPaid = KeyStorage.getInstance(context)
            .shouldShowPaidStatus(item, userJsonString)

        binding.apply {
            viewPaidContainer.visibility = if (shouldShowPaid) VISIBLE else GONE
            ivPlay.visibility = if (shouldShowPaid) GONE else VISIBLE

            if (shouldShowPaid) {
                viewPaidText.text = "${context.getString(R.string.buy)} ${item.cost ?: "0"} €"
                viewPaidContainer.setOnClickListener {
                    listener.onMusicPackPurchaseButtonPressed(item)
                }
            }

            // --- Language & Title ---
            val isEnglish = lan.equals("en", ignoreCase = true)
            tvTitle.text = if (isEnglish) item.songName else item.songNameFrench ?: item.songName

            // --- Guided / UnGuided visibility ---
            val guidedValue = if (isEnglish) item.isGuided else item.isFGuided
            llGuided.visibility = if (guidedValue == 1) VISIBLE else GONE
            llUnGuided.visibility = if (guidedValue == 0) VISIBLE else GONE

            // --- Duration ---
            tvDuration.text = "${item.getMusicDurationString()} ${context.getString(R.string.min)}"

            // --- Click ---
            root.setOnClickListener {
                listener.onMusicPackPressed(item, popularList)
            }
        }
    }


    override fun getItemCount(): Int {
        return popularList.size
    }
}