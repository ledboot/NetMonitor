package com.ledboot.netmonitor

import com.android.build.api.transform.Transform
import com.android.build.gradle.internal.pipeline.TransformTask
import com.android.build.gradle.internal.transforms.DexTransform
import com.ledboot.netmonitor.transfrom.MonitorDexTransform
import org.apache.commons.io.FileUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.execution.TaskExecutionGraphListener

import java.lang.reflect.Field

public class NetMonitorPlugin implements Plugin<Project> {

    void apply(Project project) {
        System.out.println("========================");
        System.out.println("这是第二个插件!");
        System.out.println("========================");
        //删除缓存的文件夹
        def outPath = project.rootDir.absolutePath + File.separator + "output"
        println("combinePath : ---->" + outPath)
        deleteDir(new File(outPath))
        project.afterEvaluate {
            println "project.afterEvaluate"
            project.android.applicationVariants.each { variant ->
                println("variant : ---->" + variant.name.capitalize())
                //监听所有的任务
                project.getGradle().getTaskGraph().addTaskExecutionGraphListener(new TaskExecutionGraphListener() {
                    @Override
                    public void graphPopulated(TaskExecutionGraph taskGraph) {
                        for (Task task : taskGraph.getAllTasks()) {
                            if (task.getProject().equals(project)
                                    && task instanceof TransformTask
                                    && task.name.toLowerCase().contains(variant.name.toLowerCase())) {
                                Transform transform = ((TransformTask) task).getTransform()
                                println("task : ---->" + transform.name)
                                //如果开启了multiDexEnabled true,存在transformClassesWithJarMergingFor${variantName}任务
//                                if ((((transform instanceof JarMergingTransform)) && !(transform instanceof MonitorTransformer))) {
//                                    println("==fastdex find jarmerging transform. transform class: " + task.transform.getClass() + " . task name: " + task.name)
//                                    MonitorTransformer jarMergingTransform = new MonitorTransformer(transform,project)
//                                    Field field = getFieldByName(task.getClass(),'transform')
//                                    field.setAccessible(true)
//                                    field.set(task,jarMergingTransform)
//                                }

                                if ((((transform instanceof DexTransform)) && !(transform instanceof MonitorDexTransform))) {
                                    project.logger.error("==fastdex find dex transform. transform class: " + task.transform.getClass() + " . task name: " + task.name)
                                    //代理DexTransform,实现自定义的转换
                                    MonitorDexTransform fastdexTransform = new MonitorDexTransform(transform,project)
                                    Field field = getFieldByName(task.getClass(),'transform')
                                    field.setAccessible(true)
                                    field.set(task,fastdexTransform)
                                }
                            }
                        }
                    }
                });
            }


        }
        //project.android.registerTransform(new MonitorTransformer(project))
//        project.afterEvaluate {
//            println "project.afterEvaluate"
//            project.android.applicationVariants.each { variant ->
//
//                def forceLowerVersion = false
//                def isLowerVersion = false
//                if (!forceLowerVersion) {
//                    project.rootProject.buildscript.configurations.classpath.resolvedConfiguration.firstLevelModuleDependencies.each {
//                        if (it.moduleGroup == "com.android.tools.build" && it.moduleName == "gradle") {
//                            println("it.moduleVersion :" + it.moduleVersion)
//                            if (!it.moduleVersion.startsWith("1.5") && !it.moduleVersion.startsWith("2")) {
//                                isLowerVersion = true
//                                return false
//                            }
//                        }
//                    }
//                } else {
//                    isLowerVersion = true
//                }
//
//                def classesProcessTask
//                def preDexTask
//                def multiDexListTask
//                boolean multiDexEnabled = variant.apkVariantData.variantConfiguration.isMultiDexEnabled()
//                if (isLowerVersion) {
//                    println("isLowerVersion  yes")
//                    if (multiDexEnabled) {
//                        classesProcessTask = project.tasks.findByName("packageAll${variant.name.capitalize()}ClassesForMultiDex")
//                        multiDexListTask = project.tasks.findByName("create${variant.name.capitalize()}MainDexClassList")
//                    } else {
//                        classesProcessTask = project.tasks.findByName("dex${variant.name.capitalize()}")
//                        preDexTask = project.tasks.findByName("preDex${variant.name.capitalize()}")
//                    }
//                } else {
//                    println("isLowerVersion   no")
//                    String manifest_path = project.android.sourceSets.main.manifest.srcFile.path
//                    if (getMinSdkVersion(variant.mergedFlavor, manifest_path) < 21 && multiDexEnabled) {
//                        classesProcessTask = project.tasks.findByName("transformClassesWithJarMergingFor${variant.name.capitalize()}")
//                        multiDexListTask = project.tasks.findByName("transformClassesWithMultidexlistFor${variant.name.capitalize()}")
//                    } else {
//                        classesProcessTask = project.tasks.findByName("transformClassesWithDexFor${variant.name.capitalize()}")
//                    }
//                }
//
////                println("classesProcessTask :" + classesProcessTask)
////                println("multiDexListTask :" + multiDexListTask)
//                if (classesProcessTask == null) {
//                    return
//                }
//
//                File backUpDir = Utils.getBuildBackupDir(project.buildDir.absolutePath)
//                String backUpDirPath = backUpDir.absolutePath
//                def modules = [:]
//                project.rootProject.allprojects.each { pro ->
//                    //modules.add("exploded-aar" + File.separator + pro.group + File.separator + pro.name + File.separator)
//                    modules[pro.name] = "exploded-aar" + File.separator + pro.group + File.separator + pro.name + File.separator
//                }
//                def backupMap = [:]
//                def httpProcessor = "httpProcessorDex${variant.name.capitalize()}"
//                def excludeHackClasses = []
//                //每次执行，先清除之前的备份
//                FileUtils.deleteDirectory(backUpDir)
//                project.task(httpProcessor) << {
//                    def jarDependencies = []
//                    classesProcessTask.inputs.files.each { f ->
//                        if (f.isDirectory()) {
//                            f.eachFileRecurse(FileType.FILES) { file ->
//                                def oneFile = file as File
//                                def path = oneFile.absolutePath
//                                if (MonitorInjector.isExcluded(path)) {
//                                    backUpClass(backupMap, oneFile, backUpDirPath as String, modules.values())
//                                    MonitorInjector.inject(excludeHackClasses, file as File, modules.values())
//                                    if (file.path.endsWith(".jar")) {
//                                        jarDependencies.add(file.path)
//                                    }
//                                }
//                            }
//                        } else {
//                            def file =f as File
//                            def path = file.absolutePath
//                            if (MonitorInjector.isExcluded(path)) {
//                                backUpClass(backupMap, file, backUpDirPath as String, modules.values())
//                                MonitorInjector.inject(excludeHackClasses, f as File, modules.values())
//                                if (f.path.endsWith(".jar")) {
//                                    jarDependencies.add(f.path)
//                                }
//                            }
//                        }
//                    }
//                }
//                if (classesProcessTask) {
//                    def httpProcessorTask = project.tasks[httpProcessor]
//                    httpProcessorTask.dependsOn classesProcessTask.taskDependencies.getDependencies(classesProcessTask)
//                    classesProcessTask.dependsOn httpProcessorTask
//                }
//            }
//        }
    }

    Field getFieldByName(Class<?> aClass, String name) {
        Class<?> currentClass = aClass;
        while (currentClass != null) {
            try {
                return currentClass.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                // ignored.
            }
            currentClass = currentClass.getSuperclass();
        }
        return null;
    }

    private static void backUpClass(def backupMap, File file, String backUpDirPath, def modules) {
        String path = file.absolutePath
        if (!Utils.isEmpty(path)) {
            if (MonitorInjector.checkInjection(file, modules as Collection)) {
                File target = new File(backUpDirPath, "${file.name}")
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

    /**
     * 删除文件夹
     * @param dirFile
     */
    private void deleteDir(File dirFile) {
        println("deleteDir path: ---->" + dirFile.absolutePath)
        if (dirFile.exists()) {
            File[] files = dirFile.listFiles();
            if (files != null && files.size() > 0) {
                for (File f : files) {
                    deleteDir(f)
                }
                dirFile.delete();
            } else {
                dirFile.delete();
            }
        }
    }
}