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

package org.kitodo.production.plugin.catalogue.marc;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.xeoh.plugins.base.Plugin;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.kitodo.production.plugin.catalogue.marc.clients.IMarcCatalogueClient;
import org.kitodo.production.plugin.catalogue.marc.config.MarcMapping;
import org.kitodo.production.plugin.catalogue.marc.config.MarcOpacConfig;
import org.kitodo.production.plugin.catalogue.marc.processors.IMarcDocTypeMapper;
import org.kitodo.production.plugin.catalogue.marc.processors.IMarcMetadataMapper;
import org.kitodo.production.plugin.catalogue.marc.processors.IMarcRecordTransformer;
import org.kitodo.production.plugin.catalogue.marc.processors.MarcDocTypeMapper;
import org.kitodo.production.plugin.catalogue.marc.processors.MarcKitodoMetadataMapper;
import org.kitodo.production.plugin.catalogue.marc.processors.MarcXmlDomRecordTransformer;
import org.kitodo.production.plugin.catalogue.marc.util.Marc4jUtil;
import org.marc4j.marc.Record;
import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.DocStructType;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.MetadataType;
import ugh.dl.Person;
import ugh.dl.Prefs;
import ugh.exceptions.MetadataTypeNotAllowedException;
import ugh.fileformats.mets.XStream;

/**
 * Main class of the Kitodo marc catalogue plugin implementation.
 * This plugin can be used to access library catalogue systems using marcXml.
 *
 * <p>
 * The plugin provides all public methods required by {@see org.goobi.production.plugin.PluginLoader} or
 * {@see de.sub.goobi.forms.ProzesskopieForm}, which is the class using the plugin:
 *
 * <pre>
 * XMLConfiguration getXMLConfiguration()
 * void             configure(Map)
 * Object           find(String, long)
 * String           getDescription()
 * Map              getHit(Object, long, long)
 * long             getNumberOfHits(Object, long)
 * String           getTitle()
 * void             setPreferences(Prefs)
 * boolean          supportsCatalogue(String)
 * void             useCatalogue(String)
 * </pre>
 * as specified by {@code org.goobi.production.plugin.UnspecificPlugin}
 * {@code [*]} and
 * {@code org.goobi.production.plugin.CataloguePlugin.CataloguePlugin}.
 *
 */
@PluginImplementation
public class MarcPlugin implements Plugin {

    private static final Logger logger = Logger.getLogger(MarcPlugin.class);

    private static final String OPAC_CONFIGURATION_FILE = "kitodo_marc_opac.xml";

    private static final String DEFAULT_DOC_TYPE = "Monograph";

    private static final String DEFAULT_DOC_TYPE_TITLE = "monograph";

    /**
     * The file system directory where configuration files are stored. The field is initialised by the PluginLoader.
     */
    private String configDir;

    /**
     * The field preferences holds the UGH preferences.
     */
    private Prefs preferences;

    /**
     * The configuration class of the opac. The class is initialised in the configure() method called by the PluginLoader.
     */
    private MarcOpacConfig marcOpacConfig;

    /**
     * Name of the catalogue to use. Is set in the method useCatalogue().
     */
    private String catalogueName;

    /**
     * A map of authority configurations. Is set in the configure() method.
     */
    private Map<String, Map<String, String>> authorityConfigs = new HashMap<>();

    /**
     * Transformer for marcXml to marc4j records.
     */
    private IMarcRecordTransformer marcXmlRecordTransformer = new MarcXmlDomRecordTransformer();

    /**
     * Metadata mapper for marc records.
     */
    private IMarcMetadataMapper marcMapper = new MarcKitodoMetadataMapper();

    /**
     * Document type mapper for marc records.
     */
    private IMarcDocTypeMapper marcDocTypeMapper = new MarcDocTypeMapper();

    /**
     * Returns the XMLConfiguration of the plugin containing docType
     * names and conditions for structureType classification.
     *
     * @return config
     *            the XMLConfiguration of the plugin
     */
    public XMLConfiguration getXMLConfiguration() {
        return marcOpacConfig.getXmlConfiguration();
    }

    /**
     * Injects the plug-in’s configuration. The method takes a Map with
     * configuration parameters. An entry {@code configDir} is required:
     *
     * <p>
     * {@code configDir} must point to a directory on the local file system
     * where the plug-in can read individual configuration files from. The
     * configuration file {@code kitodo_marc_opac.xml} is expected in that
     * directory.
     *
     * <p>
     * This method is called at runtime after the classloader has created the
     * instance, because the plug-in constructor cannot take arguments. Confer
     * to {@code UnspecificPlugin.configure(Map)} in package
     * {@code org.goobi.production.plugin} of the core application, too.
     *
     * @param configuration
     *            a Map with configuration parameters
     */
    public void configure(Map<String, String> configuration) {
        configDir = configuration.get("configDir");
        if (configDir == null) {
            String message = "Missing entry 'configDir' in the configuration map. The marc plugin cannot be configured.";
            logger.error(message);
            throw new RuntimeException(message);
        }
        marcOpacConfig = new MarcOpacConfig(FilenameUtils.concat(configDir, OPAC_CONFIGURATION_FILE));

        Map<String, Map<String, String>> authorityConfigs = marcOpacConfig.getAuthorities();
        if (authorityConfigs != null) {
            this.authorityConfigs = authorityConfigs;
        }
    }

