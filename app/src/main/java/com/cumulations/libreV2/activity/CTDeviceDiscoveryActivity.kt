package com.cumulations.libreV2.activity

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.net.wifi.ScanResult
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
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
import com.libre.Ls9Sac.GcastUpdateData
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
import com.cumulations.libreV2.AppConstants.WIFI_SETTINGS_REQUEST_CODE
import com.libre.*
import com.libre.LibreApplication.activeSSID
import com.libre.util.PicassoTrustCertificates
import com.squareup.picasso.MemoryPolicy
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
    protected var upnpProcessor: UpnpProcessorImpl? = null
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

    private var audioRecordUtil: AudioRecordUtil? = null
    private var micTcpServer: MicTcpServer? = null

    private var busEventListener: Any = object : Any() {
        @Subscribe
        fun newDeviceFound(nodes: LSSDPNodes) {
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

            handleTheGoogleCastMessages(nettyData)
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
        fun gcastNewInernetFirmwareFound(mData: GcastUpdateData) {
            Log.d(TAG, "gcastNewInernetFirmwareFound() called with: " +
                    "isGcastInternet = [" + mData.getmDeviceName() + "]"
                    + LibreApplication.GCAST_UPDATE_AVAILABE_LIST_DATA.keys.toString())
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
    }

    private val isMicrophonePermissionGranted: Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AppUtils.isPermissionGranted(this, Manifest.permission.RECORD_AUDIO)
        } else true

    val upnpBinder: CoreUpnpService.Binder?
        get() = upnpProcessor!!.binder
    private var sAcalertDialog: AlertDialog? = null
    private var gCastUpdateAlertDialog: AlertDialog? = null


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
            Log.d("TAG", "Permission already granted.")
            if (AppUtils.isLocationServiceEnabled(this)) {
                connectedSSID = getConnectedSSIDName(this)
                Log.d("checkLocationPermission", "connectedSSID \$connectedSSID")
            } else
                showLocationMustBeEnabledDialog()
        }
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
                //                showLocPermissionDisabledSnackBar();
                showAlertForLocationPermissionRequired()
            }

        }
    }

    private fun showLocPermissionDisabledSnackBar() {
        //Permission disable by device policy or user denied permanently. Show proper error message
        Snackbar.make(parentView!!, R.string.permission_location_disabled,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.open) {
                    startActivityForResult(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(Uri.fromParts("package", packageName, null))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                            .addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS), LOCATION_PERM_SETTINGS_REQUEST_CODE)
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
                messageView.gravity = Gravity.CENTER
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

    private fun setMusicPlayerListeners(){
        val musicSceneObject = ScanningHandler.getInstance().getSceneObjectFromCentralRepo(musicPlayerIp)
        val mNodeWeGotForControl = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(musicPlayerIp)
        val control = LUCIControl(musicPlayerIp)

        playPauseView!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View) {
                if (musicSceneObject!!.currentSource == Constants.AUX_SOURCE
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
                    playPauseView!!.setImageResource(R.drawable.play_orange)
                } else {
                    if (musicSceneObject.currentSource == Constants.BT_SOURCE) {
                        control.SendCommand(MIDCONST.MID_PLAYCONTROL.toInt(), LUCIMESSAGES.PLAY, CommandType.SET)
                    } else {
                        control.SendCommand(MIDCONST.MID_PLAYCONTROL.toInt(), LUCIMESSAGES.RESUME, CommandType.SET)
                    }
                    // musicSceneObject.setPlaystatus(SceneObject.CURRENTLY_PLAYING);
                    playPauseView!!.setImageResource(R.drawable.pause_orange)
                }
            }
        })

        alexaButton!!.setOnLongClickListener(View.OnLongClickListener {
            if (!isMicrophonePermissionGranted) {
                requestRecordPermission()
                return@OnLongClickListener true
            }

            if (mNodeWeGotForControl == null || mNodeWeGotForControl.alexaRefreshToken.isEmpty()) {
                //                    (context as CTDeviceDiscoveryActivity).showToast("Login into Amazon")
                musicSceneObject!!.isAlexaBtnLongPressed = false
                toggleAVSViews(false)
                showLoginWithAmazonAlert(musicSceneObject.ipAddress)
                return@OnLongClickListener true
            }

            val phoneIp = phoneIpAddress()
            if (!musicSceneObject!!.ipAddress.isEmpty() && !phoneIp.isEmpty()) {

                Log.d("OnLongClick", "phone ip: " + phoneIp + "port: " + MicTcpServer.MIC_TCP_SERVER_PORT)
                control.SendCommand(MIDCONST.MID_MIC, Constants.START_MIC + phoneIp + "," + MicTcpServer.MIC_TCP_SERVER_PORT, LSSDPCONST.LUCI_SET)

                musicSceneObject.isAlexaBtnLongPressed = true
                toggleAVSViews(true)
                audioRecordUtil!!.startRecording(this@CTDeviceDiscoveryActivity)
            } else {
                musicSceneObject.isAlexaBtnLongPressed = false
                toggleAVSViews(false)
                showToast("Ip not available")
            }
            true
        })

        alexaButton!!.setOnTouchListener { view, motionEvent ->
            Log.d("AlexaBtn", "motionEvent = " + motionEvent.action)
            if (motionEvent.action == MotionEvent.ACTION_UP) {
                Log.d("AlexaBtn", "long press release, sceneObject isLongPressed = " + musicSceneObject!!.isAlexaBtnLongPressed)
                /*if (sceneObject?.isAlexaBtnLongPressed!!) {
                        sceneObject?.isAlexaBtnLongPressed = false
                        toggleAVSViews(false)
                    }*/
                musicSceneObject.isAlexaBtnLongPressed = false
                toggleAVSViews(false)
            }
            false
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

        if (musicPlayerWidget == null)
            return

        toggleAVSViews(sceneObject?.isAlexaBtnLongPressed!!)

        val context = this
        when (sceneObject?.currentSource) {

            Constants.NO_SOURCE,
            Constants.DDMSSLAVE_SOURCE -> {
                playPauseView?.isClickable = false
                if (sceneObject?.currentSource == Constants.NO_SOURCE){
                    songSeekBar?.visibility = View.GONE
                    albumArtView?.visibility = View.GONE
                    playPauseView?.visibility = View.GONE
                    trackNameView?.text = context.getText(R.string.libre_voice)

                    val node = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(sceneObject?.ipAddress)
                    if (node?.alexaRefreshToken.isNullOrEmpty()){
                        trackNameView?.visibility = View.VISIBLE
                        trackNameView?.text = context.getText(R.string.login_to_enable_cmds)
                        albumNameView?.visibility = View.GONE
                    } else {
                        albumNameView?.visibility = View.VISIBLE
                        trackNameView?.visibility = View.VISIBLE
                        trackNameView?.text = context.getText(R.string.app_name)
                        albumNameView?.text = context.getText(R.string.speaker_ready_for_cmds)
                    }
                }
            }

            Constants.VTUNER_SOURCE,
            Constants.TUNEIN_SOURCE -> {
                playPauseView?.setImageResource(R.drawable.play_white)
                songSeekBar?.visibility = View.VISIBLE
                songSeekBar?.progress = 0
                songSeekBar?.isEnabled = false
            }

            Constants.AUX_SOURCE -> {
                playPauseView?.setImageResource(R.drawable.play_white)
                songSeekBar?.visibility = View.VISIBLE
                songSeekBar?.progress = 0
                songSeekBar?.isEnabled = false

                trackNameView?.text = context.getText(R.string.aux)
                PicassoTrustCertificates.getInstance(context).load(R.mipmap.album_art).memoryPolicy(MemoryPolicy.NO_STORE)
                        .placeholder(R.mipmap.album_art)
                        .into(albumArtView)
            }

            Constants.BT_SOURCE -> {
                trackNameView?.text = context.getText(R.string.bluetooth)
                songSeekBar?.progress = 0
                songSeekBar?.isEnabled = false

                PicassoTrustCertificates.getInstance(context).load(R.mipmap.album_art)
                        .memoryPolicy(MemoryPolicy.NO_STORE)
                        .placeholder(R.mipmap.album_art)
                        .into(albumArtView)

                val mNode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(sceneObject.ipAddress)
                        ?: return
                LibreLogger.d(this, "BT controller value in sceneobject " + mNode.bT_CONTROLLER)
                if (mNode.bT_CONTROLLER != 1 && mNode.bT_CONTROLLER != 2 && mNode.bT_CONTROLLER != 3) {
                    playPauseView?.setImageResource(R.drawable.play_white)
                }

            }
            Constants.GCAST_SOURCE -> {
                //gCast is Playing
                playPauseView?.setImageResource(R.drawable.play_orange)
                trackNameView?.text = "Casting"

                songSeekBar?.progress = 0
                songSeekBar?.isEnabled = false
            }

            Constants.ALEXA_SOURCE, Constants.DMR_SOURCE,Constants.DMP_SOURCE -> {
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
            }
        }

        if (sceneObject != null && (sceneObject.currentSource == Constants.VTUNER_SOURCE
                        || sceneObject.currentSource == Constants.TUNEIN_SOURCE
                        || sceneObject.currentSource == Constants.BT_SOURCE
                        || sceneObject.currentSource == Constants.AUX_SOURCE
                        || sceneObject.currentSource == Constants.NO_SOURCE
                        || sceneObject.currentSource == Constants.GCAST_SOURCE)) {
            playPauseView?.isEnabled = false
            playPauseView?.setImageResource(R.drawable.play_white)
            songSeekBar?.progress = 0
            songSeekBar?.secondaryProgress = 0
            songSeekBar?.max = 100
            songSeekBar?.isEnabled = false
        }

        if (!playPauseView?.isEnabled!!) {
            playPauseView?.setImageResource(R.drawable.play_white)
        } else if (sceneObject?.playstatus == SceneObject.CURRENTLY_PLAYING) {
            playPauseView?.setImageResource(R.drawable.pause_orange)
        } else {
            playPauseView?.setImageResource(R.drawable.play_orange)
        }
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
        val luciPacket = LUCIPacket(nettyData.message)
        val msg = String(luciPacket.payload)
        val sceneObject = ScanningHandler.getInstance().getSceneObjectFromCentralRepo(nettyData.remotedeviceIp)
        if (sceneObject == null) {
            Log.e("MessageForMusicPlayer", "sceneObject null for " + nettyData.remotedeviceIp)
            return
        }

        when(luciPacket.command){
            MIDCONST.GET_UI -> {
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
        }

    }

    private fun handlePlayJsonUi(window: JSONObject, sceneObject: SceneObject?) {
        val updatedSceneObject = AppUtils.updateSceneObjectWithPlayJsonWindow(window, sceneObject!!)
        ScanningHandler.getInstance().putSceneObjectToCentralRepo(sceneObject.ipAddress, updatedSceneObject)
        updateMusicPlayViews(sceneObject)
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
            next.setImageResource(R.mipmap.prev_with_glow)
        } else {
            next.setImageResource(R.mipmap.prev_without_glow)
        }

        previous.isEnabled = controlsArr[2]
        previous.isClickable = controlsArr[2]
        if (controlsArr[2]) {
            next.setImageResource(R.mipmap.next_with_glow)
        } else {
            next.setImageResource(R.mipmap.next_without_glow)
        }
    }

    private fun handleTheGoogleCastMessages(nettyData: NettyData) {

        val packet = LUCIPacket(nettyData.getMessage())
        val dummyPacket = LUCIPacket(nettyData.getMessage())

        if (packet.command == MIDCONST.GCAST_MANUAL_UPGRADE.toInt()) {
            var msg = String(packet.getpayload())
            val mLssdpNodeDb = LSSDPNodeDB.getInstance()
            val mNode = mLssdpNodeDb.getTheNodeBasedOnTheIpAddress(nettyData.getRemotedeviceIp())
            /* if I am Getting Manual Upgrade for Sacc DEvice we should Discard it */
            if (mNode != null && mNode.friendlyname.equals(LibreApplication.sacDeviceNameSetFromTheApp, ignoreCase = true))
                return
            /* if NO Update is coming for Any Device We are Just Showing the Dialog as NO Update Available  */
            if (msg.equals(Constants.GCAST_NO_UPDATE, ignoreCase = true)) {
                showAlertDialogMessageForGCastMsgBoxes("No Update Available", mNode)
            } else if (msg.equals(Constants.GCAST_UPDATE_STARTED, ignoreCase = true)) {
                /* if Update is Started For any of Other Device We are creating a pbject and putting it in Hashmap  */
                msg = applicationContext.getString(R.string.downloadingtheFirmare)
                val mGcastData = GcastUpdateData(
                        mNode!!.ip,
                        mNode.friendlyname,
                        msg,
                        0
                )
                LibreApplication.GCAST_UPDATE_AVAILABE_LIST_DATA[mNode.ip] = mGcastData
                showAlertDialogForGcastUpdate(mNode, mGcastData)
            }

        }
        if (packet.command == MIDCONST.GCAST_PROGRESS_STATUS.toInt()) { // 66 Message Box

            val msg = String(packet.getpayload())
            LibreLogger.d(this, "GCAST_PROGRESS_STATUS " +
                    nettyData.getRemotedeviceIp() +
                    "Message " +
                    msg + " Command " + dummyPacket.command)

            val mLssdpNodeDb = LSSDPNodeDB.getInstance()
            val mNode = mLssdpNodeDb.getTheNodeBasedOnTheIpAddress(nettyData.getRemotedeviceIp())
            var mGcastData: GcastUpdateData? = LibreApplication.GCAST_UPDATE_AVAILABE_LIST_DATA[nettyData.getRemotedeviceIp()]
            if (mNode == null)
                return
            var mNewData = false
            if (mGcastData == null) {
                mGcastData = GcastUpdateData(
                        mNode.ip,
                        mNode.friendlyname,
                        "",
                        0
                )
                mNewData = true
                LibreApplication.GCAST_UPDATE_AVAILABE_LIST_DATA[mNode.ip] = mGcastData
            }

            /*if Gcast COmplete State or For SAC Device then we are showing Device will reboot
             * because after firmwareupgraade COmpleted we are giving ok and coming back to PlayNEwScreen */
            if (msg.equals(Constants.GCAST_COMPLETE, ignoreCase = true)
                    && mNode != null &&
                    !mNode.friendlyname.equals(LibreApplication.sacDeviceNameSetFromTheApp, ignoreCase = true)) {
                mGcastData.setmProgressValue(100)
                mGcastData.setmGcastUpdate(applicationContext.getString(R.string.gcast_update_done))
                showAlertDialogMessageForGCastMsgBoxes("Firmware Update Completed For ", mNode, "," + applicationContext.getString(R.string.deviceRebooting))
                return
            }

            /*if Gcast FaileState  back to PlayNEwScreen */
            if (msg.equals("255", ignoreCase = true)
                    && mNode != null &&
                    !mNode.friendlyname.equals(LibreApplication.sacDeviceNameSetFromTheApp, ignoreCase = true)) {
                showAlertDialogMessageForGCastMsgBoxes("Firmware Update Failed For ", mNode)
                mGcastData.setmGcastUpdate(applicationContext.getString(R.string.gcastFailed))
                return
            }

            try {
                mGcastData.setmProgressValue(Integer.valueOf(msg))
            } catch (e: Exception) {
                e.printStackTrace()

            }

            if (mNewData)
                showAlertDialogForGcastUpdate(mNode, mGcastData)


        }

        /**for GCAST internet upgrade */
        if (packet.command == MIDCONST.GCAST_UPDATE_MSGBOX.toInt()) {

            val msg = String(packet.getpayload())
            LibreLogger.d(this, "GCAST_UPDATE_MSGBOX " + nettyData.getRemotedeviceIp() +
                    "Message " +
                    msg + " Command " + dummyPacket.command)

            val mLssdpNodeDb = LSSDPNodeDB.getInstance()
            val mNode = mLssdpNodeDb.getTheNodeBasedOnTheIpAddress(nettyData.getRemotedeviceIp())
            if (mNode != null && !mNode.friendlyname.equals(LibreApplication.sacDeviceNameSetFromTheApp, ignoreCase = true)) {
                if (msg.equals(Constants.GCAST_NO_UPDATE, ignoreCase = true)) {
                    return
                }
                var mGcastData: GcastUpdateData? = LibreApplication.GCAST_UPDATE_AVAILABE_LIST_DATA[nettyData.getRemotedeviceIp()]
                if (mNode == null)
                    return
                var mNewData = false
                if (mGcastData == null) {
                    mGcastData = GcastUpdateData(
                            mNode.ip,
                            mNode.friendlyname,
                            "",
                            0
                    )
                    mNewData = true
                    LibreApplication.GCAST_UPDATE_AVAILABE_LIST_DATA[mNode.ip] = mGcastData
                }
                if (msg.equals(Constants.GCAST_UPDATE_STARTED, ignoreCase = true)) {
                    mGcastData.setmGcastUpdate(applicationContext.getString(R.string.downloadingtheFirmare))
                } else if (msg.equals(Constants.GCAST_NO_UPDATE, ignoreCase = true)) {
                    mGcastData.setmGcastUpdate(applicationContext.getString(R.string.noupdateAvailable))
                } else if (msg.equals(Constants.GCAST_UPDATE_IMAGE_AVAILABLE, ignoreCase = true)) {
                    mGcastData.setmGcastUpdate(applicationContext.getString(R.string.upgrading))
                } else {
                    try {
                        val mProgressValue = Integer.valueOf(msg)
                        mGcastData.setmGcastUpdate(applicationContext.getString(R.string.downloadingtheFirmare))
                        mGcastData.setmProgressValue(mProgressValue)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }
                if (mNewData)
                    showAlertDialogForGcastUpdate(mNode, mGcastData)
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
        val newIntent: Intent
        if (LSSDPNodeDB.getInstance().GetDB().size > 0) {
            //            newIntent = new Intent(context, ActiveScenesListActivity.class);
            newIntent = Intent(context, CTHomeTabsActivity::class.java)
            newIntent.putExtra(AppConstants.LOAD_FRAGMENT, CTActiveDevicesFragment::class.java.simpleName)
            newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(newIntent)
            finish()
        } else {
            //            newIntent = new Intent(context, ConfigureActivity.class);
            newIntent = Intent(context, CTHomeTabsActivity::class.java)
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
            restartApp(this@CTDeviceDiscoveryActivity)
            LibreApplication.mCleanUpIsDoneButNotRestarted = false
        }

        val `in` = Intent(this@CTDeviceDiscoveryActivity, DMRDeviceListenerForegroundService::class.java)
        `in`.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        `in`.action = Constants.ACTION.STARTFOREGROUND_ACTION
        startService(`in`)

        val connManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)

        if (mWifi != null && mWifi.isConnected && MicTcpServer.MIC_TCP_SERVER_PORT == 0
                && getConnectedSSIDName(this) != null
                && !getConnectedSSIDName(this)!!.contains(Constants.RIVAA_WAC_SSID)) {
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
            if (!wifiUtil.isWifiEnabled() && isNetworkOffCallBackEnabled) {
                if (activeSSID != null && activeSSID != "") {
                    LibreApplication.mActiveSSIDBeforeWifiOff = activeSSID
                }
                activeSSID = ""
                //                showWifiNetworkOffAlert();
                wifiConnected(false)
                return
            }

            if (!isNetworkChangesCallBackEnabled) {
                return
            }

            if (intent.action != "android.net.conn.TETHER_STATE_CHANGED" && intent.action != ConnectivityManager.CONNECTIVITY_ACTION)
                return

            if (intent.action != null && intent.action == ConnectivityManager.CONNECTIVITY_ACTION) {
                val networkInfo = intent.getParcelableExtra<NetworkInfo>(ConnectivityManager.EXTRA_NETWORK_INFO)

                Log.e("onReceive", "type = " + networkInfo.typeName + ", isConnected = " + networkInfo.isConnected)

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
                                && activeSSID != null && !activeSSID.isEmpty()
                                && activeSSID != connectedSSID
                                && context.javaClass.simpleName != CTConnectToWifiActivity::class.java.simpleName
                                && !connectedSSID!!.contains(Constants.RIVAA_WAC_SSID)) {
                            showNetworkChangeRestartAppAlert()
                        } else
                            wifiConnected(true)
                    }

                } else if (networkInfo.type == ConnectivityManager.TYPE_MOBILE && networkInfo.isConnected) {
                    Log.e("onReceive", "TYPE_MOBILE, isConnected = " + networkInfo.isConnected)
                }
            }
        }
    }//m_nwStateListener = nwStateListener;

    open fun wifiConnected(connected: Boolean) {}

    fun showNetworkChangeRestartAppAlert() {
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
        if (!this@CTDeviceDiscoveryActivity.isFinishing) {

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

                        LibreLogger.d(this, "Handling the exception while sending the stop command ")
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

            LibreApplication.PLAYBACK_HELPER_MAP.clear()
            application.getScanThread().clearNodes()
            application.getScanThread().mRunning = false
            application.getScanThread().close()
            application.getScanThread().mAliveNotifyListenerSocket = null
            application.getScanThread().mDatagramSocketForSendingMSearch = null
            ScanningHandler.getInstance().clearSceneObjectsFromCentralRepo()

            if (mCompleteCleanup) {
                this.libreDeviceInteractionListner = null
                try {
                    BusProvider.getInstance().unregister(busEventListener)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }

            if (application.getScanThread().nettyServer.mServerChannel != null && application.getScanThread().nettyServer.mServerChannel.isBound) {
                application.getScanThread().nettyServer.mServerChannel.unbind()
                val serverClose = application.getScanThread().nettyServer.mServerChannel.close()
                serverClose.awaitUninterruptibly()

                /*to resolve hang issue*/
                //                application.getScanThread().nettyServer.serverBootstrap.releaseExternalResources();
            }

            application.closeScanThread()
            LUCIControl.luciSocketMap.clear()
            LUCIControl.channelHandlerContextMap.clear()
            LSSDPNodeDB.getInstance().clearDB()
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

    /* Restarting Alll my Sockets whenever Configuring a SAC Device to SAME AP .,
     * * Till i HAave to Analyse more on this code */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun restartAllSockets(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            LibreLogger.d(this, "Karuna 2" + "App Called Here Device 1")
            val application = application as LibreApplication
            application.initiateServices()
        }
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
    }


    private fun checkForTheSACDeviceSuccessDialog(node: LSSDPNodes?) {

        val sharedPreferences = applicationContext
                .getSharedPreferences("sac_configured", Context.MODE_PRIVATE)
        val str = sharedPreferences.getString("deviceFriendlyName", "")
        if (str != null && str.equals(node!!.friendlyname.toString(), ignoreCase = true) && node.getgCastVerision() != null) {


            LibreApplication.thisSACDeviceNeeds226 = node.friendlyname
            if (LibreApplication.GCAST_UPDATE_AVAILABE_LIST_DATA.containsKey(node.ip))
                LibreApplication.GCAST_UPDATE_AVAILABE_LIST_DATA.remove(node.ip)

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

    private fun showAlertDialogMessageForGCastMsgBoxes(Message: String, node: LSSDPNodes?) {
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
    private fun showAlertDialogMessageForGCastMsgBoxes(Message: String, node: LSSDPNodes, Message2: String) {
        if (!LibreApplication.sacDeviceNameSetFromTheApp.equals("", ignoreCase = true))
            return

        val alertDialogBuilder = AlertDialog.Builder(
                this@CTDeviceDiscoveryActivity)

        // set title
        alertDialogBuilder.setTitle("Firmware Upgrade")

        // set dialog message
        alertDialogBuilder
                .setMessage(Message + node.friendlyname.toString() + Message2)
                .setCancelable(false)
                .setPositiveButton("OK") { dialog, id -> sAcalertDialog = null }
        if (sAcalertDialog == null)
            sAcalertDialog = alertDialogBuilder.create()


        sAcalertDialog!!.show()
    }

    private fun showAlertDialogForGcastUpdate(mNode: LSSDPNodes, mGcastData: GcastUpdateData) {
        if (!LibreApplication.sacDeviceNameSetFromTheApp.equals("", ignoreCase = true))
            return
        val builder = AlertDialog.Builder(this@CTDeviceDiscoveryActivity)
        // set title
        builder.setTitle("Firmware Upgrade")
        builder.setMessage(mNode.friendlyname + " New Update is available. firmware upgrade in progress with New Update")
                .setCancelable(false)
                .setPositiveButton("OK") { dialog, id ->
                    dialog.dismiss()
                    gCastUpdateAlertDialog = null
                    /**which means you have got
                     * for current master and navigate to ActiveScene */
                    /**which means you have got
                     * for current master and navigate to ActiveScene */
                    BusProvider.getInstance().post(mGcastData)
                }
        // create alertDialog1 dialog

        if (gCastUpdateAlertDialog == null)
            gCastUpdateAlertDialog = builder.create()
        if (!gCastUpdateAlertDialog?.isShowing!!)
            gCastUpdateAlertDialog!!.show()
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

            luciPackets.add(currentPlayStatePacket)
            luciPackets.add(mSceneNamePacket)
            luciPackets.add(volumePacket)
            luciPackets.add(getUIPacket)
            luciPackets.add(currentSourcePacket)

            luciControl.SendCommand(luciPackets)

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