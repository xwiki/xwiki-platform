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
import java.io.Reader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.icon.IconException;
import org.xwiki.icon.IconSet;
import org.xwiki.icon.IconSetCache;
import org.xwiki.icon.IconSetLoader;
import org.xwiki.icon.internal.context.IconSetContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
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

    private IconSetContext iconSetContext;

    private QueryManager queryManager;

    private WikiDescriptorManager wikiDescriptorManager;
    
    private ConfigurationSource configurationSource;

    private XWikiContext xcontext;

    private XWiki xwiki;

    @Before
    public void setUp() throws Exception
    {
        xcontextProvider = mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(xcontext);
        xwiki = mock(XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
        documentReferenceResolver = mocker.getInstance(new DefaultParameterizedType(null,
                DocumentReferenceResolver.class, String.class), "current");
        documentAccessBridge = mocker.getInstance(DocumentAccessBridge.class);
        iconSetCache = mocker.getInstance(IconSetCache.class);
        iconSetLoader = mocker.getInstance(IconSetLoader.class);
        iconSetContext = mocker.getInstance(IconSetContext.class);
        queryManager = mocker.getInstance(QueryManager.class);
        wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("currentWikiId");
        configurationSource = mocker.getInstance(ConfigurationSource.class, "all");
    }

    @Test
    public void getCurrentIconSet() throws Exception
    {
        String currentIconTheme = "IconThemes.SilkTheme";
        when(configurationSource.getProperty("iconTheme")).thenReturn(currentIconTheme);
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
        verify(iconSetCache).put(currentIconTheme, "currentWikiId", iconSet);
    }

    @Test
    public void getCurrentIconSetWhenInCache() throws Exception
    {
        String currentIconTheme = "IconThemes.SilkTheme";
        when(configurationSource.getProperty("iconTheme")).thenReturn(currentIconTheme);
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
    public void getCurrentIconSetWhenInContext() throws Exception
    {
        String currentIconTheme = "IconThemes.SilkTheme";
        when(configurationSource.getProperty("iconTheme")).thenReturn(currentIconTheme);
        DocumentReference iconThemeRef = new DocumentReference("xwiki", "IconThemes", "SilkTheme");
        when(documentReferenceResolver.resolve(currentIconTheme)).thenReturn(iconThemeRef);
        when(documentAccessBridge.exists(iconThemeRef)).thenReturn(true);

        IconSet iconSet = new IconSet(currentIconTheme);
        when(iconSetContext.getIconSet()).thenReturn(iconSet);

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
        when(configurationSource.getProperty("iconTheme")).thenReturn(currentIconTheme);
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
        verify(iconSetLoader, never()).loadIconSet(any(InputStreamReader.class), any());
    }

    @Test
    public void getDefaultIconWithException() throws Exception
    {
        // Mocks
        Exception exception = new MalformedURLException();
        when(xwiki.getResourceAsStream(any())).thenThrow(exception);

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

    @Test
    public void getIconSetWhenInCache() throws Exception
    {
        // Mocks
        IconSet iconSet = new IconSet("silk");
        when(iconSetCache.get("silk", "currentWikiId")).thenReturn(iconSet);

        // Test
        assertEquals(iconSet, mocker.getComponentUnderTest().getIconSet("silk"));

        // Verify
        verify(iconSetCache, never()).put(anyString(), any(IconSet.class));
    }

    @Test
    public void getIconSetWhenNotInCache() throws Exception
    {
        // Mocks
        IconSet iconSet = new IconSet("silk");
        Query query = mock(Query.class);
        when(queryManager.createQuery("FROM doc.object(IconThemesCode.IconThemeClass) obj WHERE obj.name = :name",
                Query.XWQL)).thenReturn(query);
        List<String> results = new ArrayList<>();
        results.add("IconThemes.Silk");
        when(query.<String>execute()).thenReturn(results);
        DocumentReference documentReference = new DocumentReference("wiki", "IconThemes", "Silk");
        when(documentReferenceResolver.resolve("IconThemes.Silk")).thenReturn(documentReference);
        when(iconSetLoader.loadIconSet(documentReference)).thenReturn(iconSet);

        // Test
        assertEquals(iconSet, mocker.getComponentUnderTest().getIconSet("silk"));

        // Verify
        verify(query).bindValue("name", "silk");
        verify(iconSetCache).put(documentReference, iconSet);
        verify(iconSetCache).put("silk", "currentWikiId", iconSet);
    }

    @Test
    public void getIconSetWhenDoesNotExists() throws Exception
    {
        // Mocks
        Query query = mock(Query.class);
        when(queryManager.createQuery("FROM doc.object(IconThemesCode.IconThemeClass) obj WHERE obj.name = :name",
                Query.XWQL)).thenReturn(query);
        List<String> results = new ArrayList<>();
        when(query.<String>execute()).thenReturn(results);

        // Test
        assertNull(mocker.getComponentUnderTest().getIconSet("silk"));

        // Verify
        verify(query).bindValue("name", "silk");
    }

    @Test
    public void getIconSetWhenException() throws Exception
    {
        // Mocks
        Exception exception = new QueryException("exception in the query", null, null);
        when(queryManager.createQuery(any(), any())).thenThrow(exception);

        // Test
        Exception caughtException = null;
        try {
            mocker.getComponentUnderTest().getIconSet("silk");
        } catch (IconException e) {
            caughtException = e;
        }
        assertNotNull(caughtException);
        assertEquals(exception, caughtException.getCause());
        assertEquals("Failed to load the icon set [silk].", caughtException.getMessage());
    }

    @Test
    public void getDefaultIconSet() throws Exception
    {
        // Mock
        IconSet iconSet = new IconSet("default");
        when(iconSetLoader.loadIconSet(any(Reader.class), eq("default"))).thenReturn(iconSet);
        InputStream is = getClass().getResourceAsStream("/test.iconset");
        when(xwiki.getResourceAsStream("/resources/icons/default.iconset")).thenReturn(is);

        // Test
        assertEquals(iconSet, mocker.getComponentUnderTest().getIconSet("default"));

        // Verify
        verifyZeroInteractions(queryManager);
    }

    @Test
    public void getIconSetNames() throws Exception
    {
        // Mocks
        Query query = mock(Query.class);
        when(queryManager.createQuery("SELECT obj.name FROM Document doc, doc.object(IconThemesCode.IconThemeClass) obj"
                + " ORDER BY obj.name", Query.XWQL)).thenReturn(query);
        List<String> results = new ArrayList<>();
        when(query.<String>execute()).thenReturn(results);

        // Test
        assertTrue(results == mocker.getComponentUnderTest().getIconSetNames());
    }

    @Test
    public void getIconSetNamesWhenException() throws Exception
    {
        // Mocks
        QueryException exception = new QueryException("exception in the query", null, null);
        when(queryManager.createQuery(any(), eq(Query.XWQL))).thenThrow(exception);

        // Test
        IconException caughtException = null;
        try {
            mocker.getComponentUnderTest().getIconSetNames();
        } catch (IconException e) {
            caughtException = e;
        }

        // Verify
        assertNotNull(caughtException);
        assertEquals("Failed to get the name of all icon sets.", caughtException.getMessage());
        assertEquals(exception, caughtException.getCause());
    }
}
