package com.cumulations.libreV2.activity

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.appcompat.app.AppCompatActivity
import com.libre.qactive.R
import com.libre.qactive.Scanning.Constants
import com.libre.qactive.luci.LSSDPNodeDB
import kotlinx.android.synthetic.main.ct_activity_media_sources.*
import kotlinx.android.synthetic.main.ct_activity_source_selection.*
import kotlinx.android.synthetic.main.ct_activity_source_selection.iv_alexa_settings
import kotlinx.android.synthetic.main.ct_activity_source_selection.iv_back
import kotlinx.android.synthetic.main.ct_activity_source_selection.tv_device_name

class CTSourceSelectionActivity : AppCompatActivity() {

    var currentIpAddress: String? = null

    var currentSource: String? = null

    var bundle = Bundle()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ct_activity_source_selection)

        bundle = intent.extras!!

        currentIpAddress = bundle.getString(Constants.CURRENT_DEVICE_IP)

        currentSource = bundle.getString(Constants.CURRENT_SOURCE)

        val lssdpNodes = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(currentIpAddress)

        tv_device_name.text = lssdpNodes.friendlyname


        iv_back.setOnClickListener {
            onBackPressed()
        }


        if (lssdpNodes.getgCastVerision() != null) {
            //gcast != null -> hide alexa
            iv_alexa_settings.visibility = View.GONE
        }else{
            iv_alexa_settings.visibility = View.VISIBLE
        }

        iv_alexa_settings?.setOnClickListener {
            val mNode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(currentIpAddress)
            if (mNode?.alexaRefreshToken?.isEmpty()!!) {
                startActivity(Intent(this@CTSourceSelectionActivity, CTAmazonLoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(Constants.CURRENT_DEVICE_IP, currentIpAddress)
                    putExtra(Constants.FROM_ACTIVITY, CTMediaSourcesActivity::class.java.simpleName)
                })
            } else {
                startActivity(Intent(this@CTSourceSelectionActivity, CTAlexaThingsToTryActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(Constants.CURRENT_DEVICE_IP, currentIpAddress)
                    putExtra(Constants.FROM_ACTIVITY, CTMediaSourcesActivity::class.java.simpleName)
                })
            }
        }


        tv_net.setOnClickListener {
            if (!tv_net.isSelected) {
                tv_net.isSelected = true
                tv_device.visibility = View.VISIBLE

                startActivity(Intent(this@CTSourceSelectionActivity, CTMediaSourcesActivity::class.java).apply {
                    putExtra(Constants.CURRENT_DEVICE_IP, currentIpAddress)
                    putExtra(Constants.CURRENT_SOURCE, "" + currentSource)
                })
            } else {
                tv_net.isSelected = false
                tv_device.visibility = View.GONE

            }
        }



        ll_source_settings.setOnClickListener {
            val intent = Intent(this@CTSourceSelectionActivity, CTSourceSettingsActivity::class.java)
            val bundle = Bundle()
            bundle.putString("deviceFriendlyName", lssdpNodes.friendlyname)
            bundle.putString(Constants.CURRENT_DEVICE_IP,currentIpAddress)
            intent.putExtras(bundle)
            startActivity(intent)
        }

        tv_hdmi.setOnClickListener {
            tv_hdmi.isSelected = !tv_hdmi.isSelected
        }

        tv_opt.setOnClickListener {
            tv_opt.isSelected = !tv_opt.isSelected
        }

        tv_blu.setOnClickListener {
            tv_blu.isSelected = !tv_blu.isSelected
        }

        tv_ana.setOnClickListener {
            tv_ana.isSelected = !tv_ana.isSelected
        }
    }
}