package com.azhon.netty.udp;

import android.util.Log;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.CharsetUtil;

public class Udpserver {
    public static void init() {
        new Thread(){
            @Override
            public void run() {
                try {
                    Bootstrap      b     = new Bootstrap();
                    EventLoopGroup group = new NioEventLoopGroup();
                    b.group(group)
                            .channel(NioDatagramChannel.class)
                            .option(ChannelOption.SO_BROADCAST, true)
                            .handler(new UdpServerHandler());
                    Log.d("nettyudp","server");
                    b.bind(1112).sync().channel().closeFuture().await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }

    private static class UdpServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {
            String body = packet.content().toString(CharsetUtil.UTF_8);
            Log.d("nettyudp","server>>"+body);

            DatagramPacket data = new DatagramPacket(Unpooled.copiedBuffer("echo from server", CharsetUtil.UTF_8), packet.sender());
            ctx.writeAndFlush(data);
        }
    }

}

