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
package org.xwiki.lesscss.internal;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link org.xwiki.lesscss.internal.DefaultLESSSkinFileCache}.
 *
 * @since 6.1M2
 * @version $Id$
 */
public class DefaultLESSSkinFileCacheTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultLESSSkinFileCache> mocker =
            new MockitoComponentMockingRule<>(DefaultLESSSkinFileCache.class);

    private CacheManager cacheManager;

    private Cache<String> cache;

    private DocumentReferenceResolver<String> documentReferenceResolver;

    private EntityReferenceSerializer<String> entityReferenceSerializer;

    private WikiDescriptorManager wikiDescriptorManager;

    private ObjectReference objectReference;

    @Before
    public void setUp() throws Exception
    {
        cacheManager = mocker.getInstance(CacheManager.class);
        cache = mock(Cache.class);
        CacheFactory cacheFactory = mock(CacheFactory.class);
        when(cacheManager.getCacheFactory()).thenReturn(cacheFactory);
        CacheConfiguration configuration = new CacheConfiguration("lesscss.skinfiles.cache");
        when(cacheFactory.<String>newCache(eq(configuration))).thenReturn(cache);
        documentReferenceResolver = mocker.getInstance(
                new DefaultParameterizedType(null, DocumentReferenceResolver.class, String.class));
        entityReferenceSerializer = mocker.getInstance(
                new DefaultParameterizedType(null, EntityReferenceSerializer.class, String.class));
        wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        DocumentReference colorThemeRef = new DocumentReference("currentWiki", "ColorTheme", "CT");
        DocumentReference colorThemeRef2 = new DocumentReference("currentWiki", "ColorTheme", "CT2");
        when(documentReferenceResolver.resolve(eq("colorTheme"), any(WikiReference.class))).thenReturn(colorThemeRef);
        when(documentReferenceResolver.resolve(eq("colorTheme2"), any(WikiReference.class))).thenReturn(colorThemeRef2);
        when(entityReferenceSerializer.serialize(colorThemeRef)).thenReturn("currentWiki:ColorTheme.CT");
        when(entityReferenceSerializer.serialize(colorThemeRef2)).thenReturn("currentWiki:ColorTheme.CT2");
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("currentWiki");
        objectReference = new ObjectReference("SSX", new DocumentReference("wiki", "space", "page"));
        when(entityReferenceSerializer.serialize(eq(objectReference))).thenReturn("XObjectRef");
    }

    @Test
    public void get() throws Exception
    {
        // Mocks
        when(cache.get("FILE_4skin_25currentWiki:ColorTheme.CT_4file")).thenReturn("Expected output for file");
        when(cache.get("ENTITY_4skin_25currentWiki:ColorTheme.CT_10XObjectRef")).thenReturn("Expected output for XO");

        // Tests
        assertEquals("Expected output for file", mocker.getComponentUnderTest().get("file", "skin", "colorTheme"));
        assertEquals("Expected output for XO", mocker.getComponentUnderTest().get(
                objectReference, "skin", "colorTheme"));
    }

    @Test
    public void set() throws Exception
    {
        // Tests
        mocker.getComponentUnderTest().set("file", "skin", "colorTheme", "css for file");
        mocker.getComponentUnderTest().set(objectReference, "skin", "colorTheme", "css for xo");

        // Verify
        verify(cache).set(eq("FILE_4skin_25currentWiki:ColorTheme.CT_4file"), eq("css for file"));
        verify(cache).set(eq("ENTITY_4skin_25currentWiki:ColorTheme.CT_10XObjectRef"), eq("css for xo"));
    }

    @Test
    public void clear() throws Exception
    {
        // Test
        mocker.getComponentUnderTest().clear();

        // Verify
        verify(cache).removeAll();
    }

    @Test
    public void clearFromFileSystemSkin() throws Exception
    {
        // Init

        // Add the first one twice
        mocker.getComponentUnderTest().set("file1", "skin1", "colorTheme", "css1");
        mocker.getComponentUnderTest().set("file1", "skin1", "colorTheme", "css1");

        // Others
        mocker.getComponentUnderTest().set("file1", "skin2", "colorTheme", "css2");
        mocker.getComponentUnderTest().set("file2", "skin1", "colorTheme", "css3");
        mocker.getComponentUnderTest().set(objectReference, "skin1", "colorTheme", "css4");

        // Testskin1
        mocker.getComponentUnderTest().clearFromFileSystemSkin("skin1");

        // Verify
        verify(cache, times(1)).remove("FILE_5skin1_25currentWiki:ColorTheme.CT_5file1");
        verify(cache, times(1)).remove("ENTITY_5skin1_25currentWiki:ColorTheme.CT_10XObjectRef");
        verify(cache).remove("FILE_5skin1_25currentWiki:ColorTheme.CT_5file2");
        verify(cache, never()).remove("FILE_5skin2_25currentWiki:ColorTheme.CT_5file1");
    }

    @Test
    public void clearFromColorTheme() throws Exception
    {
        // Init
        DocumentReference colorThemeRef2 = new DocumentReference("currentWiki", "ColorTheme", "CT2");
        when(documentReferenceResolver.resolve(eq("colorTheme2"), any(WikiReference.class))).thenReturn(colorThemeRef2);
        when(entityReferenceSerializer.serialize(colorThemeRef2)).thenReturn("currentWiki:ColorTheme.CT2");

        // Add the first one twice
        mocker.getComponentUnderTest().set("file1", "skin1", "colorTheme", "css1");
        mocker.getComponentUnderTest().set("file1", "skin1", "colorTheme", "css1");

        // Others
        mocker.getComponentUnderTest().set("file1", "skin1", "colorTheme2", "css2");
        mocker.getComponentUnderTest().set("file1", "skin2", "colorTheme", "css3");
        mocker.getComponentUnderTest().set(objectReference, "skin1", "colorTheme", "css4");

        // Test
        mocker.getComponentUnderTest().clearFromColorTheme("colorTheme");

        // Verify
        verify(cache, times(1)).remove("FILE_5skin1_25currentWiki:ColorTheme.CT_5file1");
        verify(cache, times(1)).remove("ENTITY_5skin1_25currentWiki:ColorTheme.CT_10XObjectRef");
        verify(cache).remove("FILE_5skin2_25currentWiki:ColorTheme.CT_5file1");
        verify(cache, never()).remove("FILE_5skin2_26currentWiki:ColorTheme.CT2_5file1");
    }

    @Test
    public void clearFromEntity() throws Exception
    {
        // Add the first one twice
        mocker.getComponentUnderTest().set("file1", "skin1", "colorTheme", "css1");
        mocker.getComponentUnderTest().set("file1", "skin1", "colorTheme", "css1");

        // Others
        mocker.getComponentUnderTest().set("file1", "skin1", "colorTheme2", "css2");
        mocker.getComponentUnderTest().set("file1", "skin2", "colorTheme", "css3");
        mocker.getComponentUnderTest().set(objectReference, "skin1", "colorTheme", "css4");

        // Test
        mocker.getComponentUnderTest().clearFromEntity(objectReference);

        // Verify
        verify(cache, never()).remove("FILE_5skin1_25currentWiki:ColorTheme.CT_5file1");
        verify(cache, times(1)).remove("ENTITY_5skin1_25currentWiki:ColorTheme.CT_10XObjectRef");
        verify(cache, never()).remove("FILE_5skin2_25currentWiki:ColorTheme.CT_5file1");
        verify(cache, never()).remove("FILE_5skin2_26currentWiki:ColorTheme.CT2_5file1");
    }

}
