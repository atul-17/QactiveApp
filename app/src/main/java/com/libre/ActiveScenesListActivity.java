package com.libre;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cumulations.libreV2.AppUtils;
import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity;
import com.libre.LErrorHandeling.LibreError;
import com.libre.Network.NewSacActivity;
import com.libre.Scanning.Constants;
import com.libre.Scanning.ScanningHandler;
import com.libre.alexa.userpoolManager.AlexaUtils.AlexaConstants;
import com.libre.alexa.userpoolManager.MainActivity;
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
import com.libre.util.PicassoTrustCertificates;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by karunakaran on 7/22/2015.
 */
public class ActiveScenesListActivity extends CTDeviceDiscoveryActivity implements LibreDeviceInteractionListner {

    private LinearLayout footerViewsecond;
    private RelativeLayout footerView;
    static ProgressDialog mProgressDialog;
    private boolean isDoubleTap;
    ImageView img_arrow;
    public static final int RELEASE_ALL_SUCCESS = 0x75;
    public static final int RELEASE_ALL_STARTED = 0x76;
    public static final int APP_NETWROK_CHANGED = 0x77;

    public static final int RELEASE_ALL_TIMEOUT = 3000;

    public static final int PREPARATION_INIT = 0x77;
    public static final int PREPARATION_COMPLETED = 0x78;
    public static final int PREPARATION_TIMEOUT_CONST = 0x79;

    private boolean mDropAllIniatiated = false;
    String TAG = "SCeneListActivity";
    ListView SceneListView;
    ActiveSceneAdapter activeAdapter;
    ScanningHandler mScanHandler = ScanningHandler.getInstance();
    public SwipeRefreshLayout swipeRefreshLayout;
    boolean mDeviceFound = false, mMasterFound = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_scenes_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);

        LSSDPNodeDB nodeDB = LSSDPNodeDB.getInstance();
        if (nodeDB.GetDB().size() <= 0){
            startActivity(new Intent(ActiveScenesListActivity.this,ConfigureActivity.class));
            finish();
        }

        SceneListView = (ListView) findViewById(R.id.active_scene_list);
        Log.e("ActiveScenesList", "active_scene_list");
        activeAdapter = new ActiveSceneAdapter(this, R.layout.scenes_listview_layout,handler);

        SceneListView.setAdapter(activeAdapter);
        activeAdapter.clear();
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh);
        img_arrow = (ImageView) findViewById(R.id.imgView_arrow);
        footerViewsecond = (LinearLayout) findViewById(R.id.footer_second_view);
        footerView = (RelativeLayout) findViewById(R.id.footer_first);
        footerView.setOnClickListener(onclickListner);
        if (!LibreApplication.getIs3PDAEnabled()){
            findViewById(R.id.playnew_icon).setVisibility(View.GONE);
        }else{
            findViewById(R.id.playnew_icon).setVisibility(View.VISIBLE);
        }

        findViewById(R.id.playnew_icon).setOnClickListener(onclickListner);
        findViewById(R.id.favorites_icon).setOnClickListener(onclickListner);
        findViewById(R.id.config_item).setOnClickListener(onclickListner);

        findViewById(R.id.imgView_arrow).setOnClickListener(onclickListner);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                swipeRefreshLayout.setRefreshing(true);
               /* activeAdapter.clear();
                activeAdapter.notifyDataSetChanged();
                mScanHandler.clearSceneObjectsFromCentralRepo();*/

                handler.removeCallbacksAndMessages(null);

                refreshDevices();
            }
        });


    }

