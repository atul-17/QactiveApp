package com.libre.ManageDevice;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.libre.ActiveScenesListActivity;
import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity;
import com.libre.LErrorHandeling.LibreError;
import com.libre.LibreApplication;
import com.libre.LibreWebViewActivity;
import com.libre.NewManageDevices.NewManageDevices;
import com.libre.R;
import com.libre.Scanning.Constants;
import com.libre.Scanning.ScanningHandler;
import com.libre.SceneObject;
import com.libre.constants.DeviceMasterSlaveFreeConstants;
import com.libre.constants.LSSDPCONST;
import com.libre.constants.LUCIMESSAGES;
import com.libre.constants.MIDCONST;
import com.libre.luci.LSSDPNodeDB;
import com.libre.luci.LSSDPNodes;
import com.libre.luci.LUCIControl;
import com.libre.luci.LUCIPacket;
import com.libre.netty.LibreDeviceInteractionListner;
import com.libre.netty.NettyData;
import com.libre.util.LibreLogger;

import java.util.ArrayList;

public class CreateNewScene extends CTDeviceDiscoveryActivity implements LibreDeviceInteractionListner {

    ManageDeviceHandler mManageDeviceHandler = ManageDeviceHandler.getInstance();
    ScanningHandler mScanHandler = ScanningHandler.getInstance();
//    Intent mAddDeviceToMasterIntent;
    ListView deviceListView ;
    String[] deviceNameList;
    private ImageButton m_back;
    SwipeRefreshLayout swipeRefreshLayout;
    FreeDeviceListAdapter freeDeviceListAdapter;

