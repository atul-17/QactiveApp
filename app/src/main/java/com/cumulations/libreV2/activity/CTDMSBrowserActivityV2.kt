package com.cumulations.libreV2.activity

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.content.ContextCompat
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.cumulations.libreV2.AppConstants
import com.cumulations.libreV2.closeKeyboard
import com.cumulations.libreV2.fragments.CTDMRBrowserFragmentV2
import com.libre.LErrorHandeling.LibreError
import com.libre.LibreApplication
import com.libre.R
import com.libre.Scanning.Constants
import com.libre.Scanning.Constants.*
import com.libre.Scanning.ScanningHandler
import com.libre.app.dlna.dmc.processor.impl.DMSProcessorImpl
import com.libre.app.dlna.dmc.processor.interfaces.DMSProcessor
import com.libre.app.dlna.dmc.processor.upnp.LoadLocalContentService
import com.libre.app.dlna.dmc.server.ContentTree
import com.libre.app.dlna.dmc.server.MusicServer
import com.libre.app.dlna.dmc.server.MusicServer.*
import com.libre.app.dlna.dmc.utility.DMRControlHelper
import com.libre.app.dlna.dmc.utility.DMSBrowseHelper
import com.libre.app.dlna.dmc.utility.PlaybackHelper
import com.libre.app.dlna.dmc.utility.UpnpDeviceManager
import com.libre.constants.MIDCONST
import com.libre.luci.LSSDPNodes
import com.libre.luci.LUCIPacket
import com.libre.luci.Utils
import com.libre.netty.LibreDeviceInteractionListner
import com.libre.netty.NettyData
import com.libre.util.LibreLogger
import kotlinx.android.synthetic.main.ct_activity_upnp_browser.*
import kotlinx.android.synthetic.main.music_playing_widget.*
import org.fourthline.cling.model.meta.RemoteDevice
import org.fourthline.cling.model.types.ServiceType
import org.fourthline.cling.support.model.DIDLObject
import org.fourthline.cling.support.model.container.Container
import org.fourthline.cling.support.model.item.Item
import org.json.JSONObject
import java.util.ArrayList
import java.util.Stack
import kotlin.collections.HashMap
import kotlin.collections.set

class CTDMSBrowserActivityV2 : CTDeviceDiscoveryActivity(), DMSProcessor.DMSProcessorListener, LibreDeviceInteractionListner {
    companion object {
        private val TAG = CTDMSBrowserActivityV2::class.java.name
    }

    private var currentFragment: CTDMRBrowserFragmentV2? = null
    private val currentIpAddress: String? by lazy {
        intent?.getStringExtra(CURRENT_DEVICE_IP)
    }
    var dmsProcessor: DMSProcessor? = null
    private var didlObjectStack: Stack<DIDLObject>? = Stack()
    var dmsBrowseHelper: DMSBrowseHelper? = null
    private var position = 0
    private var browsingCancelled = false
    private var needSetListViewScroll = false
    private val searchResultsDIDLObjectList = ArrayList<DIDLObject>()

    private var dmsDeviceUDN: String? = null
    private var selectedDIDLObject: DIDLObject? = null

    private val tabTitles = arrayOf(
            ALBUMS,
            ARTISTS,
            SONGS,
            GENRES
    )

    private val fragmentMap: HashMap<String, CTDMRBrowserFragmentV2> = HashMap()

    @SuppressLint("HandlerLeak")
    internal var handler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                NETWORK_TIMEOUT -> {
                    LibreLogger.d(this, "handler message recieved")
                    val error = LibreError(currentIpAddress, getString(R.string.requestTimeout))
                    showErrorMessage(error)
                    dismissDialog()
                    openNowPlaying(true)
                }
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

