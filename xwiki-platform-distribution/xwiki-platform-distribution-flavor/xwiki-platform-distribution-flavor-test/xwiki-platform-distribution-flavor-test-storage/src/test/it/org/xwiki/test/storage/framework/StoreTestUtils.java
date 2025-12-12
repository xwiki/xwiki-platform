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

import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpPost;
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
    public static String getPageAsString(final String address) throws IOException
    {
        final HttpMethod ret = doPost(address, null, null);
        return new String(ret.getResponseBody(), "UTF-8");
    }

    /** Method to easily do a post request to the site. */
    public static HttpMethod doPost(final String address, final XWikiCredentials userNameAndPassword,
        final Map<String, String> parameters) throws IOException
    {
        final XWikiHTTPClient client = new XWikiHTTPClient();
        final HttpPost method = new HttpPost(address);

        if (userNameAndPassword != null) {
            client.setDefaultCredentials(userNameAndPassword);
        }

        if (parameters != null) {
            for (Map.Entry<String, String> e : parameters.entrySet()) {
                method.addParameter(e.getKey(), e.getValue());
            }
        }
        client.executeMethod(method);
        return method;
    }

    public static HttpMethod doUpload(final String address, final HTTPCredentials userNameAndPassword,
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
}
