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
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
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

    @Before
    public void setUp() throws Exception
    {
        cacheManager = mocker.getInstance(CacheManager.class);
        cache = mock(Cache.class);
        CacheFactory cacheFactory = mock(CacheFactory.class);
        when(cacheManager.getCacheFactory()).thenReturn(cacheFactory);
        CacheConfiguration configuration = new CacheConfiguration("lesscss.skinfiles.cache");
        when(cacheFactory.<String>newCache(eq(configuration))).thenReturn(cache);
    }

    @Test
    public void get() throws Exception
    {
        // Mock
        when(cache.get("6wikiId_4skin_10colorTheme_4file")).thenReturn("Expected output");

        // Test
        String result = mocker.getComponentUnderTest().get("file", "wikiId", "skin", "colorTheme");

        // Verify
        assertEquals("Expected output", result);
    }

    @Test
    public void set() throws Exception
    {
        // Test
        mocker.getComponentUnderTest().set("file", "wikiId", "skin", "colorTheme", "css");

        // Verify
        verify(cache).set(eq("6wikiId_4skin_10colorTheme_4file"), eq("css"));
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
    public void clearWithParams() throws Exception
    {
        // Init

        // Add the first one twice
        mocker.getComponentUnderTest().set("file1", "wiki1", "skin1", "colorTheme1", "css1");
        mocker.getComponentUnderTest().set("file1", "wiki1", "skin1", "colorTheme1", "css1");

        // Others
        mocker.getComponentUnderTest().set("file1", "wiki1", "skin2", "colorTheme1", "css2");
        mocker.getComponentUnderTest().set("file1", "wiki2", "skin1", "colorTheme1", "css3");

        // Test
        mocker.getComponentUnderTest().clear("wiki1");

        // Verify
        verify(cache, times(1)).remove("5wiki1_5skin1_11colorTheme1_5file1");
        verify(cache).remove("5wiki1_5skin2_11colorTheme1_5file1");
        verify(cache, never()).remove("5wiki2_5skin1_11colorTheme1_5file1");
    }

}
