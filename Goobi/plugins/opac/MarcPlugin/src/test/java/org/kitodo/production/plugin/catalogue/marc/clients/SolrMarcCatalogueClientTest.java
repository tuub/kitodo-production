package org.kitodo.production.plugin.catalogue.marc.clients;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.InputStream;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;

public class SolrMarcCatalogueClientTest {

    private SolrMarcCatalogueClient solrMarcCatalogueClient = new SolrMarcCatalogueClient();;

    @Test
    public void testUnescapeMarc() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("touchpoint_solr_response.xml");

        InputStream expectedStream = classLoader.getResourceAsStream("touchpoint_marc_unescaped.xml");
        StringWriter writer = new StringWriter();
        IOUtils.copy(expectedStream, writer);
        String expectedString = writer.toString();

        String unescapedMarc = solrMarcCatalogueClient.unescapeMarcRecords(inputStream);

        assertEquals("The marc record in the solr response should have been unescaped",
                expectedString,
                unescapedMarc);

    }
}
