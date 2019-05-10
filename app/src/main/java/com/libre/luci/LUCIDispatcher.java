package com.libre.luci;

import com.libre.netty.NettyAndroidClient;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Created by praveena on 11/21/15.
 */
public class LUCIDispatcher {


    /* A Datastructure class to store both ipaddress and byteArray of the message */
        class DispatchData {
        String ipAddress;
        byte[] databyte;

        DispatchData(String ip, byte[] arrayOfBytes) {
            ipAddress = ip;
            databyte = arrayOfBytes;
        }
    }


    /* This is the queue which will store all messages to be sent */
    Queue<DispatchData> queue = new ArrayDeque<>();

    public void addToQueue(String ipaddres, byte[] arrayData) {

        queue.add(new DispatchData(ipaddres, arrayData));

    }


    /* This is the thread which will pickup the data if present from the queue and will send the same to DUT */
    /* While dispatching the message if the socket is not present it will create the same and then send the message */

    public class MessengerThread extends Thread {

        boolean toStop = true;

        public MessengerThread() {

        }


        @Override
        public void run() {
            try {
                while (toStop) {
                    if (queue.size() > 0) {
                        DispatchData data = queue.remove();
                        String serverIp = data.ipAddress;
                        byte[] dataArrya = data.databyte;
                        dispatchData(serverIp, dataArrya);

                    } else {
                        sleep(100);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }


        }

        private void dispatchData(String ip, byte[] arrayData) {

            int LUCI_CONTROL_PORT = 7777;
            InetAddress local = null;
            if (!(LUCIControl.luciSocketMap.containsKey(ip))) {

                try {

                    int server_port = LUCI_CONTROL_PORT;
                    try {
                        local = InetAddress.getByName(ip);
                    } catch (UnknownHostException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    NettyAndroidClient tcpSocketSendCtrl = new NettyAndroidClient(local, LUCI_CONTROL_PORT);
                    LUCIControl.luciSocketMap.put(ip, tcpSocketSendCtrl);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            NettyAndroidClient nettyAndroidClient = LUCIControl.luciSocketMap.get(ip);
                /* here we can wirte ping */
            TcpSendData(nettyAndroidClient,arrayData);
        }

    }

    public static boolean TcpSendData(NettyAndroidClient tcpSocket, byte[] message) {
        if(tcpSocket!=null) {
            tcpSocket.write(message);
        }
        return true;
    }
}




