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
package org.xwiki.test.ui;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.administration.test.po.LocalizationAdministrationSectionPage;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.browser.IgnoreBrowser;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verify the ability to change the language.
 *
 * @version $Id$
 * @since 12.8RC1
 */
@UITest
public class LanguageTest
{
    // @Rule
    // public AdminAuthenticationRule adminAuthenticationRule = new AdminAuthenticationRule(true, testUtils);

    /**
     * Make sure we set back the language to monolingual and english for other tests that come thereafter
     */
    @AfterAll
    public static void afterClass(TestUtils testUtils)
    {
        reset(testUtils);
    }

    /**
     * Ensure the default language is English and that the wiki is in monolingual mode
     */
    @BeforeAll
    void before(TestUtils testUtils)
    {
        reset(testUtils);
    }

    private static void reset(TestUtils testUtils)
    {
        // Reset default language configuration
        setLanguageSettings(false, "en");
        // Reset current language
        testUtils.getDriver().manage().deleteCookieNamed("language");
    }

    @Test
    @Order(1)
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason = "See https://jira.xwiki.org/browse/XE-1146")
    void testChangeLanguageInMonolingualModeUsingTheAdministrationPreference(TestUtils testUtils)
    {
        WikiEditPage edit = WikiEditPage.gotoPage("Test", "LanguageTest");
        edit.setContent("{{velocity}}context = ($xcontext.locale), doc = ($doc.locale), "
                            + "default = ($doc.defaultLocale), tdoc = ($tdoc.locale), "
                            + "tdocdefault = ($tdoc.defaultLocale){{/velocity}}");
        ViewPage vp = edit.clickSaveAndView();

        // Current language must be "en"
        assertEquals("Invalid content", vp.getContent(),
            "context = (en), doc = (), default = (en), tdoc = (), tdocdefault = (en)");

        // Change default language to "fr"
        AdministrationPage adminPage = AdministrationPage.gotoPage();
        LocalizationAdministrationSectionPage sectionPage = adminPage.clickLocalizationSection();
        sectionPage.setDefaultLanguage("fr");
        sectionPage.clickSave();

        // Now language must be "fr"
        vp = testUtils.gotoPage("Test", "LanguageTest");
        assertTrue(isPageInFrench(testUtils), "Page not in French!");
        assertEquals("Invalid content", vp.getContent(),
            "context = (fr), doc = (), default = (en), tdoc = (), tdocdefault = (en)");
    }

    @Test
    @Order(2)
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason = "See https://jira.xwiki.org/browse/XE-1146")
    void testPassingLanguageInRequestHasNoEffectInMonoligualMode(TestUtils testUtils)
    {
        testUtils.gotoPage("Main", "WebHome", "view", "language=fr");
        assertTrue(isPageInEnglish(testUtils), "Page not in English!");
    }

    @Test
    @Order(3)
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason = "See https://jira.xwiki.org/browse/XE-1146")
    void testChangeLanguageInMultilingualModeUsingTheLanguageRequestParameter(TestUtils testUtils)
    {
        setLanguageSettings(true, "en", Arrays.asList("en", "fr"));

        testUtils.gotoPage("Main", "WebHome", "view", "language=fr");
        assertTrue(isPageInFrench(testUtils), "Page not in French!");
    }

    @Test
    @Order(4)
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason = "See https://jira.xwiki.org/browse/XE-1146")
    void testHeaderCorrectLanguage(TestUtils testUtils)
    {
        setLanguageSettings(true, "en", Arrays.asList("en", "fr"));

        testUtils.gotoPage("Main", "Test", "view");
        checkLanguageTagsArePresent(testUtils, "en");

        testUtils.gotoPage("Main", "Test", "view", "language=fr");
        checkLanguageTagsArePresent(testUtils, "fr");
    }

    /**
     * Assert that the given <code>language</code> is present in various attributes and tags on the page
     *
     * @param language the language to use, should be a valid language, e.g. "en"
     */
    private void checkLanguageTagsArePresent(TestUtils testUtils, String language)
    {
        WebElement html = testUtils.getDriver().findElement(By.tagName("html"));
        assertEquals(language, html.getAttribute("lang"));
        assertEquals(language, html.getAttribute("xml:lang"));

        ViewPage vp = new ViewPage();
        // For retro-compatibility only
        assertEquals(language, vp.getHTMLMetaDataValue("language"));

        String content = testUtils.getDriver().getPageSource();
        assertTrue(content.contains("language=" + language));
    }

    /**
     * Check if the currently displayed page is in English, by looking at the "Log-Out" link
     */
    private boolean isPageInEnglish(TestUtils testUtils)
    {
        return testUtils.getDriver().findElement(By.className("xdocLastModification")).getText().toLowerCase()
                   .contains("last modified by");
    }

    /**
     * Check if the currently displayed page is in French, by looking at the "Log-Out" link
     */
    private boolean isPageInFrench(TestUtils testUtils)
    {
        return testUtils.getDriver().findElement(By.className("xdocLastModification")).getText().toLowerCase()
                   .contains("modifié par");
    }

    private static void setLanguageSettings(boolean isMultiLingual, String defaultLanguage)
    {
        setLanguageSettings(isMultiLingual, defaultLanguage, null);
    }

    private static void setLanguageSettings(boolean isMultiLingual, String defaultLanguage,
        List<String> supportedLanguages)
    {
        AdministrationPage adminPage = AdministrationPage.gotoPage();
        LocalizationAdministrationSectionPage sectionPage = adminPage.clickLocalizationSection();
        sectionPage.setMultiLingual(isMultiLingual);
        if (defaultLanguage != null) {
            sectionPage.setDefaultLanguage(defaultLanguage);
        }
        if (supportedLanguages != null) {
            sectionPage.setSupportedLanguages(supportedLanguages);
        }
        sectionPage.clickSave();
    }
}
