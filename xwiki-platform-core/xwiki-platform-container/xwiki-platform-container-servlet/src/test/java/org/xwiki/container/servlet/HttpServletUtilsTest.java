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
package org.xwiki.container.servlet;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Validate {@link HttpServletUtils}.
 * 
 * @version $Id$
 */
public class HttpServletUtilsTest
{
    private HttpServletRequest request(String urlString, Map<String, String>... headerGroup)
        throws MalformedURLException
    {
        URL url = new URL(urlString);

        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getScheme()).thenReturn(url.getProtocol());
        when(request.getServerName()).thenReturn(url.getHost());
        when(request.getServerPort()).thenReturn(url.getPort());

        for (Map<String, String> headers : headerGroup) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                when(request.getHeader(entry.getKey())).thenReturn(entry.getValue());
            }
        }

        return request;
    }

    private void assertSourceBaseURL(String expected, String input, Map<String, String>... headers)
        throws MalformedURLException
    {
        assertEquals(expected, HttpServletUtils.getSourceBaseURL(request(input, headers)).toString());
    }

    private Map<String, String> forwarded(String forwarded)
    {
        Map<String, String> headers = new HashMap<>();

        headers.put(HttpServletUtils.HEADER_FORWARDED, forwarded);

        return headers;
    }

    private Map<String, String> xhost(String host)
    {
        Map<String, String> headers = new HashMap<>();

        headers.put(HttpServletUtils.HEADER_X_FORWARDED_HOST, host);

        return headers;
    }

    private Map<String, String> xproto(String proto)
    {
        Map<String, String> headers = new HashMap<>();

        headers.put(HttpServletUtils.HEADER_X_FORWARDED_PROTO, proto);

        return headers;
    }

    // Tests

    @Test
    public void getSourceBaseURL() throws MalformedURLException
    {
        assertSourceBaseURL("http://host:8080", "http://host:8080");
        assertSourceBaseURL("http://host", "http://host");
        assertSourceBaseURL("https://host", "https://host");
        assertSourceBaseURL("https://host:80", "https://host:80");

        assertSourceBaseURL("http://host:8080", "http://host:8080", forwarded(""));
        assertSourceBaseURL("http://host:8080", "http://host:8080", forwarded(" "));
        assertSourceBaseURL("https://sourcehost", "http://host:8080", forwarded("proto=https; host=sourcehost"));
        assertSourceBaseURL("https://sourcehost", "http://host:8080", forwarded(" proto = https ; host = sourcehost "));
        assertSourceBaseURL("https://sourcehost", "http://host:8080",
            forwarded("proto=https, proto=http; host=sourcehost, host=sourcehost2"));
        assertSourceBaseURL("https://host:8080", "http://host:8080", forwarded(" proto = https "));

        assertSourceBaseURL("http://host:8080", "http://host:8080", xhost(""));
        assertSourceBaseURL("http://host:8080", "http://host:8080", xhost(" "));
        assertSourceBaseURL("http://sourcehost", "http://host:8080", xhost("sourcehost"));
        assertSourceBaseURL("http://sourcehost:98", "http://host:8080", xhost("sourcehost:98"));
        assertSourceBaseURL("http://sourcehost:98", "http://host:8080", xhost("sourcehost:98,sourcehost2"));
        assertSourceBaseURL("http://sourcehost:98", "http://host:8080", xhost(" sourcehost:98 ,sourcehost2"));
        assertSourceBaseURL("https://host:8080", "http://host:8080", xproto("https"));
        assertSourceBaseURL("https://host:8080", "http://host:8080", xproto(" https "));
        assertSourceBaseURL("https://sourcehost", "http://host:8080", xhost("sourcehost"), xproto("https"));

        assertSourceBaseURL("https://sourcehost", "http://host:8080", forwarded("proto=https; host=sourcehost"),
            xhost("sourcehost2"), xproto("http"));
        assertSourceBaseURL("https://sourcehost", "http://host:8080", forwarded("host=sourcehost"),
            xhost("sourcehost2"), xproto("https"));
    }
}
