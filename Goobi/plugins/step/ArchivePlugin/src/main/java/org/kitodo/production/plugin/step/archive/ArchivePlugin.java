package org.kitodo.production.plugin.step.archive;

import de.sub.goobi.beans.Prozesseigenschaft;
import de.sub.goobi.beans.Schritt;
import de.sub.goobi.config.ConfigMain;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.persistence.SchrittDAO;
import de.sub.goobi.persistence.apache.StepObject;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.goobi.production.constants.Parameters;
import org.goobi.production.enums.PluginGuiType;
import org.goobi.production.enums.PluginType;
import org.goobi.production.enums.StepReturnValue;
import org.goobi.production.plugin.interfaces.IStepPlugin;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Kitodo step plugin to create a package from a process to be used for long-term archiving.
 */
@PluginImplementation
public class ArchivePlugin implements IStepPlugin {

    private static final Logger LOGGER = Logger.getLogger(ArchivePlugin.class);

    /**
     * The plugin name is used to identify this plugin in Kitodo.Production and it is written to the METS file and submission manifest.
     */
    private static final String PLUGIN_NAME = "Kitodo Archive Plugin";

    /**
     * The plugin name is written to the METS file and submission manifest.
     */
    private static final String PLUGIN_VERSION = "1.0.0";

    /**
     * The config filename for the plugin. It is located in the Kitodo.Production config directory.
     */
    private static final String CONFIG_FILENAME = "kitodo_archive_plugin.xml";

    /**
     * The type of master files in the config file (used in: {@code //export/@type}).
     */
    private static final String TYPE_MASTER = "master";

    /**
     * The type of OCR files in the config file (used in: {@code //export/@type}).
     */
    private static final String TYPE_OCR = "ocr";

    /**
     * The plugin configuration.
     */
    private ArchiveConfiguration config;

    /**
     * Configuration of the master files.
     */
    private Map.Entry<String, ArchiveConfiguration.ExportConfig> masterExportConfig;

    /**
     * The current step in he process.
     */
    private Schritt step;

    private String returnPath;

    /**
     * Construct and load configuration.
     *
     * @throws ConfigurationException
     */
    public ArchivePlugin() throws ConfigurationException {
        String configDir = ConfigMain.getParameter(Parameters.CONFIG_DIR);
        config = new ArchiveConfiguration(FilenameUtils.concat(configDir, CONFIG_FILENAME));
    }

    @Override
    public void initialize(Schritt step, String returnPath) {
        LOGGER.debug("initialize(Schritt, String) called");
        this.step = step;
        this.returnPath = returnPath;
    }

    @Override
    public void initialize(StepObject stepobject, String returnPath) {
        LOGGER.debug("initialize(StepObject, String) called");
        this.returnPath = returnPath;

        // Create Schritt from StepObject
        SchrittDAO schrittDAO = new SchrittDAO();
        try {
            this.step = schrittDAO.get(stepobject.getId());
        } catch (DAOException e) {
            Helper.setFehlerMeldung("Could not get Schritt with id '" + stepobject.getId() + "': " + e);
        }
    }

