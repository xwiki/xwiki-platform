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

import org.apache.commons.lang3.Strings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
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
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
@ComponentTest
class DefaultLESSCompilerTest
{
    @InjectMockComponents
    private DefaultLESSCompiler defaultLESSCompiler;

    @MockComponent
    private LESSResourcesCache cache;

    @MockComponent
    private CachedLESSCompiler cachedLESSCompiler;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private CurrentColorThemeGetter currentColorThemeGetter;

    @MockComponent
    private SkinReferenceFactory skinReferenceFactory;

    @MockComponent
    private ColorThemeReferenceFactory colorThemeReferenceFactory;

    @MockComponent
    private LESSContext lessContext;

    @MockComponent
    private Logger logger;

    @Mock
    private XWikiContext xcontext;

    @Mock
    private XWiki xwiki;

    @Mock
    private LESSResourceReference lessResourceReference;

    private final SkinReference skinReference = new FSSkinReference("skin");

    private final ColorThemeReference colorThemeReference = new NamedColorThemeReference("colorTheme");

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);
        when(this.xwiki.getSkin(this.xcontext)).thenReturn("skin");
        when(this.currentColorThemeGetter.getCurrentColorTheme(true, "default")).thenReturn("colorTheme");
        when(this.skinReferenceFactory.createReference("skin")).thenReturn(this.skinReference);
        when(this.colorThemeReferenceFactory.createReference("colorTheme")).thenReturn(this.colorThemeReference);

        when(this.cache.getMutex(this.lessResourceReference, new FSSkinReference("skin"),
            new NamedColorThemeReference("colorTheme"))).thenReturn("mutex");
    }

    @Test
    void compileWhenInCache() throws Exception
    {
        // Mocks
        when(this.cache.get(this.lessResourceReference, new FSSkinReference("skin"),
            new NamedColorThemeReference("colorTheme"))).thenReturn("cached output");

        // Test
        assertEquals("cached output",
            this.defaultLESSCompiler.compile(this.lessResourceReference, false, false, false));

        // Verify
        verify(this.cache, never()).set(this.lessResourceReference, this.skinReference, this.colorThemeReference,
            "cache output");
    }

    @Test
    void compileWhenNotInCache() throws Exception
    {
        // Mocks
        when(this.cachedLESSCompiler.compute(this.lessResourceReference, false, false, true, "skin"))
            .thenReturn("compiled output");

        // Test
        assertEquals("compiled output",
            this.defaultLESSCompiler.compile(this.lessResourceReference, false, false, false));

        // Verify
        verify(this.cache).set(this.lessResourceReference, this.skinReference, this.colorThemeReference,
            "compiled output");
    }

    @Test
    void compileWhenInCacheButForced() throws Exception
    {
        // Mocks
        when(this.cachedLESSCompiler.compute(this.lessResourceReference, false, false, true, "skin"))
            .thenReturn("compiled output");

        // Test
        assertEquals("compiled output", this.defaultLESSCompiler.compile(
            this.lessResourceReference, false, false, "skin", true));

        // Verify
        verify(this.cache).set(any(LESSResourceReference.class), any(SkinReference.class),
            any(ColorThemeReference.class), any());
        verify(this.cache, never()).get(this.lessResourceReference, this.skinReference, this.colorThemeReference);
    }

    @Test
    void compileSkinFileWhenInCacheButCacheDisabled() throws Exception
    {
        // Mock
        when(this.lessContext.isCacheDisabled()).thenReturn(true);
        when(this.cachedLESSCompiler.compute(this.lessResourceReference, false, false, true, "skin"))
            .thenReturn("compiled output");

        // Test
        assertEquals("compiled output",
            this.defaultLESSCompiler.compile(this.lessResourceReference, false, false, "skin", true));

        // Verify that the cache is disabled
        verifyNoInteractions(this.cache);
    }

    @Test
    void compileWhenInCacheAndHTMLExport() throws Exception
    {
        // Mocks
        when(this.cache.get(this.lessResourceReference, this.skinReference, this.colorThemeReference))
            .thenReturn("cached output");

        when(this.lessContext.isHtmlExport()).thenReturn(true);

        // Test
        assertEquals("cached output",
            this.defaultLESSCompiler.compile(this.lessResourceReference, false, true, false));

        // Verify that the velocity is executed
        verify(this.cachedLESSCompiler).compute(this.lessResourceReference, false, true, false, "skin");

        // Verify we don't put anything in the cache
        verify(this.cache, never()).set(any(LESSResourceReference.class), any(SkinReference.class),
            any(ColorThemeReference.class), any());
    }

    @Test
    void compileWhenError() throws Exception
    {
        // Mocks
        LESSCompilerException expectedException = new LESSCompilerException("an exception");
        when(this.cachedLESSCompiler.compute(any(LESSResourceReference.class), anyBoolean(), anyBoolean(), anyBoolean(),
            any())).thenThrow(expectedException);

        // Test
        String result = this.defaultLESSCompiler.compile(this.lessResourceReference, false, false, false);

        // Asserts
        assertTrue(Strings.CS.startsWith(result, "/* org.xwiki.lesscss.compiler.LESSCompilerException: an exception"));
        assertTrue(Strings.CS.endsWith(result, "*/"));
        verify(this.cache).set(this.lessResourceReference, this.skinReference, this.colorThemeReference, result);
        verify(this.logger).error("Error during the compilation of the resource [{}].", this.lessResourceReference,
            expectedException);
    }
}
