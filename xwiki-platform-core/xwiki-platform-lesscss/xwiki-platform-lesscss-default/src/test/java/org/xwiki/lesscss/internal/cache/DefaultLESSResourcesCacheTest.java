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
package org.xwiki.lesscss.internal.cache;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.lesscss.colortheme.NamedColorThemeReference;
import org.xwiki.lesscss.resources.LESSSkinFileResourceReference;
import org.xwiki.lesscss.skin.FSSkinReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link DefaultLESSResourcesCache}.
 *
 * @since 6.1M2
 * @version $Id$
 */
public class DefaultLESSResourcesCacheTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultLESSResourcesCache> mocker =
            new MockitoComponentMockingRule<>(DefaultLESSResourcesCache.class);

    private CacheManager cacheManager;

    private Cache<String> cache;

    private CacheKeyFactory cacheKeyFactory;

    @Before
    public void setUp() throws Exception
    {
        cacheManager = mocker.getInstance(CacheManager.class);
        cache = mock(Cache.class);
        CacheFactory cacheFactory = mock(CacheFactory.class);
        when(cacheManager.getCacheFactory()).thenReturn(cacheFactory);
        CacheConfiguration configuration = new CacheConfiguration("lesscss.skinfiles.cache");
        when(cacheFactory.<String>newCache(eq(configuration))).thenReturn(cache);
        cacheKeyFactory = mocker.getInstance(CacheKeyFactory.class);

        LESSSkinFileResourceReference lessSkinFileResourceReference = new LESSSkinFileResourceReference("lessResource");
        when(cacheKeyFactory.getCacheKey(eq(lessSkinFileResourceReference), eq(new FSSkinReference("skin")),
            eq(new NamedColorThemeReference("colorTheme")), eq(true))).thenReturn("12_lessResource_4_skin_10_colorTheme");
    }

    @Test
    public void get() throws Exception
    {
        // Mock
        when(cache.get("12_lessResource_4_skin_10_colorTheme")).thenReturn("Expected output");

        // Test
        String result = mocker.getComponentUnderTest().get(new LESSSkinFileResourceReference("lessResource"),
            new FSSkinReference("skin"), new NamedColorThemeReference("colorTheme"));

        // Verify
        assertEquals("Expected output", result);
    }

    @Test
    public void set() throws Exception
    {
        // Test
        mocker.getComponentUnderTest().set(
            new LESSSkinFileResourceReference("lessResource"), new FSSkinReference("skin"),
            new NamedColorThemeReference("colorTheme"), "css");

        // Verify
        verify(cache).set(eq("12_lessResource_4_skin_10_colorTheme"), eq("css"));
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
    public void clearFromSkin() throws Exception
    {
        // Mocks
        LESSSkinFileResourceReference file1 = new LESSSkinFileResourceReference("file1");
        LESSSkinFileResourceReference file2 = new LESSSkinFileResourceReference("file2");
        when(cacheKeyFactory.getCacheKey(eq(file1), eq(new FSSkinReference("skin1")),
                eq(new NamedColorThemeReference("colorTheme")), eq(true))).thenReturn("k1");
        when(cacheKeyFactory.getCacheKey(eq(file1), eq(new FSSkinReference("skin2")),
            eq(new NamedColorThemeReference("colorTheme")), eq(true))).thenReturn("k3");
        when(cacheKeyFactory.getCacheKey(eq(file2), eq(new FSSkinReference("skin1")),
                eq(new NamedColorThemeReference("colorTheme")), eq(true))).thenReturn("k4");

        // Add the first one twice
        mocker.getComponentUnderTest().set(new LESSSkinFileResourceReference("file1"), new FSSkinReference("skin1"),
                new NamedColorThemeReference("colorTheme"), "css1");
        mocker.getComponentUnderTest().set(new LESSSkinFileResourceReference("file1"), new FSSkinReference("skin1"),
                new NamedColorThemeReference("colorTheme"), "css1");

        // Others
        mocker.getComponentUnderTest().set(new LESSSkinFileResourceReference("file1"), new FSSkinReference("skin2"),
                new NamedColorThemeReference("colorTheme"), "css2");
        mocker.getComponentUnderTest().set(new LESSSkinFileResourceReference("file2"), new FSSkinReference("skin1"),
                new NamedColorThemeReference("colorTheme"), "css3");

        // Test
        mocker.getComponentUnderTest().clearFromSkin(new FSSkinReference("skin1"));

        // Verify
        verify(cache, times(1)).remove("k1");
        verify(cache).remove("k4");
        verify(cache, never()).remove("k3");
    }

    @Test
    public void clearFromColorTheme() throws Exception
    {
        // Mocks
        LESSSkinFileResourceReference file1 = new LESSSkinFileResourceReference("file1");
        LESSSkinFileResourceReference file2 = new LESSSkinFileResourceReference("file2");

        when(cacheKeyFactory.getCacheKey(eq(file1), eq(new FSSkinReference("skin1")),
                eq(new NamedColorThemeReference("colorTheme1")), eq(true))).thenReturn("k1");
        when(cacheKeyFactory.getCacheKey(eq(file1), eq(new FSSkinReference("skin1")),
                eq(new NamedColorThemeReference("colorTheme2")), eq(true))).thenReturn("k3");
        when(cacheKeyFactory.getCacheKey(eq(file2), eq(new FSSkinReference("skin2")),
                eq(new NamedColorThemeReference("colorTheme1")), eq(true))).thenReturn("k4");
        // Add the first one twice
        mocker.getComponentUnderTest().set(new LESSSkinFileResourceReference("file1"), new FSSkinReference("skin1"),
                new NamedColorThemeReference("colorTheme1"), "css1");
        mocker.getComponentUnderTest().set(new LESSSkinFileResourceReference("file1"), new FSSkinReference("skin1"),
                new NamedColorThemeReference("colorTheme1"), "css1");

        // Others
        mocker.getComponentUnderTest().set(new LESSSkinFileResourceReference("file1"), new FSSkinReference("skin1"),
                new NamedColorThemeReference("colorTheme2"), "css2");
        mocker.getComponentUnderTest().set(new LESSSkinFileResourceReference("file2"), new FSSkinReference("skin2"),
                new NamedColorThemeReference("colorTheme1"), "css3");

        // Test
        mocker.getComponentUnderTest().clearFromColorTheme(new NamedColorThemeReference("colorTheme1"));

        // Verify
        verify(cache, times(1)).remove("k1");
        verify(cache).remove("k4");
        verify(cache, never()).remove("k3");
    }

    @Test
    public void clearFromLESSResource() throws Exception
    {
        // Mocks
        LESSSkinFileResourceReference file1 = new LESSSkinFileResourceReference("file1");
        LESSSkinFileResourceReference file2 = new LESSSkinFileResourceReference("file2");

        when(cacheKeyFactory.getCacheKey(eq(file1), eq(new FSSkinReference("skin1")),
                eq(new NamedColorThemeReference("colorTheme1")), eq(true))).thenReturn("k1");
        when(cacheKeyFactory.getCacheKey(eq(file2), eq(new FSSkinReference("skin1")),
                eq(new NamedColorThemeReference("colorTheme1")), eq(true))).thenReturn("k3");
        when(cacheKeyFactory.getCacheKey(eq(file1), eq(new FSSkinReference("skin2")),
                eq(new NamedColorThemeReference("colorTheme1")), eq(true))).thenReturn("k4");

        // Add the first one twice
        mocker.getComponentUnderTest().set(new LESSSkinFileResourceReference("file1"), new FSSkinReference("skin1"),
                new NamedColorThemeReference("colorTheme1"), "css1");
        mocker.getComponentUnderTest().set(new LESSSkinFileResourceReference("file1"), new FSSkinReference("skin1"),
                new NamedColorThemeReference("colorTheme1"), "css1");

        // Others
        mocker.getComponentUnderTest().set(new LESSSkinFileResourceReference("file2"), new FSSkinReference("skin1"),
                new NamedColorThemeReference("colorTheme1"), "css");
        mocker.getComponentUnderTest().set(new LESSSkinFileResourceReference("file1"), new FSSkinReference("skin2"),
                new NamedColorThemeReference("colorTheme1"), "css3");

        // Test
        mocker.getComponentUnderTest().clearFromLESSResource(new LESSSkinFileResourceReference("file1"));

        // Verify
        verify(cache, times(1)).remove("k1");
        verify(cache).remove("k4");
        verify(cache, never()).remove("k3");
    }

}
