package com.azhon.netty.tcp.client;

import android.util.Log;

import com.azhon.netty.bean.PkgDataBean;

import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * 项目名:    Netty-Android
 * 包名       com.azhon.netty.tcp.client
 * 文件名:    ClientHandler
 * 创建时间:  2019-09-07 on 22:54
 * 描述:     TODO
 *
 * @author 阿钟
 */

public class ClientHandler extends SimpleChannelInboundHandler<Object> {

    private static final String TAG = "ClientHandler";
    private NettyClient client;

    public ClientHandler(NettyClient nettyClient) {
        this.client = nettyClient;

    }

    /**
     * 当收到数据的回调
     *
     * @param channelHandlerContext 封装的连接对像
     * @param o
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
        Log.d(TAG, "客户端收到了数据：" + o.toString());
    }

    /**
     * 与服务端连接成功的回调
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        Log.d(TAG, "与服务端连接成功：" + ctx.toString());
    }

    /**
     * 与服务端断开的回调
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        Log.d(TAG, "与服务端断开连接：" + ctx.toString());
        //启动重连
        reConnect(ctx);
    }

    /**
     * 5s重连一次服务端
     *
     * @param ctx
     */
    private void reConnect(final ChannelHandlerContext ctx) {
        EventLoop loop = ctx.channel().eventLoop();
        loop.schedule(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "连接断开，发起重连");
                client.connect();
            }
        }, 5, TimeUnit.SECONDS);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
        if (evt instanceof IdleStateEvent) {
            if (((IdleStateEvent) evt).state() == IdleState.READER_IDLE) {
                sendHeartPkg(ctx);
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        Log.d(TAG, "exceptionCaught: "+cause.getMessage());
    }

    /**
     * 发送心跳
     */
    private void sendHeartPkg(ChannelHandlerContext ctx) {
        PkgDataBean bean = new PkgDataBean();
        bean.setCmd((byte) 0x02);
        bean.setData("心跳数据包");
        bean.setDataLength((byte) bean.getData().getBytes().length);
        ctx.channel().writeAndFlush(bean);
        Log.d(TAG, "客户端发送心跳成功");
    }
}