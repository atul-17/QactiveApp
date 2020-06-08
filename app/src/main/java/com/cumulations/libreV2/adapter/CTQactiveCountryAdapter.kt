package com.cumulations.libreV2.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blongho.country_data.Country
import com.libre.qactive.R
import kotlinx.android.synthetic.main.custom_bottom_sheet_adapter_layout.view.*

class CTQactiveCountryAdapter(val context: Context, val itemNamesList: List<Country>) : RecyclerView.Adapter<CTQactiveCountryAdapter.CTQactiveCountryProdctHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CTQactiveCountryProdctHolder {

        return CTQactiveCountryProdctHolder(LayoutInflater.from(context).inflate(R.layout.custom_bottom_sheet_adapter_layout, parent, false))
    }

    override fun getItemCount(): Int {
        return itemNamesList.size
    }

    override fun onBindViewHolder(holder: CTQactiveCountryProdctHolder, position: Int) {
        val countryOrProductName: String = itemNamesList[position].name
        holder.bindCountryItems(countryOrProductName)
    }


    inner class CTQactiveCountryProdctHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindCountryItems(countryOrProductName: String) {
            itemView.tvCountryOrProductName.text = countryOrProductName
        }


    }
}