package com.cumulations.libreV2.activity

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import androidx.appcompat.app.AlertDialog
import android.util.Log
import android.view.View
import android.widget.Toast
import com.amazon.identity.auth.device.AuthError
import com.amazon.identity.auth.device.authorization.api.AmazonAuthorizationManager
import com.amazon.identity.auth.device.authorization.api.AuthorizationListener
import com.amazon.identity.auth.device.authorization.api.AuthzConstants
import com.cumulations.libreV2.AppConstants
import com.libre.qactive.LErrorHandeling.LibreError
import com.libre.qactive.R
import com.libre.qactive.Scanning.Constants
import com.libre.qactive.alexa.AlexaUtils
import com.libre.qactive.alexa.CompanionProvisioningInfo
import com.libre.qactive.alexa.DeviceProvisioningInfo
import com.libre.qactive.alexa.LibreAlexaConstants.*
import com.libre.qactive.constants.AlexaConstants
import com.libre.qactive.constants.LSSDPCONST
import com.libre.qactive.constants.LUCIMESSAGES
import com.libre.qactive.constants.MIDCONST
import com.libre.qactive.luci.LSSDPNodeDB
import com.libre.qactive.luci.LSSDPNodes
import com.libre.qactive.luci.LUCIControl
import com.libre.qactive.luci.LUCIPacket
import com.libre.qactive.netty.LibreDeviceInteractionListner
import com.libre.qactive.netty.NettyData
import com.libre.qactive.util.LibreLogger
import kotlinx.android.synthetic.main.ct_activity_amazon_login.*
import org.json.JSONException
import org.json.JSONObject

class CTAmazonLoginActivity : CTDeviceDiscoveryActivity(), View.OnClickListener, LibreDeviceInteractionListner {

    companion object {
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

    private var mProgressDialog: ProgressDialog? = null;

    private var isAuthorizedOnBackpress = false;

    @SuppressLint("HandlerLeak")
    private val handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            if (msg.what == ACCESS_TOKEN_TIMEOUT || msg.what == ALEXA_META_DATA_TIMER) {
                LibreLogger.d(this, "AmazonLogin_Auth" + "amazon timeout")
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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        this.intent = intent
    }

    override fun onResume() {
        super.onResume()

        try {
            mAuthManager = AmazonAuthorizationManager(this, Bundle.EMPTY)

            setAlexaViews()

        } catch (e: Exception) {
            Log.d("atul_package_name", this@CTAmazonLoginActivity.packageName)
            LibreLogger.d(this, "amazon auth exception ${e.toString()}")
            disableViews()
            Toast.makeText(this, "" + e.message, Toast.LENGTH_SHORT).show()
        }

        registerForDeviceEvents(this)

        if (!isAuthorizedOnBackpress) {
            closeLoader()
        }
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

                if (speakerNode?.mdeviceProvisioningInfo == null || !isMetaDateRequestSent) {
                    showLoader()
                    AlexaUtils.sendAlexaMetaDataRequest(speakerIpAddress)
                    handler.sendEmptyMessageDelayed(ALEXA_META_DATA_TIMER, 15 * 1000)
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
//                        showLoader()
                        mAuthManager!!.authorize(APP_SCOPES, options, AuthListener())
                    }
                } catch (e: JSONException) {
                    closeLoader()
                    LibreLogger.d(this, "AmazonLogin_Auth" + "json ex")
                    e.printStackTrace()
                }

            }

