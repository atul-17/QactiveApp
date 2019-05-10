package com.libre.Network;

/**
 * Created by karunakaran on 7/27/2015.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.libre.util.LibreLogger;

import java.util.List;

public class EnableWifi extends AsyncTask<Void, String, Void> {

    private static final String TAG = "EnableWiFi";
    private Context context;
    private String networkSSID;
    private String networkPasskey;
    private String networkSecurity;
    private boolean sacDevice= false;

    private static int MAXRETRY = 2;
    private static WifiManager wifiManager;


    private OnWiFiConnectionListener wifiConnectionListener;
    private ConnectionReceiver connectionReceiver;

    EnableWifi wifiEnabler;
    public EnableWifi(Context context, String ssid,String passkey,String security,boolean mSacDevice) {
        this.context = context;
        networkSSID = ssid;
        networkPasskey=passkey;
        networkSecurity = security;
        sacDevice = mSacDevice;

        LibreLogger.d(this,"EnableWiFi.Constructor " + " MAXRETRY:"+MAXRETRY);
        LibreLogger.d(this,"EnableWiFi.Constructor " + " ssid:"+ssid);
        LibreLogger.d(this,"EnableWiFi.Constructor " + " passkey:"+passkey);

        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

    }

    @Override
    protected void onPreExecute()
    {
        if(!TextUtils.isEmpty(networkSSID)){
            handleScanResultsAvailable();
            if(!wifiManager.isWifiEnabled()){
                wifiManager.setWifiEnabled(true);
                wifiManager.disconnect();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            if(!isConnectedSSID(context, networkSSID)) {
                if (Connect(context, networkSSID, networkPasskey, networkSecurity, sacDevice)) {
                    wifiConnectionListener.onConnecting("\nWiFi Connected!");
                }
            }
        }
    }

    @Override
    protected Void doInBackground(Void... params) {

        int i = 0;

        if(!TextUtils.isEmpty(networkSSID)){
            publishProgress("isConnected:" + "Connecting.....");

         //  while (!isDoinProgressConnectedSSID(context, networkSSID))
           while(!isConnectedSSID(context, networkSSID))
          {

               //Wait to connect
               try {
                   Thread.sleep(5000);
               } catch (InterruptedException e) {
                   // TODO Auto-generated catch block
                   e.printStackTrace();
               }
           }
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(String... msg) {
        wifiConnectionListener.onConnecting(msg[0]);
    }

    @Override
    protected void onPostExecute(Void result) {

        if(TextUtils.isEmpty(networkSSID)){
            wifiConnectionListener.onWiFiConnected(false, "Empty SSID! Please set correct SSID in WiFi setting!");
        }else{

            if(isConnectedSSID(context, networkSSID)){
                MAXRETRY = -1;
                wifiConnectionListener.onWiFiConnected(true, networkSSID + " is connected!");
                try {
                    context.unregisterReceiver(connectionReceiver);
                }catch(Exception e){

                }
            }else if(MAXRETRY>0){

                MAXRETRY--;
                wifiConnectionListener.onConnecting("Retry Left:"+MAXRETRY);
                /* Re Creation of the Async Task For ReCreation */
                wifiEnabler = new EnableWifi(context,networkSSID,networkPasskey,networkSecurity,sacDevice);
                wifiEnabler.setWifiConnectionListener(wifiConnectionListener);
                wifiEnabler.execute();

            }else{
                try {
                    context.unregisterReceiver(connectionReceiver);
                }catch(Exception e){

                }
                wifiConnectionListener.onWiFiConnected(false, networkSSID+" not found! or signal is very weak!");
            }
        }
    }


    /**
     * Broadcast receiver for connection related events
     */
    private class ConnectionReceiver extends BroadcastReceiver {

        WifiManager wifi = (WifiManager) context.getSystemService(context.WIFI_SERVICE);


        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            SupplicantState supState;
            WifiInfo wifiInfo = wifi.getConnectionInfo();
            Log.e("WifiInfo",""+wifiInfo.getSupplicantState());
            Log.e("WifiInfo",""+wifiInfo.getSSID());
            {
                wifiConnectionListener.onConnecting(""+wifiInfo.getSupplicantState());
            }
            if(wifiInfo.getSupplicantState() == SupplicantState.DISCONNECTED){
                wifiConnectionListener.onError("Failure Of Connection"+wifiInfo.getSSID());
            }
            if (networkInfo.isConnected()) {
                if (wifiInfo.getSSID().replace("\"", "").equals(networkSSID)) {
                    LibreLogger.d(this, networkSSID+ "WifiInfoSSID"+ wifiInfo.getSSID());
                    if(wifiEnabler!=null) {
                        wifiEnabler.cancel(true);
                    }
                    wifiConnectionListener.onWiFiConnected(true,"Connected Successfully");

                }
            }
        }
    }

    public void handleScanResultsAvailable() {
        WifiConnection mWifiConnect = WifiConnection.getInstance();
        wifiManager.startScan();
        List<ScanResult> list = wifiManager.getScanResults();

        if (list != null) {
            for (int i = list.size() - 1; i >= 0; i--) {

                final ScanResult scanResult = list.get(i);
                if (scanResult == null) {
                    continue;
                }

                if (TextUtils.isEmpty(scanResult.SSID)) {
                    continue;
                }
             //   mWifiConnect.putWifiScanResultSecurity(scanResult.SSID, scanResult.capabilities);

            }
        }
    }
    public String getconnectedSSIDname()
    {

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String ssid = wifiInfo.getSSID();
        Log.d(TAG,"getConnectedSSIDName wifiInfo = "+wifiInfo.toString());
        if (ssid.startsWith("\"") && ssid.endsWith("\"")){
            ssid = ssid.substring(1, ssid.length()-1);
        }
        Log.d(TAG, "Connected SSID" + ssid);
        return ssid;
    }
    /**
     *
     * @param context
     * @param networkSSID
     * @return true
     */
    public boolean Connect(Context context, String networkSSID,String password,String mSecurity,boolean mSacDevice){
        LibreLogger.d(this,"EnableWiFi.Constructor " + " MAXRETRY:"+MAXRETRY);
        LibreLogger.d(this,"EnableWiFi.Constructor " + " ssid:"+networkSSID);
        LibreLogger.d(this,"EnableWiFi.Constructor " + " passkey:"+password);

        WifiConfiguration wifiConfiguration = new WifiConfiguration();
        if(password==null||password.equals(""))
        {
            if(mSacDevice){

                wifiConfiguration.status=WifiConfiguration.Status.ENABLED;
                Log.v(TAG," ConnectWAC ConnectDDMSOOH SSID= "+networkSSID);
                wifiConfiguration.SSID = "\"".concat(networkSSID).concat("\"");
                wifiConfiguration.allowedAuthAlgorithms.clear();
                wifiConfiguration.allowedGroupCiphers.clear();
                wifiConfiguration.allowedPairwiseCiphers.clear();
                wifiConfiguration.allowedProtocols.clear();
                wifiConfiguration.allowedKeyManagement.clear();
                wifiConfiguration.priority = 1;
                wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

            }else {
                wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                Log.d(TAG, " Connect ConnectDDMSOOH if(Password.equals)");
                wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN); // For WPA2
                wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.WPA); // For WPA
                wifiConfiguration.priority = 1;
            }

        }
        else {

            Log.d(TAG, " Connect ConnectDDMSOOH inPassword = " + password);
            handleScanResultsAvailable();;
            mSecurity = WifiConnection.getInstance().getMainSSIDSec();
            Log.d(TAG, " Connect ConnectDDMSOOH insecurity = " + mSecurity);
            if (mSecurity.contains("WEP")) {
                wifiConfiguration.SSID = "\"" + networkSSID + "\"";
                wifiConfiguration.wepKeys[0] = "\"" + password + "\"";
                wifiConfiguration.wepTxKeyIndex = 0;
                wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            } else if (mSecurity.contains("PSK")) {
                Log.d(TAG, " Connect ConnectDDMSOOH inPassword = " + "PSK");
                wifiConfiguration.SSID = "\"" + networkSSID + "\"";
                wifiConfiguration.preSharedKey = "\"" + password + "\"";
                wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN); // For WPA2
                wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.WPA); // For WPA

            }
        }

        wifiManager.disconnect();
        wifiManager.saveConfiguration();
        int netId =wifiManager.addNetwork(wifiConfiguration);

        /* If netId = -1 , then it Couldnt able to add to network So we are fetching the already configured Network*/
        if(netId==-1){
            List<WifiConfiguration> listwifiConfiguration;
            listwifiConfiguration = wifiManager.getConfiguredNetworks();

            for (int i = 0; i < listwifiConfiguration.size(); i++) {
                String configSSID = listwifiConfiguration.get(i).SSID;
                Log.d(TAG,"Config SSID"+configSSID+"Active SSID"+wifiConfiguration.SSID);
                if(configSSID.equals(wifiConfiguration.SSID))
                {
                    netId =listwifiConfiguration.get(i).networkId;
                    break;
                }
                else
                    Log.e(TAG,"network is not there in wifi Manger"+netId);
            }
        }
        connectionReceiver = new ConnectionReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        context.registerReceiver(connectionReceiver, intentFilter);

        wifiManager.enableNetwork(netId, true);
        wifiManager.reconnect();
     //   context.unregisterReceiver(context);

        return true;

    }

    public static boolean isConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = null;

        if (connectivityManager != null) {
            networkInfo = connectivityManager.getActiveNetworkInfo();
        }

        return networkInfo == null ? false : networkInfo.getState() == NetworkInfo.State.CONNECTED;
    }

    /* This method to know about the current Connected SSID and Input SSID are Same*/
    public boolean isConnectedSSID(Context context, String networkSSID){

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        wifiConnectionListener.onConnecting("Connected SSID:" + wifiInfo.getSSID());

        String  mSSID = "\"" + networkSSID + "\"";

        return (wifiInfo.getSSID() == null || wifiInfo.getSSID().contains(mSSID.substring(1, mSSID.length() - 1)));

    }
    static String IsScanResult(Context context,String netssid){
        List<ScanResult> list = wifiManager.getScanResults();

        if (list != null) {
            int i = list.size()-1;
            while(i >= 0) {
                final ScanResult scanResult = list.get(i);

                if (scanResult == null) {
                    continue;
                }

                if (TextUtils.isEmpty(scanResult.SSID)) {
                    continue;
                }

                if(scanResult.SSID.equals(netssid)){
                    return scanResult.capabilities;
                }
                i--;
            }
        }

        return "";
    }
    /**
     * Check if netssid is in the wifi scan list
     * @param context
     * @param netssid
     * @return true if the netssid is in WiFi scan list, false otherwise
     */
    static boolean InScanResult(Context context, String netssid){

        List<ScanResult> list = wifiManager.getScanResults();

        if (list != null) {
            int i = list.size()-1;
            while(i >= 0) {
                final ScanResult scanResult = list.get(i);

                if (scanResult == null) {
                    continue;
                }

                if (TextUtils.isEmpty(scanResult.SSID)) {
                    continue;
                }
                String tocompare = scanResult.SSID.toString();
                if(tocompare.compareTo(netssid)==0){

                    return true;
                }
                i--;
            }
        }

        return false;
    }


    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public String getNetworkSSID() {
        return networkSSID;
    }

    public void setNetworkSSID(String networkSSID) {
        this.networkSSID = networkSSID;
    }

    public OnWiFiConnectionListener getWifiConnectionListener() {
        return wifiConnectionListener;
    }

    public void setWifiConnectionListener(
            OnWiFiConnectionListener wifiConnectionListener) {
        this.wifiConnectionListener = wifiConnectionListener;
    }


    public interface OnWiFiConnectionListener {
        void onConnecting(String msg);
        void onWiFiConnected(boolean is_connected_to_correct_ssid, String failure_reason_if_any);
        void onError(String msg);

    }


    public static int getMAXRETRY() {
        return MAXRETRY;
    }

    public static void setMAXRETRY(int mAXRETRY) {
        MAXRETRY = mAXRETRY;
    }

}