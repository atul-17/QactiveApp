package com.libre.ManageDevice;

import android.util.Log;

import com.libre.Scanning.ScanningHandler;
import com.libre.luci.LSSDPNodeDB;
import com.libre.luci.LSSDPNodes;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by karunakaran on 8/5/2015.
 */
public class ManageDeviceHandler {
    public LinkedHashMap<String,LSSDPNodes> deviceIPNode = new LinkedHashMap<>();
    public ScanningHandler mScanHandler = ScanningHandler.getInstance();
    public String selectedIpaddress = "";
    public int selectedPosition=-1;
    private static ManageDeviceHandler instance = new ManageDeviceHandler();
    LSSDPNodeDB nodesDB = LSSDPNodeDB.getInstance();

    private LinkedHashMap<String,String[]> mMapMasterSlave = new LinkedHashMap<>();

    public void addmMapMasterSlave(String mMasterIp,String[] mSlaveIps){
        mMapMasterSlave.put(mMasterIp,mSlaveIps);
    }

    public void removemMapMasterSlave(String mMasterIp,String[] mSlaveIps){
        mMapMasterSlave.remove(mMasterIp);
    }

    public String[] getmMapMasterSlave(String mMasterIp){
        return mMapMasterSlave.get(mMasterIp);
    }



    public ArrayList<String> slaveIp ;
    protected ManageDeviceHandler() {
        // Exists only to defeat instantiation.
    }
    public static ManageDeviceHandler getInstance() {
        if(instance == null) {
            instance = new ManageDeviceHandler();
        }
        return instance;
    }

    public  int getPortfromURL(String url) {


        URL aURL = null;
        try {
            aURL = new URL(url);
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if(aURL==null)
            Log.v("ManageDeviceHandler","Failed to create URL");
        return aURL != null ? aURL.getPort() : 0;

    }
    public String getZoneID(){
        int port = 3000;

        Iterator<LSSDPNodes> iterator = nodesDB.GetDB().iterator();
        while (iterator.hasNext()) {
            LSSDPNodes node = iterator.next();
            if(node.getDeviceState().equals("M"))
            {
                String zoneId = node.getZoneID();
                //int temp = getPortfromURL("http://zoneId");
               // i/f(port<=temp)
                  //  port = temp+100;
            }

        }
        return "239.255.255.251:"+port;
    }
    public String getUniqueZoneID()
    {
        int port=3000;

        Iterator<LSSDPNodes> iterator = nodesDB.GetDB().iterator();
        while (iterator.hasNext()) {
            LSSDPNodes node = iterator.next();
            if(node.getDeviceState().equals("M"))
            {
                String zoneId = node.getZoneID();
                int temp = getPortfromURL("http://"+zoneId);
                if(port<=temp)
                    port = temp+100;
            }

        }
        return "239.255.255.251:"+port;
    }
}
