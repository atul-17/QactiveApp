package com.libre.qactive.luci;

import android.util.Log;

/**
 * This class represents the message being exchanged between device and the application
 */
public class LUCIPacket {

    private static final String TAG = null;

    static int HEADER_SIZE = 10;

    public short remoteID;
    public byte CommandType;
    public short Command;
    public byte CommandStatus;
    public short CRC;
    public short DataLen;
    //Bitstream of the LUCI header
    public byte[] header;
    //size of the LUCI payload
    public int payload_size;
    //Bitstream of the LUCI payload
    public byte[] payload;


    public LUCIPacket(byte[] message, short message_size, short aCommand, byte cmd_type) {
        //fill default fields:
        remoteID = 0;
        CommandType = cmd_type;
        Command = aCommand;
        CommandStatus = 0;
        CRC = 0;
        DataLen = message_size;


        //get the header bitsream:
        header = new byte[HEADER_SIZE];

        header[0] = (byte) (remoteID & 0x00FF);
        header[1] = (byte) ((remoteID & 0xFF00) >> 8);
        header[2] = CommandType;
        header[3] = (byte) (Command & 0x00FF);
        header[4] = (byte) ((Command & 0xFF00) >> 8);
        header[5] = CommandStatus;
        header[6] = (byte) (CRC & 0x00FF);
        header[7] = (byte) ((CRC & 0xFF00) >> 8);
        header[8] = (byte) (DataLen & 0x00FF);
        header[9] = (byte) ((DataLen & 0xFF00) >> 8);
        //get the payload bitstream:
        payload_size = message_size;

        if (payload_size > 0) {
            payload = new byte[payload_size];

            for (int i = 0; i < payload_size; i++)
                payload[i] = message[i];

        }

    }

    public LUCIPacket(byte[] message, short message_size, short aCommand) {
        //fill default fields:
        remoteID = 0;
        CommandType = 2;
        Command = aCommand;
        CommandStatus = 0;
        CRC = 0;
        DataLen = message_size;


        //get the header bitsream:
        header = new byte[HEADER_SIZE];

        header[0] = (byte) (remoteID & 0x00FF);
        header[1] = (byte) ((remoteID & 0xFF00) >> 8);
        header[2] = CommandType;
        header[3] = (byte) (Command & 0x00FF);
        header[4] = (byte) ((Command & 0xFF00) >> 8);
        header[5] = CommandStatus;
        header[6] = (byte) (CRC & 0x00FF);
        header[7] = (byte) ((CRC & 0xFF00) >> 8);
        header[8] = (byte) (DataLen & 0x00FF);
        header[9] = (byte) ((DataLen & 0xFF00) >> 8);
        //get the payload bitstream:
        payload_size = message_size;

        payload = new byte[payload_size];

        for (int i = 0; i < payload_size; i++)
            payload[i] = message[i];


    }

    public void setCommand(byte command) {
        this.CommandType = command;
        header[2] = CommandType;

    }

    public LUCIPacket(String message, short message_size, short aCommand) {
        //fill default fields:
        remoteID = 0;
        CommandType = 1;
        Command = aCommand;
        CommandStatus = 0;
        CRC = 0;
        DataLen = message_size;

        //get the header bitsream:
        header = new byte[HEADER_SIZE];

        header[0] = (byte) (remoteID & 0x00FF);
        header[1] = (byte) ((remoteID & 0xFF00) >> 8);
        header[2] = CommandType;
        header[3] = (byte) (Command & 0x00FF);
        header[4] = (byte) ((Command & 0xFF00) >> 8);
        header[5] = CommandStatus;
        header[6] = (byte) (CRC & 0x00FF);
        header[7] = (byte) ((CRC & 0xFF00) >> 8);
        header[8] = (byte) (DataLen & 0x00FF);
        header[9] = (byte) ((DataLen & 0xFF00) >> 8);
        //get the payload bitstream:
        payload_size = message_size;

        payload = new byte[payload_size];

        payload = message.getBytes();

    }
   /* public LUCIPacket(String message, short message_size, short aCommand,byte cmd_type)
    {
        //fill default fields:
        remoteID=10;
        commandType=cmd_type;
        command=aCommand;
        CommandStatus=0;
        CRC=0;
        DataLen=message_size;

        //get the header bitsream:
        header = new byte[HEADER_SIZE];

        header[0]=(byte) (remoteID & 0x00FF);
        header[1]=(byte) ((remoteID & 0xFF00)>> 8);
        header[2]=(byte) commandType;
        header[3]=(byte) (command & 0x00FF);
        header[4]=(byte) ((command & 0xFF00)>> 8);
        header[5]=(byte) CommandStatus;
        header[6]=(byte) (CRC & 0x00FF);
        header[7]=(byte) ((CRC & 0xFF00)>> 8);
        header[8]=(byte) (DataLen & 0x00FF);
        header[9]=(byte) ((DataLen & 0xFF00)>> 8);
        //get the payload bitstream:
        payload_size =  message_size;

        payload = new byte[payload_size];

        payload =  message.getBytes();

    }
    */

    /**
     * ******This one is only for RESP******
     */
    public LUCIPacket(byte[] buffer) {

        remoteID = (short) (((buffer[1] & 0xFF) << 8) & (buffer[0] & 0xFF));
        CommandType = buffer[2];
        Command = (short)(buffer[3] * 16 + buffer[4]& 0xFF);
        //command = buffer[4];//(short) (((buffer[4] & 0xFF)) & ((buffer[3] & 0xFF) << 8)) ;
        CommandStatus = buffer[5];

        byte[] jsonBuf;
        int len = buffer.length - 10;
        jsonBuf = new byte[len];

        for (int i = 0; i < len; i++)
            jsonBuf[i] = buffer[i + 10];

        DataLen=(short)len;
        payload=jsonBuf;
    }


    public byte[] getpayload() {
        return payload;
    }


    public int getpayload_length() {
        return (payload_size);
    }


    public int getlength() {
        return (payload_size + HEADER_SIZE);
    }

    public int getremoteID() {
        return remoteID;
    }

    public int getCommandType() {
        return CommandType;
    }

    public int getCommand() {
        return Command;
    }

    public int getCommandStatus() {
        return CommandStatus;
    }

    public int getDataLen() {
        return DataLen;
    }

    //--------------------------
    //getpacket: returns the packet bitstream and its length
    //--------------------------
    public int getPacket(byte[] packet) {
        //construct the packet = header + payload
        for (int i = 0; i < HEADER_SIZE; i++) {
            packet[i] = header[i];
            // Log.e(TAG,"p="+packet[i]);
        }

        String str ="";
        byte[] newByte = new byte[packet.length];
        for(int i=0;i<packet.length;i++){
            newByte[i] = packet[i];
            str += newByte[i] +",";
        }

        for (int i = 0; i < payload_size; i++) {
            packet[i + HEADER_SIZE] = payload[i];
            str += payload[i] +",";
            // Log.e(TAG,"p="+packet[HEADER_SIZE+i]);
        }
        Log.d( TAG, "Finalized String:: Luci Packet :" +str);
        //return total size of the packet
        return (payload_size + HEADER_SIZE);
    }


}
