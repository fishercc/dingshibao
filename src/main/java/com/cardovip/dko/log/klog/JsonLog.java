package com.cardovip.dko.log.klog;

import android.util.Log;


import com.cardovip.dko.log.log;
import com.cardovip.dko.log.LogUtil;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by zhaokaiqiang on 15/11/18.
 */
@StringFogIgnore
public class JsonLog {

    public static void printJson(String tag, String msg, String headString) {

        String message;

        try {
            if (msg.startsWith("{")) {
                JSONObject jsonObject = new JSONObject(msg);
                message = jsonObject.toString(log.JSON_INDENT);
            } else if (msg.startsWith("[")) {
                JSONArray jsonArray = new JSONArray(msg);
                message = jsonArray.toString(log.JSON_INDENT);
            } else {
                message = msg;
            }
        } catch (JSONException e) {
            message = msg;
        }

        LogUtil.printLine(tag, true);
        message = headString + log.LINE_SEPARATOR + message;
        String[] lines = message.split(log.LINE_SEPARATOR);
        for (String line : lines) {
            Log.d(tag, "║ " + line);
        }
        LogUtil.printLine(tag, false);
    }
}
