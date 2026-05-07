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

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.lesscss.internal.colortheme.ColorThemeReference;
import org.xwiki.lesscss.internal.skin.SkinReference;
import org.xwiki.lesscss.resources.LESSResourceReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ComponentTest
class CacheKeyFactoryTest
{
    @InjectMockComponents
    private CacheKeyFactory cacheKeyFactory;

    @MockComponent
    private XWikiContextCacheKeyFactory xcontextCacheKeyFactory;

    @MockComponent
    private Container container;

    @Test
    void getCacheKeyWithContext()
    {
        // Mocks
        LESSResourceReference lessResource = mock(LESSResourceReference.class);
        SkinReference skin = mock(SkinReference.class);
        ColorThemeReference colorTheme = mock(ColorThemeReference.class);

        when(lessResource.serialize()).thenReturn("lessResource");
        when(skin.serialize()).thenReturn("skin");
        when(colorTheme.serialize()).thenReturn("colorTheme");
        when(this.xcontextCacheKeyFactory.getCacheKey()).thenReturn("XWikiContext[Mock]");

        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HashMap<String, String[]> parameters = new HashMap<>();
        parameters.put("test", new String[] { "a", "b" });
        when(httpRequest.getParameterMap()).thenReturn(parameters);
        final ServletRequest servletRequest = new ServletRequest(httpRequest);
        when(this.container.getRequest()).thenReturn(servletRequest);

        // Test
        assertEquals("12_lessResource_4_skin_10_colorTheme_test:a|b_18_XWikiContext[Mock]",
            this.cacheKeyFactory.getCacheKey(lessResource, skin, colorTheme, true));
    }

    @Test
    void getCacheKeyWithoutContext()
    {
        // Mocks
        LESSResourceReference lessResource = mock(LESSResourceReference.class);
        SkinReference skin = mock(SkinReference.class);
        ColorThemeReference colorTheme = mock(ColorThemeReference.class);

        when(lessResource.serialize()).thenReturn("lessResource");
        when(skin.serialize()).thenReturn("skin");
        when(colorTheme.serialize()).thenReturn("colorTheme");

        // Test
        assertEquals("12_lessResource_4_skin_10_colorTheme",
            this.cacheKeyFactory.getCacheKey(lessResource, skin, colorTheme, false));
    }
}
