package com.cumulations.libreV2.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cumulations.libreV2.activity.CTWifiListActivity
import com.cumulations.libreV2.model.ScanResultItem
import com.libre.R
import kotlinx.android.synthetic.main.ct_list_item_wifi.view.*

class CTWifiListAdapter(val context: Context,
                        var scanResultList: MutableList<ScanResultItem>?) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, i: Int): ScanResultItemViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.ct_list_item_wifi, parent, false)
        return ScanResultItemViewHolder(view)
    }


    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        val scanResultItem = scanResultList?.get(position)
        if (viewHolder is ScanResultItemViewHolder){
            viewHolder.bindScanResultItem(scanResultItem,position)
        }
    }

    override fun getItemCount(): Int {
        return scanResultList?.size!!
    }

    inner class ScanResultItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindScanResultItem(scanResultItem: ScanResultItem?, position: Int) {
            itemView.tv_ssid_name.text = scanResultItem?.ssid
            itemView.tv_ssid_security.text = scanResultItem?.security?.toUpperCase()

            itemView.ll_ssid.setOnClickListener {
                if (context is CTWifiListActivity)
                    context.goBackToConnectWifiScreen(scanResultItem!!)
            }
        }
    }

    fun updateList(scanResultList: MutableList<ScanResultItem>?) {
        this.scanResultList = scanResultList
        notifyDataSetChanged()
    }
}



