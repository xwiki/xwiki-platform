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

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.openqa.selenium.TimeoutException;
import org.xwiki.administration.test.po.AdministrablePage;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.administration.test.po.ThemesAdministrationSectionPage;
import org.xwiki.flamingo.test.po.EditThemePage;
import org.xwiki.flamingo.test.po.PreviewBox;
import org.xwiki.flamingo.test.po.ThemeApplicationWebHomePage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.integration.junit.LogCaptureConfiguration;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UI tests for the Flamingo Theme Application.
 *
 * @version $Id$
 * @since 6.3M1
 */
@UITest
class FlamingoThemeIT
{
    @AfterEach
    void verify(LogCaptureConfiguration logCaptureConfiguration)
    {
        // TODO: Understand the problem and fix it
        logCaptureConfiguration.registerExcludes("line 13: NS_ERROR_NOT_INITIALIZED");
    }

    @Test
    void validateColorThemeFeatures(TestUtils setup, TestInfo info)
    {
        setup.loginAsSuperAdmin();

        // First make sure the theme we'll create doesn't exist
        String testMethodName = info.getTestMethod().get().getName();
        setup.deletePage("FlamingoThemes", testMethodName);

        // Note: we don't reset the color theme before we start even though the test below could fail and thus have
        // our test theme set. We don't do that since we want to test that the default CT is Charcoal by default.
        // The reason why it's ok is because we have only a single UI test in this module and thus there's no risk
        // that another test would fail because it's expecting to have Charcoal defined.
        // Only caveat is that if you run this test several times and it fails the first time then it may fail the
        // second time when we test that the default CT is Charcoal...

        // Go to the Theme section of the wiki administration UI
        AdministrationPage administrationPage = AdministrationPage.gotoPage();
        ThemesAdministrationSectionPage presentationAdministrationSectionPage =
            administrationPage.clickThemesSection();

        // Click on "manage color theme"
        presentationAdministrationSectionPage.manageColorThemes();
        ThemeApplicationWebHomePage themeApplicationWebHomePage = new ThemeApplicationWebHomePage();

        // Ensure the current theme is correct (default is "Charcoal")
        assertEquals("Charcoal", themeApplicationWebHomePage.getCurrentTheme());

        // Ensure the other themes listed are the expected ones
        List<String> otherThemes = themeApplicationWebHomePage.getOtherThemes();
        assertTrue(otherThemes.contains("Marina"));
        assertTrue(otherThemes.contains("Garden"));
        assertTrue(otherThemes.contains("Kitty"));
        assertFalse(otherThemes.contains("Charcoal"));

        // Create a new theme both to validate that the feature works and to be used as the new theme to set in the
        // tests below.
        validateThemeCreation(themeApplicationWebHomePage, testMethodName);

        // Validate viewing and setting a color theme from the color theme home page
        validateViewAndSetThemeFromThemeHomePage(testMethodName);

        // Validate setting a color theme from the wiki Admin UI
        validateSetThemeFromWikiAdminUI(testMethodName);

        // Validate setting a color theme from the page Admin UI for the page and its children
        validateSetThemeFromPageAdminUI(testMethodName, setup, info);
    }

    private void validateThemeCreation(ThemeApplicationWebHomePage themeApplicationWebHomePage, String testMethodName)
    {
        EditThemePage editThemePage = themeApplicationWebHomePage.createNewTheme(testMethodName);

        // First, disable auto refresh because it slows down the test
        // (and can even make it fails if the computer is slow)
        editThemePage.setAutoRefresh(false);

        verifyAllVariablesCategoriesArePresent(editThemePage);
        verifyVariablesCategoriesDoesNotDisappear(editThemePage);
        verifyThatPreviewWorks(editThemePage);

        editThemePage.clickSaveAndView();
    }

    private void validateViewAndSetThemeFromThemeHomePage(String testMethodName)
    {
        // Go back to the theme application
        ThemeApplicationWebHomePage themeApplicationWebHomePage = ThemeApplicationWebHomePage.gotoPage();

        // Set the new theme as current, from the Theme Home page
        themeApplicationWebHomePage.useTheme(testMethodName);
        // Verify that the new theme is used
        assertEquals(testMethodName, themeApplicationWebHomePage.getCurrentTheme());
        // Check the colors used on the page
        assertCustomThemeColors(themeApplicationWebHomePage);

        // Verify we can view a theme by clicking it
        themeApplicationWebHomePage.seeTheme(testMethodName);

        // Switch back to Charcoal
        // TODO: Replace this with a setup.updateObject() call since we don't need to test the useTheme() UI as it's
        //  been tested already. We just need this to be as fast as possible.
        themeApplicationWebHomePage = ThemeApplicationWebHomePage.gotoPage();
        themeApplicationWebHomePage.useTheme("Charcoal");
    }

