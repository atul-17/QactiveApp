package com.cumulations.libreV2.activity

import android.content.Intent
import com.libre.LibreApplication

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
        if (!LibreApplication.getIs3PDAEnabled()) {
            intentToHome(this)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        killApp()
    }
}