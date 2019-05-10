package com.libre.Ls9Sac;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity;
import com.libre.LibreApplication;
import com.libre.PlayNewActivity;
import com.libre.R;
import com.libre.Scanning.Constants;
import com.libre.Scanning.ScanningHandler;
import com.libre.constants.LSSDPCONST;
import com.libre.constants.MIDCONST;
import com.libre.luci.LSSDPNodeDB;
import com.libre.luci.LSSDPNodes;
import com.libre.luci.LUCIControl;
import com.libre.luci.LUCIPacket;
import com.libre.netty.BusProvider;
import com.libre.netty.LibreDeviceInteractionListner;
import com.libre.netty.NettyData;
import com.libre.netty.RemovedLibreDevice;
import com.libre.util.LibreLogger;

public class GoogleCastUpdateAfterSac extends CTDeviceDiscoveryActivity implements LibreDeviceInteractionListner {

    private final static int TIMEOUT_FOR_GET_UPDATE_STATUS = 60000;
    private TextView mGoogleCastDetails;
    private ProgressBar mGoogleCastUpdateBar;
    private ProgressBar mRetrivingGcastUpdateInformation;

    private String mLs9DeviceIpAddressForCastUpdate= "";
    private Button btnDone ;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case Constants.PREPARATION_INITIATED:
                    mGoogleCastDetails.setText("Checking for Update");
                    showProgressDialog();
                    break;
                case Constants.PREPARATION_COMPLETED:
                    closeProgressDialog();
                    break;
                case Constants.PREPARATION_TIMEOUT:
                    mGoogleCastDetails.setText(getString(R.string.no_update_gcast));
                    mGoogleCastUpdateBar.setVisibility(View.GONE);
                    showDialogFirmwareUpgradeUploaded(getString(R.string.no_update_gcast));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mRetrivingGcastUpdateInformation.setVisibility(View.GONE);
                        }
                    });
                     break;
                case Constants.DOWNLOADING_UPDATE_MESSAGE_TIMEOUT:
                    handler.sendEmptyMessage(Constants.PREPARATION_TIMEOUT);
                    break;
            }
        }
    };

    private void showAlertDialogForClickingWrongNetwork() {
        if (!GoogleCastUpdateAfterSac.this.isFinishing()) {
            setAlertDialog1(null);
            AlertDialog.Builder builder = new AlertDialog.Builder(GoogleCastUpdateAfterSac.this);
            String Message = "Update Not Found , Going To Sound Scenes Screen";
            builder.setMessage(Message)
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            getAlertDialog1().dismiss();
                            restartApp(GoogleCastUpdateAfterSac.this);
                        }
                    });

            if (getAlertDialog1() == null) {
                setAlertDialog1(builder.show());
                /*TextView messageView = (TextView) alertDialog1.findViewById(android.R.id.message);
                messageView.setGravity(Gravity.CENTER);*/
            }

            getAlertDialog1().show();

        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_cast_update_after_sac);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        mGoogleCastDetails = (TextView)findViewById(R.id.mGcastUpdateStatus);
        mGoogleCastUpdateBar = (ProgressBar)findViewById(R.id.mProgressBarGCastUpdateStatus);
        mRetrivingGcastUpdateInformation = (ProgressBar)findViewById(R.id.retrivingDeviceInformation);

        mGoogleCastUpdateBar.setVisibility(View.GONE);
        btnDone = (Button) findViewById(R.id.btnDone);
        btnDone.setVisibility(View.GONE);
        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                SharedPreferences sharedPreferences = getApplicationContext()
                        .getSharedPreferences("sac_configured", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                LibreLogger.d(this, "goNext putting values");
                editor.putString("deviceFriendlyName", LibreApplication.sacDeviceNameSetFromTheApp);
                LibreLogger.d(this, "goNext after" + LibreApplication.sacDeviceNameSetFromTheApp);
                editor.commit();
                LibreLogger.d(this, "goNext comitted");
                LibreApplication.sacDeviceNameSetFromTheApp = "";
                removeTheDeviceFromRepo(mLs9DeviceIpAddressForCastUpdate);
                Intent ssid = new Intent(GoogleCastUpdateAfterSac.this,
                        PlayNewActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(ssid);
                finish();
            }
        });

        /**/
        registerForDeviceEvents(this);
        /**sending delayed message to close progress bar*/
        handler.sendEmptyMessage(Constants.PREPARATION_INITIATED);
        handler.sendEmptyMessageDelayed(Constants.PREPARATION_TIMEOUT, TIMEOUT_FOR_GET_UPDATE_STATUS);
        mLs9DeviceIpAddressForCastUpdate =  getIntent().getExtras().getString(ConnectingToMainNetwork.SAC_CURRENT_IPADDRESS);
        send223MsgBoxToKnowGcastStatus();

    }

    @Override
    protected void onResume() {
        /**/
        disableNetworkChangeCallBack();
        disableNetworkOffCallBack();
        registerForDeviceEvents(this);
        mLs9DeviceIpAddressForCastUpdate =  getIntent().getExtras().getString(ConnectingToMainNetwork.SAC_CURRENT_IPADDRESS);
        super.onResume();
    }
    private void showProgressDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
               // mGoogleCastUpdateBar.setVisibility(View.GONE);
                mRetrivingGcastUpdateInformation.setVisibility(View.VISIBLE);
            }
        });
    }

    private void closeProgressDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // mGoogleCastUpdateBar.setVisibility(View.VISIBLE);
                mRetrivingGcastUpdateInformation.setVisibility(View.GONE);
            }
        });
    }
    @Override
    public void onStartComplete() {
        super.onStartComplete();

    }

    private void send223MsgBoxToKnowGcastStatus() {
        new LUCIControl(mLs9DeviceIpAddressForCastUpdate).SendCommand(
                MIDCONST.GCAST_UPDATE_AVAILABLE,
                "",
                LSSDPCONST.LUCI_GET
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_google_cast_update_after_sac, menu);
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

    }

    @Override
    public void deviceGotRemoved(String ipaddress) {

    }

    @Override
    public void onBackPressed() {
        /*super.onBackPressed();
        finish();*/
    }

    @Override
    public void messageRecieved(NettyData packet) {
        LUCIPacket mPacket = new LUCIPacket(packet.getMessage());
        LibreLogger.d(this,"MessageReceived For 223"+ new String(mPacket.getpayload()) + "from Ipaddress" +
        packet.getRemotedeviceIp());
        try {
            if (!mLs9DeviceIpAddressForCastUpdate.equalsIgnoreCase(packet.getRemotedeviceIp()))
                return;
        }catch(Exception e){
            LibreLogger.d(this, "Something wrong here " + new String(mPacket.getpayload()) + "from Ipaddress" + packet.getRemotedeviceIp()
                    + mLs9DeviceIpAddressForCastUpdate);
            e.printStackTrace();
            return;

        }
        switch(mPacket.getCommand()){
            case MIDCONST.GCAST_UPDATE_AVAILABLE:{
                try{
                    String mGcastUpdateAvailablity = new String(mPacket.getpayload());
                    if(!mGcastUpdateAvailablity.equalsIgnoreCase("")){
                        if (handler.hasMessages(Constants.PREPARATION_TIMEOUT)) {
                            handler.removeMessages(Constants.PREPARATION_TIMEOUT);
                            handler.sendEmptyMessage(Constants.PREPARATION_COMPLETED);
                            handler.sendEmptyMessageDelayed(Constants.DOWNLOADING_UPDATE_MESSAGE_TIMEOUT, Constants.DOWNLADING_UPDATE_TIMEOUT);
                        }
                    }

                    //mGoogleCastDetails.setText(mGcastUpdateAvailablity);
                    /* Downloading the Firmware And Updating ... */
                    try{
                        int mProgressValue = Integer.valueOf(mGcastUpdateAvailablity);
                        mGoogleCastUpdateBar.setVisibility(View.VISIBLE);
                        mGoogleCastDetails.setText(getString(R.string.downloadingtheFirmare));
                        mGoogleCastUpdateBar.setProgress(mProgressValue);
                    }catch(Exception e) {
                        e.printStackTrace();
                    }
                    if(mGcastUpdateAvailablity.equalsIgnoreCase(Constants.GCAST_UPDATE_STARTED)){
                        mGoogleCastUpdateBar.setVisibility(View.VISIBLE);
                        mGoogleCastDetails.setText(getString(R.string.downloadingtheFirmare));
                    }else if(mGcastUpdateAvailablity.equalsIgnoreCase(Constants.GCAST_NO_UPDATE)){
                        btnDone.setVisibility(View.VISIBLE);
                        mGoogleCastUpdateBar.setVisibility(View.GONE);
                        mGoogleCastDetails.setText(getString(R.string.noupdateAvailable));
                    }else if(mGcastUpdateAvailablity.equalsIgnoreCase(Constants.GCAST_UPDATE_IMAGE_AVAILABLE)){
                        mGoogleCastUpdateBar.setVisibility(View.VISIBLE);
                        mGoogleCastUpdateBar.setProgress(0);
                        mGoogleCastDetails.setText(getString(R.string.upgrading));
                    }

                    LibreLogger.d(this, "GCAST_UPDATE_AVAILABLE Gcast Update Progress Status " + mGcastUpdateAvailablity);

                }catch(Exception e){
                    mGoogleCastDetails.setText("Exception");
                    e.printStackTrace();
                }
            }
                break;
            case MIDCONST.GCAST_PROGRESS_STATUS:
                try{
                    String mGcastProgresStatus = new String(mPacket.getpayload());
                    LibreLogger.d(this,"GCAST_PROGRESS_STATUS Gcast Update Progress Status " + mGcastProgresStatus);
                    int mProgressValue = 0;
                    try {
                        mProgressValue = Integer.parseInt(mGcastProgresStatus);
                        mGoogleCastUpdateBar.setProgress(mProgressValue);
                    }catch(Exception e){

                    }
                    if(mProgressValue == 100 || mGcastProgresStatus.equalsIgnoreCase(Constants.GCAST_COMPLETE)){
                        mGoogleCastUpdateBar.setProgress(100);
                        btnDone.setVisibility(View.VISIBLE);
                        mGoogleCastDetails.setText(getString(R.string.gcast_update_done) + " , Setup is Complete " );
                    }else if(mProgressValue == 255){
                        showDialogFirmwareUpgradeUploaded(getString(R.string.gcastFailed));
                    }
                }catch(Exception e){
                    e.printStackTrace();
                }
                break;
        }

    }

    @Override
    protected void onDestroy() {
        /* Remove the Download Update Message Timeout */
        if(handler.hasMessages(Constants.DOWNLOADING_UPDATE_MESSAGE_TIMEOUT))
            handler.removeMessages(Constants.DOWNLOADING_UPDATE_MESSAGE_TIMEOUT);
        super.onDestroy();
    }

    private void showDialogFirmwareUpgradeUploaded(String Message) {
        if (!GoogleCastUpdateAfterSac.this.isFinishing()) {
            setAlertDialog1(null);
            AlertDialog.Builder builder = new AlertDialog.Builder(GoogleCastUpdateAfterSac.this);

            builder.setMessage(Message)
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            getAlertDialog1().dismiss();

                            SharedPreferences sharedPreferences = getApplicationContext()
                                    .getSharedPreferences("sac_configured", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            LibreLogger.d(this, "goNext putting values");
                            editor.putString("deviceFriendlyName", LibreApplication.sacDeviceNameSetFromTheApp);
                            LibreLogger.d(this, "goNext after" + LibreApplication.sacDeviceNameSetFromTheApp);
                            editor.commit();
                            LibreLogger.d(this, "goNext comitted");
                            LibreApplication.sacDeviceNameSetFromTheApp ="";
                            /* Remove the Download Update Message Timeout */
                            if(handler.hasMessages(Constants.DOWNLOADING_UPDATE_MESSAGE_TIMEOUT))
                                handler.removeMessages(Constants.DOWNLOADING_UPDATE_MESSAGE_TIMEOUT);
                            removeTheDeviceFromRepo(mLs9DeviceIpAddressForCastUpdate);
                            Intent ssid = new Intent(GoogleCastUpdateAfterSac.this,
                                    PlayNewActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(ssid);
                            finish();
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
    public void removeTheDeviceFromRepo(String ipadddress) {
        if(LUCIControl.luciSocketMap.containsKey(ipadddress)) {
            LUCIControl.luciSocketMap.remove(ipadddress);
            BusProvider.getInstance().post(new RemovedLibreDevice(ipadddress));

            LSSDPNodeDB mNodeDB1 = LSSDPNodeDB.getInstance();
            LSSDPNodes mNode = mNodeDB1.getTheNodeBasedOnTheIpAddress(ipadddress);
            String mIpAddress = ipadddress;


            LSSDPNodeDB mNodeDB = LSSDPNodeDB.getInstance();
            try {
                if (ScanningHandler.getInstance().isIpAvailableInCentralSceneRepo(mIpAddress)) {
                    boolean status = ScanningHandler.getInstance().removeSceneMapFromCentralRepo(mIpAddress);
                    LibreLogger.d(this, "Googlecast Removing centralrepo scenemap" + status);
                }

            } catch (Exception e) {
                LibreLogger.d(this, "Googlecast Removing centralrepo scenemap" + "Removal Exception ");
            }
            mNodeDB.clearNode(mIpAddress);
        }

    }
}
