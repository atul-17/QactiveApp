package com.cumulations.libreV2.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cumulations.libreV2.AppUtils
import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity
import com.cumulations.libreV2.activity.CTHomeTabsActivity
import com.cumulations.libreV2.adapter.CTDeviceListAdapter
import com.cumulations.libreV2.model.SceneObject
import com.cumulations.libreV2.tcp_tunneling.TCPTunnelPacket
import com.cumulations.libreV2.tcp_tunneling.TunnelingControl
import com.cumulations.libreV2.tcp_tunneling.TunnelingData
import com.cumulations.libreV2.tcp_tunneling.TunnelingFragmentListener
import com.libre.LErrorHandeling.LibreError
import com.libre.LibreApplication
import com.libre.R
import com.libre.Scanning.Constants
import com.libre.Scanning.Constants.*
import com.libre.Scanning.ScanningHandler
import com.libre.constants.LUCIMESSAGES
import com.libre.constants.MIDCONST
import com.libre.luci.LSSDPNodeDB
import com.libre.luci.LSSDPNodes
import com.libre.luci.LUCIPacket
import com.libre.netty.LibreDeviceInteractionListner
import com.libre.netty.NettyData
import com.libre.util.LibreLogger
import com.libre.util.PicassoTrustCertificates
import kotlinx.android.synthetic.main.ct_fragment_discovery_list.*
import org.json.JSONObject

class CTActiveDevicesFragment:Fragment(),LibreDeviceInteractionListner,View.OnClickListener, TunnelingFragmentListener {
    private val mScanHandler = ScanningHandler.getInstance()
    private lateinit var deviceListAdapter:CTDeviceListAdapter

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater?.inflate(R.layout.ct_fragment_discovery_list,container,false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setListeners(view)
    }

    private fun setListeners(view: View?) {
//        iv_refresh?.setOnClickListener(this)
    }

    private fun initViews(view: View?) {
        val nodeDB = LSSDPNodeDB.getInstance()
        if (nodeDB.GetDB().size <= 0) {
            (activity as CTHomeTabsActivity).openFragment(CTNoDeviceFragment::class.java.simpleName,animate = true)
        }

//        toolbar.title = ""

        deviceListAdapter = CTDeviceListAdapter(activity)
        rv_device_list?.layoutManager = LinearLayoutManager(activity)
        rv_device_list?.adapter = deviceListAdapter
    }

    override fun onResume() {
        super.onResume()

        deviceListAdapter.clear()
        (activity as CTDeviceDiscoveryActivity).registerForDeviceEvents(this)
        (activity as CTHomeTabsActivity).setTunnelFragmentListener(this)

        updateFromCentralRepositryDeviceList()

        /*val sceneKeySet = mScanHandler.sceneObjectFromCentralRepo.keys.toTypedArray()
        if (sceneKeySet.isEmpty()) {
            refreshDevices()
        }*/
    }

    fun refreshDeviceListWithUpdatedScenes(){

    }

    override fun onClick(p0: View?) {
        when(p0?.id){
            R.id.iv_refresh -> {
                handler.removeCallbacksAndMessages(null)
                refreshDevices()
            }
        }
    }

    private fun showLoader(show:Boolean){
//        if (show) progress_bar.visibility = View.VISIBLE else progress_bar.visibility = View.GONE
    }

    private val mTaskHandlerForSendingMSearch = Handler()
    private val mMyTaskRunnableForMSearch = object : Runnable {
        override fun run() {
            showLoader(false)
            LibreLogger.d(this, "My task is Sending 1 Minute Once M-Search")
            val application = activity.application as LibreApplication
            application.scanThread.UpdateNodes()
        }
    }

