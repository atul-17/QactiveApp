package com.libre.netty;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.libre.LibreApplication;
import com.libre.Scanning.Constants;
import com.libre.Scanning.ScanningHandler;
import com.libre.SceneObject;
import com.libre.alexa.DeviceProvisioningInfo;
import com.libre.app.dlna.dmc.server.ContentTree;
import com.libre.app.dlna.dmc.utility.PlaybackHelper;
import com.libre.app.dlna.dmc.utility.UpnpDeviceManager;
import com.libre.constants.LSSDPCONST;
import com.libre.constants.LUCIMESSAGES;
import com.libre.constants.MIDCONST;
import com.libre.luci.LSSDPNodeDB;
import com.libre.luci.LSSDPNodes;
import com.libre.luci.LUCIControl;
import com.libre.luci.LUCIPacket;
import com.libre.util.LibreLogger;
import com.squareup.otto.Bus;

import org.fourthline.cling.model.meta.RemoteDevice;
import org.json.JSONObject;

import java.net.SocketAddress;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;


/**
 * Created by praveena on 7/10/15.
 */
public class NettyClientHandler extends ChannelHandlerAdapter {

    private static final String TAG = "NettyClientHandler";
    public ChannelHandlerContext mChannelContext;

    private final String remotedevice;
    Handler handler;


