package com.ledboot.gradle.inject

import javassist.ClassPool
import javassist.CtClass
import javassist.CtConstructor
import javassist.CtMethod
import javassist.bytecode.AnnotationsAttribute
import javassist.bytecode.ClassFile
import javassist.bytecode.ConstPool
import javassist.bytecode.annotation.Annotation

/**
 * Created by ouyangxingyu198 on 17/4/6.
 */

public class OkHttp3InjectStrategy implements InjectStrategy {
    /**
     * 自定义monitor Annotation
     */
    private def monitorAnnotation = "com.ledboot.annotation.MonitorTag"

    @Override
    void inject(ClassPool classPool, CtClass ctClass) {
        def className = ctClass.getClassFile().name
        if (className.equals("okhttp3.Request\$Builder")) {
            //给类增加注解
            ClassFile cf = ctClass.getClassFile()
            ConstPool cp = cf.getConstPool()
            AnnotationsAttribute attr = new AnnotationsAttribute(cp, AnnotationsAttribute.visibleTag)
            Annotation annotation = new Annotation(cp, classPool.getCtClass(monitorAnnotation))
            attr.setAnnotation(annotation)
            cf.addAttribute(attr)
            cf.setVersionToJava5()
            //方法注入Interceptor
            CtMethod method = ctClass.getDeclaredMethod("headers")
            method.insertAfter(
                    "com.ledboot.interceptor.INetInterceptor okHttpInterceptor = new com.ledboot.interceptor.OkHttpInterceptor();\n" +
                    "okHttpInterceptor.interceptorRequestHead(this.headers);")

            CtConstructor constructor = ctClass.getDeclaredConstructor(null);
            constructor.insertAfter("com.ledboot.interceptor.INetInterceptor okHttpInterceptor = new com.ledboot.interceptor.OkHttpInterceptor();\n" +
                    "okHttpInterceptor.interceptorRequestHead(this.headers);")
        }
    }
}
