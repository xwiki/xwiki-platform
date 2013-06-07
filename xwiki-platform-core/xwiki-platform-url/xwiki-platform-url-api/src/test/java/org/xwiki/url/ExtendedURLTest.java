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
package org.xwiki.url;

import java.net.URI;
import java.net.URL;

import org.junit.Test;
import org.xwiki.url.internal.ExtendedURL;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link org.xwiki.url.internal.ExtendedURL}.
 *
 * @version $Id$
 * @since 5.1M1
 */
public class ExtendedURLTest
{
    @Test
    public void withoutPrefix() throws Exception
    {
        URL url = new URL("http://localhost:8080/some/path");
        ExtendedURL extendedURL = new ExtendedURL(url);
        assertEquals(new URI("http://localhost:8080/some/path"), extendedURL.getURI());
        assertEquals(2, extendedURL.getSegments().size());
        assertThat(extendedURL.getSegments(), hasItems("some", "path"));
    }

    @Test
    public void withPrefix() throws Exception
    {
        URL url = new URL("http://localhost:8080/xwiki/path");
        ExtendedURL extendedURL = new ExtendedURL(url, "xwiki");
        assertEquals(new URI("http://localhost:8080/xwiki/path"), extendedURL.getURI());
        assertEquals(1, extendedURL.getSegments().size());
        assertThat(extendedURL.getSegments(), hasItems("path"));
    }

    @Test
    public void withPrefixStartingWithSlash() throws Exception
    {
        URL url = new URL("http://localhost:8080/xwiki/path");
        ExtendedURL extendedURL = new ExtendedURL(url, "/xwiki");
        assertEquals(new URI("http://localhost:8080/xwiki/path"), extendedURL.getURI());
        assertEquals(1, extendedURL.getSegments().size());
        assertThat(extendedURL.getSegments(), hasItems("path"));
    }

    @Test
    public void withInvalidPrefix() throws Exception
    {
        URL url = new URL("http://localhost:8080/some/path");
        try {
            new ExtendedURL(url, "/xwiki");
            fail("Should have thrown an exception here");
        } catch (URLCreationException e) {
            assertEquals("URL Path [/some/path] doesn't start with [/xwiki]", e.getMessage());
        }
    }

    @Test
    public void equality() throws Exception
    {
        ExtendedURL extendedURL1 = new ExtendedURL(new URL("http://localhost:8080/some/path"));
        ExtendedURL extendedURL2 = new ExtendedURL(new URL("http://localhost:8080/some/path"));
        assertEquals(extendedURL1, extendedURL2);
    }

    @Test
    public void invalidURL() throws Exception
    {
        try {
            // Invalid URL since the space in the page name isn't encoded.
            new ExtendedURL(new URL("http://host/xwiki/bin/view/space/page name"));
            fail("Should have thrown an exception here");
        } catch (URLCreationException expected) {
            assertEquals("Invalid URL [http://host/xwiki/bin/view/space/page name]", expected.getMessage());
        }
    }

    @Test
    public void withEncodedChars() throws Exception
    {
        ExtendedURL extendedURL = new ExtendedURL(new URL("http://host/xwiki/bin/view/space/page%20name?param=%2D"));
        assertThat(extendedURL.getSegments(), hasItem("page name"));
        assertEquals("param=-", extendedURL.getURI().getQuery());
    }

    /**
     * Verify we ignore path parameters (see section 3.3 of the RFC 2396: http://www.faqs.org/rfcs/rfc2396.html).
     */
    @Test
    public void ignoreSpecialPathParameters() throws Exception
    {
        ExtendedURL extendedURL = new ExtendedURL(new URL(
            "http://host/xwiki/bin/view/space;param1=value1/page;param2=value2"));
        assertThat(extendedURL.getSegments(), hasItem("space"));
        assertThat(extendedURL.getSegments(), hasItem("page"));

        // Ensure we don't remove ";" when they are encoded in order to allow the ";" character to be in document names
        // for example.
        extendedURL = new ExtendedURL(new URL("http://host/xwiki/bin/view/space/my%3Bpage"));
        assertThat(extendedURL.getSegments(), hasItem("my;page"));
    }
}
