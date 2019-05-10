package com.cumulations.libreV2.activity

import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.util.Log
import android.view.View
import com.cumulations.libreV2.AppConstants
import com.cumulations.libreV2.WifiUtil
import com.cumulations.libreV2.fragments.*
import com.cumulations.libreV2.removeShiftMode
import com.libre.LibreApplication
import com.libre.R
import com.libre.Scanning.Constants
import com.libre.Scanning.ScanningHandler
import com.libre.luci.LSSDPNodeDB
import com.libre.luci.LSSDPNodes
import com.libre.netty.LibreDeviceInteractionListner
import com.libre.netty.NettyData
import com.libre.util.LibreLogger
import kotlinx.android.synthetic.main.ct_activity_home_tabs.*


class CTHomeTabsActivity : CTDeviceDiscoveryActivity(),LibreDeviceInteractionListner {
    private var wifiUtil:WifiUtil? = null
    private var tabSelected: String = ""
    private var loadFragmentName:String? = null
    private var isDoubleTap: Boolean = false

    private val mTaskHandlerForSendingMSearch = Handler()
    private val mMyTaskRunnableForMSearch = Runnable {
        showLoader(false)
        val application = application as LibreApplication
        application.scanThread.UpdateNodes()

        if (LSSDPNodeDB.getInstance().GetDB().size <= 0) {
            openFragment(CTNoDeviceFragment::class.java.simpleName,animate = false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ct_activity_home_tabs)
        initViews()
        setListeners()

        wifiUtil = WifiUtil(this)
        if (!wifiUtil?.isWifiEnabled()!! && isNetworkOffCallBackEnabled) {
            openFragment(CTNoWifiFragment::class.java.simpleName,animate = false)
            return
        }

        loadFragmentName = intent?.getStringExtra(AppConstants.LOAD_FRAGMENT)
        if(loadFragmentName == null){
            loadFragmentName = if (LSSDPNodeDB.getInstance().GetDB().size > 0) {
                CTActiveDevicesFragment::class.java.simpleName
            } else {
                CTNoDeviceFragment::class.java.simpleName
            }
        }

        openFragment(loadFragmentName!!,animate = false)
    }

    private fun setListeners() {
        val bundle: Bundle? = Bundle()
        var fragmentToLoad: Fragment? = null
        bottom_navigation.setOnNavigationItemSelectedListener {

            when (it.itemId) {
                R.id.action_discover -> {

                    if (!wifiUtil?.isWifiEnabled()!! && isNetworkOffCallBackEnabled) {
                        removeAllFragments()
                        openFragment(CTNoWifiFragment::class.java.simpleName,animate = false)
                        return@setOnNavigationItemSelectedListener true
                    }

                    iv_refresh?.visibility = View.VISIBLE
                    refreshDevices()
                    return@setOnNavigationItemSelectedListener true
                }

                R.id.action_add -> {
                    iv_refresh?.visibility = View.GONE
                    fragmentToLoad = CTDeviceSetupInfoFragment()
                    fragmentToLoad?.arguments = bundle
                }

                R.id.action_tutorial -> {
                    iv_refresh?.visibility = View.GONE
                    fragmentToLoad = CTTutorialsFragment()
                }

                R.id.action_settings -> {
                    iv_refresh?.visibility = View.GONE
                    fragmentToLoad = CTSettingsFragment()
                }
            }

            Log.d("bottom nav clicked","clicked ${it.title}")
            if (fragmentToLoad == null)
                false
            else loadFragment(fragmentToLoad!!,animate = true)
        }

        iv_refresh?.setOnClickListener {
            refreshDevices()
        }
    }

    private fun initViews() {
        toolbar.title = ""
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        removeShiftMode(bottom_navigation)
    }

    private fun showLoader(show:Boolean){
        if (show) progress_bar.visibility = View.VISIBLE else progress_bar.visibility = View.GONE
    }

    private fun removeAllFragments(){
        for (fragment in supportFragmentManager.fragments) {
            supportFragmentManager.beginTransaction().remove(fragment).commit()
        }
    }

    fun refreshDevices() {
        LibreLogger.d(this, "Refresh Devices With hiding")
        showLoader(true)
//        fl_container.visibility = View.GONE
        removeAllFragments()
        val application = application as LibreApplication
        application.scanThread.UpdateNodes()
        /*Send m-search packet after 5 seconds*/
        mTaskHandlerForSendingMSearch.postDelayed(mMyTaskRunnableForMSearch, Constants.LOADING_TIMEOUT.toLong())
        showScreenAfterDelay()
    }

    private fun showScreenAfterDelay(){
        Handler().postDelayed({
            runOnUiThread {
                val sceneKeySet = ScanningHandler.getInstance().sceneObjectFromCentralRepo.keys.toTypedArray()
                LibreLogger.d(this,"sceneKeySet size = ${sceneKeySet.size}")
                if (/*sceneKeySet.isNotEmpty()*/LSSDPNodeDB.getInstance().GetDB().size > 0) {
                    openFragment(CTActiveDevicesFragment::class.java.simpleName,animate = false)
                } /*else openFragment(CTNoDeviceFragment::class.java.simpleName,animate = false)*/
            }
        },2000)
    }

    fun openFragment(fragmentClassName:String,animate:Boolean){
        val fragment: Fragment?
        iv_refresh?.visibility = View.VISIBLE
        when(fragmentClassName){
            CTNoDeviceFragment::class.java.simpleName -> {
                fragment = CTNoDeviceFragment()
                bottom_navigation.menu.getItem(0).isChecked = true
                loadFragment(fragment,animate)
            }

            CTActiveDevicesFragment::class.java.simpleName -> {
                fragment = CTActiveDevicesFragment()
                bottom_navigation.menu.getItem(0).isChecked = true
                loadFragment(fragment,animate)
            }

            CTDeviceSetupInfoFragment::class.java.simpleName -> {
                iv_refresh?.visibility = View.GONE
                fragment = CTDeviceSetupInfoFragment()
                bottom_navigation.menu.getItem(1).isChecked = true
                loadFragment(fragment,animate)
            }

            CTNoWifiFragment::class.java.simpleName -> {
                iv_refresh?.visibility = View.GONE
                fragment = CTNoWifiFragment()
                bottom_navigation.menu.getItem(0).isChecked = true
                loadFragment(fragment,animate)
            }
        }
    }

    private fun loadFragment(fragment: Fragment?,animate: Boolean): Boolean {
        //switching fragment
        if (fragment != null) {
            Log.d("loadFragment", fragment::class.java.simpleName)
            supportFragmentManager
                    .beginTransaction()
                    .apply {
                        if (animate) setCustomAnimations(R.anim.slide_in_right,R.anim.slide_out_left)
                        replace(R.id.fl_container, fragment)
                        commit()
                    }

//            fl_container.visibility = View.VISIBLE

            tabSelected = fragment::class.java.simpleName
            return true
        }
        return false
    }

    override fun wifiConnected(connected: Boolean) {
        if (connected){
            refreshDevices()
        } else
            openFragment(CTNoWifiFragment::class.java.simpleName,animate = false)
    }

    override fun deviceDiscoveryAfterClearingTheCacheStarted() {}

    override fun newDeviceFound(node: LSSDPNodes?) {
        LibreLogger.d(this,"newDeviceFound ${node?.friendlyname}")
        mTaskHandlerForSendingMSearch.removeCallbacks(mMyTaskRunnableForMSearch)
        val sceneKeySet = ScanningHandler.getInstance().sceneObjectFromCentralRepo.keys.toTypedArray()
        LibreLogger.d(this,"sceneKeySet size = ${sceneKeySet.size}")
        if (sceneKeySet.isNotEmpty()) {
            openFragment(CTActiveDevicesFragment::class.java.simpleName,animate = false)
        }
    }

    override fun deviceGotRemoved(ipaddress: String?) {
    }

    override fun messageRecieved(packet: NettyData?) {
    }

    override fun onBackPressed() {
        if (isDoubleTap) {
            ensureDMRPlaybackStopped()
            super.onBackPressed()
            killApp()
            return
        }
        showToast(R.string.doubleTapToExit)
        isDoubleTap = true
        Handler().postDelayed({ isDoubleTap = false }, 2000)
    }
}
