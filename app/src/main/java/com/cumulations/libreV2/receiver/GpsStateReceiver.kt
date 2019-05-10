package com.cumulations.libreV2.receiver

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import com.cumulations.libreV2.AppUtils
import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity

class GpsStateReceiver : BroadcastReceiver() {
    private lateinit var activity: Activity

    fun setActivity(activity: Activity){
        this.activity = activity
    }

    override fun onReceive(p0: Context?, p1: Intent?) {
        when(p1?.action){
            LocationManager.PROVIDERS_CHANGED_ACTION -> {
                if (activity is CTDeviceDiscoveryActivity){
                    if (activity.isFinishing)
                        return
                    if (!AppUtils.isLocationServiceEnabled(activity))
                        (activity as CTDeviceDiscoveryActivity).showLocationMustBeEnabledDialog()
                }
            }
        }
    }
}