package com.cumulations.libreV2.activity

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.appcompat.app.AppCompatActivity
import com.libre.qactive.R
import com.libre.qactive.Scanning.Constants
import com.libre.qactive.luci.LSSDPNodeDB
import com.libre.qactive.luci.LSSDPNodes
import kotlinx.android.synthetic.main.ct_acitvity_now_playing.*
import kotlinx.android.synthetic.main.ct_activity_source_selection.*
import kotlinx.android.synthetic.main.ct_activity_source_selection.iv_alexa_settings
import kotlinx.android.synthetic.main.ct_activity_source_selection.iv_back
import kotlinx.android.synthetic.main.ct_activity_source_selection.iv_device_settings
import kotlinx.android.synthetic.main.ct_activity_source_selection.tv_device_name

class CTSourceSelectionActivity : AppCompatActivity() {

    var currentIpAddress: String? = null

    var currentSource: String? = null

    var bundle = Bundle()

    var node: LSSDPNodes? = null;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ct_activity_source_selection)

        bundle = intent.extras!!

        currentIpAddress = bundle.getString(Constants.CURRENT_DEVICE_IP)

        currentSource = bundle.getString(Constants.CURRENT_SOURCE)

        val lssdpNodes = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(currentIpAddress)

        tv_device_name.text = lssdpNodes.friendlyname


        iv_back.setOnClickListener {
            onBackPressed()
        }

        node = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(currentIpAddress)


        if (lssdpNodes.getgCastVerision() != null) {
            //gcast != null -> hide alexa
            iv_alexa_settings.visibility = View.INVISIBLE
        } else {
            iv_alexa_settings.visibility = View.VISIBLE
        }

        iv_device_settings?.setOnClickListener {
            startActivity(Intent(this@CTSourceSelectionActivity, CTDeviceSettingsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(Constants.CURRENT_DEVICE_IP, currentIpAddress)
                putExtra(Constants.FROM_ACTIVITY, CTSourceSelectionActivity::class.java.simpleName)
            })
        }


        iv_alexa_settings?.setOnClickListener {
            val mNode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(currentIpAddress)
            if (mNode?.alexaRefreshToken?.isEmpty()!!) {
                startActivity(Intent(this@CTSourceSelectionActivity, CTAmazonLoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(Constants.CURRENT_DEVICE_IP, currentIpAddress)
                    putExtra(Constants.FROM_ACTIVITY, CTSourceSelectionActivity::class.java.simpleName)
                })
            } else {
                startActivity(Intent(this@CTSourceSelectionActivity, CTAlexaThingsToTryActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(Constants.CURRENT_DEVICE_IP, currentIpAddress)
                    putExtra(Constants.FROM_ACTIVITY, CTSourceSelectionActivity::class.java.simpleName)
                })
            }
        }

        setTheSourceIconFromCurrentSceneObject()

//        tv_net.setOnClickListener {
//            if (!tv_net.isSelected) {
//
////                tv_device.visibility = View.VISIBLE
//
//
//
//            } else {
//                tv_net.isSelected = false
////                ivSourceType.visibility = View.GONE
//            }
//        }



        ll_source_settings.setOnClickListener {
            val intent = Intent(this@CTSourceSelectionActivity, CTSourceSettingsActivity::class.java)
            val bundle = Bundle()
            bundle.putString("deviceFriendlyName", lssdpNodes.friendlyname)
            bundle.putString(Constants.CURRENT_DEVICE_IP, currentIpAddress)
            intent.putExtras(bundle)
            startActivity(intent)
        }

        tv_hdmi.setOnClickListener {
            tv_hdmi.isSelected = !tv_hdmi.isSelected
        }

        tv_opt.setOnClickListener {
            tv_opt.isSelected = !tv_opt.isSelected
        }

        tv_blu.setOnClickListener {
            tv_blu.isSelected = !tv_blu.isSelected
        }

        tv_ana.setOnClickListener {
            tv_ana.isSelected = !tv_ana.isSelected
        }

    }

    private fun setTheSourceIconFromCurrentSceneObject() {
        if (node?.currentSource == null)
            return

        ivSourceType?.visibility = View.VISIBLE
        var imgResId = R.drawable.songs_borderless

        when (node?.currentSource) {

            Constants.NO_SOURCE -> {
                tv_net.isSelected = false
                ivSourceType?.visibility = View.INVISIBLE
            }

            Constants.DMR_SOURCE -> {
                tv_net.isSelected = true
                imgResId = R.drawable.ic_white_dlna
            }
            Constants.DMP_SOURCE -> {
                tv_net.isSelected = true
                imgResId = /*R.mipmap.network*/R.drawable.media_servers_enabled
            }

            Constants.SPOTIFY_SOURCE -> {
                tv_net.isSelected = true
                imgResId = R.mipmap.spotify
            }

            Constants.USB_SOURCE -> {
                tv_net.isSelected = true
                imgResId = /*R.mipmap.usb*/R.drawable.usb_storage_enabled

            }
            Constants.SDCARD_SOURCE -> {
                tv_net.isSelected = true
                imgResId = R.mipmap.sdcard

            }
            Constants.VTUNER_SOURCE -> {
                tv_net.isSelected = true
                imgResId = R.mipmap.vtuner_logo
            }

            Constants.TUNEIN_SOURCE -> {
                tv_net.isSelected = true
                imgResId = R.mipmap.tunein_logo1
            }

            Constants.AUX_SOURCE -> {
                tv_net.isSelected = true
                imgResId = R.drawable.ic_aux_in
            }

            Constants.BT_SOURCE -> {
                tv_net.isSelected = true
                imgResId = R.drawable.ic_bt_on
            }

            Constants.DEEZER_SOURCE -> {
                tv_net.isSelected = true
                imgResId = R.mipmap.deezer_logo
            }

            Constants.TIDAL_SOURCE -> {
                tv_net.isSelected = true
                imgResId = R.mipmap.tidal_white_logo
            }

            Constants.FAVOURITES_SOURCE -> {
                tv_net.isSelected = true
                imgResId = R.mipmap.ic_remote_favorite
            }

            Constants.ALEXA_SOURCE -> {
                tv_net.isSelected = true
                imgResId = R.drawable.alexa_blue_white_100px
            }
            Constants.GCAST_SOURCE -> {
                tv_net.isSelected = true
                imgResId = R.mipmap.ic_cast_white_24dp_2x
            }

            Constants.AIRPLAY_SOURCE -> {
                tv_net.isSelected = true
                imgResId = R.drawable.ic_white_airplay
            }

            Constants.ROON_SOURCE -> {
                tv_net.isSelected = true
                imgResId = R.drawable.ic_roon_white
            }

        }
        ivSourceType.setImageResource(imgResId)
    }
}