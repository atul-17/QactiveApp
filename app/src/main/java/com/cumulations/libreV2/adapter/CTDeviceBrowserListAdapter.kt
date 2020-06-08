package com.cumulations.libreV2.adapter

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cumulations.libreV2.activity.CTDeviceBrowserActivity
import com.cumulations.libreV2.model.DataItem
import com.libre.qactive.R
import com.libre.qactive.util.PicassoTrustCertificates
import kotlinx.android.synthetic.main.ct_remotecommand_item.view.*

class CTDeviceBrowserListAdapter(val context: Context,
                                 var dataItemList: MutableList<DataItem>?) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, i: Int): DataItemViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.ct_remotecommand_item, parent, false)
        return DataItemViewHolder(view)
    }


    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        val scanResultItem = dataItemList?.get(position)
        if (viewHolder is DataItemViewHolder) {
            viewHolder.bindDataItem(scanResultItem, position)
        }
    }

    override fun getItemCount(): Int {
        return dataItemList?.size!!
    }

    inner class DataItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindDataItem(dataItem: DataItem?, position: Int) {
            itemView?.item_title?.text = dataItem?.itemName
            itemView?.item_title?.isSelected = true

            val folder = " Folder"
            val file = " File "

            if (dataItem?.itemType == folder) {
                itemView?.item_icon?.setImageResource(R.drawable.album_borderless)
            } else {
                if (!dataItem?.itemAlbumURL.isNullOrEmpty()) {
                    // default image for tidal file is tidal logo, else load the image in the URL
                    if (dataItem?.itemType == folder) {
                        PicassoTrustCertificates.getInstance(context).load(dataItem?.itemAlbumURL)
                                .placeholder(R.drawable.album_borderless)
                                .error(R.drawable.album_borderless)
                                .into(itemView?.item_icon)
                    } else {
                        PicassoTrustCertificates.getInstance(context).load(dataItem?.itemAlbumURL)
                                .placeholder(R.drawable.songs_borderless)
                                .error(R.drawable.songs_borderless)
                                .into(itemView?.item_icon)
                    }
                } else {
                    itemView?.item_icon?.setImageResource(R.drawable.songs_borderless)
                }
            }

            itemView?.item_fav_button?.visibility = View.VISIBLE

            if (dataItem?.favorite == null) return

            when (dataItem?.favorite) {
                0 -> itemView?.item_fav_button?.visibility = View.GONE
                1 -> itemView?.item_fav_button?.setImageResource(R.mipmap.ic_remote_not_favorite)
                2 -> itemView?.item_fav_button?.setImageResource(R.mipmap.ic_remote_favorite)
            }

            itemView?.row_layout?.setOnClickListener {
                if (context is CTDeviceBrowserActivity){
                    context.onDataItemClicked(position)
                }
            }

            itemView?.item_fav_button?.setOnClickListener {
                if (context is CTDeviceBrowserActivity){
                    context.onFavClicked(position)
                }
            }
        }
    }

    fun updateList(dataItemList: MutableList<DataItem>?) {
        this.dataItemList = dataItemList
        notifyDataSetChanged()
    }
}



