package com.cumulations.libreV2.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import androidx.appcompat.app.AlertDialog
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.cumulations.libreV2.AppConstants
import com.cumulations.libreV2.AppUtils
import com.cumulations.libreV2.model.ScanResultItem
import com.libre.qactive.LErrorHandeling.LibreError
import com.libre.qactive.LibreApplication
import com.cumulations.libreV2.model.WifiConnection
import com.libre.qactive.R
import com.libre.qactive.Scanning.Constants
import com.libre.qactive.constants.LSSDPCONST
import com.libre.qactive.constants.MIDCONST
import com.libre.qactive.luci.LSSDPNodeDB
import com.libre.qactive.luci.LUCIControl
import com.libre.qactive.netty.BusProvider
import com.libre.qactive.serviceinterface.LSDeviceClient
import com.libre.qactive.util.LibreLogger
import kotlinx.android.synthetic.main.activity_google_cast_update_after_sac.*
import kotlinx.android.synthetic.main.ct_activity_connect_to_wifi.*
import kotlinx.android.synthetic.main.ct_activity_connect_to_wifi.iv_back
import kotlinx.android.synthetic.main.ct_activity_connect_to_wifi.toolbar
import kotlinx.android.synthetic.main.ct_device_settings.*
import retrofit.Callback
import retrofit.RetrofitError
import retrofit.client.Response
import java.util.*

class CTConnectToWifiActivity: CTDeviceDiscoveryActivity(),View.OnClickListener {
    private var DeviceName: String? = null
    internal var DeviceNameChanged  = false

    private val activityName by lazy {
        intent?.getStringExtra(Constants.FROM_ACTIVITY)
    }
    private val deviceSSID by lazy {
        intent?.getStringExtra(AppConstants.DEVICE_SSID)
    }
    private val deviceIP by lazy {
        intent?.getStringExtra(AppConstants.DEVICE_IP)
    }
    private val deviceName by lazy {
        intent?.getStringExtra(AppConstants.DEVICE_NAME)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ct_activity_connect_to_wifi)
        initViews()
        setListeners()

        disableNetworkChangeCallBack()
       LibreLogger.d(this,"suma in connect to wifi activity sac device name"+deviceSSID);
        LibreLogger.d(this,"suma in connect to wifi activity sac device IP"+deviceIP);
        LibreLogger.d(this,"suma in connect to wifi activity sac device Name"+deviceName);