    /**
     * Initially queries the library catalogue with the given query. If
     * successful, it returns a List of all hits.
     *
     * @param query
     *            the query to the catalogue
     * @param timeout
     *            timeout in milliseconds after which the operation shall return
     * @return a FindResult that may be used for future operations on the query
     */
    public Object find(String query, long timeout) {

        List<Record> marcRecords = queryCatalogue(catalogueName, query, timeout);

        return marcRecords;
    }

    /**
     * Provides a reference to the file system directory where configuration
     * files are read from.
     *
     * @return the file system directory with the configuration files
     */
    String getConfigDir() {
        return configDir;
    }

    /**
     * Returns a human-readable description of the plug-in’s functionality in
     * English. The parameter language is ignored.
     *
     * @param language not used
     * @return a human-readable description of the plug-in’s functionality
     */
    public String getDescription(Locale language) {
        return "The marc plugin can be used to access library catalogue systems using marc.";
    }

    /**
     * Returns the hit with the given index from the given search result. The
     * object returned is a Map&lt;String, Object>. It contains the full hit as
     * {@code fileformat}, the docType as {@code type} and some bibliographic
     * meta-data to show a bibliographic hit summary as supposed in class
     * {@code Hit} in package
     * {@code org.goobi.production.plugin.CataloguePlugin} of the core
     * application.
     *
     * <p>
     * Confer to {@code CataloguePlugin.getHit(Object, long, long)} in package
     * {@code org.goobi.production.plugin.CataloguePlugin} of the core
     * application, too.
     *
     * @param searchResult
     *            a FindResult created by {@link #find(String, long)}
     * @param index
     *            the zero-based index of the hit
     * @param timeout
     *            timeout in milliseconds. Not used since we do not call the opac here.
     * @return a Map with the hit
     */
    public Map<String, Object> getHit(Object searchResult, long index, long timeout) {

        if (searchResult == null || !(searchResult instanceof List)) {
            throw new RuntimeException("The search result is not a list, something is very wrong");
        }

        List marcRecords = (List) searchResult;
        if (index >= marcRecords.size()) {
            String message = "The index " + index + " points outside the search result list, list size: " + marcRecords.size();
            logger.error(message);
            throw new RuntimeException(message);
        }

        Record marcRecord = (Record) marcRecords.get((int) index);

        try {

            // Get document type and create digital document
            Map<String, String> docTypeConfig = marcDocTypeMapper.mapDocType(marcRecord, marcOpacConfig.getDocTypes());
            if (docTypeConfig == null) {
                docTypeConfig = marcOpacConfig.getDocTypes().get(DEFAULT_DOC_TYPE);
            }
            final String docTypeName = docTypeConfig.get("rulesetType");
            final String docTypeTitle = docTypeConfig.get("title");

            Map<String, List<Map<String, String>>> mappings;

            DigitalDocument digitalDocument = new DigitalDocument();
            DocStructType docStructType = preferences.getDocStrctTypeByName(docTypeName);
            DocStruct docStruct = digitalDocument.createDocStruct(docStructType);
            digitalDocument.setLogicalDocStruct(docStruct);

            // Add physical docStruct
            DocStructType physDocStructType = preferences.getDocStrctTypeByName(docTypeConfig.get("physDocStructType"));
            DocStruct physDocStruct = digitalDocument.createDocStruct(physDocStructType);
            digitalDocument.setPhysicalDocStruct(physDocStruct);

            if (StringUtils.equals("true", docTypeConfig.get("isMultiVolume"))) {
                // Get parent data
                String parentCatalogueName = marcOpacConfig.getConfig(catalogueName).get("parentDataCatalogue");
                if (parentCatalogueName == null) {
                    parentCatalogueName = catalogueName;
                }
                String identifier = marcOpacConfig.getSearchFields(parentCatalogueName).get("Identifier");
                String query = identifier + ":";

                List<Map<String, String>> parentIds = Marc4jUtil.getMarcFieldValues(
                        marcRecord,
                        Arrays.asList(docTypeConfig.get("parentField")));
                if (CollectionUtils.isNotEmpty(parentIds)) {
                    String parentField = parentIds.get(0).get("value");
                    if (docTypeConfig.get("parentFieldFilter") != null) {
                        parentField = parentField.replaceAll(docTypeConfig.get("parentFieldFilter"), "");
                    }
                    query += parentField;
                }
                List<Record> parentRecords = queryCatalogue(parentCatalogueName, query, timeout);

                if (CollectionUtils.isNotEmpty(parentRecords)) {
                    mappings = fillDocStruct(docStruct, null, parentRecords.get(0));
                    DocStruct childDocStruct = docStruct.createChild(docTypeConfig.get("childRulesetType"), digitalDocument, preferences);
                    fillDocStruct(childDocStruct, physDocStruct, marcRecord);

                } else {
                    // No parent record found, treat as monograph
                    mappings = fillDocStruct(docStruct, physDocStruct, marcRecord);
                }

            } else {
                mappings = fillDocStruct(docStruct, physDocStruct, marcRecord);
            }

            if (StringUtils.equals("true", docTypeConfig.get("isPeriodical"))) {
                // Create an empty child volume to be filled in the form
                docStruct.createChild(docTypeConfig.get("childRulesetType"), digitalDocument, preferences);
            }


            // The XStream instance is here only used as a container for the DigitalDocument,
            // since the receiving implementation ProzesskopieForm requires a Fileformat object.
            Fileformat fileformat = new XStream(preferences);
            fileformat.setDigitalDocument(digitalDocument);

            // Create a hitMap return object with fileformat, type and some basic metadata for the construction of the process title
            Map<String, Object> hitMap = new HashMap<>();
            hitMap.put("fileformat", fileformat);
            hitMap.put("type", docTypeTitle);
            if (CollectionUtils.isNotEmpty(mappings.get("TitleDocMain")) ) {
                hitMap.put("title", mappings.get("TitleDocMain").get(0).get("value"));
            }
            if (CollectionUtils.isNotEmpty(mappings.get("Author"))) {
                hitMap.put("creator", mappings.get("Author").get(0).get("value"));
            }

            return hitMap;

        } catch (Exception e) {
            String message = "Error mapping the marc record to kitodo metadata: " + e;
            logger.error(message, e);
            throw new RuntimeException(message);
        }

    }

