package com.ledboot.gradle

import org.gradle.api.Project

/**
 * Created by Gwynn on 17/4/5.
 */
public class MonitorVariant {

    final Project project
    final def androidVariant
    final String variantName
    final String manifestPath

    MonitorVariant(Project project, Object androidVariant) {
        this.project = project
        this.androidVariant = androidVariant
        this.variantName = androidVariant.name.capitalize()
        this.manifestPath = androidVariant.outputs.first().processManifest.manifestOutputFile
    }
}
