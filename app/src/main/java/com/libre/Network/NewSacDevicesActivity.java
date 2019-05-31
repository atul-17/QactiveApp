package com.libre.Network;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity;
import com.libre.LibreApplication;
import com.libre.R;
import com.libre.Scanning.Constants;
import com.libre.Scanning.ScanningHandler;
import com.libre.SceneObject;
import com.libre.app.dlna.dmc.utility.PlaybackHelper;
import com.libre.app.dlna.dmc.utility.UpnpDeviceManager;
import com.libre.luci.LSSDPNodes;
import com.libre.luci.LUCIControl;
import com.libre.netty.LibreDeviceInteractionListner;
import com.libre.netty.NettyAndroidClient;
import com.libre.netty.NettyData;
import com.libre.serviceinterface.LSDeviceClient;
import com.libre.util.LibreLogger;

import org.fourthline.cling.model.meta.RemoteDevice;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by khajan on 5/4/16.
 */
public class NewSacDevicesActivity extends CTDeviceDiscoveryActivity implements LibreDeviceInteractionListner {

    public WifiManager mWifiManager = null;

    boolean mCoarseLocation = false;
    boolean mFineLocation = false;

    public static final String PSK = "PSK";
    public static final String WEP = "WEP";
    public static final String OPEN = "Open";
    private static final int MAX_PRIORITY = 99999;

    ListView sacListView;

    private Runnable progressRunnable;

    public ProgressDialog mProgressDialog;
    public int mPriorityGiven = -1;

    ConnectionReceiver connectionReceiver;

    private Handler pdCanceller;

    List<ScanResult> mScanResults;
    public final int mLower2GHzFrequency = 2412;
    public final int mHigher2GHzFrequency = 2484;

    private boolean mRequestPermissioninProgress = false;
    WifiConnection wifiConnect = WifiConnection.getInstance();

    boolean mFromSacDeviceConfigurationScreen = false;

    ScanResult mNetworkScanResultToConnect;
    String mNetworkSsidToConnect;
    String mNetworkPassKeyToConnect;
    String mNetworkSecurityToConnect;
    /***/
    Timer timer;
    TimerTask timerTask;
    WifiReceiver receiverWifi = new WifiReceiver();
    List<String> sacDevices;
    SacAdapter sacAdapter;
    ProgressBar sacProgressBar;
    TextView emptyText;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case Constants.PREPARATION_INITIATED:
                    sacProgressBar.setVisibility(View.VISIBLE);
                    emptyText.setText(getString(R.string.noSacDevices));
                    break;
                case Constants.PREPARATION_COMPLETED:

                case Constants.PREPARATION_TIMEOUT:
                    sacProgressBar.setVisibility(View.GONE);
                    break;
            }
        }
    };
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(NewSacDevicesActivity.this, SacInstructionsActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_pair_share);

