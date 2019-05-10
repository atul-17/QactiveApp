package com.libre.Network;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import com.libre.ActiveScenesListActivity;
import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity;
import com.libre.LibreApplication;
import com.libre.R;
import com.libre.SacSettingsOption;
import com.libre.Scanning.Constants;
import com.libre.Scanning.ScanningHandler;
import com.libre.constants.LSSDPCONST;
import com.libre.luci.LSSDPNodeDB;
import com.libre.luci.LSSDPNodes;
import com.libre.luci.LUCIControl;
import com.libre.netty.LibreDeviceInteractionListner;
import com.libre.netty.NettyData;
import com.libre.util.LibreLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;


public class NewSacActivity extends CTDeviceDiscoveryActivity implements LibreDeviceInteractionListner {

    public final int mLower2GHzFrequency = 2412;
    public final int mHigher2GHzFrequency = 2484;

    public static final String PSK = "PSK";
    public static final String WEP = "WEP";
    public static final String OPEN = "Open";
    private static final int MAX_PRIORITY = 99999;
//    public WifiManager mWifiManager = null;
    public ExpandableListView mSpeakersExpandableListView, ssidListView;
    public HashMap<String, LSSDPNodes> mConfiguredDevicesNameHashMap = new HashMap<>();
    public HashMap<String, String> mConfiguredDevicesIpwithNameHashMap = new HashMap<>();
    public ArrayList<String> mConfiguredDevicesName = new ArrayList<>();
    public ArrayList<String> mAddMoreSpeakersTab = new ArrayList<>();
    public ArrayList<LSSDPNodes> mLSSDPDevices = new ArrayList<>();
    public Button btnConfigSetupRoomGroup, btnConfigSetupHomeGroup;
    public TextView sac_list_title, addMoreSpeakers;
    public ProgressDialog mProgressDialog;
    public AlertDialog alertDialog;
    ExpandableListAdapters mSpeakerExpandableListAdapter;
    LSSDPNodeDB lssdpDB = LSSDPNodeDB.getInstance();
    LibreApplication libreApplication;
    WifiReceiver receiverWifi = new WifiReceiver();
    HashMap<String, List<String>> devicesListHashMap = new HashMap<>();
    List<String> deviceListDataHeader = new ArrayList<>();
    ScanningHandler mScanHandler = ScanningHandler.getInstance();
    WifiConnection wifiConnect = WifiConnection.getInstance();
    boolean mFromSacDeviceConfigurationScreen = false;
    boolean mFromManualConfig = false;
    Handler mProgressDialogHandler = new Handler();
    List<ScanResult> mScanResults;
    HashMap<String, ScanResult> mScanResultsWithSsidMap = new HashMap<>();
    String mNetworkSsidToConnect;
    ScanResult mNetworkScanResultToConnect;
    String mNetworkPassKeyToConnect;
    String mNetworkSecurityToConnect;
    ConnectionReceiver connectionReceiver;
    private Handler pdCanceller;
    private Runnable progressRunnable;

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


    /* OnCreeate For New Sac Activity*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sac_list);

        //StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        //StrictMode./(policy);

        /* Wifi Scan results And populate it in mScanResults List */
//        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
//        mWifiManager.startScan();
//        mScanResults = mWifiManager.getScanResults();
        sac_list_title = (TextView) findViewById(R.id.lblListHeader);
        sac_list_title.setText(getString(R.string.sac_device_titel));
        addMoreSpeakers = (TextView) findViewById(R.id.addMoreSpeakers);
        addMoreSpeakers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //disableNetworkOffCallBack();
                //disableNetworkChangeCallBack();
                startActivity(new Intent(NewSacActivity.this, SacSettingsOption.class));
                finish();
            }
        });
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);

        mFromSacDeviceConfigurationScreen = getIntent().getBooleanExtra("mSacDevice", false);
        mFromManualConfig = getIntent().getBooleanExtra("mManaulConfig", false);

      /* Commenting and Placing it OnResume
      * *//* Regestring Callback *//*
        registerForDeviceEvents(this);
        disableNetworkChangeCallBack();*/

       /* if(! receiverWifi.isOrderedBroadcast())
        registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));*/

        /*UI Elements Enabling.*/
        mSpeakersExpandableListView = (ExpandableListView) findViewById(R.id.sac_listview);
        mSpeakersExpandableListView.setGroupIndicator(null);


        btnConfigSetupHomeGroup = (Button) findViewById(R.id.btnConfig);
        btnConfigSetupRoomGroup = (Button) findViewById(R.id.imgBtnfav);

        btnConfigSetupHomeGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(NewSacActivity.this, "Setup Home Group : Currently Not Active", Toast.LENGTH_SHORT).show();

            }
        });

        btnConfigSetupRoomGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(NewSacActivity.this, "Setup Room Group : Currently Not Active", Toast.LENGTH_SHORT).show();
            }
        });

        mAddMoreSpeakersTab.add(getString(R.string.title_activity_manage_device));
        /* Setting Up Title For Expandable ListViews */
        deviceListDataHeader.add(getString(R.string.lsdp_device_title));
        deviceListDataHeader.add(getString(R.string.sac_device_titel));
        devicesListHashMap.put(deviceListDataHeader.get(0), mConfiguredDevicesName);
        devicesListHashMap.put(deviceListDataHeader.get(1), mAddMoreSpeakersTab);

        //  deviceListDataHeader.add(getString(R.string.sac_device_titel));
        //  devicesListHashMap.put(deviceListDataHeader.get(1), wifiConnect.getAllSacDeviceList());



        /* Inititalizing Adapters */
        mSpeakerExpandableListAdapter = new ExpandableListAdapters(this, deviceListDataHeader, devicesListHashMap, mSpeakersExpandableListView);
        mSpeakersExpandableListView.setAdapter(mSpeakerExpandableListAdapter);

