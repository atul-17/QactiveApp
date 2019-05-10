package com.libre.LErrorHandeling;

/**
 * Created by khajan on 19/2/16.
 * this class is created to show error messages to user
 * whenever you want show message post this object subscriber will receive and will show accordingly
 */
public class LibreError {

    public String getErrorMessage() {
        return errorMessage;
    }

    private String errorMessage;
    private String ipAddress;

    public int getmTimeout() {
        return mTimeout;
    }

    public void setmTimeout(int mTimeout) {
        this.mTimeout = mTimeout;
    }

    private int mTimeout = 0;

    public LibreError(String ipAddress, String errorMessage, int timeout) {
        this.ipAddress = ipAddress;
        this.errorMessage = errorMessage;
        this.mTimeout = timeout;
    }

    public LibreError(String ipAddress, String errorMessage) {
        this.ipAddress = ipAddress;
        this.errorMessage = errorMessage;
        this.mTimeout = 0;
    }

    @Override
    public String toString() {
        if (ipAddress != null)
            return ipAddress + " " + errorMessage;
        else
            return errorMessage;
    }
}
