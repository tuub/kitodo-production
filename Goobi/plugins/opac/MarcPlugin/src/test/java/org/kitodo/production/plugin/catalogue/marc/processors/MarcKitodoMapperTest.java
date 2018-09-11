package org.kitodo.production.plugin.catalogue.marc.processors;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.kitodo.production.plugin.catalogue.marc.config.MarcOpacConfig;
import org.marc4j.MarcReader;
import org.marc4j.MarcXmlReader;
import org.marc4j.marc.Record;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for the marc-kitodo metadata mapper.
 */
public class MarcKitodoMapperTest {

    private IMarcMetadataMapper marcKitodoMapper;
    private MarcOpacConfig marcOpacConfig;

    @Before
    public void init() {
        ClassLoader classLoader = getClass().getClassLoader();
        String configFilePath = classLoader.getResource("kitodo_marc_opac_test.xml").toString();
        marcOpacConfig = new MarcOpacConfig(configFilePath);

        marcKitodoMapper = new MarcKitodoMetadataMapper();
    }

    @Test
    public void testB3katMapping() {
        Record record = readMarcRecord("b3kat_marc_test.xml");
        assertNotNull(record);

        Map<String, List<Map<String, String>>> mappings = marcKitodoMapper.map(
                record,
                marcOpacConfig.getMappings(null),
                marcOpacConfig.getAuthorities());

        assertNotNull(mappings);
        assertEquals("There should be 10 metadata fields", 10, mappings.size());
        assertNotNull("There should be an author field", mappings.get("Author"));
        assertEquals("There should be two authors", 2, mappings.get("Author").size());
        assertEquals("The first author is 'Bulgakov, Michail'", "Bulgakov, Michail", mappings.get("Author").get(0).get("value"));
        assertEquals("The second author is 'Ball, Hugo'", "Ball, Hugo", mappings.get("Author").get(1).get("value"));
        assertEquals("The author 'Bulgakov, Michail' has GND", "118517376", mappings.get("Author").get(0).get("authValue"));
        assertNull("The author 'Ball, Hugo' has no GND", mappings.get("Author").get(1).get("authority"));
        assertNotNull("There should be a publication year field", mappings.get("PublicationYear"));
        assertEquals("There should be two publication years", 2, mappings.get("PublicationYear").size());
        assertEquals("The first publication year should be 1968", "1968", mappings.get("PublicationYear").get(0).get("value"));
        assertEquals("The second publication year should be 1969/1970", "1969/1970", mappings.get("PublicationYear").get(1).get("value"));
        assertNotNull("There should be a title field", mappings.get("TitleDocMain"));
        assertEquals("There should be one title", 1, mappings.get("TitleDocMain").size());
        assertEquals("The title should be 'Der Meister und Margarita'",
                "Der Meister und Margarita", mappings.get("TitleDocMain").get(0).get("value"));
        assertNotNull("There should be an sorting title field", mappings.get("TitleDocMainShort"));
        assertEquals("There should be one sorting title", 1, mappings.get("TitleDocMainShort").size());
        assertEquals("The sorting title should be 'Meister und Margarita'",
                "Meister und Margarita", mappings.get("TitleDocMainShort").get(0).get("value"));
    }

    @Test
    public void testAlmaMapping() {
        Record record = readMarcRecord("alma_marc_test.xml");
        assertNotNull(record);

        Map<String, List<Map<String, String>>> mappings = marcKitodoMapper.map(
                record,
                marcOpacConfig.getMappings(null),
                marcOpacConfig.getAuthorities());

        assertNotNull(mappings);
        assertNotNull("There should be an author field", mappings.get("Author"));
        assertEquals("There should be one author", 1, mappings.get("Author").size());
        assertEquals("The authors is 'Bulgakov, Michail'", "Bulgakov, Michail", mappings.get("Author").get(0).get("value"));
        assertNotNull("There should be a publication year field", mappings.get("PublicationYear"));
        assertEquals("There should be one publication year", 1, mappings.get("PublicationYear").size());
        assertEquals("The publication year should be 1968", "1968", mappings.get("PublicationYear").get(0).get("value"));
        assertNotNull("There should be a publisher name field", mappings.get("PublisherName"));
        assertEquals("There should be one publisher ", 1, mappings.get("PublisherName").size());
        assertEquals("The publisher name should be 'Dt. Buchgemeinschaft'", "Dt. Buchgemeinschaft", mappings.get("PublisherName").get(0).get("value"));
        assertNotNull("There should be a title field", mappings.get("TitleDocMain"));
        assertEquals("There should be one title", 1, mappings.get("TitleDocMain").size());
        assertEquals("The title should be 'Der Meister und Margarita'",
                "Der Meister und Margarita", mappings.get("TitleDocMain").get(0).get("value"));
        assertNotNull("There should be an sorting title field", mappings.get("TitleDocMainShort"));
        assertEquals("There should be one sorting title", 1, mappings.get("TitleDocMainShort").size());
        assertEquals("The sorting title should be 'Meister und Margarita'",
                "Meister und Margarita", mappings.get("TitleDocMainShort").get(0).get("value"));
        assertNotNull("There should be a subtitle field", mappings.get("TitleDocSub1"));
        assertEquals("There should be one subtitle", 1, mappings.get("TitleDocSub1").size());
        assertEquals("The subtitle should be 'Roman'",
                "Roman", mappings.get("TitleDocSub1").get(0).get("value"));

    }

    @Test
    public void testAuthMapping() {
        Record record = readMarcRecord("authorities_marc_test.xml");
        assertNotNull(record);

        Map<String, List<Map<String, String>>> mappings = marcKitodoMapper.map(
                record,
                marcOpacConfig.getMappings(null),
                marcOpacConfig.getAuthorities());

        assertNotNull(mappings);
        assertNotNull("There should be Keyword fields", mappings.get("Keyword"));
        assertEquals("There should be 4 Keyword fields", 4, mappings.get("Keyword").size());
        assertEquals("The first Keyword should have a certain value", "Bismarck, Otto von", mappings.get("Keyword").get(0).get("value"));
        assertEquals("The first Keyword should have a certain authValue", "11851136X", mappings.get("Keyword").get(0).get("authValue"));
        assertEquals("The second Keyword should have a certain value", "Geschichte 1871-1898", mappings.get("Keyword").get(1).get("value"));
        assertNull("The second Keyword should have no authValue", mappings.get("Keyword").get(1).get("authValue"));

        assertNotNull("There should be Classification fields", mappings.get("Classification"));
        assertEquals("There should be one Classification field", 1, mappings.get("Classification").size());
        assertEquals("There value of the Classification should be 'NP 2790'", "NP 2790", mappings.get("Classification").get(0).get("value"));
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
