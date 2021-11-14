package com.azhon.netty.server;


import android.os.Handler;
import android.util.Log;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;

/**
 * 项目名:    Netty-Android
 * 包名       com.azhon.netty.server
 * 文件名:    NettyServer
 * 创建时间:  2019-09-06 on 00:11
 * 描述:     TODO
 *
 * @author 阿钟
 */

public class NettyServer {
    private static final String TAG = "NettyServer";

    //端口
    private static final int PORT = 7010;
    private Handler handler;

    /**
     * 启动tcp服务端
     */
    public void startServer() {
        try {
            EventLoopGroup bossGroup = new NioEventLoopGroup();
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        //分隔符
                        ByteBuf delimiter = Unpooled.copiedBuffer("$".getBytes());

                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            //解决粘包
                            pipeline.addLast(new DelimiterBasedFrameDecoder(65535, delimiter));
                            //添加发送数据编码器
                            pipeline.addLast(new ServerEncoder());
                            //添加解码器，对收到的数据进行解码
                            pipeline.addLast(new ServerDecoder());
                            //添加数据处理
                            pipeline.addLast(new ServerHandler());
                        }
                    });
            //服务器启动辅助类配置完成后，调用 bind 方法绑定监听端口，调用 sync 方法同步等待绑定操作完成
            b.bind(PORT).sync();
            handler.obtainMessage(0, "TCP 服务启动成功 PORT = " + PORT).sendToTarget();
            Log.d(TAG, "TCP 服务启动成功 PORT = " + PORT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }
}
