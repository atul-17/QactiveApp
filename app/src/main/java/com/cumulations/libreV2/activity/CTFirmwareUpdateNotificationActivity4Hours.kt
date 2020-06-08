package com.cumulations.libreV2.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.libre.qactive.R
import kotlinx.android.synthetic.main.ct_activity_firmware_update_notifications_screen_4_hours.*
import kotlinx.android.synthetic.main.ct_activity_firmware_update_notifications_screen_4_hours.iv_back
import kotlinx.android.synthetic.main.ct_activity_firmware_update_notifications_screen_4_hours.tv_device_name
import kotlinx.android.synthetic.main.ct_activity_source_settings.*

class CTFirmwareUpdateNotificationActivity4Hours : AppCompatActivity(){

    var bundle: Bundle? = Bundle()

    var deviceFriendlyName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ct_activity_firmware_update_notifications_screen_4_hours)


        bundle = intent.extras

        if (bundle != null) {
            deviceFriendlyName = bundle!!.getString("deviceFriendlyName", "")
        }


        tv_device_name.text = deviceFriendlyName

        iv_back.setOnClickListener {
            onBackPressed()
        }
    }
}