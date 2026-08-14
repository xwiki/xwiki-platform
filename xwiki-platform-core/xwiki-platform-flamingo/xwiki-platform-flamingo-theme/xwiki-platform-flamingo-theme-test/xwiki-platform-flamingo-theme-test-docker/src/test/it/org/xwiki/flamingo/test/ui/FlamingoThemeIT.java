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

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.Color;
import org.xwiki.administration.test.po.AdministrablePage;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.administration.test.po.ThemesAdministrationSectionPage;
import org.xwiki.flamingo.test.po.EditThemePage;
import org.xwiki.flamingo.test.po.PreviewBox;
import org.xwiki.flamingo.test.po.ThemeApplicationWebHomePage;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.integration.junit.LogCaptureConfiguration;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

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
    private static final String THEMES_SPACE = "FlamingoThemes";

    private static final String COLOR_THEME_PREFERENCE = "colorTheme";

    private static final String LOGO_NAME = "logo.png";

    /**
     * The brand colors set by {@link #changeBaseBrandColors}. They are dark enough to keep a proper contrast with the
     * light backgrounds and texts they are combined with.
     */
    private static final String BRAND_PRIMARY = "#1a4d80";

    private static final String BRAND_INFO = "#0b5c73";

    private static final String BRAND_DANGER = "#a11212";

    @AfterEach
    void verify(LogCaptureConfiguration logCaptureConfiguration)
    {
        // TODO: Understand the problem and fix it
        logCaptureConfiguration.registerExcludes("line 13: NS_ERROR_NOT_INITIALIZED");
    }

    @Test
    @Order(1)
    void validateColorThemeFeatures(TestUtils setup, TestInfo info)
    {
        setup.loginAsSuperAdmin();

        // First make sure the theme we'll create doesn't exist
        String testMethodName = info.getTestMethod().get().getName();
        setup.deletePage(THEMES_SPACE, testMethodName);

        // Note: we don't reset the color theme before we start even though the test below could fail and thus have
        // our test theme set. We don't do that since we want to test that the default CT is Charcoal by default.
        // The reason why it's ok is that this test is executed first (see @Order) and the other tests of this class
        // restore the color theme they set, so the default one is still in place here.
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
        validateSetThemeFromWikiAdminUI(testMethodName, setup);

        // Validate setting a color theme from the page Admin UI for the page and its children
        validateSetThemeFromPageAdminUI(testMethodName, setup, info);
    }

    @Test
    @Order(2)
    void changeThemeLogo(TestUtils setup, TestConfiguration testConfiguration, TestInfo info) throws Exception
    {
        setup.loginAsSuperAdmin();

        // First make sure the theme we'll create doesn't exist
        String themeName = info.getTestMethod().get().getName();
        DocumentReference themeReference = new DocumentReference("xwiki", THEMES_SPACE, themeName);
        setup.deletePage(themeReference);

        // Set the logo of a new theme by uploading an image with the attachment picker of the "Logos" category.
        EditThemePage editThemePage = ThemeApplicationWebHomePage.gotoPage().createNewTheme(themeName);
        // Disable the auto refresh of the preview because it slows down the test.
        editThemePage.setAutoRefresh(false);
        editThemePage.selectVariableCategory("Logos");
        File logoFile = new File(testConfiguration.getBrowser().getTestResourcesPath(), LOGO_NAME);
        editThemePage.setImageVariableValue("logo", logoFile.getAbsolutePath());
        // The picker selects the image it has just uploaded, but as a temporary attachment: it's only attached to the
        // theme when the theme is saved.
        assertEquals(LOGO_NAME, editThemePage.getImageVariableValue("logo"));
        editThemePage.clickSaveAndView();
        AttachmentReference logoReference = new AttachmentReference(LOGO_NAME, themeReference);
        assertTrue(setup.rest().exists(logoReference),
            String.format("The [%s] image was not attached to the theme when saving it", LOGO_NAME));

        // Verify that the uploaded image is displayed as the wiki logo when the theme is used, in place of the logo
        // provided by the skin.
        String previousColorTheme = setup.setWikiPreference(COLOR_THEME_PREFERENCE, THEMES_SPACE + '.' + themeName);
        try {
            ViewPage viewPage = setup.gotoPage("Main", "WebHome");
            // The logo URL also carries the revision of the attachment, which is not relevant here.
            assertEquals(setup.getURL(logoReference, "download", null),
                StringUtils.substringBefore(viewPage.getLogoImageURL(), "?"));
        } finally {
            // Restore the color theme that was in use so that this test doesn't affect the other tests.
            setup.setWikiPreference(COLOR_THEME_PREFERENCE, Objects.toString(previousColorTheme, ""));
        }
    }

    @Test
    @Order(3)
    void changeBaseBrandColors(TestUtils setup, TestInfo info) throws Exception
    {
        setup.loginAsSuperAdmin();

        // First make sure the theme we'll create doesn't exist
        String themeName = info.getTestMethod().get().getName();
        setup.deletePage(THEMES_SPACE, themeName);

        // Set the brand colors of the "Base colors" category in a new theme.
        EditThemePage editThemePage = ThemeApplicationWebHomePage.gotoPage().createNewTheme(themeName);
        // Disable the auto refresh of the preview because it slows down the test.
        editThemePage.setAutoRefresh(false);
        editThemePage.selectVariableCategory("Base colors");
        // Set the first color with the color picker, to check that the picker is there and works, and the remaining
        // ones by typing their value directly, which is faster.
        editThemePage.pickVariableColor("brand-primary", BRAND_PRIMARY);
        editThemePage.setVariableValue("brand-info", BRAND_INFO);
        editThemePage.setVariableValue("brand-danger", BRAND_DANGER);

        // The preview box next to each input is only filled in by the color picker, so these assertions also verify
        // that the picker is initialized for the variables whose value was typed directly.
        assertColor(BRAND_PRIMARY, editThemePage.getColorPreview("brand-primary"));
        assertColor(BRAND_INFO, editThemePage.getColorPreview("brand-info"));
        assertColor(BRAND_DANGER, editThemePage.getColorPreview("brand-danger"));

        editThemePage.clickSaveAndView();

        // The brand info and brand danger colors are exposed to wiki pages through the color theme variables they are
        // mapped to, so use these variables to color some text. The brand primary color is not exposed as a color theme
        // variable, but the skin uses it for the "text-primary" style.
        DocumentReference testPage =
            new DocumentReference("xwiki", info.getTestClass().get().getSimpleName(), themeName);
        setup.createPage(testPage, "{{velocity}}\n"
            + "(% id=\"brandPrimary\" class=\"text-primary\" %)brand primary\n\n"
            + "(% id=\"brandInfo\" style=\"color: $theme.notificationInfoColor\" %)brand info\n\n"
            + "(% id=\"brandDanger\" style=\"color: $theme.notificationErrorColor\" %)brand danger\n"
            + "{{/velocity}}");

        String previousColorTheme = setup.setWikiPreference(COLOR_THEME_PREFERENCE, THEMES_SPACE + '.' + themeName);
        try {
            ViewPage viewPage = setup.gotoPage(testPage);
            assertColor(BRAND_PRIMARY, viewPage.getElementCSSValue(By.id("brandPrimary"), "color"));
            assertColor(BRAND_INFO, viewPage.getElementCSSValue(By.id("brandInfo"), "color"));
            assertColor(BRAND_DANGER, viewPage.getElementCSSValue(By.id("brandDanger"), "color"));
        } finally {
            // Restore the color theme that was in use so that this test doesn't affect the other tests.
            setup.setWikiPreference(COLOR_THEME_PREFERENCE, Objects.toString(previousColorTheme, ""));
        }
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

    private void validateSetThemeFromWikiAdminUI(String testMethodName, TestUtils setup)
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

        // Verify that the default button background defined by the theme is applied on a wiki page, the "Cancel"
        // button of the editor being one of the buttons using that style.
        // Note: the skin doesn't use the @btn-default-bg value as-is, it lightens it by 5% (see buttons.less), which
        // turns the #99ccff set on the theme into #b3d9ff.
        WikiEditPage wikiEditPage = WikiEditPage.gotoPage("Main", "WebHome");
        assertColor(179, 217, 255, wikiEditPage.getCancelButtonBackgroundColor());
        wikiEditPage.clickCancel();

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
        assertColor(255, 218, 218, page.getPageBackgroundColor());
        // The text color set by the theme is inherited from the "body" element. Note that we cannot assert it on the
        // page content since the 'lessCode' variable set by this test colors the whole ".main" block in blue.
        assertColor(102, 51, 0, page.getTextColor());
        // Test 'lessCode' is correctly handled, the title being inside the ".main" block it colors.
        assertColor(0, 0, 255, page.getTitleColor());
        assertEquals("monospace", page.getTitleFontFamily().toLowerCase());
    }

    /**
     * @param expectedColor the expected color, in any CSS notation (e.g. {@code #1a4d80})
     * @param obtainedColor the obtained color, in any CSS notation (e.g. {@code rgb(26, 77, 128)})
     */
    private void assertColor(String expectedColor, String obtainedColor)
    {
        assertEquals(Color.fromString(expectedColor), Color.fromString(obtainedColor),
            String.format("Wrong color [expected = %s | obtained = %s]", expectedColor, obtainedColor));
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
        // Change the color of the page's text. It's dark enough to keep a proper contrast with the page backgrounds.
        editThemePage.setVariableValue("text-color", "#663300");
        // Again...
        editThemePage.selectVariableCategory("Typography");
        editThemePage.setVariableValue("font-family-base", "Monospace");
        // Change the background of the buttons using the default style. It's light enough to keep a proper contrast
        // with the button text color.
        editThemePage.selectVariableCategory("Buttons");
        editThemePage.setVariableValue("btn-default-bg", "#99ccff");
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
