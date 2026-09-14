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
package org.xwiki.test.storage.framework;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.xwiki.http.internal.XWikiCredentials;
import org.xwiki.http.internal.XWikiHTTPClient;

/**
 * Test saving and downloading of attachments.
 * 
 * @version $Id$
 * @since 5.0M2
 */
public final class StoreTestUtils
{
    /**
     * The status code and the body of a response, both read before the connection is released, so that tests can
     * assert on them without having to manage the connection themselves.
     *
     * @param code the status code of the response
     * @param body the body of the response
     */
    public record Response(int code, byte[] body)
    {
        /**
         * @return the body of the response as an UTF-8 string
         */
        public String bodyAsString()
        {
            return new String(this.body, StandardCharsets.UTF_8);
        }
    }

    public static String getPageAsString(final String address) throws IOException
    {
        return doPost(address, null, null).bodyAsString();
    }

    /** Method to easily do a post request to the site. */
    public static Response doPost(final String address, final XWikiCredentials userNameAndPassword,
        final Map<String, String> parameters) throws IOException
    {
        final HttpPost method = new HttpPost(address);

        if (parameters != null) {
            List<NameValuePair> formParameters = new ArrayList<>(parameters.size());
            for (Map.Entry<String, String> e : parameters.entrySet()) {
                formParameters.add(new BasicNameValuePair(e.getKey(), e.getValue()));
            }
            method.setEntity(new UrlEncodedFormEntity(formParameters, StandardCharsets.UTF_8));
        }

        return execute(method, userNameAndPassword);
    }

    public static Response doUpload(final String address, final XWikiCredentials userNameAndPassword,
        final Map<String, byte[]> uploads) throws IOException
    {
        final HttpPost method = new HttpPost(address);

        MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
        for (Map.Entry<String, byte[]> e : uploads.entrySet()) {
            entityBuilder.addBinaryBody("filepath", e.getValue(), ContentType.DEFAULT_BINARY, e.getKey());
        }
        method.setEntity(entityBuilder.build());

        return execute(method, userNameAndPassword);
    }

    private static Response execute(ClassicHttpRequest request, XWikiCredentials credentials) throws IOException
    {
        try (XWikiHTTPClient client = new XWikiHTTPClient()) {
            return client.execute(request, credentials, (response, context) -> new Response(response.getCode(),
                response.getEntity() != null ? EntityUtils.toByteArray(response.getEntity()) : new byte[0]));
        }
    }
}