            R.id.iv_back, R.id.tv_skip -> handleBackPressed()
        }
    }

    override fun deviceDiscoveryAfterClearingTheCacheStarted() {

    }

    override fun newDeviceFound(node: LSSDPNodes) {

    }

    override fun deviceGotRemoved(ipaddress: String) {

    }

    override fun messageRecieved(packet: NettyData) {
        LibreLogger.d(this, "AlexaSignInActivity: New message for " + packet.getRemotedeviceIp())
        val messagePacket = LUCIPacket(packet.getMessage())
        when (messagePacket.command) {
            MIDCONST.ALEXA_COMMAND.toInt() -> {

                val alexaMessage = String(messagePacket.getpayload())
                LibreLogger.d(this, "Alexa Value From 234  $alexaMessage")

                try {
                    if (alexaMessage?.isNotEmpty()) {
                        val jsonRootObject = JSONObject(alexaMessage)
                        if (jsonRootObject.has(AppConstants.TITLE)) {
                            val title = jsonRootObject.getString(AppConstants.TITLE)
                            if (title != null) {
                                if (title == AlexaConstants.ACCESS_TOKENS_STATUS) {
                                    val status = jsonRootObject.getBoolean(AppConstants.STATUS)
                                    handler.removeMessages(ACCESS_TOKEN_TIMEOUT)
                                    closeLoader()
                                    LibreLogger.d(this, "AmazonLogin_Auth" + "Access Token_Status")
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
//                                        closeLoader()
                                            btn_login_amazon?.performClick()
                                        }
                                    }
                                }
                            }
                        }
                    }

                } catch (e: JSONException) {
                    e.printStackTrace()
                    LibreLogger.d(this, "AmazonLogin_Auth" + "Json ex")
                    closeLoader()
                    handler.removeMessages(ALEXA_META_DATA_TIMER)
                    handler.removeMessages(ACCESS_TOKEN_TIMEOUT)
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
            mProgressDialog?.dismiss()
            mProgressDialog = null
//            dismissDialog()
//            progress_bar!!.visibility = View.GONE
        }
    }

    private fun showLoader() {
        runOnUiThread {
            mProgressDialog = ProgressDialog(this@CTAmazonLoginActivity, ProgressDialog.THEME_HOLO_DARK)
            mProgressDialog?.setCancelable(false)
            mProgressDialog?.setMessage(getString(R.string.pleaseWait))
            mProgressDialog?.show()
//            showProgressDialog(R.string.pleaseWait)
//            progress_bar!!.visibility = View.VISIBLE
        }
    }

    private inner class AuthListener : AuthorizationListener {
        override fun onSuccess(response: Bundle) {
            try {
                isAuthorizedOnBackpress = true
                showLoader()


                val authorizationCode = response.getString(AuthzConstants.BUNDLE_KEY.AUTHORIZATION_CODE.`val`)
                val redirectUri = mAuthManager!!.redirectUri
                val clientId = mAuthManager!!.clientId
                val sessionId = speakerNode!!.mdeviceProvisioningInfo.sessionId

                LibreLogger.d(this, "Alexa Value From 234ALEXA_COMMAND, session ID $sessionId")
                val companionProvisioningInfo = CompanionProvisioningInfo(sessionId, clientId, redirectUri, authorizationCode)
                val luciControl = LUCIControl(speakerIpAddress)
                luciControl.SendCommand(MIDCONST.ALEXA_COMMAND.toInt(), LUCIMESSAGES.AUTHCODE_EXCH + companionProvisioningInfo.toJson()!!.toString(), LSSDPCONST.LUCI_SET)
                Log.e("AuthListener", "companionProvisioningInfo during authorization" + companionProvisioningInfo.toJson()!!.toString())

                if (handler.hasMessages(ALEXA_META_DATA_TIMER))
                    handler.removeMessages(ALEXA_META_DATA_TIMER)
                handler.sendEmptyMessageDelayed(ACCESS_TOKEN_TIMEOUT, 15 * 1000)

            } catch (authError: AuthError) {
                LibreLogger.d(this, "AmazonLogin_Auth" + "Auth error")
                closeLoader()
                authError.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
                LibreLogger.d(this, "AmazonLogin_Auth" + "Auth error_2_exception")
                closeLoader()
            }

        }

        override fun onError(ae: AuthError) {
            closeLoader()
            LibreLogger.d(this, "AmazonLogin_AuthAuthError during authorization$ae")
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
            LibreLogger.d(this, "AmazonLogin_AuthUser cancelled authorization $cause")
            val finalError = "Login cancelled"
            closeLoader()
            runOnUiThread {
                if (!isFinishing) {
                    showAlertDialog(finalError)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
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
//        if (!from.isNullOrEmpty()) {
//            if (from!!.equals(CTConnectingToMainNetwork::class.java.simpleName, ignoreCase = true)) {
//                intentToHome(this)
//            } else if (from!!.equals(SourcesOptionActivity::class.java.simpleName, ignoreCase = true)
//                    || from == CTAlexaThingsToTryActivity::class.java.simpleName) {
//                val intent = Intent(this@CTAmazonLoginActivity, SourcesOptionActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                intent.putExtra(Constants.CURRENT_DEVICE_IP, speakerIpAddress)
//                startActivity(intent)
//                finish()
//            } else if (from!!.equals(CTDeviceSettingsActivity::class.java.simpleName, ignoreCase = true)) {
//                val intent = Intent(this@CTAmazonLoginActivity, CTDeviceSettingsActivity::class.java)
//                intent.putExtra(Constants.CURRENT_DEVICE_IP, speakerIpAddress)
//                startActivity(intent)
//                finish()
//            }
//        } else onBackPressed()
        if (!from.isNullOrEmpty() && from!!.equals(CTConnectingToMainNetwork::class.java.simpleName, ignoreCase = true)) {
            intentToHome(this)
        } else onBackPressed()
    }
}