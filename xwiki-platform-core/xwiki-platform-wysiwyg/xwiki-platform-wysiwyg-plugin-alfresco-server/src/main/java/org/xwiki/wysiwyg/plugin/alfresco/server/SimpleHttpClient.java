/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.wysiwyg.plugin.alfresco.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;

/**
 * A simple HTTP Client.
 * 
 * @version $Id$
 */
public class SimpleHttpClient
{
    /**
     * The interface used to handle the response.
     * 
     * @param <T> the type of object read from the response content
     */
    public static interface ResponseHandler<T>
    {
        /**
         * Read the response content stream.
         * 
         * @param content the response content stream
         * @return the object read from the response content
         */
        T read(InputStream content);
    }

    /**
     * The HTTP client used to execute the requests.
     */
    private final HttpClient httpClient;

    /**
     * Creates a new instance with the specified name.
     * 
     * @param name the name of the HTTP client
     */
    public SimpleHttpClient(String name)
    {
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));

        BasicHttpParams parameters = new BasicHttpParams();
        ThreadSafeClientConnManager connectionManager = new ThreadSafeClientConnManager(parameters, schemeRegistry);
        httpClient = new DefaultHttpClient(connectionManager, parameters);
        httpClient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, name);
    }

    /**
     * Executes a GET HTTP request on the specified URL with the specified query string parameters.
     * 
     * @param <T> the type of object read from the response content
     * @param url the target of the GET request
     * @param queryStringParameters the list of query string parameters
     * @param handler the response handler
     * @return the object read from the response content
     * @throws IOException if this method fails to send the request or to read the response
     */
    public <T> T doGet(String url, List<Entry<String, String>> queryStringParameters, ResponseHandler<T> handler)
        throws IOException
    {
        List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        for (Entry<String, String> entry : queryStringParameters) {
            parameters.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        return sendRequest(new HttpGet(url + '?' + URLEncodedUtils.format(parameters, "UTF-8")), handler);
    }

    /**
     * Executes a POST HTTP request on the specified URL with the specified content.
     * 
     * @param <T> the type of object read from the response content
     * @param url the target of the POST request
     * @param content the posted content
     * @param contentType the media type of the posted content
     * @param handler the response handler
     * @return the object read from the response content
     * @throws IOException if this method fails to send the request or to read the response
     */
    public <T> T doPost(String url, String content, String contentType, ResponseHandler<T> handler) throws IOException
    {
        HttpPost request = new HttpPost(url);
        request.setHeader("Content-type", contentType);
        try {
            request.setEntity(new StringEntity(content));
        } catch (UnsupportedEncodingException e) {
            // Shouldn't happen.
        }
        return sendRequest(request, handler);
    }

    /**
     * Sends the given HTTP request.
     * 
     * @param <T> the type of object read from the response content
     * @param request the HTTP request to be sent
     * @param handler the response handler
     * @return the object read from the response content
     * @throws IOException if this method fails to send the request or to read the response
     */
    private <T> T sendRequest(HttpUriRequest request, ResponseHandler<T> handler) throws IOException
    {
        HttpResponse response = httpClient.execute(request);
        if (response.getEntity() != null) {
            InputStream contentStream = null;
            try {
                contentStream = response.getEntity().getContent();
                return handler.read(contentStream);
            } finally {
                try {
                    contentStream.close();
                } catch (IOException e) {
                    // Ignore.
                }
            }
        }
        return null;
    }
}
