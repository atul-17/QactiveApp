package com.cumulations.libreV2.tcp_tunneling;

import android.util.Log;

import com.cumulations.libreV2.tcp_tunneling.enums.PayloadType;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public class TunnelingControl {

    private String clientSocketIp;
    private static ConcurrentHashMap<String, Socket> tunnelingClientsMap = new ConcurrentHashMap<>();
    public static final int TUNNELING_CLIENT_PORT = 50005;

    public TunnelingControl(String clientSocketIp) {
        this.clientSocketIp = clientSocketIp;
    }

    public static boolean isTunnelingClientPresent(String ip){
        /*if (!tunnelingClientsMap.containsKey(ip))
            return false;
        Socket socket = tunnelingClientsMap.get(ip);
        if (socket == null) return false;

        return socket.isConnected() && !socket.isClosed();*/
        return tunnelingClientsMap.containsKey(ip);
    }

    public static void addTunnelingClient(Socket socket){
        Log.d("addTunnelingClient","socket "+socket.getInetAddress().getHostAddress()
        +" isPresent = "+tunnelingClientsMap.containsKey(socket.getInetAddress().getHostAddress()));
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
            tunnelingClientsMap.remove(socketIp);
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
                    if (client == null){
                        Log.e("sendDataModeCommand","socket "+clientSocketIp+" null");
                        tunnelingClientsMap.remove(clientSocketIp);
                        return;
                    }

                    DataOutputStream out = new DataOutputStream(client.getOutputStream());
                    out.write(sendMessage);
//                    out.write(sendMessage,0,sendMessage.length);
//                    out.flush();

                    Log.i("sendDataModeCommand","byte[] written "+ Arrays.toString(sendMessage));
                } catch (Exception e){
                    e.printStackTrace();
                    if (e instanceof IOException){
                        tunnelingClientsMap.remove(clientSocketIp);
                    }
                }
            }
        }).start();
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
                    if (client == null){
                        Log.e("sendCommand","socket "+clientSocketIp+" null");
                        tunnelingClientsMap.remove(clientSocketIp);
                        return;
                    }

                    DataOutputStream out = new DataOutputStream(client.getOutputStream());
                    out.write(sendMessage);
                    Log.i("sendCommand","byte[] written "+ Arrays.toString(sendMessage));

                    Thread.sleep(300);
                    sendDataModeCommand();

                } catch (Exception e){
                    e.printStackTrace();
                    if (e instanceof IOException){
                        tunnelingClientsMap.remove(clientSocketIp);
                    }
                }
            }
        }).start();
    }

    public void sendCommand(PayloadType payloadType,byte payloadValue){
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
                    if (client == null){
                        Log.e("sendCommand","socket "+clientSocketIp+" null");
                        tunnelingClientsMap.remove(clientSocketIp);
                        return;
                    }

                    DataOutputStream out = new DataOutputStream(client.getOutputStream());
                    out.write(sendMessage);
                    Log.i("sendCommand","byte[] written "+ Arrays.toString(sendMessage));

                    Thread.sleep(300);
                    sendDataModeCommand();

                } catch (Exception e){
                    e.printStackTrace();
                    if (e instanceof IOException){
                        tunnelingClientsMap.remove(clientSocketIp);
                    }
                }
            }
        }).start();
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String readableByteArrayToHexString(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a) {
            sb.append("0x");
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
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