    public String mMasterIpSelected = "";
    public String mMasterZoneIdSelected = "";
    public String mMasterConcurrentIdSelected = "";
    private Menu mMenu;
    ArrayAdapter adapter;
    ArrayList list = new ArrayList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_source_device);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        registerForDeviceEvents(this);

        deviceListView = (ListView) findViewById(R.id.active_devices_list);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh);
        freeDeviceListAdapter =  new FreeDeviceListAdapter(CreateNewScene.this,
                R.layout.activity_layout_textview_item);
    /*    m_back = (ImageButton) findViewById(R.id.back);
        m_back.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });*/

        /* Populate the Free Devices */
            addFreeDevicesFromCentralNodeDBToAdapter();
           deviceListView.setAdapter(freeDeviceListAdapter);
     //   }
        /* Device List View Select Item Listener : Selecting Master */
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

                if (freeDeviceListAdapter.isLS9Available && position == freeDeviceListAdapter.FreeDeviceLSSDPNodeMap.size()) {

                    // deep linking
                    if (googleTosCheckIsSuccess())
                    launchTheApp("com.google.android.apps.chromecast.app","CREATE_GROUP");


                } else {
                    try {
                        LSSDPNodes mMasterNode = FreeDeviceListAdapter.FreeDeviceLSSDPNodeMap.get(position);
                        if (mMasterNode != null)
//                clickingOnFreeDevice(mMasterNode);
                /*temporary change removing dialog*/
                            mHandleclickingOnFreeDevice(mMasterNode);
                    } catch (Exception e) {

                    }
                }
            }
        });
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeRefreshLayout.setRefreshing(true);
                /*freeDeviceListAdapter.clear();
                freeDeviceListAdapter.notifyDataSetChanged();*/
                clearMenuDetails();
                refreshDevices();


            }
        });

    }
    public void clickingOnFreeDevice(final LSSDPNodes mNode){
        final LUCIControl mLuciCommandToSend = new LUCIControl(mNode.getIP());
        final Dialog dialog = new Dialog(CreateNewScene.this);

        //setting custom layout to dialog
        dialog.setContentView(R.layout.free_ddms_change_dialog);
        dialog.setTitle(getString(R.string.grouping));

        //adding text dynamically
        TextView txt = (TextView) dialog.findViewById(R.id.tvTitleQues);
        txt.setText(getString(R.string.groupingMsg));

        //adding button click event
        Button btnJoinAll = (Button) dialog.findViewById(R.id.btnJoinAll);
        btnJoinAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Message msg = new Message();
                msg.what = DeviceMasterSlaveFreeConstants.LUCI_SEND_MASTER_COMMAND;
                uiHandler.sendEmptyMessage(msg.what);


                Intent mAddDeviceToMasterIntent = new Intent(CreateNewScene.this, NewManageDevices.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mAddDeviceToMasterIntent.putExtra("activity_name", "CreateNewScene");
                mMasterIpSelected = mNode.getIP();
                mAddDeviceToMasterIntent.putExtra("master_ip", mMasterIpSelected);

                mLuciCommandToSend.SendCommand(MIDCONST.MID_JOIN_OR_DROP, LUCIMESSAGES.JOIN_ALL, LSSDPCONST.LUCI_SET);
                dialog.dismiss();
            }
        });
        Button btnSetMaster = (Button) dialog.findViewById(R.id.btnSetSlave);
        btnSetMaster.setText("Set Master");
        btnSetMaster.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHandleclickingOnFreeDevice(mNode);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    public void mHandleclickingOnFreeDevice(LSSDPNodes mMasterNode){
        if(mMasterNode==null)
            return;
        if(mMasterNode.getCurrentSource() == MIDCONST.GCAST_SOURCE
                && mMasterNode.getmPlayStatus() == SceneObject.CURRENTLY_PLAYING){
            LibreError error = new LibreError(mMasterNode.getIP(), Constants.SPEAKER_IS_CASTING,1);
            //BusProvider.getInstance().post(error);
            //return;
        }
        Message msg = new Message();
        msg.what = DeviceMasterSlaveFreeConstants.LUCI_SEND_MASTER_COMMAND;
        uiHandler.sendEmptyMessage(msg.what);
        mMasterIpSelected = mMasterNode.getIP();





//        Intent mAddDeviceToMasterIntent = new Intent(CreateNewScene.this, NewManageDevices.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        mAddDeviceToMasterIntent.putExtra("activity_name", "CreateNewScene");
//        mAddDeviceToMasterIntent.putExtra("master_ip", mMasterIpSelected);

        String Zoneid = "";

                /* instead of depending on the connected SSID, we will use the network mode of the device communicated during the
                Msearch Response.
                 If the networkmode doesnt contain WLAN string then Zone Id is needs to be incremented */

        if (mMasterNode.getNetworkMode().contains("WLAN")==false) {
            Zoneid = mManageDeviceHandler.getUniqueZoneID();
        } else {
            Zoneid = mManageDeviceHandler.getZoneID();
        }

        mMasterZoneIdSelected = Zoneid;
        mMasterConcurrentIdSelected = mMasterNode.getcSSID();

        Log.e("CreateNewScene", "Zone id for " + Zoneid);

        LUCIControl luciControl = new LUCIControl(mMasterNode.getIP());


        ArrayList<LUCIPacket>  setOfCommandsWhileMakingMaster=new ArrayList<LUCIPacket>();
        LUCIPacket  zoneid104Packet = new LUCIPacket(Zoneid.getBytes(), (short) Zoneid.length(), (short) MIDCONST.MID_DDMS_ZONE_ID, (byte) LSSDPCONST.LUCI_SET);
        LibreLogger.d(this, "Slave ZoneID" + Zoneid);
        setOfCommandsWhileMakingMaster.add(zoneid104Packet);

        LUCIPacket  setSlave100Packet = new LUCIPacket(LUCIMESSAGES.SETMASTER.getBytes(), (short) LUCIMESSAGES.SETMASTER.length(), (short) MIDCONST.MID_DDMS, (byte) LSSDPCONST.LUCI_SET);
        setOfCommandsWhileMakingMaster.add(setSlave100Packet);


        luciControl.SendCommand(setOfCommandsWhileMakingMaster);
    }
    @Override
    public void onBackPressed() {
        if(! swipeRefreshLayout.isRefreshing()) {
          /*  if (mScanHandler.getSceneObjectFromCentralRepo().size() > 0)*/ {
                Intent newIntent = new Intent(CreateNewScene.this, ActiveScenesListActivity.class);
                startActivity(newIntent);
                finish();
            } /*else {
                Intent newIntent = new Intent(CreateNewScene.this, PlayNewActivity.class);
                startActivity(newIntent);
                finish();
            }*/
            super.onBackPressed();
        }else{
            Toast.makeText(getApplicationContext(), getString(R.string.refreshing), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerForDeviceEvents(this);
        /*Karuna For Testing ... */
        /*sendMesearchUpdateNodes();*/
        freeDeviceListAdapter.clear();
            addFreeDevicesFromCentralNodeDBToAdapter();
            freeDeviceListAdapter.notifyDataSetChanged();

    }

    public void addFreeDevicesFromCentralNodeDBToAdapter() {

        freeDeviceListAdapter.clear();

        ArrayList<LSSDPNodes> mFreeDeviceListNode = mScanHandler.getFreeDeviceList();
        if(mFreeDeviceListNode.size()>0) {
            freeDeviceListAdapter.clear();
            for (LSSDPNodes mFreeNode : mFreeDeviceListNode) {
                /*if((mFreeNode.getCurrentSource()==MIDCONST.GCAST_SOURCE) &&
                        (mFreeNode.getmPlayStatus() == SceneObject.CURRENTLY_PLAYING)){

                }else*/ {
                    freeDeviceListAdapter.add(mFreeNode);
                }
            }
        /*notifying after adding devices to adapter*/
        freeDeviceListAdapter.notifyDataSetChanged();
        }else if(mFreeDeviceListNode.size()==0){

            freeDeviceListAdapter.notifyDataSetChanged();
        }

    }

    @Override
    protected void onStop() {

        unRegisterForDeviceEvents();
        swipeRefreshLayout.setRefreshing(false);
        uiHandler.removeCallbacksAndMessages(null);


        super.onStop();
    }

    public void noDevicesFound(){
        list.clear();
        list.add(getResources().getString(R.string.noDeviceFound));
        adapter = new ArrayAdapter(CreateNewScene.this,R.layout.free_devices_list_item,R.id.list_item_textView,list);
        deviceListView.setAdapter(adapter);
    }
    public void refreshDevices() {
        LibreLogger.d(this, "RefreshDevices Without clearing");
        final LibreApplication application = (LibreApplication) getApplication();
        // application.getScanThread().clearNodes();
        application.getScanThread().UpdateNodes();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(false);
            }
        }, 10000);
     /*   final LibreApplication application = (LibreApplication) getApplication();
        application.getScanThread().clearNodes();
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

        new Handler().postDelayed(new Runnable() { *//* Swipe Refersh UI will show the circular bar of 1750ms , in this three M-search will happen in the interval of time 500ms *//*
            @Override
            public void run() {
                freeDeviceListAdapter.notifyDataSetChanged();
                swipeRefreshLayout.setRefreshing(false);

                addFreeDevicesFromCentralNodeDBToAdapter();

                *//*we have to launch screen based on Master Free *//*
                if (mScanHandler.getFreeDeviceList().size() == 0) {
                    if (mScanHandler.getSceneObjectFromCentralRepo().size() <= 0) {
                        Intent intent = new Intent(CreateNewScene.this, PlayNewActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Intent intent = new Intent(CreateNewScene.this, ActiveScenesListActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }
                }
            }
        }, 4650);*/
    }


    public void sendMesearchUpdateNodes() {

        final LibreApplication application = (LibreApplication) getApplication();
        application.getScanThread().UpdateNodes();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                application.getScanThread().UpdateNodes();
            }
        }, 500);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                application.getScanThread().UpdateNodes();

            }
        }, 1000);

    }

    /*this method will update data to adapter as well as to Central DB even though some other app is changing state of device*/
    public void UpdateMastersZoneIdOrConcurrentSSSIDInCentralDB(String ipaddress, String mDeviceState) {

        LibreLogger.d(this, ipaddress + "DeviceState-" + mDeviceState);

        LSSDPNodes mToBeUpdateNode = mScanHandler.getLSSDPNodeFromCentralDB(ipaddress);
        LSSDPNodeDB mNodeDB = LSSDPNodeDB.getInstance();

        if (mToBeUpdateNode != null) {
            switch (mDeviceState) {
                case "Master":
                    mToBeUpdateNode.setDeviceState("M");
                    mToBeUpdateNode.setZoneID(mMasterZoneIdSelected);

                     /* adding the concurrentSSID for a safety sake */
                    mToBeUpdateNode.setcSSID(mMasterConcurrentIdSelected);

                      /* manageDevi*/
                    LSSDPNodes mMasterNode = mScanHandler.getLSSDPNodeFromCentralDB(ipaddress);
                    if (mMasterNode != null) {
                        SceneObject mMastersceneObjec = new SceneObject(" ", mMasterNode.getFriendlyname(), 0, mMasterNode.getIP());
                        mScanHandler.putSceneObjectToCentralRepo(ipaddress, mMastersceneObjec);

                        if (mMasterIpSelected.equals(ipaddress)) {


                            /* Added by Praveen as earlier we were finishing the activity before updating the database
                             * Updating db was in the end of switch case but we were finishing the activity before that */
                            mNodeDB.renewLSSDPNodeDataWithNewNode(mToBeUpdateNode);
                            //// sending 107 before navigating
                            LUCIControl luciControl = new LUCIControl(ipaddress);
                            luciControl.SendCommand(MIDCONST.MID_SCENE_NAME, "", LSSDPCONST.LUCI_GET);


                            Log.d("NewRefresh", "Master id being sent from createnew screen " + mMasterIpSelected);
                            Intent mAddDeviceToMasterIntent = new Intent(CreateNewScene.this, NewManageDevices.class);
                            mAddDeviceToMasterIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            mAddDeviceToMasterIntent.putExtra("activity_name", "CreateNewScene");
                            mAddDeviceToMasterIntent.putExtra("master_ip", ipaddress);
                            startActivity(mAddDeviceToMasterIntent);
                            finish();
                        } else {
                            LibreLogger.d(this, "Igonerd the master created as it is done by other user");
                        }
                    }

                    break;
                case "Slave":
                    mToBeUpdateNode.setDeviceState("S");

                    if (mScanHandler.getconnectedSSIDname(getApplicationContext()) == mScanHandler.HN_MODE) {
                    /*need to change this*/
                        mToBeUpdateNode.setcSSID("");
                    } else {
                    /*need to change this*/
                        mToBeUpdateNode.setZoneID("");
                    }
                    break;
                case "Free":
                    if (mToBeUpdateNode.getDeviceState().equals("M")) {
                        ArrayList<LSSDPNodes> mSlaveList = mScanHandler.getSlaveListForMasterIp(mToBeUpdateNode.getIP(),
                                mScanHandler.getconnectedSSIDname(getApplicationContext()));
                        for (LSSDPNodes mSlaveNode : mSlaveList) {
                            mSlaveNode.setDeviceState("F");
                            mNodeDB.renewLSSDPNodeDataWithNewNode(mSlaveNode);
                        }
                    }
                    mToBeUpdateNode.setDeviceState("F");
                    break;
            }
            mNodeDB.renewLSSDPNodeDataWithNewNode(mToBeUpdateNode);

            addFreeDevicesFromCentralNodeDBToAdapter();
            updateMenuDetails();

            /*sending M-Search*/
            sendMesearchUpdateNodes();

        }

    }


    public String getconnectedSSIDname() {
        WifiManager wifiManager;
        wifiManager = (WifiManager) CreateNewScene.this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String ssid = wifiInfo.getSSID();
        Log.d("CreateNewScene", "getConnectedSSIDName wifiInfo = " + wifiInfo.toString());
        if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
            ssid = ssid.substring(1, ssid.length() - 1);
        }
        Log.d("CreateNewScene", "Connected SSID" + ssid);
        return ssid;
    }

    public ProgressDialog mProgressDialog;

    public void closeLoader() {

        if(CreateNewScene.this.isFinishing())
            return;

           /* putting the try catch to avoid the widnows leak*/
        try {
                if (mProgressDialog != null && mProgressDialog.isShowing()) {
                    Log.e("LuciControl", "progress Dialog Closed");
                    mProgressDialog.setCancelable(true);
                    mProgressDialog.dismiss();
                    mProgressDialog.cancel();

            }
        }catch (Exception e){

        }

    }

    public void showLoader(String msg) {
        if (CreateNewScene.this.isFinishing())
            return;

        if (mProgressDialog == null) {
            Log.e("LUCIControl", "Null");
            mProgressDialog = ProgressDialog.show(CreateNewScene.this, getString(R.string.notice), getString(R.string.changing) + msg + "...", true, true, null);
        }
        mProgressDialog.setCancelable(false);

        if (!mProgressDialog.isShowing()) {
            if (!(CreateNewScene.this.isFinishing())) {
                mProgressDialog = ProgressDialog.show(CreateNewScene.this, getString(R.string.notice), getString(R.string.changing) + msg + "...", true, true, null);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_select_source_device, menu);
        this.mMenu = menu;
        return true;
    }

    public void updateMenuDetails() {
        try {
            MenuItem mMenuItem = this.mMenu.findItem(R.id.numberOfFreeDevices);
            mMenuItem.setTitle("F: " + mScanHandler.getFreeDeviceList().size());
        } catch (Exception e) {

        }
    }

    public void clearMenuDetails() {
        try {
            MenuItem mMenuItem = this.mMenu.findItem(R.id.numberOfFreeDevices);
            mMenuItem.setTitle("F: " + 0);
        } catch (Exception e) {

        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        this.mMenu = menu;
        updateMenuDetails();
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onDestroy() {
        closeLoader();
        super.onDestroy();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.numberOfFreeDevices) {

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void deviceDiscoveryAfterClearingTheCacheStarted() {

    }

    @Override
    public void newDeviceFound(LSSDPNodes node) {
        LibreLogger.d(this, "New Device Found in Create New Device Found" + node.getFriendlyname() + " Ip address" + node.getIP());
        if (node.getDeviceState().contains("F")) {

//            SceneObject sceneObjec = new SceneObject(" ", node.getFriendlyname(), 0, node.getIP());
            freeDeviceListAdapter.add(node);
            freeDeviceListAdapter.notifyDataSetChanged();
            updateMenuDetails();

            /* important to avoid the duplication in the active scenes screen*/
                if (mScanHandler.isIpAvailableInCentralSceneRepo(node.getIP())) {
                    mScanHandler.removeSceneMapFromCentralRepo(node.getIP());
            }

            //   swipeRefreshLayout.setRefreshing(false);
        } else if (node.getDeviceState().contains("M")) {
            if (!mScanHandler.isIpAvailableInCentralSceneRepo(node.getIP())) {
                SceneObject sceneObjec = new SceneObject(" ", node.getFriendlyname(), 0, node.getIP());
                mScanHandler.putSceneObjectToCentralRepo(node.getIP(), sceneObjec);
            }
        }
        /* Commenting Because , Team Suggested Not To make more complicate in Code
         If Master is Selected But it Got rebooted , We Should remove all the Information  *//*
        if(mMasterIpSelected.equalsIgnoreCase(node.getIP())){
            LibreLogger.d(this,"Master Ip selected and new Device Ip Are Same "+ node.getIP());
            closeLoader();
            uiHandler.removeMessages(DeviceMasterSlaveFreeConstants.ACTION_TO_INDICATE_TIMEOUT);
            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_message_devicerebooted) +
                     " " + node.getIP(), Toast.LENGTH_SHORT).show();
            *//*mHandleclickingOnFreeDevice(node);*//*
        }*/
    }

    @Override
    public void deviceGotRemoved(String mIpAddress) {
        // ManageDeviceAdapter.mMasterSpecificSlaveAndFreeDeviceMap.remove(mIpAddress);
        /*LSSDPNodeDB mNodeDB = LSSDPNodeDB.getInstance();
        for(LSSDPNodes mNode:mNodeDB.GetDB()) {
            if(mNode.getIP().equals(mIpAddress)) {
                freeDeviceListAdapter.remove(mNode);
                freeDeviceListAdapter.notifyDataSetChanged();
            }
        }*/
        boolean breakloop = false;
        LibreLogger.d(this,"Device Got Removed "+ mIpAddress);
        if(mMasterIpSelected.equalsIgnoreCase(mIpAddress)){
            uiHandler.removeMessages(DeviceMasterSlaveFreeConstants.ACTION_TO_INDICATE_TIMEOUT);
            uiHandler.sendEmptyMessage(DeviceMasterSlaveFreeConstants.ACTION_TO_INDICATE_TIMEOUT);
        }
        // since getCount returns 1 when no free devices are available,crash may occur. So the if condition
        if(freeDeviceListAdapter.isFreeDeviceAvailable())
            return;
        for (int i = 0; i < freeDeviceListAdapter.getCount(); i++) {
            LSSDPNodes mNode = freeDeviceListAdapter.getItem(i);
            if (mNode!=null && mNode.getIP().equals(mIpAddress)) {
                freeDeviceListAdapter.remove(mNode);
                freeDeviceListAdapter.notifyDataSetChanged();
                updateMenuDetails();
                breakloop = true;
                break;
            }
            if (breakloop)
                break;
        }

    }

    @Override
    public void messageRecieved(NettyData packet) {
        Log.e("CreateNewScene", new String(packet.getMessage()));
        LUCIPacket mLucipacket = new LUCIPacket(packet.getMessage());
        if (mLucipacket.getCommand() == 103) {
            String msg = new String(packet.getMessage());
            Log.e("103MessageCreateScene", "Data is--" + msg + "--And IPAddress is--" + packet.getRemotedeviceIp());
            if (msg.contains("MASTER")) {


                UpdateMastersZoneIdOrConcurrentSSSIDInCentralDB(packet.getRemotedeviceIp(), "Master");

            } else if (msg.contains("SLAVE")) {
                UpdateMastersZoneIdOrConcurrentSSSIDInCentralDB(packet.getRemotedeviceIp(), "Slave");
            } else if (msg.contains("FREE")) {
                UpdateMastersZoneIdOrConcurrentSSSIDInCentralDB(packet.getRemotedeviceIp(), "Free");
            }
        } else if (mLucipacket.getCommand() == 0) {
            LUCIControl mLuci = new LUCIControl(packet.getRemotedeviceIp());
            mLuci.sendAsynchronousCommand();
            LibreLogger.d(this, "Commnad failed!!! got 0 from the device");

        }else if(mLucipacket.getCommand() == MIDCONST.MID_CURRENT_SOURCE){
            LSSDPNodes mNode = mScanHandler.getLSSDPNodeFromCentralDB(packet.getRemotedeviceIp());
            if(mNode.getCurrentSource() == MIDCONST.GCAST_SOURCE) {
                //addFreeDevicesFromCentralNodeDBToAdapter();
            }
        }else if(mLucipacket.getCommand() == MIDCONST.MID_CURRENT_PLAY_STATE){
            LSSDPNodes mNode = mScanHandler.getLSSDPNodeFromCentralDB(packet.getRemotedeviceIp());
            /*if((mNode.getCurrentSource()==MIDCONST.GCAST_SOURCE) &&
                    (mNode.getmPlayStatus() == SceneObject.CURRENTLY_PLAYING)){
                freeDeviceListAdapter.remove(mNode);
                freeDeviceListAdapter.notifyDataSetChanged();
            }else */{
                freeDeviceListAdapter.add(mNode);
                freeDeviceListAdapter.notifyDataSetChanged();
            }


        }
    }

    Handler uiHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Message failedMsg = new Message();
            failedMsg.what = DeviceMasterSlaveFreeConstants.ACTION_TO_INDICATE_TIMEOUT;
            switch (msg.what) {
                case DeviceMasterSlaveFreeConstants.LUCI_SEND_MASTER_COMMAND: {
                    /* initiating the timeout message in case of failure */
                    uiHandler.sendEmptyMessageDelayed(failedMsg.what, 35000);
                    try {
                        showLoader("Master");
                    }catch(Exception e){

                    }
                }
                break;
                case DeviceMasterSlaveFreeConstants.LUCI_SUCCESS_MASTER_COMMAND: {
                    Bundle b = msg.getData();
                    String ipaddress = b.getString("ipAddress");
                    uiHandler.removeMessages(failedMsg.what);
                    closeLoader();
                    UpdateMastersZoneIdOrConcurrentSSSIDInCentralDB(ipaddress, "Master");

//                     /* manageDevi*/
//                    LSSDPNodes mMasterNode = mScanHandler.getLSSDPNodeFromCentralDB(ipaddress);
//                    if (mMasterNode != null) {
//                        SceneObject mMastersceneObjec = new SceneObject(" ", mMasterNode.getFriendlyname(), 0, mMasterNode.getIP());
//                        mScanHandler.putSceneObjectToCentralRepo(ipaddress, mMastersceneObjec);
//                        finish();
//                        startActivity(mAddDeviceToMasterIntent);
//                    }

                }
                break;
                case DeviceMasterSlaveFreeConstants.ACTION_TO_INDICATE_TIMEOUT: {
                    closeLoader();
                    if (!(CreateNewScene.this.isFinishing())) {
                        new AlertDialog.Builder(CreateNewScene.this)
                                .setTitle(getString(R.string.deviceStateChanging))
                                .setMessage(getString(R.string.failed))
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                    }
                                }).setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                    }
                }
                break;
            }

        }
    };

    public void launchTheApp(String appPackageName) {

        Intent intent = getPackageManager().getLaunchIntentForPackage(appPackageName);


        if (intent != null) {
            startActivity(intent);

        } else {

            redirectingToPlayStore(intent, appPackageName);

        }

    }

    public void launchTheApp(String appPackageName,String deepLinkExtension) {

        Intent intent = getPackageManager().getLaunchIntentForPackage(appPackageName);
        if (intent != null) {
          /*  intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);*/
            intent = new Intent(appPackageName+"."+deepLinkExtension);
            startActivity(intent);

        } else {
            redirectingToPlayStore(intent, appPackageName);
        }

    }
    public void redirectingToPlayStore(Intent intent, String appPackageName) {

        try {

            intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse("market://details?id=" + appPackageName));
            startActivity(intent);

        } catch (android.content.ActivityNotFoundException anfe) {

            intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse("http://play.google.com/store/apps/details?id=" + appPackageName));
            startActivity(intent);

        }

    }

    public void showGoogleTosDialog(final Context context) {
        LayoutInflater factory = LayoutInflater.from(this);
        final View gcastDialog = factory.inflate(
                R.layout.gcast_alert_dialog, null);
        TextView textView= (TextView)gcastDialog. findViewById(R.id.google_text);
        textView.setText(clickableString(getString((R.string.google_tos))));
        textView.setAutoLinkMask(RESULT_OK);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setGravity(Gravity.LEFT);
        Linkify.addLinks(clickableString(getString((R.string.google_tos))), Linkify.WEB_URLS);



        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(getString(R.string.googleTerms))
                .setCancelable(false)
                        //                .setMessage(clickableString(getString((R.string.google_tos))))
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .setPositiveButton(getString(R.string.accept), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        SharedPreferences sharedpreferences = getApplicationContext()
                                .getSharedPreferences(Constants.SHOWN_GOOGLE_TOS, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedpreferences.edit();
                        editor.putString(Constants.SHOWN_GOOGLE_TOS, "Yes");
                        editor.commit();


                        LUCIControl control=new LUCIControl("");
                        control.sendGoogleTOSAcceptanceIfRequired(CreateNewScene.this, "true");



                        launchTheApp("com.google.android.apps.chromecast.app");


                    }

                });

        AlertDialog  googAlert = builder.create();
        googAlert.setView(gcastDialog);
        googAlert.show();
    }

    private SpannableString clickableString(String sampleString) {
        SpannableString spannableString = new SpannableString(sampleString);
        ClickableSpan googleTermsClicked = new ClickableSpan() {
            @Override
            public void onClick(View textView) {

                Intent intent=new Intent(getApplicationContext(),LibreWebViewActivity.class);
                intent.putExtra("url", "http://www.google.com/intl/en/policies/privacy/");
                startActivity(intent);
/*
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse("http://www.google.com/intl/en/policies/privacy/"));
                startActivity(intent);
*/





            }
        };


        ClickableSpan termsClicked = new ClickableSpan() {
            @Override
            public void onClick(View textView) {

                Intent intent=new Intent(getApplicationContext(),LibreWebViewActivity.class);
                intent.putExtra("url","https://www.google.com/intl/en/policies/terms/");
                startActivity(intent);

/*
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse("https://www.google.com/intl/en/policies/terms/"));
                startActivity(intent);
*/
            }
        };

        ClickableSpan privacyClicked = new ClickableSpan() {
            @Override
            public void onClick(View textView) {

                Intent intent=new Intent(getApplicationContext(),LibreWebViewActivity.class);
                intent.putExtra("url","http://www.google.com/intl/en/policies/privacy/");
                startActivity(intent);

/*
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse("https://www.google.com/intl/en/policies/privacy/"));
                startActivity(intent);
*/
            }
        };

        spannableString.setSpan(googleTermsClicked, 4, 25, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(termsClicked, 395, 418, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(privacyClicked, 422, 444, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.brand_orange)), 4, 25,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.brand_orange)), 395, 418,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.brand_orange)), 422, 444,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return spannableString;
    }


    private boolean googleTosCheckIsSuccess() {
        SharedPreferences sharedPreferences = getApplicationContext()
                .getSharedPreferences(Constants.SHOWN_GOOGLE_TOS, Context.MODE_PRIVATE);
        String shownGoogle = sharedPreferences.getString(Constants.SHOWN_GOOGLE_TOS, null);
        if (shownGoogle==null){

            showGoogleTosDialog(this);
            return false;
        }

        return true;
    }
}
