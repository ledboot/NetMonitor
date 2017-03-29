package com.ledboot.netmonitor;

import java.util.Set;

import okhttp3.Headers;


/**
 * Created by ouyangxingyu198 on 17/3/28.
 */

public class Test {

    public void addHead(Headers.Builder headerBuilder){
        Headers headers = headerBuilder.build();
        Set<String> names = headers.names();
        for (String name: names) {
            System.out.println("name:"+name+" values: "+headers.get(name));
        }
    }

    public void add(int a,int b){
        System.out.println("param1--->"+a);
        System.out.println("param2--->"+b);
    }

}