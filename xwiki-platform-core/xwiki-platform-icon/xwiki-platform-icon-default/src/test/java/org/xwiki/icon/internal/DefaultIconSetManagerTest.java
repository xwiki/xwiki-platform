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

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.bridge.DocumentAccessBridge;
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
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link org.xwiki.icon.internal.DefaultIconSetManager}.
 *
 * @version $Id$
 * @since 6.2M1
 */
@ComponentTest
class DefaultIconSetManagerTest
{
    @InjectMockComponents
    private DefaultIconSetManager iconSetManager;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    private IconSetCache iconSetCache;

    @MockComponent
    private IconSetLoader iconSetLoader;

    @MockComponent
    private IconSetContext iconSetContext;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    @Named("all")
    private ConfigurationSource configurationSource;

    @Mock
    private XWikiContext xcontext;

    @Mock
    private XWiki xwiki;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @BeforeEach
    void setUp()
    {
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("currentWikiId");
    }

    @Test
    void getCurrentIconSet() throws Exception
    {
        String currentIconTheme = "IconThemes.SilkTheme";
        when(this.configurationSource.getProperty("iconTheme")).thenReturn(currentIconTheme);
        DocumentReference iconThemeRef = new DocumentReference("xwiki", "IconThemes", "SilkTheme");
        when(this.documentReferenceResolver.resolve(currentIconTheme)).thenReturn(iconThemeRef);
        when(this.documentAccessBridge.exists(iconThemeRef)).thenReturn(true);

        IconSet iconSet = new IconSet(currentIconTheme);
        when(this.iconSetLoader.loadIconSet(iconThemeRef)).thenReturn(iconSet);

        // Test
        IconSet result = this.iconSetManager.getCurrentIconSet();

        // Verify
        assertEquals(iconSet, result);
        verify(this.iconSetCache).put(iconThemeRef, iconSet);
        verify(this.iconSetCache).put(currentIconTheme, "currentWikiId", iconSet);
    }

    @Test
    void getCurrentIconSetWhenInCache() throws Exception
    {
        String currentIconTheme = "IconThemes.SilkTheme";
        when(this.configurationSource.getProperty("iconTheme")).thenReturn(currentIconTheme);
        DocumentReference iconThemeRef = new DocumentReference("xwiki", "IconThemes", "SilkTheme");
        when(this.documentReferenceResolver.resolve(currentIconTheme)).thenReturn(iconThemeRef);
        when(this.documentAccessBridge.exists(iconThemeRef)).thenReturn(true);

        IconSet iconSet = new IconSet(currentIconTheme);
        when(this.iconSetCache.get(iconThemeRef)).thenReturn(iconSet);

        // Test
        IconSet result = this.iconSetManager.getCurrentIconSet();

        // Verify
        assertEquals(iconSet, result);
        verify(this.iconSetLoader, never()).loadIconSet(any(DocumentReference.class));
    }

    @Test
    void getCurrentIconSetWhenInContext() throws Exception
    {
        String currentIconTheme = "IconThemes.SilkTheme";
        when(this.configurationSource.getProperty("iconTheme")).thenReturn(currentIconTheme);
        DocumentReference iconThemeRef = new DocumentReference("xwiki", "IconThemes", "SilkTheme");
        when(this.documentReferenceResolver.resolve(currentIconTheme)).thenReturn(iconThemeRef);
        when(this.documentAccessBridge.exists(iconThemeRef)).thenReturn(true);

        IconSet iconSet = new IconSet(currentIconTheme);
        when(this.iconSetContext.getIconSet()).thenReturn(iconSet);

        // Test
        IconSet result = this.iconSetManager.getCurrentIconSet();

        // Verify
        assertEquals(iconSet, result);
        verify(this.iconSetLoader, never()).loadIconSet(any(DocumentReference.class));
    }

    @Test
    void getCurrentIconSetWhenItDoesNotExist() throws Exception
    {
        String currentIconTheme = "xwiki:IconThemes.SilkTheme";
        when(this.configurationSource.getProperty("iconTheme")).thenReturn(currentIconTheme);
        DocumentReference iconThemeRef = new DocumentReference("xwiki", "IconThemes", "SilkTheme");
        when(this.documentReferenceResolver.resolve(currentIconTheme)).thenReturn(iconThemeRef);
        when(this.documentAccessBridge.exists(iconThemeRef)).thenReturn(false);

        when(this.iconSetCache.get(iconThemeRef)).thenReturn(null);

        // Test
        IconSet result = this.iconSetManager.getCurrentIconSet();

        // Verify
        assertNull(result);
        verify(this.iconSetLoader, never()).loadIconSet(any(DocumentReference.class));
    }

