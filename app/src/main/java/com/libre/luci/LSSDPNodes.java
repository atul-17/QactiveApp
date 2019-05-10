package com.libre.luci;
/*********************************************************************************************
 * Copyright (C) 2014 Libre Wireless Technology
 * <p/>
 * "Junk Yard Lab" Project
 * <p/>
 * Libre Sync Android App
 * Author: Subhajeet Roy
 ***********************************************************************************************/

import com.libre.util.LibreLogger;
import com.libre.alexa.DeviceProvisioningInfo;
import com.libre.util.Sources;

import java.net.InetAddress;

/**
 * This is the class at threpresent the Libre devices discovered through LSSDP protocall
 */
public class LSSDPNodes {

    private final String m5GHZ = "5G";
    private final String m2GHZ = "2G";
    public static final String m5GHZ_VALUE = "5GHz";
    public static final String m2GHZ_VALUE = "2.4GHz";
    private InetAddress nodeaddress;
    private String friendlyname;
    private String nodeUDPport;
    private String nodeTCPport;
    private String DeviceState; //?this can be M/F/S M:Audio master S:Audio CLient and Free: Not in DDMS mode
    private String SpeakerType; // L/R/S
    private String P2PState;
    private String USN;
    private String ZoneID;
    private String cSSID;
    private String fwVersion = "";
    private String networkMode;
    private String speechVolume;
    private DeviceProvisioningInfo mdeviceProvisioningInfo;
    private String alexaRefreshToken;
    private String serialNumber;

    public String getSpeechVolume() {
        return speechVolume;
    }

    public void setSpeechVolume(String speechVolume) {
        this.speechVolume = speechVolume;
    }
    public DeviceProvisioningInfo getMdeviceProvisioningInfo() {
        return mdeviceProvisioningInfo;
    }

    public void setMdeviceProvisioningInfo(DeviceProvisioningInfo mdeviceProvisioningInfo) {
        this.mdeviceProvisioningInfo = mdeviceProvisioningInfo;
    }
    public String getAlexaRefreshToken() {
        return alexaRefreshToken;
    }

