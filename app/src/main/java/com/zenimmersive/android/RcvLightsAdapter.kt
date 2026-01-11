package com.zenimmersive.android

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.zenimmersive.android.databinding.LightItemBinding
import com.zenimmersive.android.databinding.RoomItemBinding
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.hue.HueLightManager
import com.zenimmersive.android.model.Light
import com.zenimmersive.android.model.LightListResult

class RcvLightsAdapter(var MAX_HUE: Int) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var itemList = arrayListOf<Item>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == 0) {
            return RoomViewHolder(
                RoomItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }
        return LightViewHolder(
            LightItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int = itemList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        var itemType = getItemViewType(position)

        if (itemType == 0) {
            var roomHolder = holder as RoomViewHolder
            var context = holder.viewBinding.root.context.applicationContext
            roomHolder.viewBinding.viewRoomTitle.text = itemList[position].roomName
        } else {
            var lightHolder = holder as LightViewHolder
            var context = holder.viewBinding.root.context.applicationContext


            lightHolder.viewBinding.viewDeviceName.text =
                itemList[position].light?.metadata?.name ?: "UNKNOWN"

            lightHolder.viewBinding.lightState.setOnCheckedChangeListener(null)
            lightHolder.itemView.setOnClickListener(null)
            lightHolder.viewBinding.lightState.isChecked =
                itemList[position].light?.systemUseCase ?: false
            lightHolder.viewBinding.lightState.post {
                lightHolder.viewBinding.lightState.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (isChecked) {

                    }

                    itemList[position].light?.let { light ->
                        KeyStorage.getInstance(context).setLightState(light.id ?: "", isChecked)
                        HueLightManager.getInstance(context).changeLightState(
                            light,
                            isChecked
                        )
                        // Trigger Zone Update whenever selection changes
                        HueLightManager.getInstance(context).ensureZenZoneGroup()
                    }
                }

                lightHolder.itemView.setOnClickListener {
                    lightHolder.viewBinding.lightState.isChecked =
                        !lightHolder.viewBinding.lightState.isChecked
                }
            }
        }
    }

    private fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun getItemViewType(position: Int): Int = if (itemList[position].room) 0 else 1
    fun bindResult(result: LightListResult) {
        itemList.clear()
        result.rooms?.forEach {
            var item = Item()
            item.room = true
            item.roomId = it.id
            item.roomName = it.metadata?.name
            itemList.add(item)

            it.roomLightList?.forEach {
                var lightItem = Item()
                lightItem.room = false
                lightItem.light = it
                itemList.add(lightItem)
            }
        }

        result.unConfigLights?.let {
            if (it.isNotEmpty()) {
                var item = Item()
                item.room = true
                item.roomId = "-"
                item.roomName = "UNCONFIGURED"
                itemList.add(item)
            }
            it.forEach {
                var lightItem = Item()
                lightItem.room = false
                lightItem.light = it
                itemList.add(lightItem)
            }
        }

        notifyDataSetChanged()
    }

    class LightViewHolder(var viewBinding: LightItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root)

    class RoomViewHolder(var viewBinding: RoomItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root)


}

class Item {
    var room = false
    var light: Light? = null

    var roomId: String? = null
    var roomName: String? = null
}