    @Test
    void getDefaultIcon() throws Exception
    {
        InputStream is = getClass().getResourceAsStream("/test.iconset");
        when(this.xwiki.getResourceAsStream("/resources/icons/default.iconset")).thenReturn(is);

        IconSet iconSet = new IconSet("default");
        when(this.iconSetLoader.loadIconSet(any(InputStreamReader.class), eq("default"))).thenReturn(iconSet);

        // Test
        IconSet result = this.iconSetManager.getDefaultIconSet();

        // Verify
        assertEquals(iconSet, result);
        verify(this.iconSetCache).put("default", iconSet);
    }

    @Test
    void getDefaultIconWhenInCache() throws Exception
    {
        IconSet iconSet = new IconSet("default");
        when(this.iconSetCache.get("default")).thenReturn(iconSet);

        // Test
        IconSet result = this.iconSetManager.getDefaultIconSet();

        // Verify
        assertEquals(iconSet, result);
        verify(this.iconSetLoader, never()).loadIconSet(any(InputStreamReader.class), any());
    }

    @Test
    void getDefaultIconWithException() throws Exception
    {
        // Mocks
        Exception exception = new MalformedURLException();
        when(this.xwiki.getResourceAsStream(any())).thenThrow(exception);

        // Test
        Exception exceptionCaught = null;
        try {
            this.iconSetManager.getDefaultIconSet();
        } catch (IconException e) {
            exceptionCaught = e;
        }

        // Verify
        assertNotNull(exceptionCaught);
        assertEquals(exception, exceptionCaught.getCause());
        assertEquals("Failed to load the current default icon set resource.", exceptionCaught.getMessage());
    }

    @Test
    void getIconSetWhenInCache() throws Exception
    {
        // Mocks
        IconSet iconSet = new IconSet("silk");
        when(this.iconSetCache.get("silk", "currentWikiId")).thenReturn(iconSet);

        // Test
        assertEquals(iconSet, this.iconSetManager.getIconSet("silk"));

        // Verify
        verify(this.iconSetCache, never()).put(anyString(), any(IconSet.class));
    }

    @Test
    void getIconSetWhenNotInCache() throws Exception
    {
        // Mocks
        IconSet iconSet = new IconSet("silk");
        Query query = mock(Query.class);
        when(this.queryManager.createQuery("FROM doc.object(IconThemesCode.IconThemeClass) obj WHERE obj.name = :name",
            Query.XWQL)).thenReturn(query);
        List<String> results = new ArrayList<>();
        results.add("IconThemes.Silk");
        when(query.<String>execute()).thenReturn(results);
        DocumentReference documentReference = new DocumentReference("wiki", "IconThemes", "Silk");
        when(this.documentReferenceResolver.resolve("IconThemes.Silk")).thenReturn(documentReference);
        when(this.iconSetLoader.loadIconSet(documentReference)).thenReturn(iconSet);

        // Test
        assertEquals(iconSet, this.iconSetManager.getIconSet("silk"));

        // Verify
        verify(query).bindValue("name", "silk");
        verify(this.iconSetCache).put(documentReference, iconSet);
        verify(this.iconSetCache).put("silk", "currentWikiId", iconSet);
    }

    @Test
    void getIconSetWhenDoesNotExists() throws Exception
    {
        // Mocks
        Query query = mock(Query.class);
        when(this.queryManager.createQuery("FROM doc.object(IconThemesCode.IconThemeClass) obj WHERE obj.name = :name",
            Query.XWQL)).thenReturn(query);
        List<String> results = new ArrayList<>();
        when(query.<String>execute()).thenReturn(results);

        // Test
        assertNull(this.iconSetManager.getIconSet("silk"));

        // Verify
        verify(query).bindValue("name", "silk");
    }

    @Test
    void getIconSetWhenException() throws Exception
    {
        // Mocks
        Exception exception = new QueryException("exception in the query", null, null);
        when(this.queryManager.createQuery(any(), any())).thenThrow(exception);

        // Test
        Exception caughtException = null;
        try {
            this.iconSetManager.getIconSet("silk");
        } catch (IconException e) {
            caughtException = e;
        }
        assertNotNull(caughtException);
        assertEquals(exception, caughtException.getCause());
        assertEquals("Failed to load the icon set [silk].", caughtException.getMessage());
    }

