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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.lesscss.internal.colortheme.NamedColorThemeReference;
import org.xwiki.lesscss.internal.resources.LESSSkinFileResourceReference;
import org.xwiki.lesscss.internal.skin.FSSkinReference;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link DefaultLESSResourcesCache}.
 *
 * @version $Id$
 * @since 6.1M2
 */
@ComponentTest
class DefaultLESSResourcesCacheTest
{
    @InjectMockComponents
    private DefaultLESSResourcesCache defaultLESSResourcesCache;

    @MockComponent
    private CacheManager cacheManager;

    @MockComponent
    private CacheKeyFactory cacheKeyFactory;

    private Cache<String> cache;

    @AfterComponent
    void afterComponents() throws Exception
    {
        this.cache = mock(Cache.class);
        CacheFactory cacheFactory = mock(CacheFactory.class);
        when(this.cacheManager.getCacheFactory()).thenReturn(cacheFactory);
        CacheConfiguration configuration = new CacheConfiguration("lesscss.skinfiles.cache");
        when(cacheFactory.<String>newCache(configuration)).thenReturn(this.cache);
    }

    @BeforeEach
    void setUp()
    {
        LESSSkinFileResourceReference lessSkinFileResourceReference =
            new LESSSkinFileResourceReference("lessResource", null, null);
        when(this.cacheKeyFactory.getCacheKey(lessSkinFileResourceReference, new FSSkinReference("skin"),
            new NamedColorThemeReference("colorTheme"), true)).thenReturn(
            "12_lessResource_4_skin_10_colorTheme");
    }

    private LESSSkinFileResourceReference createLESSSkinFileResourceReference(String fileName)
    {
        return new LESSSkinFileResourceReference(fileName, null, null);
    }

    @Test
    void get()
    {
        // Mock
        when(this.cache.get("12_lessResource_4_skin_10_colorTheme")).thenReturn("Expected output");

        // Test
        String result =
            this.defaultLESSResourcesCache.get(new LESSSkinFileResourceReference("lessResource", null, null),
                new FSSkinReference("skin"), new NamedColorThemeReference("colorTheme"));

        // Verify
        assertEquals("Expected output", result);
    }

    @Test
    void set()
    {
        // Test
        this.defaultLESSResourcesCache.set(
            new LESSSkinFileResourceReference("lessResource", null, null), new FSSkinReference("skin"),
            new NamedColorThemeReference("colorTheme"), "css");

        // Verify
        verify(this.cache).set("12_lessResource_4_skin_10_colorTheme", "css");
    }

    @Test
    void clear()
    {
        // Test
        this.defaultLESSResourcesCache.clear();

        // Verify
        verify(this.cache).removeAll();
    }

    @Test
    void clearFromSkin()
    {
        // Mocks
        LESSSkinFileResourceReference file1 = createLESSSkinFileResourceReference("file1");
        LESSSkinFileResourceReference file2 = createLESSSkinFileResourceReference("file2");
        when(this.cacheKeyFactory.getCacheKey(file1, new FSSkinReference("skin1"),
            new NamedColorThemeReference("colorTheme"), true)).thenReturn("k1");
        when(this.cacheKeyFactory.getCacheKey(file1, new FSSkinReference("skin2"),
            new NamedColorThemeReference("colorTheme"), true)).thenReturn("k3");
        when(this.cacheKeyFactory.getCacheKey(file2, new FSSkinReference("skin1"),
            new NamedColorThemeReference("colorTheme"), true)).thenReturn("k4");

        // Add the first one twice
        this.defaultLESSResourcesCache.set(createLESSSkinFileResourceReference("file1"), new FSSkinReference("skin1"),
            new NamedColorThemeReference("colorTheme"), "css1");
        this.defaultLESSResourcesCache.set(createLESSSkinFileResourceReference("file1"), new FSSkinReference("skin1"),
            new NamedColorThemeReference("colorTheme"), "css1");

        // Others
        this.defaultLESSResourcesCache.set(createLESSSkinFileResourceReference("file1"), new FSSkinReference("skin2"),
            new NamedColorThemeReference("colorTheme"), "css2");
        this.defaultLESSResourcesCache.set(createLESSSkinFileResourceReference("file2"), new FSSkinReference("skin1"),
            new NamedColorThemeReference("colorTheme"), "css3");

        // Test
        this.defaultLESSResourcesCache.clearFromSkin(new FSSkinReference("skin1"));

        // Verify
        verify(this.cache).remove("k1");
        verify(this.cache).remove("k4");
        verify(this.cache, never()).remove("k3");
    }

