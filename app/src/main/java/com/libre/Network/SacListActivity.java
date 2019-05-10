package com.libre.Network;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
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
import android.widget.Toast;

import com.libre.ActiveScenesListActivity;
import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity;
import com.libre.LibreApplication;
import com.libre.R;
import com.libre.Scanning.Constants;
import com.libre.Scanning.ScanningHandler;
import com.libre.luci.LSSDPNodeDB;
import com.libre.luci.LSSDPNodes;
import com.libre.netty.LibreDeviceInteractionListner;
import com.libre.netty.NettyData;
import com.libre.util.LibreLogger;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class SacListActivity extends CTDeviceDiscoveryActivity implements LibreDeviceInteractionListner {

    public static WifiManager mWifiManager = null;
    static ProgressDialog progressDialog;
    public ExpandableListView mSpeakersExpandableListView, ssidListView;
    public ArrayList<String> mConfiguredDevicesName = new ArrayList<>();
    public ArrayList<LSSDPNodes> mLSSDPDevices = new ArrayList<>();
    public Button btnConfigSetupRoomGroup, btnConfigSetupHomeGroup;
    public ProgressDialog mProgressDialog;
    public AlertDialog alertDialog;
    ExpandableListAdapters listAdapter;
    LSSDPNodeDB lssdpDB = LSSDPNodeDB.getInstance();
    LibreApplication libreApplication;
    WifiReceiver receiverWifi = new WifiReceiver();
    HashMap<String, List<String>> devicesListHashMap = new HashMap<>();
    List<String> deviceListDataHeader = new ArrayList<>();
    ScanningHandler mScanHandler = ScanningHandler.getInstance();
    WifiConnection wifiConnect = WifiConnection.getInstance();
    boolean mFromSacDeviceConfigurationScreen = false;
    boolean mFromManualConfig = false;
    Handler mHandler = new Handler() {

        public void handleMessage(final Message msg) {
            if (msg.what == Constants.HTTP_POST_DONE_SUCCESSFULLY) {
                Log.e("sendPostRunnable", "CONNECTE TO POST");
                wifiConnect.setmSACDevicePostDone(false);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showLoader(wifiConnect.getMainSSID());
                    }
                });
                connecToSSId(wifiConnect.getMainSSID(), wifiConnect.getMainSSIDPwd(), false);
                mLSSDPDevices.clear();
                mConfiguredDevicesName.clear();
                refreshDeviceListInUi();
            } else if (msg.what == Constants.CONNECTED_TO_MAIN_SSID) {
                handleScanResultsAvailable();

                sendMSearchWithoutClear();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeLoader();
                    }
                });
                Log.e("SACLISTACTIVITY", "DEVICE Connected To Main SSID......");

            } else if (msg.what == Constants.LSSDP_NEW_NODE_FOUND) {
                Log.e("SACLISTACTIVITY", "DEVICE CONFIGURED SUCCESSFULLY......");
                Toast.makeText(getApplicationContext(), getString(R.string.deviceSuccesfullyConfig), Toast.LENGTH_SHORT).show();

            } else if (msg.what == Constants.CONNECTED_TO_SAC_DEVICE) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        handleScanResultsAvailable();
                        closeLoader();
                        Toast.makeText(getApplicationContext(), getString(R.string.connectedtoSAC) + msg.getData().toString(), Toast.LENGTH_SHORT).show();
                        Intent ssid = new Intent(SacListActivity.this, ssid_configuration.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        Bundle b = msg.getData();
                        String netId = b.getString("netid");
                        String deviceNmae = b.getString("deviceName");

                        Log.e("SAC", msg.getData().toString());
                        ssid.putExtra("DeviceIP", "192.168.43.2");
                        ssid.putExtra("activity", "SacListActivity");
                        ssid.putExtra("DeviceSSID", (netId));
                        startActivity(ssid);
                        finish();
                    }
                });
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sac_list);

        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);

        mFromSacDeviceConfigurationScreen = getIntent().getBooleanExtra("mSacDevice", false);
        mFromManualConfig = getIntent().getBooleanExtra("mManaulConfig", false);

        /* Regestring Callback */
        registerForDeviceEvents(this);
        disableNetworkChangeCallBack();

        registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        /*UI Elements Enabling.*/
        mSpeakersExpandableListView = (ExpandableListView) findViewById(R.id.sac_listview);
        mSpeakersExpandableListView.setGroupIndicator(null);

        if (wifiConnect.getAllSacDevice().length == 0) {
            wifiConnect.addSacDevices(getResources().getString(R.string.title_no_sac_device));
            mSpeakersExpandableListView.setClickable(false);
        }

        btnConfigSetupHomeGroup = (Button) findViewById(R.id.btnConfig);
        btnConfigSetupRoomGroup = (Button) findViewById(R.id.imgBtnfav);

        btnConfigSetupHomeGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(SacListActivity.this, "Setup Home Group : Currently Not Active", Toast.LENGTH_SHORT).show();

            }
        });

        btnConfigSetupRoomGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(SacListActivity.this, "Setup Room Group : Currently Not Active", Toast.LENGTH_SHORT).show();
            }
        });

        /* Setting Up Title For Expandable ListViews */
        deviceListDataHeader.add(getString(R.string.lsdp_device_title));
        devicesListHashMap.put(deviceListDataHeader.get(0), mConfiguredDevicesName);

        deviceListDataHeader.add(getString(R.string.sac_device_titel));
        devicesListHashMap.put(deviceListDataHeader.get(1), wifiConnect.getAllSacDeviceList());

        /* Inititalizing Adapters */
        listAdapter = new ExpandableListAdapters(this, deviceListDataHeader, devicesListHashMap,mSpeakersExpandableListView);
        mSpeakersExpandableListView.setAdapter(listAdapter);

        if (!mSpeakersExpandableListView.isGroupExpanded(0))
            mSpeakersExpandableListView.expandGroup(0, false);
        if (!mSpeakersExpandableListView.isGroupExpanded(1))
            mSpeakersExpandableListView.expandGroup(1, false);



        /* Sac List View Group Expand Listener */
        mSpeakersExpandableListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            @Override
            public void onGroupExpand(int groupPosition) {

                if (!mSpeakersExpandableListView.isGroupExpanded(0))
                    mSpeakersExpandableListView.expandGroup(0, false);
                if (!mSpeakersExpandableListView.isGroupExpanded(1))
                    mSpeakersExpandableListView.expandGroup(1, false);

                LibreApplication application = (LibreApplication) getApplication();
                application.getScanThread().clearNodes();
                application.getScanThread().UpdateNodes();

                sendMSearchWithoutClear();
                refreshDeviceListInUi();
            }
        });


        /* SAC List View Group Collapse Listener */
        mSpeakersExpandableListView.setOnGroupCollapseListener(new ExpandableListView.OnGroupCollapseListener() {
            @Override
            public void onGroupCollapse(int groupPosition) {
                if (!mSpeakersExpandableListView.isGroupExpanded(0))
                    mSpeakersExpandableListView.expandGroup(0, false);
                if (!mSpeakersExpandableListView.isGroupExpanded(1))
                    mSpeakersExpandableListView.expandGroup(1, false);
                LibreLogger.d(this, "setOnGroupCollapseListener" + groupPosition);
                sendMSearchWithoutClear();
                refreshDeviceListInUi();
            }
        });

        /* SAC List View Child Click Listener  */
        mSpeakersExpandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                                        int groupPosition, int childPosition, long id) {
                // TODO Auto-generated method stub
                /* For Two Title Screens , Child Click it will come to the same screen Callback only ,
                * So To confirm From which Title its coming we are making this check*/

                /* we can Remove this equal Comparison but we have to compare the groupPosition Comparison with Integer */

                if (deviceListDataHeader.get(groupPosition).equals(getString(R.string.sac_device_titel))) {

                    if (!wifiConnect.getSacDevice(childPosition).contains(getResources().getString(R.string.title_no_sac_device))) {
                        mWifiManager.disconnect();
                        ///   showLoader(wifiConnect.getSacDevice(childPosition));
                        wifiConnect.setMainSSIDDetails(getconnectedSSIDname(), "", "");
                        handleScanResultsAvailable();

                        connecToSSId(wifiConnect.getSacDevice(childPosition), "", true);
                    }

                } else if (deviceListDataHeader.get(groupPosition).equals(getString(R.string.lsdp_device_title))) {
                    if (!(mConfiguredDevicesName.contains(getResources().getString(R.string.title_no_lssdp_device)))) {

                        try {
                            Intent ssid = new Intent(SacListActivity.this, LSSDPDeviceNetworkSettings.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            LSSDPNodes node = lssdpDB.getLSSDPNodeFromPosition(childPosition);
                            ssid.putExtra("DeviceName", node.getFriendlyname());
                            ssid.putExtra(Constants.CURRENT_DEVICE_IP, node.getIP());
                            ssid.putExtra("activityName", "SacListActivity");
                            startActivity(ssid);
                        } catch (Exception e) {
                            LibreLogger.d(this, " Exception");
                        }

                    }
                }
                return false;
            }
        });
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiverWifi);
        unRegisterForDeviceEvents();
        super.onDestroy();
    }

    public void getConfiguredSpeakersName() {
        LSSDPNodeDB mLSSDPNodeDB = LSSDPNodeDB.getInstance();
        if (mLSSDPNodeDB.GetDB().size() > 0) {
            mConfiguredDevicesName.clear();

            ArrayList<LSSDPNodes> clonedNodeArray = (ArrayList<LSSDPNodes>) mLSSDPNodeDB.GetDB().clone();
            for (LSSDPNodes mNode : clonedNodeArray) {
                LibreLogger.d(this, "Node Adding" + mNode.getFriendlyname());
                if (!mConfiguredDevicesName.contains(mNode.getFriendlyname())) {
                    mConfiguredDevicesName.add(mNode.getFriendlyname());
                }
            }
        } else {
            mConfiguredDevicesName.clear();
            mConfiguredDevicesName.add(getString(R.string.title_no_lssdp_device));
        }
    }

    public void handleScanResultsAvailable() {
       // wifiConnect.clearWifiScanResult();
        wifiConnect.clearSacDevices();

        mWifiManager.startScan();
        List<ScanResult> list = mWifiManager.getScanResults();

        if (list != null) {
            for (int i = list.size() - 1; i >= 0; i--) {

                final ScanResult scanResult = list.get(i);
                if (scanResult == null) {
                    continue;
                }

                if (TextUtils.isEmpty(scanResult.SSID)) {
                    continue;
                }
                //wifiConnect.putWifiScanResultSecurity(scanResult.SSID, scanResult.capabilities);
                if (scanResult.SSID.contains("GRConfigure_")) {
                    wifiConnect.addSacDevices(scanResult.SSID);
                }
            }
        }
        if (wifiConnect.getAllSacDevice().length == 0) {
            wifiConnect.addSacDevices(getResources().getString(R.string.title_no_sac_device));
        }
    }

    @Override
    public void deviceDiscoveryAfterClearingTheCacheStarted() {

    }

    @Override
    protected void onResume() {
        super.onResume();

        registerForDeviceEvents(this);

        if (mFromManualConfig) {
            new AlertDialog.Builder(SacListActivity.this)
                    .setTitle(getString(R.string.manualConfig))
                    .setMessage(getString(R.string.connectNewNetwork))
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            enableNetworkChangeCallBack();
                            mHandler.sendEmptyMessage(Constants.HTTP_POST_DONE_SUCCESSFULLY);
                            mFromManualConfig = false;
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(getApplicationContext(), getString(R.string.discardChanges), Toast.LENGTH_SHORT).show();
                            sendMSearchWithoutClear();
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
                //showLoader(wifiConnect.getMainSSID());
                //  mHandler.sendEmptyMessage(Constants.HTTP_POST_DONE_SUCCESSFULLY);
                connecToSSId(wifiConnect.getMainSSID(), wifiConnect.getMainSSIDPwd(), false);
                wifiConnect.setmSACDevicePostDone(false);
                mFromManualConfig = false;
                mFromSacDeviceConfigurationScreen = false;
                mFromSacDeviceConfigurationScreen = false;
                mLSSDPDevices.clear();
                mConfiguredDevicesName.clear();
                refreshDeviceListInUi();
            }
        }
        LibreApplication application = (LibreApplication) getApplication();
        application.getScanThread().UpdateNodes();
        wifiConnect.clearSacDevices();
        handleScanResultsAvailable();
        refreshDeviceListInUi();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unRegisterForDeviceEvents();
    }

    @Override
    public void newDeviceFound(LSSDPNodes node) {
        Log.e("New Node Found", node.getFriendlyname());
        // Toast.makeText(getApplicationContext(),"New Node Found:"+node.getFriendlyname(),Toast.LENGTH_SHORT).show();
        wifiConnect.clearSacDevices();
        mLSSDPDevices.add(node);
        refreshDeviceListInUi();
    }

    @Override
    public void deviceGotRemoved(String mIpAddress) {
        LibreLogger.d(this, "Device Got Removed" + mIpAddress);
        refreshDeviceListInUi();
    }

    @Override
    public void messageRecieved(NettyData packet) {

    }

    public void connecToSSId(final String mNetworkSsidToConnect, String passkey, final boolean bSACDEVICE) {

        if (!bSACDEVICE) {/* If Its Not SAC Device We are showing Loader and Disconnect From Previous Network Connect*/
            showLoader(mNetworkSsidToConnect);
            //mWifiManager.disconnect();
        }
        handleScanResultsAvailable();
        mWifiManager.disconnect();
        EnableWifi wifiEnabler = new EnableWifi(getApplicationContext(), mNetworkSsidToConnect.trim(), passkey.trim(), wifiConnect.getWifiScanResutSecurity(mNetworkSsidToConnect.trim()), true);
        EnableWifi.setMAXRETRY(3);
        showLoader("Started connecting");
        wifiEnabler.setWifiConnectionListener(new EnableWifi.OnWiFiConnectionListener() {

            public void onConnecting(final String msg) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mProgressDialog.setMessage(getString(R.string.status) + msg);
                    }
                });

                LibreLogger.d(this, msg);
                //We Didnt Do Anything Because we are already showing a Loader stating Connecting to <SSID Name>
            }

            public void onWiFiConnected(boolean is_connected_to_correct_ssid,
                                        String failure_reason_if_any) {
                if (is_connected_to_correct_ssid) {
                    if (bSACDEVICE) {

                        try { /* Sleep Given For Getting Ip From Speakers, Because DHCP Ip from Speaker will Take Time , This is approximate value */
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        getDeviceName(mNetworkSsidToConnect); // InitiateSACDeviceNameFetch
                    } else {
                        Toast.makeText(getApplicationContext(),
                                "Status : Connected to " + mNetworkSsidToConnect + "Reason : " + failure_reason_if_any,
                                Toast.LENGTH_SHORT).show();
                        mHandler.sendEmptyMessageDelayed(Constants.CONNECTED_TO_MAIN_SSID, 10000);
                        closeLoader();
                        ;
                    }

                } else {
                    Toast.makeText(getApplicationContext(), "Failure : Connection to " + mNetworkSsidToConnect + "Reason : " + failure_reason_if_any,
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String msg) {
                if (msg.contains(mNetworkSsidToConnect)) {
                    //   warningalertDialogShow(getApplicationContext(),"Wifi Network Connection Error");
                    closeLoader();
                    ;
                }
            }
        });
        wifiEnabler.execute();
    }

    public void warningalertDialogShow(Context context, String message) {
        if (!SacListActivity.this.isFinishing()) {
            if (alertDialog == null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        alertDialog = new AlertDialog.Builder(SacListActivity.this).create();
                        alertDialog.setTitle(getString(R.string.wifiError));
                        alertDialog.setMessage(getString(R.string.failed)+"\n " +
                                getString(R.string.wifiError));
                        alertDialog.setButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                alertDialog.dismiss();
                            }
                        });
                        alertDialog.show();
                    }
                });

            }
        }
    }

    public void getDeviceName(final String netid) {
        new HttpHandler() {

            @Override
            public void onError(int value) {

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
                Log.e("SSID_CONFIGURATION, GET", result);
                if (result != "") {
                    /* If I Got The Device Name Properly Then I am Changing the Screen To ssid_configuration*/
                    wifiConnect.putssidDeviceNameSAC(netid, result);
                    Message msg = new Message();// = ((Message) m_handler).obtain(m_handler, 0x10, node);
                    msg.what = Constants.CONNECTED_TO_SAC_DEVICE;
                    Bundle b = new Bundle();
                    b.putString("netid", netid);
                    b.putString("deviceName", result);
                    msg.setData(b);
                    mHandler.sendMessage(msg);
                    closeHTTPURLConnection();
                } else {
                    getDeviceName(netid);
                }
            }

            @Override
            public void closeHTTPURLConnection() {
                HttpURLConnection mURL = getHttpURLConnectionMethod();
                mURL.disconnect();
                ;
            }
        }.execute();
    }

    public void refreshDeviceListInUi() {

        getConfiguredSpeakersName();
        handleScanResultsAvailable();
        if (wifiConnect.mSACDevicesList != null && wifiConnect.mSACDevicesList.size() == 0) {
            wifiConnect.addSacDevices(getResources().getString(R.string.title_no_sac_device));
        }
        if (listAdapter != null)
            listAdapter.notifyDataSetChanged();
    }

    public String getconnectedSSIDname() {
        WifiManager wifiManager;
        wifiManager = (WifiManager) SacListActivity.this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String ssid = wifiInfo.getSSID();
        Log.d("CreateNewScene", "getConnectedSSIDName wifiInfo = " + wifiInfo.toString());
        if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
            ssid = ssid.substring(1, ssid.length() - 1);
        }
        Log.d("SacListActivity", "Connected SSID" + ssid);
        return ssid;
    }


    public void sendMSearchWithoutClear() {

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
    }

    public void closeLoader() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mProgressDialog != null && mProgressDialog.isShowing()) {
                    Log.e("LuciControl", "progress Dialog Closed");
                    mProgressDialog.setCancelable(false);
                    mProgressDialog.dismiss();
                    mProgressDialog.cancel();
                }
            }
        });


    }

    public void showLoader(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mProgressDialog == null) {
                    mProgressDialog = ProgressDialog.show(SacListActivity.this, getString(R.string.notice), msg + "...", true, true, null);
                }
                mProgressDialog.setCancelable(false);

                if (!mProgressDialog.isShowing()) {
                    if (!(SacListActivity.this.isFinishing())) {
                        mProgressDialog = ProgressDialog.show(SacListActivity.this, getString(R.string.notice), msg + "...", true, true, null);
                    }
                }
            }
        });

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
        boolean mMasterNotFound =
                mScanHandler.getSceneObjectFromCentralRepo().isEmpty();

       /* if (mMasterNotFound) {
            Intent intent = new Intent(SacListActivity.this, PlayNewActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } else */{
            Intent intent = new Intent(SacListActivity.this, ActiveScenesListActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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
            application.getScanThread().clearNodes();

            sendMSearchWithoutClear();
            refreshDeviceListInUi();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public class WifiReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context c, Intent intent) {
            handleScanResultsAvailable();
            if (wifiConnect.mSACDevicesList.size() > 0) {

                refreshDeviceListInUi();
                getConfiguredSpeakersName();
            }
        }
    }
}
