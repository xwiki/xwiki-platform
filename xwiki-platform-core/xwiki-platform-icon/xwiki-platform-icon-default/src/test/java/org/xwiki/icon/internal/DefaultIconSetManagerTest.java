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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.icon.IconException;
import org.xwiki.icon.IconSet;
import org.xwiki.icon.IconSetCache;
import org.xwiki.icon.IconSetLoader;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link org.xwiki.icon.internal.DefaultIconSetManager}.
 *
 * @since 6.2M1
 * @version $Id$
 */
public class DefaultIconSetManagerTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultIconSetManager> mocker =
            new MockitoComponentMockingRule<>(DefaultIconSetManager.class);

    private Provider<XWikiContext> xcontextProvider;

    private DocumentReferenceResolver<String> documentReferenceResolver;

    private DocumentAccessBridge documentAccessBridge;

    private IconSetCache iconSetCache;

    private IconSetLoader iconSetLoader;

    private XWikiContext xcontext;

    private XWiki xwiki;

    @Before
    public void setUp() throws Exception
    {
        xcontextProvider = mocker.getInstance(new DefaultParameterizedType(null, Provider.class, XWikiContext.class));
        xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(xcontext);
        xwiki = mock(XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
        documentReferenceResolver = mocker.getInstance(new DefaultParameterizedType(null,
                DocumentReferenceResolver.class, String.class));
        documentAccessBridge = mocker.getInstance(DocumentAccessBridge.class);
        iconSetCache = mocker.getInstance(IconSetCache.class);
        iconSetLoader = mocker.getInstance(IconSetLoader.class);
    }

    @Test
    public void getCurrentIconSet() throws Exception
    {
        String currentIconTheme = "IconThemes.SilkTheme";
        when(xwiki.getXWikiPreference("iconTheme", xcontext)).thenReturn(currentIconTheme);
        DocumentReference iconThemeRef = new DocumentReference("xwiki", "IconThemes", "SilkTheme");
        when(documentReferenceResolver.resolve(currentIconTheme)).thenReturn(iconThemeRef);
        when(documentAccessBridge.exists(iconThemeRef)).thenReturn(true);

        IconSet iconSet = new IconSet(currentIconTheme);
        when(iconSetLoader.loadIconSet(iconThemeRef)).thenReturn(iconSet);

        // Test
        IconSet result = mocker.getComponentUnderTest().getCurrentIconSet();

        // Verify
        assertEquals(iconSet, result);
        verify(iconSetCache).put(iconThemeRef, iconSet);
    }

    @Test
    public void getCurrentIconSetWhenInCache() throws Exception
    {
        String currentIconTheme = "IconThemes.SilkTheme";
        when(xwiki.getXWikiPreference("iconTheme", xcontext)).thenReturn(currentIconTheme);
        DocumentReference iconThemeRef = new DocumentReference("xwiki", "IconThemes", "SilkTheme");
        when(documentReferenceResolver.resolve(currentIconTheme)).thenReturn(iconThemeRef);
        when(documentAccessBridge.exists(iconThemeRef)).thenReturn(true);

        IconSet iconSet = new IconSet(currentIconTheme);
        when(iconSetCache.get(iconThemeRef)).thenReturn(iconSet);

        // Test
        IconSet result = mocker.getComponentUnderTest().getCurrentIconSet();

        // Verify
        assertEquals(iconSet, result);
        verify(iconSetLoader, never()).loadIconSet(any(DocumentReference.class));
    }

    @Test
    public void getCurrentIconSetWhenItDoesNotExist() throws Exception
    {
        String currentIconTheme = "xwiki:IconThemes.SilkTheme";
        when(xwiki.getXWikiPreference("iconTheme", xcontext)).thenReturn(currentIconTheme);
        DocumentReference iconThemeRef = new DocumentReference("xwiki", "IconThemes", "SilkTheme");
        when(documentReferenceResolver.resolve(currentIconTheme)).thenReturn(iconThemeRef);
        when(documentAccessBridge.exists(iconThemeRef)).thenReturn(false);

        when(iconSetCache.get(iconThemeRef)).thenReturn(null);

        // Test
        IconSet result = mocker.getComponentUnderTest().getCurrentIconSet();

        // Verify
        assertNull(result);
        verify(iconSetLoader, never()).loadIconSet(any(DocumentReference.class));
    }

    @Test
    public void getDefaultIcon() throws Exception
    {
        InputStream is = getClass().getResourceAsStream("/test.iconset");
        when(xwiki.getResourceAsStream("/resources/icons/default.iconset")).thenReturn(is);

        IconSet iconSet = new IconSet("default");
        when(iconSetLoader.loadIconSet(any(InputStreamReader.class), eq("default"))).thenReturn(iconSet);

        // Test
        IconSet result = mocker.getComponentUnderTest().getDefaultIconSet();

        // Verify
        assertEquals(iconSet, result);
        verify(iconSetCache).put("default", iconSet);
    }

    @Test
    public void getDefaultIconWhenInCache() throws Exception
    {
        IconSet iconSet = new IconSet("default");
        when(iconSetCache.get("default")).thenReturn(iconSet);

        // Test
        IconSet result = mocker.getComponentUnderTest().getDefaultIconSet();

        // Verify
        assertEquals(iconSet, result);
        verify(iconSetLoader, never()).loadIconSet(any(InputStreamReader.class), anyString());
    }

    @Test
    public void getDefaultIconWithException() throws Exception
    {
        // Mocks
        Exception exception = new MalformedURLException();
        when(xwiki.getResourceAsStream(anyString())).thenThrow(exception);

        // Test
        Exception exceptionCaught = null;
        try {
            mocker.getComponentUnderTest().getDefaultIconSet();
        } catch (IconException e) {
            exceptionCaught = e;
        }

        // Verify
        assertNotNull(exceptionCaught);
        assertEquals(exception, exceptionCaught.getCause());
        assertEquals("Failed to get the current default icon set.", exceptionCaught.getMessage());
    }
}
