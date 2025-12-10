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

class HomeSearchPlaylistAdapter (val context: Context, var searchList: ArrayList<AlbumMusic?>, val listener: SearchFilterSongsAdapterListener): RecyclerView.Adapter<HomeSearchPlaylistAdapter.ViewHolder>() {

    var lan = KeyStorage.getInstance(context).getString(KeyStorage.Companion.APP_SELECTED_LANGUAGE)
    var userJsonString = KeyStorage.getInstance(context).getString(Constants.USER_DATA, "")

    class ViewHolder(val binding: SearchMusicItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(SearchMusicItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = searchList[position] ?: return
        val binding = holder.binding

        val isEnglish = lan.isNullOrBlank() || lan == "en"


        // Load image
        Glide.with(context)
            .load(item.getBackgroundVertical(lan))
            .into(binding.ivMusic)

        // Paid / Free visibility
        val shouldShowPaid = KeyStorage.getInstance(context)
            .shouldShowPaidStatus(item, userJsonString)

        binding.apply {
            viewPaidContainer.visibility = if (shouldShowPaid) VISIBLE else GONE
            ivPlay.visibility = if (shouldShowPaid) GONE else VISIBLE

            if (shouldShowPaid) {
                viewPaidText.text = "${context.getString(R.string.buy).uppercase()} ${item.cost ?: "0"} €"
            }

            // Title
            tvTitle.text = if(isEnglish) item.songName else item.songNameFrench

            // "New" badge
            tvNew.visibility = if (item.isNew == 1) VISIBLE else GONE

            // Guided / UnGuided
            val isGuided = item.isGuided == 1
            llGuided.visibility = if (isGuided) VISIBLE else GONE
            llUnGuided.visibility = if (isGuided) GONE else VISIBLE

            // Duration
            tvDuration.text = "${item.getMusicDurationString()} ${context.getString(R.string.min)}"

            // Click listener
            root.setOnClickListener {
                listener.searchFilterSongsItemClick(position, item)
            }
        }
    }


    override fun getItemCount(): Int {
        return searchList.size
    }

    fun updateSearchSongList(searchList: ArrayList<AlbumMusic?>){
        this.searchList = searchList
        notifyDataSetChanged()
    }

    interface SearchFilterSongsAdapterListener{
        fun searchFilterSongsItemClick(position: Int, songsData: AlbumMusic)
    }
}