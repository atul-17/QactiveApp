package com.libre.ManageDevice;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import com.libre.ActiveScenesListActivity;
import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity;
import com.libre.LibreApplication;
import com.libre.PlayNewActivity;
import com.libre.R;
import com.libre.Scanning.Constants;
import com.libre.Scanning.ScanningHandler;
import com.libre.SceneObject;
import com.libre.SourcesOptionActivity;
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
import com.libre.nowplaying.NowPlayingActivity;
import com.libre.util.LibreLogger;

import java.util.ArrayList;

/**
 * Created by karunakaran on 8/4/2015.
 */
public class ManageDevices extends CTDeviceDiscoveryActivity implements LibreDeviceInteractionListner {
    ListView listView;
    ManageDeviceAdapter mManageDeviceAdapter;
    ManageDeviceHandler  mDeviceHandler = ManageDeviceHandler.getInstance();
    int save=-1;
    public ScanningHandler mScanHandler = ScanningHandler.getInstance();
    SwipeRefreshLayout swipeRefreshLayout;
    Button nextBtn;
    String mMasterIP;
    String mActivityName;

    Menu mMenu;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_managedevices_list);

        Intent myIntent = getIntent(); // gets the previously created intent
        mMasterIP = myIntent.getStringExtra("master_ip");
        mActivityName = myIntent.getStringExtra("activity_name");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        registerForDeviceEvents(this);
        nextBtn = (Button) findViewById(R.id.btnSave);


        if (mActivityName.contains("NowPlayingActivity")) {
            setTitle("Manage Speakers");
            nextBtn.setText("Done");
        }

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh);
        listView = (ListView) findViewById(R.id.listView);

        ManageDeviceAdapter.mMasterSpecificSlaveAndFreeDeviceMap.clear();

     //   LibreApplication application=(LibreApplication)getApplication();
       // application.getScanThread().clearNodes();
     //   application.getScanThread().UpdateNodes();


        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(! (mActivityName.contains("NowPlayingActivity"))){


                    LSSDPNodes mMasterNode = mScanHandler.getLSSDPNodeFromCentralDB(mMasterIP);
                    if(mMasterNode!=null) {

                        SceneObject newSceneObject = new SceneObject(" ", mMasterNode.getFriendlyname(), 0, mMasterNode.getIP());
                        if(!mScanHandler.isIpAvailableInCentralSceneRepo(mMasterIP))
                            mScanHandler.putSceneObjectToCentralRepo(mMasterIP, newSceneObject);
                    }
                    //ActiveSceneAdapter.mMasterSpecificSlaveAndFreeDeviceMap.put(mMasterIP,newSceneObject);

                    Intent mActiveScenesList = new Intent(ManageDevices.this, SourcesOptionActivity.class);
                    mActiveScenesList.putExtra(Constants.CURRENT_DEVICE_IP, mMasterIP);
                    mActiveScenesList.putExtra("current_source", "0");
                    startActivity(mActiveScenesList);
                    finish();
                }else{
                    Intent mActiveScenesList = new Intent(ManageDevices.this, ActiveScenesListActivity.class);
                    startActivity(mActiveScenesList);
                }
            }
        });

        mManageDeviceAdapter = new ManageDeviceAdapter(this,
                R.layout.manage_devices_list_item,
                mManageDevicesHandler,
        mMasterIP,mActivityName);

        listView.setAdapter(mManageDeviceAdapter);






     /*   swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeRefreshLayout.setRefreshing(true);
            //    ManageDeviceAdapter.mMasterSpecificSlaveAndFreeDeviceMap.clear();
                mManageDeviceAdapter.clear();
                mManageDeviceAdapter.notifyDataSetChanged();
                LibreApplication application = (LibreApplication) getApplication();
                application.getScanThread().clearNodes();
                application.getScanThread().UpdateNodes();
            }
        });*/

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeRefreshLayout.setRefreshing(true);
                mManageDeviceAdapter.clear();
                ManageDeviceAdapter.mMasterSpecificSlaveAndFreeDeviceMap.clear();

                mManageDeviceAdapter.notifyDataSetChanged();
                clearMenuDetails();
                /* Swipe Refersh UI will show the circular bar of 1750ms , in this three M-search will happen in the interval of time 500ms */
                refreshDevices();
