package com.cumulations.libreV2.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cumulations.libreV2.AppConstants;
import com.cumulations.libreV2.WifiUtil;
import com.libre.qactive.LErrorHandeling.LibreError;
import com.libre.qactive.LibreApplication;
import com.libre.qactive.Ls9Sac.GoogleCastUpdateAfterSac;
import com.cumulations.libreV2.model.WifiConnection;

import com.libre.qactive.R;
import com.libre.qactive.Scanning.Constants;
import com.libre.qactive.Scanning.ScanningHandler;
import com.libre.qactive.alexa.DeviceProvisioningInfo;
import com.libre.qactive.alexa.AlexaUtils;
import com.libre.qactive.constants.LUCIMESSAGES;
import com.libre.qactive.constants.MIDCONST;
import com.libre.qactive.luci.LSSDPNodeDB;
import com.libre.qactive.luci.LSSDPNodes;
import com.libre.qactive.luci.LUCIPacket;
import com.libre.qactive.netty.BusProvider;
import com.libre.qactive.netty.LibreDeviceInteractionListner;
import com.libre.qactive.netty.NettyData;
import com.libre.qactive.util.LibreLogger;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;

import static com.libre.qactive.Scanning.Constants.GCAST_COMPLETE;

public class CTConnectingToMainNetwork extends CTDeviceDiscoveryActivity implements LibreDeviceInteractionListner {
    private final WifiConnection wifiConnect = WifiConnection.getInstance();
    private WifiManager mWifiManager;
    private AlertDialog alertConnectingtoNetwork;
    private ProgressBar progressBar;
    private TextView mainMsgText,subMsgText;
    private String locale = "";
    private DeviceProvisioningInfo mDeviceProvisioningInfo;
    private WifiUtil wifiUtil;

    private boolean mRestartOfAllSockets = false;

    public static final String SAC_CURRENT_IPADDRESS = "sac_current_ipaddress";
    private String mSACConfiguredIpAddress = "";
    private String fwInternetUpgradeMessage = "";

