/*
 * (c) Kitodo. Key to digital objects e. V. <contact@kitodo.org>
 *
 * This file is part of the Kitodo project.
 *
 * It is licensed under GNU General Public License version 3 or later.
 *
 * For the full copyright and license information, please read the
 * GPL3-License.txt file that was distributed with this source code.
 */

package org.kitodo.production.plugin.catalogue.marc.processors;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.lang.StringUtils;
import org.marc4j.MarcReader;
import org.marc4j.MarcXmlReader;
import org.marc4j.marc.Record;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * An implementation of the MarcXML to Marc4j transformer using DOM.
 */
public class MarcXmlDomRecordTransformer implements IMarcRecordTransformer {

    private static final String DEFAULT_RECORD_TAG = "record";

    /**
     * Takes an InputStream containing an XML document with marc records and transfers it to a list of marc4j Record objects.
     *
     * @param inputStream the input stream
     * @param recordTag   the tag name of the marc records
     * @return a list of marc4j records
     */
    @Override
    public List<Record> transform(InputStream inputStream, String recordTag) {
        if (inputStream == null) {
            throw new RuntimeException("The input stream must not be null");
        }
        if (recordTag == null) {
            recordTag = DEFAULT_RECORD_TAG;
        }

        List<Record> marcRecords = new ArrayList<>();

        try {
            /*
             * Create a DOM Document object from the input stream.
             */
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document responseDocument = builder.parse(inputStream);
            inputStream.close();

            /*
             * Transfer the original document to a format readable for marc4j.
             */
            Document marcDocument = builder.newDocument();
            Node collection = marcDocument.appendChild(marcDocument.createElement("collection"));
            NodeList records = responseDocument.getElementsByTagName(recordTag);
            int numRecords = records.getLength();
            for (int idx = 0; idx < numRecords; idx++) {
                Node recordElement = records.item(idx);
                // Get the record only if is is a marc record, i.e. if has a leader child element. Otherwise it might be an oai record.
                if (hasChildNodeWithName(recordElement, "leader")) {
                    Node recordNode = marcDocument.importNode(recordElement, true);
                    collection.appendChild(recordNode);
                }
            }

            /*
             * Create an input stream from the marc DOM document.
             */
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Source xmlSource = new DOMSource(marcDocument);
            Result outputTarget = new StreamResult(outputStream);
            TransformerFactory.newInstance().newTransformer().transform(xmlSource, outputTarget);
            InputStream marcStream = new ByteArrayInputStream(outputStream.toByteArray());

            /*
             * Read the marc document and create a list of record objects.
             */
            MarcReader reader = new MarcXmlReader(marcStream);
            while (reader.hasNext()) {
                marcRecords.add(reader.next());
            }

        } catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {
            throw new RuntimeException("Error reading and transfering xml response to marc4j: " + e, e);
        }

        return marcRecords;
    }

    private boolean hasChildNodeWithName(Node node, String childName) {
        NodeList children = node.getChildNodes();
        int numChildren = children.getLength();
        for (int idx = 0; idx < numChildren; idx++) {
            Node child = children.item(idx);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                if (StringUtils.equals(child.getLocalName(), childName)) {
                    return true;
                }
            }
        }
        return false;
    }

}
