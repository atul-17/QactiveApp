package com.cumulations.libreV2.activity

import android.os.Bundle
import android.view.View
import com.cumulations.libreV2.launchTheApp
import com.libre.R
import com.libre.Scanning.Constants
import com.libre.constants.LSSDPCONST
import com.libre.constants.LUCIMESSAGES
import com.libre.constants.MIDCONST
import com.libre.luci.LSSDPNodeDB
import com.libre.luci.LUCIControl
import kotlinx.android.synthetic.main.ct_activity_alexa_things_to_try.*


/**
 * Created by Amit on 12/14/2016.
 */

class CTAlexaThingsToTryActivity : CTDeviceDiscoveryActivity(), View.OnClickListener {

    private val deviceIp by lazy {
        intent?.getStringExtra(Constants.CURRENT_DEVICE_IP)
    }
    private val fromActivity by lazy {
        intent?.getStringExtra(Constants.FROM_ACTIVITY)
    }
    private val prevScreen by lazy {
        intent?.getStringExtra(Constants.PREV_SCREEN)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ct_activity_alexa_things_to_try)

        LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(deviceIp).apply {
            if (this.alexaRefreshToken.isNullOrEmpty()){
                tv_done?.text = getText(R.string.done)
            } else tv_done?.text = getText(R.string.logout)
        }

        iv_back.setOnClickListener(this)
        tv_done.setOnClickListener(this)
        ll_alexa_app.setOnClickListener(this)
        tv_alexa_app.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.tv_done ->{
                if(v.id == R.id.tv_done && tv_done?.text.toString() == getString(R.string.logout)){
                    /*Logout clicked*/
                    amazonLogout()
                } else {
                    handleBackPress()
                }
            }

            R.id.ll_alexa_app,R.id.tv_alexa_app -> launchTheApp(this,"com.amazon.dee.app")

            R.id.iv_back -> handleBackPress()
        }

    }

    private fun amazonLogout(){
        LUCIControl(deviceIp).SendCommand(MIDCONST.ALEXA_COMMAND.toInt(), LUCIMESSAGES.SIGN_OUT, LSSDPCONST.LUCI_SET)
        val mNode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(deviceIp)
        mNode?.alexaRefreshToken = ""

        try {
            Thread.sleep(500)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        handleBackPress()
    }

    private fun handleBackPress(){
        /*if (!fromActivity.isNullOrEmpty()) {
            if (fromActivity == CTAmazonLoginActivity::class.java.simpleName
                    || fromActivity == CTAlexaThingsToTryActivity::class.java.simpleName) {

                *//*startActivity(Intent(this@CTAlexaThingsToTryActivity, SourcesOptionActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(Constants.CURRENT_DEVICE_IP, deviceIp)
                })
                finish()*//*
            } else if (fromActivity == CTConnectingToMainNetwork::class.java.simpleName) {
                intentToHome(this)
            } else if (fromActivity == CTDeviceSettingsActivity::class.java.simpleName) {
                startActivity(Intent(this@CTAlexaThingsToTryActivity, CTDeviceSettingsActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(Constants.CURRENT_DEVICE_IP, deviceIp)
                })
                finish()
            }
        }*/

        if (!fromActivity.isNullOrEmpty() && fromActivity == CTConnectingToMainNetwork::class.java.simpleName) {
            intentToHome(this)
        } else onBackPressed()
    }

}
