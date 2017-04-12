package com.ledboot.gradle.transfrom

import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.TransformInput
import com.ledboot.gradle.inputs.MonitorJarInput

public class MonitorTransformInput implements TransformInput {
    private TransformInput base

    MonitorTransformInput(TransformInput input) {
        base = input
    }

    @Override
    Collection<JarInput> getJarInputs() {
        Collection<JarInput> jarInputs = new HashSet<>()
        base.jarInputs.each {
            if (jarInputs.size() == 0) {
                def jarPath = MonitorDexTransform.COMBINED_JAR_PATH
                MonitorJarInput jar = new MonitorJarInput(it, jarPath)
                jarInputs.add(jar)
            }
        }
        return jarInputs
    }

    @Override
    Collection<DirectoryInput> getDirectoryInputs() {
        return new HashSet<DirectoryInput>()
    }
}