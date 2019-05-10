package com.libre.Ls9Sac;

/**
 * Created by karunakaran on 7/17/2016.
 */
public class GcastUpdateData {
    private String mGcastUpdate;
    private int mProgressValue;
    private String mIPAddress;
    private String mDeviceName;

    public GcastUpdateData(String mIpadddress,String mDeviceName ,String GcastUpdate, int ProgressValue) {
        this.mIPAddress = mIpadddress;
        this.mDeviceName = mDeviceName;
        this.mGcastUpdate = GcastUpdate;
        this.mProgressValue = ProgressValue;
    }

    public String getmDeviceName() {
        return mDeviceName;
    }

    public void setmDeviceName(String mDeviceName) {
        this.mDeviceName = mDeviceName;
    }

    public String getmGcastUpdate() {
        return mGcastUpdate;
    }

    public void setmGcastUpdate(String mGcastUpdate) {
        this.mGcastUpdate = mGcastUpdate;
    }

    public int getmProgressValue() {
        return mProgressValue;
    }

    public void setmProgressValue(int mProgressValue) {
        this.mProgressValue = mProgressValue;
    }

    public String getmIPAddress() {
        return mIPAddress;
    }

    public void setmIPAddress(String mIPAddress) {
        this.mIPAddress = mIPAddress;
    }
}
