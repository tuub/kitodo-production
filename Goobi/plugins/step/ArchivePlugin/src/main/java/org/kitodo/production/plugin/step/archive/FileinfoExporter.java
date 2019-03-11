package org.kitodo.production.plugin.step.archive;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.function.Consumer;
import javax.xml.bind.DatatypeConverter;
import org.apache.commons.io.IOUtils;

/**
 * Copy file from {@link FileInfo} to exportDir, calculate checksum and save it to FileInfo.
 */
public class FileinfoExporter implements Consumer<FileInfo> {

    /**
     * Directory where the source files are in.
     */
    private File sourceDir;

    /**
     * Directory to copy the files to.
     */
    private File exportDir;

    /**
     * The hash algorithm to be used to calculate checksums.
     */
    private HashType hashType;

    public File getSourceDir() {
        return sourceDir;
    }

    public void setSourceDir(File sourceDir) {
        this.sourceDir = sourceDir;
    }

    public File getExportDir() {
        return exportDir;
    }

    public void setExportDir(File exportDir) {
        this.exportDir = exportDir;
    }

    public HashType getHashType() {
        return hashType;
    }

    public void setHashType(HashType hashType) {
        this.hashType = hashType;
    }

    /**
     * Constructor with default hashType SHA256.
     *
     * @param sourceDir Directory where the source files are in.
     * @param exportDir Directory to copy the files to.
     */
    FileinfoExporter(File sourceDir, File exportDir) {
        this(sourceDir, exportDir, HashType.SHA256);
    }

    /**
     * Constructor with explicit hashType parameter.
     *
     * @param sourceDir Directory where the source files are in.
     * @param exportDir Directory to copy the files to.
     * @param hashType The hash algorithm to be used to calculate checksums.
     */
    FileinfoExporter(File sourceDir, File exportDir, HashType hashType) {
        this.sourceDir = sourceDir;
        this.exportDir = exportDir;
        this.hashType = hashType;
    }

    @Override
    public void accept(FileInfo fileinfo) {
        File sourceFile = new File(sourceDir.getPath(), fileinfo.file.getName());
        try (InputStream inputStream = new FileInputStream(sourceFile)) {
            MessageDigest digest = MessageDigest.getInstance(hashType.getName());
            DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest);
            File outputFile = new File(exportDir.getPath(), fileinfo.file.getName());
            try (OutputStream outputStream = new FileOutputStream(outputFile)) {
                fileinfo.size = IOUtils.copy(digestInputStream, outputStream);
                fileinfo.checksum = DatatypeConverter.printHexBinary(digest.digest());
                fileinfo.checksumType = hashType.getName();
            } catch (Exception e) {
                // Caught to ensure closing stream.
                throw e;
            }
        } catch (Exception e) {
            throw new RuntimeException("", e);
        }
    }

    /**
     * The hash type to use.
     *
     * <p>
     *     Has to match Java and METS specifications.
     */
    public enum HashType {
        MD5("MD5"),
        SHA1("SHA1"),
        SHA256("SHA-256"),
        SHA512("SHA-512");

        private String name;

        HashType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
