package com.ledboot.netmonitor.transfrom

import com.android.SdkConstants
import com.android.build.api.transform.Context
import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.SecondaryInput
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.android.build.api.transform.TransformOutputProvider
import com.android.builder.packaging.ZipEntryFilter
import com.google.common.io.Files
import com.ledboot.netmonitor.TransformProxy
import com.ledboot.netmonitor.inputs.MonitorJar
import groovy.io.FileType
import javassist.ClassPool
import javassist.CtMethod
import org.gradle.api.Project
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

/**
 * Created by ouyangxingyu198 on 17/4/1.
 */
public class MonitorDexTransform extends TransformProxy {

    Project project

    MonitorDexTransform(Transform base, Project project) {
        super(base)
        this.project = project;
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, IOException, InterruptedException {

        Collection<TransformInput> inputs = transformInvocation.inputs
        Collection<TransformInput> referencedInputs = transformInvocation.referencedInputs
        TransformOutputProvider outputProvider = transformInvocation.outputProvider;

        def outPath = project.rootDir.absolutePath + File.separator + "output"
        println("combinePath : ---->" + outPath)

        //重新编译class文件
        def inputClassNames = getClassNames(inputs)
        def referencedClassNames = getClassNames(referencedInputs)
        def allClassNames = merge(inputClassNames, referencedClassNames);
        // Create and populate the Javassist class pool
        ClassPool classPool = createClassPool(inputs, referencedInputs)
        // Append android.jar to class pool. We don't need the class names of them but only the class in the pool for
        // javassist. See https://github.com/realm/realm-java/issues/2703.
        addBootClassesToClassPool(classPool)
        inputClassNames.each {
            def ctClass = classPool.getCtClass(it)
            if (it.equals("com.ledboot.netmonitor.Test")) {
                CtMethod method = ctClass.getDeclaredMethod("addHead");
                method.insertAfter("com.ledboot.interceptor.HttpInterceptor.injectHeader(\$1);")
            } else if (it.equals("okhttp3.Request\$Builder")) {
                CtMethod method = ctClass.getDeclaredMethod("headers");
                method.insertAfter("com.ledboot.interceptor.HttpInterceptor.injectHeader(\$1);")
            }
            ctClass.writeFile(getOutputFile(outputProvider).canonicalPath)
        }

        //合并class文件
        JarMerger jarMerger = new JarMerger(new File(outPath + File.separator + "combined.jar"))
        jarMerger .setFilter(new ZipEntryFilter() {
            @Override
            public boolean checkEntry(String archivePath) {
                return archivePath.endsWith(SdkConstants.DOT_CLASS)
            }
        })
        jarMerger.addFolder(new File(outPath))
        jarMerger.close()

        //调用基类的transform
        base.transform(new MonitorTransformInvocation(transformInvocation))
    }

    /**
     * 重新打包jar
     * @param packagePath 将这个目录下的所有文件打包成jar
     * @param destPath 打包好的jar包的绝对路径
     */
    public static void zipJar(String packagePath, String destPath) {

        File file = new File(packagePath)
        JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(destPath))
        file.eachFileRecurse { File f ->
            String entryName = f.getAbsolutePath().substring(packagePath.length() + 1)
            outputStream.putNextEntry(new ZipEntry(entryName))
            if (!f.directory) {
                InputStream inputStream = new FileInputStream(f)
                outputStream << inputStream
                inputStream.close()
            }
        }
        outputStream.close()
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

    private File getOutputFile(TransformOutputProvider outputProvider) {
//        return outputProvider.getContentLocation(
//                'main1', getInputTypes(), getScopes(), Format.DIRECTORY)

        File file = new File(project.rootDir.absolutePath + File.separator + "output")
        if (!file.exists()) {
            file.mkdirs()
        }
        return file;
//        return outputProvider.getContentLocation(directoryInput.name,
//                directoryInput.contentTypes, directoryInput.scopes,
//                Format.DIRECTORY)
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

    class MonitorTransformInvocation implements TransformInvocation {
        TransformInvocation base;

        MonitorTransformInvocation(TransformInvocation transformInvocation) {
            super()
            base = transformInvocation;
        }

        @Override
        Context getContext() {
            return base.context
        }

        @Override
        Collection<TransformInput> getInputs() {
            Collection<TransformInput> monitorsInput = new HashSet<>();
            base.inputs.each {
                monitorsInput.add(new MonitorTransformInput(it))
            }
            return monitorsInput;
        }

        @Override
        Collection<TransformInput> getReferencedInputs() {
            return base.referencedInputs
        }

        @Override
        Collection<SecondaryInput> getSecondaryInputs() {
            return base.secondaryInputs
        }

        @Override
        TransformOutputProvider getOutputProvider() {
            return base.outputProvider
        }

        @Override
        boolean isIncremental() {
            return base.incremental
        }
    }

    class MonitorTransformInput implements TransformInput {
        TransformInput base;

        MonitorTransformInput(TransformInput input) {
            super()
            base = input
        }

        @Override
        Collection<JarInput> getJarInputs() {
            Collection<JarInput> jarInputs = new HashSet<>()
            base.jarInputs.each {
                println("jarInput filePath : ---> "+it.file.absolutePath)
                if (jarInputs.size() == 0) {
                    def jarPath = project.rootDir.absolutePath + File.separator + "output" + File.separator + "combined.jar";
                    MonitorJar jar = new MonitorJar(it, jarPath)
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
}