//    @Override
//    protected void onNewIntent(Intent intent) {
//        super.onNewIntent(intent);
//        setIntent(intent);
//        if (intent.hasExtra(SpalshScreenActivity.APP_RESTARTING)) {
//            Log.d(TAG, "onNewIntent() called with: " + "intent = [" + intent + "]");
//            swipeRefreshLayout.setRefreshing(true);
//        }
//    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    @Override
    protected void onDestroy() {
        try{
            if(mTaskHandlerForSendingMSearch.hasMessages(MSEARCH_TIMEOUT_SEARCH)) {
                mTaskHandlerForSendingMSearch.removeMessages(MSEARCH_TIMEOUT_SEARCH);
            }
            mTaskHandlerForSendingMSearch.removeCallbacks(mMyTaskRunnableForMSearch);
        }catch(Exception e){
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        /* WHy We have to Clear the Adapter in Onresume, Screen lock and Unlock*/
        activeAdapter.clear();

        /*registering for message events*/
        registerForDeviceEvents(this);

        updateFromCentralRepositryDeviceList();
//        activeAdapter.notifyDataSetChanged();


        /* on returning to ActiveScene List if we dont have master we will intiate the ssh*/

        String[] mMasterIpKeySet = mScanHandler.getSceneObjectFromCentralRepo().keySet().toArray(new String[0]);
        if (mMasterIpKeySet.length <= 0) {
            swipeRefreshLayout.setRefreshing(true);
            refreshDevices();
        }

        /* Setting the footer image  */
        setTheFooterIcons();

        /*Karuna For Testing ... */
        /*refreshDevicesWithoutClearing();*/
    }

    public void setTheFooterIcons() {

       if( LSSDPNodeDB.isLS9ExistsInTheNetwork()) {
           TextView icon = (TextView) findViewById(R.id.favorites_icon);
           Drawable top = getResources().getDrawable(R.mipmap.ic_cast_black_24dp);
           icon.setCompoundDrawablesWithIntrinsicBounds(null, top, null, null);
           icon.setText(R.string.favorite_button_playcast);
       }else{
           TextView icon = (TextView) findViewById(R.id.favorites_icon);
           Drawable top = getResources().getDrawable(R.mipmap.ic_favorites);
           icon.setCompoundDrawablesWithIntrinsicBounds(null, top, null, null);
           icon.setText(R.string.favorite_button);
       }
    }

    public void refreshDevicesWithoutClearing() {
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
/* Commenting for the Refresh we Have to Do one time Not Multiple  And wait For 10 Seconds*/
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

      /*  new Handler().postDelayed(new Runnable() { *//* Swipe Refersh UI will show the circular bar of 1750ms , in this three M-search will happen in the interval of time 500ms *//*
            @Override
            public void run() {

                //application.getScanThread().UpdateNodes();

                activeAdapter.notifyDataSetChanged();
                swipeRefreshLayout.setRefreshing(false);


                String[] mMasterIpKeySet = mScanHandler.getSceneObjectFromCentralRepo().keySet().toArray(new String[0]);
                if (mMasterIpKeySet.length <= 0) {
                    LibreLogger.d(this, "refresh");
                    if (!mDropAllIniatiated) {
                    *//* if at the end of active scenes if we dont have the master then we will call the playnew scenes *//*
                        Intent newIntent = new Intent(ActiveScenesListActivity.this, PlayNewActivity.class);
                        startActivity(newIntent);
                        finish();
                    }

                }


            }
        }, 5650); // Callign For Some reasons*/
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

            String[] mMasterIpKeySet = mScanHandler.getSceneObjectFromCentralRepo().keySet().toArray(new String[0]);
            if (mMasterIpKeySet.length <= 0) {
                    /* if at the end of active scenes if we dont have the master then we will call the playnew scenes */
               /* if(LibreApplication.mCleanUpIsDoneButNotRestarted==false) {
                    Intent newIntent = new Intent(ActiveScenesListActivity.this, PlayNewActivity.class);
                    startActivity(newIntent);
                    finish();
                }*/

            }
            /* and here comes the "trick" */
           // mTaskHandlerForSendingMSearch.postDelayed(this, MSEARCH_TIMEOUT_SEARCH);
        }
    };
   /* private void sendMSearchInIntervalOfTime() {
        mTaskHandlerForSendingMSearch.postDelayed(mMyTaskRunnableForMSearch, MSEARCH_TIMEOUT_SEARCH);

    }*/
    public void refreshDevices() {
        LibreLogger.d(this, "RefreshDevices With clearing");
        final LibreApplication application = (LibreApplication) getApplication();
        //application.getScanThread().clearNodes();
        application.getScanThread().UpdateNodes();

        mTaskHandlerForSendingMSearch.postDelayed(mMyTaskRunnableForMSearch,10000);
       new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(false);

            }
        }, 10000);
       /* new Handler().postDelayed(new Runnable() {
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

                //application.getScanThread().UpdateNodes();

                activeAdapter.notifyDataSetChanged();
                swipeRefreshLayout.setRefreshing(false);


                String[] mMasterIpKeySet = mScanHandler.getSceneObjectFromCentralRepo().keySet().toArray(new String[0]);
                if (mMasterIpKeySet.length <= 0) {
                    *//* if at the end of active scenes if we dont have the master then we will call the playnew scenes *//*
                    Intent newIntent = new Intent(ActiveScenesListActivity.this, PlayNewActivity.class);
                    startActivity(newIntent);
                    finish();

                }


            }
        }, 2650);*/
    }

    private void updateFromCentralRepositryDeviceList() {


        String[] mMasterIpKeySet = mScanHandler.getSceneObjectFromCentralRepo().keySet().toArray(new String[0]);
        LibreLogger.d(this, "Master is Getting Added T Active Scenes Adapter" + mScanHandler.getSceneObjectFromCentralRepo().keySet());
        /* Fix For If Master is Available But SceneObject is Not Available in the Network */
        if(mMasterIpKeySet.length<=0){

            if(LSSDPNodeDB.getInstance().isAnyMasterIsAvailableInNetwork()){
                for(LSSDPNodes nodes : LSSDPNodeDB.getInstance().GetDB()){
                    if (nodes.getDeviceState() != null /*&& nodes.getDeviceState().contains("M")*/) {
                        if (!ScanningHandler.getInstance().isIpAvailableInCentralSceneRepo(nodes.getIP())) {
                            SceneObject sceneObjec = new SceneObject(" ", nodes.getFriendlyname(), 0, nodes.getIP());
                            ScanningHandler.getInstance().putSceneObjectToCentralRepo(nodes.getIP(), sceneObjec);
                            LibreLogger.d(this, "Master is not available in Central Repo So Created SceneObject " + nodes.getIP());
                        }
                    }
                }
            }
        }
        mMasterIpKeySet = mScanHandler.getSceneObjectFromCentralRepo().keySet().toArray(new String[0]);
        for (String mMasterIp : mMasterIpKeySet) {
            LibreLogger.d(this, "Master is Getting Added T Active Scenes Adapter" + mMasterIp);
            mDeviceFound = false;
            mMasterFound = true;

            /* This is needed to make sure the unwanted free device is not coming */
            LSSDPNodes node = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(mMasterIp);
            if (node != null) {

               /* if (node.getDeviceState().contains("F") || node.getDeviceState().contains("S")) {
                    mScanHandler.removeSceneMapFromCentralRepo(node.getIP());
                    continue;
                }*/

            }


            try {
                LUCIControl luciControl = new LUCIControl(mMasterIp);


                ArrayList<LUCIPacket> luciPackets = new ArrayList<>();
                LUCIPacket mSceneNamePacket = new LUCIPacket(null, (short) 0, (short) MIDCONST.MID_SCENE_NAME, (byte) LSSDPCONST.LUCI_GET);
                /*removing it to resolve flickering issue*/
//                LUCIPacket volumePacket = new LUCIPacket(null, (short) 0, (short) MIDCONST.ZONE_VOLUME, (byte) LSSDPCONST.LUCI_GET);
                LUCIPacket currentSourcePacket = new LUCIPacket(null, (short) 0, (short) MIDCONST.MID_CURRENT_SOURCE, (byte) LSSDPCONST.LUCI_GET);
                LUCIPacket currentPlayStatePacket = new LUCIPacket(null, (short) 0, (short) MIDCONST.MID_CURRENT_PLAY_STATE, (byte) LSSDPCONST.LUCI_GET);
                LUCIPacket getUIPacket = new LUCIPacket(LUCIMESSAGES.GET_PLAY.getBytes(), (short) LUCIMESSAGES.GET_PLAY.length(),
                        (short) MIDCONST.MID_REMOTE_UI, (byte) LSSDPCONST.LUCI_SET);

                luciPackets.add(currentPlayStatePacket);
                luciPackets.add(mSceneNamePacket);
//                luciPackets.add(volumePacket);
                luciPackets.add(getUIPacket);
                luciPackets.add(currentSourcePacket);

                luciControl.SendCommand(luciPackets);

            } catch (Exception e) {
                e.printStackTrace();
            }

            activeAdapter.add(mScanHandler.getSceneObjectFromCentralRepo(mMasterIp));
            activeAdapter.notifyDataSetChanged();
        }

    }


    protected void onStop() {
        super.onStop();

        /* This is important to avoid firing the PlayNew activity
        * after the refreshDevices timeout happens when there a*/
        mDropAllIniatiated = true;

        unRegisterForDeviceEvents();
        clearScenePreparationFlags();
        /*removing all messages from handler*/
        if (handler != null)
            handler.removeCallbacksAndMessages(null);
    }


    private void clearScenePreparationFlags() {
        ConcurrentHashMap<String, SceneObject> mSceneList = mScanHandler.getSceneObjectFromCentralRepo();
        for (String masterIp : mSceneList.keySet()) {
            SceneObject sceneObject = mScanHandler.getSceneObjectFromCentralRepo(masterIp);
            if (sceneObject != null)
                sceneObject.setPreparingState(SceneObject.PREPARING_STATE.PREPARING_SUCCESS);
        }
    }


    /* Libre device interaction methods - START */
    @Override
    public void deviceDiscoveryAfterClearingTheCacheStarted() {

    }


    @Override
    public void newDeviceFound(LSSDPNodes node) {
        ArrayList<String> mSlaveIp = new ArrayList<>();

        LibreLogger.d(this, "New device is found with the ip address = " + node.getIP() + "Device State" + node.getDeviceState());
        mDeviceFound = true;
        if (node.getDeviceState().contains("M")) {

            mDeviceFound = false;
            mMasterFound = true;
            LUCIControl luciControl = new LUCIControl(node.getIP());
            try {


                ArrayList<LUCIPacket> luciPackets = new ArrayList<>();
                /*removing it to resolve flickering issue*/
//                LUCIPacket volumePacket = new LUCIPacket(null, (short) 0, (short) MIDCONST.ZONE_VOLUME, (byte) LSSDPCONST.LUCI_GET);
                LUCIPacket currentScenePacket = new LUCIPacket(null, (short) 0, (short) MIDCONST.MID_SCENE_NAME, (byte) LSSDPCONST.LUCI_GET);
                LUCIPacket getUIPacket = new LUCIPacket(LUCIMESSAGES.GET_PLAY.getBytes(), (short) LUCIMESSAGES.GET_PLAY.length(),
                        (short) MIDCONST.MID_REMOTE_UI, (byte) LSSDPCONST.LUCI_SET);
                luciPackets.add(getUIPacket);
                luciPackets.add(currentScenePacket);
                ;

//                luciPackets.add(volumePacket);


                luciControl.SendCommand(luciPackets);


            } catch (Exception e) {
                e.printStackTrace();
            }


            LibreLogger.d(this, "New device is found with the ip address = " + node.getIP());
            SceneObject sceneObjec = new SceneObject(" ", node.getFriendlyname(), 0, node.getIP());
            LibreLogger.d(this, "New device is found with the ip address = " + node.getFriendlyname());
            if (!mScanHandler.isIpAvailableInCentralSceneRepo(node.getIP())) {
                mScanHandler.putSceneObjectToCentralRepo(node.getIP(), sceneObjec);
            } else {
                SceneObject mSceneObj = mScanHandler.getSceneObjectFromCentralRepo(node.getIP());
                if (mSceneObj != null)
                    sceneObjec.setSceneName(mSceneObj.getSceneName());
            }
            if (!activeAdapter.checkSceneobjectIsPresentinList(node.getIP())) {
                activeAdapter.add(sceneObjec);
                activeAdapter.notifyDataSetChanged();
            }
            /*
             commenting to avoid immediate refresh */
            //    swipeRefreshLayout.setRefreshing(false);
        }

    }

    @Override
    public void deviceGotRemoved(String mIpAddress) {
        LibreLogger.d(this, "Device is Removed with the ip address = " + mIpAddress);

        SceneObject scneObj = activeAdapter.getSceneObjectFromAdapter(mIpAddress);
        if (scneObj != null) {
            activeAdapter.remove(scneObj);
            LibreLogger.d("ActiveScenesAdapaterBhargav", "" + scneObj.getSceneName());
            /*Commenting For HAri Changes , Because we are not going to remove anything from Data*/
           /* swipeRefreshLayout.setRefreshing(true);*/
            /* we are calling it SwipeRefreshLayout */
            /*refreshDevices();*/
            if (activeAdapter.getCount()==0){
                swipeRefreshLayout.setRefreshing(true);
                refreshDevices();
            }
        }
        activeAdapter.notifyDataSetChanged();

        if (LSSDPNodeDB.getInstance().GetDB().size() <= 0){
            startActivity(new Intent(ActiveScenesListActivity.this,ConfigureActivity.class));
            finish();
        }

    }

    @Override
    public void messageRecieved(NettyData dataRecived) {

        LibreLogger.d(this, "New message appeared for the device " + dataRecived.getRemotedeviceIp());
        SceneObject sceneObject = activeAdapter.getSceneObjectFromAdapter(dataRecived.getRemotedeviceIp());
        if(sceneObject==null){
            sceneObject = mScanHandler.getSceneObjectFromCentralRepo(dataRecived.getRemotedeviceIp());
        }
        LUCIPacket packet = new LUCIPacket(dataRecived.getMessage());
        /**/
        if(packet.getCommand() ==103 ) {
            String msg = new String(packet.payload);
                    /* A device became master */
            /*if (msg.contains("MASTER"))*/ {
                        /* Will ask for the play status and update the UI */

                LUCIControl luciControl = new LUCIControl(dataRecived.getRemotedeviceIp());
                try {
                            /*which means i have already registered for 3 that's why i got 103
                            * SO no need to send it again*/
                    luciControl.sendAsynchronousCommand();


                    ArrayList<LUCIPacket> luciPackets = new ArrayList<>();
                    LUCIPacket mScenePacket = new LUCIPacket(null, (short) 0, (short) MIDCONST.MID_SCENE_NAME, (byte) LSSDPCONST.LUCI_GET);
                            /*removing it to resolve flickering issue*/
//                            LUCIPacket volumePacket = new LUCIPacket(null, (short) 0, (short) MIDCONST.ZONE_VOLUME, (byte) LSSDPCONST.LUCI_GET);
                    LUCIPacket getUIPacket = new LUCIPacket(LUCIMESSAGES.GET_PLAY.getBytes(), (short) LUCIMESSAGES.GET_PLAY.length(),
                            (short) MIDCONST.MID_REMOTE_UI, (byte) LSSDPCONST.LUCI_SET);

                    luciPackets.add(mScenePacket);
//                            luciPackets.add(volumePacket);
                    luciPackets.add(getUIPacket);

                    luciControl.SendCommand(luciPackets);

                } catch (Exception e) {
                    e.printStackTrace();
                }


                LibreLogger.d(this, "A device became master found with the ip addrs = " + dataRecived.getRemotedeviceIp());
                LSSDPNodes node = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(dataRecived.getRemotedeviceIp());

                SceneObject sceneObjec;
                if (node != null) {
                    sceneObjec = new SceneObject(" ", node.getFriendlyname(), 0, dataRecived.getRemotedeviceIp());
                    LibreLogger.d(this, "device became master its name  = " + node.getFriendlyname());

                } else {
                    sceneObjec = new SceneObject(" ", "", 0, dataRecived.getRemotedeviceIp());

                }
                if (node!=null && !mScanHandler.isIpAvailableInCentralSceneRepo(node.getIP())) {
                    mScanHandler.putSceneObjectToCentralRepo(node.getIP(), sceneObjec);
                }

                activeAdapter.add(sceneObjec);
                activeAdapter.notifyDataSetChanged();
            }/* A device became slave or free so remove it from sceneobject if it is present and refresh UI*/
          /*  else {
                LibreLogger.d(this, "A device became slave or free so remove it from sceneobject if it is present and refresh UI" + dataRecived.getRemotedeviceIp());
                if (mScanHandler.isIpAvailableInCentralSceneRepo(dataRecived.getRemotedeviceIp())) {
                    mScanHandler.removeSceneMapFromCentralRepo(dataRecived.getRemotedeviceIp());

                }
                SceneObject sceneObjec = new SceneObject(" ", "", 0, dataRecived.getRemotedeviceIp());
                activeAdapter.remove(sceneObjec);
                activeAdapter.notifyDataSetChanged();

            }*/
            //activeAdapter.notifyDataSetChanged();
             /* Handle the case when there is no sceneobjetc remaining in the UI  */
            String[] mMasterIpKeySet = mScanHandler.getSceneObjectFromCentralRepo().keySet().toArray(new String[0]);
            if (mMasterIpKeySet.length <= 0) {
                LibreLogger.d(this, "Handle the case when there is no sceneobjetc remaining in the UI" + dataRecived.getRemotedeviceIp());
                swipeRefreshLayout.setRefreshing(true);
                refreshDevices();
            }
            return;
        }

        if (sceneObject != null) {
            LibreLogger.d(this, "Recieved the scene details as zoneVolume For Before checking  = " + sceneObject.getvolumeZoneInPercentage());
            LUCIPacket packet1 = new LUCIPacket(dataRecived.getMessage());
            LibreLogger.d(this, "New message appeared for the device " + dataRecived.getRemotedeviceIp() + "Command " + packet.getCommand());
            switch (packet1.getCommand()) {
                case MIDCONST.MID_SCENE_NAME: {
                    /* This message box indicates the Scene Name*//*
;*/
                     /* if Command Type 1 , then Scene Name information will be come in the same packet
if command type 2 and command status is 1 , then data will be empty., at that time we should not update the value .*/
                    if(packet.getCommandStatus()==1
                            && packet.getCommandType()==2){
                        return;
                    }
                    String message = new String(packet.getpayload());
                    //int duration = Integer.parseInt(message);
                    try {
                        sceneObject.setSceneName(message);// = duration/60000.0f;
                        LibreLogger.d(this, "Recieved the Scene Name to be " + sceneObject.getSceneName());
                        mScanHandler.putSceneObjectToCentralRepo(dataRecived.getRemotedeviceIp(), sceneObject);
                        activeAdapter.add(sceneObject);
                        activeAdapter.notifyDataSetChanged();
                    } catch (Exception e) {

                    }
                }
                break;
                case 49: {
                    /* This message box indicates the current playing status of the scene, information like current seek position*//*
;*/
                    String message = new String(packet.getpayload());
                    int duration=0;
                    try {
                         duration = Integer.parseInt(message);
                    }catch(Exception e){
                        LibreLogger.d(this,"Handling the app crash for 49 message box value sent ="+duration);
                        break;
                    }
                    sceneObject.setCurrentPlaybackSeekPosition(duration);// = duration/60000.0f;
                    LibreLogger.d(this, "Recieved the current Seek position to be " + sceneObject.getCurrentPlaybackSeekPosition());
                    mScanHandler.putSceneObjectToCentralRepo(dataRecived.getRemotedeviceIp(), sceneObject);


                    break;
                }


                case MIDCONST.VOLUME_CONTROL: {
                    /*this message box is to get volume*/

                    String message = new String(packet.getpayload());
                    try {
                        int duration = Integer.parseInt(message);
                        if (sceneObject.getVolumeValueInPercentage() != duration) {
                            sceneObject.setVolumeValueInPercentage(duration);
                            mScanHandler.putSceneObjectToCentralRepo(dataRecived.getRemotedeviceIp(), sceneObject);
                            LibreLogger.d(this, "Recieved the current volume to be" + sceneObject.getVolumeValueInPercentage());
                            activeAdapter.add(sceneObject);
                            activeAdapter.notifyDataSetChanged();
                        }


                    } catch (Exception e) {

                    }
                }
                break;


                case 50: {

                    /*this MB to get current sources*/
                    String message = new String(packet.getpayload());
                    try {
                        int mSource = Integer.parseInt(message);

                        sceneObject.setCurrentSource(mSource);

                        if (sceneObject.getCurrentSource() == Constants.ALEXA_SOURCE/*alexa*/){
                           /* sceneObject.setAlbum_name("");
                            sceneObject.setTrackName("");
                            sceneObject.setArtist_name("");*/
                            //sceneObject.setTotalTimeOfTheTrack(0);
                           // sceneObject.setPlayUrl("");
                        }
                        mScanHandler.putSceneObjectToCentralRepo(dataRecived.getRemotedeviceIp(), sceneObject);
                        LSSDPNodes mGcastCheckNode = mScanHandler.getLSSDPNodeFromCentralDB(dataRecived.getRemotedeviceIp());

                       /* if(mGcastCheckNode.getCurrentSource() == MIDCONST.GCAST_SOURCE){
                            if(mGcastCheckNode.getmPlayStatus() == SceneObject.CURRENTLY_PLAYING){
                                activeAdapter.remove(sceneObject);
                                activeAdapter.notifyDataSetChanged();
                                return;
                            }
                        }*/
                        activeAdapter.add(sceneObject);
                        activeAdapter.notifyDataSetChanged();
                        LibreLogger.d(this, "Recieved the current source as  " + sceneObject.getCurrentSource());
                    } catch (Exception e) {

                    }
                    break;
                }

                case 42: {
                    /*this MB to get UI layout*/

                    try {

                        if (sceneObject.getCurrentSource() == Constants.ALEXA_SOURCE){
                            return;
                        }

                        String message = new String(packet.payload);
                        LibreLogger.d(this,"MB : 42, msg = "+message);
                        JSONObject root = new JSONObject(message);
                        int cmd_id = root.getInt(LUCIMESSAGES.TAG_CMD_ID);
                        JSONObject window = root.getJSONObject(LUCIMESSAGES.ALEXA_KEY_WINDOW_CONTENT);
                        LibreLogger.d(this, "PLAY JSON is \n= " + message);


                        if (cmd_id == 3) {

                            /*Karuna change : Yet to redefine it */
                            Message msg = new Message();
                            msg.what = PREPARATION_COMPLETED;
                            Bundle data = new Bundle();
                            data.putString("ipAddress", dataRecived.getRemotedeviceIp());
                            msg.setData(data);
                            handler.sendMessage(msg);
                            activeAdapter.notifyDataSetChanged();

                            sceneObject = AppUtils.INSTANCE.updateSceneObjectWithPlayJsonWindow(window,sceneObject);

                            /* this is done to avoid  image refresh everytime the 42 message is recieved and the song playing back is the same */
                            LibreLogger.d(this, "Invalidating the scene name for  = " + sceneObject.getIpAddress() + "/" + "coverart.jpg");

                            LibreLogger.d(this, "Recieved the scene details as trackname = " + sceneObject.getSceneName() + ": " + sceneObject.getCurrentPlaybackSeekPosition());
                            LibreLogger.d(this, "Recieved the scene details as zoneVolume For checking  = " + sceneObject.getvolumeZoneInPercentage());

                            mScanHandler.putSceneObjectToCentralRepo(dataRecived.getRemotedeviceIp(), sceneObject);
                            activeAdapter.add(sceneObject);
                            activeAdapter.notifyDataSetChanged();


                        }


                    } catch (Exception e) {
                        e.printStackTrace();

                    }

                    break;
                }

                case 51: {
                    String str = new String(packet.payload);
                    LibreLogger.d(this,"MB : 51, msg = "+str);
                    if (!str.equals("")) {
                        int playstatus = Integer.parseInt(str);
                        LSSDPNodes mGcastCheckNode = mScanHandler.getLSSDPNodeFromCentralDB(dataRecived.getRemotedeviceIp());
                        /*if(mGcastCheckNode.getCurrentSource() == MIDCONST.GCAST_SOURCE){
                           if(mGcastCheckNode.getmPlayStatus() == SceneObject.CURRENTLY_PLAYING){
                                activeAdapter.remove(sceneObject);
                                activeAdapter.notifyDataSetChanged();
                                return;
                            }else{
                                if(!activeAdapter.checkSceneobjectIsPresentinList(sceneObject.getIpAddress())) {
                                    activeAdapter.add(sceneObject);
                                    activeAdapter.notifyDataSetChanged();
                                }
                            }
                        }*/
                        if (sceneObject.getPlaystatus() != playstatus) {
                            sceneObject.setPlaystatus(playstatus);
                            mScanHandler.putSceneObjectToCentralRepo(dataRecived.getRemotedeviceIp(), sceneObject);

                            activeAdapter.add(sceneObject);
                            activeAdapter.notifyDataSetChanged();
                        }
                        if (sceneObject.getCurrentSource() != 19 &&  mGcastCheckNode!=null &&
						mGcastCheckNode.getCurrentSource() != MIDCONST.GCAST_SOURCE
						) {
                            if (sceneObject.getPlaystatus() == SceneObject.CURRENTLY_PLAYING) {
                                LibreLogger.d(this, "playing state is received");
                            /*closing progress loader*/
//                            handler.sendEmptyMessage(PREPARATION_COMPLETED);

                                Message msg = new Message();
                                msg.what = PREPARATION_COMPLETED;
                                Bundle data = new Bundle();
                                data.putString("ipAddress", dataRecived.getRemotedeviceIp());
                                msg.setData(data);

                                handler.sendMessage(msg);
                            } else if (sceneObject.getPlaystatus() == SceneObject.CURRENTLY_PAUSED) {
                                LibreLogger.d(this, "Pause state is received");
                            } else {
                            /*initiating progress loader*/
                                if (sceneObject.getPlaystatus() == SceneObject.CURRENTLY_STOPPED
                                        && sceneObject.getCurrentSource() != Constants.NO_SOURCE) {
                                    Message msg = new Message();
                                    msg.what = PREPARATION_INIT;
                                    Bundle data = new Bundle();
                                    data.putString("ipAddress", dataRecived.getRemotedeviceIp());
                                    msg.setData(data);
                                    handler.sendMessage(msg);

                                    LibreLogger.d(this, "Stop state is received");
                                }

                            }
                            activeAdapter.notifyDataSetChanged();
                        }


                    }
                    break;
                }

//                case MIDCONST.ZONE_VOLUME: {
//
//                    String message = new String(packet.getpayload());
//                    try {
//                        int duration = Integer.parseInt(message);
//                        if (sceneObject.getvolumeZoneInPercentage() != duration) {
//                            sceneObject.setvolumeZoneInPercentage(duration);
//                            mScanHandler.putSceneObjectToCentralRepo(dataRecived.getRemotedeviceIp(), sceneObject);
//                            LibreLogger.d(this, "Recieved the current volume to be" + sceneObject.getvolumeZoneInPercentage());
//                            activeAdapter.add(sceneObject);
//                            activeAdapter.notifyDataSetChanged();
//                        }
//
//
//                    } catch (Exception e) {
//
//                    }
//                }
//                break;

                case 103: {
                    String msg = new String(packet.payload);
                    /* A device became master */
                 /*   if (msg.contains("MASTER"))*/ {
                        /* Will ask for the play status and update the UI */

                        LUCIControl luciControl = new LUCIControl(dataRecived.getRemotedeviceIp());
                        try {
                            /*which means i have already registered for 3 that's why i got 103
                            * SO no need to send it again*/
                            luciControl.sendAsynchronousCommand();


                            ArrayList<LUCIPacket> luciPackets = new ArrayList<>();
                            LUCIPacket mScenePacket = new LUCIPacket(null, (short) 0, (short) MIDCONST.MID_SCENE_NAME, (byte) LSSDPCONST.LUCI_GET);
                            /*removing it to resolve flickering issue*/
//                            LUCIPacket volumePacket = new LUCIPacket(null, (short) 0, (short) MIDCONST.ZONE_VOLUME, (byte) LSSDPCONST.LUCI_GET);
                            LUCIPacket getUIPacket = new LUCIPacket(LUCIMESSAGES.GET_PLAY.getBytes(), (short) LUCIMESSAGES.GET_PLAY.length(),
                                    (short) MIDCONST.MID_REMOTE_UI, (byte) LSSDPCONST.LUCI_SET);

                            luciPackets.add(mScenePacket);
//                            luciPackets.add(volumePacket);
                            luciPackets.add(getUIPacket);

                            luciControl.SendCommand(luciPackets);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }


                        LibreLogger.d(this, "A device became master found with the ip addrs = " + dataRecived.getRemotedeviceIp());
                        LSSDPNodes node = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(dataRecived.getRemotedeviceIp());

                        SceneObject sceneObjec;
                        if (node != null) {
                            sceneObjec = new SceneObject(" ", node.getFriendlyname(), 0, dataRecived.getRemotedeviceIp());
                            LibreLogger.d(this, "device became master its name  = " + node.getFriendlyname());

                        } else {
                            sceneObjec = new SceneObject(" ", "", 0, dataRecived.getRemotedeviceIp());

                        }
                        if (!mScanHandler.isIpAvailableInCentralSceneRepo(node.getIP())) {
                            mScanHandler.putSceneObjectToCentralRepo(node.getIP(), sceneObjec);
                        }

                        activeAdapter.add(sceneObjec);
                        activeAdapter.notifyDataSetChanged();
                    }

                  /* A device became slave or free
                   * so remove it from sceneobject if it is present and refresh UI*/
                /*    else {
                        LibreLogger.d(this, "A device became slave or free so remove it from sceneobject if it is present and refresh UI" + dataRecived.getRemotedeviceIp());
                        if (mScanHandler.isIpAvailableInCentralSceneRepo(dataRecived.getRemotedeviceIp())) {
                            mScanHandler.removeSceneMapFromCentralRepo(dataRecived.getRemotedeviceIp());
                            SceneObject sceneObjec = new SceneObject(" ", "", 0, dataRecived.getRemotedeviceIp());
                            activeAdapter.remove(sceneObjec);
                            activeAdapter.notifyDataSetChanged();
                        }

                    }*/



                    /* Handle the case when there is no sceneobjetc remaining in the UI  */
                    String[] mMasterIpKeySet = mScanHandler.getSceneObjectFromCentralRepo().keySet().toArray(new String[0]);
                    if (mMasterIpKeySet.length <= 0) {
                        LibreLogger.d(this, "Handle the case when there is no sceneobjetc remaining in the UI" + dataRecived.getRemotedeviceIp());
                        swipeRefreshLayout.setRefreshing(true);
                        refreshDevices();
                    }
                }
                break;
                case 54:{
                    String message = new String(packet.payload);
                    LibreLogger.d(this, " message 54 recieved  " + message);
                    try {
                        LibreError error=null;
                        if(message.contains(Constants.FAIL)) {
                            error = new LibreError(dataRecived.getRemotedeviceIp(), Constants.FAIL_ALERT_TEXT);

                        }else if(message.contains(Constants.SUCCESS)){
                            closeLoader();

                            Message msg = new Message();
                            msg.what = PREPARATION_COMPLETED;
                            Bundle data = new Bundle();
                            data.putString("ipAddress", dataRecived.getRemotedeviceIp());
                            msg.setData(data);
                            handler.sendMessage(msg);


                        } else if (message.contains(Constants.NO_URL)) {
                            error = new LibreError(dataRecived.getRemotedeviceIp(), Constants.NO_URL_ALERT_TEXT);
                        }else if(message.contains(Constants.NO_PREV_SONG)){
                            error = new LibreError(dataRecived.getRemotedeviceIp(), getResources().getString(R.string.NO_PREV_SONG_ALERT_TEXT));
                        }else if(message.contains(Constants.NO_NEXT_SONG)){
                            error = new LibreError(dataRecived.getRemotedeviceIp(), getResources().getString(R.string.NO_NEXT_SONG_ALERT_TEXT));
                        }
                        else if (message.contains(Constants.DMR_SONG_UNSUPPORTED)) {
                            error = new LibreError(dataRecived.getRemotedeviceIp(), getResources().getString(R.string.SONG_NOT_SUPPORTED));
                        }else if(message.contains(LUCIMESSAGES.NEXTMESSAGE)){
                            activeAdapter.doNextPrevious(true,sceneObject,true);
                        }else if(message.contains(LUCIMESSAGES.PREVMESSAGE)){
                            activeAdapter.doNextPrevious(false,sceneObject,true);
                        }
                        PicassoTrustCertificates.getInstance(ActiveScenesListActivity.this).invalidate(sceneObject.getAlbum_art());
                        closeLoader();
                        if (error != null)
                            showErrorMessage(error);

                    } catch (Exception e) {
                        e.printStackTrace();
                        LibreLogger.d(this, " Json exception ");

                    }
                }break;

            }
        }


    }

    View.OnClickListener onclickListner = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            switch (view.getId())

            {


                case R.id.footer_first:
                  // don't put break here - bhargav

                case R.id.imgView_arrow: {
                    if (footerViewsecond.getVisibility() == View.VISIBLE) {
                        footerViewsecond.setVisibility(View.GONE);
                        img_arrow.setRotation(180);
                    } else {
                        footerViewsecond.setVisibility(View.VISIBLE);
                        img_arrow.setRotation(0);
                    }
                }
                break;

                case R.id.footer_second_view:

                    break;

                case R.id.playnew_icon: {

                    if (LibreApplication.getIs3PDAEnabled()){
                        Intent intent = new Intent(ActiveScenesListActivity.this, MainActivity.class);
                        intent.putExtra(AlexaConstants.INTENT_FROM_ACTIVITY,AlexaConstants.INTENT_FROM_ACTIVITY_ACTIVE_SCENES);
                        startActivity(intent);
                    }else{
                        showToast("This feature is not available");
                    }

                    /*Intent configIntent = new Intent(ActiveScenesListActivity.this, CreateNewScene.class);
                    startActivity(configIntent);*/
                }
                break;

                case R.id.favorites_icon:


                    if (LSSDPNodeDB.isLS9ExistsInTheNetwork()){

                        SharedPreferences sharedPreferences = getApplicationContext()
                                .getSharedPreferences(Constants.SHOWN_GOOGLE_TOS, Context.MODE_PRIVATE);
                        String shownGoogle = sharedPreferences.getString(Constants.SHOWN_GOOGLE_TOS, null);
                        if (shownGoogle == null) {
                            showGoogleTosDialog(ActiveScenesListActivity.this);
                        }
                        else {
                            Intent intent=new Intent(ActiveScenesListActivity.this,GcastSources.class);
                            startActivity(intent);
                        }
                    }

                    else {
                        Toast.makeText(ActiveScenesListActivity.this, getString(R.string.favourits_not_implemented), Toast.LENGTH_LONG).show();
                    }

                    break;

                case R.id.config_item: {
                    Intent configIntent = new Intent(ActiveScenesListActivity.this, NewSacActivity.class);
                    startActivity(configIntent);
                    finish();
                }
                break;

            }

        }

    };
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            if (msg.what == RELEASE_ALL_STARTED) {
                showLoader();
            }

            if (msg.what == RELEASE_ALL_SUCCESS) {
                closeLoader();
                handler.removeMessages(RELEASE_ALL_TIMEOUT);

                  /*to ensure we are updating adapter*/
                activeAdapter.clear();
                activeAdapter.notifyDataSetChanged();
                /*making sure to call PlayNewActtivity activity Bug : 2277*/
                Intent intent = new Intent(ActiveScenesListActivity.this, PlayNewActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();

            }

            if (msg.what == RELEASE_ALL_TIMEOUT) {
                closeLoader();

                  /*to ensure we are updating adapter*/
                activeAdapter.clear();
                activeAdapter.notifyDataSetChanged();

                /*making sure to call PlayNewActtivity activity Bug : 2277 */
                Intent intent = new Intent(ActiveScenesListActivity.this, PlayNewActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }

            if (msg.what == PREPARATION_INIT) {

                String ipaddress = msg.getData().getString("ipAddress");
                triggerTimeOutHandlerForIpAddress(ipaddress);

            }
            if (msg.what == PREPARATION_COMPLETED) {
                String ipaddress = msg.getData().getString("ipAddress");
                removeTimeOutTriggerForIpaddress(ipaddress);

            }
            if (msg.what == PREPARATION_TIMEOUT_CONST) {
                String ipaddress = msg.getData().getString("ipAddress");


                    /*posting error to application here we are using bus because it doest extend DeviceDiscoverActivity*/
                LibreError error = new LibreError(ipaddress, getResources().getString(R.string.PREPARATION_TIMEOUT_HAPPENED));
                showErrorMessage(error);

                SceneObject mSceneObject = mScanHandler.getSceneObjectFromCentralRepo(ipaddress);
                if (mSceneObject != null) {
                    mSceneObject.setPreparingState(SceneObject.PREPARING_STATE.PREPARING_FAILED);
                }
                activeAdapter.notifyDataSetChanged();

            }

            if (msg.what == Constants.ALEXA_NEXT_PREV_INIT) {
                String ipaddress = msg.getData().getString("ipAddress");
                Message timeOutMessage = new Message();
                Bundle data = new Bundle();
                data.putString("ipAddress", ipaddress);
                timeOutMessage.setData(data);
                timeOutMessage.obj = ipaddress;
                timeOutMessage.what = Constants.ALEXA_NEXT_PREV_HANDLER;
                handler.sendMessageDelayed(timeOutMessage, Constants.ALEXA_NEXT_PREV_TIMEOUT);
                SceneObject mSceneObject = mScanHandler.getSceneObjectFromCentralRepo(ipaddress);
                if (mSceneObject != null) {
                    mSceneObject.setPreparingState(SceneObject.PREPARING_STATE.PREPARING_INITIATED);
                }
                activeAdapter.notifyDataSetChanged();
            }

            if (msg.what == Constants.ALEXA_NEXT_PREV_HANDLER) {
                String ipaddress = msg.getData().getString("ipAddress");
                handler.removeMessages(Constants.ALEXA_NEXT_PREV_HANDLER, ipaddress);
                SceneObject mSceneObject = mScanHandler.getSceneObjectFromCentralRepo(ipaddress);
                if (mSceneObject != null) {
                    mSceneObject.setPreparingState(SceneObject.PREPARING_STATE.PREPARING_SUCCESS);
                }
                activeAdapter.notifyDataSetChanged();

            }
        }
    };

    private void removeTimeOutTriggerForIpaddress(String ipAddress) {

        handler.removeMessages(PREPARATION_TIMEOUT_CONST, ipAddress);
        SceneObject mSceneObject = mScanHandler.getSceneObjectFromCentralRepo(ipAddress);
        if (mSceneObject != null) {
            mSceneObject.setPreparingState(SceneObject.PREPARING_STATE.PREPARING_SUCCESS);
        }
        activeAdapter.notifyDataSetChanged();
    }

    private void triggerTimeOutHandlerForIpAddress(String ipAddress) {
        Message timeOutMessage = new Message();

        /* Added by Praveen to address the crash for PREPARATION_TIMEOUT_CONST */
        Bundle data = new Bundle();
        data.putString("ipAddress", ipAddress);
        timeOutMessage.setData(data);

        timeOutMessage.obj = ipAddress;
        timeOutMessage.what = PREPARATION_TIMEOUT_CONST;
        handler.sendMessageDelayed(timeOutMessage, Constants.PREPARATION_TIMEOUT);

        SceneObject mSceneObject = mScanHandler.getSceneObjectFromCentralRepo(ipAddress);
        if (mSceneObject != null) {
            mSceneObject.setPreparingState(SceneObject.PREPARING_STATE.PREPARING_INITIATED);
        }
        activeAdapter.notifyDataSetChanged();
    }


    private void showLoader() {

        if (mProgressDialog == null) {

            if (!(ActiveScenesListActivity.this.isFinishing())) {
                mProgressDialog = ProgressDialog.show(ActiveScenesListActivity.this,getString(R.string.notice),
                        getString(R.string.pleaseWait), true, true, null);
            }
        }

		/*We got the CrashAnalytics Logs , if progressDialog is Null*/
        if (mProgressDialog != null)
            mProgressDialog.setCancelable(false);

        if (!mProgressDialog.isShowing()) {
            Log.e("LUCIControl", "is Showing");
            if (!(ActiveScenesListActivity.this.isFinishing())) {
                mProgressDialog = ProgressDialog.show(ActiveScenesListActivity.this, getString(R.string.notice),
                        getString(R.string.pleaseWait), true, true, null);
            }

            mProgressDialog.show();
        }

    }

       private void closeLoader() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            Log.e("LuciControl", "progress Dialog Closed");
            mProgressDialog.setCancelable(true);
            mProgressDialog.dismiss();
            mProgressDialog.cancel();
        }
    }

    /*This function gets Called When Drop All Pressed*/

    private void dropAllDevices() {
        ScanningHandler mScanningHandler = ScanningHandler.getInstance();
        ConcurrentHashMap<String, SceneObject> centralSceneObjectRepo = mScanningHandler.getSceneObjectFromCentralRepo();


        LSSDPNodeDB mNodeDB = LSSDPNodeDB.getInstance();

        ArrayList<LSSDPNodes> mNodeDBArrayList = mNodeDB.GetDB();


        for (LSSDPNodes node : mNodeDBArrayList) {
            LUCIControl luciControl = new LUCIControl(node.getIP());
            LibreLogger.d("this", "Release Scene Slave Ip List" + node.getFriendlyname());
            //* Sending Asynchronous Registration *//*
            luciControl.sendAsynchronousCommand();
            luciControl.SendCommand(MIDCONST.MID_DDMS, LUCIMESSAGES.SETFREE, LSSDPCONST.LUCI_SET);
        }


        /*clearing central repository*/
        mScanningHandler.clearSceneObjectsFromCentralRepo();
    }

    /* This function gets called when release all pressed*/
    private void releaseAllDevicesDialogue(int size) {

        if (ActiveScenesListActivity.this.isFinishing())
            return;

        String numberOfDevices = size + " ";

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                ActiveScenesListActivity.this);
        alertDialogBuilder.setTitle(getString(R.string.appHasDetected) + numberOfDevices + getString(R.string.devicesText));
        alertDialogBuilder
                .setMessage(getString(R.string.clearAllMsg))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        /**
                         * sending free to all devices including DB
                         */
                        dialog.dismiss();

                        handler.sendEmptyMessage(RELEASE_ALL_STARTED);
                        mDropAllIniatiated = true;

                        /* this is Drop All Function */
                        dropAllDevices();
