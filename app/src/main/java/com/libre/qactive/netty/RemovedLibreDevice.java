package com.libre.qactive.netty;

/**
 * Created by karunakaran on 4/28/2016.
 */
public class RemovedLibreDevice {


    public RemovedLibreDevice(String ipAddress){
        this.IpAddress=ipAddress;
    }
    private String IpAddress = "";


    public String getmIpAddress() {
        return IpAddress;
    }

    public void setmIpAddress(String mIpAddress) {
        this.IpAddress = mIpAddress;
    }







}
