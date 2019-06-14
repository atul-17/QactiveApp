package com.cumulations.libreV2.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cumulations.libreV2.AppConstants;
import com.cumulations.libreV2.WifiUtil;
import com.libre.LErrorHandeling.LibreError;
import com.libre.LibreApplication;
import com.libre.Ls9Sac.GoogleCastUpdateAfterSac;
import com.cumulations.libreV2.model.WifiConnection;
import com.libre.R;
import com.libre.Scanning.Constants;
import com.libre.Scanning.ScanningHandler;
import com.libre.alexa.DeviceProvisioningInfo;
import com.libre.alexa.AlexaUtils;
import com.libre.constants.LUCIMESSAGES;
import com.libre.constants.MIDCONST;
import com.libre.luci.LSSDPNodeDB;
import com.libre.luci.LSSDPNodes;
import com.libre.luci.LUCIPacket;
import com.libre.netty.BusProvider;
import com.libre.netty.LibreDeviceInteractionListner;
import com.libre.netty.NettyData;
import com.libre.util.LibreLogger;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.SocketException;

public class CTConnectingToMainNetwork extends CTDeviceDiscoveryActivity implements LibreDeviceInteractionListner {
    private final WifiConnection wifiConnect = WifiConnection.getInstance();
    private WifiManager mWifiManager;
    private AlertDialog alertConnectingtoNetwork;

    /**
     * Broadcast receiver for connection related events
     */
    private ProgressBar progressBar;
    private TextView mMessageText;
    private String locale = "";
    private DeviceProvisioningInfo mDeviceProvisioningInfo;
    private WifiUtil wifiUtil;

    private boolean mRestartOfAllSockets = false;

    public static final String SAC_CURRENT_IPADDRESS = "sac_current_ipaddress";
    private String mSACConfiguredIpAddress = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        disableNetworkChangeCallBack();
        disableNetworkOffCallBack();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.ct_activity_device_connecting);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        progressBar = findViewById(R.id.setup_progress_bar);
        mMessageText = findViewById(R.id.tv_setup_info);

        wifiUtil = new WifiUtil(this);
