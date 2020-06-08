package com.cumulations.libreV2.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blongho.country_data.Country
import com.blongho.country_data.World
import com.cumulations.libreV2.adapter.CTQactiveCountryAdapter
import com.cumulations.libreV2.adapter.CTQactiveProductAdapter
import com.cumulations.libreV2.utils.RecyclerItemClickListener
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.libre.qactive.R
import com.libre.qactive.Scanning.Constants
import com.libre.qactive.luci.LSSDPNodeDB
import kotlinx.android.synthetic.main.ct_activity_qactive_registraion_details.*
import kotlinx.android.synthetic.main.ct_activity_qactive_registraion_details.iv_alexa_settings
import kotlinx.android.synthetic.main.ct_activity_qactive_registraion_details.iv_back
import kotlinx.android.synthetic.main.ct_activity_user_registration.*


class CTQactiveRegDetailsActivity : CTDeviceDiscoveryActivity() {

    var bottomSheetDialog: BottomSheetDialog? = null

    var ctQactiveCountryAdapter: CTQactiveCountryAdapter? = null

    var ctQactiveProductAdapter: CTQactiveProductAdapter? = null

    var countryList: MutableList<Country> = ArrayList()

    var productList: MutableList<String> = ArrayList()

    var selectedCountryName : String=""

    var selectedProductName : String=""

    var currentIpAddress: String? = null

    var bundle = Bundle()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ct_activity_qactive_registraion_details)

        bundle = intent.extras!!

        currentIpAddress = bundle.getString(Constants.CURRENT_DEVICE_IP)

        if (currentIpAddress!=null) {
            val lssdpNodes = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(currentIpAddress)

            if (lssdpNodes.getgCastVerision() != null) {
                //gcast != null -> hide alexa
                iv_alexa_settings.visibility = View.GONE
            } else {
                iv_alexa_settings.visibility = View.VISIBLE
            }

            iv_alexa_settings?.setOnClickListener {
                val mNode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(currentIpAddress)
                if (mNode?.alexaRefreshToken?.isEmpty()!!) {
                    startActivity(Intent(this@CTQactiveRegDetailsActivity, CTAmazonLoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        putExtra(Constants.CURRENT_DEVICE_IP, currentIpAddress)
                        putExtra(Constants.FROM_ACTIVITY, CTMediaSourcesActivity::class.java.simpleName)
                    })
                } else {
                    startActivity(Intent(this@CTQactiveRegDetailsActivity, CTAlexaThingsToTryActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        putExtra(Constants.CURRENT_DEVICE_IP, currentIpAddress)
                        putExtra(Constants.FROM_ACTIVITY, CTMediaSourcesActivity::class.java.simpleName)
                    })
                }
            }
        }else{
            iv_alexa_settings.visibility = View.INVISIBLE
        }

        productList.add("Q200")
        productList.add("Q400")

        World.init(applicationContext)

        countryList = World.getAllCountries();

        countryList = countryList.sortedBy { it.name?.toString() } as MutableList<Country>

        tv_country_name.text =  countryList[0].name

        tv_product_name.text = productList[0]

        iv_back.setOnClickListener {
            onBackPressed()
        }


        btnSubmit.setOnClickListener {
            intentToHome(this)
        }



        ivShowCountryList.setOnClickListener {
            showCountryList()
        }

        ivProductList.setOnClickListener {
            showProductList()
        }

    }


    fun showCountryList() {
        val view = layoutInflater.inflate(R.layout.custom_bottom_sheet_layout, null)


        val tvHeadingLabel: AppCompatTextView = view.findViewById(R.id.tvHeadingLabel)
        val ivCloseIcon: AppCompatImageView = view.findViewById(R.id.ivCloseIcon)
        val rvList: RecyclerView = view.findViewById(R.id.rvList)

        tvHeadingLabel.text = "Countries"

        ivCloseIcon.setOnClickListener {
            bottomSheetDialog?.dismiss()
        }

        bottomSheetDialog = BottomSheetDialog(this@CTQactiveRegDetailsActivity)
        bottomSheetDialog?.setContentView(view)



        setCountryAdapter(countryList, rvList)

        bottomSheetDialog?.setCancelable(false)
        bottomSheetDialog?.show()


        rvList.addOnItemTouchListener(RecyclerItemClickListener(this@CTQactiveRegDetailsActivity,rvList,object :RecyclerItemClickListener.OnItemClickListener{
            override fun onLongItemClick(view: View?, position: Int) {
                TODO("Not yet implemented")
            }

            override fun onItemClick(view: View?, position: Int) {
                tv_country_name.text = countryList[position].name
                bottomSheetDialog?.dismiss()
            }
        }))
    }

    fun setCountryAdapter(countryList: MutableList<Country>, rvList: RecyclerView) {
        ctQactiveCountryAdapter = CTQactiveCountryAdapter(this@CTQactiveRegDetailsActivity, countryList)

        rvList.layoutManager = LinearLayoutManager(this@CTQactiveRegDetailsActivity)
        rvList.adapter = ctQactiveCountryAdapter

    }


    fun showProductList() {

        val view = layoutInflater.inflate(R.layout.custom_bottom_sheet_layout, null)

        val tvHeadingLabel: AppCompatTextView = view.findViewById(R.id.tvHeadingLabel)
        val ivCloseIcon: AppCompatImageView = view.findViewById(R.id.ivCloseIcon)
        val rvList: RecyclerView = view.findViewById(R.id.rvList)

        tvHeadingLabel.text = "Products"

        ivCloseIcon.setOnClickListener {
            bottomSheetDialog?.dismiss()
        }



        setProductAdapter(productList, rvList)

        bottomSheetDialog = BottomSheetDialog(this@CTQactiveRegDetailsActivity)
        bottomSheetDialog?.setContentView(view)

        bottomSheetDialog?.setCancelable(false)
        bottomSheetDialog?.show()


        bottomSheetDialog?.setCancelable(false)
        bottomSheetDialog?.show()

        rvList.addOnItemTouchListener(RecyclerItemClickListener(this@CTQactiveRegDetailsActivity,rvList,object :RecyclerItemClickListener.OnItemClickListener{
            override fun onLongItemClick(view: View?, position: Int) {
                TODO("Not yet implemented")
            }

            override fun onItemClick(view: View?, position: Int) {
                tv_product_name.text = productList[position]
                bottomSheetDialog?.dismiss()
            }
        }))
    }

    fun setProductAdapter(productList: MutableList<String>, rvList: RecyclerView) {
        ctQactiveProductAdapter = CTQactiveProductAdapter(this@CTQactiveRegDetailsActivity, productList)

        rvList.layoutManager = LinearLayoutManager(this@CTQactiveRegDetailsActivity)
        rvList.adapter = ctQactiveProductAdapter

    }

}