package com.cumulations.libreV2

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import androidx.core.app.ActivityCompat
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.util.Patterns
import com.cumulations.libreV2.activity.CTWifiListActivity
import com.libre.qactive.Scanning.Constants
import com.cumulations.libreV2.model.SceneObject
import com.libre.qactive.LibreApplication
import com.libre.qactive.R
import com.libre.qactive.Scanning.ScanningHandler
import com.libre.qactive.alexa.ControlConstants
import com.libre.qactive.constants.LSSDPCONST
import com.libre.qactive.constants.LUCIMESSAGES
import com.libre.qactive.constants.MIDCONST
import com.libre.qactive.luci.LUCIControl
import com.libre.qactive.util.LibreLogger
import org.json.JSONArray
import org.json.JSONObject
import java.util.regex.Pattern


object AppUtils {

    fun getConnectedSSID(context: Context): String? {
        var connectedSSID = ""
        val wifiManager: WifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        connectedSSID = wifiInfo.ssid.replace("\"", "", false)
        Log.e("connectedSSID", connectedSSID)

        if (connectedSSID == null || connectedSSID == "<unknown ssid>") {
            return ""
        }

        return connectedSSID
    }

    fun isPermissionGranted(context: Context, permission: String): Boolean =
            ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun shouldShowPermissionRationale(context: Context, permission: String): Boolean =
            ActivityCompat.shouldShowRequestPermissionRationale(context as Activity, permission)

    fun requestPermission(context: Context, permission: String, requestId: Int) =
            ActivityCompat.requestPermissions(context as Activity, arrayOf(permission), requestId)

    fun isEmailValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isContainsSpecialCharacters(value: String): Boolean {
        val special = Pattern.compile("[!@#$%&*()_+:;/><=|<>.?{}\\[\\]~-]")
        val hasSpecial = special.matcher(value)
        return hasSpecial.find()
    }

    fun isContainsNumber(value: String): Boolean {
        val digit = Pattern.compile("[0-9]")
        val hasDigit = digit.matcher(value)
        return hasDigit.find()
    }

    fun isOnline(context: Context): Boolean {
        val connectivityMananger: ConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return connectivityMananger.activeNetworkInfo != null && connectivityMananger.activeNetworkInfo.isConnected
    }

