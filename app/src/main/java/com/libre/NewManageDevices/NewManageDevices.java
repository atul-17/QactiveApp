/*
 * ********************************************************************************************
 *  *
 *  * Copyright (C) 2014 Libre Wireless Technology
 *  *
 *  * "Libre Sync" Project
 *  *
 *  * Libre Sync Android App
 *  * Author: Android App Team
 *  *
 *  **********************************************************************************************
 */

package com.libre.NewManageDevices;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.libre.ActiveScenesListActivity;
import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity;
import com.libre.LErrorHandeling.LibreError;
import com.libre.LibreApplication;
import com.libre.ManageDevice.CreateNewScene;
import com.libre.ManageDevice.ManageDeviceHandler;
import com.libre.PlayNewActivity;
import com.libre.R;
import com.libre.Scanning.Constants;
import com.libre.Scanning.ScanningHandler;
import com.libre.SceneObject;
import com.libre.SourcesOptionActivity;
import com.libre.app.dlna.dmc.server.ContentTree;
import com.libre.app.dlna.dmc.utility.PlaybackHelper;
import com.libre.app.dlna.dmc.utility.UpnpDeviceManager;
import com.libre.constants.CommandType;
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

import org.fourthline.cling.model.meta.RemoteDevice;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;

import static com.libre.Scanning.ScanningHandler.HN_MODE;
import static com.libre.Scanning.ScanningHandler.SA_MODE;

/**
 * Created by karunakaran on 8/4/2015.
 */
public class NewManageDevices extends CTDeviceDiscoveryActivity implements LibreDeviceInteractionListner {
    ListView listView;
    NewManageAdapter mManageDeviceAdapter;
    ManageDeviceHandler mDeviceHandler = ManageDeviceHandler.getInstance();
    int save = -1;
    public ScanningHandler mScanHandler = ScanningHandler.getInstance();
    SwipeRefreshLayout swipeRefreshLayout;
    Button nextBtn;
    String mMasterIP;
    String mActivityName;
    Menu mMenu;
    EditText mSceneNameEditText;
    ImageButton mSceneNameImageButton;
    boolean mDeviceNameChanged = false;

    private SceneObject currentSceneObject;
    public  String utf8truncate(String input, int length) {
        StringBuffer result = new StringBuffer(length);
        int resultlen = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            int charlen = 0;
            if (c <= 0x7f) {
                charlen = 1;
            } else if (c <= 0x7ff) {
                charlen = 2;
            } else if (c <= 0xd7ff) {
                charlen = 3;
            } else if (c <= 0xdbff) {
                charlen = 4;
            } else if (c <= 0xdfff) {
                charlen = 0;
            } else if (c <= 0xffff) {
                charlen = 3;
            }
            if (resultlen + charlen > length) {
                break;
            }
            result.append(c);
            resultlen += charlen;
        }
        return result.toString();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_managedevices_list);

        Log.d("NewRefresh", "On Create is called in NewManageDevices");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        registerForDeviceEvents(this);
        nextBtn = (Button) findViewById(R.id.btnSave);

        mSceneNameImageButton = (ImageButton) findViewById(R.id.btnSceneName);
        mSceneNameEditText = (EditText) findViewById(R.id.eTSceneName);
        mSceneNameEditText.setEnabled(false);
        final int[] mPreviousLength = new int[1];
        final String[] mTextWatching = new String[1];
        mSceneNameEditText.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                Log.d("Karuna","After Text Changed");
                if(mSceneNameEditText.getText().toString().getBytes().length==50 && mSceneNameEditText.isEnabled()) {
                    Toast.makeText(NewManageDevices.this, getString(R.string.sceneLengthReached), Toast.LENGTH_SHORT).show();

                }

                if(mSceneNameEditText.getText().toString().getBytes().length>50) {
                    if(mTextWatching[0].isEmpty()||mTextWatching[0]==null){

                        mSceneNameEditText.setText(utf8truncate(mSceneNameEditText.getText().toString(),50));
                        mSceneNameEditText.setSelection(mTextWatching[0].length());
                    }else {
                        mSceneNameEditText.setText(mTextWatching[0]);
                        mSceneNameEditText.setSelection(mTextWatching[0].length());
                    }
                    Toast.makeText(NewManageDevices.this,getString(R.string.sceneNameLength),Toast.LENGTH_SHORT).show();/*
                    LibreError error = new LibreError("Sorry!!!!", getString(R.string.deviceLength));
                    showErrorMessage(error);*/
                  /*  new AlertDialog.Builder(LSSDPDeviceNetworkSettings.this)
                            .setTitle(getString(R.string.deviceNameChanging))
                            .setMessage(getString(R.string.deviceLength))
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();

                                }
                            }).setIcon(android.R.drawable.ic_dialog_alert)
                            .show();*/
                }else {
                    mTextWatching[0] = mSceneNameEditText.getText().toString();
                }
               /* Log.d("Karuna","After Text Changed");
                if(mSceneNameEditText.getText().toString().getBytes().length>50) {
                    if(mTextWatching[0].isEmpty()||mTextWatching[0]==null){
                        mSceneNameEditText.setText(LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(mMasterIP).getFriendlyname());
                        mSceneNameEditText.setSelection(mTextWatching[0].length());
                    }else {
                        mSceneNameEditText.setText(mTextWatching[0]);
                        mSceneNameEditText.setSelection(mTextWatching[0].length());
                    }
                    new AlertDialog.Builder(NewManageDevices.this)
                            .setTitle(getString(R.string.deviceNameChanging))
                            .setMessage(getString(R.string.deviceLength))
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            }).setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                }else {
                    mTextWatching[0] = mSceneNameEditText.getText().toString();
                }*/
            }

            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
                Log.d("Karuna","Before Text Changed");
            }

            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {

                Log.d("Karuna","On Text Changed");
                // Toast.makeText(getApplicationContext(), "Device Name cannot be more than 50", Toast.LENGTH_SHORT).show();
                // if(! deviceName.isClickable()) {
                mDeviceNameChanged = true;

            }
        });
        mSceneNameEditText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_RIGHT = 2;
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    try {
                        if (event.getRawX() >= (mSceneNameEditText.getRight() - mSceneNameEditText.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                            // your action here
                            mSceneNameEditText.setText("");

                            return true;
                        }
                    } catch (Exception e) {
                        //Toast.makeText(getApplication(), "dsd", Toast.LENGTH_SHORT).show();
                        LibreLogger.d(this, "ignore this log");
                    }
                }
                return false;
            }
        });

        mSceneNameImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mSceneNameEditText.isEnabled()) {
                    mSceneNameImageButton.setImageResource(R.mipmap.check);
                    mSceneNameEditText.setClickable(true);
                    mSceneNameEditText.setEnabled(true);
                    mSceneNameEditText.setFocusableInTouchMode(true);
                    mSceneNameEditText.setFocusable(true);
                    mSceneNameEditText.requestFocus();
                    mSceneNameEditText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.cancwel, 0);
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(mSceneNameEditText, InputMethodManager.SHOW_IMPLICIT);
                } else {
                    mSceneNameImageButton.setImageResource(R.mipmap.ic_mode_edit_black_24dp);
//                    mSceneNameEditText.setClickable(false);
//                    mSceneNameEditText.setEnabled(false);
                    mSceneNameEditText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                    if (mDeviceNameChanged) {
                        if (!mSceneNameEditText.getText().toString().equals("") && !mSceneNameEditText.getText().toString().trim().equalsIgnoreCase("NULL")) {
                            mSceneNameEditText.setClickable(false);
                            mSceneNameEditText.setEnabled(false);
                            LUCIControl mLuci = new LUCIControl(mMasterIP);
                            mLuci.sendAsynchronousCommand();
                            mLuci.SendCommand(MIDCONST.MID_SCENE_NAME, mSceneNameEditText.getText().toString(), LSSDPCONST.LUCI_SET);
                            // UpdateLSSDPNodeDeviceName(mMasterIP, mSceneNameEditText.getText().toString());

                            SceneObject sceneObjectFromCentralRepo = mScanHandler.getSceneObjectFromCentralRepo(mMasterIP);
                            if (sceneObjectFromCentralRepo != null) {
                                sceneObjectFromCentralRepo.setSceneName(mSceneNameEditText.getText().toString());
                                mScanHandler.putSceneObjectToCentralRepo(mMasterIP, sceneObjectFromCentralRepo);
                            }

                            mDeviceNameChanged = false;
                        } else {
                            if (!(NewManageDevices.this.isFinishing())) {
                                if (mSceneNameEditText.getText().toString().equals("")) {
                                    new AlertDialog.Builder(NewManageDevices.this)
                                            .setTitle(getString(R.string.sceneNameChange))
                                            .setMessage(" " +
                                                    getString(R.string.sceneNameEmpty))
                                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.cancel();
                                                }
                                            }).setIcon(android.R.drawable.ic_dialog_alert)
                                            .show();

                                } else if (mSceneNameEditText.getText().toString().trim().equalsIgnoreCase("NULL")) {
                                    new AlertDialog.Builder(NewManageDevices.this)
                                            .setTitle(getString(R.string.sceneNameChange))
                                            .setMessage(" " +
                                                    getString(R.string.sceneNameNull))
                                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.cancel();
                                                }
                                            }).setIcon(android.R.drawable.ic_dialog_alert)
                                            .show();
                                }
                            }

                        }


                    }