/*
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        final LibreApplication application = (LibreApplication) getApplication();
                        application.getScanThread().clearNodes();
                        application.getScanThread().UpdateNodes();
                        clearMenuDetails();

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
                        swipeRefreshLayout.setRefreshing(false);
                    }
                }, 2100);*/

            }
        });
    }

    public void refreshDevices(){

        final LibreApplication application = (LibreApplication) getApplication();
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

        new Handler().postDelayed(new Runnable() { /* Swipe Refersh UI will show the circular bar of 1750ms , in this three M-search will happen in the interval of time 500ms */
            @Override
            public void run() {

               // application.getScanThread().UpdateNodes();

                swipeRefreshLayout.setRefreshing(false);
                mManageDeviceAdapter.notifyDataSetChanged();
            }
        }, 2650);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){

        getMenuInflater().inflate(R.menu.menu_manage_devices_list
                , menu);

        super.onCreateOptionsMenu(menu);
        this.mMenu = menu;
        return true;
    }
    @Override
    protected void onDestroy() {
        swipeRefreshLayout.setRefreshing(false);
        super.onDestroy();
        mActivityName="";

        closeLoader();

    }

    @Override
    public void deviceDiscoveryAfterClearingTheCacheStarted() {

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        this.mMenu = menu;
        updateMenuDetails();
        return super.onPrepareOptionsMenu(menu);
    }
    public void StartLSSDPScanAfterRelease(){

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
    public void UpdateLSSDPNodeList(String ipaddress,String mDeviceState){
        Log.e("ScanningHandler","Updating LSSDP Device State "+mDeviceState);

        LSSDPNodes mToBeUpdateNode = mScanHandler.getLSSDPNodeFromCentralDB(ipaddress);
        LSSDPNodes mMasterNode = mScanHandler.getLSSDPNodeFromCentralDB(mMasterIP);
        LSSDPNodeDB mNodeDB = LSSDPNodeDB.getInstance();

        if(mMasterNode!=null && mToBeUpdateNode !=null) {
            LUCIControl luciControl = new LUCIControl(mToBeUpdateNode.getIP());
            if (mDeviceState.equals("Master")) {
                mToBeUpdateNode.setDeviceState("M");
            } else if (mDeviceState.equals("Slave")) {
                mToBeUpdateNode.setDeviceState("S");
                if (mScanHandler.getconnectedSSIDname(getApplicationContext()) == mScanHandler.HN_MODE) {
                    mToBeUpdateNode.setcSSID(mMasterNode.getcSSID());
                } else {
                    mToBeUpdateNode.setZoneID(mMasterNode.getZoneID());
                }
            } else if (mDeviceState.equals("Free")) {
                if(mToBeUpdateNode.getDeviceState().equals("M")){
                    ArrayList<LSSDPNodes> mSlaveList = mScanHandler.getSlaveListForMasterIp(mToBeUpdateNode.getIP(),
                            mScanHandler.getconnectedSSIDname(getApplicationContext()));
                    for(LSSDPNodes mSlaveNode:mSlaveList){
                        mSlaveNode.setDeviceState("F");
                        mNodeDB.renewLSSDPNodeDataWithNewNode(mSlaveNode);
                    }
                }
                mToBeUpdateNode.setDeviceState("F");

                /**/
            /*    if (mScanHandler.getConnectedSSIDName(getApplicationContext()) == mScanHandler.HN_MODE) {
                    mToBeUpdateNode.setcSSID("");
                } else {
                    mToBeUpdateNode.setZoneID("");
                }*/
            }
            mNodeDB.renewLSSDPNodeDataWithNewNode(mToBeUpdateNode);


            mManageDeviceAdapter.notifyDataSetChanged();
            updateMenuDetails();
        }

    }

    public void clearMenuDetails(){

        try {
            MenuItem mMenuItem = this.mMenu.findItem(R.id.numberOfMaster);
            mMenuItem.setTitle("M: " + 0);

            mMenuItem = this.mMenu.findItem(R.id.numberOfSlave);
            mMenuItem.setTitle("S: " + 0);

            mMenuItem = this.mMenu.findItem(R.id.numberOfFree);
            ;
            mMenuItem.setTitle("F: " + 0);
        }catch(Exception e){

        }
    }

    public void updateMenuDetails(){

        try {
            MenuItem mMenuItem = this.mMenu.findItem(R.id.numberOfMaster);
            int mNumberOfDevices = mScanHandler.getNumberOfSlavesForMasterIp(mMasterIP,
                    mScanHandler.getconnectedSSIDname(getApplicationContext()));
            if (mNumberOfDevices > 0)
                mMenuItem.setTitle("M: " + 1);
            else
                mMenuItem.setTitle("M: " + 0);

            mMenuItem = this.mMenu.findItem(R.id.numberOfSlave);
            mMenuItem.setTitle("S: " + (mNumberOfDevices - 1));

            mMenuItem = this.mMenu.findItem(R.id.numberOfFree);
            ;
            mMenuItem.setTitle("F: " + mScanHandler.getFreeDeviceList().size());
        }catch(Exception e){

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerForDeviceEvents(this);

        LibreApplication application=(LibreApplication)getApplication();
        //application.getScanThread().clearNodes();
        application.getScanThread().UpdateNodes();

        updateFreeAndSlaveDevicesForMasterDevices(mMasterIP);
        //updateMenuDetails();
        mManageDeviceAdapter.notifyDataSetChanged();
    }

    public void updateFreeAndSlaveDevicesForMasterDevices(String mMasterIP){


        SceneObject mSceneObject = mScanHandler.getSceneObjectFromCentralRepo(mMasterIP);
        /* Sending Luci Commands For the Master */
        LUCIControl masterluciControl = new LUCIControl(mMasterIP);
        masterluciControl.sendAsynchronousCommand();
        masterluciControl.SendCommand(41, LUCIMESSAGES.GET_PLAY, 2);
        masterluciControl.SendCommand(64, null, 1);


        if(mSceneObject!=null) {
            mManageDeviceAdapter.add(mSceneObject);
        }else{
            LSSDPNodes mMasterNode = mScanHandler.getLSSDPNodeFromCentralDB(mMasterIP);
            if(mMasterNode != null) {
                SceneObject sceneObjec = new SceneObject(" ", mMasterNode.getFriendlyname(), 0, mMasterNode.getIP());
                LibreLogger.d(this,"Ipaddress of Master Node" + mMasterNode.getIP());

                mScanHandler.putSceneObjectToCentralRepo(mMasterIP, sceneObjec);
                mManageDeviceAdapter.add(sceneObjec);
            }
        }

        ArrayList<LSSDPNodes> mSlaveLSSDPNode = mScanHandler.getSlaveListForMasterIp(mMasterIP,
                mScanHandler.getconnectedSSIDname(getApplicationContext()));
        ArrayList<LSSDPNodes> mFreeDeviceList = mScanHandler.getFreeDeviceList();
        //if(mActivityName.contains("NowPlayingActivity")) {
            for (LSSDPNodes mSlaveNode : mSlaveLSSDPNode) {
                LUCIControl luciControl = new LUCIControl(mSlaveNode.getIP());
                luciControl.sendAsynchronousCommand();
                luciControl.SendCommand(41, LUCIMESSAGES.GET_PLAY, 2);
                //luciControl.SendCommand(64, null, 1);
                {
                    //  swipeRefreshLayout.setRefreshing(false);
                    SceneObject sceneObjec = new SceneObject(" ", mSlaveNode.getFriendlyname(), 0, mSlaveNode.getIP());
                    mManageDeviceAdapter.add(sceneObjec);
                    mManageDeviceAdapter.notifyDataSetChanged();
                }
            }
        //}
        for(LSSDPNodes mFreeDeviceNode : mFreeDeviceList){
            LUCIControl luciControl = new LUCIControl(mFreeDeviceNode.getIP());
            luciControl.sendAsynchronousCommand();
            luciControl.SendCommand(41, LUCIMESSAGES.GET_PLAY, 2);
            //luciControl.SendCommand(64, null, 1);
            {
             //   swipeRefreshLayout.setRefreshing(false);
                SceneObject sceneObjec = new SceneObject(" ", mFreeDeviceNode.getFriendlyname(), 0, mFreeDeviceNode.getIP());
                mManageDeviceAdapter.add(sceneObjec);
                mManageDeviceAdapter.notifyDataSetChanged();
            }
        }
        mManageDeviceAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unRegisterForDeviceEvents();
    }

    @Override
    public void newDeviceFound(LSSDPNodes node) {
        LibreLogger.d(this, "New device is found with the ip address = " + node.getIP());
        LibreLogger.d(this, "New device is found with the Selected Ipaddressx  = " + mMasterIP);
        int mMode = mScanHandler.getconnectedSSIDname(getApplicationContext());
        LSSDPNodes mMasternode  = mScanHandler.getLSSDPNodeFromCentralDB(mMasterIP);
        if(node!=null && mMasternode!=null){
            if (node.getDeviceState().contains("F") ||
                    (node.getDeviceState().contains("S") && (mMasternode!=null) &&
                            ( ((mMode == mScanHandler.HN_MODE) && (mMasternode!=null) && mMasternode.getcSSID().equals(node.getcSSID())) ||
                            ((mMode == mScanHandler.SA_MODE) && (mMasternode!=null) && mMasternode.getZoneID().equals(node.getZoneID())) )
                            || node.getIP().equals(mMasterIP))) {
                /*if(node.getDeviceState().contains("S")){
                    mScanHandler.addSlaveToMasterHashMap(node.getIP());
                }*/
                LUCIControl luciControl = new LUCIControl(node.getIP());
                luciControl.SendCommand(41, LUCIMESSAGES.GET_PLAY, 2);
                luciControl.SendCommand(64, null, 1);
                {
                //    swipeRefreshLayout.setRefreshing(false);
                    SceneObject sceneObjec = new SceneObject(" ", node.getFriendlyname(), 0, node.getIP());
                   // if(node.getIP().equals(mMasterIP)){


                    if(!mScanHandler.isIpAvailableInCentralSceneRepo(mMasterIP)){
                        mScanHandler.putSceneObjectToCentralRepo(mMasterIP,sceneObjec);
                    }
                    mManageDeviceAdapter.add(sceneObjec);
                    mManageDeviceAdapter.notifyDataSetChanged();
                    updateMenuDetails();
                    if(node.getDeviceState().equals("M")) { /* this is to Ensure Slaves Comes in UI after Getting Master*/
                        updateFreeAndSlaveDevicesForMasterDevices(mMasterIP);
                    }
                }
            }
        }

    }

    @Override
    public void deviceGotRemoved(String mIpAddress) {
        SceneObject scneObj = mManageDeviceAdapter.getSceneObjectFromAdapter(mIpAddress);

        mManageDeviceAdapter.remove(scneObj);
        updateMenuDetails();
		  mManageDeviceAdapter.notifyDataSetChanged();
        if(mScanHandler.removeSceneMapFromCentralRepo(mIpAddress)){

        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(!mActivityName.contains("NowPlayingActivity")) {
            Intent intent = new Intent(ManageDevices.this, ActiveScenesListActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }else{

            Intent intent = new Intent(this, NowPlayingActivity.class);
            intent.putExtra(Constants.CURRENT_DEVICE_IP,mMasterIP);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.FreeAll: {
                Message msg1 = new Message();
                Bundle b1 = new Bundle();
                msg1.what = DeviceMasterSlaveFreeConstants.LUCI_SEND_SLAVE_COMMAND;

                ArrayList<LSSDPNodes> mSlaveListForMyMaster1 = mScanHandler.getSlaveListForMasterIp(mMasterIP,
                        mScanHandler.getconnectedSSIDname(getApplicationContext()));


                for (LSSDPNodes mSlaveNode : mSlaveListForMyMaster1) {
                    LibreLogger.d("this", "Release Scene Slave Ip List" + mSlaveNode.getFriendlyname());
                    LUCIControl luciControl = new LUCIControl(mSlaveNode.getIP());

                        /* Sending Asynchronous Registration*/
                    luciControl.sendAsynchronousCommand();

                    luciControl.SendCommand(MIDCONST.MID_DDMS, LUCIMESSAGES.SETFREE, LSSDPCONST.LUCI_SET);
                    /*try {
                        Thread.sleep(200);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }*/
                    msg1.what = DeviceMasterSlaveFreeConstants.LUCI_SEND_FREE_COMMAND;

                    mManageDevicesHandler.sendEmptyMessage(msg1.what);

                }
                StartLSSDPScanAfterRelease();
                updateMenuDetails();

                return true;
            }

        /*    case R.id.JoinAll: {
                ArrayList<LSSDPNodes> mFreeSpeakerList = mScanHandler.getFreeDeviceList();
                final LSSDPNodes mMasterNode = mScanHandler.getLSSDPNodeFromCentralDB(mMasterIP);
                    if(mMasterNode==null)
                    {
                        Toast.makeText(ManageDevices.this,"Master is not available" + " Please Refresh ..",Toast.LENGTH_SHORT).show();
                        return true;
                    }
                for (LSSDPNodes mFreeNode : mFreeSpeakerList) {
                    Message msg = new Message();
                    Bundle b = new Bundle();
                    msg.what = DeviceMasterSlaveFreeConstants.LUCI_SEND_SLAVE_COMMAND;
                    LibreLogger.d("this", "Adding slave To Master" + mFreeNode.getFriendlyname());
                    LUCIControl luciControl = new LUCIControl(mFreeNode.getIP());
                        *//* Sending Asynchronous Registration*//*
                    luciControl.sendAsynchronousCommand();

                    String mCSSID = mMasterNode.getcSSID();
                    String mZoneID = mMasterNode.getZoneID();

                    if (mCSSID != null) {
                        luciControl.SendCommand(MIDCONST.MID_SSID, mCSSID, LSSDPCONST.LUCI_SET);
                        LibreLogger.d(this, "Slave Concurrent SSID" + mCSSID);

                    }
                    luciControl.SendCommand(MIDCONST.MID_DDMS_ZONE_ID, mZoneID, LSSDPCONST.LUCI_SET);
                    LibreLogger.d(this, "Slave ZoneID" + mZoneID);
                    luciControl.SendCommand(MIDCONST.MID_DDMS, LUCIMESSAGES.SETSLAVE, LSSDPCONST.LUCI_SET);
                    *//*try {
                        Thread.sleep(200);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }*//*
                    b.putString("ipAddress", mFreeNode.getIP());

                    msg.setData(b);
                    mManageDevicesHandler.sendMessage(msg);

                }
                StartLSSDPScanAfterRelease();
                updateMenuDetails();
            }
                return true;*/
            /*case R.id.ReleaseScene:

                final LSSDPNodes mMasternode = mScanHandler.getLSSDPNodeFromCentralDB(mMasterIP);
                if(mMasternode==null)
                {
                    Toast.makeText(ManageDevices.this,"Master is not available " + "  Please Refresh ..",Toast.LENGTH_SHORT).show();
                    return true;
                }
                ArrayList<LSSDPNodes> mSlaveListForMyMaster = mScanHandler.getSlaveListForMasterIp(mMasterIP,
                        mScanHandler.getConnectedSSIDName(getApplicationContext()));

                for (LSSDPNodes mSlaveNode : mSlaveListForMyMaster) {
                    LibreLogger.d("this", "Release Scene Slave Ip List" + mSlaveNode.getFriendlyname());
                    LUCIControl luciControl = new LUCIControl(mSlaveNode.getIP());
                        *//* Sending Asynchronous Registration*//*
                    luciControl.sendAsynchronousCommand();
                    luciControl.SendCommand(MIDCONST.MID_DDMS, LUCIMESSAGES.SETFREE, LSSDPCONST.LUCI_SET);

                }

                LUCIControl mMluciControl = new LUCIControl(mMasternode.getIP());
                mMluciControl.sendAsynchronousCommand();
                mMluciControl.SendCommand(MIDCONST.MID_DDMS, LUCIMESSAGES.SETFREE, LSSDPCONST.LUCI_SET);
                LibreLogger.d("this", "Release Scene Master Name" + mMasternode.getFriendlyname());

                if (mScanHandler.removeSceneMapFromCentralRepo(mMasterIP)) {
                    LibreLogger.d("this", "Release Scene Removed From Central Repo " + mMasterIP);
                }
                Message msg1 = new Message();
                Bundle b1 = new Bundle();

                msg1.what = DeviceMasterSlaveFreeConstants.LUCI_SEND_FREE_COMMAND;
                b1.putString("ipAddress", mMasterIP);
                msg1.setData(b1);
                mManageDevicesHandler.sendMessage(msg1);
                StartLSSDPScanAfterRelease();
                if (!mActivityName.equals("NowPlayingActivity")) {
                    Intent intent = new Intent(ManageDevices.this, CreateNewScene.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
                   finish();
                //Checking For OtherM
                return true;*/

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public void messageRecieved(NettyData dataRecived) {
        LibreLogger.d(this, "New message appeared for the device " + dataRecived.getRemotedeviceIp());



        SceneObject sceneObject= mManageDeviceAdapter.getSceneObjectFromAdapter(dataRecived.getRemotedeviceIp());




            LUCIPacket packet = new LUCIPacket(dataRecived.getMessage());
            LibreLogger.d(this, "ReceivedCommand Status " + packet.getCommandStatus() + "For the command " + packet.getCommand());
            switch (packet.getCommand()) {
                case 64: {

                    String message = new String(packet.getpayload());
                    if (sceneObject!=null) {
                        try {
                            int duration = Integer.parseInt(message);
                            sceneObject.setVolumeValueInPercentage(duration);
                            LibreLogger.d(this, "Recieved the current volume to be" + sceneObject.getVolumeValueInPercentage());
                            SceneObject sceneObjectFromCentralRepo = mScanHandler.getSceneObjectFromCentralRepo(dataRecived.getRemotedeviceIp());

                            if(sceneObjectFromCentralRepo!=null){
                                sceneObjectFromCentralRepo.setVolumeValueInPercentage(duration);
                                mScanHandler.putSceneObjectToCentralRepo(dataRecived.getRemotedeviceIp(), sceneObjectFromCentralRepo);
                            }

                            mManageDeviceAdapter.add(sceneObject);
                            mManageDeviceAdapter.notifyDataSetChanged();

                        } catch (Exception e) {

                        }
                    }
                }
                break;
                case 103:
                    LibreLogger.d(this, "Command 103 " + new String(packet.getpayload()));

                    Message successMsg = new Message();
                    String message = new String(packet.getpayload());
                    Bundle b = new Bundle();
                    b.putString("ipAddress", dataRecived.getRemotedeviceIp());
                    successMsg.setData(b);

                    successMsg.obj = mScanHandler.getLSSDPNodeFromCentralDB(dataRecived.getRemotedeviceIp());

                    if(message.contains("FREE")) {
                        successMsg.what = DeviceMasterSlaveFreeConstants.LUCI_SUCCESS_FREE_COMMAND;
                        mManageDevicesHandler.sendMessage(successMsg);
                    }else if(message.contains("SLAVE")) {
                        LSSDPNodes mMasterNode = mScanHandler.getLSSDPNodeFromCentralDB(mMasterIP);
                        LSSDPNodes mSlaveNode = mScanHandler.getLSSDPNodeFromCentralDB(dataRecived.getRemotedeviceIp());
                        if(mMasterNode!=null && mSlaveNode!=null){
                            if((mManageDeviceAdapter.mWifiMode ==mScanHandler.HN_MODE)) {//&& (mMasterNode.getcSSID().equalsIgnoreCase(mSlaveNode.getcSSID()))){
                                successMsg.what = DeviceMasterSlaveFreeConstants.LUCI_SUCCESS_SLAVE_COMMAND;
                                mManageDevicesHandler.sendMessage(successMsg);
                            }else if((mManageDeviceAdapter.mWifiMode ==mScanHandler.SA_MODE)  && (mMasterNode.getZoneID().equalsIgnoreCase(mSlaveNode.getZoneID()))){
                                successMsg.what = DeviceMasterSlaveFreeConstants.LUCI_SUCCESS_SLAVE_COMMAND;
                                mManageDevicesHandler.sendMessage(successMsg);
                            }else  if((mManageDeviceAdapter.mWifiMode ==mScanHandler.SA_MODE) && (mSlaveNode.getZoneID().contains("3000")) &&
                                    (mSlaveNode.getDeviceState().equalsIgnoreCase("F"))){
                                /* this if loop is added to make sure that device side issue of new slave coming up with the 3000 as its zoneid in SA mode*/
                                successMsg.what = DeviceMasterSlaveFreeConstants.LUCI_SUCCESS_SLAVE_COMMAND;
                                mManageDevicesHandler.sendMessage(successMsg);
                            }
                            else{/*M-search is called here as we seen to having old information of a node in our DB*/
                                LibreLogger.d(this,mMasterNode.getZoneID()+"::mSlaveNode Zoneid ::" + mSlaveNode.getZoneID());
                                mManageDeviceAdapter.remove(mManageDeviceAdapter.getSceneObjectFromAdapter(mSlaveNode.getIP()));
                                mManageDeviceAdapter.notifyDataSetChanged();
                                StartLSSDPScanAfterRelease();
                                updateMenuDetails();
                            }
                        }
                    }else if(message.contains("MASTER")) { /* Commenting Below code for showing Two Masters in ManageDevcies*/
                     //   successMsg.what = DeviceMasterSlaveFreeConstants.LUCI_SUCCESS_MASTER_COMMAND;
                        //mManageDevicesHandler.sendMessage(successMsg);
                        mManageDeviceAdapter.remove(mManageDeviceAdapter.getSceneObjectFromAdapter(dataRecived.getRemotedeviceIp()));
                        mManageDeviceAdapter.notifyDataSetChanged();
                        StartLSSDPScanAfterRelease();
                    }

                    updateMenuDetails();
                    //closeLoader();
                    break;
              /*  case 105:
                {
                    String msg = new String(packet.getpayload());
                    LSSDPNodes mToBeUpdateNode = mScanHandler.getLSSDPNodeFromCentralDB(dataRecived.getRemotedeviceIp());
                    mToBeUpdateNode.setcSSID(msg);
                    LSSDPNodeDB mNodeDB = LSSDPNodeDB.getInstance();
                    mNodeDB.renewLSSDPNodeDataWithNewNode(mToBeUpdateNode);
                    mManageDeviceAdapter.notifyDataSetChanged();

                }
                break;
                case 104:
                {
                    String msg = new String(packet.getpayload());
                    LSSDPNodes mToBeUpdateNode = mScanHandler.getLSSDPNodeFromCentralDB(dataRecived.getRemotedeviceIp());
                    mToBeUpdateNode.setZoneID(msg);
                    LSSDPNodeDB mNodeDB = LSSDPNodeDB.getInstance();
                    mNodeDB.renewLSSDPNodeDataWithNewNode(mToBeUpdateNode);
                    mManageDeviceAdapter.notifyDataSetChanged();

                }
                    break;*/
                case 0:
                    LUCIControl mLuci  = new LUCIControl(dataRecived.getRemotedeviceIp());
                    mLuci.sendAsynchronousCommand();
                    break;
            }
    }
    Handler mManageDevicesHandler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Message failedMsg = new Message();
            failedMsg.what = DeviceMasterSlaveFreeConstants.LUCI_TIMEOUT_COMMAND;
            switch(msg.what){
                case DeviceMasterSlaveFreeConstants.LUCI_SEND_MASTER_COMMAND:{
                    mManageDevicesHandler.sendEmptyMessageDelayed(failedMsg.what,20000);
                    showLoader("Master");
                }break;
                case DeviceMasterSlaveFreeConstants.LUCI_SEND_FREE_COMMAND:{
                    mManageDevicesHandler.sendEmptyMessageDelayed(failedMsg.what,20000);
                    showLoader("FREE");
                }break;
                case DeviceMasterSlaveFreeConstants.LUCI_SEND_SLAVE_COMMAND:{
                    mManageDevicesHandler.sendEmptyMessageDelayed(failedMsg.what,20000);
                    showLoader("SLAVE");
                }break;
                case DeviceMasterSlaveFreeConstants.LUCI_SUCCESS_MASTER_COMMAND:{
                    mManageDevicesHandler.removeMessages(failedMsg.what);
                    closeLoader();
                    Bundle b  = new Bundle();
                    b = msg.getData();
                    String ipaddress = b.getString("ipAddress");
                    UpdateLSSDPNodeList(ipaddress,"Master");
                  //  mScanHandler.updateSlaveToMasterHashMap();
                    mManageDeviceAdapter.notifyDataSetChanged();
                }break;
                case DeviceMasterSlaveFreeConstants.LUCI_SUCCESS_SLAVE_COMMAND:{
                    mManageDevicesHandler.removeMessages(failedMsg.what);
                    closeLoader();
                    Bundle b  = new Bundle();
                    b = msg.getData();
                    String ipaddress = b.getString("ipAddress");
                    UpdateLSSDPNodeList(ipaddress,"Slave");

                    mManageDeviceAdapter.notifyDataSetChanged();
                }break;
                case DeviceMasterSlaveFreeConstants.LUCI_SUCCESS_FREE_COMMAND:{
                    mManageDevicesHandler.removeMessages(failedMsg.what);
                    Bundle b  = new Bundle();
                    b = msg.getData();
                    String ipaddress = b.getString("ipAddress");
                    closeLoader();

                    LSSDPNodes node = mScanHandler.getLSSDPNodeFromCentralDB(ipaddress);

                    if ( (node != null) && node.getDeviceState().equals("M") )
                    {
                        if(mScanHandler.isIpAvailableInCentralSceneRepo(ipaddress)){
                            mScanHandler.removeSceneMapFromCentralRepo(ipaddress);
                        }
                        if(mActivityName.contains("CreateNewScene")) {
                            if(node != null)
                                UpdateLSSDPNodeList(node.getIP(), "Free");


                            Intent intent = new Intent(ManageDevices.this, CreateNewScene.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            for(LSSDPNodes mSlaveNode : mScanHandler.getSlaveListForMasterIp(node.getIP(),
                                    mManageDeviceAdapter.mWifiMode)){
                                UpdateLSSDPNodeList(mSlaveNode.getIP(), "Free");
                            }
                            startActivity(intent);
                            finish();
                        }else if(mActivityName.contains("NowPlayingActivity")){
                            LSSDPNodeDB mNodeDB = LSSDPNodeDB.getInstance();
                            boolean mMasterFound = false;
                            if(node != null)
                                UpdateLSSDPNodeList(node.getIP(), "Free");
                                ArrayList<LSSDPNodes> clonedNodeDB= mNodeDB.GetDB();
                            for (LSSDPNodes mNode : clonedNodeDB){
                                if(mNode.getDeviceState().contains("M") && !mNode.getIP().contains(ipaddress)){
                                   // Toast.makeText(getApplicationContext(), "Master Found :" + mNode.getFriendlyname(), Toast.LENGTH_SHORT).show();
                                    mMasterFound = true;
                                }
                                if(mMasterFound){
                                    break;
                                }
                            }
                            if(!mMasterFound) {
                                Intent intent = new Intent(ManageDevices.this, PlayNewActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);

                                for(LSSDPNodes mSlaveNode : mScanHandler.getSlaveListForMasterIp(node.getIP(),
                                        mManageDeviceAdapter.mWifiMode)){
                                    UpdateLSSDPNodeList(mSlaveNode.getIP(), "Free");
                                }
                         //       StartLSSDPScan();
                                finish();
                            }else{
                                Intent intent = new Intent(ManageDevices.this, ActiveScenesListActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                for(LSSDPNodes mSlaveNode : mScanHandler.getSlaveListForMasterIp(node.getIP(),
                                        mManageDeviceAdapter.mWifiMode)){
                                    UpdateLSSDPNodeList(mSlaveNode.getIP(), "Free");
                                }
                            //    StartLSSDPScan();
                                finish();

                            }
                        }
                    }else{
                        if(node != null)
                            UpdateLSSDPNodeList(node.getIP(), "Free");
                   }

                    mManageDeviceAdapter.notifyDataSetChanged();
                }break;
                case DeviceMasterSlaveFreeConstants.LUCI_TIMEOUT_COMMAND:{
                    mManageDevicesHandler.removeMessages(failedMsg.what);
                    closeLoader();
                    updateMenuDetails();
                    if (!(ManageDevices.this.isFinishing())) {
                        new AlertDialog.Builder(ManageDevices.this)
                                .setTitle("Device State Changing")
                                .setMessage("FAILED")
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                    }
                                }).setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                    }
                }break;
            }

        }
    };
    public  ProgressDialog mProgressDialog;
    public  void closeLoader() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            Log.e("LuciControl", "progress Dialog Closed");
            mProgressDialog.setCancelable(true);
            mProgressDialog.dismiss();
            mProgressDialog.cancel();
            mManageDeviceAdapter.notifyDataSetChanged();
        }

    }

    public  void showLoader(String msg) {
        if (mProgressDialog == null) {
            Log.e("LUCIControl","Null");
            mProgressDialog = ProgressDialog.show(ManageDevices.this, getResources().getString(R.string.notice), "Changing " +  msg +"...", true, true, null);
        }
        mProgressDialog.setCancelable(false);

        if (!mProgressDialog.isShowing()) {
            if (!(ManageDevices.this.isFinishing())){
                mProgressDialog = ProgressDialog.show(ManageDevices.this, getResources().getString(R.string.notice), "Changing " + msg + "...", true, true, null);
            }
        }
    }
}
