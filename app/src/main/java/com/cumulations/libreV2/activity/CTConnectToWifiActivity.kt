package com.cumulations.libreV2.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import com.cumulations.libreV2.AppConstants
import com.cumulations.libreV2.AppUtils
import com.cumulations.libreV2.model.ScanResultItem
import com.libre.LErrorHandeling.LibreError
import com.libre.LibreApplication
import com.libre.Network.WifiConnection
import com.libre.R
import com.libre.Scanning.Constants
import com.libre.netty.BusProvider
import com.libre.serviceinterface.LSDeviceClient
import com.libre.util.LibreLogger
import kotlinx.android.synthetic.main.ct_activity_connect_to_wifi.*
import retrofit.Callback
import retrofit.RetrofitError
import retrofit.client.Response
import java.util.*

class CTConnectToWifiActivity: CTDeviceDiscoveryActivity(),View.OnClickListener {
    private val activityName by lazy {
        intent?.getStringExtra(Constants.FROM_ACTIVITY)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ct_activity_connect_to_wifi)
        initViews()
        setListeners()

        disableNetworkChangeCallBack()
    }

    private fun setListeners() {
        iv_back?.setOnClickListener(this)
        btn_cancel.setOnClickListener(this)
        btn_next.setOnClickListener(this)
        ll_select_wifi.setOnClickListener(this)
    }

    private fun initViews() {
        toolbar.title = ""
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val deviceName = intent?.getStringExtra(AppConstants.DEVICE_NAME)
        et_device_name.setText(deviceName)
        et_device_name.post {
            et_device_name.setSelection(et_device_name.length())
        }
    }

    override fun onClick(p0: View?) {
        when(p0?.id){
            R.id.iv_back -> onBackPressed()
            R.id.ll_select_wifi -> {
                startActivityForResult(Intent(this,CTWifiListActivity::class.java).apply {
                    putExtra(Constants.FROM_ACTIVITY,this@CTConnectToWifiActivity::class.java.simpleName)
                    putExtra(AppConstants.DEVICE_IP,intent?.getStringExtra(AppConstants.DEVICE_IP))
                },AppConstants.GET_SELECTED_SSID_REQUEST_CODE)
            }
            R.id.btn_next -> {
                if (fieldsValid()){

                    LibreApplication.sacDeviceNameSetFromTheApp = et_device_name.text.toString()
                    wifiConnect.setMainSSIDPwd(et_wifi_password.text.toString())

                    writeSacConfig(et_device_name.text.toString())
                }
            }
            R.id.btn_cancel -> onBackPressed()
        }
    }

    private fun fieldsValid():Boolean{
        if (et_device_name.text.toString().isEmpty()) {
            showToast(getString(R.string.device_name_empty))
            return false
        }

        if (et_wifi_password.text.toString().isEmpty()) {
            showToast(getString(R.string.password_empty_error))
            return false
        }

        if (et_wifi_password.text.toString().length<8) {
            showToast(getString(R.string.wifi_password_invalid))
            return false
        }

        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when(requestCode){
            AppConstants.GET_SELECTED_SSID_REQUEST_CODE->{
                if (resultCode == Activity.RESULT_OK){
                    val scanResultItem = data?.getSerializableExtra(AppConstants.SELECTED_SSID) as ScanResultItem
                    tv_selected_wifi?.visibility = View.VISIBLE
                    tv_selected_wifi?.text = scanResultItem.ssid
                    iv_right_arrow?.visibility = View.GONE

                    tv_security.text = "Security Type : ${scanResultItem.security}"

                    wifiConnect.setMainSSID(scanResultItem.ssid)
                    wifiConnect.setMainSSIDSec(scanResultItem.security)
                }
            }
        }
    }

    private var mDeviceNameChanged = false

    private val mHandler = @SuppressLint("HandlerLeak")
    object : Handler(){
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            when(msg?.what){

                Constants.HTTP_POST_FAILED->{
                    val error = LibreError("Not able to connect ", wifiConnect.getMainSSID())
                    BusProvider.getInstance().post(error)
                }
            }
        }
    }

    private var wifiConnect = WifiConnection.getInstance()

    private fun writeSacConfig(etDeviceName: String) {
        showProgressDialog(getString(R.string.configuring_device))
        var baseUrl = ""
        val deviceIp = intent?.getStringExtra(AppConstants.DEVICE_IP)
        /* HTTP Post */
        baseUrl = if (activityName == CTDeviceSettingsActivity::class.java.simpleName) {
            wifiConnect.mPreviousSSID = connectedSSID
            "http://$deviceIp:80"
        } else {
            "http://192.168.43.1:80"
        }

        Log.d("writeSacConfig", "Base Url = $baseUrl")
        try {
            wifiConnect = WifiConnection.getInstance()

            val deviceSSID = intent?.getStringExtra(AppConstants.DEVICE_SSID)
            val ssidDeviceName = wifiConnect.getssidDeviceNameSAC(deviceSSID)
            val params = LinkedHashMap<String, String>()
            params[AppConstants.SAC_DATA] = etDeviceName
            if (activityName == CTDeviceSettingsActivity::class.java.simpleName) {
                params[AppConstants.SAC_SSID] = wifiConnect.getMainSSID() + "\n"
            } else {
                params[AppConstants.SAC_SSID] = wifiConnect.getMainSSID()
            }

            Log.d("writeSacConfig", "sending wifi ssid " + wifiConnect.getMainSSID())
            if (wifiConnect.getMainSSIDPwd() != null)
                params[AppConstants.SAC_PASSPHRASE] = wifiConnect.getMainSSIDPwd().trim { it <= ' ' }
            else
                params[AppConstants.SAC_PASSPHRASE] = ""
            Log.d("writeSacConfig", "sending wifi passphrase " + wifiConnect.getMainSSIDPwd())

            params[AppConstants.SAC_SECURITY] = wifiConnect.getMainSSIDSec()
            LibreLogger.d(this, "sending wifi security as " + wifiConnect.getMainSSIDSec())


            if (wifiConnect.getMainSSIDSec().equals("WEP", ignoreCase = true)) {
                val mValue = WifiConnection.getInstance().getKeyIndexForWEP()
                if (mValue != null) {
                    params[AppConstants.SAC_KEY_INDEX] = mValue
                }
            }


            if (etDeviceName.isEmpty()
                    || activityName == CTDeviceSettingsActivity::class.java.simpleName
                    || etDeviceName == ssidDeviceName) {
                mDeviceNameChanged = false
                params[AppConstants.SAC_DEVICE_NAME] = ""
            } else {
                if (et_device_name.text.isNotEmpty() && et_device_name.text.toString().toByteArray().size <= 50) {
                    mDeviceNameChanged = true
                    val modifiedEtDeviceName = etDeviceName.trim { it <= ' ' } /*etDeviceName.replaceAll("\n", "");*/
                    params[AppConstants.SAC_DEVICE_NAME] = modifiedEtDeviceName
                } else {
                    if (!isFinishing) {
                        if (etDeviceName.isEmpty()) {
                            AlertDialog.Builder(this)
                                    .setTitle(getString(R.string.deviceNameChanging))
                                    .setMessage(getString(R.string.failed) + "\n " + getString(R.string.deviceNamecannotBeEmpty))
                                    .setPositiveButton(android.R.string.yes) { dialog, which ->
                                        dialog.cancel()
                                    }
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .show()
                        } else if (etDeviceName.toByteArray().size > 50) {
                            AlertDialog.Builder(this)
                                    .setTitle(getString(R.string.deviceNameChanging))
                                    .setMessage(getString(R.string.failed) + " \n " + getString(R.string.deviceLength))
                                    .setPositiveButton(android.R.string.yes) { dialog, which ->
                                        dialog.cancel()
                                    }
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .show()
                        }
                    }
                    params[AppConstants.SAC_DEVICE_NAME] = ""
                }

            }

            val lsDeviceClient = LSDeviceClient(baseUrl)
            val deviceNameService = lsDeviceClient.deviceNameService

            deviceNameService.handleSacConfiguration(params,object : Callback<String> {
                override fun success(t: String?, response: Response?) {
                    dismissDialog()
                    if (t == null)
                        return

                    if (t.contains("SAC credentials received")) {

                        mHandler.sendEmptyMessage(Constants.HTTP_POST_DONE_SUCCESSFULLY)

                        wifiConnect.setmSACDevicePostDone(true)
                        AppUtils.storeSSIDInfoToSharedPreferences(this@CTConnectToWifiActivity,wifiConnect.getMainSSID(), wifiConnect.getMainSSIDPwd())
                        if (mDeviceNameChanged) {
                            showProgressDialog(getString(R.string.deviceRebooting))
                            mDeviceNameChanged = false
                        }

                        val error = LibreError("Successfully Credentials posted ,", t)
                        BusProvider.getInstance().post(error)

//                        unbindWifiNetwork(this@CTConnectToWifiActivity)

                        LibreApplication.sacDeviceNameSetFromTheApp = et_device_name.text.toString()
                        startActivity(Intent(this@CTConnectToWifiActivity, CTConnectingToMainNetwork::class.java)
                                .apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                })
                        finish()
                    } else {
                        val error = LibreError("Error, connecting to Main Network Credentials  ," +
                                "and Got Response Message as ", t)
                        BusProvider.getInstance().post(error)
                    }
                }

                override fun failure(error: RetrofitError?) {
                    dismissDialog()
                    wifiConnect.setmSACDevicePostDone(false)
                    error?.printStackTrace()
                    Log.d("handleSacFailure", error?.message)
                    mHandler.sendEmptyMessage(Constants.HTTP_POST_FAILED)
                }

            })
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler.removeCallbacksAndMessages(null)
    }
}