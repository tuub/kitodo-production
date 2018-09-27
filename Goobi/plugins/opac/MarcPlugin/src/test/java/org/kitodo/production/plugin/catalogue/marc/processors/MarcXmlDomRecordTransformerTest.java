package org.kitodo.production.plugin.catalogue.marc.processors;

import java.io.InputStream;
import java.util.List;
import org.junit.Test;
import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;

import static org.junit.Assert.assertEquals;

/**
 * Test for the marcXml DOM transformer.
 */
public class MarcXmlDomRecordTransformerTest {

    MarcXmlDomRecordTransformer transformer = new MarcXmlDomRecordTransformer();

    @Test
    public void testTransformFromOaiMarcXml() {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("oai_full_response.xml");

        List<Record> marcRecords = transformer.transform(inputStream, "marc:record");

        assertEquals("The marc record list should contain one object", 1, marcRecords.size());

        Record marcRecord = marcRecords.get(0);
        ControlField controlField = (ControlField) marcRecord.getVariableField("001");
        assertEquals("The id in control field 001 should be BV024449795", "BV024449795", controlField.getData());

        DataField titleField = (DataField) marcRecord.getVariableField("245");
        assertEquals("The title in 245a should be Einstein", "Einstein", titleField.getSubfield('a').getData());

    }

    @Test
    public void testTransformFromAlmaMarcXml() {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("alma_full_response.xml");

        List<Record> marcRecords = transformer.transform(inputStream, null);

        assertEquals("The marc record list should contain one object", 1, marcRecords.size());

        Record marcRecord = marcRecords.get(0);
        ControlField controlField = (ControlField) marcRecord.getVariableField("001");
        assertEquals("The id in control field 001 should be 9921328575102884", "9921328575102884", controlField.getData());

        DataField titleField = (DataField) marcRecord.getVariableField("245");
        assertEquals("The title in 245a should be 'The Einstein journal of biology and medicine :'",
                "The Einstein journal of biology and medicine :",
                titleField.getSubfield('a').getData());

    }

    @Test
    public void testTransformFromSolrMarcXml() {
        ClassLoader classLoader = getClass().getClassLoader();
        // The unescaping of the solr response ist tested elsewhere
        InputStream inputStream = classLoader.getResourceAsStream("touchpoint_marc_unescaped.xml");

        List<Record> marcRecords = transformer.transform(inputStream, "mx:record");

        assertEquals("The marc record list should contain one object", 1, marcRecords.size());

        Record marcRecord = marcRecords.get(0);
        ControlField controlField = (ControlField) marcRecord.getVariableField("001");
        assertEquals("The id in control field 001 should be 5496610", "5496610", controlField.getData());

        DataField titleField = (DataField) marcRecord.getVariableField("245");
        assertEquals("The title in 245a must be correct",
                "Postilla super Epistolas et Evangelia quadragesimalia (cum Quaestionibus Antonii Betontini et Alexandri de Ales)",
                titleField.getSubfield('a').getData());

    }
}
