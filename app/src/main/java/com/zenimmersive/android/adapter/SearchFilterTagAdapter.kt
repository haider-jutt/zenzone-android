package com.zenimmersive.android.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.zenimmersive.android.R
import com.zenimmersive.android.apiresponsemodel.Tag
import com.zenimmersive.android.databinding.TextFilterItemBinding

class SearchFilterTagAdapter (val context: Context, private val filterTagList: ArrayList<Tag?>, val listener: TagsListListener): RecyclerView.Adapter<SearchFilterTagAdapter.ViewHolder>() {

    class ViewHolder(val binding: TextFilterItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(TextFilterItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.tvCategory.text = filterTagList[position]?.tageName
        if (filterTagList[position]?.selected == true){
            holder.binding.llMainView.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context,R.color.colorPrimary))
        }else{
            holder.binding.llMainView.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context,R.color.gray_blue))
        }

        holder.binding.root.setOnClickListener {
            filterTagList[position]?.selected = !filterTagList[position]!!.selected
            notifyDataSetChanged()
//            listener.tagItemClick(position, filterTagList[position])
        }
    }

    override fun getItemCount(): Int {
        return filterTagList.size
    }

    interface TagsListListener{
        fun tagItemClick(position: Int, data: Tag?)
    }

}