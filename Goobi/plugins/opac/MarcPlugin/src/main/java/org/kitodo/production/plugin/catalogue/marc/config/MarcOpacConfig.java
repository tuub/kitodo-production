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

package org.kitodo.production.plugin.catalogue.marc.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * A class reading the opac config file and providing methods for accessing the configurations.
 */
public class MarcOpacConfig {

    private static final Logger logger = Logger.getLogger(MarcOpacConfig.class);

    private static final String DEFAULT_ADD_SUBFIELD_SEPARATOR = " ";

    private XMLConfiguration xmlConfiguration;

    private List<String> supportedCatalogues;

    /**
     * Constructor. Takes a file path to the config file and initializes the XMLConfiguration.
     *
     * @param configFilePath the path of the configuration file
     */
    public MarcOpacConfig(String configFilePath) {
        try {
            xmlConfiguration = new XMLConfiguration(configFilePath);
        } catch (ConfigurationException e) {
            throw new RuntimeException("The opac configuration file " + configFilePath + " cannot be read: " + e, e);
        }
        xmlConfiguration.setExpressionEngine(new XPathExpressionEngine());
        supportedCatalogues = xmlConfiguration.getList("catalogues/catalogue/@title");
    }

    /**
     * Gets the xml configuration. Required by the PluginLoader.
     * @return the XmlConfiguration instance
     */
    public XMLConfiguration getXmlConfiguration() {
        return xmlConfiguration;
    }

    /**
     * Gets all supported catalogues.
     * @return a list of catalogue names
     */
    public List<String> getSupportedCatalogues() {
        return supportedCatalogues;
    }

    /**
     * Tells if a given catalogue is supported by the plugin.
     * @param catalogue the catalogue name
     * @return true, if the catalogue is supported, false otherwise
     */
    public boolean supportsCatalogue(String catalogue) {
        return supportedCatalogues.contains(catalogue);
    }

    /**
     * Gets the search fields for a given catalogue.
     *
     * @param catalogueName the name of the catalogue
     * @return a map with search fields
     */
    public HashMap<String, String> getSearchFields(String catalogueName) {
        HashMap<String, String> searchFields = new HashMap<>();
        List<SubnodeConfiguration> searchFieldConfigs =
                xmlConfiguration.configurationsAt("catalogues/catalogue[@title='" + catalogueName + "']/searchFields/searchField");

        for (SubnodeConfiguration searchField : searchFieldConfigs) {
            searchFields.put(searchField.getString("@label"), searchField.getString("@value"));
        }

        return searchFields;
    }

    /**
     * Gets the config parameter for a given catalogue.
     *
     * @param catalogueName the name of the catalogue
     * @return a map with the configs
     */
    public Map<String, String> getConfig(String catalogueName) {
        Map<String, String> config = new HashMap<>();
        config.put("parentDataCatalogue",
                xmlConfiguration.configurationAt("catalogues/catalogue[@title='" + catalogueName + "']")
                        .getString("parentDataCatalogue"));
        SubnodeConfiguration subnodeConfiguration =
                xmlConfiguration.configurationAt("catalogues/catalogue[@title='" + catalogueName + "']/config");

        config.put("url", subnodeConfiguration.getString("@url"));
        config.put("class", subnodeConfiguration.getString("@class"));
        config.put("apikey", subnodeConfiguration.getString("@apikey"));
        config.put("recordTag", subnodeConfiguration.getString("@recordTag"));
        config.put("maxRecords", subnodeConfiguration.getString("@maxRecords"));

        return config;
    }

    /**
     * Gets the marc metadata mappings.
     *
     * @param docStructTypeName the name of a document struct type (not required)
     * @return a map of all marc mappings
     */
    public Map<String, MarcMapping> getMappings(String docStructTypeName) {
        Map<String, MarcMapping> mappings = new HashMap<>();

        // Default mappings
        mapFields(xmlConfiguration.configurationsAt("mapping/metadata"), mappings, true);

        // Particular mappings for a certain docStructType
        if (docStructTypeName != null) {
            mapFields(xmlConfiguration.configurationsAt("mapping/metadata[@docStructType='" + docStructTypeName + "']"),
                    mappings,
                    false);
        }

        return mappings;
    }

