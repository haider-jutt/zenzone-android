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

class SubAlbumSongsAdapter (val context: Context, val songsList: List<AlbumMusic?>?, val listener: SubAlbumSongAdapterListener?): RecyclerView.Adapter<SubAlbumSongsAdapter.ViewHolder>() {
    val lan = KeyStorage.getInstance(context).getString(APP_SELECTED_LANGUAGE)
    var userJsonString = KeyStorage.getInstance(context).getString(Constants.USER_DATA, "")

    class ViewHolder(val binding: SmallMusicItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(SmallMusicItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Glide.with(context).load(songsList?.get(position)?.getBackgroundVertical(lan)).into(holder.binding.ivMusic)

        var musicPack = songsList?.get(position)

        if(KeyStorage.getInstance(context).shouldShowPaidStatus(musicPack, userJsonString)) {
            holder.binding.ivPlay.visibility = GONE
            holder.binding.viewPaidContainer.visibility = VISIBLE
            holder.binding.viewPaidText.text = "${context.getString(R.string.buy).uppercase()} ${musicPack?.cost?:"0"} €"
        }else{
            holder.binding.viewPaidContainer.visibility = GONE
            holder.binding.ivPlay.visibility = VISIBLE
        }

        if (!lan.isNullOrBlank()){
            if (lan.equals("en")){
                holder.binding.tvTitle.text = songsList?.get(position)?.songName

                if (songsList?.get(position)?.isGuided == 0){
                    holder.binding.llGuided.visibility = GONE
                    holder.binding.llUnGuided.visibility = VISIBLE
                }else{
                    holder.binding.llGuided.visibility = VISIBLE
                    holder.binding.llUnGuided.visibility = GONE
                }
            }else{

                holder.binding.tvTitle.text = songsList?.get(position)?.songNameFrench

                if (songsList?.get(position)?.isFGuided == 0){
                    holder.binding.llGuided.visibility = GONE
                    holder.binding.llUnGuided.visibility = VISIBLE
                }else{
                    holder.binding.llGuided.visibility = VISIBLE
                    holder.binding.llUnGuided.visibility = GONE
                }
            }
        }else{

            holder.binding.tvTitle.text = songsList?.get(position)?.songName

            if (songsList?.get(position)?.isGuided == 0){
                holder.binding.llGuided.visibility = GONE
                holder.binding.llUnGuided.visibility = VISIBLE
            }else{
                holder.binding.llGuided.visibility = VISIBLE
                holder.binding.llUnGuided.visibility = GONE
            }
        }

        holder.binding.tvDuration.text = "${songsList?.get(position)?.getMusicDurationString()} ${context.getString(
            R.string.min)}"

        holder.binding.root.setOnClickListener {
            listener?.onSubAlbumSongClick(songsList as ArrayList<AlbumMusic?>?, songsList?.get(position)!!)
        }
    }

    override fun getItemCount(): Int {
        return songsList?.size ?: 0
    }

    interface SubAlbumSongAdapterListener{
        fun onSubAlbumSongClick(subAlbumSongsList: ArrayList<AlbumMusic?>?, songData: AlbumMusic)
    }
}