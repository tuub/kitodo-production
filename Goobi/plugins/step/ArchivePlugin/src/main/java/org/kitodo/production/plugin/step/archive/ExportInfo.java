package org.kitodo.production.plugin.step.archive;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple data holder for a set of files to be exported.
 */
public class ExportInfo {

    /**
     * Source directory of the files.
     */
    public File sourceDir;

    /**
     * Destination directory.
     */
    public File exportDir;

    /**
     * File set
     */
    public List<FileInfo> fileInfos = new ArrayList<>();
}
