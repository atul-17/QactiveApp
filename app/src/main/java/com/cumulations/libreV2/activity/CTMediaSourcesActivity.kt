package com.cumulations.libreV2.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import com.cumulations.libreV2.AppConstants
import com.cumulations.libreV2.SharedPreferenceHelper
import com.libre.LErrorHandeling.LibreError
import com.libre.R
import com.libre.RemoteSourcesList
import com.libre.Scanning.Constants
import com.libre.Scanning.ScanningHandler
import com.libre.SceneObject
import com.libre.StaticInstructions.spotifyInstructions
import com.libre.alexa_signin.AlexaUtils
import com.libre.app.dlna.dmc.LocalDMSActivity
import com.libre.constants.CommandType
import com.libre.constants.LSSDPCONST
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
                val error = LibreError(currentIpAddress, Constants.INTERNET_ITEM_SELECTED_TIMEOUT_MESSAGE)
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
        setMusicPlayerWidget(fl_music_play_widget,currentIpAddress!!)
    }

    override fun onStart() {
        super.onStart()
        initViews()
        setListeners()
    }

    private fun setListeners() {
        iv_toggle_bluetooth?.setOnClickListener {
            val luciControl = LUCIControl(currentIpAddress)
            timeOutHandler!!.sendEmptyMessageDelayed(AUX_BT_TIMEOUT, Constants.LOADING_TIMEOUT.toLong())
            if (iv_toggle_bluetooth?.isChecked!!) {
//                showLoaderAndAskSource(getString(R.string.BtOnAlert));
                iv_toggle_bluetooth?.isChecked = false
                val msg = Message().apply {
                    what = BT_AUX_INITIATED
                    data = Bundle().apply {
                        putString("MessageText", getString(R.string.BtOnAlert))
                    }
                }
                timeOutHandler!!.sendMessage(msg)

                if (iv_toggle_aux?.isChecked!!) {
                    luciControl.SendCommand(MIDCONST.MID_AUX_STOP, null, LSSDPCONST.LUCI_SET)
                    iv_toggle_aux?.isChecked = false
                }
                // sleep when BT to AUX switch
                Handler().postDelayed({
                    luciControl.SendCommand(MIDCONST.MID_BLUETOOTH, BLUETOOTH_ON, LSSDPCONST.LUCI_SET)
                }, 1000)
            } else {
                iv_toggle_bluetooth?.isChecked = true

                val msg = Message().apply {
                    what = BT_AUX_INITIATED
                    data = Bundle().apply {
                        putString("MessageText", getString(R.string.BtOffAlert))
                    }
                }
                timeOutHandler!!.sendMessage(msg)
                luciControl.SendCommand(MIDCONST.MID_BLUETOOTH, BLUETOOTH_OFF, LSSDPCONST.LUCI_SET)
            }
        }

        iv_toggle_aux?.setOnClickListener {
            iv_toggle_aux?.isChecked = !iv_toggle_aux?.isChecked!!
            val luciControl = LUCIControl(currentIpAddress)
            timeOutHandler!!.sendEmptyMessage(ACTION_INITIATED)
            timeOutHandler!!.sendEmptyMessageDelayed(AUX_BT_TIMEOUT, Constants.LOADING_TIMEOUT.toLong())
            if (iv_toggle_aux.isChecked) {
                val msg = Message().apply {
                    what = BT_AUX_INITIATED
                    data = Bundle().apply {
                        putString("MessageText", getString(R.string.AuxOnAlert))
                    }
                }
                timeOutHandler!!.sendMessage(msg)
                if (iv_toggle_bluetooth!!.isChecked) {
                    luciControl.SendCommand(MIDCONST.MID_BLUETOOTH, BLUETOOTH_OFF, LSSDPCONST.LUCI_SET)
                    iv_toggle_bluetooth?.isChecked = false
                }

                Handler().postDelayed({
                    luciControl.SendCommand(MIDCONST.MID_AUX_START, null, LSSDPCONST.LUCI_SET)
                }, 1000)
            } else {
                val msg = Message().apply {
                    what = BT_AUX_INITIATED
                    data = Bundle().apply {
                        putString("MessageText", getString(R.string.AuxOffAlert))
                    }
                }
                timeOutHandler!!.sendMessage(msg)
                /* Setting the source to default */
                luciControl.SendCommand(MIDCONST.MID_AUX_STOP, null, LSSDPCONST.LUCI_SET)
            }
        }

        val sceneObject = mScanHandler.getSceneObjectFromCentralRepo(currentIpAddress)
        seek_bar_volume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                Log.d("onProgressChanged $progress", "fromUser $fromUser")
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                Log.d("onStartTracking", "${seekBar.progress}")
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

                LibreLogger.d("onStopTracking", "${seekBar.progress}")
                LUCIControl.SendCommandWithIp(MIDCONST.VOLUME_CONTROL, "" + seekBar.progress, CommandType.SET, sceneObject.ipAddress)

                sceneObject!!.volumeValueInPercentage = seekBar.progress
                mScanHandler.putSceneObjectToCentralRepo(sceneObject.ipAddress, sceneObject)
            }
        })

        iv_back?.setOnClickListener {
            onBackPressed()
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
            mediaSourcesList.add(spotify)
            adapter.notifyDataSetChanged()
        } else {
            mediaSourcesList.clear()
            mediaSourcesList.add(myDevice)
            mediaSourcesList.add(usbStorage)
            mediaSourcesList.add(mediaServer)
            mediaSourcesList.add(spotify)
            adapter.notifyDataSetChanged()
        }

        if (lssdpNodes?.alexaRefreshToken.isNullOrEmpty() && !SharedPreferenceHelper(this).isAlexaLoginAlertDontAskChecked()){
            showAlexaLoginAlert()
        }

        if (currentSource != null) {
            val currentSource = Integer.valueOf(currentSource)
            val sceneObject = mScanHandler.sceneObjectFromCentralRepo[currentIpAddress] ?: return

            /* Karuna , if Zone is playing in BT/AUX and We Released the Zone
            and we creating the same Guy as a Master then Aux should not Switch ON as a Default*/
            if (currentSource == Constants.AUX_SOURCE
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
    }

    private fun showAlexaLoginAlert() {
        AlertDialog.Builder(this).apply {
            setTitle(R.string.alexa_not_connected)
            setMessage(getString(R.string.sign_in_az))

            val checkBoxView = View.inflate(this@CTMediaSourcesActivity,R.layout.alert_checkbox,null)
            checkBoxView.cb_dont?.setOnCheckedChangeListener { compoundButton, b ->
                val sharedPreferenceHelper = SharedPreferenceHelper(this@CTMediaSourcesActivity)
                if (/*compoundButton.isPressed && */b){
                    sharedPreferenceHelper.alexaLoginAlertDontAsk(dontAsk = true)
                } else {
                    sharedPreferenceHelper.alexaLoginAlertDontAsk(dontAsk = false)
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

    private fun requestRefreshToken() {
        timeOutHandler!!.sendEmptyMessageDelayed(ALEXA_REFRESH_TOKEN_TIMER, Constants.LOADING_TIMEOUT.toLong())
        AlexaUtils.sendAlexaRefreshTokenRequest(currentIpAddress)
    }

    private fun readTheCurrentBluetoothStatus() {
        LUCIControl(currentIpAddress).SendCommand(MIDCONST.MID_BLUETOOTH, null, LSSDPCONST.LUCI_GET)
    }

    private fun readTheCurrentAuxStatus() {
        LUCIControl(currentIpAddress).SendCommand(MIDCONST.MID_CURRENT_SOURCE.toInt(), null, LSSDPCONST.LUCI_GET)
    }

    private fun showLoaderAndAskSource(source: String?) {
        if (this@CTMediaSourcesActivity.isFinishing)
            return
        //asking source
        val luciControl = LUCIControl(currentIpAddress)
        luciControl.SendCommand(MIDCONST.MID_CURRENT_SOURCE.toInt(), null, LSSDPCONST.LUCI_GET)
        showLoader()
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
        LibreLogger.d(this, "Message recieved for ipaddress " + remotedeviceIp + "command is " + luciPacket.command)

        val currentNode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(remotedeviceIp)
        val sceneObject = mScanHandler.getSceneObjectFromCentralRepo(remotedeviceIp)

        if (currentIpAddress!!.equals(remotedeviceIp, ignoreCase = true)) {
            when (luciPacket.command) {

                MIDCONST.GET_UI -> {

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
                }

                MIDCONST.MID_CURRENT_PLAY_STATE.toInt() -> {

                    val message = String(luciPacket.getpayload())
                    try {
                        val duration = Integer.parseInt(message)
                        sceneObject?.playstatus = duration

                        if (mScanHandler.isIpAvailableInCentralSceneRepo(currentIpAddress)) {
                            mScanHandler.putSceneObjectToCentralRepo(currentIpAddress, sceneObject)
                        }

                        if (sceneObject?.currentSource == Constants.AUX_SOURCE) {
                            if (iv_toggle_bluetooth!!.isChecked) {
                                iv_toggle_bluetooth!!.isChecked = false
                            }
                            if (sceneObject.playstatus == SceneObject.CURRENTLY_PLAYING) {
                                iv_toggle_aux.isChecked = true
                            }
                        } else if (sceneObject?.currentSource == Constants.BT_SOURCE) {
                            if (iv_toggle_aux.isChecked) {
                                iv_toggle_aux.isChecked = false
                            }
                            if (sceneObject.playstatus == SceneObject.CURRENTLY_PLAYING)
                                iv_toggle_bluetooth!!.isChecked = true
                        } else {
                            iv_toggle_aux.isChecked = false
                            iv_toggle_bluetooth!!.isChecked = false
                        }
                        LibreLogger.d(this, "Recieved the playstate to be" + sceneObject?.playstatus)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }

                MIDCONST.MID_CURRENT_SOURCE.toInt() -> {
                    val currentSource = String(luciPacket.getpayload())
                    // Toast.makeText(getApplicationContext(),"Message 50 is Received"+message,Toast.LENGTH_SHORT).show();
                    LibreLogger.d(this, " message 50 is recieved  $currentSource")
                    if (currentSource.contains(Constants.AUX_SOURCE.toString())) {
                        timeOutHandler!!.removeMessages(AUX_BT_TIMEOUT)
                        timeOutHandler!!.sendEmptyMessage(AUX_BT_TIMEOUT)

                        sceneObject.currentSource = Constants.AUX_SOURCE
                        mScanHandler.sceneObjectFromCentralRepo[currentIpAddress!!] = sceneObject

                        if (iv_toggle_bluetooth!!.isChecked)
                            iv_toggle_bluetooth!!.isChecked = false
                        iv_toggle_aux.isChecked = true
                    } else if (currentSource.contains(Constants.BT_SOURCE.toString())) {
                        /*removing timeout and closing loader*/
                        timeOutHandler!!.removeMessages(AUX_BT_TIMEOUT)
                        timeOutHandler!!.sendEmptyMessage(AUX_BT_TIMEOUT)

                        sceneObject.currentSource = Constants.BT_SOURCE
                        mScanHandler.sceneObjectFromCentralRepo[currentIpAddress!!] = sceneObject

                        if (iv_toggle_aux.isChecked)
                            iv_toggle_aux.isChecked = false
                        iv_toggle_bluetooth!!.isChecked = true
                    } else if (currentSource.contains(Constants.NO_SOURCE.toString()) || currentSource.contains("NO_SOURCE")) {
                        LibreLogger.d(this, " No Source received hence closing dialog")
                        /**Closing loader after 1.5 second */
                        timeOutHandler!!.removeMessages(AUX_BT_TIMEOUT)
                        timeOutHandler!!.sendEmptyMessageDelayed(AUX_BT_TIMEOUT, 1500)
                        iv_toggle_bluetooth!!.isChecked = false
                        iv_toggle_aux.isChecked = false
                    } else {

                        sceneObject.currentSource = -1
                        mScanHandler.sceneObjectFromCentralRepo[remotedeviceIp] = sceneObject

                        iv_toggle_aux.isChecked = false
                        iv_toggle_bluetooth!!.isChecked = false
                    }
                }

                MIDCONST.MID_ENV_READ -> {
                    /* This code is crashing for array out of index exception and hence will be handled with a try and catch -Praveen*/
                    try {
                        val messages = String(luciPacket.getpayload())
                        if (messages.contains("AlexaRefreshToken")) {
                            //                            alexaLayout.setVisibility(View.VISIBLE);
                            LibreLogger.d(this, " got alexa token $messages")
                            val token = messages.substring(messages.indexOf(":") + 1)
                            val mNodeDB = LSSDPNodeDB.getInstance()
                            val mNode = mNodeDB.getTheNodeBasedOnTheIpAddress(nettyData.getRemotedeviceIp())
                            if (mNode != null) {
                                mNode.alexaRefreshToken = token
                            }
                            timeOutHandler!!.removeMessages(ALEXA_REFRESH_TOKEN_TIMER)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
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
                        val intent = Intent(this@CTMediaSourcesActivity, RemoteSourcesList::class.java)
                        intent.putExtra(Constants.CURRENT_DEVICE_IP, currentIpAddress)
                        intent.putExtra(Constants.CURRENT_SOURCE_INDEX_SELECTED, currentSourceIndexSelected)
                        LibreLogger.d(this, "removing handler message")
                        startActivity(intent)
                        finish()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {

        }
    }

    override fun onResume() {
        super.onResume()
        /*Registering to receive messages*/
        registerForDeviceEvents(this)
        readTheCurrentBluetoothStatus()
        readTheCurrentAuxStatus()
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
                    context.getString(R.string.my_device) -> itemView.iv_source_icon.setImageResource(R.drawable.add_device_selected)
                    context.getString(R.string.usb_storage) -> itemView.iv_source_icon.setImageResource(R.drawable.add_device_selected)
                    context.getString(R.string.mediaserver) -> itemView.iv_source_icon.setImageResource(R.drawable.add_device_selected)
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
                        context.getString(R.string.spotify) -> {
                            val spotifyIntent = Intent(this@CTMediaSourcesActivity, spotifyInstructions::class.java)
                            spotifyIntent.putExtra(Constants.CURRENT_DEVICE_IP, currentIpAddress)
                            spotifyIntent.putExtra(Constants.CURRENT_SOURCE, currentSource)
                            startActivity(spotifyIntent)
                        }

                        context.getString(R.string.my_device) -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                    Toast.makeText(this@CTMediaSourcesActivity, getString(R.string.locationEnable), Toast.LENGTH_SHORT).show()
                                    return@setOnClickListener
                                }
                            }
                            showLoader()
                            val localIntent = Intent(this@CTMediaSourcesActivity, LocalDMSActivity::class.java)
                            localIntent.putExtra(AppConstants.IS_LOCAL_DEVICE_SELECTED, true)
                            localIntent.putExtra(Constants.CURRENT_DEVICE_IP, currentIpAddress)
                            startActivity(localIntent)
                            finish()
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
}
