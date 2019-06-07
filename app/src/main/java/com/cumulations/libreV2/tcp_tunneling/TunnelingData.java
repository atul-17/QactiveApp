package com.cumulations.libreV2.tcp_tunneling;

public class TunnelingData {
    private String remoteClientIp;
    private byte[] remoteMessage;

    public TunnelingData(String remoteClientIp, byte[] remoteMessage) {
        this.remoteClientIp = remoteClientIp;
        this.remoteMessage = remoteMessage;
    }

    public String getRemoteClientIp() {
        return remoteClientIp;
    }

    public byte[] getRemoteMessage() {
        return remoteMessage;
    }
}
