package com.cumulations.libreV2.adapter

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import com.cumulations.libreV2.AppUtils
import com.cumulations.libreV2.activity.*
import com.cumulations.libreV2.fragments.CTActiveDevicesFragment
import com.cumulations.libreV2.isConnectedToSAMode
import com.cumulations.libreV2.model.SceneObject
import com.cumulations.libreV2.tcp_tunneling.enums.BatteryType
import com.libre.qactive.LErrorHandeling.LibreError
import com.libre.qactive.Scanning.Constants
import com.libre.qactive.Scanning.ScanningHandler
import com.libre.qactive.alexa.AudioRecordCallback
import com.libre.qactive.alexa.AudioRecordUtil
import com.libre.qactive.alexa.MicExceptionListener
import com.libre.qactive.alexa.MicTcpServer
import com.libre.qactive.constants.LSSDPCONST
import com.libre.qactive.constants.LUCIMESSAGES
import com.libre.qactive.constants.MIDCONST
import com.libre.qactive.luci.LSSDPNodeDB
import com.libre.qactive.luci.LUCIControl
import com.libre.qactive.luci.Utils
import com.libre.qactive.netty.BusProvider
import com.libre.qactive.LibreApplication
import com.libre.qactive.R
import com.libre.qactive.util.LibreLogger
import com.libre.qactive.util.PicassoTrustCertificates
import kotlinx.android.synthetic.main.ct_acitvity_now_playing.*
import kotlinx.android.synthetic.main.ct_list_item_speakers.view.*
import kotlinx.android.synthetic.main.music_playing_widget.view.*
import java.util.concurrent.ConcurrentMap

class CTDeviceListAdapter(val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>(),
        AudioRecordCallback, MicExceptionListener {
    private var sceneObjectMap: LinkedHashMap<String, SceneObject> = LinkedHashMap()
    var audioRecordUtil: AudioRecordUtil? = null
    private var micTcpServer: MicTcpServer? = null

    init {
        audioRecordUtil = AudioRecordUtil.getAudioRecordUtil()
        micTcpServer = MicTcpServer.getMicTcpServer()
        (context as CTDeviceDiscoveryActivity).libreApplication.registerForMicException(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, i: Int): SceneObjectItemViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.ct_list_item_speakers, parent, false)
        return SceneObjectItemViewHolder(view)
    }


    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
//        val sceneObject = sceneObjectList?.get(position)

        val ipAddress = sceneObjectMap.keys.toTypedArray()[position]
        val sceneObject = sceneObjectMap[ipAddress]

        if (viewHolder is SceneObjectItemViewHolder) {
            viewHolder.bindSceneObject(sceneObject, position)
        }
    }

    override fun getItemCount(): Int {
        return /*sceneObjectList?.size!!*/ sceneObjectMap.keys.size
    }

    inner class SceneObjectItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private var currentTrackName: String? = "-1"
