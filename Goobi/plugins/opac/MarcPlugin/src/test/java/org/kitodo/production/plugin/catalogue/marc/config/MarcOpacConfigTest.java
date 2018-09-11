package org.kitodo.production.plugin.catalogue.marc.config;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * Unit tests for the marc opac config.
 */
public class MarcOpacConfigTest {

    private MarcOpacConfig marcOpacConfig;

    @Before
    public void init() {
        ClassLoader classLoader = getClass().getClassLoader();
        String configFilePath = classLoader.getResource("kitodo_marc_opac_test.xml").toString();
        marcOpacConfig = new MarcOpacConfig(configFilePath);
    }

    @Test
    public void testSupportsCatalogue() {
        List<String> supportedCatalogues = marcOpacConfig.getSupportedCatalogues();

        assertEquals("There should be two catalogues", 2, supportedCatalogues.size());
        assertTrue("The catalogue 'Some Alma API' is there", supportedCatalogues.contains("Some Alma API"));
        assertTrue("The catalogue 'B3Kat OAI-PMH' is there", supportedCatalogues.contains("B3Kat OAI-PMH"));
    }

    @Test
    public void testGetSearchFields() {
        Map<String, String> searchFields = marcOpacConfig.getSearchFields("Some Alma API");

        assertEquals("Catalogue Some Alma API should be two search fields", 2, searchFields.size());
        assertEquals("The Identifier field should have value mms_id", "mms_id", searchFields.get("Identifier"));
        assertEquals("The foo field should have value bar", "bar", searchFields.get("foo"));

        searchFields = marcOpacConfig.getSearchFields("B3Kat OAI-PMH");

        assertEquals("Catalogue B3Kat OAI_PMH should be one search field", 1, searchFields.size());
        assertEquals("The Identifier field should have value identifier", "identifier", searchFields.get("Identifier"));
    }

    @Test
    public void testGetConfig() {
        Map<String, String> config = marcOpacConfig.getConfig("Some Alma API");
        assertNotNull("The url of catalogue Some Alma API should not be null", config.get("url"));
        assertNotNull("The apikey of catalogue Some Alma API should not be null", config.get("apikey"));
        assertNotNull("The class of catalogue Some Alma API should not be null", config.get("class"));
        assertNotNull("The recordTag of catalogue Some Alma API should not be null", config.get("recordTag"));

        assertEquals("The catalogue Some Alma API should have a certain url",
                "https://api-eu.hosted.exlibrisgroup.com/almaws/v1/bibs/", config.get("url"));
        assertEquals("The catalogue Some Alma API should have a certain apikey",
                "secret", config.get("apikey"));
        assertEquals("The catalogue Some Alma API should have a certain class",
                "org.kitodo.production.plugin.catalogue.marc.clients.AlmaMarcCatalogueClient", config.get("class"));
        assertEquals("The recordTag of Some Alma API should be 'record'",
                "record", config.get("recordTag"));

        assertNotNull("The Alma API entry should have a parentDataCatalogue", config.get("parentDataCatalogue"));
        assertEquals("The Alma API entry entry should have parentDataCatalogue 'B3Kat OAI-PMH'", "B3Kat OAI-PMH", config.get("parentDataCatalogue"));

        config = marcOpacConfig.getConfig("B3Kat OAI-PMH");
        assertNull("The apikey of catalogue B3Kat should be null", config.get("apikey"));

    }

    @Test
    public void testGetMappings() {
        Map<String, MarcMapping> mappings = marcOpacConfig.getMappings(null);

        assertNotNull("The mappings map must not be null", mappings);
        assertNotNull("There should be a TitleDocMain entry", mappings.get("TitleDocMain"));
        assertNotNull("There should be an Author entry", mappings.get("Author"));
        assertEquals("The TitleDocMain entry should have marc field 245a", "245a", mappings.get("TitleDocMain").getMarcFields().get(0));
        assertNotNull("The TitleDocMain entry should have a replacemants map", mappings.get("TitleDocMain").getReplacements());
        assertFalse("The TitleDocMain entry should not be added to the physical doc struct", mappings.get("TitleDocMain").isAddToPhysDocStruct());
        assertEquals("The TitleDocMain entry should have two replacements", 3, mappings.get("TitleDocMain").getReplacements().size());
        assertEquals("The Author should have alternative marc field 700a", "700a", mappings.get("Author").getAltMarcFields().get(0));
        assertEquals("The Author should have a matching entry", ".+,.+", mappings.get("Author").getMatching());
        assertEquals("The Author should have an authority", "gnd", mappings.get("Author").getAuthority());

        assertNotNull("There should be a shelfmarksource entry", mappings.get("shelfmarksource"));
        assertTrue("The shelfmarksource should be added to the physical doc struct", mappings.get("shelfmarksource").isAddToPhysDocStruct());

        assertNotNull("There should be a Keyword entry", mappings.get("Keyword"));
        assertEquals("The Keyword entry should have seven marc fields", 7, mappings.get("Keyword").getMarcFields().size());
        assertTrue("The Keyword entry marc fields should contain 650a", mappings.get("Keyword").getMarcFields().contains("650a"));
        assertTrue("The Keyword entry marc fields should contain 611a", mappings.get("Keyword").getMarcFields().contains("611a"));
        assertEquals("The Keyword entry should have no alternative marc fields", 0, mappings.get("Keyword").getAltMarcFields().size());
        assertEquals("The Keyword entry should have authority 'gnd'", "gnd", mappings.get("Keyword").getAuthority());
        assertTrue("The Keyword authority should be exclusive", mappings.get("Keyword").isAuthorityExclusive());
    }

