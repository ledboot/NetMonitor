package com.ledboot.netmonitor.inputs

import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Status
import org.gradle.api.Project;

/**
 * Created by ouyangxingyu198 on 17/4/1.
 */
class MonitorJar implements JarInput{

    private JarInput base
    private def jarPath
    public MonitorJar(JarInput jarInput,def jarPath) {
        super()
        base = jarInput;
        this.jarPath = jarPath;
    }

    @Override
    void setProperty(String property, Object newValue) {
        base.setProperty(property, newValue)
    }

    @Override
    void setMetaClass(MetaClass metaClass) {
        base.setMetaClass(metaClass)
    }

    @Override
    Object invokeMethod(String name, Object args) {
        return base.invokeMethod(name, args)
    }

    @Override
    MetaClass getMetaClass() {
        return base.getMetaClass()
    }

    @Override
    Object getProperty(String property) {
        return base.getProperty(property)
    }

    @Override
    Status getStatus() {
        println("JarInput:base.name -->"+base.status)
        return base.status
    }

    @Override
    String getName() {
        println("JarInput:base.name -->"+base.name)
        return base.name
    }

    @Override
    File getFile() {
        return new File(jarPath)
    }

    @Override
    Set<QualifiedContent.ContentType> getContentTypes() {
        return base.contentTypes
    }

    @Override
    Set<QualifiedContent.Scope> getScopes() {
        return base.scopes
    }
}
