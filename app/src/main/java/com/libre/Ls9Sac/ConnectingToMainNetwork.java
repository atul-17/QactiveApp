package com.libre.Ls9Sac;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cumulations.libreV2.AppConstants;
import com.cumulations.libreV2.activity.CTAlexaThingsToTryActivity;
import com.libre.ActiveScenesListActivity;
import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity;
import com.libre.LErrorHandeling.LibreError;
import com.libre.LibreApplication;
import com.libre.Network.WifiConnection;
import com.libre.R;
import com.libre.Scanning.Constants;
import com.libre.Scanning.ScanningHandler;
import com.libre.alexa.DeviceProvisioningInfo;
import com.libre.alexa.MicExceptionListener;
import com.libre.alexa.MicTcpServer;
import com.libre.alexa_signin.AlexaSignInActivity;
import com.libre.constants.LSSDPCONST;
import com.libre.constants.LUCIMESSAGES;
import com.libre.constants.MIDCONST;
import com.libre.luci.LSSDPNodeDB;
import com.libre.luci.LSSDPNodes;
import com.libre.luci.LUCIControl;
import com.libre.luci.LUCIPacket;
import com.libre.netty.BusProvider;
import com.libre.netty.LibreDeviceInteractionListner;
import com.libre.netty.NettyData;
import com.libre.util.LibreLogger;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.SocketException;
import java.util.List;

public class ConnectingToMainNetwork extends CTDeviceDiscoveryActivity implements LibreDeviceInteractionListner, MicTcpServer.MicTcpServerExceptionListener {
    private static final String PSK = "PSK";
    private static final String WEP = "WEP";
    private static final String OPEN = "Open";
    private static final int MAX_PRIORITY = 99999;
    private final WifiConnection wifiConnect = WifiConnection.getInstance();
    private WifiManager mWifiManager;
    private AlertDialog alertConnectingtoNetwork ;

    /**
     * Broadcast receiver for connection related events
     */
    private ConnectionReceiver connectionReceiver;
    private ProgressBar m_LoadingDialog;
    private TextView mMessageText;
    private String productId = "", dsn = "", sessionId = "", codeChallenge = "", codeChallengeMethod = "", locale ="";
    private DeviceProvisioningInfo mDeviceProvisioningInfo;
    private MicTcpServer micTcpServer;
    private MicExceptionListener micExceptionActivityListener;
    private MicTcpServer.MicTcpServerExceptionListener micListen;

    @Override
    protected void onDestroy() {
        try{
            if(mTaskHandlerForSendingMSearch.hasMessages(MSEARCH_TIMEOUT_SEARCH)) {
                mTaskHandlerForSendingMSearch.removeMessages(MSEARCH_TIMEOUT_SEARCH);
            }
            mTaskHandlerForSendingMSearch.removeCallbacks(mMyTaskRunnableForMSearch);
            unregisterReceiver(connectionReceiver);
        }catch(Exception e){
            e.printStackTrace();
        }
        super.onDestroy();
        unRegisterForDeviceEvents();
    }

    private boolean mRestartOfAllSockets = false;

