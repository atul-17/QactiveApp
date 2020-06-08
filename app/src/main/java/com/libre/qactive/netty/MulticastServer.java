package com.libre.qactive.netty;

/**
 * Created by karunakaran on 9/24/2015.
 */
import com.libre.qactive.Scanning.Utils;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ChannelFactory;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.InternetProtocolFamily;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.net.*;
import java.util.Enumeration;
import java.util.concurrent.Executors;

public class MulticastServer extends Thread {
    private InetSocketAddress groupAddress;

    public MulticastServer(InetSocketAddress groupAddress) {
        this.groupAddress = groupAddress;
    }

    private class MulticastHandler extends  SimpleChannelInboundHandler<DatagramPacket> {


        @Override
        protected void messageReceived(ChannelHandlerContext channelHandlerContext, DatagramPacket datagramPacket) throws Exception {
            System.out.println("Message Received:: " + datagramPacket.content().toString());
        }
    }

    public void run() {
        EventLoopGroup group = new NioEventLoopGroup();
        try {

            NetworkInterface ni =    Utils.getActiveNetworkInterface();
            String name = ni.getName();
            NetworkInterface ni2 = NetworkInterface.getByName(name);
            Enumeration<InetAddress> addresses = ni2.getInetAddresses();
            InetAddress localAddress = null;
            while (addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();
                if (address instanceof Inet4Address){
                    localAddress = address;
                }
            }
            Bootstrap b = new Bootstrap()
                    .group(group)
                    .channelFactory(new ChannelFactory<NioDatagramChannel>() {
                        @Override
                        public NioDatagramChannel  newChannel() {
                            return new NioDatagramChannel(InternetProtocolFamily.IPv4);
                        }
                    })
                    .localAddress(localAddress, groupAddress.getPort())
                    .option(ChannelOption.IP_MULTICAST_IF, ni)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .handler(new ChannelInitializer<NioDatagramChannel>() {
                        @Override
                        public void initChannel(NioDatagramChannel ch) throws Exception {
                            ch.pipeline().addLast(new MulticastHandler());
                        }
                    });

            NioDatagramChannel ch = (NioDatagramChannel)b.bind(groupAddress.getPort()).sync().channel();
            ch.joinGroup(groupAddress, ni).sync();
            ch.closeFuture().await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
     //   InetSocketAddress groupAddress = new InetSocketAddress("239.255.27.1", 1234);
       // new MulticastServer(groupAddress).run();
    }
}
