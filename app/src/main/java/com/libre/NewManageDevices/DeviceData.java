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

package com.libre.NewManageDevices;

/**
 * Created by praveena on 10/19/15.
 */
public class DeviceData {

    int volumeInPercentage;
    String deviceName;
    /* Slave,master etc*/
    String deviceState;
    Boolean isMuted;
    /* LEFT,Right etc */
    String speakerType;
    private int volumeValueInPercentage;
    private String ipAddress;



    DeviceData(String deviceName,String ipAddress){
        this.deviceName=deviceName;
        this.ipAddress=ipAddress;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public void setVolumeValueInPercentage(int volumeValueInPercentage) {
        this.volumeValueInPercentage = volumeValueInPercentage;
    }

    public int getVolumeValueInPercentage() {
        return volumeValueInPercentage;
    }

    public String getIpAddress() {
        return ipAddress;
    }
}
