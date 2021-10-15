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

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.lesscss.compiler.LESSCompilerException;
import org.xwiki.lesscss.internal.LESSContext;
import org.xwiki.lesscss.internal.cache.LESSResourcesCache;
import org.xwiki.lesscss.internal.colortheme.ColorThemeReference;
import org.xwiki.lesscss.internal.colortheme.ColorThemeReferenceFactory;
import org.xwiki.lesscss.internal.colortheme.CurrentColorThemeGetter;
import org.xwiki.lesscss.internal.colortheme.NamedColorThemeReference;
import org.xwiki.lesscss.internal.skin.FSSkinReference;
import org.xwiki.lesscss.internal.skin.SkinReference;
import org.xwiki.lesscss.internal.skin.SkinReferenceFactory;
import org.xwiki.lesscss.resources.LESSResourceReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
public class DefaultLESSCompilerTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultLESSCompiler> mocker =
            new MockitoComponentMockingRule<>(DefaultLESSCompiler.class);

    private LESSResourcesCache cache;

    private CachedLESSCompiler cachedLESSCompiler;

    private Provider<XWikiContext> xcontextProvider;

    private CurrentColorThemeGetter currentColorThemeGetter;

    private SkinReferenceFactory skinReferenceFactory;

    private ColorThemeReferenceFactory colorThemeReferenceFactory;

    private LESSContext lessContext;

    private XWikiContext xcontext;

    private XWiki xwiki;
    
    private LESSResourceReference lessResourceReference;
    
    private SkinReference skinReference = new FSSkinReference("skin");
    
    private ColorThemeReference colorThemeReference = new NamedColorThemeReference("colorTheme");

    @Before
    public void setUp() throws Exception
    {
        cache = mocker.getInstance(LESSResourcesCache.class);
        cachedLESSCompiler = mocker.getInstance(CachedLESSCompiler.class);
        currentColorThemeGetter = mocker.getInstance(CurrentColorThemeGetter.class);
        skinReferenceFactory = mocker.getInstance(SkinReferenceFactory.class);
        colorThemeReferenceFactory = mocker.getInstance(ColorThemeReferenceFactory.class);
        lessContext = mocker.getInstance(LESSContext.class);
        xcontextProvider = mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(xcontext);
        xwiki = mock(XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
        when(xwiki.getSkin(xcontext)).thenReturn("skin");
        when(currentColorThemeGetter.getCurrentColorTheme(true, "default")).thenReturn("colorTheme");
        when(skinReferenceFactory.createReference("skin")).thenReturn(skinReference);
        when(colorThemeReferenceFactory.createReference("colorTheme")).thenReturn(colorThemeReference);
        
        lessResourceReference = mock(LESSResourceReference.class);

        when(cache.getMutex(eq(lessResourceReference), eq(new FSSkinReference("skin")),
                eq(new NamedColorThemeReference("colorTheme")))).thenReturn("mutex");
    }

    @Test
    public void compileWhenInCache() throws Exception
    {
        // Mocks
        when(cache.get(eq(lessResourceReference), eq(new FSSkinReference("skin")),
                eq(new NamedColorThemeReference("colorTheme")))).thenReturn("cached output");

        // Test
        assertEquals("cached output",
                mocker.getComponentUnderTest().compile(lessResourceReference, false, false, false));

        // Verify
        verify(cache, never()).set(eq(lessResourceReference), eq(skinReference), eq(colorThemeReference), 
                eq("cache output"));
    }

    @Test
    public void compileWhenNotInCache() throws Exception
    {
        // Mocks
        when(cachedLESSCompiler.compute(eq(lessResourceReference), eq(false), eq(false), eq(true), eq("skin"))).
                thenReturn("compiled output");

        // Test
        assertEquals("compiled output",
                mocker.getComponentUnderTest().compile(lessResourceReference, false, false, false));

        // Verify
        verify(cache).set(eq(lessResourceReference), eq(skinReference), eq(colorThemeReference),
                eq("compiled output"));
    }

    @Test
    public void compileWhenInCacheButForced() throws Exception
    {
        // Mocks
        when(cachedLESSCompiler.compute(eq(lessResourceReference), eq(false), eq(false), eq(true), eq("skin"))).
                thenReturn("compiled output");

        // Test
        assertEquals("compiled output", mocker.getComponentUnderTest().compile(
                lessResourceReference, false, false, "skin", true));

        // Verify
        verify(cache, times(1)).set(any(LESSResourceReference.class), any(SkinReference.class),
                any(ColorThemeReference.class), any());
        verify(cache, never()).get(eq(lessResourceReference), eq(skinReference), eq(colorThemeReference));
    }

    @Test
    public void compileSkinFileWhenInCacheButCacheDisabled() throws Exception
    {
        // Mock
        when(lessContext.isCacheDisabled()).thenReturn(true);
        when(cachedLESSCompiler.compute(eq(lessResourceReference), eq(false), eq(false),eq(true), eq("skin"))).
                thenReturn("compiled output");

        // Test
        assertEquals("compiled output", 
                mocker.getComponentUnderTest().compile(lessResourceReference, false, false, "skin", true));

        // Verify that the cache is disabled
        verifyNoInteractions(cache);
    }

    @Test
    public void compileWhenInCacheAndHTMLExport() throws Exception
    {
        // Mocks
        when(cache.get(eq(lessResourceReference), eq(skinReference), 
                eq(colorThemeReference))).thenReturn("cached output");
        
        when(lessContext.isHtmlExport()).thenReturn(true);

        // Test
        assertEquals("cached output",
                mocker.getComponentUnderTest().compile(lessResourceReference, false, true, false));
        
        // Verify that the velocity is executed
        verify(cachedLESSCompiler).compute(eq(lessResourceReference), eq(false), eq(true),
                eq(false), eq("skin"));
        
        // Verify we don't put anything in the cache
        verify(cache, never()).set(any(LESSResourceReference.class), any(SkinReference.class),
                any(ColorThemeReference.class), any());
    }
    
    @Test
    public void compileWhenError() throws Exception
    {
        // Mocks
        LESSCompilerException expectedException = new LESSCompilerException("an exception");
        when(cachedLESSCompiler.compute(any(LESSResourceReference.class), anyBoolean(), anyBoolean(), anyBoolean(),
                any())).thenThrow(expectedException);
        
        // Test
        String result = mocker.getComponentUnderTest().compile(lessResourceReference, false, false, false);
        
        // Asserts
        assertTrue(StringUtils.startsWith(result, "/* org.xwiki.lesscss.compiler.LESSCompilerException: an exception"));
        assertTrue(StringUtils.endsWith(result, "*/"));
        verify(cache).set(eq(lessResourceReference), eq(skinReference), eq(colorThemeReference), eq(result));
        verify(mocker.getMockedLogger()).error(eq("Error during the compilation of the resource [{}]."),
                eq(lessResourceReference), eq(expectedException));
    }

}