//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//        getSupportActionBar().setDisplayShowTitleEnabled(true);


        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        sacListView = (ListView) findViewById(R.id.sac_list_view);

        sacProgressBar = (ProgressBar) findViewById(R.id.loading_progressbar);

        /**sending delayed message to close progress bar*/
        handler.sendEmptyMessage(Constants.PREPARATION_INITIATED);
        handler.sendEmptyMessageDelayed(Constants.PREPARATION_TIMEOUT, Constants.LOADING_TIMEOUT);


        emptyText = (TextView) findViewById(android.R.id.empty);
        sacListView.setEmptyView(emptyText);

        sacDevices = new ArrayList<>();


        sacAdapter = new SacAdapter(this, sacDevices);
        sacListView.setAdapter(sacAdapter);

        sacListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

                disableNetworkChangeCallBack();

                disableNetworkOffCallBack();
                mNetworkSsidToConnect = wifiConnect.getSacDevice(position);
                mNetworkPassKeyToConnect = "";
                mNetworkSecurityToConnect = OPEN;
                //    mNetworkScanResultToConnect = mScanResultsWithSsidMap.get(mNetworkSsidToConnect);
                wifiConnect.setPreviousSSID(getConnectedSsidInfo(""));
                showLoader(mNetworkSsidToConnect);


                try {
                    if (receiverWifi.isOrderedBroadcast())
                        unregisterReceiver(receiverWifi);
                } catch (Exception e) {

                }
                disableNetworkChangeCallBack();

                disableNetworkOffCallBack();
                CleanUpTcpSockets();
                connectToSpecificNetwork();

                        /*loader timeout : Increasing the Timeout For H&T as per the Bala's comment by KK */
                pdCanceller.postDelayed(progressRunnable, 120000);
            }
        });

    }
    public void CleanUpTcpSockets(){
        try {
            if( LUCIControl.luciSocketMap.size()>0) {
                for (NettyAndroidClient mAndroid : LUCIControl.luciSocketMap.keySet().toArray(new NettyAndroidClient[0])) {

                    try {
                        RemoteDevice renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(mAndroid.getRemotehost());
                    /* For the New DMR Implementation , whenever a Source Switching is happening
                    * we are Stopping the Playback */
                        if (renderingDevice != null) {
                            String renderingUDN = renderingDevice.getIdentity().getUdn().toString();
                            PlaybackHelper playbackHelper = LibreApplication.PLAYBACK_HELPER_MAP.get(renderingUDN);
                            if(ScanningHandler.getInstance().isIpAvailableInCentralSceneRepo(mAndroid.getRemotehost())){
                                SceneObject mScene = ScanningHandler.getInstance().getSceneObjectFromCentralRepo(mAndroid.getRemotehost());
                                if(mScene.getPlayUrl().contains(LibreApplication.LOCAL_IP))
                                    playbackHelper.StopPlayback();
                                playbackHelper = null;
                                LibreApplication.PLAYBACK_HELPER_MAP.remove(renderingUDN);
                             }

                        }
                    } catch (Exception e) {

                    }
                    new LUCIControl(mAndroid.getRemotehost()).deRegister();
                    mAndroid.getHandler().mChannelContext.close();
                }
                LUCIControl.luciSocketMap.clear();


                LibreApplication application = (LibreApplication) this.getApplicationContext();
                application.micTcpClose();
            }
            ;
        }catch(Exception e){

        }
    }

    /* Get current Connected SSID INfo*/
    public String getConnectedSsidInfo() {
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        return wifiInfo.getSSID().replace("\"", "");
    }


    /* This method is made only for previousSSID as once you have connected to LSConfigure still we have to show previous connected ssid*/
    public String getConnectedSsidInfo(String previousSSID) {

        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        if (wifiInfo.getSSID().replace("\"", "").toLowerCase().startsWith(Constants.WAC_SSID.toLowerCase())) {
            return wifiConnect.getPreviousSSID();
        }
        return wifiInfo.getSSID().replace("\"", "");
    }


    public void startTimer() {
        //set a new Timer
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();

        //schedule the timer, after the first 5000ms the TimerTask will run every 5000ms
        timer.schedule(timerTask, 5000, 5000); //
    }

    public void stopTimer() {
        //stopMediaServer the timer, if it's not already null

        if (timer != null) {
            timer.cancel();
            timer = null;
        }

    }

    public void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {

                //use a handler to run a toast that shows the current timestamp
                handler.post(new Runnable() {
                    public void run() {
                        //get the current timeStamp
                       /* mWifiManager.startScan();
                        mScanResults = mWifiManager.getScanResults();
*/
                        startScan();
                      /*  Toast toast = Toast.makeText(getApplicationContext(), "Scanning.." ,Toast.LENGTH_SHORT);
                        toast.show();*/
                    }
                });
            }
        };
    }


    @Override
    protected void onResume() {
        super.onResume();


        /* Regestring Callback */
        registerForDeviceEvents(this);
        sendMSearchWithoutClear();

        /* for the First Time it should load with ScanResulst */
        startScan();



        /*this is created to handle timeout*/
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (getConnectedSsidInfo().equalsIgnoreCase(mNetworkSsidToConnect)) {
                    Toast.makeText(getApplicationContext(), getString(R.string.httpFailed), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.sacFailedNetworkIssue), Toast.LENGTH_SHORT).show();
                }
                closeLoader();
                registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                registerReceiver(receiverWifi, new IntentFilter(WifiManager.EXTRA_RESULTS_UPDATED));
            }
        };
        pdCanceller = new Handler();

        /* For the Android 6.0 Version REquesting Permission Especially Done for MI phones */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!mRequestPermissioninProgress)
                requestPermission();
        }


        registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        registerReceiver(receiverWifi, new IntentFilter(WifiManager.EXTRA_RESULTS_UPDATED));

        startTimer();

