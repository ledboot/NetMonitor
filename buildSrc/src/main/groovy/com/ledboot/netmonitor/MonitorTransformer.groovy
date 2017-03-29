package com.ledboot.netmonitor

import com.android.SdkConstants

import com.android.build.api.transform.*
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Sets
import javassist.ClassPool
import org.gradle.api.Project
import groovy.io.FileType
import com.google.common.io.Files

import java.util.jar.JarFile
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils

import static com.android.build.api.transform.QualifiedContent.*

/**
 * Created by ouyangxingyu198 on 17/3/27.
 */

public class MonitorTransformer extends Transform {

    private Project project

    public MonitorTransformer(Project project) {
        this.project = project
        println("MonitorTransformer------")
    }

    @Override
    String getName() {
        return "MonitorTransformer"
    }
    // 指定输入的类型，通过这里的设定，可以指定我们要处理的文件类型
    //这样确保其他类型的文件不会传入
    @Override
    Set<ContentType> getInputTypes() {
        return ImmutableSet.<ContentType> of(DefaultContentType.CLASSES)
    }
    // 指定Transform的作用范围
    @Override
    Set<Scope> getScopes() {
        return Sets.immutableEnumSet(Scope.PROJECT, Scope.EXTERNAL_LIBRARIES, Scope.SUB_PROJECTS)
    }

    @Override
    Set<Scope> getReferencedScopes() {
        return ImmutableSet.<QualifiedContent.Scope> of(QualifiedContent.Scope.PROJECT
                , QualifiedContent.Scope.PROJECT_LOCAL_DEPS
                , QualifiedContent.Scope.EXTERNAL_LIBRARIES
                , QualifiedContent.Scope.SUB_PROJECTS
                , QualifiedContent.Scope.SUB_PROJECTS_LOCAL_DEPS)
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(Context context, Collection<TransformInput> inputs, Collection<TransformInput> referencedInputs, TransformOutputProvider outputProvider, boolean isIncremental) throws IOException, TransformException, InterruptedException {
        println("---------transform---------->")
        inputs.each { input ->
            //对类型为“文件夹”的input进行遍历
            input.directoryInputs.each { DirectoryInput directoryInput ->
                //文件夹里面包含的是我们手写的类以及R.class、BuildConfig.class以及R$XXX.class等+
                HttpInjecter.injectDir(directoryInput.file.absolutePath, "com"+File.separator+"ledboot"+File.separator+"netmonitor")

                println("directoryInput.file.absolutePath : " + directoryInput.file.absolutePath)

                // 获取output目录
                def dest = outputProvider.getContentLocation(directoryInput.name,
                        directoryInput.contentTypes, directoryInput.scopes,
                        Format.DIRECTORY)
                println("class dest path : " + dest)
                // 将input的目录复制到output指定目录
                FileUtils.copyDirectory(directoryInput.file, dest)
            }
            //对类型为jar文件的input进行遍历
             input.jarInputs.each { JarInput jarInput ->
                println("--------jarInputs--------")
                //jar文件一般是第三方依赖库jar文件

                // 重命名输出文件（同目录copyFile会冲突）
                def jarName = jarInput.name
                def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
                if (jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.length() - 4)
                }
                //生成输出路径
                def dest = outputProvider.getContentLocation(jarName + md5Name,
                        jarInput.contentTypes, jarInput.scopes, Format.JAR)
                println("jar dest path : " + dest)
                //将输入内容复制到输出
                FileUtils.copyFile(jarInput.file, dest)
            }
        }
    }

    /*@Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        // Find all the class names
        println("---------transform---------->")
        Collection<TransformInput> inputs = transformInvocation.inputs
        Collection<TransformInput> referencedInputs = transformInvocation.referencedInputs
        TransformOutputProvider outputProvider = transformInvocation.outputProvider;
        transformInvocation.

        def inputClassNames = getClassNames(inputs)

        def referencedClassNames = getClassNames(referencedInputs)
        def allClassNames = merge(inputClassNames, referencedClassNames);

        // Create and populate the Javassist class pool
        ClassPool classPool = createClassPool(inputs, referencedInputs)
        // Append android.jar to class pool. We don't need the class names of them but only the class in the pool for
        // javassist. See https://github.com/realm/realm-java/issues/2703.
        addBootClassesToClassPool(classPool)

//        def dest = outputProvider.getContentLocation(directoryInput.name,
//                directoryInput.contentTypes, directoryInput.scopes,
//                Format.DIRECTORY)
//        def testClass = classPool.get("com.ledboot.netmonitor.Test")
//        println(testClass.)
//        if (testClass.isFrozen()) {
//            testClass.defrost()
//        }
//        CtMethod method = c.getDeclaredMethod("add");
//        method.insertAfter("{com.ledboot.interceptor.HttpInterceptor.injectHeader(\$1)}")
//        c.writeFile(testClass.)
//        c.detach()
        // Use accessors instead of direct field access
        inputClassNames.each {
            def ctClass = classPool.getCtClass(it)
            if (it.equals("com.ledboot.netmonitor.Test")) {
                CtMethod method = ctClass.getDeclaredMethod("add");
                method.insertAfter("{com.ledboot.interceptor.HttpInterceptor.injectHeader(\$1)}")
            }
            ctClass.writeFile(getOutputFile(outputProvider).canonicalPath)
        }
        copyResourceFiles(inputs, outputProvider)

    }*/

    private copyResourceFiles(Collection<TransformInput> inputs, TransformOutputProvider outputProvider) {
        inputs.each {
            it.directoryInputs.each {
                def dirPath = it.file.absolutePath
                it.file.eachFileRecurse(FileType.FILES) {
                    if (!it.absolutePath.endsWith(SdkConstants.DOT_CLASS)) {
                        def dest = new File(getOutputFile(outputProvider),
                                it.absolutePath.substring(dirPath.length()))
                        dest.parentFile.mkdirs()
                        Files.copy(it, dest)
                    }
                }
            }
            // no need to implement the code for `it.jarInputs.each` since PROJECT SCOPE does not use jar input.
        }
    }

    private File getOutputFile(TransformOutputProvider outputProvider) {
        return outputProvider.getContentLocation(
                'monitor', getInputTypes(), getScopes(), Format.DIRECTORY)
//        return outputProvider.getContentLocation(directoryInput.name,
//                directoryInput.contentTypes, directoryInput.scopes,
//                Format.DIRECTORY)
    }

    private static Set<String> getClassNames(Collection<TransformInput> inputs) {
        Set<String> classNames = new HashSet<String>()
        inputs.each {
            it.directoryInputs.each {
                def dirPath = it.file.absolutePath
                it.file.eachFileRecurse(FileType.FILES) {
                    if (it.absolutePath.endsWith(SdkConstants.DOT_CLASS)) {
                        def className =
                                it.absolutePath.substring(
                                        dirPath.length() + 1,
                                        it.absolutePath.length() - SdkConstants.DOT_CLASS.length()
                                ).replace(File.separatorChar, '.' as char)
                        classNames.add(className)
                    }
                }
            }

            it.jarInputs.each {
                def jarFile = new JarFile(it.file)
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
