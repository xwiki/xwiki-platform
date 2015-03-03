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
import org.xwiki.lesscss.cache.LESSResourcesCache;
import org.xwiki.lesscss.colortheme.ColorThemeReference;
import org.xwiki.lesscss.colortheme.ColorThemeReferenceFactory;
import org.xwiki.lesscss.colortheme.NamedColorThemeReference;
import org.xwiki.lesscss.internal.LESSContext;
import org.xwiki.lesscss.internal.colortheme.CurrentColorThemeGetter;
import org.xwiki.lesscss.resources.LESSResourceReference;
import org.xwiki.lesscss.resources.LESSSkinFileResourceReference;
import org.xwiki.lesscss.skin.FSSkinReference;
import org.xwiki.lesscss.skin.SkinReference;
import org.xwiki.lesscss.skin.SkinReferenceFactory;
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
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
public class DefaultIntegratedLESSCompilerTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultIntegratedLESSCompiler> mocker =
            new MockitoComponentMockingRule<>(DefaultIntegratedLESSCompiler.class);

    private LESSResourcesCache cache;

    private CachedIntegratedLESSCompiler cachedIntegratedLESSCompiler;

    private Provider<XWikiContext> xcontextProvider;

    private CurrentColorThemeGetter currentColorThemeGetter;

    private SkinReferenceFactory skinReferenceFactory;

    private ColorThemeReferenceFactory colorThemeReferenceFactory;

    private LESSContext lessContext;

    private XWikiContext xcontext;

    private XWiki xwiki;

    @Before
    public void setUp() throws Exception
    {
        cache = mocker.getInstance(LESSResourcesCache.class);
        cachedIntegratedLESSCompiler = mocker.getInstance(CachedIntegratedLESSCompiler.class);
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
        when(skinReferenceFactory.createReference("skin")).thenReturn(new FSSkinReference("skin"));
        when(colorThemeReferenceFactory.createReference("colorTheme")).thenReturn(
                new NamedColorThemeReference("colorTheme"));
        when(cache.getMutex(eq(new LESSSkinFileResourceReference("file")), eq(new FSSkinReference("skin")),
                eq(new NamedColorThemeReference("colorTheme")))).thenReturn("mutex");
    }

    @Test
    public void compileWhenInCache() throws Exception
    {
        // Mocks
        when(cache.get(eq(new LESSSkinFileResourceReference("file")), eq(new FSSkinReference("skin")),
                eq(new NamedColorThemeReference("colorTheme")))).thenReturn("cached output");

        // Test
        assertEquals("cached output",
                mocker.getComponentUnderTest().compile(new LESSSkinFileResourceReference("file"), false, false, false));

        // Verify
        verify(cache, never()).set(any(LESSResourceReference.class), any(SkinReference.class),
                any(ColorThemeReference.class), anyString());
    }

    @Test
    public void compileWhenNotInCache() throws Exception
    {
        LESSSkinFileResourceReference resource = new LESSSkinFileResourceReference("file");

        // Mocks
        when(cachedIntegratedLESSCompiler.compute(eq(resource), eq(false), eq(false), eq(true), eq("skin"))).
                thenReturn("compiled output");

        // Test
        assertEquals("compiled output",
                mocker.getComponentUnderTest().compile(new LESSSkinFileResourceReference("file"), false, false, false));

        // Verify
        verify(cache).set(eq(resource), eq(new FSSkinReference("skin")), eq(new NamedColorThemeReference("colorTheme")),
                eq("compiled output"));
    }

    @Test
    public void compileWhenInCacheButForced() throws Exception
    {
        LESSSkinFileResourceReference resource = new LESSSkinFileResourceReference("file");

        // Mocks
        when(cachedIntegratedLESSCompiler.compute(eq(resource), eq(false), eq(false), eq(true), eq("skin"))).
                thenReturn("compiled output");

        // Test
        assertEquals("compiled output", mocker.getComponentUnderTest().compile(
                new LESSSkinFileResourceReference("file"), false, false, "skin", true));

        // Verify
        verify(cache, times(1)).set(any(LESSResourceReference.class), any(SkinReference.class),
                any(ColorThemeReference.class), anyString());
        verify(cache, never()).get(eq(resource), eq(new FSSkinReference("skin")),
                eq(new NamedColorThemeReference("colorTheme")));
    }

    @Test
    public void compileSkinFileWhenInCacheButCacheDisabled() throws Exception
    {
        // Mock
        LESSSkinFileResourceReference resource = new LESSSkinFileResourceReference("file");
        when(lessContext.isCacheDisabled()).thenReturn(true);
        when(cachedIntegratedLESSCompiler.compute(eq(resource), eq(false), eq(false),eq(true), eq("skin"))).
                thenReturn("compiled output");

        // Test
        assertEquals("compiled output", mocker.getComponentUnderTest().compile(resource, false, false, "skin", true));

        // Verify that the cache is disabled
        verifyZeroInteractions(cache);
    }

    @Test
    public void compileWhenInCacheAndHTMLExport() throws Exception
    {
        // Mocks
        when(cache.get(eq(new LESSSkinFileResourceReference("file")), eq(new FSSkinReference("skin")),
                eq(new NamedColorThemeReference("colorTheme")))).thenReturn("cached output");
        
        when(lessContext.isHtmlExport()).thenReturn(true);

        // Test
        assertEquals("cached output",
                mocker.getComponentUnderTest().compile(new LESSSkinFileResourceReference("file"), false, true, false));
        
        // Verify that the velocity is executed
        verify(cachedIntegratedLESSCompiler).compute(eq(new LESSSkinFileResourceReference("file")), eq(false), eq(true),
                eq(false), eq("skin"));
        // Verify we don't put anything in the cache
        verify(cache, never()).set(any(LESSResourceReference.class), any(SkinReference.class),
                any(ColorThemeReference.class), anyString());
    }

}
