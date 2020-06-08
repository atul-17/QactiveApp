package com.cumulations.libreV2.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cumulations.libreV2.activity.CTDMSBrowserActivityV2
import com.cumulations.libreV2.activity.CTUpnpFileBrowserActivity
import com.libre.qactive.R
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.ct_list_item_didl_object.view.*
import org.fourthline.cling.support.model.DIDLObject
import org.fourthline.cling.support.model.container.Container
import org.fourthline.cling.support.model.item.Item
import java.net.MalformedURLException

class CTDIDLObjectListAdapter(val context: Context,
                              var didlObjectList: MutableList<DIDLObject>?) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, i: Int): DIDLObjectItemViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.ct_list_item_didl_object, parent, false)
        return DIDLObjectItemViewHolder(view)
    }


    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        val scanResultItem = didlObjectList?.get(position)
        if (viewHolder is DIDLObjectItemViewHolder){
            viewHolder.bindDIDLObjectItem(scanResultItem,position)
        }
    }

    override fun getItemCount(): Int {
        return didlObjectList?.size!!
    }

    inner class DIDLObjectItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindDIDLObjectItem(didlObject: DIDLObject?, position: Int) {
            
            itemView.tv_didl_item_name.text = didlObject?.title
            itemView.tv_didl_item_artist.text = didlObject?.creator

            if (didlObject is Item) {
                Log.d("bindDataItem", "Object is of type Item : ")

                itemView.tv_didl_item_artist?.visibility = View.VISIBLE
                itemView.tv_container_count?.visibility = View.GONE
                val uri = didlObject.getFirstPropertyValue(DIDLObject.Property.UPNP.ALBUM_ART_URI::class.java)
                if (uri != null) {
                    try {
                        val androiduri = android.net.Uri.parse(uri!!.toString())


                        /*when{
                            didlObject?.refID?.contains(ContentTree.AUDIO_ALBUMS_ID)!! -> {
                                Picasso.with(context)
                                        .load(androiduri)
                                        .placeholder(R.drawable.albums_border)
                                        .error(R.drawable.albums_border)
                                        .into(itemView.iv_didl_item_album_art)
                            }

                            didlObject?.refID?.contains(ContentTree.AUDIO_ARTISTS_ID)!! -> {
                                Picasso.with(context)
                                        .load(androiduri)
                                        .placeholder(R.drawable.artists_border)
                                        .error(R.drawable.artists_border)
                                        .into(itemView.iv_didl_item_album_art)
                            }

                            didlObject?.refID?.contains(ContentTree.AUDIO_GENRES_ID)!! -> {
                                Picasso.with(context)
                                        .load(androiduri)
                                        .placeholder(R.drawable.album_borderless)
                                        .error(R.drawable.album_borderless)
                                        .into(itemView.iv_didl_item_album_art)
                            }

                            else -> {
                                Picasso.with(context)
                                        .load(androiduri)
                                        .placeholder(R.drawable.songs_border)
                                        .error(R.drawable.songs_border)
                                        .into(itemView.iv_didl_item_album_art)
                            }
                        }*/

                        Picasso.with(context)
                                .load(androiduri)
                                .placeholder(R.drawable.songs_border)
                                .error(R.drawable.songs_border)
                                .into(itemView.iv_didl_item_album_art)
                        Log.d("bindDataItem", "Item : " + uri!!.toURL() + " " + didlObject.getTitle())
                    } catch (e: MalformedURLException) {
                        e.printStackTrace()
                        Log.d("bindDataItem", "exception "+e.message)
                    }

                } else {
                    itemView.iv_didl_item_album_art.setImageResource(R.drawable.songs_border)
                }
            } else {
                when{
                    didlObject?.clazz?.value?.contains("album",true)!! ->
                        itemView.iv_didl_item_album_art.setImageResource(R.drawable.albums_border)

                    didlObject?.clazz?.value?.contains("artist",true)!! ->
                        itemView.iv_didl_item_album_art.setImageResource(R.drawable.artists_border)

                    didlObject?.clazz?.value?.contains("genre",true)!! ->
                        itemView.iv_didl_item_album_art.setImageResource(R.drawable.album_borderless)
                    else -> itemView.iv_didl_item_album_art.setImageResource(R.drawable.songs_border)
                }

                itemView.tv_container_count?.visibility = View.VISIBLE
                itemView.tv_didl_item_artist?.visibility = View.GONE
                itemView.tv_container_count.text = (didlObject as Container).childCount?.toString()
            }

            itemView.ll_didl_item.setOnClickListener {
                if (context is CTUpnpFileBrowserActivity){
                    context.handleDIDLObjectClick(position)
                }

                if (context is CTDMSBrowserActivityV2){
                    context.handleDIDLObjectClick(position)
                }
            }
        }
    }

    fun updateList(didlObjectList: MutableList<DIDLObject>?) {
        this.didlObjectList = didlObjectList
        notifyDataSetChanged()
    }
}