    private Map<String, List<Map<String, String>>> fillDocStruct(DocStruct docStruct, DocStruct physDocStruct, Record marcRecord) {
        // Get the mappings, check cardinalities and add values to the document
        Map<String, MarcMapping> marcMappings = marcOpacConfig.getMappings(docStruct.getType().getName());
        Map<String, List<Map<String, String>>> mappings = marcMapper.map(marcRecord, marcMappings, authorityConfigs);

        for (String metadataField : mappings.keySet()) {
            MetadataType metadataType = preferences.getMetadataTypeByName(metadataField);
            List<Map<String, String>> values = mappings.get(metadataField);
            if (CollectionUtils.isEmpty(values)) {
                continue;
            }

            // If maximum one value is allowed, skip the rest
            if (StringUtils.startsWith(metadataType.getNum(), "1")) {
                addMetadataValue(docStruct, metadataType, values.get(0));
            } else {
                for (Map<String, String> valueMap : values) {
                    addMetadataValue(docStruct, metadataType, valueMap);
                }
            }

            if (physDocStruct != null && marcMappings.get(metadataField).isAddToPhysDocStruct()) {
                addMetadataValue(physDocStruct, metadataType, values.get(0));
            }
        }
        return mappings;
    }

    private void addMetadataValue(DocStruct docStruct, MetadataType metadataType, Map<String, String> valueMap) {
        String value = valueMap.get("value");
        if (value == null) {
            return;
        }
        try {
            if (metadataType.getIsPerson()) {
                Person person = new Person(metadataType);
                person.setDisplayname(value);
                String[] names = value.split(",", 2);
                person.setLastname(names[0]);
                if (names.length > 1) {
                    person.setFirstname(names[1]);
                }
                addAuthorityData(person, valueMap);
                docStruct.addPerson(person);
            } else {
                Metadata metadata = new Metadata(metadataType);
                metadata.setValue(value);
                addAuthorityData(metadata, valueMap);
                docStruct.addMetadata(metadata);
            }
        } catch (MetadataTypeNotAllowedException e) {
            // if a metadata type is not allowed for a certain document type, we simply skip it
            logger.info(e.toString());
        }
    }

    private void addAuthorityData(Metadata metadata, Map<String, String> valueMap) {
        if (valueMap.get("authValue") != null) {
            metadata.setAutorityFile(
                    valueMap.get("authId"),
                    valueMap.get("authUri"),
                    valueMap.get("authUri") + valueMap.get("authValue"));
        }
    }

