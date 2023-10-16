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
package org.xwiki.export.pdf.job;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PDFExportJobRequest}.
 * 
 * @version $Id$
 */
class PDFExportJobRequestTest
{
    private PDFExportJobRequest request = new PDFExportJobRequest();

    @Test
    void setBaseURL() throws Exception
    {
        assertNull(this.request.getBaseURL());
        assertNull(this.request.getContext());

        // Without a context set.
        URL baseURL = new URL("http://www.xwiki.org");
        this.request.setBaseURL(baseURL);

        assertEquals(baseURL, this.request.getBaseURL());
        assertNull(this.request.getContext());

        // With a context set.
        baseURL = new URL("http://localhost:8080/xwiki/bin/view/Some/Page?color=blue&sheet=Some.Sheet&color=red&bar#foo");
        this.request.setContext(new HashMap<>());
        this.request.setBaseURL(baseURL);

        assertEquals(baseURL, this.request.getBaseURL());
        assertEquals(baseURL, this.request.getContext().get("request.url"));
        assertEquals("Some.Sheet", this.request.getContext().get("sheet"));

        Map<String, String[]> expectedParameters = new HashMap<>();
        expectedParameters.put("color", new String[] {"blue", "red"});
        expectedParameters.put("sheet", new String[] {"Some.Sheet"});
        expectedParameters.put("bar", new String[] {null});
        Map<String, String[]> actualParameters =
            (Map<String, String[]>) this.request.getContext().get("request.parameters");

        assertEquals(expectedParameters.keySet(), actualParameters.keySet());
        for (String key : expectedParameters.keySet()) {
            assertArrayEquals(expectedParameters.get(key), actualParameters.get(key));
        }

        // Without query string and fragmenmt identifier.
        baseURL = new URL("http://localhost:8080/xwiki/bin/view/Some/Page");
        this.request.setBaseURL(baseURL);

        assertEquals(baseURL, this.request.getBaseURL());
        assertEquals(baseURL, this.request.getContext().get("request.url"));
        actualParameters = (Map<String, String[]>) this.request.getContext().get("request.parameters");
        assertTrue(actualParameters.isEmpty());

        // Null base URL.
        this.request.setBaseURL(null);
        assertNull(this.request.getBaseURL());
        assertTrue(this.request.getContext().isEmpty());
    }
}
