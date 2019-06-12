package com.cumulations.libreV2.activity

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.ProgressDialog
import android.content.*
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.net.wifi.ScanResult
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.AppCompatImageButton
import android.support.v7.widget.AppCompatImageView
import android.support.v7.widget.AppCompatSeekBar
import android.support.v7.widget.AppCompatTextView
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*

import com.cumulations.libreV2.AppConstants
import com.cumulations.libreV2.*
import com.cumulations.libreV2.AppUtils
import com.cumulations.libreV2.SharedPreferenceHelper
import com.cumulations.libreV2.WifiUtil
import com.cumulations.libreV2.fragments.CTActiveDevicesFragment
import com.cumulations.libreV2.fragments.CTNoDeviceFragment
import com.github.johnpersano.supertoasts.SuperToast
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.libre.LErrorHandeling.LibreError
import com.libre.Ls9Sac.FwUpgradeData
import com.libre.Ls9Sac.GcastUpdateStatusAvailableListView
import com.libre.Scanning.Constants
import com.libre.Scanning.ScanningHandler
import com.libre.alexa.AudioRecordCallback
import com.libre.alexa.AudioRecordUtil
import com.libre.alexa.MicExceptionListener
import com.libre.alexa.MicTcpServer
import com.libre.alexa.userpoolManager.AlexaListeners.AlexaLoginListener
import com.libre.app.dlna.dmc.gui.abstractactivity.UpnpListenerActivity
import com.libre.app.dlna.dmc.processor.impl.UpnpProcessorImpl
import com.libre.app.dlna.dmc.processor.upnp.CoreUpnpService
import com.libre.app.dlna.dmc.server.ContentTree
import com.libre.app.dlna.dmc.utility.UpnpDeviceManager
import com.libre.constants.CommandType
import com.libre.constants.LSSDPCONST
import com.libre.constants.LUCIMESSAGES
import com.libre.constants.MIDCONST
import com.libre.luci.LSSDPNodeDB
import com.libre.luci.LSSDPNodes
import com.libre.luci.LUCIControl
import com.libre.luci.LUCIPacket
import com.libre.luci.Utils
import com.libre.netty.BusProvider
import com.libre.netty.LibreDeviceInteractionListner
import com.libre.netty.NettyData
import com.libre.netty.RemovedLibreDevice
import com.libre.util.LibreLogger
import com.squareup.otto.Subscribe

import org.json.JSONObject

import java.net.SocketException

import com.cumulations.libreV2.AppConstants.LOCATION_PERMISSION_REQUEST_CODE
import com.cumulations.libreV2.AppConstants.LOCATION_PERM_SETTINGS_REQUEST_CODE
import com.cumulations.libreV2.AppConstants.LOCATION_SETTINGS_REQUEST_CODE
import com.cumulations.libreV2.AppConstants.MICROPHONE_PERMISSION_REQUEST_CODE
import com.cumulations.libreV2.AppConstants.MICROPHONE_PERM_SETTINGS_REQUEST_CODE
import com.cumulations.libreV2.AppConstants.READ_STORAGE_REQUEST_CODE
import com.cumulations.libreV2.AppConstants.STORAGE_PERM_SETTINGS_REQUEST_CODE
import com.cumulations.libreV2.AppConstants.WIFI_SETTINGS_REQUEST_CODE
import com.cumulations.libreV2.tcp_tunneling.TCPTunnelPacket
import com.cumulations.libreV2.tcp_tunneling.TunnelingData
import com.cumulations.libreV2.tcp_tunneling.TunnelingControl
import com.libre.*
import com.libre.LibreApplication.activeSSID
import com.libre.alexa_signin.AlexaUtils
import com.libre.util.PicassoTrustCertificates
import com.squareup.picasso.Picasso
import java.util.*

/**
 * Created by Amit on 10/05/19.
 */
@SuppressLint("Registered")
open class CTDeviceDiscoveryActivity : UpnpListenerActivity(), AudioRecordCallback, MicExceptionListener {

    companion object {
        private val TAG = CTDeviceDiscoveryActivity::class.java.simpleName
    }

    private var libreDeviceInteractionListner: LibreDeviceInteractionListner? = null
    var upnpProcessor: UpnpProcessorImpl? = null
    private var localNetworkStateReceiver: LocalNetworkStateReceiver? = null
    private var mProgressDialog: ProgressDialog? = null

    var isNetworkChangesCallBackEnabled = true
        private set
    var isNetworkOffCallBackEnabled = true
        private set
    protected var alertDialog1: AlertDialog? = null
    private var alertRestartApp: AlertDialog? = null
    private var isActivityPaused = false
    var alexaLoginListener: AlexaLoginListener? = null
        private set
    lateinit var libreApplication: LibreApplication

    private var mandateDialog: AlertDialog? = null
    lateinit var sharedPreferenceHelper: SharedPreferenceHelper
    private var parentView: View? = null
    var connectedSSID: String? = null

    private var musicPlayerIp: String? = null
    private var musicPlayerWidget: ViewGroup? = null
    private var albumArtView: AppCompatImageView? = null
    private var currentSourceView: AppCompatImageView? = null
    private var playPauseView: AppCompatImageView? = null
    private var alexaButton: AppCompatImageButton? = null
    private var songSeekBar: AppCompatSeekBar? = null
    private var trackNameView: AppCompatTextView? = null
    private var albumNameView: AppCompatTextView? = null
    private var listeningView: AppCompatTextView? = null
    private var playinLayout: LinearLayout? = null
    private var currentTrackName = "-1"

    private var audioRecordUtil: AudioRecordUtil? = null
    private var micTcpServer: MicTcpServer? = null

    private var busEventListener: Any = object : Any() {
        @Subscribe
        fun newDeviceFound(nodes: LSSDPNodes) {
            LibreLogger.d(this, "newDeviceFound, node = " + nodes.friendlyname)
            if (libreDeviceInteractionListner != null) {
                libreDeviceInteractionListner!!.newDeviceFound(nodes)
            }
            if (nodes.getgCastVerision() != null)
                checkForTheSACDeviceSuccessDialog(nodes)
        }

        @Subscribe
        fun newMessageRecieved(nettyData: NettyData?) {
            val dummyPacket = LUCIPacket(nettyData!!.getMessage())
            LibreLogger.d(this, "New message appeared for the device " + nettyData.getRemotedeviceIp() +
                    "For the CommandStatus " + dummyPacket.commandStatus + " for the command " + dummyPacket.command)
            if (libreDeviceInteractionListner != null) {
                if (nettyData != null)
                    libreDeviceInteractionListner!!.messageRecieved(nettyData)
            }

            handleGCastMessage(nettyData)
            parseMessageForMusicPlayer(nettyData)
        }

        @Subscribe
        fun deviceGotRemovedFromIpAddress(deviceIpAddress: String?) {
            LibreLogger.d(this, "deviceGotRemovedFromIpAddress, " + deviceIpAddress!!)
            if (deviceIpAddress != null) {
                if (libreDeviceInteractionListner != null) {
                    libreDeviceInteractionListner!!.deviceGotRemoved(deviceIpAddress)
                }
            }
        }

        @Subscribe
        fun deviceGotRemoved(removedLibreDevice: RemovedLibreDevice?) {
            LibreLogger.d(this, "deviceGotRemoved, " + removedLibreDevice!!.getmIpAddress())
            if (removedLibreDevice != null) {
                if (appInForeground(this@CTDeviceDiscoveryActivity)) {
                    if (LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(removedLibreDevice.getmIpAddress()) != null) {
                        alertDialogForDeviceNotAvailable(LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(removedLibreDevice.getmIpAddress()))
                    }
                } else {
                    removeTheDeviceFromRepo(removedLibreDevice.getmIpAddress())
                    if (libreDeviceInteractionListner != null) {
                        libreDeviceInteractionListner!!.deviceGotRemoved(removedLibreDevice.getmIpAddress())
                    }
                }
            }
        }

        @Subscribe
        fun fwUpdateInternetFound(fwUpgradeData: FwUpgradeData) {
            Log.d(TAG, "fwUpdateInternetFound, device = " + fwUpgradeData.getmDeviceName()
                    + ",[" + LibreApplication.FW_UPDATE_AVAILABLE_LIST.keys.toString()+"]")
            val intent = Intent(this@CTDeviceDiscoveryActivity, GcastUpdateStatusAvailableListView::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        @Subscribe
        fun libreErrorReceived(libreError: LibreError) {
            if (libreDeviceInteractionListner != null && !isActivityPaused || !isFinishing) {
                showErrorMessage(libreError)
            }
        }

        @Subscribe
        fun tunnelingMessageReceived(tunnelingData: TunnelingData){
            LibreLogger.d(this,"tunnelingMessageReceived, data = ${TunnelingControl.getReadableHexByteArray(tunnelingData.remoteMessage)}")

            if (tunnelingData.remoteMessage.size == 4){
                /*Model Id only when 0x01 0x05 0x05 0x01~0x08*/
                val byteArray = tunnelingData.remoteMessage
                if (byteArray[0].toInt() == 0x01
                        && byteArray[1].toInt() == 0x05
                        && byteArray[2].toInt() == 0x05) {

                    val tcpTunnelPacket = TCPTunnelPacket(tunnelingData.remoteMessage, tunnelingData.remoteMessage.size)
                    if (tcpTunnelPacket.modelId != null) {
                        val lssdpNodes = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(tunnelingData.remoteClientIp)
                        lssdpNodes?.modelId = tcpTunnelPacket.modelId
                        LibreLogger.d(this, "tunnelingMessageReceived, modelId = ${tcpTunnelPacket.modelId}")
                    }
                }
            }

            if (libreDeviceInteractionListner != null) {
                Log.d(TAG, "tunnelDataReceived, libreDeviceInteractionListner = " + libreDeviceInteractionListner!!::class.java.simpleName)
                if (libreDeviceInteractionListner is Activity) {
                    if ((libreDeviceInteractionListner as Activity).isVisibleToUser()) {
                        tunnelDataReceived(tunnelingData)
                    }
                }
            }
        }
    }

    /*Will be overridden by Child classes who want this data*/
    open fun tunnelDataReceived(tunnelingData: TunnelingData){

    }

    private val isMicrophonePermissionGranted: Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AppUtils.isPermissionGranted(this, Manifest.permission.RECORD_AUDIO)
        } else true

    val upnpBinder: CoreUpnpService.Binder?
        get() = upnpProcessor!!.binder
    private var sAcalertDialog: AlertDialog? = null
    private var fwUpdateAlertDialog: AlertDialog? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        libreApplication = application as LibreApplication
        sharedPreferenceHelper = SharedPreferenceHelper(this)

        val intentFilter = IntentFilter()
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        localNetworkStateReceiver = LocalNetworkStateReceiver()

        registerReceiver(localNetworkStateReceiver, intentFilter)

        upnpProcessor = UpnpProcessorImpl(this)
        upnpProcessor!!.bindUpnpService()
        upnpProcessor!!.addListener(this)
        upnpProcessor!!.addListener(UpnpDeviceManager.getInstance())
        libreApplication.gpsStateReceiver.setActivity(this)
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
        parentView = window.decorView.rootView
        initEventsAndServers()
        proceedToHome()
    }

