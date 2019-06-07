/*
 * ********************************************************************************************
 *  *
 *  * Copyright (C) 2014 Libre Wireless Technology
 *  *
 *  * "Libre Sync" Project
 *  *
 *  * Libre Sync Android App
 *  * Author: Android App Team
 *  *
 *  **********************************************************************************************
 */

package com.libre.Scanning;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.util.Log;

import com.libre.LibreApplication;
import com.libre.SceneObject;
import com.libre.app.dlna.dmc.server.ContentTree;
import com.libre.app.dlna.dmc.utility.PlaybackHelper;
import com.libre.app.dlna.dmc.utility.UpnpDeviceManager;
import com.libre.luci.LSSDPNodeDB;
import com.libre.luci.LSSDPNodes;
import com.libre.util.LibreLogger;
import com.libre.util.Sources;

import org.fourthline.cling.model.meta.RemoteDevice;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by karunakaran on 8/1/2015.
 */
public class ScanningHandler {


    private static ScanningHandler instance = new ScanningHandler();
    public ConcurrentHashMap<String, SceneObject> centralSceneObjectRepo = new ConcurrentHashMap<>();
    public Handler mSACHandler = null;
    public LSSDPNodeDB lssdpNodeDB = LSSDPNodeDB.getInstance();
    public static final int HN_MODE = 0;
    public static final int SA_MODE = 1;

    protected ScanningHandler() {
        // Exists only to defeat instantiation.
    }
    public void clearRemoveShuffleRepeatState(String mIpaddress){
        RemoteDevice renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(mIpaddress);
        if(renderingDevice!=null){
            String renderingUDN = renderingDevice.getIdentity().getUdn().toString();
            PlaybackHelper playbackHelper = LibreApplication.PLAYBACK_HELPER_MAP.get(renderingUDN);
            if(playbackHelper!=null) {
                playbackHelper.setIsShuffleOn(false);
                playbackHelper.setRepeatState(0);
            }
        }
    }
    public static ScanningHandler getInstance() {
        if (instance == null) {
            instance = new ScanningHandler();
        }
        return instance;
    }

    public ConcurrentHashMap<String, SceneObject> getSceneObjectFromCentralRepo() {
        return centralSceneObjectRepo;
    }


    public LSSDPNodes getLSSDPNodeFromCentralDB(String ip) {
        return lssdpNodeDB.getTheNodeBasedOnTheIpAddress(ip);
    }


    public void addHandler(Handler mHandler) {
        mSACHandler = mHandler;
    }

    public void removeHandler(Handler mHandler) {
        mSACHandler = null;
    }

    public Handler getHandler() {
        return mSACHandler;
    }


