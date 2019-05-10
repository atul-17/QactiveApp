package com.libre.luci;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.libre.util.LibreLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * ******************************************************************************************
 * <p/>
 * Copyright (C) 2014 Libre Wireless Technology
 * <p/>
 * "Junk Yard Lab" Project
 * <p/>
 * Libre Sync Android App
 * Author: Subhajeet Roy
 * <p/>
 * *********************************************************************************************
 */
public class Utils {
    public  String getIPAddress(boolean useIPv4) {
        InetAddress mAddress = getLocalV4Address(getActiveNetworkInterface());
        if (mAddress != null) {
            return mAddress.getHostAddress();
        }
        return "127.0.0.0";
    }


   /*   try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
          //  getActiveNetworkInterface();
            for (NetworkInterface intf : interfaces) {
                Log.d("ip check","Bhargav " + intf.getName());
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;
                        //Log.d("ip check","in getIpAddress "+addr.getHostAddress());

                        if (useIPv4) {
                            if (isIPv4) {
                                getActiveNetworkInterface();
                                return sAddr;
                            }
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                getActiveNetworkInterface();
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) { } // for now eat exceptions
        getActiveNetworkInterface();
        return "";*/

    public static NetworkInterface getActiveNetworkInterface() {

        Enumeration<NetworkInterface> interfaces = null;
        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
        } catch (Exception e) {
            return null;
        }

        if (interfaces == null)
            return null;
        while (interfaces.hasMoreElements()) {
            NetworkInterface iface = interfaces.nextElement();
            Enumeration<InetAddress> inetAddresses = iface.getInetAddresses();

//            Log.d("ip check","Bhargav in getActiveInterface "+iface.getName());
            /* Check if we have a non-local address. If so, this is the active
             * interface.
             *
             * This isn't a perfect heuristic: I have devices which this will
             * still detect the wrong interface on, but it will handle the
             * common cases of wifi-only and Ethernet-only.
             */
            if (iface.getName().startsWith("w")) {
                //this is a perfect hack for getting wifi alone

                while (inetAddresses.hasMoreElements()) {
                    InetAddress addr = inetAddresses.nextElement();

                    if (!(addr.isLoopbackAddress() || addr.isLinkLocalAddress())) {
                        Log.d("LSSDP", "DisplayName" + iface.getDisplayName() + "Name" + iface.getName()+
                        "Address "+ addr +" Host Address" + addr.getHostAddress());

                        return iface;
                    }
                }
            }
        }

        return null;
    }

    public static InetAddress getLocalV4Address(NetworkInterface netif) {


        Enumeration addrs;
        try {
            addrs = netif.getInetAddresses();
        } catch (NullPointerException e) {
            e.printStackTrace();
            return null;
        }
        while (addrs.hasMoreElements()) {
            InetAddress addr = (InetAddress) addrs.nextElement();
            if (addr instanceof Inet4Address && !addr.isLoopbackAddress())
                return addr;
        }
        return null;
    }

    public static String readerToString(BufferedReader reader) {
        StringBuffer rawBody = new StringBuffer();
        String line = null;
        try {
            while ((line = reader.readLine()) != null)
                rawBody.append(line);
        } catch (Exception e) { /*report an error*/ }
        return rawBody.toString();
    }

    public static String inputStreamToString(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;

        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }

        is.close();

        return sb.toString();
    }

    public static String readAsset(Context context, String assetPath) throws IOException {
        String asset = null;
        AssetManager am = context.getAssets();
        try {
            InputStream is = am.open(assetPath);
            int length = is.available();
            byte[] data = new byte[length];
            is.read(data);
            is.close();
            asset = new String(data, "ASCII");
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        return asset;
    }

    public static final int byteArrayToInt(byte[] arr, int offset) {
        if (arr == null || arr.length - offset < 4)
            return -1;

        int r0 = (arr[offset] & 0xFF) << 24;
        int r1 = (arr[offset + 1] & 0xFF) << 16;
        int r2 = (arr[offset + 2] & 0xFF) << 8;
        int r3 = arr[offset + 3] & 0xFF;
        return r0 + r1 + r2 + r3;
    }

    public static String convertToMacAddress(String input){
        StringBuilder builder = new StringBuilder(input.length() * 3 / 2 - 1);
        for (int i = 0; i < input.length(); i += 2) {
            if (i != 0) {
                builder.append(":");
            }
            builder.append(input, i, i + 2);
        }
        return builder.toString();
    }
}