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

import static org.hamcrest.CoreMatchers.containsStringIgnoringCase;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verify the ability to change the localization settings.
 *
 * @version $Id$
 * @since 13.4RC1
 * @since 12.10.8
 */
@UITest
class LocalizationIT
{
    @BeforeEach
    void setUp(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
        // Reset default language configuration.
        resetLanguageSettings(setup);
    }

    @Test
    @Order(1)
    void changeLanguageInMonolingualModeUsingTheAdministrationPreference(TestUtils setup, TestReference testReference)
    {
        ViewPage vp = setup.createPage(testReference,
            "{{velocity}}context = ($xcontext.locale), doc = ($doc.locale), "
            + "default = ($doc.defaultLocale), tdoc = ($tdoc.locale), "
            + "tdocdefault = ($tdoc.defaultLocale){{/velocity}}");

        // Current language must be "en".
        assertEquals(vp.getContent(), "context = (en), doc = (), default = (en), tdoc = (), tdocdefault = (en)",
            "Invalid content");

        // Change default language to "fr".
        AdministrationPage adminPage = AdministrationPage.gotoPage();
        LocalizationAdministrationSectionPage sectionPage = adminPage.clickLocalizationSection();
        sectionPage.setDefaultLanguage("fr");
        sectionPage.clickSave();

        // Now language must be "fr".
        vp = setup.gotoPage(testReference);
        assertThat("Page not in French!", vp.getLastModifiedText(), containsStringIgnoringCase("modifié par"));
        assertEquals(vp.getContent(), "context = (fr), doc = (), default = (en), tdoc = (), tdocdefault = (en)",
            "Invalid content");
    }

    @Test
    @Order(2)
    void passingLanguageInRequestHasNoEffectInMonolingualMode(TestUtils setup, TestReference testReference)
    {
        setup.createPage(testReference, "");
        setup.gotoPage(testReference, "view", "language=fr");
        ViewPage vp = new ViewPage();
        assertThat("Page not in English!", vp.getLastModifiedText(), containsStringIgnoringCase("last modified by"));
    }

    @Test
    @Order(3)
    void changeLanguageInMultilingualModeUsingTheLanguageRequestParameter(TestUtils setup, TestReference testReference)
    {
        setLanguageSettings(true, Arrays.asList("en", "fr"));
        setup.createPage(testReference, "");
        setup.gotoPage(testReference, "view", "language=fr");
        ViewPage vp = new ViewPage();
        assertThat("Page not in French!", vp.getLastModifiedText(), containsStringIgnoringCase("modifié par"));
    }

    @Test
    @Order(4)
    void headerCorrectLanguage(TestUtils setup)
    {
        setLanguageSettings(true, Arrays.asList("en", "fr"));

        setup.gotoPage("Main", "Test", "view");
        assertLanguageTagsArePresent(setup, "en");

        setup.gotoPage("Main", "Test", "view", "language=fr");
        assertLanguageTagsArePresent(setup, "fr");
    }

    /**
     * Assert that the given <code>language</code> is present in various attributes and tags on the page.
     *
     * @param language the language to use, should be a valid language, e.g. "en"
     */
    private void assertLanguageTagsArePresent(TestUtils setup, String language)
    {
        WebElement html = setup.getDriver().findElement(By.tagName("html"));
        assertEquals(language, html.getAttribute("lang"));
        assertEquals(language, html.getAttribute("xml:lang"));

        ViewPage vp = new ViewPage();
        // For retro-compatibility only.
        assertEquals(language, vp.getHTMLMetaDataValue("language"));

        String content = setup.getDriver().getPageSource();
        assertTrue(content.contains("language=" + language));
    }

    private void resetLanguageSettings(TestUtils setup)
    {
        setLanguageSettings(false, null);
        // Reset current language.
        setup.getDriver().manage().deleteCookieNamed("language");
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