    private AppCompatImageView setupProgressImage;
    private boolean mb223TimerRunning;
    public static final long OOPS_TIMEOUT = 45*1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        disableNetworkChangeCallBack();
        disableNetworkOffCallBack();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.ct_activity_device_connecting);

        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        progressBar = findViewById(R.id.setup_progress_bar);
        mainMsgText = findViewById(R.id.tv_setup_info);
        subMsgText = findViewById(R.id.please_wait_label);
        setupProgressImage = findViewById(R.id.setup_progress_image);

        wifiUtil = new WifiUtil(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        LibreLogger.d(this, "onStart");
//        registerForDeviceEvents(this);
        showProgressDialog();
        LibreApplication.isSacFlowStarted = true;
        /*Get Connected Ssid Name */
        if (getConnectedSSIDName(this).equals(wifiConnect.getMainSSID())) {
            mHandler.sendEmptyMessage(Constants.CONNECTED_TO_MAIN_SSID_SUCCESS);
        } else {
            mHandler.sendEmptyMessage(Constants.HTTP_POST_DONE_SUCCESSFULLY);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerForDeviceEvents(this);
    }

    @SuppressLint("HandlerLeak")
    private final
    Handler mHandler = new Handler() {
        @Override

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            LibreLogger.d(this, "Handler Message what " + msg.what);

            String ssid = getConnectedSSIDName(CTConnectingToMainNetwork.this);
            switch (msg.what){
                case Constants.HTTP_POST_DONE_SUCCESSFULLY:
                    Log.i("mHandler","HTTP_POST_DONE_SUCCESSFULLY, ssid = "+ssid);
                    setSetupInfoTexts(getString(R.string.mConnectingToMainNetwork) + " " + wifiConnect.getMainSSID(),
                            getString(R.string.pleaseWait));
                    if (ssid.equals(wifiConnect.mainSSID)) {
                        mHandler.removeMessages(Constants.CONNECTED_TO_MAIN_SSID_SUCCESS);
                        mHandler.sendEmptyMessage(Constants.CONNECTED_TO_MAIN_SSID_SUCCESS);
                        return;
                    }
                    wifiUtil.disconnectCurrentWifi();
                    wifiUtil.connectWiFiToSSID(wifiConnect.getMainSSID(), wifiConnect.getMainSSIDPwd(),wifiConnect.mainSSIDSec);
                    mHandler.sendEmptyMessageDelayed(Constants.CONNECTED_TO_MAIN_SSID_FAIL, OOPS_TIMEOUT);
                    break;

                case Constants.CONNECTED_TO_MAIN_SSID_SUCCESS:
                    Log.i("mHandler","CONNECTED_TO_MAIN_SSID_SUCCESS, ssid = "+ssid);
                    /* Removing Failed Callback */
                    mHandler.removeMessages(Constants.CONNECTED_TO_MAIN_SSID_FAIL);
                    if (ssid.equals(wifiConnect.getMainSSID())) {
                        cleanUpCode(false);
                        boolean mRestartOfAllSockets = false;
                        LibreLogger.d(this, "Connected To Main SSID " + wifiConnect.getMainSSID() + mRestartOfAllSockets);
                        LibreApplication.activeSSID = wifiConnect.getMainSSID();
//                        LSSDPNodeDB.getInstance().clearDB();
                        LibreApplication.LOCAL_IP = "";
                        LibreApplication.mCleanUpIsDoneButNotRestarted = false;
                        restartAllSockets();
                        mHandler.sendEmptyMessageDelayed(Constants.SEARCHING_FOR_DEVICE, 500);
                    }
                    break;

                case Constants.SEARCHING_FOR_DEVICE:
                    Log.i("mHandler","SEARCHING_FOR_DEVICE, ssid = "+ssid);
                    sendMSearchInIntervalOfTime();
                    mHandler.sendEmptyMessageDelayed(Constants.TIMEOUT_FOR_SEARCHING_DEVICE, OOPS_TIMEOUT);
                    LibreLogger.d(this, "Searching For The Device " + LibreApplication.sacDeviceNameSetFromTheApp);
                    setSetupInfoTexts(getString(R.string.setting_up_speaker),getString(R.string.pleaseWait));
                    break;

                case Constants.TIMEOUT_FOR_SEARCHING_DEVICE:
                case Constants.CONNECTED_TO_MAIN_SSID_FAIL:
                case Constants.FW_UPGRADE_REBOOT_TIMER:
                    Log.i("mHandler","TIMEOUT_FOR_SEARCHING_DEVICE||FW_UPGRADE_REBOOT_TIMER");
                    closeProgressDialog();
                    openOOPSScreen();
                    break;

                case Constants.CONFIGURED_DEVICE_FOUND:
                    Log.i("mHandler","CONFIGURED_DEVICE_FOUND");
                    closeProgressDialog();
                    goToNextScreen();
                    LibreLogger.d(this,"suma in connecting to main n/w New device found");
                    break;

                case Constants.CONNECTED_TO_DIFFERENT_SSID:
                    Log.i("mHandler","CONNECTED_TO_DIFFERENT_SSID");
                    showAlertDialogForClickingWrongNetwork();
                    break;

                case Constants.ALEXA_CHECK_TIMEOUT:
                    Log.i("mHandler","ALEXA_CHECK_TIMEOUT");
                    /*taking user to the usual screen flow ie configure speakers*/
                    mHandler.sendEmptyMessage(Constants.CONFIGURED_DEVICE_FOUND);
                    LibreLogger.d(this, "New Device Found  five, friendlyName =\n " + LibreApplication.sacDeviceNameSetFromTheApp);

                    LibreLogger.d(this,"suma in connecting to main n/w New device found");


                    break;

                case Constants.WAITING_FOR_223_MB:
                    Log.i("mHandler","WAITING_FOR_223_MB");
                    /*taking user to the usual screen flow ie configure speakers*/
                    mb223TimerRunning = false;
                    readAlexaToken(mSACConfiguredIpAddress);
                    break;
            }
        }
    };

    private void openOOPSScreen() {
        startActivity(new Intent(CTConnectingToMainNetwork.this, CTSetupFailedActivity.class));
        finish();
    }

    private final int MSEARCH_TIMEOUT_SEARCH = 2000;
    private Handler mTaskHandlerForSendingMSearch = new Handler();
    public boolean mBackgroundMSearchStoppedDeviceFound = false;
    private Runnable mMyTaskRunnableForMSearch = new Runnable() {
        @Override
        public void run() {
            LibreLogger.d(this, "My task is Sending 1 Minute Once M-Search");
            /* do what you need to do */
            if (mBackgroundMSearchStoppedDeviceFound)
                return;

            final LibreApplication application = (LibreApplication) getApplication();
            application.getScanThread().UpdateNodes();
            /* and here comes the "trick" */
            mTaskHandlerForSendingMSearch.postDelayed(this, MSEARCH_TIMEOUT_SEARCH);
        }
    };

    private void sendMSearchInIntervalOfTime() {
        mTaskHandlerForSendingMSearch.postDelayed(mMyTaskRunnableForMSearch, MSEARCH_TIMEOUT_SEARCH);
    }

    @Override
    public void onBackPressed() {
        /*disable back press*/
    }

    @Override
    public void deviceDiscoveryAfterClearingTheCacheStarted() {

    }

    @Override
    public void newDeviceFound(final LSSDPNodes node) {
        LibreLogger.d(this, "New Device Found one, friendlyName =\n " + node.getFriendlyname() +", gCastVersion = "+ node.getgCastVerision());
        LibreLogger.d(this, "New Device Found  two, friendlyName =\n " + LibreApplication.sacDeviceNameSetFromTheApp +", FriendlyName = "+ node.getFriendlyname());

        String time = DateFormat.getInstance().format(System.currentTimeMillis());

        //            if (LibreApplication.sacDeviceNameSetFromTheApp.equals(node.getFriendlyname())) {
//                LibreLogger.d(this, "Configured Device Found = " + node.getFriendlyname() +" at "+ time);
//                LibreLogger.d(this, "newDeviceFound, fwInternetUpgradeMessage = " + fwInternetUpgradeMessage);
//                mHandler.removeMessages(Constants.TIMEOUT_FOR_SEARCHING_DEVICE);
//                mSACConfiguredIpAddress = node.getIP();
//
//                if (fwInternetUpgradeMessage == null || fwInternetUpgradeMessage.isEmpty()){
//                    mHandler.sendEmptyMessageDelayed(Constants.WAITING_FOR_223_MB,Constants.INTERNET_PLAY_TIMEOUT);
//                    mb223TimerRunning = true;
//                    AlexaUtils.getDeviceUpdateStatus(mSACConfiguredIpAddress);
//                    return;
//                }
//
//                if (fwInternetUpgradeMessage.equals(Constants.NO_UPDATE)
//                        || fwInternetUpgradeMessage.equals(GCAST_COMPLETE)
//                        || fwInternetUpgradeMessage.equals(Constants.BATTERY_POWER)) {
//                    if (fwInternetUpgradeMessage.equals(GCAST_COMPLETE)) {
//                        mHandler.removeMessages(Constants.FW_UPGRADE_REBOOT_TIMER);
//                    }
//                    readAlexaToken(mSACConfiguredIpAddress);
//                }
//            }
        if (LibreApplication.sacDeviceNameSetFromTheApp.equals(node.getFriendlyname())) {
            LibreLogger.d(this, "Hurray !! New Device Found and same as Configured Device"
                    + node.getFriendlyname()
                    + node.getgCastVerision());
            mSACConfiguredIpAddress = node.getIP();
            if (node.getgCastVerision() != null) {
                mHandler.sendEmptyMessage(Constants.CONFIGURED_DEVICE_FOUND);
                LibreLogger.d(this, "suma in connecting to main n/w New device found callback");
            }
            else {
                /*changes specific to alexa check*/
                if (mSACConfiguredIpAddress != null && !mSACConfiguredIpAddress.isEmpty()) {
                    if (mHandler.hasMessages(Constants.ALEXA_CHECK_TIMEOUT)) {
                        mHandler.removeMessages(Constants.ALEXA_CHECK_TIMEOUT);
                        LibreLogger.d(this, "New Device Found  three, friendlyName =\n " + LibreApplication.sacDeviceNameSetFromTheApp +", FriendlyName = "+ node.getFriendlyname());

                    }
                    if (mHandler.hasMessages(Constants.TIMEOUT_FOR_SEARCHING_DEVICE))
                        mHandler.removeMessages(Constants.TIMEOUT_FOR_SEARCHING_DEVICE);
                    LibreLogger.d(this, "New Device Found  four, friendlyName =\n " + LibreApplication.sacDeviceNameSetFromTheApp +", FriendlyName = "+ node.getFriendlyname());

                    readAlexaToken(mSACConfiguredIpAddress);
                }
            }
        }
    }

    @Override
    public void deviceGotRemoved(String ipaddress) {

    }

    @Override
    public void messageRecieved(NettyData nettyData) {
        String nettyDataRemotedeviceIp = nettyData.getRemotedeviceIp();
        LUCIPacket packet = new LUCIPacket(nettyData.getMessage());
        String message = new String(packet.getpayload());

        LibreLogger.d(this, "messageRecieved, mb223TimerRunning = " + mb223TimerRunning);
        LibreLogger.d(this, "Message recieved ipAddress " + nettyDataRemotedeviceIp
                + ", command " + packet.getCommand()
                + ", message " + message + ", at " + System.currentTimeMillis());

        if (mSACConfiguredIpAddress.equalsIgnoreCase(nettyDataRemotedeviceIp)) {

            LSSDPNodes node = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(nettyDataRemotedeviceIp);
            if (node == null)
                return;
            if (!node.getFriendlyname().equals(LibreApplication.sacDeviceNameSetFromTheApp))
                return;

            String time = DateFormat.getInstance().format(System.currentTimeMillis());

            switch (packet.Command) {
                case MIDCONST.ALEXA_COMMAND:
                    LibreLogger.d(this, "Alexa Value From 230  " + message);
                    if (mb223TimerRunning) {
                        return;
                    }

                    if (message != null && !message.isEmpty()) {
                        /* Parse JSon */
                        try {
                            JSONObject jsonRootObject = new JSONObject(message);
                            // JSONArray jsonArray = jsonRootObject.optJSONArray("Window CONTENTS");
                            JSONObject jsonObject = jsonRootObject.getJSONObject(LUCIMESSAGES.ALEXA_KEY_WINDOW_CONTENT);
                            String productId = jsonObject.optString(LUCIMESSAGES.ALEXA_KEY_PRODUCT_ID);
                            String dsn = jsonObject.optString(LUCIMESSAGES.ALEXA_KEY_DSN);
                            String sessionId = jsonObject.optString(LUCIMESSAGES.ALEXA_KEY_SESSION_ID);
                            String codeChallenge = jsonObject.optString(LUCIMESSAGES.ALEXA_KEY_CODE_CHALLENGE);
                            String codeChallengeMethod = jsonObject.optString(LUCIMESSAGES.ALEXA_KEY_CODE_CHALLENGE_METHOD);
                            if (jsonObject.has(LUCIMESSAGES.ALEXA_KEY_LOCALE))
                                locale = jsonObject.optString(LUCIMESSAGES.ALEXA_KEY_LOCALE);
                            mDeviceProvisioningInfo = new DeviceProvisioningInfo(productId, dsn, sessionId, codeChallenge, codeChallengeMethod, locale);

                            if (node != null) {
                                node.setMdeviceProvisioningInfo(mDeviceProvisioningInfo);
                            }

                            mHandler.removeMessages(Constants.ALEXA_CHECK_TIMEOUT);
                           // if (fwInternetUpgradeMessage.equals(Constants.NO_UPDATE)
                                //    || fwInternetUpgradeMessage.equals(Constants.BATTERY_POWER)) {
                                mHandler.sendEmptyMessage(Constants.CONFIGURED_DEVICE_FOUND);
                          //  }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    break;

                case MIDCONST.MID_ENV_READ:

                    if (mb223TimerRunning) {
                        LibreLogger.d(this, "messageRecieved, mb 208 return mb223TimerRunning = true");
                        return;
                    }

                    if (message.contains("AlexaRefreshToken")) {
                        mHandler.removeMessages(Constants.ALEXA_CHECK_TIMEOUT);
                        String token = message.substring(message.indexOf(":") + 1);
                        LSSDPNodes mNode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(nettyData.getRemotedeviceIp());
                        if (mNode != null) {
                            mNode.setAlexaRefreshToken(token);
                            if (token != null && !token.isEmpty()) {
                                if (fwInternetUpgradeMessage.equals(Constants.NO_UPDATE)
                                        || fwInternetUpgradeMessage.equals(Constants.BATTERY_POWER)) {
                                    mHandler.sendEmptyMessage(Constants.CONFIGURED_DEVICE_FOUND);
                                    LibreLogger.d(this, "suma in connecting to main n/w New device found ENV READ 208");

                                }
                            } else {
                                /*request metadata for login*/
                                readAlexaMetaData(mSACConfiguredIpAddress);
                            }
                        }
                    }
                    break;

                case MIDCONST.FW_UPGRADE_INTERNET_LS9:
                    LibreLogger.d(this, "messageRecieved, 223 msg = " + message + " at " + time);

                    if (message.isEmpty())
                        return;

                    fwInternetUpgradeMessage = message;
                    if (fwInternetUpgradeMessage != null && !fwInternetUpgradeMessage.isEmpty()) {
                        mHandler.removeMessages(Constants.WAITING_FOR_223_MB);
                        mb223TimerRunning = false;
                        switch (fwInternetUpgradeMessage) {
                            case Constants.NO_UPDATE:
                                readAlexaToken(mSACConfiguredIpAddress);
                                break;

                            case Constants.UPDATE_STARTED:
                            case Constants.UPDATE_DOWNLOAD:
                            case Constants.UPDATE_IMAGE_AVAILABLE:
                                setSetupInfoTexts(getString(R.string.updating_your_speaker), getString(R.string.you_will_see_flashing));
                                break;

//                            case Constants.UPDATE_IMAGE_AVAILABLE:
//                                setSetupInfoTexts(getString(R.string.now_rebooting), getString(R.string.indicating_light));
//                                mHandler.sendEmptyMessageDelayed(Constants.FW_UPGRADE_REBOOT_TIMER, 3 * 60 * 1000);
//                                readAlexaToken(mSACConfiguredIpAddress);
//                                break;
                        }
                    }

                    break;

                case MIDCONST.FW_UPGRADE_PROGRESS:
                    LibreLogger.d(this, "messageRecieved, 66 msg = " + message + " at " + time);

                    if (message.isEmpty())
                        return;

                    fwInternetUpgradeMessage = message;

                    /*if Gcast Failed State  back to PlayNEwScreen */
                    if (fwInternetUpgradeMessage.equals("255")) {
                        LibreLogger.d(this, "Firmware Update Failed For " + node.getFriendlyname());
                        return;
                    }

                    if (!fwInternetUpgradeMessage.isEmpty()) {
                        mHandler.removeMessages(Constants.WAITING_FOR_223_MB);
                        mb223TimerRunning = false;
                        if (fwInternetUpgradeMessage.equals(GCAST_COMPLETE)) {
                            setSetupInfoTexts(getString(R.string.now_rebooting), getString(R.string.indicating_light));
                            mHandler.sendEmptyMessageDelayed(Constants.FW_UPGRADE_REBOOT_TIMER, 3 * 60 * 1000);
                        }
                    }

                    break;
            }
        }
    }

    private void setSetupInfoTexts(String mainMsg,String subMsg) {
        mainMsgText.setText(mainMsg);
        subMsgText.setText(subMsg);

        if (subMsg.equals(getString(R.string.pleaseWait))){
            setupProgressImage.setImageResource(R.drawable.setup_progress1);
        } else if (subMsg.equals(getString(R.string.you_will_see_flashing))){
            setupProgressImage.setImageResource(R.drawable.setup_progress2);
        } else if (subMsg.equals(getString(R.string.indicating_light))){
            setupProgressImage.setImageResource(R.drawable.setup_progress3);
        }
    }

    private void showProgressDialog() {
        progressBar.setVisibility(View.VISIBLE);
    }

    private void closeProgressDialog() {
        progressBar.setVisibility(View.GONE);
    }

    private void callGoogleCastUpdateScreen() {
        ActivityCompat.finishAffinity(this);
        if (LibreApplication.FW_UPDATE_AVAILABLE_LIST.containsKey(mSACConfiguredIpAddress)) {
            LibreApplication.FW_UPDATE_AVAILABLE_LIST.remove(mSACConfiguredIpAddress);
        }
        startActivity(new Intent(CTConnectingToMainNetwork.this, GoogleCastUpdateAfterSac.class)
                .putExtra(SAC_CURRENT_IPADDRESS, mSACConfiguredIpAddress));
        finish();

    }

    private void readAlexaToken(String configuredDeviceIp) {
        AlexaUtils.sendAlexaRefreshTokenRequest(configuredDeviceIp);
        mHandler.sendEmptyMessageDelayed(Constants.ALEXA_CHECK_TIMEOUT, Constants.INTERNET_PLAY_TIMEOUT);
    }

    private void readAlexaMetaData(String configuredDeviceIp) {
        AlexaUtils.sendAlexaMetaDataRequest(configuredDeviceIp);
        mHandler.sendEmptyMessageDelayed(Constants.ALEXA_CHECK_TIMEOUT, Constants.INTERNET_PLAY_TIMEOUT);
    }

    private void showDeviceAlertDialog(final String message) {
        if (!CTConnectingToMainNetwork.this.isFinishing()) {
            setAlertDialog1(null);
            AlertDialog.Builder builder = new AlertDialog.Builder(CTConnectingToMainNetwork.this);
            builder.setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            getAlertDialog1().dismiss();

                            if (message.contains(getString(R.string.noDeviceFound))) {
                                intentToHome(CTConnectingToMainNetwork.this);
                            }

                            mHandler.sendEmptyMessage(Constants.CONFIGURED_DEVICE_FOUND);
                        }
                    });

            if (getAlertDialog1() == null) {
                setAlertDialog1(builder.show());
                TextView messageView = (TextView) getAlertDialog1().findViewById(android.R.id.message);
                messageView.setGravity(Gravity.CENTER);
            }

            getAlertDialog1().show();

        }
    }

    private void goToNextScreen(){
        LSSDPNodes mNode = ScanningHandler.getInstance().getLSSDPNodeFromCentralDB(mSACConfiguredIpAddress);
        if (mNode == null){
            showToast("Device not available in central db");
            intentToHome(CTConnectingToMainNetwork.this);
            return;
        }

        if (mNode.getmDeviceCap().getmSource().isAlexaAvsSource()) {
            if (mNode.getAlexaRefreshToken() == null || mNode.getAlexaRefreshToken().isEmpty()) {
                Intent newIntent = new Intent(CTConnectingToMainNetwork.this, CTAmazonInfoActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                newIntent.putExtra(Constants.CURRENT_DEVICE_IP, mSACConfiguredIpAddress);
                newIntent.putExtra(AppConstants.DEVICE_PROVISIONING_INFO, mDeviceProvisioningInfo);
                newIntent.putExtra(Constants.FROM_ACTIVITY, CTConnectingToMainNetwork.class.getSimpleName());
                startActivity(newIntent);
                finish();
            } else {
                Intent i = new Intent(CTConnectingToMainNetwork.this, CTAlexaThingsToTryActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.putExtra(Constants.CURRENT_DEVICE_IP, mSACConfiguredIpAddress);
                i.putExtra(Constants.FROM_ACTIVITY, CTConnectingToMainNetwork.class.getSimpleName());
                startActivity(i);
                finish();
            }
        } else {
            intentToHome(CTConnectingToMainNetwork.this);
        }
    }

    private void showAlertDialogForClickingWrongNetwork() {
        String ssid = getConnectedSSIDName(this);
        if (ssid.isEmpty()
                || ssid.contains(Constants.SA_SSID_RIVAA_CONCERT)
                || ssid.contains(Constants.SA_SSID_RIVAA_STADIUM)
                || ssid.equals(wifiConnect.mainSSID)) {
            return;
        }

        if (!CTConnectingToMainNetwork.this.isFinishing()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(CTConnectingToMainNetwork.this);
            String Message = String.format(getString(R.string.newrestartApp),
                    ssid, wifiConnect.getMainSSID());
            /*String Message = getResources().getString(R.string.mConnectedToSsid) + "\n" + getConnectedSSIDName(ConnectingToMainNetwork.this) + "\n" +
                    getString(R.string.restartTitle);*/
            builder.setMessage(Message)
                    .setCancelable(false)
                    .setPositiveButton(R.string.open_wifi_settings, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                            if (alertConnectingtoNetwork!=null && alertConnectingtoNetwork.isShowing()) {
                                alertConnectingtoNetwork.dismiss();
                                alertConnectingtoNetwork = null;
                            }

                            mHandler.removeMessages(Constants.WAITING_FOR_223_MB);
                            mb223TimerRunning = false;

                            Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                            startActivityForResult(intent, AppConstants.WIFI_SETTINGS_REQUEST_CODE);
                        }
                    });
            if (alertConnectingtoNetwork == null) {
                alertConnectingtoNetwork = builder.create();
            }

            closeProgressDialog();
            alertConnectingtoNetwork.show();
            TextView messageView = (TextView) alertConnectingtoNetwork.findViewById(android.R.id.message);
            messageView.setGravity(Gravity.CENTER);

        }
    }

    @Override
    public void connectivityOnReceiveCalled(Context context, Intent intent) {
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();

        String connectedSSID = getConnectedSSIDName(this);
        Log.d("connectivityOnReceive", "wifiConnect.mainSSID " + wifiConnect.mainSSID);

        if (connectedSSID.equals(wifiConnect.mainSSID)
                && wifiInfo.getSupplicantState() == SupplicantState.INACTIVE) {
            LibreError error = new LibreError("Not able to connect ", wifiConnect.getMainSSID() +
                    "Authentication Error  , App Will be closed ");
            BusProvider.getInstance().post(error);
            mHandler.removeMessages(Constants.CONNECTED_TO_MAIN_SSID_FAIL);
            mHandler.sendEmptyMessageDelayed(Constants.CONNECTED_TO_MAIN_SSID_FAIL, OOPS_TIMEOUT);
        }

        if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED && !connectedSSID.isEmpty()) {
            if (connectedSSID.equals(wifiConnect.mainSSID)) {
                if (!isFinishing() && alertConnectingtoNetwork!=null && alertConnectingtoNetwork.isShowing())
                    alertConnectingtoNetwork.dismiss();

                mHandler.removeMessages(Constants.CONNECTED_TO_MAIN_SSID_FAIL);
                mHandler.removeMessages(Constants.CONNECTED_TO_MAIN_SSID_SUCCESS);
                mHandler.sendEmptyMessageDelayed(Constants.CONNECTED_TO_MAIN_SSID_SUCCESS,500);
            } else if (!(connectedSSID.contains(Constants.SA_SSID_RIVAA_CONCERT)
                    || connectedSSID.contains(Constants.SA_SSID_RIVAA_STADIUM))) {

                mHandler.removeMessages(Constants.TIMEOUT_FOR_SEARCHING_DEVICE);
                mHandler.removeMessages(Constants.SEARCHING_FOR_DEVICE);
                mHandler.removeMessages(Constants.CONNECTED_TO_MAIN_SSID_FAIL);

                mHandler.sendEmptyMessage(Constants.CONNECTED_TO_DIFFERENT_SSID);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unRegisterForDeviceEvents();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fwInternetUpgradeMessage = null;
        mHandler.removeCallbacksAndMessages(null);
        mTaskHandlerForSendingMSearch.removeCallbacksAndMessages(null);
    }
}
