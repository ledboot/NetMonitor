package com.ledboot.netmonitor.inject

import javassist.ClassPool;
import javassist.CtClass;

/**
 * Created by ouyangxingyu198 on 17/4/6.
 */

public class InjectController {

    public static void inject(String className, ClassPool classPool,CtClass ctClass){
        if( className != null && ctClass != null ){
            InjectStrategy inject= null;
            //根据传入的className来指定注入机制
            //okhttp3
            if(className.startsWith("okhttp3")){
                println(InjectController.class.simpleName+" : inject okhttp3")
                inject = new OkHttp3InjectStrategy()
            }else if(className.equals("com.ledboot.netmonitor.Test")){
                println(InjectController.class.simpleName+" : inject Test")
                inject = new TestInjectStrategy()
            }
            if(inject != null){
                inject.inject(classPool,ctClass)
            }
        }
    }
}
