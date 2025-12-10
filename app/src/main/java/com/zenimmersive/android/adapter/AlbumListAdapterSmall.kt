package com.zenimmersive.android.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zenimmersive.android.R
import com.zenimmersive.android.apiresponsemodel.HomeDataResModel
import com.zenimmersive.android.databinding.AlbumItemSsmBinding
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.helper.KeyStorage.Companion.APP_SELECTED_LANGUAGE

class AlbumListAdapterSmall (val context: Context, val albumsList: ArrayList<HomeDataResModel.Result.Album?>, val listener: AlbumItemPressListener): RecyclerView.Adapter<AlbumListAdapterSmall.ViewHolder>() {
    val lan = KeyStorage.getInstance(context).getString(APP_SELECTED_LANGUAGE)

    class ViewHolder(val binding: AlbumItemSsmBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(AlbumItemSsmBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val lanBigBackImgSubAlbum :String?
        val lanAlbumName :String?
        val lanDescription :String?
        if (!lan.isNullOrBlank()){
            if (lan.equals("en")){
                Glide.with(context).load(albumsList[position]?.bigBackgroundImgSubAlbum).into(holder.binding.ivMusic)
                holder.binding.tvTitle.text = albumsList[position]?.albumName
                lanBigBackImgSubAlbum = albumsList[position]?.bigBackgroundImgSubAlbum
                lanAlbumName = albumsList[position]?.albumName
                lanDescription = albumsList[position]?.description
            }else{
                Glide.with(context).load(albumsList[position]?.bigBackgroundImgSubAlbumFrench).into(holder.binding.ivMusic)
                holder.binding.tvTitle.text = albumsList[position]?.albumNameFrench
                lanBigBackImgSubAlbum = albumsList[position]?.bigBackgroundImgSubAlbumFrench
                lanAlbumName = albumsList[position]?.albumNameFrench
                lanDescription = albumsList[position]?.descriptionFrench
            }
        }else{
            Glide.with(context).load(albumsList[position]?.smallBackgroundImgSubAlbum).into(holder.binding.ivMusic)
            holder.binding.tvTitle.text = albumsList[position]?.albumName
            lanBigBackImgSubAlbum = albumsList[position]?.bigBackgroundImgSubAlbum
            lanAlbumName = albumsList[position]?.albumName
            lanDescription = albumsList[position]?.description
        }

        holder.binding.tvSessions.text = "${albumsList[position]?.totalSong} ${context.getString(R.string.sessions)}"
        
        holder.binding.root.setOnClickListener {
            listener.albumClickListener(albumsList[position]?.albumId!!, lanBigBackImgSubAlbum?:"", lanAlbumName?:"", albumsList[position]?.totalSong!!, lanDescription ?: "")
        }
    }

    override fun getItemCount(): Int {
        return albumsList.size
    }


}