//                    LUCIControl mLuci = new LUCIControl(mMasterIP);
//                    mLuci.SendCommand(MIDCONST.MID_SCENE_NAME,mSceneNameEditText.getText().toString(),LSSDPCONST.LUCI_SET);

                }
            }
        });

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh);
        swipeRefreshLayout.setOnRefreshListener(onRefreshListener);
        listView = (ListView) findViewById(R.id.listView);

        /*setting node flag to default*/
        clearLSSDPNodeFlags();


        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSceneNameEditText.isEnabled()) {
                    new AlertDialog.Builder(NewManageDevices.this)
                            .setTitle(getString(R.string.sceneNameChanging))
                            .setMessage(getString(R.string.sceneNameChangingMsg))
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    goNext();

                                }
                            }).setIcon(android.R.drawable.ic_dialog_alert)
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            })
                            .show();
                } else
                    goNext();

            }
        });


      /*  swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeRefreshLayout.setRefreshing(true);
              *//*  mManageDeviceAdapter.clear();
                mManageDeviceAdapter.notifyDataSetChanged();*//*

                *//* function which resets the device counts *//*
                clearMenuDetails();

                *//* Swipe Refersh UI will show the circular bar of 1750ms , in this three M-search will happen in the interval of time 500ms *//*
                clearAndRefreshNodes();

                addSlavesAndFreeDeviceFromCentralDBToAdapter(mMasterIP);

            }
        });*/
    }

    private void goNext() {
        if (!(mActivityName.contains("NowPlayingActivity"))) {

                    /* User is coming from CreateNewScene class */
            LSSDPNodes mMasterNode = mScanHandler.getLSSDPNodeFromCentralDB(mMasterIP);
            if (mMasterNode != null) {
                if (!mScanHandler.isIpAvailableInCentralSceneRepo(mMasterIP)) {
                    SceneObject newSceneObject = new SceneObject(mSceneNameEditText.getText().toString(), mMasterNode.getFriendlyname(), 0, mMasterNode.getIP());
                    mScanHandler.putSceneObjectToCentralRepo(mMasterIP, newSceneObject);
                }
            }

            Intent mActiveScenesList = new Intent(NewManageDevices.this, SourcesOptionActivity.class);
            mActiveScenesList.putExtra(Constants.CURRENT_DEVICE_IP, mMasterIP);
            mActiveScenesList.putExtra("current_source", "0");
            startActivity(mActiveScenesList);
            finish();
        } else {

                    /* If the user is coming from Nowplaying screen then launch NowPlayingActivity */
            Intent mActiveScenesList = new Intent(NewManageDevices.this, NowPlayingActivity.class);
            mActiveScenesList.putExtra(Constants.CURRENT_DEVICE_IP, mMasterIP);
            startActivity(mActiveScenesList);
            finish();
        }
    }

    public void clearAndRefreshNodes() {
                /* Swipe Refersh UI will show the circular bar of 1750ms , in this three M-search will happen in the interval of time 500ms */

        final LibreApplication application = (LibreApplication) getApplication();
        //application.getScanThread().clearNodes();
        application.getScanThread().UpdateNodes();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(false);
            }
        }, 10000);

        /*new Handler().postDelayed(new Runnable() {
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

                // application.getScanThread().UpdateNodes();

                swipeRefreshLayout.setRefreshing(false);
                mManageDeviceAdapter.notifyDataSetChanged();
                addSlavesAndFreeDeviceFromCentralDBToAdapter(mMasterIP);


                *//*If there is no master we will navigate to CreateNewScene*//*
                if (mScanHandler.getSceneObjectFromCentralRepo().size() == 0) {
                    Intent intent = new Intent(NewManageDevices.this, CreateNewScene.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }

            }
        }, 2650);*/
    }


    /*TO prevent ConcurrentModification exceptions*/
    private synchronized void clearLSSDPNodeFlags() throws ConcurrentModificationException {
        LSSDPNodeDB mNodeDB = LSSDPNodeDB.getInstance();
        for (LSSDPNodes lssdpNodes : mNodeDB.GetDB()) {
            lssdpNodes.setCurrentState(LSSDPNodes.STATE_CHANGE_STAGE.NO_TRACKING);
        }

    }

    public void UpdateLSSDPNodeDeviceName(String ipaddress, String mDeviceName) {

        LSSDPNodes mToBeUpdateNode = mScanHandler.getLSSDPNodeFromCentralDB(ipaddress);

        LSSDPNodeDB mNodeDB = LSSDPNodeDB.getInstance();
        if (mToBeUpdateNode != null) {
            mToBeUpdateNode.setFriendlyname(mDeviceName);
            mNodeDB.renewLSSDPNodeDataWithNewNode(mToBeUpdateNode);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

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
        mActivityName = "";

//        /*to prevent memory leaks*/
//        mManageDevicesHandler.removeCallbacksAndMessages(this);

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

    @Override
    protected void onPause() {
        super.onPause();
        closeLoader();
    }

    public void StartLSSDPScanAfterRelease() {

//        final LibreApplication application = (LibreApplication) getApplication();
//        application.getScanThread().UpdateNodes();
//
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                application.getScanThread().UpdateNodes();
//            }
//        }, 500);
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                application.getScanThread().UpdateNodes();
//
//            }
//        }, 1000);
//
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                addSlavesAndFreeDeviceFromCentralDBToAdapter(mMasterIP);
//
//            }
//        }, 1500);

    }

    public void UpdateLSSDPNodeListWithStateAndConcurentId(String ipaddress, String mDeviceState, String zoneIdOrConcurrentSSID) {
        Log.e("ScanningHandler", "Updating LSSDP Device State " + mDeviceState);

        LSSDPNodes mToBeUpdateNode = mScanHandler.getLSSDPNodeFromCentralDB(ipaddress);
        LSSDPNodes mMasterNode = mScanHandler.getLSSDPNodeFromCentralDB(mMasterIP);
        LSSDPNodeDB mNodeDB = LSSDPNodeDB.getInstance();

        if (mMasterNode != null && mToBeUpdateNode != null) {
            LUCIControl luciControl = new LUCIControl(mToBeUpdateNode.getIP());
            if (mDeviceState.equals("Slave")) {

                mToBeUpdateNode.setDeviceState("S");
                if (mScanHandler.getconnectedSSIDname(getApplicationContext()) == mScanHandler.HN_MODE) {
                    mToBeUpdateNode.setcSSID(zoneIdOrConcurrentSSID);
                } else {
                    mToBeUpdateNode.setZoneID(zoneIdOrConcurrentSSID);
                }
            } else if (mDeviceState.equals("Free")) {

                mToBeUpdateNode.setDeviceState("F");
                if (mScanHandler.getconnectedSSIDname(getApplicationContext()) == mScanHandler.HN_MODE) {
                    mToBeUpdateNode.setcSSID(zoneIdOrConcurrentSSID);
                } else {
                    mToBeUpdateNode.setZoneID(zoneIdOrConcurrentSSID);
                }

            } else if (mDeviceState.equals("Master")) {
                mToBeUpdateNode.setDeviceState("M");
                if (mScanHandler.getconnectedSSIDname(getApplicationContext()) == mScanHandler.HN_MODE) {
                    mToBeUpdateNode.setcSSID(zoneIdOrConcurrentSSID);
                } else {
                    mToBeUpdateNode.setZoneID(zoneIdOrConcurrentSSID);
                }
            }


            mNodeDB.renewLSSDPNodeDataWithNewNode(mToBeUpdateNode);


            /*updating adapter*/
//            addSlavesAndFreeDeviceFromCentralDBToAdapter(mMasterIP);


//            mManageDeviceAdapter.notifyDataSetChanged();
            updateMenuDetails();
        }

    }

    public void UpdateLSSDPNodeList(String ipaddress, String mDeviceState) {
        Log.e("ScanningHandler", "Updating LSSDP Device State " + mDeviceState);

        LSSDPNodes mToBeUpdateNode = mScanHandler.getLSSDPNodeFromCentralDB(ipaddress);
        LSSDPNodes mMasterNode = mScanHandler.getLSSDPNodeFromCentralDB(mMasterIP);
        LSSDPNodeDB mNodeDB = LSSDPNodeDB.getInstance();

        if (mMasterNode != null && mToBeUpdateNode != null) {
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
                if (mToBeUpdateNode.getDeviceState().equals("M")) {
                    ArrayList<LSSDPNodes> mSlaveList = mScanHandler.getSlaveListForMasterIp(mToBeUpdateNode.getIP(),
                            mScanHandler.getconnectedSSIDname(getApplicationContext()));
                    for (LSSDPNodes mSlaveNode : mSlaveList) {
                        mSlaveNode.setDeviceState("F");
                        mNodeDB.renewLSSDPNodeDataWithNewNode(mSlaveNode);
                    }
                }else{
                    LibreLogger.d("command 103",mToBeUpdateNode.getDeviceState().toString());
                }
                mToBeUpdateNode.setDeviceState("F");

            }
            mNodeDB.renewLSSDPNodeDataWithNewNode(mToBeUpdateNode);


            /*updating adapter*/
            addSlavesAndFreeDeviceFromCentralDBToAdapter(mMasterIP);


            mManageDeviceAdapter.notifyDataSetChanged();
            updateMenuDetails();
        }

    }

    public void clearMenuDetails() {

        try {
            MenuItem mMenuItem = this.mMenu.findItem(R.id.numberOfMaster);
            mMenuItem.setTitle("M: " + 0);

            mMenuItem = this.mMenu.findItem(R.id.numberOfSlave);
            mMenuItem.setTitle("S: " + 0);

            mMenuItem = this.mMenu.findItem(R.id.numberOfFree);
            mMenuItem.setTitle("F: " + 0);
        } catch (Exception e) {

        }
    }

    public void updateMenuDetails() {
/*
        try {
            MenuItem mMenuItem = this.mMenu.findItem(R.id.numberOfMaster);
            int mNumberOfDevices = mScanHandler.getNumberOfSlavesForMasterIp(mMasterIP,
                    mScanHandler.getConnectedSSIDName(getApplicationContext()));
            if (mNumberOfDevices > 0)
                mMenuItem.setTitle("M: " + 1);
            else
                mMenuItem.setTitle("M: " + 0);

            mMenuItem = this.mMenu.findItem(R.id.numberOfSlave);
            mMenuItem.setTitle("S: " + (mNumberOfDevices - 1));

            mMenuItem = this.mMenu.findItem(R.id.numberOfFree);
            mMenuItem.setTitle("F: " + mScanHandler.getFreeDeviceList().size());
        } catch (Exception e) {

        }*/
    }


    SwipeRefreshLayout.OnRefreshListener onRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            swipeRefreshLayout.setRefreshing(true);
            mManageDeviceAdapter.clear();
            mManageDeviceAdapter.notifyDataSetChanged();
        /* function which resets the device counts */
            clearMenuDetails();

        /* Swipe Refersh UI will show the circular bar of 1750ms , in this three M-search will happen in the interval of time 500ms */
            clearAndRefreshNodes();

            addSlavesAndFreeDeviceFromCentralDBToAdapter(mMasterIP);
        }


    };

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("NewRefresh", "OnResume of NewManageDevices");

        Intent myIntent = getIntent(); // gets the previously created intent
        mMasterIP = myIntent.getStringExtra("master_ip");
        mActivityName = myIntent.getStringExtra("activity_name");
        if(mActivityName==null)
            mActivityName="";
        /** Special condition to check if the selected mMasterIp is indeed a master device
         * if not we have to make sure to refresh the screen */
        if(mMasterIP!=null  ){
            LSSDPNodes masterNode = mScanHandler.getLSSDPNodeFromCentralDB(mMasterIP);
            if (masterNode!=null && masterNode.getDeviceState()!=null && !masterNode.getDeviceState().equalsIgnoreCase("M") ){
        /* this is a case where we are seeing that our master IP, is not a master!!
        * Initiate the refresh */
                onRefreshListener.onRefresh();
            }

        }

        if (mMasterIP != null) {
            // request scene name
            LUCIControl luciControl = new LUCIControl(mMasterIP);
            luciControl.SendCommand(MIDCONST.MID_SCENE_NAME, null, LSSDPCONST.LUCI_GET);
            LibreLogger.d(this, "requesting scene Name");
        }

        currentSceneObject = mScanHandler.getSceneObjectFromCentralRepo(mMasterIP);
        // ActiveSceneAdapter.mMasterSpecificSlaveAndFreeDeviceMap.get(currentIpAddress);
        if (currentSceneObject != null)
            mSceneNameEditText.setText("" + currentSceneObject.getSceneName());

        mManageDeviceAdapter = new NewManageAdapter(this,
                R.layout.manage_devices_list_item,
                mManageDevicesHandler,
                mMasterIP, mActivityName);
        listView.setAdapter(mManageDeviceAdapter);

                /* if the user is coming from Nowplaying screen we need to display the button as Done */
        if (mActivityName.contains("NowPlayingActivity")) {
            setTitle(getString(R.string.title_activity_device_list));
            nextBtn.setText(getString(R.string.done));
        }else if (mActivityName.contains("CreateNewScene")){
            nextBtn.setText(getString(R.string.next));
        }

        Log.d("NewRefresh", "masterip currentlt being considered is" + mMasterIP);

        registerForDeviceEvents(this);
        /*Karuna For Testing ... */
       /* LibreApplication application = (LibreApplication) getApplication();
        application.getScanThread().UpdateNodes();*/
        addSlavesAndFreeDeviceFromCentralDBToAdapter(mMasterIP);
        mManageDeviceAdapter.notifyDataSetChanged();
    }

    public void addSlavesAndFreeDeviceFromCentralDBToAdapter(String mMasterIP) {

        /*commenting intentionally */
        mManageDeviceAdapter.clear();

        Log.d("Refreshing", "inside the function find slaves and freedevices for the master " + mMasterIP);

        SceneObject mSceneObject = mScanHandler.getSceneObjectFromCentralRepo(mMasterIP);
        LSSDPNodes mMasterNode = mScanHandler.getLSSDPNodeFromCentralDB(mMasterIP);

        if (mSceneObject == null || mMasterNode == null)
            return;

//        if (mSceneObject.getSceneSourceName() != null)
//            mSceneNameEditText.setText(mSceneObject.getSceneSourceName());

        /* Sending Luci Commands For the Master */
        LUCIControl masterluciControl = new LUCIControl(mMasterIP);
        masterluciControl.sendAsynchronousCommand();
//        masterluciControl.SendCommand(MIDCONST.MID_SCENE_NAME, null, LSSDPCONST.LUCI_GET);
        masterluciControl.SendCommand(41, LUCIMESSAGES.GET_PLAY, 2);
        /*will ask volume only while creating LUCI sockets*/
//        masterluciControl.SendCommand(64, null, 1);


        /* We are not Making use of the SceneObject other than getting ip & volume ,
        * Both we can get through LSsdpNode & Volume Hashmap.*/
     //   if (mSceneObject != null)
     {
            /* Master device is present */
            DeviceData data = new DeviceData(mMasterNode.getFriendlyname(), mMasterNode.getIP());
            if(LibreApplication.INDIVIDUAL_VOLUME_MAP.get(mMasterNode.getIP())!=null) {
                data.setVolumeValueInPercentage(LibreApplication.INDIVIDUAL_VOLUME_MAP.get(mMasterNode.getIP()));
            }else{/* If Master is Not available in Volume Map then we are making it to Default
            and Sending Volume to update the Screen */
                LibreLogger.d(this,"Volume Map is null For the Master So default Volume 50 is Adding " + mMasterNode.getIP());
                data.setVolumeValueInPercentage(50);
                masterluciControl.SendCommand(64, null, CommandType.GET);
            }
         mManageDeviceAdapter.add(data);

        }/*else{
            LSSDPNodes mMasterNode = mScanHandler.getLSSDPNodeFromCentralDB(mMasterIP);
            if(mMasterNode != null) {
                SceneObject sceneObjec = new SceneObject(" ", mMasterNode.getFriendlyname(), 0, mMasterNode.getIP());
                LibreLogger.d(this,"Ipaddress of Master Node" + mMasterNode.getIP());
                mScanHandler.putSceneObjectToCentralRepo(mMasterIP, sceneObjec);
                mManageDeviceAdapter.add(sceneObjec);
            }
        }*/

        ArrayList<LSSDPNodes> mSlaveLSSDPNode = mScanHandler.getSlaveListForMasterIp(mMasterIP,
                mScanHandler.getconnectedSSIDname(getApplicationContext()));

        Log.d("Refreshing", "number of slaves found are " + mSlaveLSSDPNode.size());

        ArrayList<LSSDPNodes> mFreeDeviceList = mScanHandler.getFreeDeviceList();

        Log.d("Refreshing", "number of free devices found are " + mFreeDeviceList.size());


        /* changed from 42 to 64 */
        for (LSSDPNodes mSlaveNode : mSlaveLSSDPNode) {
            LUCIControl luciControl = new LUCIControl(mSlaveNode.getIP());
            luciControl.sendAsynchronousCommand();
            /*commenting because will ask volume only while creating LUCI sockets*/
//            luciControl.SendCommand(64, null, 1);
            DeviceData data = new DeviceData(mSlaveNode.getFriendlyname(), mSlaveNode.getIP());
            mManageDeviceAdapter.add(data);

        }


        for (LSSDPNodes mFreeDeviceNode : mFreeDeviceList) {
            LUCIControl luciControl = new LUCIControl(mFreeDeviceNode.getIP());
            luciControl.sendAsynchronousCommand();
            /*commenting because will ask volume only while creating LUCI sockets*/
//            luciControl.SendCommand(64, null, 1);
            DeviceData deviceData = new DeviceData(mFreeDeviceNode.getFriendlyname(), mFreeDeviceNode.getIP());
            mManageDeviceAdapter.add(deviceData);

        }
        enableEditSceneNameButton();
        LibreLogger.d("Refreshing", "Notifying");
//        mManageDeviceAdapter.notifyDataSetChanged();
    }


    /*This overloaded function will get called only when we will receive 103 for slave
    It is created to prevent flickering issue while changing device state from free to slave*/
    private void addSlavesAndFreeDeviceFromCentralDBToAdapter(String mMasterIP, String stateChangedDeviceIp) {

        SceneObject mSceneObject = mScanHandler.getSceneObjectFromCentralRepo(mMasterIP);
        LSSDPNodes mMasterNode = mScanHandler.getLSSDPNodeFromCentralDB(mMasterIP);

        LSSDPNodes mToBeUpdateNode = mScanHandler.getLSSDPNodeFromCentralDB(stateChangedDeviceIp);

        if (mSceneObject == null || mMasterNode == null)
            return;

//        if (mSceneObject.getSceneSourceName() != null)
//            mSceneNameEditText.setText(mSceneObject.getSceneSourceName());

        /* Sending Luci Commands For the Master */
        LUCIControl masterluciControl = new LUCIControl(mMasterIP);
        masterluciControl.sendAsynchronousCommand();
//        masterluciControl.SendCommand(MIDCONST.MID_SCENE_NAME, null, LSSDPCONST.LUCI_GET);
        masterluciControl.SendCommand(41, LUCIMESSAGES.GET_PLAY, 2);

        /*commenting because will ask volume only while creating LUCI sockets*/
//        masterluciControl.SendCommand(MIDCONST.VOLUME_CONTROL, null, 1);


        if (mSceneObject != null) {
            /* Master device is present */
            DeviceData data = new DeviceData(mMasterNode.getFriendlyname(), mSceneObject.getIpAddress());
            data.setVolumeValueInPercentage(mSceneObject.getVolumeValueInPercentage());
            mManageDeviceAdapter.add(data);
        }

        /*updating volume for 103 device */
        if (mToBeUpdateNode != null) {
            LUCIControl luciControl = new LUCIControl(mToBeUpdateNode.getIP());
            luciControl.sendAsynchronousCommand();
            /*commenting because will ask volume only while creating LUCI sockets*/
//            luciControl.SendCommand(64, null, 1);
            DeviceData data = new DeviceData(mToBeUpdateNode.getFriendlyname(), mToBeUpdateNode.getIP());
            mManageDeviceAdapter.add(data);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unRegisterForDeviceEvents();
        /*to prevent memory leaks*/
        if (mManageDevicesHandler != null)
            mManageDevicesHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void newDeviceFound(LSSDPNodes node) {
        LibreLogger.d(this, "New device is found with the ip address = " + node.getIP());
        LibreLogger.d(this, "New device is found with the Selected Ipaddressx  = " + mMasterIP);

        Log.d("DiscoveryIssue", "NewDeviceFound" + node);
        int mMode = mScanHandler.getconnectedSSIDname(getApplicationContext());
        LSSDPNodes mMasternode = mScanHandler.getLSSDPNodeFromCentralDB(mMasterIP);

        if (node != null && mMasternode != null) {

            if (node.getDeviceState().contains("F") ||
                    (node.getDeviceState().contains("S") && (mMasternode != null) &&
                            (((mMode == mScanHandler.HN_MODE) && (mMasternode != null) && mMasternode.getcSSID().equals(node.getcSSID())) ||
                                    ((mMode == mScanHandler.SA_MODE) && (mMasternode != null) && mMasternode.getZoneID().equals(node.getZoneID())))
                            || node.getIP().equals(mMasterIP))) {


                LUCIControl luciControl = new LUCIControl(node.getIP());
                if (node.getDeviceState().contains("M")) {
                    luciControl.SendCommand(MIDCONST.MID_SCENE_NAME, null, LSSDPCONST.LUCI_GET);
                }
                luciControl.SendCommand(41, LUCIMESSAGES.GET_PLAY, 2);

                /*commenting because will ask volume only while creating LUCI sockets*/
//                luciControl.SendCommand(64, null, 1);

                DeviceData data = new DeviceData(node.getFriendlyname(), node.getIP());

                if (!mScanHandler.isIpAvailableInCentralSceneRepo(mMasterIP)) {
                    SceneObject sceneObjec = new SceneObject(mSceneNameEditText.getText().toString(), node.getFriendlyname(), 0, node.getIP());
                    mScanHandler.putSceneObjectToCentralRepo(mMasterIP, sceneObjec);
                } else {
                    Log.d("DiscoveryIssue", "NewDeviceFound Added--123 " + node);
                    mSceneNameEditText.setText(mScanHandler.getSceneObjectFromCentralRepo(mMasterIP).getSceneName());
                }

                Log.d("DiscoveryIssue", "NewDeviceFound Added-- " + node);

                mManageDeviceAdapter.add(data);
                mManageDeviceAdapter.notifyDataSetChanged();
                updateMenuDetails();
                if (node.getDeviceState().equals("M")) { /* this is to Ensure Slaves Comes in UI after Getting Master*/
                    addSlavesAndFreeDeviceFromCentralDBToAdapter(mMasterIP);
                }

            }
        }

    }

    @Override
    public void deviceGotRemoved(final String mIpAddress) {
        DeviceData scneObj = mManageDeviceAdapter.getSceneObjectFromAdapter(mIpAddress);
        mManageDeviceAdapter.remove(scneObj);
        updateMenuDetails();
        mManageDeviceAdapter.notifyDataSetChanged();
        if (mScanHandler.removeSceneMapFromCentralRepo(mIpAddress)) {

        }


                /* MasterIP is gettign Removed in Manage Devices then it will go to PlayNewScreen */
        if (mMasterIP.equals(mIpAddress)) {
            if (mScanHandler.getSceneObjectFromCentralRepo().size() > 0) {
                Intent intent = new Intent(NewManageDevices.this, ActiveScenesListActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            } else {
                Intent intent = new Intent(NewManageDevices.this, PlayNewActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
            Toast.makeText(getApplicationContext(), "Master Got Removed , So Going Back to Play New", Toast.LENGTH_SHORT).show();

        }

    }

    @Override
    public void onBackPressed() {

        /* if the user is coming from nowplaying activity then we need to launch the ActiveScenesList */
        // super.onBackPressed();
        if (mSceneNameEditText.isEnabled()) {
            new AlertDialog.Builder(NewManageDevices.this)
                    .setTitle(getString(R.string.sceneNameChanging))
                    .setMessage(getString(R.string.sceneNameChangingMsg))
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            goBack();

                        }
                    }).setIcon(android.R.drawable.ic_dialog_alert)
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .show();
        } else
            goBack();

    }

    private void goBack() {
        if (!mActivityName.contains("NowPlayingActivity")) {
            Intent intent = new Intent(NewManageDevices.this, ActiveScenesListActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

        } else {

            Intent intent = new Intent(NewManageDevices.this, NowPlayingActivity.class);
            intent.putExtra(Constants.CURRENT_DEVICE_IP, mMasterIP);
            startActivity(intent);
            finish();
        }
    }
    private void showErrorMessageForDifferentWifiBand(){
        /* Commented For Ls9 Changes
        LSSDPNodes mNodeForInformation = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(mMasterIP);
        if(mNodeForInformation!=null){
            if(mNodeForInformation.getmWifiBand().equalsIgnoreCase(LSSDPNodes.m5GHZ_VALUE)) {
                LibreError error = new LibreError("", getString(R.string.m5GHzMessage));
                showErrorMessage(error);
            }else{
                LibreError error = new LibreError("", getString(R.string.m2GHzMessage));
                showErrorMessage(error);
            }
        }*/
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(mManageDeviceAdapter.isAnyDeviceInChangingState()){
            LibreLogger.d(this, "one or more device is in changing state");
            Toast.makeText(this,getResources().getString(R.string.device_state), Toast.LENGTH_SHORT).show();
            return false;
        }
        switch (item.getItemId()) {

            case R.id.FreeAll: {

//                msg1.what = DeviceMasterSlaveFreeConstants.ACTION_TO_CREATE_SLAVE_INITIATED;


                 /*removing timeout handler*/
//                mManageDevicesHandler.removeMessages(DeviceMasterSlaveFreeConstants.ACTION_TO_INDICATE_TIMEOUT);


                ArrayList<LSSDPNodes> mSlaveListForMyMaster1 = mScanHandler.getSlaveListForMasterIp(mMasterIP,
                        mScanHandler.getconnectedSSIDname(getApplicationContext()));
                LSSDPNodes mMasterNode = mScanHandler.getLSSDPNodeFromCentralDB(mMasterIP);

                if(mMasterNode.getCurrentSource()== Constants.GCAST_SOURCE
                        && mMasterNode.getmPlayStatus() == SceneObject.CURRENTLY_PLAYING){
                    LibreError error = new LibreError(mMasterNode.getFriendlyname(), Constants.SPEAKER_IS_CASTING +
                    " Free All is Not Allowed");
                 //   showErrorMessage(error);
                    return true;
                }

                if (mSlaveListForMyMaster1.size() == 0) {

                    new AlertDialog.Builder(NewManageDevices.this)
                            .setTitle("Free All Devices")
                            .setMessage("All the devices in the zone are already Free!")
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }).show();

                    return true;
                } else {
                    for (LSSDPNodes mSlaveNode : mSlaveListForMyMaster1) {
                        LibreLogger.d("this", "Release Scene Slave Ip List" + mSlaveNode.getFriendlyname());

                        Message msg = new Message();
                        Bundle b = new Bundle();
                        b.putString("ipAddress", mSlaveNode.getIP());
                        msg.what = DeviceMasterSlaveFreeConstants.ACTION_TO_FREE_DEVICE_INITATED;
                        msg.setData(b);
                        mManageDevicesHandler.sendMessage(msg);

                        LUCIControl luciControl = new LUCIControl(mSlaveNode.getIP());
                        /* Sending Asynchronous Registration*/
                        luciControl.sendAsynchronousCommand();
                        luciControl.SendCommand(MIDCONST.MID_DDMS, LUCIMESSAGES.SETFREE, LSSDPCONST.LUCI_SET);
                    }
                    StartLSSDPScanAfterRelease();
                    updateMenuDetails();

                    return true;
                }
            }

         /*   case R.id.DefaultVolume:{
                ArrayList<LSSDPNodes> mSlaveListForMyMaster1 = mScanHandler.getSlaveListForMasterIp(mMasterIP,
                        mScanHandler.getConnectedSSIDName(getApplicationContext()));
                LSSDPNodes mMasterNode = mScanHandler.getLSSDPNodeFromCentralDB(mMasterIP);
                LUCIControl mMasterLuciControl = new LUCIControl(mMasterNode.getIP());
                mMasterLuciControl.SendCommand(MIDCONST.MID_VOLUME,""+50 , LSSDPCONST.LUCI_SET);
                for (LSSDPNodes mSlaveNode : mSlaveListForMyMaster1) {
                    LibreLogger.d("this", "Changing the Volume for the Device Name " + mSlaveNode.getFriendlyname());
                    LUCIControl luciControl = new LUCIControl(mSlaveNode.getIP());
                    luciControl.SendCommand(MIDCONST.MID_VOLUME, "" + 50, LSSDPCONST.LUCI_SET);
                }
            }
            return true;*/

            case R.id.JoinAllToThisScene: {
                showErrorMessageForDifferentWifiBand();
                ArrayList<LSSDPNodes> mFreeSpeakerList = mScanHandler.getFreeDeviceList();
                final LSSDPNodes mMasterNode = mScanHandler.getLSSDPNodeFromCentralDB(mMasterIP);
                if (mMasterNode == null) {
                    Toast.makeText(NewManageDevices.this, getString(R.string.masterNotAvailable) + getString(R.string.pleaseRefresh), Toast.LENGTH_SHORT).show();
                    return true;
                }

                if(mMasterNode.getCurrentSource()== Constants.GCAST_SOURCE
                        && mMasterNode.getmPlayStatus() == SceneObject.CURRENTLY_PLAYING){
                    LibreError error = new LibreError(mMasterNode.getFriendlyname(), Constants.SPEAKER_IS_CASTING +
                            " Join All To This Scene is Not Allowed");
                    //showErrorMessage(error);
                    return true;
                }
                /*removing timeout handler*/
//                mManageDevicesHandler.removeMessages(DeviceMasterSlaveFreeConstants.ACTION_TO_INDICATE_TIMEOUT);

                for (LSSDPNodes mFreeNode : mFreeSpeakerList) {
                    /* Commented For Ls9
                    if(mFreeNode!=null &&  mFreeNode.getmWifiBand().equalsIgnoreCase(mMasterNode.getmWifiBand()))*/ {
                    Message msg = new Message();
                    Bundle b = new Bundle();
                    msg.what = DeviceMasterSlaveFreeConstants.ACTION_TO_CREATE_SLAVE_INITIATED;

                    b.putString("ipAddress", mFreeNode.getIP());
                    msg.setData(b);
                    mManageDevicesHandler.sendMessage(msg);

                    LibreLogger.d("this", "Adding slave To Master" + mFreeNode.getFriendlyname());
                    LUCIControl luciControl = new LUCIControl(mFreeNode.getIP());

                        /* Sending Asynchronous Registration*/
                    luciControl.sendAsynchronousCommand();

                    String mCSSID = mMasterNode.getcSSID();
                    String mZoneID = mMasterNode.getZoneID();
                    ArrayList<LUCIPacket> setOfCommandsWhileMakingSlave = new ArrayList<LUCIPacket>();
                    if (mCSSID != null) {
                        LUCIPacket concurrentSsid105Packet = new LUCIPacket(mCSSID.getBytes(),
                                (short) mCSSID.length(), (short) MIDCONST.MID_SSID,
                                (byte) LSSDPCONST.LUCI_SET);
                        setOfCommandsWhileMakingSlave.add(concurrentSsid105Packet);
                        //luciControl.SendCommand(MIDCONST.MID_SSID, mCSSID, LSSDPCONST.LUCI_SET);
                        LibreLogger.d(this, "Slave Concurrent SSID" + mCSSID);

                    }
                    LUCIPacket zoneid104Packet = new LUCIPacket(mZoneID.getBytes(),
                            (short) mZoneID.length(), (short) MIDCONST.MID_DDMS_ZONE_ID, (byte) LSSDPCONST.LUCI_SET);
                    setOfCommandsWhileMakingSlave.add(zoneid104Packet);
                    //luciControl.SendCommand(MIDCONST.MID_DDMS_ZONE_ID, mZoneID, LSSDPCONST.LUCI_SET);
                    LibreLogger.d(this, "Slave ZoneID" + mZoneID);
                    LUCIPacket setSlave100Packet = new LUCIPacket(LUCIMESSAGES.SETSLAVE.getBytes(),
                            (short) LUCIMESSAGES.SETSLAVE.length(), (short) MIDCONST.MID_DDMS, (byte) LSSDPCONST.LUCI_SET);
                    setOfCommandsWhileMakingSlave.add(setSlave100Packet);


                    luciControl.SendCommand(setOfCommandsWhileMakingSlave);
                 //   luciControl.SendCommand(MIDCONST.MID_DDMS, LUCIMESSAGES.SETSLAVE, LSSDPCONST.LUCI_SET);
                    try {
                        Thread.sleep(150);
                    } catch (Exception e) {
                        e.printStackTrace();
                        }
                    }

                }


//                StartLSSDPScanAfterRelease();
                updateMenuDetails();
            }
            return true;

            case R.id.JoinAll: {
                showErrorMessageForDifferentWifiBand();
                final LSSDPNodes mMasternode = mScanHandler.getLSSDPNodeFromCentralDB(mMasterIP);//(sceneAddressList.get(mCurrentNowPlayingScene));
                if (mMasternode == null) {
                    Toast.makeText(this, getString(R.string.masterNotFound), Toast.LENGTH_LONG).show();
                    return true;
                }
                if(mMasternode.getCurrentSource()== Constants.GCAST_SOURCE
                        && mMasternode.getmPlayStatus() == SceneObject.CURRENTLY_PLAYING){
                    LibreError error = new LibreError(mMasternode.getFriendlyname(), Constants.SPEAKER_IS_CASTING +
                            " Join All is Not Allowed ");
              //      showErrorMessage(error);
                    return true;
                }
                 /*removing timeout handler*/
//                mManageDevicesHandler.removeMessages(DeviceMasterSlaveFreeConstants.ACTION_TO_INDICATE_TIMEOUT);

                /*Showing loader for join all as we are getting master 103 first so have given timeout of 5 sec*/
                showLoader("");

                mManageDevicesHandler.sendEmptyMessageDelayed(DeviceMasterSlaveFreeConstants.JOIN_ALL_TIMEOUT, 5000);

                LUCIControl mMluciControl = new LUCIControl(mMasternode.getIP());
                mMluciControl.sendAsynchronousCommand();
                mMluciControl.SendCommand(MIDCONST.MID_JOIN_OR_DROP, LUCIMESSAGES.JOIN_ALL, LSSDPCONST.LUCI_SET);
            }
            return true;




            /*case R.id.JoinAllToThisScene:{
                final LSSDPNodes mMasternode = mScanHandler.getLSSDPNodeFromCentralDB(mMasterIP);//(sceneAddressList.get(mCurrentNowPlayingScene));
                if (mMasternode == null) {
                    Toast.makeText(this, "Opps!Master not found", Toast.LENGTH_LONG).show();
                    return true;
                }

                LUCIControl mMluciControl = new LUCIControl(mMasternode.getIP());
                mMluciControl.sendAsynchronousCommand();
                mMluciControl.SendCommand(MIDCONST.MID_JOIN_OR_DROP, LUCIMESSAGES.JOIN_ALL, LSSDPCONST.LUCI_SET);
            }
                return true;*/
            /*case R.id.DropThisScene:
            {
                final LSSDPNodes mMasternode = mScanHandler.getLSSDPNodeFromCentralDB(mMasterIP);
                if (mMasternode == null) {
                    Toast.makeText(NewManageDevices.this, "Master is not available " + "  Please Refresh ..", Toast.LENGTH_SHORT).show();
                    return true;
                }
                ArrayList<LSSDPNodes> mSlaveListForMyMaster = mScanHandler.getSlaveListForMasterIp(mMasterIP,
                        mScanHandler.getConnectedSSIDName(getApplicationContext()));

               *//* for (LSSDPNodes mSlaveNode : mSlaveListForMyMaster) {
                    LibreLogger.d("this", "Release Scene Slave Ip List" + mSlaveNode.getFriendlyname());
                    LUCIControl luciControl = new LUCIControl(mSlaveNode.getIP());
                        *//**//* Sending Asynchronous Registration*//**//*
                    luciControl.sendAsynchronousCommand();
                    luciControl.SendCommand(MIDCONST.MID_DDMS, LUCIMESSAGES.SETFREE, LSSDPCONST.LUCI_SET);

                }*//*

                LUCIControl mMluciControl = new LUCIControl(mMasternode.getIP());
                mMluciControl.sendAsynchronousCommand();
                mMluciControl.SendCommand(MIDCONST.MID_DDMS, LUCIMESSAGES.DROP, LSSDPCONST.LUCI_SET);
                LibreLogger.d("this", "Release Scene Master Name" + mMasternode.getFriendlyname());

                if (mScanHandler.removeSceneMapFromCentralRepo(mMasterIP)) {
                    LibreLogger.d("this", "Release Scene Removed From Central Repo " + mMasterIP);
                }
                Message msg1 = new Message();
                Bundle b1 = new Bundle();

                msg1.what = DeviceMasterSlaveFreeConstants.ACTION_TO_FREE_DEVICE_INITATED;
                b1.putString("ipAddress", mMasterIP);
                msg1.setData(b1);
                mManageDevicesHandler.sendMessage(msg1);
                StartLSSDPScanAfterRelease();

                if (!mActivityName.equals("NowPlayingActivity")) {
                    Intent intent = new Intent(NewManageDevices.this, CreateNewScene.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
                *//*commented intentionally*//*
//                finish();

                *//*this is added have to change it*//*

                if (mScanHandler.getSceneObjectFromCentralRepo().size() <= 0) {
                    Intent intent = new Intent(NewManageDevices.this, PlayNewActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Intent intent = new Intent(NewManageDevices.this, ActiveScenesListActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
            }return true;*/


            case R.id.ReleaseScene:
                if ((NewManageDevices.this.isFinishing())) {
                    return true;
                }
                final LSSDPNodes mMasternode = mScanHandler.getLSSDPNodeFromCentralDB(mMasterIP);
                if(mMasternode.getCurrentSource()== Constants.GCAST_SOURCE
                        && mMasternode.getmPlayStatus() == SceneObject.CURRENTLY_PLAYING){
                    LibreError error = new LibreError(mMasternode.getFriendlyname(), Constants.SPEAKER_IS_CASTING +
                            " Release Scene is Not Allowed ");
       //             showErrorMessage(error);
                    return true;
                }
/*.setTitle(getString(R.string.deviceNameChanging)*/
                new AlertDialog.Builder(NewManageDevices.this)

                        .setMessage(getString(R.string.releaseSceneMsg))
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                final LSSDPNodes mMasternode = mScanHandler.getLSSDPNodeFromCentralDB(mMasterIP);
                                if (mMasternode == null) {
                                    Toast.makeText(NewManageDevices.this, getString(R.string.masterNotAvailable) + getString(R.string.pleaseRefresh), Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                ArrayList<LSSDPNodes> mSlaveListForMyMaster = mScanHandler.getSlaveListForMasterIp(mMasterIP,
                                        mScanHandler.getconnectedSSIDname(getApplicationContext()));

                                for (LSSDPNodes mSlaveNode : mSlaveListForMyMaster) {
                                    LibreLogger.d("this", "Release Scene Slave Ip List" + mSlaveNode.getFriendlyname());
                                    LUCIControl luciControl = new LUCIControl(mSlaveNode.getIP());
                                    /*Sending Asynchronous Registration*/
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


                                Message msg = new Message();
                                Bundle b = new Bundle();
                                msg.what = DeviceMasterSlaveFreeConstants.ACTION_TO_FREE_DEVICE_INITATED;
                                b.putString("ipAddress", mMasterIP);
                                msg.setData(b);
                                mManageDevicesHandler.sendMessage(msg);



                                 /*timeout message for release this scene*/
                                Message timeOutMessage = new Message();
                                timeOutMessage.what = DeviceMasterSlaveFreeConstants.RELEASE_ALL_TIMEOUT;
                                /*sending master ip address*/
                                timeOutMessage.setData(b);
                                mManageDevicesHandler.sendMessageDelayed(timeOutMessage, 7000);


                                StartLSSDPScanAfterRelease();

//                                if (!mActivityName.equals("NowPlayingActivity")) {
//                                    Intent intent = new Intent(NewManageDevices.this, CreateNewScene.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                                    startActivity(intent);
//                                }
//
//                                /* this is added have to change it*/
//
//                                if (mScanHandler.getSceneObjectFromCentralRepo().size() <= 0) {
//                                    Intent intent = new Intent(NewManageDevices.this, PlayNewActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                                    startActivity(intent);
//                                    finish();
//                                } else {
//                                    Intent intent = new Intent(NewManageDevices.this, ActiveScenesListActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                                    startActivity(intent);
//                                    finish();
//                                }
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
//                                Toast.makeText(getApplicationContext(), "Device state Not Changed", Toast.LENGTH_SHORT).show();
                                dialog.cancel();
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();

                //Checking For OtherM
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    public void handleSlaveSuccessfullMessageBox(String stateChangedDeviceipAddress, String zoneIdOrConCureentIdOfStateChangingDevice) {




        /* important to avoid the duplication in the active scenes screen*/
        if (mScanHandler.isIpAvailableInCentralSceneRepo(stateChangedDeviceipAddress)) {
            mScanHandler.removeSceneMapFromCentralRepo(stateChangedDeviceipAddress);
        }


        LSSDPNodes mMasterNode = mScanHandler.getLSSDPNodeFromCentralDB(mMasterIP);
        Log.e("Karuna " ,mMasterNode.getNetworkMode());
        if(mMasterNode.getNetworkMode().equalsIgnoreCase("WLAN") == false){
            mManageDeviceAdapter.mWifiMode = SA_MODE;
        }else{
            mManageDeviceAdapter.mWifiMode =  HN_MODE;
        }
        Log.e("Karuna 1" ,mManageDeviceAdapter.mWifiMode+"");
        switch (mManageDeviceAdapter.mWifiMode) {

            case ScanningHandler.SA_MODE: {

                ArrayList<LSSDPNodes> mSlaveList = mScanHandler.getSlaveListForMasterIp(mMasterIP, ScanningHandler.SA_MODE);
                if ((mMasterNode != null) && (mMasterNode.getZoneID().equals(zoneIdOrConCureentIdOfStateChangingDevice)
                        && mSlaveList.size()==0 && LibreApplication.haveShowPropritaryGooglePopup==false
                        && LSSDPNodeDB.isLS9ExistsInTheNetwork())) {
                        showPropritaryGooglePopupDialog();
                    }

                UpdateLSSDPNodeListWithStateAndConcurentId(stateChangedDeviceipAddress, "Slave", zoneIdOrConCureentIdOfStateChangingDevice);
//                addSlavesAndFreeDeviceFromCentralDBToAdapter(mMasterIP);
                addSlavesAndFreeDeviceFromCentralDBToAdapter(mMasterIP, stateChangedDeviceipAddress);
                Log.e("Karuna 2" ,mMasterNode.getZoneID()+"" + zoneIdOrConCureentIdOfStateChangingDevice);
                if ((mMasterNode != null) && (!mMasterNode.getZoneID().equals(zoneIdOrConCureentIdOfStateChangingDevice))) {
                    DeviceData mSlaveDeviceData = mManageDeviceAdapter.getSceneObjectFromAdapter(stateChangedDeviceipAddress);
                    if (mSlaveDeviceData != null) {
                        mManageDeviceAdapter.remove(mSlaveDeviceData);
                    }
                }
                else{

                }
                /*commenting to sync between small progress and row background*/
//                mManageDeviceAdapter.notifyDataSetChanged();
            }
            break;

            case ScanningHandler.HN_MODE:/* we cant compare the concurent SSID in Manage Devices*/ {

                ArrayList<LSSDPNodes> mSlaveList = mScanHandler.getSlaveListForMasterIp(mMasterIP, ScanningHandler.HN_MODE);
                if ((mMasterNode != null) && (mMasterNode.getcSSID().equals(zoneIdOrConCureentIdOfStateChangingDevice) && mSlaveList.size()==0
                        && LibreApplication.haveShowPropritaryGooglePopup==false
                  && LSSDPNodeDB.isLS9ExistsInTheNetwork())) {
                    if(mMasterNode.getgCastVerision() != null)
                        showPropritaryGooglePopupDialog();
                }


                UpdateLSSDPNodeListWithStateAndConcurentId(stateChangedDeviceipAddress, "Slave", zoneIdOrConCureentIdOfStateChangingDevice);
//                addSlavesAndFreeDeviceFromCentralDBToAdapter(mMasterIP);
                addSlavesAndFreeDeviceFromCentralDBToAdapter(mMasterIP, stateChangedDeviceipAddress);
                if ((mMasterNode != null) && (!mMasterNode.getcSSID().equals(zoneIdOrConCureentIdOfStateChangingDevice))) {
                    DeviceData mSlaveDeviceData = mManageDeviceAdapter.getSceneObjectFromAdapter(stateChangedDeviceipAddress);
                    if (mSlaveDeviceData != null) {
                        mManageDeviceAdapter.remove(mSlaveDeviceData);
                    }
                }
                /*commenting to sync between small progress and row background*/
//                mManageDeviceAdapter.notifyDataSetChanged();
            }
            break;
        }

        removeTimeOutTriggerForIpaddress(stateChangedDeviceipAddress);
        /*removing */
        Message timeOutMessage = new Message();
        timeOutMessage.what = DeviceMasterSlaveFreeConstants.ACTION_TO_INDICATE_TIMEOUT;
        mManageDevicesHandler.removeMessages(timeOutMessage.what);

        closeLoader();

        if (stateChangedDeviceipAddress.contains(mMasterIP)) {

            Intent intent = new Intent(NewManageDevices.this, ActiveScenesListActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();

        }

    }

    private void showPropritaryGooglePopupDialog() {

        /* First check if this is already accepted by the end User */

                                                                                                                                                                                                                                                                                   /* Google cast settings*/
        SharedPreferences sharedPreferences = getApplicationContext()
                .getSharedPreferences(Constants.SHOWN_GOOGLE_TOS, Context.MODE_PRIVATE);
        String shownGoogle = sharedPreferences.getString(Constants.PROPRITARY, null);
        LibreApplication.haveShowPropritaryGooglePopup=true;

        if (shownGoogle==null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(NewManageDevices.this);
            builder.setCancelable(false)
                    .setMessage(getString(R.string.cannotPlayCast))
                    .setNegativeButton(getString(R.string.dontShowAgain), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                            SharedPreferences sharedpreferences = getApplicationContext()
                                    .getSharedPreferences(Constants.SHOWN_GOOGLE_TOS, Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedpreferences.edit();
                            editor.putString(Constants.PROPRITARY, "Yes");
                            editor.commit();



                        }
                    })
                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                        }

                    });

            AlertDialog alert = builder.create();
            alert.show();
        }
        else {
            /* Already user has accepted and hence no need to worry */
            return;
        }

    }

    public void handleSlaveSuccessfullMessageBox(String ipAddress) {

        /* important to avoid the duplication in the active scenes screen*/
        if (mScanHandler.isIpAvailableInCentralSceneRepo(ipAddress)) {
            mScanHandler.removeSceneMapFromCentralRepo(ipAddress);
        }


        LSSDPNodes mNewSlaveNode = mScanHandler.getLSSDPNodeFromCentralDB(ipAddress);
        LSSDPNodes mMasterNode = mScanHandler.getLSSDPNodeFromCentralDB(mMasterIP);


        Message successMsg = new Message();

        Bundle b = new Bundle();
        b.putString("ipAddress", ipAddress);
        successMsg.setData(b);

        successMsg.obj = mScanHandler.getLSSDPNodeFromCentralDB(ipAddress);

        switch (mManageDeviceAdapter.mWifiMode) {

            case ScanningHandler.SA_MODE:
                /* This will check for the curreent master only a new slave is added and from Freee Device it added as slave to other zone
                * */
                if (!(mMasterNode.getZoneID().equalsIgnoreCase(mNewSlaveNode.getZoneID()) /*comparing Master zone id & newSlaveNode Zoneid*/
                        || ((mNewSlaveNode.getZoneID().contains("3000")) &&
                        (mNewSlaveNode.getDeviceState().equalsIgnoreCase("F"))))) {  /* Free device and zoneid is 3000 */

                    LibreLogger.d(this, mMasterNode.getZoneID() + "::mSlaveNode Zoneid ::" + mNewSlaveNode.getZoneID());
                    mManageDeviceAdapter.remove(mManageDeviceAdapter.getSceneObjectFromAdapter(mNewSlaveNode.getIP()));
                    mManageDeviceAdapter.notifyDataSetChanged();
                    StartLSSDPScanAfterRelease();
                    updateMenuDetails();

                } else {
                    Message timeOutMessage = new Message();
                    timeOutMessage.what = DeviceMasterSlaveFreeConstants.ACTION_TO_INDICATE_TIMEOUT;
                    mManageDevicesHandler.removeMessages(timeOutMessage.what);
                    closeLoader();
                    UpdateLSSDPNodeList(ipAddress, "Slave");
                    mManageDeviceAdapter.notifyDataSetChanged();
                }
                break;
            case ScanningHandler.HN_MODE:/* we cant compare the concurent SSID in Manage Devices*/
                Message timeOutMessage = new Message();
                timeOutMessage.what = DeviceMasterSlaveFreeConstants.ACTION_TO_INDICATE_TIMEOUT;
                mManageDevicesHandler.removeMessages(timeOutMessage.what);
                closeLoader();
                UpdateLSSDPNodeList(ipAddress, "Slave");
                mManageDeviceAdapter.notifyDataSetChanged();
                break;
        }
    }

    public void handleMasterSuccessfullMessageBox(String ipAddress, String zoneIdOrConcurrnetid) {
        /* This case will occur when we are in manage device Screen it should discard the new created master*/
        UpdateLSSDPNodeListWithStateAndConcurentId(ipAddress, "Master", zoneIdOrConcurrnetid);
        /*removing only when got 103 for other master*/
        if (!ipAddress.equalsIgnoreCase(mMasterIP)) {
            mManageDeviceAdapter.remove(mManageDeviceAdapter.getSceneObjectFromAdapter(ipAddress));
            mManageDeviceAdapter.notifyDataSetChanged();
        }
        StartLSSDPScanAfterRelease();


    }

    public void handleMasterSuccessfullMessageBox(String ipAddress) {
        /* This case will occur when we are in manage device Screen it should discard the new created master*/
        mManageDeviceAdapter.remove(mManageDeviceAdapter.getSceneObjectFromAdapter(ipAddress));
        mManageDeviceAdapter.notifyDataSetChanged();
        StartLSSDPScanAfterRelease();


    }


    public void handleFreeSuccessfullMessageBox(String ipAddress) {

            /* important to avoid the duplication in the active scenes screen*/
        if (mScanHandler.isIpAvailableInCentralSceneRepo(ipAddress)) {
            mScanHandler.removeSceneMapFromCentralRepo(ipAddress);
        }


        if (mManageDeviceAdapter.getSceneObjectFromAdapter(ipAddress) != null) {
            Message timeOutMessage = new Message();
            timeOutMessage.what = DeviceMasterSlaveFreeConstants.ACTION_TO_INDICATE_TIMEOUT;
            mManageDevicesHandler.removeMessages(timeOutMessage.what);
            closeLoader();


            if (mMasterIP.contains(ipAddress)) {
                finishActivity(ipAddress);
            }
            LSSDPNodes mNewFreeNode = mScanHandler.getLSSDPNodeFromCentralDB(ipAddress);
            if (mNewFreeNode != null)
                UpdateLSSDPNodeList(mNewFreeNode.getIP(), "Free");
            mManageDeviceAdapter.notifyDataSetChanged();
            ;
            /* If a free or Master or Slave of Specific Master it will be in the List then it should be noticced*/

         /*   Message successMsg = new Message();
            Bundle b = new Bundle();
            b.putString("ipAddress", ipAddress);
            successMsg.setData(b);
            successMsg.obj = NmScanHandler.getLSSDPNodeFromCentralDB(ipAddress);
            successMsg.what = DeviceMasterSlaveFreeConstants.LUCI_SUCCESS_FREE_COMMAND;
            mManageDevicesHandler.sendMessage(successMsg);*/
        } else {
            /* If a Slave of another Master made it free by another app then that have to be add to Manage Device Screen as
            * Free Device*/
            LSSDPNodes mNewFreeNode = mScanHandler.getLSSDPNodeFromCentralDB(ipAddress);
            if (mNewFreeNode != null) {
                DeviceData data = new DeviceData(mNewFreeNode.getFriendlyname(), mNewFreeNode.getIP());
                mManageDeviceAdapter.add(data);
                mManageDeviceAdapter.notifyDataSetChanged();
                UpdateLSSDPNodeList(ipAddress, "Free");
            }
            StartLSSDPScanAfterRelease();
        }
    }

    public void handleFreeSuccessfullMessageBox(String ipAddress, String zoneidOrConCurrentSSid) {

            /* important to avoid the duplication in the active scenes screen*/
        if (mScanHandler.isIpAvailableInCentralSceneRepo(ipAddress)) {
            mScanHandler.removeSceneMapFromCentralRepo(ipAddress);
        }


        removeTimeOutTriggerForIpaddress(ipAddress);

        if (mManageDeviceAdapter.getSceneObjectFromAdapter(ipAddress) != null) {
            Message timeOutMessage = new Message();
            timeOutMessage.what = DeviceMasterSlaveFreeConstants.ACTION_TO_INDICATE_TIMEOUT;
            mManageDevicesHandler.removeMessages(timeOutMessage.what);
            closeLoader();


            if (mMasterIP.contains(ipAddress)) {
                finishActivity(ipAddress);
            }
            LSSDPNodes mNewFreeNode = mScanHandler.getLSSDPNodeFromCentralDB(ipAddress);
            if (mNewFreeNode != null)
                UpdateLSSDPNodeListWithStateAndConcurentId(mNewFreeNode.getIP(), "Free", zoneidOrConCurrentSSid);
            mManageDeviceAdapter.notifyDataSetChanged();
            ;
            /* If a free or Master or Slave of Specific Master it will be in the List then it should be noticced*/

         /*   Message successMsg = new Message();
            Bundle b = new Bundle();
            b.putString("ipAddress", ipAddress);
            successMsg.setData(b);
            successMsg.obj = NmScanHandler.getLSSDPNodeFromCentralDB(ipAddress);
            successMsg.what = DeviceMasterSlaveFreeConstants.LUCI_SUCCESS_FREE_COMMAND;
            mManageDevicesHandler.sendMessage(successMsg);*/
        } else {
            /* If a Slave of another Master made it free by another app then that have to be add to Manage Device Screen as
            * Free Device*/
            LSSDPNodes mNewFreeNode = mScanHandler.getLSSDPNodeFromCentralDB(ipAddress);
            if (mNewFreeNode != null) {
                /* Updating the database before adding a new device to addmore speakers screen */
                UpdateLSSDPNodeList(ipAddress, "Free");
                DeviceData data = new DeviceData(mNewFreeNode.getFriendlyname(), mNewFreeNode.getIP());
                mManageDeviceAdapter.add(data);
                mManageDeviceAdapter.notifyDataSetChanged();
            }
            StartLSSDPScanAfterRelease();
        }
    }
    private void enableEditSceneNameButton(){

        if(mManageDeviceAdapter.getCountSlaves()>1){
            mSceneNameImageButton.setVisibility(View.VISIBLE);
        }else{
            mSceneNameImageButton.setVisibility(View.INVISIBLE);

        }

    }
    @Override
    public void messageRecieved(NettyData dataRecived) {
        LibreLogger.d(this, "New message appeared for the device " + dataRecived.getRemotedeviceIp());
        DeviceData deviceData = mManageDeviceAdapter.getSceneObjectFromAdapter(dataRecived.getRemotedeviceIp());

        LUCIPacket packet = new LUCIPacket(dataRecived.getMessage());
        LibreLogger.d(this, "ReceivedCommand Status " + packet.getCommandStatus() + "For the command " + packet.getCommand());
        switch (packet.getCommand()) {
            case MIDCONST.MID_DEVNAME:{
                LSSDPNodeDB mNodeDB  = LSSDPNodeDB.getInstance();
                LSSDPNodes mNode = mNodeDB.getTheNodeBasedOnTheIpAddress(dataRecived.getRemotedeviceIp());
                if(mNode!=null){
                    /*if command status is one Ignore it */
                     /* if Command Type 1 , then Scene Name information will be come in the same packet
if command type 2 and command status is 1 , then data will be empty., at that time we should not update the value .*/
                    if(packet.getCommandStatus()==1
                            && packet.getCommandType() == 2)
                        return;
                    String message = new String(packet.getpayload());
                    if(message!=null) {
                        mNode.setFriendlyname(message);
                        DeviceData mData = mManageDeviceAdapter.getSceneObjectFromAdapter(dataRecived.getRemotedeviceIp());
                        if(mData!=null)
                            mData.setDeviceName(message);
                        mManageDeviceAdapter.notifyDataSetChanged();
                    }
                }

            }
            break;
            case MIDCONST.MID_SCENE_NAME: {
                    /* This message box indicates the Scene Name*//*
*/
                    /*if command status is one Ignore it */
                     /* if Command Type 1 , then Scene Name information will be come in the same packet
                if command type 2 and command status is 1 , then data will be empty., at that time we should not update the value .*/
                 if(packet.getCommandStatus()==1
                                && packet.getCommandType()==2)
                            return;
                /* Error in Scene Name Set*/
                if(packet.getCommandStatus() == 2){
                    LibreError error = new LibreError(dataRecived.getRemotedeviceIp(), Constants.SCENE_NAME_CANT_CHANGE);
                    showErrorMessage(error);
                    enableEditSceneNameButton();
                    ArrayList<LSSDPNodes> mSlaveList = mScanHandler.
                            getSlaveListForMasterIp(mMasterIP,mScanHandler.getconnectedSSIDname(getApplicationContext()));
                    for(LSSDPNodes mSlaveNode : mSlaveList) {
                        removeTheDeviceFromRepo(mSlaveNode.getIP());
                    }
                    return;
                }
                String message = new String(packet.getpayload());
                LibreLogger.d(this, "Received Message is " + message);
                SceneObject sceneObjectFromCentralRepo = mScanHandler.getSceneObjectFromCentralRepo(dataRecived.getRemotedeviceIp());
                if (sceneObjectFromCentralRepo != null) {
                    sceneObjectFromCentralRepo.setSceneName((message));
                    if (mMasterIP.equals(dataRecived.getRemotedeviceIp()) && !message.equals("")) {
                        mSceneNameEditText.setText(message);
                    }
                    mScanHandler.putSceneObjectToCentralRepo(dataRecived.getRemotedeviceIp(), sceneObjectFromCentralRepo);
                }
                enableEditSceneNameButton();
/*

                if(dataRecived.getRemotedeviceIp().contains(mMasterIP)) {
                    mSceneNameEditText.setText(message);
                }
*/


            }
            break;
            case 64: {

                String message = new String(packet.getpayload());
                if (deviceData != null) {
                    try {
                        int duration = Integer.parseInt(message);
                        deviceData.setVolumeValueInPercentage(duration);
                        LibreLogger.d(this, "Recieved the current volume to be" + deviceData.getVolumeValueInPercentage());

                        SceneObject sceneObjectFromCentralRepo = mScanHandler.getSceneObjectFromCentralRepo(dataRecived.getRemotedeviceIp());
                        if (sceneObjectFromCentralRepo != null) {
                            sceneObjectFromCentralRepo.setVolumeValueInPercentage(duration);
                            mScanHandler.putSceneObjectToCentralRepo(dataRecived.getRemotedeviceIp(), sceneObjectFromCentralRepo);
                        }

                        mManageDeviceAdapter.add(deviceData);
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
              /*  Bundle b = new Bundle();
                b.putString("ipAddress", dataRecived.getRemotedeviceIp());
                successMsg.setData(b);

                successMsg.obj = mScanHandler.getLSSDPNodeFromCentralDB(dataRecived.getRemotedeviceIp());*/

                if (message.contains("FREE")) {
                    String substring = message.substring(message.lastIndexOf(",") + 1);

                    handleFreeSuccessfullMessageBox(dataRecived.getRemotedeviceIp(), substring);
                   /* successMsg.what = DeviceMasterSlaveFreeConstants.LUCI_SUCCESS_FREE_COMMAND;
                    mManageDevicesHandler.sendMessage(successMsg);*/
                } else if (message.contains("SLAVE")) {



                    /* Now device is giving us the zoneid/concurrentssid also so we will consider the same*/
                    String substring = message.substring(message.lastIndexOf(",") + 1);

                    handleSlaveSuccessfullMessageBox(dataRecived.getRemotedeviceIp(), substring);

                    //handleSlaveSuccessfullMessageBox(dataRecived.getRemotedeviceIp());
                    /*
                    LSSDPNodes mMasterNode = mScanHandler.getLSSDPNodeFromCentralDB(mMasterIP);
                    LSSDPNodes mSlaveNode = mScanHandler.getLSSDPNodeFromCentralDB(dataRecived.getRemotedeviceIp());
                    if (mMasterNode != null && mSlaveNode != null) {

                        if ((mManageDeviceAdapter.mWifiMode == mScanHandler.HN_MODE)) {
                            successMsg.what = DeviceMasterSlaveFreeConstants.LUCI_SUCCESS_SLAVE_COMMAND;
                            mManageDevicesHandler.sendMessage(successMsg);

                        } else if ((mManageDeviceAdapter.mWifiMode == mScanHandler.SA_MODE) &&
                                (mMasterNode.getZoneID().equalsIgnoreCase(mSlaveNode.getZoneID()))) {
                            successMsg.what = DeviceMasterSlaveFreeConstants.LUCI_SUCCESS_SLAVE_COMMAND;
                            mManageDevicesHandler.sendMessage(successMsg);
                        } else if ((mManageDeviceAdapter.mWifiMode == mScanHandler.SA_MODE) &&
                                (mSlaveNode.getZoneID().contains("3000")) &&
                                (mSlaveNode.getDeviceState().equalsIgnoreCase("F"))) {
                                /* this if loop is added to make sure that device side issue of new slave coming up with the 3000 as its zoneid in SA mode*/
                       /*     successMsg.what = DeviceMasterSlaveFreeConstants.LUCI_SUCCESS_SLAVE_COMMAND;
                         /*   mManageDevicesHandler.sendMessage(successMsg);
                        } else {/*M-search is called here as we seen to having old information of a node in our DB*/
                           /* LibreLogger.d(this, mMasterNode.getZoneID() + "::mSlaveNode Zoneid ::" + mSlaveNode.getZoneID());
                            mManageDeviceAdapter.remove(mManageDeviceAdapter.getSceneObjectFromAdapter(mSlaveNode.getIP()));
                            mManageDeviceAdapter.notifyDataSetChanged();
                            StartLSSDPScanAfterRelease();
                            updateMenuDetails();
                        }
                    }*/
                } else if (message.contains("MASTER")) { /* Commenting Below code for showing Two Masters in ManageDevcies*/
                    //   successMsg.what = DeviceMasterSlaveFreeConstants.LUCI_SUCCESS_MASTER_COMMAND;
                    //mManageDevicesHandler.sendMessage(successMsg);
                    /*mManageDeviceAdapter.remove(mManageDeviceAdapter.getSceneObjectFromAdapter(dataRecived.getRemotedeviceIp()));
                    mManageDeviceAdapter.notifyDataSetChanged();
                    StartLSSDPScanAfterRelease();*/

                    /* Now device is giving us the zoneid/concurrentssid also so we will consider the same*/
                    String substring = message.substring(message.lastIndexOf(",") + 1);
                    handleMasterSuccessfullMessageBox(dataRecived.getRemotedeviceIp(), substring);
                }
                enableEditSceneNameButton();
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
            case MIDCONST.MID_CURRENT_PLAY_STATE:
            case MIDCONST.MID_CURRENT_SOURCE:
                mManageDeviceAdapter.notifyDataSetChanged();
                break;
            case 0:
                LUCIControl mLuci = new LUCIControl(dataRecived.getRemotedeviceIp());
                mLuci.sendAsynchronousCommand();
                break;
        }
    }

    /* This function will trigger the timeoutHandler for a ipaddress */
    public void triggerTimeOutHandlerForIpAddress(String ipAddress) {
        Message timeOutMessage = new Message();
        timeOutMessage.obj = ipAddress;
        timeOutMessage.what = DeviceMasterSlaveFreeConstants.ACTION_TO_INDICATE_TIMEOUT;
        mManageDevicesHandler.sendMessageDelayed(timeOutMessage, 35000);


        LSSDPNodes mToBeUpdateNode = mScanHandler.getLSSDPNodeFromCentralDB(ipAddress);
        if (mToBeUpdateNode != null) {
            mToBeUpdateNode.setCurrentState(LSSDPNodes.STATE_CHANGE_STAGE.CHANGE_INITIATED);
        }
        mManageDeviceAdapter.notifyDataSetChanged();
    }

    public void removeTimeOutTriggerForIpaddress(String ipAddress) {
        mManageDevicesHandler.removeMessages(DeviceMasterSlaveFreeConstants.ACTION_TO_INDICATE_TIMEOUT, ipAddress);
        LSSDPNodes mToBeUpdateNode = mScanHandler.getLSSDPNodeFromCentralDB(ipAddress);

        Log.d("KhajanDeviceRemoveTime", ipAddress);


        if (mToBeUpdateNode != null) {
            mToBeUpdateNode.setCurrentState(LSSDPNodes.STATE_CHANGE_STAGE.CHANGE_SUCCESS);
        }
        mManageDeviceAdapter.notifyDataSetChanged();
    }


    Handler mManageDevicesHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Message timeOutMessage = new Message();
            timeOutMessage.what = DeviceMasterSlaveFreeConstants.ACTION_TO_INDICATE_TIMEOUT;
            switch (msg.what) {
                case DeviceMasterSlaveFreeConstants.LUCI_SEND_MASTER_COMMAND: {
                    //showLoader("Master");
                    String ipaddress = msg.getData().getString("ipAddress");
                    triggerTimeOutHandlerForIpAddress(ipaddress);
                }
                break;
                case DeviceMasterSlaveFreeConstants.ACTION_TO_FREE_DEVICE_INITATED: {
                    //showLoader("FREE");
                    String ipaddress = msg.getData().getString("ipAddress");
                    triggerTimeOutHandlerForIpAddress(ipaddress);
                }
                break;
                case DeviceMasterSlaveFreeConstants.ACTION_TO_CREATE_SLAVE_INITIATED: {
                    //showLoader("SLAVE");
                    String ipaddress = msg.getData().getString("ipAddress");
                    triggerTimeOutHandlerForIpAddress(ipaddress);

                }
                break;
                case DeviceMasterSlaveFreeConstants.LUCI_SUCCESS_MASTER_COMMAND: {

                    Message timeOutMessage1 = new Message();
                    timeOutMessage1.what = DeviceMasterSlaveFreeConstants.ACTION_TO_INDICATE_TIMEOUT;
                    mManageDevicesHandler.removeMessages(timeOutMessage1.what);

                    closeLoader();
                    Bundle b = new Bundle();
                    b = msg.getData();
                    String ipaddress = b.getString("ipAddress");

                    triggerTimeOutHandlerForIpAddress(ipaddress);


                    UpdateLSSDPNodeList(ipaddress, "Master");
                    //  mScanHandler.updateSlaveToMasterHashMap();
                    mManageDeviceAdapter.notifyDataSetChanged();
                }
                break;
                case DeviceMasterSlaveFreeConstants.LUCI_SUCCESS_SLAVE_COMMAND: {

                    Message timeOutMessage2 = new Message();
                    timeOutMessage2.what = DeviceMasterSlaveFreeConstants.ACTION_TO_INDICATE_TIMEOUT;
                    mManageDevicesHandler.removeMessages(timeOutMessage2.what);

                    closeLoader();
                    Bundle b = new Bundle();
                    b = msg.getData();
                    String ipaddress = b.getString("ipAddress");

                    removeTimeOutTriggerForIpaddress(ipaddress);
                    UpdateLSSDPNodeList(ipaddress, "Slave");

                    mManageDeviceAdapter.notifyDataSetChanged();
                }
                break;
                case DeviceMasterSlaveFreeConstants.LUCI_SUCCESS_FREE_COMMAND: {

                    Message timeOutMessage3 = new Message();
                    timeOutMessage3.what = DeviceMasterSlaveFreeConstants.ACTION_TO_INDICATE_TIMEOUT;
                    mManageDevicesHandler.removeMessages(timeOutMessage3.what);


                    Bundle b = new Bundle();
                    b = msg.getData();
                    String ipaddress = b.getString("ipAddress");
                    closeLoader();

                    LSSDPNodes node = mScanHandler.getLSSDPNodeFromCentralDB(ipaddress);

                    if ((node != null) && node.getDeviceState().equals("M")) {
                        if (mScanHandler.isIpAvailableInCentralSceneRepo(ipaddress)) {
                            mScanHandler.removeSceneMapFromCentralRepo(ipaddress);
                        }
                        if (mActivityName.contains("CreateNewScene")) {
                            if (node != null)
                                UpdateLSSDPNodeList(node.getIP(), "Free");


                            Intent intent = new Intent(NewManageDevices.this, CreateNewScene.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            for (LSSDPNodes mSlaveNode : mScanHandler.getSlaveListForMasterIp(node.getIP(),
                                    mManageDeviceAdapter.mWifiMode)) {
                                UpdateLSSDPNodeList(mSlaveNode.getIP(), "Free");
                                removeTimeOutTriggerForIpaddress(node.getIP());
                                node.setCurrentState(LSSDPNodes.STATE_CHANGE_STAGE.NO_TRACKING);

                            }
                            startActivity(intent);
                            finish();
                        } else if (mActivityName.contains("NowPlayingActivity")) {
                            LSSDPNodeDB mNodeDB = LSSDPNodeDB.getInstance();
                            boolean mMasterFound = false;
                            if (node != null)
                                UpdateLSSDPNodeList(node.getIP(), "Free");
                            ArrayList<LSSDPNodes> clonedNodeDB = mNodeDB.GetDB();
                            for (LSSDPNodes mNode : clonedNodeDB) {
                                if (mNode.getDeviceState().contains("M") && !mNode.getIP().contains(ipaddress)) {
                                    // Toast.makeText(getApplicationContext(), "Master Found :" + mNode.getFriendlyname(), Toast.LENGTH_SHORT).show();
                                    mMasterFound = true;
                                }
                                if (mMasterFound) {
                                    break;
                                }
                            }
                            if (!mMasterFound) {
                                Intent intent = new Intent(NewManageDevices.this, PlayNewActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);

                                for (LSSDPNodes mSlaveNode : mScanHandler.getSlaveListForMasterIp(node.getIP(),
                                        mManageDeviceAdapter.mWifiMode)) {
                                    UpdateLSSDPNodeList(mSlaveNode.getIP(), "Free");
                                }
                                //       StartLSSDPScan();
                                finish();
                            } else {
                                Intent intent = new Intent(NewManageDevices.this, ActiveScenesListActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                for (LSSDPNodes mSlaveNode : mScanHandler.getSlaveListForMasterIp(node.getIP(),
                                        mManageDeviceAdapter.mWifiMode)) {
                                    UpdateLSSDPNodeList(mSlaveNode.getIP(), "Free");
                                    removeTimeOutTriggerForIpaddress(ipaddress);
                                }
                                //    StartLSSDPScan();
                                finish();

                            }
                        }
                    } else {
                        if (node != null)
                            UpdateLSSDPNodeList(node.getIP(), "Free");
                    }

                    mManageDeviceAdapter.notifyDataSetChanged();
                }
                break;
                case DeviceMasterSlaveFreeConstants.ACTION_TO_INDICATE_TIMEOUT: {
                    mManageDevicesHandler.removeMessages(timeOutMessage.what);
                    closeLoader();
                    updateMenuDetails();

                    String ipaddrress = (String) msg.obj;
                    LSSDPNodes node = mScanHandler.getLSSDPNodeFromCentralDB(ipaddrress);
                    if (node != null) {
                        node.setCurrentState(LSSDPNodes.STATE_CHANGE_STAGE.CHANGE_FAIL);
                        mManageDeviceAdapter.notifyDataSetChanged();
                    }

                    /*
                    if (!(NewManageDevices.this.isFinishing())) {
                        new AlertDialog.Builder(NewManageDevices.this)
                                .setTitle("Device State Changing")
                                .setMessage("FAILED")
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                    }
                                }).setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                    }
*/
                }
                break;

                case DeviceMasterSlaveFreeConstants.JOIN_ALL_TIMEOUT:
                    closeLoader();
                    break;
                case DeviceMasterSlaveFreeConstants.RELEASE_ALL_TIMEOUT:

                    Bundle b = new Bundle();
                    b = msg.getData();
                    String ipaddress = b.getString("ipAddress");
                    closeLoader();

                    finishActivity(ipaddress);


//                    /* this is added have to change it*/
//                    if (mScanHandler.getSceneObjectFromCentralRepo().size() <= 0) {
//                        Intent intent = new Intent(NewManageDevices.this, PlayNewActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                        startActivity(intent);
//                        finish();
//                    } else {
//                        Intent intent = new Intent(NewManageDevices.this, ActiveScenesListActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                        startActivity(intent);
//                        finish();
//                    }


                    break;
            }

        }
    };

    public ProgressDialog mProgressDialog;

    public void closeLoader() {

        if (NewManageDevices.this.isFinishing())
            return;
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            Log.e("LuciControl", "progress Dialog Closed");
            mProgressDialog.setCancelable(true);
            mProgressDialog.dismiss();
            mProgressDialog.cancel();
            mManageDeviceAdapter.notifyDataSetChanged();
        }

    }

    public void showLoader(String msg) {

        if (NewManageDevices.this.isFinishing())
            return;


        if (mProgressDialog == null) {
            Log.e("LUCIControl", "Null");
            mProgressDialog = ProgressDialog.show(NewManageDevices.this, getString(R.string.notice), getString(R.string.changing) + msg + "...", true, true, null);
        }
        mProgressDialog.setCancelable(false);

        if (!mProgressDialog.isShowing()) {
            if (!(NewManageDevices.this.isFinishing())) {
                mProgressDialog = ProgressDialog.show(NewManageDevices.this, getString(R.string.notice), getString(R.string.changing) + msg + "...", true, true, null);
            }
        }


    }

    /*sending dmr stop command from application*/
    private void stopDMRPlayback(String currentIpaddress) {
        try {
            LibreLogger.d(this, "Going to Remove the DMR Playback");
            RemoteDevice renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(currentIpaddress);
                    /* For the New DMR Implementation , whenever a freeing master
                    * we are Stopping the Playback */
            if (renderingDevice != null) {
                String renderingUDN = renderingDevice.getIdentity().getUdn().toString();
                PlaybackHelper playbackHelper = LibreApplication.PLAYBACK_HELPER_MAP.get(renderingUDN);
                ScanningHandler mScanHandler = ScanningHandler.getInstance();
                SceneObject currentSceneObject = mScanHandler.getSceneObjectFromCentralRepo(currentIpaddress);
                try {
                    if ( playbackHelper!=null   && renderingDevice != null
                            && currentSceneObject != null
                            && currentSceneObject.getPlayUrl() != null
                            && !currentSceneObject.getPlayUrl().equalsIgnoreCase("")
                            && currentSceneObject.getPlayUrl().contains(LibreApplication.LOCAL_IP)
                            && currentSceneObject.getPlayUrl().contains(ContentTree.AUDIO_PREFIX)

                            ) {

                        playbackHelper.StopPlayback();
                    }
                } catch (Exception e) {

                    LibreLogger.d(this, "Handling the exception while sending the stop command ");
                }
                LibreApplication.PLAYBACK_HELPER_MAP.remove(renderingUDN);
            }
        } catch (Exception e) {
            LibreLogger.d(this, "Removing the DMR Playback is Exception");
        }
    }

    private void finishActivity(String ipaddress) {
        LSSDPNodes node = mScanHandler.getLSSDPNodeFromCentralDB(ipaddress);

        /*stopping DMR playback*/
        stopDMRPlayback(ipaddress);

        if (mScanHandler.isIpAvailableInCentralSceneRepo(ipaddress))
            mScanHandler.removeSceneMapFromCentralRepo(ipaddress);


        if (node != null)
            UpdateLSSDPNodeList(node.getIP(), "Free");


        if (mScanHandler.getSceneObjectFromCentralRepo().size() > 0) {
            Intent intent = new Intent(NewManageDevices.this, ActiveScenesListActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } else {
            Intent intent = new Intent(NewManageDevices.this, PlayNewActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }

    }

    protected void onNewIntent(Intent intent) {
        Log.d("NewRefresh-newIntent", "newIntent of NewManageDevices");
        super.onNewIntent(intent);
        setIntent(intent);

/*
        Intent myIntent = getIntent(); // gets the previously created intent
        mMasterIP = myIntent.getStringExtra("master_ip");
        mActivityName = myIntent.getStringExtra("activity_name");

        mManageDeviceAdapter = new NewManageAdapter(this,
                R.layout.manage_devices_list_item,
                mManageDevicesHandler,
                mMasterIP, mActivityName);
        listView.setAdapter(mManageDeviceAdapter);

                */
/* if the user is coming from Nowplaying screen we need to display the button as Done *//*

        if (mActivityName.contains("NowPlayingActivity")) {
            setTitle("Manage Speakers");
            nextBtn.setText("Done");
        }

        Log.d("NewRefresh", "masterip currentlt being considered is"+mMasterIP);

        registerForDeviceEvents(this);
        LibreApplication application = (LibreApplication) getApplication();
        application.getScanThread().UpdateNodes();
        addSlavesAndFreeDeviceFromCentralDBToAdapter(mMasterIP);
        mManageDeviceAdapter.notifyDataSetChanged();
*/

    }
}






