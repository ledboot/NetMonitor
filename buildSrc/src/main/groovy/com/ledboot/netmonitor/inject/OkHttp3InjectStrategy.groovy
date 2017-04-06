package com.ledboot.netmonitor.inject

import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import javassist.bytecode.AnnotationsAttribute
import javassist.bytecode.ClassFile
import javassist.bytecode.ConstPool
import javassist.bytecode.annotation.Annotation;

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
        if(className.equals("okhttp3.Request\$Builder")) {
            Object o = ctClass.getAnnotation(classPool.getCtClass(monitorAnnotation).toClass())
            if (o == null) {
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
                method.insertBefore("com.ledboot.interceptor.HttpInterceptor.injectHeader(\$1);")
            }
        }
    }
}
