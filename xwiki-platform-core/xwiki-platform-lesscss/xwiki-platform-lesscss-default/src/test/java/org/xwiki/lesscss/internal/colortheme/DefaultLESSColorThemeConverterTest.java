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
package org.xwiki.lesscss.internal.colortheme;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.lesscss.cache.ColorThemeCache;
import org.xwiki.lesscss.colortheme.ColorTheme;
import org.xwiki.lesscss.colortheme.ColorThemeReferenceFactory;
import org.xwiki.lesscss.colortheme.NamedColorThemeReference;
import org.xwiki.lesscss.resources.LESSSkinFileResourceReference;
import org.xwiki.lesscss.skin.FSSkinReference;
import org.xwiki.lesscss.skin.SkinReferenceFactory;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link org.xwiki.lesscss.internal.colortheme.DefaultLESSColorThemeConverter}.
 *
 * @version $Id$
 * @since 6.4M3
 */
public class DefaultLESSColorThemeConverterTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultLESSColorThemeConverter> mocker =
            new MockitoComponentMockingRule<>(DefaultLESSColorThemeConverter.class);

    private ColorThemeCache cache;

    private Provider<XWikiContext> xcontextProvider;

    private CurrentColorThemeGetter currentColorThemeGetter;

    private SkinReferenceFactory skinReferenceFactory;

    private ColorThemeReferenceFactory colorThemeReferenceFactory;

    private XWikiContext xcontext;

    private XWiki xwiki;

    @Before
    public void setUp() throws Exception
    {
        cache = mocker.getInstance(ColorThemeCache.class);
        currentColorThemeGetter = mocker.getInstance(CurrentColorThemeGetter.class);
        skinReferenceFactory = mocker.getInstance(SkinReferenceFactory.class);
        colorThemeReferenceFactory = mocker.getInstance(ColorThemeReferenceFactory.class);
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
    public void getColorThemeFromSkinFileWhenInCache() throws Exception
    {
        // Mocks
        ColorTheme cachedColorTheme = new ColorTheme();
        cachedColorTheme.put("key", "value1");
        when(cache.get(eq(new LESSSkinFileResourceReference("file")), eq(new FSSkinReference("skin")),
                eq(new NamedColorThemeReference("colorTheme")))).thenReturn(cachedColorTheme);

        // Test
        ColorTheme result = mocker.getComponentUnderTest().getColorThemeFromSkinFile("file", "skin", false);

        // Verify
        assertEquals(cachedColorTheme, result);
        // Verify that the returned value is not the instance stored in the cache (that the end-user would wrongly be
        // able to modify)
        assertTrue(result != cachedColorTheme);
        // Be extra-sure :)
        result.put("key", "value2");
        assertNotEquals("value2", cachedColorTheme.get("key"));

    }

}
