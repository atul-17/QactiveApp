package com.cumulations.libreV2

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.SupplicantState
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiConfiguration.GroupCipher
import android.net.wifi.WifiConfiguration.KeyMgmt
import android.net.wifi.WifiManager
import android.util.Log
import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity
import com.libre.Scanning.Constants
import com.libre.util.LibreLogger


class WifiUtil(private val context: Context) {
    companion object {
        // Constants used for different security types
        const val WPA2 = "WPA2"
        const val WPA = "WPA"
        const val WEP = "WEP"
        const val OPEN = "Open"
        /* For EAP Enterprise fields */
        const val WPA_EAP = "WPA-EAP"
        const val IEEE8021X = "IEEE8021X"
    }

    private val wifiManager: WifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private var wifiList: List<ScanResult> = arrayListOf()
    private val myBroadCastReceiver: MyBroadcastReceiver = MyBroadcastReceiver()

    fun isWifiOn():Boolean{
        return wifiManager.isWifiEnabled && wifiManager?.connectionInfo?.supplicantState == SupplicantState.COMPLETED
    }

    fun getWifiSupplicantState():SupplicantState{
        val supplicantState = wifiManager.connectionInfo.supplicantState
        LibreLogger.d(this,"supplicantState name = ${supplicantState.name}")
        return supplicantState
    }

    fun startWifiScan() {
        if (!wifiManager.isWifiEnabled)
        {
            Log.d("startWifiScan", "wifiManager is disabled..making it enabled")
            wifiManager.isWifiEnabled = true
        }
        Log.d("TAG", "starting Wifi scan...")
        wifiManager.startScan()
        context.registerReceiver(myBroadCastReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
    }

    fun isSACModeOn():Boolean{
        for (scanResult in wifiList){
            if (scanResult.SSID == Constants.RIVAA_WAC_SSID)
                return true
        }
        return false
    }

    inner class MyBroadcastReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            wifiList = wifiManager.scanResults
            Log.d("TAG", "SCAN RESULTS: " + wifiList.size)

            if (wifiList.isEmpty()){
                if (wifiManager.startScan())
                    return
            }

            if (wifiList.isNotEmpty())
                if (context is CTDeviceDiscoveryActivity) {
                    context.onWifiScanDone(wifiList)
                }

            val wifiNamesList = arrayListOf<String>()
            for (name in wifiList) {
                wifiNamesList.add(name.SSID)
            }

            unregister()
        }

    }

    fun unregister() {
        try {
            context.unregisterReceiver(myBroadCastReceiver)
        } catch (e:java.lang.Exception){
            e.printStackTrace()
        }
    }

    private fun getSecurityType(ssid: String): String {
        if (wifiList.isEmpty()) {
            Log.e("getSecurityType","start wifiscan")
            return ""
        }

        for (scanResult in wifiList){
            if (scanResult.SSID == ssid){
                return getScanResultSecurity(scanResult)
            }
        }

        return ""
    }

    private fun getScanResultSecurity(scanResult: ScanResult): String {
        val cap = scanResult.capabilities
        val securityModes = arrayOf<String>(WEP, WPA, WPA2, WPA_EAP, IEEE8021X)
        for (i in securityModes.indices.reversed()) {
            if (cap.contains(securityModes[i])) {
                return securityModes[i]
            }
        }

        return OPEN
    }

    /**
     * @return The security of a given [WifiConfiguration].
     */
    fun getWifiConfigurationSecurity(wifiConfig: WifiConfiguration): String {
        when {
            wifiConfig.allowedKeyManagement.get(KeyMgmt.NONE) -> // If we never set group ciphers, wpa_supplicant puts all of them.
                // For open, we don't set group ciphers.
                // For WEP, we specifically only set WEP40 and WEP104, so CCMP
                // and TKIP should not be there.
                return if (!wifiConfig.allowedGroupCiphers.get(GroupCipher.CCMP)
                        && (wifiConfig.allowedGroupCiphers.get(GroupCipher.WEP40)
                                || wifiConfig.allowedGroupCiphers.get(GroupCipher.WEP104))) {
                    WEP
                } else {
                    OPEN
                }
            wifiConfig.allowedKeyManagement.get(KeyMgmt.WPA_EAP) -> return WPA_EAP
            wifiConfig.allowedKeyManagement.get(KeyMgmt.IEEE8021X) -> return IEEE8021X
            wifiConfig.allowedProtocols.get(WifiConfiguration.Protocol.RSN) -> return WPA2
            wifiConfig.allowedProtocols.get(WifiConfiguration.Protocol.WPA) -> return WPA
            else -> {
                Log.w("getWifiConfigSecurity", "Unknown security type from WifiConfiguration, falling back on open.")
                return OPEN
            }
        }
    }

    fun forgetWifiSSid(ssid: String): Boolean{
        val wifiConfiguration = getExistingWifiConfig(ssid)
        if (wifiConfiguration!=null && wifiConfiguration.networkId!=-1){
            return wifiManager.removeNetwork(wifiConfiguration.networkId)
        }
        return true
    }

    fun disconnectCurrentWifi():Boolean{
        return wifiManager?.disconnect()
    }

    fun connectWiFiToSSID(networkSSID: String, networkPass: String, networkSec: String):Int {
        var networkId = -1
        try {
            Log.v("connectWiFiToSSID", "SSID " + networkSSID + "Pwd : " + networkPass)
            var wifiConfiguration = getExistingWifiConfig(networkSSID)

            if (wifiConfiguration == null){
                wifiConfiguration = WifiConfiguration()
                wifiConfiguration.SSID = "\"" + networkSSID + "\""   // Please note the quotes. String should contain ssid in quotes
                wifiConfiguration.status = WifiConfiguration.Status.ENABLED
                wifiConfiguration.priority = 1000

                var securityType = networkSec

                if (securityType.isEmpty()) {
                    securityType = getSecurityType(networkSSID)
                    if (securityType.isEmpty()){
                        Log.d("connectWiFiToSSID","security Type empty")
                        return -1
                    }
                }

                when {
                    securityType.contains("WEP") -> {
                        Log.v("connectWiFiToSSID", "Configuring WEP")
                        wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                        wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN)
                        wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.WPA)
                        wifiConfiguration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN)
                        wifiConfiguration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED)
                        wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
                        wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
                        wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
                        wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104)

                        if (networkPass.matches("^[0-9a-fA-F]+$".toRegex())) {
                            wifiConfiguration.wepKeys[0] = networkPass
                        } else {
                            wifiConfiguration.wepKeys[0] = "\"" + networkPass + "\""
                        }

                        wifiConfiguration.wepTxKeyIndex = 0

                    }
                    securityType.contains("WPA")|| securityType.contains("WPA2") -> {
                        Log.v("connectWiFiToSSID", "Configuring WPA")

                        wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN)
                        wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.WPA)
                        wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
                        wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
                        wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
                        wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
                        wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104)
                        wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
                        wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)

                        wifiConfiguration.preSharedKey = "\"" + networkPass + "\""

                    }
                    else -> {
                        Log.v("connectWiFiToSSID", "Configuring OPEN network")
                        wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                        wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN)
                        wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.WPA)
                        wifiConfiguration.allowedAuthAlgorithms.clear()
                        wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
                        wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
                        wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
                        wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104)
                        wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
                        wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)
                    }
                }

                networkId = wifiManager.addNetwork(wifiConfiguration)
                Log.d("connectWiFiToSSID","config added")
            } else {
                Log.d("connectWiFiToSSID","config exists")
                networkId = wifiConfiguration?.networkId
//                if (wifiConfiguration.SSID != "\"KubuHomeHub\"") return -2
            }

            Log.v("connectWiFiToSSID", "networkId result $networkId")

