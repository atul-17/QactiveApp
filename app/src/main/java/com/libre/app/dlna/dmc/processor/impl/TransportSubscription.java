package com.libre.app.dlna.dmc.processor.impl;

import com.libre.util.LibreLogger;

import org.fourthline.cling.controlpoint.SubscriptionCallback;
import org.fourthline.cling.model.gena.CancelReason;
import org.fourthline.cling.model.gena.GENASubscription;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.RemoteService;

/**
 * Created by karunakaran on 2/22/2016.
 */
public class TransportSubscription extends SubscriptionCallback {

    public TransportSubscription(RemoteService service) {
        super(service,600);
    }
    @Override
    protected void failed(GENASubscription genaSubscription, UpnpResponse upnpResponse, Exception e, String s) {
        LibreLogger.d(this,"Failed" + s);
    }

    @Override
    protected void established(GENASubscription genaSubscription) {
        LibreLogger.d(this, "established" + genaSubscription.toString());
    }

    @Override
    protected void ended(GENASubscription genaSubscription, CancelReason cancelReason, UpnpResponse upnpResponse) {

        LibreLogger.d(this,"ended" + genaSubscription.toString() );
    }

    @Override
    protected void eventReceived(GENASubscription genaSubscription) {
        LibreLogger.d(this,"eventReceived" + genaSubscription.toString());
    }

    @Override
    protected void eventsMissed(GENASubscription genaSubscription, int i) {

    }
}