    public void setAlexaRefreshToken(String alexaRefreshToken) {
        this.alexaRefreshToken = alexaRefreshToken;
    }



    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }



    public String getmTimeZone() {
        return mTimeZone;
    }

    public void setmTimeZone(String mTimeZone) {
        this.mTimeZone = mTimeZone;
    }

    private String mTimeZone = "";
    public boolean ismDeviceType() {
        return mLS9DeviceType;
    }

    public void setmDeviceType(boolean mDeviceType) {
        this.mLS9DeviceType = mDeviceType;
    }

    public boolean ismSpotifyEnabled() {
        return mSpotifyEnabled;
    }

    public void setmSpotifyEnabled(boolean mSpotifyEnabled) {
        this.mSpotifyEnabled = mSpotifyEnabled;
    }

    public boolean mSpotifyEnabled ;
    private boolean mLS9DeviceType = false ;
  private int BT_CONTROLLER;
    public void setBT_CONTROLLER(int value){
        this.BT_CONTROLLER = value;
    }
    public int getBT_CONTROLLER(){
        return BT_CONTROLLER;
    }
    public DeviceCap getmDeviceCap() {
        return mDeviceCap;
    }

    public void setmDeviceCap(DeviceCap mDeviceCap) {
        this.mDeviceCap = mDeviceCap;
    }

    private DeviceCap mDeviceCap;

    public int getCurrentSource() {
        return currentSource;
    }

    public void setCurrentSource(int currentSource) {
        this.currentSource = currentSource;
    }

    private int currentSource =0;

    public int getmPlayStatus() {
        return mPlayStatus;
    }

    public void setmPlayStatus(int mPlayStatus) {
        this.mPlayStatus = mPlayStatus;
    }

    private int mPlayStatus = -1;


    public String getgCastVersionFromEureka() {
        return gCastVersionFromEureka;
    }

    public void setgCastVersionFromEureka(String gCastVersionFromEureka) {
        this.gCastVersionFromEureka = gCastVersionFromEureka;
    }

    private String gCastVersionFromEureka;
    public String getFirstNotification() {
        return firstNotification;
    }

    public void setFirstNotification(String firstNotification) {
        this.firstNotification = firstNotification;
    }

    private String firstNotification;

    public String getgCastVerision() {
        return gCastVerision;
    }

    public void setgCastVerision(String gCastVerision) {
        //gCastVerision="Gcast-12";
        this.gCastVerision = gCastVerision;
    }

    private String gCastVerision;
    private String mWifiBand= m2GHZ_VALUE;

    public STATE_CHANGE_STAGE getCurrentState() {
        return currentState;
    }

    public void setCurrentState(STATE_CHANGE_STAGE currentState) {
        this.currentState = currentState;
    }

    private STATE_CHANGE_STAGE currentState;


    public LSSDPNodes(InetAddress addr, String name, String cport, String tcpport,
                      String state, String type,
                      String p2p, String aUSN, String aZoneID, String cSSID) {
        super();
        this.nodeaddress = addr;
        this.friendlyname = name;
        this.nodeUDPport = cport;
        this.DeviceState = state;
        this.SpeakerType = type;
        this.P2PState = p2p;
        this.USN = aUSN.trim();
        this.nodeTCPport = tcpport;
        this.ZoneID = aZoneID;
        this.cSSID = cSSID;

    }

    public String getmWifiBand() {
        return mWifiBand;
    }

    public void setmWifiBand(String mWifiBand) {
        if(mWifiBand.equalsIgnoreCase(m5GHZ)){
            this.mWifiBand=m5GHZ_VALUE;
        }else{
            this.mWifiBand=m2GHZ_VALUE;
        }

    }

    public enum STATE_CHANGE_STAGE {
        CHANGE_SUCCESS, CHANGE_FAIL, CHANGE_INITIATED, NO_TRACKING;

    }

    public void setFriendlyname(String input) {
        friendlyname = input;
    }

    public void setVersion(String input) {
        fwVersion = input;
    }

    public void setDeviceState(String input) {
        DeviceState = input;
    }

    public String getDeviceState() {
        return DeviceState;
    }

    public String getSpeakerType() {
        return SpeakerType;
    }

    public void setSpeakerType(String inputSpeakerType) {
        SpeakerType = inputSpeakerType;
    }

    public String getFriendlyname() {
        return friendlyname;
    }

    public String setFriendlyname() {
        return friendlyname;
    }

    public InetAddress getNodeAddress() {
        return nodeaddress;
    }

    public void setNodeAddress(InetAddress mAddress) {

        nodeaddress = mAddress;

    }

    public String getPort() {
        return nodeUDPport;
    }

    public String getIP() {
        return nodeaddress.getHostAddress().trim();
    }

    public String getVersion() {
        return fwVersion;
    }

    public String getUSN() {
        return USN.trim();
    }

    public String getP2PState() {
        return P2PState;
    }

    public String getZoneID() {
        return ZoneID;
    }

    public void setZoneID(String mZoneId) {
        this.ZoneID = mZoneId;
    }

    public String getcSSID() {
        return cSSID;
    }

    public void setcSSID(String cSSID) {
        this.cSSID = cSSID;
    }

    @Override
    public String toString() {
        return "{" + "FriendlyName" + ":" + friendlyname +
                ", NodeAddress" + ":" + nodeaddress +
                ", DeviceState" + ":" + DeviceState +
                ", Concurrent_SSID" + ":" + cSSID +
                ", Zone_ID" + ":" + ZoneID + "}";
    }

    public void setNetworkMode(String networkMode) {
        this.networkMode = networkMode;
    }

    public String getNetworkMode() {
        return networkMode;
    }

    public void createDeviceCap(String mDeviceCap,String mHexValue,Sources mSource){
        this.mDeviceCap = new DeviceCap(mDeviceCap,mHexValue,mSource);
    }


    public class DeviceCap {
        public String getmDeviceCap() {
            return mDeviceModel;
        }

        public void setmDeviceCap(String mDeviceCap) {
            this.mDeviceModel = mDeviceCap;
        }

        public String getmHexValue() {
            return mHexValue;
        }

        public void setmHexValue(String mHexValue) {
            this.mHexValue = mHexValue;
        }

        public Sources getmSource() {
            return mSource;
        }

        public void setmSource(Sources mSource) {
            this.mSource = mSource;
        }

        private String mDeviceModel = "";
        private String mHexValue = "";
        private Sources mSource ;
        public DeviceCap(String mDeviceCap,String mHexValue,Sources mSource){
            this.mDeviceModel = mDeviceCap;
            this.mHexValue = mHexValue;
            this.mSource = mSource;
        }
    }
}