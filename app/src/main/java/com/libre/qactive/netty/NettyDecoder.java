package com.libre.qactive.netty;

/* Created by praveena on 7/16/15.*/

import android.util.Log;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class NettyDecoder extends ByteToMessageDecoder { // (1)



    byte[] globalBytesRead =new byte[0];


    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) { // (2)

        synchronized (out) {

            int numOfBytesReadInThisIteration = in.readableBytes();
            byte[] bytesReadInThisIteration = new byte[numOfBytesReadInThisIteration];
            in.readBytes(bytesReadInThisIteration);
            globalBytesRead = addToGlobalArray(globalBytesRead, bytesReadInThisIteration);


        /* First check ff the read bytes is atleast the size of header*/
            if (globalBytesRead.length < 10) {

                Log.d("NettyDecoder", "Header is not present yet");
                return; // (3)
            }
            else if (getMessageBoxofTheCommand(globalBytesRead)<0||getMessageBoxofTheCommand(globalBytesRead)>1200 )
            {
                String tempExtrString=new String();
                for (int j=0; j<globalBytesRead.length; j++) {
                    tempExtrString+=""+ Integer.toHexString(globalBytesRead[j]);
                }
                Log.d("NettyDecoder", "globalbytes are ignored as they constituted invalid stream " + tempExtrString);
                Log.d("NettyDecoder", "globalbytes are ignored had messagebox as " + getMessageBoxofTheCommand(globalBytesRead));
                globalBytesRead=new byte[0];
                return;
            }


        /* Now you have the header completly, check the data length and contsize till you get*/
            else {

                int lengthOfThePacketWeAreLookingFor = getPacketLength(globalBytesRead);

                if (globalBytesRead.length < lengthOfThePacketWeAreLookingFor) {
                    Log.d("NettyDecoder", "We were looking for packet of size " + lengthOfThePacketWeAreLookingFor + " what we have recieved till now is " + globalBytesRead.length);
                    return;
                } else {
                    Log.d("NettyDecoder", "We were looking for packet of size " + lengthOfThePacketWeAreLookingFor + " what we have recieved till now is " + globalBytesRead.length);
                    if (lengthOfThePacketWeAreLookingFor == globalBytesRead.length) {
                    /* No extra bytes read and hence everything is fine */
                        Log.d("NettyDecoder", "We wrote messagebox " + getMessageBoxofTheCommand(globalBytesRead));
                        String tempExtrString=new String();
                        for (int j=10; j<globalBytesRead.length; j++) {
                            tempExtrString+=""+ Integer.toHexString(globalBytesRead[j]);
                        }
                        Log.d("NettyDecoder", "Extracted data was" + new String(globalBytesRead));

                        out.add(globalBytesRead);
                        globalBytesRead = new byte[0];
                        Log.d("NettyDecoder", "Reintialized the globalbytes");

                    } else {
                        Log.d("NettyDecoder", "clearly the Data we recieved is more than one Luci packet");

                    /* takeout the extra bytes separately */
                        byte[] extraBytes = new byte[globalBytesRead.length - lengthOfThePacketWeAreLookingFor];
                        for (int i = lengthOfThePacketWeAreLookingFor; i < globalBytesRead.length; i++) {
                            extraBytes[i - lengthOfThePacketWeAreLookingFor] = globalBytesRead[i];

                        }


                    /* write the global bytes till datalength */
                        byte[] newGlobalArry = new byte[lengthOfThePacketWeAreLookingFor];
                        for (int i = 0; i < lengthOfThePacketWeAreLookingFor; i++) {
                            newGlobalArry[i] = globalBytesRead[i];
                        }

                        out.add(newGlobalArry);


                        Log.d("NettyDecoder", "Extracted only " + newGlobalArry.length + " as we were looking for packet size of  " + lengthOfThePacketWeAreLookingFor + " whose message box is" + getMessageBoxofTheCommand(newGlobalArry));
                        String globalString=new String();
                        for (int j=10; j<newGlobalArry.length; j++) {
                            globalString+=""+ Integer.toHexString(newGlobalArry[j]);
                        }
                        Log.d("NettyDecoder", "Extracted data is" + new String(newGlobalArry));

                        globalBytesRead = extraBytes;
                        if (globalBytesRead.length >= 10) {

                            Log.d("NettyDecoder", "Now we are processing extrabytes with total length" + extraBytes.length + " while packet length from header is " + getPacketLength(extraBytes));
                            processExtraBytes(out);

                        }


                    }


                }
            }

        }
    }

    private void processExtraBytes(List<Object> out) {

         /* First check ff the read bytes is atleast the size of header*/
        if (globalBytesRead.length < 10) {

            Log.d("NettyDecoder_extra", "Header is not present yet");
            return; // (3)
        }
        else if (getMessageBoxofTheCommand(globalBytesRead)<0||getMessageBoxofTheCommand(globalBytesRead)>1200 )
        {
            String tempExtrString=new String();
            for (int j=0; j<globalBytesRead.length; j++) {
                tempExtrString+=""+ Integer.toHexString(globalBytesRead[j]);
            }
            Log.d("NettyDecoder_extra", "globalbytes are ignored as they constituted invalid stream " + tempExtrString);
            Log.d("NettyDecoder_extra", "globalbytes are ignored had messagebox as " + getMessageBoxofTheCommand(globalBytesRead));
            globalBytesRead=new byte[0];
            return;
        }


        /* Now you have the header completly, check the data length and contsize till you get*/
        else {

            int lengthOfThePacketWeAreLookingFor = getPacketLength(globalBytesRead);

            if (globalBytesRead.length < lengthOfThePacketWeAreLookingFor) {
                Log.d("NettyDecoder_extra", "We were looking for packet of size " + lengthOfThePacketWeAreLookingFor + " what we have recieved till now is " + globalBytesRead.length);
                return;
            } else {
                Log.d("NettyDecoder_extra", "We were looking for packet of size " + lengthOfThePacketWeAreLookingFor + " what we have recieved till now is " + globalBytesRead.length);
                if (lengthOfThePacketWeAreLookingFor == globalBytesRead.length) {
                    /* No extra bytes read and hence everything is fine */
                    Log.d("NettyDecoder_extra", "We wrote messagebox " + getMessageBoxofTheCommand(globalBytesRead));
                    String tempExtrString=new String();
                    for (int j=10; j<globalBytesRead.length; j++) {
                        tempExtrString+=""+ Integer.toHexString(globalBytesRead[j]);
                    }
                    Log.d("NettyDecoder_extra", "Extracted data was" + new String(globalBytesRead));

                    out.add(globalBytesRead);
                    globalBytesRead = new byte[0];
                    Log.d("NettyDecoder_extra", "Reintialized the globalbytes");

                } else {
                    Log.d("NettyDecoder_extra", "clearly the Data we recieved is more than one Luci packet");

                    /* takeout the extra bytes separately */
                    byte[] extraBytes = new byte[globalBytesRead.length - lengthOfThePacketWeAreLookingFor];
                    for (int i = lengthOfThePacketWeAreLookingFor; i < globalBytesRead.length; i++) {
                        extraBytes[i - lengthOfThePacketWeAreLookingFor] = globalBytesRead[i];

                    }


                    /* write the global bytes till datalength */
                    byte[] newGlobalArry = new byte[lengthOfThePacketWeAreLookingFor];
                    for (int i = 0; i < lengthOfThePacketWeAreLookingFor; i++) {
                        newGlobalArry[i] = globalBytesRead[i];
                    }

                    out.add(newGlobalArry);


                    Log.d("NettyDecoder_extra", "Extracted only " + newGlobalArry.length + " as we were looking for packet size of  " + lengthOfThePacketWeAreLookingFor + " whose message box is" + getMessageBoxofTheCommand(newGlobalArry));
                    String globalString=new String();
                    for (int j=10; j<newGlobalArry.length; j++) {
                        globalString+=""+ Integer.toHexString(newGlobalArry[j]);
                    }
                    Log.d("NettyDecoder_extra", "Extracted data is" + new String(newGlobalArry));





                    globalBytesRead = extraBytes;
                    if (globalBytesRead.length >= 10) {

                        Log.d("NettyDecoder_extra", "Now we are processing extrabytes with total length" + extraBytes.length + " while packet length from header is " + getPacketLength(extraBytes));
                        processExtraBytes(out);

                    }


                }


            }
        }

    }



    public int getPacketLength(byte[] packetbytes){

    int offset=0;
    int datalengthInPacket = (packetbytes[offset + 8] << 8) | (packetbytes[offset + 9] & 0xFF);
    int msgBox = packetbytes[3] * 16 + packetbytes[4];
    return  datalengthInPacket+10;

}

    public byte[] addToGlobalArray(byte[] one,byte[] two){
        byte[] combined = new byte[one.length + two.length];

        for (int i = 0; i < combined.length; ++i)
        {
            combined[i] = i < one.length ? one[i] : two[i - one.length];
        }

        return combined;
    }

    public int getDatalengthForExtra(byte[] packetbytes){


        int offset=0;
        int datalengthInPacket = (packetbytes[offset + 8] << 8) | (packetbytes[offset + 9] & 0xFF);

        int msgBox = packetbytes[3] * 16 + packetbytes[4];


        return  datalengthInPacket+10;

    }


    public int getMessageBoxofTheCommand(byte[] packetbytes) {
        return packetbytes[3] * 16 + (packetbytes[4] & 0xFF);
    }




    }