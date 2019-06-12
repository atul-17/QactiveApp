package com.cumulations.libreV2.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.SeekBar
import android.widget.Toast

import com.cumulations.libreV2.AppConstants
import com.cumulations.libreV2.AppUtils
import com.cumulations.libreV2.BlurTransformation
import com.libre.LErrorHandeling.LibreError
import com.libre.LibreApplication
import com.libre.R
import com.libre.Scanning.Constants
import com.libre.Scanning.Constants.*
import com.libre.Scanning.ScanningHandler
import com.libre.SceneObject
import com.libre.app.dlna.dmc.processor.interfaces.DMRProcessor
import com.libre.app.dlna.dmc.utility.UpnpDeviceManager
import com.libre.constants.CommandType
import com.libre.constants.LSSDPCONST
import com.libre.constants.LUCIMESSAGES
import com.libre.constants.MIDCONST
import com.libre.luci.LSSDPNodeDB
import com.libre.luci.LSSDPNodes
import com.libre.luci.LUCIControl
import com.libre.luci.LUCIPacket
import com.libre.netty.BusProvider
import com.libre.netty.LibreDeviceInteractionListner
import com.libre.netty.NettyData
import com.libre.nowplaying.NowPlayingActivity
import com.libre.util.LibreLogger
import com.libre.util.PicassoTrustCertificates
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.ct_acitvity_now_playing.*
import kotlinx.android.synthetic.main.ct_acitvity_now_playing.iv_album_art
import kotlinx.android.synthetic.main.ct_acitvity_now_playing.iv_play_pause
import kotlinx.android.synthetic.main.ct_acitvity_now_playing.seek_bar_song
import kotlinx.android.synthetic.main.ct_acitvity_now_playing.tv_album_name
import kotlinx.android.synthetic.main.ct_acitvity_now_playing.tv_track_name
import kotlinx.android.synthetic.main.ct_alexa_widget.*

import org.fourthline.cling.model.ModelUtil
import org.fourthline.cling.model.message.UpnpResponse
import org.fourthline.cling.model.meta.Action
import org.json.JSONObject

/**
 * Created by AMit on 13/5/2019.
 */

