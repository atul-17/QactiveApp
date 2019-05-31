package com.libre;
/*********************************************************************************************
 * Copyright (C) 2014 Libre Wireless Technology
 * <p/>
 * "Junk Yard Lab" Project
 * <p/>
 * Libre Sync Android App
 * Author: Subhajeet Roy
 ***********************************************************************************************/

import android.app.Application;
import android.content.Context;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.StrictMode;
import android.support.multidex.MultiDex;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.cumulations.libreV2.AppUtils;
import com.cumulations.libreV2.receiver.GpsStateReceiver;
import com.libre.Ls9Sac.GcastUpdateData;
import com.libre.Scanning.Constants;
import com.libre.Scanning.ScanThread;
import com.libre.Scanning.ScanningHandler;
import com.libre.alexa.MicExceptionListener;
import com.libre.alexa.MicTcpServer;
import com.libre.app.dlna.dmc.utility.PlaybackHelper;
import com.libre.luci.LSSDPNodeDB;
import com.libre.luci.LUCIControl;
import com.libre.util.GoogleTOSTimeZone;
import com.libre.util.LibreLogger;

import io.fabric.sdk.android.Fabric;

import java.net.SocketException;
import java.util.HashMap;
import java.util.LinkedHashMap;

//import cumulations.cutekit.CuteKit;

public class LibreApplication extends Application implements MicTcpServer.MicTcpServerExceptionListener {

    public static boolean haveShowPropritaryGooglePopup=false;
    public static boolean mCleanUpIsDoneButNotRestarted=false;
    public static boolean is3PDAEnabled = /*true*/false;    // to disable cognito user screen

    public static boolean mStoragePermissionGranted = false;

    public static boolean GOOGLE_TOS_ACCEPTED = false;
    public static String thisSACDeviceNeeds226="";
    public static String sacDeviceNameSetFromTheApp = "";
    public static boolean mLuciThreadInitiated = false;

    public static boolean isSacFlowStarted = false;

    public static boolean hideErrorMessage;
    private static final String TAG = LibreApplication.class.getSimpleName();

    public static String activeSSID;
    public static String mActiveSSIDBeforeWifiOff;


    /*this map is having all devices volume(64) based on IP address*/
    public static HashMap<String, Integer> INDIVIDUAL_VOLUME_MAP = new HashMap<>();

    public static HashMap<String, GoogleTOSTimeZone> GOOGLE_TIMEZONE_MAP= new HashMap<>();

    public static LinkedHashMap<String,GcastUpdateData> GCAST_UPDATE_AVAILABE_LIST_DATA = new LinkedHashMap<>();

    /*this map is having zone volume(219) only for master to avoid flickering in active scene*/
    public static HashMap<String, Integer> ZONE_VOLUME_MAP = new HashMap<>();
    public static int mTcpPortInUse = -1;
    private MicTcpServer micTcpServer;

    /**
     * This hashmap stores the DMRplayback helper for a udn
     */
    public static HashMap<String, PlaybackHelper> PLAYBACK_HELPER_MAP = new HashMap<String, PlaybackHelper>();
    public static String LOCAL_UDN = "";
    public static String LOCAL_IP = "";
    public GpsStateReceiver gpsStateReceiver;

    public String getDeviceIpAddress() {
        return deviceIpAddress;
    }

    public void setDeviceIpAddress(String deviceIpAddress) {
        this.deviceIpAddress = deviceIpAddress;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    private String deviceIpAddress;
    ScanThread wt = null;
    Thread scanThread = null;

    public void initiateServices() {
        initLUCIServices();
    }

    public static boolean getIs3PDAEnabled(){
        return is3PDAEnabled;
    }

    private MicExceptionListener micExceptionActivityListener;
    private boolean isGpsReceiverRegistered;

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        Log.i(TAG, "onCreated");
        super.onCreate();
        Fabric.with(this, new Crashlytics());
        MultiDex.install(this);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()   // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .build());
        }

        restrictNetworkToWifi();
        registerGpsStateReceiver();

