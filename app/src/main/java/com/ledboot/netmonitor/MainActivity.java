package com.ledboot.netmonitor;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.ledboot.NetMonitor;

import java.util.Set;

import okhttp3.Headers;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Test test = new Test();
        Headers.Builder builder = new Headers.Builder();
        builder.add("a","a");
        builder.add("b","b");
        NetMonitor.init(this);
        NetMonitor.put("1234","asdgasdgasdg");
        test.addHead(builder);
        Headers headers = builder.build();
        Set<String> names = headers.names();
        for (String name: names) {
            System.out.println("name:"+name+" values: "+headers.get(name));
        }
        test.add(1,2);
    }
}
