package com.cumulations.libreV2.activity

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.cumulations.libreV2.AppUtils
import com.cumulations.libreV2.closeKeyboard
import com.cumulations.libreV2.fragments.CTAlexaLocaleDialogFragment
import com.cumulations.libreV2.fragments.CTAudioOutputDialogFragment
import com.cumulations.libreV2.isConnectedToSAMode
import com.cumulations.libreV2.tcp_tunneling.*
import com.cumulations.libreV2.tcp_tunneling.enums.AQModeSelect
import com.cumulations.libreV2.tcp_tunneling.enums.PayloadType
import com.cumulations.libreV2.writeAwayModeSettingsToDevice
import com.libre.qactive.R
import com.libre.qactive.Scanning.Constants
import com.libre.qactive.Scanning.ScanningHandler
import com.libre.qactive.alexa.LibreAlexaConstants
import com.libre.qactive.constants.LSSDPCONST
import com.libre.qactive.constants.LUCIMESSAGES
import com.libre.qactive.constants.MIDCONST
import com.libre.qactive.luci.*
import com.libre.qactive.netty.LibreDeviceInteractionListner
import com.libre.qactive.netty.NettyData
import com.libre.qactive.util.LibreLogger
import kotlinx.android.synthetic.main.ct_activity_media_sources.*
import kotlinx.android.synthetic.main.ct_device_settings.*
import kotlinx.android.synthetic.main.ct_device_settings.iv_back
import kotlinx.android.synthetic.main.ct_device_settings.seek_bar_volume
import kotlinx.android.synthetic.main.ct_device_settings.toolbar
import kotlinx.android.synthetic.main.ct_device_settings.tv_device_name
import kotlinx.android.synthetic.main.ct_dlg_fragment_edit_away_mode.view.*


class CTDeviceSettingsActivity : CTDeviceDiscoveryActivity(), LibreDeviceInteractionListner {
    private var currentLocale: String? = null
    private var currentDeviceNode: LSSDPNodes? = null
    private lateinit var luciControl: LUCIControl
    private var switchStatus: String? = null
    private var seekbarVolumeValue: String? = null
    private var DeviceName: String? = null
    internal var mDeviceNameChanged = false

    private val currentDeviceIp by lazy {
        intent?.getStringExtra(Constants.CURRENT_DEVICE_IP)
    }

