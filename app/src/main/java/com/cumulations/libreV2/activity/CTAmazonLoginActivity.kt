package com.cumulations.libreV2.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import android.widget.Toast
import com.amazon.identity.auth.device.AuthError
import com.amazon.identity.auth.device.authorization.api.AmazonAuthorizationManager
import com.amazon.identity.auth.device.authorization.api.AuthorizationListener
import com.amazon.identity.auth.device.authorization.api.AuthzConstants
import com.cumulations.libreV2.AppConstants
import com.libre.LErrorHandeling.LibreError
import com.libre.R
import com.libre.Scanning.Constants
import com.libre.alexa.CompanionProvisioningInfo
import com.libre.alexa.DeviceProvisioningInfo
import com.libre.alexa.LibreAlexaConstants.*
import com.libre.alexa.userpoolManager.AlexaUtils.AlexaConstants
import com.libre.alexa_signin.AlexaUtils
import com.libre.constants.LSSDPCONST
import com.libre.constants.LUCIMESSAGES
import com.libre.constants.MIDCONST
import com.libre.luci.LSSDPNodeDB
import com.libre.luci.LSSDPNodes
import com.libre.luci.LUCIControl
import com.libre.luci.LUCIPacket
import com.libre.netty.LibreDeviceInteractionListner
import com.libre.netty.NettyData
import com.libre.util.LibreLogger
import kotlinx.android.synthetic.main.ct_activity_amazon_login.*
import org.json.JSONException
import org.json.JSONObject

class CTAmazonLoginActivity : CTDeviceDiscoveryActivity(), View.OnClickListener, LibreDeviceInteractionListner {

    companion object{
        const val ALEXA_META_DATA_TIMER = 0x12
        const val ACCESS_TOKEN_TIMEOUT = 301
    }
    private var mAuthManager: AmazonAuthorizationManager? = null

    private var deviceProvisioningInfo: DeviceProvisioningInfo? = null
    private val speakerIpAddress by lazy {
        intent.getStringExtra(Constants.CURRENT_DEVICE_IP)
    }
    private val from by lazy {
        intent.getStringExtra(Constants.FROM_ACTIVITY)
    }
    private var alertDialog: AlertDialog? = null
    private var speakerNode: LSSDPNodes? = null
    private var isMetaDateRequestSent = false

    @SuppressLint("HandlerLeak")
    private val handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            if (msg.what == ACCESS_TOKEN_TIMEOUT || msg.what == ALEXA_META_DATA_TIMER) {
                closeLoader()
                /*showing error*/
                val error = LibreError(speakerIpAddress, getString(R.string.requestTimeout))
                showErrorMessage(error)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ct_activity_amazon_login)

        if (intent != null) {
            speakerNode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(speakerIpAddress)

            if (intent?.hasExtra(AppConstants.DEVICE_PROVISIONING_INFO)!!) {
                deviceProvisioningInfo = intent.getSerializableExtra(AppConstants.DEVICE_PROVISIONING_INFO) as DeviceProvisioningInfo
            }

            if (deviceProvisioningInfo != null) {
                isMetaDateRequestSent = true
                speakerNode?.mdeviceProvisioningInfo = deviceProvisioningInfo
            }
        }

