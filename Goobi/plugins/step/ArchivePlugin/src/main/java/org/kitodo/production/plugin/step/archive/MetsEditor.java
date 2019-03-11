package org.kitodo.production.plugin.step.archive;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Modify METS file.
 */
public class MetsEditor {

    /**
     * The METS file.
     */
    private File metsFile;

    /**
     * The DOM document.
     */
    private Document doc;

    /**
     * The XPath object.
     */
    private XPath xpath;

    /**
     * XML namespaces to be used in this METS file.
     */
    private static Map<String, String> NAMESPACES;

    static {
        // TODO: Read namespaces from Kitodo ruleset file to match those settings.
        NAMESPACES = new HashMap<>();
        NAMESPACES.put("mets", "http://www.loc.gov/METS/");
        NAMESPACES.put("mods", "http://www.loc.gov/mods/v3");
        NAMESPACES.put("xlink", "http://www.w3.org/1999/xlink");
    }

    /**
     * Constructor.
     *
     * <p>
     *     Reads the METS file and prepares DOM and XPath.
     *
     * @param metsFile the METS file to work with
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    public MetsEditor(File metsFile) throws ParserConfigurationException, IOException, SAXException {
        this.metsFile = metsFile;
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setNamespaceAware(true);
        docFactory.setValidating(false);
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        doc = docBuilder.parse(metsFile);
        xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new NamespaceContext() {

            @Override
            public Iterator getPrefixes(String namespaceURI) {
                return Collections.singletonList(getPrefix(namespaceURI)).iterator();
            }

            @Override
            public String getPrefix(String namespaceURI) {
                for (Map.Entry<String, String> namespace : NAMESPACES.entrySet()) {
                    if (namespace.getValue().equals(namespaceURI)) {
                        return namespace.getKey();
                    }
                }
                return null;
            }

            @Override
            public String getNamespaceURI(String prefix) {
                return NAMESPACES.get(prefix);
            }
        });
    }

    /**
     * Remove all file groups except the given one.
     *
     * @param excludeGroup Remove all filegroups but this one.
     * @throws XPathExpressionException
     */
    public void removeAllFileGroupsBut(String excludeGroup) throws XPathExpressionException {
        NodeList removeNodes = nodes("//mets:fileGrp[@USE!='" + excludeGroup + "']");
        int length = removeNodes.getLength();
        for (int i = 0; i < length; i++) {
            Node removeNode = removeNodes.item(i);
            removeFileGroup(removeNode);
        }
    }

    /**
     * Remove a file group and all references to it.
     *
     * @param node the file group node to be removed
     * @throws XPathExpressionException
     */
    public void removeFileGroup(Node node) throws XPathExpressionException {
        String[] Ids = values(node, "mets:file/@ID");
        for (String id : Ids) {
            // remove fptr nodes
            NodeList removeNodes = nodes(
                "//mets:structMap[@TYPE='PHYSICAL']/mets:div/mets:div/mets:fptr[@FILEID='" + id + "']");
            int length = removeNodes.getLength();
            for (int j = 0; j < length; j++) {
                Node removeNode = removeNodes.item(j);
                removeNode.getParentNode().removeChild(removeNode);
            }
        }
        node.getParentNode().removeChild(node);
    }

    /**
     * Rename a file group.
     *
     * <p>
     *     Changes the {@code @USE} attribute of the file group node.
     *
     * @param oldUse the name to search for
     * @param newUse the new name to set
     * @throws XPathExpressionException
     */
    public void renameFileGroup(String oldUse, String newUse) throws XPathExpressionException {
        value("//mets:fileGrp[@USE='" + oldUse + "']/@USE", newUse);
    }

    /**
     * Set or create a file group.
     *
     * @param name the name ({@code @USE}) of the file group.
     * @param fileInfos the fileset to write info the file group
     * @throws XPathExpressionException
     */
    public void setFileGroup(String name, List<FileInfo> fileInfos) throws XPathExpressionException {

        // Use a file group with the same name, if it exists
        Element fileGrpNode = (Element)node("//mets:fileGrp[@USE='" + name + "']");
        if (fileGrpNode == null) {
            // ... or create a new file group
            fileGrpNode = createElement("mets", "fileGrp");
            fileGrpNode.setAttribute("USE", name);
            node("//mets:fileSec").appendChild(fileGrpNode);
        }

        // Create a file entries for every file
        for (FileInfo fileInfo : fileInfos) {
            String filename = fileInfo.file.getName();

            // If the file group existed before, the file may exist too - use it.
            Node fileNode = node(fileGrpNode, "mets:file/mets:FLocat[substring(@xlink:href, "
                + "string-length(@xlink:href) - string-length('/" + filename + "') + 1) = '/" + filename + "']/..");
            if (fileNode == null) {
                // ... or create a new file node
                fileNode = createFile(name, fileInfo);
                fileGrpNode.appendChild(fileNode);
                String basename = FilenameUtils.getBaseName(filename);
                String masterFileId = value(
                    "//mets:fileGrp/mets:file/mets:FLocat[contains(@xlink:href,'/" + basename + "')]/../@ID");
                Node page = node(
                    "//mets:structMap[@TYPE='PHYSICAL']/mets:div/mets:div/mets:fptr[@FILEID='" + masterFileId + "']/..");
                Element fptrNode = createElement("mets", "fptr");
                String fileId = value(fileNode, "@ID");
                fptrNode.setAttribute("FILEID", fileId);
                page.appendChild(fptrNode);
            }

            // update file informations in that node
            modifyFile((Element)fileNode, name, fileInfo);
        }
    }

