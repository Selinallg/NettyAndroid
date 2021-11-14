package com.azhon.netty.bean;

import org.json.JSONException;
import org.json.JSONObject;

public class Result {
    /**
     * 响应请求c成功
     */
    public static String ok(String msg) {
        JSONObject object = new JSONObject();
        try {
            object.put("code", 100);
            object.put("msg", msg);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object.toString();
    }

    /**
     * 响应请求失败
     */
    public static String error(String msg) {
        JSONObject object = new JSONObject();
        try {
            object.put("code", 101);
            object.put("msg", msg);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object.toString();
    }
}