    @Override
    public boolean execute() {

        LOGGER.debug("execute() called");

        String processTitle = step.getProzess().getTitel();

        // Get export config for the master files
        try {
            masterExportConfig = config.exportConfigs.entrySet().stream()
                .filter(configEntry -> TYPE_MASTER.equals(configEntry.getValue().type))
                .findAny()
                .orElseThrow(RuntimeException::new);
        } catch (Exception e) {
            Helper.setFehlerMeldung("No master export config found for process title '" + processTitle + "'.");
            return false;
        }

        // Get master files directory
        File masterDir;
        try {
            masterDir = new File(step.getProzess().getImagesOrigDirectory(false));
        } catch (Exception e) {
            Helper.setFehlerMeldung("Could not get master files for process title '" + processTitle + "': " + e);
            return false;
        }

        // Get master image file paths
        File[] masterFiles = masterDir.listFiles(Helper.dataFilter);
        if (masterFiles == null) {
            masterFiles = new File[0];
        }

        // Create temp file for METS file export.
        Path tempMetsPath;
        try {
            tempMetsPath = Files.createTempFile("mets_" + processTitle, ".xml");
        } catch (IOException ex) {
            Helper.setFehlerMeldung("Could not create temporary file for process title '" + processTitle + "': " + ex);
            return false;
        }

        // Export METS file using Kitodo
        try {
            ArchiveMetsExporter metsExporter = new ArchiveMetsExporter();
            metsExporter.export(step.getProzess(), tempMetsPath.toString());
        } catch (Exception ex) {
            Helper.setFehlerMeldung("Could not export METS file to temporary file for process title '" + processTitle + "': " + ex);
            return false;
        }

        // Read workId from METS file
        MetsEditor metsEditor;
        String workId;
        try {
            metsEditor = new MetsEditor(tempMetsPath.toFile());
            workId = metsEditor.getWorkId();
        } catch (Exception e) {
            Helper.setFehlerMeldung("Could not read work ID from METS '" + tempMetsPath + "' for process title '" + processTitle + "': " + e);
            return false;
        }

        // The final METS file will saved to this file
        File metsFile = Paths.get(config.exportBaseDir, workId, "metadata.xml").toFile().getAbsoluteFile();
        LOGGER.debug("ExportMetsPath = " + metsFile);

        // Check if the process' project has a fulltext filegroup. If so, the OCR files (<export type="ocr">) are required.
        Map<String, ExportInfo> sourceFileinfos;
        try {
            boolean hasOcr = step.getProzess().getProjekt().getFilegroups().stream()
                .anyMatch(projectFileGroup -> config.fulltextFileGroup.equals(projectFileGroup.getName()));
            sourceFileinfos = populateFileInfos(masterFiles, new File(step.getProzess().getProcessDataDirectory()), workId, hasOcr);
        } catch (Exception e) {
            Helper.setFehlerMeldung("Could not populate Fileinfos for process title '" + processTitle + "': " + e);
            return false;
        }

        // Export master files
        for (ExportInfo exportInfo : sourceFileinfos.values()) {
            try {
                File exportDir = exportInfo.exportDir;
                exportDir.mkdirs();
                exportInfo.fileInfos.forEach(new FileinfoExporter(exportInfo.sourceDir, exportDir));
            } catch (Exception e) {
                Helper.setFehlerMeldung("Could not export process files for process title '" + processTitle + "': " + e);
                return false;
            }
        }

        // Edit METS file
        try {
            metsEditor.addCreator(PLUGIN_NAME + " " + PLUGIN_VERSION, "ARCHIVIST");
            metsEditor.removeAllFileGroupsBut(config.masterFileGroup);
            metsEditor.renameFileGroup(config.masterFileGroup, masterExportConfig.getKey());
            for (Map.Entry<String, ExportInfo> sourceFileinfo : sourceFileinfos.entrySet()) {
                metsEditor.setFileGroup(sourceFileinfo.getKey(), sourceFileinfo.getValue().fileInfos);
            }
            metsEditor.repairKnownIssues();
            metsEditor.save(metsFile);

            List<String> messages = metsEditor.validate(metsFile);
            if (!messages.isEmpty()) {
                throw new Exception("METS validation failed: " + String.join("; ", messages));
            }

        } catch (Exception e) {
            Helper.setFehlerMeldung("Could not modify METS file for process title '" + processTitle + "': " + e);
            return false;
        }

        // Write submission manifest
        try {
            config.manifest.put("SubmissionName", workId);
            config.manifest.put("DataSourceSystem", PLUGIN_NAME + " " + PLUGIN_VERSION);

            // Set "SubmissionDescription" by using a process property
            String archiveDescription = step.getProzess().getEigenschaften().stream()
                .filter(prozesseigenschaft -> config.descriptionProperty.equals(prozesseigenschaft.getTitel()))
                .map(Prozesseigenschaft::getWert)
                .findFirst()
                .orElseThrow(() -> new Exception("Could not find process property '" + config.descriptionProperty + "' for SubmissionDescription."));
            config.manifest.put("SubmissionDescription", archiveDescription);

            // Set license by "accessCondition"
            String accessCondition = metsEditor.value("//mods:accessCondition/@xlink:href");
            switch(accessCondition) {
                case "http://rightsstatements.org/vocab/InC/1.0/":
                    config.manifest.put("License", "N/A");
                    config.manifest.put("RightsDescription", accessCondition);
                    break;
                case "https://creativecommons.org/publicdomain/mark/1.0/":
                case "https://creativecommons.org/publicdomain/zero/1.0/deed.de":
                    config.manifest.put("License", accessCondition);
                    config.manifest.put("RightsDescription", "");
                    break;
            }

            writeSubmissionManifest(new File(config.exportBaseDir, workId));
        } catch (Exception e) {
            Helper.setFehlerMeldung("Could not save submission manifest for process title '" + processTitle + "': " + e);
            return false;
        }

        // Run command after package was created
        if (!(config.runAfterCreated.length == 1 && StringUtils.isBlank(config.runAfterCreated[0]))) {
            try {
                String source = new File(config.exportBaseDir, workId).toString();
                for (int i = 0; i < config.runAfterCreated.length; i++) {
                    if (config.runAfterCreated[i].contains("{source}")) {
                        config.runAfterCreated[i] = config.runAfterCreated[i].replace("{source}", source);
                    }
                }
                runCommand(config.runAfterCreated, config.runLogFile);
            } catch(Exception e) {
                Helper.setFehlerMeldung("Could not transfer archive package for process title '" + processTitle + "': " + e);
                return false;
            }
        }

        return true;
    }