    @SuppressLint("HandlerLeak")
    internal var mediaHandler: Handler = object : Handler() {

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            when (msg.what) {
                MediaEnum.MEDIA_PROCESS_INIT -> {
                    val mNetIf = Utils.getActiveNetworkInterface()
                    if (mNetIf == null) {
                        LibreLogger.d(this, "mNetIf is Null")
                        this.sendEmptyMessage(MediaEnum.MEDIA_LOADING_FAIL)
                    } else {
                        startService(Intent(this@CTDMSBrowserActivityV2, LoadLocalContentService::class.java))
                        showToast(R.string.loadingLocalContent)
                    }
                    sendEmptyMessageDelayed(MediaEnum.MEDIA_PROCESS_DONE, Constants.LOADING_TIMEOUT.toLong())
                }

                MediaEnum.MEDIA_PROCESS_DONE -> {
                    dismissDialog()
                    LibreLogger.d(this, "DMR ready")
                    /*loading stopMediaServer with success message*/
                    showToast("Local content loading Done!")
                    onStartComplete()
                }

                MediaEnum.MEDIA_LOADING_FAIL -> {
                    dismissDialog()
                    showToast(R.string.restartAppManually)
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
        setContentView(R.layout.ct_activity_upnp_browser)

        toolbar?.title = ""
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(false)

        dmsDeviceUDN = intent?.getStringExtra(DEVICE_UDN)

        if (LibreApplication.LOCAL_UDN.trim().isEmpty()) {
            LibreLogger.d(this, "start local media loading")
            /* this is the case where the content is not present and hence we need to load */
            showProgressDialog(R.string.loadingLocalContent)
            mediaHandler.sendEmptyMessage(MediaEnum.MEDIA_PROCESS_INIT)
        }

        setupTabsViewPager()
        setListeners()
    }

    override fun onStart() {
        super.onStart()
        setMusicPlayerWidget(fl_music_play_widget,currentIpAddress!!)
    }

    private fun setListeners() {
        iv_back?.setOnClickListener {
            unRegisterForDeviceEvents()
            onBackPressed()
        }

        et_search_media?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                LibreLogger.d(this, "text changed $s")
                if (s.isEmpty())
                    return
                searchResultsDIDLObjectList.clear()
                val currentFragment = getCurrentFragment()
                for (didlObject in currentFragment?.getCurrentDIDLObjectList()!!) {
                    if (didlObject.title.toString().toLowerCase().contains(s.toString().toLowerCase())) {
                        LibreLogger.d(this, "exist " + s + " in " + didlObject.title)
                        searchResultsDIDLObjectList.add(didlObject)
                    }
                }

                /*For now showing in existing fragment*/
                currentFragment?.updateBrowserList(searchResultsDIDLObjectList)
            }

            override fun afterTextChanged(s: Editable) {

            }
        })
    }

    private fun setupTabsViewPager() {
        val tabsViewPagerAdapter = TabsViewPagerAdapter(supportFragmentManager)
        view_pager_tabs.adapter = tabsViewPagerAdapter
        view_pager_tabs.offscreenPageLimit = 4
        tabs_layout_music_type?.setupWithViewPager(view_pager_tabs)
        tabs_layout_music_type?.setTabTextColors(ContextCompat.getColor(this, R.color.white),
                ContextCompat.getColor(this, R.color.app_text_color_enabled))
    }

    override fun onResume() {
        Log.d("UpnpSplashScreen", "DMS Resume")
        super.onResume()
        registerForDeviceEvents(this)
        if (upnpProcessor != null) {
            upnpProcessor?.addListener(this)
        }
    }

    private fun updateTitle(title: String) {
        tv_device_name?.text = title
    }

    fun browse(didlObject: DIDLObject?) {
        val id = didlObject!!.id
        Log.i("browse", "Browse id:$id")
        browsingCancelled = false
        if (id == ContentTree.ROOT_ID) {
            updateTitle(getString(R.string.music))
        } /*else {
            updateTitle(didlObject.title)
        }*/

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
        closeKeyboard(this@CTDMSBrowserActivityV2, currentFocus)

        Log.i(javaClass.name, "play, position$position")
        dmsBrowseHelper!!.saveDidlListAndPosition(currentFragment?.getCurrentDIDLObjectList(), position)
        dmsBrowseHelper!!.browseObjectStack = didlObjectStack
        dmsBrowseHelper!!.scrollPosition = currentFragment?.getFirstVisibleItemPosition()!!

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
            if (didlObjectStack!!.size == 1) {
                et_search_media?.visibility = View.GONE
                LibreLogger.d(this, "Hide search at first page")
            }

            val containersList = result!!["Containers"]
            if (containersList?.isNotEmpty()!!) {
                val childContainerDIDLObjectList = ArrayList<DIDLObject>()
                for (container in containersList) {
                    Log.e("onBrowseComplete", "container title = ${container.title}, id ${container.id}, clazz = ${container.clazz.value}")
                    /*Browse only for top containers = Albums,Artists,Songs,Genres*/
                    if (container.id == ContentTree.AUDIO_ALBUMS_ID
                            || container.id == ContentTree.AUDIO_ARTISTS_ID
                            || container.id == ContentTree.AUDIO_SONGS_ID
                            || container.id == ContentTree.AUDIO_GENRES_ID){
                        didlObjectStack?.push(container)
                        browse(container)
                    } else {
                        childContainerDIDLObjectList?.add(container)
                    }
                }

                when {
                    parentObjectId?.contains(ContentTree.AUDIO_ALBUMS_ID)!! -> {
                        val fragmentV2 = fragmentMap[ALBUMS]
                        fragmentV2?.updateBrowserList(childContainerDIDLObjectList)
                        fragmentMap[ALBUMS] = fragmentV2!!
                    }

                    parentObjectId?.contains(ContentTree.AUDIO_ARTISTS_ID)!! -> {
                        val fragmentV2 = fragmentMap[ARTISTS]
                        fragmentV2?.updateBrowserList(childContainerDIDLObjectList)
                        fragmentMap[ARTISTS] = fragmentV2!!
                    }

                    parentObjectId?.contains(ContentTree.AUDIO_SONGS_ID)!! -> {
                        val fragmentV2 = fragmentMap[SONGS]
                        fragmentV2?.updateBrowserList(childContainerDIDLObjectList)
                        fragmentMap[SONGS] = fragmentV2!!
                    }

                    parentObjectId?.contains(ContentTree.AUDIO_GENRES_ID)!! -> {
                        val fragmentV2 = fragmentMap[GENRES]
                        fragmentV2?.updateBrowserList(childContainerDIDLObjectList)
                        fragmentMap[GENRES] = fragmentV2!!
                    }
                }
            }

            val itemsList = result["Items"] as List<Item>
            if (itemsList?.isNotEmpty()!!) {
                val itemsDIDLObjectList = ArrayList<DIDLObject>()
                showProgressDialog(R.string.pleaseWait)
                for (item in itemsList) {
                    Log.e("CTDMRBrowserFragment", "item = " + item.title)
                    Log.e("onBrowseComplete", "item id ${item.id}, clazz = ${item.clazz.value}")
                    itemsDIDLObjectList?.add(item)
                }

                when {
                    parentObjectId?.contains(ContentTree.AUDIO_ALBUMS_ID)!! -> {
                        val fragmentV2 = fragmentMap[ALBUMS]
                        fragmentV2?.updateBrowserList(itemsDIDLObjectList)
                        fragmentMap[ALBUMS] = fragmentV2!!
                    }

                    parentObjectId?.contains(ContentTree.AUDIO_ARTISTS_ID)!! -> {
                        val fragmentV2 = fragmentMap[ARTISTS]
                        fragmentV2?.updateBrowserList(itemsDIDLObjectList)
                        fragmentMap[ARTISTS] = fragmentV2!!
                    }

                    parentObjectId?.contains(ContentTree.AUDIO_SONGS_ID)!! -> {
                        val fragmentV2 = fragmentMap[SONGS]
                        fragmentV2?.updateBrowserList(itemsDIDLObjectList)
                        fragmentMap[SONGS] = fragmentV2!!
                    }

                    parentObjectId?.contains(ContentTree.AUDIO_GENRES_ID)!! -> {
                        val fragmentV2 = fragmentMap[GENRES]
                        fragmentV2?.updateBrowserList(itemsDIDLObjectList)
                        fragmentMap[GENRES] = fragmentV2!!
                    }
                }
            }

            dismissDialog()

            if (needSetListViewScroll) {
                needSetListViewScroll = false
                getCurrentFragment()?.scrollToPosition(dmsBrowseHelper!!.scrollPosition)
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
            showToast(message)
            onBackPressed()
        }
    }

    /*override fun onBackPressed() {
        closeKeyboard(this@CTDMSBrowserActivityV2, currentFocus)
        if (didlObjectStack!!.isEmpty() || didlObjectStack!!.peek().id == ContentTree.AUDIO_ID) {
            *//*val intent = Intent(this, CTNowPlayingActivity::class.java)
            intent.putExtra(CURRENT_DEVICE_IP, currentIpAddress)
            startActivity(intent)
            finish()*//*
            finish()
        } else {
            didlObjectStack!!.pop()
            if (!didlObjectStack!!.isEmpty()) {
                browse(didlObjectStack!!.peek())
            }
        }
    }*/

    override fun onStartComplete() {
        // TODO Auto-generated method stub

        if (LibreApplication.LOCAL_UDN.trim().isEmpty())
            return

        if (intent?.getBooleanExtra(AppConstants.IS_LOCAL_DEVICE_SELECTED, false)!!) {
            dmsDeviceUDN = LibreApplication.LOCAL_UDN.trim()
        }

        dmsBrowseHelper = if (LibreApplication.LOCAL_UDN.equals(dmsDeviceUDN!!, ignoreCase = true))
            DMSBrowseHelper(true, dmsDeviceUDN)
        else
            DMSBrowseHelper(false, dmsDeviceUDN)

        if (dmsBrowseHelper == null) return

        val dmsDevice = dmsBrowseHelper!!.getDevice(UpnpDeviceManager.getInstance())
        didlObjectStack = dmsBrowseHelper!!.browseObjectStack.clone() as Stack<DIDLObject>
        Log.d("onStartComplete", "didlObjectStack = $didlObjectStack")

        if (dmsDevice == null) {

            upnpProcessor!!.searchDMR()
            val builder = AlertDialog.Builder(this@CTDMSBrowserActivityV2)
            builder.setMessage(getString(R.string.deviceMissing))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.retry)) { dialog, id -> onStartComplete() }
                    .setNegativeButton(getString(R.string.cancel)) { dialog, id -> dialog.dismiss() }
            val alert = builder.create()
            alert.show()

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
                            updateTitle(dmsDevice.details.friendlyName)
                            dmsProcessor?.addListener(this)
                            needSetListViewScroll = true

                            /*Peek : Get the item at the top of the stack without removing it*/
                            browse(didlObjectStack?.peek())
                        }
                    }
                }
            } else {
                updateTitle(dmsDevice.details.friendlyName)
                dmsProcessor?.addListener(this)
                needSetListViewScroll = true
                /*Peek : Get the item at the top of the stack without removing it*/
                browse(didlObjectStack?.peek())
            }
        }
        /* initiating the search DMR */
        upnpProcessor?.searchDMR()
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
        dmsProcessor?.removeListener(this)
        handler.removeCallbacksAndMessages(null)
        mediaHandler.removeCallbacksAndMessages(null)
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


    override fun deviceDiscoveryAfterClearingTheCacheStarted() {

    }

    override fun newDeviceFound(node: LSSDPNodes) {

    }

    override fun deviceGotRemoved(ipaddress: String) {
        if (currentIpAddress != null && currentIpAddress!!.equals(ipaddress, ignoreCase = true)) {
            intentToHome(this)
        }
    }

    override fun messageRecieved(dataRecived: NettyData) {

        val packet = LUCIPacket(dataRecived.getMessage())

        if (packet.command == MIDCONST.SET_UI) {
            val message = String(packet.getpayload())
            LibreLogger.d(this, " message 42 recieved  $message")
//            parseJsonAndReflectInUI(message)
        }

    }


    /**
     * This function gets the Json string
     */
    private fun parseJsonAndReflectInUI(jsonStr: String?) {

        LibreLogger.d(this, "Json Recieved from remote device " + jsonStr!!)
        if (jsonStr != null) {
            try {
                runOnUiThread {
                    dismissDialog()
                    handler.removeMessages(NETWORK_TIMEOUT)
                }

                val root = JSONObject(jsonStr)
                val cmd_id = root.getInt(TAG_CMD_ID)
                LibreLogger.d(this, "Command Id$cmd_id")

                if (cmd_id == 3) {
                    /* This means user has selected the song to be playing and hence we will need to navigate
                     him to the Active scene list */
                    unRegisterForDeviceEvents()
                    openNowPlaying(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

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

        currentFragment = getCurrentFragment()
        val clickedDIDLObject = currentFragment?.getCurrentDIDLObjectList()?.get(clickedDIDLPosition)
        this@CTDMSBrowserActivityV2.position = clickedDIDLPosition

        Log.i(TAG, "handleDIDLObjectClick position:" + this@CTDMSBrowserActivityV2.position)

        et_search_media?.setText("")
        closeKeyboard(this@CTDMSBrowserActivityV2, currentFocus)

        if (clickedDIDLObject is Container) {
            didlObjectStack!!.push(clickedDIDLObject)
            Log.d("handleClick", "didlObjectStack = $didlObjectStack")
//            browse(clickedDIDLObject)

            /*Go to file listing screen*/
            startActivity(Intent(this,CTUpnpFileBrowserActivity::class.java).apply {
                putExtra(CURRENT_DEVICE_IP,currentIpAddress)
                putExtra(DEVICE_UDN,dmsDeviceUDN)
                putExtra(CLICKED_DIDL_ID,clickedDIDLObject?.id)
                putExtra(DIDL_TITLE,clickedDIDLObject?.title)
            })


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

        val intent = Intent(this@CTDMSBrowserActivityV2, CTNowPlayingActivity::class.java)
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

    inner class TabsViewPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): CTDMRBrowserFragmentV2 {
            when (tabTitles[position]) {
                ALBUMS -> {
                    if (fragmentMap[ALBUMS] == null) {
                        fragmentMap[ALBUMS] = CTDMRBrowserFragmentV2().apply {
                            arguments = Bundle().apply {
                                putString(MUSIC_TYPE, ALBUMS)
                            }
                        }
                    } else return fragmentMap[ALBUMS]!!
                }
                ARTISTS -> {
                    if (fragmentMap[ARTISTS] == null) {
                        fragmentMap[ARTISTS] = CTDMRBrowserFragmentV2().apply {
                            arguments = Bundle().apply {
                                putString(MUSIC_TYPE, ARTISTS)
                            }
                        }
                    } else return fragmentMap[ARTISTS]!!
                }

                SONGS -> {
                    if (fragmentMap[SONGS] == null) {
                        fragmentMap[SONGS] = CTDMRBrowserFragmentV2().apply {
                            arguments = Bundle().apply {
                                putString(MUSIC_TYPE, SONGS)
                            }
                        }
                    } else return fragmentMap[SONGS]!!
                }

                GENRES -> {
                    if (fragmentMap[GENRES] == null) {
                        fragmentMap[GENRES] = CTDMRBrowserFragmentV2().apply {
                            arguments = Bundle().apply {
                                putString(MUSIC_TYPE, GENRES)
                            }
                        }
                    } else return fragmentMap[GENRES]!!
                }
            }
            return fragmentMap[tabTitles[position]]!!
        }

        /*override fun getItemPosition(`object`: Any?): Int {
            return *//*super.getItemPosition(`object`)*//*PagerAdapter.POSITION_NONE
        }*/

        override fun destroyItem(container: ViewGroup?, position: Int, `object`: Any?) {
            super.destroyItem(container, position, `object`)
            fragmentMap.remove(tabTitles[position])
        }

        override fun getCount(): Int {
            return tabTitles.size
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return tabTitles[position]
        }
    }

    override fun onBackPressed() {
        unRegisterForDeviceEvents()
        finish()
    }

    fun getCurrentFragment(): CTDMRBrowserFragmentV2? {
        val currentFragmentIndex = view_pager_tabs?.currentItem
        if (currentFragmentIndex!! >= 0) {
            val fragment = supportFragmentManager?.fragments?.get(currentFragmentIndex) as CTDMRBrowserFragmentV2?
            if (fragment != null) {
                fragmentMap[tabTitles[currentFragmentIndex]] = fragment
            }
            return fragment
        }
        return null
    }
}
