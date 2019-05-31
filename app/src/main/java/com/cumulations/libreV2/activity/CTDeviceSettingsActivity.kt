package com.cumulations.libreV2.activity

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.SeekBar
import com.cumulations.libreV2.AppConstants
import com.cumulations.libreV2.closeKeyboard
import com.cumulations.libreV2.fragments.CTAlexaLocaleDialogFragment
import com.cumulations.libreV2.writeAwayModeSettingsToDevice
import com.libre.R
import com.libre.Scanning.Constants
import com.libre.Scanning.ScanningHandler
import com.libre.alexa.LibreAlexaConstants
import com.libre.constants.LSSDPCONST
import com.libre.constants.LUCIMESSAGES
import com.libre.constants.MIDCONST
import com.libre.luci.*
import com.libre.netty.LibreDeviceInteractionListner
import com.libre.netty.NettyData
import com.libre.util.LibreLogger
import kotlinx.android.synthetic.main.ct_device_settings.*
import kotlinx.android.synthetic.main.ct_dlg_fragment_edit_away_mode.view.*


class CTDeviceSettingsActivity : CTDeviceDiscoveryActivity(), LibreDeviceInteractionListner {
    private var currentLocale: String? = null
    private lateinit var currentDeviceNode: LSSDPNodes
    private lateinit var luciControl: LUCIControl
    private var switchStatus: String? = null
    private var seekbarVolumeValue: String? = null
    private val deviceSSID by lazy {
        intent?.getStringExtra(AppConstants.DEVICE_SSID)
    }
    private val currentDeviceIp by lazy {
        intent?.getStringExtra(Constants.CURRENT_DEVICE_IP)
    }
    internal var mAudioOutput = ""
    internal var mAudioPreset = ""

    val STEREO = 0
    val LEFT = 1
    val RIGHT = 2