    fun isLocationServiceEnabled(context: Context): Boolean {
        val locationManager = context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun getWifiIp(context: Context): String {
        val wifiMan = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInf = wifiMan.connectionInfo
        val ipAddress = wifiInf.ipAddress
        return String.format("%d.%d.%d.%d", ipAddress and 0xff, ipAddress shr 8 and 0xff, ipAddress shr 16 and 0xff, ipAddress shr 24 and 0xff)
    }

    fun showAlertForNotConnectedToSAC(context: Context) {
        if (!(context as AppCompatActivity).isFinishing!!) {
            val builder = AlertDialog.Builder(context)
            val message = context.getString(R.string.title_error_sac_message) +
                    "\n(" + Constants.WAC_SSID_RIVAA_CONCERT + "XXXXXX)"
            builder.setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton(context.getString(R.string.ok)) { dialog, id ->
                        dialog.dismiss()
                        if (context is CTWifiListActivity) {
                            context.onBackPressed()
                        }
                    }
            builder.show()
        }
    }

    fun storeSSIDInfoToSharedPreferences(context: Context, deviceSSID: String, password: String) {
        context.getSharedPreferences("Your_Shared_Prefs", Context.MODE_PRIVATE).apply {
            edit()
                    .putString(deviceSSID, password)
                    .apply()
        }
    }

    fun updateSceneObjectWithPlayJsonWindow(window: JSONObject, oldSceneObject: SceneObject): SceneObject {
        oldSceneObject?.trackName = window.getString("TrackName")
        oldSceneObject?.album_art = window.getString("CoverArtUrl")
        oldSceneObject?.playstatus = window.getInt("PlayState")
        val mPlayURL = window.getString("PlayUrl")
        if (mPlayURL != null)
            oldSceneObject?.playUrl = mPlayURL
        /*For favourite*/
        oldSceneObject?.setIsFavourite(window.getBoolean("Favourite"))

        /*Added for Shuffle and Repeat, update only if it's not playing from local DMR
        * Because we need to sync with playback helper in that case not MB*/
        if (!isLocalDMRPlaying(oldSceneObject)) {
            oldSceneObject?.shuffleState = window.getInt("Shuffle")
            oldSceneObject?.repeatState = window.getInt("Repeat")
        }
        oldSceneObject?.album_name = window.getString("Album")
        oldSceneObject?.artist_name = window.getString("Artist")
        oldSceneObject?.genre = window.getString("Genre")
        oldSceneObject?.totalTimeOfTheTrack = window.getLong("TotalTime")
        oldSceneObject?.currentSource = window.getInt("Current Source")
        if (oldSceneObject?.currentSource == Constants.AUX_SOURCE
        /*|| oldSceneObject?.currentSource == Constants.EXTERNAL_SOURCE*/) {
            oldSceneObject?.artist_name = "Aux Playing"
        }

        if (oldSceneObject?.currentSource == Constants.BT_SOURCE) {
            oldSceneObject?.artist_name = "Bluetooth Playing"
        }
        parseControlJsonForAlexa(window, oldSceneObject)

        return oldSceneObject
    }

    private fun parseControlJsonForAlexa(window: JSONObject, currentSceneObject: SceneObject) {
        try {

            val controlJson = window.getString("ControlsJson")
            if (controlJson == null || controlJson.isEmpty() || controlJson.equals("null", true))
                return

            val controlsJsonArr = JSONArray(controlJson)
            if (controlsJsonArr != null) {
                val flags = booleanArrayOf(false, false, false)
                for (i in 0 until controlsJsonArr.length()) {
                    LibreLogger.d(this, "JSON recieved for Alexa controls " + controlsJsonArr.get(i))
                    // sample JSON {\"enabled\":true,\"name\":\"PLAY_PAUSE\",\"selected\":false,\"type\":\"BUTTON\"}
                    val jsonObject = controlsJsonArr.getJSONObject(i)
                    val name = jsonObject.getString("name")
                    val enabled = jsonObject.getBoolean("enabled")
                    when (name) {
                        ControlConstants.PLAY_PAUSE -> if (enabled) {
                            flags[0] = true
                        }
                        ControlConstants.NEXT -> if (enabled) {
                            flags[1] = true
                        }
                        ControlConstants.PREVIOUS -> if (enabled) {
                            flags[2] = true
                        }
                    }
                }
                currentSceneObject.setAlexaControls(flags)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isAnyDevicePlaying(): Boolean {
        if (ScanningHandler.getInstance().sceneObjectMapFromRepo.isEmpty())
            return false

        ScanningHandler.getInstance().sceneObjectMapFromRepo.forEach { (ipAddress: String?, sceneObject: SceneObject?) ->
            LibreLogger.d("isAnyDevicePlaying", "$ipAddress, ${sceneObject.sceneName}")
            /*Wrong!! we should return only after iterating through whole map objects*/
//            return sceneObject?.playstatus == SceneObject.CURRENTLY_PLAYING

            if (sceneObject?.playstatus == SceneObject.CURRENTLY_PLAYING)
                return true
        }

        return false
    }

    fun stopAllDevicesPlaying() {
        ScanningHandler.getInstance().sceneObjectMapFromRepo.forEach { (ipAddress: String?, sceneObject: SceneObject?) ->
            LibreLogger.d("stopAllDevicesPlaying", "$ipAddress, playStatus = ${sceneObject.playstatus}")
            LUCIControl(ipAddress).SendCommand(MIDCONST.MID_PLAYCONTROL.toInt(), LUCIMESSAGES.STOP, LSSDPCONST.LUCI_SET)
        }
    }

    fun isActivePlaylistNotAvailable(sceneObject: SceneObject?): Boolean {
        LibreLogger.d(this, "isActivePlaylistNotAvailable, ${sceneObject?.sceneName}")
        /* For Playing , If DMR is playing then we should give control for Play/Pause*/
        return sceneObject?.currentSource == Constants.NO_SOURCE
                || sceneObject?.currentSource == Constants.DDMSSLAVE_SOURCE
                || (sceneObject?.currentSource == Constants.DMR_SOURCE
                && (sceneObject?.playstatus == SceneObject.CURRENTLY_STOPPED
                || sceneObject?.playstatus == SceneObject.CURRENTLY_NOTPLAYING))
    }

    fun isDMRPlayingFromOtherPhone(sceneObject: SceneObject?): Boolean {
        LibreLogger.d(this, "isDMRPlayingFromOtherPhone, ${sceneObject?.sceneName}")
        /* For Playing , If DMR is playing then we should give control for Play/Pause*/
        return sceneObject?.currentSource == Constants.DMR_SOURCE
                && sceneObject?.playstatus == SceneObject.CURRENTLY_PLAYING
                && !sceneObject.playUrl.contains(LibreApplication.LOCAL_IP)
    }

    fun isLocalDMRPlaying(sceneObject: SceneObject?): Boolean {
        LibreLogger.d(this, "isLocalDMRPlaying, ${sceneObject?.sceneName}")
        /* For Playing , If DMR is playing then we should give control for Play/Pause*/
        if (sceneObject?.playUrl != null) {
            return sceneObject?.currentSource == Constants.DMR_SOURCE
                    && sceneObject.playUrl.contains(LibreApplication.LOCAL_IP)
                    && (sceneObject?.playstatus == SceneObject.CURRENTLY_PLAYING
                    || sceneObject?.playstatus == SceneObject.CURRENTLY_PAUSED)
        } else {
            return false
        }
    }
}