        /*Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler(){

            @Override
            public void uncaughtException(Thread paramThread, Throwable paramThrowable) {
                myHandlingWhenAppForceStopped (paramThread, paramThrowable);
            }
        });*/
    }

    public void registerGpsStateReceiver(){
        if (isGpsReceiverRegistered)
            return;
        gpsStateReceiver = new GpsStateReceiver();
        /*Keeping receiver active for app level*/
        /*activity.*/
        registerReceiver(gpsStateReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
        isGpsReceiverRegistered = true;
    }

    private void restrictNetworkToWifi(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkRequest.Builder request = new NetworkRequest.Builder();
            request.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);

            if (connectivityManager == null) return;

            connectivityManager.registerNetworkCallback(request.build(), new ConnectivityManager.NetworkCallback() {

                @Override
                public void onAvailable(Network network) {

                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                        ConnectivityManager.setProcessDefaultNetwork(network);
                        LibreLogger.d("", "setProcessDefaultNetwork wifi done");
                    }

                    LibreApplication.LOCAL_IP = AppUtils.INSTANCE.getWifiIp(LibreApplication.this);
                    initiateServices();
                }
            });
        } else {
            LibreApplication.LOCAL_IP = AppUtils.INSTANCE.getWifiIp(LibreApplication.this);
            initiateServices();
        }
    }

    private void myHandlingWhenAppForceStopped(Thread paramThread, Throwable paramThrowable) {
        Log.e("Alert", "Lets See if it Works !!!" +
                "paramThread:::" + paramThread +
                "paramThrowable:::" + paramThrowable);
         /* Killing our Android App with The PID For the Safe Case */
        int pid = android.os.Process.myPid();
        android.os.Process.killProcess(pid);
        System.exit(0);

    }

    public void initLUCIServices() {
        try {
            wt = ScanThread.getInstance();
            LibreLogger.d(this, "Karuna mRunning " + mLuciThreadInitiated);
            if (!LibreApplication.mLuciThreadInitiated) {
                scanThread = new Thread(wt);
                scanThread.start();
                LibreApplication.mLuciThreadInitiated = true;
            }
            micTcpStart();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ScanThread getScanThread() {
        return wt;
    }

    /**
     * clearing all collections related to application
     */
    public void clearApplicationCollections() {
        try {
            Log.d("Scan_Netty", "clearApplicationCollections() called with: " + "");
            PLAYBACK_HELPER_MAP.clear();
            INDIVIDUAL_VOLUME_MAP.clear();
            ZONE_VOLUME_MAP.clear();
            LUCIControl.luciSocketMap.clear();
            LSSDPNodeDB.getInstance().clearDB();
            ScanningHandler.getInstance().clearSceneObjectsFromCentralRepo();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void restart() throws SocketException {
        if (wt != null)
            wt.close();
        if (micTcpServer != null)
            micTcpServer.close();
        try {
            scanThread = new Thread(wt);
            scanThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void micTcpStart() throws SocketException {

        try {
            ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            String connectedSSID = AppUtils.INSTANCE.getConnectedSSID(this);
            if (connectedSSID == null || connectedSSID.toLowerCase().contains(Constants.RIVAA_WAC_SSID.toLowerCase())) {
                return;
            }
            if (mWifi == null || !mWifi.isConnected()) return;

            micTcpServer = MicTcpServer.getMicTcpServer();
            micTcpServer.startTcpServer(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void micTcpClose() throws SocketException {
        try {
            micTcpServer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void micTcpServerException(Exception e) {
        LibreLogger.d(this,"micTcpServerException = "+e.getMessage());
        if (micExceptionActivityListener!=null){
            Log.e("micTcpServerEx","listener "+micExceptionActivityListener.getClass().getSimpleName());
            micExceptionActivityListener.micExceptionCaught(e);
        }
    }

    public void registerForMicException(MicExceptionListener listener){
        micExceptionActivityListener = listener;
    }

    public void unregisterMicException(){
        micExceptionActivityListener = null;
    }

    /*private boolean isMobileDataEnabled() {
        boolean mobileDataEnabled = false;
        LibreLogger.d(this,"suma check if mobile data is enabled or not");
        ConnectivityManager cm1 = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info1 = null;
        if (cm1 != null) {
            info1 = cm1.getActiveNetworkInfo();
        }
        if (info1 != null) {
            if (info1.getType() == ConnectivityManager.TYPE_MOBILE) {
                try {
                    Class cmClass = Class.forName(cm1.getClass().getName());
                    Method method = cmClass.getDeclaredMethod("getMobileDataEnabled");
                    method.setAccessible(true);
                    mobileDataEnabled = (Boolean) method.invoke(cm1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
        return mobileDataEnabled;
    }*/
}
