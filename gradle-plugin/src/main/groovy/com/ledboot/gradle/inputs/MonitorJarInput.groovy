package com.ledboot.gradle.inputs

import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Status

/**
 * Created by ouyangxingyu198 on 17/4/1.
 */
class MonitorJarInput implements JarInput {

    private JarInput base
    private def jarPath

    public MonitorJarInput(JarInput jarInput, def jarPath) {
        base = jarInput
        this.jarPath = jarPath
    }

    @Override
    Status getStatus() {
        return base.status
    }

    @Override
    String getName() {
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
