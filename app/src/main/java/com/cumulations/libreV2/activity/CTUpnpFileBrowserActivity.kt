package com.cumulations.libreV2.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import com.cumulations.libreV2.adapter.CTDIDLObjectListAdapter
import com.cumulations.libreV2.closeKeyboard
import com.libre.LErrorHandeling.LibreError
import com.libre.LibreApplication
import com.libre.R
import com.libre.Scanning.Constants
import com.libre.Scanning.Constants.*
import com.libre.Scanning.ScanningHandler
import com.libre.app.dlna.dmc.processor.impl.DMSProcessorImpl
import com.libre.app.dlna.dmc.processor.interfaces.DMSProcessor
import com.libre.app.dlna.dmc.server.MusicServer
import com.libre.app.dlna.dmc.utility.DMRControlHelper
import com.libre.app.dlna.dmc.utility.DMSBrowseHelper
import com.libre.app.dlna.dmc.utility.PlaybackHelper
import com.libre.app.dlna.dmc.utility.UpnpDeviceManager
import com.libre.util.LibreLogger
import kotlinx.android.synthetic.main.ct_activity_file_browser.*
import kotlinx.android.synthetic.main.music_playing_widget.*
import org.fourthline.cling.model.meta.RemoteDevice
import org.fourthline.cling.model.types.ServiceType
import org.fourthline.cling.support.model.DIDLObject
import org.fourthline.cling.support.model.container.Container
import org.fourthline.cling.support.model.item.Item
import java.util.*
import kotlin.collections.set

class CTUpnpFileBrowserActivity : CTDeviceDiscoveryActivity(), DMSProcessor.DMSProcessorListener {
    companion object {
        private val TAG = CTUpnpFileBrowserActivity::class.java.name
    }

    private val currentIpAddress: String? by lazy {
        intent?.getStringExtra(CURRENT_DEVICE_IP)
    }
    private val clickedDIDLId: String? by lazy {
        intent?.getStringExtra(CLICKED_DIDL_ID)
    }
    private var deviceUDN: String? = null
    
    private var dmsProcessor: DMSProcessor? = null
    private var didlObjectStack: Stack<DIDLObject>? = Stack()
    private var dmsBrowseHelper: DMSBrowseHelper? = null
    private var position = 0
    private var browsingCancelled = false
    private var needSetListViewScroll = false
    private var selectedDIDLObject: DIDLObject? = null

    private var didlObjectList:ArrayList<DIDLObject>? = ArrayList()
    private var didlObjectArrayAdapter: CTDIDLObjectListAdapter? = null

    @SuppressLint("HandlerLeak")
    internal var handler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                SERVICE_NOT_FOUND -> {
                    /*Error case when we not able to find AVTransport Service*/
                    val error = LibreError(currentIpAddress, resources.getString(R.string.AVTRANSPORT_NOT_FOUND))
                    showErrorMessage(error)
                    openNowPlaying(true)
                }

                DO_BACKGROUND_DMR -> {
                    LibreLogger.d(this, "DMR search DO_BACKGROUND_DMR")
                    upnpProcessor!!.searchDMR()

                    var renderingDevice: RemoteDevice? = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(currentIpAddress)
                    if (renderingDevice == null && upnpProcessor != null) {

                        LibreLogger.d(this, "we dont have the renderer in the Device Manager")
                        LibreLogger.d(this, "Checking the renderer in the registry")
                        /* Special check to make sure we dont have the device in the registry the */
                        renderingDevice = upnpProcessor!!.getTheRendererFromRegistryIp(currentIpAddress)
                        if (renderingDevice != null) {
                            LibreLogger.d(this, "WOW! We found the device in registry and hence we will start the playback with the new helper")
                            dismissDialog()
                            play(selectedDIDLObject!!)
                            this.removeMessages(DO_BACKGROUND_DMR)
                            return
                        }
                    }

                    if (renderingDevice != null) {
                        dismissDialog()
                        play(selectedDIDLObject!!)
                        this.removeMessages(DO_BACKGROUND_DMR)
                    } else {
                        val DO_BACKGROUND_DMR_TIMEOUT = 6000
                        this.sendEmptyMessageDelayed(DO_BACKGROUND_DMR, DO_BACKGROUND_DMR_TIMEOUT.toLong())
                        LibreLogger.d(this, "DMR search request issued")
                    }
                }
            }
        }
    }

    private var mTaskHandler: Handler? = null
    private var mMyTaskRunnable: Runnable = object : Runnable {

        override fun run() {

            if (MusicServer.getMusicServer().isMediaServerReady) {
                handleDIDLObjectClick(adapterClickedPosition)
                adapterClickedPosition = -1
                showToast("Local content loading Done!")
                return
            }

            LibreLogger.d(this, "Loading the Songs..")
            /* and here comes the "trick" */
            mTaskHandler!!.postDelayed(this, 100)

        }

    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ct_activity_file_browser)
