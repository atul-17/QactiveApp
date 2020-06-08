package com.cumulations.libreV2.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import com.libre.qactive.LibreApplication
import com.libre.qactive.R
import com.libre.qactive.Scanning.Constants
import com.libre.qactive.app.dlna.dmc.processor.upnp.LoadLocalContentService


class CTSplashScreenActivityV2 : CTDeviceDiscoveryActivity() {

    /*override fun onCreate(savedInstanceState: Bundle?) {
        *//*Important to set theme before onCreate*//*
        setTheme(R.style.SplashTheme)

        super.onCreate(savedInstanceState)
    }*/


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ct_activity_gif_splash_screen)

    }

    override fun proceedToHome() {
        openNextScreen()
    }

    private fun openNextScreen() {

        Handler().post {
            if (!LibreApplication.LOCAL_IP.isNullOrEmpty()) {
                startService(Intent(this@CTSplashScreenActivityV2, LoadLocalContentService::class.java))
            }
        }

        Handler().postDelayed({
            if (!LibreApplication.getIs3PDAEnabled()) {
                intentToRegScreen();

            }
        }, 2000)
    }


     fun intentToRegScreen(){
         val intent = Intent(this@CTSplashScreenActivityV2,CTUserRegistration::class.java)
         val bundle = Bundle()
         bundle.putString(Constants.CURRENT_DEVICE_IP,null)
         intent.putExtras(bundle)
         startActivity(intent)
         finish()
     }

    override fun onBackPressed() {
        super.onBackPressed()
        killApp()
    }
}