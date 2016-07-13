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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.icon.IconSet;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link org.xwiki.icon.internal.DefaultIconSetCache}.
 *
 * @since 6.2M1
 * @version $Id$
 */
public class DefaultIconSetCacheTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultIconSetCache> mocker =
            new MockitoComponentMockingRule<>(DefaultIconSetCache.class);

    private CacheManager cacheManager;

    private EntityReferenceSerializer<String> entityReferenceSerializer;

    private Cache<IconSet> cache;

    @Before
    public void setUp() throws Exception
    {
        cacheManager = mocker.getInstance(CacheManager.class);
        entityReferenceSerializer = mocker.getInstance(new DefaultParameterizedType(null,
                EntityReferenceSerializer.class, String.class));
        cache = mock(Cache.class);
        CacheFactory cacheFactory = mock(CacheFactory.class);
        when(cacheManager.getCacheFactory()).thenReturn(cacheFactory);
        CacheConfiguration configuration = new CacheConfiguration("iconset");
        when(cacheFactory.<IconSet>newCache(eq(configuration))).thenReturn(cache);
    }

    @Test
    public void getByName() throws Exception
    {
        IconSet iconSet = new IconSet("key");
        when(cache.get("NAMED:key")).thenReturn(iconSet);

        IconSet result = mocker.getComponentUnderTest().get("key");
        assertTrue(iconSet == result);
    }

    @Test
    public void getByNameAndWiki() throws Exception
    {
        IconSet iconSet = new IconSet("key");
        when(cache.get("NAMED:6wikiId_key")).thenReturn(iconSet);

        IconSet result = mocker.getComponentUnderTest().get("key", "wikiId");
        assertTrue(iconSet == result);
    }

    @Test
    public void getByDocRef() throws Exception
    {
        IconSet iconSet = new IconSet("key");
        DocumentReference docRef = new DocumentReference("a","b","c");
        when(entityReferenceSerializer.serialize(docRef)).thenReturn("a:b.c");
        when(cache.get("DOC:a:b.c")).thenReturn(iconSet);

        IconSet result = mocker.getComponentUnderTest().get(docRef);
        assertTrue(iconSet == result);
    }

    @Test
    public void putByName() throws Exception
    {
        IconSet iconSet = new IconSet("key");
        mocker.getComponentUnderTest().put("key", iconSet);
        verify(cache).set("NAMED:key", iconSet);
    }

    @Test
    public void putByNameAndWiki() throws Exception
    {
        IconSet iconSet = new IconSet("key");
        mocker.getComponentUnderTest().put("key", "wikiId", iconSet);
        verify(cache).set("NAMED:6wikiId_key", iconSet);
    }

    @Test
    public void putByDocRef() throws Exception
    {
        IconSet iconSet = new IconSet("key");
        DocumentReference docRef = new DocumentReference("a","b","c");
        when(entityReferenceSerializer.serialize(docRef)).thenReturn("a:b.c");
        mocker.getComponentUnderTest().put(docRef, iconSet);
        verify(cache).set("DOC:a:b.c", iconSet);
    }

    @Test
    public void clear() throws Exception
    {
        mocker.getComponentUnderTest().clear();
        verify(cache).removeAll();
    }

    @Test
    public void clearByName() throws Exception
    {
        mocker.getComponentUnderTest().clear("key");
        verify(cache).remove("NAMED:key");
    }

    @Test
    public void clearByNameAndWiki() throws Exception
    {
        mocker.getComponentUnderTest().clear("key", "wikiId");
        verify(cache).remove("NAMED:6wikiId_key");
    }

    @Test
    public void clearByDocRef() throws Exception
    {
        DocumentReference docRef = new DocumentReference("a","b","c");
        when(entityReferenceSerializer.serialize(docRef)).thenReturn("a:b.c");
        mocker.getComponentUnderTest().clear(docRef);
        verify(cache).remove("DOC:a:b.c");
    }

    @Test
    public void initializeWhenError() throws Exception
    {
        DefaultIconSetCache cache = mocker.getComponentUnderTest();
        CacheFactory cacheFactory = mock(CacheFactory.class);
        when(cacheManager.getCacheFactory()).thenReturn(cacheFactory);

        Exception exception = new CacheException("ERROR");
        when(cacheFactory.newCache(any(CacheConfiguration.class))).thenThrow(exception);

        Exception exceptionCaught = null;
        try {
            cache.initialize();
        } catch(InitializationException e){
            exceptionCaught = e;
        }

        assertNotNull(exceptionCaught);
        assertEquals("Failed to initialize the IconSet Cache.", exceptionCaught.getMessage());
        assertEquals(exception, exceptionCaught.getCause());
    }


}
