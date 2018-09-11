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
import java.util.Map;
import org.apache.commons.lang.StringUtils;

/**
 * Catalogue Client querying an OAI-PMH server.
 */
public class OaiMarcCatalogueClient extends AbstractCatalogueClient {
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

        String identifier = StringUtils.substringAfter(params.get("query"), ":");
        /*
         * If the url contains a question mark, assume all params are contained in the url string.
         */
        if (urlString.contains("?")) {
            if (urlString.contains("{identifier}")) {
                urlString = urlString.replace("{identifier}", identifier);
            } else {
                urlString += params.get("identifier");
            }
        } else {
            urlString += "?verb=GetRecord&metadataPrefix=marc21&identifier=" + identifier;
        }

        return connect(urlString, timeout, null);
    }
}
