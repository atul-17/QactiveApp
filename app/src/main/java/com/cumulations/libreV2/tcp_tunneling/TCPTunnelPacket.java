package com.cumulations.libreV2.tcp_tunneling;

import com.cumulations.libreV2.tcp_tunneling.enums.AQModeSelect;
import com.cumulations.libreV2.tcp_tunneling.enums.BatteryType;
import com.cumulations.libreV2.tcp_tunneling.enums.ModelId;
import com.cumulations.libreV2.tcp_tunneling.enums.PayloadType;
import com.libre.Scanning.Constants;

/**
 * Created by Amit T
 * This class represents tcp raw data received from tunneling connection
 * And Tcp Raw payload for sending
 * <p>
 * For TCP tunneling we need not send MB 111 as it seems to be already established connection on Port 50005
 */
public class TCPTunnelPacket {

    /*
    * TCP Raw Payload :
    *   "CommandType(1 Byte)"
        "Command Mode(1 Byte)"
        "Payload Data[0](1 Byte)"
        "Payload Data[1](1 Byte)"
        "Payload Data[2](1 Byte)"
        "Payload Data[3](1 Byte)"
        "Payload Data[4](1 Byte)"
            ..
            ..
            ..
            ..
        "Payload Data[n-1](1 Byte)"
        "Payload Data[n](1 Byte)"
    *
    * */

    private static final String TAG = TCPTunnelPacket.class.getSimpleName();
    private byte commandType;
    public byte commandMode;

    /*Possible CommandType(1 Byte)*/
    public static final byte HOST_COMMAND = 0x01;
    public static final byte APP_COMMAND = 0x02;

    /*Possible Command Mode(1 Byte)*/
    public static final byte SET_MODE = 0x01;
    public static final byte GET_MODE = 0x02;

    private byte batteryPercentagePayload;

    private byte sourcePayload;
    private byte volumeIndexPayload;
    private byte aqModePayload;
    private byte bassVolumePayload;     /*0~10 (-5dB ~5dB)*/
    private byte trebleVolumePayload;   /*0~10 (-5dB ~5dB)*/
    private byte ddmsModePayload;       /*0x00 Home mode 0x01 Away mode*/

    private int MAX_SEND_BYTE_SIZE = 4;
    public byte[] sendPayload;
    private byte[] receivedPayload;
    private byte modelIdByte;

    public TCPTunnelPacket(byte[] rawData) throws IndexOutOfBoundsException {
        receivedPayload = rawData;
        /*
         * Typically we receive the data mode packets like this (python response in String format)
         * "b'\x01\x02\xff\x00\x00\x00\x08\x00\xff\xff\xff\xff\x00\xff\xff\xff\x03\x07\xff\xff\x00\xff\xff\xff'"
         * */
        //fill default fields:
        commandType = rawData[0];
        commandMode = rawData[1];

        //parse individual payload:
        batteryPercentagePayload = rawData[3];
        sourcePayload = rawData[5];
        volumeIndexPayload = rawData[6];
        aqModePayload = rawData[7];
        bassVolumePayload = rawData[16];
        trebleVolumePayload = rawData[17];
        ddmsModePayload = rawData[20];
    }

    public TCPTunnelPacket(byte[] productInfoData,int byteLength) throws IndexOutOfBoundsException {
        /* "b'\x01\x05\x05\x01~x08'"*/
        if (byteLength >= 4){
            modelIdByte = productInfoData[3];
        }
    }

    public TCPTunnelPacket(byte commandType, byte commandMode, PayloadType payloadType, byte payloadValue) {
        byte[] packet;
        if (payloadType != PayloadType.GET_DATA_MODE) {
            packet = new byte[MAX_SEND_BYTE_SIZE];
        } else packet = new byte[3];

        packet[0] = commandType;
        packet[1] = commandMode;

        switch (commandMode) {

            case SET_MODE:

                switch (payloadType) {
                    case DEVICE_SOURCE:
                        packet[2] = (byte) 0x03;
                        packet[3] = payloadValue;
                        break;

                    case DEVICE_VOLUME:
                        packet[2] = (byte) 0x04;
                        packet[3] = payloadValue;
                        break;

                    case AQ_MODE_SELECT:
                        packet[2] = (byte) 0x05;
                        packet[3] = payloadValue;
                        break;

                    case BASS_VOLUME:
                        packet[2] = (byte) 0x10;
                        packet[3] = payloadValue;
                        break;

                    case TREBLE_VOLUME:
                        packet[2] = (byte) 0x11;
                        packet[3] = payloadValue;
                        break;

                    case GET_DATA_MODE:
                        packet[2] = (byte) 0x02;
                        break;
                }

                break;

            case GET_MODE:

                switch (payloadType) {
                    case GET_DATA_MODE:
                        packet[2] = (byte) 0x02;
                        break;

                    case GET_MODEL_ID:
                        packet[2] = (byte) 0x03;
                        packet[3] = (byte) 0x05;
                        break;
                }
                break;

        }

        sendPayload = packet;
    }

    public BatteryType getBatteryLevel() {
        switch (batteryPercentagePayload) {
            case 0x64:
                return BatteryType.BATTERY_HIGH;
            case 0x46:
                return BatteryType.BATTERY_MIDDLE;
            case 0x1E:
                return BatteryType.BATTERY_LOW;
            case 0x10:
                return BatteryType.BATTERY_WARNING;
        }

        return null;
    }

    public int getCurrentSource() {
        switch (sourcePayload) {
            case 0x00:
                return Constants.AUX_SOURCE;
            case 0x01:
                return Constants.NO_SOURCE;
            case 0x02:
                return Constants.BT_SOURCE;
            case 0x03:
                return Constants.USB_SOURCE;
            default:
                return -1;
        }
    }

    public AQModeSelect getAqMode() {
        switch (aqModePayload) {
            case 0x00:
                return AQModeSelect.Trillium;
            case 0x01:
                return AQModeSelect.Power;
            case 0x02:
                return AQModeSelect.Left;
            case 0x03:
                return AQModeSelect.Right;
        }
        return null;
    }

    public int getBassValue() {
        return bassVolumePayload;
    }

    public int getTrebleValue() {
        return trebleVolumePayload;
    }

    public int getVolume() {
        return volumeIndexPayload * 5;
    }

    public ModelId getModelId(){
        switch (modelIdByte){
            case 0x01:
                return ModelId.Arena;
            case 0x02:
                return ModelId.Festival;
            case 0x03:
                return ModelId.Central;
            case 0x04:
                return ModelId.Concert;
            case 0x05:
                return ModelId.Stadium;
            case 0x06:
                return ModelId.G1;
            case 0x07:
                return ModelId.G2;
            case 0x08:
                return ModelId.Station;
        }

        return null;
    }
}
