package com.libre.qactive;

/**
 * Created by root on 11/13/15.
 */

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;

import android.widget.RemoteViews;

import com.cumulations.libreV2.activity.CTHomeTabsActivity;
import com.cumulations.libreV2.model.SceneObject;
import com.libre.qactive.Scanning.Constants;
import com.libre.qactive.Scanning.ScanThread;
import com.libre.qactive.Scanning.ScanningHandler;

import com.libre.qactive.app.dlna.dmc.processor.impl.UpnpProcessorImpl;
import com.libre.qactive.app.dlna.dmc.processor.interfaces.UpnpProcessor;
import com.libre.qactive.app.dlna.dmc.server.ContentTree;
import com.libre.qactive.app.dlna.dmc.utility.DMRControlHelper;
import com.libre.qactive.app.dlna.dmc.utility.PlaybackHelper;
import com.libre.qactive.app.dlna.dmc.utility.UpnpDeviceManager;
import com.libre.qactive.constants.LSSDPCONST;
import com.libre.qactive.constants.LUCIMESSAGES;
import com.libre.qactive.constants.MIDCONST;
import com.libre.qactive.luci.LSSDPNodeDB;
import com.libre.qactive.luci.LUCIControl;
import com.libre.qactive.util.LibreLogger;
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
    public static boolean mDMRForegroundServiceRunning = false;
    private UpnpProcessorImpl m_upnpProcessor;
    public boolean isTaskeRemoved;
    private static final long MSEARCH_TIMEOUT = 10 * 60 * 1000;

    private Handler mTaskHandler;

    @Override
    public void onCreate() {
        LibreLogger.d(this, "onCreate");
        super.onCreate();
        m_upnpProcessor = new UpnpProcessorImpl(this);
        m_upnpProcessor.addListener(this);
        /*Searching the Renderer issue*/
        m_upnpProcessor.addListener(UpnpDeviceManager.getInstance());
        m_upnpProcessor.bindUpnpService();

        mTaskHandler = new Handler();
        mTaskHandler.postDelayed(mMyTaskRunnable, 2000);
    }

    private PendingIntent getNotificationPendingIntent() {

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, CTHomeTabsActivity.class);
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

        LibreLogger.d(this, "onStartCommand, action = "+intent.getAction());

//        if (intent == null || intent.getAction() == null) {
//            return Service.START_STICKY_COMPATIBILITY;
//        }


        if (intent.getAction() != null) {
            if (intent.getAction().equals(Constants.ACTION.STARTFOREGROUND_ACTION)) {

                if (mDMRForegroundServiceRunning) {
                    return Service.START_STICKY_COMPATIBILITY;
                }

                getNotificationPendingIntent().cancel();
                mDMRForegroundServiceRunning = true;
                RemoteViews contentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.custom_notification_layout);
                //set the button listeners
                setStopAction(contentView);

                Intent homeTabs = new Intent(this, CTHomeTabsActivity.class);
                homeTabs.setAction(Constants.ACTION.MAIN_ACTION);
//                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, homeTabs,
//                        PendingIntent.FLAG_UPDATE_CURRENT);
//
//                Intent stopIntent = new Intent(this, DMRDeviceListenerForegroundService.class);
//                stopIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
//                PendingIntent stopPendingIntent = PendingIntent.getService(this, 0,
//                        stopIntent, 0);

                Bitmap icon = BitmapFactory.decodeResource(getResources(), R.mipmap.app_icon_square);

                Notification notification = /*new NotificationCompat.Builder(this)*/
                        getNotificationBuilder(this,
                                getString(R.string.foreground_channel_id),
                                NotificationManagerCompat.IMPORTANCE_LOW)
                                .setSmallIcon(R.mipmap.app_icon_square)
                                .setContentTitle(getString(R.string.app_name))
                                .setContentText(getString(R.string.notification_ticker_text))
//                                .setTicker(getString(R.string.app_name))
//                                .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                                .setContent(contentView)
                                .setContentIntent(getNotificationPendingIntent())
//                                .addAction(android.R.drawable.ic_menu_close_clear_cancel,
//                                        "Stop", stopPendingIntent)
                                .setOngoing(true)
                                .build();

                LibreLogger.d(this, "onStartCommand startForeground");
                startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, notification);

            } else if (intent.getAction().equals(Constants.ACTION.STOPFOREGROUND_ACTION)) {
                NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.cancel(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE);
                LibreLogger.d(this, "onStartCommand stopForeground");
                stopForeground(true);
                stopSelf();
            }
        }

        return START_NOT_STICKY;
    }


    public void setStopAction(RemoteViews view) {
        //TODO screencapture listener

        Intent stopIntent = new Intent(this, DMRDeviceListenerForegroundService.class);
        stopIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0,
                stopIntent, 0);
        view.setOnClickPendingIntent(R.id.iv_stop_foreground, stopPendingIntent);
    }


    @Override
    public void onDestroy() {
        LibreLogger.d(this, "onDestroy");
        m_upnpProcessor.unbindUpnpService();
        mDMRForegroundServiceRunning = false;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Used only in case of bound services.
        return null;
    }

    private void ensureDMRPlaybackStopped() {

        ScanningHandler mScanningHandler = ScanningHandler.getInstance();
        ConcurrentHashMap<String, SceneObject> centralSceneObjectRepo = mScanningHandler.getSceneObjectMapFromRepo();

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
                        && currentSceneObject.getCurrentSource() == Constants.DMR_SOURCE
                        && !currentSceneObject.getPlayUrl().equalsIgnoreCase("")
                ) {

                    LUCIControl control = new LUCIControl(currentSceneObject.getIpAddress());
                    control.SendCommand(MIDCONST.MID_PLAYCONTROL, LUCIMESSAGES.STOP, LSSDPCONST.LUCI_SET);

                }
            } catch (Exception e) {
                LibreLogger.d(this, "ensureDMRPlaybackStopped exception : "+e.getMessage());
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
        LibreLogger.d(this, "onTaskRemoved");

        ensureDMRPlaybackStopped();
        ScanThread scan = ScanThread.getInstance();
        scan.setmContext(this);
        scan.close();

        LSSDPNodeDB db = LSSDPNodeDB.getInstance();
        db.clearDB();
        isTaskeRemoved = true;
        ScanningHandler.getInstance().clearSceneObjectsFromCentralRepo();
//        Toast.makeText(this, getString(R.string.closingTheApp), Toast.LENGTH_SHORT).show();
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
            LibreLogger.d(this, "Sending periodic DMR search");
            /* and here comes the "trick" */
            mTaskHandler.postDelayed(mMyTaskRunnable, MSEARCH_TIMEOUT);
        }

    };

    public NotificationCompat.Builder getNotificationBuilder(Context context, String channelId, int importance) {
        NotificationCompat.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            prepareChannel(context, channelId, importance);
            builder = new NotificationCompat.Builder(context, channelId);
        } else {
            builder = new NotificationCompat.Builder(context);
        }
        return builder;
    }

    @TargetApi(Build.VERSION_CODES.O)
    private static void prepareChannel(Context context, String id, int importance) {
        final String appName = context.getString(R.string.app_name);
        String description = context.getString(R.string.notifications_channel_description);
        final NotificationManager nm = (NotificationManager) context.getSystemService(Activity.NOTIFICATION_SERVICE);

        if (nm != null) {
            NotificationChannel nChannel = nm.getNotificationChannel(id);
            if (nChannel == null) {
                nChannel = new NotificationChannel(id, appName, importance);
                nChannel.setDescription(description);
                nm.createNotificationChannel(nChannel);
            }
        }
    }

}
