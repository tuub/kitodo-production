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

package org.kitodo.production.plugin.catalogue.marc.clients;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;



/**
 * Catalogue Client querying a Solr server, e.g. a Touchpoint Solr.
 */
public class SolrMarcCatalogueClient extends AbstractCatalogueClient {

    private static final String MARC_RECORD_PATH = "/response/result/doc/str[@name='marc']";

    /**
     * Queries a solr server using the given url and params.
     *
     * @param urlString the url of the catalogue.
     * @param params    query params map, MUST contain a query key.
     * @param timeout timeout in ms
     * @return an InputStream with the response.
     */
    @Override
    public InputStream query(String urlString, Map<String, String> params, long timeout) throws Exception {

        if (params == null || params.get("query") == null ) {
            throw new RuntimeException("The params map must contain a query. Map content: " + params);
        }

        String query = params.get("query");
        String key = StringUtils.substringBefore(query, ":");
        String value = StringUtils.substringAfter(query, ":");
        // Escape solr special characters
        //value = ClientUtils.escapeQueryChars(value);

        Map<String, String> postParams = new HashMap<>();
        postParams.put("q", key + ":" + value);

        InputStream response = post(urlString, timeout, postParams);
        String unescapedRecords = unescapeMarcRecords(response);

        return new ByteArrayInputStream(unescapedRecords.getBytes());
    }

    /**
     * Since the marc records in the solr response are escaped, we need to fish them out of the response unescaped.
     *
     * @param in the input stream with the response
     * @return a string with the unescaped records
     * @throws Exception by severe errors
     */
    String unescapeMarcRecords(InputStream in)throws Exception {

        StringBuffer records = new StringBuffer("<collection>");
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        Document xmlDocument = builder.parse(in);
        in.close();

        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList nodeList = (NodeList) xPath.compile(MARC_RECORD_PATH).evaluate(xmlDocument, XPathConstants.NODESET);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                records.append(node.getTextContent());
            }
        }
        records.append("</collection>");
        return records.toString();
    }
}
