package com.libre.app.dlna.dmc.processor.upnp;

import android.Manifest;
import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.cumulations.libreV2.AppUtils;
import com.libre.app.dlna.dmc.processor.impl.UpnpProcessorImpl;
import com.libre.app.dlna.dmc.server.MusicServer;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class LoadLocalContentService extends IntentService {
    // TODO: Rename actions, choose action names that describe tasks that this

    public LoadLocalContentService() {
        super("LoadLocalContentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M
                && !AppUtils.INSTANCE.isPermissionGranted(this, Manifest.permission.READ_EXTERNAL_STORAGE)){
            return;
        }

        UpnpProcessorImpl  upnpProcessor = new UpnpProcessorImpl(this);
        upnpProcessor.bindUpnpService();
        /* Using the applicationContet to avoid memort leak */
        MusicServer.getMusicServer().prepareMediaServer(getApplicationContext(), upnpProcessor.getBinder());
        upnpProcessor.unbindUpnpService();
        Log.e("LoadLocalContentService","started");
    }
}