    public NettyClientHandler(final String remotedevice) {
        this.remotedevice = remotedevice;

        HandlerThread thread = new HandlerThread("MyHandlerThread");
        thread.start();

        handler = new Handler(thread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == Constants.CHECK_ALIVE) {
                    if (LUCIControl.luciSocketMap.containsKey(remotedevice)) {
                        if ((System.currentTimeMillis() - LUCIControl.luciSocketMap.get(remotedevice).getLastNotifiedTime() > 10000)) {


                            try {


                                LibreLogger.d(this, "Trying to write bye bye");
                                if (mChannelContext != null && isSocketToBeRemovedFromTheTCPMap(mChannelContext, remotedevice)) {

                                    String messageStr = "" + LibreApplication.LOCAL_IP;
                                    LUCIPacket packet = new LUCIPacket(messageStr.getBytes(), (short) messageStr.length(), (short) MIDCONST.MID_DE_REGISTER, (byte) LSSDPCONST.LUCI_SET);

                                    int msg_length = packet.getlength();
                                    byte[] message = new byte[msg_length];
                                    packet.getPacket(message);

                                    write(message, true);

                                }
                            } catch (Exception e) {
                                LibreLogger.d(this, "Failure while sending the deregyster command");
                            }
                            String message = msg.getData().getString("Message");
                            if (message != null) {
                                LibreLogger.d(this, "Send the Message as " + message + " to the ip " + remotedevice);
                            } else {
                                LibreLogger.d(this, "Send the Message as NULL  to the ip " + remotedevice);
                            }

                            if (mChannelContext != null && isSocketToBeRemovedFromTheTCPMap(mChannelContext, remotedevice)) {
                                if (handler.hasMessages(Constants.CHECK_ALIVE))
                                    handler.removeMessages(Constants.CHECK_ALIVE);
                                BusProvider.getInstance().post(new RemovedLibreDevice(remotedevice));
                            }

                        }
                    }
                }

            }
        };

    }

    private void write(byte[] sbytes, boolean b) {

        if (mChannelContext != null) {
            final ByteBuf byteBufMsg = mChannelContext.channel().alloc().buffer(sbytes.length);
            byteBufMsg.writeBytes(sbytes);
            byte[] bytenew = new byte[sbytes.length];
            String str = "";
            bytenew = byteBufMsg.array();
            for (int i = 0; i < bytenew.length; i++) {
                str += bytenew[i] + ",";
            }
/*
            ChannelFuture channelFuture = mChannelContext.channel().writeAndFlush(byteBufMsg);
            boolean variable = channelFuture.isSuccess();
            Log.e(TAG,"Exact Data Sent : " +variable+": DataSent To Device :"+str);
            channelFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    Log.d(TAG, "Application writing " + new String(sbytes) + " completed ");

                }
            });
*/

            LibreLogger.d(this, "Channel Is Connected or Not " + mChannelContext.channel().isActive());
//            mChannelContext.pipeline().fireChannelRead("1");
            try {
                mChannelContext.channel().writeAndFlush(byteBufMsg).sync();
                handler.sendEmptyMessageDelayed(Constants.CHECK_ALIVE, 10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                LibreLogger.d(this, "InterruptedException Happend in Writing the Data, Dont Know What is the Issue ");
            } catch (Exception e) {
                e.printStackTrace();

                LibreLogger.d(this, "Exception Happend in Writing the Data, Pending Write Issues Have been Cancelled, Device Got Rebooted ");
            }

        }

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {

        byte[] databytes = (byte[]) msg;

        /*ByteBuf m = (ByteBuf) msg;

        int numReadBytes = ((ByteBuf) msg).readableBytes();
        byte[] bytes = new byte[numReadBytes];
        m.readBytes(bytes);

        Log.d(TAG, "Application received of length mumReadByyes =" +numReadBytes);

        byte[] databytes=bytes.clone();*/
        NettyData nData = new NettyData(remotedevice, databytes);


        LUCIPacket packet = new LUCIPacket(nData.getMessage());

        if (packet.getCommand() == MIDCONST.ALEXA_COMMAND) {

            String alexaMessage = new String(packet.getpayload());
            LibreLogger.d(this, "Alexa Value From 230  " + alexaMessage);

            try {
                JSONObject jsonRootObject = new JSONObject(alexaMessage);
                // JSONArray jsonArray = jsonRootObject.optJSONArray("Window CONTENTS");
                JSONObject jsonObject = jsonRootObject.getJSONObject(LUCIMESSAGES.ALEXA_KEY_WINDOW_CONTENT);
                String productId = jsonObject.optString("PRODUCT_ID").toString();
                String dsn = jsonObject.optString("DSN").toString();
                String sessionId = jsonObject.optString("SESSION_ID").toString();
                String codeChallenge = jsonObject.optString("CODE_CHALLENGE").toString();
                String codeChallengeMethod = jsonObject.optString("CODE_CHALLENGE_METHOD").toString();
                String locale = "";
                if (jsonObject.has("LOCALE"))
                    locale = jsonObject.optString("LOCALE").toString();
                LSSDPNodes node = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(nData.getRemotedeviceIp());
                if (node != null) {
                    DeviceProvisioningInfo mDeviceProvisioningInfo = new DeviceProvisioningInfo(productId, dsn, sessionId, codeChallenge, codeChallengeMethod, locale);
                    node.setMdeviceProvisioningInfo(mDeviceProvisioningInfo);

                }


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        /* Special case to handle the 54 for DMR playback completed */
        if (packet.getCommand() == 54) {
            String message = new String(packet.getpayload());

            String mIpaddress = nData.getRemotedeviceIp();
            ScanningHandler mScanHandler = ScanningHandler.getInstance();
            SceneObject currentSceneObject = mScanHandler.getSceneObjectFromCentralRepo(mIpaddress);

            if (message != null && (message.equalsIgnoreCase(Constants.DMR_PLAYBACK_COMPLETED)
                    || message.contains(Constants.FAIL)
                    || message.contains(Constants.DMR_SONG_UNSUPPORTED)
                    || message.equalsIgnoreCase(LUCIMESSAGES.NEXTMESSAGE))) {

                LibreLogger.d(this, "Libre logger sent the DMR playback completed ");
                if (currentSceneObject.getPlayUrl() != null)
                    LibreLogger.d(this, "DMR completed for URL " + currentSceneObject.getPlayUrl());
                if (currentSceneObject != null && currentSceneObject.getPlayUrl() != null &&
                        currentSceneObject.getPlayUrl().contains(LibreApplication.LOCAL_IP)
                        && currentSceneObject.getPlayUrl().contains(ContentTree.AUDIO_PREFIX)
                        && currentSceneObject.getCurrentSource() == Constants.DMR_SOURCE) {

                    /* If the playback url is from our ip address
                     *  and the current source is DMR then only we need to send the URL. We get the source change in the message box 10.
                     *  mantis id:584
                     *  */

                    LibreLogger.d(this, "App is going to next song");
                    RemoteDevice renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(nData.getRemotedeviceIp());
                    if (renderingDevice != null) {
                        String renderingUDN = renderingDevice.getIdentity().getUdn().toString();
                        PlaybackHelper playbackHelper = LibreApplication.PLAYBACK_HELPER_MAP.get(renderingUDN);
                        if (playbackHelper != null) {
                            playbackHelper.playNextSong(1);

                        } else {
                            LibreLogger.d(this, "DMR completed for URL " + currentSceneObject.getPlayUrl() +
                                    "PlayBackHelper is Null Without Knowing The Reason");
                        }

                    }
                }
            } else if (message != null && message.equalsIgnoreCase(LUCIMESSAGES.PREVMESSAGE)) {
                if (currentSceneObject.getPlayUrl() != null)
                    LibreLogger.d(this, "DMR completed for URL " + currentSceneObject.getPlayUrl());
                if (currentSceneObject != null && currentSceneObject.getPlayUrl() != null &&
                        currentSceneObject.getPlayUrl().contains(LibreApplication.LOCAL_IP)
                        && currentSceneObject.getPlayUrl().contains(ContentTree.AUDIO_PREFIX)
                        && currentSceneObject.getCurrentSource() == Constants.DMR_SOURCE) {

                    /* If the playback url is from our ip address
                     *  and the current source is DMR then only we need to send the URL. We get the source change in the message box 10.
                     *  mantis id:584
                     *  */

                    LibreLogger.d(this, "App is going to next song");
                    RemoteDevice renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(nData.getRemotedeviceIp());
                    if (renderingDevice != null) {
                        String renderingUDN = renderingDevice.getIdentity().getUdn().toString();
                        PlaybackHelper playbackHelper = LibreApplication.PLAYBACK_HELPER_MAP.get(renderingUDN);
                        if (playbackHelper != null) {
                            playbackHelper.playNextSong(-1);

                        } else {
                            LibreLogger.d(this, "DMR completed for URL " + currentSceneObject.getPlayUrl() +
                                    "PlayBackHelper is Null Without Knowing The Reason");
                        }

                    }
                }
            }
        }


        Bus bus = BusProvider.getInstance();

        try {
            bus.post(nData);
        } catch (Exception e) {
            e.printStackTrace();
            LibreLogger.d(this, TAG + "Exception for throwing an event in otto");
            if (LUCIControl.luciSocketMap.containsKey(remotedevice)) {
                if (isSocketToBeRemovedFromTheTCPMap(ctx, remotedevice)) {
                    LUCIControl.luciSocketMap.remove(remotedevice);
                }
            }
        }


    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LibreLogger.d(this, TAG + " exception Exception " + remotedevice);
        cause.printStackTrace();
        ctx.close();
        /**
         * LatestDiscoveryChanges
         */
        if (LUCIControl.luciSocketMap.containsKey(remotedevice)) {
            if (isSocketToBeRemovedFromTheTCPMap(ctx, remotedevice)) {
                BusProvider.getInstance().post(new RemovedLibreDevice(remotedevice));
                LUCIControl.luciSocketMap.remove(remotedevice);
            }
        }
    }

    public void channelActive(ChannelHandlerContext ctx) {
        LibreLogger.d(this, TAG + "Channel active " + remotedevice + "id as " + ctx.channel().id());
        mChannelContext = ctx;

    }

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
        super.connect(ctx, remoteAddress, localAddress, promise);

        LibreLogger.d(this, TAG + "connecting to  " + remoteAddress.toString() + "from my localdevice " + localAddress);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        LibreLogger.d(this, TAG + "remotedevice disconnected " + remotedevice);

        super.disconnect(ctx, promise);
        /**
         * LatestDiscoveryChanges
         */
        if (LUCIControl.luciSocketMap.containsKey(remotedevice)) {
            if (isSocketToBeRemovedFromTheTCPMap(ctx, remotedevice)) {
                LUCIControl.luciSocketMap.remove(remotedevice);
            }
        }
    }


/*    public void write(String stringMessage){


        Log.d(TAG, "Application is writing " + stringMessage);
        ChannelFuture channelFuture = mChannelContext.writeAndFlush(Unpooled.copiedBuffer(stringMessage, CharsetUtil.UTF_8));

    }*/

    public void write(final byte[] sbytes) {

        if (mChannelContext != null) {
            final ByteBuf byteBufMsg = mChannelContext.channel().alloc().buffer(sbytes.length);
            byteBufMsg.writeBytes(sbytes);
            byte[] bytenew = new byte[sbytes.length];
            String str = "";
            bytenew = byteBufMsg.array();
            for (int i = 0; i < bytenew.length; i++) {
                str += bytenew[i] + ",";
            }

            /*ChannelFuture channelFuture = mChannelContext.channel().writeAndFlush(byteBufMsg);
            boolean variable = channelFuture.isSuccess();
            Log.e(TAG,"Exact Data Sent : " +variable+": DataSent To Device :"+str);
            channelFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    Log.d(TAG, "Application writing " + new String(sbytes) + " completed ");

                }
            });
*/

            LibreLogger.d(this, "Channel Is Connected or Not " + mChannelContext.channel().isActive());
//            mChannelContext.pipeline().fireChannelRead("1");
            try {
                mChannelContext.channel().writeAndFlush(byteBufMsg).sync();
                /* Device will not Send CommandStatus=1 for Volume */
                int mMessageBox = getMessageBoxofTheCommand(sbytes);
                /* KK According to the EMail I have Sent on 16 March , Regarding the LUCI Messages ,Device is not sending Command Status as
                 * i.e., no immeidate response from the device.Only they will repsonse for the below Messagebox if we are doing GET
                 * */
                /* We are not using the below msgbox in the for Setting the Values , thats ComandType as SET */
                /*LUCI_MESSAGEBOX_CURRENT_SOURCE = #MB50
                LUCI_MESSAGEBOX_PLAYSTATE = #MB51
                LUCI_MESSAGEBOX_RSSI = #MB151

                *//* We are using the below msgbox thats ComandType as SET & GET*//*
                LUCI_MESSAGEBOX_VOLUMECONTROL = #MB64
                LUCI_MESSAGEBOX_ZVOLUMECONTROL = #MB219
                LUCI_MESSAGEBOX_KEYCONTROL = #MB41
                LUCI_MESSAGEBOX_REMOTE_UI = #MB42
                LUCI_MESSAGEBOX_LEDCONTROL = #MB207
                LUCI_MESSAGEBOX_NV_READ = #MB208
                LUCI_MESSAGEBOX_SETUP_STEREO_PAIR = #MB108
                LUCI_MESSAGEBOX_DEREGISTER_ASYNC  =#MB4*/
                if (!((mMessageBox == MIDCONST.VOLUME_CONTROL
                        /*|| mMessageBox == MIDCONST.ZONE_VOLUME*/
                        || mMessageBox == MIDCONST.MID_ENV_READ
                        || mMessageBox == MIDCONST.GET_UI
                        || mMessageBox == MIDCONST.MID_REMOTE
                        || mMessageBox == MIDCONST.MID_IOT_CONTROL
                        || mMessageBox == MIDCONST.MID_DE_REGISTER)
                        && getCommandType(sbytes) == LSSDPCONST.LUCI_SET)) {
                    LibreLogger.d(this, "Amit + NettyClient HAndler Timer is Started " + mMessageBox + " :: Value :: " + getCommandType(sbytes));

                    Message msg = new Message();
                    Bundle b = new Bundle();
                    b.putString("Message", str);
                    msg.what = Constants.CHECK_ALIVE;
                    msg.setData(b);
                    handler.sendMessageDelayed(msg, 10000);
                } else {
                    LibreLogger.d(this, "Amit + NettyClient HAndler is  Not Started " + mMessageBox + " :: Value :: " + getCommandType(sbytes));
                }
                /*handler.sendEmptyMessageDelayed(Constants.CHECK_ALIVE, 10000);*/
            } catch (InterruptedException e) {
                e.printStackTrace();
                LibreLogger.d(this, "InterruptedException Happend in Writing the Data, Dont Know What is the Issue ");
            } catch (Exception e) {
                e.printStackTrace();

                LibreLogger.d(this, "Exception Happend in Writing the Data, Pending Write Issues Have been Cancelled, Device Got Rebooted ");
            }

        }

    }
    /* For Getting The Message Box From the Command */
    /* KK  :: Commenting because Android is following the approach of Little indian . So if we are try to get Command from
     * LuciPacket we are getting the wrong value , so we are initiating the timer for all the Commands  but still it will works because
     * NOTIFY packets will update the last Notifed Time. And Our App will not work for 16Bit thats MsgBox greater than 255
     * So I remodified the Code  to support 16 Bit
     *
     * NettyDecoder .java , We dont need to change the Function because , Devec always follow the apporach of Big Indian
     * And Speakers are having inbuilt support to auto conversion of Little Indian To Big Indian */
    /*public int getMessageBoxofTheCommand(byte[] packetbytes) {
        return packetbytes[3] * 16 + (packetbytes[4] & 0xFF);
    }*/

    /* KK:: Remodified Code For Getting The Message Box From the Command */
    public int getMessageBoxofTheCommand(byte[] packetbytes) {
        return packetbytes[3] & 0xFF + ((packetbytes[4] & 0xFF) << 8);
    }


    /* For Getting The Message Box From the Command */
    public int getCommandType(byte[] packetbytes) {
        return packetbytes[2];
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        LibreLogger.d(this, "Channel is broken " + remotedevice + "id as " + ctx.channel().id());
        if (handler.hasMessages(Constants.CHECK_ALIVE))
            handler.removeMessages(Constants.CHECK_ALIVE);
        /**
         * LatestDiscoveryChanges
         */
        if (LUCIControl.luciSocketMap.containsKey(remotedevice)) {
            if (isSocketToBeRemovedFromTheTCPMap(ctx, remotedevice)) {
                LUCIControl.luciSocketMap.remove(remotedevice);
            }
        }
        try {
            ctx.close();
        } catch (Exception e) {
            e.printStackTrace();
            LibreLogger.d(this, " Channel Is Broken And Closing Exception Happend in Writing the Data, Dont Know What is the Issue " + remotedevice);
        }
    }

    private boolean isSocketToBeRemovedFromTheTCPMap(ChannelHandlerContext ctx, String mIpAddress) {
        NettyAndroidClient mAndroidClient = LUCIControl.luciSocketMap.get(remotedevice);
        /* If Hashmap is returning Null that means Hashmap is Empty for the that particular AP , So we can return it as False */
        if (mAndroidClient == null || mAndroidClient.handler == null || mAndroidClient.handler.mChannelContext == null)
            return false;

        if (ctx != null && ctx.channel().id() == mAndroidClient.handler.mChannelContext.channel().id()) {
            /*LibreLogger.d(this, "BROKEN" + remotedevice + "id MATCH  "
                    + ctx.channel().id() + " " +
                    LUCIControl.luciSocketMap.get(remotedevice).handler.mChannelContext.channel().id());
*/

            return true;
        } else {
            LibreLogger.d(this, "BROKEN" + remotedevice + "id DIDNOT MATCH  "
                    + ctx.channel().id() + " " +
                    LUCIControl.luciSocketMap.get(remotedevice).handler.mChannelContext.channel().id());

            return false;
        }
    }


}