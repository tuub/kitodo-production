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

import java.util.List;
import java.util.Map;
import org.kitodo.production.plugin.catalogue.marc.config.MarcMapping;
import org.marc4j.marc.Record;

/**
 * Interface for mapping marc records to Kitodo metadata.
 */
public interface IMarcMetadataMapper {

    /**
     * Takes a marc4j record and maps it to Kitodo metadata fields.
     *
     * @param marcRecord the marc record
     * @param marcMappings a map of marc mappings
     * @param authorityConfig a map with authority data, may be null
     * @return a DigitalDocument with the metadata
     */
    Map<String, List<Map<String, String>>> map(
            Record marcRecord, Map<String,
            MarcMapping> marcMappings,
            Map<String, Map<String, String>> authorityConfig);

}
