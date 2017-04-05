package com.ledboot.interceptor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ouyangxingyu198 on 17/3/28.
 */

public class HttpInterceptor {
    /**
     * 注入头部头部
     * @param o
     */
    public static void injectHeader(Object o){
        try {
            Method method = o.getClass().getDeclaredMethod("add",new Class[]{String.class,String.class});
            if(method != null){
                method.invoke(o,new Object[]{"key1","123456"});
            }else{
                System.out.println("method is null!");
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * test
     */
    public static void injectHeader(int a,int b ){
        System.out.println("---injectHeader---"+(a+b));
    }

    /**
     * test
     */
    public static void addMap(Map map){
        if(map == null){
            map = new HashMap();
        }
        map.put("a","b");
        System.out.println("a----b");
    }
}
