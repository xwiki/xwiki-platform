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
package org.xwiki.url.internal.standard;

import java.net.URL;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.url.internal.ExtendedURL;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DefaultWikiReferenceExtractor}.
 *
 * @version $Id$
 * @since 5.1M1
 */
public class DefautWikiReferenceExtractorTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultWikiReferenceExtractor> mocker =
        new MockitoComponentMockingRule(DefaultWikiReferenceExtractor.class);

    @Test
    public void extractWhenDomainBased() throws Exception
    {
        StandardURLConfiguration urlConfiguration = mocker.getInstance(StandardURLConfiguration.class);
        when(urlConfiguration.isPathBasedMultiWiki()).thenReturn(false);

        WikiReferenceResolver domainBasedResolver = mocker.getInstance(WikiReferenceResolver.class, "domain");
        when(domainBasedResolver.resolve("wiki.server.com")).thenReturn(new WikiReference("wiki"));

        ExtendedURL url = new ExtendedURL(new URL("http://wiki.server.com/xwiki/bin/view/Main/WebHome"), "xwiki");
        Pair<WikiReference, Boolean> extractionResult = this.mocker.getComponentUnderTest().extract(url);
        assertEquals(new WikiReference("wiki"), extractionResult.getLeft());
        assertFalse(extractionResult.getRight());
    }

    @Test
    public void extractWhenPathBased() throws Exception
    {
        StandardURLConfiguration urlConfiguration = mocker.getInstance(StandardURLConfiguration.class);
        when(urlConfiguration.isPathBasedMultiWiki()).thenReturn(true);
        when(urlConfiguration.getWikiPathPrefix()).thenReturn("wiki");

        WikiReferenceResolver pathBasedResolver = mocker.getInstance(WikiReferenceResolver.class, "path");
        when(pathBasedResolver.resolve("someWiki")).thenReturn(new WikiReference("somewiki"));

        ExtendedURL url = new ExtendedURL(new URL("http://localhost/xwiki/wiki/someWiki/view/Main/WebHome"), "xwiki");
        Pair<WikiReference, Boolean> extractionResult = this.mocker.getComponentUnderTest().extract(url);
        assertEquals(new WikiReference("somewiki"), extractionResult.getLeft());
        assertTrue(extractionResult.getRight());
    }

    @Test
    public void extractWhenPathBasedButWithDomainBasedURL() throws Exception
    {
        StandardURLConfiguration urlConfiguration = mocker.getInstance(StandardURLConfiguration.class);
        when(urlConfiguration.isPathBasedMultiWiki()).thenReturn(true);

        WikiReferenceResolver domainBasedResolver = mocker.getInstance(WikiReferenceResolver.class, "domain");
        when(domainBasedResolver.resolve("wiki.server.com")).thenReturn(new WikiReference("wiki"));

        ExtendedURL url = new ExtendedURL(new URL("http://wiki.server.com/xwiki/bin/view/Main/WebHome"), "xwiki");
        Pair<WikiReference, Boolean> extractionResult = this.mocker.getComponentUnderTest().extract(url);
        assertEquals(new WikiReference("wiki"), extractionResult.getLeft());
        assertFalse(extractionResult.getRight());
    }
}
