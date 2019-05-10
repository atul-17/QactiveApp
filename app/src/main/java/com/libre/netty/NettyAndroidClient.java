package com.libre.netty;

/**
 * Created by praveena on 7/10/15.
 */

import android.util.Log;

import java.net.InetAddress;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;


public class NettyAndroidClient {

    private static final String TAG = NettyAndroidClient.class.getName();
    NettyClientHandler handler;
    String remotehost;
    int port;
    long lastNotifiedTime;

    public long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(long creationTime) {

        this.creationTime = creationTime;
    }

    long creationTime;

    private static NettyAndroidClient dummyCLient = new NettyAndroidClient();
    /*need not create every time for all clients*/
    private static EventLoopGroup workerGroup = new NioEventLoopGroup();


    private NettyAndroidClient() {

    }

    public static NettyAndroidClient getDummyInstance() {
        dummyCLient.setRemotehost("0.0.0.0");
        dummyCLient.creationTime = System.currentTimeMillis();
        return dummyCLient;
    }

    public NettyAndroidClient(InetAddress host, int port) throws Exception {


        handler = new NettyClientHandler(host.getHostAddress());
        remotehost = host.getHostAddress();
        port = port;


        Bootstrap b = new Bootstrap(); // (1)

        b.group(workerGroup); // (2)
        b.channel(NioSocketChannel.class); // (3)

        b.option(ChannelOption.TCP_NODELAY, true);
        b.option(ChannelOption.SO_KEEPALIVE, true); // (4)
        b.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast("IdleChecker", new IdleStateHandler(10, 10, 10));
                ch.pipeline().addLast("IdleDisconnecter", new HeartbeatHandler(remotehost));
                ch.pipeline().addLast(new NettyDecoder(), handler);
            }
        });

        // Start the client.
        ChannelFuture f = b.connect(host, port).sync();
        setCreationTime(System.currentTimeMillis());
        /*f.addListener(new ChannelFutureListener() {
                          @Override
                          public void operationComplete(ChannelFuture channelFuture) throws Exception {

                              if (channelFuture.isSuccess()) {
                                  LibreLogger.d(this, "Channel success for " + channelFuture.channel().remoteAddress());

                              } else {
                                  LibreLogger.d(this, "CHANNEL FAILED for " + channelFuture.channel().remoteAddress());

                              }
                          }
                      }
        );*/

        /*    ChannelFuture f = b.connect(host, port).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    if(channelFuture.isSuccess()){
                        Log.d(TAG,"Creation of Socket Successful"+ channelFuture.channel().id()+":"+
                                remotehost);
                        socketCreated = true;
                    }else{
                        Log.d(TAG,"Creation of Socket UnSuccessful");
                        socketCreated = false;
                    }
                }
            }).await();*/
        Log.e("Scan_Netty", "Created a Netty socket to listen ");


    }

    public NettyClientHandler getHandler(){
        return handler;
    }
    public String getRemotehost() {
        return remotehost;
    }

    public void setRemotehost(String remotehost) {
        this.remotehost = remotehost;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void write(byte[] bytes) {
        if (handler != null) {
            handler.write(bytes);

        }
    }


    public long getLastNotifiedTime() {
        return lastNotifiedTime;
    }

    public void setLastNotifiedTime(long notifiedTime) {
        lastNotifiedTime = notifiedTime;
    }


    /**
     * LatestDiscoveryChanges
     * this method will close existing NettyAndroidClient
     */
    public void closeSocket() {
        if (handler != null && handler.mChannelContext != null)
            handler.mChannelContext.close();
    }


}