//            val list = wifiManager.configuredNetworks
//            for (i in list) {
//                if (i.SSID != null && i.SSID == "\"" + networkSSID + "\"") {
//                    Log.v("connectWiFiToSSID", "WifiConfiguration SSID " + i.SSID)

            if (networkId!=-1) {
                val isDisconnected = wifiManager.disconnect()
                Log.v("connectWiFiToSSID", "isDisconnected : $isDisconnected")

                val isEnabled = wifiManager.enableNetwork(/*i.networkId*/networkId, true)
                Log.v("connectWiFiToSSID", "isEnabled : $isEnabled")

                val isReconnected = wifiManager.reconnect()
                Log.v("connectWiFiToSSID", "isReconnected : $isReconnected")
            } else {
                Log.e("connectWiFiToSSID","failed to connect wifi")
            }

//                    break
//                }
//            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return networkId
    }

    private fun getExistingWifiConfig(ssid: String): WifiConfiguration?{
        for (wifiConfiguration in wifiManager.configuredNetworks){
            Log.d("getExistingWifiConfig","${wifiConfiguration.SSID}  = $ssid")
            if (wifiConfiguration.SSID == "\"$ssid\"") {
                return wifiConfiguration
            }
        }

        return null
    }

    fun removeNetwork(networkId:Int):Boolean {
        return wifiManager.removeNetwork(networkId)
    }
}