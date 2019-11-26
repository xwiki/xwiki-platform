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
package com.xpn.xwiki.web;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Validate {@link XWikiServletRequestStub}.
 * 
 * @version $Id$
 */
public class XWikiServletRequestStubTest
{
    @Test
    public void copy() throws MalformedURLException
    {
        URL requestURL = new URL("http://host:42/contextPath/path");
        String contextPath = "contextPath";
        Map<String, String[]> requestParameters = new HashMap<>();
        requestParameters.put("param1", new String[] { "value12", "value12" });
        requestParameters.put("param2", new String[] { "value22", "value22" });

        XWikiServletRequestStub request = new XWikiServletRequestStub(requestURL, contextPath, requestParameters);
        request.setDaemon(false);

        XWikiServletRequestStub copiedRequest = new XWikiServletRequestStub(request);

        assertEquals(requestURL.toString(), copiedRequest.getRequestURL().toString());
        assertEquals(contextPath, copiedRequest.getContextPath());
        assertFalse(copiedRequest.isDaemon());
    }
}
