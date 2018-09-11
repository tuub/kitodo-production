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

import java.util.Arrays;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.kitodo.production.plugin.catalogue.marc.util.Marc4jUtil;
import org.marc4j.marc.Record;

/**
 * Implementation of the marc document type mapper.
 */
public class MarcDocTypeMapper implements IMarcDocTypeMapper {

    /**
     * Maps a marc record to a Kitodo document type.
     *
     * @param marcRecord the marc record
     * @param docTypes configured document types
     * @return the configurations of the mapped document type or null if none found
     */
    public Map<String, String> mapDocType(Record marcRecord, Map<String, Map<String, String>> docTypes) {
        for (String docTypeName : docTypes.keySet()) {
            Map<String, String> docType = docTypes.get(docTypeName);

            String leader = marcRecord.getLeader().marshal();
            if (leader == null || leader.length() < 8 || !leader.substring(6,8).matches(docType.get("marcLeader"))) {
                continue;
            }

            String notContainingField = docType.get("notContainingField");
            if (StringUtils.isNotBlank(notContainingField)
                    && CollectionUtils.isNotEmpty(Marc4jUtil.getMarcFieldValues(marcRecord, Arrays.asList(notContainingField)))) {
                continue;
            }

            String parentField = docType.get("parentField");
            if (StringUtils.isNotBlank(parentField)
                    && CollectionUtils.isEmpty(Marc4jUtil.getMarcFieldValues(marcRecord, Arrays.asList(parentField)))) {
                continue;
            }
            return docType;
        }
        return null;
    }
}
