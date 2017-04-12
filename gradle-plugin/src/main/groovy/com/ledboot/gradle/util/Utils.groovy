package com.ledboot.gradle.util

import com.android.SdkConstants
import com.android.build.api.transform.TransformInput
import com.google.common.base.Joiner
import com.ledboot.gradle.MonitorVariant
import groovy.io.FileType
import groovy.json.JsonSlurper
import javassist.ClassPool
import org.gradle.api.Project

import java.util.jar.JarFile


public class Utils {

    public static final String MONITOR_INTERMEDIATES = "build/intermediates/monitor_intermediates/"

    private static Set<String> INJECT_LIST = new HashSet<String>() {
        {
            add("okhttp");
        }
    };

    private static final Joiner PATH_JOINER = Joiner.on(File.separatorChar)


    public static boolean isAllowClassInject(String className) {
        boolean flag
        for (String str : INJECT_LIST) {
            if (className.startsWith(str)) {
                flag = true
                break
            }
        }
        return flag
    }

    public static boolean isAllowJarInject(String jarFileName) {
        boolean flag
        for (String str : INJECT_LIST) {
            if (jarFileName.contains(str)) {
                flag = true
                break
            }
        }
        return flag
    }

    public static String getRelativePath(File root, File target) {
        String path = target.absolutePath.replace(root.absolutePath, "")
        while (path.startsWith("/") || (path.startsWith("\\"))) {
            path = path.substring(1)
        }
        return path
    }

    public static def getProperty(Project project, String property) {
        if (project.hasProperty(property)) {
            return project.getProperties()[property];
        }
        return null;
    }

    public static String getFreelineCacheDir(String rootDirPath) {
        return rootDirPath
    }

    public static String getBuildAssetsDir(String buildDirPath) {
        def buildAssetsDir = new File(getBuildCacheDir(buildDirPath), "monitor-assets")
        if (!buildAssetsDir.exists() || !buildAssetsDir.isDirectory()) {
            buildAssetsDir.mkdirs()
        }
        return buildAssetsDir.absolutePath
    }

    public static String getBuildCacheDir(String buildDirPath) {
        def buildCacheDir = new File(buildDirPath, "hookhttp")
        if (!buildCacheDir.exists() || !buildCacheDir.isDirectory()) {
            buildCacheDir.mkdirs()
        }
        return buildCacheDir.absolutePath
    }

    public static File getBuildBackupDir(String buildDirPath) {
        def buildBackupDir = new File(getBuildCacheDir(buildDirPath), "monitor-backup")
        if (!buildBackupDir.exists() || !buildBackupDir.isDirectory()) {
            buildBackupDir.mkdirs()
        }
        return buildBackupDir
    }

    public static String getAndroidGradlePluginVersion(Project project) {
        return getGradlePluginVersion(project, "com.android.tools.build", "gradle")
    }

    public static String getMonitorGradlePluginVersion(Project project) {
        return getGradlePluginVersion(project, "com.ledboot.netmonitor", "gradle")
    }

    public
    static String getGradlePluginVersion(Project project, String moduleGroup, String moduleName) {
        String version = "0.0.0"
        project.rootProject.buildscript.configurations.classpath.resolvedConfiguration.firstLevelModuleDependencies.each {
            if (it.moduleGroup == moduleGroup && it.moduleName == moduleName) {
                version = it.moduleVersion
                return false
            }
        }
        return version
    }

    public static def getJson(String url) {
        return new JsonSlurper().parseText(new URL(url).text)
    }

    public static boolean saveJson(String json, String fileName, boolean override) {
        def pending = new File(fileName)
        if (pending.exists() && pending.isFile()) {
            if (override) {
                println "Old file $pending.absolutePath removed."
                pending.delete()
            } else {
                println "File $pending.absolutePath exists."
                return false
            }
        }

        pending << json
        println "Save to $pending.absolutePath"
        return true
    }


    public static String joinPath(String... sep) {
        if (sep.length == 0) {
            return "";
        }
        if (sep.length == 1) {
            return sep[0];
        }

        return new File(sep[0], joinPath(Arrays.copyOfRange(sep, 1, sep.length))).getPath();
    }

    public static String getOsName() {
        return System.getProperty("os.name");
    }

    public static boolean isWindows() {
        return getOsName().startsWith("Windows");
    }

    public static boolean isEmpty(String text) {
        return text == null || text == '' || text.trim() == ''
    }


    public static Set<String> getClassNames(Collection<TransformInput> inputs) {
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
    public
    static ClassPool createClassPool(Collection<TransformInput> inputs, Collection<TransformInput> referencedInputs) {
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

    // There is no official way to get the path to android.jar for transform.
    // See https://code.google.com/p/android/issues/detail?id=209426
    public static void addBootClassesToClassPool(Project project, ClassPool classPool) {
        try {
            project.android.bootClasspath.each {
                String path = it.absolutePath
                classPool.appendClassPath(path)
            }
        } catch (Exception e) {
            // Just log it. It might not impact the transforming if the method which needs to be transformer doesn't
            // contain classes from android.jar.
        }
    }

    public static File getOutputFile(MonitorVariant monitorVariant, String name) {

        File file = new File(PATH_JOINER.join(monitorVariant.project.projectDir, MONITOR_INTERMEDIATES, monitorVariant.variantName, name))
        if (!file.exists()) {
            file.mkdirs()
        }
        return file

    }
}