    /**
     * Create a file node.
     *
     * <p>
     *     It only creates the file node and writes an ID. It doesn't write any further file infos. This is done by {@link #modifyFile(Element, String, FileInfo)}.
     *
     * @param fileGroup the fileGroup is used to create a unique file ID
     * @param fileInfo fileinfo is used to create a unique file ID
     * @return a new file node containing a Flocat child
     */
    private Node createFile(String fileGroup, FileInfo fileInfo) {
        Element fileNode = createElement("mets", "file");
        String fileId = "FILE_" + FilenameUtils.getBaseName(fileInfo.file.getName()) + "_" + fileGroup;
        fileNode.setAttribute("ID", fileId);
        Element flocatNode = createElement("mets", "FLocat");
        flocatNode.setAttribute("LOCTYPE", "URL");
        fileNode.appendChild(flocatNode);
        return fileNode;
    }

    /**
     * Set file infos to a file node.
     *
     * @param node the file node to write file infos to
     * @param parentDir the base directory to which the filename will be appended
     * @param fileInfo these fileinfos will be written to the file node
     * @throws XPathExpressionException
     */
    private void modifyFile(Element node, String parentDir, FileInfo fileInfo) throws XPathExpressionException {
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        String mimeType = fileNameMap.getContentTypeFor(fileInfo.file.getName());
        node.setAttribute("MIMETYPE", mimeType);
        node.setAttribute("SIZE", Long.toString(fileInfo.size));
        node.setAttribute("CHECKSUM", fileInfo.checksum);
        node.setAttribute("CHECKSUMTYPE", fileInfo.checksumType);
        Element flocatNode = (Element)node(node, "mets:FLocat");
        flocatNode.setAttributeNode(
            createAttribute("xlink", "href", FilenameUtils.concat(parentDir, fileInfo.file.getPath())));
    }

    /**
     * Add creator infos about the application.
     *
     * @param name name of the creator application
     * @param role role of the creator. Must match METS specs. {@see <a href="https://www.loc.gov/standards/mets/docs/mets.v1-9.html#agent">METS reference</a>}
     * @throws XPathExpressionException
     */
    public void addCreator(String name, String role) throws XPathExpressionException {
        Node metsHdrNode = node("//mets:metsHdr");
        Element agentNode = createElement("mets", "agent");
        agentNode.setAttribute("ROLE", role);
        agentNode.setAttribute("OTHERTYPE", "SOFTWARE");
        agentNode.setAttribute("TYPE", "OTHER");
        Element nameNode = createElement("mets", "name");
        nameNode.appendChild(doc.createTextNode(name));
        agentNode.appendChild(nameNode);
        metsHdrNode.appendChild(agentNode);
    }

    /**
     * Repair known METS issues to make it valid.
     *
     * @throws XPathExpressionException
     */
    public void repairKnownIssues() throws XPathExpressionException {

        // Rename mods:accessCondition[@href] to mods:accessCondition[@xlink:href]
        {
            NodeList nodes = nodes("//mods:accessCondition[@href]");
            IntStream.range(0, nodes.getLength())
                .mapToObj(nodes::item)
                .forEach(node -> {
                    NamedNodeMap attributes = node.getAttributes();
                    Attr attr = (Attr)attributes.getNamedItem("href");
                    String value = attr.getValue();
                    attributes.removeNamedItem("href");
                    Attr newAttr = createAttribute("xlink", "href", value);
                    attributes.setNamedItemNS(newAttr);
                });
        }
    }

    /**
     * Get work ID from mods:recordIdentifier.
     *
     * @return the work ID
     * @throws XPathExpressionException
     */
    public String getWorkId() throws XPathExpressionException {
        return value("//mods:recordIdentifier[@source='kobv']/text()");
    }

    /**
     * Create a DOM element with namespace.
     *
     * @param namespace the namespace
     * @param name the local name
     * @return the new DOM element
     */
    private Element createElement(String namespace, String name) {
        Element element = doc.createElementNS(NAMESPACES.get(namespace), name);
        element.setPrefix(namespace);
        return element;
    }

