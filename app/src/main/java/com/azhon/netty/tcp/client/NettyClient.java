package com.azhon.netty.tcp.client;

import android.os.Handler;
import android.util.Log;

import java.net.InetSocketAddress;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * 项目名:    Netty-Android
 * 包名       com.azhon.netty.tcp.client
 * 文件名:    NettyClient
 * 创建时间:  2019-09-07 on 22:45
 * 描述:     TODO
 *
 * @author 阿钟
 */

public class NettyClient {

    private static final String TAG = "NettyClient";
    private final int PORT = 7010;
    private final String IP = "192.168.31.102";
    private static NettyClient nettyClient;
    private Channel channel;
    private Handler handler;
    private Bootstrap bootstrap;

    public static NettyClient getInstance() {
        if (nettyClient == null) {
            nettyClient = new NettyClient();
        }
        return nettyClient;
    }

    /**
     * 需要在子线程中发起连接
     */
    private NettyClient() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                connect();
            }
        }).start();
    }

    /**
     * 获取与服务端的连接
     */
    public static Channel getChannel() {
        if (nettyClient == null) {
            return null;
        }
        return nettyClient.channel;
    }

    /**
     * 连接服务端
     */
    public void connect() {
        try {
            NioEventLoopGroup group = new NioEventLoopGroup();
            bootstrap = new Bootstrap()
                    // 指定channel类型
                    .channel(NioSocketChannel.class)
                    // 指定EventLoopGroup
                    .group(group)
                    // 指定Handler
                    .handler(new ChannelInitializer<SocketChannel>() {
                        //分隔符
                        ByteBuf delimiter = Unpooled.copiedBuffer("$".getBytes());

                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            // .addLast(new IdleStateHandler(30, 10, 0))
                            //参数1：代表读套接字超时的时间，例如30秒没收到数据会触发读超时回调;
                            //参数2：代表写套接字超时时间，例如10秒没有进行写会触发写超时回调;
                            //参数3：将在未执行读取或写入时触发超时回调，0代表不处理；读超时尽量设置大于写超时代表多次写超时时写心跳包，多次写了心跳数据仍然读超时代表当前连接错误，即可断开连接重新连接

                            pipeline.addLast(new IdleStateHandler(10, 0, 0));
                            //解决粘包
                            pipeline.addLast(new DelimiterBasedFrameDecoder(65535, delimiter));
                            //添加发送数据编码器
                            pipeline.addLast(new ClientEncoder());
                            //添加收到的数据解码器
                            pipeline.addLast(new ClientDecoder());
                            //添加数据处理器
                            pipeline.addLast(new ClientHandler(NettyClient.this));
                        }
                    });
//           // 设置相关参数
            // 接收数据的缓冲区太小 解决办法：
//            bootstrap.option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(5000, 5000, 8000)); //接收缓冲区 最小值太小时数据接收不全
//            bootstrap.option(ChannelOption.TCP_NODELAY, true); //无阻塞
//            bootstrap.option(ChannelOption.SO_KEEPALIVE, true); //长连接
//            bootstrap.option(ChannelOption.SO_TIMEOUT, 30000); //收发超时
            // 连接到服务端
            ChannelFuture channelFuture = bootstrap.connect(new InetSocketAddress(IP, PORT));
            // 添加连接状态监听
            channelFuture.addListener(new ConnectListener(this));
            //获取连接通道
            channel = channelFuture.sync().channel();
            handler.obtainMessage(0, "连接成功").sendToTarget();
        } catch (Exception e) {
            handler.obtainMessage(0, "连接失败").sendToTarget();
            Log.e(TAG, "连接失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }


    /**
     * 重连
     */
    public void reConnect() {
        try {
            ChannelFuture channelFuture = bootstrap.connect(new InetSocketAddress(IP, PORT));
            channel = channelFuture.sync().channel();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
