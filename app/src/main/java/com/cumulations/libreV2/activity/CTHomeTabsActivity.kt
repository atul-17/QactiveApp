package com.cumulations.libreV2.activity

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.Fragment
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.DrawableImageViewTarget
import com.bumptech.glide.request.target.Target
import com.cumulations.libreV2.AppConstants
import com.cumulations.libreV2.AppUtils
import com.cumulations.libreV2.WifiUtil
import com.cumulations.libreV2.fragments.*
import com.cumulations.libreV2.model.SceneObject
import com.cumulations.libreV2.removeShiftMode
import com.cumulations.libreV2.tcp_tunneling.TunnelingData
import com.cumulations.libreV2.tcp_tunneling.TunnelingFragmentListener
import com.libre.qactive.LibreApplication
import com.libre.qactive.R
import com.libre.qactive.Scanning.Constants
import com.libre.qactive.Scanning.ScanningHandler
import com.libre.qactive.luci.LSSDPNodeDB
import com.libre.qactive.luci.LSSDPNodes
import com.libre.qactive.netty.LibreDeviceInteractionListner
import com.libre.qactive.netty.NettyData
import com.libre.qactive.util.LibreLogger
import kotlinx.android.synthetic.main.ct_activity_home_tabs.*


class CTHomeTabsActivity : CTDeviceDiscoveryActivity(), LibreDeviceInteractionListner {
    private var wifiUtil: WifiUtil? = null
    private var tabSelected: String = ""
    private var loadFragmentName: String? = null
    private var isDoubleTap: Boolean = false

    private val mTaskHandlerForSendingMSearch = Handler()
    var ivProgressBarGif: AppCompatImageView? = null

    private val mMyTaskRunnableForMSearch = Runnable {
        showLoader(false)
        Log.d("atul_gif_loader", "false_mMyTaskRunnableForMSearch");
        val application = application as LibreApplication?
        application?.scanThread?.UpdateNodes()

        if (!wifiUtil?.isWifiOn()!!) {
            openFragment(CTNoWifiFragment::class.java.simpleName, animate = false)
            return@Runnable
        }

        if (LSSDPNodeDB.getInstance().GetDB().size <= 0) {
            openFragment(CTNoDeviceFragment::class.java.simpleName, animate = false)
        } else {
            openFragment(CTActiveDevicesFragment::class.java.simpleName, animate = false)
        }
    }

