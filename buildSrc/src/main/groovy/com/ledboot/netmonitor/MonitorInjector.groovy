package com.ledboot.netmonitor;

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

class MonitorInjector {

    private static String TAG = MonitorInjector.class.getSimpleName();

    public static void inject(List<String> excludeClasses, File file, Collection<String> modules) {

        println(TAG + "--inject")

        if (file.path.endsWith(".class")
                && !isExcluded(file.path, excludeClasses)) {
            realInject(file)
        } else if (file.path.endsWith(".jar")) {
            println "find jar: ${file.path}"
            if (checkInjection(file, modules)) {
                println "inject jar: ${file.path}"
                realInject(file)
            }
        }
    }

    public static boolean checkInjection(File file, Collection<String> modules) {
        println(TAG + "checkInjection: " + file.absolutePath)
//        return (file.absolutePath.contains("intermediates" + File.separator + "exploded-aar" + File.separator)
//                    && !file.absolutePath.contains("com.antfortune.freeline")
//                    && !file.absolutePath.contains("com.android.support")
//                    && file.absolutePath.contains("com.squareup.okhttp3")
//                    && isProjectModuleJar(file.absolutePath, modules))
//        modules.each {
//            module ->
//                println("module :  " + module)
//        }
        if (file.absolutePath.contains("intermediates" + File.separator + "exploded-aar" + File.separator)) {
            return false
        } else {
            return true
        }
//        return file.absolutePath.contains("com.squareup.okhttp3")
    }

    public static boolean isExcluded(String path, List<String> excludeClasses) {
        for (String exclude : excludeClasses) {
            if (!exclude.endsWith(".class")) {
                exclude = exclude + ".class"
            }
            if (path.endsWith(exclude)) {
                println "exclude class: ${path}"
                return true
            }
        }
        return false
    }

    public static boolean isExcluded(String path) {
        if (path.endsWith(".class") || path.endsWith(".jar")) {
            if (path =~ 'R\\$[a-zA-Z]*\\.class' || path.endsWith("R.class")) {
//                println("isExcluded false ===" + path)
                return false
            } else {
//                println("isExcluded  true ===" + path)
                return true
            }
        }
        return false
    }


    private static boolean isProjectModuleJar(String path, Collection<String> modules) {
        for (String module : modules) {
            if (path.contains(module)) {
                return true
            }
        }
        return false
    }

    private static void realInject(File file) {
        File pending = new File(file.parent, file.name + ".pending")
        try {
            if (file.path.endsWith(".class")) {
                FileInputStream fis = new FileInputStream(file)
                FileOutputStream fos = new FileOutputStream(pending)
                println "inject: ${file.path}"
                byte[] bytes = hackClass(file.path, null, false, fis);
                fos.write(bytes)
                fis.close()
                fos.close()
            } else if (file.path.endsWith(".jar")) {
                def jar = new JarFile(file)
                Enumeration enumeration = jar.entries()
                JarOutputStream jos = new JarOutputStream(new FileOutputStream(pending))
                while (enumeration.hasMoreElements()) {
                    InputStream is
                    try {
                        JarEntry jarEntry = (JarEntry) enumeration.nextElement();
                        String entryName = jarEntry.getName();
                        ZipEntry zipEntry = new ZipEntry(entryName)

                        is = jar.getInputStream(jarEntry)
                        jos.putNextEntry(zipEntry)

                        if (entryName.endsWith(".class")) {
//                            println "inject jar class: ${entryName}"
                            jos.write(hackClass(file.path, entryName, true, is))
                        } else {
//                            println "skip jar entry: ${entryName}"
                            jos.write(readBytes(is))
                        }
                    } catch (Exception e) {
                        println "inject jar with exception: ${e.getMessage()}"
                    } finally {
                        if (is != null) {
                            is.close()
                        }
                        jos.closeEntry()
                    }
                }
                jos.close()
                jar.close()
            }

            if (file.exists()) {
                file.delete()
            }
            def renameSuccess = pending.renameTo(file)
            println("renameSuccess ==" + renameSuccess)
        } catch (Exception e) {
            if (pending.exists()) {
                pending.delete()
            }
            println "inject error: ${file.path},Exception =${e.getMessage()}"
        }
    }

    private
    static byte[] hackClass(String path, String entry, boolean isJar, InputStream inputStream) {
        ClassReader cr = new ClassReader(inputStream)
        ClassWriter cw = new ClassWriter(cr, 0)
        ClassVisitor cv = new MonitorClassVisitor(path, entry, isJar, Opcodes.ASM4, cw)
        cr.accept(cv, 0)
        return cw.toByteArray()
    }

    private static byte[] readBytes(InputStream is) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];

        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();

        return buffer.toByteArray();
    }

}