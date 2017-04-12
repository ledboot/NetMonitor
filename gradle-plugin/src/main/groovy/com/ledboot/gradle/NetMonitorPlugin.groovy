package com.ledboot.gradle

import com.android.build.api.transform.Transform
import com.android.build.gradle.internal.pipeline.TransformTask
import com.android.build.gradle.internal.transforms.DexTransform
import com.android.build.gradle.internal.transforms.JarMergingTransform
import com.ledboot.gradle.transfrom.MonitorDexTransform
import com.ledboot.gradle.transfrom.MonitorJarMergingTransform
import com.ledboot.gradle.util.Parser
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.execution.TaskExecutionGraphListener

import java.lang.reflect.Field

public class NetMonitorPlugin implements Plugin<Project> {

    void apply(Project project) {
        project.repositories.add(project.getRepositories().jcenter())
        project.dependencies.add("compile", "com.ledboot.monitor:monitor:${Version.VERSION}")
        project.afterEvaluate {
            project.android.applicationVariants.each { variant ->

                boolean multiDexEnabled = variant.apkVariantData.variantConfiguration.isMultiDexEnabled()

                def variantName = variant.name.capitalize()
                try {
                    //与instant run有冲突需要禁掉instant run
                    def instantRunTask = project.tasks.getByName("transformClassesWithInstantRunFor${variantName}")
                    if (instantRunTask) {
                        throw new GradleException(
                                "NetMonitorPlugin does not support instant run mode, please trigger build"
                                        + " by assemble${variantName} or disable instant run"
                                        + " in 'File->Settings...'."
                        )
                    }
                } catch (UnknownTaskException e) {
                    // Not in instant run mode, continue.
                }
                MonitorVariant monitorVariant = new MonitorVariant(project, variant)
                //监听所有的任务
                project.getGradle().getTaskGraph().addTaskExecutionGraphListener(new TaskExecutionGraphListener() {
                    @Override
                    public void graphPopulated(TaskExecutionGraph taskGraph) {
                        for (Task task : taskGraph.getAllTasks()) {
                            if (task.getProject().equals(project)
                                    && task instanceof TransformTask
                                    && task.name.toLowerCase().contains(variant.name.toLowerCase())) {
                                Transform transform = ((TransformTask) task).getTransform()
                                if (multiDexEnabled) {
                                    if ((((transform instanceof JarMergingTransform)) && !(transform instanceof MonitorJarMergingTransform))) {
                                        MonitorJarMergingTransform jarMergingTransform = new MonitorJarMergingTransform(transform, monitorVariant)
                                        Field field = getFieldByName(task.getClass(), 'transform')
                                        field.setAccessible(true)
                                        field.set(task, jarMergingTransform)
                                    }
                                } else {
                                    if ((((transform instanceof DexTransform)) && !(transform instanceof MonitorDexTransform))) {
                                        //代理DexTransform,实现自定义的转换
                                        MonitorDexTransform monitorDexTransform = new MonitorDexTransform(transform, monitorVariant)
                                        Field field = getFieldByName(task.getClass(), 'transform')
                                        field.setAccessible(true)
                                        field.set(task, monitorDexTransform)
                                    }
                                }


                            }
                        }
                    }
                });
            }


        }
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


    private static int getMinSdkVersion(def mergedFlavor, String manifestPath) {
        if (mergedFlavor.minSdkVersion != null) {
            return mergedFlavor.minSdkVersion.apiLevel
        } else {
            return Parser.getMinSdkVersion(manifestPath)
        }
    }
}