/*                    *//*this is synchronous function*//*
                        releaseAllMasterandSlave();*/
                    /*sometime we have slave without master this will handle boundary condition */
                    /*this is synchronous function*/
//                        freeAllDevicesFromDB();

                        handler.sendEmptyMessageDelayed(RELEASE_ALL_TIMEOUT,5000);

                        /*ScanThread.getInstance().UpdateNodes();*/


                    }
                })
                .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        dialog.cancel();
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_play_new, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

      /*  //noinspection SimplifiableIfStatement
        if (id == R.id.release_all) {
            try {

                ArrayList<LSSDPNodes> nodesList = LSSDPNodeDB.getInstance().GetDB();
                if (nodesList == null)
                    return true;

                releaseAllDevicesDialogue(nodesList.size());
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this,getString(R.string.some_error_occured_while_freeing), Toast.LENGTH_SHORT).show();
            }
        }
*/
        return super.onOptionsItemSelected(item);
    }


    /*Releasing all devices*/
    private void releaseAllMasterandSlave() {
        ScanningHandler mScanningHandler = ScanningHandler.getInstance();
        ConcurrentHashMap<String, SceneObject> centralSceneObjectRepo = mScanningHandler.getSceneObjectFromCentralRepo();

            /*which means no master present hence all devices are free so need to do anything*/
        if (centralSceneObjectRepo == null || centralSceneObjectRepo.size() == 0) {
            LibreLogger.d(this, "No master present");
            return;
        }

        for (String masterIPAddress : centralSceneObjectRepo.keySet()) {
            ArrayList<LSSDPNodes> lssdpNodesArrayList = mScanningHandler
                    .getSlaveListForMasterIp(masterIPAddress, mScanningHandler.getconnectedSSIDname(getApplicationContext()));

            if (lssdpNodesArrayList != null && lssdpNodesArrayList.size() != 0) {
                for (LSSDPNodes mSlaves : lssdpNodesArrayList) {
                /*freeing slave for particular master*/
                    LUCIControl luciControl = new LUCIControl(mSlaves.getIP());
                    /*send this command only when you bother about 103*/
                    luciControl.sendAsynchronousCommand();
                    luciControl.SendCommand(MIDCONST.MID_DDMS, LUCIMESSAGES.SETFREE, LSSDPCONST.LUCI_SET);

                    try {
                        Thread.sleep(150);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            /*freeing master*/
            LUCIControl luciControl = new LUCIControl(masterIPAddress);
            luciControl.sendAsynchronousCommand();
            luciControl.SendCommand(MIDCONST.MID_DDMS, LUCIMESSAGES.SETFREE, LSSDPCONST.LUCI_SET);

        }
        /*clearing central repository*/
        mScanningHandler.clearSceneObjectsFromCentralRepo();
    }

    /*freeing all devices from DB and clearing it*/
    private boolean freeAllDevicesFromDB() {

        ArrayList<LSSDPNodes> nodesList = LSSDPNodeDB.getInstance().GetDB();
        if (nodesList != null && nodesList.size() > 0) {

            for (LSSDPNodes nodes : nodesList) {
                LUCIControl luciControl = new LUCIControl(nodes.getIP());
                    /*send this command only when you bother about 103*/
                luciControl.sendAsynchronousCommand();
                luciControl.SendCommand(MIDCONST.MID_DDMS, LUCIMESSAGES.SETFREE, LSSDPCONST.LUCI_SET);
                try {
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        LSSDPNodeDB.getInstance().clearDB();
        handler.sendEmptyMessage(RELEASE_ALL_SUCCESS);
        return true;
    }


    @Override
    public void onBackPressed() {
        if (isDoubleTap) {
            ensureDMRPlaybackStopped();
            super.onBackPressed();
            killApp();
            return;
        }
        Toast.makeText(this, getString(R.string.doubleTapToExit), Toast.LENGTH_SHORT).show();
        isDoubleTap = true;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                isDoubleTap = false;
            }
        }, 2000);
    }

    public void showGoogleTosDialog(Context context) {
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
                        editor.putString(Constants.SHOWN_GOOGLE_TOS, getString(R.string.yes));
                        editor.commit();


                        LUCIControl control=new LUCIControl("");
                        control.sendGoogleTOSAcceptanceIfRequired(ActiveScenesListActivity.this, "true");



                        Intent intent=new Intent(ActiveScenesListActivity.this,GcastSources.class);
                        startActivity(intent);


                    }

                });

        if(getAlertDialog1() ==null) {
            setAlertDialog1(builder.create());
            getAlertDialog1().setView(gcastDialog);
        }
        if(getAlertDialog1() !=null && !getAlertDialog1().isShowing())
            getAlertDialog1().show();
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


    public void ensureAppKill(){

            Log.d("NetworkChanged", "App is Restarting");
            //finish();
        /* Stopping ForeGRound Service Whenwe are Restarting the APP */
            Intent in = new Intent(ActiveScenesListActivity.this, DMRDeviceListenerForegroundService.class);
            in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            in.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
            stopService(in);


        /* * Finish this activity, and tries to finish all activities immediately below it
     * in the current task that have the same affinity.*/
            ActivityCompat.finishAffinity(this);
        /* Killing our Android App with The PID For the Safe Case */
            int pid = android.os.Process.myPid();
            android.os.Process.killProcess(pid);

        }

    }


