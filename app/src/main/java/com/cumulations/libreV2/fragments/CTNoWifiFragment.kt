package com.cumulations.libreV2.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cumulations.libreV2.AppConstants
import com.cumulations.libreV2.AppUtils
import com.cumulations.libreV2.activity.CTHomeTabsActivity
import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity
import com.libre.qactive.LibreApplication
import com.cumulations.libreV2.model.WifiConnection
import com.libre.qactive.Scanning.Constants
import com.libre.qactive.Scanning.ScanningHandler
import com.cumulations.libreV2.model.SceneObject
import com.libre.qactive.R
import com.libre.qactive.luci.LSSDPNodeDB
import com.libre.qactive.luci.LSSDPNodes
import com.libre.qactive.netty.LibreDeviceInteractionListner
import com.libre.qactive.netty.NettyData
import kotlinx.android.synthetic.main.ct_fragment_no_wifi.*

class CTNoWifiFragment: Fragment(),LibreDeviceInteractionListner,View.OnClickListener {
    private val mScanHandler = ScanningHandler.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater?.inflate(R.layout.ct_fragment_no_wifi,container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setListeners()
        if (activity!=null) {
            ((activity) as CTHomeTabsActivity).showLoader(false)
        }
    }

    private fun setListeners() {
//        iv_refresh.setOnClickListener(this)
        tv_wifi_settings?.setOnClickListener(this)
    }

    private fun initViews() {
//        toolbar.title = ""
    }

    override fun onResume() {
        super.onResume()
        (activity as CTDeviceDiscoveryActivity).registerForDeviceEvents(this)
        handler?.sendEmptyMessageDelayed(1, 3000)
    }

    override fun onClick(p0: View?) {
        when(p0?.id){
            R.id.iv_refresh -> (activity as CTHomeTabsActivity).refreshDevices()
            R.id.tv_wifi_settings -> {
                (activity as CTDeviceDiscoveryActivity).disableNetworkChangeCallBack()
                (activity as CTDeviceDiscoveryActivity).disableNetworkOffCallBack()

                WifiConnection.getInstance().mPreviousSSID = AppUtils.getConnectedSSID(activity!!)
                LibreApplication.activeSSID = WifiConnection.getInstance().mPreviousSSID

                startActivityForResult(Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                }, AppConstants.WIFI_SETTINGS_REQUEST_CODE)
            }
        }
    }

    @SuppressLint("HandlerLeak")
    internal var handler: Handler? = object : Handler() {

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (LSSDPNodeDB.getInstance().GetDB().size > 0) {
                (activity as CTHomeTabsActivity).openFragment(CTActiveDevicesFragment::class.java.simpleName,animate = true)
            }
            this.sendEmptyMessageDelayed(1, 3000)
        }
    }

    override fun deviceDiscoveryAfterClearingTheCacheStarted() {

    }

    override fun newDeviceFound(node: LSSDPNodes) {
        handler?.removeMessages(1)
        updateSceneObjectAndOpenDeviceList(node.ip)
    }

    /*this method will get called when we are getting master 103*/
    private fun updateSceneObjectAndOpenDeviceList(ipaddress: String) {

        val node = mScanHandler.getLSSDPNodeFromCentralDB(ipaddress)
        if (node != null) {
            if (!mScanHandler.isIpAvailableInCentralSceneRepo(ipaddress)) {
                val sceneObject = SceneObject(" ", node.friendlyname, 0f, node.ip)
                mScanHandler.putSceneObjectToCentralRepo(ipaddress, sceneObject)
                (activity as CTHomeTabsActivity).openFragment(CTActiveDevicesFragment::class.java.simpleName,animate = true)
            }
        }

    }

    override fun deviceGotRemoved(ipaddress: String) {

    }

    override fun messageRecieved(packet: NettyData) {

    }

    override fun onStop() {
        super.onStop()
        handler?.removeMessages(1)
        (activity as CTDeviceDiscoveryActivity).unRegisterForDeviceEvents()
    }
}