//        enableNetworkChangeCallBack();
//        enableNetworkOffCallBack();


        loadSacDevicesFromScanResultList();


        /* If Sac Device Is Already Configured it should start Connect to Main SSID : This Changes are done
         * for the  Single Handler Usage */
        if (wifiConnect.getmSACDevicePostDone()) {
            Log.d("What_Is_Wrong", "onResume() called with: " + "inside if condition");
            mNetworkSsidToConnect = wifiConnect.getMainSSID();
            mNetworkPassKeyToConnect = wifiConnect.getMainSSIDPwd();
            mNetworkSecurityToConnect = wifiConnect.getMainSSIDSec();
//            mNetworkScanResultToConnect = mScanResultsWithSsidMap.get(mNetworkSsidToConnect);

            enableNetworkChangeCallBack();

//            disableNetworkOffCallBack();
            // connectToSpecificNetwork();

            wifiConnect.setmSACDevicePostDone(false);
            mFromSacDeviceConfigurationScreen = false;
            mFromSacDeviceConfigurationScreen = false;
        }
    }

    private void sendMSearchWithoutClear() {

        LibreLogger.d(this, "Sending M Search Without Clear");
        final LibreApplication application = (LibreApplication) getApplication();
        //application.getScanThread().clearNodes();
        application.getScanThread().UpdateNodes();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                application.getScanThread().UpdateNodes();
            }
        }, 150);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                application.getScanThread().UpdateNodes();

            }
        }, 650);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                application.getScanThread().UpdateNodes();

            }
        }, 1150);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                application.getScanThread().UpdateNodes();

            }
        }, 1650);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                application.getScanThread().UpdateNodes();

            }
        }, 2150);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                application.getScanThread().UpdateNodes();
