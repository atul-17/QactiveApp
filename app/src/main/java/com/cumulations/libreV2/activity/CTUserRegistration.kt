package com.cumulations.libreV2.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.libre.qactive.R
import com.libre.qactive.Scanning.Constants
import com.libre.qactive.luci.LSSDPNodeDB
import kotlinx.android.synthetic.main.ct_activity_source_selection.*
import kotlinx.android.synthetic.main.ct_activity_user_registration.*
import kotlinx.android.synthetic.main.ct_activity_user_registration.iv_alexa_settings
import kotlinx.android.synthetic.main.ct_activity_user_registration.iv_back

class CTUserRegistration : CTDeviceDiscoveryActivity(){

    var currentIpAddress: String? = null

    var bundle = Bundle()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ct_activity_user_registration)


        bundle = intent.extras!!

        currentIpAddress = bundle.getString(Constants.CURRENT_DEVICE_IP)

        if (currentIpAddress!=null) {
            val lssdpNodes = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(currentIpAddress)

            if (lssdpNodes.getgCastVerision() != null) {
                //gcast != null -> hide alexa
                iv_alexa_settings.visibility = View.INVISIBLE
            } else {
                iv_alexa_settings.visibility = View.VISIBLE
            }

            iv_alexa_settings?.setOnClickListener {
                val mNode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(currentIpAddress)
                if (mNode?.alexaRefreshToken?.isEmpty()!!) {
                    startActivity(Intent(this@CTUserRegistration, CTAmazonLoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        putExtra(Constants.CURRENT_DEVICE_IP, currentIpAddress)
                        putExtra(Constants.FROM_ACTIVITY, CTMediaSourcesActivity::class.java.simpleName)
                    })
                } else {
                    startActivity(Intent(this@CTUserRegistration, CTAlexaThingsToTryActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        putExtra(Constants.CURRENT_DEVICE_IP, currentIpAddress)
                        putExtra(Constants.FROM_ACTIVITY, CTMediaSourcesActivity::class.java.simpleName)
                    })
                }
            }
        }else{
            iv_alexa_settings.visibility = View.INVISIBLE
        }


        tv_register_now.setOnClickListener {
            val intent = Intent(this@CTUserRegistration,CTQactiveRegDetailsActivity::class.java)
            val bundle = Bundle()
            bundle.putString(Constants.CURRENT_DEVICE_IP,currentIpAddress)
            intent.putExtras(bundle)
            startActivity(intent)

        }

        tv_later.setOnClickListener {
           intentToHome(this@CTUserRegistration)
        }

        tv_do_not_show_again.setOnClickListener {
            intentToHome(this@CTUserRegistration)
        }


        iv_back.setOnClickListener {
            onBackPressed()
        }


    }


}