//        loadLssdpDevicesFromCentralRepo();
//        loadSacDevicesFromScanResultList();

        /* Expanding By Default */
        //if (!mSpeakersExpandableListView.isGroupExpanded(0))
        mSpeakersExpandableListView.expandGroup(0, false);
        mSpeakersExpandableListView.expandGroup(1, false);
//        mSpeakersExpandableListView.expandGroup(1, false);
        //if (!mSpeakersExpandableListView.isGroupExpanded(1))

        mSpeakersExpandableListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener(){

            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                return true;
            }
        });


        mSpeakersExpandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView expandableListView, View view, int groupPosition, int childPosition, long id) {
                 /* For Two Title Screens , Child Click it will come to the same screen Callback only ,
                * So To confirm From which Title its coming we are making this check*/

                /* we can Remove this equal Comparison but we have to compare the groupPosition Comparison with Integer */

                /*if (deviceListDataHeader.get(groupPosition).equals(getString(R.string.sac_device_titel))) {

                    if (!wifiConnect.getSacDevice(childPosition).contains(getResources().getString(R.string.title_no_sac_device))) {
                        mNetworkSsidToConnect = wifiConnect.getSacDevice(childPosition);
                        mNetworkPassKeyToConnect = "";
                        mNetworkSecurityToConnect = OPEN;
                        //    mNetworkScanResultToConnect = mScanResultsWithSsidMap.get(mNetworkSsidToConnect);
                        wifiConnect.setPreviousSSID(getConnectedSsidInfo(""));
                        showLoader(mNetworkSsidToConnect);
                        disableNetworkOffCallBack();
                        disableNetworkChangeCallBack();
                        try {
                            if(receiverWifi.isOrderedBroadcast())
                                     unregisterReceiver(receiverWifi);
                        }catch(Exception e){

                        }
                        connectToSpecificNetwork();

                        *//*loader timeout : Increasing the Timeout For H&T as per the Bala's comment by KK *//*
                        pdCanceller.postDelayed(progressRunnable, 90000);


                    }

                }*/
                if (deviceListDataHeader.get(groupPosition).equals(getString(R.string.lsdp_device_title))) {

                    if (!deviceListDataHeader.get(groupPosition).equals(getString(R.string.title_no_lssdp_device))) {
                        try {
                            Intent ssid = new Intent(NewSacActivity.this, LSSDPDeviceNetworkSettings.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            //LSSDPNodes node = lssdpDB.getLSSDPNodeFromPosition(childPosition);
                            final LSSDPNodes node = lssdpDB.getTheNodeBasedOnTheIpAddress(mConfiguredDevicesName.get(childPosition)); //mConfiguredDevicesNameHashMap.get(mConfiguredDevicesName.get(childPosition));
                            //Toast.makeText(getApplicationContext(),"Device Configuration  ",Toast.LENGTH_SHORT).show();
                            if(node==null){
loadLssdpDevicesFromCentralRepo();
                                return false;
}
                            sendLuci(node.getIP());
/*                            if (ping(node.getIP(),"")) */{
                                LibreLogger.d(this,"ping response");
                                closeLoader();
                                ssid.putExtra("DeviceName", node.getFriendlyname());
                                ssid.putExtra(Constants.CURRENT_DEVICE_IP, node.getIP());
                                ssid.putExtra("activityName", "SacListActivity");
                                startActivity(ssid);
                            } /*else {
                                closeLoader();
                                Toast.makeText(getApplicationContext(), "Device Unreachable", Toast.LENGTH_SHORT).show();
                            }*/
                        } catch (Exception e) {
                            LibreLogger.d(this, " Exception");
                        }
                    }
                }else{
                    startActivity(new Intent(NewSacActivity.this, SacActivityScreenForLaunchWifiSettings.class));
                    finish();
                }
                return false;
            }
        });


    }

    private void sendLuci(String currentIpAddress) {
        /*this method is to get UI and asynchronous call*/

        LibreLogger.d(this, "NewSacActivity: Sending the 50 for ip address " + currentIpAddress);
        LUCIControl luciControl = new LUCIControl(currentIpAddress);

        /* This is where we get the currentSource */
        luciControl.SendCommand(50, null, 1);
    }
    @Override
    protected void onDestroy() {
       /* try {
            if (receiverWifi != null && receiverWifi.isOrderedBroadcast())
                unregisterReceiver(receiverWifi);
            if (connectionReceiver != null && connectionReceiver.isOrderedBroadcast())
                unregisterReceiver(connectionReceiver);
        }catch(Exception e){

        }*/
        stopTimer();
        unRegisterForDeviceEvents();
        super.onDestroy();
    }

    @Override
    protected void onResume() {

        /*this is created to handle timeout*/
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), getString(R.string.sacFailed), Toast.LENGTH_SHORT).show();
                closeLoader();
                registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            }
        };
        pdCanceller = new Handler();

        /* for the First Time it should load with ScanResulst */
