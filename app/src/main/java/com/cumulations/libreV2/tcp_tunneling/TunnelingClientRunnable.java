package com.cumulations.libreV2.tcp_tunneling;

import android.util.Log;

import com.libre.netty.BusProvider;
import com.libre.util.LibreLogger;

import java.io.DataInputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;

public class TunnelingClientRunnable implements Runnable {
    private String speakerIpAddress;

    public TunnelingClientRunnable(String speakerIpAddress) {
        this.speakerIpAddress = speakerIpAddress;
    }

    @Override
    public void run() {
        Socket clientSocket;
        try {
            clientSocket = new Socket(InetAddress.getByName(speakerIpAddress), TunnelingControl.TUNNELING_CLIENT_PORT);
            LibreLogger.d(this, "TunnelingClientRunnable, socket connected "
                    + clientSocket.isConnected() + " for "
                    + clientSocket.getInetAddress().getHostAddress() + " port "
                    + clientSocket.getPort());

            clientSocket.setSoTimeout(60 * 1000);
            clientSocket.setKeepAlive(true);

            LibreLogger.d(this, "TunnelingClientRunnable, clientSocket isConnected = " + clientSocket.isConnected());
            if (clientSocket.isConnected()) {
                if (!TunnelingControl.isTunnelingClientPresent(speakerIpAddress)) {
                    Log.e("tunnelingClientsMap", "inserting " + speakerIpAddress);
                    TunnelingControl.addTunnelingClient(clientSocket);

                    /*Requesting Data Mode from Host (Speaker)*/
                    new TunnelingControl(clientSocket.getInetAddress().getHostAddress()).sendDataModeCommand();

                    Thread.sleep(200);
                    new TunnelingControl(speakerIpAddress).sendGetModelIdCommand();
                }

                DataInputStream dIn = new DataInputStream(clientSocket.getInputStream());

                /*while (true) {
                    byte[] message = new byte[32];
                    int byteLengthRead = dIn.read(message);
                    if (byteLengthRead > 0) {
                        LibreLogger.d(this, "TunnelingClientRunnable, byteLengthRead = " + byteLengthRead);
                        LibreLogger.d(this, "TunnelingClientRunnable, message[] = " + TunnelingControl.getReadableHexByteArray(message));
                        TunnelingData tunnelingData = new TunnelingData(speakerIpAddress, message);
                        BusProvider.getInstance().post(tunnelingData);
                    }
                }*/

                while (true) {
                    int length = dIn.available();  // read length of incoming message
                    if (length > 0) {
                        LibreLogger.d(this, "TunnelingClientRunnable, dIn length = " + length);
                        byte[] message = new byte[length];
                        /*Reading byte[] from socket*/
                        dIn.readFully(message); // read the message

                        LibreLogger.d(this, "TunnelingClientRunnable, message[] = " + TunnelingControl.getReadableHexByteArray(message));

                        TunnelingData tunnelingData = new TunnelingData(speakerIpAddress, message);
                        BusProvider.getInstance().post(tunnelingData);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("TunnelingClientRunnable", "exception " + e.getMessage());
        }
    }
}