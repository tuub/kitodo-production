package org.kitodo.production.plugin.step.archive;

import java.io.File;

/**
 * Simple file info holder.
 */
public class FileInfo {

    /**
     * The file.
     */
    public File file;

    /**
     * Checksum of the file.
     */
    public String checksum;

    /**
     * Checksum type. Has to match a Java and METS specs.
     * @see <a href="https://www.loc.gov/standards/mets/docs/mets.v1-9.html#FLocat">METS reference</a>
     */
    public String checksumType;

    /**
     * File size in bytes.
     */
    public long size;

    /**
     * Constructor.
     *
     * @param file the corresponding file
     */
    FileInfo(File file) {
        this.file = file;
    }
}
