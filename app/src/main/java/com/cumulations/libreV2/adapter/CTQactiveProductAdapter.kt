package com.cumulations.libreV2.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blongho.country_data.Country

import com.libre.qactive.R
import kotlinx.android.synthetic.main.custom_bottom_sheet_adapter_layout.view.*

class CTQactiveProductAdapter(val context: Context,
                              val itemNamesList: List<String>) : RecyclerView.Adapter<CTQactiveProductAdapter.CTQactiveProductHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CTQactiveProductHolder {
        return CTQactiveProductHolder(LayoutInflater.from(context).inflate(R.layout.custom_bottom_sheet_adapter_layout, parent, false))
    }

    override fun getItemCount(): Int {
        return itemNamesList.size
    }

    override fun onBindViewHolder(holder: CTQactiveProductHolder, position: Int) {
        holder.bindProductItems(itemNamesList[position])
    }

    inner class CTQactiveProductHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindProductItems(productName: String) {
            itemView.tvCountryOrProductName.text = productName
        }
    }
}