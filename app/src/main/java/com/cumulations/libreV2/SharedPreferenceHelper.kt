package com.cumulations.libreV2

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

class SharedPreferenceHelper(val context: Context) {
    companion object{
        const val ALEXA_ALERT_DONT_ASK = "alexaAlertDon'tAsk"
    }

    private var sharePreference: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    fun firstTimeAskedPermission(permission:String, isFirstTime:Boolean){
        sharePreference.edit().apply {
            putBoolean(permission, isFirstTime)
        }.apply()
    }

    fun isFirstTimeAskingPermission(permission:String):Boolean{
        sharePreference.apply {
            return getBoolean(permission,false)
        }
    }

    fun alexaLoginAlertDontAsk(ipAddress:String, dontAsk:Boolean){
        sharePreference.edit().apply {
            putBoolean("${ALEXA_ALERT_DONT_ASK}_$ipAddress", dontAsk)
        }.apply()
    }

    fun isAlexaLoginAlertDontAskChecked(ipAddress:String):Boolean{
        sharePreference.apply {
            return getBoolean("${ALEXA_ALERT_DONT_ASK}_$ipAddress",false)
        }
    }
}
