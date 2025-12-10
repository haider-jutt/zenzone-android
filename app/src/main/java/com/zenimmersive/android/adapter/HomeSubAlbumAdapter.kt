package com.zenimmersive.android.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zenimmersive.android.R
import com.zenimmersive.android.apiresponsemodel.SubAlbum
import com.zenimmersive.android.databinding.PlaylistSubalbumItemViewBinding
import com.zenimmersive.android.helper.LayoutMarginDecoration
import kotlin.math.roundToInt

class HomeSubAlbumAdapter (val context: Context, val subAlbumList: ArrayList<SubAlbum?>, val subAlbumSongsListener: SubAlbumSongsAdapter.SubAlbumSongAdapterListener?): RecyclerView.Adapter<HomeSubAlbumAdapter.ViewHolder>() {

    class ViewHolder(val binding: PlaylistSubalbumItemViewBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(PlaylistSubalbumItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        var item = subAlbumList[position]
        holder.binding.tvSubAlbumTitle.text = item?.albumName
        holder.binding.tvSessions.text = "${item?.albumMusic?.size.toString()} ${context.getString(R.string.sessions)}"

        holder.binding.rcvMusic.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        val songAdapter = SubAlbumSongsAdapter(context, item?.albumMusic, subAlbumSongsListener)
        holder.binding.rcvMusic.addItemDecoration(
            LayoutMarginDecoration(
                context.resources.getDimension(com.intuit.sdp.R.dimen._10sdp)
                    .roundToInt(), 0, 0, 0
            )
        )
        holder.binding.rcvMusic.adapter = songAdapter
    }

    override fun getItemCount(): Int {
        return subAlbumList.size
    }
}