//        setViews()
    }

    override fun onStartComplete() {
        super.onStartComplete()
        if (LibreApplication.LOCAL_UDN.trim().isEmpty())
            return

        deviceUDN = intent?.getStringExtra(DEVICE_UDN)

        dmsBrowseHelper = if (LibreApplication.LOCAL_UDN.equals(deviceUDN!!, ignoreCase = true))
            DMSBrowseHelper(true, deviceUDN)
        else
            DMSBrowseHelper(false, deviceUDN)

        if (dmsBrowseHelper == null) {
            LibreLogger.d(this,"dmsBrowseHelper null")
            return
        }

        val dmsDevice = dmsBrowseHelper!!.getDevice(UpnpDeviceManager.getInstance())

        didlObjectStack = dmsBrowseHelper!!.browseObjectStack.clone() as Stack<DIDLObject>
        Log.d("onStartComplete", "didlObjectStack = $didlObjectStack")

        if (dmsDevice == null) {
            LibreLogger.d(this,"dmsDevice null")
        } else {
            dmsProcessor = DMSProcessorImpl(dmsDevice, upnpProcessor!!.controlPoint)
            if (dmsProcessor == null) {
                showToast(R.string.cannotCreateDMS)
                finish()
            } else if (intent.hasExtra(FROM_ACTIVITY)) {
                val renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(currentIpAddress)
                if (renderingDevice != null) {
                    val renderingUDN = renderingDevice.identity.udn.toString()
                    val playbackHelper = LibreApplication.PLAYBACK_HELPER_MAP[renderingUDN]
                    if (playbackHelper != null && playbackHelper.dmsHelper != null) {
                        didlObjectStack = playbackHelper.dmsHelper.browseObjectStack
                        Log.d("onStartComplete", "FROM_ACTIVITY, didlObjectStack = $didlObjectStack")
                        if (didlObjectStack != null) {
                            dmsProcessor?.addListener(this)
                            needSetListViewScroll = true
                            /*Peek : Get the item at the top of the stack without removing it*/
                            browse(didlObjectStack?.peek())
                        }
                    }
                }
            } else {
                dmsProcessor?.addListener(this)
                needSetListViewScroll = true
                /*Peek : Get the item at the top of the stack without removing it*/
//                browse(didlObjectStack?.peek())
                browseByDIDLId(clickedDIDLId)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        setViews()
    }
    
    private fun setViews(){
        toolbar?.title = ""
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(false)
        setMusicPlayerWidget(fl_music_play_widget,currentIpAddress!!)

        updateTitle(intent?.getStringExtra(DIDL_TITLE)!!)

        didlObjectArrayAdapter = CTDIDLObjectListAdapter(this, ArrayList())
        rv_browser_list?.layoutManager = LinearLayoutManager(this)
        rv_browser_list?.adapter = didlObjectArrayAdapter
        
        setListeners()
    }

    private fun setListeners() {
        iv_back?.setOnClickListener {
            onBackPressed()
        }
    }

    private fun updateTitle(title: String) {
        tv_folder_name?.text = title
    }

    override fun onResume() {
        Log.d("UpnpSplashScreen", "DMS Resume")
        super.onResume()
        upnpProcessor?.addListener(this)
    }

    private fun browseByDIDLId(didlObjectId:String?){
        Log.i("browse", "Browse id:$didlObjectId")
        browsingCancelled = false
        runOnUiThread {
            showProgressDialog(R.string.pleaseWait)
        }

        try {
            dmsProcessor?.browse(didlObjectId)
        } catch (t: Throwable) {
            dismissDialog()
            showToast(R.string.browseFailed)
        }
    }

    private fun browse(didlObject: DIDLObject?) {
        val id = didlObject!!.id
        Log.i("browse", "Browse id:$id")
        browsingCancelled = false
        updateTitle(didlObject.title)
        runOnUiThread {
            showProgressDialog(R.string.pleaseWait)
        }

        try {
            dmsProcessor?.browse(id)
        } catch (t: Throwable) {
            dismissDialog()
            showToast(R.string.browseFailed)
        }
    }

    fun play(didlObject: DIDLObject) {
        Log.i(TAG, "play item title:" + didlObject.title)
        closeKeyboard(this@CTUpnpFileBrowserActivity, currentFocus)

        dmsBrowseHelper!!.saveDidlListAndPosition(didlObjectList, position)
        Log.i(javaClass.name, "position$position")
        dmsBrowseHelper!!.browseObjectStack = didlObjectStack
        dmsBrowseHelper!!.scrollPosition = (rv_browser_list?.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()

        Log.d("play", "didlObjectStack = $didlObjectStack")
        Log.d("play", "scrollPosition = ${dmsBrowseHelper?.scrollPosition}")

        /* here first find the UDN of the current selected device */
        var renderingDevice: RemoteDevice? = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(currentIpAddress)
        if (renderingDevice == null && upnpProcessor != null) {
            renderingDevice = upnpProcessor!!.getTheRendererFromRegistryIp(currentIpAddress)
            UpnpDeviceManager.getInstance().onRemoteDeviceAdded(renderingDevice)
        }
        if (renderingDevice == null) {
            runOnUiThread { showToast(R.string.deviceNotFound) }
            return
        }

        val renderingUDN = renderingDevice.identity.udn.toString()
        if (LibreApplication.PLAYBACK_HELPER_MAP[renderingUDN] == null) {
            /* If it is local device then assign local audio manager */
            if (LibreApplication.LOCAL_UDN.equals(renderingUDN, ignoreCase = true)) {
                val audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val dmr = DMRControlHelper(audioManager)
                val playbackHelper = PlaybackHelper(dmr)
                LibreApplication.PLAYBACK_HELPER_MAP[renderingUDN] = playbackHelper
            } else {
                if (renderingDevice == null) return
                val service = renderingDevice.findService(ServiceType(DMRControlHelper.SERVICE_NAMESPACE,
                        DMRControlHelper.SERVICE_AVTRANSPORT_TYPE))
                if (service == null) {
                    /*AVTransport not found so showing error to user*/
                    handler.sendEmptyMessage(SERVICE_NOT_FOUND)
                    return
                }
                /*crash Fix when Service is Null , stil lwe trying to get Actions.*/
                val dmrControl = DMRControlHelper(renderingUDN,
                        upnpProcessor!!.controlPoint, renderingDevice, service)
                val playbackHelper = PlaybackHelper(dmrControl)
                LibreApplication.PLAYBACK_HELPER_MAP[renderingUDN] = playbackHelper
            }
        } else {
            val mPlay = LibreApplication.PLAYBACK_HELPER_MAP[renderingUDN]
            if (mPlay?.playbackStopped!!) {
                val service = renderingDevice.findService(ServiceType(DMRControlHelper.SERVICE_NAMESPACE,
                        DMRControlHelper.SERVICE_AVTRANSPORT_TYPE))
                if (service == null) {
                    /*AVTransport not found so showing error to user*/
                    handler.sendEmptyMessage(SERVICE_NOT_FOUND)
                    return
                }
                val dmrControl = DMRControlHelper(renderingUDN,
                        upnpProcessor!!.controlPoint, renderingDevice, service)
                val playbackHelper = PlaybackHelper(dmrControl)
                LibreApplication.PLAYBACK_HELPER_MAP[renderingUDN] = playbackHelper
            }
        }

        try {
            val playbackHelper = LibreApplication.PLAYBACK_HELPER_MAP[renderingUDN]
            playbackHelper?.dmsHelper = dmsBrowseHelper?.clone()
            playbackHelper?.playSong()

            libreApplication.deviceIpAddress = currentIpAddress
            openNowPlaying(true)
        } catch (e: Exception) {
            //handling the excetion when the device is rebooted
            LibreLogger.d(this, "EXCEPTION while setting the song!!!")
        }

    }

    override fun onBrowseComplete(parentObjectId: String?, result: Map<String, List<DIDLObject>>) {
        LibreLogger.d(this, "Browse Completed , parentObjectId = $parentObjectId")
        if (browsingCancelled) {
            browsingCancelled = false
            return
        }

        runOnUiThread {
            didlObjectList?.clear()
            val containersList = result!!["Containers"]
            if (containersList?.isNotEmpty()!!) {
                for (container in containersList){
                    didlObjectList?.add(container)
                }
            }

            val itemsList = result["Items"] as List<Item>
            if (itemsList?.isNotEmpty()!!) {
                for (item in itemsList){
                    didlObjectList?.add(item)
                }
            }

            dismissDialog()
            didlObjectArrayAdapter?.updateList(didlObjectList as MutableList<DIDLObject>?)

            if (didlObjectArrayAdapter?.didlObjectList?.isEmpty()!!){
                showToast(R.string.noContent)
                return@runOnUiThread
            }

            if (needSetListViewScroll) {
                needSetListViewScroll = false
                rv_browser_list?.scrollToPosition(dmsBrowseHelper?.scrollPosition!!)
            }
        }
    }

    override fun onBrowseFail(message: String) {
        LibreLogger.d(this, "Browse Failed = $message")
        if (browsingCancelled) {
            browsingCancelled = false
            return
        }
        runOnUiThread {
            dismissDialog()
            showToast(/*getString(R.string.loadingMusic) + */message)
//            onBackPressed()
        }
    }

    override fun onBackPressed() {
        closeKeyboard(this, currentFocus)
        if (intent?.getStringExtra(DIDL_TITLE)!! == tv_folder_name?.text?.toString()!!) {
            super.onBackPressed()
        } else {
            didlObjectStack!!.pop()
            if (!didlObjectStack!!.isEmpty()) {
                browse(didlObjectStack!!.peek())
            }
        }
    }

    override fun onRemoteDeviceAdded(device: RemoteDevice) {
        super.onRemoteDeviceAdded(device)
        val ip = device.identity.descriptorURL.host
        LibreLogger.d(this, "Remote device with added with ip $ip")
        if (ip.equals(currentIpAddress!!, ignoreCase = true)) {
            if (selectedDIDLObject != null) {
                /* This is a case where user has selected a DMS source but that time  rendering device was null and hence we play with the storedPlayer object*/
                runOnUiThread {
                    val handler = Handler()
                    handler.postDelayed({
                        dismissDialog()
                        play(selectedDIDLObject!!)
                    }, 2000)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        unRegisterForDeviceEvents()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onRemoteDeviceRemoved(device: RemoteDevice) {
        super.onRemoteDeviceRemoved(device)
        val ip = device.identity.descriptorURL.host
        LibreLogger.d(this, "Remote device with removed with ip $ip")
        try {
            /* KK , Change for Continuing the play after Device Got connected */
            if (ip.equals(currentIpAddress!!, ignoreCase = true)) {
                selectedDIDLObject = null
            }
            LibreLogger.d(this, "Remote device removed $ip")
            //mDeviceGotRemoved(ip);
        } catch (e: Exception) {
            LibreLogger.d(this, "Remote device removed " + ip + "And Exception Happend")
        }

    }

    private var adapterClickedPosition = -1

    fun handleDIDLObjectClick(clickedDIDLPosition: Int) {
        if (!MusicServer.getMusicServer().isMediaServerReady) {
            showToast("Loading all contents,Please Wait")
            adapterClickedPosition = clickedDIDLPosition
            showProgressDialog(R.string.pleaseWait)
            mTaskHandler = Handler()
            mTaskHandler!!.postDelayed(mMyTaskRunnable, 0)
            return
        }

        val clickedDIDLObject = didlObjectList?.get(clickedDIDLPosition)
        this@CTUpnpFileBrowserActivity.position = clickedDIDLPosition

        Log.i(TAG, "handleDIDLObjectClick position:" + this@CTUpnpFileBrowserActivity.position)

        closeKeyboard(this@CTUpnpFileBrowserActivity, currentFocus)

        if (clickedDIDLObject is Container) {
            didlObjectStack!!.push(clickedDIDLObject)
            Log.d("handleClick", "didlObjectStack = $didlObjectStack")
            browse(clickedDIDLObject)
        } else if (clickedDIDLObject is Item) {
            val renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(currentIpAddress)
            if (renderingDevice != null && !UpnpDeviceManager.getInstance().getRemoteRemoved(currentIpAddress)) {
                play(clickedDIDLObject)
            } else {
                selectedDIDLObject = clickedDIDLObject
                handler.sendEmptyMessage(DO_BACKGROUND_DMR)
            }
        }
    }

    private fun openNowPlaying(setDMR: Boolean) {
        unRegisterForDeviceEvents()

        val intent = Intent(this@CTUpnpFileBrowserActivity, CTNowPlayingActivity::class.java)
        /* This change is done to make sure that album art is reflectd after source swithing from Aux to other-START*/
        if (setDMR) {
            val sceneObjectFromCentralRepo = ScanningHandler.getInstance().getSceneObjectFromCentralRepo(currentIpAddress)
            if (sceneObjectFromCentralRepo != null) {
                /* Setting the DMR source */
                sceneObjectFromCentralRepo.currentSource = DMR_SOURCE
                sceneObjectFromCentralRepo.currentPlaybackSeekPosition = 0f
                sceneObjectFromCentralRepo.totalTimeOfTheTrack = 0
            }
        }
        /* This change is done to make sure that album art is reflectd after source swithing from Aux to other*- END*/
        intent.putExtra(CURRENT_DEVICE_IP, currentIpAddress)
        startActivity(intent)
        finish()
    }
}
