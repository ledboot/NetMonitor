package com.ledboot.netmonitor;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import okhttp3.Headers;

import static org.junit.Assert.assertEquals;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
        Headers.Builder headerBuilder = new Headers.Builder();
        resetHeader(headerBuilder);
        System.out.println(headerBuilder.get("key1"));
    }

    /**
     * 重置头部
     * @param o
     */
    private void resetHeader(Object o){
        try {
            Method method = o.getClass().getDeclaredMethod("add",new Class[]{String.class,String.class});
            if(method != null){
                method.invoke(o, new Object[]{"key1", "123456"});
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