//        private var previousSourceIndex: Int = 0

        fun bindSceneObject(sceneObject: SceneObject?, position: Int) {

            try {
                val ipAddress = sceneObject?.ipAddress

                LibreLogger.d(this, "Scene Ipaddress $ipAddress")
                LibreLogger.d(this, "Scene Name " + sceneObject?.sceneName)

                val node = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(ipAddress)


                /* Fix by KK , When Album art is not updating properly */
//                if (currentTrackName == null)
//                    currentTrackName = ""

                /* if (sceneObject.getCurrentSource() != 14) {*/ /* Karuna Commenting For the Single Function to Update the UI*/

//                clearViews()
                if (sceneObject != null) {
                    if (!sceneObject.sceneName.isNullOrEmpty() && !sceneObject.sceneName.equals("NULL", ignoreCase = true)) {
                        if (itemView.tv_device_name.text.toString() != sceneObject.sceneName) {
                            itemView.tv_device_name.text = sceneObject.sceneName
                            itemView.tv_device_name.isSelected = true
                        }
                    } else {
                        itemView.tv_device_name.text = ""
                    }

                    if (!sceneObject.trackName.isNullOrEmpty() && !sceneObject.trackName.equals("NULL", ignoreCase = true)) {
                        var trackname = sceneObject.trackName

                        /* This change is done to handle the case of deezer where the song name is appended by radio or skip enabled */
                        if (trackname.contains(Constants.DEZER_RADIO))
                            trackname = trackname.replace(Constants.DEZER_RADIO, "")

                        if (trackname.contains(Constants.DEZER_SONGSKIP))
                            trackname = trackname.replace(Constants.DEZER_SONGSKIP, "")

                        if (itemView.tv_track_name.text.toString() != trackname) {
                            itemView.tv_track_name.text = trackname
                            itemView.tv_track_name.isSelected = true
                        }
                    } else {
                        itemView.tv_track_name.text = ""
                    }

                    if (!sceneObject.artist_name.isNullOrEmpty()
                            && !sceneObject.artist_name.equals("null", ignoreCase = true)) {
                        if (itemView.tv_album_name.text.toString() != sceneObject.artist_name) {
                            itemView.tv_album_name.text = sceneObject.artist_name
                            itemView.tv_album_name.isSelected = true
                        }
                    } else if (!sceneObject.album_name.isNullOrEmpty()
                            && !sceneObject.album_name.equals("null", ignoreCase = true)) {
                        if (itemView.tv_album_name.text.toString() != sceneObject.album_name) {
                            itemView.tv_album_name.text = sceneObject.album_name
                            itemView.tv_album_name.isSelected = true
                        }
                    } else {
                        itemView.tv_album_name.text = ""
                    }

                    /*this is to show loading dialog while we are preparing to play*/
                    if (sceneObject.currentSource != Constants.AUX_SOURCE
                            /*&& sceneObject!!.currentSource != Constants.EXTERNAL_SOURCE*/
                            && sceneObject.currentSource != Constants.BT_SOURCE
                            && sceneObject.currentSource != Constants.GCAST_SOURCE) {

                        /*Album Art For All other Sources Except */
                        if (!sceneObject.album_art.isNullOrEmpty() && sceneObject.album_art.equals("coverart.jpg", ignoreCase = true)) {
                            val albumUrl = "http://" + sceneObject.ipAddress + "/" + "coverart.jpg"
                            /* If Track Name is Different just Invalidate the Path And if we are resuming the Screen(Screen OFF and Screen ON) , it will not re-download it */

                            if (sceneObject.trackName != null
                                    && !currentTrackName.equals(sceneObject.trackName, ignoreCase = true)) {
                                currentTrackName = sceneObject.trackName!!
                                val mInvalidated = (context as CTDeviceDiscoveryActivity).mInvalidateTheAlbumArt(sceneObject, albumUrl)
                                LibreLogger.d(this, "Invalidated the URL $albumUrl Status $mInvalidated")
                            }

                            PicassoTrustCertificates.getInstance(context).load(albumUrl)
                                    /*.memoryPolicy(MemoryPolicy.NO_CACHE).networkPolicy(NetworkPolicy.NO_CACHE)*/
                                    .placeholder(R.mipmap.album_art)
                                    .error(R.mipmap.album_art)
                                    .into(itemView.iv_album_art)
                        } else {
                            when {
                                !sceneObject.album_art.isNullOrEmpty() -> {

                                    if (sceneObject.trackName != null
                                            && !currentTrackName.equals(sceneObject.trackName, ignoreCase = true)) {
                                        currentTrackName = sceneObject.trackName!!
                                        val mInvalidated = (context as CTDeviceDiscoveryActivity).mInvalidateTheAlbumArt(sceneObject, sceneObject.album_art)
                                        LibreLogger.d(this, "Invalidated the URL ${sceneObject.album_art} Status $mInvalidated")
                                    }

                                    PicassoTrustCertificates.getInstance(context)
                                            .load(sceneObject.album_art)
                                            /*   .memoryPolicy(MemoryPolicy.NO_CACHE).networkPolicy(NetworkPolicy.NO_CACHE)*/
                                            .placeholder(R.mipmap.album_art)
                                            .error(R.mipmap.album_art)
                                            .into(itemView.iv_album_art)
                                }

                                else -> {
                                    itemView.iv_album_art!!.setImageResource(R.mipmap.album_art)
                                }
                            }
                        }
                    }

                    if (sceneObject.playstatus == SceneObject.CURRENTLY_PLAYING) {
                        itemView.iv_play_pause.setImageResource(R.drawable.pause_white)
                    } else {
                        itemView.iv_play_pause.setImageResource(R.drawable.play_white)
                    }

                    if (sceneObject.currentSource == Constants.ALEXA_SOURCE) {
                        setControlIconsForAlexa(sceneObject, itemView.iv_play_pause)
                    }

//                    /* Setting the current seekbar progress -Start*/
//                    val duration = sceneObject.currentPlaybackSeekPosition
//                    itemView.seek_bar_song.max = sceneObject.totalTimeOfTheTrack.toInt() / 1000
//                    itemView.seek_bar_song.secondaryProgress = sceneObject.totalTimeOfTheTrack.toInt() / 1000
//                    Log.d("seek_bar_song", "Duration = " + duration / 1000)
//                    itemView.seek_bar_song.progress = duration.toInt() / 1000
//
////                    /*suma volume bar changes QActive individual volume */
//                    if (sceneObject.currentSource !== Constants.AIRPLAY_SOURCE &&
//                            sceneObject.currentSource !== Constants.SPOTIFY_SOURCE
//                           ) {
//                        if (LibreApplication.ZONE_VOLUME_MAP.containsKey(ipAddress)) {
//                            LibreApplication.ZONE_VOLUME_MAP[ipAddress]?.let { itemView.seek_bar_volume.progress = it }
//                        } else {
//
//                            val control = LUCIControl(ipAddress)
//                            control.SendCommand(MIDCONST.ZONE_VOLUME, null, LSSDPCONST.LUCI_GET)
//                            if (sceneObject.getvolumeZoneInPercentage() >= 0)
//                                itemView.seek_bar_volume.progress = sceneObject.getvolumeZoneInPercentage()
//
//                        }
//                    } else {/* For Airplay & spotify We havve to send and updating only 64*/
//                        if (LibreApplication.INDIVIDUAL_VOLUME_MAP.containsKey(ipAddress)) {
//                            LibreApplication.INDIVIDUAL_VOLUME_MAP[ipAddress]?.let { itemView.seek_bar_volume.progress = it }
//                        } else {
//                            val control = LUCIControl(ipAddress)
//                            control.SendCommand(MIDCONST.VOLUEM_CONTROL, null, LSSDPCONST.LUCI_GET)
//                            if (sceneObject.volumeValueInPercentage >= 0)
//                                itemView.seek_bar_volume.progress = sceneObject.volumeValueInPercentage
//
//                        }
//                    }


//                    if (LibreApplication.INDIVIDUAL_VOLUME_MAP.containsKey(sceneObject!!.ipAddress)) {
//                        itemView.seek_bar_volume!!.progress = LibreApplication.INDIVIDUAL_VOLUME_MAP[sceneObject?.ipAddress!!]!!
//                    } else {
//                        val control = LUCIControl(sceneObject!!.ipAddress)
//                        control.SendCommand(MIDCONST.VOLUME_CONTROL, null, LSSDPCONST.LUCI_GET)
//                        if (sceneObject!!.volumeValueInPercentage >= 0)
//                            itemView.seek_bar_volume!!.progress = sceneObject!!.volumeValueInPercentage
//                    }

//                    if (LibreApplication.ZONE_VOLUME_MAP.containsKey(sceneObject.getIpAddress())) {
//                        LibreApplication.ZONE_VOLUME_MAP[sceneObject.getIpAddress()]?.let { itemView.seek_bar_volume.setProgress(it) }
//                        LibreLogger.d(this, "suma volume bar in setview")
//                    } else {
//                        val control = LUCIControl(sceneObject.getIpAddress())
//                        if (node != null) {
//                            if (sceneObject.getCurrentSource() !== Constants.SPOTIFY_SOURCE
//                                    && sceneObject.getCurrentSource() !== Constants.AIRPLAY_SOURCE
//                                    && !node.getmDeviceCap().getmSource().isAlexaAvsSource()) {
//                                control.SendCommand(MIDCONST.ZONE_VOLUME, null, LSSDPCONST.LUCI_GET)
//                            } else {
//                                control.SendCommand(MIDCONST.VOLUEM_CONTROL, null, LSSDPCONST.LUCI_GET)
//                            }
//                        }
//
//                        if (sceneObject.getvolumeZoneInPercentage() >= 0)
//                            itemView.seek_bar_volume.setProgress(sceneObject.getvolumeZoneInPercentage())
//                        LibreLogger.d(this, "suma volume bar volumepercentage")
//                    }
//
//                    /*For free speakers irrespective of the state use Individual volume*/
//                    if (LibreApplication./*ZONE_VOLUME_MAP*/INDIVIDUAL_VOLUME_MAP.containsKey(ipAddress)) {
//                        itemView.seek_bar_volume.progress = LibreApplication.INDIVIDUAL_VOLUME_MAP[ipAddress]!!
//                    } else {
//                        LUCIControl(ipAddress).SendCommand(MIDCONST./*ZONE_VOLUME*/VOLUME_CONTROL, null, LSSDPCONST.LUCI_GET)
//                        if (sceneObject.volumeValueInPercentage >= 0)
//                            itemView.seek_bar_volume.progress = sceneObject.volumeValueInPercentage
//                    }

                    if (itemView.seek_bar_volume.progress == 0) {
                        itemView.iv_volume_mute?.setImageResource(R.drawable.ic_volume_mute)
                    } else itemView.iv_volume_mute?.setImageResource(R.drawable.volume_low_enabled)

                    if (LibreApplication.INDIVIDUAL_VOLUME_MAP.containsKey(sceneObject!!.ipAddress)) {
                        itemView.seek_bar_volume!!.progress = LibreApplication.INDIVIDUAL_VOLUME_MAP[sceneObject?.ipAddress!!]!!
                        LibreLogger.d(this, "suma in ct device list adapter if part")
                        itemView.seek_bar_volume!!.progress = sceneObject!!.volumeValueInPercentage


                    } else {
                        val control = LUCIControl(sceneObject!!.ipAddress)
                        control.SendCommand(MIDCONST.VOLUME_CONTROL, null, LSSDPCONST.LUCI_GET)
                        if (sceneObject!!.volumeValueInPercentage >= 0)
                            itemView.seek_bar_volume!!.progress = sceneObject!!.volumeValueInPercentage
                        LibreLogger.d(this, "suma in ct device list adapter else part")

                    }

//                    if (itemView.seek_bar_volume.progress==0){
//                        iv_volume_down?.setImageResource(R.drawable.ic_volume_mute)
//                    } else iv_volume_down?.setImageResource(R.drawable.volume_low_enabled)
//


//                    if (itemView.seek_bar_volume.progress==0){
//                        itemView?.iv_volume_mute?.setImageResource(R.drawable.ic_volume_mute)
//                    } else itemView?.iv_volume_mute?.setImageResource(R.drawable.volume_low_enabled)

                    /* This line should always be here do not move this line above the ip-else loop where we check the current Source*/
//                    previousSourceIndex = sceneObject!!.currentSource

//                    toggleAVSViews(sceneObject?.isAlexaBtnLongPressed)

                    updateViews(sceneObject)
                    updatePlayPause(sceneObject)
                    handleThePlayIconsForGrayoutOption(sceneObject)
                    setBatteryViews(sceneObject)

                    handleClickListeners(sceneObject, position)
                }

                if (node.getgCastVerision() != null) {
                    //gcast != null -> hide alexa
                    itemView.ib_alexa_avs_btn.visibility = (View.INVISIBLE)
                    if (itemView.tv_album_name?.text?.toString()?.contains(context.getString(R.string.speaker_ready_cmds))!!) {
                        itemView.tv_album_name.visibility = View.INVISIBLE
                    } else {
                        itemView.tv_album_name.visibility = View.VISIBLE
                    }
                }else{
                    itemView.ib_alexa_avs_btn.visibility = (View.VISIBLE)
                    itemView.tv_album_name.visibility = View.VISIBLE
                }


            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun setBatteryViews(sceneObject: SceneObject?) {
            if (sceneObject?.isBatteryPluggedIn!!) {
                itemView?.iv_battery_status?.visibility = View.VISIBLE
                when (sceneObject.batteryType!!) {
                    BatteryType.BATTERY_LOW, BatteryType.BATTERY_WARNING -> {
                        if (sceneObject.isBatteryCharging) {
                            itemView?.iv_battery_status?.setImageResource(R.drawable.riva_battery_low_charging)
                        } else {
                            itemView?.iv_battery_status?.setImageResource(R.drawable.riva_battery_low)
                        }
                    }

                    BatteryType.BATTERY_MIDDLE -> {
                        if (sceneObject.isBatteryCharging) {
                            itemView?.iv_battery_status?.setImageResource(R.drawable.riva_battery_medium_charging)
                        } else {
                            itemView?.iv_battery_status?.setImageResource(R.drawable.riva_battery_medium)
                        }
                    }

                    BatteryType.BATTERY_HIGH -> {
                        if (sceneObject.isBatteryCharging) {
                            itemView?.iv_battery_status?.setImageResource(R.drawable.riva_battery_high_charging)
                        } else {
                            itemView?.iv_battery_status?.setImageResource(R.drawable.riva_battery_high)
                        }
                    }
                }
            } else {
                itemView?.iv_battery_status?.visibility = View.GONE
            }
        }

        private fun clearViews() {
            itemView?.tv_device_name?.text = ""
            itemView?.tv_track_name?.text = ""
            itemView?.tv_album_name?.text = ""
        }

        private fun handleClickListeners(sceneObject: SceneObject?, position: Int) {

            itemView.cv_speaker.setOnClickListener {
                if (sceneObjectMap.keys.isNotEmpty()) {
                    gotoSourcesOption(sceneObject?.ipAddress!!, sceneObject.currentSource)
                } else {
                    /**notifying adapter as sometime sceneObject is present but not lssdp node */
                    notifyDataSetChanged()
                }
            }

            itemView?.iv_album_art?.setOnClickListener {
                if (itemView.tv_track_name?.text?.toString()?.contains(context.getString(R.string.app_name))!!
                        || itemView.tv_track_name?.text?.toString()?.contains(context.getString(R.string.login_to_enable_cmds))!!
                        || itemView.iv_play_pause?.visibility == View.GONE) {
                    return@setOnClickListener
                }

                context.startActivity(Intent(context, CTNowPlayingActivity::class.java).apply {
                    putExtra(Constants.CURRENT_DEVICE_IP, sceneObject?.ipAddress)
                })
            }



            itemView.iv_play_pause.setOnClickListener {

                val mScanHandler = ScanningHandler.getInstance()
                val mNodeWeGotForControl = mScanHandler.getLSSDPNodeFromCentralDB(sceneObject?.ipAddress)
                        ?: return@setOnClickListener
                if (sceneObject == null)
                    return@setOnClickListener

                if (sceneObject.currentSource == Constants.AUX_SOURCE
                        /*|| sceneObject!!.currentSource == Constants.EXTERNAL_SOURCE*/
                        || sceneObject.currentSource == Constants.GCAST_SOURCE
                        || sceneObject.currentSource == Constants.VTUNER_SOURCE
                        || sceneObject.currentSource == Constants.TUNEIN_SOURCE
                        || sceneObject.currentSource == Constants.BT_SOURCE
                        && (mNodeWeGotForControl.getgCastVerision() == null
                                && (mNodeWeGotForControl.bT_CONTROLLER == SceneObject.CURRENTLY_NOTPLAYING || mNodeWeGotForControl.bT_CONTROLLER == SceneObject.CURRENTLY_PLAYING)
                                || (mNodeWeGotForControl.getgCastVerision() != null && mNodeWeGotForControl.bT_CONTROLLER < SceneObject.CURRENTLY_PAUSED))) {
                    val error = LibreError("", Constants.PLAY_PAUSE_NOT_ALLOWED, 1)//TimeoutValue !=0 means ,its VERYSHORT
                    BusProvider.getInstance().post(error)
                    return@setOnClickListener
                }

                /* For Playing , If DMR is playing then we should give control for Play/Pause*/
                if (AppUtils.isActivePlaylistNotAvailable(sceneObject)) {
                    LibreLogger.d(this, "currently not playing, so take user to sources option activity")

                    val error = LibreError("", context.getString(R.string.no_active_playlist), 1)
                    BusProvider.getInstance().post(error)

                    gotoSourcesOption(sceneObject.ipAddress, sceneObject.currentSource)
                    return@setOnClickListener
                }

                LibreLogger.d(this, "current source is not DMR" + sceneObject.currentSource)

                if (sceneObject.playstatus == SceneObject.CURRENTLY_PLAYING) {
                    LUCIControl.SendCommandWithIp(MIDCONST.MID_PLAYCONTROL.toInt(), LUCIMESSAGES.PAUSE, LSSDPCONST.LUCI_SET, sceneObject.ipAddress)
                    itemView.iv_play_pause.setImageResource(R.drawable.play_white)
                } else {
                    if (sceneObject.currentSource == Constants.BT_SOURCE) { /* Change Done By Karuna, Because for BT Source there is no RESUME*/
                        LUCIControl.SendCommandWithIp(MIDCONST.MID_PLAYCONTROL.toInt(), LUCIMESSAGES.PLAY, LSSDPCONST.LUCI_SET, sceneObject.ipAddress)
                    } else {
                        LUCIControl.SendCommandWithIp(MIDCONST.MID_PLAYCONTROL.toInt(), LUCIMESSAGES.RESUME, LSSDPCONST.LUCI_SET, sceneObject.ipAddress)
                    }
                    itemView.iv_play_pause.setImageResource(R.drawable.pause_white)
                }
            }


//            itemView.seek_bar_volume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
//                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
//                    Log.d("onProgressChanged$progress", "" + position)
//                }
//
//                override fun onStartTrackingTouch(seekBar: SeekBar) {
//                    Log.d("onStartTracking", "" + position)
//                }
//
//                override fun onStopTrackingTouch(seekBar: SeekBar) {
//
//                    LibreLogger.d("onStopTracking", "" + position)
//                    if (seekBar.progress==0){
//                        itemView?.iv_volume_mute?.setImageResource(R.drawable.ic_volume_mute)
//                    } else itemView?.iv_volume_mute?.setImageResource(R.drawable.volume_low_enabled)
//
//                    LUCIControl.SendCommandWithIp(MIDCONST.VOLUME_CONTROL, "" + seekBar.progress, LSSDPCONST.LUCI_SET, sceneObject?.ipAddress)
//
//                    sceneObject!!.volumeValueInPercentage = seekBar.progress
//                    sceneObject.setvolumeZoneInPercentage(seekBar.progress)
//
//                    ScanningHandler.getInstance().putSceneObjectToCentralRepo(sceneObject.ipAddress,sceneObject)
//
////                    TunnelingControl(sceneObject?.ipAddress).sendCommand(PayloadType.DEVICE_VOLUME,(seekBar.progress/5).toByte())
//                }
//            })

            itemView.seek_bar_volume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    Log.d("onProgresChanged$progress", "" + position)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    Log.d("onStartTracking", "" + position)
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {

                    LibreLogger.d("onStopTracking", "" + position)

                    if (sceneObject != null) {
                        if (sceneObject!!.getCurrentSource() !== Constants.AIRPLAY_SOURCE &&
                                sceneObject!!.getCurrentSource() !== Constants.SPOTIFY_SOURCE
                                && sceneObject!!.getCurrentSource() !== Constants.ALEXA_SOURCE) {
                            LUCIControl.SendCommandWithIp(MIDCONST.VOLUEM_CONTROL/*ZONE_VOLUME*/, "" + seekBar.progress, LSSDPCONST.LUCI_SET, sceneObject?.ipAddress)
                        } else {
                            LUCIControl.SendCommandWithIp(MIDCONST.VOLUEM_CONTROL, "" + seekBar.progress, LSSDPCONST.LUCI_SET, sceneObject?.ipAddress)
                        }
                        sceneObject!!.setvolumeZoneInPercentage(seekBar.progress)
                        //scene.setVolumeValueInPercentage(seekBar.getProgress());
                        sceneObjectMap.put(sceneObject.ipAddress, sceneObject)
                        //LUCIControl.SendCommandWithIp(MIDCONST.VOLUEM_CONTROL, "" + seekBar.getProgress(), CommandType.SET, holder.ipaddress);
                    }

                    val sceneObjectFromCentralRepo = ScanningHandler.getInstance().getSceneObjectFromCentralRepo(sceneObject?.ipAddress)
                    if (sceneObjectFromCentralRepo != null) {
                        //  sceneObjectFromCentralRepo.setVolumeValueInPercentage(seekBar.getProgress());
                        sceneObjectFromCentralRepo!!.setvolumeZoneInPercentage(seekBar.progress)
                        //  ScanningHandler.getInstance().putSceneObjectToCentralRepo(getAppl, ip, sceneObjectFromCentralRepo)
                    }
                }
            })

            itemView.ib_alexa_avs_btn.setOnLongClickListener {

                if (!isMicrophonePermissionGranted()) {
                    (context as CTDeviceDiscoveryActivity).requestRecordPermission()
                    return@setOnLongClickListener true
                }

                val lssdpNodes = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(sceneObject?.ipAddress)
                if (lssdpNodes == null || lssdpNodes.alexaRefreshToken.isNullOrEmpty()) {
                    toggleAVSViews(false)
                    (context as CTDeviceDiscoveryActivity).showLoginWithAmazonAlert(sceneObject?.ipAddress!!)
                    return@setOnLongClickListener true
                }

                val phoneIp = Utils().getIPAddress(true)
                if (!sceneObject?.ipAddress.isNullOrEmpty() && !phoneIp.isNullOrEmpty()) {

                    Log.d("OnLongClick", "phone ip: " + phoneIp + "port: " + MicTcpServer.MIC_TCP_SERVER_PORT)
                    LUCIControl(sceneObject?.ipAddress).SendCommand(MIDCONST.MID_MIC, Constants.START_MIC + phoneIp + "," + MicTcpServer.MIC_TCP_SERVER_PORT, LSSDPCONST.LUCI_SET)

                    toggleAVSViews(true)
                    audioRecordUtil?.startRecording(this@CTDeviceListAdapter)
                } else {
                    toggleAVSViews(showListening = false)
                    (context as CTDeviceDiscoveryActivity).showToast("Ip not available")
                }

                return@setOnLongClickListener true
            }

            itemView.ib_alexa_avs_btn?.setOnTouchListener { view, motionEvent ->
                Log.d("AlexaBtn", "motionEvent = " + motionEvent.action)
                if (motionEvent.action == MotionEvent.ACTION_UP || motionEvent.action == MotionEvent.ACTION_CANCEL) {
                    /*if (motionEvent.action != MotionEvent.ACTION_DOWN) {*/
                    Log.d("AlexaBtn", "long press release, sceneObject isLongPressed = " + sceneObject?.isAlexaBtnLongPressed)
                    toggleAVSViews(false)
                }
                return@setOnTouchListener false
            }

            itemView.iv_device_settings?.setOnClickListener {
                context.startActivity(Intent(context, CTDeviceSettingsActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(Constants.CURRENT_DEVICE_IP, sceneObject?.ipAddress)
                    putExtra(Constants.FROM_ACTIVITY, CTActiveDevicesFragment::class.java.simpleName)
                })
            }
        }

        private fun updatePlayPause(sceneObject: SceneObject?) {
            if (sceneObject != null
                    && (sceneObject.currentSource == Constants.VTUNER_SOURCE
                            || sceneObject.currentSource == Constants.TUNEIN_SOURCE
                            || sceneObject.currentSource == Constants.BT_SOURCE
                            || sceneObject.currentSource == Constants.AUX_SOURCE
                            /*|| sceneObject.currentSource == Constants.EXTERNAL_SOURCE*/
                            || sceneObject.currentSource == Constants.NO_SOURCE
                            || sceneObject.currentSource == Constants.GCAST_SOURCE
                            || (sceneObject.currentSource == Constants.DMR_SOURCE
                            && (sceneObject.playstatus == SceneObject.CURRENTLY_STOPPED
                            || sceneObject.playstatus == SceneObject.CURRENTLY_NOTPLAYING)))) {
                itemView.iv_play_pause.isEnabled = false
                itemView.iv_play_pause.setImageResource(R.drawable.play_white)
                itemView.seek_bar_song.progress = 0
                itemView.seek_bar_song.secondaryProgress = 0
                itemView.seek_bar_song.max = 100
                itemView.seek_bar_song.isEnabled = false
            } else {
                itemView.iv_play_pause.isEnabled = true
                itemView.seek_bar_song.isEnabled = true
            }
        }

        private fun setControlIconsForAlexa(currentSceneObject: SceneObject?, playPause: AppCompatImageView) {
            if (currentSceneObject == null) {
                return
            }
            val controlsArr = currentSceneObject.controlsValue ?: return
            playPause.isEnabled = controlsArr[0]
            playPause.isClickable = controlsArr[0]
            if (!controlsArr[0]) {
                playPause.setImageResource(R.drawable.play_white)
            }
        }

        private fun updateViews(sceneObject: SceneObject?) {
            LibreLogger.d(this, "updateViews ${sceneObject?.sceneName} current source = ${sceneObject?.currentSource}")

            itemView?.iv_current_source?.visibility = View.GONE
            itemView.iv_aux_bt?.visibility = View.GONE
            when (sceneObject?.currentSource) {

                Constants.NO_SOURCE,
                Constants.DDMSSLAVE_SOURCE -> {
                    itemView.iv_play_pause.isClickable = false
                    if (sceneObject.currentSource == Constants.NO_SOURCE) {
                        handleAlexaViews(sceneObject)
                    }
                }

                Constants.VTUNER_SOURCE,
                Constants.TUNEIN_SOURCE -> {
                    itemView.iv_play_pause.setImageResource(R.drawable.play_white)
                    itemView?.seek_bar_song?.visibility = View.VISIBLE
                    itemView.seek_bar_song.progress = 0
                    itemView.seek_bar_song.isEnabled = false
                }

                /*For Riva Tunneling, When switched to Aux, its External Source*/
                Constants.AUX_SOURCE -> {
                    handleAlexaViews(sceneObject)
                    itemView.iv_aux_bt?.visibility = View.VISIBLE
                    itemView.iv_aux_bt?.setImageResource(R.drawable.ic_aux_in)
                }

                Constants.BT_SOURCE -> {
                    /*itemView.tv_track_name.text = context.getText(R.string.bluetooth)
                    itemView.tv_album_name.visibility = View.GONE
                    itemView.iv_album_art.visibility = View.GONE
                    itemView.seek_bar_song.progress = 0
                    itemView.seek_bar_song.isEnabled = false

                    val mNode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(sceneObject.ipAddress)
                            ?: return
                    LibreLogger.d(this, "BT controller value in sceneobject " + mNode.bT_CONTROLLER)
                    if (mNode.bT_CONTROLLER != 1 && mNode.bT_CONTROLLER != 2 && mNode.bT_CONTROLLER != 3) {
                        itemView.iv_play_pause.setImageResource(R.drawable.play_white)
                    }*/

                    handleAlexaViews(sceneObject)
                    itemView.iv_aux_bt?.visibility = View.VISIBLE
                    itemView.iv_aux_bt?.setImageResource(R.drawable.ic_bt_on)

                }
                Constants.GCAST_SOURCE -> {
                    //gCast is Playing
                    itemView.iv_play_pause.setImageResource(R.drawable.play_white)
                    itemView.tv_track_name.text = "Casting"
                    itemView.seek_bar_volume.isEnabled = false
                    itemView.seek_bar_volume.progress = 0
                    itemView.seek_bar_volume.isClickable = false

                    itemView.seek_bar_song.progress = 0
                    itemView.seek_bar_song.isEnabled = false
                }

                Constants.ALEXA_SOURCE,
                Constants.DMR_SOURCE,
                Constants.DMP_SOURCE,
                Constants.SPOTIFY_SOURCE,
                Constants.USB_SOURCE -> {
                    if (!sceneObject.trackName.isNullOrEmpty()
                            && !sceneObject.trackName.equals("NULL", ignoreCase = true)) {
                        itemView?.iv_play_pause?.visibility = View.VISIBLE
                        itemView?.tv_track_name?.visibility = View.VISIBLE

                        if ((!sceneObject.album_name.isNullOrEmpty()
                                        && !sceneObject.album_name.equals("NULL", ignoreCase = true))
                                || (!sceneObject.artist_name.isNullOrEmpty()
                                        && !sceneObject.artist_name.equals("NULL", ignoreCase = true))) {
                            itemView?.tv_album_name?.visibility = View.VISIBLE
                        }

                        if (!sceneObject.album_art.isNullOrEmpty() && !sceneObject.album_art.equals("NULL", ignoreCase = true)) {
                            itemView?.iv_album_art?.visibility = View.VISIBLE
                        }

                        if (sceneObject.totalTimeOfTheTrack > 0 && sceneObject.currentPlaybackSeekPosition >= 0) {
                            itemView?.seek_bar_song?.visibility = View.VISIBLE
                            itemView?.seek_bar_song?.isEnabled = true
                        }
                    } else {
                        handleAlexaViews(sceneObject)
                    }

                    if (sceneObject.currentSource == Constants.SPOTIFY_SOURCE) {
                        itemView?.iv_current_source?.visibility = View.VISIBLE
                        itemView?.iv_current_source?.setImageResource(R.mipmap.spotify)
                    }

                    if (sceneObject.currentSource == Constants.ALEXA_SOURCE) {
                        itemView?.iv_current_source?.visibility = View.VISIBLE
                        when {
                            sceneObject.playUrl?.contains("Spotify", true)!! -> itemView?.iv_current_source?.setImageResource(R.mipmap.spotify)
                            sceneObject.playUrl?.contains("Deezer", true)!! -> itemView?.iv_current_source?.setImageResource(R.mipmap.riva_deezer_icon)
                            sceneObject.playUrl?.contains("Pandora", true)!! -> itemView?.iv_current_source?.setImageResource(R.mipmap.riva_pandora_icon)
                            else -> itemView?.iv_current_source?.visibility = View.GONE
                        }
                    }

                    if (sceneObject.currentSource == Constants.USB_SOURCE) {
                        itemView?.iv_aux_bt?.visibility = View.VISIBLE
                        itemView?.iv_aux_bt?.setImageResource(R.drawable.usb_storage_enabled)
                    }
                }

                else -> handleAlexaViews(sceneObject)
            }

            toggleAlexaBtnForSAMode()
            if (itemView.seek_bar_volume.progress == 0) {
                itemView.iv_volume_mute?.setImageResource(R.drawable.ic_volume_mute)
            } else itemView.iv_volume_mute?.setImageResource(R.drawable.volume_low_enabled)

            if (LibreApplication.INDIVIDUAL_VOLUME_MAP.containsKey(sceneObject!!.ipAddress)) {
                itemView.seek_bar_volume!!.progress = LibreApplication.INDIVIDUAL_VOLUME_MAP[sceneObject?.ipAddress!!]!!
                LibreLogger.d(this, "suma in ct device list adapter if part")
                itemView.seek_bar_volume!!.progress = sceneObject!!.volumeValueInPercentage

            } else {
                val control = LUCIControl(sceneObject!!.ipAddress)
                control.SendCommand(MIDCONST.VOLUME_CONTROL, null, LSSDPCONST.LUCI_GET)
                if (sceneObject!!.volumeValueInPercentage >= 0)
                    itemView.seek_bar_volume!!.progress = sceneObject!!.volumeValueInPercentage
                LibreLogger.d(this, "suma in ct device list adapter else part")

            }

            when (sceneObject?.tunnelingCurrentSource) {
                Constants.AUX_SOURCE -> {
                    itemView?.iv_aux_bt?.visibility = View.VISIBLE
                    itemView?.iv_aux_bt?.setImageResource(R.drawable.ic_aux_in)
                }

                Constants.BT_SOURCE -> {
                    itemView?.iv_aux_bt?.visibility = View.VISIBLE
                    itemView?.iv_aux_bt?.setImageResource(R.drawable.ic_bt_on)
                }

                Constants.USB_SOURCE -> {
                    itemView?.iv_aux_bt?.visibility = View.VISIBLE
                    itemView?.iv_aux_bt?.setImageResource(R.drawable.usb_storage_enabled)
                }

                Constants.TUNNELING_WIFI_SOURCE, -1 -> {
                    itemView?.iv_aux_bt?.visibility = View.GONE
                }
            }

            if (AppUtils.isActivePlaylistNotAvailable(sceneObject)) {
                LibreLogger.d(this, "isActivePlaylistNotAvailable true")
                handleAlexaViews(sceneObject)
            }

            if (AppUtils.isDMRPlayingFromOtherPhone(sceneObject)) {
                LibreLogger.d(this, "isDMRPlayingFromOtherPhone true")
                itemView?.iv_play_pause?.visibility = View.GONE
            }

            if (AppUtils.isLocalDMRPlaying(sceneObject)) {
                itemView.iv_play_pause.visibility = View.VISIBLE
            }
        }

        private fun handleAlexaViews(sceneObject: SceneObject?) {
            itemView?.seek_bar_song?.visibility = View.GONE
            itemView?.iv_album_art?.visibility = View.GONE
            itemView?.iv_play_pause?.visibility = View.GONE
            itemView?.tv_track_name?.text = context.getText(R.string.libre_voice)

            val node = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(sceneObject?.ipAddress)

            if (node.getgCastVerision() != null) {
                itemView.ib_alexa_avs_btn.visibility = View.GONE
            }else{
                itemView.ib_alexa_avs_btn.visibility = View.VISIBLE
                if (node?.alexaRefreshToken.isNullOrEmpty()) {
                    itemView?.tv_track_name?.visibility = View.VISIBLE
                    itemView?.tv_track_name?.text = context.getText(R.string.login_to_enable_cmds)
                    itemView?.tv_album_name?.visibility = View.GONE
                } else {
                    itemView?.tv_album_name?.visibility = View.VISIBLE
                    itemView?.tv_track_name?.visibility = View.VISIBLE
                    itemView?.tv_track_name?.text = context.getText(R.string.app_name)
                    itemView?.tv_album_name?.text = context.getText(R.string.speaker_ready_for_cmds)

                }
            }

        }

        private fun toggleAlexaBtnForSAMode() {
            val ssid = AppUtils.getConnectedSSID(context)
            itemView?.ib_alexa_avs_btn?.isEnabled = ssid != null && !isConnectedToSAMode(ssid)
            if (itemView?.ib_alexa_avs_btn?.isEnabled!!) {
                itemView.ib_alexa_avs_btn?.alpha = 1f
            } else itemView.ib_alexa_avs_btn?.alpha = .5f
        }

        private fun handleThePlayIconsForGrayoutOption(sceneObject: SceneObject?) {
            if (itemView != null) {
                if (!itemView.iv_play_pause?.isEnabled!!) {
                    itemView.iv_play_pause.setImageResource(R.drawable.play_white)
                } else if (sceneObject?.playstatus == SceneObject.CURRENTLY_PLAYING) {
                    itemView.iv_play_pause.setImageResource(R.drawable.pause_white)
                } else {
                    itemView.iv_play_pause.setImageResource(R.drawable.play_white)
                }
            }
        }

        private fun toggleAVSViews(showListening: Boolean) {

            if (showListening) {
                itemView.tv_alexa_listening.visibility = View.VISIBLE
                itemView.ll_playing_layout.visibility = View.INVISIBLE
            } else {
                itemView.tv_alexa_listening.visibility = View.GONE
                itemView.ll_playing_layout.visibility = View.VISIBLE
            }
        }
    }

    private fun gotoSourcesOption(ipaddress: String, currentSource: Int) {
        context.startActivity(Intent(context, CTSourceSelectionActivity::class.java).apply {
            putExtra(Constants.CURRENT_DEVICE_IP, ipaddress)
            putExtra(Constants.CURRENT_SOURCE, "" + currentSource)
        })
    }

    fun addDeviceToList(sceneObject: SceneObject?) {
        if (sceneObject == null)
            return
        Log.d("addDevice", "ip ${sceneObject.ipAddress} scene ${sceneObject.trackName}")
        sceneObjectMap[sceneObject.ipAddress!!] = sceneObject
//        notifyDataSetChanged()

        /*To update particular item only*/
        val pos = ArrayList<SceneObject>(sceneObjectMap.values).indexOf(sceneObject)
        Log.d("addDeviceToList", "pos = $pos, ${sceneObject.sceneName}")
        notifyItemChanged(pos, sceneObject)
    }

    fun getDeviceSceneFromAdapter(deviceIp: String): SceneObject? {
        Log.d("getDevice", "ip $deviceIp")
        return sceneObjectMap[deviceIp]
    }

    fun removeDeviceFromList(sceneObject: SceneObject?) {
        Log.d("removeDevice", "ip ${sceneObject?.ipAddress} scene ${sceneObject?.trackName}")
        sceneObjectMap.remove(sceneObject?.ipAddress)
        notifyDataSetChanged()
    }

    fun addAllDevices(sceneObjectConcurrentMap: ConcurrentMap<String, SceneObject>) {

        sceneObjectMap.clear()
        sceneObjectConcurrentMap.forEach { (ipAddress, sceneObject) ->
            Log.d("addAllDevices", "ip $ipAddress scene ${sceneObject.trackName}")
            sceneObjectMap[ipAddress] = sceneObject
        }

        notifyDataSetChanged()
    }

    fun clear() {
        sceneObjectMap.clear()
//        sceneObjectMap = LinkedHashMap()
        notifyDataSetChanged()
    }

    override fun recordError(error: String?) {
        (context as CTDeviceDiscoveryActivity).showToast(error!!)
    }

    override fun recordStopped() {
        Log.e(this::class.java.simpleName, "recordStopped")
    }

    override fun recordProgress(byteBuffer: ByteArray?) {
        Log.e(this::class.java.simpleName, "recordProgress")
    }

    override fun sendBufferAudio(audioBufferBytes: ByteArray?) {
        micTcpServer?.sendDataToClient(audioBufferBytes)
    }

    override fun micExceptionCaught(e: java.lang.Exception?) {
        Log.e(this::class.java.simpleName, "micExceptionCaught ${e?.message}")
        audioRecordUtil?.stopRecording()
    }

    private fun isMicrophonePermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return AppUtils.isPermissionGranted(context, Manifest.permission.RECORD_AUDIO)
        }
        return true
    }
}



