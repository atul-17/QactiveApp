package com.libre.Ls9Sac;

/**
 * Created by karunakaran on 7/17/2016.
 */
public class FwUpgradeData {
    private String updateMsg;
    private int mProgressValue;
    private String mIPAddress;
    private String mDeviceName;

    public FwUpgradeData(String mIpadddress, String mDeviceName , String updateMsg, int ProgressValue) {
        this.mIPAddress = mIpadddress;
        this.mDeviceName = mDeviceName;
        this.updateMsg = updateMsg;
        this.mProgressValue = ProgressValue;
    }

    public String getmDeviceName() {
        return mDeviceName;
    }

    public void setmDeviceName(String mDeviceName) {
        this.mDeviceName = mDeviceName;
    }

    public String getUpdateMsg() {
        return updateMsg;
    }

    public void setUpdateMsg(String updateMsg) {
        this.updateMsg = updateMsg;
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
