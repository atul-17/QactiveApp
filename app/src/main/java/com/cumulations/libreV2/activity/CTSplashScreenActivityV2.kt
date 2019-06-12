package com.cumulations.libreV2.activity

import android.content.Intent
import android.os.Bundle
import com.libre.LibreApplication
import com.libre.R
import com.libre.alexa.userpoolManager.AlexaUtils.AlexaConstants
import com.libre.alexa.userpoolManager.MainActivity

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
        } else {
            val intent = Intent(this@CTSplashScreenActivityV2, MainActivity::class.java)
            intent.putExtra(AlexaConstants.INTENT_FROM_ACTIVITY, AlexaConstants.INTENT_FROM_ACTIVITY_SPLASH_SCREEN)
            startActivity(intent)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        killApp()
    }
}