    private var awayModeSettingsDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ct_device_settings)


        ll_source_config.setOnClickListener {
            val intent = Intent(this@CTDeviceSettingsActivity, CTSourceSettingsActivity::class.java)
            val bundle = Bundle()
            val lssdpNodes = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(currentDeviceIp)
            bundle.putString("deviceFriendlyName",lssdpNodes.friendlyname)
            bundle.putString(Constants.CURRENT_DEVICE_IP,lssdpNodes.ip)
            intent.putExtras(bundle)
            startActivity(intent)
        }


        ll_user_reg.setOnClickListener {
            val intent = Intent(this@CTDeviceSettingsActivity, CTUserRegistration::class.java)
            val bundle = Bundle()
            bundle.putString(Constants.CURRENT_DEVICE_IP,currentDeviceIp)
            intent.putExtras(bundle)
            startActivity(intent)
        }

        ll_firmware_update_4_hours.setOnClickListener {
            val intent = Intent(this@CTDeviceSettingsActivity, CTFirmwareUpdateNotificationActivity4Hours::class.java)
            val bundle = Bundle()
            val lssdpNodes = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(currentDeviceIp)
            bundle.putString("deviceFriendlyName",lssdpNodes.friendlyname)
            intent.putExtras(bundle)
            startActivity(intent)
        }

        ll_firmware_update_3_min.setOnClickListener {
            val intent = Intent(this@CTDeviceSettingsActivity, CTFirmwareUpdateNotificationsActivity3Min::class.java)
            val bundle = Bundle()
            val lssdpNodes = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(currentDeviceIp)
            bundle.putString("deviceFriendlyName",lssdpNodes.friendlyname)
            intent.putExtras(bundle)
            startActivity(intent)
        }









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
        //   var searchString = findViewById<View>(R.id.tv_device_name) as EditText

        val lssdpNodes = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(currentDeviceIp)

        Handler().postDelayed({
            if (isFinishing)
                return@postDelayed
            closeLoader(audio_progress_bar.id)
            closeLoader(system_firmware_progress_bar.id)
            closeLoader(host_firmware_progress_bar.id)
            closeLoader(network_name_progress_bar.id)
            closeLoader(login_progress_bar.id)
            closeLoader(locale_progress_bar.id)
        }, Constants.ITEM_CLICKED_TIMEOUT.toLong())

        currentDeviceNode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(currentDeviceIp)

        tv_toolbar_title.text = currentDeviceNode?.friendlyname
        //   tv_toolbar_title?.post {
        tv_toolbar_title?.isSelected = true
        // }
        tv_device_name.setText(currentDeviceNode?.friendlyname)
        DeviceName = tv_device_name.text.toString()
        tv_device_name.isEnabled = false;

        //    tv_device_name.text = currentDeviceNode?.friendlyname

        //searchString=currentDeviceNode?.friendlyname

        if (currentDeviceNode != null && !currentDeviceNode?.version.isNullOrEmpty()) {
            var dutExistingFirmware = currentDeviceNode?.version

            val arrayString = dutExistingFirmware?.split("\\.".toRegex())?.dropLastWhile { it.isEmpty() }?.toTypedArray()
            dutExistingFirmware = dutExistingFirmware?.substring(0, dutExistingFirmware.indexOf('.'))
            val dutExistingHostVersion = arrayString?.get(1)?.replace("[a-zA-z]".toRegex(), "")
            /*String mFirmwareVersionToDisplay = dutExistingFirmware.replaceAll("[a-zA-z]", "")+"."+arrayString[1]+"."+
                    arrayString[2];*/
//            tv_system_firmware.text = dutExistingFirmware.replace("[a-zA-z]".toRegex(), "")
            tv_system_firmware.text = currentDeviceNode?.version
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

        closeLoader(soft_update_progress_bar.id)

        val ssid = AppUtils.getConnectedSSID(this)

        if (currentDeviceNode != null) {
            if (currentDeviceNode?.getmDeviceCap()?.getmSource()?.isAlexaAvsSource!!
                    && ssid != null && !isConnectedToSAMode(ssid)) {
                if (lssdpNodes.getgCastVerision() == null) {
                    //gcast != null -> hide alexa
                    ll_alexa_settings.visibility = View.GONE
                }else{
                    ll_alexa_settings?.visibility = View.VISIBLE
                }
            } else {
                closeLoader(login_progress_bar.id)
                closeLoader(locale_progress_bar.id)
                ll_alexa_settings?.visibility = View.GONE
            }
        }

        if (lssdpNodes.getgCastVerision() != null) {
            //gcast != null -> hide alexa
                llAlexaSpeechVol.visibility = View.GONE
        }else{
            llAlexaSpeechVol.visibility = View.VISIBLE
        }




//        if (TunnelingControl.isTunnelingClientPresent(currentDeviceIp)){
//            toggleTunnelingVisibility(show = true)
//        } else toggleTunnelingVisibility(show = false)

//        toggleTunnelingVisibility(show = TunnelingControl.isTunnelingClientPresent(currentDeviceIp))
    }

    private fun setListeners() {
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
                    putString(Constants.CURRENT_LOCALE, currentLocale)
                }

                show(supportFragmentManager, this::class.java.simpleName)
            }
        }

        switch_speech_volume_follow.setOnCheckedChangeListener { compoundButton, b ->
            val luciControl = LUCIControl(currentDeviceIp)
            if (b) {
                tv_switch_status.text = getText(R.string.on).toString().toUpperCase()
                switchStatus = "1"
                speechVolume.setVisibility(View.INVISIBLE);
                LibreLogger.d(this, "suma in speech volume invisible")
                luciControl.SendCommand(MIDCONST.MID_MIC, "SV:$seekbarVolumeValue,$switchStatus", LSSDPCONST.LUCI_SET)
            } else {
                tv_switch_status.text = getText(R.string.off).toString().toUpperCase()
                switchStatus = "0"
                luciControl.SendCommand(MIDCONST.MID_MIC, "SV:$seekbarVolumeValue,$switchStatus", LSSDPCONST.LUCI_SET)
                speechVolume.setVisibility(View.VISIBLE);
                LibreLogger.d(this, "suma in speech volume visible")

            }
        }


        /*suma adding edittext editting*/

        btnEditSceneName?.setOnClickListener {
            LibreLogger.d(this, "suma is edit device name true\n" + tv_device_name.isEnabled)
            if (!tv_device_name.isEnabled) {

                val mNode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(currentDeviceIp)
                if (mNode != null && mNode.getgCastVerision() != null) {
                    // Toast.makeText(LSSDPDeviceNetworkSettings.this, getString(R.string.castDeviceNameChangeMsg), Toast.LENGTH_SHORT).show();
                    return@setOnClickListener
                }

                LibreLogger.d(this, "bhargav123 clicked")
                btnEditSceneName.setImageResource(R.mipmap.check)
                tv_device_name.isClickable = true
                tv_device_name.isEnabled = true
                tv_device_name.isFocusableInTouchMode = true
                tv_device_name.isFocusable = true
                tv_device_name.requestFocus()
                tv_device_name.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.cancwel, 0)
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(tv_device_name, InputMethodManager.SHOW_IMPLICIT)

            } else {
                LibreLogger.d(this, "bhargav123 clicked again")
                btnEditSceneName.setImageResource(R.mipmap.edit_white)
                tv_device_name.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
//                                    tv_device_name.setClickable(false);
//                                    tv_device_name.setEnabled(false);
                if (mDeviceNameChanged) {
                    if (tv_device_name.text.toString() != "" && !tv_device_name.text.toString().trim({ it <= ' ' }).equals("NULL", ignoreCase = true)) {
                        if (tv_device_name.getText().toString().toByteArray().size > 50) {
                            android.app.AlertDialog.Builder(this@CTDeviceSettingsActivity)
                                    .setTitle(getString(R.string.deviceNameChanging))
                                    .setMessage(getString(R.string.deviceLength))
                                    .setPositiveButton("Yess") { dialog, which -> dialog.cancel() }.setIcon(android.R.drawable.ic_dialog_alert)
                                    .show()
                            return@setOnClickListener
                        } else {
                            val mLuci = LUCIControl(currentDeviceIp)
                            //  mLuci.sendAsynchronousCommand()
                            mLuci.SendCommand(MIDCONST.MID_DEVNAME, tv_device_name.getText().toString(), LSSDPCONST.LUCI_SET)

                            currentDeviceIp?.let { it1 -> UpdateLSSDPNodeDeviceName(it1, tv_device_name.getText().toString()) }
                            tv_device_name.setClickable(false)
                            tv_device_name.setEnabled(false)
                        }
                    } else {
//                        if (!this@LSSDPDeviceNetworkSettings.isFinishing()) {
//                            if (deviceName.getText().toString() == "") {
//                                android.app.AlertDialog.Builder(this@LSSDPDeviceNetworkSettings)
//                                        .setTitle(getString(R.string.deviceNameChanging))
//                                        .setMessage(getString(R.string.deviceNameEmpty))
//                                        .setPositiveButton(getString(R.string.yes)) { dialog, which -> dialog.cancel() }.setIcon(android.R.drawable.ic_dialog_alert)
//                                        .show()
//                            }
//                        }
                    }


                }
            }
        }


        val mTextWatching = arrayOfNulls<String>(1)
        tv_device_name.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable) {
                Log.d("Karuna", "After Text Changed")
                if (tv_device_name.text.toString().toByteArray().size == 50 && tv_device_name.isEnabled) {
                    Toast.makeText(this@CTDeviceSettingsActivity, "DEvice length Reached", Toast.LENGTH_SHORT).show()

                }

                if (tv_device_name.text.toString().toByteArray().size > 50) {
                    if (mTextWatching[0]?.isEmpty() ?: mTextWatching[0] == null) {

                        tv_device_name.setText(utf8truncate(tv_device_name.text.toString(), 50))
                        mTextWatching[0]?.length?.let { tv_device_name.setSelection(it) }
                    } else {
                        tv_device_name.setText(mTextWatching[0])
                        mTextWatching[0]?.length?.let { tv_device_name.setSelection(it) }
                    }
                    Toast.makeText(this@CTDeviceSettingsActivity, getString(R.string.deviceLength), Toast.LENGTH_SHORT).show()/*
                    LibreError error = new LibreError("Sorry!!!!", getString(R.string.deviceLength));
                    showErrorMessage(error);*/
                    /*  new AlertDialog.Builder(LSSDPDeviceNetworkSettings.this)
                            .setTitle(getString(R.string.deviceNameChanging))
                            .setMessage(getString(R.string.deviceLength))
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();

                                }
                            }).setIcon(android.R.drawable.ic_dialog_alert)
                            .show();*/
                } else {
                    mTextWatching[0] = tv_device_name.text.toString()
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) {
                Log.d("Karuna", "Before Text Changed")
            }

            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {
                Log.d("Karuna", "On Text Changed")
                // if(! deviceName.isClickable()) {
                mDeviceNameChanged = true
            }
        })

        tv_device_name.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val DRAWABLE_RIGHT = 2
                if (event.action == MotionEvent.ACTION_UP) {
                    try {
                        if (event.rawX >= tv_device_name.right - tv_device_name.compoundDrawables[DRAWABLE_RIGHT].bounds.width()) {
                            // your action here
                            tv_device_name.setText("")
                            return true
                        }
                    } catch (e: Exception) {
                        //Toast.makeText(getApplication(), "dsd", Toast.LENGTH_SHORT).show();
                        LibreLogger.d(this, "ignore this log")
                    }

                }
                return false
            }
        })






        seek_bar_volume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                LibreLogger.d(this@CTDeviceSettingsActivity, "Seekbar Position track " + seekBar.progress + "  " + seekBar.max)
                tv_volume_value.text = "" + progress + "dB"
                seekbarVolumeValue = "" + progress
                if (fromUser) {
                    val luciControl = LUCIControl(currentDeviceIp)
                    luciControl.SendCommand(MIDCONST.MID_MIC, "SV:$seekbarVolumeValue,$switchStatus", 2)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                LibreLogger.d(this@CTDeviceSettingsActivity, "Seekbar Position trackstop" + seekBar.progress + "  " + seekBar.max)
            }
        })

        seek_bar_bass.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                /*LibreLogger.d(this, "seek_bar_bass " + seekBar.progress + "  " + seekBar.max)
                tv_bass_value.text = "${progress-5}dB"

                TunnelingControl(currentDeviceIp).sendCommand(PayloadType.BASS_VOLUME, (progress-5).toByte())*/
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                LibreLogger.d(this@CTDeviceSettingsActivity, "seek_bar_bass " + seekBar.progress + "  " + seekBar.max)
//                tv_bass_value.text = "${seekBar.progress - 5}dB"
                TunnelingControl(currentDeviceIp).sendCommand(PayloadType.BASS_VOLUME, seekBar.progress.toByte())
            }
        })

        seek_bar_treble.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                /*LibreLogger.d(this, "seek_bar_treble " + seekBar.progress + "  " + seekBar.max)
                tv_treble_value.text = "${progress-5}dB"

                TunnelingControl(currentDeviceIp).sendCommand(PayloadType.TREBLE_VOLUME, (progress-5).toByte())*/
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                LibreLogger.d(this@CTDeviceSettingsActivity, "seek_bar_treble " + seekBar.progress + "  " + seekBar.max)
//                tv_treble_value.text = "${seekBar.progress - 5}dB"
                TunnelingControl(currentDeviceIp).sendCommand(PayloadType.TREBLE_VOLUME, seekBar.progress.toByte())
            }
        })

        tv_edit_away_mode?.setOnClickListener {
            showAwayModeAlert(tv_network_name.text.toString(), tv_wifi_pwd.text.toString())
        }

        iv_back?.setOnClickListener {
            onBackPressed()
        }

        tv_audio_output?.setOnClickListener {
            if (tv_audio_output?.text?.toString().isNullOrEmpty())
                return@setOnClickListener

            val audioOutput = tv_audio_output?.text?.toString()

            CTAudioOutputDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(Constants.AUDIO_OUTPUT, audioOutput)
                }

                show(supportFragmentManager, this::class.java.simpleName)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerForDeviceEvents(this)
        requestLuciUpdates()
        enableOrDisableEditDeviceNameButton();
        tv_device_name.isEnabled == false;
        tv_device_name.setText(currentDeviceNode?.getFriendlyname())
        DeviceName = tv_device_name.getText().toString()

        TunnelingControl(currentDeviceIp).sendDataModeCommand()
    }

    private fun enableOrDisableEditDeviceNameButton() {
        val mNode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(currentDeviceIp)
        if (mNode != null && mNode.getgCastVerision() != null) {
            /*we need to show toast so don't hide*/
            btnEditSceneName.setVisibility(View./*INVISIBLE*/VISIBLE)
        } else {
            btnEditSceneName.setVisibility(View.VISIBLE)
        }
    }

    private fun showLoader(progressBarId: Int) {
        findViewById<ProgressBar>(progressBarId).visibility = View.VISIBLE
        when (progressBarId) {
            R.id.audio_progress_bar -> tv_audio_output?.visibility = View.INVISIBLE
            R.id.system_firmware_progress_bar -> tv_system_firmware?.visibility = View.INVISIBLE
            R.id.host_firmware_progress_bar -> tv_host_firmware?.visibility = View.INVISIBLE
            R.id.network_name_progress_bar -> tv_network_name?.visibility = View.INVISIBLE
            R.id.login_progress_bar -> tv_amazon_login?.visibility = View.INVISIBLE
            R.id.locale_progress_bar -> tv_alexa_locale?.visibility = View.INVISIBLE
            R.id.soft_update_progress_bar -> tv_soft_update?.visibility = View.INVISIBLE
        }
    }

    private fun closeLoader(progressBarId: Int) {
        findViewById<ProgressBar>(progressBarId).visibility = View.INVISIBLE
        when (progressBarId) {
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
            intentToHome(this)
        }
    }

    override fun messageRecieved(nettyData: NettyData) {

        val remoteDeviceIp = nettyData.getRemotedeviceIp()

        val packet = LUCIPacket(nettyData.getMessage())
        LibreLogger.d(this, "Message received for ipaddress " + remoteDeviceIp + ", command is " + packet.command)

        if (currentDeviceIp!! == remoteDeviceIp) {

            LibreLogger.d(this, "Command = " + packet.command + ", payload msg = ${String(packet.payload)}")
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

    fun UpdateLSSDPNodeDeviceName(ipaddress: String, mDeviceName: String) {

        val mToBeUpdateNode = ScanningHandler.getInstance().getLSSDPNodeFromCentralDB(ipaddress)

        val mNodeDB = LSSDPNodeDB.getInstance()
        if (mToBeUpdateNode != null) {
            mToBeUpdateNode!!.setFriendlyname(mDeviceName)
            mNodeDB.renewLSSDPNodeDataWithNewNode(mToBeUpdateNode)
        }

    }

    private fun requestLuciUpdates() {
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

        val readSpeechVolumePacket = LUCIPacket(
                LUCIMESSAGES.READ_SPEECH_VOLUME.toByteArray(),
                LUCIMESSAGES.READ_SPEECH_VOLUME.length.toShort(),
                MIDCONST.MID_ENV_READ.toShort(),
                LSSDPCONST.LUCI_GET.toByte())

        luciPackets.add(ddmsSSIDLUCIPacket)
        luciPackets.add(ddmsPwdLUCIPacket)
        luciPackets.add(readSpeechVolumePacket)


        if (currentDeviceNode != null) {
            if (currentDeviceNode?.getmDeviceCap()?.getmSource()?.isAlexaAvsSource!!) {
                luciPackets.add(currentLocaleLUCIPacket)
                luciPackets.add(alexaRefreshTokenPacket)

            }
        }
        luciControl.SendCommand(luciPackets)
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

    /*Suma edittext issue Fixes*/
    fun utf8truncate(input: String, length: Int): String {
        val result = StringBuffer(length)
        var resultlen = 0
        for (i in 0 until input.length) {
            val c = input[i]
            var charlen = 0
            if (c.toInt() <= 0x7f) {
                charlen = 1
            } else if (c.toInt() <= 0x7ff) {
                charlen = 2
            } else if (c.toInt() <= 0xd7ff) {
                charlen = 3
            } else if (c.toInt() <= 0xdbff) {
                charlen = 4
            } else if (c.toInt() <= 0xdfff) {
                charlen = 0
            } else if (c.toInt() <= 0xffff) {
                charlen = 3
            }
            if (resultlen + charlen > length) {
                break
            }
            result.append(c)
            resultlen += charlen
        }
        return result.toString()
    }

    fun sendUpdatedLangToDevice(selectedLang: String) {
        currentLocale = selectedLang
        updateLang()
        luciControl.SendCommand(MIDCONST.ALEXA_COMMAND.toInt(), LUCIMESSAGES.UPDATE_LOCALE + currentLocale, LSSDPCONST.LUCI_SET)
    }

    fun updateAudioOutputOfDevice(aqModeSelect: AQModeSelect) {
        showLoader(audio_progress_bar.id)
//        tv_audio_output?.text = aqModeSelect.name
        TunnelingControl(currentDeviceIp).sendCommand(PayloadType.AQ_MODE_SELECT, aqModeSelect.value.toByte())
    }

    private fun showAwayModeAlert(ssid: String, pwd: String) {
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

            if (customView.et_network_name?.text?.isEmpty()!!) {
                showToast("Enter network name")
                return@setOnClickListener
            }

            if (customView.et_pwd?.text?.isEmpty()!!) {
                showToast("Enter password")
                return@setOnClickListener
            }

            if (customView.et_network_name?.text.toString() != ssid
                    || customView.et_pwd?.text.toString() != pwd) {
                closeKeyboard(this, it)
                /*Write env items to device*/
                writeAwayModeSettingsToDevice(
                        customView.et_network_name.text.toString().trim(),
                        customView.et_pwd.text.toString().trim(),
                        currentDeviceIp!!)
                awayModeSettingsDialog?.dismiss()
            }
        }

        customView.btn_cancel.setOnClickListener {
            closeKeyboard(this, it)
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

    override fun tunnelDataReceived(tunnelingData: TunnelingData) {
        super.tunnelDataReceived(tunnelingData)
        if (tunnelingData.remoteClientIp == currentDeviceIp && tunnelingData.remoteMessage.size >= 24) {
            val tcpTunnelPacket = TCPTunnelPacket(tunnelingData.remoteMessage)

            LibreLogger.d(this, "tunnelDataReceived, ip ${tunnelingData.remoteClientIp} treble ${tcpTunnelPacket.trebleValue}")
            if (tcpTunnelPacket.trebleValue >= 0) {
                tv_treble_value.text = "${tcpTunnelPacket.trebleValue - 5}dB"
                seek_bar_treble?.progress = tcpTunnelPacket.trebleValue
                seek_bar_treble?.max = 10
            }

            LibreLogger.d(this, "tunnelDataReceived, ip ${tunnelingData.remoteClientIp} bass ${tcpTunnelPacket.bassValue}")
            if (tcpTunnelPacket.bassValue >= 0) {
                tv_bass_value.text = "${tcpTunnelPacket.bassValue - 5}dB"
                seek_bar_bass?.progress = tcpTunnelPacket.bassValue
                seek_bar_bass?.max = 10
            }

            tv_audio_output?.text = tcpTunnelPacket.aqMode?.name
            closeLoader(audio_progress_bar.id)

        }
    }

//    private fun toggleTunnelingVisibility(show:Boolean){
//        if (show) ll_tunneling_controls?.visibility = View.VISIBLE
//        else ll_tunneling_controls?.visibility = View.GONE
//    }
}
