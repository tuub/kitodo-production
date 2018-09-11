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

import java.io.InputStream;
import java.util.List;
import org.marc4j.marc.Record;

/**
 * Interface for marc xml transform.
 */
public interface IMarcRecordTransformer {

    /**
     * Takes an InputStream containing a document with marc records and transfers it to a list of marc4j Record objects.
     *
     * @param inputStream the input stream
     * @param recordTag the tag name of the marc records
     * @return a list of marc4j records
     */
    List<Record> transform(InputStream inputStream, String recordTag);

}
