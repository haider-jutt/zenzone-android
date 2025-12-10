package com.zenimmersive.android.adapter

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.zenimmersive.android.R
import com.zenimmersive.android.apiresponsemodel.Tag
import com.zenimmersive.android.databinding.SearchDefaultPlaylistItemBinding
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.helper.KeyStorage.Companion.APP_SELECTED_LANGUAGE
import com.zenimmersive.android.helper.hide
import com.zenimmersive.android.helper.show

class HomeDefaultSearchTagsAdapter (val context: Context, val tagsList: ArrayList<Tag?>, val listener: DefaultSearchTagsListener): RecyclerView.Adapter<HomeDefaultSearchTagsAdapter.ViewHolder>() {
    val lan = KeyStorage.getInstance(context).getString(APP_SELECTED_LANGUAGE)
    class ViewHolder(val binding: SearchDefaultPlaylistItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(SearchDefaultPlaylistItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.tvFailureText.hide()
        holder.binding.ivMusic.show()
        holder.binding.ivMusic.scaleType = ImageView.ScaleType.FIT_XY

        val isEnglish = lan.isNullOrBlank() || lan == "en"

        val imageUrl = if (isEnglish) {
            tagsList[position]?.tageImage
        } else {
            tagsList[position]?.tageImageFrench
        }

        val title = if (isEnglish) {
            tagsList[position]?.tageName
        } else {
            tagsList[position]?.tageNameFrench
        }


        holder.binding.tvTitle.text = title
        holder.binding.tvFailureText.text = title?.uppercase()?:""
        holder.binding.tvSessions.text = "${tagsList[position]?.songCount.toString()} ${context.getString(R.string.sessions)}"


        Glide.with(context).load(imageUrl).placeholder(R.drawable.placeholder).error(R.drawable.placeholder_error)
            .addListener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable?>,
                    isFirstResource: Boolean
                ): Boolean {
                    holder.binding.tvFailureText.show()
                    holder.binding.ivMusic.hide()
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable?>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

            })
            .into(holder.binding.ivMusic)



        holder.binding.root.setOnClickListener {
            listener.defaultTagItemClick(position, tagsList[position]!!)
        }
        holder.binding.detailContainer.hide()
    }

    override fun getItemCount(): Int {
        return tagsList.size
    }

    interface DefaultSearchTagsListener{
        fun defaultTagItemClick(position: Int, data: Tag)
    }
}