    open fun proceedToHome() {}

    fun checkLocationPermission() {
        if (!AppUtils.isPermissionGranted(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            requestLocationPermission()
        } else {
            Log.d("checkLocationPermission", "Permission already granted.")
            if (AppUtils.isLocationServiceEnabled(this)) {
                connectedSSID = getConnectedSSIDName(this)
                Log.d("checkLocationPermission", "connectedSSID \$connectedSSID")
            } else
                showLocationMustBeEnabledDialog()
        }
    }

    open fun storagePermissionAvailable(){}

    fun checkReadStoragePermission():Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !AppUtils.isPermissionGranted(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Log.d("checkStoragePermission", "Not granted")
            requestStoragePermission()
            return false
        } else {
            Log.d("checkStoragePermission", "Permission already granted.")
            storagePermissionAvailable()
        }
        return true
    }

    private fun requestLocationPermission() {
        Log.i("requestLocPerm", "Location permission has NOT been granted. Requesting permission.")
        if (AppUtils.shouldShowPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // For example if the user has previously denied the permission.
            Log.i("requestLocPerm", "Displaying location permission rationale to provide additional context.")
            Snackbar.make(parentView!!, R.string.permission_location_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok) { AppUtils.requestPermission(this@CTDeviceDiscoveryActivity, Manifest.permission.ACCESS_FINE_LOCATION, LOCATION_PERMISSION_REQUEST_CODE) }
                    .show()
        } else {
            if (!sharedPreferenceHelper.isFirstTimeAskingPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                sharedPreferenceHelper.firstTimeAskedPermission(Manifest.permission.ACCESS_FINE_LOCATION, true)
                // No explanation needed, we can request the permission.
                // Camera permission has not been granted yet. Request it directly.
                AppUtils.requestPermission(this, Manifest.permission.ACCESS_FINE_LOCATION, LOCATION_PERMISSION_REQUEST_CODE)
            } else {
                //                showStoragePermDisabledSnackBar();
                showAlertForLocationPermissionRequired()
            }

        }
    }