    @Test
    void getIconSetWhenOneFails() throws Exception
    {
        // Mocks
        IconSet iconSet = new IconSet("silk");
        Query query = mock(Query.class);
        when(this.queryManager.createQuery("FROM doc.object(IconThemesCode.IconThemeClass) obj WHERE obj.name = :name",
            Query.XWQL)).thenReturn(query);
        List<String> results = List.of("FakeIcon.Silk", "IconThemes.Silk");
        when(query.<String>execute()).thenReturn(results);
        DocumentReference fakeDocumentReference = new DocumentReference("wiki", "FakeIcon", "Silk");
        when(this.documentReferenceResolver.resolve("FakeIcon.Silk")).thenReturn(fakeDocumentReference);
        when(this.iconSetLoader.loadIconSet(fakeDocumentReference)).thenThrow(new IconException("Test"));

        DocumentReference documentReference = new DocumentReference("wiki", "IconThemes", "Silk");
        when(this.documentReferenceResolver.resolve("IconThemes.Silk")).thenReturn(documentReference);
        when(this.iconSetLoader.loadIconSet(documentReference)).thenReturn(iconSet);

        // Test
        assertEquals(iconSet, this.iconSetManager.getIconSet("silk"));

        // Verify
        verify(query).bindValue("name", "silk");
        verify(this.iconSetCache).put(documentReference, iconSet);
        verify(this.iconSetCache).put("silk", "currentWikiId", iconSet);
        verify(this.iconSetLoader).loadIconSet(fakeDocumentReference);
    }

    @Test
    void getIconSetWhenAllFail() throws Exception
    {
        // Mocks
        Query query = mock(Query.class);
        when(this.queryManager.createQuery("FROM doc.object(IconThemesCode.IconThemeClass) obj WHERE obj.name = :name",
            Query.XWQL)).thenReturn(query);
        List<String> results = List.of("FakeIcon.Silk", "IconThemes.Silk");
        when(query.<String>execute()).thenReturn(results);
        DocumentReference fakeDocumentReference = new DocumentReference("wiki", "FakeIcon", "Silk");
        when(this.documentReferenceResolver.resolve("FakeIcon.Silk")).thenReturn(fakeDocumentReference);
        IconException fakeException = new IconException("Fake");
        when(this.iconSetLoader.loadIconSet(fakeDocumentReference)).thenThrow(fakeException);

        DocumentReference documentReference = new DocumentReference("wiki", "IconThemes", "Silk");
        when(this.documentReferenceResolver.resolve("IconThemes.Silk")).thenReturn(documentReference);
        when(this.iconSetLoader.loadIconSet(documentReference)).thenThrow(new IconException("Real"));

        // Test
        IconException exception = assertThrows(IconException.class, () -> this.iconSetManager.getIconSet("silk"));
        assertEquals("Failed to load the icon set [silk] from 2 documents, reporting the first exception, see the"
            + " log for additional errors.", exception.getMessage());
        assertEquals(fakeException, exception.getCause());

        assertEquals(1, this.logCapture.size());
        assertEquals("Failed loading icon set [silk] from multiple matching documents, "
                + "ignored this additional exception, reason: [IconException: Real].",
            this.logCapture.getMessage(0));

        // Verify
        verify(query).bindValue("name", "silk");
        verify(this.iconSetCache, never()).put(anyString(), any());
        verify(this.iconSetCache, never()).put(any(DocumentReference.class), any());
        verify(this.iconSetLoader).loadIconSet(fakeDocumentReference);
    }

    @Test
    void getDefaultIconSet() throws Exception
    {
        // Mock
        IconSet iconSet = new IconSet("default");
        when(this.iconSetLoader.loadIconSet(any(Reader.class), eq("default"))).thenReturn(iconSet);
        InputStream is = getClass().getResourceAsStream("/test.iconset");
        when(this.xwiki.getResourceAsStream("/resources/icons/default.iconset")).thenReturn(is);

        // Test
        assertEquals(iconSet, this.iconSetManager.getIconSet("default"));

        // Verify
        verifyNoInteractions(this.queryManager);
    }

    @Test
    void getIconSetNames() throws Exception
    {
        // Mocks
        Query query = mock(Query.class);
        when(this.queryManager.createQuery(
            "SELECT obj.name FROM Document doc, doc.object(IconThemesCode.IconThemeClass) obj"
                + " ORDER BY obj.name", Query.XWQL)).thenReturn(query);
        List<String> results = new ArrayList<>();
        when(query.<String>execute()).thenReturn(results);

        // Test
        assertTrue(results == this.iconSetManager.getIconSetNames());
    }

    @Test
    void getIconSetNamesWhenException() throws Exception
    {
        // Mocks
        QueryException exception = new QueryException("exception in the query", null, null);
        when(this.queryManager.createQuery(any(), eq(Query.XWQL))).thenThrow(exception);

        // Test
        IconException caughtException = null;
        try {
            this.iconSetManager.getIconSetNames();
        } catch (IconException e) {
            caughtException = e;
        }

        // Verify
        assertNotNull(caughtException);
        assertEquals("Failed to get the name of all icon sets.", caughtException.getMessage());
        assertEquals(exception, caughtException.getCause());
    }
}
