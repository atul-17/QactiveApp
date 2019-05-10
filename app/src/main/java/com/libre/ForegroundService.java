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
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import com.libre.Scanning.Constants;
import com.libre.Scanning.ScanningHandler;
import com.libre.app.dlna.dmc.processor.impl.UpnpProcessorImpl;
import com.libre.app.dlna.dmc.utility.PlaybackHelper;
import com.libre.app.dlna.dmc.utility.UpnpDeviceManager;
import com.libre.constants.LSSDPCONST;
import com.libre.constants.LUCIMESSAGES;
import com.libre.constants.MIDCONST;
import com.libre.luci.LUCIControl;
import com.libre.util.LibreLogger;

import org.fourthline.cling.model.meta.RemoteDevice;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by khajan on 2/9/15.
 */
public class ForegroundService extends Service {
    private static final String LOG_TAG = "CoreUpnpService_ground";
    private LibreApplication m_myApp;
    private UpnpProcessorImpl m_upnpProcessor;


    @Override
    public void onCreate() {
        super.onCreate();
        m_myApp = (LibreApplication) getApplication();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(LOG_TAG, "Called onStartCommand");

        String ipAddress = m_myApp.getDeviceIpAddress();

        if (intent == null || intent.getAction() == null)
            return 0;

        m_upnpProcessor = new UpnpProcessorImpl(this);
        m_upnpProcessor.bindUpnpService();


        if (intent.getAction().equals(Constants.ACTION.STARTFOREGROUND_ACTION)) {
            Log.d(LOG_TAG, "Received Start Foreground Intent ");


            Intent nIntent = getPackageManager().
                    getLaunchIntentForPackage("com.libre");
            nIntent.setAction(Constants.ACTION.MAIN_ACTION);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, nIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
//            notificationBuilder.setContentIntent(pendingIntent);


//            Intent notificationIntent = new Intent(this, SpalshScreenActivity.class);
//            notificationIntent.setAction(Constants.ACTION.MAIN_ACTION);
//            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
//                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
//                    notificationIntent, 0);

            Intent previousIntent = new Intent(this, ForegroundService.class);
            previousIntent.setAction(Constants.ACTION.PREV_ACTION);
            PendingIntent ppreviousIntent = PendingIntent.getService(this, 0,
                    previousIntent, 0);

            Intent playIntent = new Intent(this, ForegroundService.class);
            playIntent.setAction(Constants.ACTION.PLAY_ACTION);
            PendingIntent pplayIntent = PendingIntent.getService(this, 0,
                    playIntent, 0);

            Intent nextIntent = new Intent(this, ForegroundService.class);
            nextIntent.setAction(Constants.ACTION.NEXT_ACTION);
            PendingIntent pnextIntent = PendingIntent.getService(this, 0,
                    nextIntent, 0);

            Intent stopIntent = new Intent(this, ForegroundService.class);
            stopIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
            PendingIntent stopPendingIntent = PendingIntent.getService(this, 0,
                    stopIntent, 0);


            Bitmap icon = BitmapFactory.decodeResource(getResources(),
                    R.drawable.ic_launcher);

            Notification.Builder builder = new Notification.Builder(this)
                    .setWhen(System.currentTimeMillis())
                    .setContentIntent(pendingIntent).setOngoing(true);
            Notification notification = builder.getNotification();
            notification.tickerText = "Libre Music";
            notification.icon = R.drawable.ic_launcher;
            RemoteViews contentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.custom_notification_layout);

            //set the button listeners
            setListeners(contentView);

            notification.contentView = contentView;
            notification.flags |= Notification.FLAG_ONGOING_EVENT;
            CharSequence contentTitle = "From Shortcuts";


/*            Notification notification = new NotificationCompat.Builder(this)
                    .setContentTitle("Libre Music Player")
                    .setTicker("Libre Music Player")
                    .setContentText("My Music")
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .addAction(android.R.drawable.ic_media_previous,
                            "Previous", ppreviousIntent)
                    .addAction(android.R.drawable.ic_media_play, "Play",
                            stopPendingIntent)
                    .addAction(android.R.drawable.ic_media_next, "Next",
                            pnextIntent).build();

            RemoteViews contentView=new RemoteViews(this.getPackageName(), R.layout.notification_layout);*/


            startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE,
                    notification);

        } else if (intent.getAction().equals(Constants.ACTION.PREV_ACTION)) {
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancel(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE);
            if (m_myApp.getDeviceIpAddress() == null || m_myApp.getDeviceIpAddress().equalsIgnoreCase(""))
                return 0;

            LUCIControl luciControl = new LUCIControl(m_myApp.getDeviceIpAddress());
            luciControl.SendCommand(MIDCONST.MID_PLAYCONTROL, LUCIMESSAGES.PLAY_PREV, LSSDPCONST.LUCI_SET);
            Log.d(LOG_TAG, "Clicked Previous");
        } else if (intent.getAction().equals(Constants.ACTION.PLAY_ACTION)) {

            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancel(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE);

            if (m_myApp.getDeviceIpAddress() == null || m_myApp.getDeviceIpAddress().equalsIgnoreCase(""))
                return 0;

            LUCIControl luciControl = new LUCIControl(m_myApp.getDeviceIpAddress());
            luciControl.SendCommand(MIDCONST.MID_PLAYCONTROL, LUCIMESSAGES.RESUME, LSSDPCONST.LUCI_SET);
            Log.d(LOG_TAG, "Clicked Play");
        } else if (intent.getAction().equals(Constants.ACTION.NEXT_ACTION)) {

            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancel(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE);

            if (m_myApp.getDeviceIpAddress() == null || m_myApp.getDeviceIpAddress().equalsIgnoreCase(""))
                return 0;
            LUCIControl luciControl = new LUCIControl(m_myApp.getDeviceIpAddress());
            luciControl.SendCommand(MIDCONST.MID_PLAYCONTROL, LUCIMESSAGES.PLAY_NEXT, LSSDPCONST.LUCI_SET);

            Log.d(LOG_TAG, "Clicked Next");
        } else if (intent.getAction().equals(Constants.ACTION.STOPFOREGROUND_ACTION)) {
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancel(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE);
            ensureDMRPlaybackStopped();
            Log.d(LOG_TAG, "Received Stop Foreground Intent");
            stopForeground(true);
            stopSelf();
        }
        return START_STICKY;
    }

    public void setListeners(RemoteViews view) {
        //TODO screencapture listener

        Intent stopIntent = new Intent(this, ForegroundService.class);
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
            if (renderingDevice != null) {
                String renderingUDN = renderingDevice.getIdentity().getUdn().toString();
                PlaybackHelper playbackHelper = LibreApplication.PLAYBACK_HELPER_MAP.get(renderingUDN);
                if (playbackHelper != null)
                    playbackHelper.StopPlayback();
            }
        }
    }

}