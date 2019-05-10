package com.libre;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
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
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity;
import com.libre.Scanning.Constants;
import com.libre.Scanning.ScanningHandler;
import com.libre.alexa.userpoolManager.AlexaListeners.AlexaLoginListener;
import com.libre.alexa.userpoolManager.AlexaUtils.AlexaConstants;
import com.libre.alexa.userpoolManager.MainActivity;
import com.libre.luci.LSSDPNodes;
import com.libre.luci.Utils;
import com.libre.netty.LibreDeviceInteractionListner;
import com.libre.netty.NettyData;
import com.libre.util.LibreLogger;

import java.net.NetworkInterface;
import java.util.ArrayList;

/**
 * Created by praveena on 7/24/15.
 * <p/>
 * This activity will search for devices in the network and will call Configure or Activity List Activity
 */
public class SpalshScreenActivity extends CTDeviceDiscoveryActivity implements LibreDeviceInteractionListner,AlexaLoginListener {

    private static final java.lang.String TAG = "SplashScreen";
    public static final String APP_RESTARTING = "RestartingApplication";
    HandlerThread mThread;
    ServiceHandler mServiceHandler;

    ArrayList<String> devList = new ArrayList<String>();
    final int DEVICES_FOUND = 412;
    final int TIME_EXPIRED = 413;
    final int MSEARCH_REQUEST = 414;
    final int MEDIA_PROCESS_INIT = 415;
    final int MEDIA_PROCESS_DONE = 416;
    final int NETWORK_IS_NULL = 417;

    boolean deviceFound, freeDeviceFound;
    LibreApplication application;
    private ProgressBar m_progressDlg;

    TextView mAppVersion;
    AlertDialog alert;

    //         for cast
    public String gCastDevice;

    public ScanningHandler mScanHandler = ScanningHandler.getInstance();

