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
import org.xwiki.lesscss.colortheme.ColorThemeReferenceSerializer;
import org.xwiki.lesscss.colortheme.NamedColorThemeReference;
import org.xwiki.lesscss.resources.LESSResourceReferenceSerializer;
import org.xwiki.lesscss.resources.LESSSkinFileResourceReference;
import org.xwiki.lesscss.skin.FSSkinReference;
import org.xwiki.lesscss.skin.SkinReferenceSerializer;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

public class CacheKeyFactoryTest
{
    @Rule
    public MockitoComponentMockingRule<CacheKeyFactory> mocker =
            new MockitoComponentMockingRule<>(CacheKeyFactory.class);

    private LESSResourceReferenceSerializer lessResourceReferenceSerializer;

    private SkinReferenceSerializer skinReferenceSerializer;

    private ColorThemeReferenceSerializer colorThemeReferenceSerializer;
    
    private XWikiContextCacheKeyFactory xcontextCacheKeyFactory;

    @Before
    public void setUp() throws Exception
    {
        lessResourceReferenceSerializer = mocker.getInstance(LESSResourceReferenceSerializer.class);
        skinReferenceSerializer = mocker.getInstance(SkinReferenceSerializer.class);
        colorThemeReferenceSerializer = mocker.getInstance(ColorThemeReferenceSerializer.class);
        xcontextCacheKeyFactory = mocker.getInstance(XWikiContextCacheKeyFactory.class);
    }

    @Test
    public void getCacheKeyWithContext() throws Exception
    {
        // Mocks
        when(lessResourceReferenceSerializer.serialize(eq(new LESSSkinFileResourceReference("file")))).
                thenReturn("file");
        when(skinReferenceSerializer.serialize(eq(new FSSkinReference("skin")))).thenReturn("skin");
        when(colorThemeReferenceSerializer.serialize(new NamedColorThemeReference("colorTheme"))).thenReturn("colorTheme");
        when(xcontextCacheKeyFactory.getCacheKey()).thenReturn("XWikiContext[Mock]");

        // Test
        assertEquals("4_file_4_skin_10_colorTheme_18_XWikiContext[Mock]",
                mocker.getComponentUnderTest().getCacheKey(
                        new LESSSkinFileResourceReference("file"), new FSSkinReference("skin"),
                        new NamedColorThemeReference("colorTheme"), true));
    }

    @Test
    public void getCacheKeyWithoutContext() throws Exception
    {
        // Mocks
        when(lessResourceReferenceSerializer.serialize(eq(new LESSSkinFileResourceReference("file")))).
                thenReturn("file");
        when(skinReferenceSerializer.serialize(eq(new FSSkinReference("skin")))).thenReturn("skin");
        when(colorThemeReferenceSerializer.serialize(new NamedColorThemeReference("colorTheme"))).thenReturn("colorTheme");

        // Test
        assertEquals("4_file_4_skin_10_colorTheme",
                mocker.getComponentUnderTest().getCacheKey(
                        new LESSSkinFileResourceReference("file"), new FSSkinReference("skin"),
                        new NamedColorThemeReference("colorTheme"), false));
    }


}
