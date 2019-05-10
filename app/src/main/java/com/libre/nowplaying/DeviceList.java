package com.libre.nowplaying;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.os.Bundle;
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
import com.libre.ManageDevice.ManageDeviceHandler;
import com.libre.R;
import com.libre.Scanning.ScanningHandler;
import com.libre.SceneObject;
import com.libre.constants.DeviceMasterSlaveFreeConstants;
import com.libre.constants.LUCIMESSAGES;
import com.libre.luci.LSSDPNodeDB;
import com.libre.luci.LSSDPNodes;
import com.libre.luci.LUCIControl;
import com.libre.luci.LUCIPacket;
import com.libre.netty.LibreDeviceInteractionListner;
import com.libre.netty.NettyData;
import com.libre.util.LibreLogger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;

public class DeviceList extends CTDeviceDiscoveryActivity implements LibreDeviceInteractionListner {
    public String concurrentSSID;
    public String mMasterIP="";
    public ArrayList<LSSDPNodes> mSlaveList = new ArrayList<>();
    LSSDPNodeDB mLSSDPNodeDB = LSSDPNodeDB.getInstance();
    DeviceListAdapter mDeviceAdapter;
    ManageDeviceHandler mManageDeviceHandler = ManageDeviceHandler.getInstance();
    int save=-1;
    SwipeRefreshLayout swipeRefreshLayout;
    ScanningHandler mScanHandler = ScanningHandler.getInstance();
    Button btnSave;
    ListView listView;
    public LinkedHashMap<String,ArrayList<SceneObject>>  masterSceneObject =
            new LinkedHashMap<String ,ArrayList<SceneObject>>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        Intent myIntent = getIntent(); // gets the previously created intent
        mMasterIP = myIntent.getStringExtra("master_ip");
        mManageDeviceHandler.selectedIpaddress = mMasterIP;

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        registerForDeviceEvents(this);

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh);
        LibreApplication application=(LibreApplication)getApplication();
        application.getScanThread().clearNodes();
        application.getScanThread().UpdateNodes();

        listView = (ListView) findViewById(R.id.listView);
        btnSave = (Button) findViewById(R.id.btnSave);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent mActiveScenesList = new Intent(DeviceList.this, ActiveScenesListActivity.class);
                startActivity(mActiveScenesList);
            }
        });
        concurrentSSID = getConcurrentSSID(mMasterIP);

        mDeviceAdapter = new DeviceListAdapter(this,R.layout.manage_devices_list_item,mMasterIP,mDeviceListHandler);
        //updateMasterSceneObject(concurrentSSID);
        listView.setAdapter(mDeviceAdapter);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeRefreshLayout.setRefreshing(false);
                mDeviceAdapter.clear();
                DeviceListAdapter.sceneMap.clear();

                mDeviceAdapter.notifyDataSetChanged();
                LibreApplication application=(LibreApplication)getApplication();
                application.getScanThread().clearNodes();
                application.getScanThread().UpdateNodes();
            }
        });


    }
    public void updateMasterSceneObject(String mConcurrentSSID){
        ArrayList<SceneObject> mSlaveDevices = new ArrayList<>();
        ArrayList<LSSDPNodes> mlssdpNodeList =  mLSSDPNodeDB.GetDB();
        Iterator node = mlssdpNodeList.iterator();
        Log.e("DevicesList","MaiNID" + mConcurrentSSID);
        while(node.hasNext()){
            LSSDPNodes mNode = (LSSDPNodes) node.next();
            if(mNode.getcSSID().equals(mConcurrentSSID)){
                Log.e("DeviceList",mNode.getFriendlyname());
                Log.e("DeviceList",mNode.getIP());
                SceneObject obj = new SceneObject(" ",mNode.getFriendlyname(),0,mNode.getIP());
                mDeviceAdapter.add(obj);
                mDeviceAdapter.notifyDataSetChanged();
            }else if(mNode.getDeviceState().equals("F")){
                SceneObject obj = new SceneObject(" ",mNode.getFriendlyname(),0,mNode.getIP());
                mDeviceAdapter.add(obj);
                mDeviceAdapter.notifyDataSetChanged();
            }
        }
        if(mSlaveDevices.size()>0)
            masterSceneObject.put(mMasterIP, mSlaveDevices);
        else {

            masterSceneObject.put(mMasterIP,mSlaveDevices);
        }

    }
    public String getConcurrentSSID(String mMasterIp){

        ArrayList<LSSDPNodes> mlssdpNodeList =  mLSSDPNodeDB.GetDB();
        Iterator node = mlssdpNodeList.iterator();
        while(node.hasNext()){
            LSSDPNodes mNode = (LSSDPNodes) node.next();
            if(mNode.getIP().equals(mMasterIp)){
                return mNode.getcSSID();
            }
        }
        return "";
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_device_list, menu);
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
    public void deviceDiscoveryAfterClearingTheCacheStarted() {

    }

    @Override
    public void newDeviceFound(LSSDPNodes node) {
        LibreLogger.d(this, "New device is found with the ip address = " + node.getIP());
        LibreLogger.d(this, "New device is found with the Selected Ipaddressx  = " + mMasterIP);
        //if(  ! (node.getDeviceState().contains("S") || (node.getDeviceState().contains("M") )&&(node.getIP().equals(mDeviceHandler.selectedIpaddress)) ))
        LSSDPNodes mMasternode  =mScanHandler.getLSSDPNodeFromCentralDB(mMasterIP);

        if(node.getDeviceState().contains("F") ||
                (node.getDeviceState().contains("S") && mMasternode.getcSSID().equals(node.getcSSID())
                || node.getIP().equals(mMasterIP)))
      //  if(node.getDeviceState().contains("F") || node.getIP().equals(mMasterIP))
        {
            LUCIControl luciControl = new LUCIControl(node.getIP());
            luciControl.SendCommand(41, LUCIMESSAGES.GET_PLAY, 2);
            luciControl.SendCommand(64, null, 1);
            {
                swipeRefreshLayout.setRefreshing(false);
                SceneObject sceneObjec = new SceneObject(" ", node.getFriendlyname(), 0, node.getIP());
                mDeviceAdapter.add(sceneObjec);
                mDeviceAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void deviceGotRemoved(String mIpAddress) {

    }

    @Override
    public void messageRecieved(NettyData dataRecived) {
        LibreLogger.d(this, "New message appeared for the device " + dataRecived.getRemotedeviceIp());
        SceneObject sceneObject= mDeviceAdapter.getSceneObjectFromAdapter(dataRecived.getRemotedeviceIp());

        if (sceneObject!=null) {

            LUCIPacket packet = new LUCIPacket(dataRecived.getMessage());
            switch (packet.getCommand()) {


                case 64: {

                    String message = new String(packet.getpayload());
                    try {
                        int duration = Integer.parseInt(message);
                        sceneObject.setVolumeValueInPercentage(duration);
                        LibreLogger.d(this, "Recieved the current volume to be" + sceneObject.getVolumeValueInPercentage());
                        mDeviceAdapter.add(sceneObject);
                        mDeviceAdapter.notifyDataSetChanged();

                    } catch (Exception e) {

                    }
                }
                break;
                case 103:
                    Message successMsg = new Message();
                    successMsg.what = DeviceMasterSlaveFreeConstants.LUCI_SUCCESS_SLAVE_COMMAND;
                    mDeviceListHandler.sendEmptyMessage(successMsg.what);
                    break;

            }
        }
    }
    Handler mDeviceListHandler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Message failedMsg = new Message();
            failedMsg.what = DeviceMasterSlaveFreeConstants.LUCI_TIMEOUT_COMMAND;
            switch(msg.what){
                case DeviceMasterSlaveFreeConstants.LUCI_SEND_MASTER_COMMAND:{
                    mDeviceListHandler.sendEmptyMessageDelayed(failedMsg.what,20000);
                    showLoader("Master");
                }break;
                case DeviceMasterSlaveFreeConstants.LUCI_SEND_FREE_COMMAND:{
                    mDeviceListHandler.sendEmptyMessageDelayed(failedMsg.what,20000);
                    showLoader("FREE");
                }break;
                case DeviceMasterSlaveFreeConstants.LUCI_SEND_SLAVE_COMMAND:{
                    mDeviceListHandler.sendEmptyMessageDelayed(failedMsg.what,20000);
                    showLoader("SLAVE");
                }break;
                case DeviceMasterSlaveFreeConstants.LUCI_SUCCESS_MASTER_COMMAND:{
                    mDeviceListHandler.removeMessages(failedMsg.what);
                    closeLoader();
                }break;
                case DeviceMasterSlaveFreeConstants.LUCI_SUCCESS_SLAVE_COMMAND:{
                    mDeviceListHandler.removeMessages(failedMsg.what);
                    closeLoader();
                }break;
                case DeviceMasterSlaveFreeConstants.LUCI_SUCCESS_FREE_COMMAND:{
                    mDeviceListHandler.removeMessages(failedMsg.what);
                    closeLoader();
                }break;
                case DeviceMasterSlaveFreeConstants.LUCI_TIMEOUT_COMMAND:{
                    mDeviceListHandler.removeMessages(failedMsg.what);
                    closeLoader();
                    if (!(DeviceList.this.isFinishing())) {
                        new AlertDialog.Builder(DeviceList.this)
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
//            mProgressDialog.dismiss();
            mProgressDialog.cancel();
            mDeviceAdapter.notifyDataSetChanged();
        }

    }

    public  void showLoader(String msg) {
        if (mProgressDialog == null) {
            Log.e("LUCIControl","Null");
            mProgressDialog = ProgressDialog.show(DeviceList.this, getString(R.string.notice), getString(R.string.changing) +  msg +"...", true, true, null);
        }
         mProgressDialog.setCancelable(false);

        if (!mProgressDialog.isShowing()) {
            if (!(DeviceList.this.isFinishing())){
                mProgressDialog = ProgressDialog.show(DeviceList.this, getString(R.string.notice), getString(R.string.changing) + msg + "...", true, true, null);
            }
        }
    }

}
