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
package org.xwiki.localization.test.ui;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.administration.test.po.LocalizationAdministrationSectionPage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.hamcrest.CoreMatchers.containsStringIgnoringCase;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verify the ability to change the language.
 *
 * @version $Id$
 * @since 13.4RC1
 * @since 12.10.8
 */
@UITest
class LanguageIT
{
    @BeforeEach
    void setUp(TestUtils testUtils)
    {
        testUtils.loginAsSuperAdmin();
        // Reset default language configuration.
        resetLanguageSettings();
        // Reset current language.
        testUtils.getDriver().manage().deleteCookieNamed("language");
    }

    @Test
    @Order(1)
    void changeLanguageInMonolingualModeUsingTheAdministrationPreference(TestUtils testUtils)
    {
        WikiEditPage edit = WikiEditPage.gotoPage("Test", "LanguageTest");
        edit.setContent("{{velocity}}context = ($xcontext.locale), doc = ($doc.locale), "
            + "default = ($doc.defaultLocale), tdoc = ($tdoc.locale), "
            + "tdocdefault = ($tdoc.defaultLocale){{/velocity}}");
        ViewPage vp = edit.clickSaveAndView();

        // Current language must be "en".
        assertEquals(vp.getContent(), "context = (en), doc = (), default = (en), tdoc = (), tdocdefault = (en)",
            "Invalid content");

        // Change default language to "fr".
        AdministrationPage adminPage = AdministrationPage.gotoPage();
        LocalizationAdministrationSectionPage sectionPage = adminPage.clickLocalizationSection();
        sectionPage.setDefaultLanguage("fr");
        sectionPage.clickSave();

        // Now language must be "fr".
        vp = testUtils.gotoPage("Test", "LanguageTest");
        assertThat("Page not in French!", vp.getLastModifiedText(), containsStringIgnoringCase("modifié par"));
        assertEquals(vp.getContent(), "context = (fr), doc = (), default = (en), tdoc = (), tdocdefault = (en)",
            "Invalid content");
    }

    @Test
    @Order(2)
    void passingLanguageInRequestHasNoEffectInMonolingualMode(TestUtils testUtils, TestReference testReference)
    {
        testUtils.createPage(testReference, "");
        testUtils.gotoPage(testReference, "view", "language=fr");
        ViewPage vp = new ViewPage();
        assertThat("Page not in English!", vp.getLastModifiedText(), containsStringIgnoringCase("last modified by"));
    }

    @Test
    @Order(3)
    void changeLanguageInMultilingualModeUsingTheLanguageRequestParameter(TestUtils testUtils,
        TestReference testReference)
    {
        setLanguageSettings(true, Arrays.asList("en", "fr"));
        testUtils.createPage(testReference, "");
        testUtils.gotoPage(testReference, "view", "language=fr");
        ViewPage vp = new ViewPage();
        assertThat("Page not in French!", vp.getLastModifiedText(), containsStringIgnoringCase("modifié par"));
    }

    @Test
    @Order(4)
    void headerCorrectLanguage(TestUtils testUtils)
    {
        setLanguageSettings(true, Arrays.asList("en", "fr"));

        testUtils.gotoPage("Main", "Test", "view");
        assertLanguageTagsArePresent(testUtils, "en");

        testUtils.gotoPage("Main", "Test", "view", "language=fr");
        assertLanguageTagsArePresent(testUtils, "fr");
    }

    /**
     * Assert that the given <code>language</code> is present in various attributes and tags on the page.
     *
     * @param language the language to use, should be a valid language, e.g. "en"
     */
    private void assertLanguageTagsArePresent(TestUtils testUtils, String language)
    {
        WebElement html = testUtils.getDriver().findElement(By.tagName("html"));
        assertEquals(language, html.getAttribute("lang"));
        assertEquals(language, html.getAttribute("xml:lang"));

        ViewPage vp = new ViewPage();
        // For retro-compatibility only.
        assertEquals(language, vp.getHTMLMetaDataValue("language"));

        String content = testUtils.getDriver().getPageSource();
        assertTrue(content.contains("language=" + language));
    }

    private void resetLanguageSettings()
    {
        setLanguageSettings(false, null);
    }

    private void setLanguageSettings(boolean isMultiLingual, List<String> supportedLanguages)
    {
        AdministrationPage adminPage = AdministrationPage.gotoPage();
        LocalizationAdministrationSectionPage sectionPage = adminPage.clickLocalizationSection();
        sectionPage.setMultiLingual(isMultiLingual);
        sectionPage.setDefaultLanguage("en");
        if (supportedLanguages != null) {
            sectionPage.setSupportedLanguages(supportedLanguages);
        }
        sectionPage.clickSave();
    }
}
