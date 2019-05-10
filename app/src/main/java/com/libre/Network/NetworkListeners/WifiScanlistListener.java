package com.libre.Network.NetworkListeners;

/**
 * Created by suma on 23/11/18.
 */

public interface WifiScanlistListener {
    public void success(Object response);
    public void failure(Exception e);
}
