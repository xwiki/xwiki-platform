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
import org.xwiki.administration.test.po.ThemesAdministrationSectionPage;
import org.xwiki.flamingo.test.po.EditThemePage;
import org.xwiki.flamingo.test.po.PreviewBox;
import org.xwiki.flamingo.test.po.ThemeApplicationWebHomePage;
import org.xwiki.flamingo.test.po.ViewThemePage;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.test.ui.po.ConfirmationPage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
    public SuperAdminAuthenticationRule superAdminAuthenticationRule = new SuperAdminAuthenticationRule(getUtil());

    @Test
    public void editFlamingoTheme() throws Exception
    {
        // Go to the presentation section of the administration
        AdministrationPage administrationPage = AdministrationPage.gotoPage();
        ThemesAdministrationSectionPage presentationAdministrationSectionPage =
                administrationPage.clickThemesSection();
        // Click on "manage color theme"
        presentationAdministrationSectionPage.manageColorThemes();
        ThemeApplicationWebHomePage themeApplicationWebHomePage = new ThemeApplicationWebHomePage();
        EditThemePage editThemePage = themeApplicationWebHomePage.createNewTheme(getTestMethodName());
        editThemePage.clickSaveAndView();

        administrationPage = AdministrationPage.gotoPage();
        presentationAdministrationSectionPage = administrationPage.clickThemesSection();

        // Select a color theme
        presentationAdministrationSectionPage.setColorTheme(getTestMethodName());
        assertEquals(getTestMethodName(), presentationAdministrationSectionPage.getCurrentColorTheme());

        // Click on the 'customize' button
        presentationAdministrationSectionPage.clickOnCustomize();
        editThemePage = new EditThemePage();

        // Wait for the preview to be fully loaded
        assertTrue(editThemePage.isPreviewBoxLoading());
        editThemePage.waitUntilPreviewIsLoaded();
        // First, disable auto refresh because it slows down the test
        // (and can even make it fails if the computer is slow)
        editThemePage.setAutoRefresh(false);

        verifyAllVariablesCategoriesArePresent(editThemePage);
        verifyVariablesCategoriesDoesNotDisappear(editThemePage);
        verifyThatPreviewWorks(editThemePage);

        // We do not have a way top clear the browser's cache with selenium
        // (see http://stackoverflow.com/questions/19310888/clear-browser-cache-using-selenium-webdriver).
        // So we cannot ensure that saving an existing theme works.

        // Delete the test page
        getUtil().rest().deletePage("FlamingoThemes", getTestMethodName());
    }

    private void verifyAllVariablesCategoriesArePresent(EditThemePage editThemePage) throws Exception
    {
        List<String> categories = editThemePage.getVariableCategories();
        assertEquals(11, categories.size());
        assertTrue(categories.contains("Logos"));
        assertTrue(categories.contains("Base colors"));
        assertTrue(categories.contains("Typography"));
        assertTrue(categories.contains("Tables"));
        assertTrue(categories.contains("Buttons"));
        assertTrue(categories.contains("Navigation Bar"));
        assertTrue(categories.contains("Drop downs"));
        assertTrue(categories.contains("Forms"));
        assertTrue(categories.contains("Panels"));
        assertTrue(categories.contains("Breadcrumb"));
        assertTrue(categories.contains("Advanced"));
    }

    private void verifyVariablesCategoriesDoesNotDisappear(EditThemePage editThemePage) throws Exception
    {
        // Because of an incompatibility between PrototypeJS and Bootstrap, the variables categories can disappear
        // (see: https://jira.xwiki.org/browse/XWIKI-11670).
        // This test verifies that the bug is still fixed.
        assertEquals(11, editThemePage.getVariableCategories().size());
        // We click on different categories
        editThemePage.selectVariableCategory("Base colors");
        editThemePage.selectVariableCategory("Typography");
        // We verify that they are still there
        assertEquals(11, editThemePage.getVariableCategories().size());
    }

    /**
     * @since 6.3M2
     */
    private void verifyThatPreviewWorks(EditThemePage editThemePage) throws Exception
    {
        // Verify that the preview is working with the current values
        PreviewBox previewBox = editThemePage.getPreviewBox();
        assertFalse(previewBox.hasError());
        // Select a variable category and change value
        editThemePage.selectVariableCategory("Base colors");
        editThemePage.setVariableValue("xwiki-page-content-bg", "#ff0000");
        // Again...
        editThemePage.selectVariableCategory("Typography");
        editThemePage.setVariableValue("font-family-base", "Monospace");
        // Test that the @lessCode variable is handled too!
        editThemePage.selectVariableCategory("Advanced");
        editThemePage.setTextareaValue("lessCode", ".main{ color: #0000ff; }");
        // Refresh
        editThemePage.refreshPreview();
        // Verify that there is still no errors
        assertFalse(previewBox.hasError());
        // Verify that the modification have been made in the preview
        assertEquals("rgba(255, 0, 0, 1)", previewBox.getPageBackgroundColor());
        assertEquals("monospace", previewBox.getFontFamily());
        // Test 'lessCode' is correctly handled (since 7.3M1)
        assertEquals("rgba(0, 0, 255, 1)", previewBox.getTextColor());
    }

    /**
     * @since 6.3RC1
     */
    @Test
    public void createNewTheme() throws Exception
    {
        // Go to the presentation section of the administration
        AdministrationPage administrationPage = AdministrationPage.gotoPage();
        ThemesAdministrationSectionPage presentationAdministrationSectionPage =
                administrationPage.clickThemesSection();

        // Click on "manage color theme"
        presentationAdministrationSectionPage.manageColorThemes();
        ThemeApplicationWebHomePage themeApplicationWebHomePage = new ThemeApplicationWebHomePage();

        // Ensure the current theme is correct
        assertEquals("Charcoal", themeApplicationWebHomePage.getCurrentTheme());
        // Ensure the other themes are correct
        List<String> otherThemes = themeApplicationWebHomePage.getOtherThemes();
        assertTrue(otherThemes.contains("Marina"));
        assertTrue(otherThemes.contains("Garden"));
        assertTrue(otherThemes.contains("Kitty"));
        assertFalse(otherThemes.contains("Charcoal"));

        // Create a new theme
        EditThemePage editThemePage = themeApplicationWebHomePage.createNewTheme("Test");
        editThemePage.waitUntilPreviewIsLoaded();

        // First, disable auto refresh because it slows down the test
        // (and can even make it fails if the computer is slow)
        editThemePage.setAutoRefresh(false);

        // Set variables
        editThemePage.selectVariableCategory("Base colors");
        editThemePage.setVariableValue("xwiki-page-content-bg", "#ff0000");
        editThemePage.selectVariableCategory("Typography");
        editThemePage.setVariableValue("font-family-base", "Monospace");
        editThemePage.selectVariableCategory("Advanced");
        // Insert lessCode too
        editThemePage.setTextareaValue("lessCode", ".main{ color: #0000ff; }");
        // Save the theme
        editThemePage.clickSaveAndView();

        // Go back to the theme application
        themeApplicationWebHomePage = ThemeApplicationWebHomePage.gotoPage();
        // Set the new theme as current
        themeApplicationWebHomePage.useTheme("Test");
        // Verify that the new theme is used
        assertEquals("Test", themeApplicationWebHomePage.getCurrentTheme());
        // Look at the values
        assertEquals("rgba(255, 0, 0, 1)", themeApplicationWebHomePage.getPageBackgroundColor());
        assertEquals("monospace", themeApplicationWebHomePage.getFontFamily().toLowerCase());
        // Test 'lessCode' is correctly handled
        assertEquals("rgba(0, 0, 255, 1)", themeApplicationWebHomePage.getTextColor());

        // Switch back to Charcoal
        themeApplicationWebHomePage.useTheme("Charcoal");

        // Remove the theme
        ViewThemePage themePage = themeApplicationWebHomePage.seeTheme("Test");
        themePage.waitUntilPreviewIsLoaded();
        ConfirmationPage confirmationPage = themePage.delete();
        confirmationPage.confirmDeletePage().waitUntilIsTerminated();
    }

}
