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
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

/**
 * An abstract catalogue client for shared methods.
 */
public abstract class AbstractCatalogueClient implements IMarcCatalogueClient {

    /**
     * Connects to a given url using a get method.
     *
     * @param urlString the url string
     * @param timeout timeout in ms
     * @param headers a map of http headers
     * @return an input stream with the response
     * @throws IOException by severe IO errors
     */
    protected InputStream get(String urlString, long timeout, Map<String, String> headers) throws IOException {
        GetMethod getMethod = new GetMethod(urlString);
        HttpClient httpClient = new HttpClient();
        httpClient.getParams().setParameter("http.connection.timeout", (int) timeout);
        if (headers != null) {
            for (String header : headers.keySet()) {
                getMethod.setRequestHeader(header, headers.get(header));
            }
        }
        httpClient.executeMethod(getMethod);

        return evaluateResponse(getMethod, urlString);
    }

    /**
     * Connects to a given url using a post method.
     *
     * @param urlString the url string
     * @param timeout timeout in ms
     * @param params a map of post params
     * @return an input stream with the response
     * @throws IOException by severe IO errors
     */
    protected InputStream post(String urlString, long timeout, Map<String, String> params) throws IOException {
        PostMethod postMethod = new PostMethod(urlString);
        HttpClient httpClient = new HttpClient();
        httpClient.getParams().setParameter("http.connection.timeout", (int) timeout);
        if (params != null) {
            for (String param : params.keySet()) {
                postMethod.addParameter(param, params.get(param));
            }
        }
        httpClient.executeMethod(postMethod);

        return evaluateResponse(postMethod, urlString);
    }

    private InputStream evaluateResponse(HttpMethod method, String urlString) throws IOException {
        int statusCode = method.getStatusCode();
        if (statusCode >= 300 && statusCode != 404) {
            throw new RuntimeException("Couldn't connect to url " + urlString + ". Http status code: " + statusCode);
        }
        return method.getResponseBodyAsStream();

    }
}
