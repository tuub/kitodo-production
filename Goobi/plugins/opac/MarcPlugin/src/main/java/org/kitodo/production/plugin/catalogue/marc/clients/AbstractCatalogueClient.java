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
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

/**
 * An abstract catalogue client for shared methods.
 */
public abstract class AbstractCatalogueClient implements IMarcCatalogueClient {

    /**
     * Connects to a given url.
     * @param urlString the url string
     * @param timeout timeout in ms
     * @return an input stream with the response
     * @throws IOException by severe IO errors
     */
    protected InputStream connect(String urlString, long timeout, Map<String, String> headers) throws IOException {
        GetMethod getMethod = new GetMethod(urlString);
        HttpClient httpClient = new HttpClient();
        httpClient.getParams().setParameter("http.connection.timeout", (int) timeout);
        if (headers != null) {
            for (String header : headers.keySet()) {
                getMethod.setRequestHeader(header, headers.get(header));
            }
        }
        httpClient.executeMethod(getMethod);

        int statusCode = getMethod.getStatusCode();
        if (statusCode >= 500) {
            throw new RuntimeException("Couldn't connect to url " + urlString + ". Http status code: " + statusCode);
        }
        return getMethod.getResponseBodyAsStream();
    }
}
