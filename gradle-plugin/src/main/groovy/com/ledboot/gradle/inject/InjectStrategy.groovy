package com.ledboot.gradle.inject

import javassist.ClassPool
import javassist.CtClass;

/**
 * Created by ouyangxingyu198 on 17/4/6.
 */

public interface InjectStrategy {
    /**
     * 向指定的class文件中嵌码
     * @param classPool
     * @param ctClass
     */
    void inject(ClassPool classPool, CtClass ctClass);
}