    public boolean findDupicateNode(LSSDPNodes mNode) {
        boolean found = false;
        int size = lssdpNodeDB.GetDB().size();
        /*preventing array index out of bound*/
        try {
            for (int i = 0; i < size; i++) {
                if (lssdpNodeDB.GetDB().get(i).getIP().equals(mNode.getIP())) {
                    LSSDPNodes oldNode = lssdpNodeDB.GetDB().get(i);
                    lssdpNodeDB.renewLSSDPNodeDataWithNewNode(mNode);
                    found = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            LibreLogger.d(this, "Exception occurred while finding duplicates");
        }
        return found;
    }


    public String parseHeaderValue(String content, String headerName) {
        Scanner s = new Scanner(content);
        s.nextLine(); // Skip the start line

        while (s.hasNextLine()) {
            String line = s.nextLine();
            if (line.equals(""))
                return null;
            int index = line.indexOf(':');
            if (index == -1)
                return null;
            String header = line.substring(0, index);
            if (headerName.equalsIgnoreCase(header.trim())) {
                /*Have Commented the Trim For parsing the DeviceState & FriendlyName with Space*/
                return line.substring(index + 1);
            }
        }

        return null;
    }

    private String parseStartLine(String content) {
        Scanner s = new Scanner(content);
        return s.nextLine();
    }

    public InetAddress getInetAddressFromSocketAddress(SocketAddress mSocketAddress) {
        InetAddress address = null;
        if (mSocketAddress instanceof InetSocketAddress) {
            address = ((InetSocketAddress) mSocketAddress).getAddress();

        }
        return address;
    }

    public SceneObject getSceneObjectFromCentralRepo(String mMasterIp) {
        try {
            return centralSceneObjectRepo.get(mMasterIp);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void clearSceneObjectsFromCentralRepo() {
        centralSceneObjectRepo.clear();
        ;
    }

    public boolean isIpAvailableInCentralSceneRepo(String mMasterIP) {
        return centralSceneObjectRepo.containsKey(mMasterIP);
    }


    public ArrayList<LSSDPNodes> getFreeDeviceList() {
        ArrayList<LSSDPNodes> mFreeDevice = new ArrayList<>();
        LSSDPNodeDB mNodeDB = LSSDPNodeDB.getInstance();
        try {
            for (LSSDPNodes mNode : mNodeDB.GetDB()) {
                if ((mNode.getDeviceState().equals("F"))
                        ) {

                    {

                        if (!mFreeDevice.contains(mNode))
                            mFreeDevice.add(mNode);
                    }
                    LibreLogger.d(this,"Current Source of Node "+ mNode.getFriendlyname() +
                    " is "+mNode.getCurrentSource());
                }
                LibreLogger.d(this, "Get Slave List For MasterIp :" +
                        mFreeDevice.size() + "DB Size" +
                        mNodeDB.GetDB().size());
            }
        } catch (Exception e) {
            LibreLogger.d(this, e.getMessage() + "Exception happened while interating in getFreeDeviceList");
        }
        return mFreeDevice;
    }


    public ArrayList<LSSDPNodes> getSlaveListForMasterIp(String mMasterIp, int mMode) {
        LSSDPNodes mMasterNode = getLSSDPNodeFromCentralDB(mMasterIp);
        LSSDPNodeDB mNodeDB = LSSDPNodeDB.getInstance();
        ArrayList<LSSDPNodes> mSlaveIpList = new ArrayList<>();
        if (mMasterNode.getNetworkMode().contains("WLAN") == false) {
            mMode = SA_MODE;
        } else {
            mMode = HN_MODE;
        }

        try {
            for (LSSDPNodes mSlaveNode : mNodeDB.GetDB()) {

                LibreLogger.d(this, "Name Of the Speaker in the Loop :" + mSlaveNode.getFriendlyname() + "Device State " +
                        mSlaveNode.getDeviceState()
                        + " Zone ID " + mSlaveNode.getZoneID());

                if (mMode == HN_MODE && (mMasterNode != null) && (mSlaveNode != null) &&
                        mSlaveNode.getcSSID().equals(mMasterNode.getcSSID()) &&
                        mSlaveNode.getDeviceState().equals("S")) {
                    LibreLogger.d(this, "HN Mode Slave Speakers Found For MasterIp :" + mSlaveNode.getFriendlyname() + "mMaster" +
                            mMasterNode.getFriendlyname());
                    mSlaveIpList.add(mSlaveNode);
                } else if (mMode == SA_MODE && (mMasterNode != null) && (mSlaveNode != null)
                        && mSlaveNode.getZoneID().equals(mMasterNode.getZoneID()) &&
                        mSlaveNode.getDeviceState().equals("S")) {
                    LibreLogger.d(this, "SA Mode Slave Speakers Found For MasterIp :" + mSlaveNode.getFriendlyname() + "mMaster" +
                            mMasterNode.getFriendlyname());
                    mSlaveIpList.add(mSlaveNode);
                }
            }
            LibreLogger.d(this, "Get Slave List For MasterIp :" + mSlaveIpList.size());
        } catch (Exception e) {
            LibreLogger.d(this, e.getMessage() + "Exception happened while interating in getSlaveListForMasterIp");
        }
        return mSlaveIpList;
    }


    public boolean removeSceneMapFromCentralRepo(String mMasterIp) {
        LibreLogger.d(this, "Clearing :" + mMasterIp);
        centralSceneObjectRepo.remove(mMasterIp);
        clearRemoveShuffleRepeatState(mMasterIp);
        if (isIpAvailableInCentralSceneRepo(mMasterIp)) {
            return false;
        }
        return true;
    }

    public void putSceneObjectToCentralRepo(String nodeMasterIp, SceneObject mScene) {
        centralSceneObjectRepo.put(nodeMasterIp, mScene);
    }

    public int getconnectedSSIDname(Context mContext) {
        WifiManager wifiManager;
        wifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String ssid = wifiInfo.getSSID();
        LibreLogger.d(this, "getConnectedSSIDName wifiInfo = " + wifiInfo.toString());
        if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
            ssid = ssid.substring(1, ssid.length() - 1);
        }

        LibreLogger.d(this, "Connected SSID" + ssid);
        if (ssid.contains(Constants.DDMS_SSID)) {
            return SA_MODE;
        }else{
            if(lssdpNodeDB.GetDB().size()>0){
                LSSDPNodes mSampleNode = lssdpNodeDB.GetDB().get(0);
                if(mSampleNode.getNetworkMode().equalsIgnoreCase("WLAN") == false){
                    return SA_MODE;
                }else{
                    return HN_MODE;
                }
            }
        }
        return HN_MODE;
    }

    public int getNumberOfSlavesForMasterIp(String mMasterIp, int mMode) {
        LSSDPNodes mMasterNode = getLSSDPNodeFromCentralDB(mMasterIp);
        if (mMasterNode == null) {

            ScanningHandler mScanHandler = ScanningHandler.getInstance();
            if (mScanHandler.isIpAvailableInCentralSceneRepo(mMasterIp)){
            LibreLogger.d(this,"Removed the sceneobject explicitly");
                mScanHandler.removeSceneMapFromCentralRepo(mMasterIp);
            }


            /**Node is not present hence removing scene object
             * this is to resolve 0 number of device issue*/
  //          if(mMasterIp!=null || !mMasterIp.isEmpty())
  //              BusProvider.getInstance().post((mMasterIp));
            return 1;
        }
        if (mMasterNode.getNetworkMode().contains("WLAN") == false) {
            mMode = SA_MODE;
        } else {
            mMode = HN_MODE;
        }

        int NumberOfSlaves = 1;
        ArrayList<LSSDPNodes> mNodeDBData = (ArrayList<LSSDPNodes>) lssdpNodeDB.GetDB().clone(); //mNodeDB.GetDB();
        LibreLogger.d(this, "Number Of Speakers in the Network:" + mNodeDBData.size());
        try {
            for (LSSDPNodes mSlaveNode : mNodeDBData) {
                // LibreLogger.d(this," Slave Speakers Found  :"+mSlaveNode.getFriendlyname()+"mMaster"+
                // mSlaveNode.getcSSID() + "Zone ID " + mSlaveNode.getZoneID() + "mMAster AND SLAVE" + mMasterNode.getZoneID());
                if (mMode == HN_MODE && (mMasterNode != null) && (mSlaveNode != null) && (mSlaveNode.getcSSID().equals(mMasterNode.getcSSID()))
                        && (mSlaveNode.getDeviceState().equals("S"))) {
                    LibreLogger.d(this, "HN Mode Slave Speakers Found For MasterIp :" + mSlaveNode.getFriendlyname() + "mMaster" +
                            mMasterNode.getFriendlyname());
                    NumberOfSlaves = NumberOfSlaves + 1;
                } else if (mMode == SA_MODE && (mMasterNode != null) && (mSlaveNode != null) && (mSlaveNode.getZoneID().equals(mMasterNode.getZoneID()))
                        && (mSlaveNode.getDeviceState().equals("S"))) {
                    LibreLogger.d(this, "SA Mode Slave Speakers Found For MasterIp :" + mSlaveNode.getFriendlyname() + "mMaster" +
                            mMasterNode.getFriendlyname());
                    NumberOfSlaves = NumberOfSlaves + 1;
                }
            }
        } catch (Exception e) {
            LibreLogger.d(this, e.getMessage() + "Exception happened while interating in getNumberOfSlavesForMasterIp");
        }
        LibreLogger.d(this, "Number Of Speakers For MasterIp :" + NumberOfSlaves);
        return NumberOfSlaves;
    }
    public Sources getSources(String hexString) {


        String mHexString = hexString;
        Sources mNewSources = new Sources();
        long mInputSource;
        BigInteger value = null;

        if (mHexString.startsWith("0x")) {
            value = new BigInteger(mHexString.substring(2), 16);
        } else {
            value = new BigInteger(mHexString, 16);

        }
        String valueBin = value.toString(2);
        StringBuilder input1 = new StringBuilder();

        //appending 0 incase the string lenght is not 32
        if(valueBin.length() < 32){
            int sizeOfZero = 32 - valueBin.length();
            for(int i=0; i<sizeOfZero;i++){
                valueBin = "0"+valueBin;
            }
        }

        // append a string into StringBuilder input1
        input1.append(valueBin);

        // reverse StringBuilder input1
        input1 = input1.reverse();
        LibreLogger.d(this, "sources bit string array " + input1);

        Log.d("Amrittp", "value of hex: " + mHexString + " value for 0:  " + valueBin.charAt(0) + " value for 28:  " + valueBin.charAt(28));
              /*  +valueBin.charAt(0)
                +valueBin.charAt(1)
                +valueBin.charAt(2)
                +valueBin.charAt(3)
                +valueBin.charAt(4)
                +valueBin.charAt(5)
                +valueBin.charAt(6)
                +valueBin.charAt(7)
                +valueBin.charAt(8)
                +valueBin.charAt(9)
                +valueBin.charAt(10)
                +valueBin.charAt(11)
                +valueBin.charAt(12)
                +valueBin.charAt(13)
                +valueBin.charAt(14)
                +valueBin.charAt(15)
                +valueBin.charAt(16)
                +valueBin.charAt(17)
                +valueBin.charAt(18)
                +valueBin.charAt(19)
                +valueBin.charAt(20)
                +valueBin.charAt(21)
                +valueBin.charAt(22)
                +valueBin.charAt(23)
                +valueBin.charAt(24)
                +valueBin.charAt(25)
                +valueBin.charAt(26)
                +valueBin.charAt(27)
                +valueBin.charAt(28)
                +valueBin.charAt(29)
                +valueBin.charAt(30)
                +valueBin.charAt(31)
                +valueBin.charAt(32)
*/



        for (int position = 0; position < input1.length(); position++) {
            boolean mResult = false;
            if (input1.charAt(position) == '1') {
                mResult = true;
            }
            try {
//                Log.e("getSources","input1 at "+(position+1)+" = "+input1.charAt(position+1));
                switch (position + 1) {
                    case Constants.AIRPLAY_SOURCE:
                        mNewSources.setAirplay(mResult);
                        break;
                    case Constants.DMR_SOURCE:
                        mNewSources.setDmr(mResult);
                        break;
                    case Constants.DMP_SOURCE:
                        mNewSources.setDmp(mResult);
                        break;
                    case Constants.SPOTIFY_SOURCE:
                        mNewSources.setSpotify(mResult);
                        break;
                    case Constants.USB_SOURCE:
                        mNewSources.setUsb(mResult);
                        break;
                    case Constants.SDCARD_SOURCE:
                        mNewSources.setSDcard(mResult);
                        break;
                    case Constants.MELON_SOURCE:
                        mNewSources.setMelon(mResult);
                        break;
                    case Constants.VTUNER_SOURCE:
                        mNewSources.setvTuner(mResult);
                        break;
                    case Constants.TUNEIN_SOURCE:
                        mNewSources.setTuneIn(mResult);
                        break;
                    case Constants.MIRACAST_SOURCE:
                        mNewSources.setMiracast(mResult);
                        break;
                    case Constants.DDMSSLAVE_SOURCE:
                        mNewSources.setDDMS_Slave(mResult);
                        break;
                    case Constants.AUX_SOURCE:
                    case Constants.EXTERNAL_SOURCE:
                        mNewSources.setAuxIn(mResult);
                        break;
                    case Constants.APPLEDEVICE_SOURCE:
                        mNewSources.setAppleDevice(mResult);
                        break;
                    case Constants.DIRECTURL_SOURCE:
                        mNewSources.setDirect_URL(mResult);
                        break;
                    case Constants.BT_SOURCE:
                        mNewSources.setBluetooth(mResult);
                        break;
                    case Constants.DEEZER_SOURCE:
                        mNewSources.setDeezer(mResult);
                        break;
                    case Constants.TIDAL_SOURCE:
                        mNewSources.setTidal(mResult);
                        break;
                    case Constants.FAVOURITES_SOURCE:
                        mNewSources.setFavourites(mResult);
                        break;
                    case Constants.GCAST_SOURCE:
                        mNewSources.setGoogleCast(mResult);
                        break;
                    /*case Constants.EXTERNAL_SOURCE:
                        mNewSources.setExternalSource(mResult);
                        break;*/
                    case Constants.ALEXA_SOURCE:
                        mNewSources.setAlexaAvsSource(mResult);
                        break;

                }
            } catch (Exception e) {
                Log.d("Amritt", "Execption: " + e);
            }
        }
        System.out.println(mNewSources.toPrintString());
        Log.d("Amritt", "mNewSources: " + mNewSources.toPrintString());
        return mNewSources;
    }
    private String mParseHexFromDeviceCap(String mInputString){
        int indexOfSemoColon = mInputString.indexOf("::");
        String mToBeOutput =mInputString.substring(indexOfSemoColon+2,mInputString.length());
        return mToBeOutput;
    }
    public LSSDPNodes getLSSDPNodeFromMessage(SocketAddress socketAddress, String inputString) {
        LSSDPNodes lssdpNode = null;
        String DEFAULT_ZONEID = "239.255.255.251:3000";
        String s1 = parseStartLine(inputString);
        //   Log.e("Scan_Netty", s1);
        if (s1.equals(Constants.SL_NOTIFY)) {
            String dp = inputString;

            String st1 = parseHeaderValue(dp, "PORT");
            String st2 = parseHeaderValue(dp, "DeviceName");
            String st3 = parseHeaderValue(dp, "State");
            String st4 = parseHeaderValue(dp, "USN");
            String netmode = parseHeaderValue(dp, "NetMODE");
            String type = parseHeaderValue(dp, "SPEAKERTYPE");
            String SSID = parseHeaderValue(dp, "DDMSConcurrentSSID");
            String sCastVersionNumber = parseHeaderValue(inputString, "CAST_FWVERSION");
            String fw = parseHeaderValue(dp, "FWVERSION");
            String sDeviceCap = parseHeaderValue(inputString, "SOURCE_LIST");
			String sCastTimeZone = parseHeaderValue(inputString, "CAST_TIMEZONE");
            String sWifiBand = parseHeaderValue(inputString,"WIFIBAND");
            /**LatestDiscoveryChanges
             *
             */
            String firstNotification = parseHeaderValue(dp, "FN");


            if (type == null)
                type = "0";
            String zoneid = parseHeaderValue(dp, "ZoneID");

            if (zoneid == null || zoneid.equals(""))
                zoneid = DEFAULT_ZONEID;
            InetAddress addr = null;
            if (socketAddress instanceof InetSocketAddress) {
                addr = ((InetSocketAddress) socketAddress).getAddress();
            }

            LSSDPNodes lssdpNode1 = new LSSDPNodes(addr, st2, st1, "0", st3, type, "0", st4, zoneid, SSID);

            ScanningHandler mScanHandler = ScanningHandler.getInstance();
            SceneObject sceneObjec = new SceneObject(" ", "", 0, lssdpNode1.getIP());
            if (!mScanHandler.isIpAvailableInCentralSceneRepo(lssdpNode1.getIP())) {
                mScanHandler.putSceneObjectToCentralRepo(lssdpNode1.getIP(), sceneObjec);
            }

            if (sDeviceCap != null) {
                String[] mSplitUpDeviceCap = sDeviceCap.split("::");
                lssdpNode1.createDeviceCap(mSplitUpDeviceCap[0], (mSplitUpDeviceCap[1]), getSources(mParseHexFromDeviceCap(sDeviceCap)));
				}
            if(sWifiBand!=null){
                lssdpNode1.setmWifiBand(sWifiBand);
            }
           /*
                lssdpNode1.setDeviceCap(mSplitUpDeviceCap[0]);
                lssdpNode1.setmSourceList(getSources(mParseHexFromDeviceCap(sDeviceCap)));*//*
            }*/
                /**LatestDiscoveryChanges
                 setting first notification to LSSDP node*/
                if (firstNotification != null)
                    lssdpNode1.setFirstNotification(firstNotification);

                lssdpNode1.setNetworkMode(netmode);

                if (fw != null)
                    lssdpNode1.setVersion(fw);


            /*adding for the google cast version */
                if (sCastVersionNumber != null)
                    lssdpNode1.setgCastVerision(sCastVersionNumber);


            if(sCastTimeZone!=null)
                lssdpNode1.setmTimeZone(sCastTimeZone);
                return lssdpNode1;
            } else if (s1.equals(Constants.SL_OK)) {
                String fw = parseHeaderValue(inputString, "FWVERSION");
                String usn = parseHeaderValue(inputString, "USN");
                String port = parseHeaderValue(inputString, "PORT");
                String devicename = parseHeaderValue(inputString, "DeviceName");
                String state = parseHeaderValue(inputString, "State");
                String netmode = parseHeaderValue(inputString, "NetMODE");
                String speakertype = parseHeaderValue(inputString, "SPEAKERTYPE");
                String tcpport = parseHeaderValue(inputString, "TCPPORT");
                String zone_id = parseHeaderValue(inputString, "ZoneID");
                String SSID = parseHeaderValue(inputString, "DDMSConcurrentSSID");
                String sCastVersionNumber = parseHeaderValue(inputString, "CAST_FWVERSION");
                String sDeviceCap = parseHeaderValue(inputString, "SOURCE_LIST");
                String sCastTimeZone = parseHeaderValue(inputString, "CAST_TIMEZONE");
                String sWifiBand = parseHeaderValue(inputString,"WIFIBAND");
                /**LatestDiscoveryChanges
                 *
                 */
                String firstNotification = parseHeaderValue(inputString, "FN");


            if (speakertype == null) {
                speakertype = "0";
            }
            if (zone_id == null || zone_id.equals("")) {
                zone_id = Constants.DEFAULT_ZONEID;
            }
            InetAddress address = null;
            if (socketAddress instanceof InetSocketAddress) {
                address = ((InetSocketAddress) socketAddress).getAddress();

            }
            Log.e("LSSDP " + "Socket Address : " + socketAddress.toString(), "InetAddress" + address.toString() + "Host ADDress" + address.getHostAddress());

            /* Setting the netmode parameter to make sure that the zoneid is
                    * incremented even for ethernet mode and not just SA mode*/

            lssdpNode = new LSSDPNodes(address, devicename, port, "0", state, speakertype, "0", usn, zone_id, SSID);


            if (sDeviceCap != null) {
                    String[] mSplitUpDeviceCap = sDeviceCap.split("::");
                    lssdpNode.createDeviceCap(mSplitUpDeviceCap[0], (mSplitUpDeviceCap[1]), getSources(mParseHexFromDeviceCap(sDeviceCap)));
                }
                if(sWifiBand!=null){
                    lssdpNode.setmWifiBand(sWifiBand);
                }
                /**LatestDiscoveryChanges
                 setting first notification to LSSDP node*/
                if (firstNotification != null)
                    lssdpNode.setFirstNotification(firstNotification);

                lssdpNode.setNetworkMode(netmode);

            /*adding for the google cast version */
                if (sCastVersionNumber != null)
                    lssdpNode.setgCastVerision(sCastVersionNumber);

                if (fw != null)
                    lssdpNode.setVersion(fw);

                if (sCastVersionNumber != null)
                    lssdpNode.setgCastVerision(sCastVersionNumber);


            if(sCastTimeZone!=null)
                lssdpNode.setmTimeZone(sCastTimeZone);
            return lssdpNode;
        } else {
            return null;
        }
    }

    public LSSDPNodes getMasterIpForSlave(LSSDPNodes mSlaveLssdpNode){
        LSSDPNodes masterLssdpNode = null;
        ArrayList<LSSDPNodes> masterNodesArrayList = LSSDPNodeDB.getInstance().getAllMasterNodes();
        for (LSSDPNodes masterLssdpNodes : masterNodesArrayList){
            String networkMode = mSlaveLssdpNode.getNetworkMode();

                if (networkMode!=null && !networkMode.isEmpty()){
                    //When slave device is in Home network mode
                    if (networkMode.equalsIgnoreCase("WLAN")) {
                        if (masterLssdpNodes.getcSSID().equals(mSlaveLssdpNode.getcSSID())) {
                            masterLssdpNode = masterLssdpNodes;
                        }
                    }
                    //When slave device is in SA  or Etho mode
                    else  /*(networkMode.equalsIgnoreCase("P2P")) */{
                        if (masterLssdpNodes.getZoneID().equals(mSlaveLssdpNode.getZoneID())) {
                            masterLssdpNode= masterLssdpNodes;
                        }
                    }
                }
        }
        return masterLssdpNode;
    }
    public boolean ToBeNeededToLaunchSourceScreen(SceneObject currentSceneObject ){
        if(currentSceneObject!=null) {
            try {
                String masterIPAddress = currentSceneObject.getIpAddress();
                RemoteDevice renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(masterIPAddress);
                String renderingUDN = renderingDevice.getIdentity().getUdn().toString();
                PlaybackHelper playbackHelper = LibreApplication.PLAYBACK_HELPER_MAP.get(renderingUDN);

                try {
                    if (playbackHelper != null
                            && playbackHelper.getDmsHelper()!=null
                            && renderingDevice != null
                            && currentSceneObject != null
                            && currentSceneObject.getCurrentSource() == Constants.DMR_SOURCE
                            && currentSceneObject.getPlayUrl() != null
                            && !currentSceneObject.getPlayUrl().equalsIgnoreCase("")
                            && currentSceneObject.getPlayUrl().contains(LibreApplication.LOCAL_IP)
                            && currentSceneObject.getPlayUrl().contains(ContentTree.AUDIO_PREFIX)
                            && (currentSceneObject.getPlaystatus() != SceneObject.CURRENTLY_STOPPED
                            && currentSceneObject.getPlaystatus() != SceneObject.CURRENTLY_NOTPLAYING)

                            ) {

                        return false;
                    }
                } catch (Exception e) {

                    LibreLogger.d(this, "Handling the exception while sending the stopMediaServer command ");

                }
            }catch(Exception e){
                LibreLogger.d(this, " 1 Handling the exception while sending the stopMediaServer command ");
            }
        }
        return true;

    }
}
