package com.libre;

/**
 * Created by root on 11/13/15.
 */

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.libre.Scanning.Constants;
import com.libre.Scanning.ScanThread;
import com.libre.Scanning.ScanningHandler;
import com.libre.app.dlna.dmc.processor.impl.UpnpProcessorImpl;
import com.libre.app.dlna.dmc.processor.interfaces.UpnpProcessor;
import com.libre.app.dlna.dmc.server.ContentTree;
import com.libre.app.dlna.dmc.utility.DMRControlHelper;
import com.libre.app.dlna.dmc.utility.PlaybackHelper;
import com.libre.app.dlna.dmc.utility.UpnpDeviceManager;
import com.libre.constants.CommandType;
import com.libre.constants.LUCIMESSAGES;
import com.libre.constants.MIDCONST;
import com.libre.luci.LSSDPNodeDB;
import com.libre.luci.LUCIControl;
import com.libre.util.LibreLogger;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.model.types.ServiceType;

import java.util.concurrent.ConcurrentHashMap;


/**
 * Created by khajan on 2/9/15.
 * <p/>
 * This service listens for DMR devices in the background.
 * On finding the DMR device, it will create the playbackhelper for the same so that we can
 * operate actions like seek
 */
public class DMRDeviceListenerForegroundService extends Service implements UpnpProcessor.UpnpProcessorListener {
    private static final String LOG_TAG = "CoreUpnpService_ground";

    public static boolean mDMRForegroundServiceRunning = false;
    private LibreApplication m_myApp;
    private UpnpProcessorImpl m_upnpProcessor;
    public boolean isTaskeRemoved;
    private static final long MSEARCH_TIMEOUT = 10*60*1000;

    private Handler mTaskHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        m_myApp = (LibreApplication) getApplication();


        m_upnpProcessor = new UpnpProcessorImpl(this);
        m_upnpProcessor.addListener(this);
        /*Searching the Renderer issue*/
        m_upnpProcessor.addListener(UpnpDeviceManager.getInstance());
        m_upnpProcessor.bindUpnpService();


        mTaskHandler = new Handler();
        mTaskHandler.postDelayed(mMyTaskRunnable, 2000);
    }

    private PendingIntent getNotificationPendingIntent() {

//        Intent activeScene = new Intent(this, ActiveScenesListActivity.class);
//        activeScene.setAction(Constants.ACTION.MAIN_ACTION);
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, activeScene,
//                PendingIntent.FLAG_UPDATE_CURRENT);


        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, ActiveScenesListActivity.class);
        resultIntent.setAction(Constants.ACTION.MAIN_ACTION);
        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        // Adds the back stack for the Intent (but not the Intent itself)
//        stackBuilder.addParentStack(ControlActivity.class);

        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);

        return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(LOG_TAG, "Called onStartCommand");

        String ipAddress = m_myApp.getDeviceIpAddress();

        if (intent == null || intent.getAction() == null)
            return 0;


        if (intent.getAction().equals(Constants.ACTION.STARTFOREGROUND_ACTION)) {
            Log.d(LOG_TAG, "Received Start Foreground Intent ");
            mDMRForegroundServiceRunning = true;


            RemoteViews contentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.custom_notification_layout);

            //set the button listeners
            setListeners(contentView);


            Intent activeScene = new Intent(this, ActiveScenesListActivity.class);
            activeScene.setAction(Constants.ACTION.MAIN_ACTION);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, activeScene,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            Intent stopIntent = new Intent(this, DMRDeviceListenerForegroundService.class);
            stopIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
            PendingIntent stopPendingIntent = PendingIntent.getService(this, 0,
                    stopIntent, 0);

            Bitmap icon = BitmapFactory.decodeResource(getResources(),
                    R.drawable.ic_launcher);
            getNotificationPendingIntent().cancel();

            Notification notification = new NotificationCompat.Builder(this)
                    .setContentTitle(getString(R.string.app_name))
                    .setTicker(getString(R.string.app_name))
                    .setContentText(getString(R.string.notification_ticker_text))
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                    .setContent(contentView)
//                    .setContentIntent(pendingIntent)
                    /* .setContentIntent(getNotificationPendingIntent())*/
                    .setOngoing(true).build();