    internal var handler: Handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {

            if (msg.what == PREPARATION_INIT) {
                val ipaddress = msg.data.getString("ipAddress")
                triggerTimeOutHandlerForIpAddress(ipaddress)
            }
            if (msg.what == PREPARATION_COMPLETED) {
                val ipaddress = msg.data.getString("ipAddress")
                removeTimeOutTriggerForIpaddress(ipaddress)
            }
        }
    }

    private fun removeTimeOutTriggerForIpaddress(ipAddress: String) {

        handler.removeMessages(PREPARATION_TIMEOUT_CONST, ipAddress)
        val mSceneObject = mScanHandler.getSceneObjectFromCentralRepo(ipAddress)
        if (mSceneObject != null) {
            mSceneObject.preparingState = SceneObject.PREPARING_STATE.PREPARING_SUCCESS
        }
        deviceListAdapter.notifyDataSetChanged()
    }

    private fun triggerTimeOutHandlerForIpAddress(ipAddress: String) {
        val timeOutMessage = Message()
        /* Added by Praveen to address the crash for PREPARATION_TIMEOUT_CONST */
        val data = Bundle()
        data.putString("ipAddress", ipAddress)
        timeOutMessage.data = data

        timeOutMessage.obj = ipAddress
        timeOutMessage.what = PREPARATION_TIMEOUT_CONST
        handler.sendMessageDelayed(timeOutMessage, Constants.PREPARATION_TIMEOUT.toLong())

        val mSceneObject = mScanHandler.getSceneObjectFromCentralRepo(ipAddress)
        if (mSceneObject != null) {
            mSceneObject.preparingState = SceneObject.PREPARING_STATE.PREPARING_INITIATED
        }
        deviceListAdapter.notifyDataSetChanged()
    }

    private var mDeviceFound: Boolean = false
    private var mMasterFound: Boolean = false

    fun updateFromCentralRepositryDeviceList() {
        var sceneKeySet = mScanHandler.sceneObjectFromCentralRepo.keys.toTypedArray()
        LibreLogger.d(this, "Master is Getting Added T Active Scenes Adapter" + mScanHandler.sceneObjectFromCentralRepo.keys)
        /* Fix For If Device is Available But SceneObject is Not Available in the Network */
        if (sceneKeySet.isEmpty()) {
            if (LSSDPNodeDB.getInstance().isDeviceInNodeRepo) {
                for (nodes in LSSDPNodeDB.getInstance().GetDB()) {
                    if (!ScanningHandler.getInstance().isIpAvailableInCentralSceneRepo(nodes.ip)) {
                        val sceneObject = SceneObject(" ", nodes.friendlyname, 0f, nodes.ip)
                        ScanningHandler.getInstance().putSceneObjectToCentralRepo(nodes.ip, sceneObject)
                        LibreLogger.d(this, "Device is not available in Central Repo So Created SceneObject " + nodes.ip)
                    }
                }
            }
        }

        sceneKeySet = mScanHandler.sceneObjectFromCentralRepo.keys.toTypedArray()

        for (sceneIp in sceneKeySet) {
            deviceListAdapter.addDeviceToList(mScanHandler.sceneObjectFromCentralRepo[sceneIp])
            (activity as CTDeviceDiscoveryActivity).requestLuciUpdates(sceneIp)
            /*Get Tunneling Data*/
            TunnelingControl(sceneIp).sendDataModeCommand()
        }

    }

    override fun deviceDiscoveryAfterClearingTheCacheStarted() {

    }

    override fun newDeviceFound(node: LSSDPNodes) {
        showLoader(false)

        Log.d("newDeviceFound", "device found ip ${node.ip} Device State ${node.deviceState}")
        mDeviceFound = true

        (activity as CTDeviceDiscoveryActivity).requestLuciUpdates(node.ip)

        val sceneObject = SceneObject(" ", node.friendlyname, 0f, node.ip)
        LibreLogger.d(this, "New device is found with the ip address = " + node.friendlyname)
        if (!mScanHandler.isIpAvailableInCentralSceneRepo(node.ip)) {
            mScanHandler.putSceneObjectToCentralRepo(node.ip, sceneObject)
        } else {
            val repoSceneObject = mScanHandler.getSceneObjectFromCentralRepo(node.ip)
            if (repoSceneObject != null)
                sceneObject.sceneName = repoSceneObject.sceneName
        }

        deviceListAdapter?.addDeviceToList(sceneObject)
    }

    private fun refreshDevices() {
        LibreLogger.d(this, "Refresh Devices With clearing")
        showLoader(true)
        val application = activity.application as LibreApplication
        application.scanThread.UpdateNodes()
        /*Start m-search thread after 10 seconds*/
        mTaskHandlerForSendingMSearch.postDelayed(mMyTaskRunnableForMSearch, Constants.REFRESH_DEVICE_TIMEOUT)
    }

    override fun deviceGotRemoved(ipaddress: String) {
        LibreLogger.d(this, "Device is Removed with the ip address = $ipaddress")

        val sceneObject = deviceListAdapter.getDeviceSceneFromAdapter(ipaddress)
        if (sceneObject != null) {
            deviceListAdapter.removeDeviceFromList(sceneObject)
            LibreLogger.d(this, "" + sceneObject!!.sceneName)

            if (deviceListAdapter.itemCount == 0) {
                refreshDevices()
            }
        }

        if (LSSDPNodeDB.getInstance().GetDB().size <= 0) {
            (activity as CTHomeTabsActivity).openFragment(CTNoDeviceFragment::class.java.simpleName,animate = true)
        }
    }

    override fun messageRecieved(nettyData: NettyData) {
        LibreLogger.d(this, "messageRecieved device " + nettyData.getRemotedeviceIp())

        var sceneObject: SceneObject? = deviceListAdapter.getDeviceSceneFromAdapter(nettyData.getRemotedeviceIp())
        if (sceneObject == null) {
            sceneObject = mScanHandler.getSceneObjectFromCentralRepo(nettyData.getRemotedeviceIp())
        }

        val luciPacket = LUCIPacket(nettyData.getMessage())
        val msg = String(luciPacket.payload)

        when(luciPacket.command){
            MIDCONST.MID_DEVICE_STATE_ACK -> {
                try {
                    (activity as CTDeviceDiscoveryActivity).requestLuciUpdates(nettyData.remotedeviceIp)
                } catch (e: Exception) {
                    e.printStackTrace()
                }


                LibreLogger.d(this, "A device became master found with the ip addrs = " + nettyData.getRemotedeviceIp())
                val node = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(nettyData.getRemotedeviceIp())

                val newSceneObject: SceneObject
                if (node != null) {
                    newSceneObject = SceneObject(" ", node.friendlyname, 0f, nettyData.getRemotedeviceIp())
                    LibreLogger.d(this, "device became master its name  = " + node.friendlyname)
                } else {
                    newSceneObject = SceneObject(" ", "", 0f, nettyData.getRemotedeviceIp())
                }

                if (node != null && !mScanHandler.isIpAvailableInCentralSceneRepo(node.ip)) {
                    mScanHandler.putSceneObjectToCentralRepo(node.ip, newSceneObject)
                }

                deviceListAdapter.addDeviceToList(newSceneObject)
                val mMasterIpKeySet = mScanHandler.sceneObjectFromCentralRepo.keys.toTypedArray()
                if (mMasterIpKeySet.isEmpty()) {
                    LibreLogger.d(this, "Handle the case when there is no scene object remaining in the UI" + nettyData.getRemotedeviceIp())
                    refreshDevices()
                }
            }

            MIDCONST.MID_SCENE_NAME -> {
                /* if command Type 1 , then Scene Name information will be come in the same packet
                if command type 2 and command status is 1 , then data will be empty., at that time
                we should not update the value .*/
                if (luciPacket.commandStatus == 1 && luciPacket.commandType == 2) {
                    return
                }

                //int duration = Integer.parseInt(message);
                try {
                    sceneObject?.sceneName = msg// = duration/60000.0f;
                    LibreLogger.d(this, "Scene Name " + sceneObject?.sceneName)
                    mScanHandler.putSceneObjectToCentralRepo(nettyData.getRemotedeviceIp(), sceneObject)
                    deviceListAdapter.addDeviceToList(sceneObject)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            MIDCONST.MID_PLAYTIME.toInt() -> {
                if (msg.isEmpty()) return
                /* This message box indicates the current playing status of the scene, information like current seek position*/
                var duration = 0
                try {
                    duration = Integer.parseInt(msg)
                } catch (e: Exception) {
                    e.printStackTrace()
                    LibreLogger.d(this, "Handling the app crash for 49 message box value sent =$duration")
                }

                sceneObject?.currentPlaybackSeekPosition = duration.toFloat()// = duration/60000.0f;
                LibreLogger.d(this, "Recieved the current Seek position to be " + sceneObject?.currentPlaybackSeekPosition)
                mScanHandler.putSceneObjectToCentralRepo(nettyData.getRemotedeviceIp(), sceneObject)
                deviceListAdapter.addDeviceToList(sceneObject)
            }

            MIDCONST.VOLUME_CONTROL -> {
                /*this message box is to get volume*/
                try {
                    val duration = Integer.parseInt(msg)
                    Log.e("VOLUME_CONTROL","value = $duration")
                    if (sceneObject?.volumeValueInPercentage != duration) {
                        sceneObject?.volumeValueInPercentage = duration
                        mScanHandler.putSceneObjectToCentralRepo(nettyData.getRemotedeviceIp(), sceneObject)
                        LibreLogger.d(this, "Recieved the current volume to be" + sceneObject?.volumeValueInPercentage)
                        deviceListAdapter.addDeviceToList(sceneObject)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            MIDCONST.MID_CURRENT_SOURCE.toInt() -> {
                /*this MB to get current sources*/
                try {
                    LibreLogger.d(this, "Recieved the current source as  " + sceneObject?.currentSource)
                    val mSource = Integer.parseInt(msg)
                    sceneObject?.currentSource = mSource
                    mScanHandler.putSceneObjectToCentralRepo(nettyData.getRemotedeviceIp(), sceneObject)
                    deviceListAdapter.addDeviceToList(sceneObject)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            MIDCONST.SET_UI -> {
                try {

                    LibreLogger.d(this, "MB : 42, msg = $msg")
                    val root = JSONObject(msg)
                    val cmdId = root.getInt(LUCIMESSAGES.TAG_CMD_ID)
                    val window = root.getJSONObject(LUCIMESSAGES.ALEXA_KEY_WINDOW_CONTENT)
                    LibreLogger.d(this, "PLAY JSON is \n= $msg")


                    if (cmdId == 3) {

                        val handlerMessage = Message()
                        handlerMessage.what = PREPARATION_COMPLETED
                        val data = Bundle()
                        data.putString("ipAddress", nettyData.getRemotedeviceIp())
                        handlerMessage.data = data
                        handler.sendMessage(handlerMessage)
                        deviceListAdapter.notifyDataSetChanged()

                        sceneObject = AppUtils.updateSceneObjectWithPlayJsonWindow(window,sceneObject!!)

                        /* this is done to avoid  image refresh everytime the 42 message is recieved and the song playing back is the same */
                        LibreLogger.d(this, "Invalidating the scene name for  = " + sceneObject?.ipAddress + "/" + "coverart.jpg")

                        LibreLogger.d(this, "Recieved the scene details as trackname = " + sceneObject?.trackName + ": " + sceneObject?.currentPlaybackSeekPosition)
                        LibreLogger.d(this, "Recieved the scene details as zoneVolume For checking  = " + sceneObject?.getvolumeZoneInPercentage())

                        mScanHandler.putSceneObjectToCentralRepo(nettyData.getRemotedeviceIp(), sceneObject)
                        deviceListAdapter.addDeviceToList(sceneObject)
                    }


                } catch (e: Exception) {
                    e.printStackTrace()

                }

            }

            MIDCONST.MID_CURRENT_PLAY_STATE.toInt() -> {
                LibreLogger.d(this, "MB : 51, msg = $msg")
                if (msg.isNotEmpty()) {

                    val playstatus = Integer.parseInt(msg)
                    val mGcastCheckNode = mScanHandler.getLSSDPNodeFromCentralDB(nettyData.getRemotedeviceIp())

                    val handlerMsg = Message()

                    if (sceneObject?.playstatus != playstatus) {
                        sceneObject?.playstatus = playstatus
                        mScanHandler.putSceneObjectToCentralRepo(nettyData.getRemotedeviceIp(), sceneObject)
                        deviceListAdapter.addDeviceToList(sceneObject)
                    }

                    if (sceneObject?.currentSource != Constants.BT_SOURCE
                            && mGcastCheckNode != null
                            && mGcastCheckNode.currentSource != MIDCONST.GCAST_SOURCE) {

                        if (sceneObject?.playstatus == SceneObject.CURRENTLY_PLAYING) {
                            LibreLogger.d(this, "playing state is received")

                            handlerMsg.what = PREPARATION_COMPLETED
                            val data = Bundle()
                            data.putString("ipAddress", nettyData.getRemotedeviceIp())
                            handlerMsg.data = data
                            handler.sendMessage(handlerMsg)
                        } else if (sceneObject?.playstatus == SceneObject.CURRENTLY_PAUSED) {
                            LibreLogger.d(this, "Pause state is received")
                        } else {
                            /*initiating progress loader*/
                            if (sceneObject?.playstatus == SceneObject.CURRENTLY_STOPPED
                                    && sceneObject.currentSource != Constants.NO_SOURCE) {
                                handlerMsg.what = PREPARATION_INIT
                                val data = Bundle()
                                data.putString("ipAddress", nettyData.getRemotedeviceIp())
                                handlerMsg.data = data
                                handler.sendMessage(handlerMsg)
                                LibreLogger.d(this, "Stop state is received")
                            }

                        }
                        deviceListAdapter.notifyDataSetChanged()
                    }
                }
            }

            MIDCONST.MID_DEVICE_ALERT_STATUS.toInt() -> {
                LibreLogger.d(this, " message 54 recieved  $msg")
                try {
                    var error: LibreError? = null
                    when {
                        msg.contains(Constants.FAIL) -> error = LibreError(nettyData.getRemotedeviceIp(), Constants.FAIL_ALERT_TEXT)
                        msg.contains(Constants.SUCCESS) -> {
                            showLoader(false)

                            val handlerMsg = Message()
                            handlerMsg.what = PREPARATION_COMPLETED
                            val data = Bundle()
                            data.putString("ipAddress", nettyData.getRemotedeviceIp())
                            handlerMsg.data = data
                            handler.sendMessage(handlerMsg)


                        }
                        msg.contains(Constants.NO_URL) -> error = LibreError(nettyData.getRemotedeviceIp(), Constants.NO_URL_ALERT_TEXT)
                        msg.contains(Constants.NO_PREV_SONG) -> error = LibreError(nettyData.getRemotedeviceIp(), resources.getString(R.string.NO_PREV_SONG_ALERT_TEXT))
                        msg.contains(Constants.NO_NEXT_SONG) -> error = LibreError(nettyData.getRemotedeviceIp(), resources.getString(R.string.NO_NEXT_SONG_ALERT_TEXT))
                        msg.contains(Constants.DMR_SONG_UNSUPPORTED) -> error = LibreError(nettyData.getRemotedeviceIp(), resources.getString(R.string.SONG_NOT_SUPPORTED))
                    }
                    PicassoTrustCertificates.getInstance(activity).invalidate(sceneObject?.album_art)
                    showLoader(false)

                    if (error != null)
                        (activity as CTDeviceDiscoveryActivity).showErrorMessage(error)

                } catch (e: Exception) {
                    Log.e("exception","MB 54 ${e.message}")
                    e.printStackTrace()
                }

            }

            MIDCONST.MID_MIC -> {
                if (msg.contains("BLOCKED",true)) {
                    deviceListAdapter.audioRecordUtil?.stopRecording()
                    (activity as CTDeviceDiscoveryActivity).showToast(getString(R.string.deviceMicOn))
                    sceneObject?.isAlexaBtnLongPressed = false
                    /*updating boolean status to repo as well*/
                    ScanningHandler.getInstance().putSceneObjectToCentralRepo(sceneObject?.ipAddress,sceneObject)
                    deviceListAdapter?.addDeviceToList(sceneObject)
                }
            }

            MIDCONST.MID_ENV_READ -> {
                if (msg.contains("AlexaRefreshToken")) {
                    deviceListAdapter?.addDeviceToList(sceneObject)
                }
            }
        }
    }

    private fun clearScenePreparationFlags() {
        val mSceneList = mScanHandler.sceneObjectFromCentralRepo
        for (masterIp in mSceneList.keys) {
            val sceneObject = mScanHandler.getSceneObjectFromCentralRepo(masterIp)
            if (sceneObject != null)
                sceneObject.preparingState = SceneObject.PREPARING_STATE.PREPARING_SUCCESS
        }
    }

    override fun onDestroyView() {
        clearScenePreparationFlags()
        (activity as CTDeviceDiscoveryActivity).libreApplication.unregisterMicException()
        handler.removeCallbacksAndMessages(null)
        mTaskHandlerForSendingMSearch.removeCallbacks(mMyTaskRunnableForMSearch)
        (activity as CTDeviceDiscoveryActivity).unRegisterForDeviceEvents()
        (activity as CTHomeTabsActivity).removeTunnelFragmentListener()
        super.onDestroyView()
    }

    override fun onTunnelDataReceived(tunnelingData: TunnelingData?) {
        LibreLogger.d(this,"onTunnelDataReceived, ip = ${tunnelingData?.remoteClientIp}")
        if (tunnelingData?.remoteMessage?.size!! >= 24) {
            val tcpTunnelPacket = TCPTunnelPacket(tunnelingData.remoteMessage)

            val sceneObject = ScanningHandler.getInstance().sceneObjectFromCentralRepo[tunnelingData.remoteClientIp]
            /*if (tcpTunnelPacket.volume>=0){
                sceneObject?.volumeValueInPercentage = tcpTunnelPacket.volume
                LibreApplication.INDIVIDUAL_VOLUME_MAP[sceneObject?.ipAddress] = sceneObject?.volumeValueInPercentage
            }*/

            if (tcpTunnelPacket.currentSource == Constants.BT_SOURCE
                    || tcpTunnelPacket.currentSource == Constants.AUX_SOURCE
                    || tcpTunnelPacket.currentSource == Constants.NO_SOURCE){
                sceneObject?.currentSource = tcpTunnelPacket.currentSource
            }
            mScanHandler.putSceneObjectToCentralRepo(sceneObject?.ipAddress, sceneObject)
            deviceListAdapter?.addDeviceToList(sceneObject)
        }
    }
}