    public static final String SAC_CURRENT_IPADDRESS = "sac_current_ipaddress";
    private String mSACConfiguredIpAddress = "";
    @SuppressLint("HandlerLeak")
    private final
    Handler mHandler = new Handler() {
        @Override

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            LibreLogger.d(this, "Handler Message Got " + msg.toString());
            if (msg.what == Constants.HTTP_POST_DONE_SUCCESSFULLY) {
                setTextWithMessage(getString(R.string.mConnectingToMainNetwork) + " " + wifiConnect.getMainSSID());
                mWifiManager.disconnect();
                LSSDPNodeDB.getInstance().clearDB();
                cleanUpCode(false);
                connectToSpecificNetwork(wifiConnect.getMainSSID(), wifiConnect.getMainSSIDPwd(), wifiConnect.getMainSSIDSec());

            } else if (msg.what == Constants.CONNECTED_TO_MAIN_SSID_SUCCESS) {
                /* Removing Failed Callback */
                mHandler.removeMessages(Constants.CONNECTED_TO_MAIN_SSID_FAIL);

                if (getConnectedSSIDName(ConnectingToMainNetwork.this).equalsIgnoreCase(wifiConnect.getMainSSID())) {

                    LibreLogger.d(this, "Connected To Main SSID " + wifiConnect.getMainSSID() + mRestartOfAllSockets);
                    LibreApplication.activeSSID = wifiConnect.getMainSSID();
                    //restartApp(getApplicationContext());
                    if (!mRestartOfAllSockets) {
                        LSSDPNodeDB.getInstance().clearDB();
                        LibreApplication.LOCAL_IP="";
                        LibreApplication.mCleanUpIsDoneButNotRestarted = false;
                        mRestartOfAllSockets = restartAllSockets(ConnectingToMainNetwork.this);
                        mHandler.sendEmptyMessageDelayed(Constants.SEARCHING_FOR_DEVICE,500);
                    }
                } else {
                    showAlertDialogForClickingWrongNetwork();
                }
            } else if (msg.what == Constants.HTTP_POST_FAILED) {
                Toast.makeText(ConnectingToMainNetwork.this, getString(R.string.httpPostFailed), Toast.LENGTH_LONG).show();
            } else if (msg.what == Constants.SEARCHING_FOR_DEVICE) {
                sendMSearchInIntervalOfTime();
                mHandler.sendEmptyMessageDelayed(Constants.TIMEOUT_FOR_SEARCHING_DEVICE, 70000);
                LibreLogger.d(this, "Searching For The Device " + LibreApplication.sacDeviceNameSetFromTheApp);
                setTextWithMessage(getString(R.string.mSearchingTheDevce) + "\n" + LibreApplication.sacDeviceNameSetFromTheApp);
            } else if (msg.what == Constants.TIMEOUT_FOR_SEARCHING_DEVICE) {
                closeProgressDialog();
                setTextWithMessage(getString(R.string.mTimeoutSearching) + "\n" + LibreApplication.sacDeviceNameSetFromTheApp);
                showDialogifDeviceNotFound(getString(R.string.noDeviceFound));
                /*Call the Activity To PlayNewScreen */
               // onBackPressed();
            } else if (msg.what == Constants.CONFIGURED_DEVICE_FOUND) {
                closeProgressDialog();
                if(mHandler.hasMessages(Constants.TIMEOUT_FOR_SEARCHING_DEVICE))
                    mHandler.removeMessages(Constants.TIMEOUT_FOR_SEARCHING_DEVICE);
                setTextWithMessage(getString(R.string.mConfiguredSuccessfully) + "\n" + LibreApplication.sacDeviceNameSetFromTheApp);
                callGoogleCastUpdateScreen();
            } else if (msg.what == Constants.CONNECTED_TO_DIFFERENT_SSID) {
                closeProgressDialog();
                showAlertDialogForClickingWrongNetwork();
            } else if (msg.what == Constants.ALEXA_CHECK_TIMEOUT) {
                /*taking user to the usual screen flow ie configure speakers*/
                closeProgressDialog();
                setTextWithMessage(getString(R.string.mConfiguredSuccessfully) + " " + LibreApplication.sacDeviceNameSetFromTheApp);
                                /* Toast.makeText(getApplicationContext(),"Libre App is Restarting .." , Toast.LENGTH_SHORT).show();*/
                showDialogifDeviceNotFound(getString(R.string.mConfiguredSuccessfully) + " " + LibreApplication.sacDeviceNameSetFromTheApp);
            }
            else {

                ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

                if (mWifi.isConnected()) {
                    if (getConnectedSSIDName(ConnectingToMainNetwork.this).contains(wifiConnect.getMainSSID())) {
                        mHandler.sendEmptyMessage(Constants.CONNECTED_TO_MAIN_SSID_SUCCESS);
                    } else if (!getConnectedSSIDName(ConnectingToMainNetwork.this).toLowerCase().contains(Constants.WAC_SSID.toLowerCase())) {
                        showAlertDialogForConnectedToWrongNetwork();
                    }
                }else{
                    alertBoxForNetworkOff();
                }
/*
                LibreError error = new LibreError("Not able to connect ", wifiConnect.getMainSSID());
                BusProvider.getInstance().post(error);
                Intent in = new Intent(ConnectingToMainNetwork.this, DMRDeviceListenerForegroundService.class);
                in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                in.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
                stopService(in);
                try {
                    ActivityCompat.finishAffinity(ConnectingToMainNetwork.this);
                }catch(Exception e){
                    e.printStackTrace();
                }
                int pid = android.os.Process.myPid();
                android.os.Process.killProcess(pid);
                finish();*/
            }

        }
    };
    private AlertDialog alertDialogNetworkOff ;
    public void alertBoxForNetworkOff() {
        if (!ConnectingToMainNetwork.this.isFinishing()) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                    ConnectingToMainNetwork.this);

            // set title
            alertDialogBuilder.setTitle(getString(R.string.wifiConnectivityStatus));

            // set dialog message
            alertDialogBuilder
                    .setMessage(getString(R.string.connectToWifi))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // if this button is clicked, close
                            // current activity
                            dialog.cancel();

                            Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivityForResult(intent, 1235);

                        }
                    });
                  /*  .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // if this button is clicked, just close
                            // the dialog box and do nothing
                            Toast.makeText(ConnectingToMainNetwork.this, getString(R.string.connectToWifi), Toast.LENGTH_SHORT).show();
                            ;
                            dialog.cancel();
                        }
                    });*/

            // create alertDialog1 dialog
            if (alertDialogNetworkOff == null)
                alertDialogNetworkOff = alertDialogBuilder.create();

            // show it

            alertDialogNetworkOff.show();
        }
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
    private void showDialogifDeviceNotFound(final String Message) {
        if (!ConnectingToMainNetwork.this.isFinishing()) {
            setAlertDialog1(null);
            AlertDialog.Builder builder = new AlertDialog.Builder(ConnectingToMainNetwork.this);

            builder.setMessage(Message)
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            getAlertDialog1().dismiss();

                            if (Message.contains(getString(R.string.noDeviceFound))){
                                Intent ssid = new Intent(ConnectingToMainNetwork.this,
                                        ActiveScenesListActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(ssid);
                                finish();
                                return;
                            }

                            LibreApplication libreApplication = (LibreApplication) getApplication();
                            LibreApplication.isSacFlowStarted = false;
                            try {
                                libreApplication.restart();
                                libreApplication.micTcpStart();
                            } catch (SocketException e) {
                                e.printStackTrace();
                            }

                            if (mDeviceProvisioningInfo!=null){
                                LSSDPNodes mNode = ScanningHandler.getInstance().getLSSDPNodeFromCentralDB(mSACConfiguredIpAddress);
                                if (mNode.getAlexaRefreshToken() == null || mNode.getAlexaRefreshToken().isEmpty()) {
                                    Intent newIntent = new Intent(ConnectingToMainNetwork.this, AlexaSignInActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    newIntent.putExtra(Constants.CURRENT_DEVICE_IP, mSACConfiguredIpAddress);
                                    newIntent.putExtra(AppConstants.DEVICE_PROVISIONING_INFO, mDeviceProvisioningInfo);
                                    newIntent.putExtra(Constants.FROM_ACTIVITY, ConnectingToMainNetwork.class.getSimpleName());
                                    startActivity(newIntent);
                                    finish();
                                } else {
                                    Intent i = new Intent(ConnectingToMainNetwork.this, CTAlexaThingsToTryActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    i.putExtra(Constants.CURRENT_DEVICE_IP, mSACConfiguredIpAddress);
                                    i.putExtra(Constants.FROM_ACTIVITY, ConnectingToMainNetwork.class.getSimpleName());
                                    startActivity(i);
                                    finish();
                                }
                            } else {
                                Intent ssid = new Intent(ConnectingToMainNetwork.this,
                                        ActiveScenesListActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(ssid);
                                finish();
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
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1234) {
            LibreLogger.d(this, "came back from wifi list");
            ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (!mWifi.isConnected()) {
                alertBoxForNetworkOff();
            }else{
                restartApp(getApplicationContext());
            }
        }else if(requestCode == 1235){
            restartApp(getApplicationContext());
        }
    }
    private AlertDialog alertWrongNetwork;
    private void showAlertDialogForConnectedToWrongNetwork(){
            if (!ConnectingToMainNetwork.this.isFinishing()) {
                alertWrongNetwork = null;
                AlertDialog.Builder builder = new AlertDialog.Builder(ConnectingToMainNetwork.this);
                String Message =  String.format(getString(R.string.newrestartApp),
                        getConnectedSSIDName(ConnectingToMainNetwork.this), wifiConnect.getMainSSID());
            /*String Message = getResources().getString(R.string.mConnectedToSsid) + "\n" + getConnectedSSIDName(ConnectingToMainNetwork.this) + "\n" +
                    getString(R.string.restartTitle);*/
                builder.setMessage(Message)
                        .setCancelable(false)
                        .setPositiveButton(getString(R.string.gotoSettings), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                                alertWrongNetwork.dismiss();
                                alertWrongNetwork = null;
                                Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                startActivityForResult(intent, 1234);

                            }
                        });
                if (alertWrongNetwork == null) {
                    alertWrongNetwork = builder.show();
               /* TextView messageView = (TextView) alertDialog1.findViewById(android.R.id.message);
                messageView.setGravity(Gravity.CENTER);*/
                }

                alertWrongNetwork.show();
            }
    }
    private void showAlertDialogForClickingWrongNetwork() {
        if (!ConnectingToMainNetwork.this.isFinishing()) {
            setAlertDialog1(null);
            AlertDialog.Builder builder = new AlertDialog.Builder(ConnectingToMainNetwork.this);
            String Message =  String.format(getString(R.string.newrestartApp),
                    getConnectedSSIDName(ConnectingToMainNetwork.this), wifiConnect.getMainSSID());
            /*String Message = getResources().getString(R.string.mConnectedToSsid) + "\n" + getConnectedSSIDName(ConnectingToMainNetwork.this) + "\n" +
                    getString(R.string.restartTitle);*/
            builder.setMessage(Message)
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                            alertConnectingtoNetwork.dismiss();
                            alertConnectingtoNetwork = null;
                            Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivityForResult(intent, 1234);
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
    public void onBackPressed() {
        /*Intent ssid = new Intent(ConnectingToMainNetwork.this, SacActivityScreenForLaunchWifiSettings.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(ssid);
        finish();
        super.onBackPressed();*/
    }

    @Override
    public void deviceDiscoveryAfterClearingTheCacheStarted() {

    }

/* Setting Maximum Priority to Wifi Network */

    @Override
    public void newDeviceFound(LSSDPNodes node) {
        LibreLogger.d(this, "New Device Found and same as Configured Device" + node.getFriendlyname()
                + node.getgCastVerision());
        if (node != null) {
            if (LibreApplication.sacDeviceNameSetFromTheApp.equals(node.getFriendlyname())) {
                LibreLogger.d(this, "Hurray !! New Device Found and same as Configured Device"
                        + node.getFriendlyname()
                + node.getgCastVerision());
                mSACConfiguredIpAddress=node.getIP();
                if(node.getgCastVerision()!=null)
                    mHandler.sendEmptyMessage(Constants.CONFIGURED_DEVICE_FOUND);
                else{
                    /*changes specific to alexa check*/
                    if (mSACConfiguredIpAddress!=null && !mSACConfiguredIpAddress.isEmpty()) {
                        if (mHandler.hasMessages(Constants.ALEXA_CHECK_TIMEOUT)){
                            mHandler.removeMessages(Constants.ALEXA_CHECK_TIMEOUT);
                        }
                        if(mHandler.hasMessages(Constants.TIMEOUT_FOR_SEARCHING_DEVICE))
                            mHandler.removeMessages(Constants.TIMEOUT_FOR_SEARCHING_DEVICE);

                        checkForAlexaToken(mSACConfiguredIpAddress);
                    }


//                    closeProgressDialog();
//                    if(mHandler.hasMessages(Constants.TIMEOUT_FOR_SEARCHING_DEVICE))
//                        mHandler.removeMessages(Constants.TIMEOUT_FOR_SEARCHING_DEVICE);
//                    setTextWithMessage(getString(R.string.mConfiguredSuccessfully) + " " + LibreApplication.sacDeviceNameSetFromTheApp);
//                                /* Toast.makeText(getApplicationContext(),"Libre App is Restarting .." , Toast.LENGTH_SHORT).show();*/
//                    showDialogifDeviceNotFound(getString(R.string.mConfiguredSuccessfully) + " " + LibreApplication.sacDeviceNameSetFromTheApp);

                    /*restartApp(ConnectingToMainNetwork.this);*/
                   /* Intent ssid = new Intent(ConnectingToMainNetwork.this,
                            NewSacActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(ssid);
                    finish();*/
                }

            } else {
                LibreLogger.d(this, "New Device Found But not we configured Device" + node.getFriendlyname());
               // mSACConfiguredIpAddress=null;
            }
        }
    }

    @Override
    public void deviceGotRemoved(String ipaddress) {

    }
  /* Shfit Piriorty and Save */

    @Override
    public void messageRecieved(NettyData nettyData) {
        byte[] buffer = nettyData.getMessage();
        String ipaddressRecieved = nettyData.getRemotedeviceIp();

        LUCIPacket packet = new LUCIPacket(nettyData.getMessage());
        Log.d("LedStatus", "Message recieved for ipaddress " + ipaddressRecieved + "command is " + packet.getCommand());

        if (mSACConfiguredIpAddress.equalsIgnoreCase(ipaddressRecieved)) {

            switch (packet.getCommand()) {

                case MIDCONST.ALEXA_COMMAND: {
                    String alexaMessage = new String(packet.getpayload());
                    LibreLogger.d(this, "Alexa Value From 230  " + alexaMessage);
                    if (alexaMessage != null && !alexaMessage.isEmpty()) {
                    /* Parse JSon */
                        try {
                            JSONObject jsonRootObject = new JSONObject(alexaMessage);
                            // JSONArray jsonArray = jsonRootObject.optJSONArray("Window CONTENTS");
                            JSONObject jsonObject = jsonRootObject.getJSONObject(LUCIMESSAGES.ALEXA_KEY_WINDOW_CONTENT);
                            productId = jsonObject.optString("PRODUCT_ID").toString();
                            dsn = jsonObject.optString("DSN").toString();
                            sessionId = jsonObject.optString("SESSION_ID").toString();
                            codeChallenge = jsonObject.optString("CODE_CHALLENGE").toString();
                            codeChallengeMethod = jsonObject.optString("CODE_CHALLENGE_METHOD").toString();
                            if (jsonObject.has("LOCALE"))
                                locale = jsonObject.optString("LOCALE").toString();
                            mDeviceProvisioningInfo = new DeviceProvisioningInfo(productId, dsn, sessionId, codeChallenge, codeChallengeMethod, locale);

                            closeProgressDialog();
                            mHandler.removeMessages(Constants.ALEXA_CHECK_TIMEOUT);
                            setTextWithMessage(getString(R.string.mConfiguredSuccessfully) + " " + LibreApplication.sacDeviceNameSetFromTheApp);
                                /* Toast.makeText(getApplicationContext(),"Libre App is Restarting .." , Toast.LENGTH_SHORT).show();*/
                            showDialogifDeviceNotFound(getString(R.string.mConfiguredSuccessfully) + " " + LibreApplication.sacDeviceNameSetFromTheApp);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }


                }
                break;
            }
        }
    }

    private int getMaxPriority(final WifiManager wifiManager) {
        final List<WifiConfiguration> configurations = wifiManager.getConfiguredNetworks();
        int pri = 0;
        for (final WifiConfiguration config : configurations) {
            if (config.priority > pri) {
                pri = config.priority;
            }
        }
        return pri;
    }

    /* Converting a String to Quoted String
    * Syntax for Setting SSID in wifi Conf : / SSIDNAME /*/
    private String convertToQuotedString(String string) {
        if (TextUtils.isEmpty(string)) {
            return "";
        }
        final int lastPos = string.length() - 1;
        if (lastPos > 0 && (string.charAt(0) == '"' && string.charAt(lastPos) == '"')) {
            return string;
        }
        return "\"" + string + "\"";
    }

    private int shiftPriorityAndSave(final WifiManager wifiMgr) {
        final List<WifiConfiguration> configurations = wifiMgr.getConfiguredNetworks();
        final int size = configurations.size();
        for (int i = 0; i < size; i++) {
            final WifiConfiguration config = configurations.get(i);
            config.priority = i;
            wifiMgr.updateNetwork(config);
        }
        wifiMgr.saveConfiguration();
        return size;
    }

    private String getScanResultSecurity(String mSecurity) {
        final String[] securityModes = {WEP, PSK};
        for (int i = securityModes.length - 1; i >= 0; i--) {
            if (mSecurity.contains(securityModes[i])) {
                return securityModes[i];
            }
        }
        return OPEN;
    }

    /**
     * Start to connect to a specific wifi network
     */
    private void connectToSpecificNetwork(final String mNetworkSsidToConnect, final String mNetworkPassKeyToConnect, final String mNetworkSecurityToConnect) {


        new Thread(new Runnable() {

            @Override

            public void run() {
                ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
                if (networkInfo.isConnected() && wifiInfo.getSSID().replace("\"", "").equals(mNetworkSsidToConnect)) {
                    mHandler.sendEmptyMessage(Constants.CONNECTED_TO_MAIN_SSID_SUCCESS);
                    return;
                }
                final WifiConfiguration conf = new WifiConfiguration();
                conf.allowedAuthAlgorithms.clear();
                conf.allowedGroupCiphers.clear();
                conf.allowedPairwiseCiphers.clear();
                conf.allowedProtocols.clear();

                conf.allowedKeyManagement.clear();

                LibreLogger.d(this, "Nework Security  Scan Resut To Connect From Function " + getScanResultSecurity(mNetworkSecurityToConnect));
                LibreLogger.d(this, "Network Security To Connect " + mNetworkSecurityToConnect);
                conf.SSID = convertToQuotedString(mNetworkSsidToConnect);  //convertToQuotedString(mScanResult.SSID);
                // Make it the highest priority.
                int newPri = getMaxPriority(mWifiManager) + 1;
                if (newPri > MAX_PRIORITY) {
                    newPri = shiftPriorityAndSave(mWifiManager);
                }
                LibreLogger.d(this, "Network Security To Connect " + mNetworkSecurityToConnect +
                        "priority Generated is " + newPri);
                conf.status = WifiConfiguration.Status.ENABLED;
                conf.priority = newPri;
                switch (getScanResultSecurity(mNetworkSecurityToConnect)) {
                    case WEP:
                        conf.wepKeys[0] = "\"" + mNetworkPassKeyToConnect + "\"";
                        conf.wepTxKeyIndex = 0;
                        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                        break;
                    case PSK:
                        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                        conf.preSharedKey = "\"" + mNetworkPassKeyToConnect + "\"";
                        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                        conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                        conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                        conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN); // For WPA2
                        conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA); // For WPA
                        break;
                    case OPEN:
                        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                        conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.NONE);
                        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                        break;
                }

                int netId = mWifiManager.addNetwork(conf);
                LibreLogger.d(this, "Netid We got For the Ssid is " + mNetworkSsidToConnect +
                        "and Netid is " + netId);
                if (netId == -1) {
                    LibreLogger.d(this, "Failed to set the settings for  " + mNetworkSsidToConnect);
                    final List<WifiConfiguration> mWifiConfiguration = mWifiManager.getConfiguredNetworks();
                    for (int i = 0; i < mWifiConfiguration.size(); i++) {
                        String configSSID = mWifiConfiguration.get(i).SSID;
                        LibreLogger.d(this, "Config SSID" + configSSID + "Active SSID" + conf.SSID);
                        if (configSSID.equals(conf.SSID)) {
                            netId = mWifiConfiguration.get(i).networkId;
                            LibreLogger.d(this, "network id" + netId);
                            break;
                        }
                    }
                }
                connectionReceiver = new ConnectionReceiver(mNetworkSsidToConnect);
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
                intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
                intentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
                intentFilter.addAction("android.net.wifi.STATE_CHANGE");
                intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
                intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
                registerReceiver(connectionReceiver, intentFilter);
                mHandler.sendEmptyMessageDelayed(Constants.CONNECTED_TO_MAIN_SSID_FAIL, 120000);
                boolean mCatchDisconnect = mWifiManager.enableNetwork(netId, true);
                LibreLogger.d(this, "Wifi MAnager calling enableNetwork" + mCatchDisconnect);
            }

        }).start();

    }

    private void setTextWithMessage(final String mMessage) {
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
                m_LoadingDialog.setVisibility(View.VISIBLE);
            }
        });
    }

    private void closeProgressDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                m_LoadingDialog.setVisibility(View.GONE);
            }
        });
    }
    private void callGoogleCastUpdateScreen(){
        ActivityCompat.finishAffinity(this);
        if(LibreApplication.GCAST_UPDATE_AVAILABE_LIST_DATA.containsKey(mSACConfiguredIpAddress)){
            LibreApplication.GCAST_UPDATE_AVAILABE_LIST_DATA.remove(mSACConfiguredIpAddress);
        }
        startActivity(new Intent(ConnectingToMainNetwork.this, GoogleCastUpdateAfterSac.class)
                .putExtra(SAC_CURRENT_IPADDRESS, mSACConfiguredIpAddress));
        finish();

    }
    @Override
    public void onStartComplete() {
        super.onStartComplete();
        LibreLogger.d(this, "OnCreate Called");
        showProgressDialog();
        /*Get Connected Ssid Name */
        String mConnectedSsidName = getConnectedSSIDName(this);
        if (mConnectedSsidName.equalsIgnoreCase(wifiConnect.getMainSSID())) {
            LibreLogger.d(this, "OnCreate MainSSIDSuccess ");
            mHandler.sendEmptyMessage(Constants.CONNECTED_TO_MAIN_SSID_SUCCESS);
        } else {
            mHandler.sendEmptyMessage(Constants.HTTP_POST_DONE_SUCCESSFULLY);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        disableNetworkChangeCallBack();
        disableNetworkOffCallBack();
        registerForDeviceEvents(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connecting_to_main_network);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);


        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);


        m_LoadingDialog = (ProgressBar) findViewById(R.id.wifiConnectionLoader);
        mMessageText = (TextView) findViewById(R.id.mMessageStatus);

        disableNetworkChangeCallBack();
        disableNetworkOffCallBack();


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_connecting_to_main_network, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void micTcpServerException(Exception e) {
        LibreLogger.d(this,"micTcpServerException = "+e.getMessage());
        micExceptionActivityListener.micExceptionCaught(e);
    }

    public void registerForMicException(MicExceptionListener listener){
        micExceptionActivityListener = listener;
    }

    public void unregisterMicException(){
        micExceptionActivityListener = null;
    }

    private class ConnectionReceiver extends BroadcastReceiver {

        private final String mNetworkSsidToConnect;

        ConnectionReceiver(String ssid) {
            mNetworkSsidToConnect = ssid;
        }

        @Override

        public void onReceive(Context context, Intent intent) {

            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);

            NetworkInfo networkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
            LibreLogger.d(this, "OnRecieve Method: Wifi Info " + wifiInfo.getSSID());
            LibreLogger.d(this, "OnRecieve Method: mNetworkSSIDToConnect " + mNetworkSsidToConnect);
            LibreLogger.d(this, "OnRecieve Method: Network State " + networkInfo.getState());
            LibreLogger.d(this, "OnRecieve Method: Network Detailed State " + networkInfo.getDetailedState());
            LibreLogger.d(this, "Supplicant State " + wifiInfo.getSupplicantState());
            if (wifiInfo.getSupplicantState() == SupplicantState.INACTIVE) {
                LibreError error = new LibreError("Not able to connect ", wifiConnect.getMainSSID() +
                        "Authentication Error  , App Will be closed ");
                BusProvider.getInstance().post(error);
                if (mHandler.hasMessages(Constants.CONNECTED_TO_MAIN_SSID_FAIL))
                    mHandler.removeMessages(Constants.CONNECTED_TO_MAIN_SSID_FAIL);
                mHandler.sendEmptyMessageDelayed(Constants.CONNECTED_TO_MAIN_SSID_FAIL, 60000);
            }
            boolean mVariable;
            if ((android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)) {
                mVariable=true;
            }else{
                mVariable = networkInfo != null && (networkInfo.isConnected());
            }
            /*if (networkInfo != null && (networkInfo.isConnected())) */
            if(mVariable){
                if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
                    LibreLogger.d(this, " 1 OnRecieve Wifi Info " + wifiInfo.getSSID());
                    LibreLogger.d(this, " 1 OnRecieve mNetworkSSIDToConnect " + mNetworkSsidToConnect);
                    LibreLogger.d(this, " 1 OnRecieve Network State " + networkInfo.getState());
                    if (wifiInfo.getSSID().contains(mNetworkSsidToConnect)) {
                        mHandler.sendEmptyMessageDelayed(Constants.CONNECTED_TO_MAIN_SSID_SUCCESS,500);
                    } else if (!wifiInfo.getSSID().toLowerCase().contains(Constants.WAC_SSID.toLowerCase())) {
                        LibreError error = new LibreError("Not able to connect to ", wifiConnect.getMainSSID() +
                                "  but Connected To " + wifiInfo.getSSID());
                        BusProvider.getInstance().post(error);
                        if (mHandler.hasMessages(Constants.CONNECTED_TO_MAIN_SSID_FAIL))
                            mHandler.removeMessages(Constants.CONNECTED_TO_MAIN_SSID_FAIL);
                        mHandler.sendEmptyMessage(Constants.CONNECTED_TO_DIFFERENT_SSID);
                    }/*else if (wifiInfo.getSSID().contains(Constants.WAC_SSID)){
                        if (mHandler.hasMessages(Constants.CONNECTED_TO_MAIN_SSID_FAIL))
                            mHandler.removeMessages(Constants.CONNECTED_TO_MAIN_SSID_FAIL);
                        mHandler.sendEmptyMessage(Constants.HTTP_POST_DONE_SUCCESSFULLY);
                    }*/
                }
            }
        }

    }

    private void checkForAlexaToken(String configuredDeviceIp) {
        LUCIControl luciControl = new LUCIControl(configuredDeviceIp);
        luciControl.SendCommand(MIDCONST.ALEXA_COMMAND, "DEVICE_METADATA_REQUEST", LSSDPCONST.LUCI_SET);
        String readAlexaRefreshToken = "READ_AlexaRefreshToken";
        luciControl.SendCommand(208, readAlexaRefreshToken,1);
        mHandler.sendEmptyMessageDelayed(Constants.ALEXA_CHECK_TIMEOUT,Constants.INTERNET_PLAY_TIMEOUT);
    }
}