        iv_back.setOnClickListener(this)
        btn_login_amazon!!.setOnClickListener(this)
        tv_skip!!.setOnClickListener(this)
    }

    override fun onResume() {
        super.onResume()

        try {
            mAuthManager = AmazonAuthorizationManager(this, Bundle.EMPTY)
            setAlexaViews()
        } catch (e: Exception) {
            LibreLogger.d(this, "amazon auth exception" + e.message + "\n" + e.stackTrace)
            disableViews()
            Toast.makeText(this, "" + e.message, Toast.LENGTH_SHORT).show()
        }

        registerForDeviceEvents(this)
    }

    private fun disableViews() {
        btn_login_amazon!!.isEnabled = false
        btn_login_amazon!!.alpha = 0.5f
    }

    override fun onClick(view: View) {

        when (view.id) {

            R.id.btn_login_amazon -> {
                /*if (speakerNode == null) {
                    return
                }*/

                if (speakerNode == null || !isMetaDateRequestSent) {
                    showLoader()
                    AlexaUtils.sendAlexaMetaDataRequest(speakerIpAddress)
                    handler.sendEmptyMessageDelayed(ALEXA_META_DATA_TIMER, 15000)
                    return
                }


                val options = Bundle()
                val scopeData = JSONObject()
                val productInfo = JSONObject()
                val productInstanceAttributes = JSONObject()
                try {
                    productInstanceAttributes.put(DEVICE_SERIAL_NUMBER, speakerNode!!.mdeviceProvisioningInfo.dsn)
                    productInfo.put(PRODUCT_ID, speakerNode!!.mdeviceProvisioningInfo.productId)
                    productInfo.put(PRODUCT_INSTANCE_ATTRIBUTES, productInstanceAttributes)
                    scopeData.put(ALEXA_ALL_SCOPE, productInfo)

                    val codeChallenge = speakerNode!!.mdeviceProvisioningInfo.codeChallenge
                    val codeChallengeMethod = speakerNode!!.mdeviceProvisioningInfo.codeChallengeMethod

                    options.putString(AuthzConstants.BUNDLE_KEY.SCOPE_DATA.`val`, scopeData.toString())
                    options.putBoolean(AuthzConstants.BUNDLE_KEY.GET_AUTH_CODE.`val`, true)
                    options.putString(AuthzConstants.BUNDLE_KEY.CODE_CHALLENGE.`val`, codeChallenge)
                    options.putString(AuthzConstants.BUNDLE_KEY.CODE_CHALLENGE_METHOD.`val`, codeChallengeMethod)
                    if (mAuthManager != null) {
                        showLoader()
                        mAuthManager!!.authorize(APP_SCOPES, options, AuthListener())
                    }
                } catch (e: JSONException) {
                    closeLoader()
                    e.printStackTrace()
                }

            }

            R.id.iv_back,R.id.tv_skip -> handleBackPressed()

            /*R.id.tv_skip ->
                if (!from.isNullOrEmpty()) {
                    startActivity(Intent(this, CTAlexaThingsToTryActivity::class.java)
                            .putExtra(Constants.CURRENT_DEVICE_IP, speakerIpAddress)
                            .putExtra(Constants.FROM_ACTIVITY, from)
                            .putExtra(Constants.PREV_SCREEN, CTAmazonLoginActivity::class.java.simpleName))
                    finish()
                } else onBackPressed()*/
        }
    }

    override fun deviceDiscoveryAfterClearingTheCacheStarted() {

    }

    override fun newDeviceFound(node: LSSDPNodes) {

    }

    override fun deviceGotRemoved(ipaddress: String) {

    }

    override fun messageRecieved(packet: NettyData) {
        LibreLogger.d(this, "AlexaSignInActivity: New message appeared for the device " + packet.getRemotedeviceIp())
        val messagePacket = LUCIPacket(packet.getMessage())
        when (messagePacket.command) {
            MIDCONST.ALEXA_COMMAND.toInt() -> {

                val alexaMessage = String(messagePacket.getpayload())
                LibreLogger.d(this, "Alexa Value From 234  $alexaMessage")
                try {
                    val jsonRootObject = JSONObject(alexaMessage)
                    if (jsonRootObject.has(AppConstants.TITLE)) {
                        val title = jsonRootObject.getString(AppConstants.TITLE)
                        if (title != null) {
                            if (title == AlexaConstants.ACCESS_TOKENS_STATUS) {
                                val status = jsonRootObject.getBoolean(AppConstants.STATUS)
                                handler.removeMessages(ACCESS_TOKEN_TIMEOUT)
                                closeLoader()
                                if (status) {
                                    intentToThingToTryActivity()
                                } else {
                                    showSomethingWentWrongAlert(this@CTAmazonLoginActivity)
                                }
                            } else {
                                val jsonObject = jsonRootObject.getJSONObject(LUCIMESSAGES.ALEXA_KEY_WINDOW_CONTENT)
                                val productId = jsonObject.optString(LUCIMESSAGES.ALEXA_KEY_PRODUCT_ID)
                                val dsn = jsonObject.optString(LUCIMESSAGES.ALEXA_KEY_DSN)
                                val sessionId = jsonObject.optString(LUCIMESSAGES.ALEXA_KEY_SESSION_ID)
                                val codeChallenge = jsonObject.optString(LUCIMESSAGES.ALEXA_KEY_CODE_CHALLENGE)
                                val codeChallengeMethod = jsonObject.optString(LUCIMESSAGES.ALEXA_KEY_CODE_CHALLENGE_METHOD)
                                var locale = ""
                                if (jsonObject.has(LUCIMESSAGES.ALEXA_KEY_LOCALE))
                                    locale = jsonObject.optString(LUCIMESSAGES.ALEXA_KEY_LOCALE)
                                if (speakerNode != null) {
                                    val mDeviceProvisioningInfo = DeviceProvisioningInfo(productId, dsn, sessionId, codeChallenge, codeChallengeMethod, locale)
                                    speakerNode!!.mdeviceProvisioningInfo = mDeviceProvisioningInfo
                                    handler.removeMessages(ALEXA_META_DATA_TIMER)
                                    isMetaDateRequestSent = true
                                    setAlexaViews()
                                    if (isMetaDateRequestSent) {
                                        closeLoader()
                                        btn_login_amazon?.performClick()
                                    }
                                }
                            }
                        }
                    }

                } catch (e: JSONException) {
                    closeLoader()
                    e.printStackTrace()
                }
            }
        }
    }

    private fun setAlexaViews() {
        btn_login_amazon!!.isEnabled = true
        btn_login_amazon!!.alpha = 1f
        tv_skip!!.visibility = View.VISIBLE
    }

    private fun intentToThingToTryActivity() {
        val alexaLangScreen = Intent(this@CTAmazonLoginActivity, CTAlexaThingsToTryActivity::class.java)
        alexaLangScreen.putExtra(Constants.CURRENT_DEVICE_IP, speakerIpAddress)
        if (intent?.hasExtra(Constants.FROM_ACTIVITY)!!)
            alexaLangScreen.putExtra(Constants.FROM_ACTIVITY, from)
        alexaLangScreen.putExtra(Constants.PREV_SCREEN, CTAmazonLoginActivity::class.java.simpleName)
        startActivity(alexaLangScreen)
        finish()
    }

    private fun closeLoader() {
        runOnUiThread {
            dismissDialog()
//            progress_bar!!.visibility = View.GONE
        }
    }

    private fun showLoader() {
        runOnUiThread {
            showProgressDialog(R.string.pleaseWait)
//            progress_bar!!.visibility = View.VISIBLE
        }
    }

    private inner class AuthListener : AuthorizationListener {
        override fun onSuccess(response: Bundle) {
            try {
                val authorizationCode = response.getString(AuthzConstants.BUNDLE_KEY.AUTHORIZATION_CODE.`val`)
                val redirectUri = mAuthManager!!.redirectUri
                val clientId = mAuthManager!!.clientId
                val sessionId = speakerNode!!.mdeviceProvisioningInfo.sessionId

                LibreLogger.d(this, "Alexa Value From 234, session ID $sessionId")
                val companionProvisioningInfo = CompanionProvisioningInfo(sessionId, clientId, redirectUri, authorizationCode)
                val luciControl = LUCIControl(speakerIpAddress)
                luciControl.SendCommand(MIDCONST.ALEXA_COMMAND.toInt(), LUCIMESSAGES.AUTHCODE_EXCH + companionProvisioningInfo.toJson()!!.toString(), LSSDPCONST.LUCI_SET)
                Log.e("AuthListener", "companionProvisioningInfo during authorization" + companionProvisioningInfo.toJson()!!.toString())

                showLoader()
                if (handler.hasMessages(ALEXA_META_DATA_TIMER))
                    handler.removeMessages(ALEXA_META_DATA_TIMER)
                handler.sendEmptyMessageDelayed(ACCESS_TOKEN_TIMEOUT, Constants.CHECK_ALIVE_TIMEOUT)

            } catch (authError: AuthError) {
                closeLoader()
                authError.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
                closeLoader()
            }

        }

        override fun onError(ae: AuthError) {
            closeLoader()
            Log.e("LSSDPNETWORK", "AuthError during authorization", ae)
            var error: String? = ae.message
            if (error == null || error.isEmpty())
                error = ae.toString()
            val finalError = error
            runOnUiThread {
                if (!isFinishing) {
                    showAlertDialog(finalError)
                }
            }
        }

        override fun onCancel(cause: Bundle) {
            Log.e("LSSDPNETWORK", "User cancelled authorization $cause")
            val finalError = "Login cancelled"
            runOnUiThread {
                if (!isFinishing) {
                    showAlertDialog(finalError)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        closeLoader()
        unRegisterForDeviceEvents()
        handler.removeCallbacksAndMessages(null)
    }

    private fun showAlertDialog(error: String?) {

        if (alertDialog != null && alertDialog!!.isShowing)
            alertDialog!!.dismiss()

        val builder = AlertDialog.Builder(this@CTAmazonLoginActivity)
        builder.setTitle("Amazon Login Error")
        builder.setMessage(error)
        builder.setNeutralButton("Close") { dialogInterface, i -> alertDialog!!.dismiss() }

        if (alertDialog == null) {
            alertDialog = builder.create()
            alertDialog!!.show()
        }

    }

    private fun handleBackPressed() {
        /*if (!from.isNullOrEmpty()) {
            if (from!!.equals(CTConnectingToMainNetwork::class.java.simpleName, ignoreCase = true)) {
                intentToHome(this)
            } else if (from!!.equals(SourcesOptionActivity::class.java.simpleName, ignoreCase = true)
                    || from == CTAlexaThingsToTryActivity::class.java.simpleName) {
                *//*val intent = Intent(this@CTAmazonLoginActivity, SourcesOptionActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.putExtra(Constants.CURRENT_DEVICE_IP, speakerIpAddress)
                startActivity(intent)
                finish()*//*
            } else if (from!!.equals(CTDeviceSettingsActivity::class.java.simpleName, ignoreCase = true)) {
                val intent = Intent(this@CTAmazonLoginActivity, CTDeviceSettingsActivity::class.java)
                intent.putExtra(Constants.CURRENT_DEVICE_IP, speakerIpAddress)
                startActivity(intent)
                finish()
            }
        } else onBackPressed()*/
        if (!from.isNullOrEmpty() && from!!.equals(CTConnectingToMainNetwork::class.java.simpleName, ignoreCase = true)) {
            intentToHome(this)
        } else onBackPressed()
    }
}
