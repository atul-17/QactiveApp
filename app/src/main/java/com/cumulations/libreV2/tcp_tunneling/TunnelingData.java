package com.cumulations.libreV2.tcp_tunneling;

public class TunnelingData {
    private String remoteClientIp;
    private byte[] remoteMessage;
    private boolean isACKData;

    public TunnelingData(String remoteClientIp, byte[] remoteMessage) {
        this.remoteClientIp = remoteClientIp;
        this.remoteMessage = remoteMessage;

        if (remoteMessage.length>=4) {
            byte commandType = remoteMessage[0];
            byte commandMode = remoteMessage[1];
            byte commandStatus = remoteMessage[4];

            if (commandType == (byte) 0x01
                    && commandMode == (byte) 0xFF
                    && (commandStatus == (byte) 0x00 || commandStatus == (byte) 0x01)) {
                isACKData = true;
            }
        }
    }

    public String getRemoteClientIp() {
        return remoteClientIp;
    }

    public byte[] getRemoteMessage() {
        return remoteMessage;
    }

    public boolean isACKData() {
        return isACKData;
    }
}
