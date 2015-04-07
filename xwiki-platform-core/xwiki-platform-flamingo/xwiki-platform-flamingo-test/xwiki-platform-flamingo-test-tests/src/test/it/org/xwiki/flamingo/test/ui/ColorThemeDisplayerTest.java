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
package org.xwiki.flamingo.test.ui;

import java.util.List;

import org.junit.Test;
import org.xwiki.administration.test.po.ColorThemePropertyDisplayerPage;
import org.xwiki.test.ui.AbstractTest;

import static org.junit.Assert.assertTrue;

/**
 * UI tests for the displayer of the 'color theme' property.
 *
 * @version $Id$
 * @since 6.3M2
 */
public class ColorThemeDisplayerTest extends AbstractTest
{
    @Test
    public void colorThemeDisplayerTest() throws Exception
    {
        ColorThemePropertyDisplayerPage colorThemePropertyDisplayerPage = ColorThemePropertyDisplayerPage.gotoPage();
        testColorThemesArePresent(colorThemePropertyDisplayerPage);
        testColorThemesDisplayer(colorThemePropertyDisplayerPage);
    }

    /**
     * Test that all color themes are present
     */
    private void testColorThemesArePresent(ColorThemePropertyDisplayerPage colorThemePropertyDisplayerPage)
            throws Exception
    {
        List<String> colorThemes = colorThemePropertyDisplayerPage.getColorThemes();
        assertTrue(colorThemes.contains("Charcoal"));
        assertTrue(colorThemes.contains("Marina"));
        assertTrue(colorThemes.contains("Kitty"));
        assertTrue(colorThemes.contains("Garden"));
        assertTrue(colorThemes.contains("Mint"));
        assertTrue(colorThemes.contains("Dusk"));
        assertTrue(colorThemes.contains("Ruby"));
        assertTrue(colorThemes.contains("Azure"));
    }

    /**
     * Test that the color theme displayer group the color theme by application
     */
    private void testColorThemesDisplayer(ColorThemePropertyDisplayerPage colorThemePropertyDisplayerPage)
            throws Exception
    {
        // Get the old color themes
        List<String> oldColorThemes = colorThemePropertyDisplayerPage.getColibriColorThemes();
        // Flamingo theme should be there
        assertTrue(!oldColorThemes.contains("Charcoal"));
        assertTrue(oldColorThemes.contains("Azure"));

        // Get the flamingo themes
        List<String> flamingoThemes = colorThemePropertyDisplayerPage.getFlamingoThemes();
        assertTrue(flamingoThemes.contains("Charcoal"));
        // Old color theme should not be there
        assertTrue(!flamingoThemes.contains("Azure"));
    }
}
