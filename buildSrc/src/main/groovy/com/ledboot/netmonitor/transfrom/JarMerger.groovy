package com.ledboot.netmonitor.transfrom

import com.android.annotations.NonNull
import com.android.annotations.Nullable
import com.android.build.gradle.tasks.annotations.TypedefRemover
import com.android.builder.packaging.ZipEntryFilter
import com.android.builder.packaging.ZipAbortException
import com.android.utils.FileUtils
import com.google.common.io.Closer

import java.nio.file.attribute.FileTime
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Jar Merger class.
 */
public class JarMerger {

    private final byte[] buffer = new byte[8192]

    private static final FileTime ZERO_TIME = FileTime.fromMillis(0)

    @NonNull
    private final File jarFile
    private Closer closer
    private JarOutputStream jarOutputStream

    private ZipEntryFilter filter
    private TypedefRemover typedefRemover

    public JarMerger(@NonNull File jarFile) throws IOException {
        this.jarFile = jarFile
    }

    public void setTypedefRemover(@Nullable TypedefRemover typedefRemover) {
        this.typedefRemover = typedefRemover
    }

    private void init() throws IOException {
        if (closer == null) {
            FileUtils.mkdirs(jarFile.getParentFile())

            closer = Closer.create()

            FileOutputStream fos = closer.register(new FileOutputStream(jarFile))
            BufferedOutputStream bos = closer.register(new BufferedOutputStream(fos))
            jarOutputStream = closer.register(new JarOutputStream(bos))
        }
    }

    /**
     * Sets a list of regex to exclude from the jar.
     */
    void setFilter(@NonNull ZipEntryFilter filter) {
        this.filter = filter
    }

     void addFolder(@NonNull File folder) throws IOException {
        addFolder(folder, true)
    }

     void addFolder(@NonNull File folder, boolean removeEntryTimestamp) throws IOException {
        init()
        try {
            addFolder(folder, "", removeEntryTimestamp)
        } catch (ZipAbortException e) {
            throw new IOException(e)
        }
    }

    void addFolder(@NonNull File folder, @NonNull String path, boolean removeEntryTimestamp)
            throws IOException, ZipAbortException {
        println("addFolder folder path")
        File[] files = folder.listFiles()
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String entryPath = path + file.getName()
                    if (filter == null || filter.checkEntry(entryPath)) {
                        if (typedefRemover != null && typedefRemover.isRemoved(entryPath)) {
                            continue
                        }

                        // new entry
                        final JarEntry jarEntry = new JarEntry(entryPath)
                        if (removeEntryTimestamp) {
                            jarEntry.setLastModifiedTime(ZERO_TIME)
                            jarEntry.setLastAccessTime(ZERO_TIME)
                            jarEntry.setCreationTime(ZERO_TIME)
                        }
                        jarOutputStream.putNextEntry(jarEntry)

                        // put the file content
                        Closer localCloser = Closer.create()
                        InputStream fis = localCloser.register(new FileInputStream(file))
                        if (typedefRemover != null) {
                            fis = typedefRemover.filter(entryPath, fis)
                            assert fis != null // because we checked isRemoved above
                        }

                        int count
                        while ((count = fis.read(buffer)) != -1) {
                            jarOutputStream.write(buffer, 0, count)
                        }

                        // close the entry
                        jarOutputStream.closeEntry()
                    }
                } else if (file.isDirectory()) {
                    addFolder(file, path + file.getName() + "/", removeEntryTimestamp)
                }
            }
        }
    }

    public void addJar(@NonNull File file) throws IOException {
        addJar(file, false)
    }

    public void addJar(@NonNull File file, boolean removeEntryTimestamp) throws IOException {
        init()

        Closer localCloser = Closer.create()
        FileInputStream fis = localCloser.register(new FileInputStream(file))
        ZipInputStream zis = localCloser.register(new ZipInputStream(fis))

        // loop on the entries of the jar file package and put them in the final jar
        ZipEntry entry
        while ((entry = zis.getNextEntry()) != null) {
            // do not take directories or anything inside a potential META-INF folder.
            if (entry.isDirectory()) {
                continue
            }

            String name = entry.getName()
            if (filter != null && !filter.checkEntry(name)) {
                continue
            }

            JarEntry newEntry

            // Preserve the STORED method of the input entry.
            if (entry.getMethod() == JarEntry.STORED) {
                newEntry = new JarEntry(entry)
            } else {
                // Create a new entry so that the compressed len is recomputed.
                newEntry = new JarEntry(name)
            }
            if (removeEntryTimestamp) {
                newEntry.setLastModifiedTime(ZERO_TIME)
                newEntry.setLastAccessTime(ZERO_TIME)
                newEntry.setCreationTime(ZERO_TIME)
            }

            // add the entry to the jar archive
            jarOutputStream.putNextEntry(newEntry)

            // read the content of the entry from the input stream, and write it into the archive.
            int count
            while ((count = zis.read(buffer)) != -1) {
                jarOutputStream.write(buffer, 0, count)
            }

            // close the entries for this file
            jarOutputStream.closeEntry()
            zis.closeEntry()
        }

    }

    public void addEntry(@NonNull String path, @NonNull byte[] bytes) throws IOException {
        init()

        jarOutputStream.putNextEntry(new JarEntry(path))
        jarOutputStream.write(bytes)
        jarOutputStream.closeEntry()
    }

    public void close() throws IOException {
        if (closer != null) {
            closer.close()
        }
    }
}