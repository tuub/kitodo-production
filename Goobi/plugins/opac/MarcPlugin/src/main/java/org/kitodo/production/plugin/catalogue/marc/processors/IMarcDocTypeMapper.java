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

import java.util.Map;
import org.marc4j.marc.Record;

/**
 * Interface for mapping marc records to Kitodo document types.
 */
public interface IMarcDocTypeMapper {

    /**
     * Maps a marc record to a Kitodo document type.
     *
     * @param marcRecord the marc record
     * @param docTypes configured document types
     * @return the configurations of the mapped document type or null if none found
     */
    Map<String, String> mapDocType(Record marcRecord, Map<String, Map<String, String>> docTypes);
}
