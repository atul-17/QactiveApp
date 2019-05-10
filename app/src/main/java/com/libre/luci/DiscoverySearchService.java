package com.libre.luci;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import com.libre.constants.LSSDPCONST;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

/**
 * This class helps us to find the devices around us using the lssdp
 * msearch and also manages the notify
 *
 * Created by praveena on 7/24/15.
 */
public class DiscoverySearchService extends Service{

public static final String TAG = DiscoverySearchService.class.getName();

    public static final int MSG_REGISTER = 1;
    public static final int MSG_UNREGISTER = 2;
    public static final int START_SCAN_FROM_BEGINING=3;


    /* public static final String LSSDP_MULTICAST_ADDRESS = "239.255.255.250"; Commenting For HnT Discovery Ip Change*/
    public static final String LSSDP_MULTICAST_ADDRESS = "239.255.255.250";
    public static final int LSSDP_PORT = 1800;


    private final Messenger mMessenger;
    private NetworkInterface mNetIf;


    private final List<Messenger> mClients = new LinkedList<Messenger>();
    private static MulticastSocket mMulticastSocket;
    private static ServerSocket serverSocket = null;

    public DiscoverySearchService() {

            mMessenger = new Messenger(new IncomingHandler(this));
        }

        @Override
        public IBinder onBind(Intent intent) {
            return mMessenger.getBinder();
        }

        private static class IncomingHandler extends Handler {


            private final WeakReference<DiscoverySearchService> mService;

            public IncomingHandler(DiscoverySearchService service) {
                mService = new WeakReference<DiscoverySearchService>(service);
            }

            @Override
            public void handleMessage(Message msg) {
                DiscoverySearchService service = mService.get();
                if (service != null) {
                    switch (msg.what) {

                        case MSG_REGISTER:
                            service.mClients.add(msg.replyTo);
                            Log.d(TAG, "Registered");
                            break;

                        case MSG_UNREGISTER:
                            service.mClients.remove(msg.replyTo);
                            Log.d(TAG, "Unegistered");
                            break;

                        case START_SCAN_FROM_BEGINING:
                            super.handleMessage(msg);
                    }
                }
            }




        }



    public boolean CreateSockets() throws SocketException {

        if (mNetIf == null) {
            Log.d(TAG, "Network interface is false");
            return false;
        }

        try {
            Socket clientSocket;


            String name = mNetIf.getName();
            mMulticastSocket = new MulticastSocket(LSSDP_PORT);

            mMulticastSocket.setReuseAddress(true);
            //   Log.v(TAG, "CREATING LSSDP SOCKET AT Interface  " + name);
            mMulticastSocket.joinGroup(new InetSocketAddress(LSSDP_MULTICAST_ADDRESS, LSSDP_PORT), NetworkInterface.getByName(name));
            Log.i(TAG, "MultiSocket address  is" + LSSDP_MULTICAST_ADDRESS + "and Network interface is name is=" + NetworkInterface.getByName(name) +
                    "Local v4 adderess is" + Utils.getLocalV4Address(mNetIf));
            //  Log.d(TAG, "Multicast done");

            try {
                serverSocket = new ServerSocket(LSSDP_PORT, 0, Utils.getLocalV4Address(mNetIf));
            } catch (IOException e) {
                // Log.d(TAG, "Unicast failed");
                e.printStackTrace();
            }

            Log.i(TAG, "socket created sucessfully");


        } catch (IOException e) {
                Log.d(TAG, "IO EXception");
                Log.d(TAG, "CREATING LSSDP SOCKETS FAILED");
                e.printStackTrace();
            return false;
        }
        return true;

    }

    /**
     * This function perfroms MesearchRequest clearing the local set of devices
     *
     */
    private void startMsearchFromFresh() {

        String MSearchPayload = "M-SEARCH * HTTP/1.1\r\n" +
                "MX: 10\r\n" +
                "ST: urn:schemas-upnp-org:device:DDMSServer:1\r\n" +
                "HOST: 239.255.255.250:1800\r\n" +
                "MAN: \"ssdp:discover\"\r\n" +
                "\r\n";


        Log.d(TAG, "Sending M-search");
        DatagramPacket MSearch = null;
        try {
            MSearch = new DatagramPacket(MSearchPayload.getBytes(),
                    MSearchPayload.length(), InetAddress.getByName(LSSDP_MULTICAST_ADDRESS), LSSDP_PORT);
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block

            e.printStackTrace();
        }

        try {

            if (mMulticastSocket == null) {
                Log.d(TAG, "SOcket is not created,handler is created");

            } else {
                mMulticastSocket.send(MSearch);
                Log.d(TAG, "Socket is not Created");
            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


    }


}
