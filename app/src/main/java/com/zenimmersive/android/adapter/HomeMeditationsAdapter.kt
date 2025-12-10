package com.zenimmersive.android.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zenimmersive.android.R
import com.zenimmersive.android.apiresponsemodel.Tag
import com.zenimmersive.android.databinding.MeditationMusicItemBinding
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.helper.KeyStorage.Companion.APP_SELECTED_LANGUAGE

class HomeMeditationsAdapter (val context: Context, val meditationList: ArrayList<Tag?>, val listener: HomeMeditationsAdapter): RecyclerView.Adapter<HomeMeditationsAdapter.ViewHolder>() {
    val lan = KeyStorage.getInstance(context).getString(APP_SELECTED_LANGUAGE)

    class ViewHolder(val binding: MeditationMusicItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(MeditationMusicItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (!lan.isNullOrBlank()){
            if (lan.equals("en")){
                Glide.with(context).load(meditationList[position]?.tageImage).into(holder.binding.ivMusic)
                holder.binding.tvTitle.text = meditationList[position]?.tageName
            }else{
                Glide.with(context).load(meditationList[position]?.tageImageFrench).into(holder.binding.ivMusic)
                holder.binding.tvTitle.text = meditationList[position]?.tageNameFrench
            }
        }else{
            Glide.with(context).load(meditationList[position]?.tageImage).into(holder.binding.ivMusic)
            holder.binding.tvTitle.text = meditationList[position]?.tageName
        }

        holder.binding.tvSessions.text = "${meditationList[position]?.songCount} ${context.getString(R.string.sessions)}"

        holder.binding.root.setOnClickListener {
            listener.meditationItemClick(position, meditationList[position])
        }
    }

    override fun getItemCount(): Int {
        return meditationList.size
    }

    interface HomeMeditationsAdapter{
        fun meditationItemClick(position: Int, data: Tag?)
    }
}