    /**
     * Returns the number of hits from a given search result.
     *
     * @param searchResult
     *            a list of search results
     * @param timeout
     *            ignored because there is no network acceess in this step
     * @return the number of hits
     */
    public long getNumberOfHits(Object searchResult, long timeout) {

        if (searchResult == null || !(searchResult instanceof List)) {
            throw new RuntimeException("The search result is not a list, something is very wrong");
        }
        return ((List) searchResult).size();
    }

    /**
     * Returns a human-readable name for the plug-in in English. The parameter
     * language is ignored.
     *
     * @param language not used
     * @return a human-readable name for the plug-in
     */
    public String getTitle(Locale language) {
        return "Marc Catalogue Plugin";
    }

    /**
     * Sets the Ugh preferences to be used to create the main result.
     *
     * @param preferences
     *            Ugh preferences to use
     */
    public void setPreferences(Prefs preferences) {
        this.preferences = preferences;
    }

    /**
     * Returns whether this plug-in is able to access the catalogue identified
     * by the given String. This depends on the configuration.
     *
     * @param catalogue
     *            a String identifying the catalogue
     * @return whether the plug-in is able to access that catalogue
     */
    public boolean supportsCatalogue(String catalogue) {
        return marcOpacConfig.supportsCatalogue(catalogue);
    }

    /**
     * Returns the names of all catalogues supported by this plug-in. This
     * depends on the plug-in’s configuration.
     *
     * @return list of catalogue names
     */
    public List<String> getSupportedCatalogues() {
        return marcOpacConfig.getSupportedCatalogues();
    }

    /**
     * Returns the names of all docTypes configured for this plugin. This
     * depends on the plug-in’s configuration.
     *
     * @return list of doc type names
     */
    public List<String> getAllConfigDocTypes() {

        List<String> docTypes = new ArrayList<>();
        Map<String, Map<String, String>> docTypeConfig = marcOpacConfig.getDocTypes();
        if (docTypeConfig != null) {
            docTypes.addAll(docTypeConfig.keySet());
        }
        return docTypes;
    }


    /**
     * Sets a catalogue to be used.
     *
     * @param catalogue
     *            the name of the catalogue
     */
    public void useCatalogue(String catalogue) {
        catalogueName = catalogue;
    }

    /**
     * Method querying a catalogue of a given name.
     *
     * <p>
     * The catalogue class is instantiated here, because there are cases where a different catalogue must be used for
     * parent data.
     *
     * @param catalogueName the name of the catalogue
     * @param query the query
     * @param timeout timeout in ms
     * @return a list of marc4j record objects or null
     */
    private List<Record> queryCatalogue(String catalogueName, String query, long timeout) {

        List<Record> marcRecords;
        IMarcCatalogueClient catalogueClient;
        Map<String, String> catalogueClientConfig = marcOpacConfig.getConfig(catalogueName);
        String catalogueClass = catalogueClientConfig.get("class");
        try {
            catalogueClient = (IMarcCatalogueClient) Class.forName(catalogueClass).newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            String message =  "Couldn't instantiate catalogue client class " + catalogueClass + ": " + e;
            logger.error(message, e);
            throw new RuntimeException(message);
        }

        try {
            Map<String, String> params = new HashMap<>();
            params.put("query", query);
            params.putAll(catalogueClientConfig);

            InputStream clientResponseStream = catalogueClient.query(catalogueClientConfig.get("url"), params, timeout);
            marcRecords = marcXmlRecordTransformer.transform(clientResponseStream, catalogueClientConfig.get("recordTag"));

        } catch (Exception e) {
            String message = "Error querying the catalogue client: " + e;
            logger.error(message, e);
            throw new RuntimeException(message);
        }

        return marcRecords;
    }

    /**
     * Returns the search fields for the given catalogue.
     *
     * @param catalogueName
     *            the name of the catalogue
     * @return Map containing the search fields of the selected OPAC
     */
    public HashMap<String, String> getSearchFields(String catalogueName) {
        return marcOpacConfig.getSearchFields(catalogueName);
    }

    /**
     * Returns the institutions the result can be filtered by for the given
     * catalogue, i.e. none. The method is required by ProzesskopieForm.
     *
     * @param catalogueName
     *            the name of the catalogue for which the list of search fields
     *            will be returned
     * @return Map containing the institutions for which the selected catalogue
     *         supports filtering
     */
    public HashMap<String, String> getInstitutions(String catalogueName) {
        return new HashMap<>();
    }

    /**
     * Returns the URL parameter used for institution filtering in this plug-in. Called by ProzesskopieForm.
     *
     * <p>
     * This function is not yet used.
     *
     * @param catalogueName
     *            the name of the catalogue for which the institution filter
     *            parameter is returned
     * @return String the URL parameter used for institution filtering
     */
    public String getInstitutionFilterParameter(String catalogueName) {
        return "";
    }
}