class CTNowPlayingActivity : CTDeviceDiscoveryActivity(), View.OnClickListener,
        LibreDeviceInteractionListner, DMRProcessor.DMRProcessorListener {

    companion object {
        const val REPEAT_OFF = 0
        const val REPEAT_ONE = 1
        const val REPEAT_ALL = 2
        const val PLAYBACK_TIMER_TIMEOUT = 5000
    }

    private var currentSceneObject: SceneObject? = null
    private val currentIpAddress: String? by lazy {
        intent?.getStringExtra(CURRENT_DEVICE_IP)
    }
    private var isLocalDMRPlayback = false

    /* Added to handle the case where trackname is empty string"*/
    private var currentTrackName = "-1"
    private var isStillPlaying = false
    private var is49MsgBoxReceived = false

    private var mScanHandler: ScanningHandler? = null

    private val startPlaybackTimerhandler = Handler()
    private val startPlaybackTimerRunnable = object : Runnable {
        override fun run() {
            if (isStillPlaying) {
                if (is49MsgBoxReceived) {
                    // Do nothing, set is49MsgBoxReceived to false so that for next 49 msg box received
                    // this value gets updated
                    is49MsgBoxReceived = false
                    /*this is hack to check again if is49MsgBoxReceived is still false after making it false
                     * which will confirm no 49 msg box received after total 10 seconds*/
                    startPlaybackTimerhandler.postDelayed(this, PLAYBACK_TIMER_TIMEOUT.toLong())
                } else {

                    if (!isFinishing) {
                        disablePlayback()
                    }
                    /*Hack : send any luci command to check device still alive or not when user pauses and stays on same
                     * screen and does nothing*/
                    LUCIControl(currentIpAddress).SendCommand(MIDCONST.MID_CURRENT_SOURCE.toInt(), null, LSSDPCONST.LUCI_GET)
                }
            }
        }
    }

    @SuppressLint("HandlerLeak")
    internal var showLoaderHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {

            when (msg.what) {
                PREPARATION_INITIATED -> {
                    showLoader(true)
                    /*disabling views while initiating */
                    preparingToPlay(false)
                }

                PREPARATION_COMPLETED -> {
                    showLoader(false)
                    /*enabling views while initiating */
                    preparingToPlay(true)
                }

                PREPARATION_TIMEOUT_CONST -> {
                    preparingToPlay(true)
                    showLoader(false)
                }

                ALEXA_NEXT_PREV_HANDLER -> dismissDialog()
            }
        }

    }

    private fun disablePlayback() {
        /*make player status to paused, ie show play icon*/
        iv_play_pause!!.setImageResource(R.drawable.play_white)
        iv_next!!.setImageResource(R.drawable.next_disabled)
        iv_previous!!.setImageResource(R.drawable.prev_disabled)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ct_acitvity_now_playing)
        mScanHandler = ScanningHandler.getInstance()
    }

    override fun onStart() {
        super.onStart()
        initViews()
        if (currentIpAddress != null) {
            currentSceneObject = ScanningHandler.getInstance().getSceneObjectFromCentralRepo(currentIpAddress) // ActiveSceneAdapter.mMasterSpecificSlaveAndFreeDeviceMap.get(currentIpAddress);
            if (currentSceneObject == null) {
                Log.d("NetworkChanged", "NowPlayingFragment onCreateView")
                showToast(R.string.deviceNotFound)
            } else if (currentSceneObject!!.currentSource == DMR_SOURCE) {
                /* Check if the playback is through DMR and it is */
                isLocalDMRPlayback = true
                setViews()
            }
        }
    }

    private fun gotoSourcesOption(ipaddress: String?, currentSource: Int) {
        val error = LibreError("", resources.getString(R.string.no_active_playlist), 1)
        BusProvider.getInstance().post(error)
        startActivity(Intent(this, CTMediaSourcesActivity::class.java).apply {
            putExtra(CURRENT_DEVICE_IP, ipaddress)
            putExtra(CURRENT_SOURCE, "" + currentSource)
            putExtra(FROM_ACTIVITY, CTNowPlayingActivity::class.java.simpleName)
        })
    }

    private fun isLocalDMR(sceneObject: SceneObject): Boolean {
        val renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(sceneObject.ipAddress)
        if (renderingDevice != null) {
            val renderingUDN = renderingDevice.identity.udn.toString()
            val playbackHelper = LibreApplication.PLAYBACK_HELPER_MAP[renderingUDN]
            val mIsDmrPlaying = ScanningHandler.getInstance().ToBeNeededToLaunchSourceScreen(sceneObject)
            LibreLogger.d(this, " local DMR is playing$mIsDmrPlaying")
            return !mIsDmrPlaying
        }
        return false
    }

    private fun isActivePlayListNotAvailable(sceneObject: SceneObject?): Boolean {
        return sceneObject != null
                && sceneObject.currentSource == NO_SOURCE
                || (sceneObject!!.currentSource == DMR_SOURCE
                && ScanningHandler.getInstance().ToBeNeededToLaunchSourceScreen(sceneObject))
    }

    /*initialize variables here*/
    private fun initViews() {
        showLoader(false)

        iv_play_pause!!.setOnClickListener(this)
        iv_previous!!.setOnClickListener(this)
        iv_next!!.setOnClickListener(this)
        iv_shuffle!!.setOnClickListener(this)
        iv_repeat!!.setOnClickListener(this)
        iv_back?.setOnClickListener {
            onBackPressed()
        }

        iv_alexa_account?.setOnClickListener {
            val mNode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(currentIpAddress)
            if (mNode?.alexaRefreshToken?.isEmpty()!!) {
                startActivity(Intent(this@CTNowPlayingActivity, CTAmazonLoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(CURRENT_DEVICE_IP, currentIpAddress)
                    putExtra(FROM_ACTIVITY, CTNowPlayingActivity::class.java.simpleName)
                })
            } else {
                startActivity(Intent(this@CTNowPlayingActivity, CTAlexaThingsToTryActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(CURRENT_DEVICE_IP, currentIpAddress)
                    putExtra(FROM_ACTIVITY, CTNowPlayingActivity::class.java.simpleName)
                })
            }
        }

        seek_bar_volume!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {

                if (!doVolumeChange(seekBar.progress))
                    showToast(R.string.actionFailed)
            }
        })

        seek_bar_song!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                LibreLogger.d(this, "Bhargav SEEK:Seekbar Position track " + seekBar.progress + "  " + seekBar.max)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                LibreLogger.d(this, " Bhargav SEEK:Seekbar Position trackstop" + seekBar.progress + "  " + seekBar.max)
                val theRenderer = getTheRenderer(currentIpAddress)
                if (isLocalDMRPlayback && theRenderer == null) {
                    showToast(R.string.NoRenderer)
                } else if (!doSeek(seekBar.progress)) {
                    if (currentSceneObject != null) {
                        val error = LibreError(currentSceneObject!!.ipAddress, resources.getString(R.string.RENDERER_NOT_PRESENT))
                        BusProvider.getInstance().post(error)
                    }
                }

            }
        })

        setMusicPlayerWidget(fl_alexa_widget,currentIpAddress!!)
    }

    private fun showLoader(show: Boolean) {
        if (show)
            loader!!.visibility = View.VISIBLE
        else
            loader!!.visibility = View.GONE
    }

    /*this method will take care of views while we are preparing to play*/
    private fun preparingToPlay(value: Boolean) {

        if (value) {
            setTheSourceIconFromCurrentSceneObject()
        } else {
            enableViews(value)
        }
    }

    private fun disableViews(currentSrc: Int, message: String?) {

        /* Dont have to call this function explicitly make use of setrceIco */

        when (currentSrc) {

            NO_SOURCE -> {
                /*setting seek to zero*/
                seek_bar_song!!.progress = 0
                seek_bar_song!!.isEnabled = false
                seek_bar_song!!.isClickable = false
                tv_current_duration!!.text = "0:00"
                tv_total_duration!!.text = "0:00"

                iv_play_pause!!.isClickable = false
                iv_play_pause!!.isEnabled = false

//                artistName!!.text = ""
                tv_album_name!!.text = ""
                tv_track_name!!.text = ""
                LibreLogger.d(this, "Libre - return from WiFi mode through hard key")
            }

            DDMSSLAVE_SOURCE -> {
                /*setting seek to zero*/
                seek_bar_song!!.progress = 0
                seek_bar_song!!.isEnabled = false
                seek_bar_song!!.isClickable = false
                tv_current_duration!!.text = "0:00"
                tv_total_duration!!.text = "0:00"
                iv_play_pause!!.isClickable = false
                iv_play_pause!!.isEnabled = false
//                artistName!!.text = ""
                tv_album_name!!.text = ""
                tv_track_name!!.text = ""
                LibreLogger.d(this, "Gear4 - return from WiFi mode through hard key")
            }

            AUX_SOURCE ->
                //Aux
            {       /*setting seek to zero*/
                seek_bar_song!!.progress = 0
                seek_bar_song!!.isEnabled = false
                seek_bar_song!!.isClickable = false
                tv_current_duration!!.text = "0:00"
                tv_total_duration!!.text = "0:00"
                iv_play_pause!!.setImageResource(R.drawable.play_white)
                iv_next!!.setImageResource(R.drawable.next_disabled)
                iv_previous!!.setImageResource(R.drawable.prev_disabled)
//                artistName!!.text = message
                tv_album_name!!.text = ""
                tv_track_name!!.text = ""
                LibreLogger.d(this, "we set the song name to empty for disabling " + currentSceneObject!!.trackName + " in disabling view where artist name is " + message)

            }

            BT_SOURCE ->
                //Bluetooth
            {
                /*setting seek to zero*/
                seek_bar_song!!.progress = 0
                seek_bar_song!!.isEnabled = false
                seek_bar_song!!.isClickable = false
                tv_current_duration!!.text = "0:00"
                tv_total_duration!!.text = "0:00"
//                artistName!!.text = message
                tv_album_name!!.text = ""
                tv_track_name!!.text = ""
                // hide repeat and shuffle
                iv_repeat!!.visibility = View.GONE
                iv_shuffle!!.visibility = View.GONE
                val mNode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(currentIpAddress)
                        ?: return
                LibreLogger.d(this, "BT controller value in sceneobject " + mNode.bT_CONTROLLER)
                //    Toast.makeText(getContext(), "BT controller value in sceneobject " + currentSceneObject.getBT_CONTROLLER(), Toast.LENGTH_SHORT);
                if (mNode.bT_CONTROLLER == SceneObject.CURRENTLY_STOPPED || mNode.bT_CONTROLLER == SceneObject.CURRENTLY_PAUSED || mNode.bT_CONTROLLER == 3) {
                    seek_bar_volume!!.isEnabled = true
                    seek_bar_volume!!.isClickable = true
                } else {
                    iv_play_pause!!.setImageResource(R.drawable.play_white)
                    iv_next!!.setImageResource(R.drawable.next_disabled)
                    iv_previous!!.setImageResource(R.drawable.prev_disabled)
                }

            }

            GCAST_SOURCE -> {
                //gCast is Playing

                /*setting seek to zero*/
                seek_bar_song!!.progress = 0
                seek_bar_song!!.isEnabled = false
                seek_bar_song!!.isClickable = false
                tv_current_duration!!.text = "0:00"
                tv_total_duration!!.text = "0:00"
                iv_play_pause!!.setImageResource(R.drawable.play_white)
                iv_next!!.setImageResource(R.drawable.next_disabled)
                iv_previous!!.setImageResource(R.drawable.prev_disabled)

                seek_bar_volume!!.progress = 0
                seek_bar_volume!!.isClickable = false
                seek_bar_volume!!.isEnabled = false
//                artistName!!.text = "Casting"
                tv_album_name!!.text = ""
                tv_track_name!!.text = ""
            }


            DEEZER_SOURCE -> {

                if (message != null && message.contains(DEZER_RADIO)) {
                    iv_previous!!.isEnabled = false
                    iv_previous!!.isClickable = false
                }
            }

            ALEXA_SOURCE -> {
                seek_bar_song!!.isClickable = false
                iv_play_pause!!.setImageResource(R.drawable.play_orange)
                iv_next!!.setImageResource(R.drawable.next_enabled)
                iv_previous!!.setImageResource(R.drawable.prev_enabled)
                if (currentSceneObject != null && !currentSceneObject?.genre.isNullOrEmpty() && !currentSceneObject!!.genre.equals("null", ignoreCase = true))
                    tv_album_name?.text = "${tv_album_name.text}, ${currentSceneObject?.genre}"
            }
        }


    }

    private fun enableViews(enable:Boolean) {
        seek_bar_song!!.isEnabled = enable
        seek_bar_song!!.isClickable = enable
        iv_play_pause!!.isClickable = enable
        iv_play_pause!!.isEnabled = enable
        iv_previous!!.isClickable = enable
        iv_previous!!.isEnabled = enable
        iv_next!!.isEnabled = enable
        iv_next!!.isClickable = enable
        seek_bar_volume!!.isEnabled = enable
        seek_bar_volume!!.isClickable = enable
    }

    private fun playPauseNextPrevAllowed(): Boolean {
        val mNodeWeGotForControl = mScanHandler!!.getLSSDPNodeFromCentralDB(currentIpAddress)
        return (currentSceneObject!!.currentSource != AUX_SOURCE
                && currentSceneObject!!.currentSource != GCAST_SOURCE
                && currentSceneObject!!.currentSource != VTUNER_SOURCE
                && currentSceneObject!!.currentSource != TUNEIN_SOURCE
                && currentSceneObject!!.currentSource != NO_SOURCE
                && (currentSceneObject!!.currentSource != BT_SOURCE
                || (mNodeWeGotForControl.getgCastVerision() != null
                && mNodeWeGotForControl.bT_CONTROLLER != SceneObject.CURRENTLY_NOTPLAYING
                && mNodeWeGotForControl.bT_CONTROLLER != SceneObject.CURRENTLY_PLAYING)
                || (mNodeWeGotForControl.getgCastVerision() == null
                && mNodeWeGotForControl.bT_CONTROLLER >= SceneObject.CURRENTLY_PAUSED)))
    }

    override fun onExceptionHappend(actionCallback: Action<*>, mTitle: String, cause: String) {
        LibreLogger.d(this, "Exception Happend for the Title $mTitle for the cause of $cause")
    }

    override fun onClick(view: View) {
        /*stopMediaServer any iv_previous started playback handler started by removing them*/
        startPlaybackTimerhandler.removeCallbacks(startPlaybackTimerRunnable)

        if (currentSceneObject == null) {
            showToast(R.string.Devicenotplaying)
        } else {
            val mNodeWeGotForControl = mScanHandler!!.getLSSDPNodeFromCentralDB(currentIpAddress)
                    ?: return
            isLocalDMRPlayback = currentSceneObject!!.currentSource == DMR_SOURCE

            when (view.id) {
                R.id.iv_play_pause -> {
                    if (!playPauseNextPrevAllowed()) {
                        val error = LibreError("", resources.getString(R.string.PLAY_PAUSE_NOT_ALLOWED), 1)
                        BusProvider.getInstance().post(error)
                        return
                    }
                    if (currentSceneObject!!.currentSource == NO_SOURCE || currentSceneObject!!.currentSource == DMR_SOURCE && (currentSceneObject!!.playstatus == SceneObject.CURRENTLY_STOPPED || currentSceneObject!!.playstatus == SceneObject.CURRENTLY_NOTPLAYING)) {
                        LibreLogger.d(this, "currently not playing, so take user to sources option activity")
                        gotoSourcesOption(currentSceneObject!!.ipAddress, currentSceneObject!!.currentSource)
                        return
                    }

                    val control = LUCIControl(currentSceneObject!!.ipAddress)
                    if (currentSceneObject!!.playstatus == SceneObject.CURRENTLY_PLAYING) {
                        control.SendCommand(MIDCONST.MID_PLAYCONTROL.toInt(), LUCIMESSAGES.PAUSE, CommandType.SET)
                        isStillPlaying = false
                        iv_play_pause!!.setImageResource(R.drawable.play_white)
                        iv_next!!.setImageResource(R.drawable.next_disabled)
                        iv_previous!!.setImageResource(R.drawable.prev_disabled)
                    } else {
                        isStillPlaying = true
                        if (currentSceneObject!!.currentSource == BT_SOURCE) {
                            control.SendCommand(MIDCONST.MID_PLAYCONTROL.toInt(), LUCIMESSAGES.PLAY, CommandType.SET)
                        } else {
                            control.SendCommand(MIDCONST.MID_PLAYCONTROL.toInt(), LUCIMESSAGES.RESUME, CommandType.SET)
                        }
                        iv_play_pause!!.setImageResource(R.drawable.pause_orange)
                        iv_next!!.setImageResource(R.drawable.next_enabled)
                        iv_previous!!.setImageResource(R.drawable.prev_enabled)

                    }

                    if (currentSceneObject!!.currentSource != BT_SOURCE
                            && currentSceneObject!!.currentSource != 15
                            && currentSceneObject!!.currentSource != GCAST_SOURCE
                            && currentSceneObject!!.currentSource != ALEXA_SOURCE) {
                        showLoaderHandler.sendEmptyMessageDelayed(PREPARATION_TIMEOUT_CONST, Constants.PREPARATION_TIMEOUT.toLong())
                        showLoaderHandler.sendEmptyMessage(PREPARATION_INITIATED)
                    }
                }

                R.id.iv_previous -> {
                    if (!playPauseNextPrevAllowed()) {
                        val error = LibreError("", resources.getString(R.string.NEXT_PREVIOUS_NOT_ALLOWED), 1)
                        BusProvider.getInstance().post(error)
                        return
                    }

                    if (isActivePlayListNotAvailable(currentSceneObject)) {
                        LibreLogger.d(this, "currently not playing, so take user to sources option activity")
                        gotoSourcesOption(currentIpAddress, currentSceneObject!!.currentSource)
                        return
                    }
                    if (iv_previous!!.isEnabled) {
                        doNextPrevious(false)
                    } else {
                        val error = LibreError(currentIpAddress, getString(R.string.requestTimeout))
                        BusProvider.getInstance().post(error)
                    }
                }

                R.id.iv_next -> {
                    LibreLogger.d(this, "bhargav1")
                    if (!playPauseNextPrevAllowed()) {
                        val error = LibreError("", resources.getString(R.string.NEXT_PREVIOUS_NOT_ALLOWED), 1)
                        BusProvider.getInstance().post(error)
                        return
                    }

                    if (isActivePlayListNotAvailable(currentSceneObject)) {
                        LibreLogger.d(this, "currently not playing, so take user to sources option activity")
                        gotoSourcesOption(currentIpAddress, currentSceneObject!!.currentSource)
                        return
                    }
                    if (iv_next!!.isEnabled) {
                        LibreLogger.d(this, "bhargav12")
                        doNextPrevious(true)
                    } else {
                        LibreLogger.d(this, "bhargav1else")
                        val error = LibreError(currentIpAddress, getString(R.string.requestTimeout))
                        BusProvider.getInstance().post(error)
                    }
                }

                R.id.iv_shuffle ->

                    /*this is added to support shuffle and repeat option for Local Content*/
                    if (isLocalDMRPlayback) {
                        val renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(currentSceneObject!!.ipAddress)
                        if (renderingDevice != null) {
                            val renderingUDN = renderingDevice.identity.udn.toString()
                            val playbackHelper = LibreApplication.PLAYBACK_HELPER_MAP[renderingUDN]
                            if (playbackHelper != null) {
                                if (currentSceneObject!!.shuffleState == 0) {
                                    /*which means shuffle is off hence making it on*/
                                    playbackHelper.setIsShuffleOn(true)
                                    currentSceneObject!!.shuffleState = 1
                                } else {
                                    /*which means shuffle is on hence making it off*/
                                    currentSceneObject!!.shuffleState = 0
                                    playbackHelper.setIsShuffleOn(false)
                                }
                                /*inserting to central repo*/
                                mScanHandler!!.putSceneObjectToCentralRepo(currentIpAddress, currentSceneObject)
                                setViews()
                            }
                        }
                    } else {
                        val luciControl = LUCIControl(currentSceneObject!!.ipAddress)
                        if (currentSceneObject!!.shuffleState == 0) {
                            /*which means shuffle is off*/
                            luciControl.SendCommand(MIDCONST.MID_PLAYCONTROL.toInt(), "SHUFFLE:ON", LSSDPCONST.LUCI_SET)
                        } else
                            luciControl.SendCommand(MIDCONST.MID_PLAYCONTROL.toInt(), "SHUFFLE:OFF", LSSDPCONST.LUCI_SET)
                    }


                R.id.iv_repeat ->
                    /*this is added to support shuffle and repeat option for Local Content*/
                    if (isLocalDMRPlayback) {
                        val renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(currentSceneObject!!.ipAddress)
                        if (renderingDevice != null) {
                            val renderingUDN = renderingDevice.identity.udn.toString()
                            val playbackHelper = LibreApplication.PLAYBACK_HELPER_MAP[renderingUDN]
                            if (playbackHelper != null) {
                                when {
                                    currentSceneObject!!.repeatState == REPEAT_ALL -> {
                                        playbackHelper.repeatState = REPEAT_OFF
                                        currentSceneObject!!.repeatState = REPEAT_OFF
                                    }
                                    currentSceneObject!!.repeatState == REPEAT_OFF -> {
                                        playbackHelper.repeatState = REPEAT_ONE
                                        currentSceneObject!!.repeatState = REPEAT_ONE
                                    }
                                    currentSceneObject!!.repeatState == REPEAT_ONE -> {
                                        playbackHelper.repeatState = REPEAT_ALL
                                        currentSceneObject!!.repeatState = REPEAT_ALL
                                    }

                                    /*inserting to central repo*/
                                }
                                /*inserting to central repo*/
                                mScanHandler!!.putSceneObjectToCentralRepo(currentIpAddress, currentSceneObject)
                                setViews()
                            }
                        }
                    } else {
                        /**/
                        val shuffleLuciControl = LUCIControl(currentSceneObject!!.ipAddress)
                        when {
                            currentSceneObject!!.repeatState == REPEAT_ALL -> {
                                shuffleLuciControl.SendCommand(MIDCONST.MID_PLAYCONTROL.toInt(), "REPEAT:OFF", LSSDPCONST.LUCI_SET)
                                iv_repeat!!.setImageResource(R.drawable.repeat_disabled)
                                currentSceneObject!!.repeatState = REPEAT_OFF
                            }
                            currentSceneObject!!.repeatState == REPEAT_OFF && currentSceneObject!!.currentSource != SPOTIFY_SOURCE -> {
                                shuffleLuciControl.SendCommand(MIDCONST.MID_PLAYCONTROL.toInt(), "REPEAT:ONE", LSSDPCONST.LUCI_SET)
                                currentSceneObject!!.repeatState = REPEAT_ONE
                            }
                            currentSceneObject!!.repeatState == REPEAT_OFF && currentSceneObject!!.currentSource == SPOTIFY_SOURCE -> {
                                shuffleLuciControl.SendCommand(MIDCONST.MID_PLAYCONTROL.toInt(), "REPEAT:ALL", LSSDPCONST.LUCI_SET)
                                currentSceneObject!!.repeatState = REPEAT_ALL
                            }
                            currentSceneObject!!.repeatState == REPEAT_ONE -> {
                                shuffleLuciControl.SendCommand(MIDCONST.MID_PLAYCONTROL.toInt(), "REPEAT:ALL", LSSDPCONST.LUCI_SET)
                                currentSceneObject!!.repeatState = REPEAT_ALL
                            }
                            /* If the current source is spotify  *///Not equal to spotify
                        }/* If the current source is spotify  *///Not equal to spotify
                        setViews()
                    }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerForDeviceEvents(this)
        if (currentSceneObject == null)
            return
        isLocalDMRPlayback = false
        requestLuciUpdates(currentIpAddress!!)
        setViews()
        /* This is done to make sure we retain the album
           art even when the fragment gets recycled in viewpager */
//        updateAlbumArt()
    }

    override fun onStop() {
        super.onStop()
        unRegisterForDeviceEvents()
        showLoaderHandler.removeCallbacksAndMessages(null)
        startPlaybackTimerhandler.removeCallbacksAndMessages(null)
    }

    override fun deviceDiscoveryAfterClearingTheCacheStarted() {

    }

    override fun newDeviceFound(node: LSSDPNodes) {

    }

    override fun deviceGotRemoved(mIpAddress: String) {

    }

    override fun messageRecieved(nettyData: NettyData) {
        LibreLogger.d(this, "Nowplaying: New message appeared for the device " + nettyData.getRemotedeviceIp())
        if (currentSceneObject != null && nettyData.getRemotedeviceIp().equals(currentIpAddress!!, ignoreCase = true)) {
            val packet = LUCIPacket(nettyData.getMessage())
            LibreLogger.d(this, "Packet is _" + packet.command)

            when (packet.command) {
                MIDCONST.MID_PLAYTIME.toInt() -> {
                    //                This message box indicates the current playing status of the scene, information like current seek position*//*
                    val message = String(packet.getpayload())
                    if (message.isNotEmpty()) {
                        val longDuration = java.lang.Long.parseLong(message)

                        currentSceneObject!!.currentPlaybackSeekPosition = longDuration.toFloat()

                        /* Setting the current seekbar progress -Start*/
                        val duration = currentSceneObject!!.currentPlaybackSeekPosition
                        seek_bar_song!!.max = currentSceneObject!!.totalTimeOfTheTrack.toInt() / 1000
                        seek_bar_song!!.secondaryProgress = currentSceneObject!!.totalTimeOfTheTrack.toInt() / 1000
                        Log.d("SEEK", "Duration = " + duration / 1000)
                        seek_bar_song!!.progress = duration.toInt() / 1000

                        /* Setting the current seekbar progress -END*/
                        setTheSourceIconFromCurrentSceneObject()
                        tv_current_duration!!.text = convertMillisToSongTime((duration / 1000).toLong())
                        tv_total_duration!!.text = convertMillisToSongTime(currentSceneObject!!.totalTimeOfTheTrack / 1000)

                        Log.d("Bhargav SEEK", "Duration = " + duration / 1000)
                        Log.d("SEEK", "Duration = " + duration / 1000)

                        is49MsgBoxReceived = true
                        if (isStillPlaying) {
                            /*For now we check this only in NowPlaying screen, hence add and remove handler only when fragment is active*/
                            if (!isFinishing) {
                                /*stopMediaServer any iv_previous handler started by removing them*/
                                startPlaybackTimerhandler.removeCallbacks(startPlaybackTimerRunnable)
                                /*start fresh handler as timer to montior device got removed or 49 not recieved after 5 seconds*/
                                startPlaybackTimerhandler.postDelayed(startPlaybackTimerRunnable, PLAYBACK_TIMER_TIMEOUT.toLong())
                            }
                        }

                        if (mScanHandler!!.isIpAvailableInCentralSceneRepo(currentIpAddress)) {
                            mScanHandler!!.putSceneObjectToCentralRepo(currentIpAddress, currentSceneObject)
                        }
                        LibreLogger.d(this, "Nowplaying: Recieved the current Seek position to be " + currentSceneObject!!.currentPlaybackSeekPosition.toInt())
                    }
                }

                MIDCONST.MID_CURRENT_SOURCE.toInt() -> {

                    val message = String(packet.getpayload())
                    try {
                        val duration = Integer.parseInt(message)
                        currentSceneObject!!.currentSource = duration
                        setTheSourceIconFromCurrentSceneObject()
                        if (currentSceneObject!!.currentSource == 14 || currentSceneObject!!.currentSource == 19 || currentSceneObject!!.currentSource == 0 || currentSceneObject!!.currentSource == 12) {
                            iv_album_art!!.setImageResource(R.mipmap.album_art)
                        }
                        LibreLogger.d(this, "Recieved the current source as  " + currentSceneObject!!.currentSource)

                        if (currentSceneObject!!.currentSource == ALEXA_SOURCE/*alexa*/) {
                            disableViews(currentSceneObject!!.currentSource, "")
                        }

                        if (mScanHandler!!.isIpAvailableInCentralSceneRepo(currentIpAddress)) {
                            mScanHandler!!.putSceneObjectToCentralRepo(currentIpAddress, currentSceneObject)
                        }
                        /*Song name, artist name, genre being empty*/
                        setViews()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }

                MIDCONST.MID_CURRENT_PLAY_STATE.toInt() -> {
                    val message = String(packet.getpayload())
                    LibreLogger.d(this, "MB : 51, msg = $message")
                    try {
                        val duration = Integer.parseInt(message)
                        currentSceneObject!!.playstatus = duration
                        if (currentSceneObject!!.playstatus == SceneObject.CURRENTLY_PLAYING) {
                            if (currentSceneObject!!.currentSource != MIDCONST.GCAST_SOURCE) {
                                iv_play_pause!!.setImageResource(R.drawable.pause_orange)
                                iv_next!!.setImageResource(R.drawable.next_enabled)
                                iv_previous!!.setImageResource(R.drawable.prev_enabled)
                            }
                            if (currentSceneObject!!.currentSource != BT_SOURCE) {
                                showLoaderHandler.sendEmptyMessage(PREPARATION_COMPLETED)
                                showLoaderHandler.removeMessages(PREPARATION_TIMEOUT_CONST)
                            }
                            isStillPlaying = true

                        } else {
                            if (currentSceneObject!!.currentSource != MIDCONST.GCAST_SOURCE) {
                                iv_play_pause!!.setImageResource(R.drawable.play_orange)
                                iv_next!!.setImageResource(R.drawable.next_disabled)
                                iv_previous!!.setImageResource(R.drawable.prev_disabled)

                            }
                            if (currentSceneObject!!.playstatus == SceneObject.CURRENTLY_PAUSED) {
                                isStillPlaying = false
                                /* this case happens only when the user has paused from the App so close the existing loader if any */
                                if (currentSceneObject!!.currentSource != 19) {
                                    showLoaderHandler.sendEmptyMessage(PREPARATION_COMPLETED)
                                    showLoaderHandler.removeMessages(PREPARATION_TIMEOUT_CONST)
                                }

                            }
                            if (currentSceneObject!!.playstatus == SceneObject.CURRENTLY_STOPPED && currentSceneObject!!.currentSource != 0) {

                                if (currentSceneObject!!.currentSource != BT_SOURCE
                                        && currentSceneObject!!.currentSource != 15
                                        && currentSceneObject!!.currentSource != GCAST_SOURCE
                                        && currentSceneObject!!.currentSource != ALEXA_SOURCE) {
                                    showLoaderHandler.sendEmptyMessageDelayed(PREPARATION_TIMEOUT_CONST, Constants.PREPARATION_TIMEOUT.toLong())
                                    showLoaderHandler.sendEmptyMessage(PREPARATION_INITIATED)
                                }
                            }
                        }

                        if (mScanHandler!!.isIpAvailableInCentralSceneRepo(currentIpAddress)) {
                            mScanHandler!!.putSceneObjectToCentralRepo(currentIpAddress, currentSceneObject)
                        }

                        setTheSourceIconFromCurrentSceneObject()
                        LibreLogger.d(this, "Stop state is received")
                        LibreLogger.d(this, "Recieved the playstate to be" + currentSceneObject!!.playstatus)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }

                MIDCONST.MID_PLAYCONTROL.toInt() -> {

                    val message = String(packet.getpayload())
                    val ERROR_FAIL = "Error_Fail"
                    val ERROR_NOURL = "Error_NoURL"
                    val ERROR_LASTSONG = "Error_LastSong"
                    LibreLogger.d(this, "recieved 40 $message")
                    try {
                        when {
                            message.equals(ERROR_FAIL, ignoreCase = true) -> {
                                val error = LibreError(currentSceneObject!!.ipAddress, Constants.ERROR_FAIL)
                                BusProvider.getInstance().post(error)
                            }
                            message.equals(ERROR_NOURL, ignoreCase = true) -> {
                                val error = LibreError(currentSceneObject!!.ipAddress, Constants.ERROR_NOURL)
                                BusProvider.getInstance().post(error)
                            }
                            message.equals(ERROR_LASTSONG, ignoreCase = true) -> {
                                val error = LibreError(currentSceneObject!!.ipAddress, Constants.ERROR_LASTSONG)
                                BusProvider.getInstance().post(error)

                                showLoaderHandler.sendEmptyMessage(PREPARATION_COMPLETED)
                                preparingToPlay(false)
                                showLoaderHandler.removeMessages(PREPARATION_TIMEOUT_CONST)
                            }
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }

                MIDCONST.VOLUME_CONTROL -> {

                    val message = String(packet.getpayload())
                    try {
                        val duration = Integer.parseInt(message)
                        currentSceneObject!!.volumeValueInPercentage = duration
                        LibreApplication.INDIVIDUAL_VOLUME_MAP[nettyData.getRemotedeviceIp()] = duration
                        if (mScanHandler!!.isIpAvailableInCentralSceneRepo(currentIpAddress)) {
                            mScanHandler!!.putSceneObjectToCentralRepo(currentIpAddress, currentSceneObject)
                            seek_bar_volume!!.progress = LibreApplication./*ZONE_VOLUME_MAP*/INDIVIDUAL_VOLUME_MAP[currentSceneObject?.ipAddress!!]!!
                            if (seek_bar_volume.progress==0){
                                iv_volume_down?.setImageResource(R.drawable.ic_volume_mute)
                            } else iv_volume_down?.setImageResource(R.drawable.volume_low_enabled)
                        }
                        LibreLogger.d(this, "Recieved the current volume to be " + currentSceneObject!!.volumeValueInPercentage)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }

                MIDCONST.SET_UI -> {
                    try {
                        val message = String(packet.payload)
                        LibreLogger.d(this, "MB : 42, msg = $message")
                        val root = JSONObject(message)
                        val cmd_id = root.getInt(LUCIMESSAGES.TAG_CMD_ID)
                        val window = root.getJSONObject(LUCIMESSAGES.ALEXA_KEY_WINDOW_CONTENT)
                        LibreLogger.d(this, "PLAY JSON is \n= $message\n For ip$currentIpAddress")
                        if (cmd_id == 3) {

                            showLoaderHandler.sendEmptyMessageDelayed(PREPARATION_COMPLETED, 1000)
                            showLoaderHandler.removeMessages(PREPARATION_TIMEOUT_CONST)

                            currentSceneObject = AppUtils.updateSceneObjectWithPlayJsonWindow(window, currentSceneObject!!)

                            if (LibreApplication.LOCAL_IP.isNotEmpty() && currentSceneObject!!.playUrl.contains(LibreApplication.LOCAL_IP))
                                isLocalDMRPlayback = true
                            else if (currentSceneObject!!.currentSource == DMR_SOURCE)
                                isLocalDMRPlayback = true


                            /*Added for Shuffle and Repeat*/
                            if (isLocalDMRPlayback) {
                                /**this is for local content */
                                val renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(currentSceneObject!!.ipAddress)
                                if (renderingDevice != null) {
                                    val renderingUDN = renderingDevice.identity.udn.toString()
                                    val playbackHelper = LibreApplication.PLAYBACK_HELPER_MAP[renderingUDN]
                                    if (playbackHelper != null) {
                                        /**shuffle is on */
                                        if (playbackHelper.isShuffleOn) {
                                            currentSceneObject!!.shuffleState = 1
                                        } else {
                                            currentSceneObject!!.shuffleState = 0
                                        }
                                        /*setting by default*/
                                        if (playbackHelper.repeatState == REPEAT_ONE) {
                                            currentSceneObject!!.repeatState = REPEAT_ONE
                                        }
                                        if (playbackHelper.repeatState == REPEAT_ALL) {
                                            currentSceneObject!!.repeatState = REPEAT_ALL
                                        }
                                        if (playbackHelper.repeatState == REPEAT_OFF) {
                                            currentSceneObject!!.repeatState = REPEAT_OFF
                                        }

                                    }
                                }
                            } else {
                                /**this check made as we will not get shuffle and repeat state in 42, case of DMR
                                 * so we are updating it locally */
                                currentSceneObject!!.shuffleState = window.getInt("Shuffle")
                                currentSceneObject!!.repeatState = window.getInt("Repeat")
                            }

                            if (mScanHandler!!.isIpAvailableInCentralSceneRepo(currentIpAddress)) {
                                mScanHandler!!.putSceneObjectToCentralRepo(currentIpAddress, currentSceneObject)
                            }
                            setViews() //This will take care of disabling the views
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }
                MIDCONST.MID_DEVICE_ALERT_STATUS.toInt() -> {
                    val message = String(packet.payload)
                    LibreLogger.d(this, " message 54 recieved  $message")
                    try {
                        var error: LibreError? = null
                        if (message != null && (message.equals(Constants.DMR_PLAYBACK_COMPLETED, ignoreCase = true) || message.contains(Constants.FAIL))) {

                            /* If the song completes then make the seekbar to the starting of the song */
                            currentSceneObject!!.currentPlaybackSeekPosition = 0f
                            seek_bar_song!!.progress = 0
                        } else if (message.contains(Constants.FAIL)) {
                            error = LibreError(currentIpAddress, resources.getString(R.string.FAIL_ALERT_TEXT))
                        } else if (message.contains(Constants.SUCCESS)) {
                            showLoader(false)
                        } else if (message.contains(Constants.NO_URL)) {
                            error = LibreError(currentIpAddress, resources.getString(R.string.NO_URL_ALERT_TEXT))
                        } else if (message.contains(Constants.NO_PREV_SONG)) {
                            error = LibreError(currentIpAddress, resources.getString(R.string.NO_PREV_SONG_ALERT_TEXT))
                        } else if (message.contains(Constants.NO_NEXT_SONG)) {
                            error = LibreError(currentIpAddress, resources.getString(R.string.NO_NEXT_SONG_ALERT_TEXT))
                        } else if (message.contains(Constants.DMR_SONG_UNSUPPORTED)) {
                            error = LibreError(currentIpAddress, resources.getString(R.string.SONG_NOT_SUPPORTED))
                        } else if (message.contains(LUCIMESSAGES.NEXTMESSAGE)) {
                            handleNextPrevForMB(true)
                        } else if (message.contains(LUCIMESSAGES.PREVMESSAGE)) {
                            handleNextPrevForMB(false)
                        }
                        //                        PicassoTrustCertificates.getInstance(getActivity()).invalidate(currentSceneObject.getAlbum_art());
                        showLoader(false)
                        if (error != null)
                            BusProvider.getInstance().post(error)

                    } catch (e: Exception) {
                        e.printStackTrace()
                        LibreLogger.d(this, " Json exception ")

                    }

                }
            }
        }

    }

    /*Setting views here*/
    private fun setViews() {
        if (currentSceneObject == null) {
            return
        }
        /*making visibility gone*/
        iv_shuffle!!.visibility = View.GONE
        iv_repeat!!.visibility = View.GONE

        if (LibreApplication.INDIVIDUAL_VOLUME_MAP.containsKey(currentSceneObject!!.ipAddress)) {
            seek_bar_volume!!.progress = LibreApplication.INDIVIDUAL_VOLUME_MAP[currentSceneObject?.ipAddress!!]!!
        } else {
            val control = LUCIControl(currentSceneObject!!.ipAddress)
            control.SendCommand(MIDCONST.VOLUME_CONTROL, null, LSSDPCONST.LUCI_GET)
            if (currentSceneObject!!.volumeValueInPercentage >= 0)
                seek_bar_volume!!.progress = currentSceneObject!!.volumeValueInPercentage
        }

        if (seek_bar_volume.progress==0){
            iv_volume_down?.setImageResource(R.drawable.ic_volume_mute)
        } else iv_volume_down?.setImageResource(R.drawable.volume_low_enabled)

        LibreLogger.d(this, "" + currentSceneObject!!.trackName)
        if (!currentSceneObject?.trackName.isNullOrEmpty() && !currentSceneObject?.trackName?.equals("NULL", ignoreCase = true)!!) {
            var trackname: String? = currentSceneObject!!.trackName
            /* This change is done to handle the case of deezer where the song name is appended by radio or skip enabled */
            if (trackname != null && trackname.contains(DEZER_RADIO)) {
                trackname = trackname.replace(DEZER_RADIO, "")
            }

            if (trackname != null && trackname.contains(DEZER_SONGSKIP)) {
                trackname = trackname.replace(DEZER_SONGSKIP, "")
            }

            tv_track_name!!.text = trackname
        } else {
            tv_track_name!!.text = ""
        }

        LibreLogger.d(this, "" + currentSceneObject!!.album_name)
        if (!currentSceneObject?.album_name.isNullOrEmpty() && !currentSceneObject?.album_name?.equals("NULL", ignoreCase = true)!!) {
            tv_album_name!!.text = currentSceneObject?.album_name
        } else {
            tv_album_name!!.text = ""
        }

        tv_album_name?.post {
            tv_album_name?.isSelected = true
        }

        LibreLogger.d(this, "" + currentSceneObject!!.artist_name)
        if (!currentSceneObject?.artist_name.isNullOrEmpty() && !currentSceneObject!!.artist_name.equals("NULL", ignoreCase = true)) {
            tv_album_name?.text = "${tv_album_name?.text.toString()}, ${currentSceneObject?.artist_name}"
        }


        /*this condition making sure that shuffle and repeat is being shown only for USB/SD card*/
        /*added for deezer/tidal source*/
        if (currentSceneObject!!.currentSource == USB_SOURCE
                || currentSceneObject!!.currentSource == SDCARD_SOURCE
                || currentSceneObject!!.currentSource == DEEZER_SOURCE
                || currentSceneObject!!.currentSource == TIDAL_SOURCE
                || currentSceneObject!!.currentSource == SPOTIFY_SOURCE
                || currentSceneObject!!.currentSource == NETWORK_DEVICES
                || currentSceneObject!!.currentSource == FAVOURITES_SOURCE
                || currentSceneObject!!.currentSource == DMR_SOURCE) {

            /*making visibile only for USB/SD card*/
            iv_shuffle!!.visibility = View.VISIBLE
            iv_repeat!!.visibility = View.VISIBLE

            if (currentSceneObject!!.shuffleState == NO_SOURCE) {
                /*which means shuffle is off*/
                iv_shuffle!!.setImageResource(R.drawable.shuffle_disabled)
            } else {
                /*shuffle is on */
                iv_shuffle!!.setImageResource(R.drawable.shuffle_enabled)
            }

            /*if other is playing local DMR we should hide repeat and shuffle*/
            if (currentSceneObject!!.playUrl != null
                    && !currentSceneObject!!.playUrl.contains(LibreApplication.LOCAL_IP)
                    && currentSceneObject!!.currentSource == DMR_SOURCE) {
                iv_shuffle!!.visibility = View.GONE
                iv_repeat!!.visibility = View.GONE
            }

            /*this for repeat state*/
            when (currentSceneObject!!.repeatState) {
                REPEAT_OFF -> iv_repeat!!.setImageResource(R.drawable.repeat_disabled)
                REPEAT_ONE -> iv_repeat!!.setImageResource(R.drawable.ic_repeat_one)
                REPEAT_ALL -> iv_repeat!!.setImageResource(R.drawable.ic_repeatall)
            }
        }

        if (currentSceneObject!!.playstatus == SceneObject.CURRENTLY_PLAYING) {
            if (playPauseNextPrevAllowed()) {
                iv_play_pause!!.setImageResource(R.drawable.pause_orange)
                Log.e("sumacheck", "playcheck1")
                iv_next!!.setImageResource(R.drawable.next_enabled)
                iv_previous!!.setImageResource(R.drawable.prev_enabled)
            }
        } else {
            if (playPauseNextPrevAllowed()) {
                iv_play_pause!!.setImageResource(R.drawable.play_orange)
                iv_next!!.setImageResource(R.drawable.next_disabled)
                iv_previous!!.setImageResource(R.drawable.prev_disabled)
            }
        }

        /* Setting the current seekbar progress -Start*/
        val duration = currentSceneObject!!.currentPlaybackSeekPosition
        seek_bar_song!!.max = currentSceneObject!!.totalTimeOfTheTrack.toInt() / 1000
        seek_bar_song!!.secondaryProgress = currentSceneObject!!.totalTimeOfTheTrack.toInt() / 1000
        Log.d("SEEK", "Duration = " + duration / 1000)
        seek_bar_song!!.progress = duration.toInt() / 1000

        tv_current_duration!!.text = convertMillisToSongTime((duration.toInt() / 1000).toLong())
        tv_total_duration!!.text = convertMillisToSongTime(currentSceneObject!!.totalTimeOfTheTrack / 1000)

        if (!currentSceneObject?.trackName.isNullOrEmpty() && !currentTrackName?.equals(currentSceneObject?.trackName, ignoreCase = true)) {
            currentTrackName = currentSceneObject?.trackName!!
        }

        updateAlbumArt()
        setTheSourceIconFromCurrentSceneObject()
        setControlsForAlexaSource(currentSceneObject)
    }

    private fun setControlsForAlexaSource(currentSceneObject: SceneObject?) {
        LibreLogger.d(this,"setControlsForAlexaSource playUrl = ${currentSceneObject?.playUrl}")
        iv_source_icon.visibility = View.VISIBLE

        setControlIconsForAlexa(currentSceneObject, iv_play_pause, iv_next, iv_previous)

        /* String alexaSourceURL = currentSceneObject.getAlexaSourceImageURL();
        if (alexaSourceURL!=null){
            setImageFromURL(alexaSourceURL,R.drawable.default_album_art,alexaSourceImage);
        }*/

        when {
            currentSceneObject?.playUrl?.toLowerCase()?.contains("tunein",false)!! -> iv_source_icon.setImageResource(R.drawable.tunein_image2)
            currentSceneObject?.playUrl?.toLowerCase()?.contains("iheartradio",false)!! -> iv_source_icon.setImageResource(R.drawable.iheartradio_image2)
            currentSceneObject?.playUrl?.toLowerCase()?.contains("amazon music",false)!! -> iv_source_icon.setImageResource(R.drawable.amazon_image2)
            currentSceneObject?.playUrl?.toLowerCase()?.contains("siriusxm",false)!! -> iv_source_icon.setImageResource(R.drawable.sirius_image2)
            else -> iv_source_icon.setImageResource(R.drawable.alexa_blue_white_100px)
        }
    }

    private fun updateAlbumArt() {
        if (currentSceneObject!!.currentSource != AUX_SOURCE
                && currentSceneObject!!.currentSource != BT_SOURCE
                && currentSceneObject!!.currentSource != GCAST_SOURCE) {
            var album_url = ""
            if (!currentSceneObject!!.album_art.isNullOrEmpty() && currentSceneObject?.album_art?.equals("coverart.jpg", ignoreCase = true)!!) {

                album_url = "http://" + currentSceneObject!!.ipAddress + "/" + "coverart.jpg"
                /* If Track Name is Different just Invalidate the Path
                 * And if we are resuming the Screen(Screen OFF and Screen ON) , it will not re-download it */
                if (currentSceneObject!!.trackName != null
                        && !currentTrackName.equals(currentSceneObject!!.trackName, ignoreCase = true)) {
                    currentTrackName = currentSceneObject?.trackName!!
                    val mInvalidated = mInvalidateTheAlbumArt(currentSceneObject!!, album_url)
                    LibreLogger.d(this, "Invalidated the URL $album_url Status $mInvalidated")
                }

                PicassoTrustCertificates.getInstance(this).load(album_url)
                        .error(R.mipmap.album_art).placeholder(R.mipmap.album_art)
                        .into(iv_album_art)

                /*Blurred Album art*/
                PicassoTrustCertificates.getInstance(this)
                        .load(album_url)
                        .transform(BlurTransformation(this/*,20*/))
                        .error(R.mipmap.blurred_album_art).placeholder(R.mipmap.blurred_album_art)
                        .into(iv_blurred_album_art)
            } else {
                when {
                    !currentSceneObject?.album_art.isNullOrEmpty() -> {
                        album_url = currentSceneObject!!.album_art

                        /*if (currentSceneObject!!.trackName != null
                                && !currentTrackName.equals(currentSceneObject!!.trackName, ignoreCase = true)) {
                            currentTrackName = currentSceneObject?.trackName!!
                            val mInvalidated = mInvalidateTheAlbumArt(currentSceneObject!!, album_url)
                            LibreLogger.d(this, "Invalidated the URL $album_url Status $mInvalidated")
                        }*/

                        Picasso.with(this).load(album_url).placeholder(R.mipmap.album_art)
                                /*.memoryPolicy(MemoryPolicy.NO_CACHE).networkPolicy(NetworkPolicy.NO_CACHE)*/
                                .error(R.mipmap.album_art)
                                .into(iv_album_art)

                        /*Blurred Album art*/
                        Picasso.with(this)
                                .load(album_url)
                                .transform(BlurTransformation(this/*,20*/))
                                .error(R.mipmap.blurred_album_art).placeholder(R.mipmap.blurred_album_art)
                                .into(iv_blurred_album_art)
                    }

                    else -> {
                        iv_album_art?.setImageResource(R.mipmap.album_art)
                        iv_blurred_album_art?.setImageResource(R.mipmap.blurred_album_art)
                    }
                }
            }
        } /*else iv_album_art?.setImageResource(R.mipmap.album_art)*/
    }

    /* This function takes care of setting the image next to sources button depending on the
    current source that is being played.
    */
    private fun
            setTheSourceIconFromCurrentSceneObject() {

        if (currentSceneObject == null)
            return
        /* Enabling the views by default which gets updated to disabled based on the need */
        enableViews(enable = true)

        var imgResId = /*R.mipmap.ic_sources*/R.drawable.songs_borderless

        when(currentSceneObject?.currentSource){
            DMR_SOURCE -> {
                imgResId = R.drawable.my_device_enabled
                tv_source_type.text = getText(R.string.my_device)
            }
            DMP_SOURCE -> {
                imgResId = /*R.mipmap.network*/R.drawable.media_servers_enabled
                tv_source_type.text = getText(R.string.mediaserver)
            }
            SPOTIFY_SOURCE -> {
                imgResId = R.mipmap.spotify
                tv_source_type.text = getText(R.string.spotify)
            }
            USB_SOURCE -> {
                imgResId = /*R.mipmap.usb*/R.drawable.usb_storage_enabled
                tv_source_type.text = getText(R.string.usb_storage)
            }
            SDCARD_SOURCE -> {
                imgResId = R.mipmap.sdcard
                tv_source_type.text = getText(R.string.sdcard)
            }
            VTUNER_SOURCE -> {
                imgResId = R.mipmap.vtuner_logo
                tv_source_type.text = "VTUNER"
                /*disabling views for VTUNER*/
                disableViews(currentSceneObject!!.currentSource, currentSceneObject!!.album_name)
            }

            TUNEIN_SOURCE -> {
                imgResId = R.mipmap.tunein_logo1
                tv_source_type.text = "TuneIn"
                disableViews(currentSceneObject!!.currentSource, currentSceneObject!!.album_name)
            }

            AUX_SOURCE -> {
                imgResId = R.drawable.ic_aux_in
                tv_source_type.text = getText(R.string.aux)
                /* added to make sure we dont show the album art during aux */
                disableViews(currentSceneObject!!.currentSource, getString(R.string.aux))
            }

            BT_SOURCE -> {
                imgResId = R.drawable.ic_bt_on
                tv_source_type.text = getText(R.string.btOn)
                when {
                    currentSceneObject!!.playstatus == SceneObject.CURRENTLY_STOPPED -> disableViews(currentSceneObject!!.currentSource, getString(R.string.btOn))
                    currentSceneObject!!.playstatus == SceneObject.CURRENTLY_PAUSED -> disableViews(currentSceneObject!!.currentSource, getString(R.string.btOn))
                    else -> disableViews(currentSceneObject!!.currentSource, getString(R.string.btOn))
                }
                /* added to make sure we dont show the album art during aux */
                disableViews(currentSceneObject!!.currentSource, getString(R.string.aux))
            }

            DEEZER_SOURCE -> {
                imgResId = R.mipmap.deezer_logo
                tv_source_type.text = "Deezer"
                disableViews(currentSceneObject!!.currentSource, currentSceneObject!!.trackName)
            }

            TIDAL_SOURCE -> {
                imgResId = R.mipmap.tidal_white_logo
                tv_source_type.text = "Tidal"
            }

            FAVOURITES_SOURCE -> {
                imgResId = R.mipmap.ic_remote_favorite
                tv_source_type.text = getText(R.string.favorite_button)
            }

            ALEXA_SOURCE -> {
                imgResId = R.drawable.alexa_blue_white_100px
                tv_source_type.text = getText(R.string.alexaText)
            }
            GCAST_SOURCE -> {
                imgResId = R.mipmap.ic_cast_white_24dp_2x
                tv_source_type.text = getText(R.string.casting)
            }
        }

        iv_source_icon!!.setImageResource(imgResId)

        handleThePlayIconsForGrayoutOption()
    }

    private fun handleThePlayIconsForGrayoutOption() {
        if (iv_previous!!.isEnabled) {
            iv_previous!!.setImageResource(R.drawable.prev_enabled)
        } else
            iv_previous!!.setImageResource(R.drawable.prev_disabled)

        if (iv_next!!.isEnabled) {
            iv_next!!.setImageResource(R.drawable.next_enabled)
        } else
            iv_next!!.setImageResource(R.drawable.next_disabled)

        if (currentSceneObject!!.currentSource != MIDCONST.GCAST_SOURCE) {
            if (!iv_play_pause!!.isEnabled) {
                iv_play_pause!!.setImageResource(R.drawable.play_white)
            } else if (currentSceneObject!!.playstatus == SceneObject.CURRENTLY_PLAYING) {
                iv_play_pause!!.setImageResource(R.drawable.pause_orange)
            } else {
                iv_play_pause!!.setImageResource(R.drawable.play_orange)
            }
            updatePlayPauseNextPrevForCurrentSource(currentSceneObject)
        }
    }

    private fun updatePlayPauseNextPrevForCurrentSource(sceneObject: SceneObject?) {
        if (sceneObject?.currentSource == VTUNER_SOURCE
                        || sceneObject?.currentSource == TUNEIN_SOURCE
                        || sceneObject?.currentSource == BT_SOURCE
                        || sceneObject?.currentSource == AUX_SOURCE
                        || sceneObject?.currentSource == NO_SOURCE) {
            iv_play_pause!!.setImageResource(R.drawable.play_white)
            iv_next!!.setImageResource(R.drawable.next_disabled)
            iv_previous!!.setImageResource(R.drawable.prev_disabled)
        }
    }

    private fun convertMillisToSongTime(time: Long): String {
        val seconds = time.toInt() % 60
        val mins = (time / 60).toInt() % 60
        return if (seconds < 10) "$mins:0$seconds" else "$mins:$seconds"
    }

    fun getTheRenderer(ipAddress: String?): DMRProcessor? {
        val renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(ipAddress)
        if (renderingDevice != null) {
            val renderingUDN = renderingDevice.identity.udn.toString()
            val playbackHelper = LibreApplication.PLAYBACK_HELPER_MAP[renderingUDN]
            return if (playbackHelper != null) {
                LibreApplication.PLAYBACK_HELPER_MAP[renderingUDN] = playbackHelper

                val dmrControlHelper = playbackHelper.dmrHelper
                if (dmrControlHelper != null) {
                    val dmrProcessor = dmrControlHelper.dmrProcessor
                    dmrProcessor.removeListener(this)
                    dmrProcessor.addListener(this)
                    dmrProcessor
                } else
                    null
            } else
                null
        } else
            return null
    }

    /* Seek should work on both Luci command and DMR command */
    internal fun doSeek(pos: Int): Boolean {
        val duration: String
        if (isLocalDMRPlayback) {
            val theRenderer = getTheRenderer(currentIpAddress) ?: return false

            val format = ModelUtil.toTimeString(pos.toLong())
            theRenderer.seek(format)
            LibreLogger.d(this, "LocalDMR pos = " + pos + " total time of the song " + currentSceneObject!!.totalTimeOfTheTrack / 1000 + "format = " + format)
            return true
        } else if (currentSceneObject!!.currentSource == VTUNER_SOURCE || currentSceneObject!!.currentSource == TUNEIN_SOURCE) {
            showToast(R.string.seek_not_allowed)
            return true
        } else {
            val control = LUCIControl(currentSceneObject!!.ipAddress)
            LibreLogger.d(this, "Remote seek = " + pos + " total time of the song " + currentSceneObject!!.totalTimeOfTheTrack)
            //duration = (pos * 0.01 * currentSceneObject.getTotalTimeOfTheTrack()) + "";
            duration = (pos * 1000).toString() + ""
            seek_bar_song!!.progress = pos
            LibreLogger.d(this, "Rempote Seek  = " + duration + " total time of the song " + currentSceneObject!!.totalTimeOfTheTrack)
            control.SendCommand(MIDCONST.MID_PLAYCONTROL.toInt(), "SEEK:$duration", LSSDPCONST.LUCI_SET)

        }/*this is to prevent playback pause when clicking on seekbar while vtuner and tunein */
        return true

    }


    private fun doNextPrevious(isNextPressed: Boolean) {
        if (isLocalDMRPlayback && currentSceneObject != null && (currentSceneObject!!.currentSource == NO_SOURCE
                        || currentSceneObject!!.currentSource == DMR_SOURCE
                        || currentSceneObject!!.currentSource == DDMSSLAVE_SOURCE)) {
            val renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(currentIpAddress)
            if (renderingDevice != null) {
                val renderingUDN = renderingDevice.identity.udn.toString()
                val playbackHelper = LibreApplication.PLAYBACK_HELPER_MAP[renderingUDN]

                if (playbackHelper == null
                        || playbackHelper.dmsHelper == null
                        || !currentSceneObject!!.playUrl.contains(LibreApplication.LOCAL_IP)) {

                    showToast(R.string.no_active_playlist)
                    /* In SA mode we need to go to local content while in HN mode we will go to sources option */
                    if (LibreApplication.activeSSID.contains(DDMS_SSID)) {
                        val localIntent = Intent(this, CTDMSBrowserActivityV2::class.java)
                        localIntent.putExtra(AppConstants.IS_LOCAL_DEVICE_SELECTED, true)
                        localIntent.putExtra(DEVICE_UDN, LibreApplication.LOCAL_UDN)
                        localIntent.putExtra(CURRENT_DEVICE_IP, currentIpAddress)
                        startActivity(localIntent)
                    } else {
                        val localIntent = Intent(this, CTMediaSourcesActivity::class.java)
                        localIntent.putExtra(CURRENT_DEVICE_IP, currentIpAddress)
                        localIntent.putExtra(Constants.CURRENT_SOURCE, "" + currentSceneObject!!.currentSource)
                        localIntent.putExtra(FROM_ACTIVITY, CTNowPlayingActivity::class.java.simpleName)
                        startActivity(localIntent)
                    }
                    this.finish()
                    return
                }

                if ((currentSceneObject?.shuffleState == 0) && (currentSceneObject?.repeatState == REPEAT_OFF)) {
                    if (isNextPressed){
                        if (playbackHelper.isThisTheLastSong || playbackHelper.isThisOnlySong){
                            showToast(R.string.lastSongPlayed)
                            showLoaderHandler.sendEmptyMessage(PREPARATION_COMPLETED)
                            return
                        }
                    } else {
                        if (playbackHelper.isThisFirstSong || playbackHelper.isThisOnlySong){
                            showToast(R.string.onlyOneSong)
                            showLoaderHandler.sendEmptyMessage(PREPARATION_COMPLETED)
                            return
                        }
                    }
                }

                showLoaderHandler.sendEmptyMessageDelayed(PREPARATION_TIMEOUT_CONST, Constants.PREPARATION_TIMEOUT.toLong())
                showLoaderHandler.sendEmptyMessage(PREPARATION_INITIATED)
                if (isNextPressed) {
                    playbackHelper.playNextSong(1)
                } else
                    playbackHelper.playNextSong(-1)
            }
        } else {
            if (currentSceneObject != null && currentSceneObject!!.currentSource == DEEZER_SOURCE) {
                val trackname = currentSceneObject!!.trackName
                if (isNextPressed) {
                    if (trackname != null && trackname.contains(DEZER_SONGSKIP)) {
                        showLoader(false)
                        Toast.makeText(this, "Activate Deezer Premium+ from your computer", Toast.LENGTH_LONG).show()
                        return
                    }
                }
            }

            if (currentSceneObject!!.currentSource != BT_SOURCE
                    && currentSceneObject!!.currentSource != AUX_SOURCE
                    && currentSceneObject!!.currentSource != GCAST_SOURCE) {

                showLoaderHandler.sendEmptyMessageDelayed(PREPARATION_TIMEOUT_CONST, Constants.PREPARATION_TIMEOUT.toLong())
                showLoaderHandler.sendEmptyMessage(PREPARATION_INITIATED)
            }
            val control = LUCIControl(currentSceneObject!!.ipAddress)
            if (isNextPressed)
                control.SendCommand(MIDCONST.MID_PLAYCONTROL.toInt(), LUCIMESSAGES.PLAY_NEXT, CommandType.SET)
            else
                control.SendCommand(MIDCONST.MID_PLAYCONTROL.toInt(), LUCIMESSAGES.PLAY_PREV, CommandType.SET)
        }


    }

    private fun handleNextPrevForMB(isNextPressed: Boolean) {

        if (isLocalDMRPlayback && currentSceneObject != null && (currentSceneObject!!.currentSource == 0 || currentSceneObject!!.currentSource == 2)) {
            val renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(currentIpAddress)
            if (renderingDevice != null) {
                val renderingUDN = renderingDevice.identity.udn.toString()
                val playbackHelper = LibreApplication.PLAYBACK_HELPER_MAP[renderingUDN]

                if (playbackHelper == null
                        || playbackHelper.dmsHelper == null
                        || currentSceneObject != null && !currentSceneObject!!.playUrl.contains(LibreApplication.LOCAL_IP)) {
                    return
                }

                showLoaderHandler.sendEmptyMessageDelayed(PREPARATION_TIMEOUT_CONST, Constants.PREPARATION_TIMEOUT.toLong())
                showLoaderHandler.sendEmptyMessage(PREPARATION_INITIATED)
                if (isNextPressed) {
                    playbackHelper.playNextSong(1)
                } else {
                    /* Setting the current seekbar progress -Start*/
                    val duration = currentSceneObject!!.currentPlaybackSeekPosition
                    Log.d("Current Duration ", "Duration = " + duration / 1000)
                    val durationInSeeconds = duration / 1000
                    if (durationInSeeconds < 5) {
                        playbackHelper.playNextSong(-1)
                    } else {
                        playbackHelper.playNextSong(0)
                    }
                }
            }
        }
    }

    internal fun doVolumeChange(currentVolumePosition: Int): Boolean {
        /* We can make use of CurrentIpAddress instead of CurrenScneObject.getIpAddress*/
        val control = LUCIControl(currentIpAddress)
        control.SendCommand(MIDCONST.VOLUME_CONTROL, "" + currentVolumePosition, CommandType.SET)
        currentSceneObject!!.volumeValueInPercentage = currentVolumePosition
        mScanHandler!!.putSceneObjectToCentralRepo(currentIpAddress, currentSceneObject)
        return true
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {

        /* Added to avoid user pressing the volume change when user is getting call*/
        if (NowPlayingActivity.isPhoneCallbeingReceived) return true

        val action = event.action
        val keyCode = event.keyCode
        val volumeControl = LUCIControl(currentIpAddress)
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (action == KeyEvent.ACTION_DOWN) {
                    if (seek_bar_volume!!.progress > 85) {
                        volumeControl.SendCommand(MIDCONST./*ZONE_VOLUME*/VOLUME_CONTROL, "" + 100, CommandType.SET)
                    } else {
                        volumeControl.SendCommand(MIDCONST./*ZONE_VOLUME*/VOLUME_CONTROL, "" + (seek_bar_volume!!.progress + 6), CommandType.SET)
                    }
                }
                return true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (action == KeyEvent.ACTION_DOWN) {
                    if (seek_bar_volume!!.progress < 15) {
                        volumeControl.SendCommand(MIDCONST./*ZONE_VOLUME*/VOLUME_CONTROL, "" + 0, CommandType.SET)
                    } else {
                        volumeControl.SendCommand(MIDCONST./*ZONE_VOLUME*/VOLUME_CONTROL, "" + (seek_bar_volume!!.progress - 6), CommandType.SET)
                    }
                }
                return true
            }
            else -> return super.dispatchKeyEvent(event)
        }

    }


    override fun onUpdatePosition(position: Long, duration: Long) {

    }

    override fun onUpdateVolume(currentVolume: Int) {

    }

    override fun onPaused() {

    }

    override fun onStoped() {

    }

    override fun onSetURI() {

    }

    override fun onPlayCompleted() {

    }

    override fun onPlaying() {

    }

    override fun onActionSuccess(action: Action<*>) {

    }

    override fun onActionFail(actionCallback: String?, response: UpnpResponse, cause: String?) {
        var cause = cause
        LibreLogger.d(this, " fragment that recieved the callback is " + currentSceneObject!!.ipAddress)

        if (cause != null && cause.contains("Seek:Error")) {
            cause = "Seek Failed!"
        }
        val error = LibreError(currentIpAddress, cause)
        BusProvider.getInstance().post(error)
        LibreLogger.d(this, " fragment posted the error " + currentSceneObject!!.ipAddress)

        runOnUiThread {
            /*we are setting seek bar to iv_previous position once seek got failed*/
            if (actionCallback != null && actionCallback.contains(resources.getString(R.string.SEEK_FAILED)))
                seek_bar_song!!.progress = (currentSceneObject!!.currentPlaybackSeekPosition / 1000).toInt()
        }

    }
}

