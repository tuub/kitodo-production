package org.kitodo.production.plugin.step.archive;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

/**
 * Plugin configuration loader and holder.
 */
public class ArchiveConfiguration {

    /**
     * Get all elements under {@code <manifest>} in config file. They are used by {@link ArchivePlugin#writeSubmissionManifest(File)}.
     */
    public Map<String, String> manifest;

    /**
     * Fill submission-manifests "SubmissionDescription" using this process property field.
     */
    public String descriptionProperty;

    /**
     * Base directory to store all export packages to.
     */
    public String exportBaseDir;

    /**
     * Name ({@code @USE}) of the file group in the exported METS file containing all master images.
     */
    public String masterFileGroup;

    /**
     * Name ({@code @USE}) of the file group in the exported METS file containing all OCR files.
     *
     * <p>
     *     This is used to determine if the project has OCR or not.
     */
    public String fulltextFileGroup;

    /**
     * Export configurations of the file sets defined in configuration file by {@code <export>}.
     */
    public Map<String, ExportConfig> exportConfigs;

    /**
     * Copy the archive package to this SCP destination path.
     */
    public String[] runAfterCreated;

    /**
     * The archive packages will be transferred using SCP. This log file stores SCP output.
     */
    public File runLogFile;

    /**
     * Constructor.
     *
     * <p>
     *     Create object and read configuration file.
     *
     * @param filepath Path ot configuration file
     * @throws ConfigurationException
     */
    ArchiveConfiguration(final String filepath) throws ConfigurationException {

        char delimiter = XMLConfiguration.getDefaultListDelimiter();
        XMLConfiguration.setDefaultListDelimiter(',');
        XMLConfiguration xmlConfig = new XMLConfiguration(filepath);

        manifest = new HashMap<>();
        SubnodeConfiguration manifestConfig = xmlConfig.configurationAt("manifest");
        StreamSupport.stream(
            Spliterators.spliteratorUnknownSize((Iterator<String>)manifestConfig.getKeys(), Spliterator.ORDERED), false)
            .forEachOrdered(key -> manifest.put(key, manifestConfig.getString(key)));

        descriptionProperty = xmlConfig.getString("descriptionProperty");
        exportBaseDir = xmlConfig.getString("exportBaseDir");
        masterFileGroup = xmlConfig.getString("masterFileGroup");
        fulltextFileGroup = xmlConfig.getString("fulltextFileGroup");

        exportConfigs = new HashMap<>();
        List<HierarchicalConfiguration> exports = xmlConfig.configurationsAt("export");
        for (HierarchicalConfiguration export : exports) {
            ExportConfig exportConfig = new ExportConfig();
            String name = export.getString("[@name]");
            exportConfig.type = export.getString("[@type]");
            exportConfig.sourceDirPattern = export.getString("sourceDirPattern");
            exportConfig.extension = export.getString("extension");
            exportConfigs.put(name, exportConfig);
        }

        // This should be done using getStringArray(). But it doesn't work because of the changed default delimiter in Kitodo.Production
        runAfterCreated = xmlConfig.getStringArray("runAfterCreated");
        runLogFile = new File(xmlConfig.getString("runLogFile"));

        // reset to Kitodo.Production default delimiter
        XMLConfiguration.setDefaultListDelimiter(delimiter);
    }

    public static class ExportConfig {
        public String type;
        public String sourceDirPattern;
        public String extension;
    }
}
