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

package org.kitodo.production.plugin.catalogue.marc.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;

/**
 * Utilities for Marc4j.
 */
public class Marc4jUtil {

    private static final Logger logger = Logger.getLogger(Marc4jUtil.class);

    /**
     * Gets all marc record field values matching a given tag code.
     *
     * @param record a marc record
     * @param tagCodes a list of a mard field codes.
     * @return a list of values
     */
    public static List<Map<String, String>> getMarcFieldValues(Record record, List<String> tagCodes) {
        return getMarcFieldValues(record, tagCodes, null, null, false);
    }

    /**
     * Gets all marc record field values and possibly additional subfields and corresponding authority values matching a given tag code.
     *
     * @param record a marc record
     * @param tagCodes a list of a mard field codes.
     * @param authority a map with authority configurations, may be null
     * @param authExclusive true if only values with a certain authority should be taken
     * @return a list of values
     */
    public static List<Map<String, String>> getMarcFieldValues(
            Record record,
            List<String> tagCodes,
            Map<String, String> addSubfields,
            Map<String, String> authority,
            boolean authExclusive) {

        List<Map<String, String>> values = new ArrayList<>();

        for (String tagCode : tagCodes) {
            if (StringUtils.isBlank(tagCode)) {
                return values;
            }

            // Control field
            if (tagCode.matches("00\\d")) {
                ControlField controlField = (ControlField) record.getVariableField(tagCode);
                if (controlField != null) {
                    values.add(createMapWithValue(controlField.getData()));
                }
                return values;
            }

            // Data field
            if (StringUtils.length(tagCode) != 4) {
                logger.warn("Invalid marc tag code in the configuration: " + tagCode);
                return values;
            }
            List<DataField> fields = record.getVariableFields(tagCode.substring(0, 3));
            for (DataField field : fields) {

                Subfield subfield = field.getSubfield(tagCode.charAt(3));
                if (subfield != null) {
                    Map<String, String> valueMap = createMapWithValue(subfield.getData());

                    // Add values of additional subfields
                    if (addSubfields != null) {
                        for (String addSubfieldCode : addSubfields.keySet()) {
                            if (StringUtils.length(addSubfieldCode) < 1) {
                                continue;
                            }
                            Subfield addSubfield = field.getSubfield(addSubfieldCode.charAt(0));
                            if (addSubfield != null) {
                                valueMap.put("value", valueMap.get("value") + addSubfields.get(addSubfieldCode) + addSubfield.getData());
                            }
                        }
                    }

                    // Check authority subfield
                    if (authority != null) {
                        if (authExclusive && StringUtils.isNotBlank(authority.get("idSubfield"))) {
                            Subfield authIdSubfield = field.getSubfield(authority.get("idSubfield").charAt(0));
                            if (authIdSubfield == null || !StringUtils.equals(authIdSubfield.getData(), authority.get("id"))) {
                                continue;
                            }
                        }
                        char auth = '0'; // Default
                        if (StringUtils.isNotBlank(authority.get("valueSubfield"))) {
                            auth = authority.get("valueSubfield").charAt(0);
                        }
                        Subfield authSubfield = field.getSubfield(auth);
                        if (authSubfield != null && authSubfield.getData().startsWith(authority.get("prefix"))) {
                            valueMap.put("authValue", authSubfield.getData().replace(authority.get("prefix"), ""));
                            valueMap.put("authId", authority.get("id"));
                            valueMap.put("authUri", authority.get("uri"));
                        }
                    }
                    values.add(valueMap);
                }
            }
        }
        return values;
    }

    private static Map<String, String> createMapWithValue(String value) {
        Map<String, String> map = new HashMap<>();
        map.put("value", value);
        return map;
    }

}
