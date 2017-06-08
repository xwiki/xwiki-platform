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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;

/**
 * Test saving and downloading of attachments.
 * 
 * @version $Id$
 * @since 5.0M2
 */
public final class StoreTestUtils
{
    public static String getPageAsString(final String address) throws IOException
    {
        final HttpMethod ret = doPost(address, null, null);
        return new String(ret.getResponseBody(), "UTF-8");
    }

    /** Method to easily do a post request to the site. */
    public static HttpMethod doPost(final String address, final UsernamePasswordCredentials userNameAndPassword,
        final Map<String, String> parameters) throws IOException
    {
        final HttpClient client = new HttpClient();
        final PostMethod method = new PostMethod(address);

        if (userNameAndPassword != null) {
            client.getState().setCredentials(AuthScope.ANY, userNameAndPassword);
            client.getParams().setAuthenticationPreemptive(true);
        }

        if (parameters != null) {
            for (Map.Entry<String, String> e : parameters.entrySet()) {
                method.addParameter(e.getKey(), e.getValue());
            }
        }
        client.executeMethod(method);
        return method;
    }

    public static HttpMethod doUpload(final String address, final UsernamePasswordCredentials userNameAndPassword,
        final Map<String, byte[]> uploads) throws IOException
    {
        final HttpClient client = new HttpClient();
        final PostMethod method = new PostMethod(address);

        if (userNameAndPassword != null) {
            client.getState().setCredentials(AuthScope.ANY, userNameAndPassword);
            client.getParams().setAuthenticationPreemptive(true);
        }

        Part[] parts = new Part[uploads.size()];
        int i = 0;
        for (Map.Entry<String, byte[]> e : uploads.entrySet()) {
            parts[i++] = new FilePart("filepath", new ByteArrayPartSource(e.getKey(), e.getValue()));
        }
        MultipartRequestEntity entity = new MultipartRequestEntity(parts, method.getParams());
        method.setRequestEntity(entity);

        client.executeMethod(method);
        return method;
    }

    /**
     * Encodes a given string so that it may be used as a URL component. Compatable with javascript decodeURIComponent,
     * though more strict than encodeURIComponent: all characters except [a-zA-Z0-9], '.', '-', '*', '_' are converted
     * to hexadecimal, and spaces are substituted by '+'.
     * 
     * @param s
     * @since 3.2M1
     */
    public static String escapeURL(String s)
    {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // should not happen
            throw new RuntimeException(e);
        }
    }
}