//                if (mConfiguredDevicesName.contains(getResources().getString(R.string.title_for_refreshing))) {
//                    mConfiguredDevicesName.remove(getResources().getString(R.string.title_for_refreshing));
//                    mConfiguredDevicesName.add(getResources().getString(R.string.title_no_lssdp_device));
//                    mSpeakerExpandableListAdapter.removeChildFromAdapter(0, getResources().getString(R.string.title_for_refreshing));
//                    mSpeakerExpandableListAdapter.addNewChildToAdapter(0, getResources().getString(R.string.title_no_lssdp_device));
//                    mSpeakerExpandableListAdapter.notifyDataSetChanged();
//
//                }

            }
        }, 2650);
    }


    /**
     * blocking call always do in background thread
     */
    private void startScan() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                /* Less than Marshmallow Version , it should do the Scan without the Scan */
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    mCoarseLocation = true;
                    mFineLocation = true;
                }
                if (mCoarseLocation && mFineLocation) {
                    mWifiManager.startScan();

                    mScanResults = mWifiManager.getScanResults();
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (!mRequestPermissioninProgress) {
                            LibreLogger.d(this, "Start Scan Calling " + mRequestPermissioninProgress);
                            requestPermission();
                        }
                    }
                }
            }
        }).start();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
      /*  super.onRequestPermissionsResult(requestCode, permissions, grantResults);*/
        switch (requestCode) {

            case 200:
                if (grantResults.length > 0) {
                    mCoarseLocation = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    mFineLocation = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (mCoarseLocation && mFineLocation) {
                        mRequestPermissioninProgress = false;
                        startScan();
                    } else if (grantResults[0] == PackageManager.PERMISSION_DENIED ||
                            grantResults[1] == PackageManager.PERMISSION_DENIED) {
                        mRequestPermissioninProgress = true;
                        boolean should = false;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                           /* should = NewSacActivity.this.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION);

                            if (should) {*/
                            //user denied without Never ask again, just show rationale explanation
                            AlertDialog.Builder builder = new AlertDialog.Builder(NewSacDevicesActivity.this);
                            builder.setTitle(getString(R.string.permisssionUnavailable));
                            builder.setMessage(getString(R.string.permissionMsg));
                            builder.setPositiveButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    Toast.makeText(getApplicationContext(), getString(R.string.cantConfigSAC), Toast.LENGTH_SHORT).show();
                                }
                            });
                            builder.setNegativeButton(getString(R.string.gotoSettings), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    goToSettings();
                                    Toast.makeText(getApplicationContext(), getString(R.string.restartAfterPermit), Toast.LENGTH_LONG).show();
                                }
                            });
                            builder.show();
                        }
                    }
                }
                break;
            default: {
                // super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }

        }
    }

    private void goToSettings() {
        if (!NewSacDevicesActivity.this.isFinishing()) {
            Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getApplicationContext().getPackageName()));
            myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
            myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(myAppSettings);
        }
    }

    private void requestPermission() {
        if (hasPermission((Manifest.permission.ACCESS_FINE_LOCATION)) && hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            mCoarseLocation = true;
            mFineLocation = true;

        } else {
            String[] perms = {"android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION"};
            int permsRequestCode = 200;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(perms, permsRequestCode);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean hasPermission(String permission) {
        if (canMakeSmores()) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
            }
        }
        return true;
    }

    private boolean canMakeSmores() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }

    @Override
    public void deviceDiscoveryAfterClearingTheCacheStarted() {

    }

    @Override
    public void newDeviceFound(LSSDPNodes node) {
/*
        SharedPreferences sharedPreferences = getApplicationContext()
                .getSharedPreferences("sac_configured", Context.MODE_PRIVATE);
        String str = sharedPreferences.getString("deviceFriendlyName", "");
        if (str != null && str.equalsIgnoreCase(node.getFriendlyname().toString()) && node.getgCastVerision() != null) {
            LibreLogger.d(this, "bhargav ");
            //sending time zone to the gCast device
            LUCIControl luciControl = new LUCIControl(node.getIP());
            luciControl.SendCommand(222, "2:" + TimeZone.getDefault().getID().toString(), LSSDPCONST.LUCI_SET);
            //Toast.makeText(getApplicationContext(),str + " in preference",Toast.LENGTH_SHORT).show();
            final Dialog dialog = new Dialog(NewSacDevicesActivity.this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.gcast_dialog);
            TextView textBody = (TextView) dialog.findViewById(R.id.textBody);
            textBody.append(node.getFriendlyname().toString());
            Button noThanks = (Button) dialog.findViewById(R.id.no);
            Button learnMore = (Button) dialog.findViewById(R.id.learnMore);
            learnMore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //https://support.google.com/googlecast/answer/6076570
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setData(Uri.parse("https://www.google.com/cast/audio/learn"));
                    startActivity(intent);
                }
            });
            noThanks.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.cancel();
                }
            });
            dialog.show();
            sharedPreferences.edit().remove("deviceFriendlyName").commit();
        } else if (node != null && node.getgCastVerision() == null && str.equalsIgnoreCase(node.getFriendlyname())) {
            sharedPreferences.edit().remove("deviceFriendlyName").commit();
        }

*/

    }

    @Override
    public void deviceGotRemoved(String ipaddress) {

    }

    @Override
    public void messageRecieved(NettyData packet) {

    }

    public class WifiReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context c, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) ||
                    action.equals(WifiManager.EXTRA_RESULTS_UPDATED)) {
//                startScan();;
/*
                 mWifiManager.startScan();*/
                 mScanResults = mWifiManager.getScanResults();
                loadSacDevicesFromScanResultList();
            }
        }
    }


    public long lastupdatedTime;
    public int SCAN_INTERVAL = 5000; // in ms

    /* Load SAC Devices From Wifi Scan Results */
    public void loadSacDevicesFromScanResultList() {
        wifiConnect.clearSacDevices();
        sacDevices.clear();
        sacAdapter.notifyDataSetChanged();
        sacListView.invalidateViews();
        if (mScanResults != null) {
            for (int i = mScanResults.size() - 1; i >= 0; i--) {
                final ScanResult scanResult = mScanResults.get(i);
                if (scanResult == null) {
                    continue;
                }
                if (TextUtils.isEmpty(scanResult.SSID)) {
                    continue;
                }
                if (mLower2GHzFrequency <= scanResult.frequency && scanResult.frequency <= mHigher2GHzFrequency) {
                  //  wifiConnect.putWifiScanResultSecurity(scanResult.SSID, scanResult.capabilities);
                }
                /* Commenting For Not Using After Review
                mScanResultsWithSsidMap.put(scanResult.SSID, scanResult);*/
                   /* LibreLogger.d(this,"Karuna" + scanResult.SSID);*/
                if (scanResult.SSID.equals(mNetworkSsidToConnect)) {
                    mNetworkScanResultToConnect = scanResult;
                }

                if (scanResult.SSID.toLowerCase().contains(Constants.WAC_SSID.toLowerCase())) { /* SoundStream For Gear4 App*/

                    wifiConnect.addSacDevices(scanResult.SSID);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            /**duplicate device check*/
                            if (!sacDevices.contains(scanResult.SSID)) {

                                handler.removeMessages(Constants.PREPARATION_INITIATED);
                                handler.sendEmptyMessage(Constants.PREPARATION_COMPLETED);
                                sacProgressBar.setVisibility(View.GONE);
                                sacDevices.add(scanResult.SSID);
                                sacAdapter.notifyDataSetChanged();
                                sacListView.invalidateViews();
                            }
//                                mSpeakerExpandableListAdapter.addNewChildToAdapter(1, scanResult.SSID);
                        }
                    });

                }
            }
        }
        if (wifiConnect.getAllSacDevice().length == 0) {
            wifiConnect.addSacDevices(getResources().getString(R.string.title_no_sac_device));
        }
        lastupdatedTime = System.currentTimeMillis();
    }


    /* Shfit Piriorty and Save */
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


    private void callCleanup(){

    }
    /**
     * Start to connect to a specific wifi network
     */
    private void connectToSpecificNetwork() {


        new Thread(new Runnable() {
            @Override
            public void run() {
                //  loadSacDevicesFromScanResultList();
                ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
                //    mWifiManager.disconnect();
                if (networkInfo.isConnected() && wifiInfo.getSSID().replace("\"", "").equals(mNetworkSsidToConnect)) {
                    if (mNetworkSsidToConnect.toLowerCase().contains(Constants.WAC_SSID.toLowerCase())) {
                        try {
                            /*while (!ping("192.168.43.1")) {
//                        setMessageToLoader("Getting Ip Address...");
                            }*/
                            handleAfterConnectedToSacDevice();
                            ///  unregisterReceiver(connectionReceiver);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        handleAfterConnectedToMainSsid();
                    }
                    return;
                }
                final WifiConfiguration conf = new WifiConfiguration();

                conf.allowedAuthAlgorithms.clear();
                conf.allowedGroupCiphers.clear();
                conf.allowedPairwiseCiphers.clear();
                conf.allowedProtocols.clear();
                conf.allowedKeyManagement.clear();


                LibreLogger.d(this, "Network Security  Scan Resut To Connect From Function " + getScanResultSecurity(mNetworkSecurityToConnect));
                LibreLogger.d(this, "Network Security To Connect " + mNetworkSecurityToConnect);

                conf.SSID = convertToQuotedString(mNetworkSsidToConnect);  //convertToQuotedString(mScanResult.SSID);

                // Make it the highest priority.
                int newPri = getMaxPriority(mWifiManager) + 1;
                if (newPri > MAX_PRIORITY) {
                    newPri = shiftPriorityAndSave(mWifiManager);
                }
                conf.priority = newPri;
                LibreLogger.d(this, "Network Security To Connect " + mNetworkSecurityToConnect +
                        "priority Generated is " + newPri);
                mPriorityGiven = newPri;
                conf.status = WifiConfiguration.Status.ENABLED;

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
                        conf.allowedPairwiseCiphers.set(WifiConfiguration.AuthAlgorithm.OPEN);
                        break;
                }
                // mWifiManager.disconnect(); // For Ls9


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

                connectionReceiver = new ConnectionReceiver();
                IntentFilter intentFilter = new IntentFilter();

                intentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
                intentFilter.addAction("android.net.wifi.STATE_CHANGE");

                intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
                intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

                intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
                intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
                intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
                registerReceiver(connectionReceiver, intentFilter);
                LibreLogger.d(this, "Wifi MAnager calling  Register ConenctionReceived");

                boolean mCatchDisconnect = mWifiManager.enableNetwork(netId, true);
                LibreLogger.d(this, "Wifi MAnager calling enableNetwork" + mCatchDisconnect);
                //  mWifiManager.reconnect(); //Removal For Ls9b
            }
        }).start();


        //unregisterReceiver(connectionReceiver);
    }

    private void handleAfterConnectedToMainSsid() {
        LibreLogger.d(this, "Handle After Connected to Main SSID " + wifiConnect.getPreviousSSID() + "SSID :: " +
                getConnectedSsidInfo());
        if (!wifiConnect.getPreviousSSID().contains(getConnectedSsidInfo())) {
//            enableNetworkChangeCallBack();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    restartApplicationForNetworkChanges(NewSacDevicesActivity.this, getConnectedSsidInfo());
                }
            });

        }
        closeLoader();

        try {
            sendMSearchWithoutClear();
//            loadLssdpDevicesFromCentralRepo();
            loadSacDevicesFromScanResultList();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /* Setting Maximum Priority to Wifi Network*/
    private static int getMaxPriority(final WifiManager wifiManager) {
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

    public static String convertToQuotedString(String string) {
        if (TextUtils.isEmpty(string)) {
            return "";
        }

        final int lastPos = string.length() - 1;
        if (lastPos > 0 && (string.charAt(0) == '"' && string.charAt(lastPos) == '"')) {
            return string;
        }

        return "\"" + string + "\"";
    }


    public boolean ping(final String url) {
        try {
            String returnvalue = new pingAsyncTask().execute(url).get();
            if (!returnvalue.equals("0")) {
                return true;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return false;
        }
        return false;

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


    /* Ping For Checking Whether Ping Async Task is Configured ..*/
    public class pingAsyncTask extends AsyncTask<String, Integer, String> {

        /* private final PingConnectionListener listener;
         pingAsyncTask(PingConnectionListener listener)
         {
             this.listener=listener;
         }*/
        @Override
        protected String doInBackground(String... params) {
            InetAddress addr = null;
            try {
                addr = InetAddress.getByName(params[0]);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            try {
                if (addr.isReachable(7000)) {
                    LibreLogger.d(this, "InetAddress" + "\n" + addr + "- Respond OK");
                    //   listener.pingSuccess();
                    /*which means device has got ip address so getting device name*/
//                    handleAfterConnectedToSacDevice();
                    return "1";
                } else {
                    // LibreLogger.d(this, "InetAddress" + "\n" + addr + "- Respond False");
                    // listener.pingFailed();
                    return "0";
                }
            } catch (IOException e) {
                e.printStackTrace();
                return "0";
            }

        }
    }


    /* Handle After Connected To SAC Device */
    public void handleAfterConnectedToSacDevice() {
//        getDeviceName(mNetworkSsidToConnect);
        getDeviceName(mNetworkSsidToConnect, "");
    }

    private void getDeviceName(final String netId, String ss) {
        LSDeviceClient lsDeviceClient = new LSDeviceClient();
        LSDeviceClient.DeviceNameService deviceNameService = lsDeviceClient.getDeviceNameService();


        deviceNameService.getSacDeviceName(new Callback<String>() {
            @Override
            public void success(String deviceName, Response response) {

                Intent ssid = new Intent(NewSacDevicesActivity.this, ssid_configuration.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                wifiConnect.putssidDeviceNameSAC(netId, deviceName);

                if (deviceName!=null) {
                    Log.e("SACDeviceName---", deviceName);
                    ssid.putExtra("DeviceIP", "192.168.43.2");
                    ssid.putExtra("activity", "SacListActivity");
                    ssid.putExtra("DeviceSSID", mNetworkSsidToConnect);

                    startActivity(ssid);
                    finish();
                }

            }

            @Override
            public void failure(RetrofitError error) {
                Toast.makeText(getApplicationContext(),"Please  retry By Clicking the " + mNetworkSsidToConnect, Toast.LENGTH_SHORT).show();

                try{
                    closeLoader();
                    /*removing timeout callback*/
                    if (pdCanceller != null)
                        pdCanceller.removeCallbacks(progressRunnable);

                }catch(Exception e){

                }

            }
        });

    }

//    /* Getting Device Name  */
//    public void getDeviceName(final String netid) {
//        new HttpHandler() {
//
//            @Override
//            public void onError(final int responseCode) {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Toast.makeText(getApplicationContext(), "Error occured in Getting DeviceName as a" +
//                                " response code as " + responseCode, Toast.LENGTH_SHORT).show();
//                    }
//                });
//
//                closeLoader();
//            }
//
//            @Override
//            public HttpURLConnection getHttpURLConnectionMethod() {
//                try {
//                    URL url = new URL("http://192.168.43.1:80/devicename.asp");
//                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
//                    return urlConnection;
//                } catch (Exception e) {
//
//                }
//                HttpURLConnection urlConnection = null;
//                return urlConnection;
//            }
//
//            @Override
//            public void onResponse(String result) {
//                LibreLogger.d(this, "SSID_CONFIGURATION, GET" + result);
////                setMessageToLoader("Getting Device Name");
//                if (result != "") {
//                    /* If I Got The Device Name Properly Then I am Changing the Screen To ssid_configuration*/
//                    wifiConnect.putssidDeviceNameSAC(netid, result);
//                    Message msg = new Message();// = ((Message) m_handler).obtain(m_handler, 0x10, node);
//                    msg.what = Constants.CONNECTED_TO_SAC_DEVICE;
//
//                    closeHTTPURLConnection();
////                    closeLoader();
//                    Intent ssid = new Intent(OnlySacActivity.this, ssid_configuration.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    wifiConnect.putssidDeviceNameSAC(netid, result);
//
//                    Log.e("SAC", msg.getData().toString());
//                    ssid.putExtra("DeviceIP", "192.168.43.2");
//                    ssid.putExtra("activity", "SacListActivity");
//                    ssid.putExtra("DeviceSSID", mNetworkSsidToConnect);
//
//                    startActivity(ssid);
//                    finish();
//
//                } else {
//                    getDeviceName(netid);
//                }
//            }
//
//            @Override
//            public void closeHTTPURLConnection() {
//                HttpURLConnection mURL = getHttpURLConnectionMethod();
//                mURL.disconnect();
//            }
//        }.execute();
//    }

    public void showLoader(final String msg) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("ShowingLoader", "Showing loader method");
                if (mProgressDialog == null) {
                    mProgressDialog = ProgressDialog.show(NewSacDevicesActivity.this, getResources().getString(R.string.notice), getResources().getString(R.string.connecting) + "...", true, true, null);
                }
                mProgressDialog.setCancelable(false);
                if (!mProgressDialog.isShowing()) {
                    if (!(NewSacDevicesActivity.this.isFinishing())) {
                        mProgressDialog = ProgressDialog.show(NewSacDevicesActivity.this, getResources().getString(R.string.notice), getResources().getString(R.string.connecting) + "...", true, true, null);
                    }
                }

            }
        });
    }

    public void closeLoader() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mProgressDialog != null && mProgressDialog.isShowing()) {
                    mProgressDialog.setCancelable(false);
                    mProgressDialog.dismiss();
                    mProgressDialog.cancel();
                }
            }
        });

    }


    /**
     * Broadcast receiver for connection related events
     */
    private class ConnectionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            WifiInfo wifiInfo = mWifiManager.getConnectionInfo();

