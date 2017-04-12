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
        CtMethod method = ctClass.getDeclaredMethod("addHead")
        method.insertAfter(
                "com.ledboot.interceptor.INetInterceptor okHttpInterceptor = new com.ledboot.interceptor.OkHttpInterceptor();\n" +
                "okHttpInterceptor.interceptorRequestHead(\$1);")
    }
}
