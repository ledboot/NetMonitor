package com.ledboot.netmonitor.transfrom

import com.android.SdkConstants
import com.android.build.api.transform.*
import com.android.build.gradle.internal.transforms.JarMerger
import com.android.builder.packaging.ZipEntryFilter
import com.android.utils.FileUtils
import com.google.common.base.Joiner
import com.google.common.io.Files
import com.ledboot.netmonitor.MonitorVariant
import com.ledboot.netmonitor.inject.InjectController
import groovy.io.FileType
import javassist.ClassPool
import javassist.CtMethod
import javassist.bytecode.AnnotationsAttribute
import javassist.bytecode.ClassFile
import javassist.bytecode.ConstPool
import javassist.bytecode.annotation.Annotation
import org.gradle.api.Project

import java.util.jar.JarFile

/**
 * Created by ouyangxingyu198 on 17/4/1.
 */
public class MonitorDexTransform extends TransformProxy {

    private Project project
    private MonitorVariant monitorVariant
    public static final String MONITOR_INTERMEDIATES = "build/intermediates/monitor_intermediates/"
    public static final String COMBINED_JAR = "combined.jar"

    private static final Joiner PATH_JOINER = Joiner.on(File.separatorChar)

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

        File outJarDir = getOutputFile("jar")
        File outClassDir = getOutputFile("class")
//        def outPath = project.rootDir.absolutePath + File.separator + "output"
//        println("combinePath : ---->" + outPath)

        //重新编译class文件
        def inputClassNames = getClassNames(inputs)
//        def referencedClassNames = getClassNames(referencedInputs)
//        def allClassNames = merge(inputClassNames, referencedClassNames)
        // Create and populate the Javassist class pool
        ClassPool classPool = createClassPool(inputs, referencedInputs)
        // Append android.jar to class pool. We don't need the class names of them but only the class in the pool for
        // javassist. See https://github.com/realm/realm-java/issues/2703.
        addBootClassesToClassPool(classPool)
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

    private File getOutputFile(String name) {

        File file = new File(PATH_JOINER.join(project.projectDir, MONITOR_INTERMEDIATES, monitorVariant.variantName, name))
        if (!file.exists()) {
            file.mkdirs()
        }
        return file

    }

    private static Set<String> getClassNames(Collection<TransformInput> inputs) {
        Set<String> classNames = new HashSet<String>()
        inputs.each {
            it.directoryInputs.each {
                def dirPath = it.file.absolutePath
                println("dirPath:----->" + dirPath)
                it.file.eachFileRecurse(FileType.FILES) {
                    if (it.absolutePath.endsWith(SdkConstants.DOT_CLASS)) {
                        def className =
                                it.absolutePath.substring(
                                        dirPath.length() + 1,
                                        it.absolutePath.length() - SdkConstants.DOT_CLASS.length()
                                ).replace(File.separatorChar, '.' as char)
                        println("class name: ----> " + className)
                        classNames.add(className)
                    }
                }
            }

            it.jarInputs.each {
                def jarFile = new JarFile(it.file)
                println("jarFile:---->" + it.file.absolutePath)
                jarFile.entries().findAll {
                    !it.directory && it.name.endsWith(SdkConstants.DOT_CLASS)
                }.each {
                    def path = it.name
                    // The jar might not using File.separatorChar as the path separator. So we just replace both `\` and
                    // `/`. It depends on how the jar file was created.
                    // See http://stackoverflow.com/questions/13846000/file-separators-of-path-name-of-zipentry
                    String className = path.substring(0, path.length() - SdkConstants.DOT_CLASS.length())
                            .replace('/' as char, '.' as char)
                            .replace('\\' as char, '.' as char)
                    classNames.add(className)
                }
            }
        }
        return classNames
    }

    /**
     * Creates and populates the Javassist class pool.
     *
     * @param inputs the inputs provided by the Transform API
     * @param referencedInputs the referencedInputs provided by the Transform API
     * @return the populated ClassPool instance
     */
    private ClassPool createClassPool(Collection<TransformInput> inputs, Collection<TransformInput> referencedInputs) {
        // Don't use ClassPool.getDefault(). Doing consecutive builds in the same run (e.g. debug+release)
        // will use a cached object and all the classes will be frozen.
        ClassPool classPool = new ClassPool(null)
        classPool.appendSystemPath()

        inputs.each {
            it.directoryInputs.each {
                classPool.appendClassPath(it.file.absolutePath)
            }

            it.jarInputs.each {
                classPool.appendClassPath(it.file.absolutePath)
            }
        }

        referencedInputs.each {
            it.directoryInputs.each {
                classPool.appendClassPath(it.file.absolutePath)
            }

            it.jarInputs.each {
                classPool.appendClassPath(it.file.absolutePath)
            }
        }

        return classPool
    }

    private static Set<String> merge(Set<String> set1, Set<String> set2) {
        Set<String> merged = new HashSet<String>()
        merged.addAll(set1)
        merged.addAll(set2)
        return merged;
    }
    // There is no official way to get the path to android.jar for transform.
    // See https://code.google.com/p/android/issues/detail?id=209426
    private void addBootClassesToClassPool(ClassPool classPool) {
        try {
            project.android.bootClasspath.each {
                String path = it.absolutePath
                println "Add boot class " + path + " to class pool."
                classPool.appendClassPath(path)
            }
        } catch (Exception e) {
            // Just log it. It might not impact the transforming if the method which needs to be transformer doesn't
            // contain classes from android.jar.
            println("Cannot get bootClasspath caused by:", e)
        }
    }

}