    /**
     * Create a DOM attribute with namespace.
     *
     * @param namespace the namespace
     * @param name the attribute local name
     * @param value the attribute value
     * @return the new DOM attribute
     */
    private Attr createAttribute(String namespace, String name, String value) {
        Attr attribute = doc.createAttributeNS(NAMESPACES.get(namespace), name);
        attribute.setPrefix(namespace);
        attribute.setValue(value);
        return attribute;
    }

    /**
     * Get values of the nodes.
     *
     * @param node the root node
     * @param expression the XPath expression to query for
     * @return an array of values of all matching nodes or null if no nodes found
     * @throws XPathExpressionException
     */
    private String[] values(Node node, String expression) throws XPathExpressionException {
        NodeList nodes = nodes(node, expression);
        int length = nodes.getLength();
        List<String> values = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            values.add(nodes.item(i).getNodeValue());
        }
        return values.toArray(new String[0]);
    }

    /**
     * Get a value of a node.
     *
     * @param node the root node
     * @param expression the XPath expression to query for
     * @return the first nodes value or null if no nodes found
     * @throws XPathExpressionException
     */
    private String value(Node node, String expression) throws XPathExpressionException {
        String[] values = values(node, expression);
        return values.length > 0 ? values[0] : null;
    }

    /**
     * Get a value from the whole document.
     *
     * @param expression the XPath expression to query for
     * @return the first nodes value or null if no nodes found
     * @throws XPathExpressionException
     */
    public String value(String expression) throws XPathExpressionException {
        return value(doc, expression);
    }

    /**
     * Set a value.
     *
     * @param node the root node
     * @param expression the XPath expression to query for
     * @param value the value to be set
     * @throws XPathExpressionException
     */
    private void value(Node node, String expression, String value) throws XPathExpressionException {
        Node editNode = node(node, expression);
        editNode.setNodeValue(value);
    }

    /**
     * Set a value.
     *
     * @param expression the XPath expression to query for
     * @param value the value to be set
     * @throws XPathExpressionException
     */
    private void value(String expression, String value) throws XPathExpressionException {
        value(doc, expression, value);
    }

    /**
     * Get nodes by XPath expression.
     *
     * @param expression the XPath expression to query for
     * @return node list
     * @throws XPathExpressionException
     */
    private NodeList nodes(String expression) throws XPathExpressionException {
        return nodes(doc, expression);
    }

    /**
     * Get nodes by XPath expression.
     *
     * @param node the root node
     * @param expression the XPath expression to query for
     * @return node list
     * @throws XPathExpressionException
     */
    private NodeList nodes(Node node, String expression) throws XPathExpressionException {
        return (NodeList)xpath.evaluate(expression, node, XPathConstants.NODESET);
    }

    /**
     * Get the node.
     *
     * @param node the node
     * @param expression XPath expression
     * @return node or null
     */
    private Node node(Node node, String expression) throws XPathExpressionException {
        NodeList nodes = nodes(node, expression);
        if (nodes.getLength() > 0) {
            return nodes.item(0);
        }
        return null;
    }

    private Node node(String expression) throws XPathExpressionException {
        return node(doc, expression);
    }

    /**
     * Save to XML file.
     *
     * @throws FileNotFoundException
     * @throws TransformerException
     */
    public void save() throws FileNotFoundException, TransformerException {
        save(metsFile);
    }

    /**
     * Save to XML file.
     *
     * @param file output file
     * @throws FileNotFoundException
     * @throws TransformerException
     */
    public void save(File file) throws FileNotFoundException, TransformerException {
        Transformer tr = TransformerFactory.newInstance().newTransformer();
        tr.setOutputProperty(OutputKeys.INDENT, "yes");
        tr.setOutputProperty(OutputKeys.METHOD, "xml");
        tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        tr.transform(new DOMSource(doc), new StreamResult(new FileOutputStream(file)));
    }

    /**
     * Validate the XML file.
     *
     * @param file the XML file
     * @return If there are problems found the error messages will be returned in a list. On success the list is empty
     * @throws SAXException
     * @throws IOException
     * @throws ParserConfigurationException
     */
    public List<String> validate(File file) throws SAXException, IOException, ParserConfigurationException {

        String[] features = new String[]{
            "http://apache.org/xml/features/validation/schema-full-checking",
            "http://apache.org/xml/features/honour-all-schemaLocations",
            "http://apache.org/xml/features/validate-annotations",
        };

        MetsErrorHandler errorHandler = new MetsErrorHandler();

        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schemaFactory.setErrorHandler(errorHandler);
        for (String feature : features) {
            schemaFactory.setFeature(feature, true);
        }
        Schema schema = schemaFactory.newSchema();
        Validator validator = schema.newValidator();
        validator.setErrorHandler(errorHandler);
        for (String feature : features) {
            schemaFactory.setFeature(feature, true);
            validator.setFeature(feature, true);
        }

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        documentBuilder.setErrorHandler(errorHandler);
        Document doc = documentBuilder.parse(file);
        DOMSource source = new DOMSource(doc);
        validator.validate(source);

        return errorHandler.getMessages();
    }
}
