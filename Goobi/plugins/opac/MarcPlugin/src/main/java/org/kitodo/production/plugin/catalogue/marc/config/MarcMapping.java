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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A class holding marc mappings.
 */
public class MarcMapping {

    /**
     * Kitodo metadata field.
     */
    private String field;

    /**
     * List of marc field codes. A marc field code is either a control field (e.g. '001')
     * or a combination of data field and subfield (e.g. '245a').
     */
    private List<String> marcFields = new ArrayList<>();

    /**
     * List of alternative marc field codes.
     */
    private List<String> altMarcFields = new ArrayList<>();

    /**
     * A filter regex: take only values matching this regex.
     */
    private String matching;

    /**
     * Flag if this field should be added to the physical docstruct as well, not just the logical docstruct.
     */
    private boolean addToPhysDocStruct;

    /**
     * An authority id (e.g. 'gnd').
     */
    private String authority;

    /**
     * Flag if only fields having this authority should be taken.
     */
    private boolean authorityExclusive;

    /**
     * A map of replacements to be performed on the field value. Key: regex, value: replacement.
     */
    private Map<String, String> replacements = new HashMap<>();

    /**
     * A map of additional subfields. Key: subfield code, value: separator.
     */
    private Map<String, String> addSubfields = new LinkedHashMap<>();


    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public List<String> getMarcFields() {
        return marcFields;
    }

    public void setMarcFields(List<String> marcFields) {
        this.marcFields = marcFields;
    }

    public void addMarcField(String marcField) {
        this.marcFields.add(marcField);
    }

    public List<String> getAltMarcFields() {
        return altMarcFields;
    }

    public void setAltMarcFields(List<String> altMarcFields) {
        this.altMarcFields = altMarcFields;
    }

    public void addAltMarcField(String marcField) {
        this.altMarcFields.add(marcField);
    }

    public String getMatching() {
        return matching;
    }

    public void setMatching(String matching) {
        this.matching = matching;
    }

    public Map<String, String> getReplacements() {
        return replacements;
    }

    public void setReplacements(Map<String, String> replacements) {
        this.replacements = replacements;
    }

    public boolean isAddToPhysDocStruct() {
        return addToPhysDocStruct;
    }

    public void setAddToPhysDocStruct(boolean addToPhysDocStruct) {
        this.addToPhysDocStruct = addToPhysDocStruct;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public boolean isAuthorityExclusive() {
        return authorityExclusive;
    }

    public void setAuthorityExclusive(boolean authorityExclusive) {
        this.authorityExclusive = authorityExclusive;
    }

    public Map<String, String> getAddSubfields() {
        return addSubfields;
    }

    public void setAddSubfields(Map<String, String> addSubfields) {
        this.addSubfields = addSubfields;
    }

}