    @Test
    public void testGetMappingsForVolume() {
        Map<String, MarcMapping> mappings = marcOpacConfig.getMappings("Volume");

        assertNotNull("The mappings map must not be null", mappings);
        assertNotNull("There should be a TitleDocMain entry", mappings.get("TitleDocMain"));
        assertNotNull("There should be an Author entry", mappings.get("Author"));
        assertEquals("The TitleDocMain entry should have marc field 245p", "245p", mappings.get("TitleDocMain").getMarcFields().get(0));
        assertNotNull("The TitleDocMain entry should have a replacemants map", mappings.get("TitleDocMain").getReplacements());
        assertEquals("The TitleDocMain entry should have two replacements", 3, mappings.get("TitleDocMain").getReplacements().size());
        assertEquals("The Author should have alternative marc field 700a", "700a", mappings.get("Author").getAltMarcFields().get(0));
        assertEquals("The Author should have a matching entry", ".+,.+", mappings.get("Author").getMatching());
    }

    @Test
    public void testGetDocTypes() {
        Map<String, Map<String, String>> docTypes = marcOpacConfig.getDocTypes();

        assertNotNull("The doctypes map must not be null", docTypes);
        assertEquals("The doctypes map should have three entries", 3, docTypes.size());
        assertNotNull("There should be an entry 'Monograph'", docTypes.get("Monograph"));
        assertNotNull("There should be an as entry 'MultiVolumeWork'", docTypes.get("MultiVolumeWork"));
        assertEquals("The Monograph entry should have a types map of size 10", 10, docTypes.get("Monograph").size());
        assertNotNull("The Monograph entry should have a rulesetType", docTypes.get("Monograph").get("rulesetType"));
        assertEquals("The Monograph entry should have rulesetType Monograph", "Monograph", docTypes.get("Monograph").get("rulesetType"));
        assertNotNull("The Monograph entry should have a title", docTypes.get("Monograph").get("title"));
        assertEquals("The Monograph entry should have title monograph", "monograph", docTypes.get("Monograph").get("title"));

        assertNotNull("The MultiVolumeWork entry should have a parentField", docTypes.get("MultiVolumeWork").get("parentField"));
        assertEquals("The MultiVolumeWork entry should have parentField 773w", "773w", docTypes.get("MultiVolumeWork").get("parentField"));
        assertNotNull("The MultiVolumeWork entry should have a marcLeader regex", docTypes.get("MultiVolumeWork").get("marcLeader"));
        assertEquals("The MultiVolumeWork entry should have marcLeader regex 'a[am]'", "a[am]", docTypes.get("MultiVolumeWork").get("marcLeader"));

    }

    @Test
    public void testGetAuthority() {
        Map<String, Map<String, String>> authorities = marcOpacConfig.getAuthorities();

        assertNotNull("The authorities map must not be null", authorities);
        assertEquals("The authorities map should have two entries", 2, authorities.size());
        assertNotNull("There should be an entry 'gnd'", authorities.get("gnd"));
        assertEquals("The gnd entry should have a config map of size 5", 5, authorities.get("gnd").size());
        assertNotNull("The gnd entry should have a uri", authorities.get("gnd").get("uri"));
        assertEquals("The gnd entry should have uri 'http://d-nb.info/gnd/'", "http://d-nb.info/gnd/", authorities.get("gnd").get("uri"));
        assertNotNull("The gnd entry should have a prefix", authorities.get("gnd").get("prefix"));
        assertEquals("The gnd entry should have prefix (DE-588)", "(DE-588)", authorities.get("gnd").get("prefix"));
        assertEquals("The gnd entry should have valueSubfield '0'", "0", authorities.get("gnd").get("valueSubfield"));
        assertEquals("The gnd entry should have idSubfield '2'", "2", authorities.get("gnd").get("idSubfield"));

        assertNotNull("There should be an entry 'rvk'", authorities.get("rvk"));
        assertEquals("The rvk entry should have a config map of size 5", 5, authorities.get("rvk").size());
        assertEquals("The rvk entry should have id 'rvk'", "rvk", authorities.get("rvk").get("id"));
        assertEquals("The rvk entry should have idSubfield '2'", "2", authorities.get("rvk").get("idSubfield"));
        assertNull("The rvk entry should have no valueSubfield entry", authorities.get("rvk").get("valueSubfield"));

    }

}
