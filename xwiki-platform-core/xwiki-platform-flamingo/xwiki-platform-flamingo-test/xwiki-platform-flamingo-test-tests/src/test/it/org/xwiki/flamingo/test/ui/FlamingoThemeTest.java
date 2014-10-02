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

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.administration.test.po.PresentationAdministrationSectionPage;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * UI tests for the Flamingo Theme Application.
 *
 * @version $Id$
 * @since 6.3M1
 */
public class FlamingoThemeTest extends AbstractTest
{
    @Rule
    public SuperAdminAuthenticationRule superAdminAuthenticationRule =
            new SuperAdminAuthenticationRule(getUtil(), getDriver());
    @Test
    public void editFlamingoTheme() throws Exception
    {
        // Go to the presentation section of the administration
        AdministrationPage administrationPage = AdministrationPage.gotoPage();
        PresentationAdministrationSectionPage presentationAdministrationSectionPage =
                administrationPage.clickPresentationSection();

        // Test that all color themes are present
        testColorThemesArePresent(presentationAdministrationSectionPage);

        // Test that the color theme displayer group the color theme by application
        // I currently comment this test because the displayer is not set on XWikiPreferences by default...
        // testColorThemesDisplayer(presentationAdministrationSectionPage);

        // Select the 'Charcoal' color theme
        presentationAdministrationSectionPage.setColorTheme("Charcoal");
        assertEquals("Charcoal", presentationAdministrationSectionPage.getCurrentColorTheme());

        // Click on the 'customize' button
        presentationAdministrationSectionPage.clickOnCustomize();
    }

    /**
     * Test that all color themes are present
     */
    private void testColorThemesArePresent(PresentationAdministrationSectionPage presentationAdministrationSectionPage)
            throws Exception
    {
        List<String> colorThemes = presentationAdministrationSectionPage.getColorThemes();
        assertTrue(colorThemes.contains("Charcoal"));
        assertTrue(colorThemes.contains("Mint"));
        assertTrue(colorThemes.contains("Dusk"));
        assertTrue(colorThemes.contains("Ruby"));
        assertTrue(colorThemes.contains("Azure"));
    }

    /**
     * Test that the color theme displayer group the color theme by application
     */
    private void testColorThemesDisplayer(PresentationAdministrationSectionPage presentationAdministrationSectionPage)
            throws Exception
    {
        // Get the old color themes
        List<String> oldColorThemes = presentationAdministrationSectionPage.getColibriColorThemes();
        // Flamingo theme should be there
        assertTrue(!oldColorThemes.contains("Charcoal"));
        assertTrue(oldColorThemes.contains("Azure"));

        // Get the flamingo themes
        List<String> flamingoThemes = presentationAdministrationSectionPage.getFlamingoThemes();
        assertTrue(flamingoThemes.contains("Charcoal"));
        // Old color theme should not be there
        assertTrue(!flamingoThemes.contains("Azure"));
    }
}
