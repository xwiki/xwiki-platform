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
import org.xwiki.lesscss.LESSSkinFileResourceReference;
import org.xwiki.lesscss.internal.colortheme.ColorThemeFullNameGetter;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class CacheKeyFactoryTest
{
    @Rule
    public MockitoComponentMockingRule<CacheKeyFactory> mocker =
            new MockitoComponentMockingRule<>(CacheKeyFactory.class);

    private ColorThemeFullNameGetter colorThemeFullNameGetter;

    @Before
    public void setUp() throws Exception
    {
        colorThemeFullNameGetter = mocker.getInstance(ColorThemeFullNameGetter.class);
    }

    @Test
    public void getCacheKey() throws Exception
    {
        // Mock
        when(colorThemeFullNameGetter.getColorThemeFullName("ColorTheme")).thenReturn("wiki:Space.ColorTheme");

        // Test
        CacheKey cacheKey = mocker.getComponentUnderTest().getCacheKey("skin", "ColorTheme",
            new LESSSkinFileResourceReference("file"));

        // Verify
        assertEquals("skin", cacheKey.getSkin());
        assertEquals("wiki:Space.ColorTheme", cacheKey.getColorTheme());
        assertEquals(new LESSSkinFileResourceReference("file"), cacheKey.getLessResourceReference());
    }


}
