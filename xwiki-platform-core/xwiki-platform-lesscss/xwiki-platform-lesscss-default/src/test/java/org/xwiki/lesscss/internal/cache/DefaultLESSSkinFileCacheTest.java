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
import org.xwiki.lesscss.LESSResourceReferenceSerializer;
import org.xwiki.lesscss.LESSSkinFileResourceReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link org.xwiki.lesscss.internal.cache.DefaultLESSSkinFileCache}.
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

    private CacheKeyFactory cacheKeyFactory;

    private CacheKeySerializer cacheKeySerializer;

    private LESSResourceReferenceSerializer lessResourceReferenceSerializer;

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
        cacheKeySerializer = mocker.getInstance(CacheKeySerializer.class);
        lessResourceReferenceSerializer = mocker.getInstance(LESSResourceReferenceSerializer.class);

        LESSSkinFileResourceReference lessSkinFileResourceReference = new LESSSkinFileResourceReference("file");
        CacheKey cacheKey = new CacheKey("skin", "colorTheme", lessSkinFileResourceReference);
        when(cacheKeyFactory.getCacheKey(eq("skin"), eq("colorTheme"),
            eq(new LESSSkinFileResourceReference("file")))).thenReturn(cacheKey);
        when(cacheKeySerializer.serialize(cacheKey)).thenReturn("4skin_25currentWiki:ColorTheme.CT_10FILE[file]");
    }

    @Test
    public void get() throws Exception
    {
        // Mock
        when(cache.get("4skin_25currentWiki:ColorTheme.CT_10FILE[file]")).thenReturn("Expected output");

        // Test
        String result = mocker.getComponentUnderTest().get(
            new LESSSkinFileResourceReference("file"), "skin", "colorTheme");

        // Verify
        assertEquals("Expected output", result);
    }

    @Test
    public void set() throws Exception
    {
        // Test
        mocker.getComponentUnderTest().set(
            new LESSSkinFileResourceReference("file"), "skin", "colorTheme", "css");

        // Verify
        verify(cache).set(eq("4skin_25currentWiki:ColorTheme.CT_10FILE[file]"), eq("css"));
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
        CacheKey cacheKey1 = new CacheKey("skin1", "colorTheme", file1);
        CacheKey cacheKey3 = new CacheKey("skin2", "colorTheme", file1);
        CacheKey cacheKey4 = new CacheKey("skin1", "colorTheme", file2);
        when(cacheKeyFactory.getCacheKey(eq("skin1"), eq("colorTheme"), eq(file1))).thenReturn(cacheKey1);
        when(cacheKeyFactory.getCacheKey(eq("skin2"), eq("colorTheme"), eq(file1))).thenReturn(cacheKey3);
        when(cacheKeyFactory.getCacheKey(eq("skin1"), eq("colorTheme"), eq(file2))).thenReturn(cacheKey4);
        when(cacheKeySerializer.serialize(eq(cacheKey1))).thenReturn("k1");
        when(cacheKeySerializer.serialize(eq(cacheKey3))).thenReturn("k3");
        when(cacheKeySerializer.serialize(eq(cacheKey4))).thenReturn("k4");

        // Add the first one twice
        mocker.getComponentUnderTest().set(new LESSSkinFileResourceReference("file1"), "skin1", "colorTheme", "css1");
        mocker.getComponentUnderTest().set(new LESSSkinFileResourceReference("file1"), "skin1", "colorTheme", "css1");

        // Others
        mocker.getComponentUnderTest().set(new LESSSkinFileResourceReference("file1"), "skin2", "colorTheme", "css2");
        mocker.getComponentUnderTest().set(new LESSSkinFileResourceReference("file2"), "skin1", "colorTheme", "css3");

        when(lessResourceReferenceSerializer.serialize(eq(file1))).thenReturn("FILE[file1]");
        when(lessResourceReferenceSerializer.serialize(eq(file2))).thenReturn("FILE[file2]");

        // Test
        mocker.getComponentUnderTest().clearFromSkin("skin1");

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
        CacheKey cacheKey1 = new CacheKey("skin1", "colorTheme1", file1);
        CacheKey cacheKey3 = new CacheKey("skin1", "colorTheme2", file1);
        CacheKey cacheKey4 = new CacheKey("skin2", "colorTheme1", file2);
        when(cacheKeyFactory.getCacheKey(eq("skin1"), eq("colorTheme1"), eq(file1))).thenReturn(cacheKey1);
        when(cacheKeyFactory.getCacheKey(eq("skin1"), eq("colorTheme2"), eq(file1))).thenReturn(cacheKey3);
        when(cacheKeyFactory.getCacheKey(eq("skin2"), eq("colorTheme1"), eq(file2))).thenReturn(cacheKey4);
        when(cacheKeySerializer.serialize(eq(cacheKey1))).thenReturn("k1");
        when(cacheKeySerializer.serialize(eq(cacheKey3))).thenReturn("k3");
        when(cacheKeySerializer.serialize(eq(cacheKey4))).thenReturn("k4");

        // Add the first one twice
        mocker.getComponentUnderTest().set(new LESSSkinFileResourceReference("file1"), "skin1", "colorTheme1", "css1");
        mocker.getComponentUnderTest().set(new LESSSkinFileResourceReference("file1"), "skin1", "colorTheme1", "css1");

        // Others
        mocker.getComponentUnderTest().set(new LESSSkinFileResourceReference("file1"), "skin1", "colorTheme2", "css2");
        mocker.getComponentUnderTest().set(new LESSSkinFileResourceReference("file2"), "skin2", "colorTheme1", "css3");

        when(lessResourceReferenceSerializer.serialize(eq(file1))).thenReturn("FILE[file1]");
        when(lessResourceReferenceSerializer.serialize(eq(file2))).thenReturn("FILE[file2]");

        // Test
        mocker.getComponentUnderTest().clearFromColorTheme("colorTheme1");

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

        CacheKey cacheKey1 = new CacheKey("skin1", "colorTheme1", file1);
        CacheKey cacheKey3 = new CacheKey("skin1", "colorTheme1", file2);
        CacheKey cacheKey4 = new CacheKey("skin2", "colorTheme1", file1);
        when(cacheKeyFactory.getCacheKey(eq("skin1"), eq("colorTheme1"), eq(file1))).thenReturn(cacheKey1);
        when(cacheKeyFactory.getCacheKey(eq("skin1"), eq("colorTheme1"), eq(file2))).thenReturn(cacheKey3);
        when(cacheKeyFactory.getCacheKey(eq("skin2"), eq("colorTheme1"), eq(file1))).thenReturn(cacheKey4);
        when(cacheKeySerializer.serialize(eq(cacheKey1))).thenReturn("k1");
        when(cacheKeySerializer.serialize(eq(cacheKey3))).thenReturn("k3");
        when(cacheKeySerializer.serialize(eq(cacheKey4))).thenReturn("k4");

        when(lessResourceReferenceSerializer.serialize(eq(file1))).thenReturn("FILE[file1]");
        when(lessResourceReferenceSerializer.serialize(eq(file2))).thenReturn("FILE[file2]");

        // Add the first one twice
        mocker.getComponentUnderTest().set(new LESSSkinFileResourceReference("file1"), "skin1", "colorTheme1", "css1");
        mocker.getComponentUnderTest().set(new LESSSkinFileResourceReference("file1"), "skin1", "colorTheme1", "css1");

        // Others
        mocker.getComponentUnderTest().set(new LESSSkinFileResourceReference("file2"), "skin1", "colorTheme1", "css2");
        mocker.getComponentUnderTest().set(new LESSSkinFileResourceReference("file1"), "skin2", "colorTheme1", "css3");

        // Test
        mocker.getComponentUnderTest().clearFromLESSResource(new LESSSkinFileResourceReference("file1"));

        // Verify
        verify(cache, times(1)).remove("k1");
        verify(cache).remove("k4");
        verify(cache, never()).remove("k3");
    }

}
