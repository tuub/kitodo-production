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
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Map;
import org.apache.commons.lang.StringUtils;

/**
 * Catalogue Client querying an OAI-PMH server.
 */
public class SruMarcCatalogueClient extends AbstractCatalogueClient {

    private static final String DEFAULT_MAX_RECORDS = "10";

    /**
     * Queries a catalogue using the given url and params.
     *
     * @param urlString the url of the catalogue.
     * @param params    query params, MUST contain an identifier.
     * @param timeout timeout in ms
     * @return an InputStream with the response.
     */
    @Override
    public InputStream query(String urlString, Map<String, String> params, long timeout) throws IOException {

        if (params == null || params.get("query") == null ) {
            throw new RuntimeException("The params map must contain a query. Map content: " + params);
        }

        String query = StringUtils.substringBefore(params.get("query"), ":")
                + "="
                + StringUtils.substringAfter(params.get("query"), ":");
        //query = URLEncoder.encode(query, Charset.forName("UTF-8"));

        String maxRecords = DEFAULT_MAX_RECORDS;
        if (params.get("maxRecords") != null) {
            maxRecords = params.get("maxRecords");
        }
        urlString = urlString.replace("{maxRecords}", maxRecords);

        urlString = urlString.replace("{query}", query);

        return connect(urlString, timeout, null);
    }
}
