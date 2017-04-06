package com.ledboot.netmonitor.inject

import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import javassist.bytecode.AnnotationsAttribute
import javassist.bytecode.ClassFile
import javassist.bytecode.ConstPool
import javassist.bytecode.annotation.Annotation

/**
 * Created by ouyangxingyu198 on 17/4/6.
 */
class TestInjectStrategy implements InjectStrategy {
    @Override
    void inject(ClassPool classPool, CtClass ctClass) {
        //获取注解信息
        println(TestInjectStrategy.class.name + "-----inject-------")
        CtMethod method = ctClass.getDeclaredMethod("addHead")
        method.insertAfter("com.ledboot.interceptor.HttpInterceptor.injectHeader(\$1);")
    }
}
