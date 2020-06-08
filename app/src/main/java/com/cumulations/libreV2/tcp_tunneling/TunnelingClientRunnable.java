package com.cumulations.libreV2.tcp_tunneling;

import android.util.Log;

import com.libre.qactive.netty.BusProvider;
import com.libre.qactive.util.LibreLogger;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

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
            LibreLogger.d(this, "run, socket connected "
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

//                    Thread.sleep(200);
//                    new TunnelingControl(speakerIpAddress).sendGetModelIdCommand();
                }

                DataInputStream dIn = new DataInputStream(clientSocket.getInputStream());

                while (true) {
                    byte[] message = new byte[32];
                    int byteLengthRead = dIn.read(message);
                    LibreLogger.d(this, "TunnelingClientRunnable, byteLengthRead = " + byteLengthRead);
                    if (byteLengthRead > 0) {
                        LibreLogger.d(this, "TunnelingClientRunnable, ip = "+ speakerIpAddress +", message[] = " + TunnelingControl.getReadableHexByteArray(message));
                        TunnelingData tunnelingData = new TunnelingData(speakerIpAddress, message);
                        if (!tunnelingData.isACKData()) {
                            BusProvider.getInstance().post(tunnelingData);
                        }
                    } else {
                        LibreLogger.d(this, "TunnelingClientRunnable, ip = "+ speakerIpAddress +" break");
                        break;
                    }
                }
            }

        } catch (Exception e) {
//            e.printStackTrace();
            Log.e("TunnelingClientRunnable", "exception " + e.getMessage());
            if (e instanceof IOException){
                Socket existingSocket = TunnelingControl.getTunnelingClient(speakerIpAddress);
                if (existingSocket!=null && !existingSocket.isClosed()){
                    try {
                        existingSocket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    TunnelingControl.removeTunnelingClient(speakerIpAddress);
                }
            }
        }
    }
}
