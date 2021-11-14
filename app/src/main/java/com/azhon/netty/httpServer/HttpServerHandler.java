package com.azhon.netty.httpServer;

import android.net.Uri;
import android.util.Log;

import com.azhon.netty.bean.Result;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;

public class HttpServerHandler extends ChannelInboundHandlerAdapter {
    private static final String TAG = "HttpServerHandler";

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof FullHttpRequest)) {
            Log.e(TAG, "未知请求：" + msg.toString());
            return;
        }
        FullHttpRequest httpRequest = (FullHttpRequest) msg;
        String     path   = httpRequest.uri();
        HttpMethod method = httpRequest.method();

        String              route  = parseRoute(path);
        Map<String, Object> params = new HashMap<>();
        if (method == HttpMethod.GET) {
            parseGetParams(params, path);
        } else if (method == HttpMethod.POST) {
            parsePostParams(params, httpRequest);
        } else {
            //错误的请求方式
            ByteBuf byteBuf = Unpooled.copiedBuffer(Result.error("不支持的请求方式").getBytes());
            response(ctx, "text/json;charset=UTF-8", byteBuf, HttpResponseStatus.BAD_REQUEST);
        }
        Log.d(TAG, "==================接收到了请求==================");
        Log.d(TAG, "route = " + route);
        Log.d(TAG, "method = " + method);
        Log.d(TAG, "params = " + params.toString());

        handlerRequest(ctx, route, params);
    }

    /**
     * 处理每个请求
     */
    private void handlerRequest(ChannelHandlerContext ctx, String route, Map<String, Object> params) throws Exception {
        switch (route) {
            case "login":
                ByteBuf login;
                if ("admin".equals(params.get("name")) && "123".equals(params.get("psd"))) {
                    login = Unpooled.copiedBuffer(Result.ok("登录成功").getBytes());
                } else {
                    login = Unpooled.copiedBuffer(Result.error("登录失败").getBytes());
                }
                response(ctx, "text/json;charset=UTF-8", login, HttpResponseStatus.OK);
                break;
            case "getImage":
                //返回一张图片
                ByteBuf image = getImage(new File("/storage/emulated/0/Android/data/com.azhon.nettyhttp/cache/test.jpg"));
                response(ctx, "image/jpg", image, HttpResponseStatus.OK);
                break;
            case "json":
                ByteBuf json = Unpooled.copiedBuffer(Result.ok("测试post请求成功").getBytes());
                response(ctx, "text/json;charset=UTF-8", json, HttpResponseStatus.OK);
                break;
            default:
                ByteBuf error = Unpooled.copiedBuffer(Result.error("未实现的请求地址").getBytes());
                response(ctx, "text/json;charset=UTF-8", error, HttpResponseStatus.BAD_REQUEST);
                break;
        }
    }

    /**
     * 解析Get请求参数
     */
    private void parseGetParams(Map<String, Object> params, String path) {
        //拼接成全路径好取参数
        Uri              uri      = Uri.parse("http://127.0.0.1" + path);
        Set<String>      names    = uri.getQueryParameterNames();
        Iterator<String> iterator = names.iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            params.put(key, uri.getQueryParameter(key));
        }
    }

    /**
     * 解析Post请求参数，以提交的body为json为例
     */
    private void parsePostParams(Map<String, Object> params, FullHttpRequest httpRequest) throws JSONException {
        ByteBuf content = httpRequest.content();
        String           body   = content.toString(CharsetUtil.UTF_8);
        JSONObject       object = new JSONObject(body);
        Iterator<String> keys   = object.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            params.put(key, object.opt(key));
        }
    }

    /**
     * 解析调用的接口（路由地址）
     */
    private String parseRoute(String path) {
        if (path.contains("?")) {
            String uri = path.split("\\?")[0];
            return uri.substring(1);
        } else {
            return path.substring(1);
        }
    }


    /**
     * 返回图片
     */
    private ByteBuf getImage(File file) throws Exception {
        ByteBuf         byteBuf = Unpooled.buffer();
        FileInputStream stream  = new FileInputStream(file);
        int             len;
        byte[] buff = new byte[1024];
        while ((len = stream.read(buff)) != -1) {
            byteBuf.writeBytes(buff, 0, len);
        }
        return byteBuf;
    }

    /**
     * 响应请求结果
     *
     * @param ctx         返回
     * @param contentType 响应类型
     * @param content     消息
     * @param status      状态
     */
    private void response(ChannelHandlerContext ctx, String contentType, ByteBuf content, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, content);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
        // 解决跨域问题
        response.headers().add("Access-Control-Allow-Origin", "*");
        response.headers().add("Access-Control-Allow-Methods", "GET, POST, PUT,DELETE,OPTIONS,PATCH");
        response.headers().add("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Authorization");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}

