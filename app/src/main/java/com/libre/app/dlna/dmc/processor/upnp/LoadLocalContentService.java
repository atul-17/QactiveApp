package com.libre.app.dlna.dmc.processor.upnp;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;

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

        UpnpProcessorImpl  m_upnpProcessor = new UpnpProcessorImpl(this);
        m_upnpProcessor.bindUpnpService();
        MusicServer musicServer = MusicServer.getMusicServer();

        /* Using the applicationContet to avoid memort leak */
        musicServer.prepareMediaServer(getApplicationContext(), m_upnpProcessor.getBinder());
        m_upnpProcessor.unbindUpnpService();
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionFoo(String param1, String param2) {
        // TODO: Handle action Foo
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private void handleActionBaz(String param1, String param2) {
        // TODO: Handle action Baz
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
