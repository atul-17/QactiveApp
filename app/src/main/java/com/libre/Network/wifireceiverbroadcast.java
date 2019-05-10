package com.libre.Network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

/**
 * Created by karunakaran on 7/30/2015.
 */
public class wifireceiverbroadcast extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        WifiManager wifiManager = (WifiManager) context
                .getSystemService(Context.WIFI_SERVICE);

        NetworkInfo networkInfo = intent
                .getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        if (networkInfo != null) {
            Log.d("RECEIVER", "Type : " + networkInfo.getType()
                    + "State : " + networkInfo.getState());


            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {

                //get the different network states
                if (networkInfo.getState() == NetworkInfo.State.CONNECTING || networkInfo.getState() ==        NetworkInfo.State.CONNECTED) {
                    
                }
            }
        }

    }
}