    @Test
    void clearFromColorTheme()
    {
        // Mocks
        LESSSkinFileResourceReference file1 = createLESSSkinFileResourceReference("file1");
        LESSSkinFileResourceReference file2 = createLESSSkinFileResourceReference("file2");

        when(this.cacheKeyFactory.getCacheKey(file1, new FSSkinReference("skin1"),
            new NamedColorThemeReference("colorTheme1"), true)).thenReturn("k1");
        when(this.cacheKeyFactory.getCacheKey(file1, new FSSkinReference("skin1"),
            new NamedColorThemeReference("colorTheme2"), true)).thenReturn("k3");
        when(this.cacheKeyFactory.getCacheKey(file2, new FSSkinReference("skin2"),
            new NamedColorThemeReference("colorTheme1"), true)).thenReturn("k4");
        // Add the first one twice
        this.defaultLESSResourcesCache.set(createLESSSkinFileResourceReference("file1"), new FSSkinReference("skin1"),
            new NamedColorThemeReference("colorTheme1"), "css1");
        this.defaultLESSResourcesCache.set(createLESSSkinFileResourceReference("file1"), new FSSkinReference("skin1"),
            new NamedColorThemeReference("colorTheme1"), "css1");

        // Others
        this.defaultLESSResourcesCache.set(createLESSSkinFileResourceReference("file1"), new FSSkinReference("skin1"),
            new NamedColorThemeReference("colorTheme2"), "css2");
        this.defaultLESSResourcesCache.set(createLESSSkinFileResourceReference("file2"), new FSSkinReference("skin2"),
            new NamedColorThemeReference("colorTheme1"), "css3");

        // Test
        this.defaultLESSResourcesCache.clearFromColorTheme(new NamedColorThemeReference("colorTheme1"));

        // Verify
        verify(this.cache).remove("k1");
        verify(this.cache).remove("k4");
        verify(this.cache, never()).remove("k3");
    }

    @Test
    void clearFromLESSResource()
    {
        // Mocks
        LESSSkinFileResourceReference file1 = createLESSSkinFileResourceReference("file1");
        LESSSkinFileResourceReference file2 = createLESSSkinFileResourceReference("file2");

        when(this.cacheKeyFactory.getCacheKey(file1, new FSSkinReference("skin1"),
            new NamedColorThemeReference("colorTheme1"), true)).thenReturn("k1");
        when(this.cacheKeyFactory.getCacheKey(file2, new FSSkinReference("skin1"),
            new NamedColorThemeReference("colorTheme1"), true)).thenReturn("k3");
        when(this.cacheKeyFactory.getCacheKey(file1, new FSSkinReference("skin2"),
            new NamedColorThemeReference("colorTheme1"), true)).thenReturn("k4");

        // Add the first one twice
        this.defaultLESSResourcesCache.set(createLESSSkinFileResourceReference("file1"), new FSSkinReference("skin1"),
            new NamedColorThemeReference("colorTheme1"), "css1");
        this.defaultLESSResourcesCache.set(createLESSSkinFileResourceReference("file1"), new FSSkinReference("skin1"),
            new NamedColorThemeReference("colorTheme1"), "css1");

        // Others
        this.defaultLESSResourcesCache.set(createLESSSkinFileResourceReference("file2"), new FSSkinReference("skin1"),
            new NamedColorThemeReference("colorTheme1"), "css");
        this.defaultLESSResourcesCache.set(createLESSSkinFileResourceReference("file1"), new FSSkinReference("skin2"),
            new NamedColorThemeReference("colorTheme1"), "css3");

        // Test
        this.defaultLESSResourcesCache.clearFromLESSResource(createLESSSkinFileResourceReference("file1"));

        // Verify
        verify(this.cache).remove("k1");
        verify(this.cache).remove("k4");
        verify(this.cache, never()).remove("k3");
    }
}
