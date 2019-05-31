package com.cumulations.libreV2.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.provider.Settings
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cumulations.libreV2.AppConstants
import com.cumulations.libreV2.AppUtils
import com.cumulations.libreV2.activity.CTConnectToWifiActivity
import com.cumulations.libreV2.toHtmlSpanned
import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity
import com.libre.LErrorHandeling.LibreError
import com.libre.LibreApplication
import com.libre.Network.WifiConnection
import com.libre.R
import com.libre.Scanning.Constants
import com.libre.serviceinterface.LSDeviceClient
import com.libre.util.LibreLogger
import kotlinx.android.synthetic.main.ct_fragment_device_setup_instructions.*
import retrofit.Callback
import retrofit.RetrofitError
import retrofit.client.Response

class CTDeviceSetupInfoFragment:Fragment(),View.OnClickListener {
    private val deviceDiscoveryActivity by lazy {
        activity as CTDeviceDiscoveryActivity
    }
    private var mDeviceName: String? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater?.inflate(R.layout.ct_fragment_device_setup_instructions,container,false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setListeners()
    }

    private fun setListeners() {
        btn_wifi_settings.setOnClickListener(this)
        btn_next.setOnClickListener(this)
    }

    private fun initViews() {
        btn_next.isEnabled = false
    }

    override fun onStart() {
        super.onStart()
        deviceDiscoveryActivity.checkLocationPermission()
    }

    override fun onResume() {
        super.onResume()
        if (deviceDiscoveryActivity.getConnectedSSIDName(deviceDiscoveryActivity).contains(Constants.RIVAA_WAC_SSID)!!)
            btn_next.isEnabled = true
    }

    override fun onClick(p0: View?) {
        when(p0?.id){
            R.id.btn_wifi_settings -> {

                (activity as CTDeviceDiscoveryActivity).disableNetworkChangeCallBack()
                (activity as CTDeviceDiscoveryActivity).disableNetworkOffCallBack()

                WifiConnection.getInstance().mPreviousSSID = AppUtils.getConnectedSSID(activity)
                LibreApplication.activeSSID = WifiConnection.getInstance().mPreviousSSID

                startActivityForResult(Intent(Settings.ACTION_WIFI_SETTINGS).apply {
//                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                }, AppConstants.WIFI_SETTINGS_REQUEST_CODE)

//                btn_next?.isEnabled = true
            }

            R.id.btn_next -> {
                if (!deviceDiscoveryActivity.connectedSSID?.contains(Constants.RIVAA_WAC_SSID)!!) {
                    AppUtils.showAlertForNotConnectedToSAC(deviceDiscoveryActivity)
                    return
                }

                /*Commenting for RIVAA SAC*/
                /*val phoneIpaddresss = deviceDiscoveryActivity.phoneIpAddress()
                if (phoneIpaddresss != null && phoneIpaddresss!!.contains("192.168.255."))
                    return*/

                if (deviceDiscoveryActivity.connectedSSID?.endsWith(".d")!!) {
                    return
                }

//                forceNetworkToUseCurrentWifi()
                retrieveDeviceName()
            }
        }
    }

    private fun retrieveDeviceName(){
        if (mDeviceName == null) {
            deviceDiscoveryActivity.showProgressDialog(getString(R.string.retrieving))
            if (handler?.hasMessages(AppConstants.GETTING_DEVICE_NAME)!!)
                handler?.removeMessages(AppConstants.GETTING_DEVICE_NAME)
            handler?.sendEmptyMessageDelayed(AppConstants.GETTING_DEVICE_NAME, 15000)
            getDeviceName()
        } else {
            Log.e("retrieveDeviceName","Device name = $mDeviceName")
            WifiConnection.getInstance().putssidDeviceNameSAC(deviceDiscoveryActivity.connectedSSID, mDeviceName)
            startActivity(Intent(deviceDiscoveryActivity, CTConnectToWifiActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(Constants.FROM_ACTIVITY,CTDeviceSetupInfoFragment::class.java.simpleName)
                putExtra(AppConstants.DEVICE_IP, AppConstants.SAC_IP_ADDRESS)
                putExtra(AppConstants.DEVICE_NAME, mDeviceName)
                putExtra(AppConstants.DEVICE_SSID, deviceDiscoveryActivity.connectedSSID)
            })
        }
    }

    private fun getDeviceName() {
        Log.d("GetDeviceName","start")
        val lsDeviceClient = LSDeviceClient()
        val deviceNameService = lsDeviceClient.deviceNameService

        deviceNameService.getSacDeviceName(object : Callback<String> {
            override fun success(deviceName: String, response: Response) {
                mDeviceName = deviceName.toHtmlSpanned().toString()
                LibreLogger.d(this, "Device name $mDeviceName, seeking scan result")
                if (mDeviceName != null) {
                    Log.e("SACDeviceName---", mDeviceName)
                    WifiConnection.getInstance().putssidDeviceNameSAC(deviceDiscoveryActivity.connectedSSID, mDeviceName)
                    startActivity(Intent(deviceDiscoveryActivity, CTConnectToWifiActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        putExtra(Constants.FROM_ACTIVITY,CTDeviceSetupInfoFragment::class.java.simpleName)
                        putExtra(AppConstants.DEVICE_IP, AppConstants.SAC_IP_ADDRESS)
                        putExtra(AppConstants.DEVICE_NAME, mDeviceName)
                        putExtra(AppConstants.DEVICE_SSID, deviceDiscoveryActivity.connectedSSID)
                    })
                }
            }

            override fun failure(error: RetrofitError) {
                error.printStackTrace()
                Log.d("getDeviceName","error ${error.message}")
                retrieveDeviceName()
            }
        })
    }

    @SuppressLint("HandlerLeak")
    internal var handler: Handler? = object : Handler() {

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when(msg.what){
                AppConstants.GETTING_DEVICE_NAME ->{
                    Log.d("handler","${msg.what} timeout")
                    deviceDiscoveryActivity.dismissDialog()
                    /*showing error*/
                    val error = LibreError("", getString(R.string.requestTimeout))
                    deviceDiscoveryActivity?.showErrorMessage(error)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        deviceDiscoveryActivity?.dismissDialog()
        handler?.removeMessages(AppConstants.GETTING_DEVICE_NAME)
    }
}