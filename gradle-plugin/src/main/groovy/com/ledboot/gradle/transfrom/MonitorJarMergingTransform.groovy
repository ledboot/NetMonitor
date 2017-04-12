package com.ledboot.gradle.transfrom

import com.android.SdkConstants
import com.android.build.api.transform.*
import com.android.build.gradle.internal.transforms.JarMerger
import com.android.builder.packaging.ZipEntryFilter
import com.android.utils.FileUtils
import com.ledboot.gradle.MonitorVariant
import com.ledboot.gradle.inject.InjectController
import com.ledboot.gradle.util.Utils
import javassist.ClassPool
import org.gradle.api.Project

/**
 * Created by Gwynn on 17/4/12.
 */
public class MonitorJarMergingTransform extends TransformProxy {

    private Project project
    private MonitorVariant monitorVariant

    MonitorJarMergingTransform(Transform base, MonitorVariant monitorVariant) {
        super(base)
        this.project = monitorVariant.project
        this.monitorVariant = monitorVariant
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        TransformOutputProvider outputProvider = transformInvocation.getOutputProvider()
        Collection<TransformInput> inputs = transformInvocation.getInputs()
        Collection<TransformInput> referencedInputs = transformInvocation.getReferencedInputs()

        // all the output will be the same since the transform type is COMBINED.
        // and format is SINGLE_JAR so output is a jar
        File jarFile = outputProvider.getContentLocation("combined", getOutputTypes(), getScopes(), Format.JAR)

        File outClassDir = Utils.getOutputFile(monitorVariant, "class")

        FileUtils.mkdirs(jarFile.getParentFile())
        FileUtils.deleteIfExists(jarFile)


        def inputClassNames = Utils.getClassNames(inputs)

        ClassPool classPool = Utils.createClassPool(inputs, referencedInputs)
        Utils.addBootClassesToClassPool(project, classPool)

        inputClassNames.each {
            def ctClass = classPool.getCtClass(it)
            if (Utils.isAllowClassInject(ctClass.getClassFile().name)) {
                //注入拦截信息
                InjectController.inject(it, classPool, ctClass)
                ctClass.writeFile(outClassDir.canonicalPath)
            }
        }


        JarMerger jarMerger = new JarMerger(jarFile)
        jarMerger.setFilter(new ZipEntryFilter() {
            @Override
            public boolean checkEntry(String archivePath) {
                return archivePath.endsWith(SdkConstants.DOT_CLASS)
            }
        })

        for (TransformInput input : inputs) {
            for (JarInput jarInput : input.getJarInputs()) {
                System.out.println("jarInput.getName()=" + jarInput.getFile().getAbsolutePath())
                if (!Utils.isAllowJarInject(jarInput.getFile().getAbsolutePath())) {
                    jarMerger.addJar(jarInput.getFile())
                }
            }

            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                System.out.println("input.getDirectoryInputs()=" + directoryInput.getFile())
                jarMerger.addFolder(directoryInput.getFile())
            }
        }

        jarMerger.addFolder(outClassDir)
        jarMerger.close()

        FileUtils.cleanOutputDir(outClassDir)
    }
}
