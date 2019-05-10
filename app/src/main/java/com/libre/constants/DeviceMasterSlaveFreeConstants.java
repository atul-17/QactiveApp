package com.libre.constants;

/**
 * Created by karunakaran on 8/26/2015.
 */
public class DeviceMasterSlaveFreeConstants {
    public static final int LUCI_SEND_MASTER_COMMAND = 0x41;
    public static final int LUCI_SEND_SLAVE_COMMAND = 0x42;
    public static final int LUCI_SEND_FREE_COMMAND = 0x43;
    public static final int LUCI_SUCCESS_MASTER_COMMAND = 0x44;
    public static final int LUCI_SUCCESS_SLAVE_COMMAND = 0x45;
    public static final int LUCI_SUCCESS_FREE_COMMAND = 0x46;
    public static final int LUCI_TIMEOUT_COMMAND = 0x47;
    public static final int ACTION_TO_CREATE_SLAVE_INITIATED = 0x48;
    public static final int ACTION_TO_FREE_DEVICE_INITATED = 0x49;
    public static final int ACTION_TO_INDICATE_TIMEOUT = 0x50;


    public static final int JOIN_ALL_TIMEOUT = 0x51;

    public static final int RELEASE_ALL_TIMEOUT = 0x52;
}
