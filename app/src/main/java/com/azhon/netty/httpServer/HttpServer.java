package com.azhon.netty.httpServer;

import android.util.Log;

import java.net.InetSocketAddress;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;


public class HttpServer {
    private static final String TAG = "HttpServer";
    //服务开启在的端口
    public static final int PORT = 7020;

    public void startHttpServer() {
        try {
            EventLoopGroup bossGroup   = new NioEventLoopGroup();
            EventLoopGroup  workerGroup = new NioEventLoopGroup();
            ServerBootstrap b           = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            // http服务器端对request解码
                            pipeline.addLast(new HttpRequestDecoder());
                            // http服务器端对response编码
                            pipeline.addLast(new HttpResponseEncoder());
                            // 在处理POST消息体时需要加上
                            pipeline.addLast(new HttpObjectAggregator(65536));
                            // 处理发起的请求
                            pipeline.addLast(new HttpServerHandler());
                        }
                    });
            //绑定服务在7020端口上
            b.bind(new InetSocketAddress(PORT)).sync();
            Log.d(TAG, "HTTP服务启动成功 PORT=" + PORT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