    private var mScanHandler = ScanningHandler.getInstance()
    private var awayModeSettingsDialog:AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ct_device_settings)
    }

    override fun onStart() {
        super.onStart()
        luciControl = LUCIControl(currentDeviceIp)
        initViews()
        setListeners()
    }

    private fun initViews() {
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(false)
        toolbar?.title = ""

        showLoader(audio_progress_bar.id)
        showLoader(system_firmware_progress_bar.id)
        showLoader(host_firmware_progress_bar.id)
        showLoader(network_name_progress_bar.id)
        showLoader(login_progress_bar.id)
        showLoader(locale_progress_bar.id)
//        showLoader(soft_update_progress_bar.id)

        currentDeviceNode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(currentDeviceIp)

        tv_toolbar_title.text = currentDeviceNode.friendlyname
        tv_device_name.text = currentDeviceNode.friendlyname

        if (currentDeviceNode != null && currentDeviceNode.version != null) {
            var dutExistingFirmware = currentDeviceNode.version

            val arrayString = dutExistingFirmware.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            dutExistingFirmware = dutExistingFirmware.substring(0, dutExistingFirmware.indexOf('.'))
            val dutExistingHostVersion = arrayString[1].replace("[a-zA-z]".toRegex(), "")
            /*String mFirmwareVersionToDisplay = dutExistingFirmware.replaceAll("[a-zA-z]", "")+"."+arrayString[1]+"."+
                    arrayString[2];*/
//            tv_system_firmware.text = dutExistingFirmware.replace("[a-zA-z]".toRegex(), "")
            tv_system_firmware.text = currentDeviceNode.version
            tv_host_firmware.text = dutExistingHostVersion
            tv_mac_address.text = Utils.convertToMacAddress(currentDeviceNode?.usn)
            closeLoader(system_firmware_progress_bar.id)
            closeLoader(host_firmware_progress_bar.id)
        }

        tv_ip_address?.text = currentDeviceNode?.ip

        /*this is the data for Audio Presets*/
        val presetSpinnerData = resources.getStringArray(R.array.audio_preset_array)
        val presetDataAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, presetSpinnerData)
        presetDataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        /*Speaker type data*/
        val spinnerData = resources.getStringArray(R.array.audio_output_array)
        val dataAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, spinnerData)
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        closeLoader(audio_progress_bar.id)
        closeLoader(soft_update_progress_bar.id)

        if (currentDeviceNode?.getmDeviceCap()?.getmSource()?.isAlexaAvsSource!!){
            ll_alexa_settings?.visibility = View.VISIBLE
        } else {
            closeLoader(login_progress_bar.id)
            closeLoader(locale_progress_bar.id)
            ll_alexa_settings?.visibility = View.GONE
        }
    }

    private fun setListeners(){
        tv_amazon_login.setOnClickListener {
            if (tv_amazon_login?.text == getString(R.string.logged_in)) {
                startActivity(Intent(this@CTDeviceSettingsActivity, CTAlexaThingsToTryActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(Constants.CURRENT_DEVICE_IP, currentDeviceIp)
                    putExtra(Constants.FROM_ACTIVITY, CTDeviceSettingsActivity::class.java.simpleName)
                })
            } else {
                startActivity(Intent(this@CTDeviceSettingsActivity, CTAmazonLoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(Constants.CURRENT_DEVICE_IP, currentDeviceIp)
                    putExtra(Constants.FROM_ACTIVITY, CTDeviceSettingsActivity::class.java.simpleName)
                })
            }
        }

        tv_alexa_locale?.setOnClickListener {
            if (currentLocale.isNullOrEmpty())
                return@setOnClickListener

            CTAlexaLocaleDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(Constants.CURRENT_LOCALE,currentLocale)
                }

                show(supportFragmentManager,this::class.java.simpleName)
            }
        }

        switch_speech_volume_follow.setOnCheckedChangeListener { compoundButton, b ->
            val luciControl = LUCIControl(currentDeviceIp)
            if (b) {
                tv_switch_status.text = getText(R.string.on).toString().toUpperCase()
                switchStatus = "1"
                luciControl.SendCommand(MIDCONST.MID_MIC, "SV:$seekbarVolumeValue,$switchStatus", LSSDPCONST.LUCI_SET)
            } else {
                tv_switch_status.text = getText(R.string.off).toString().toUpperCase()
                switchStatus = "0"
                luciControl.SendCommand(MIDCONST.MID_MIC, "SV:$seekbarVolumeValue,$switchStatus", LSSDPCONST.LUCI_SET)

            }
        }

        seek_bar_volume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                LibreLogger.d(this, "Seekbar Position track " + seekBar.progress + "  " + seekBar.max)
                tv_volume_value.text = "" + progress + "dB"
                seekbarVolumeValue = "" + progress
                if (fromUser) {
                    val luciControl = LUCIControl(currentDeviceIp)
                    luciControl.SendCommand(MIDCONST.MID_MIC, "SV:$seekbarVolumeValue,$switchStatus", 2)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                LibreLogger.d(this, "Seekbar Position trackstop" + seekBar.progress + "  " + seekBar.max)


            }
        })

        tv_edit_away_mode?.setOnClickListener {
            showAwayModeAlert(tv_network_name.text.toString(),tv_wifi_pwd.text.toString())
        }

        iv_back?.setOnClickListener {
            onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        registerForDeviceEvents(this)
        requestLuciUpdates()
    }

    private fun showLoader(progressBarId : Int) {
        findViewById<ProgressBar>(progressBarId).visibility = View.VISIBLE
        when(progressBarId){
            R.id.audio_progress_bar -> tv_audio_output?.visibility = View.INVISIBLE
            R.id.system_firmware_progress_bar -> tv_system_firmware?.visibility = View.INVISIBLE
            R.id.host_firmware_progress_bar -> tv_host_firmware?.visibility = View.INVISIBLE
            R.id.network_name_progress_bar -> tv_network_name?.visibility = View.INVISIBLE
            R.id.login_progress_bar -> tv_amazon_login?.visibility = View.INVISIBLE
            R.id.locale_progress_bar -> tv_alexa_locale?.visibility = View.INVISIBLE
            R.id.soft_update_progress_bar -> tv_soft_update?.visibility = View.INVISIBLE
        }
    }

    private fun closeLoader(progressBarId : Int) {
        findViewById<ProgressBar>(progressBarId).visibility = View.INVISIBLE
        when(progressBarId){
            R.id.audio_progress_bar -> tv_audio_output?.visibility = View.VISIBLE
            R.id.system_firmware_progress_bar -> tv_system_firmware?.visibility = View.VISIBLE
            R.id.host_firmware_progress_bar -> tv_host_firmware?.visibility = View.VISIBLE
            R.id.network_name_progress_bar -> tv_network_name?.visibility = View.VISIBLE
            R.id.login_progress_bar -> tv_amazon_login?.visibility = View.VISIBLE
            R.id.locale_progress_bar -> tv_alexa_locale?.visibility = View.VISIBLE
            R.id.soft_update_progress_bar -> tv_soft_update?.visibility = View.VISIBLE
        }
    }

    override fun deviceDiscoveryAfterClearingTheCacheStarted() {

    }

    override fun newDeviceFound(node: LSSDPNodes) {

    }

    override fun deviceGotRemoved(ipaddress: String?) {
        if (ipaddress != null && currentDeviceIp != null && ipaddress == currentDeviceIp) {
            onBackPressed()
        }
    }

    override fun messageRecieved(nettyData: NettyData) {

        val remoteDeviceIp = nettyData.getRemotedeviceIp()

        val packet = LUCIPacket(nettyData.getMessage())
        LibreLogger.d(this, "Message received for ipaddress " + remoteDeviceIp + ", command is " + packet.command)

        if (currentDeviceIp!! == remoteDeviceIp) {

            LibreLogger.d(this,"Command = "+packet.command+", payload msg = ${String(packet.payload)}")
            when (packet.command) {

                MIDCONST.MID_ENV_READ -> {
                    val message = String(packet.payload)

                    if (message.contains("ddms_SSID") /*ddms_SSID:RIVAACONCERT*/) {
                        closeLoader(network_name_progress_bar.id)
                        tv_network_name.text = message.substring(message.indexOf(":") + 1)
                    }

                    if (message.contains("ddms_password") /*ddms_password:12345678*/) {
                        tv_wifi_pwd.text = message.substring(message.indexOf(":") + 1)
                    }

                    if (message.contains("CurrentLocale") /*CurrentLocale:en-US*/) {
                        currentLocale = message.substring(message.indexOf(":") + 1)
                        updateLang()
                    }

                    if (message.contains("speechvolume") /*speechvolume:8,1*/) {
                        val speechvolume = message.substring(message.indexOf(":") + 1)
                        val mNode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(remoteDeviceIp)
                        if (mNode != null) {
                            mNode.speechVolume = speechvolume
                            var splitSpeechVolume: Array<String>? = null
                            if (!mNode.speechVolume.isNullOrEmpty()) {
                                LibreLogger.d(this, "mnNode.getSpeechVolume(): " + mNode.speechVolume)
                                splitSpeechVolume = mNode.speechVolume.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                                seekbarVolumeValue = splitSpeechVolume[0]
                                switchStatus = splitSpeechVolume[1]
                                Log.d("speechvolume", "seekbarVolumeValue: " + seekbarVolumeValue + "switchStatus: " + switchStatus)

                                if (switchStatus != null) {
                                    when {
                                        switchStatus!!.equals("-1", ignoreCase = true) -> {
                                            switchStatus = "0"
                                            tv_switch_status.text = getText(R.string.off).toString().toUpperCase()
                                        }
                                        switchStatus!!.equals("0", ignoreCase = true) -> {
                                            switch_speech_volume_follow.isChecked = false
                                            tv_switch_status.text = getText(R.string.off).toString().toUpperCase()
                                        }
                                        switchStatus!!.equals("1", ignoreCase = true) -> {
                                            switch_speech_volume_follow.isChecked = true
                                            tv_switch_status.text = getText(R.string.on).toString().toUpperCase()
                                            val luciControl = LUCIControl(currentDeviceIp)
                                            luciControl.SendCommand(MIDCONST.MID_MIC, "SV:$seekbarVolumeValue,$switchStatus", LSSDPCONST.LUCI_SET)
                                        }
                                    }
                                }

                                if (seekbarVolumeValue != null) {
                                    seek_bar_volume.progress = seekbarVolumeValue?.toInt()!!
                                } else {
                                    seek_bar_volume.progress = 0
                                }
                                tv_volume_value.text = "${seek_bar_volume.progress}dB"
                            }
                        }
                    }

                    if (message.contains("AlexaRefreshToken")) {
                        closeLoader(login_progress_bar.id)
                        val token = message.substring(message.indexOf(":") + 1)
                        val mNode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(nettyData.getRemotedeviceIp())
                        if (mNode != null) {
                            mNode.alexaRefreshToken = token
                        }
                        if (token.isEmpty())
                            tv_amazon_login?.text = getString(R.string.logged_out)
                        else tv_amazon_login?.text = getString(R.string.logged_in)
                    }

                    val messageArray = message.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

                    if (messageArray.isEmpty())
                        return
                    val audioPreset = messageArray[0]
                    var status = ""
                    if (messageArray.size > 1) {
                        status = messageArray[1]
                    }
                    if (audioPreset != null && audioPreset.equals("audiopreset", ignoreCase = true) && !status.equals("0", ignoreCase = true)) {
                        return
                    }
                }

                91 -> {
                    val macID = String(packet.getpayload())
                    LibreLogger.d(this, "bhargav  mac is id $macID")
                }
                145 -> {
                    val audioMessage = String(packet.getpayload())
                    Log.d("AudioPresetValue", "Received Data is - $audioMessage")
                }
            }

        }

    }

    private fun requestLuciUpdates(){
        val luciPackets = ArrayList<LUCIPacket>()

        val ddmsSSIDLUCIPacket = LUCIPacket(
                LUCIMESSAGES.READ_DDMS_SSID.toByteArray(),
                LUCIMESSAGES.READ_DDMS_SSID.length.toShort(),
                MIDCONST.MID_ENV_READ.toShort(),
                LSSDPCONST.LUCI_GET.toByte())
        val ddmsPwdLUCIPacket = LUCIPacket(
                LUCIMESSAGES.READ_DDMS_PWD.toByteArray(),
                LUCIMESSAGES.READ_DDMS_PWD.length.toShort(),
                MIDCONST.MID_ENV_READ.toShort(),
                LSSDPCONST.LUCI_GET.toByte())
        val currentLocaleLUCIPacket = LUCIPacket(
                LUCIMESSAGES.READ_CURRENT_LOCALE.toByteArray(),
                LUCIMESSAGES.READ_CURRENT_LOCALE.length.toShort(),
                MIDCONST.MID_ENV_READ.toShort(),
                LSSDPCONST.LUCI_GET.toByte())

        val alexaRefreshTokenPacket = LUCIPacket(
                LUCIMESSAGES.READ_ALEXA_REFRESH_TOKEN_MSG.toByteArray(),
                LUCIMESSAGES.READ_ALEXA_REFRESH_TOKEN_MSG.length.toShort(),
                MIDCONST.MID_ENV_READ.toShort(),
                LSSDPCONST.LUCI_GET.toByte())

        luciPackets.add(ddmsSSIDLUCIPacket)
        luciPackets.add(ddmsPwdLUCIPacket)

        if (currentDeviceNode?.getmDeviceCap()?.getmSource()?.isAlexaAvsSource!!) {
            luciPackets.add(currentLocaleLUCIPacket)
            luciPackets.add(alexaRefreshTokenPacket)
        }

        luciControl.SendCommand(luciPackets)

        luciControl.SendCommand(MIDCONST.MID_ENV_READ, LUCIMESSAGES.READ_AUDIO_PRESET, LSSDPCONST.LUCI_SET)

        luciControl.SendCommand(145, null, LSSDPCONST.LUCI_GET)

        luciControl.SendCommand(MIDCONST.MID_ENV_READ, LUCIMESSAGES.READ_SPEECH_VOLUME, LSSDPCONST.LUCI_SET)
    }

    private fun updateLang() {
        when (currentLocale) {
            LibreAlexaConstants.Languages.ENG_US -> {
                tv_alexa_locale.text = getString(R.string.engUSLang)
            }
            LibreAlexaConstants.Languages.ENG_GB -> {
                tv_alexa_locale.text = getString(R.string.engUKLang)
            }
            LibreAlexaConstants.Languages.DE -> {
                tv_alexa_locale.text = getString(R.string.deutschLang)
            }
        }

        closeLoader(locale_progress_bar?.id!!)
    }

    fun sendUpdatedLangToDevice(selectedLang:String) {
        currentLocale = selectedLang
        updateLang()
        luciControl.SendCommand(MIDCONST.ALEXA_COMMAND.toInt(), LUCIMESSAGES.UPDATE_LOCALE + currentLocale, LSSDPCONST.LUCI_SET)
    }

    private fun showAwayModeAlert(ssid:String,pwd:String) {
        if (awayModeSettingsDialog != null && awayModeSettingsDialog?.isShowing!!)
            awayModeSettingsDialog?.dismiss()

        val builder = AlertDialog.Builder(this)
        val customView = layoutInflater.inflate(R.layout.ct_dlg_fragment_edit_away_mode, null)
        builder.setView(customView)
        builder.setCancelable(false)

        customView?.et_network_name?.apply {
            setText(ssid)
            post {
                setSelection(length())
            }
        }

        customView?.et_pwd?.apply {
            setText(pwd)
            post {
                setSelection(length())
            }
        }

        customView?.btn_ok?.setOnClickListener {

            if (customView?.et_network_name?.text?.isEmpty()!!){
                showToast("Enter network name")
                return@setOnClickListener
            }

            if (customView?.et_pwd?.text?.isEmpty()!!){
                showToast("Enter password")
                return@setOnClickListener
            }

            if (customView?.et_network_name?.text.toString() != ssid
                    || customView?.et_pwd?.text.toString() != pwd){
                closeKeyboard(this,it)
                /*Write env items to device*/
                writeAwayModeSettingsToDevice(
                        customView.et_network_name.text.toString().trim(),
                        customView.et_pwd.text.toString().trim(),
                        currentDeviceIp!!)
                awayModeSettingsDialog?.dismiss()
            }
        }

        customView.btn_cancel.setOnClickListener {
            closeKeyboard(this,it)
            awayModeSettingsDialog?.dismiss()
        }

        if (awayModeSettingsDialog == null || !awayModeSettingsDialog?.isShowing!!)
            awayModeSettingsDialog = builder.create()
        awayModeSettingsDialog?.show()

    }

    override fun onStop() {
        super.onStop()
        unRegisterForDeviceEvents()
    }
}
