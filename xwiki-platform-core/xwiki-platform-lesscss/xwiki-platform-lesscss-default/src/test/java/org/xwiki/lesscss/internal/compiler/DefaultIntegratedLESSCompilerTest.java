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
package org.xwiki.lesscss.internal.compiler;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.lesscss.LESSResourceReference;
import org.xwiki.lesscss.LESSSkinFileCache;
import org.xwiki.lesscss.LESSSkinFileResourceReference;
import org.xwiki.lesscss.internal.cache.CacheKey;
import org.xwiki.lesscss.internal.cache.CacheKeyFactory;
import org.xwiki.lesscss.internal.colortheme.CurrentColorThemeGetter;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
public class DefaultIntegratedLESSCompilerTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultIntegratedLESSCompiler> mocker =
            new MockitoComponentMockingRule<>(DefaultIntegratedLESSCompiler.class);

    private LESSSkinFileCache cache;

    private CachedIntegratedLESSCompiler cachedIntegratedLESSCompiler;

    protected Provider<XWikiContext> xcontextProvider;

    private CurrentColorThemeGetter currentColorThemeGetter;

    private CacheKeyFactory cacheKeyFactory;

    private XWikiContext xcontext;

    private XWiki xwiki;

    @Before
    public void setUp() throws Exception
    {
        cache = mocker.getInstance(LESSSkinFileCache.class);
        cachedIntegratedLESSCompiler = mocker.getInstance(CachedIntegratedLESSCompiler.class);
        currentColorThemeGetter = mocker.getInstance(CurrentColorThemeGetter.class);
        cacheKeyFactory = mocker.getInstance(CacheKeyFactory.class);
        xcontextProvider = mocker.getInstance(new DefaultParameterizedType(null, Provider.class, XWikiContext.class));
        xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(xcontext);
        xwiki = mock(XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
        when(xwiki.getSkin(xcontext)).thenReturn("skin");
        when(currentColorThemeGetter.getCurrentColorTheme("default")).thenReturn("colorTheme");
    }

    @Test
    public void compileWhenInCache() throws Exception
    {
        LESSSkinFileResourceReference resource = new LESSSkinFileResourceReference("file");

        // Mocks
        CacheKey cacheKey = new CacheKey("skin", "colorTheme", resource);
        when(cacheKeyFactory.getCacheKey(eq("skin"), eq("colorTheme"), eq(new LESSSkinFileResourceReference("file")))).
            thenReturn(cacheKey);
        when(cache.get(eq(resource), eq("skin"), eq("colorTheme"))).thenReturn("cached output");

        // Test
        assertEquals("cached output",
                mocker.getComponentUnderTest().compile(new LESSSkinFileResourceReference("file"), false, false));

        // Verify
        verify(cache, never()).set(any(LESSResourceReference.class), anyString(), anyString(), anyString());
    }

    @Test
    public void compileWhenNotInCache() throws Exception
    {
        LESSSkinFileResourceReference resource = new LESSSkinFileResourceReference("file");

        // Mocks
        CacheKey cacheKey = new CacheKey("skin", "colorTheme", resource);
        when(cacheKeyFactory.getCacheKey(eq("skin"), eq("colorTheme"), eq(new LESSSkinFileResourceReference("file")))).
                thenReturn(cacheKey);
        when(cachedIntegratedLESSCompiler.compute(eq(resource), eq(false), eq("skin"))).thenReturn("compiled output");

        // Test
        assertEquals("compiled output",
                mocker.getComponentUnderTest().compile(new LESSSkinFileResourceReference("file"), false, false));

        // Verify
        verify(cache).set(eq(resource), eq("skin"), eq("colorTheme"), eq("compiled output"));
    }

    @Test
    public void compileWhenInCacheButForced() throws Exception
    {
        LESSSkinFileResourceReference resource = new LESSSkinFileResourceReference("file");

        // Mocks
        CacheKey cacheKey = new CacheKey("skin", "colorTheme", resource);
        when(cacheKeyFactory.getCacheKey(eq("skin"), eq("colorTheme"), eq(new LESSSkinFileResourceReference("file")))).
                thenReturn(cacheKey);
        when(cachedIntegratedLESSCompiler.compute(eq(resource), eq(false), eq("skin"))).thenReturn("compiled output");

        // Test
        assertEquals("compiled output",
                mocker.getComponentUnderTest().compile(new LESSSkinFileResourceReference("file"), false, "skin", true));

        // Verify
        verify(cache, times(1)).set(any(LESSResourceReference.class), anyString(), anyString(), anyString());
        verify(cache, never()).get(eq(resource), eq("skin"), eq("colorTheme"));
    }
}