//        wifiUtil.startWifiScan();
    }

    @Override
    public void onStartComplete() {
        super.onStartComplete();
        LibreLogger.d(this, "onStartComplete Called");
        showProgressDialog();
        /*Get Connected Ssid Name */
        if (getConnectedSSIDName(this).equalsIgnoreCase(wifiConnect.getMainSSID())) {
            LibreLogger.d(this, "onStartComplete CONNECTED_TO_MAIN_SSID_SUCCESS ");
            mHandler.sendEmptyMessage(Constants.CONNECTED_TO_MAIN_SSID_SUCCESS);
        } else {
            mHandler.sendEmptyMessage(Constants.HTTP_POST_DONE_SUCCESSFULLY);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        LibreLogger.d(this, "onStart");
        showProgressDialog();
        LibreApplication.isSacFlowStarted = true;
        /*Get Connected Ssid Name */
        if (getConnectedSSIDName(this).equalsIgnoreCase(wifiConnect.getMainSSID())) {
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
            LibreLogger.d(this, "Handler Message Got " + msg.toString());
            if (msg.what == Constants.HTTP_POST_DONE_SUCCESSFULLY) {
                setSetupInfoText(getString(R.string.mConnectingToMainNetwork) + " " + wifiConnect.getMainSSID());
                cleanUpCode(false);
                if (getConnectedSSIDName(CTConnectingToMainNetwork.this).equals(wifiConnect.mainSSID)) {
                    mHandler.sendEmptyMessage(Constants.CONNECTED_TO_MAIN_SSID_SUCCESS);
                    return;
                }
                wifiUtil.disconnectCurrentWifi();
                wifiUtil.connectWiFiToSSID(wifiConnect.getMainSSID(), wifiConnect.getMainSSIDPwd(),wifiConnect.mainSSIDSec);
                mHandler.sendEmptyMessageDelayed(Constants.CONNECTED_TO_MAIN_SSID_FAIL, 120000);

            } else if (msg.what == Constants.CONNECTED_TO_MAIN_SSID_SUCCESS) {
                /* Removing Failed Callback */
                mHandler.removeMessages(Constants.CONNECTED_TO_MAIN_SSID_FAIL);
                if (getConnectedSSIDName(CTConnectingToMainNetwork.this).equals(wifiConnect.getMainSSID())) {
                    LibreLogger.d(this, "Connected To Main SSID " + wifiConnect.getMainSSID() + mRestartOfAllSockets);
                    LibreApplication.activeSSID = wifiConnect.getMainSSID();
//                    if (!mRestartOfAllSockets) {
                        LSSDPNodeDB.getInstance().clearDB();
                        LibreApplication.LOCAL_IP = "";
                        LibreApplication.mCleanUpIsDoneButNotRestarted = false;
                        /*mRestartOfAllSockets = */restartAllSockets();
                        mHandler.sendEmptyMessageDelayed(Constants.SEARCHING_FOR_DEVICE, 500);
//                    }
                } else {
                    showAlertDialogForClickingWrongNetwork();
                }
            } else if (msg.what == Constants.HTTP_POST_FAILED) {
                Toast.makeText(CTConnectingToMainNetwork.this, getString(R.string.httpPostFailed), Toast.LENGTH_LONG).show();
            } else if (msg.what == Constants.SEARCHING_FOR_DEVICE) {
                sendMSearchInIntervalOfTime();
                mHandler.sendEmptyMessageDelayed(Constants.TIMEOUT_FOR_SEARCHING_DEVICE, Constants.SEARCH_DEVICE_TIMEOUT);
                LibreLogger.d(this, "Searching For The Device " + LibreApplication.sacDeviceNameSetFromTheApp);
                setSetupInfoText(getString(R.string.mSearchingTheDevce) + "\n" + LibreApplication.sacDeviceNameSetFromTheApp);
            } else if (msg.what == Constants.TIMEOUT_FOR_SEARCHING_DEVICE) {
                closeProgressDialog();
                setSetupInfoText(getString(R.string.mTimeoutSearching) + "\n" + LibreApplication.sacDeviceNameSetFromTheApp);
//                showDeviceAlertDialog(getString(R.string.noDeviceFound));
                startActivity(new Intent(CTConnectingToMainNetwork.this, CTSetupFailedActivity.class));
                finish();
            } else if (msg.what == Constants.CONFIGURED_DEVICE_FOUND) {
                closeProgressDialog();
                if (mHandler.hasMessages(Constants.TIMEOUT_FOR_SEARCHING_DEVICE))
                    mHandler.removeMessages(Constants.TIMEOUT_FOR_SEARCHING_DEVICE);
                setSetupInfoText(getString(R.string.mConfiguredSuccessfully) + "\n" + LibreApplication.sacDeviceNameSetFromTheApp);
//                callGoogleCastUpdateScreen();
            } else if (msg.what == Constants.CONNECTED_TO_DIFFERENT_SSID) {
                closeProgressDialog();
                showAlertDialogForClickingWrongNetwork();
            } else if (msg.what == Constants.ALEXA_CHECK_TIMEOUT) {
                /*taking user to the usual screen flow ie configure speakers*/
                closeProgressDialog();
                setSetupInfoText(getString(R.string.mConfiguredSuccessfully) + " " + LibreApplication.sacDeviceNameSetFromTheApp);
                /* Toast.makeText(getApplicationContext(),"Libre App is Restarting .." , Toast.LENGTH_SHORT).show();*/
                showDeviceAlertDialog(getString(R.string.mConfiguredSuccessfully) + " " + LibreApplication.sacDeviceNameSetFromTheApp);
            }
        }
    };

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
    public void newDeviceFound(LSSDPNodes node) {
        LibreLogger.d(this, "New Device Found and same as Configured Device" + node.getFriendlyname() + node.getgCastVerision());
        if (node != null) {
            if (LibreApplication.sacDeviceNameSetFromTheApp.equals(node.getFriendlyname())) {
                LibreLogger.d(this, "Hurray !! New Device Found and same as Configured Device" + node.getFriendlyname() + node.getgCastVerision());
                mSACConfiguredIpAddress = node.getIP();
                if (node.getgCastVerision() != null)
                    mHandler.sendEmptyMessage(Constants.CONFIGURED_DEVICE_FOUND);
                else {
                    /*changes specific to alexa check*/
                    if (mSACConfiguredIpAddress != null && !mSACConfiguredIpAddress.isEmpty()) {
                        if (mHandler.hasMessages(Constants.ALEXA_CHECK_TIMEOUT)) {
                            mHandler.removeMessages(Constants.ALEXA_CHECK_TIMEOUT);
                        }
                        if (mHandler.hasMessages(Constants.TIMEOUT_FOR_SEARCHING_DEVICE))
                            mHandler.removeMessages(Constants.TIMEOUT_FOR_SEARCHING_DEVICE);

                        readAlexaToken(mSACConfiguredIpAddress);
                    }
                }

            } else {
                LibreLogger.d(this, "New Device Found But not we configured Device" + node.getFriendlyname());
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
        Log.d("messageRecieved", "Message recieved for ipaddress " + nettyDataRemotedeviceIp + "command is " + packet.getCommand());

        if (mSACConfiguredIpAddress.equalsIgnoreCase(nettyDataRemotedeviceIp)) {
            String alexaMessage = new String(packet.getpayload());
            if (packet.getCommand() == MIDCONST.ALEXA_COMMAND) {
                LibreLogger.d(this, "Alexa Value From 230  " + alexaMessage);
                if (alexaMessage != null && !alexaMessage.isEmpty()) {
                    /* Parse JSon */
                    try {
                        JSONObject jsonRootObject = new JSONObject(alexaMessage);
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

                        LSSDPNodes node = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(nettyDataRemotedeviceIp);
                        if (node != null) {
                            node.setMdeviceProvisioningInfo(mDeviceProvisioningInfo);
                        }

                        closeProgressDialog();
                        mHandler.removeMessages(Constants.ALEXA_CHECK_TIMEOUT);
                        setSetupInfoText(getString(R.string.mConfiguredSuccessfully) + " " + LibreApplication.sacDeviceNameSetFromTheApp);
                        /* Toast.makeText(getApplicationContext(),"Libre App is Restarting .." , Toast.LENGTH_SHORT).show();*/
                        showDeviceAlertDialog(getString(R.string.mConfiguredSuccessfully) + " " + LibreApplication.sacDeviceNameSetFromTheApp);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (packet.Command == MIDCONST.MID_ENV_READ){
                if (alexaMessage.contains("AlexaRefreshToken")) {
                    String token = alexaMessage.substring(alexaMessage.indexOf(":") + 1);
                    LSSDPNodes mNode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(nettyData.getRemotedeviceIp());
                    if (mNode != null) {
                        mNode.setAlexaRefreshToken(token);
                        if (token!=null && !token.isEmpty()){
                            closeProgressDialog();
                            mHandler.removeMessages(Constants.ALEXA_CHECK_TIMEOUT);
                            setSetupInfoText(getString(R.string.mConfiguredSuccessfully) + " " + LibreApplication.sacDeviceNameSetFromTheApp);
                            /* Toast.makeText(getApplicationContext(),"Libre App is Restarting .." , Toast.LENGTH_SHORT).show();*/
                            showDeviceAlertDialog(getString(R.string.mConfiguredSuccessfully) + " " + LibreApplication.sacDeviceNameSetFromTheApp);
                        } else {
                            /*request metadata for login*/
                            readAlexaMetaData(mSACConfiguredIpAddress);
                        }
                    }
                }
            }
        }
    }

    private void setSetupInfoText(final String mMessage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mMessageText.setText(mMessage);
            }
        });
    }

    private void showProgressDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.VISIBLE);
            }
        });
    }

    private void closeProgressDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.GONE);
            }
        });
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

                            try {
                                getLibreApplication().restart();
                                getLibreApplication().micTcpStart();
                            } catch (SocketException e) {
                                e.printStackTrace();
                            }

                            LSSDPNodes mNode = ScanningHandler.getInstance().getLSSDPNodeFromCentralDB(mSACConfiguredIpAddress);
                            if (mNode == null){
                                showToast("Device not available in central db");
                                intentToHome(CTConnectingToMainNetwork.this);
                                return;
                            }

                            if (/*mDeviceProvisioningInfo != null || */mNode.getmDeviceCap().getmSource().isAlexaAvsSource()) {
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
                    });

            if (getAlertDialog1() == null) {
                setAlertDialog1(builder.show());
                TextView messageView = (TextView) getAlertDialog1().findViewById(android.R.id.message);
                messageView.setGravity(Gravity.CENTER);
            }

            getAlertDialog1().show();

        }
    }

    private void showAlertDialogForClickingWrongNetwork() {
        if (!CTConnectingToMainNetwork.this.isFinishing()) {
            setAlertDialog1(null);
            AlertDialog.Builder builder = new AlertDialog.Builder(CTConnectingToMainNetwork.this);
            String Message = String.format(getString(R.string.newrestartApp),
                    getConnectedSSIDName(CTConnectingToMainNetwork.this), wifiConnect.getMainSSID());
            /*String Message = getResources().getString(R.string.mConnectedToSsid) + "\n" + getConnectedSSIDName(ConnectingToMainNetwork.this) + "\n" +
                    getString(R.string.restartTitle);*/
            builder.setMessage(Message)
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            alertConnectingtoNetwork.dismiss();
                            alertConnectingtoNetwork = null;
                            Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
//                            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                            startActivityForResult(intent, AppConstants.WIFI_SETTINGS_REQUEST_CODE);
                        }
                    });
            if (alertConnectingtoNetwork == null) {
                alertConnectingtoNetwork = builder.show();
               /* TextView messageView = (TextView) alertDialog1.findViewById(android.R.id.message);
                messageView.setGravity(Gravity.CENTER);*/
            }

            alertConnectingtoNetwork.show();

        }
    }

    @Override
    public void connectivityOnReceiveCalled(Context context, Intent intent) {
        NetworkInfo networkInfo = /*cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI)*/intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);;
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();

        Log.d("connectivityOnReceive", "wifiInfo ssid " + wifiInfo.getSSID());
        Log.d("connectivityOnReceive", "wifiConnect Main ssid " + wifiConnect.mainSSID);
        Log.d("connectivityOnReceive", "networkInfo State " + networkInfo.getState());
        Log.d("connectivityOnReceive", "networkInfo Detailed State " + networkInfo.getDetailedState());
        Log.d("connectivityOnReceive", "wifiInfo Supplicant State " + wifiInfo.getSupplicantState());

        if (getConnectedSSIDName(this).contains(wifiConnect.mainSSID)
                && wifiInfo.getSupplicantState() == SupplicantState.INACTIVE) {
            LibreError error = new LibreError("Not able to connect ", wifiConnect.getMainSSID() +
                    "Authentication Error  , App Will be closed ");
            BusProvider.getInstance().post(error);
            if (mHandler.hasMessages(Constants.CONNECTED_TO_MAIN_SSID_FAIL))
                mHandler.removeMessages(Constants.CONNECTED_TO_MAIN_SSID_FAIL);
            mHandler.sendEmptyMessageDelayed(Constants.CONNECTED_TO_MAIN_SSID_FAIL, 60000);
        }

        if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
            if (getConnectedSSIDName(this).contains(wifiConnect.mainSSID)) {
                mHandler.sendEmptyMessageDelayed(Constants.CONNECTED_TO_MAIN_SSID_SUCCESS, 500);
            } else if (!getConnectedSSIDName(this).contains(Constants.RIVAA_WAC_SSID)) {
                LibreError error = new LibreError("Not able to connect to ", wifiConnect.getMainSSID() +
                        "  but Connected To " + wifiInfo.getSSID());
                BusProvider.getInstance().post(error);
                if (mHandler.hasMessages(Constants.CONNECTED_TO_MAIN_SSID_FAIL))
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
        try {
            if (mTaskHandlerForSendingMSearch.hasMessages(MSEARCH_TIMEOUT_SEARCH)) {
                mTaskHandlerForSendingMSearch.removeMessages(MSEARCH_TIMEOUT_SEARCH);
            }
            mTaskHandlerForSendingMSearch.removeCallbacks(mMyTaskRunnableForMSearch);
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }
}