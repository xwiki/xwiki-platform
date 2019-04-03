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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.administration.test.po.LocalizationAdministrationSectionPage;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.test.ui.browser.IgnoreBrowser;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Verify the ability to change the language.
 * 
 * @version $Id$
 * @since 2.4RC1
 */
public class LanguageTest extends AbstractTest
{
    @Rule
    public AdminAuthenticationRule adminAuthenticationRule = new AdminAuthenticationRule(true, getUtil());

    /**
     * Make sure we set back the language to monolingual and english for other tests that come thereafter
     */
    @AfterClass
    public static void afterClass()
    {
        reset();
    }

    /**
     * Ensure the default language is English and that the wiki is in monolingual mode
     */
    @Before
    public void before() throws Exception
    {
        reset();
    }

    private static void reset()
    {
        // Reset default language configuration
        setLanguageSettings(false, "en");
        // Reset current language
        getDriver().manage().deleteCookieNamed("language");
    }

    @Test
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason = "See https://jira.xwiki.org/browse/XE-1146")
    public void testChangeLanguageInMonolingualModeUsingTheAdministrationPreference()
    {
        WikiEditPage edit = WikiEditPage.gotoPage("Test", "LanguageTest");
        edit.setContent("{{velocity}}context = ($xcontext.language), doc = ($doc.language), "
            + "default = ($doc.defaultLanguage), tdoc = ($tdoc.language), "
            + "tdocdefault = ($tdoc.defaultLanguage){{/velocity}}");
        ViewPage vp = edit.clickSaveAndView();

        // Current language must be "en"
        Assert.assertEquals("Invalid content", vp.getContent(),
            "context = (en), doc = (), default = (en), tdoc = (), tdocdefault = (en)");

        // Change default language to "fr"
        AdministrationPage adminPage = AdministrationPage.gotoPage();
        LocalizationAdministrationSectionPage sectionPage = adminPage.clickLocalizationSection();
        sectionPage.setDefaultLanguage("fr");
        sectionPage.clickSave();

        // Now language must be "fr"
        vp = getUtil().gotoPage("Test", "LanguageTest");
        Assert.assertTrue("Page not in French!", isPageInFrench());
        Assert.assertEquals("Invalid content", vp.getContent(),
            "context = (fr), doc = (), default = (en), tdoc = (), tdocdefault = (en)");
    }

    @Test
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason = "See https://jira.xwiki.org/browse/XE-1146")
    public void testPassingLanguageInRequestHasNoEffectInMonoligualMode()
    {
        getUtil().gotoPage("Main", "WebHome", "view", "language=fr");
        Assert.assertTrue("Page not in English!", isPageInEnglish());
    }

    @Test
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason = "See https://jira.xwiki.org/browse/XE-1146")
    public void testChangeLanguageInMultilingualModeUsingTheLanguageRequestParameter()
    {
        setLanguageSettings(true, "en", Arrays.asList("en", "fr"));

        getUtil().gotoPage("Main", "WebHome", "view", "language=fr");
        Assert.assertTrue("Page not in French!", isPageInFrench());
    }

    @Test
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason = "See https://jira.xwiki.org/browse/XE-1146")
    public void testHeaderCorrectLanguage()
    {
        setLanguageSettings(true, "en", Arrays.asList("en", "fr"));

        getUtil().gotoPage("Main", "Test", "view");
        checkLanguageTagsArePresent("en");

        getUtil().gotoPage("Main", "Test", "view", "language=fr");
        checkLanguageTagsArePresent("fr");
    }

    @Test
    public void translateDocument() throws Exception
    {
        LocalDocumentReference referenceDEFAULT = new LocalDocumentReference(getTestClassName(), getTestMethodName());
        LocalDocumentReference referenceFR = new LocalDocumentReference(referenceDEFAULT, Locale.FRENCH);
        LocalDocumentReference referenceEN = new LocalDocumentReference(referenceDEFAULT, Locale.ENGLISH);

        String originalContent = "original content";
        String originalTitle = "original title";

        String englishContent = "english content";
        String englishTitle = "english title";

        String frenchContent = "french content";
        String frenchTitle = "french title";

        // Cleanup
        getUtil().rest().delete(referenceFR);
        getUtil().rest().delete(referenceEN);
        getUtil().rest().delete(referenceDEFAULT);

        // Create default version before setting the language settings
        // Ensure that we don't have a conflict window when creating the translations (cf. XWIKI-16299)
        ViewPage viewPage = getUtil().createPage(referenceDEFAULT, "", "");
        WikiEditPage editPage = viewPage.editWiki();
        editPage.setContent(originalContent);
        editPage.setTitle(originalTitle);
        viewPage = editPage.clickSaveAndView();
        assertEquals(originalContent, viewPage.getContent());
        assertEquals(originalTitle, viewPage.getDocumentTitle());

        // Set 2 locales
        setLanguageSettings(true, "en", Arrays.asList("en", "fr"));

        viewPage = getUtil().gotoPage(referenceDEFAULT);
        // Edit the page
        editPage = viewPage.editWiki();

        // First edition in multilingual is the edition of default document.
        assertEquals("", editPage.getMetaDataValue("locale"));
        assertEquals(Arrays.asList(Locale.FRENCH), editPage.getNotExistingLocales());
        assertEquals(Arrays.asList(), editPage.getExistingLocales());

        // set the default language to blank: it should relies on the original document
        assertEquals("en", editPage.getDefaultLanguage());
        editPage.setDefaultLanguage("");
        viewPage = editPage.clickSaveAndView();
        editPage = viewPage.editWiki();

        // so now it should see that we're not editing a translation but the default one
        assertEquals("", editPage.getMetaDataValue("locale"));

        // Translate to English
        editPage = editPage.clickTranslate("en");
        assertEquals("en", editPage.getMetaDataValue("locale"));
        editPage.setTitle(englishTitle);
        editPage.setContent(englishContent);
        viewPage = editPage.clickSaveAndView();
        assertEquals(englishTitle, viewPage.getDocumentTitle());
        assertEquals(englishContent, viewPage.getContent());

        editPage = viewPage.editWiki();
        assertEquals("en", editPage.getMetaDataValue("locale"));
        // Translate to French
        editPage = editPage.clickTranslate("fr");
        assertEquals("fr", editPage.getMetaDataValue("locale"));
        editPage.setTitle(frenchTitle);
        editPage.setContent(frenchContent);
        viewPage = editPage.clickSaveAndView();
        assertEquals(frenchTitle, viewPage.getDocumentTitle());
        assertEquals(frenchContent, viewPage.getContent());

        // Go back to english page to use english UI
        getUtil().gotoPage(referenceDEFAULT, "view", "language=en");
        viewPage = new ViewPage();

        // Go back to default page editor to ensure we can still edit it
        editPage = viewPage.editWiki();
        assertEquals("en", editPage.getMetaDataValue("locale"));
        editPage = editPage.clickTranslate("default");
        assertEquals("", editPage.getMetaDataValue("locale"));
        assertEquals(originalContent, editPage.getContent());
        assertEquals(originalTitle, editPage.getTitle());

        // Make sure all have the right content
        Page page = getUtil().rest().get(referenceFR);
        assertEquals(frenchTitle, page.getTitle());
        assertEquals(frenchContent, page.getContent());
        page = getUtil().rest().get(referenceEN);
        assertEquals(englishTitle, page.getTitle());
        assertEquals(englishContent, page.getContent());
        page = getUtil().rest().get(referenceDEFAULT);
        assertEquals(originalTitle, page.getTitle());
        assertEquals(originalContent, page.getContent());

        // Make sure that URL with languages is working too
        getUtil().gotoPage(referenceDEFAULT, "view", "language=fr");
        viewPage = new ViewPage();
        assertEquals(frenchTitle, viewPage.getDocumentTitle());
        assertEquals(frenchContent, viewPage.getContent());
        getUtil().gotoPage(referenceDEFAULT, "view", "language=en");
        viewPage = new ViewPage();
        assertEquals(englishTitle, viewPage.getDocumentTitle());
        assertEquals(englishContent, viewPage.getContent());
        // TODO: this is currently failing because of XWIKI-16307
//        getUtil().gotoPage(referenceDEFAULT, "view", "language=");
//        viewPage = new ViewPage();
//        assertEquals(originalTitle, viewPage.getDocumentTitle());
//        assertEquals(originalContent, viewPage.getContent());

        // Make sure two locales are listed for this page in the UI
        assertEquals(new HashSet<>(Arrays.asList(Locale.ENGLISH, Locale.FRENCH)), new HashSet<>(viewPage.getLocales()));

        // Verify edit mode informations in edit page
        getUtil().gotoPage(referenceDEFAULT, "edit", "language=");
        editPage = new WikiEditPage();
        editPage.waitUntilPageJSIsLoaded();

        assertEquals(Arrays.asList(), editPage.getNotExistingLocales());
        assertEquals(Arrays.asList(Locale.ENGLISH, Locale.FRENCH), editPage.getExistingLocales());
    }

    /**
     * Assert that the given <code>language</code> is present in various attributes and tags on the page
     * 
     * @param language the language to use, should be a valid language, e.g. "en"
     */
    private void checkLanguageTagsArePresent(String language)
    {
        WebElement html = getDriver().findElement(By.tagName("html"));
        Assert.assertEquals(language, html.getAttribute("lang"));
        Assert.assertEquals(language, html.getAttribute("xml:lang"));

        ViewPage vp = new ViewPage();
        // For retro-compatibility only
        Assert.assertEquals(language, vp.getHTMLMetaDataValue("language"));

        String content = getDriver().getPageSource();
        Assert.assertTrue(content.contains("language=" + language));
    }

    /**
     * Check if the currently displayed page is in English, by looking at the "Log-Out" link
     */
    private boolean isPageInEnglish()
    {
        return getDriver().findElement(By.className("xdocLastModification")).getText().toLowerCase()
            .contains("last modified by");
    }

    /**
     * Check if the currently displayed page is in French, by looking at the "Log-Out" link
     */
    private boolean isPageInFrench()
    {
        return getDriver().findElement(By.className("xdocLastModification")).getText().toLowerCase()
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
