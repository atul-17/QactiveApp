package com.cumulations.libreV2.tcp_tunneling;

import android.util.Log;

import com.cumulations.libreV2.tcp_tunneling.enums.PayloadType;
import com.libre.qactive.util.LibreLogger;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public class TunnelingControl {

    private String clientSocketIp;
    private static ConcurrentHashMap<String, Socket> tunnelingClientsMap = new ConcurrentHashMap<>();
    static final int TUNNELING_CLIENT_PORT = 50005;
    private Object TAG = this;

    public TunnelingControl(String clientSocketIp) {
        this.clientSocketIp = clientSocketIp;
    }

    public static boolean isTunnelingClientPresent(String ip){
        if (!tunnelingClientsMap.containsKey(ip))
            return false;
        Socket socket = tunnelingClientsMap.get(ip);
        if (socket == null) {
            LibreLogger.d("TunnelingControl","isTunnelingClientPresent, socket null");
        }

        LibreLogger.d("TunnelingControl","isTunnelingClientPresent, ip = "+ip+" socket isConnected "+socket.isConnected()
                +" socket isClosed "+socket.isClosed());
        return tunnelingClientsMap.containsKey(ip);
    }

    static void addTunnelingClient(Socket socket){
        Log.d("addTunnelingClient","socket "+socket.getInetAddress().getHostAddress());
        tunnelingClientsMap.put(socket.getInetAddress().getHostAddress(),socket);
    }

    public static Socket getTunnelingClient(String socketIp){
        Log.d("getTunnelingClient","socketIp "+socketIp +" isPresent = "+tunnelingClientsMap.containsKey(socketIp));
        return tunnelingClientsMap.get(socketIp);
    }

    public static void removeTunnelingClient(String socketIp){
        boolean isSocketPresent = tunnelingClientsMap.containsKey(socketIp);
        Log.d("removeTunnelingClient","isSocketPresent "+isSocketPresent);
        if (isSocketPresent){
            Socket clientSocket = tunnelingClientsMap.get(socketIp);
            try {
                if (clientSocket!=null) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                LibreLogger.d("removeTunnelingClient","removeTunnelingClient, exception "+e.getMessage());
            }
            tunnelingClientsMap.remove(socketIp);
            Log.d("removeTunnelingClient","socket "+socketIp+" removed");
        }
    }

    public static void clearTunnelingClients(){
        tunnelingClientsMap.clear();
    }

    public void sendDataModeCommand(){
        final TCPTunnelPacket tcpTunnelPacket = new TCPTunnelPacket(
                TCPTunnelPacket.APP_COMMAND,
                TCPTunnelPacket.GET_MODE,
                PayloadType.GET_DATA_MODE,
                (byte) -1);


        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] sendMessage = tcpTunnelPacket.sendPayload;
//                    byte[] sendMessage = new byte[]{
//                            (byte)0x02,(byte)0x01, (byte)0x03, (byte)0x02 /*set BT*/
//                            (byte)0x02,(byte)0x02, (byte)0x03, (byte)0x01 /*request FW version*/
//                            (byte)0x02,(byte)0x02, (byte)0x02 /*request Data mode*/
//                    };

                    if (!tunnelingClientsMap.containsKey(clientSocketIp)){
                        Log.e("sendDataModeCommand","socket not found "+clientSocketIp);
                        return;
                    }

                    Socket client = tunnelingClientsMap.get(clientSocketIp);
                    if (client == null || client.isClosed() || !client.isConnected()){
                        Log.e("sendDataModeCommand","socket null/closed/not connected");
                        tunnelingClientsMap.remove(clientSocketIp);
                        return;
                    }

                    DataOutputStream out = new DataOutputStream(client.getOutputStream());
                    out.write(sendMessage);

                    LibreLogger.d(TAG,"sendDataModeCommand ip "+clientSocketIp+" byte[] written "+ Arrays.toString(sendMessage));
                } catch (Exception e){
                    e.printStackTrace();
                    LibreLogger.d(TAG,"sendDataModeCommand, exception "+e.getMessage());
                    if (e instanceof IOException){
                        removeTunnelingSocket(clientSocketIp);
                    }
                }
            }
        }).start();
    }

    private void removeTunnelingSocket(String clientSocketIp) {
        Socket existingSocket = TunnelingControl.getTunnelingClient(clientSocketIp);
        if (existingSocket!=null && !existingSocket.isClosed()){
            try {
                existingSocket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            TunnelingControl.removeTunnelingClient(clientSocketIp);
        }
    }

    public void sendGetModelIdCommand(){
        TCPTunnelPacket tcpTunnelPacket = new TCPTunnelPacket(
                TCPTunnelPacket.APP_COMMAND,
                TCPTunnelPacket.GET_MODE,
                PayloadType.GET_MODEL_ID,
                (byte) -1);
        sendCommand(tcpTunnelPacket);
    }

    private void sendCommand(final TCPTunnelPacket tcpTunnelPacket){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] sendMessage = tcpTunnelPacket.sendPayload;
//                    byte[] sendMessage = new byte[]{
//                            (byte)0x02,(byte)0x01, (byte)0x03, (byte)0x02 /*set BT*/
//                    };

                    if (!tunnelingClientsMap.containsKey(clientSocketIp)){
                        Log.e("sendCommand","socket not found "+clientSocketIp);
                        return;
                    }

                    Socket client = tunnelingClientsMap.get(clientSocketIp);
                    if (client == null || client.isClosed() || !client.isConnected()){
                        Log.e("sendDataModeCommand","socket null/closed/not connected");
                        tunnelingClientsMap.remove(clientSocketIp);
                        return;
                    }

                    DataOutputStream out = new DataOutputStream(client.getOutputStream());
                    out.write(sendMessage);

                    LibreLogger.d(TAG,"sendCommand ip "+clientSocketIp+" byte[] written "+ Arrays.toString(sendMessage));

                    Thread.sleep(300);
                    sendDataModeCommand();

                } catch (Exception e){
                    e.printStackTrace();
                    LibreLogger.d(TAG,"sendCommand, exception "+e.getMessage());
                    if (e instanceof IOException){
                        removeTunnelingSocket(clientSocketIp);
                    }
                }
            }
        }).start();
    }

    public void sendCommand(final PayloadType payloadType, byte payloadValue){
        final TCPTunnelPacket tcpTunnelPacket = new TCPTunnelPacket(
                TCPTunnelPacket.APP_COMMAND,
                TCPTunnelPacket.SET_MODE,
                payloadType,
                payloadValue);


        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] sendMessage = tcpTunnelPacket.sendPayload;
