package com.zenimmersive.android.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.zenimmersive.android.databinding.SmallMusicItemBinding

class HomeOtherWanderingAdapter (val context: Context, val wanderingList: ArrayList<Int>): RecyclerView.Adapter<HomeOtherWanderingAdapter.ViewHolder>() {

    class ViewHolder(val binding: SmallMusicItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(SmallMusicItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.ivMusic.setImageDrawable(ContextCompat.getDrawable(context,wanderingList[position]))
    }

    override fun getItemCount(): Int {
        return wanderingList.size
    }
}