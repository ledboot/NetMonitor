package com.ledboot.gradle.inject

import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod

/**
 * Created by ouyangxingyu198 on 17/4/6.
 */
class TestInjectStrategy implements InjectStrategy {
    @Override
    void inject(ClassPool classPool, CtClass ctClass) {
        //获取注解信息
        println(TestInjectStrategy.class.name + "-----inject-------")
        CtMethod method = ctClass.getDeclaredMethod("addHead")
        method.insertAfter(
                "com.ledboot.core.interceptor.INetInterceptor okHttpInterceptor = new com.ledboot.core.interceptor.OkHttpInterceptor();\n" +
                "okHttpInterceptor.interceptorRequestHead(\$1);")
    }
}