/*
                        .addAction(android.R.drawable.ic_menu_close_clear_cancel,
                                "Stop", stopPendingIntent).build();
*/


            startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE,
                    notification);

        } else if (intent.getAction().equals(Constants.ACTION.STOPFOREGROUND_ACTION)) {
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancel(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE);

            Log.d(LOG_TAG, "Received Stop Foreground Intent");
            stopForeground(true);
            stopSelf();
        }

        return START_NOT_STICKY;
    }


    public void setListeners(RemoteViews view) {
        //TODO screencapture listener

        Intent stopIntent = new Intent(this, DMRDeviceListenerForegroundService.class);
        stopIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0,
                stopIntent, 0);
        view.setOnClickPendingIntent(R.id.two_image, stopPendingIntent);


    }


    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "Called onDestroyCommnad");
        m_upnpProcessor.unbindUpnpService();
        super.onDestroy();
        LibreLogger.d(this, "OnDestroy of the foreground service is called+CoreUpnpService");

    }

    @Override
    public IBinder onBind(Intent intent) {
        // Used only in case of bound services.
        return null;
    }

    private void ensureDMRPlaybackStopped() {

        ScanningHandler mScanningHandler = ScanningHandler.getInstance();
        ConcurrentHashMap<String, SceneObject> centralSceneObjectRepo = mScanningHandler.getSceneObjectFromCentralRepo();

        /*which means no master present hence all devices are free so need to do anything*/
        if (centralSceneObjectRepo == null || centralSceneObjectRepo.size() == 0) {
            LibreLogger.d(this, "No master present");
            return;
        }

        for (String masterIPAddress : centralSceneObjectRepo.keySet()) {


            RemoteDevice renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(masterIPAddress);
            String mIpaddress = masterIPAddress;
            ScanningHandler mScanHandler = ScanningHandler.getInstance();
            SceneObject currentSceneObject = mScanHandler.getSceneObjectFromCentralRepo(mIpaddress);

            /* Added if its Playing DMR Source then only we have to Stop the Playback */
            try {
                if (renderingDevice != null
                        && currentSceneObject != null
                        && currentSceneObject.getPlayUrl() != null
                        && currentSceneObject.getPlayUrl().contains(LibreApplication.LOCAL_IP)
                        && currentSceneObject.getPlayUrl().contains(ContentTree.AUDIO_PREFIX)
                        && currentSceneObject.getCurrentSource()== Constants.DMR_SOURCE
                        && !currentSceneObject.getPlayUrl().equalsIgnoreCase("")
                ) {

                    LUCIControl control = new LUCIControl(currentSceneObject.getIpAddress());
                    control.SendCommand(MIDCONST.MID_PLAYCONTROL, LUCIMESSAGES.STOP, CommandType.SET);

                }
            } catch (Exception e) {

                LibreLogger.d(this, "Handling the exception while sending the stopMediaServer command ");
            }
        }
    }


    @Override
    public void onRemoteDeviceAdded(RemoteDevice renderingDevice) {

        String renderingUDN = renderingDevice.getIdentity().getUdn().toString();

        LibreLogger.d(this, " Remote device found " + renderingDevice.getIdentity().getDescriptorURL());
        /* here first find the UDN of the current selected device */
        if (renderingDevice != null) {

            RemoteService service = renderingDevice.findService(new ServiceType(DMRControlHelper.SERVICE_NAMESPACE,
                    DMRControlHelper.SERVICE_AVTRANSPORT_TYPE));
            if (service == null) {
                LibreLogger.d(this, "Service is null for " + renderingDevice.getIdentity().getDescriptorURL().getHost());
                return;
            }

            DMRControlHelper dmrControl = new DMRControlHelper(renderingUDN,
                    m_upnpProcessor.getControlPoint(), renderingDevice, service);
            PlaybackHelper m_playbackHelper = new PlaybackHelper(dmrControl);
            /*added for next previous back control*/
//            DMSBrowseHelper m_browseHelper = new DMSBrowseHelper(false, renderingUDN);
//            m_playbackHelper.setDmsHelper(m_browseHelper);
            LibreApplication.PLAYBACK_HELPER_MAP.put(renderingUDN, m_playbackHelper);
            LibreLogger.d(this, "Remote device found and playback helper created" + renderingDevice.getIdentity().getDescriptorURL());

            return;
        }

    }

    @Override
    public void onRemoteDeviceRemoved(RemoteDevice device) {

        LibreLogger.d(this, "device removed " + device.getIdentity().getDescriptorURL());

    }

    @Override
    public void onLocalDeviceAdded(LocalDevice device) {

    }

    @Override
    public void onLocalDeviceRemoved(LocalDevice device) {

    }

    @Override
    public void onStartComplete() {

    }


    @Override
    public void onTaskRemoved(Intent rootIntent) {
        LibreLogger.d(this, "App closed");

        ensureDMRPlaybackStopped();
        ScanThread scan = ScanThread.getInstance();
        scan.setmContext(this);
        scan.close();

        LSSDPNodeDB db = LSSDPNodeDB.getInstance();
        db.clearDB();
        isTaskeRemoved = true;
        ScanningHandler.getInstance().clearSceneObjectsFromCentralRepo();
        Toast.makeText(this, getString(R.string.closingTheApp), Toast.LENGTH_SHORT).show();
        stopForeground(true);
        stopSelf();

        int pid = android.os.Process.myPid();
        android.os.Process.killProcess(pid);

        super.onTaskRemoved(rootIntent);

    }


    Runnable mMyTaskRunnable = new Runnable() {

        @Override

        public void run() {

            m_upnpProcessor.searchDMR();

            LibreLogger.d(this,"Sending periodic DMR search");
            /* and here comes the "trick" */
            mTaskHandler.postDelayed(mMyTaskRunnable, MSEARCH_TIMEOUT);

        }

    };

}