//        mWifiManager.startScan();
//        mScanResults = mWifiManager.getScanResults();
        registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        /* Regestring Callback */
        registerForDeviceEvents(this);
        /*sendMSearchWithoutClear();*/// Commenting For Discovery
        enableNetworkChangeCallBack();
        enableNetworkOffCallBack();


        loadLssdpDevicesFromCentralRepo();
//        loadSacDevicesFromScanResultList();
        startTimer();
        mSpeakerExpandableListAdapter.notifyDataSetChanged();


        /* This setup for MAnaual Config setup */
        if (mFromManualConfig) {
            new AlertDialog.Builder(NewSacActivity.this)
                    .setTitle(getString(R.string.manualConfig))
                    .setMessage(getString(R.string.connectNewNetwork))
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            enableNetworkChangeCallBack();
                            mNetworkSsidToConnect = wifiConnect.getMainSSID();
                            mNetworkPassKeyToConnect = wifiConnect.getMainSSIDPwd();
                            mNetworkSecurityToConnect = wifiConnect.getMainSSIDSec();
                            mNetworkScanResultToConnect = mScanResultsWithSsidMap.get(mNetworkSsidToConnect);
                            //disableNetworkChangeCallBack();
                            //disableNetworkOffCallBack();
                            connectToSpecificNetwork();
                            mFromManualConfig = false;
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(getApplicationContext(), getString(R.string.discardChanges), Toast.LENGTH_SHORT).show();
                            // sendMSearchWithoutClear();
                            ;
                            dialog.cancel();
                            mFromManualConfig = false;
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        } else {

        /* If Sac Device Is Already Configured it should start Connect to Main SSID : This Changes are done
         * for the  Single Handler Usage */
            if (wifiConnect.getmSACDevicePostDone() || mFromSacDeviceConfigurationScreen) {
                mNetworkSsidToConnect = wifiConnect.getMainSSID();
                mNetworkPassKeyToConnect = wifiConnect.getMainSSIDPwd();
                mNetworkSecurityToConnect = wifiConnect.getMainSSIDSec();
                mNetworkScanResultToConnect = mScanResultsWithSsidMap.get(mNetworkSsidToConnect);
               // disableNetworkChangeCallBack();
                //disableNetworkOffCallBack();
                // connectToSpecificNetwork();

                wifiConnect.setmSACDevicePostDone(false);
                mFromManualConfig = false;
                mFromSacDeviceConfigurationScreen = false;
                mFromSacDeviceConfigurationScreen = false;

            }
        }

        super.onResume();
    }

    @Override
    protected void onStop() {

        try {
            /*removing timeout callback*/
            if (pdCanceller != null)
                pdCanceller.removeCallbacks(progressRunnable);
            //  if (receiverWifi != null && receiverWifi.isOrderedBroadcast())
            unregisterReceiver(receiverWifi);
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
        stopTimer();
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_sac_list, menu);

        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        /* Check For Master to Swithc the screens */
      /*  boolean mMasterNotFound =
                mScanHandler.getSceneObjectFromCentralRepo().isEmpty();*/

        boolean mMasterNotFound = LSSDPNodeDB.getInstance().isAnyMasterIsAvailableInNetwork();

       /* if (!mMasterNotFound) {
            Intent intent = new Intent(NewSacActivity.this, PlayNewActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } else */{
            Intent intent = new Intent(NewSacActivity.this, ActiveScenesListActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
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
        if (id == R.id.action_refresh) {
            final LibreApplication application = (LibreApplication) getApplication();
          /* commenting for not clearing the change */
          //  application.getScanThread().clearNodes();
           // mConfiguredDevicesName.clear();
           // mConfiguredDevicesName.add(getResources().getString(R.string.title_for_refreshing));
            //mSpeakerExpandableListAdapter.notifyDataSetChanged();
            // loadSacDevicesFromScanResultList();

            sendMSearchWithoutClear();


            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void deviceDiscoveryAfterClearingTheCacheStarted() {

    }

    @Override
    public void newDeviceFound(LSSDPNodes node) {

        SharedPreferences sharedPreferences = getApplicationContext()
                .getSharedPreferences("sac_configured", Context.MODE_PRIVATE);
        String str = sharedPreferences.getString("deviceFriendlyName", "");
        if (str != null && str.equalsIgnoreCase(node.getFriendlyname().toString()) && node.getgCastVerision()!=null) {
            LibreLogger.d(this,"bhargav ");
            //sending time zone to the gCast device
            LUCIControl luciControl = new LUCIControl(node.getIP());
            luciControl.SendCommand(222, "2:" + TimeZone.getDefault().getID().toString(), LSSDPCONST.LUCI_SET);
            //Toast.makeText(getApplicationContext(),str + " in preference",Toast.LENGTH_SHORT).show();


        AlertDialog.Builder builder = new AlertDialog.Builder(NewSacActivity.this);
        builder.setCancelable(false)
                .setMessage(node.getFriendlyname().toString())
                .setNegativeButton(getString(R.string.noThanks), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .setPositiveButton(getString(R.string.learnMore), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.setData(Uri.parse("https://www.google.com/cast/audio/learn"));
                        startActivity(intent);

                    }

                });

        AlertDialog alert = builder.create();
        alert.show();


            sharedPreferences.edit().remove("deviceFriendlyName").commit();
        } else if (node != null && node.getgCastVerision() == null && str.equalsIgnoreCase(node.getFriendlyname())) {
            sharedPreferences.edit().remove("deviceFriendlyName").commit();
        }
        if(mConfiguredDevicesName.contains(getResources().getString(R.string.title_for_refreshing)))
            mSpeakerExpandableListAdapter.removeChildFromAdapter(0, getResources().getString(R.string.title_for_refreshing));

        mSpeakerExpandableListAdapter.removeChildFromAdapter(0, getResources().getString(R.string.title_no_lssdp_device));
        mSpeakerExpandableListAdapter.addNewChildToAdapter(0, node.getIP());
        mSpeakerExpandableListAdapter.notifyDataSetChanged();
        // loadLssdpDevicesFromCentralRepo();
    }

    @Override
    public void deviceGotRemoved(String mIpAddress) {
      /*  Toast.makeText(getApplicationContext(), "Device got Removed " + mIpAddress,
                Toast.LENGTH_SHORT).show();*/
        String mDeviceName = mConfiguredDevicesIpwithNameHashMap.get(mIpAddress);

        if (mConfiguredDevicesName.contains(mIpAddress)) {
            mConfiguredDevicesName.remove(mIpAddress);
        }
        mSpeakerExpandableListAdapter.removeChildFromAdapter(0, mIpAddress);//l  ssdpDB.getTheNodeBasedOnTheIpAddress(mIpAddress).getFriendlyname());
//        mSpeakerExpandableListAdapter.notifyDataSetChanged();


        mConfiguredDevicesIpwithNameHashMap.remove(mIpAddress);

        LibreLogger.d(this, "Device Got Removed And size of the HashMAp" + mConfiguredDevicesIpwithNameHashMap.size());
        if (mConfiguredDevicesName.size() == 0) {
            mSpeakerExpandableListAdapter.addNewChildToAdapter(0, getResources().getString(R.string.title_no_lssdp_device));
        }
        mSpeakerExpandableListAdapter.notifyDataSetChanged();
/*        mConfiguredDevicesNameHashMap.remove(mConfiguredDevicesName.get(mConfiguredDevicesName.indexOf(mDeviceName)));
        mConfiguredDevicesName.remove(mDeviceName);
        mConfiguredDevicesIpwithNameHashMap.remove(mIpAddress);

        if(mConfiguredDevicesIpwithNameHashMap.size()==0){
            mSpeakerExpandableListAdapter.addNewChildToAdapter(0, getResources().getString(R.string.title_no_lssdp_device));
        }*/
        //loadLssdpDevicesFromCentralRepo();
    }

    @Override
    public void messageRecieved(NettyData packet) {

    }

    public void sendMSearchWithoutClear() {

        LibreLogger.d(this, "Sending M Search Without Clear");
        final LibreApplication application = (LibreApplication) getApplication();
        //application.getScanThread().clearNodes();
        application.getScanThread().UpdateNodes();

      /*  new Handler().postDelayed(new Runnable() {
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
        }, 2150);*/

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                application.getScanThread().UpdateNodes();
               if(mConfiguredDevicesName.contains(getResources().getString(R.string.title_for_refreshing))){
                    mConfiguredDevicesName.remove(getResources().getString(R.string.title_for_refreshing));
                    mConfiguredDevicesName.add(getResources().getString(R.string.title_no_lssdp_device));
                    mSpeakerExpandableListAdapter.removeChildFromAdapter(0, getResources().getString(R.string.title_for_refreshing));
                    mSpeakerExpandableListAdapter.addNewChildToAdapter(0, getResources().getString(R.string.title_no_lssdp_device));
                    mSpeakerExpandableListAdapter.notifyDataSetChanged();

                }

            }
        }, 10000);
    }

    /* Load Configured Devices From Central Repo*/
    public void loadLssdpDevicesFromCentralRepo() {

        LSSDPNodeDB mCentralRepoDb = LSSDPNodeDB.getInstance();
        ArrayList<LSSDPNodes> mAllDeviceList = mCentralRepoDb.GetDB();
        LibreLogger.d(this, "LoadLSSDPDEvicesFromCentralREpo" + mAllDeviceList.size());
        //mConfiguredDevicesName.clear();
        mConfiguredDevicesNameHashMap.clear();
        mConfiguredDevicesName.remove(getResources().getString(R.string.title_for_refreshing));
        if (mAllDeviceList.size() > 0) {
            if(mSpeakerExpandableListAdapter.isChildAvailable(0,getResources().getString(R.string.title_no_lssdp_device))) {
                mSpeakerExpandableListAdapter.removeChildFromAdapter(0, getResources().getString(R.string.title_no_lssdp_device));
            }

            for (LSSDPNodes mNode : mAllDeviceList) {
                mConfiguredDevicesNameHashMap.put(mNode.getFriendlyname(), mNode);
                mConfiguredDevicesIpwithNameHashMap.put(mNode.getIP(), mNode.getFriendlyname());
                if (!mConfiguredDevicesName.contains(mNode.getIP()))
                    mConfiguredDevicesName.add(mNode.getIP());
                mSpeakerExpandableListAdapter.addNewChildToAdapter(0, mNode.getIP());
            }
        } else {
            mSpeakerExpandableListAdapter.addNewChildToAdapter(0, getResources().getString(R.string.title_no_lssdp_device));
            if (!mConfiguredDevicesName.contains(getResources().getString(R.string.title_no_lssdp_device))) {
                mConfiguredDevicesName.add(getResources().getString(R.string.title_no_lssdp_device));

            }
        }
        if (mSpeakerExpandableListAdapter != null)
            mSpeakerExpandableListAdapter.notifyDataSetChanged();
    }