    private fun showStoragePermDisabledSnackBar() {
        //Permission disable by device policy or user denied permanently. Show proper error message
        Snackbar.make(parentView!!, R.string.storagePermitToast,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.open) {
                    startActivityForResult(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(Uri.fromParts("package", packageName, null))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                            .addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS), STORAGE_PERM_SETTINGS_REQUEST_CODE)
                }
                .show()
    }

    fun showLocationMustBeEnabledDialog() {
        if (mandateDialog != null && mandateDialog!!.isShowing)
            mandateDialog!!.dismiss()

        if (mandateDialog == null) {
            val builder = AlertDialog.Builder(this)
            builder.setMessage(getString(R.string.enable_location))
            builder.setCancelable(false)
            builder.setPositiveButton(R.string.ok) { dialogInterface, i ->
                mandateDialog!!.dismiss()
                displayLocationSettingsRequest()
            }
            builder.setNegativeButton(R.string.cancel) { dialogInterface, i ->
                mandateDialog!!.dismiss()
                //                    killApp();
            }
            mandateDialog = builder.create()
        }

        if (!mandateDialog!!.isShowing)
            mandateDialog!!.show()
    }

    private fun createLocationRequest(): LocationRequest {
        val locationRequest = LocationRequest.create()
        locationRequest.interval = 10000
        locationRequest.fastestInterval = 5000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        return locationRequest
    }

    private fun displayLocationSettingsRequest() {
        val googleApiClient = GoogleApiClient.Builder(this)
                .addApi(LocationServices.API).build()
        googleApiClient.connect()

        val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(createLocationRequest())
                .setAlwaysShow(true)

        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener(this) {
            // All location settings are satisfied. The client can initialize
            // location requests here.
            // ...
        }

        task.addOnFailureListener(this) { e ->
            if (e is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    e.startResolutionForResult(this@CTDeviceDiscoveryActivity,
                            LOCATION_SETTINGS_REQUEST_CODE)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                    sendEx.printStackTrace()
                }

            }
        }
    }

    private fun requestStoragePermission() {
        if (AppUtils.shouldShowPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Log.i("requestLocPerm", "Displaying location permission rationale to provide additional context.")
            Snackbar.make(parentView!!, R.string.storagePermitToast, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok) { AppUtils.requestPermission(this@CTDeviceDiscoveryActivity, Manifest.permission.READ_EXTERNAL_STORAGE, READ_STORAGE_REQUEST_CODE) }
                    .show()
        } else {
            if (!sharedPreferenceHelper.isFirstTimeAskingPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                sharedPreferenceHelper.firstTimeAskedPermission(Manifest.permission.READ_EXTERNAL_STORAGE, true)
                // No explanation needed, we can request the permission.
                // Camera permission has not been granted yet. Request it directly.
                AppUtils.requestPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE, READ_STORAGE_REQUEST_CODE)
            } else {
                showStoragePermDisabledSnackBar()
            }

        }
    }

    fun killApp() {
        Log.d("Declined", "App is Killed ")
        stopDmrForegroundService()
        finishActivitiesAndKillAppProcess()
    }

    private fun stopDmrForegroundService() {
        /* Stopping ForeGRound Service Whenwe are Restarting the APP */
        stopService(Intent(this, DMRDeviceListenerForegroundService::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setAction(Constants.ACTION.STOPFOREGROUND_ACTION))
        Log.e("stopDmrService", "done")
    }

    private fun finishActivitiesAndKillAppProcess() {
        Log.e("finishKillAppProcess", "done")
        ActivityCompat.finishAffinity(this)
        /* Killing our Android App with The PID For the Safe Case */
        val pid = android.os.Process.myPid()
        android.os.Process.killProcess(pid)
    }

    fun showSomethingWentWrongAlert(context: Context) {
        try {
            if (alertDialog1 != null && alertDialog1!!.isShowing)
                alertDialog1!!.dismiss()
            val builder = AlertDialog.Builder(context)

            builder.setMessage(resources.getString(R.string.somethingWentWrong))
                    .setCancelable(false)
                    .setPositiveButton("OK") { dialog, id -> alertDialog1!!.dismiss() }

            if (alertDialog1 == null) {
                alertDialog1 = builder.create()
                val messageView = alertDialog1!!.findViewById<TextView>(android.R.id.message)
                messageView?.gravity = Gravity.CENTER
            }
            alertDialog1!!.show()

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun getConnectedSSIDName(mContext: Context): String {
        return AppUtils.getConnectedSSID(mContext)!!
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
        isActivityPaused = true
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setMusicPlayerWidget(musicPlayerWidget: ViewGroup, musicPlayerIp: String) {
        this.musicPlayerWidget = musicPlayerWidget
        this.musicPlayerIp = musicPlayerIp

        audioRecordUtil = AudioRecordUtil.getAudioRecordUtil()
        micTcpServer = MicTcpServer.getMicTcpServer()
        libreApplication.registerForMicException(this)

        songSeekBar = musicPlayerWidget.findViewById(R.id.seek_bar_song)
        albumArtView = musicPlayerWidget.findViewById(R.id.iv_album_art)
        trackNameView = musicPlayerWidget.findViewById(R.id.tv_track_name)
        albumNameView = musicPlayerWidget.findViewById(R.id.tv_album_name)
        currentSourceView = musicPlayerWidget.findViewById(R.id.iv_current_source)
        playPauseView = musicPlayerWidget.findViewById(R.id.iv_play_pause)
        alexaButton = musicPlayerWidget.findViewById(R.id.ib_alexa_avs_btn)
        listeningView = musicPlayerWidget.findViewById(R.id.tv_alexa_listening)
        playinLayout = musicPlayerWidget.findViewById(R.id.ll_playing_layout)

        setMusicPlayerListeners()

        val sceneObject = ScanningHandler.getInstance().sceneObjectFromCentralRepo[musicPlayerIp]
        updateMusicPlayViews(sceneObject)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setMusicPlayerListeners(){
        val musicSceneObject = ScanningHandler.getInstance().getSceneObjectFromCentralRepo(musicPlayerIp)
        val mNodeWeGotForControl = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(musicPlayerIp)
        val control = LUCIControl(musicPlayerIp)

        playPauseView?.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View) {
                if (musicSceneObject!!.currentSource == Constants.AUX_SOURCE
                        || musicSceneObject!!.currentSource == Constants.EXTERNAL_SOURCE
                        || musicSceneObject.currentSource == Constants.GCAST_SOURCE
                        || musicSceneObject.currentSource == Constants.VTUNER_SOURCE
                        || musicSceneObject.currentSource == Constants.TUNEIN_SOURCE
                        || musicSceneObject.currentSource == Constants.BT_SOURCE && (mNodeWeGotForControl!!.getgCastVerision() == null && (mNodeWeGotForControl.bT_CONTROLLER == SceneObject.CURRENTLY_NOTPLAYING || mNodeWeGotForControl.bT_CONTROLLER == SceneObject.CURRENTLY_PLAYING) || mNodeWeGotForControl.getgCastVerision() != null && mNodeWeGotForControl.bT_CONTROLLER < SceneObject.CURRENTLY_PAUSED)) {

                    val error = LibreError("", resources.getString(R.string.PLAY_PAUSE_NOT_ALLOWED), 1)
                    BusProvider.getInstance().post(error)
                    return

                }
                if (musicSceneObject.currentSource == Constants.NO_SOURCE || musicSceneObject.currentSource == Constants.DMR_SOURCE && (musicSceneObject.playstatus == SceneObject.CURRENTLY_STOPPED || musicSceneObject.playstatus == SceneObject.CURRENTLY_NOTPLAYING)) {
                    LibreLogger.d(this, "currently not playing, so take user to sources option activity")
                    gotoSourcesOption(musicSceneObject.ipAddress, musicSceneObject.currentSource)
                    return
                }

                if (musicSceneObject.playstatus == SceneObject.CURRENTLY_PLAYING) {
                    control.SendCommand(MIDCONST.MID_PLAYCONTROL.toInt(), LUCIMESSAGES.PAUSE, CommandType.SET)
                    playPauseView?.setImageResource(R.drawable.play_orange)
                } else {
                    if (musicSceneObject.currentSource == Constants.BT_SOURCE) {
                        control.SendCommand(MIDCONST.MID_PLAYCONTROL.toInt(), LUCIMESSAGES.PLAY, CommandType.SET)
                    } else {
                        control.SendCommand(MIDCONST.MID_PLAYCONTROL.toInt(), LUCIMESSAGES.RESUME, CommandType.SET)
                    }
                    // musicSceneObject.setPlaystatus(SceneObject.CURRENTLY_PLAYING);
                    playPauseView?.setImageResource(R.drawable.pause_orange)
                }
            }
        })

        alexaButton?.setOnLongClickListener(View.OnLongClickListener {
            if (!isMicrophonePermissionGranted) {
                requestRecordPermission()
                return@OnLongClickListener true
            }

            if (mNodeWeGotForControl == null || mNodeWeGotForControl?.alexaRefreshToken?.isNullOrEmpty()!!) {
                musicSceneObject!!.isAlexaBtnLongPressed = false
                toggleAVSViews(false)
                showLoginWithAmazonAlert(musicSceneObject.ipAddress)
                return@OnLongClickListener true
            }

            val phoneIp = phoneIpAddress()
            if (!musicSceneObject?.ipAddress.isNullOrEmpty() && phoneIp.isNotEmpty()) {

                Log.d("OnLongClick", "phone ip: " + phoneIp + "port: " + MicTcpServer.MIC_TCP_SERVER_PORT)
                control.SendCommand(MIDCONST.MID_MIC, Constants.START_MIC + phoneIp + "," + MicTcpServer.MIC_TCP_SERVER_PORT, LSSDPCONST.LUCI_SET)

                musicSceneObject.isAlexaBtnLongPressed = true
                toggleAVSViews(true)
                audioRecordUtil?.startRecording(this@CTDeviceDiscoveryActivity)
            } else {
                musicSceneObject.isAlexaBtnLongPressed = false
                toggleAVSViews(false)
                showToast("Ip not available")
            }
            true
        })

        alexaButton?.setOnTouchListener { view, motionEvent ->
            Log.d("AlexaBtn", "motionEvent = " + motionEvent.action)
            if (motionEvent.action == MotionEvent.ACTION_UP || motionEvent.action == MotionEvent.ACTION_CANCEL) {
                Log.d("AlexaBtn", "long press release, sceneObject isLongPressed = " + musicSceneObject?.isAlexaBtnLongPressed)
                /*if (sceneObject?.isAlexaBtnLongPressed!!) {
                        sceneObject?.isAlexaBtnLongPressed = false
                        toggleAVSViews(false)
                    }*/
                musicSceneObject.isAlexaBtnLongPressed = false
                toggleAVSViews(false)
            }
            false
        }

        musicPlayerWidget?.setOnClickListener {
            if (!musicSceneObject?.trackName.isNullOrEmpty()
                    || !musicSceneObject?.album_art.isNullOrEmpty()
                    || musicSceneObject?.playstatus == SceneObject.CURRENTLY_PLAYING){
                startActivity(Intent(this, CTNowPlayingActivity::class.java).apply {
                    putExtra(Constants.CURRENT_DEVICE_IP, musicPlayerIp)
                })
            }
        }
    }

    private fun toggleAVSViews(showListening: Boolean) {
        if (showListening) {
            listeningView?.visibility = View.VISIBLE
            playinLayout?.visibility = View.INVISIBLE
        } else {
            listeningView?.visibility = View.GONE
            playinLayout?.visibility = View.VISIBLE
        }
    }

    private fun updateMusicPlayViews(sceneObject: SceneObject?) {
        if (musicPlayerWidget?.id == R.id.fl_alexa_widget){
            handleAlexaViews(sceneObject!!)
            return
        }

//        toggleAVSViews(sceneObject?.isAlexaBtnLongPressed!!)

        if (!sceneObject?.trackName.isNullOrEmpty() && !sceneObject!!.trackName.equals("NULL", ignoreCase = true)) {
            var trackname = sceneObject!!.trackName

            /* This change is done to handle the case of deezer where the song name is appended by radio or skip enabled */
            if (trackname.contains(Constants.DEZER_RADIO))
                trackname = trackname.replace(Constants.DEZER_RADIO, "")

            if (trackname.contains(Constants.DEZER_SONGSKIP))
                trackname = trackname.replace(Constants.DEZER_SONGSKIP, "")

            trackNameView?.text = trackname
        }

        if (!sceneObject?.artist_name.isNullOrEmpty() && !sceneObject!!.album_name.equals("null", ignoreCase = true)) {
            albumNameView?.text = sceneObject!!.artist_name
        } else if (!sceneObject?.album_name.isNullOrEmpty() && !sceneObject!!.album_name.equals("null", ignoreCase = true)) {
            albumNameView?.text = sceneObject!!.album_name
        }

        /*Handling album art*/
        /*this is to show loading dialog while we are preparing to play*/
        if (sceneObject!!.currentSource != Constants.AUX_SOURCE
                && sceneObject!!.currentSource != Constants.EXTERNAL_SOURCE
                && sceneObject!!.currentSource != Constants.BT_SOURCE
                && sceneObject!!.currentSource != Constants.GCAST_SOURCE) {

            /*Album Art For All other Sources Except */
            if (!sceneObject!!.album_art.isNullOrEmpty() && sceneObject!!.album_art.equals("coverart.jpg", ignoreCase = true)) {
                val albumUrl = "http://" + sceneObject!!.ipAddress + "/" + "coverart.jpg"
                /* If Track Name is Different just Invalidate the Path And if we are resuming the Screen(Screen OFF and Screen ON) , it will not re-download it */

                if (sceneObject!!.trackName != null
                        && !currentTrackName.equals(sceneObject!!.trackName, ignoreCase = true)) {
                    currentTrackName = sceneObject?.trackName!!
                    val mInvalidated = mInvalidateTheAlbumArt(sceneObject!!, albumUrl)
                    LibreLogger.d(this, "Invalidated the URL $albumUrl Status $mInvalidated")
                }

                if (albumArtView != null) {
                    PicassoTrustCertificates.getInstance(this)
                            .load(albumUrl)
                            /*.memoryPolicy(MemoryPolicy.NO_CACHE).networkPolicy(NetworkPolicy.NO_CACHE)*/
                            .placeholder(R.mipmap.album_art)
                            .error(R.mipmap.album_art)
                            .into(albumArtView)
                }
            } else {
                when {
                    !sceneObject.album_art.isNullOrEmpty() -> {
                        if (albumArtView != null) {
                            /*PicassoTrustCertificates.getInstance(this)*/
                            Picasso.with(this)
                                    .load(sceneObject!!.album_art)
                                    /*   .memoryPolicy(MemoryPolicy.NO_CACHE).networkPolicy(NetworkPolicy.NO_CACHE)*/
                                    .placeholder(R.mipmap.album_art)
                                    .error(R.mipmap.album_art)
                                    .into(albumArtView)
                        }
                    }

                    else -> {
                        albumArtView?.setImageResource(R.mipmap.album_art)
                    }
                }
            }
        }

        val context = this
        currentSourceView?.visibility = View.GONE
        when (sceneObject?.currentSource) {

            Constants.NO_SOURCE,
            Constants.DDMSSLAVE_SOURCE -> {
                playPauseView?.isClickable = false
                if (sceneObject?.currentSource == Constants.NO_SOURCE){
                    handleAlexaViews(sceneObject)
                }
            }

            Constants.VTUNER_SOURCE,
            Constants.TUNEIN_SOURCE -> {
                playPauseView?.setImageResource(R.drawable.play_white)
                songSeekBar?.visibility = View.VISIBLE
                songSeekBar?.progress = 0
                songSeekBar?.isEnabled = false
            }

            /*For Riva Tunneling, When switched to Aux, its External Source*/
            Constants.AUX_SOURCE,Constants.EXTERNAL_SOURCE -> {
                /*playPauseView?.setImageResource(R.drawable.play_white)
                songSeekBar?.visibility = View.VISIBLE
                songSeekBar?.progress = 0
                songSeekBar?.isEnabled = false

                trackNameView?.text = context.getText(R.string.aux)
                albumNameView?.visibility = View.GONE
                albumArtView?.visibility = View.GONE*/
                handleAlexaViews(sceneObject)
            }

            Constants.BT_SOURCE -> {
                /*trackNameView?.text = context.getText(R.string.bluetooth)
                songSeekBar?.progress = 0
                songSeekBar?.isEnabled = false
                albumNameView?.visibility = View.GONE
                albumArtView?.visibility = View.GONE

                val mNode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(sceneObject.ipAddress)
                        ?: return
                LibreLogger.d(this, "BT controller value in sceneobject " + mNode.bT_CONTROLLER)
                if (mNode.bT_CONTROLLER != SceneObject.CURRENTLY_STOPPED && mNode.bT_CONTROLLER != SceneObject.CURRENTLY_PAUSED && mNode.bT_CONTROLLER != 3) {
                    playPauseView?.setImageResource(R.drawable.play_white)
                }*/
                handleAlexaViews(sceneObject)
            }
            Constants.GCAST_SOURCE -> {
                //gCast is Playing
                playPauseView?.setImageResource(R.drawable.play_orange)
                trackNameView?.text = "Casting"

                songSeekBar?.progress = 0
                songSeekBar?.isEnabled = false
            }

            Constants.ALEXA_SOURCE,
            Constants.DMR_SOURCE,
            Constants.DMP_SOURCE,
            Constants.SPOTIFY_SOURCE,
            Constants.USB_SOURCE -> {
                if (!sceneObject?.trackName.isNullOrEmpty()){
                    playPauseView?.visibility = View.VISIBLE
                    trackNameView?.visibility = View.VISIBLE
                    if (!sceneObject?.album_name?.isEmpty()!! || !sceneObject?.artist_name?.isEmpty()!!){
                        albumNameView?.visibility = View.VISIBLE
                    }

                    if (!sceneObject?.album_art?.isEmpty()!!){
                        albumArtView?.visibility = View.VISIBLE
                    }

                    if (sceneObject?.totalTimeOfTheTrack>0 && sceneObject.currentPlaybackSeekPosition>=0){
                        songSeekBar?.visibility = View.VISIBLE
                        songSeekBar?.isEnabled = true
                    }
                }

                if (sceneObject?.currentSource == Constants.SPOTIFY_SOURCE)
                    currentSourceView?.visibility = View.VISIBLE
            }

            else -> handleAlexaViews(sceneObject)
        }

        if (sceneObject != null && (sceneObject.currentSource == Constants.VTUNER_SOURCE
                        || sceneObject.currentSource == Constants.TUNEIN_SOURCE
                        || sceneObject.currentSource == Constants.BT_SOURCE
                        || sceneObject.currentSource == Constants.AUX_SOURCE
                        || sceneObject.currentSource == Constants.EXTERNAL_SOURCE
                        || sceneObject.currentSource == Constants.NO_SOURCE
                        || sceneObject.currentSource == Constants.GCAST_SOURCE)) {
            playPauseView?.isEnabled = false
            songSeekBar?.progress = 0
            songSeekBar?.secondaryProgress = 0
            songSeekBar?.max = 100
            songSeekBar?.isEnabled = false
        }

        if (playPauseView!=null && !playPauseView?.isEnabled!!) {
            playPauseView?.setImageResource(R.drawable.play_white)
        } else if (sceneObject?.playstatus == SceneObject.CURRENTLY_PLAYING) {
            playPauseView?.setImageResource(R.drawable.pause_orange)
        } else {
            playPauseView?.setImageResource(R.drawable.play_orange)
        }

        /* Setting the current seekbar progress -Start*/
        val duration = sceneObject.currentPlaybackSeekPosition
        songSeekBar?.max = sceneObject.totalTimeOfTheTrack.toInt() / 1000
        songSeekBar?.secondaryProgress = sceneObject.totalTimeOfTheTrack.toInt() / 1000
        Log.d("seek_bar_song", "Duration = " + duration / 1000)
        songSeekBar?.progress = duration.toInt() / 1000
    }

    private fun handleAlexaViews(sceneObject: SceneObject) {
        songSeekBar?.visibility = View.GONE
        albumArtView?.visibility = View.GONE
        playPauseView?.visibility = View.GONE
        trackNameView?.text = getText(R.string.libre_voice)

        val node = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(sceneObject?.ipAddress)
        if (node?.alexaRefreshToken.isNullOrEmpty()){
            trackNameView?.visibility = View.VISIBLE
            trackNameView?.text = getText(R.string.login_to_enable_cmds)
            albumNameView?.visibility = View.GONE
        } else {
            albumNameView?.visibility = View.VISIBLE
            trackNameView?.visibility = View.VISIBLE
            trackNameView?.text = getText(R.string.app_name)
            albumNameView?.text = getText(R.string.speaker_ready_for_cmds)
        }
    }

    fun mInvalidateTheAlbumArt(sceneObject: SceneObject, album_url: String): Boolean {
        if (!sceneObject.getmPreviousTrackName().equals(sceneObject.trackName, ignoreCase = true)) {
            PicassoTrustCertificates.getInstance(this).invalidate(album_url)
            sceneObject.setmPreviousTrackName(sceneObject.trackName)
            val sceneObjectFromCentralRepo = ScanningHandler.getInstance().getSceneObjectFromCentralRepo(sceneObject.ipAddress)
            sceneObjectFromCentralRepo?.setmPreviousTrackName(sceneObject.trackName)
            return true
        }
        return false
    }

    private fun gotoSourcesOption(ipaddress: String, currentSource: Int) {
        val mActiveScenesList = Intent(this, CTMediaSourcesActivity::class.java)
        mActiveScenesList.putExtra(Constants.CURRENT_DEVICE_IP, ipaddress)
        mActiveScenesList.putExtra(Constants.CURRENT_SOURCE, "" + currentSource)
        val error = LibreError("", resources.getString(R.string.no_active_playlist), 1)
        BusProvider.getInstance().post(error)
        startActivity(mActiveScenesList)
    }

    private fun parseMessageForMusicPlayer(nettyData: NettyData) {
        Log.e("parseForMusicPlayer", "musicIp = $musicPlayerIp, nettyData Ip = ${nettyData.remotedeviceIp}")
        if (musicPlayerIp == null || !musicPlayerIp?.equals(nettyData?.remotedeviceIp)!!) {
            Log.e("parseForMusicPlayer","musicPlayerIp $musicPlayerIp")
            return
        }

        if (musicPlayerWidget == null) {
            Log.e("parseForMusicPlayer","musicPlayerWidget null")
            return
        }

        val luciPacket = LUCIPacket(nettyData.message)
        val msg = String(luciPacket.payload)
        val sceneObject = ScanningHandler.getInstance().getSceneObjectFromCentralRepo(nettyData.remotedeviceIp)
        if (sceneObject == null) {
            Log.e("MessageForMusicPlayer", "sceneObject null for " + nettyData.remotedeviceIp)
            return
        }

        when(luciPacket.command){
            MIDCONST.SET_UI -> {
                try {
                    LibreLogger.d(this, "MB : 42, msg = \$msg")
                    val root = JSONObject(msg)
                    val cmdId = root.getInt(LUCIMESSAGES.TAG_CMD_ID)
                    val window = root.getJSONObject(LUCIMESSAGES.ALEXA_KEY_WINDOW_CONTENT)
                    LibreLogger.d(this, "PLAY JSON is \n= \$msg")
                    if (cmdId == 3) {
                        handlePlayJsonUi(window,sceneObject)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            MIDCONST.MID_MIC -> {
                if (msg.contains("BLOCKED",true)) {
                    audioRecordUtil?.stopRecording()
                    showToast(getString(R.string.deviceMicOn))
                    sceneObject?.isAlexaBtnLongPressed = false
                    /*updating boolean status to repo as well*/
                    ScanningHandler.getInstance().putSceneObjectToCentralRepo(sceneObject?.ipAddress,sceneObject)
                    toggleAVSViews(sceneObject?.isAlexaBtnLongPressed)
                }
            }

            MIDCONST.MID_CURRENT_PLAY_STATE.toInt() -> {
                LibreLogger.d(this, "MB : 51, msg = $msg")
                if (msg.isNotEmpty()) {
                    val playstatus = Integer.parseInt(msg)
                    if (sceneObject?.playstatus != playstatus) {
                        sceneObject?.playstatus = playstatus
                        ScanningHandler.getInstance().putSceneObjectToCentralRepo(nettyData.getRemotedeviceIp(), sceneObject)
                        updateMusicPlayViews(sceneObject)
                    }
                }
            }

            MIDCONST.MID_CURRENT_SOURCE.toInt() -> {
                /*this MB to get current sources*/
                try {
                    LibreLogger.d(this, "Recieved the current source as  " + sceneObject?.currentSource)
                    val mSource = Integer.parseInt(msg)
                    sceneObject?.currentSource = mSource
                    ScanningHandler.getInstance().putSceneObjectToCentralRepo(nettyData.getRemotedeviceIp(), sceneObject)
                    updateMusicPlayViews(sceneObject)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            MIDCONST.MID_ENV_READ -> {
                if (msg.contains("AlexaRefreshToken")) {
                    val token = msg.substring(msg.indexOf(":") + 1)
                    val mNode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(nettyData.getRemotedeviceIp())
                    if (mNode != null) {
                        mNode.alexaRefreshToken = token
                    }
//                    handleAlexaViews(sceneObject)
                    updateMusicPlayViews(sceneObject)
                }
            }

            MIDCONST.MID_PLAYTIME.toInt() -> {
                if (msg.isNotEmpty()) {
                    val longDuration = java.lang.Long.parseLong(msg)
                    sceneObject?.currentPlaybackSeekPosition = longDuration.toFloat()
                    ScanningHandler.getInstance().putSceneObjectToCentralRepo(nettyData.getRemotedeviceIp(), sceneObject)
                    updateMusicPlayViews(sceneObject)
                }
            }
        }

    }

    private fun handlePlayJsonUi(window: JSONObject, sceneObject: SceneObject?) {
        val updatedSceneObject = AppUtils.updateSceneObjectWithPlayJsonWindow(window, sceneObject!!)
        ScanningHandler.getInstance().putSceneObjectToCentralRepo(sceneObject.ipAddress, updatedSceneObject)
        updateMusicPlayViews(updatedSceneObject)
    }

    fun setControlIconsForAlexa(currentSceneObject: SceneObject?, play: ImageView, next: ImageView, previous: ImageView) {
        if (currentSceneObject == null) {
            return
        }
        val controlsArr = currentSceneObject.controlsValue ?: return
        play.isEnabled = controlsArr[0]
        play.isClickable = controlsArr[0]
        if (!controlsArr[0]) {
            play.setImageResource(R.drawable.play_white)
        }

        next.isEnabled = controlsArr[1]
        next.isClickable = controlsArr[1]
        if (controlsArr[1]) {
            next.setImageResource(R.drawable.prev_enabled)
        } else {
            next.setImageResource(R.drawable.prev_disabled)
        }

        previous.isEnabled = controlsArr[2]
        previous.isClickable = controlsArr[2]
        if (controlsArr[2]) {
            next.setImageResource(R.drawable.next_enabled)
        } else {
            next.setImageResource(R.drawable.next_disabled)
        }
    }

    private fun handleGCastMessage(nettyData: NettyData) {

        val packet = LUCIPacket(nettyData.getMessage())
        val msg = String(packet.getpayload())
        val mNode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(nettyData.getRemotedeviceIp())

        LibreLogger.d(this, "handleGCastMessage MB = "
                + packet.command
                + ", Ip = " + nettyData.remotedeviceIp
                +" msg = "+msg)

        when(packet.command){
            MIDCONST.FW_UPGRADE_PROGRESS.toInt() -> {
                if (mNode == null)
                    return
                var fwUpgradeData: FwUpgradeData? = LibreApplication.FW_UPDATE_AVAILABLE_LIST[nettyData.getRemotedeviceIp()]
                var mNewData = false
                if (fwUpgradeData == null) {
                    fwUpgradeData = FwUpgradeData(
                            mNode.ip,
                            mNode.friendlyname,
                            "",
                            0
                    )
                    mNewData = true
                    LibreApplication.FW_UPDATE_AVAILABLE_LIST[mNode.ip] = fwUpgradeData
                }

                /*if Gcast COmplete State or For SAC Device then we are showing Device will reboot
                 * because after firmwareupgraade COmpleted we are giving ok and coming back to PlayNEwScreen */
                if (msg.equals(Constants.GCAST_COMPLETE, ignoreCase = true)
                        && !mNode.friendlyname.equals(LibreApplication.sacDeviceNameSetFromTheApp, ignoreCase = true)) {
                    fwUpgradeData.setmProgressValue(100)
                    fwUpgradeData.updateMsg = getString(R.string.gcast_update_done)


                    showAlertForFwMsgBox(
                            "Firmware update completed for ",
                            mNode,
                            ", " + getString(R.string.deviceRebooting))
                    return
                }

                /*if Gcast Failed State  back to PlayNEwScreen */
                if (msg.equals("255", ignoreCase = true)
                        && !mNode.friendlyname.equals(LibreApplication.sacDeviceNameSetFromTheApp, ignoreCase = true)) {
                    showAlertForFwMsgBox("Firmware Update Failed For ", mNode)
                    fwUpgradeData.updateMsg = getString(R.string.fwUpdateFailed)
                    return
                }

                try {
                    fwUpgradeData.setmProgressValue(Integer.valueOf(msg))
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                if (mNewData)
                    showAlertForFirmwareUpgrade(mNode, fwUpgradeData)
            }

            MIDCONST.FW_UPGRADE_INTERNET_LS9.toInt() -> {
                if (mNode == null)
                    return

                if (!mNode.friendlyname.equals(LibreApplication.sacDeviceNameSetFromTheApp, ignoreCase = true)) {
                    if (msg.equals(Constants.NO_UPDATE, ignoreCase = true)) {
                        return
                    }
                    var fwUpgradeData: FwUpgradeData? = LibreApplication.FW_UPDATE_AVAILABLE_LIST[nettyData.getRemotedeviceIp()]
                    var mNewData = false
                    if (fwUpgradeData == null) {
                        fwUpgradeData = FwUpgradeData(
                                mNode.ip,
                                mNode.friendlyname,
                                "",
                                0
                        )
                        mNewData = true
                        LibreApplication.FW_UPDATE_AVAILABLE_LIST[mNode.ip] = fwUpgradeData
                    }

                    when {
                        msg.equals(Constants.UPDATE_STARTED, ignoreCase = true) -> fwUpgradeData.updateMsg = getString(R.string.mb223_update_started)
                        msg.equals(Constants.UPDATE_DOWNLOAD, ignoreCase = true) -> fwUpgradeData.updateMsg = getString(R.string.mb223_update_download)
                        msg.equals(Constants.UPDATE_IMAGE_AVAILABLE, ignoreCase = true) -> fwUpgradeData.updateMsg = getString(R.string.mb223_update_image_available)
                        msg.equals(Constants.NO_UPDATE, ignoreCase = true) -> fwUpgradeData.updateMsg = getString(R.string.noupdateAvailable)
                        msg.equals(Constants.DOWNLOAD_FAIL, ignoreCase = true)
                                || msg.equals(Constants.CRC_CHECK_ERROR, ignoreCase = true)-> fwUpgradeData.updateMsg = getString(R.string.mb223_download_fail)
                        else -> try {
                            val mProgressValue = Integer.valueOf(msg)
                            fwUpgradeData.updateMsg = getString(R.string.downloadingtheFirmare)
                            fwUpgradeData.setmProgressValue(mProgressValue)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    if (mNewData)
                        showAlertForFirmwareUpgrade(mNode, fwUpgradeData)
                }
            }
        }
    }


    /*overriding this method to hide Volume bar in all over app except now playing screen*/
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {

        val action = event.action
        val keyCode = event.keyCode


        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (action == KeyEvent.ACTION_DOWN) {
                    /*    LibreError error = new LibreError("Sorry!", Constants.VOLUME_PRESSED_MESSAGE);
                    showErrorMessage(error);*/
                }
                return true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (action == KeyEvent.ACTION_DOWN) {
                    /*   LibreError error = new LibreError("Sorry!", Constants.VOLUME_PRESSED_MESSAGE);
                    showErrorMessage(error);*/
                }
                return true
            }
            else -> return super.dispatchKeyEvent(event)
        }
    }

    /*this method is to show error in whole application*/
    fun showErrorMessage(message: LibreError?) {
        try {

            if (LibreApplication.hideErrorMessage)
                return
            if (isActivityPaused || isFinishing)
                return

            runOnUiThread {

                LibreLogger.d(this, "Showing the super toast " + System.currentTimeMillis() + message!!.errorMessage)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Toast.makeText(this@CTDeviceDiscoveryActivity, "" + message.errorMessage, Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                val superToast = SuperToast(this@CTDeviceDiscoveryActivity)

                if (message != null && message.errorMessage.contains("is no longer available")) {
                    LibreLogger.d(this, "Device go removed showing error in Device Discovery")
                    superToast.setGravity(Gravity.CENTER, 0, 0)
                }
                if (message.getmTimeout() == 0) {//TimeoutDefault
                    superToast.duration = SuperToast.Duration.LONG
                } else {
                    superToast.duration = SuperToast.Duration.VERY_SHORT
                }
                superToast.text = "" + message
                superToast.animations = SuperToast.Animations.FLYIN
                superToast.setIcon(SuperToast.Icon.Dark.INFO, SuperToast.IconPosition.LEFT)
                superToast.show()
                if (message != null && message.errorMessage.contains(""))
                    superToast.setGravity(Gravity.CENTER, 0, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }


    fun registerForDeviceEvents(libreListner: LibreDeviceInteractionListner) {
        this.libreDeviceInteractionListner = libreListner
    }

    fun unRegisterForDeviceEvents() {
        this.libreDeviceInteractionListner = null
    }

    fun registerAlexaLoginListener(alexaLoginListener: AlexaLoginListener) {
        this.alexaLoginListener = alexaLoginListener
    }

    fun unRegisterAlexaLoginListener() {
        this.alexaLoginListener = null
    }

    fun intentToHome(context: Context) {
        Log.d("intentToHome","called by "+context::class.java.simpleName)
        val newIntent: Intent
        if (LSSDPNodeDB.getInstance().GetDB().size > 0) {
            //            newIntent = new Intent(context, ActiveScenesListActivity.class);
            newIntent = Intent(context, CTHomeTabsActivity::class.java)
            newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP and Intent.FLAG_ACTIVITY_NEW_TASK)
            newIntent.putExtra(AppConstants.LOAD_FRAGMENT, CTActiveDevicesFragment::class.java.simpleName)
            startActivity(newIntent)
            finish()
        } else {
            //            newIntent = new Intent(context, ConfigureActivity.class);
            newIntent = Intent(context, CTHomeTabsActivity::class.java)
            newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP and Intent.FLAG_ACTIVITY_NEW_TASK)
            newIntent.putExtra(AppConstants.LOAD_FRAGMENT, CTNoDeviceFragment::class.java.simpleName)
            startActivity(newIntent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        isActivityPaused = false
    }

    private fun initEventsAndServers() {
        try {
            BusProvider.getInstance().register(busEventListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (LibreApplication.mCleanUpIsDoneButNotRestarted) {
            LibreApplication.mCleanUpIsDoneButNotRestarted = false
//            restartApp(this@CTDeviceDiscoveryActivity)
        }

        try {
            startService(Intent(this@CTDeviceDiscoveryActivity, DMRDeviceListenerForegroundService::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                action = Constants.ACTION.STARTFOREGROUND_ACTION
            })
        } catch (e:java.lang.Exception){
            e.printStackTrace()
        }

        val connManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)

        if (mWifi != null && mWifi.isConnected && MicTcpServer.MIC_TCP_SERVER_PORT == 0
                && getConnectedSSIDName(this) != null
                && !getConnectedSSIDName(this)?.contains(Constants.RIVAA_WAC_SSID)) {
            try {
                libreApplication.restart()
                libreApplication.micTcpStart()
            } catch (e: SocketException) {
                e.printStackTrace()
            }

        }
    }

    fun onWifiScanDone(scanResults: List<ScanResult>) {

    }

    override fun onStop() {
        Log.d(TAG, "onStop")
        unRegisterForDeviceEvents()

        if (upnpProcessor != null) {
            upnpProcessor!!.removeListener(UpnpDeviceManager.getInstance())
            upnpProcessor!!.unbindUpnpService()
        }

        try {
            BusProvider.getInstance().unregister(busEventListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        try {
            this.unregisterReceiver(localNetworkStateReceiver)
        } catch (e: Exception) {
            LibreLogger.d(this, "Exception happened while unregistering the reciever!! " + e.message)

        }

    }

    /* For DMR playback */
    override fun onStartComplete() {
        super.onStartComplete()
    }

    private inner class LocalNetworkStateReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "onReceive called")
            Log.d(TAG, "action = " + intent.action + ", listenToNetworkChanges = " + isNetworkChangesCallBackEnabled)

            connectivityOnReceiveCalled(context, intent)

            val wifiUtil = WifiUtil(this@CTDeviceDiscoveryActivity)
            val supplicantState = wifiUtil.getWifiSupplicantState()
            if (!wifiUtil.isWifiOn() /*&& isNetworkOffCallBackEnabled*/) {
                if (activeSSID != null && activeSSID != "") {
                    LibreApplication.mActiveSSIDBeforeWifiOff = activeSSID
                }
                activeSSID = ""
                //                showWifiNetworkOffAlert();
                wifiConnected(false)
//                return
            }

            if (intent.action != "android.net.conn.TETHER_STATE_CHANGED" && intent.action != ConnectivityManager.CONNECTIVITY_ACTION)
                return

            if (intent.action != null && intent.action == ConnectivityManager.CONNECTIVITY_ACTION) {
                val networkInfo = intent.getParcelableExtra<NetworkInfo>(ConnectivityManager.EXTRA_NETWORK_INFO)

                Log.e("onReceive", "type = " + networkInfo.typeName
                        + ", isConnected = " + networkInfo.isConnected
                        + ", supplicantState = $supplicantState")

                if (networkInfo.type == ConnectivityManager.TYPE_WIFI) {
                    connectedSSID = AppUtils.getConnectedSSID(this@CTDeviceDiscoveryActivity)
                    Log.e("onReceive", "wifiInfo, connectedSSID = " + connectedSSID!!)

                    if (connectedSSID == null || connectedSSID == "<unknown ssid>") {
                        if (networkInfo.extraInfo == null) {
                            Log.e("onReceive", "getExtraInfo null")
                        } else {
                            connectedSSID = networkInfo.extraInfo.replace("\"", "")
                            if (connectedSSID == null || connectedSSID == "<unknown ssid>") {
                                Log.e("onReceive", "networkInfo connectedSSID =" + connectedSSID!!)
                            }
                        }
                    }

                    if (networkInfo.isConnected) {
                        if (!isFinishing
                                && !activeSSID.isNullOrEmpty()
                                && activeSSID != connectedSSID
                                && context.javaClass.simpleName != CTConnectToWifiActivity::class.java.simpleName
                                && !connectedSSID!!.contains(Constants.RIVAA_WAC_SSID)) {
                            showNetworkChangeRestartAppAlert()
                        } else
                            wifiConnected(true)
                    } else wifiConnected(false)
                } else if (networkInfo.type == ConnectivityManager.TYPE_MOBILE || networkInfo.type == ConnectivityManager.TYPE_MOBILE_HIPRI) {
                    Log.e("onReceive","TYPE_MOBILE networkInfo.isConnected = ${networkInfo.isConnected}, "
                            + "wifi Supplicant name = ${supplicantState.name}")

                    if (networkInfo.isConnected) {
                        /*if (wifiUtil.isWifiOn())
                            wifiConnected(connected = true)*/
                        wifiConnected(connected = false)
                    } else wifiConnected(wifiUtil.isWifiOn())
                }
            }
        }
    }//m_nwStateListener = nwStateListener;

    open fun wifiConnected(connected: Boolean) {
        LibreLogger.d(this,"wifiConnected $connected")
        if (!connected){
//            clearing data
            cleanUpCode(false)
        } else {
            restartAllSockets()
        }
//        if (!connected) libreApplication?.clearApplicationCollections()
    }

    fun showNetworkChangeRestartAppAlert() {
        LibreLogger.d(this,"showNetworkChangeRestartAppAlert isNetworkChangesCallBackEnabled = $isNetworkChangesCallBackEnabled")

        if (!isNetworkChangesCallBackEnabled) return

        if (alertRestartApp != null && alertRestartApp!!.isShowing) {
            alertRestartApp!!.dismiss()
        }
        val builder = AlertDialog.Builder(this@CTDeviceDiscoveryActivity)
        val mMessage = getString(R.string.restartTitle)//, ssid, LibreApplication.mActiveSSIDBeforeWifiOff);
        builder.setMessage(mMessage)
                .setCancelable(false)
                .setPositiveButton(getString(R.string.ok)) { dialog, id ->
                    Log.e("NetworkRestartAlert", "ok clicked")
                    cleanUpCode(true)
                    restartApp(applicationContext)
                }
        if (alertRestartApp == null || !alertRestartApp!!.isShowing)
            alertRestartApp = builder.show()
    }

    fun showWifiNetworkOffAlert() {
        if (!this@CTDeviceDiscoveryActivity.isFinishing && !isNetworkOffCallBackEnabled) {

            if (alertRestartApp != null && alertRestartApp!!.isShowing) {
                alertRestartApp!!.dismiss()
            }

            val alertDialogBuilder = AlertDialog.Builder(
                    this@CTDeviceDiscoveryActivity)

            // set title
            alertDialogBuilder.setTitle(getString(R.string.wifiConnectivityStatus))

            // set dialog message
            alertDialogBuilder
                    .setMessage(getString(R.string.connectToWifi))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.ok)) { dialog, id ->
                        // if this button is clicked, close
                        // current activity
                        dialog.cancel()
                        //                            LibreApplication.prevConnectedSSID = LibreApplication.connectedSSID;
                        //                            LibreApplication.connectedSSID = null;
                        LibreApplication.mCleanUpIsDoneButNotRestarted = false
                        /*  startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));*/
                        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        startActivityForResult(intent, WIFI_SETTINGS_REQUEST_CODE)
                    }

            // create alertDialog1 dialog
            if (alertRestartApp == null || !alertRestartApp!!.isShowing)
                alertRestartApp = alertDialogBuilder.show()
        }
    }

    open fun removeTheDeviceFromRepo(ipadddress: String?) {
        if (ipadddress != null) {
            if (libreDeviceInteractionListner != null)
                libreDeviceInteractionListner!!.deviceGotRemoved(ipadddress)

            val mNodeDB = LSSDPNodeDB.getInstance()
            try {
                if (ScanningHandler.getInstance().isIpAvailableInCentralSceneRepo(ipadddress)) {
                    val status = ScanningHandler.getInstance().removeSceneMapFromCentralRepo(ipadddress)
                    LibreLogger.d(this, "Active Scene Adapter For the Master $status")
                }
                try {
                    val renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(ipadddress)
                    val renderingUDN = renderingDevice!!.identity.udn.toString()
                    val playbackHelper = LibreApplication.PLAYBACK_HELPER_MAP[renderingUDN]

                    val mScanHandler = ScanningHandler.getInstance()
                    val currentSceneObject = mScanHandler.getSceneObjectFromCentralRepo(ipadddress)

                    try {
                        if (playbackHelper != null
                                && renderingDevice != null
                                && currentSceneObject != null
                                && currentSceneObject.playUrl != null
                                && !currentSceneObject.playUrl.equals("", ignoreCase = true)
                                && currentSceneObject.playUrl.contains(LibreApplication.LOCAL_IP)
                                && currentSceneObject.playUrl.contains(ContentTree.AUDIO_PREFIX)) {
                            playbackHelper.StopPlayback()
                        }
                    } catch (e: Exception) {

                        LibreLogger.d(this, "Handling the exception while sending the stopMediaServer command ")
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }

            } catch (e: Exception) {
                LibreLogger.d(this, "Active Scene Adapter" + "Removal Exception ")
            }

            mNodeDB.clearNode(ipadddress)
        }

    }

    fun alertDialogForDeviceNotAvailable(mNode: LSSDPNodes?) {
        run {
            if (!this@CTDeviceDiscoveryActivity.isFinishing) {
                if (mNode == null)
                    return
                alertDialog1 = null
                val alertDialogBuilder = AlertDialog.Builder(
                        this@CTDeviceDiscoveryActivity)

                // set title
                alertDialogBuilder.setTitle(getString(R.string.deviceNotAvailable))

                // set dialog message
                alertDialogBuilder
                        .setMessage(getString(R.string.removeDeviceMsg) + " " + mNode.friendlyname + " " + getString(R.string.removeDeviceMsg2))
                        .setCancelable(false)
                        .setPositiveButton(getString(R.string.ok)) { dialog, id ->
                            // if this button is clicked, close
                            // current activity
                            alertDialog1!!.dismiss()
                            removeTheDeviceFromRepo(mNode.ip)
                            if (libreDeviceInteractionListner != null) {
                                libreDeviceInteractionListner!!.deviceGotRemoved(mNode.ip)
                            }
                        }

                // create alertDialog1 dialog
                if (alertDialog1 == null)
                    alertDialog1 = alertDialogBuilder.create()

                alertDialog1!!.show()
            }
        }
    }

    fun disableNetworkOffCallBack() {
        isNetworkOffCallBackEnabled = false
    }

    fun enableNetworkOffCallBack() {
        isNetworkOffCallBackEnabled = true
    }

    fun disableNetworkChangeCallBack() {
        isNetworkChangesCallBackEnabled = false
    }

    fun enableNetworkChangeCallBack() {
        isNetworkChangesCallBackEnabled = true
    }

    /* Whenever we are going to Do app Restarting,its not restarting the app properly and We are doing Cleanup and then We are restarting the APP
     * Till i HAave to Analyse more on this code */
    fun cleanUpCode(mCompleteCleanup: Boolean) {
        LibreLogger.d(this, "Cleanup is going To Call")
        LibreApplication.mCleanUpIsDoneButNotRestarted = true
        val application = application as LibreApplication
        try {

            for (mAndroid in LUCIControl.luciSocketMap.values.toTypedArray()) {
                if (mAndroid != null && mAndroid.handler != null && mAndroid.handler.mChannelContext != null)
                    mAndroid.handler.mChannelContext.close()
            }

            for (mAndroidChannelHandlerContext in LUCIControl.channelHandlerContextMap.values.toTypedArray()) {
                mAndroidChannelHandlerContext?.channel?.close()
            }

            ensureDMRPlaybackStopped()

            application.scanThread.mRunning = false
            application.scanThread.close()
            application.scanThread.mAliveNotifyListenerSocket = null
            application.scanThread.mDatagramSocketForSendingMSearch = null

            if (mCompleteCleanup) {
                this.libreDeviceInteractionListner = null
                try {
                    BusProvider.getInstance().unregister(busEventListener)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }

            if (application.scanThread.nettyServer.mServerChannel != null && application.scanThread.nettyServer.mServerChannel.isBound) {
                application.scanThread.nettyServer.mServerChannel.unbind()
                val serverClose = application.scanThread.nettyServer.mServerChannel.close()
                serverClose.awaitUninterruptibly()

                /*to resolve hang issue*/
                //                application.getScanThread().nettyServer.serverBootstrap.releaseExternalResources();
            }

            LUCIControl.channelHandlerContextMap.clear()
            application.clearApplicationCollections()
            LibreLogger.d(this, "Cleanup GOintFor Binder ")
            if (upnpBinder == null)
                return
            try {
                if (upnpProcessor != null) {
                    upnpProcessor!!.removeListener(this)
                    upnpProcessor!!.removeListener(UpnpDeviceManager.getInstance())
                    upnpProcessor!!.unbindUpnpService()

                    LibreLogger.d(this, "Cleanup UnBinded THe Service ")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        LibreLogger.d(this, "Cleanup is done Successfully")
        Log.d("NetworkChanged", "BroadcastReceiver Intent")
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    fun restartApp(context: Context) {
        Log.d("NetworkChanged", "App is Restarting")
        /* Stopping ForeGRound Service Whenwe are Restarting the APP */
        stopDmrForegroundService()

        restartSplashScreen()

        finishActivitiesAndKillAppProcess()
    }

    private fun restartSplashScreen() {
        Log.e("restartSplashScreen", "starting")
        val mStartActivity = Intent(this, CTSplashScreenActivity::class.java)
        /*sending to let user know that app is restarting*/
        mStartActivity.putExtra(SpalshScreenActivity.APP_RESTARTING, true)
        val mPendingIntentId = 123456
        val mPendingIntent = PendingIntent.getActivity(this, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT)
        val alarmManager = this.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 200, mPendingIntent)
    }

    fun restartAllSockets(): Boolean {
        libreApplication?.initLUCIServices()
        return true
    }

    fun phoneIpAddress(): String {
        val mUtil = Utils()
        return mUtil.getIPAddress(true)
    }

    fun restartApplicationForNetworkChanges(context: Context, ssid: String) {
        cleanUpCode(true)
        restartApp(context)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> checkLocationPermission()
            MICROPHONE_PERMISSION_REQUEST_CODE -> checkMicrophonePermission()
            READ_STORAGE_REQUEST_CODE -> checkReadStoragePermission()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == WIFI_SETTINGS_REQUEST_CODE) {
            LibreLogger.d(this, "came back from wifi list")
            val connManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
            /*For routers without Internet*/

            if (mWifi.isConnected || mWifi.detailedState == NetworkInfo.DetailedState.CONNECTED) {
                if (!isFinishing) {
                    showNetworkChangeRestartAppAlert()
                }
            }
        }

        if (requestCode == LOCATION_PERM_SETTINGS_REQUEST_CODE || requestCode == LOCATION_SETTINGS_REQUEST_CODE) {
            checkLocationPermission()
        }

        if (requestCode == MICROPHONE_PERM_SETTINGS_REQUEST_CODE) {
            checkMicrophonePermission()
        }

        if (requestCode == STORAGE_PERM_SETTINGS_REQUEST_CODE) {
            checkReadStoragePermission()
        }
    }


    private fun checkForTheSACDeviceSuccessDialog(node: LSSDPNodes?) {

        val sharedPreferences = applicationContext
                .getSharedPreferences("sac_configured", Context.MODE_PRIVATE)
        val str = sharedPreferences.getString("deviceFriendlyName", "")
        if (str != null && str.equals(node!!.friendlyname.toString(), ignoreCase = true) && node.getgCastVerision() != null) {


            LibreApplication.thisSACDeviceNeeds226 = node.friendlyname
            if (LibreApplication.FW_UPDATE_AVAILABLE_LIST.containsKey(node.ip))
                LibreApplication.FW_UPDATE_AVAILABLE_LIST.remove(node.ip)

            val alertDialogBuilder = AlertDialog.Builder(
                    this)

            // set title
            alertDialogBuilder.setTitle("Configure successful")

            // set dialog message
            alertDialogBuilder
                    .setMessage("Cast your favorite music and radio apps from your phone or tablet to your " + node.friendlyname.toString())
                    .setCancelable(false)
                    .setNegativeButton("No Thanks") { dialogInterface, i -> dialogInterface.cancel() }
                    .setPositiveButton("Learn More") { dialog, id ->
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        intent.data = Uri.parse("https://www.google.com/cast/audio/learn")
                        startActivity(intent)
                    }

            val sAcalertDialog = alertDialogBuilder.create()
            /*sAcalertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);*/
            sAcalertDialog.show()
            sharedPreferences.edit().remove("deviceFriendlyName").apply()
        } else if (node != null && node.getgCastVerision() == null && str!!.equals(node.friendlyname, ignoreCase = true)) {
            sharedPreferences.edit().remove("deviceFriendlyName").apply()
        }

    }

    private fun showAlertForFwMsgBox(Message: String, node: LSSDPNodes?) {
        if (!LibreApplication.sacDeviceNameSetFromTheApp.equals("", ignoreCase = true))
            return

        val alertDialogBuilder = AlertDialog.Builder(
                this@CTDeviceDiscoveryActivity)

        // set title
        alertDialogBuilder.setTitle("Firmware Upgrade")

        // set dialog message
        alertDialogBuilder
                .setMessage(Message + node!!.friendlyname)
                .setCancelable(false)
                .setPositiveButton("OK") { dialog, id -> sAcalertDialog = null }
        if (sAcalertDialog == null)
            sAcalertDialog = alertDialogBuilder.create()

        /*sAcalertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);*/
        sAcalertDialog!!.show()
        try {
            sAcalertDialog!!.show()
        } catch (e: Exception) {
            LibreLogger.d(this, "Permission Denied")
        }

    }

    // overloaded this method because, on success we have to show "Device is Rebooting(an extra text at the end)
    private fun showAlertForFwMsgBox(Message: String, node: LSSDPNodes, Message2: String) {
        if (!LibreApplication.sacDeviceNameSetFromTheApp.equals("", ignoreCase = true))
            return

        val alertDialogBuilder = AlertDialog.Builder(
                this@CTDeviceDiscoveryActivity)
        alertDialogBuilder.setTitle("Firmware Upgrade")
        alertDialogBuilder
                .setMessage(Message + node.friendlyname.toString() + Message2)
                .setCancelable(false)
                .setPositiveButton("OK") { dialog, id -> sAcalertDialog = null }
        if (sAcalertDialog == null)
            sAcalertDialog = alertDialogBuilder.create()
        sAcalertDialog!!.show()
    }

    private fun showAlertForFirmwareUpgrade(mNode: LSSDPNodes, fwUpgradeData: FwUpgradeData) {
        if (!LibreApplication.sacDeviceNameSetFromTheApp.equals("", ignoreCase = true))
            return
        val builder = AlertDialog.Builder(this@CTDeviceDiscoveryActivity)
        // set title
        builder.setTitle("Firmware Upgrade")
        builder.setMessage(mNode.friendlyname + ": New update available. Firmware is upgrading with new update")
                .setCancelable(false)
                .setPositiveButton("OK") { dialog, id ->
                    dialog.dismiss()
                    fwUpdateAlertDialog = null
                    BusProvider.getInstance().post(fwUpgradeData)
                }
        // create alertDialog1 dialog

        if (fwUpdateAlertDialog == null)
            fwUpdateAlertDialog = builder.create()
        if (!fwUpdateAlertDialog?.isShowing!!)
            fwUpdateAlertDialog!!.show()
    }

    fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    fun showToast(messageID: Int) {
        Toast.makeText(this, messageID, Toast.LENGTH_SHORT).show()
    }

    fun showProgressDialog(message: String) {
        if (isFinishing)
            return
        if (mProgressDialog == null || !mProgressDialog!!.isShowing) {
            mProgressDialog = ProgressDialog(this, ProgressDialog.THEME_HOLO_DARK)
        }
        mProgressDialog!!.setCancelable(false)
        mProgressDialog!!.setMessage(message)
        mProgressDialog!!.show()
    }

    fun showProgressDialog(messageID: Int) {
        if (isFinishing)
            return
        if (mProgressDialog == null || !mProgressDialog!!.isShowing) {
            mProgressDialog = ProgressDialog(this, ProgressDialog.THEME_HOLO_DARK)
        }
        mProgressDialog!!.setCancelable(false)
        mProgressDialog!!.setMessage(getString(messageID))
        mProgressDialog!!.show()
    }

    fun dismissDialog() {
        if (isFinishing)
            return
        if (mProgressDialog != null && mProgressDialog!!.isShowing)
            mProgressDialog!!.dismiss()
    }

    /*Override this method in child activity that extends this activity,
    so as to be notified every time there's network change*/
    open fun connectivityOnReceiveCalled(context: Context, intent: Intent) {
        /*Let child classes handle*/
    }

    /*this method will release local DMR playback songs*/
    fun ensureDMRPlaybackStopped() {

        val mScanningHandler = ScanningHandler.getInstance()
        val centralSceneObjectRepo = mScanningHandler.sceneObjectFromCentralRepo

        /*which means no master present hence all devices are free so need to do anything*/
        if (centralSceneObjectRepo == null || centralSceneObjectRepo.size == 0) {
            LibreLogger.d(this, "No master present")
            return
        }

        try {
            for (masterIPAddress in centralSceneObjectRepo.keys) {
                val renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(masterIPAddress)

                val mScanHandler = ScanningHandler.getInstance()
                val currentSceneObject = mScanHandler.getSceneObjectFromCentralRepo(masterIPAddress)

                if (renderingDevice != null
                        && currentSceneObject != null
                        && currentSceneObject.playUrl != null
                        && !currentSceneObject.playUrl.equals("", ignoreCase = true)
                        && currentSceneObject.playUrl.contains(LibreApplication.LOCAL_IP)
                        && currentSceneObject.playUrl.contains(ContentTree.AUDIO_PREFIX)) {
                    val renderingUDN = renderingDevice.identity.udn.toString()
                    val playbackHelper = LibreApplication.PLAYBACK_HELPER_MAP[renderingUDN]
                    playbackHelper?.StopPlayback()
                }

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun requestLuciUpdates(devicIp: String) {
        try {
            val luciControl = LUCIControl(devicIp)

            val luciPackets = ArrayList<LUCIPacket>()
            val mSceneNamePacket = LUCIPacket(null, 0.toShort(), MIDCONST.MID_SCENE_NAME.toShort(), LSSDPCONST.LUCI_GET.toByte())
            /*removing it to resolve flickering issue*/
            val volumePacket = LUCIPacket(null, 0.toShort(), MIDCONST.VOLUME_CONTROL.toShort(), LSSDPCONST.LUCI_GET.toByte())
            val currentSourcePacket = LUCIPacket(null, 0.toShort(), MIDCONST.MID_CURRENT_SOURCE, LSSDPCONST.LUCI_GET.toByte())
            val currentPlayStatePacket = LUCIPacket(null, 0.toShort(), MIDCONST.MID_CURRENT_PLAY_STATE, LSSDPCONST.LUCI_GET.toByte())
            val getUIPacket = LUCIPacket(LUCIMESSAGES.GET_PLAY.toByteArray(), LUCIMESSAGES.GET_PLAY.length.toShort(),
                    MIDCONST.MID_REMOTE_UI, LSSDPCONST.LUCI_SET.toByte())
            val currentPlayTime = LUCIPacket(null, 0.toShort(), MIDCONST.MID_PLAYTIME, LSSDPCONST.LUCI_GET.toByte())

//            luciControl.SendCommand(MIDCONST.MID_REMOTE_UI.toInt(),LUCIMESSAGES.GET_PLAY,LSSDPCONST.LUCI_SET)
            luciPackets.add(getUIPacket)
            luciPackets.add(volumePacket)
            luciPackets.add(mSceneNamePacket)
            luciPackets.add(currentSourcePacket)
            luciPackets.add(currentPlayStatePacket)
            luciPackets.add(currentPlayTime)

            luciControl.SendCommand(luciPackets)
            AlexaUtils.sendAlexaRefreshTokenRequest(devicIp)

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun showLoginWithAmazonAlert(speakerIpAddress: String): Boolean {
        if (isFinishing)
            return true

        AlertDialog.Builder(this@CTDeviceDiscoveryActivity)
                .setTitle(R.string.login_with_amazon)
                .setMessage(R.string.login_with_amazon_warn)
                .setCancelable(false)
                .setPositiveButton(R.string.amazon_login) { dialog, id ->
                    dialog.dismiss()
                    val amazonLoginScreen = Intent(this@CTDeviceDiscoveryActivity, CTAmazonLoginActivity::class.java)
                    amazonLoginScreen.putExtra(Constants.CURRENT_DEVICE_IP, speakerIpAddress)
                    startActivity(amazonLoginScreen)
                }
                .setNegativeButton(R.string.cancel) { dialogInterface, i -> dialogInterface.dismiss() }
                .show()
        return true
    }

    private fun checkMicrophonePermission() {
        if (!AppUtils.isPermissionGranted(this, Manifest.permission.RECORD_AUDIO)) {
            requestRecordPermission()
        } else {
            Log.d("checkMicrophonePerm", "Permission already granted.")
        }
    }

    fun requestRecordPermission() {
        Log.i("requestRecordPerm", "Record permission has NOT been granted. Requesting permission.")
        if (AppUtils.shouldShowPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // For example if the user has previously denied the permission.
            Log.i("requestRecordPerm", "Displaying Record permission rationale to provide additional context.")
            Snackbar.make(parentView!!, R.string.permission_record_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok) { AppUtils.requestPermission(this@CTDeviceDiscoveryActivity, Manifest.permission.RECORD_AUDIO, MICROPHONE_PERMISSION_REQUEST_CODE) }
                    .show()
        } else {
            if (!sharedPreferenceHelper.isFirstTimeAskingPermission(Manifest.permission.RECORD_AUDIO)) {
                sharedPreferenceHelper.firstTimeAskedPermission(Manifest.permission.RECORD_AUDIO, true)
                // No explanation needed, we can request the permission.
                // Camera permission has not been granted yet. Request it directly.
                AppUtils.requestPermission(this, Manifest.permission.RECORD_AUDIO, MICROPHONE_PERMISSION_REQUEST_CODE)
            } else {
                //                showRecordPermissionDisabledSnackBar();
                showAlertForRecordPermissionRequired()
            }

        }
    }

    private fun showRecordPermissionDisabledSnackBar() {
        //Permission disable by device policy or user denied permanently. Show proper error message
        Snackbar.make(parentView!!, R.string.enableRecordPermit,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.action_settings) {
                    startActivityForResult(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(Uri.fromParts("package", packageName, null))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                            .addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS), MICROPHONE_PERM_SETTINGS_REQUEST_CODE)
                }
                .show()
    }

    private fun showAlertForRecordPermissionRequired() {
        if (mandateDialog != null && mandateDialog!!.isShowing)
            mandateDialog!!.dismiss()

        if (mandateDialog == null) {
            mandateDialog = AlertDialog.Builder(this)
                    .setMessage(getString(R.string.enableRecordPermit))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.action_settings)) { dialogInterface, i ->
                        //navigate to settings
                        dialogInterface.dismiss()
                        startActivityForResult(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(Uri.fromParts("package", packageName, null))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                                .addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS), MICROPHONE_PERM_SETTINGS_REQUEST_CODE)
                    }
                    .setNegativeButton(R.string.cancel) { dialogInterface, i -> dialogInterface.dismiss() }
                    .create()
        }

        if (!mandateDialog!!.isShowing) {
            mandateDialog!!.show()
        }
    }

    private fun showAlertForLocationPermissionRequired() {
        if (mandateDialog != null && mandateDialog!!.isShowing)
            mandateDialog!!.dismiss()

        if (mandateDialog == null) {
            mandateDialog = AlertDialog.Builder(this)
                    .setMessage(getString(R.string.enableLocationPermit))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.action_settings)) { dialogInterface, i ->
                        //navigate to settings
                        dialogInterface.dismiss()
                        startActivityForResult(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(Uri.fromParts("package", packageName, null))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                                .addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS), LOCATION_PERM_SETTINGS_REQUEST_CODE)
                    }
                    .setNegativeButton(R.string.cancel) { dialogInterface, i -> dialogInterface.dismiss() }
                    .create()

        }

        if (!mandateDialog!!.isShowing) {
            mandateDialog!!.show()
        }
    }

    override fun recordError(error: String) {
        LibreLogger.d(this, "recordError = $error")
        showToast(error)
    }

    override fun recordStopped() {
        LibreLogger.d(this, "recordStopped")
    }

    override fun recordProgress(byteBuffer: ByteArray) {

    }

    override fun sendBufferAudio(audioBufferBytes: ByteArray) {
        micTcpServer!!.sendDataToClient(audioBufferBytes)
    }

    override fun micExceptionCaught(e: Exception) {
        LibreLogger.d(this, "micExceptionCaught = " + e.message)
        audioRecordUtil!!.stopRecording()
    }
}