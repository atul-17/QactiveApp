package com.cumulations.libreV2.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.View
import com.cumulations.libreV2.AppConstants
import com.cumulations.libreV2.AppUtils
import com.cumulations.libreV2.adapter.CTWifiListAdapter
import com.cumulations.libreV2.model.ScanResultItem
import com.cumulations.libreV2.model.ScanResultResponse
import com.cumulations.libreV2.toHtmlSpanned
import com.libre.Network.WifiConnection
import com.libre.Scanning.Constants
import com.libre.serviceinterface.LSDeviceClient
import kotlinx.android.synthetic.main.ct_activity_wifi_list.*
import retrofit.Callback
import retrofit.RetrofitError
import retrofit.client.Response
import com.google.gson.Gson
import com.libre.LErrorHandeling.LibreError
import com.libre.R


class CTWifiListActivity: CTDeviceDiscoveryActivity() {
    private var wifiListAdapter: CTWifiListAdapter? = null
    private var filteredScanResults: ArrayList<ScanResultItem>? = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ct_activity_wifi_list)
        disableNetworkChangeCallBack()
        initViews()
        setListeners()
    }

    private fun setListeners() {
        iv_back?.setOnClickListener{ onBackPressed() }

        swipe_refresh.setOnRefreshListener {
            WifiConnection.getInstance().clearWifiScanResult()
            filteredScanResults?.clear()
            wifiListAdapter?.scanResultList?.clear()
            wifiListAdapter?.notifyDataSetChanged()
            getScanResultsFromDevice()
        }
    }

    private fun initViews() {
        toolbar.title = ""
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        wifiListAdapter = CTWifiListAdapter(this, ArrayList())
        rv_wifi_list?.layoutManager = LinearLayoutManager(this)
        rv_wifi_list?.adapter = wifiListAdapter
    }

    override fun onStart() {
        super.onStart()
        getScanResultsFromDevice()
    }

    private fun getScanResultsFromDevice(){
        if (!getConnectedSSIDName(this).contains(Constants.RIVAA_WAC_SSID)!!) {
            AppUtils.showAlertForNotConnectedToSAC(this)
            return
        }

        if (WifiConnection.getInstance().savedScanResults.isEmpty()){
            getScanResultsForIp(intent?.getStringExtra(AppConstants.DEVICE_IP)!!)
        } else {
            rv_wifi_list.visibility = View.VISIBLE
            tv_no_data.visibility = View.GONE

            filteredScanResults = WifiConnection.getInstance().savedScanResults as ArrayList<ScanResultItem>?
            wifiListAdapter?.updateList(filteredScanResults)
        }
    }

    private fun getScanResultsForIp(deviceIp: String) {
        showProgressDialog(R.string.getting_scan_results)
        swipe_refresh.isRefreshing = true
        Log.d("getScanResultsForIp","ip = $deviceIp")
        val BASE_URL = "http://$deviceIp:80"
        val lsDeviceClient = LSDeviceClient(BASE_URL)
        val deviceNameService = lsDeviceClient.deviceNameService

        deviceNameService.getScanResultV2(object : Callback<String>{
            override fun success(stringResponse: String?, response: Response?) {
                dismissDialog()
                swipe_refresh.isRefreshing = false
                if (stringResponse == null)
                    return

                /*val listType = object : TypeToken<List<ScanResultItem>>() {}.type
                val scanResultItems:List<ScanResultItem> = Gson().fromJson(stringResponse, listType)*/

                val scanResultResponse = Gson().fromJson(stringResponse,ScanResultResponse::class.java)
                        ?: return

                if (scanResultResponse.items?.isEmpty()!!){
                    tv_no_data.visibility = View.VISIBLE
                    rv_wifi_list.visibility = View.GONE
                } else {
                    rv_wifi_list.visibility = View.VISIBLE
                    tv_no_data.visibility = View.GONE

                    sortAndSaveScanResults(scanResultResponse.items)
                    wifiListAdapter?.updateList(filteredScanResults)
                }
            }

            override fun failure(error: RetrofitError?) {
                dismissDialog()
                swipe_refresh.isRefreshing = false
                Log.e("getScanResultsForIp",error?.message)
                showToast(error?.message!!)
                getScanResultsForIp(deviceIp)
            }

        })
    }

    @SuppressLint("HandlerLeak")
    internal var handler: Handler? = object : Handler() {

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            Log.d("handler","${msg.what} timeout")
            when(msg.what){
                Constants.GETTING_SCAN_RESULTS ->{
                    Log.d("handler","${msg.what} timeout")
                    dismissDialog()
                    /*showing error*/
                    val error = LibreError("", getString(R.string.requestTimeout))
                    showErrorMessage(error)
                }
            }
        }
    }

    private fun sortAndSaveScanResults(list: List<ScanResultItem>?) {
        /*Sorted in ascending order for keys*/

        val unSortedHashmap = HashMap<String,String>()
        for (item in list!!){
            item.ssid = item.ssid.toHtmlSpanned().toString()
            item.security = item.security.toHtmlSpanned().toString()
            unSortedHashmap[item.ssid] = item.security
            if (!item.ssid?.contains(Constants.RIVAA_WAC_SSID)){
                filteredScanResults?.add(item)
            }
        }

        val sortedMap = unSortedHashmap.toSortedMap()
        sortedMap.forEach { (key, value) ->
            println("$key => $value")
            WifiConnection.getInstance().putWifiScanResultSecurity(key, value)
        }
    }

    fun goBackToConnectWifiScreen(scanResultItem: ScanResultItem){
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(AppConstants.SELECTED_SSID,scanResultItem)
        })
        finish()
    }

    override fun onStop() {
        super.onStop()
        handler?.removeCallbacksAndMessages(null)
    }
}