/*    *//* Load SAC Devices From Wifi Scan Results *//*
    public void loadSacDevicesFromScanResultList() {
       *//* mWifiManager.startScan();
        mScanResults = mWifiManager.getScanResults();*//*
        wifiConnect.clearSacDevices();
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
                    wifiConnect.putWifiScanResultSecurity(scanResult.SSID, scanResult.capabilities);
                }
                *//* Commenting For Not Using After Review
                mScanResultsWithSsidMap.put(scanResult.SSID, scanResult);*//*
                if (scanResult.SSID.equals(mNetworkSsidToConnect)) {
                    mNetworkScanResultToConnect = scanResult;
                }

                if (scanResult.SSID.contains("SoundStream_")) {
                    wifiConnect.addSacDevices(scanResult.SSID);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mSpeakerExpandableListAdapter.addNewChildToAdapter(1, scanResult.SSID);
                        }
                    });

                }

            }
        }
        if (wifiConnect.getAllSacDevice().length == 0) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mSpeakerExpandableListAdapter.addNewChildToAdapter(1, getResources().getString(R.string.title_no_sac_device));
                }
            });

            wifiConnect.addSacDevices(getResources().getString(R.string.title_no_sac_device));
        }
        if (mSpeakerExpandableListAdapter != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mSpeakerExpandableListAdapter.notifyDataSetChanged();
                }
            });

        }


    }*/

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
     * Get the security type of the wireless network
     *
     * @param scanResult the wifi scan result
     * @return one of WEP, PSK of OPEN
     */
    private String getScanResultSecurity(ScanResult scanResult) {
        final String cap = scanResult.capabilities;
        final String[] securityModes = {WEP, PSK};
        for (int i = securityModes.length - 1; i >= 0; i--) {
            if (cap.contains(securityModes[i])) {
                return securityModes[i];
            }
        }

        return OPEN;
    }

    /* Get current Connected SSID INfo*/
