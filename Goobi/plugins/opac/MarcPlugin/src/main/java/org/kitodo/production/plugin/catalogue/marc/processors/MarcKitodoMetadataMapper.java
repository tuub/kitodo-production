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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.PatternSyntaxException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.kitodo.production.plugin.catalogue.marc.config.MarcMapping;
import org.kitodo.production.plugin.catalogue.marc.util.Marc4jUtil;
import org.marc4j.marc.Record;

/**
 * Maps fields from a marc records to Kitodo metadata.
 */
public class MarcKitodoMetadataMapper implements IMarcMetadataMapper {

    /**
     * Takes a marc4j record and maps it to Kitodo metadata fields.
     *
     * @param marcRecord   the marc record
     * @param marcMappings a map of marc mappings
     * @param authorityConfig a map with authority data, may be null
     * @return a map with the metadata
     */
    @Override
    public Map<String, List<Map<String, String>>> map(
            Record marcRecord,
            Map<String, MarcMapping> marcMappings,
            Map<String, Map<String, String>> authorityConfig) {

        Map<String, List<Map<String, String>>> metadata = new HashMap<>();

        // Go through all entries in the marcMappings and add metadata if found.
        for (String metadataField : marcMappings.keySet()) {
            MarcMapping mapping = marcMappings.get(metadataField);
            List<Map<String, String>> filteredValues = new ArrayList<>();

            Map<String, String> authority = null;
            if (mapping.getAuthority() != null) {
                authority = authorityConfig.get(mapping.getAuthority());
            }

            List<Map<String, String>> marcValues = Marc4jUtil.getMarcFieldValues(
                    marcRecord,
                    mapping.getMarcFields(),
                    authority,
                    mapping.isAuthorityExclusive());

            if (CollectionUtils.isEmpty(marcValues) && CollectionUtils.isNotEmpty(mapping.getAltMarcFields())) {
                marcValues = Marc4jUtil.getMarcFieldValues(
                        marcRecord,
                        mapping.getAltMarcFields(),
                        authority,
                        mapping.isAuthorityExclusive());
            }

            for (Map<String, String> valueMap : marcValues) {
                String value = valueMap.get("value");
                if (value == null) {
                    continue;
                }

                // If there is a matching criteria and the value doesn't match, continue
                if (mapping.getMatching() != null && !value.matches(mapping.getMatching())) {
                    continue;
                }

                // Filter the value with all replacement criteria
                for (String replacementRegex : mapping.getReplacements().keySet()) {
                    try {
                        value = value.replaceAll(replacementRegex, mapping.getReplacements().get(replacementRegex));
                    } catch (PatternSyntaxException e) {
                        System.err.println("Pattern " + replacementRegex + " not valid: " + e);
                    }
                }
                value = StringUtils.trim(value);

                if (StringUtils.isNotBlank(value)) {
                    valueMap.put("value", value);
                    filteredValues.add(valueMap);
                }
            }
            if (CollectionUtils.isNotEmpty(filteredValues)) {
                metadata.put(metadataField, filteredValues);
            }
        }

        return metadata;
    }



}