//            setMessageToLoader("" + wifiInfo.getSupplicantState());

//            if (wifiInfo.getSupplicantState() == SupplicantState.DISCONNECTED) {
//                mProgressDialog.setMessage("Failure Of Connection" + wifiInfo.getSSID());
//                closeLoader();
//                return;
//            }

            LibreLogger.d(this, "Supplicant State " + wifiInfo.getSupplicantState());
            LibreLogger.d(this, "Network Info " + networkInfo.getState());
            LibreLogger.d(this, "Network Info " + networkInfo.isConnectedOrConnecting());


             if (networkInfo.isConnected()) {
                 if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {

                     LibreLogger.d(this, "Wifi Info " + wifiInfo.getSSID());
                     LibreLogger.d(this, "mNetworkSSIDToConnect " + mNetworkSsidToConnect);
                     LibreLogger.d(this, "Network State " + networkInfo.getState());

                     if (wifiInfo.getSSID().contains(mNetworkSsidToConnect)) {

                         if (mNetworkSsidToConnect.toLowerCase().contains(Constants.WAC_SSID.toLowerCase())) {

                             final List<WifiConfiguration> configurations = mWifiManager.getConfiguredNetworks();

                             for (final WifiConfiguration config : configurations) {

                                 if (config.priority == mPriorityGiven) {
                                     LibreLogger.d(this, "Priroity Set for the ssid " + config.SSID
                                             + "is " + config.priority);
                                 }
                         /*  if(config.SSID.contains(mNetworkSsidToConnect)){
                               LibreLogger.d(this,"Priroity Set for the ssid " + mNetworkSsidToConnect
                                + "is " + config.priority);
                           }*/
                             }

                             try {/*
                            while (!ping("192.168.43.1")) {
//                                setMessageToLoader("Getting Ip Address...");
                            }*/
                                 handleAfterConnectedToSacDevice();

                             } catch (Exception e) {
                                 e.printStackTrace();
                             }


                             try {
                                 unregisterReceiver(connectionReceiver);
                             } catch (Exception e) {
                                 e.printStackTrace();
                             }

                         } else {
//                        setMessageToLoader("Connected" + wifiInfo.getSSID());
                             handleAfterConnectedToMainSsid();
                         }
                     } else {
//                    Toast.makeText(context, "Connection attempt to " + mNetworkSsidToConnect + " Failed please try manually", Toast.LENGTH_SHORT).show();
//                    closeLoader();
                     }
                 } else if (wifiInfo.getSupplicantState() == SupplicantState.INACTIVE
                         || wifiInfo.getSupplicantState() == SupplicantState.INTERFACE_DISABLED) {
//                Toast.makeText(context, "Connection attempt to " + mNetworkSsidToConnect + " Failed please try manually", Toast.LENGTH_SHORT).show();
//                closeLoader();
                 }
             }
        }
    }


    @Override
    protected void onStop() {

        try {
            /*removing timeout callback*/
            if (pdCanceller != null)
                pdCanceller.removeCallbacks(progressRunnable);
            //  if (receiverWifi != null && receiverWifi.isOrderedBroadcast())
            unregisterReceiver(receiverWifi);
            stopTimer();
            // if (connectionReceiver != null && connectionReceiver.isOrderedBroadcast())

        } catch (Exception e) {

        }
        try {
            unregisterReceiver(connectionReceiver);
        } catch (Exception e) {

        }
        try {
            closeLoader();
        } catch (Exception e) {

        }
        unRegisterForDeviceEvents();
        super.onStop();
    }


    private class SacAdapter extends BaseAdapter {

        List<String> list;
        Context context;

        public SacAdapter(Context context, List<String> list) {
            this.list = list;
            this.context = context;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int i) {
            return i;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup viewGroup) {
            Holder holder;

            if (convertView == null) {
                holder = new Holder();
                LayoutInflater inflater = (LayoutInflater) (context).getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//                convertView = inflater.inflate(R.layout.new_manage_devices_list_item, null);

                convertView = inflater.inflate(R.layout.list_item, null);


                holder.DeviceName = (TextView) convertView.findViewById(R.id.lblListItem);


//                holder.DeviceName = (TextView) convertView.findViewById(R.id.tvDeviceName);

                convertView.setTag(holder);
            }
            holder = (Holder) convertView.getTag();

            holder.DeviceName.setText(list.get(position));
            return convertView;
        }
    }

    public class Holder {
        TextView DeviceName;
        ImageButton mute;
    }
}
