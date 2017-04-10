package com.ledboot;

import android.app.Application;
import android.content.Context;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ouyangxingyu198 on 17/4/10.
 */

public class NetMonitor {

    private static Context mContext;

    public static Map<String,String> cacheMap = new HashMap<>();

    /**
     * 初始化Netmonitor
     * @param context
     */
    public static void init(Context context){
        mContext = context;
        if(!(context instanceof Application)){
            mContext = context.getApplicationContext();
        }
    }

    /**
     * 设置头部参数
     * @param name
     * @param value
     */
    public static void put(String name,String value){
        cacheMap.put(name,value);
    }


}
