package org.kitodo.production.plugin.step.archive;

import java.io.File;
import org.apache.commons.configuration.ConfigurationException;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;

public class ArchiveConfigurationTest {

    private ArchiveConfiguration config;

    @Before
    public void init() throws ConfigurationException {
        ClassLoader classLoader = getClass().getClassLoader();
        String configFilePath = classLoader.getResource("kitodo_archive_plugin_test.xml").toString();
        config = new ArchiveConfiguration(configFilePath);
    }

    @Test
    public void manifestValues() {
        assertEquals("TU Berlin", config.manifest.get("SubmittingOrganization"));
        assertEquals("123", config.manifest.get("OrganizationIdentifier"));
        assertEquals("321", config.manifest.get("ContractNumber"));
        assertEquals("Me, Myself", config.manifest.get("Contact"));
        assertEquals("me@example.com", config.manifest.get("ContactEmail"));
        assertEquals("You, Yourself", config.manifest.get("TransferCurator"));
        assertEquals("you@example.com", config.manifest.get("TransferCuratorEmail"));
        assertEquals("rights", config.manifest.get("AccessRights"));
        assertEquals("license", config.manifest.get("License"));
        assertEquals("rights description", config.manifest.get("RightsDescription"));
        assertEquals("metadata.xml", config.manifest.get("MetadataFile"));
        assertEquals("http://www.loc.gov/standards/mets/version112/mets.xsd", config.manifest.get("MetadataFileFormat"));
    }

    @Test
    public void values() {
        assertEquals("/srv/kitodo/production/archive", config.exportBaseDir);
        assertEquals("/srv/kitodo/production/archive", config.exportBaseDir);
        assertEquals("ORIGINAL", config.masterFileGroup);
        assertEquals("FULLTEXT", config.fulltextFileGroup);
        assertArrayEquals(new String[]{"scp","-r","{source}","server:/path"}, config.runAfterCreated);
        assertEquals(new File("/srv/kitodo/production/logs/archive-transfer.log"), config.runLogFile);
    }

    @Test
    public void exportConfigs() {
        assertNotNull(config.exportConfigs);
        assertEquals(3, config.exportConfigs.size());
        assertNotNull(config.exportConfigs.get("master"));
        assertNotNull(config.exportConfigs.get("ocrAlto"));
        assertNotNull(config.exportConfigs.get("ocrTxt"));
        assertNull(config.exportConfigs.get("master").sourceDirPattern);
        assertEquals("regex:ocr/\\w+_alto$", config.exportConfigs.get("ocrAlto").sourceDirPattern);
        assertEquals("regex:ocr/\\w+_txt$", config.exportConfigs.get("ocrTxt").sourceDirPattern);
        assertNull( config.exportConfigs.get("master").extension);
        assertEquals("xml", config.exportConfigs.get("ocrAlto").extension);
        assertEquals("txt", config.exportConfigs.get("ocrTxt").extension);
    }
}