    public String getVersion(Context context) {
        String Version = getString(R.string.title_activity_welcome);
        PackageInfo pInfo = null;
        try {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA);

        } catch (PackageManager.NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (pInfo != null)
            Version = pInfo.versionName;

        return Version;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

       
        registerForDeviceEvents(this);
/*
        disableNetworkChangeCallBack();*/

        m_progressDlg = (ProgressBar) findViewById(R.id.loader);

        mAppVersion = (TextView) findViewById(R.id.mAppVersion);

        mAppVersion.setText(getVersion(getApplicationContext()));


        LibreLogger.d(this, "in on create");

		 /*loading circle view */
        application = (LibreApplication) getApplication();


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

        Log.d("Declined", "App is Killed ");
        //finish();
        /* Stopping ForeGRound Service Whenwe are Restarting the APP */
        Intent in = new Intent(SpalshScreenActivity.this, DMRDeviceListenerForegroundService.class);
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



        AlertDialog.Builder builder = new AlertDialog.Builder(SpalshScreenActivity.this);
        builder.setTitle(getString(R.string.googleTerms))
                .setCancelable(false)
                        //                .setMessage(clickableString(getString((R.string.google_tos))))
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ensureAppKill();
                        LibreApplication.GOOGLE_TOS_ACCEPTED=false;

                        application.getScanThread().clearNodes();
                        application.getScanThread().UpdateNodes();


                        handler.sendEmptyMessageDelayed(MSEARCH_REQUEST, 500);
                        handler.sendEmptyMessageDelayed(MSEARCH_REQUEST, 1000);
                        handler.sendEmptyMessageDelayed(MSEARCH_REQUEST, 1500);


                        showLoader();

                        mThread = new HandlerThread(TAG, android.os.Process.THREAD_PRIORITY_BACKGROUND);
                        mThread.start();
                        mServiceHandler = new ServiceHandler(mThread.getLooper());

                        m_progressDlg.setVisibility(View.VISIBLE);


                                                                                                                                                                                                                                                                                     /* initiating the search DMR */
                        getUpnpProcessor().searchDMR();

                        handler.sendEmptyMessage(MEDIA_PROCESS_INIT);


                    }
                })
                .setPositiveButton(getString(R.string.accept), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        SharedPreferences sharedpreferences = getApplicationContext()
                                .getSharedPreferences(Constants.SHOWN_GOOGLE_TOS, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedpreferences.edit();
                        editor.putString(Constants.SHOWN_GOOGLE_TOS, "Yes");
                        editor.commit();
                        LibreApplication.GOOGLE_TOS_ACCEPTED=true;

                        application.getScanThread().clearNodes();
                        application.getScanThread().UpdateNodes();


                        handler.sendEmptyMessageDelayed(MSEARCH_REQUEST, 500);
                        handler.sendEmptyMessageDelayed(MSEARCH_REQUEST, 1000);
                        handler.sendEmptyMessageDelayed(MSEARCH_REQUEST, 1500);


                        showLoader();

                        mThread = new HandlerThread(TAG, android.os.Process.THREAD_PRIORITY_BACKGROUND);
                        mThread.start();
                        mServiceHandler = new ServiceHandler(mThread.getLooper());

                        m_progressDlg.setVisibility(View.VISIBLE);


                                                                                                                                                                                                                                                                                     /* initiating the search DMR */
                        getUpnpProcessor().searchDMR();

                        handler.sendEmptyMessage(MEDIA_PROCESS_INIT);


                    }

                });

        if(alert==null) {
            alert = builder.create();
            alert.setView(gcastDialog);
        }
        if(alert!=null && !alert.isShowing())
            alert.show();
    }

    Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);


            switch (msg.what) {

                case DEVICES_FOUND:

                {
                    LibreLogger.d(this, "Devices found");
                                                                                                                                                                                                                                                                                 /*loading stop with success message*/
                }
                break;

                case TIME_EXPIRED: {

                    m_progressDlg.setVisibility(View.INVISIBLE);
                    if (!SpalshScreenActivity.this.isFinishing()) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(SpalshScreenActivity.this);
                        builder.setMessage(getString(R.string.noNetworkFound))
                                .setCancelable(false)
                                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        System.exit(0);
                                        finish();

                                    }

                                });

                        AlertDialog alert = builder.create();
                        alert.show();
                    }
                }
                break;

                case MSEARCH_REQUEST:
                    LibreLogger.d(this, "Sending the msearch nodes");
                    application.getScanThread().UpdateNodes();

                    break;

                case MEDIA_PROCESS_INIT:
                    showLoader();
                    mServiceHandler.sendEmptyMessage(MEDIA_PROCESS_INIT);
                    break;

                case MEDIA_PROCESS_DONE:

                    ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                    if (activeNetworkInfo == null)
                        return;


                    if (gCastDevice != null) {


                        AlertDialog.Builder builder = new AlertDialog.Builder(SpalshScreenActivity.this);
                        builder.setCancelable(false)
                                .setMessage(gCastDevice)
                                .setNegativeButton(getString(R.string.noThanks), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        goNext();

                                    }
                                })
                                .setPositiveButton(getString(R.string.learnMore), new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        //https://support.google.com/googlecast/answer/6076570
                                        Intent intent = new Intent(Intent.ACTION_VIEW);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        intent.setData(Uri.parse("https://www.google.com/cast/audio/learn"));
                                        startActivity(intent);

                                    }

                                });

                        AlertDialog alert = builder.create();
                        alert.show();
                        gCastDevice = null;
                    } else
                        goNext();

                    break;


            }


        }
    };

    private void goNext() {
        unRegisterForDeviceEvents();
        if (!LibreApplication.getIs3PDAEnabled()){
            loginSuccessful("");
        }else {
            registerAlexaLoginListener(this);
            Intent intent = new Intent(SpalshScreenActivity.this, MainActivity.class);
            intent.putExtra(AlexaConstants.INTENT_FROM_ACTIVITY, AlexaConstants.INTENT_FROM_ACTIVITY_SPLASH_SCREEN);
            startActivity(intent);
        /*    Intent newIntent = new Intent(SpalshScreenActivity.this, ActiveScenesListActivity.class);
            startActivity(newIntent);
            finish();*/
        }
    }

    @Override
    public void deviceDiscoveryAfterClearingTheCacheStarted() {

                                                                                                                                                                                                                                                                    /*
                                                                                                                                                                                                                                                                    Doesnt need to handle
                                                                                                                                                                                                                                                                     */
    }

    @Override
    public void newDeviceFound(LSSDPNodes node) {

        //using shared preference - for gCast configure alertDialog1
/*
        SharedPreferences sharedPreferences = getApplicationContext()
            .getSharedPreferences("sac_configured", Context.MODE_PRIVATE);
        */
/* If this function gets called then it means that there are devices in the network and hence we can throw
          * device found handler *//*

        LibreLogger.d(this, "New device is found with the ip address = " + node.getIP() + "Device State" + node.getDeviceState());
        //////////////////////////////shared preference for gCast
        String str = sharedPreferences.getString("deviceFriendlyName", "");
        if (str != null && str.equalsIgnoreCase(node.getFriendlyname().toString()) && node.getgCastVerision() != null) {
            gCastDevice = node.getFriendlyname().toString();
            //sending time zone to the gCast device
            LUCIControl luciControl = new LUCIControl(node.getIP());
            luciControl.SendCommand(222, "2:" + TimeZone.getDefault().getID().toString(), LSSDPCONST.LUCI_SET);
            //Toast.makeText(getApplicationContext(),str + " in preference",Toast.LENGTH_SHORT).show();
            sharedPreferences.edit().remove("deviceFriendlyName").commit();
        } else if (node.getgCastVerision() == null && str.equalsIgnoreCase(node.getFriendlyname().toString())) {
            sharedPreferences.edit().remove("deviceFriendlyName").commit();
            gCastDevice = null;
        }
*/

        //if (deviceFound == false) {
        if (node.getDeviceState().equals("M")) {
            LibreLogger.d(this, "Master found");
            //unRegisterForDeviceEvents();

            deviceFound = true;
            freeDeviceFound = false;
            handler.sendEmptyMessage(DEVICES_FOUND);

            handler.removeMessages(TIME_EXPIRED);
            //if(! mScanHandler.isIpAvailableInCentralSceneRepo(node.getIP())) // Review
            {
                SceneObject sceneObjec = new SceneObject(" ", node.getFriendlyname(), 0, node.getIP());
                mScanHandler.putSceneObjectToCentralRepo(node.getIP(), sceneObjec);

            }
            devList.add(node.getIP());

        } else if (node.getDeviceState().equals("F")) {
            freeDeviceFound = true;
            handler.sendEmptyMessageDelayed(DEVICES_FOUND, 3000);
            handler.removeMessages(TIME_EXPIRED);
        }
    }

    @Override
    public void deviceGotRemoved(String mIpAddress) {

    }

    @Override
    public void messageRecieved(NettyData data) {

    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        /* App is Not Restarting Properly When a App in SpalshScreen
        * And when a Screen is Transition is happening , In CTDeviceDiscoveryActivity in OnCreate
        * we are Setting connectedSSID , its wrong .
        *
        * Scenario : In Spalshscreen WIFI network is Changed , at that time ,it will launched the ActiveScenes,
        * in Background it will update the connectedSSID , So it will not restart because of that Duplicate /
        * Searching the Renderer issue will come. this si what happening for HN-SA-HN */
        LibreApplication.activeSSID = getConnectedSSIDName(SpalshScreenActivity.this);
        LibreApplication.mActiveSSIDBeforeWifiOff= LibreApplication.activeSSID;
        registerForDeviceEvents(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unRegisterForDeviceEvents();
        //unRegisterForNetworkEvents();
    }

    protected void onPause() {
        super.onPause();
    }
    final private int PERMISSION_DEVICE_DISCOVERY = 100;
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        switch (requestCode) {
            case PERMISSION_DEVICE_DISCOVERY: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && permissions[0].contains(Manifest.permission.READ_EXTERNAL_STORAGE)
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    LibreLogger.d(this, permissions[0]+" storage permission Granted");
                    afterPermit();
                    return;

                } else {

                    // permission denied!
                    LibreLogger.d(this, permissions[0]+" seeked permission Denied. So, requesting again");
                    Toast.makeText(SpalshScreenActivity.this, getString(R.string.storagePermissionToast), Toast.LENGTH_SHORT).show();
                    request();

                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
    @TargetApi(Build.VERSION_CODES.M)
    public void request() {
            /*if (!shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // user checked Never Ask again
                LibreLogger.d(this, "permit READ_EXTERNAL_STORAGE Denied for ever");
                // show dialog
                AlertDialog.Builder requestPermission = new AlertDialog.Builder(SpalshScreenActivity.this);
                requestPermission.setTitle("Permission is Not Available")
                        .setMessage("Permission to access storage is required to load songs,please goto settings and enable \'Storage\' permission and restart the app")
                        .setPositiveButton("Go To Setting", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //navigate to settings
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                            }
                        })
                        .setCancelable(false);
                if (alertDialog1 == null) {
                    alertDialog1 = requestPermission.create();
                }
                if (alertDialog1 != null && !alertDialog1.isShowing())
                    alertDialog1.show();

                return;
            }
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_DEVICE_DISCOVERY);
*/
        return;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void storagePermit(){
        LibreLogger.d(this, "permit storagePermit");
        //seeking permission
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            LibreLogger.d(this, "permit READ_EXTERNAL_STORAGE Granted");
            LibreApplication.mStoragePermissionGranted = true;
            return;
        }else{
            LibreLogger.d(this, "permit READ_EXTERNAL_STORAGE not Granted");
            LibreApplication.mStoragePermissionGranted = false;
            // request is denied by the user. so requesting
          //  request();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PERMISSION_DEVICE_DISCOVERY) {
            LibreLogger.d(this, "permit onActivityResult");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    // SYSTEM_ALERT_WINDOW permission not granted.
                    LibreLogger.d(this,"permit SYSTEM_ALERT_WINDOW not granted");
                    Toast.makeText(SpalshScreenActivity.this, getString(R.string.overlaypermissiontoast), Toast.LENGTH_SHORT).show();
                   // Permission not granted so requesting again
                    requestManageOverlayPermission();

                } else {
                    //SYSTEM_ALERT_WINDOW permission granted. So requesting storage permissions.
                    LibreLogger.d(this, "permit SYSTEM_ALERT_WINDOW granted.So,requesting storage permissions.");
                    afterPermit();
                    //storagePermit();
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void requestManageOverlayPermission() {
        //This is to request user for 'Dangerous Permissions' like SYSTEM_ALERT_WINDOW
        //first check whether the permission is granted
        LibreLogger.d(this, "permit OverlayPermissions");
            LibreLogger.d(this, "permit sdk is >=23");
        afterPermit();
        storagePermit();
            /*if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                //requesting the permission
                startActivityForResult(intent, PERMISSION_DEVICE_DISCOVERY);

            }else{
                LibreLogger.d(this, "permit overlay permissions are granted already. Now asking for storage permissions");
                afterPermit();
        //        storagePermit();
            }*/

        }


    public void onStartComplete() {
        super.onStartComplete();
        /* Sac Configured After that App have to restarted */
/*
        *//* Why We have to Initiate the ServiceHandler After that we havve to Check whether Netif is there or not
        * before that itself we should identify it and Stop the User to go beyond that and ServiceHandler *//*
        NetworkInterface mNetIf = Utils.getActiveNetworkInterface();
        if (mNetIf == null)
            handler.sendEmptyMessage(TIME_EXPIRED);*/

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            LibreLogger.d(this, "permit SDK is >= 23");
            requestManageOverlayPermission();
        }else{
            LibreLogger.d(this, "permit SDK is < 23");
            afterPermit();
        }*/
        afterPermit();
    }

    public void afterPermit(){
        SharedPreferences sharedpreferences = getApplicationContext()
                .getSharedPreferences(Constants.SHOWN_GOOGLE_TOS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString(Constants.SHOWN_GOOGLE_TOS, "Yes");
        editor.commit();
        LibreApplication.GOOGLE_TOS_ACCEPTED=true;
                                                                                                                                                                                                                                                /* Google cast settings*/
        /*
         No need to show the Google dialog in spalsh screen*/
        SharedPreferences sharedPreferences = getApplicationContext()
                .getSharedPreferences(Constants.SHOWN_GOOGLE_TOS, Context.MODE_PRIVATE);
        String shownGoogle = sharedPreferences.getString(Constants.SHOWN_GOOGLE_TOS, null);

        if (shownGoogle == null) {
            showGoogleTosDialog(this);
        } else {
            LibreApplication.GOOGLE_TOS_ACCEPTED=true;
            if(application.getScanThread()!=null ) {
                application.getScanThread().clearNodes();
                application.getScanThread().UpdateNodes();


                handler.sendEmptyMessageDelayed(MSEARCH_REQUEST, 1000);
                handler.sendEmptyMessageDelayed(MSEARCH_REQUEST, 2000);
                handler.sendEmptyMessageDelayed(MSEARCH_REQUEST, 3000);
                handler.sendEmptyMessageDelayed(MSEARCH_REQUEST, 4000);
//            handler.sendEmptyMessageDelayed(MSEARCH_REQUEST, 2500);
//            handler.sendEmptyMessageDelayed(MSEARCH_REQUEST, 3000);


            }
            showLoader();

            mThread = new HandlerThread(TAG, android.os.Process.THREAD_PRIORITY_BACKGROUND);
            mThread.start();
            mServiceHandler = new ServiceHandler(mThread.getLooper());

            m_progressDlg.setVisibility(View.VISIBLE);


                                                                                                                                                                                                                                                                                     /* initiating the search DMR */
            getUpnpProcessor().searchDMR();

            handler.sendEmptyMessage(MEDIA_PROCESS_INIT);
        }


/*
MusicServer musicServer = MusicServer.getMusicServer();
musicServer.prepareMediaServer(this,getUpnpBinder());
*/

/*if (deviceFound){
    unRegisterForDeviceEvents();
    Intent newIntent=new Intent(SpalshScreenActivity.this,ActiveScenesListActivity.class);
    startActivity(newIntent);
    finish();

}else {
    unRegisterForDeviceEvents();
    Intent newIntent=new Intent(SpalshScreenActivity.this,PlayNewActivity.class);
    startActivity(newIntent);
    finish();
}
*/


    }


    protected void onDestroy() {
        super.onDestroy();
        closeLoader();
        unRegisterAlexaLoginListener();
        Log.d("UpnpSplashScreen", "destroyed");

    }


    private void showLoader() {

        /*if (m_progressDlg == null)
            m_progressDlg = ProgressDialog.show(this, "Data Loading", "Loading local Content", true, true, null);

        if (m_progressDlg.isShowing() == false) {
            m_progressDlg.show();
        }*/
    }

    private void closeLoader() {
        //        if (m_progressDlg != null) {
        //            if (m_progressDlg.isShowing() == true) {
        //                m_progressDlg.dismiss();
        //            }
        //        }
    }

    @Override
    public void loginSuccessful(String username) {
            intentToHome(SpalshScreenActivity.this);
    }

    @Override
    public void loginFailed(String Error) {
        intentToHome(SpalshScreenActivity.this);
    }

    class ServiceHandler extends Handler {

        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {

            try {

                //isCommandinProcessing=true;;
                switch (msg.what) {


                    case MEDIA_PROCESS_INIT: {
                        long startTime = System.currentTimeMillis();

                        NetworkInterface mNetIf = Utils.getActiveNetworkInterface();

                        if (mNetIf == null)
                            handler.sendEmptyMessage(TIME_EXPIRED);

                        else {
                            LibreApplication.activeSSID = getConnectedSSIDName(getApplicationContext());
                            LibreApplication.mActiveSSIDBeforeWifiOff= LibreApplication.activeSSID;
                            long finishedTime = System.currentTimeMillis();
                            /*Intent msgIntent = new Intent(SpalshScreenActivity.this, LoadLocalContentService.class);
                            startService(msgIntent);*/

                            if (finishedTime - startTime < 4500)
                                handler.sendEmptyMessageDelayed(MEDIA_PROCESS_DONE, 4500 - (finishedTime - startTime));

                            else
                                handler.sendEmptyMessage(MEDIA_PROCESS_DONE);
                        }
                    }
                }
            } catch (Exception e) {

            }
        }
    }
}