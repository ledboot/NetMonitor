package com.ledboot.gradle.transfrom

import com.android.SdkConstants
import com.android.build.api.transform.*
import com.android.build.gradle.internal.transforms.JarMerger
import com.android.builder.packaging.ZipEntryFilter
import com.android.utils.FileUtils
import com.google.common.io.Files
import com.ledboot.gradle.MonitorVariant
import com.ledboot.gradle.inject.InjectController
import com.ledboot.gradle.util.Utils
import groovy.io.FileType
import javassist.ClassPool
import org.gradle.api.Project

/**
 * Created by ouyangxingyu198 on 17/4/1.
 */
public class MonitorDexTransform extends TransformProxy {

    private Project project
    private MonitorVariant monitorVariant
    public static final String COMBINED_JAR = "combined.jar"


    public static String COMBINED_JAR_PATH = null

    MonitorDexTransform(Transform base, MonitorVariant monitorVariant) {
        super(base)
        this.project = monitorVariant.project
        this.monitorVariant = monitorVariant
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, IOException, InterruptedException {

        Collection<TransformInput> inputs = transformInvocation.inputs
        Collection<TransformInput> referencedInputs = transformInvocation.referencedInputs
        TransformOutputProvider outputProvider = transformInvocation.outputProvider

        File outJarDir = Utils.getOutputFile(monitorVariant, "jar")
        File outClassDir = Utils.getOutputFile(monitorVariant, "class")
//        def outPath = project.rootDir.absolutePath + File.separator + "output"
//        println("combinePath : ---->" + outPath)

        //重新编译class文件
        def inputClassNames = getClassNames(inputs)
//        def referencedClassNames = Utils.getClassNames(referencedInputs)
//        def allClassNames = merge(inputClassNames, referencedClassNames)
        // Create and populate the Javassist class pool
        ClassPool classPool = Utils.createClassPool(inputs, referencedInputs)
        // Append android.jar to class pool. We don't need the class names of them but only the class in the pool for
        // javassist. See https://github.com/realm/realm-java/issues/2703.
        Utils.addBootClassesToClassPool(project, classPool)
        inputClassNames.each {
            def ctClass = classPool.getCtClass(it)
            //注入拦截信息
            InjectController.inject(it,classPool,ctClass)
            ctClass.writeFile(outClassDir.canonicalPath)
        }

        //合并class文件
        File combinedFile = new File(outJarDir, COMBINED_JAR)
        COMBINED_JAR_PATH = combinedFile.absolutePath
        JarMerger jarMerger = new JarMerger(combinedFile)
        jarMerger.setFilter(new ZipEntryFilter() {
            @Override
            public boolean checkEntry(String archivePath) {
                return archivePath.endsWith(SdkConstants.DOT_CLASS)
            }
        })
        jarMerger.addFolder(outClassDir)
        jarMerger.close()

        //调用基类的transform
        base.transform(new MonitorTransformInvocation(transformInvocation))

        FileUtils.cleanOutputDir(outClassDir)
        FileUtils.cleanOutputDir(outJarDir)
    }


    private copyResourceFiles(Collection<TransformInput> inputs, TransformOutputProvider outputProvider) {
        inputs.each {
            it.directoryInputs.each {
                def dirPath = it.file.absolutePath
                it.file.eachFileRecurse(FileType.FILES) {
                    if (!it.absolutePath.endsWith(SdkConstants.DOT_CLASS)) {
                        def dest = new File(getOutputFile(outputProvider), it.absolutePath.substring(dirPath.length()))
                        dest.parentFile.mkdirs()
                        Files.copy(it, dest)
                    }
                }
            }
            // no need to implement the code for `it.jarInputs.each` since PROJECT SCOPE does not use jar input.
        }
    }


    private static Set<String> merge(Set<String> set1, Set<String> set2) {
        Set<String> merged = new HashSet<String>()
        merged.addAll(set1)
        merged.addAll(set2)
        return merged;
    }


}