    /**
     * Create a map of all file sets to be exported.
     *
     * @param masterFiles a list of master files
     * @param processDir the process dir to be exported
     * @param workId the work ID of the work
     * @param expectOcr should the process have OCR files
     * @return a map of [key=export_name>, value={@link ExportInfo}]
     * @throws Exception
     */
    private Map<String, ExportInfo> populateFileInfos(File[] masterFiles, File processDir, String workId, boolean expectOcr) throws Exception {

        Map<String, ExportInfo> sourceFileinfos = new HashMap<>();

        if (masterFiles == null || masterFiles.length == 0) {
            return sourceFileinfos;
        }

        // Populate ExportInfo for master files
        String extension = FilenameUtils.getExtension(masterFiles[0].getName());
        ExportInfo masterExportInfo = createExportInfo(masterFiles, masterFiles[0].getParentFile(), workId, masterExportConfig.getKey(), extension);
        sourceFileinfos.put(masterExportConfig.getKey(), masterExportInfo);

        // Populate ExportInfo for everything else
        for (Map.Entry<String, ArchiveConfiguration.ExportConfig> export : config.exportConfigs.entrySet()) {
            if (masterExportConfig == export) {
                continue;
            }

            // Find a source directory matching the pattern
            PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher(export.getValue().sourceDirPattern);
            Optional<Path> sourceDir = Files.walk(processDir.toPath())
                .filter(Files::isDirectory)
                .filter(path -> pathMatcher.matches(processDir.toPath().relativize(path)))
                .findAny();

            // The OCR directory is not required if the project does not have a FULLTEXT file group
            boolean isRequired = !TYPE_OCR.equals(export.getValue().type) || expectOcr;
            if (!sourceDir.isPresent() && isRequired) {
                throw new Exception("Source directory with pattern '" + export.getValue().sourceDirPattern + "' not found.");
            }

            // Create exportInfo for this directory
            sourceDir.ifPresent(path -> {
                ExportInfo exportInfo = createExportInfo(masterFiles, path.toFile(), workId, export.getKey(), export.getValue().extension);
                sourceFileinfos.put(export.getKey(), exportInfo);
            });
        }

        return sourceFileinfos;
    }

    /**
     * Create a single ExportInfo.
     *
     * <p>
     *     Based on the masterFiles parameter it creates {@link FileInfo}s for the given sourceDir and name.
     *
     * @param masterFiles list of master files the returned file set will match to
     * @param sourceDir the source directory of the files returned
     * @param workId the work ID of the current process
     * @param name the unique name of this set of files
     * @param extension the extension to be appended on all files. the basename is taken from masterFiles.
     * @return {@link ExportInfo} of one set of files.
     */
    private ExportInfo createExportInfo(File[] masterFiles, File sourceDir, String workId, String name, String extension) {
        ExportInfo masterExportInfo = new ExportInfo();
        masterExportInfo.sourceDir = sourceDir;
        masterExportInfo.exportDir = Paths.get(config.exportBaseDir, workId, name).toFile();
        masterExportInfo.fileInfos = Arrays.stream(masterFiles)
            .map(file -> {
                String filename = FilenameUtils.getBaseName(file.getName()) + "." + extension;
                return new FileInfo(new File(filename));
            })
            .collect(Collectors.toList());
        return masterExportInfo;
    }

    /**
     * Write a submission-manifest.txt to the export directory.
     *
     * <p>
     *     It takes the parameters in <manifest> from config file and adds dynamic values to some of the fields.
     *
     * @param dir the directory to store the file in
     * @throws IOException
     */
    private void writeSubmissionManifest(File dir) throws IOException {

        String[] fields = new String[] {
            "SubmittingOrganization",
            "OrganizationIdentifier",
            "ContractNumber",
            "Contact",
            "ContactEmail",
            "TransferCurator",
            "TransferCuratorEmail",
            "SubmissionName",
            "SubmissionDescription",
            "AccessRights",
            "License",
            "RightsDescription",
            "DataSourceSystem",
            "MetadataFile",
            "MetadataFileFormat"
        };

        HashMap<String, Object> manifest = new LinkedHashMap<>();

        manifest.put("SubmissionManifestVersion", 1.3);

        for (String field : fields) {
            String value = config.manifest.getOrDefault(field, "");
            manifest.put(field, value);
        }

        StringWriter writer = new StringWriter();

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        yaml.dump(manifest, writer);

        Path file = Paths.get(dir.getPath(), "submission-manifest.txt");
        Files.write(file, writer.toString().getBytes());
    }

    private void runCommand(String[] command, File logFile) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));
        Process process = processBuilder.start();
        process.waitFor();
        int errorCode = process.exitValue();
        if (errorCode != 0) {
            throw new RuntimeException("Process returned code " + errorCode);
        }
    }

    @Override
    public String cancel() {
        LOGGER.debug("cancel() called");
        return returnPath;
    }

    @Override
    public String finish() {
        LOGGER.debug("finish() called");
        return returnPath;
    }

    @Override
    public HashMap<String, StepReturnValue> validate() {
        LOGGER.debug("validate() called");
        return null;
    }

    @Override
    public Schritt getStep() {
        LOGGER.debug("getStep() called");
        return null;
    }

    @Override
    public PluginGuiType getPluginGuiType() {
        // looks like this is used nowhere
        return PluginGuiType.NONE;
    }

    @Override
    public PluginType getType() {
        return PluginType.Step;
    }

    @Override
    public String getTitle() {
        return PLUGIN_NAME;
    }

    @Override
    public String getDescription() {
        return "Export work in an archive format.";
    }
}
