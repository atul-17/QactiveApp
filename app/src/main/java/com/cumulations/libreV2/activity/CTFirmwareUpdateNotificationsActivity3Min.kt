package com.cumulations.libreV2.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.libre.qactive.R
import kotlinx.android.synthetic.main.ct_activity_firmware_update_notifications_screen_3_mins.*
import kotlinx.android.synthetic.main.ct_activity_firmware_update_notifications_screen_3_mins.iv_back
import kotlinx.android.synthetic.main.ct_activity_firmware_update_notifications_screen_3_mins.tv_device_name
import kotlinx.android.synthetic.main.ct_activity_firmware_update_notifications_screen_4_hours.*


class CTFirmwareUpdateNotificationsActivity3Min : AppCompatActivity(){


    var bundle: Bundle? = Bundle()

    var deviceFriendlyName = ""



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ct_activity_firmware_update_notifications_screen_3_mins)


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