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
import android.widget.TextView;
import android.widget.Toast;

import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity;
import com.libre.ManageDevice.CreateNewScene;
import com.libre.Network.NewSacActivity;
import com.libre.Scanning.Constants;
import com.libre.Scanning.ScanThread;
import com.libre.Scanning.ScanningHandler;
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
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by praveena on 7/24/15.
 */
public class PlayNewActivity extends CTDeviceDiscoveryActivity implements View.OnClickListener, LibreDeviceInteractionListner {

    ProgressDialog mProgressDialog;
    private ScanningHandler mScanHandler = ScanningHandler.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playnew);
        // No devices found.
        boolean mMasterFound = LSSDPNodeDB.getInstance().isAnyMasterIsAvailableInNetwork();

        if(mScanHandler.getFreeDeviceList().size() <= 0 && !mMasterFound){
            LibreLogger.d(this,"There are no devices found. Hence navigating to Configure activity"+mScanHandler.getFreeDeviceList().size());
            startActivity(new Intent(PlayNewActivity.this,ConfigureActivity.class));
            finish();
        }
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);

        findViewById(R.id.favorites_icon).setOnClickListener(this);
        findViewById(R.id.playnew_icon).setOnClickListener(this);
        findViewById(R.id.config_item).setOnClickListener(this);

        findViewById(R.id.play_new_button).setOnClickListener(this);

        startActivity(new Intent(PlayNewActivity.this,ActiveScenesListActivity.class));
        finish();

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerForDeviceEvents(this);
        /*Karuna For Testing ... */
        setTheFooterIcons();
        /*Karuna For Testing ... */
        /*refreshDevices();*/

        setTheFooterIcons();
    }
    public void refreshDevices(){
        LibreLogger.d(this,"RefreshDevices Without clearing");
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

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                application.getScanThread().UpdateNodes();
            }
        }, 5650);
    }
    @Override
    protected void onPause() {
        super.onPause();
        unRegisterForDeviceEvents();
    }

    @Override
    public void onBackPressed() {
        ScanningHandler mScanHandler = ScanningHandler.getInstance();

        if (mScanHandler.centralSceneObjectRepo.size()<1 || LSSDPNodeDB.getInstance().isAnyMasterIsAvailableInNetwork())
        {
            Intent intentx = new Intent(Intent.ACTION_MAIN);
            intentx.addCategory(Intent.CATEGORY_HOME);
            intentx.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//***Change Here***
            startActivity(intentx);
            finish();
            System.exit(0);

        }else {
            super.onBackPressed();
            finish();
        }
   /*     Intent in = new Intent(PlayNewActivity.this, DMRDeviceListenerForegroundService.class);
        in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        in.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
        stopService(in);

        finish();
        int pid = android.os.Process.myPid();
        android.os.Process.killProcess(pid);*/
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseAllHandler.removeCallbacksAndMessages(this);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.favorites_icon:


                if (LSSDPNodeDB.isLS9ExistsInTheNetwork()){

                    SharedPreferences sharedPreferences = getApplicationContext()
                            .getSharedPreferences(Constants.SHOWN_GOOGLE_TOS, Context.MODE_PRIVATE);
                    String shownGoogle = sharedPreferences.getString(Constants.SHOWN_GOOGLE_TOS, null);
                    if (shownGoogle == null) {
                        showGoogleTosDialog(PlayNewActivity.this);
                    }
                    else {
                        Intent intent=new Intent(PlayNewActivity.this,GcastSources.class);
                        startActivity(intent);
                    }
                }
                else {
                    Toast.makeText(this, getString(R.string.favourits_not_implemented), Toast.LENGTH_LONG).show();
                }

                break;


            case R.id.playnew_icon:
                //Toast.makeText(this, "Play New", Toast.LENGTH_SHORT).show();
                Intent newIntent = new Intent(PlayNewActivity.this, CreateNewScene.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(newIntent);
                finish();
                break;

            case R.id.config_item:
                //Toast.makeText(this, "Config", Toast.LENGTH_SHORT).show();
                Intent newIntent2 = new Intent(PlayNewActivity.this, NewSacActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(newIntent2);
                finish();
                break;

            case R.id.play_new_button:
               // Toast.makeText(this, "Play New", Toast.LENGTH_SHORT).show();
                Intent newIntent1 = new Intent(PlayNewActivity.this, CreateNewScene.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(newIntent1);
                finish();
                break;
        }
    }


    Handler releaseAllHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            if (msg.what == ActiveScenesListActivity.RELEASE_ALL_STARTED) {
                showLoader();
            }

            if (msg.what == ActiveScenesListActivity.RELEASE_ALL_SUCCESS) {
                closeLoader();
                releaseAllHandler.removeMessages(ActiveScenesListActivity.RELEASE_ALL_TIMEOUT);
                         /*making sure to call CreateNewScene activity*/
                Intent intent = new Intent(PlayNewActivity.this, CreateNewScene.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

            }

            if (msg.what == ActiveScenesListActivity.RELEASE_ALL_TIMEOUT) {
                closeLoader();
                         /*making sure to call CreateNewScene activity*/
                Intent intent = new Intent(PlayNewActivity.this, CreateNewScene.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

            }
        }
    };


    private void showLoader() {

        if (mProgressDialog == null) {
            Log.e("LUCIControl", "Null");
            if (!(PlayNewActivity.this.isFinishing())) {
                mProgressDialog = ProgressDialog.show(PlayNewActivity.this, getString(R.string.notice), getString(R.string.pleaseWait), true, true, null);
            }
        }
        mProgressDialog.setCancelable(false);

        if (!mProgressDialog.isShowing()) {
            Log.e("LUCIControl", "is Showing");
            if (!(PlayNewActivity.this.isFinishing())) {
                mProgressDialog = ProgressDialog.show(PlayNewActivity.this, getString(R.string.notice), getString(R.string.pleaseWait), true, true, null);
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

    private void dropAllDevices(){
        ScanningHandler mScanningHandler = ScanningHandler.getInstance();
        ConcurrentHashMap<String, SceneObject> centralSceneObjectRepo = mScanningHandler.getSceneObjectFromCentralRepo();

            /*which means no master present hence all devices are free so need to do anything*/
        if (centralSceneObjectRepo == null || centralSceneObjectRepo.size() == 0) {
            LibreLogger.d(this, "No master present");
            return;
        }

        LSSDPNodeDB mNodeDB = LSSDPNodeDB.getInstance();
        LSSDPNodes mNodeToSendDropAll = mNodeDB.GetDB().get(0);

        LUCIControl mLuciControlToSendDropAll = new LUCIControl(mNodeToSendDropAll.getIP());
        mLuciControlToSendDropAll.SendCommand(MIDCONST.MID_JOIN_OR_DROP,LUCIMESSAGES.DROP_ALL,LSSDPCONST.LUCI_SET);

        /*clearing central repository*/
        mScanningHandler.clearSceneObjectsFromCentralRepo();
    }

    /* This function gets called when release all pressed*/
    private void releaseAllDevicesDialogue(int size) {

        String numberOfDevices = size + " ";
        if (!PlayNewActivity.this.isFinishing()) {


            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                    PlayNewActivity.this);
            alertDialogBuilder.setTitle(getString(R.string.doYouWantToFree));
            alertDialogBuilder
                    .setMessage(numberOfDevices + getString(R.string.devicesText))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            /**
                             * sending free to all devices including DB
                             */

                            dialog.dismiss();

                            releaseAllHandler.sendEmptyMessage(ActiveScenesListActivity.RELEASE_ALL_STARTED);

                            /* Drop All Devices */
                            dropAllDevices();
                         /*this is synchronous function*//*
                            releaseAllMasterandSlave();*/
               /*sometime we have slave without master this will handle boundary condition */
                         /*this is synchronous function*/
                            freeAllDevicesFromDB();


                            releaseAllHandler.sendEmptyMessage(ActiveScenesListActivity.RELEASE_ALL_TIMEOUT);

                            ScanThread.getInstance().UpdateNodes();


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
   /*     int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.release_all) {
            try {

                ArrayList<LSSDPNodes> nodesList = LSSDPNodeDB.getInstance().GetDB();
                String[] mMasterIpKeySet = mScanHandler.getSceneObjectFromCentralRepo().keySet().toArray(new String[0]);

                if (nodesList == null || mMasterIpKeySet.length == 0) {
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.NO_DEVICES_TO_DROP),Toast.LENGTH_SHORT).show();
                    return super.onOptionsItemSelected(item);
                }

                releaseAllDevicesDialogue(nodesList.size());

                return true;
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, getString(R.string.some_error_occured_while_freeing), Toast.LENGTH_SHORT).show();
            }
        }*/

        return super.onOptionsItemSelected(item);
    }


    /*Releasing all devices*/
    private boolean releaseAllMasterandSlave() {

        ScanningHandler mScanningHandler = ScanningHandler.getInstance();
        ConcurrentHashMap<String, SceneObject> centralSceneObjectRepo = mScanningHandler.getSceneObjectFromCentralRepo();

        /*which means no master present hence all devices are free so need to do anything*/
        if (centralSceneObjectRepo == null || centralSceneObjectRepo.size() == 0) {
            LibreLogger.d(this, "No master present");
            return false;
        }

        for (String masterIPAddress : centralSceneObjectRepo.keySet()) {
            ArrayList<LSSDPNodes> lssdpNodesArrayList = mScanningHandler
                    .getSlaveListForMasterIp(masterIPAddress, mScanningHandler.getconnectedSSIDname(getApplicationContext()));

            if (lssdpNodesArrayList != null && lssdpNodesArrayList.size() != 0) {
                for (LSSDPNodes mSlaves : lssdpNodesArrayList) {
                /*freeing slave for particular master*/
                    LUCIControl luciControl = new LUCIControl(mSlaves.getIP());
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
        return true;
    }

    /*freeing all devices from DB and clearing it*/
    private void freeAllDevicesFromDB() {

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


    @Override
    public void deviceDiscoveryAfterClearingTheCacheStarted() {

    }

    @Override
    public void newDeviceFound(LSSDPNodes node) {

        if (node.getDeviceState().contains("M")) {
            if (!mScanHandler.isIpAvailableInCentralSceneRepo(node.getIP())) {
                SceneObject sceneObjec = new SceneObject(" ", node.getFriendlyname(), 0, node.getIP());
                mScanHandler.putSceneObjectToCentralRepo(node.getIP(), sceneObjec);
            }
            updateSceneObjectAndLaunchActiveScene(node.getIP());
        }


    }

    @Override
    public void deviceGotRemoved(String ipaddress) {

    }

    @Override
    public void messageRecieved(NettyData packet) {

        Log.e("PlayNewActivity", "MessageReceived" + new String(packet.getMessage()));

        LUCIPacket mLucipacket = new LUCIPacket(packet.getMessage());
        if (mLucipacket.getCommand() == 103) {
            String msg = new String(packet.getMessage());
            /*which means master is present now so prompting to ActiveScenesListActivity*/

            if (msg.contains("MASTER")) {
                updateSceneObjectAndLaunchActiveScene(packet.getRemotedeviceIp());
            }


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
                        editor.putString(Constants.SHOWN_GOOGLE_TOS, getString(R.string.yes));
                        editor.commit();


                        LUCIControl control=new LUCIControl("");
                        control.sendGoogleTOSAcceptanceIfRequired(PlayNewActivity.this,"true");


                        Intent intent = new Intent(context, GcastSources.class);
                        startActivity(intent);

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
    /*this method will get called when we are getting master 103*/
    private void updateSceneObjectAndLaunchActiveScene(String ipaddress) {

        LSSDPNodes mMasterNode = mScanHandler.getLSSDPNodeFromCentralDB(ipaddress);

            if (mMasterNode!=null) {

                if (!mScanHandler.isIpAvailableInCentralSceneRepo(ipaddress)) {
                    SceneObject mMastersceneObjec = new SceneObject(" ", mMasterNode.getFriendlyname(), 0, mMasterNode.getIP());
                    mScanHandler.putSceneObjectToCentralRepo(ipaddress, mMastersceneObjec);
                }
                if(LibreApplication.mCleanUpIsDoneButNotRestarted==false) {
                    Intent intent = new Intent(PlayNewActivity.this, ActiveScenesListActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
            }

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

}