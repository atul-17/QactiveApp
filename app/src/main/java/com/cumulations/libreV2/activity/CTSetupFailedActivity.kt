package com.cumulations.libreV2.activity

import android.os.Bundle
import com.libre.qactive.R
import kotlinx.android.synthetic.main.ct_activity_device_setup_failed_info.*

class CTSetupFailedActivity : CTDeviceDiscoveryActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ct_activity_device_setup_failed_info)

        toolbar?.title = ""
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        iv_back?.setOnClickListener {
            intentToHome(this)
        }

        btn_ok_got_it?.setOnClickListener {
            intentToHome(this)
        }
    }
}
