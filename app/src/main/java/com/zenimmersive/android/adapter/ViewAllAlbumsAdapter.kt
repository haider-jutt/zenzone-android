package com.zenimmersive.android.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zenimmersive.android.R
import com.zenimmersive.android.apiresponsemodel.AlbumDetailsResModel
import com.zenimmersive.android.databinding.AlbumItemSmBinding
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.helper.KeyStorage.Companion.APP_SELECTED_LANGUAGE

class ViewAllAlbumsAdapter (val context: Context, val albumsList: ArrayList<AlbumDetailsResModel.Result?>, val listener: AlbumItemPressListener): RecyclerView.Adapter<ViewAllAlbumsAdapter.ViewHolder>() {
    val lan = KeyStorage.getInstance(context).getString(APP_SELECTED_LANGUAGE)

    class ViewHolder(val binding: AlbumItemSmBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(AlbumItemSmBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val lanBigBackImgSubAlbum :String?
        val lanAlbumName :String?
        val lanDescription :String?
        if (!lan.isNullOrBlank()){
            if (lan.equals("en")){
                Glide.with(context).load(albumsList[position]?.bigBackgroundImgAlbum).into(holder.binding.ivMusic)
                holder.binding.tvTitle.text = albumsList[position]?.albumName
                lanBigBackImgSubAlbum = albumsList[position]?.bigBackgroundImgAlbum
                lanAlbumName = albumsList[position]?.albumName
                lanDescription = albumsList[position]?.description
            }else{
                Glide.with(context).load(albumsList[position]?.bigBackgroundImgAlbumFrench).into(holder.binding.ivMusic)
                holder.binding.tvTitle.text = albumsList[position]?.albumNameFrench
                lanBigBackImgSubAlbum = albumsList[position]?.bigBackgroundImgAlbumFrench
                lanAlbumName = albumsList[position]?.albumNameFrench
                lanDescription = albumsList[position]?.descriptionFrench
            }
        }else{
            Glide.with(context).load(albumsList[position]?.smallBackgroundImgAlbum).into(holder.binding.ivMusic)
            holder.binding.tvTitle.text = albumsList[position]?.albumName
            lanBigBackImgSubAlbum = albumsList[position]?.bigBackgroundImgAlbum
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