//                    byte[] sendMessage = new byte[]{
//                            (byte)0x02,(byte)0x01, (byte)0x03, (byte)0x02 /*set BT*/
//                    };

                    if (!tunnelingClientsMap.containsKey(clientSocketIp)){
                        Log.e("sendCommand","socket not found "+clientSocketIp);
                        return;
                    }

                    Socket client = tunnelingClientsMap.get(clientSocketIp);
                    if (client == null || client.isClosed() || !client.isConnected()){
                        Log.e("sendDataModeCommand","socket null/closed/not connected");
                        tunnelingClientsMap.remove(clientSocketIp);
                        return;
                    }

                    DataOutputStream out = new DataOutputStream(client.getOutputStream());
                    out.write(sendMessage);

                    LibreLogger.d(TAG,"sendCommand ip "+clientSocketIp+" byte[] written "+ Arrays.toString(sendMessage));

                    Thread.sleep(300);
                    sendDataModeCommand();

                } catch (Exception e){
                    e.printStackTrace();
                    LibreLogger.d(TAG,"sendCommand, exception "+e.getMessage());
                    if (e instanceof IOException){
                        removeTunnelingSocket(clientSocketIp);
                    }
                }
            }
        }).start();
    }

    private static final String HEXES = "0123456789ABCDEF";
    public static String getReadableHexByteArray(byte[] raw) {
        if (raw == null) {
            return null;
        }
        final StringBuilder hex = new StringBuilder(2 * raw.length);
        for (final byte b : raw) {
            hex.append("0x")
                    .append(HEXES.charAt((b & 0xF0) >> 4))
                    .append(HEXES.charAt((b & 0x0F)))
                    .append(" ");
        }
        return hex.toString();
    }
}
