package com.libre.netty;

import android.util.Log;

import com.libre.Scanning.ScanningHandler;
import com.libre.luci.LSSDPNodeDB;
import com.libre.luci.LSSDPNodes;
import com.libre.luci.LUCIControl;
import com.libre.util.LibreLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 *
 * This file checks if the particular socket connection is still alive.
 * This gets called internally for every NettyAndroid client object
 * Created by praveena on 7/18/15.
 */
public class HeartbeatHandler extends ChannelDuplexHandler {

    String ipadddress;

    public HeartbeatHandler(String ipadddress){
        super();
        this.ipadddress=ipadddress;
    }
    /**
     * Ping a host and return an int value of 0 or 1 or 2 0=success, 1=fail,
     * 2=error
     *
     * Does not work in Android emulator and also delay by '1' second if host
     * not pingable In the Android emulator only ping to 127.0.0.1 works
     *
     * @param host
     *            host in dotted IP address format
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public  int pingHost(String host, int timeout) throws IOException,
            InterruptedException {
        Runtime runtime = Runtime.getRuntime();
        timeout /= 1000;
        String cmd = "ping -c 5 -W " + timeout + " " + host;
        Process proc = runtime.exec(cmd);
        LibreLogger.d(this,"Ping Result : " + cmd);
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(proc.getInputStream()));
        int read;
        char[] buffer = new char[4096];
        StringBuffer output = new StringBuffer();
        while ((read = reader.read(buffer)) > 0) {
            output.append(buffer, 0, read);
        }
        reader.close();
        LibreLogger.d(this,"Ping result" + output.toString()+ "For URL" + host);
        proc.waitFor();
        int exit = proc.exitValue();
        return exit;
    }
    public static boolean hasService(InetAddress host, int port) throws IOException
    {
        boolean   status = false;
        int TIMEOUT = 3000;
        Socket    sock = new Socket();

        try
        {
            sock.connect(new InetSocketAddress(host, port), TIMEOUT);
            if (sock.isConnected())
            {
                sock.close();
                status = true;
            }
        }

        catch (ConnectException | NoRouteToHostException | SocketTimeoutException ex) {}
        return status;
    }

    public boolean ping(String url) {
        InetAddress addr = null;
        try {
            addr = InetAddress.getByName(url);
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;

        }
      /*  try {

            // Executes the command.

            Process process = Runtime.getRuntime().exec("ping -c 1 "+ url);

            // Reads stdout.
            // NOTE: You can write to stdin of the command using
            //       process.getOutputStream().
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            int read;
            char[] buffer = new char[4096];
            StringBuffer output = new StringBuffer();
            while ((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            reader.close();

            // Waits for the command to finish.
            process.waitFor();

            LibreLogger.d(this,"Ping result" + output.toString()+ "For URL" + url);
        } catch (IOException e) {

            throw new RuntimeException(e);

        } catch (InterruptedException e) {

            throw new RuntimeException(e);
        }*/
        try {
            int Result = pingHost(url,5500); //For RELEASE
            LibreLogger.d(this, "First Ping Host" + "\n" + url + "- Result " + Result);
            if(Result != 0 ){
                int mResult = pingHost(url,5500);
                LibreLogger.d(this,"Second Ping Host" + "\n"+ url + "- Result " + Result);
                if(mResult !=0 )
                    return false;
                else
                    return true;
            }else{
                return true;
            }

            /*if (addr.isReachable(5000)) {
                LibreLogger.d(this,"InetAddress" + "\n"+ url + "- Respond OK");
                return true;
            } else {
                LibreLogger.d(this, "InetAddress" + "\n" + url + "- Respond NOT OK For First Ping");
                if(addr.isReachable(5000)){
                    LibreLogger.d(this, "InetAddress" + "\n" + url + "- Respond OK For Second Ping");
                    return true;
                }else {
                    LibreLogger.d(this, "InetAddress" + "\n" + url + "- Respond Not OK For Second Ping So Returning FALSE");
                    return false;
                }
            }*/
        } catch (IOException e) {
            // TODO Auto-generated catch block
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }
    @Override
    public void userEventTriggered(ChannelHandlerContext channelHandlerContext, Object evt) throws Exception {
     //   System.out.println("Event Fired"); // Doesn't trigger
        if (evt instanceof IdleStateEvent) {


            if (((IdleStateEvent) evt).state() == IdleState.READER_IDLE) {

                NettyAndroidClient nettyAndroidClient = LUCIControl.luciSocketMap.get(ipadddress);
                LSSDPNodeDB mNodeDB1 = LSSDPNodeDB.getInstance();
                LSSDPNodes mNode = mNodeDB1.getTheNodeBasedOnTheIpAddress(ipadddress);
                //Log.d("Scan_Netty", "Triggered Heartbeeat check") ;
                if (nettyAndroidClient==null) {

                    return;
                }
                if(mNode != null) {
                    LibreLogger.d(this, "Last Notified Time " + nettyAndroidClient.getLastNotifiedTime() +
                            " For the Ip " + nettyAndroidClient.getRemotehost() + " Device Name " + mNode.getFriendlyname()) ;
                }else{
                    LibreLogger.d(this, "Last Notified Time " + nettyAndroidClient.getLastNotifiedTime() +
                            " For the Ip " + nettyAndroidClient.getRemotehost());
                }

                /* If we are Missing 6 Alive notification */
                if ((System.currentTimeMillis() - (nettyAndroidClient.getLastNotifiedTime())) > 60000) {
//                    LibreLogger.d(HeartbeatHandler.class.getSimpleName(), ipadddress + "Socket removed because we did not get notification since last 11 second");
                    LibreLogger.d(HeartbeatHandler.class.getSimpleName(), ipadddress + "Missed 6 Alive Notifications ( >60 sec )");
                }
            }
        }
    }
}
