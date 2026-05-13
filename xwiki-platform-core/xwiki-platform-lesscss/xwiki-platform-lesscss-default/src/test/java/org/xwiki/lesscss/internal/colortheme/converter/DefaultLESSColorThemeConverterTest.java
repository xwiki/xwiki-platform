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
package org.xwiki.lesscss.internal.colortheme.converter;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.lesscss.internal.cache.ColorThemeCache;
import org.xwiki.lesscss.internal.colortheme.ColorTheme;
import org.xwiki.lesscss.internal.colortheme.ColorThemeReferenceFactory;
import org.xwiki.lesscss.internal.colortheme.CurrentColorThemeGetter;
import org.xwiki.lesscss.internal.colortheme.NamedColorThemeReference;
import org.xwiki.lesscss.internal.resources.LESSSkinFileResourceReference;
import org.xwiki.lesscss.internal.skin.FSSkinReference;
import org.xwiki.lesscss.internal.skin.SkinReferenceFactory;
import org.xwiki.lesscss.resources.LESSResourceReferenceFactory;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link org.xwiki.lesscss.internal.colortheme.converter.DefaultLESSColorThemeConverter}.
 *
 * @version $Id$
 * @since 7.0RC1
 */
@ComponentTest
class DefaultLESSColorThemeConverterTest
{
    @InjectMockComponents
    private DefaultLESSColorThemeConverter defaultLESSColorThemeConverter;

    @MockComponent
    private ColorThemeCache cache;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private CurrentColorThemeGetter currentColorThemeGetter;

    @MockComponent
    private SkinReferenceFactory skinReferenceFactory;

    @MockComponent
    private ColorThemeReferenceFactory colorThemeReferenceFactory;

    @MockComponent
    private LESSResourceReferenceFactory lessResourceReferenceFactory;

    @Mock
    private XWikiContext xcontext;

    @Mock
    private XWiki xwiki;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);
        when(this.xwiki.getSkin(this.xcontext)).thenReturn("skin");
        when(this.currentColorThemeGetter.getCurrentColorTheme(true, "default")).thenReturn("colorTheme");
        when(this.skinReferenceFactory.createReference("skin")).thenReturn(new FSSkinReference("skin"));
        when(this.colorThemeReferenceFactory.createReference("colorTheme"))
            .thenReturn(new NamedColorThemeReference("colorTheme"));
        when(this.cache.getMutex(new LESSSkinFileResourceReference("file", null, null), new FSSkinReference("skin"),
            new NamedColorThemeReference("colorTheme"))).thenReturn("mutex");
    }

    @Test
    void getColorThemeFromSkinFileWhenInCache() throws Exception
    {
        // Mocks
        ColorTheme cachedColorTheme = new ColorTheme();
        cachedColorTheme.put("key", "value1");

        LESSSkinFileResourceReference lessSkinFileResourceReference
            = new LESSSkinFileResourceReference("file", null, null);
        when(this.lessResourceReferenceFactory.createReferenceForSkinFile("file")).thenReturn(
            lessSkinFileResourceReference);

        when(this.cache.get(lessSkinFileResourceReference, new FSSkinReference("skin"),
            new NamedColorThemeReference("colorTheme"))).thenReturn(cachedColorTheme);

        // Test
        ColorTheme result = this.defaultLESSColorThemeConverter.getColorThemeFromSkinFile("file", "skin", false);

        // Verify
        assertEquals(cachedColorTheme, result);
        // Verify that the returned value is not the instance stored in the cache (that the end-user would wrongly be
        // able to modify)
        assertNotSame(result, cachedColorTheme);
        // Be extra-sure :)
        result.put("key", "value2");
        assertNotEquals("value2", cachedColorTheme.get("key"));
    }
}
