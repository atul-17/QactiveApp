package com.cumulations.libreV2.activity

import android.content.Intent
import android.os.Handler
import com.libre.LibreApplication
import com.libre.app.dlna.dmc.processor.upnp.LoadLocalContentService

class CTSplashScreenActivityV2 : CTDeviceDiscoveryActivity() {

    /*override fun onCreate(savedInstanceState: Bundle?) {
        *//*Important to set theme before onCreate*//*
        setTheme(R.style.SplashTheme)

        super.onCreate(savedInstanceState)
    }*/

    override fun proceedToHome() {
        openNextScreen()
    }

    private fun openNextScreen() {

        Handler().post {
            if (!LibreApplication.LOCAL_IP.isNullOrEmpty()) {
                startService(Intent(this@CTSplashScreenActivityV2, LoadLocalContentService::class.java))
            }
        }

        if (!LibreApplication.getIs3PDAEnabled()) {
            intentToHome(this)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        killApp()
    }
}