    private void mapFields(List<SubnodeConfiguration> searchFieldConfigs, Map<String, MarcMapping> mappings, boolean defaultMappings) {
        for (SubnodeConfiguration mapping : searchFieldConfigs) {
            if (defaultMappings && StringUtils.isNotBlank(mapping.getString("@docStructType"))) {
                continue;
            }
            MarcMapping marcMapping = new MarcMapping();
            marcMapping.setField(mapping.getString("field"));
            marcMapping.setMatching(mapping.getString("matching"));
            marcMapping.setAddToPhysDocStruct(StringUtils.equals("true", mapping.getString("addToPhysDocStruct")));
            marcMapping.setAuthority(mapping.getString("authority"));
            marcMapping.setAuthorityExclusive(StringUtils.equals(mapping.getString("authority/@exclusive"), "true"));

            for (Object marcField : mapping.configurationsAt("marcfield")) {
                marcMapping.addMarcField(((SubnodeConfiguration) marcField).getString(""));
            }
            for (Object altMarcField : mapping.configurationsAt("altMarcfield")) {
                marcMapping.addAltMarcField(((SubnodeConfiguration) altMarcField).getString(""));
            }
            for (Object addSubfield : mapping.configurationsAt("addSubfield")) {
                String subfield = ((SubnodeConfiguration) addSubfield).getString("");
                String separator =  ((SubnodeConfiguration) addSubfield).getString("@separator");
                if (separator == null) {
                    separator = DEFAULT_ADD_SUBFIELD_SEPARATOR;
                }
                // The attribute with separator will automatically be trimmed by the Apache Configurations.
                // As a fix to be able to have spaces in the separators, we allow the separator to be surrounded by ' and delete them
                if (separator.matches("'.*?'")) {
                    separator = separator.replaceFirst("'(.*?)'", "$1");
                }
                marcMapping.getAddSubfields().put(subfield, separator);
            }
            for (Object replace : mapping.configurationsAt("replace")) {
                marcMapping.getReplacements().put(
                        ((SubnodeConfiguration) replace).getString("pattern"),
                        ((SubnodeConfiguration) replace).getString("replacement")
                );
            }

            mappings.put(marcMapping.getField(), marcMapping);
        }
    }

    /**
     * Gets all document type configurations.
     *
     * @return a map with the configurations
     */
    public Map<String, Map<String, String>> getDocTypes() {
        Map<String, Map<String, String>> docTypes = new HashMap<>();
        List<SubnodeConfiguration> docTypeConfigs = xmlConfiguration.configurationsAt("doctypes/type");

        for (SubnodeConfiguration docTypeConfig : docTypeConfigs) {
            Map<String, String> typeMap = new HashMap<>();
            String type = docTypeConfig.getString("@rulesetType");
            if (StringUtils.isBlank(type)) {
                logger.warn("Misconfigured config file; doctype missing required attribute rulesetType. Skipping.");
                continue;
            }
            typeMap.put("rulesetType", docTypeConfig.getString("@rulesetType"));
            typeMap.put("physDocStructType", docTypeConfig.getString("@physDocStructType"));
            typeMap.put("childRulesetType", docTypeConfig.getString("@childRulesetType"));
            typeMap.put("isMultiVolume", docTypeConfig.getString("@isMultiVolume"));
            typeMap.put("isPeriodical", docTypeConfig.getString("@isPeriodical"));
            typeMap.put("title", docTypeConfig.getString("@title"));
            typeMap.put("marcLeader", docTypeConfig.getString("marcLeader"));
            typeMap.put("notContainingField", docTypeConfig.getString("notContainingField"));
            typeMap.put("parentField", docTypeConfig.getString("parentField"));
            typeMap.put("parentFieldFilter", docTypeConfig.getString("parentFieldFilter"));
            docTypes.put(type, typeMap);
        }

        return docTypes;
    }

    /**
     * Gets all authority configurations.
     *
     * @return a map with configs
     */
    public Map<String, Map<String, String>> getAuthorities() {
        Map<String, Map<String, String>> authorities = new HashMap<>();
        List<SubnodeConfiguration> authConfigs = xmlConfiguration.configurationsAt("authorities/authority");

        for (SubnodeConfiguration authConfig : authConfigs) {
            String authority = authConfig.getString("@id");
            if (StringUtils.isBlank(authority)) {
                logger.warn("Misconfigured config file; authority missing required attribute id. Skipping.");
                continue;
            }
            Map<String, String> authMap = new HashMap<>();
            authMap.put("id", authority);
            authMap.put("uri", authConfig.getString("uri"));
            authMap.put("prefix", authConfig.getString("prefix"));
            authMap.put("valueSubfield", authConfig.getString("valueSubfield"));
            authMap.put("idSubfield", authConfig.getString("idSubfield"));
            authorities.put(authority, authMap);
        }

        return authorities;
    }
}
