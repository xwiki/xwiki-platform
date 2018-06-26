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
package org.xwiki.icon.internal;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.icon.IconException;
import org.xwiki.icon.IconSet;
import org.xwiki.icon.IconType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link org.xwiki.icon.internal.DefaultIconSetLoader}.
 *
 * @version $Id$
 * @since 6.2M1
 */
public class DefaultIconSetLoaderTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultIconSetLoader> mocker =
        new MockitoComponentMockingRule<>(DefaultIconSetLoader.class);

    private DocumentAccessBridge documentAccessBridge;

    private WikiDescriptorManager wikiDescriptorManager;

    @Before
    public void setUp() throws Exception
    {
        documentAccessBridge = mocker.getInstance(DocumentAccessBridge.class);
        wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("wikiId");
    }

    private void verifies(IconSet result) throws Exception
    {
        assertNotNull(result);
        assertEquals("http://url_to_css", result.getCss());
        assertEquals("IconThemes.Default", result.getSsx());
        assertEquals("IconThemes.JS", result.getJsx());
        assertEquals("{{html clean=\"false\"}}<span class=\"fa fa-$icon\"></span>{{/html}}", result.getRenderWiki());
        assertEquals("<span class=\"fa fa-$icon\"></span>", result.getRenderHTML());
        assertEquals("fa fa-$icon", result.getRenderCustom());
        assertEquals("anchor", result.getIcon("transmit").getValue());
        assertEquals("globe", result.getIcon("earth").getValue());
        assertEquals(IconType.FONT, result.getType());
    }

    @Test
    public void loadIconSet() throws Exception
    {
        Reader content = new InputStreamReader(getClass().getResourceAsStream("/test.iconset"));

        // Test
        IconSet result = mocker.getComponentUnderTest().loadIconSet(content, "FontAwesome");

        // Verify
        verifies(result);
        assertEquals("FontAwesome", result.getName());
    }

    @Test
    public void loadIconSetFromWikiDocument() throws Exception
    {
        DocumentReference iconSetRef = new DocumentReference("xwiki", "IconThemes", "Default");
        DocumentReference iconClassRef = new DocumentReference("wikiId", "IconThemesCode", "IconThemeClass");
        when(documentAccessBridge.getProperty(eq(iconSetRef), eq(iconClassRef), eq("name"))).thenReturn("MyIconTheme");
        DocumentModelBridge doc = mock(DocumentModelBridge.class);
        when(documentAccessBridge.getDocumentInstance(iconSetRef)).thenReturn(doc);

        StringWriter content = new StringWriter();
        IOUtils.copyLarge(new InputStreamReader(getClass().getResourceAsStream("/test.iconset")), content);
        when(doc.getContent()).thenReturn(content.toString());

        // Test
        IconSet result = mocker.getComponentUnderTest().loadIconSet(iconSetRef);

        // Verify
        verifies(result);
        assertEquals("MyIconTheme", result.getName());
    }

    @Test
    public void loadIconSetWithException() throws Exception
    {
        Reader content = mock(Reader.class);
        IOException exception = new IOException("test");
        when(content.read(any(char[].class))).thenThrow(exception);

        // Test
        Exception caughException = null;
        try {
            mocker.getComponentUnderTest().loadIconSet(content, "FontAwesome");
        } catch (IconException e) {
            caughException = e;
        }

        assertNotNull(caughException);
        assert(caughException instanceof IconException);
        assertEquals(exception, caughException.getCause());
        assertEquals("Failed to load the IconSet [FontAwesome].", caughException.getMessage());
    }

    @Test
    public void loadIconSetFromWikiDocumentWithException() throws Exception
    {
        Exception exception = new Exception("test");
        when(documentAccessBridge.getDocumentInstance(any(DocumentReference.class))).thenThrow(exception);

        // Test
        Exception caughException = null;
        try {
            mocker.getComponentUnderTest().loadIconSet(new DocumentReference("a", "b", "c"));
        } catch (IconException e) {
            caughException = e;
        }

        assertNotNull(caughException);
        assert(caughException instanceof IconException);
        assertEquals(exception, caughException.getCause());
        assertEquals("Failed to load the IconSet [a:b.c].", caughException.getMessage());
    }
}
