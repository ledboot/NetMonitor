package com.ledboot.core.interceptor;

import com.ledboot.core.NetMonitor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Created by ouyangxingyu198 on 17/4/10.
 */

public class OkHttpInterceptor implements INetInterceptor{


    @Override
    public void interceptorRequestHead(Object obj) {
        try {
            Method method = obj.getClass().getDeclaredMethod("add",new Class[]{String.class,String.class});
            if(method != null){
                for(Map.Entry entry : NetMonitor.cacheMap.entrySet()){
                    method.invoke(obj,new Object[]{entry.getKey(),entry.getValue()});
                }
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
}