    private var isActivityVisible = true
    private var tunnelingFragmentListener: TunnelingFragmentListener? = null
    private var otherTabClicked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ct_activity_home_tabs)
        initViews()
        setListeners()

        wifiUtil = WifiUtil(this)
        if (!wifiUtil?.isWifiOn()!!) {
            openFragment(CTNoWifiFragment::class.java.simpleName, animate = false)
            return
        }

        loadFragmentName = intent?.getStringExtra(AppConstants.LOAD_FRAGMENT)
        if (loadFragmentName == null) {
            loadFragmentName = if (LSSDPNodeDB.getInstance().GetDB().size > 0) {
                CTActiveDevicesFragment::class.java.simpleName
            } else {
                CTNoDeviceFragment::class.java.simpleName
            }
        }

        openFragment(loadFragmentName!!, animate = false)


    }

    @SuppressLint("HardwareIds")
    override fun onStart() {
        super.onStart()
        isActivityVisible = true
        LibreLogger.d(this, "onStart, bottom_navigation?.selectedItemId = ${bottom_navigation.selectedItemId}")
        LibreLogger.d(this, "onStart, tabSelected = $tabSelected")

        when {
            LibreApplication.isSacFlowStarted -> {
                bottom_navigation?.selectedItemId = R.id.action_discover
                LibreApplication.isSacFlowStarted = false
            }
            tabSelected == CTActiveDevicesFragment::class.java.simpleName -> {
                if (supportFragmentManager?.findFragmentByTag(tabSelected) == null)
                    return
                val ctActiveDevicesFragment = supportFragmentManager?.findFragmentByTag(tabSelected) as CTActiveDevicesFragment
                ctActiveDevicesFragment?.updateFromCentralRepositryDeviceList()

//                toggleStopAllButtonVisibility()
            }
            tabSelected != CTDeviceSetupInfoFragment::class.java.simpleName -> bottom_navigation?.selectedItemId = R.id.action_discover
        }

        checkLocationPermission()
    }

    fun toggleStopAllButtonVisibility() {
        if (AppUtils.isAnyDevicePlaying())
            iv_stop_all?.visibility = View.VISIBLE
        else {
            iv_stop_all?.visibility = View.GONE
        }
    }

    private fun setListeners() {
        val bundle: Bundle? = Bundle()
        var fragmentToLoad: Fragment? = null
        bottom_navigation.setOnNavigationItemSelectedListener {

            when (it.itemId) {
                R.id.action_discover -> {

                    if (!wifiUtil?.isWifiOn()!! /*&& isNetworkOffCallBackEnabled*/) {
                        openFragment(CTNoWifiFragment::class.java.simpleName, animate = false)
                        return@setOnNavigationItemSelectedListener true
                    }

                    iv_refresh?.visibility = View.VISIBLE
                    otherTabClicked = false
                    refreshDevices()
                    return@setOnNavigationItemSelectedListener true
                }

                R.id.action_add -> {
                    otherTabClicked = true
                    iv_refresh?.visibility = View.GONE
                    fragmentToLoad = CTDeviceSetupInfoFragment()
                    fragmentToLoad?.arguments = bundle
                }

                R.id.action_tutorial -> {
                    otherTabClicked = true
                    iv_refresh?.visibility = View.GONE
                    fragmentToLoad = CTTutorialsFragment()
                }

                R.id.action_settings -> {
                    otherTabClicked = true
                    iv_refresh?.visibility = View.GONE
                    fragmentToLoad = CTSettingsFragment()
                }
            }

//            bottom_navigation?.selectedItemId = it.itemId

            mTaskHandlerForSendingMSearch.removeCallbacks(mMyTaskRunnableForMSearch)
            Log.d("bottom nav clicked", "clicked ${it.title}")
            if (fragmentToLoad == null) {
                false
            } else {
                openFragment(fragmentToLoad!!::class.java.simpleName, animate = true)
                true
            }
        }

        iv_refresh?.setOnClickListener {
            refreshDevices()
        }

        iv_stop_all?.setOnClickListener {
            AppUtils.stopAllDevicesPlaying()
        }
    }

    private fun initViews() {
        ivProgressBarGif = findViewById(R.id.iv_progress_bar_gif)
        toolbar.title = ""
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        removeShiftMode(bottom_navigation)
        Glide.with(this)
                .load(R.raw.android_spinner_white)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into(ivProgressBarGif!!);
    }

    fun showLoader(show: Boolean) {
        if (show) {
            Glide.with(this)
                    .load(R.raw.android_spinner_white)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .into(ivProgressBarGif!!);
            ivProgressBarGif?.visibility = View.VISIBLE
        } else {
            ivProgressBarGif?.visibility = View.GONE
        }
    }

    private fun removeAllFragments() {
        for (fragment in supportFragmentManager.fragments) {
            try {
                supportFragmentManager.beginTransaction().remove(fragment).commit()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun refreshDevices() {
        LibreLogger.d(this, "Refresh Devices")
        showLoader(true)
//        //fl_container.visibility = View.GONE
        removeAllFragments()
        clearBatteryInfoForDevices()
        libreApplication?.scanThread?.UpdateNodes()
        /*Send m-search packet after 5 seconds*/
        mTaskHandlerForSendingMSearch.postDelayed(mMyTaskRunnableForMSearch, Constants.LOADING_TIMEOUT.toLong())
        showScreenAfterDelay()
    }

    private fun showScreenAfterDelay() {
        Handler().postDelayed({
            if (otherTabClicked)
                return@postDelayed

            runOnUiThread {
                val sceneKeySet = ScanningHandler.getInstance().sceneObjectMapFromRepo.keys.toTypedArray()
                LibreLogger.d(this, "showScreenAfterDelay, sceneKeySet size = ${sceneKeySet.size}")
                if (LSSDPNodeDB.getInstance().GetDB().size > 0) {
                    openFragment(CTActiveDevicesFragment::class.java.simpleName, animate = false)
                } /*else {
                    openFragment(CTNoDeviceFragment::class.java.simpleName,animate = false)
                }*/
            }
        }, 2000)
    }

    fun openFragment(fragmentClassName: String, animate: Boolean) {
        var fragment: Fragment? = null
        iv_refresh?.visibility = View.VISIBLE
        when (fragmentClassName) {
            CTNoDeviceFragment::class.java.simpleName -> {
                fragment = CTNoDeviceFragment()
                bottom_navigation.menu.getItem(0).isChecked = true
            }

            CTActiveDevicesFragment::class.java.simpleName -> {
                fragment = CTActiveDevicesFragment()
                bottom_navigation.menu.getItem(0).isChecked = true
            }

            CTDeviceSetupInfoFragment::class.java.simpleName -> {
                iv_refresh?.visibility = View.GONE
                fragment = CTDeviceSetupInfoFragment()
                bottom_navigation.menu.getItem(1).isChecked = true
            }

            CTNoWifiFragment::class.java.simpleName -> {
                iv_refresh?.visibility = View.GONE
                fragment = CTNoWifiFragment()
                bottom_navigation.menu.getItem(0).isChecked = true
            }

            CTTutorialsFragment::class.java.simpleName -> {
                iv_refresh?.visibility = View.GONE
                fragment = CTTutorialsFragment()
                bottom_navigation.menu.getItem(2).isChecked = true
            }

            CTSettingsFragment::class.java.simpleName -> {
                iv_refresh?.visibility = View.GONE
                fragment = CTSettingsFragment()
                bottom_navigation.menu.getItem(3).isChecked = true
            }
        }
        loadFragment(fragment, animate)
    }

    private fun loadFragment(fragment: Fragment?, animate: Boolean): Boolean {
        //switching fragment
        if (fragment != null && isActivityVisible) {
            Log.d("loadFragment", fragment::class.java.simpleName)
            try {
                supportFragmentManager
                        .beginTransaction()
                        .apply {
                            if (animate) setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                            replace(R.id.fl_container, fragment, fragment::class.java.simpleName)
                            commit()
                        }
                tabSelected = fragment::class.java.simpleName
            } catch (e: Exception) {
                e.printStackTrace()
                LibreLogger.d(this, "loadFragment exception ${e.message}")
            }

//            fl_container.visibility = View.VISIBLE
            return true
        }
        return false
    }

    override fun wifiConnected(connected: Boolean) {
        LibreLogger.d(this, "wifiConnected, home $connected")
        /*making method to be called in parent activity as well*/
        super.wifiConnected(connected)
        /*Avoid changing when activity is not visible i.e when user goes to wifi settings
        * and stays in that screen for a while*/
        if (!isActivityVisible || tabSelected == CTDeviceSetupInfoFragment::class.java.simpleName || LibreApplication.isSacFlowStarted)
            return
        if (connected) {
            refreshDevices()
        } else {
            openFragment(CTNoWifiFragment::class.java.simpleName, animate = false)
        }
    }

    override fun deviceDiscoveryAfterClearingTheCacheStarted() {}

    override fun newDeviceFound(node: LSSDPNodes?) {
        LibreLogger.d(this, "newDeviceFound ${node?.friendlyname}")
        mTaskHandlerForSendingMSearch.removeCallbacks(mMyTaskRunnableForMSearch)
        val sceneKeySet = ScanningHandler.getInstance().sceneObjectMapFromRepo.keys.toTypedArray()
        LibreLogger.d(this, "sceneKeySet size = ${sceneKeySet.size}")
        if (/*sceneKeySet.isNotEmpty()*/LSSDPNodeDB.getInstance().GetDB().size > 0) {
            openFragment(CTActiveDevicesFragment::class.java.simpleName, animate = false)
        }
    }

    override fun deviceGotRemoved(ipaddress: String?) {
    }

    override fun messageRecieved(packet: NettyData?) {
    }

    override fun onStop() {
        super.onStop()
        isActivityVisible = false
    }

    override fun onBackPressed() {
        if (isDoubleTap) {
            ensureDMRPlaybackStopped()
            killApp()
            super.onBackPressed()
            return
        }
        showToast(R.string.doubleTapToExit)
        isDoubleTap = true
        Handler().postDelayed({ isDoubleTap = false }, 2000)
    }

    fun setTunnelFragmentListener(tunnelingFragmentListener: TunnelingFragmentListener) {
        this.tunnelingFragmentListener = tunnelingFragmentListener
    }

    fun removeTunnelFragmentListener() {
        tunnelingFragmentListener = null
    }

    override fun tunnelDataReceived(tunnelingData: TunnelingData) {
        super.tunnelDataReceived(tunnelingData)
        tunnelingFragmentListener?.onFragmentTunnelDataReceived(tunnelingData)
    }

    private fun clearBatteryInfoForDevices() {
        ScanningHandler.getInstance().sceneObjectMapFromRepo.forEach { (ip: String?, sceneObject: SceneObject?) ->
            LibreLogger.d(this, "clearBatteryInfoForDevices device ${sceneObject.sceneName}")
            sceneObject?.clearBatteryStats()
        }
    }
}
