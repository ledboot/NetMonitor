package com.ledboot.netmonitor

import groovy.io.FileType
import org.apache.commons.io.FileUtils
import org.gradle.api.Plugin
import org.gradle.api.Project

public class NetMonitorPlugin implements Plugin<Project> {

    void apply(Project project) {
        System.out.println("========================");
        System.out.println("这是第二个插件!");
        System.out.println("========================");
        project.afterEvaluate {
            println "project.afterEvaluate"
            project.android.applicationVariants.each { variant ->

                def forceLowerVersion = false
                def isLowerVersion = false
                if (!forceLowerVersion) {
                    project.rootProject.buildscript.configurations.classpath.resolvedConfiguration.firstLevelModuleDependencies.each {
                        if (it.moduleGroup == "com.android.tools.build" && it.moduleName == "gradle") {
                            println("it.moduleVersion :" + it.moduleVersion)
                            if (!it.moduleVersion.startsWith("1.5") && !it.moduleVersion.startsWith("2")) {
                                isLowerVersion = true
                                return false
                            }
                        }
                    }
                } else {
                    isLowerVersion = true
                }

                def classesProcessTask
                def preDexTask
                def multiDexListTask
                boolean multiDexEnabled = variant.apkVariantData.variantConfiguration.isMultiDexEnabled()
                if (isLowerVersion) {
                    println("isLowerVersion  yes")
                    if (multiDexEnabled) {
                        classesProcessTask = project.tasks.findByName("packageAll${variant.name.capitalize()}ClassesForMultiDex")
                        multiDexListTask = project.tasks.findByName("create${variant.name.capitalize()}MainDexClassList")
                    } else {
                        classesProcessTask = project.tasks.findByName("dex${variant.name.capitalize()}")
                        preDexTask = project.tasks.findByName("preDex${variant.name.capitalize()}")
                    }
                } else {
                    println("isLowerVersion   no")
                    String manifest_path = project.android.sourceSets.main.manifest.srcFile.path
                    if (getMinSdkVersion(variant.mergedFlavor, manifest_path) < 21 && multiDexEnabled) {
                        classesProcessTask = project.tasks.findByName("transformClassesWithJarMergingFor${variant.name.capitalize()}")
                        multiDexListTask = project.tasks.findByName("transformClassesWithMultidexlistFor${variant.name.capitalize()}")
                    } else {
                        classesProcessTask = project.tasks.findByName("transformClassesWithDexFor${variant.name.capitalize()}")
                    }
                }

                println("classesProcessTask :" + classesProcessTask)
                println("multiDexListTask :" + multiDexListTask)
                if (classesProcessTask == null) {
                    return
                }
                String backUpDirPath = Utils.getBuildBackupDir(project.buildDir.absolutePath)
                def modules = [:]
                project.rootProject.allprojects.each { pro ->
                    //modules.add("exploded-aar" + File.separator + pro.group + File.separator + pro.name + File.separator)
                    modules[pro.name] = "exploded-aar" + File.separator + pro.group + File.separator + pro.name + File.separator
                }
                def backupMap = [:]
                def httpProcessor = "httpProcessorDex${variant.name.capitalize()}"
                def excludeHackClasses = []
                project.task(httpProcessor) << {
                    def jarDependencies = []
                    classesProcessTask.inputs.files.files.each { f ->
                        if (f.isDirectory()) {
                            f.eachFileRecurse(FileType.FILES) { file ->
                                backUpClass(backupMap, file as File, backUpDirPath as String, modules.values())
                                MonitorInjector.inject(excludeHackClasses, file as File, modules.values())
                                if (file.path.endsWith(".jar")) {
                                    jarDependencies.add(file.path)
                                }
                            }
                        } else {
                            backUpClass(backupMap, f as File, backUpDirPath as String, modules.values())
                            MonitorInjector.inject(excludeHackClasses, f as File, modules.values())
                            if (f.path.endsWith(".jar")) {
                                jarDependencies.add(f.path)
                            }
                        }
                    }
                }
                if (classesProcessTask) {
                    println("--------x")
                    def httpProcessorTask = project.tasks[httpProcessor]
                    if (httpProcessorTask == null) {
                        println("httpProcessorTask is null")
                    } else {
                        println("httpProcessorTask not null")
                    }
                    httpProcessorTask.dependsOn classesProcessTask.taskDependencies.getDependencies(classesProcessTask)
                    classesProcessTask.dependsOn httpProcessorTask
                }
            }
        }
    }

    private static void backUpClass(def backupMap, File file, String backUpDirPath, def modules) {
        String path = file.absolutePath
        if (!Utils.isEmpty(path)) {
            if (path.endsWith(".class") || (path.endsWith(".jar") && MonitorInjector.checkInjection(file, modules as Collection))) {
                File target = new File(backUpDirPath, "${file.name}-${System.currentTimeMillis()}")
                FileUtils.copyFile(file, target)
                backupMap[file.absolutePath] = target.absolutePath
                println "back up ${file.absolutePath} to ${target.absolutePath}"
            }
        }
    }

    private static int getMinSdkVersion(def mergedFlavor, String manifestPath) {
        if (mergedFlavor.minSdkVersion != null) {
            return mergedFlavor.minSdkVersion.apiLevel
        } else {
            return Parser.getMinSdkVersion(manifestPath)
        }
    }
}