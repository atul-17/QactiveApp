package com.cumulations.libreV2.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.libre.qactive.R
import com.libre.qactive.Scanning.Constants
import com.libre.qactive.luci.LSSDPNodeDB
import kotlinx.android.synthetic.main.ct_activity_firmware_update_notifications_screen_4_hours.*
import kotlinx.android.synthetic.main.ct_activity_firmware_update_notifications_screen_4_hours.iv_alexa_settings
import kotlinx.android.synthetic.main.ct_activity_firmware_update_notifications_screen_4_hours.iv_back
import kotlinx.android.synthetic.main.ct_activity_firmware_update_notifications_screen_4_hours.tv_device_name
import kotlinx.android.synthetic.main.ct_activity_source_selection.*


class CTFirmwareUpdateNotificationActivity4Hours : AppCompatActivity(){

    var bundle: Bundle? = Bundle()

    var deviceFriendlyName = ""

    var currentIpAddress = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ct_activity_firmware_update_notifications_screen_4_hours)



        bundle = intent.extras




        if (bundle != null) {
            deviceFriendlyName = bundle!!.getString("deviceFriendlyName", "")
            currentIpAddress = bundle!!.getString(Constants.CURRENT_DEVICE_IP,"")
        }


        val lssdpNodes = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(currentIpAddress)

        if (lssdpNodes.getgCastVerision() != null) {
            //gcast != null -> hide alexa
            iv_alexa_settings?.visibility = View.INVISIBLE
        }else{
            iv_alexa_settings?.visibility = View.VISIBLE
        }


        iv_alexa_settings?.setOnClickListener {
            val mNode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(currentIpAddress)
            if (mNode?.alexaRefreshToken?.isEmpty()!!) {
                startActivity(Intent(this@CTFirmwareUpdateNotificationActivity4Hours, CTAmazonLoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(Constants.CURRENT_DEVICE_IP, currentIpAddress)
                    putExtra(Constants.FROM_ACTIVITY, CTMediaSourcesActivity::class.java.simpleName)
                })
            } else {
                startActivity(Intent(this@CTFirmwareUpdateNotificationActivity4Hours, CTAlexaThingsToTryActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(Constants.CURRENT_DEVICE_IP, currentIpAddress)
                    putExtra(Constants.FROM_ACTIVITY, CTMediaSourcesActivity::class.java.simpleName)
                })
            }
        }


        tv_device_name.text = deviceFriendlyName

        iv_back.setOnClickListener {
            onBackPressed()
        }
    }
}