package com.cumulations.libreV2.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.cumulations.libreV2.model.SceneObject
import com.libre.qactive.R
import com.libre.qactive.Scanning.Constants
import com.libre.qactive.luci.LSSDPNodeDB
import com.libre.qactive.luci.LSSDPNodes
import kotlinx.android.synthetic.main.ct_acitvity_now_playing.*
import kotlinx.android.synthetic.main.ct_activity_source_settings.*
import kotlinx.android.synthetic.main.ct_activity_source_settings.iv_alexa_settings
import kotlinx.android.synthetic.main.ct_activity_source_settings.iv_back
import kotlinx.android.synthetic.main.ct_activity_source_settings.tv_device_name
import kotlinx.android.synthetic.main.music_playing_widget.*


class CTSourceSettingsActivity : CTDeviceDiscoveryActivity() {

    var bundle: Bundle? = Bundle()

    var deviceFriendlyName = ""

    var currentIpAddress = ""

    var node: LSSDPNodes? = null;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ct_activity_source_settings)

        bundle = intent.extras

        if (bundle != null) {
            deviceFriendlyName = bundle!!.getString("deviceFriendlyName", "")
            currentIpAddress = bundle!!.getString(Constants.CURRENT_DEVICE_IP, "")
        }


        tv_device_name.text = deviceFriendlyName
        iv_back.setOnClickListener {
            onBackPressed()
        }

        node = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(currentIpAddress)

        if (node?.getgCastVerision() != null) {
            //gcast != null -> hide alexa
            iv_alexa_settings.visibility = View.INVISIBLE
        } else {
            iv_alexa_settings.visibility = View.VISIBLE
        }



        iv_alexa_settings?.setOnClickListener {

            if (node?.alexaRefreshToken?.isEmpty()!!) {
                startActivity(Intent(this@CTSourceSettingsActivity, CTAmazonLoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(Constants.CURRENT_DEVICE_IP, currentIpAddress)
                    putExtra(Constants.FROM_ACTIVITY, CTMediaSourcesActivity::class.java.simpleName)
                })
            } else {
                startActivity(Intent(this@CTSourceSettingsActivity, CTAlexaThingsToTryActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(Constants.CURRENT_DEVICE_IP, currentIpAddress)
                    putExtra(Constants.FROM_ACTIVITY, CTMediaSourcesActivity::class.java.simpleName)
                })
            }
        }

        setTheSourceIconFromCurrentSceneObject()
    }

    private fun setTheSourceIconFromCurrentSceneObject() {
        if (node?.currentSource == null)
            return

        ivSourceType?.visibility = View.VISIBLE

        var imgResId = R.drawable.songs_borderless

        when (node?.currentSource) {

            Constants.NO_SOURCE -> {
                ivSourceType?.visibility = View.INVISIBLE
            }

            Constants.DMR_SOURCE -> {
                imgResId = R.drawable.ic_white_dlna
            }
            Constants.DMP_SOURCE -> {
                imgResId = /*R.mipmap.network*/R.drawable.media_servers_enabled
            }

            Constants.SPOTIFY_SOURCE -> {
                imgResId = R.mipmap.spotify
            }

            Constants.USB_SOURCE -> {
                imgResId = /*R.mipmap.usb*/R.drawable.usb_storage_enabled

            }
            Constants.SDCARD_SOURCE -> {
                imgResId = R.mipmap.sdcard

            }
            Constants.VTUNER_SOURCE -> {
                imgResId = R.mipmap.vtuner_logo
            }

            Constants.TUNEIN_SOURCE -> {
                imgResId = R.mipmap.tunein_logo1
            }

            Constants.AUX_SOURCE -> {
                imgResId = R.drawable.ic_aux_in
            }

            Constants.BT_SOURCE -> {
                imgResId = R.drawable.ic_bt_on
            }

            Constants.DEEZER_SOURCE -> {
                imgResId = R.mipmap.deezer_logo
            }

            Constants.TIDAL_SOURCE -> {
                imgResId = R.mipmap.tidal_white_logo
            }

            Constants.FAVOURITES_SOURCE -> {
                imgResId = R.mipmap.ic_remote_favorite
            }

            Constants.ALEXA_SOURCE -> {
                imgResId = R.drawable.alexa_blue_white_100px
            }
            Constants.GCAST_SOURCE -> {
                imgResId = R.mipmap.ic_cast_white_24dp_2x
            }

            Constants.AIRPLAY_SOURCE -> {
                imgResId = R.drawable.ic_white_airplay
            }

            Constants.ROON_SOURCE -> {
                imgResId = R.drawable.ic_roon_white
            }

        }

        ivSourceType.setImageResource(imgResId)
    }

    override fun onResume() {
        super.onResume()
        setMusicPlayerWidget(fl_music_play_widget, currentIpAddress)
    }

}