package com.zenimmersive.android.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zenimmersive.android.R
import com.zenimmersive.android.adapter.HomeDefaultSearchTagsAdapter.DefaultSearchTagsListener
import com.zenimmersive.android.apiresponsemodel.Tag
import com.zenimmersive.android.databinding.MeditationMusicItemBinding
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.helper.KeyStorage.Companion.APP_SELECTED_LANGUAGE
import com.zenimmersive.android.helper.hide

class SearchTagsAdapter (val context: Context, val itemList: ArrayList<Tag?>, val listener: DefaultSearchTagsListener): RecyclerView.Adapter<SearchTagsAdapter.ViewHolder>() {
    val lan = KeyStorage.getInstance(context).getString(APP_SELECTED_LANGUAGE)
    class ViewHolder(val binding: MeditationMusicItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(MeditationMusicItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val isEnglish = lan.isNullOrBlank() || lan == "en"

        val imageUrl = if (isEnglish) {
            itemList[position]?.tageImage
        } else {
            itemList[position]?.tageImageFrench
        }

        val title = if (isEnglish) {
            itemList[position]?.tageName
        } else {
            itemList[position]?.tageNameFrench
        }

        Glide.with(context)
            .load(imageUrl)
            .into(holder.binding.ivMusic)

        holder.binding.tvTitle.text = title


        holder.binding.tvSessions.text = "${itemList[position]?.songCount} ${context.getString(R.string.sessions)}"

        holder.binding.root.setOnClickListener {
            listener.defaultTagItemClick(position, itemList[position]!!)
        }
        holder.binding.detailContainer.hide()
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

}