    private void validateSetThemeFromWikiAdminUI(String testMethodName)
    {
        // Go back to the Theme Admin UI to verify we can set the new theme from there too (using the select control)
        AdministrationPage administrationPage = AdministrationPage.gotoPage();
        ThemesAdministrationSectionPage presentationAdministrationSectionPage = administrationPage.clickThemesSection();

        // Set the newly created color theme as the active theme
        presentationAdministrationSectionPage.setColorTheme(testMethodName);
        assertEquals(testMethodName, presentationAdministrationSectionPage.getCurrentColorTheme());
        presentationAdministrationSectionPage.clickSave();

        // Verify that the color theme has been applied.
        assertCustomThemeColors(new ViewPage());

        // Click on the 'customize' button to edit the theme to verify it works
        presentationAdministrationSectionPage.clickOnCustomizeColorTheme();
        EditThemePage editThemePage = new EditThemePage();
        assertFalse(editThemePage.getPreviewBox().hasError(true));
        editThemePage.clickSaveAndView();

        // Switch back to Charcoal
        // TODO: Replace this with a setup.updateObject() call since we don't need to test the useTheme() UI as it's
        //  been tested already. We just need this to be as fast as possible.
        ThemeApplicationWebHomePage themeApplicationWebHomePage = ThemeApplicationWebHomePage.gotoPage();
        themeApplicationWebHomePage.useTheme("Charcoal");
    }

    private void validateSetThemeFromPageAdminUI(String testMethodName, TestUtils setup, TestInfo info)
    {
        // Create two nested pages. We'll apply the theme on the top page and verify that the nested page has it too.
        DocumentReference topPage = new DocumentReference("xwiki", info.getTestClass().get().getSimpleName() +
            "Parent", "WebHome");
        DocumentReference childPage = new DocumentReference("xwiki", Arrays.asList(
            info.getTestClass().get().getSimpleName() + "Parent",
            info.getTestClass().get().getSimpleName() + "Child"), "WebHome");
        setup.deletePage(topPage, true);
        setup.createPage(childPage, "top page");
        setup.createPage(topPage, "top page");
        AdministrablePage ap = new AdministrablePage();

        // Navigate to the top page's admin UI.
        AdministrationPage page = ap.clickAdministerPage();
        ThemesAdministrationSectionPage presentationAdministrationSectionPage = page.clickThemesSection();

        // Set the newly created color theme as the active theme for the page and children
        presentationAdministrationSectionPage.setColorTheme(testMethodName);
        assertEquals(testMethodName, presentationAdministrationSectionPage.getCurrentColorTheme());
        presentationAdministrationSectionPage.clickSave();

        // Verify that the color theme has been applied to the top page
        ViewPage vp = setup.gotoPage(topPage);
        assertCustomThemeColors(vp);

        // Verify that the color theme has been applied to the children page
        vp = setup.gotoPage(childPage);
        assertCustomThemeColors(vp);

        // Verify that the color theme has not been applied to other pages not under the top page and that we have the
        // Charcoal background color set.
        vp = setup.gotoPage("NonExistentSpace", "NonExistentPage");
        assertColor(255, 255, 255, vp.getPageBackgroundColor());
    }

    private void assertCustomThemeColors(ViewPage page)
    {
        // FIXME: The following should be put back when https://github.com/SeleniumHQ/selenium/issues/7697 will be fixed
        // for now we get rgb value with Firefox and rgba value with Chrome
        //assertEquals("rgb(255, 0, 0)", page.getPageBackgroundColor());
        // Test 'lessCode' is correctly handled
        //assertEquals("rgb(0, 0, 255)", page.getTextColor());
        assertColor(255, 218, 218, page.getPageBackgroundColor());
        assertColor(0, 0, 255, page.getTitleColor());
        assertEquals("monospace", page.getTitleFontFamily().toLowerCase());
    }

    private void assertColor(int red, int green, int blue, String obtainedValue)
    {
        assertTrue(obtainedValue.contains("rgb"), "This is not an rgb value: " + obtainedValue);
        String rgbComponent = String.format("%s, %s, %s", red, green, blue);
        assertTrue(obtainedValue.contains(rgbComponent),
            "Wrong RGB component [expected = " + rgbComponent + "| Obtained = " + obtainedValue);
    }

    private void verifyAllVariablesCategoriesArePresent(EditThemePage editThemePage)
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

    private void verifyVariablesCategoriesDoesNotDisappear(EditThemePage editThemePage)
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

    private void verifyThatPreviewWorks(EditThemePage editThemePage)
    {
        // Verify that the preview is working with the current values
        PreviewBox previewBox = editThemePage.getPreviewBox();
        assertFalse(previewBox.hasError(true));
        // Select a variable category and change value.
        // The default link color is too light to operate viewable changes without breaking contrast.
        editThemePage.selectVariableCategory("Base colors");
        editThemePage.setVariableValue("link-color", "#2c699c");
        // Change another value. We don't deactivate all WCAG checks, so we need to take care about contrast.
        editThemePage.setVariableValue("xwiki-page-content-bg", "#ffdada");
        // Again...
        editThemePage.selectVariableCategory("Typography");
        editThemePage.setVariableValue("font-family-base", "Monospace");
        // Test that the @lessCode variable is handled too!
        editThemePage.selectVariableCategory("Advanced");
        editThemePage.setTextareaValue("lessCode", ".main{ color: #0000ff; }");
        // Refresh
        // From time-to-time the preview does not load on Firefox certainly because of some JS race condition.
        // Right now we cannot get the javascript console logs because of a geckodriver limitation, so it's hard to
        // fix properly. For now, I'm trying to just trigger once again the refresh in case of first timeout.
        try {
            editThemePage.refreshPreview();
        } catch (TimeoutException e) {
            editThemePage.refreshPreview();
        }
        previewBox = editThemePage.getPreviewBox();
        // Verify that there is still no errors
        assertFalse(previewBox.hasError());
        // Verify colors
        assertCustomThemeColors(previewBox);
        previewBox.switchToDefaultContent();
    }
}