        if (activityName == "CTConnectToWifiActivity") {

            WifiConnection.getInstance().mPreviousSSID = LibreApplication.activeSSID
            et_device_name.setText(deviceSSID)

        } else {
            et_device_name.setText(wifiConnect.getssidDeviceNameSAC(deviceSSID.toString()))

        }
        enableOrDisableEditDeviceNameButton()
    }

    private fun setListeners() {
        iv_back?.setOnClickListener(this)
        btn_cancel.setOnClickListener(this)
        btn_next.setOnClickListener(this)
        ll_select_wifi.setOnClickListener(this)
        editDeviceNameBtn.setOnClickListener(this)
    }

    private fun initViews() {
        toolbar.title = ""
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val deviceName = intent?.getStringExtra(AppConstants.DEVICE_SSID)
        et_device_name.setText(deviceName)
        et_device_name.post {
            et_device_name.setSelection(et_device_name.length())
        }
        et_device_name.isEnabled=false;

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
                    wifiConnect.setMainSSIDPwd(et_wifi_password.text.toString())
                    writeSacConfig(et_device_name.text.toString())
                }
            }
            R.id.btn_cancel -> {
                onBackPressed()
            }
            R.id.editDeviceNameBtn->{
                if (!et_device_name.isEnabled()) {

                    editDeviceNameBtn.setImageResource(R.mipmap.check)

                    et_device_name.setClickable(true)

                    et_device_name.setEnabled(true)

                    et_device_name.setFocusableInTouchMode(true)

                    et_device_name.setFocusable(true)

                    et_device_name.requestFocus()

                    et_device_name.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.cancwel, 0)

                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

                    imm.showSoftInput(et_device_name, InputMethodManager.SHOW_IMPLICIT)

                } else {

                    editDeviceNameBtn.setImageResource(R.mipmap.edit_white)

                    et_device_name.setClickable(false)

                    et_device_name.setEnabled(false)

                    et_device_name.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)

                    if (!deviceSSID.equals(et_device_name.getText().toString(), ignoreCase = true)) {
                        if (activityName.equals("LSSDPDeviceNetworkSettings", ignoreCase = true)) {
                            LUCIControl(deviceIP).SendCommand(MIDCONST.MID_DEVNAME, et_device_name.getText().toString(), LSSDPCONST.LUCI_SET)
                            LibreLogger.d(this,"suma in conect to wifi activity else save device name")
                        }
                    }

                }

            }

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

        if (tv_selected_wifi?.text?.toString()?.isEmpty()!!) {
            showToast(getString(R.string.please_selecte_wifi))
            return false
        }

        val ssid = getConnectedSSIDName(this)
        if (!(ssid?.contains(Constants.SA_SSID_RIVAA_CONCERT)!! || ssid?.contains(Constants.SA_SSID_RIVAA_STADIUM)!!)) {
            AppUtils.showAlertForNotConnectedToSAC(this)
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
            wifiConnect.mPreviousSSID = getConnectedSSIDName(this)
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
                if (et_device_name.text!!.isNotEmpty() && et_device_name.text.toString().toByteArray().size <= 50) {
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

//                        mHandler.sendEmptyMessage(Constants.HTTP_POST_DONE_SUCCESSFULLY)

                        AppUtils.storeSSIDInfoToSharedPreferences(this@CTConnectToWifiActivity,wifiConnect.getMainSSID(), wifiConnect.getMainSSIDPwd())
                        if (mDeviceNameChanged) {
                            showProgressDialog(getString(R.string.deviceRebooting))
                            mDeviceNameChanged = false
                        }

                        goToSpeakerSetupScreen(t)
                    } else {
                        val error = LibreError("Error, connecting to Main Network Credentials  ," +
                                "and Got Response Message as ", t)
                        BusProvider.getInstance().post(error)
                    }
                }

                override fun failure(error: RetrofitError?) {
                    error?.printStackTrace()
                    Log.d("handleSacFailure", error?.message)

                    dismissDialog()
                    val ssid = getConnectedSSIDName(this@CTConnectToWifiActivity)
                    if (!(ssid?.contains(Constants.SA_SSID_RIVAA_CONCERT)!! || ssid?.contains(Constants.SA_SSID_RIVAA_STADIUM)!!)) {
                        /*Sometimes in OnePlus 6, after posting sac credentials, device P2P goes off before giving success response
                        * to retrofit*/
                        goToSpeakerSetupScreen(error?.message!!)
                    } else {
                        wifiConnect.setmSACDevicePostDone(false)
                        mHandler.sendEmptyMessage(Constants.HTTP_POST_FAILED)
                    }
                }

            })
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun goToSpeakerSetupScreen(message: String) {
        wifiConnect.setmSACDevicePostDone(true)

        val error = LibreError("Credentials sent to speaker",message)
        BusProvider.getInstance().post(error)

//                        unbindWifiNetwork(this@CTConnectToWifiActivity)

        LibreApplication.sacDeviceNameSetFromTheApp = et_device_name.text.toString()
        startActivity(Intent(this@CTConnectToWifiActivity, CTConnectingToMainNetwork::class.java)
                .apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler.removeCallbacksAndMessages(null)
    }

    private fun enableOrDisableEditDeviceNameButton() {
        val mNode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(deviceIP)

        if (mNode != null && mNode.getgCastVerision() != null) {
            editDeviceNameBtn.setVisibility(View.INVISIBLE)
        } else {
            editDeviceNameBtn.setVisibility(View.VISIBLE)
        }
    }

    override fun onResume() {
        super.onResume()
        //enableOrDisableEditDeviceNameButton();
//        tv_device_name.isEnabled == false;
//        tv_device_name.setText(currentDeviceNode?.getFriendlyname())
       // DeviceName = tv_device_name.getText().toString()
//        if (activityName == "CTConnectToWifiActivity") {
//
//            WifiConnection.getInstance().mPreviousSSID = LibreApplication.activeSSID
//
//            et_device_name.setText(deviceSSID)
//
//        } else {
//            et_device_name.setText(wifiConnect.getssidDeviceNameSAC(deviceSSID.toString()))
//
//        }
    }

//    private fun enableOrDisableEditDeviceNameButton() {
//        val mNode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(currentDeviceIp)
//        if (mNode != null && mNode.getgCastVerision() != null) {
//            /*we need to show toast so don't hide*/
//            btnEditSceneName.setVisibility(View./*INVISIBLE*/VISIBLE)
//        } else {
//            btnEditSceneName.setVisibility(View.VISIBLE)
//        }
//    }
    }