//    public String getConnectedSsidInfo() {
//        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
//        return wifiInfo.getSSID().replace("\"", "");
//    }


    /* This method is made only for previousSSID as once you have connected to LSConfigure still we have to show previous connected ssid*/
//    public String getConnectedSsidInfo(String previousSSID) {
//
//        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
//        if (wifiInfo.getSSID().replace("\"", "").startsWith(Constants.WAC_SSID)) {
//            return wifiConnect.getPreviousSSID();
//        }
//        return wifiInfo.getSSID().replace("\"", "");
//    }

    /* Get ScanResult From My SSID */
    public ScanResult getScanResultForSsid(String mNetworkSsid) {
        if (mScanResults.size() == 0) {
//            mScanResults = mWifiManager.getScanResults();
        }
        for (ScanResult mScan : mScanResults) {
            if (mScan.SSID.equals(mNetworkSsid)) {
                return mScan;
            }
        }
        return null;
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

    public int mPriorityGiven = -1;

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
//                WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
                //    mWifiManager.disconnect();
                /*if (networkInfo.isConnected() && wifiInfo.getSSID().replace("\"", "").equals(mNetworkSsidToConnect)) {
                    if (mNetworkSsidToConnect.contains(Constants.WAC_SSID)) {
                        try {
                            while (!ping("192.168.43.1")) {
//                        setMessageToLoader("Getting Ip Address...");
                            }
                            handleAfterConnectedToSacDevice();
                            ///  unregisterReceiver(connectionReceiver);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
//                setMessageToLoader("Connected" + wifiInfo.getSSID());
                        handleAfterConnectedToMainSsid();
                        *//*try {
                            unregisterReceiver(connectionReceiver);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }*//*
                    }
                    return;
                }*/
              /*  ScanResult mScanResult = mScanResultsWithSsidMap.get(mNetworkSsidToConnect); //getScanResultForSsid(mNetworkSsidToConnect);

                if (mScanResult == null) {
                    mScanResult = mNetworkScanResultToConnect;
                }*/
//                showLoader(mNetworkSsidToConnect);

                final WifiConfiguration conf = new WifiConfiguration();

                conf.allowedAuthAlgorithms.clear();
                conf.allowedGroupCiphers.clear();
                conf.allowedPairwiseCiphers.clear();
                conf.allowedProtocols.clear();
                conf.allowedKeyManagement.clear();

                //   LibreLogger.d(this,"Network SSID To Connect "+ mScanResult.SSID);
                //  LibreLogger.d(this,"Network BSSID To Connect "+ mScanResult.BSSID);
                //LibreLogger.d(this,"Network Security  Scan Resut To Connect "+ mScanResult.capabilities);
                LibreLogger.d(this, "Network Security  Scan Resut To Connect From Function " + getScanResultSecurity(mNetworkSecurityToConnect));

                LibreLogger.d(this, "Network Security To Connect " + mNetworkSecurityToConnect);
                // conf.SSID = convertToQuotedString(mScanResult.SSID);

                //if(mSca)
                conf.SSID = convertToQuotedString(mNetworkSsidToConnect);  //convertToQuotedString(mScanResult.SSID);
                //  conf.BSSID = mScanResult.BSSID;

                // Make it the highest priority.
//                int newPri = getMaxPriority(mWifiManager) + 1;
//                if (newPri > MAX_PRIORITY) {
//                    newPri = shiftPriorityAndSave(mWifiManager);
//                }
//                conf.priority = newPri;
//                LibreLogger.d(this, "Network Security To Connect " + mNetworkSecurityToConnect +
//                        "priority Generated is " + newPri);
//                mPriorityGiven = newPri;
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
                     /*   conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);*/

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


//                int netId = mWifiManager.addNetwork(conf);

//                LibreLogger.d(this, "Netid We got For the Ssid is " + mNetworkSsidToConnect +
//                        "and Netid is " + netId);

//                if (netId == -1) {
//                    LibreLogger.d(this, "Failed to set the settings for  " + mNetworkSsidToConnect);
//                    final List<WifiConfiguration> mWifiConfiguration = mWifiManager.getConfiguredNetworks();
//                    for (int i = 0; i < mWifiConfiguration.size(); i++) {
//                        String configSSID = mWifiConfiguration.get(i).SSID;
//                        LibreLogger.d(this, "Config SSID" + configSSID + "Active SSID" + conf.SSID);
//                        if (configSSID.equals(conf.SSID)) {
//                            netId = mWifiConfiguration.get(i).networkId;
//                            LibreLogger.d(this, "network id" + netId);
//                            break;
//                        }
//                    }
//                }

              /*  if (netId == -1) {
                    final List<WifiConfiguration> configurations = mWifiManager.getConfiguredNetworks();
                    final int size = configurations.size();
                    for (int i = 0; i < size; i++) {
                        final WifiConfiguration config = configurations.get(i);
                        config.priority = conf.priority;
                        mWifiManager.updateNetwork(config);
                    }
                    mWifiManager.saveConfiguration();
                }*/
             /*   boolean mCatchDisconnect = mWifiManager.disconnect();
                LibreLogger.d(this,"Wifi MAnager calling Disconnect" + mCatchDisconnect );*/
                connectionReceiver = new ConnectionReceiver();
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
                intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
                intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
                registerReceiver(connectionReceiver, intentFilter);
                LibreLogger.d(this, "Wifi MAnager calling  Register ConenctionReceived");

//                boolean mCatchDisconnect = mWifiManager.enableNetwork(netId, true);
//                LibreLogger.d(this, "Wifi MAnager calling enableNetwork" + mCatchDisconnect);
                //  mWifiManager.reconnect(); //Removal For Ls9b
            }
        }).start();


        //unregisterReceiver(connectionReceiver);
    }

    /* Ping For Checking Whether SAC Device is Configured ..*/
    public boolean ping(final String url, PingConnectionListener listner) {
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
        }/*
        try {
            addr = InetAddress.getByName(url);
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;

        }
        final InetAddress finalAddr = addr;
        new Thread(new Runnable() {
            public void run() {
                try {
                    if (finalAddr.isReachable(5000)) {
                        LibreLogger.d(this,"InetAddress" + "\n"+ url + "- Respond OK");
                    } else {
                        LibreLogger.d(this,"InetAddress" + "\n"+ url + "- Respond False");
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block

                }
            }
        }).start();*/
        return false;

    }
    public  int pingHost(String host, int timeout) throws IOException,
            InterruptedException {
        Runtime runtime = Runtime.getRuntime();
        timeout /= 1000;
        String cmd = "ping -c 5 -W " + timeout + " " + host;
        Process proc = runtime.exec(cmd);
        LibreLogger.d(this,"Ping Result : " + cmd);
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(proc.getInputStream()));
        int read;
        char[] buffer = new char[4096];
        StringBuffer output = new StringBuffer();
        while ((read = reader.read(buffer)) > 0) {
            output.append(buffer, 0, read);
        }
        reader.close();
        LibreLogger.d(this,"Ping result" + output.toString()+ "For URL" + host);
        proc.waitFor();
        int exit = proc.exitValue();
        return exit;
    }
    public boolean ping(String url) {
        showLoader(getString(R.string.checkingDeviceAvailability));
        InetAddress addr = null;
        try {
            addr = InetAddress.getByName(url);
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;

        }
        try {
            int Result = pingHost(url, 500); //For RELEASE
            LibreLogger.d(this,"First Ping Host" + "\n"+ url + "- Result " + Result);
            if(Result != 0 ){
             /*   int mResult = pingHost(url,5500);
                LibreLogger.d(this,"Second Ping Host" + "\n"+ url + "- Result " + Result);*/
               /* if(mResult !=0 )
                    return false;
                else
                    return true;*/
                return false;
            }else{
                return true;
            }


        } catch (IOException e) {
            // TODO Auto-generated catch block
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }
    public boolean ping(final String url,String mvale) {
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
        }/*
        try {
            addr = InetAddress.getByName(url);
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;

        }
        final InetAddress finalAddr = addr;
        new Thread(new Runnable() {
            public void run() {
                try {
                    if (finalAddr.isReachable(5000)) {
                        LibreLogger.d(this,"InetAddress" + "\n"+ url + "- Respond OK");
                    } else {
                        LibreLogger.d(this,"InetAddress" + "\n"+ url + "- Respond False");
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block

                }
            }
        }).start();*/
        return false;

    }

    /* Handle After Connected To SAC Device */
    public void handleAfterConnectedToSacDevice() {
        getDeviceName(mNetworkSsidToConnect);
    }

    /* Handle After Connected To Main SSID */
    public void handleAfterConnectedToMainSsid() {
//        LibreLogger.d(this, "Handle After Connected to Main SSID " + wifiConnect.getPreviousSSID() + "SSID :: " +
//                getConnectedSsidInfo());
//        if (!wifiConnect.getPreviousSSID().contains(getConnectedSsidInfo())) {
//            //disableNetworkOffCallBack();
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    restartApplicationForNetworkChanges(NewSacActivity.this, getConnectedSsidInfo());
//                }
//            });
//
//        }
        closeLoader();

        try {
            sendMSearchWithoutClear();
            loadLssdpDevicesFromCentralRepo();
            //loadSacDevicesFromScanResultList();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* Getting Device Name  */
    public void getDeviceName(final String netid) {
        new HttpHandler() {

            @Override
            public void onError(int value) {
                closeLoader();
            }

            @Override
            public HttpURLConnection getHttpURLConnectionMethod() {
                try {
                    URL url = new URL("http://192.168.43.1:80/devicename.asp");
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    return urlConnection;
                } catch (Exception e) {

                }
                HttpURLConnection urlConnection = null;
                return urlConnection;
            }

            @Override
            public void onResponse(String result) {
                LibreLogger.d(this, "SSID_CONFIGURATION, GET" + result);
//                setMessageToLoader("Getting Device Name");
                if (result != "") {
                    /* If I Got The Device Name Properly Then I am Changing the Screen To ssid_configuration*/
                    wifiConnect.putssidDeviceNameSAC(netid, result);
                    Message msg = new Message();// = ((Message) m_handler).obtain(m_handler, 0x10, node);
                    msg.what = Constants.CONNECTED_TO_SAC_DEVICE;

                    closeHTTPURLConnection();
//                    closeLoader();
                    Intent ssid = new Intent(NewSacActivity.this, ssid_configuration.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    wifiConnect.putssidDeviceNameSAC(netid, result);

                    Log.e("SAC", msg.getData().toString());
                    ssid.putExtra("DeviceIP", "192.168.43.2");
                    ssid.putExtra("activity", "SacListActivity");
                    ssid.putExtra("DeviceSSID", mNetworkSsidToConnect);

                    startActivity(ssid);
                    finish();

                } else {
                    getDeviceName(netid);
                }
            }

            @Override
            public void closeHTTPURLConnection() {
                HttpURLConnection mURL = getHttpURLConnectionMethod();
                mURL.disconnect();
            }
        }.execute();
    }
    /***/
    Timer timer;
    TimerTask timerTask;
    public void startTimer() {
        //set a new Timer
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();

        //schedule the timer, after the first 5000ms the TimerTask will run every 5000ms
        timer.schedule(timerTask, 5000, 5000); //
    }

    public void stopTimer() {
        //stop the timer, if it's not already null
        LibreLogger.d(this,"Timer is Stopping");
        if (timer != null) {
            timer.cancel();
            timerTask.cancel();
            timerTask = null;
            timer = null;
            LibreLogger.d(this,"Timer is Stopped Successfully ");
        }

    }

    public void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {

                /*Karuna For Testing ... */
                //use a handler to run a toast that shows the current timestamp
                /*sendMSearchAfterConfiguration();*/
            }
        };
    }

    public void sendMSearchAfterConfiguration(){
        final LibreApplication application = (LibreApplication) getApplication();
        application.getScanThread().UpdateNodes();
    }


    public void setMessageToLoader(final String mMessage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mProgressDialog != null && mProgressDialog.isShowing()) {
                    mProgressDialog.setMessage(mMessage);
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

//    public void showLoader(final String msg) {
//
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                Log.d("ShowingLoader", "Showing loader method");
//                if (mProgressDialog == null) {
//                    mProgressDialog = ProgressDialog.show(NewSacActivity.this, "Notice", msg + "...", true, true, null);
//                }
//                mProgressDialog.setCancelable(false);
//                if (!mProgressDialog.isShowing()) {
//                    if (!(NewSacActivity.this.isFinishing())) {
//                        mProgressDialog = ProgressDialog.show(NewSacActivity.this, "Notice", msg + "...", true, true, null);
//                    }
//                }
//
//            }
//        });
//    }

    public void showLoader(final String msg) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("ShowingLoader", "Showing loader method");
                if (mProgressDialog == null) {
                    mProgressDialog = ProgressDialog.show(NewSacActivity.this, getString(R.string.notice), getString(R.string.connecting) + "...", true, true, null);
                }
                mProgressDialog.setCancelable(false);
                if (!mProgressDialog.isShowing()) {
                    if (!(NewSacActivity.this.isFinishing())) {
                        mProgressDialog = ProgressDialog.show(NewSacActivity.this, getString(R.string.notice), getString(R.string.connecting) + "...", true, true, null);
                    }
                }

            }
        });
    }


    public class WifiReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context c, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {

//                mWifiManager.startScan();
//                mScanResults = mWifiManager.getScanResults();
                //loadSacDevicesFromScanResultList();
            }
        }
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
                if (addr.isReachable(1500)) {
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

    /**
     * Broadcast receiver for connection related events
     */
    private class ConnectionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

//            WifiInfo wifiInfo = mWifiManager.getConnectionInfo();

//            setMessageToLoader("" + wifiInfo.getSupplicantState());

//            if (wifiInfo.getSupplicantState() == SupplicantState.DISCONNECTED) {
//                mProgressDialog.setMessage("Failure Of Connection" + wifiInfo.getSSID());
//                closeLoader();
//                return;
//            }

//            LibreLogger.d(this, "Supplicant State " + wifiInfo.getSupplicantState());
            LibreLogger.d(this, "Network Info " + networkInfo.getState());
            LibreLogger.d(this, "Network Info " + networkInfo.isConnectedOrConnecting());


            // if (networkInfo.isConnected()) {
//            if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
//
////                LibreLogger.d(this, "Wifi Info " + wifiInfo.getSSID());
//                LibreLogger.d(this, "mNetworkSSIDToConnect " + mNetworkSsidToConnect);
//                LibreLogger.d(this, "Network State " + networkInfo.getState());
//
//                if (wifiInfo.getSSID().contains(mNetworkSsidToConnect)) {
//
//                    if (mNetworkSsidToConnect.contains(Constants.WAC_SSID)) {
//
//                        final List<WifiConfiguration> configurations = mWifiManager.getConfiguredNetworks();
//
//                        for (final WifiConfiguration config : configurations) {
//
//                            if (config.priority == mPriorityGiven) {
//                                LibreLogger.d(this, "Priroity Set for the ssid " + config.SSID
//                                        + "is " + config.priority);
//                            }
//                         /*  if(config.SSID.contains(mNetworkSsidToConnect)){
//                               LibreLogger.d(this,"Priroity Set for the ssid " + mNetworkSsidToConnect
//                                + "is " + config.priority);
//                           }*/
//                        }
//
//                        try {
//                           /* while (!ping("192.168.43.1")) {
////                                setMessageToLoader("Getting Ip Address...");
//                            }*/
//                            handleAfterConnectedToSacDevice();
//
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//
//
//                        try {
//                            unregisterReceiver(connectionReceiver);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//
//                    } else {
////                        setMessageToLoader("Connected" + wifiInfo.getSSID());
//                        handleAfterConnectedToMainSsid();
//                    }
//                } else {
////                    Toast.makeText(context, "Connection attempt to " + mNetworkSsidToConnect + " Failed please try manually", Toast.LENGTH_SHORT).show();
////                    closeLoader();
//                }
//            } else if (wifiInfo.getSupplicantState() == SupplicantState.INACTIVE
//                    || wifiInfo.getSupplicantState() == SupplicantState.INTERFACE_DISABLED) {
////                Toast.makeText(context, "Connection attempt to " + mNetworkSsidToConnect + " Failed please try manually", Toast.LENGTH_SHORT).show();
////                closeLoader();
//            }
        }
    }


}

interface PingConnectionListener {

    void pingSuccess();

    void pingFailed();
};