package com.cumulations.libreV2.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cumulations.libreV2.AppConstants
import com.cumulations.libreV2.AppUtils
import com.cumulations.libreV2.activity.CTConnectToWifiActivity
import com.cumulations.libreV2.toHtmlSpanned
import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity
import com.libre.qactive.LErrorHandeling.LibreError
import com.libre.qactive.LibreApplication
import com.cumulations.libreV2.model.WifiConnection
import com.libre.qactive.R
import com.libre.qactive.Scanning.Constants
import com.libre.qactive.serviceinterface.LSDeviceClient
import com.libre.qactive.util.LibreLogger
import kotlinx.android.synthetic.main.ct_fragment_device_setup_instructions.*
import retrofit.Callback
import retrofit.RetrofitError
import retrofit.client.Response

class CTDeviceSetupInfoFragment: Fragment(),View.OnClickListener {
    private val deviceDiscoveryActivity by lazy {
        activity as CTDeviceDiscoveryActivity
    }
    private var mDeviceName: String? = null
    private var ssid: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater?.inflate(R.layout.ct_fragment_device_setup_instructions,container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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

//        This is hot fix. Need to check scenario when speaker in SAC/SA mode connected to more then 1 app
//        phoneIp = 192.168.43.149 when app wifi connected to SA
//        phoneIp = 192.168.255.254 when app wifi connected to SAC*/
//        For sac gateway 192.168.255.249
//        For sa gateway 192.168.43.2
//        LibreLogger.d(this,"onResume, phoneIp = "+deviceDiscoveryActivity.phoneIpAddress())
//        LibreLogger.d(this,"onResume, connected router Ip = "+WifiUtil(deviceDiscoveryActivity).getConnectedRouterIp())
//        btn_next.isEnabled = deviceDiscoveryActivity.phoneIpAddress().contains(AppConstants.SAC_MODE_IP)

        Handler().postDelayed({
            ssid = deviceDiscoveryActivity.getConnectedSSIDName(deviceDiscoveryActivity)
            tv_connected_ssid?.text = "Connected ssid : $ssid"
            btn_next?.isEnabled =
                    ssid?.contains(Constants.SA_SSID_RIVAA_CONCERT)!! || ssid?.contains(Constants.SA_SSID_RIVAA_STADIUM)!!
        },300)
    }

    override fun onClick(p0: View?) {
        when(p0?.id){
            R.id.btn_wifi_settings -> {

                (activity as CTDeviceDiscoveryActivity).disableNetworkChangeCallBack()
                (activity as CTDeviceDiscoveryActivity).disableNetworkOffCallBack()

                WifiConnection.getInstance().mPreviousSSID = AppUtils.getConnectedSSID(activity!!)
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
                if (!(ssid?.contains(Constants.SA_SSID_RIVAA_CONCERT)!! || ssid?.contains(Constants.SA_SSID_RIVAA_STADIUM)!!)) {
                    AppUtils.showAlertForNotConnectedToSAC(deviceDiscoveryActivity)
                    return
                }

                val connectedSSID = deviceDiscoveryActivity?.getConnectedSSIDName(deviceDiscoveryActivity)
                if (connectedSSID?.endsWith(".d")!!) {
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
            val connectedSSID = deviceDiscoveryActivity?.getConnectedSSIDName(deviceDiscoveryActivity)
            WifiConnection.getInstance().putssidDeviceNameSAC(connectedSSID, mDeviceName)
            startActivity(Intent(deviceDiscoveryActivity, CTConnectToWifiActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(Constants.FROM_ACTIVITY,CTDeviceSetupInfoFragment::class.java.simpleName)
                putExtra(AppConstants.DEVICE_IP, AppConstants.SAC_IP_ADDRESS)
                putExtra(AppConstants.DEVICE_NAME, mDeviceName)
                putExtra(AppConstants.DEVICE_SSID, connectedSSID)
            })
        }
    }

    private fun getDeviceName() {
        Log.d("GetDeviceName","start")
        val lsDeviceClient = LSDeviceClient()
        val deviceNameService = lsDeviceClient.deviceNameService

        deviceNameService.getSacDeviceName(object : Callback<String> {
            override fun success(deviceName: String, response: Response) {
                deviceDiscoveryActivity?.dismissDialog()
                mDeviceName = deviceName.toHtmlSpanned().toString()
                LibreLogger.d(this, "Device name $mDeviceName, seeking scan result")
                if (mDeviceName != null) {
                    Log.e("SACDeviceName---", mDeviceName)
                    val connectedSSID = deviceDiscoveryActivity?.getConnectedSSIDName(deviceDiscoveryActivity)
                    WifiConnection.getInstance().putssidDeviceNameSAC(connectedSSID, mDeviceName)
                    startActivity(Intent(deviceDiscoveryActivity, CTConnectToWifiActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        putExtra(Constants.FROM_ACTIVITY,CTDeviceSetupInfoFragment::class.java.simpleName)
                        putExtra(AppConstants.DEVICE_IP, AppConstants.SAC_IP_ADDRESS)
                        putExtra(AppConstants.DEVICE_NAME, mDeviceName)
                        putExtra(AppConstants.DEVICE_SSID, connectedSSID)
                    })
                }
            }

            override fun failure(error: RetrofitError) {
                deviceDiscoveryActivity?.dismissDialog()
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

    override fun onStop() {
        super.onStop()
        deviceDiscoveryActivity?.dismissDialog()
        handler?.removeMessages(AppConstants.GETTING_DEVICE_NAME)
    }
}
