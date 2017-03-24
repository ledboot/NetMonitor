package com.ledboot.netmonitor

import groovy.json.JsonSlurper
import org.gradle.api.Project


class Utils {

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

    public static String getBuildBackupDir(String buildDirPath) {
        def buildBackupDir = new File(getBuildCacheDir(buildDirPath), "monitor-backup")
        if (!buildBackupDir.exists() || !buildBackupDir.isDirectory()) {
            buildBackupDir.mkdirs()
        }
        return buildBackupDir.absolutePath
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

}