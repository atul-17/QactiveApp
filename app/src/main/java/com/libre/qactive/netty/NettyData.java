package com.libre.qactive.netty;

/**
 * Created by praveena on 7/14/15.
 *
 * This class will carry the data received by the socket
 *
 */
public class NettyData {


    public String getRemotedeviceIp() {
        return remotedeviceIp;
    }

    public void setRemotedeviceIp(String remotedeviceIp) {
        this.remotedeviceIp = remotedeviceIp;
    }

    public byte[] getMessage() {
        return message;
    }

    public void setMessage(String sage) {
        this.message = message;
    }

    public NettyData(String remotedevice, byte[] string) {

        remotedeviceIp=remotedevice;
        message=string;
    }


    public String remotedeviceIp;
    public byte[] message;


}
