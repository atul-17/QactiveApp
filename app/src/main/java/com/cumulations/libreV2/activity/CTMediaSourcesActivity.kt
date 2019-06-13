package com.cumulations.libreV2.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import com.cumulations.libreV2.AppConstants
import com.cumulations.libreV2.SharedPreferenceHelper
import com.cumulations.libreV2.model.SceneObject
import com.cumulations.libreV2.tcp_tunneling.TCPTunnelPacket
import com.cumulations.libreV2.tcp_tunneling.TunnelingControl
import com.cumulations.libreV2.tcp_tunneling.TunnelingData
import com.cumulations.libreV2.tcp_tunneling.enums.PayloadType
import com.libre.LErrorHandeling.LibreError
import com.libre.LibreApplication
import com.libre.R
import com.libre.Scanning.Constants
import com.libre.Scanning.ScanningHandler
import com.libre.alexa.AlexaUtils
import com.libre.constants.LSSDPCONST
import com.libre.constants.LUCIMESSAGES
import com.libre.constants.MIDCONST
import com.libre.luci.LSSDPNodeDB
import com.libre.luci.LSSDPNodes
import com.libre.luci.LUCIControl
import com.libre.luci.LUCIPacket
import com.libre.netty.LibreDeviceInteractionListner
import com.libre.netty.NettyData
import com.libre.util.LibreLogger
import kotlinx.android.synthetic.main.alert_checkbox.view.*
import kotlinx.android.synthetic.main.ct_activity_media_sources.*
import kotlinx.android.synthetic.main.ct_list_item_media_sources.view.*
import kotlinx.android.synthetic.main.music_playing_widget.*
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class CTMediaSourcesActivity : CTDeviceDiscoveryActivity(), LibreDeviceInteractionListner {

    companion object {
        const val TAG_CMD_ID = "CMD ID"
        const val TAG_WINDOW_CONTENT = "Window CONTENTS"
        const val TAG_BROWSER = "Browser"
        const val GET_HOME = "GETUI:HOME"
        const val BLUETOOTH_OFF = "OFF"
        const val BLUETOOTH_ON = "ON"
        const val NETWORK_TIMEOUT = 1
        const val AUX_BT_TIMEOUT = 0x2
        const val ALEXA_REFRESH_TOKEN_TIMER = 0x12
        const val ACTION_INITIATED = 12345
        const val BT_AUX_INITIATED = ACTION_INITIATED
    }

    private lateinit var adapter: CTSourcesListAdapter

    private val mScanHandler = ScanningHandler.getInstance()

    private val myDevice = "My Device"
    private val usbStorage = "USB Storage"
    private val mediaServer = "Media Server"
    //    private static String deezer = "Deezer";
    //    private static String tidal = "TIDAL";
    //    private static String favourite = "Favourites";
    //    private static String vtuner = "vTuner";
    private val spotify = "Spotify"
    //    private static String tuneIn = "TuneIn";
    //    private static String qmusic = "QQ Music";

    private var currentIpAddress: String? = null
    private var currentSource: String? = null
    private var currentSourceIndexSelected = -1

    private val mediaSourcesList: MutableList<String> = ArrayList()


    @SuppressLint("HandlerLeak")
    private val timeOutHandler: Handler? = object : Handler() {
        override fun handleMessage(msg: Message) {

            if (msg.what == NETWORK_TIMEOUT) {
                LibreLogger.d(this, "recieved handler msg")
                closeLoader()
                /*showing error to user*/
                val error = LibreError(currentIpAddress, getString(R.string.requestTimeout))
                showErrorMessage(error)
            }

            if (msg.what == ACTION_INITIATED) {
                showLoader()
            }

            if (msg.what == BT_AUX_INITIATED) {
                val showMessage = msg.data.getString("MessageText")
                showLoaderAndAskSource(showMessage)
            }
            if (msg.what == AUX_BT_TIMEOUT) {
                closeLoader()
                val error = LibreError(currentIpAddress, getString(R.string.requestTimeout))
                showErrorMessage(error)
            }
            if (msg.what == ALEXA_REFRESH_TOKEN_TIMER) {
                closeLoader()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ct_activity_media_sources)

        toolbar?.title = ""
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(false)

        currentIpAddress = intent.getStringExtra(Constants.CURRENT_DEVICE_IP)
        currentSource = intent.getStringExtra(Constants.CURRENT_SOURCE)
    }

    override fun onStart() {
        super.onStart()
        initViews()
        setMusicPlayerWidget(fl_music_play_widget,currentIpAddress!!)
        setListeners()
    }

    private fun setListeners() {
        iv_toggle_bluetooth?.setOnClickListener {
            val luciControl = LUCIControl(currentIpAddress)
            if (iv_toggle_bluetooth?.isChecked!!) {
                /*if (iv_toggle_aux?.isChecked!!) {
                    /*WiFi == No Source*/
                    TunnelingControl(currentIpAddress).sendCommand(PayloadType.DEVICE_SOURCE,0x01*//*WiFi*//*)
//                    luciControl.SendCommand(MIDCONST.MID_AUX_STOP, null, LSSDPCONST.LUCI_SET)
                }
                // sleep when BT to AUX switch
//                luciControl.SendCommand(MIDCONST.MID_BLUETOOTH, BLUETOOTH_OFF, LSSDPCONST.LUCI_SET)
                TunnelingControl(currentIpAddress).sendCommand(PayloadType.DEVICE_SOURCE,0x01*//*WiFi*//*)
                timeOutHandler!!.sendEmptyMessageDelayed(AUX_BT_TIMEOUT, Constants.LOADING_TIMEOUT.toLong())

                val msg = Message().apply {
                    what = BT_AUX_INITIATED
                    data = Bundle().apply {
                        putString("MessageText", getString(R.string.BtOffAlert))
                    }
                }
                timeOutHandler!!.sendMessage(msg)*/
            } else {
//                luciControl.SendCommand(MIDCONST.MID_BLUETOOTH, BLUETOOTH_ON, LSSDPCONST.LUCI_SET)
                TunnelingControl(currentIpAddress).sendCommand(PayloadType.DEVICE_SOURCE,0x02/*BT*/)
                timeOutHandler!!.sendEmptyMessageDelayed(AUX_BT_TIMEOUT, Constants.LOADING_TIMEOUT.toLong())
                val msg = Message().apply {
                    what = BT_AUX_INITIATED
                    data = Bundle().apply {
                        putString("MessageText", getString(R.string.BtOnAlert))
                    }
                }
                timeOutHandler!!.sendMessage(msg)
            }
        }

        iv_toggle_aux?.setOnClickListener {
            val luciControl = LUCIControl(currentIpAddress)
            if (iv_toggle_aux.isChecked) {
                /*if (iv_toggle_bluetooth!!.isChecked) {
                    TunnelingControl(currentIpAddress).sendCommand(PayloadType.DEVICE_SOURCE,0x01*//*WiFi*//*)
//                    luciControl.SendCommand(MIDCONST.MID_BLUETOOTH, BLUETOOTH_OFF, LSSDPCONST.LUCI_SET)
                }

//                luciControl.SendCommand(MIDCONST.MID_AUX_STOP, null, LSSDPCONST.LUCI_SET)
                TunnelingControl(currentIpAddress).sendCommand(PayloadType.DEVICE_SOURCE,0x01*//*WiFi*//*)
                timeOutHandler!!.sendEmptyMessageDelayed(AUX_BT_TIMEOUT, Constants.LOADING_TIMEOUT.toLong())

                val msg = Message().apply {
                    what = BT_AUX_INITIATED
                    data = Bundle().apply {
                        putString("MessageText", getString(R.string.AuxOffAlert))
                    }
                }
                timeOutHandler!!.sendMessage(msg)*/
            } else {
                /* Setting the source to default */
//                luciControl.SendCommand(MIDCONST.MID_AUX_START, null, LSSDPCONST.LUCI_SET)
                TunnelingControl(currentIpAddress).sendCommand(PayloadType.DEVICE_SOURCE,0x00/*AUX*/)
                timeOutHandler!!.sendEmptyMessageDelayed(AUX_BT_TIMEOUT, Constants.LOADING_TIMEOUT.toLong())

                val msg = Message().apply {
                    what = BT_AUX_INITIATED
                    data = Bundle().apply {
                        putString("MessageText", getString(R.string.AuxOnAlert))
                    }
                }
                timeOutHandler!!.sendMessage(msg)
            }
        }

        seek_bar_volume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                Log.d("onProgressChanged $progress", "fromUser $fromUser")
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                Log.d("onStartTracking", "${seekBar.progress}")
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

                LibreLogger.d("onStopTracking", "${seekBar.progress}")

                if (seekBar.progress==0){
                    iv_volume_mute?.setImageResource(R.drawable.ic_volume_mute)
                } else iv_volume_mute?.setImageResource(R.drawable.volume_low_enabled)

                val sceneObject = mScanHandler.sceneObjectFromCentralRepo[currentIpAddress]
                LUCIControl.SendCommandWithIp(MIDCONST.VOLUME_CONTROL, "" + seekBar.progress, LSSDPCONST.LUCI_SET, sceneObject?.ipAddress)
                sceneObject?.volumeValueInPercentage = seekBar.progress
                mScanHandler.putSceneObjectToCentralRepo(sceneObject?.ipAddress, sceneObject)

//                TunnelingControl(currentIpAddress).sendCommand(PayloadType.DEVICE_VOLUME,(seekBar.progress/5).toByte())
            }
        })

        iv_back?.setOnClickListener {
            onBackPressed()
        }

        iv_alexa_settings?.setOnClickListener {
            val mNode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(currentIpAddress)
            if (mNode?.alexaRefreshToken?.isEmpty()!!) {
                startActivity(Intent(this@CTMediaSourcesActivity, CTAmazonLoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(Constants.CURRENT_DEVICE_IP, currentIpAddress)
                    putExtra(Constants.FROM_ACTIVITY, CTMediaSourcesActivity::class.java.simpleName)
                })
            } else {
                startActivity(Intent(this@CTMediaSourcesActivity, CTAlexaThingsToTryActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(Constants.CURRENT_DEVICE_IP, currentIpAddress)
                    putExtra(Constants.FROM_ACTIVITY, CTMediaSourcesActivity::class.java.simpleName)
                })
            }
        }

        iv_device_settings?.setOnClickListener {
            startActivity(Intent(this@CTMediaSourcesActivity, CTDeviceSettingsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(Constants.CURRENT_DEVICE_IP, currentIpAddress)
                putExtra(Constants.FROM_ACTIVITY, CTMediaSourcesActivity::class.java.simpleName)
            })
        }
    }

    private fun initViews() {
        val lssdpNodes = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(currentIpAddress)

        tv_device_name?.text = lssdpNodes?.friendlyname

        adapter = CTSourcesListAdapter(this, mediaSourcesList)
        rv_media_sources_list?.layoutManager = LinearLayoutManager(this)
        rv_media_sources_list?.adapter = adapter

        if (lssdpNodes?.getgCastVerision() != null
                && !lssdpNodes.networkMode.contains("P2P")) {
            mediaSourcesList.clear()
//            mediaSourcesList.add(spotify)
            adapter.notifyDataSetChanged()
        } else {
            mediaSourcesList.clear()
            mediaSourcesList.add(myDevice)
            mediaSourcesList.add(usbStorage)
            mediaSourcesList.add(mediaServer)
//            mediaSourcesList.add(spotify)
            adapter.notifyDataSetChanged()
        }

        if (lssdpNodes?.alexaRefreshToken.isNullOrEmpty() && !SharedPreferenceHelper(this).isAlexaLoginAlertDontAskChecked(currentIpAddress!!)){
            showAlexaLoginAlert()
        }

        if (currentSource != null) {
            val currentSource = Integer.valueOf(currentSource)
            val sceneObject = mScanHandler.sceneObjectFromCentralRepo[currentIpAddress] ?: return

            /* Karuna , if Zone is playing in BT/AUX and We Released the Zone
            and we creating the same Guy as a Master then Aux should not Switch ON as a Default*/
            if ((currentSource == Constants.AUX_SOURCE || currentSource == Constants.EXTERNAL_SOURCE)
                    && (sceneObject.playstatus == SceneObject.CURRENTLY_STOPPED
                            || sceneObject.playstatus == SceneObject.CURRENTLY_NOTPLAYING)) {
                Log.d("AUXSTATE", "--" + sceneObject.playstatus)
                iv_toggle_aux?.isChecked = false
            }
            if (currentSource == Constants.BT_SOURCE
                    && (sceneObject.playstatus == SceneObject.CURRENTLY_STOPPED
                            || sceneObject.playstatus == SceneObject.CURRENTLY_NOTPLAYING)) {
                Log.d("BTSTATE", "--" + sceneObject.playstatus)
                iv_toggle_bluetooth?.isChecked = false
            }
        }

        /*For free speakers irrespective of the state use Individual volume*/
        if (LibreApplication./*ZONE_VOLUME_MAP*/INDIVIDUAL_VOLUME_MAP.containsKey(currentIpAddress)) {
            seek_bar_volume.progress = LibreApplication.INDIVIDUAL_VOLUME_MAP[currentIpAddress]!!
        } else {
            LUCIControl(currentIpAddress).SendCommand(MIDCONST./*ZONE_VOLUME*/VOLUME_CONTROL, null, LSSDPCONST.LUCI_GET)
            val sceneObject = mScanHandler.sceneObjectFromCentralRepo[currentIpAddress]
            if (sceneObject?.volumeValueInPercentage!! >= 0)
                seek_bar_volume.progress = sceneObject!!.volumeValueInPercentage
        }
        if (seek_bar_volume.progress==0){
            iv_volume_mute?.setImageResource(R.drawable.ic_volume_mute)
        } else iv_volume_mute?.setImageResource(R.drawable.volume_low_enabled)
    }

    private fun showAlexaLoginAlert() {
        AlertDialog.Builder(this).apply {
//            setTitle(R.string.alexa_not_connected)
//            setMessage(getString(R.string.sign_in_az))

            val checkBoxView = View.inflate(this@CTMediaSourcesActivity,R.layout.alert_checkbox,null)
            checkBoxView.cb_dont?.setOnCheckedChangeListener { compoundButton, b ->
                val sharedPreferenceHelper = SharedPreferenceHelper(this@CTMediaSourcesActivity)
                if (/*compoundButton.isPressed && */b){
                    sharedPreferenceHelper.alexaLoginAlertDontAsk(currentIpAddress!!,dontAsk = true)
                } else {
                    sharedPreferenceHelper.alexaLoginAlertDontAsk(currentIpAddress!!,dontAsk = false)
                }
            }
            setView(checkBoxView)

            setPositiveButton(R.string.amazon_login) { dialogInterface, i ->
                dialogInterface.dismiss()
                startActivity(Intent(this@CTMediaSourcesActivity, CTAmazonLoginActivity::class.java).apply {
                    putExtra(Constants.CURRENT_DEVICE_IP, currentIpAddress)
                })
            }

            setNegativeButton(R.string.cancel) { dialogInterface, i ->
                dialogInterface.dismiss()
            }

            show()
        }
    }

    private fun fetchAuxBtStatus(){
        readBTControllerStatus()
        readBluetoothStatus()
        getAuxStatus()
    }

    private fun readBTControllerStatus() {
        LUCIControl(currentIpAddress).SendCommand(MIDCONST.MID_ENV_READ, LUCIMESSAGES.READ_BT_CONTROLLER, LSSDPCONST.LUCI_GET)
    }

    private fun readBluetoothStatus() {
        LUCIControl(currentIpAddress).SendCommand(MIDCONST.MID_BLUETOOTH, null, LSSDPCONST.LUCI_GET)
    }

    private fun getAuxStatus() {
        LUCIControl(currentIpAddress).SendCommand(MIDCONST.MID_CURRENT_SOURCE.toInt(), null, LSSDPCONST.LUCI_GET)
    }

    private fun showLoaderAndAskSource(source: String?) {
        if (this@CTMediaSourcesActivity.isFinishing)
            return
        //asking source
//        val luciControl = LUCIControl(currentIpAddress)
//        luciControl.SendCommand(MIDCONST.MID_CURRENT_SOURCE.toInt(), null, LSSDPCONST.LUCI_GET)
        showLoader()
        showToast(source!!)
//        fetchAuxBtStatus()
    }

    private fun showLoader() {
        progress_bar?.visibility = View.VISIBLE
    }

    private fun closeLoader() {
        progress_bar?.visibility = View.INVISIBLE
    }

    override fun deviceDiscoveryAfterClearingTheCacheStarted() {

    }

    override fun newDeviceFound(node: LSSDPNodes) {

    }

    override fun deviceGotRemoved(mIpAddress: String) {

    }

    override fun messageRecieved(nettyData: NettyData) {

        val remotedeviceIp = nettyData.getRemotedeviceIp()

        val luciPacket = LUCIPacket(nettyData.getMessage())
        LibreLogger.d(this, "Message received for " + remotedeviceIp
                + "\tCommand is " + luciPacket.command
                + "\tmsg is " + String(luciPacket.getpayload()))

        val currentNode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(remotedeviceIp)
        val sceneObject = mScanHandler.getSceneObjectFromCentralRepo(remotedeviceIp)

        if (currentIpAddress!!.equals(remotedeviceIp, ignoreCase = true)) {
            when (luciPacket.command) {

                MIDCONST.SET_UI -> {

                    val message = String(luciPacket.getpayload())
                    LibreLogger.d(this, " message 42 recieved  $message")
                    try {
                        parseJsonAndReflectInUI(message)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        LibreLogger.d(this, " Json exception ")
                    }

                }

                /* This indicates the bluetooth status*/
                MIDCONST.MID_BLUETOOTH -> {
                    val message = String(luciPacket.getpayload())
                    LibreLogger.d(this, " message 209 is recieved  $message")
                    when(message){
                        BLUETOOTH_ON -> {
                            timeOutHandler?.removeMessages(AUX_BT_TIMEOUT)
                            closeLoader()
                            iv_toggle_bluetooth?.isChecked = true
                            if (iv_toggle_aux.isChecked) {
                                iv_toggle_aux.isChecked = false
                            }
                        }
                        else -> {
                            timeOutHandler?.removeMessages(AUX_BT_TIMEOUT)
                            closeLoader()
                            iv_toggle_bluetooth?.isChecked = false
                        }
                    }
                }

                MIDCONST.MID_ENV_READ -> {
                    val messages = String(luciPacket.getpayload())
                    if (messages.contains("BT_CONTROLLER")) {
                        val BTvalue = Integer.parseInt(messages.substring(messages.indexOf(":") + 1))
                        LibreLogger.d(this, "BT_CONTROLLER value after parse is $BTvalue")
                        sceneObject?.bT_CONTROLLER = BTvalue
                    }
                }

                /*MIDCONST.MID_AUX_START -> {
                    val message = String(luciPacket.getpayload())
                    LibreLogger.d(this, " message 95 is $message")
                    if (message.contains("SUCCESS",true)){
                        timeOutHandler?.removeMessages(AUX_BT_TIMEOUT)
                        closeLoader()
                        iv_toggle_aux?.isChecked = true
                        if (iv_toggle_bluetooth!!.isChecked) {
                            iv_toggle_bluetooth!!.isChecked = false
                        }
                    }
                }

                MIDCONST.MID_AUX_STOP -> {
                    val message = String(luciPacket.getpayload())
                    LibreLogger.d(this, " message 96 is $message")
                    if (message.contains("SUCCESS",true)){
                        timeOutHandler?.removeMessages(AUX_BT_TIMEOUT)
                        closeLoader()
                        iv_toggle_aux?.isChecked = false
                    }
                }*/

                MIDCONST.MID_CURRENT_PLAY_STATE.toInt() -> {

                    val message = String(luciPacket.getpayload())
                    try {
                        val duration = Integer.parseInt(message)
                        sceneObject?.playstatus = duration

                        if (mScanHandler.isIpAvailableInCentralSceneRepo(currentIpAddress)) {
                            mScanHandler.putSceneObjectToCentralRepo(currentIpAddress, sceneObject)
                        }

                        when {
                            sceneObject?.currentSource == Constants.AUX_SOURCE
                                    || sceneObject?.currentSource == Constants.EXTERNAL_SOURCE-> {
                                if (iv_toggle_bluetooth!!.isChecked) {
                                    iv_toggle_bluetooth!!.isChecked = false
                                }
                                if (sceneObject.playstatus == SceneObject.CURRENTLY_PLAYING) {
                                    iv_toggle_aux.isChecked = true
                                }
                            }
                            sceneObject?.currentSource == Constants.BT_SOURCE -> {
                                if (iv_toggle_aux.isChecked) {
                                    iv_toggle_aux.isChecked = false
                                }
                                if (sceneObject.playstatus == SceneObject.CURRENTLY_PLAYING)
                                    iv_toggle_bluetooth!!.isChecked = true
                            }
                            /*else -> {
                                iv_toggle_aux.isChecked = false
                                iv_toggle_bluetooth!!.isChecked = false
                            }*/
                        }
                        LibreLogger.d(this, "Recieved the playstate to be" + sceneObject?.playstatus)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }

                MIDCONST.MID_CURRENT_SOURCE.toInt() -> {
                    val currentSource = String(luciPacket.getpayload())
                    LibreLogger.d(this, " message 50 is $currentSource")
                    sceneObject.currentSource = currentSource.toInt()
                    mScanHandler.sceneObjectFromCentralRepo[currentIpAddress!!] = sceneObject
                    // Toast.makeText(getApplicationContext(),"Message 50 is Received"+message,Toast.LENGTH_SHORT).show();
                    when {
                        currentSource.contains(Constants.AUX_SOURCE.toString())
                                || currentSource.contains(Constants.EXTERNAL_SOURCE.toString())-> {
                            timeOutHandler!!.removeMessages(AUX_BT_TIMEOUT)
                            closeLoader()

                            iv_toggle_aux.isChecked = true
                            if (iv_toggle_bluetooth!!.isChecked)
                                iv_toggle_bluetooth!!.isChecked = false
                        }
                        currentSource.contains(Constants.BT_SOURCE.toString()) -> {
                            /*removing timeout and closing loader*/
                            timeOutHandler!!.removeMessages(AUX_BT_TIMEOUT)
                            closeLoader()

                            iv_toggle_bluetooth!!.isChecked = true
                            if (iv_toggle_aux.isChecked)
                                iv_toggle_aux.isChecked = false
                        }
                        currentSource.contains(Constants.NO_SOURCE.toString()) || currentSource.contains("NO_SOURCE") -> {
                            LibreLogger.d(this, " No Source received hence closing dialog")
                            /**Closing loader after 1.5 second */
                            if (timeOutHandler?.hasMessages(AUX_BT_TIMEOUT)!!) {
                                timeOutHandler!!.removeMessages(AUX_BT_TIMEOUT)
//                                timeOutHandler!!.sendEmptyMessageDelayed(AUX_BT_TIMEOUT, 1500)
                                Handler().postDelayed({
                                    runOnUiThread { closeLoader() }
                                },1500)
                            }
//                            closeLoader()
                            iv_toggle_bluetooth!!.isChecked = false
                            iv_toggle_aux.isChecked = false
                        }

                        /*else -> {
                            closeLoader()
                            iv_toggle_aux.isChecked = false
                            iv_toggle_bluetooth!!.isChecked = false
                        }*/
                    }
                }

                MIDCONST.VOLUME_CONTROL -> {
                    /*this message box is to get volume*/
                    try {
                        val msg = String(luciPacket.getpayload())
                        val duration = Integer.parseInt(msg)
                        if (sceneObject?.volumeValueInPercentage != duration) {
                            sceneObject?.volumeValueInPercentage = duration
                            mScanHandler.putSceneObjectToCentralRepo(nettyData.getRemotedeviceIp(), sceneObject)
                            LibreLogger.d(this, "Recieved the current volume to be" + sceneObject?.volumeValueInPercentage)

                            seek_bar_volume?.progress = sceneObject?.volumeValueInPercentage!!

                            if (seek_bar_volume?.progress==0){
                                iv_volume_mute?.setImageResource(R.drawable.ic_volume_mute)
                            } else iv_volume_mute?.setImageResource(R.drawable.volume_low_enabled)

                            seek_bar_volume?.max = 100
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

        }
    }


    @Throws(JSONException::class)
    private fun parseJsonAndReflectInUI(jsonStr: String?) {

        LibreLogger.d(this, "Json Recieved from remote device " + jsonStr!!)
        if (jsonStr != null) {

            try {
                runOnUiThread { closeLoader() }

                val root = JSONObject(jsonStr)
                val cmd_id = root.getInt(TAG_CMD_ID)
                val window = root.getJSONObject(TAG_WINDOW_CONTENT)

                LibreLogger.d(this, "Command Id$cmd_id")

                if (cmd_id == 1) {
                    val Browser = window.getString(TAG_BROWSER)
                    if (Browser.equals("HOME", ignoreCase = true)) {
                        /* Now we have successfully got the stack intialiized to home */
                        timeOutHandler!!.removeMessages(NETWORK_TIMEOUT)
                        unRegisterForDeviceEvents()
                        val intent = Intent(this@CTMediaSourcesActivity, CTDeviceBrowserActivity::class.java)
                        intent.putExtra(Constants.CURRENT_DEVICE_IP, currentIpAddress)
                        intent.putExtra(Constants.CURRENT_SOURCE_INDEX_SELECTED, currentSourceIndexSelected)
                        LibreLogger.d(this, "removing handler message")
                        startActivity(intent)
//                        finish()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    override fun onResume() {
        super.onResume()
        /*Registering to receive messages*/
        registerForDeviceEvents(this)
        AlexaUtils.sendAlexaRefreshTokenRequest(currentIpAddress)
//        fetchAuxBtStatus()
        TunnelingControl(currentIpAddress).sendDataModeCommand()
    }

    override fun onStop() {
        super.onStop()
        /*unregister events */
        if (timeOutHandler != null)
            timeOutHandler!!.removeCallbacksAndMessages(null)
        unRegisterForDeviceEvents()
        closeLoader()
    }

    internal inner class CTSourcesListAdapter(val context: Context,
                                              var sourcesList: MutableList<String>?) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, i: Int): SourceItemViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.ct_list_item_media_sources, parent, false)
            return SourceItemViewHolder(view)
        }


        override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
            val sourceItem = sourcesList?.get(position)
            if (viewHolder is SourceItemViewHolder){
                viewHolder.bindSourceItem(sourceItem,position)
            }
        }

        override fun getItemCount(): Int {
            return sourcesList?.size!!
        }

        inner class SourceItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

            fun bindSourceItem(source: String?, position: Int) {
                itemView.tv_source_type.text = source
                when(source){
                    context.getString(R.string.my_device) -> itemView.iv_source_icon.setImageResource(R.drawable.my_device_enabled)
                    context.getString(R.string.usb_storage) -> itemView.iv_source_icon.setImageResource(R.drawable.usb_storage_enabled)
                    context.getString(R.string.mediaserver) -> itemView.iv_source_icon.setImageResource(R.drawable.media_servers_enabled)
                    context.getString(R.string.spotify) -> itemView.iv_source_icon.setImageResource(R.mipmap.spotify)
                }

                itemView.ll_media_source.setOnClickListener {
                    if (this@CTMediaSourcesActivity.isFinishing)
                        return@setOnClickListener

                    if (source != myDevice && getConnectedSSIDName(this@CTMediaSourcesActivity)?.contains(Constants.DDMS_SSID)!!) {
                        //  Toast.makeText(getApplicationContext(), "No Internet Connection ,in SA Mode", Toast.LENGTH_SHORT).show();
                        val error = LibreError(currentIpAddress, Constants.NO_INTERNET_CONNECTION)
                        showErrorMessage(error)
                        return@setOnClickListener
                    }

                    when (source) {

                        context.getString(R.string.my_device) -> {
                            if (!checkReadStoragePermission()){
                                return@setOnClickListener
                            }

//                            openDmsBrowser()
                        }

                        context.getString(R.string.usb_storage) -> {
                            currentSourceIndexSelected = 3
                            /*Reset the UI to Home ,, will wait for the confirmation of home command completion and then start the required activity*/
                            LUCIControl(currentIpAddress).SendCommand(MIDCONST.MID_REMOTE_UI.toInt(), GET_HOME, LSSDPCONST.LUCI_SET)
                            ///////////// timeout for dialog - showLoader() ///////////////////
                            timeOutHandler!!.sendEmptyMessageDelayed(NETWORK_TIMEOUT, Constants.ITEM_CLICKED_TIMEOUT.toLong())
                            showLoader()
                        }

                        context.getString(R.string.mediaserver) -> {
                            currentSourceIndexSelected = 0
                            /*Reset the UI to Home ,, will wait for the confirmation of home command completion and then start the required activity*/
                            LUCIControl(currentIpAddress).SendCommand(MIDCONST.MID_REMOTE_UI.toInt(), GET_HOME, LSSDPCONST.LUCI_SET)
                            ///////////// timeout for dialog - showLoader() ///////////////////
                            timeOutHandler!!.sendEmptyMessageDelayed(NETWORK_TIMEOUT, Constants.ITEM_CLICKED_TIMEOUT.toLong())
                            LibreLogger.d(this, "sending handler msg")
                            showLoader()

                            /*startActivity(Intent(this@CTMediaSourcesActivity, CTDMSDeviceListActivity::class.java).apply {
                                putExtra(AppConstants.IS_LOCAL_DEVICE_SELECTED, false)
                                putExtra(Constants.CURRENT_DEVICE_IP, currentIpAddress)
                            })
                            finish()*/

                            /*CTMediaServerListFragment().apply {
                                arguments = Bundle().apply {
                                    putString(Constants.CURRENT_DEVICE_IP,currentIpAddress)
                                }
                                show(supportFragmentManager,this::class.java.simpleName)
                            }*/
                        }
                    }
                }
            }
        }

        fun updateList(sourcesList: MutableList<String>?) {
            this.sourcesList = sourcesList
            notifyDataSetChanged()
        }
    }

    private fun openDmsBrowser() {
        val localIntent = Intent(this@CTMediaSourcesActivity, CTDMSBrowserActivityV2::class.java)
        localIntent.putExtra(AppConstants.IS_LOCAL_DEVICE_SELECTED, true)
        localIntent.putExtra(Constants.DEVICE_UDN, LibreApplication.LOCAL_UDN)
        localIntent.putExtra(Constants.CURRENT_DEVICE_IP, currentIpAddress)
        startActivity(localIntent)
    }

    override fun tunnelDataReceived(tunnelingData: TunnelingData) {
        if (tunnelingData.remoteClientIp == currentIpAddress && tunnelingData.remoteMessage.size >= 24) {
            val tcpTunnelPacket = TCPTunnelPacket(tunnelingData.remoteMessage)

            /*if (tcpTunnelPacket.volume>=0){
                seek_bar_volume?.progress = tcpTunnelPacket.volume

                if (seek_bar_volume?.progress == 0){
                    iv_volume_mute?.setImageResource(R.drawable.ic_volume_mute)
                } else iv_volume_mute?.setImageResource(R.drawable.volume_low_enabled)

                val sceneObject = mScanHandler.sceneObjectFromCentralRepo[currentIpAddress]
                sceneObject!!.volumeValueInPercentage = seek_bar_volume?.progress!!
                LibreApplication.INDIVIDUAL_VOLUME_MAP[sceneObject?.ipAddress] = sceneObject?.volumeValueInPercentage
                mScanHandler.putSceneObjectToCentralRepo(sceneObject.ipAddress, sceneObject)
            }*/

            when {
                tcpTunnelPacket.currentSource == Constants.BT_SOURCE -> {
                    iv_toggle_bluetooth?.isChecked = true
                    if (timeOutHandler?.hasMessages(AUX_BT_TIMEOUT)!!) timeOutHandler.removeMessages(AUX_BT_TIMEOUT)
                }
                tcpTunnelPacket.currentSource == Constants.AUX_SOURCE -> {
                    iv_toggle_aux?.isChecked = true
                    if (timeOutHandler?.hasMessages(AUX_BT_TIMEOUT)!!) timeOutHandler.removeMessages(AUX_BT_TIMEOUT)
                }
                tcpTunnelPacket.currentSource == Constants.NO_SOURCE -> {
                    iv_toggle_aux?.isChecked = false
                    iv_toggle_bluetooth?.isChecked = false
                    if (timeOutHandler?.hasMessages(AUX_BT_TIMEOUT)!!) timeOutHandler.removeMessages(AUX_BT_TIMEOUT)
                }
            }
        }
    }

    override fun storagePermissionAvailable() {
        super.storagePermissionAvailable()
        openDmsBrowser()
    }
}
