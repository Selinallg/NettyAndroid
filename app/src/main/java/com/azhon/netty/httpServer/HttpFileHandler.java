package com.azhon.netty.httpServer;

/**
 * 接收HTTP文件上传的处理器
 */

import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;

import com.azhon.netty.bean.Result;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.DiskAttribute;
import io.netty.handler.codec.http.multipart.DiskFileUpload;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.ErrorDataDecoderException;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;

public class HttpFileHandler extends SimpleChannelInboundHandler<HttpObject> {

    static {
        DiskFileUpload.deleteOnExitTemporaryFile = true; // should delete file
        // on exit (in normal
        // exit)
        DiskFileUpload.baseDirectory = null; // system temp directory
        DiskAttribute.deleteOnExitTemporaryFile = true; // should delete file on
        // exit (in normal exit)
        DiskAttribute.baseDirectory = null; // system temp directory
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {

        if (msg instanceof FullHttpRequest) {

            FullHttpRequest request = (FullHttpRequest) msg;
            URI uri = new URI(request.uri());
            String path = uri.getPath();

            if (!path.startsWith("/user")) {
                response(ctx, "", Unpooled.copiedBuffer(Result.error("未实现的请求地址").getBytes()), HttpResponseStatus.OK);
            } else if (request.method().equals(HttpMethod.OPTIONS)) { //处理跨域请求
                response(ctx, "", Unpooled.copiedBuffer(Result.ok("成功").getBytes()), HttpResponseStatus.OK);
            } else if (request.method().equals(HttpMethod.POST)){   //文件通过post进行上传
                try {

                    //"multipart/form-data" ： 代表在表单中进行文件上传
                    if (!request.headers().get(HttpHeaderNames.CONTENT_TYPE).contains("multipart/form-data")){
                        return;
                    }

                    HttpDataFactory factory = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE);
                    HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(factory, request);
                    List<InterfaceHttpData> dataList = decoder.getBodyHttpDatas();

                    if (dataList == null) {
                        return;
                    }
                    for (int ni = 0; ni < dataList.size(); ni++) {
                        writeHttpData(dataList.get(ni));
                    }

                    decoder.destroy();

                    response(ctx, "", Unpooled.copiedBuffer(Result.ok("接收成功").getBytes()), HttpResponseStatus.OK);
                } catch (ErrorDataDecoderException e1) {
                    e1.printStackTrace();
                    response(ctx, "", Unpooled.copiedBuffer(Result.error("解码失败").getBytes()), HttpResponseStatus.OK);
                }


            }
        }
    }

    private void response(ChannelHandlerContext ctx, String type, ByteBuf byteBuf, HttpResponseStatus status) {
        FullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, byteBuf);
        if (TextUtils.isEmpty(type)) {
            httpResponse.headers().add(HttpHeaderNames.CONTENT_TYPE, "text/json;charset=UTF-8");
        } else {
            httpResponse.headers().add(HttpHeaderNames.CONTENT_TYPE, type);
        }
        ctx.writeAndFlush(httpResponse).addListener(ChannelFutureListener.CLOSE);
    }

    private void writeHttpData(InterfaceHttpData data) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //TODO 判断没有存储权限则返回
            //int permission = ContextCompat.checkSelfPermission(this, "android.permission.WRITE_EXTERNAL_STORAGE");
        }


        if (data.getHttpDataType() == HttpDataType.FileUpload) {
            FileUpload fileUpload = (FileUpload) data;
            if (fileUpload.isCompleted()) {
                File dir = new File(Environment.getExternalStorageDirectory() + "/download/");
                if (!dir.exists()) {
                    dir.mkdir();
                }
                File dest = new File(dir, "record.apk");//根据接收到的文件类型来决定文件的后缀
                try {
                    fileUpload.renameTo(dest);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}