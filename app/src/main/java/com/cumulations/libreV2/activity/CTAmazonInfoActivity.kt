package com.cumulations.libreV2.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.cumulations.libreV2.AppConstants
import com.libre.R
import com.libre.Scanning.Constants
import com.libre.alexa.DeviceProvisioningInfo
import com.libre.luci.LSSDPNodeDB
import com.libre.luci.LSSDPNodes
import kotlinx.android.synthetic.main.ct_activity_amazon_signin_setup.*

class CTAmazonInfoActivity : CTDeviceDiscoveryActivity(), View.OnClickListener {

    private val speakerIpAddress by lazy {
        intent.getStringExtra(Constants.CURRENT_DEVICE_IP)
    }

    private val from by lazy {
        intent.getStringExtra(Constants.FROM_ACTIVITY)
    }

    private var speakerNode: LSSDPNodes? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ct_activity_amazon_signin_setup)

        speakerNode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(speakerIpAddress)
        if (intent?.hasExtra(AppConstants.DEVICE_PROVISIONING_INFO)!!) {
            val deviceProvisioningInfo = intent.getSerializableExtra(AppConstants.DEVICE_PROVISIONING_INFO) as DeviceProvisioningInfo
            speakerNode?.mdeviceProvisioningInfo = deviceProvisioningInfo
        }

        iv_back.setOnClickListener(this)
        btn_signin_amazon.setOnClickListener(this)
        btn_signin_later.setOnClickListener(this)
    }

    override fun onClick(view: View) {

        when (view.id) {

            R.id.btn_signin_amazon -> {

                val amazonLoginScreen = Intent(this@CTAmazonInfoActivity, CTAmazonLoginActivity::class.java)
                amazonLoginScreen.putExtra(Constants.CURRENT_DEVICE_IP, speakerIpAddress)
                amazonLoginScreen.putExtra(AppConstants.DEVICE_PROVISIONING_INFO, speakerNode!!.mdeviceProvisioningInfo)
                amazonLoginScreen.putExtra(Constants.FROM_ACTIVITY, from)
                amazonLoginScreen.putExtra(Constants.PREV_SCREEN, CTAmazonInfoActivity::class.java.simpleName)
                startActivity(amazonLoginScreen)
                finish()
            }

            R.id.iv_back -> intentToHome(this)

            R.id.btn_signin_later -> {
                startActivity(Intent(this, CTAlexaThingsToTryActivity::class.java)
                        .putExtra(Constants.CURRENT_DEVICE_IP, speakerIpAddress)
                        .putExtra(Constants.PREV_SCREEN, CTAmazonInfoActivity::class.java.simpleName))
                finish()
            }
        }
    }

    override fun onBackPressed() {
        intentToHome(this)
    }
}
