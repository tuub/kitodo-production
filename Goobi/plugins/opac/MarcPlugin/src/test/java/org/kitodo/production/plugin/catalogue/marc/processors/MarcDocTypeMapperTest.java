package org.kitodo.production.plugin.catalogue.marc.processors;

import org.junit.Before;
import org.junit.Test;
import org.kitodo.production.plugin.catalogue.marc.config.MarcOpacConfig;
import org.marc4j.MarcReader;
import org.marc4j.MarcXmlReader;
import org.marc4j.marc.Record;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.util.Map;

/**
 * Unit tests for the document type mapper.
 */
public class MarcDocTypeMapperTest {

    private IMarcDocTypeMapper marcDocTypeMapper;

    private MarcOpacConfig marcOpacConfig;

    @Before
    public void init() {
        ClassLoader classLoader = getClass().getClassLoader();
        String configFilePath = classLoader.getResource("kitodo_marc_opac_test.xml").toString();
        marcOpacConfig = new MarcOpacConfig(configFilePath);
        marcDocTypeMapper = new MarcDocTypeMapper();
    }

    @Test
    public void testMonographMapping() {
        Record record = readMarcRecord("b3kat_marc_test.xml");
        assertNotNull(record);

        Map<String, String> docType = marcDocTypeMapper.mapDocType(record, marcOpacConfig.getDocTypes());

        assertNotNull(docType);
        assertEquals("Monograph", docType.get("rulesetType"));
    }

    @Test
    public void testMultivolumeMapping() {
        Record record = readMarcRecord("multivolume_marc_test.xml");
        assertNotNull(record);

        Map<String, String> docType = marcDocTypeMapper.mapDocType(record, marcOpacConfig.getDocTypes());

        assertNotNull(docType);
        assertEquals("MultiVolumeWork", docType.get("rulesetType"));
    }

    @Test
    public void testPeriodicalMapping() {
        Record record = readMarcRecord("periodical_marc_test.xml");
        assertNotNull(record);

        Map<String, String> docType = marcDocTypeMapper.mapDocType(record, marcOpacConfig.getDocTypes());

        assertNotNull(docType);
        assertEquals("Periodical", docType.get("rulesetType"));
    }

    private Record readMarcRecord(String resourceName) {
        InputStream marcStream = ClassLoader.getSystemResourceAsStream(resourceName);
        MarcReader reader = new MarcXmlReader(marcStream);
        if (reader.hasNext()) {
            return reader.next();
        }
        return null;
    }
}
