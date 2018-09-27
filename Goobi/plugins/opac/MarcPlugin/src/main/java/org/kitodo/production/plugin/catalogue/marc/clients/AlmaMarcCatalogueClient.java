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

package org.kitodo.production.plugin.catalogue.marc.clients;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.StringUtils;

/**
 * Catalogue client for Alma API.
 */
public class AlmaMarcCatalogueClient extends AbstractCatalogueClient {

    /**
     * Queries an Alma catalogue using the Alma API.
     *
     * @param urlString the URL of the catalogue interface.
     * @param params map of parameter. MUST contain an mms_id and a apikey.
     * @param timeout timeout in ms
     * @return an input stream with the response.
     */
    @Override
    public InputStream query(String urlString, Map<String, String> params, long timeout) throws IOException {
        if (params == null || params.get("query") == null || params.get("apikey") == null) {
            throw new RuntimeException("The params map must contain a query and an apikey, Map content: " + params);
        }

        String mmsId = StringUtils.substringAfter(params.get("query"), ":");
        if (urlString.contains("{mms_id}")) {
            urlString = urlString.replace("{mms_id}", mmsId);
        } else {
            urlString += mmsId;
        }

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "apikey " + params.get("apikey"));

        return get(urlString, timeout, headers);
    }
}
