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

import org.junit.jupiter.api.Test;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.icon.IconSet;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link org.xwiki.icon.internal.DefaultIconSetCache}.
 *
 * @since 6.2M1
 * @version $Id$
 */
@ComponentTest
class DefaultIconSetCacheTest
{
    @InjectMockComponents
    private DefaultIconSetCache iconSetCache;

    @MockComponent
    private CacheManager cacheManager;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    private Cache<IconSet> cache;

    @BeforeComponent
    void setUp() throws Exception
    {
        CacheFactory cacheFactory = mock(CacheFactory.class);
        when(this.cacheManager.getCacheFactory()).thenReturn(cacheFactory);
        CacheConfiguration configuration = new CacheConfiguration("iconset");
        this.cache = mock();
        when(cacheFactory.<IconSet>newCache(configuration)).thenReturn(this.cache);
    }

    @Test
    void getByName()
    {
        IconSet iconSet = new IconSet("key");
        when(this.cache.get("NAMED:key")).thenReturn(iconSet);

        IconSet result = this.iconSetCache.get("key");
        assertSame(result, iconSet);
    }

    @Test
    void getByNameAndWiki()
    {
        IconSet iconSet = new IconSet("key");
        when(cache.get("NAMED:6wikiId_key")).thenReturn(iconSet);

        IconSet result = this.iconSetCache.get("key", "wikiId");
        assertSame(result, iconSet);
    }

    @Test
    void getByDocRef()
    {
        IconSet iconSet = new IconSet("key");
        DocumentReference docRef = new DocumentReference("a","b","c");
        when(this.entityReferenceSerializer.serialize(docRef)).thenReturn("a:b.c");
        when(this.cache.get("DOC:a:b.c")).thenReturn(iconSet);

        IconSet result = this.iconSetCache.get(docRef);
        assertSame(result, iconSet);
    }

    @Test
    void putByName()
    {
        IconSet iconSet = new IconSet("key");
        this.iconSetCache.put("key", iconSet);
        verify(this.cache).set("NAMED:key", iconSet);
    }

    @Test
    void putByNameAndWiki()
    {
        IconSet iconSet = new IconSet("key");
        this.iconSetCache.put("key", "wikiId", iconSet);
        verify(this.cache).set("NAMED:6wikiId_key", iconSet);
    }

    @Test
    void putByDocRef()
    {
        IconSet iconSet = new IconSet("key");
        DocumentReference docRef = new DocumentReference("a","b","c");
        when(this.entityReferenceSerializer.serialize(docRef)).thenReturn("a:b.c");
        this.iconSetCache.put(docRef, iconSet);
        verify(this.cache).set("DOC:a:b.c", iconSet);
    }

    @Test
    void clear()
    {
        this.iconSetCache.clear();
        verify(this.cache).removeAll();
    }

    @Test
    void clearByName()
    {
        this.iconSetCache.clear("key");
        verify(this.cache).remove("NAMED:key");
    }

    @Test
    void clearByNameAndWiki()
    {
        this.iconSetCache.clear("key", "wikiId");
        verify(this.cache).remove("NAMED:6wikiId_key");
    }

    @Test
    void clearByDocRef()
    {
        DocumentReference docRef = new DocumentReference("a","b","c");
        when(this.entityReferenceSerializer.serialize(docRef)).thenReturn("a:b.c");
        this.iconSetCache.clear(docRef);
        verify(this.cache).remove("DOC:a:b.c");
    }

    @Test
    void initializeWhenError() throws Exception
    {
        CacheFactory cacheFactory = mock(CacheFactory.class);
        when(this.cacheManager.getCacheFactory()).thenReturn(cacheFactory);

        Exception exception = new CacheException("ERROR");
        when(cacheFactory.newCache(any(CacheConfiguration.class))).thenThrow(exception);

        Throwable expected = assertThrows(InitializationException.class, () -> {
            this.iconSetCache.initialize();
        });
        assertEquals("Failed to initialize the IconSet Cache.", expected.getMessage());
        assertEquals(exception, expected.getCause());
    }
}
