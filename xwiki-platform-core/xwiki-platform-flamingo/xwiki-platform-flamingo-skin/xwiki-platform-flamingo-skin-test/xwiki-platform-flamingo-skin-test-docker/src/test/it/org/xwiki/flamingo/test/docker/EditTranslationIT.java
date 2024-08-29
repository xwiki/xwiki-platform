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
package org.xwiki.flamingo.test.docker;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.InlinePage;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test the edition of a translation.
 *
 * @version $Id$
 * @since 11.3RC1
 */
@UITest
class EditTranslationIT
{
    @BeforeAll
    public void setup(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();
    }

    @AfterAll
    public void tearDown(TestUtils setup) throws Exception
    {
        setup.setWikiPreference("multilingual", "false");
        setup.setWikiPreference("default_language", "en");
    }

    @Test
    @Order(1)
    void translateDocument(TestUtils setup, TestReference testReference) throws Exception
    {
        LocalDocumentReference referenceDEFAULT = new LocalDocumentReference(testReference);
        LocalDocumentReference referenceFR = new LocalDocumentReference(referenceDEFAULT, Locale.FRENCH);
        LocalDocumentReference referenceEN = new LocalDocumentReference(referenceDEFAULT, Locale.ENGLISH);

        String originalContent = "original content";
        String originalTitle = "original title";

        String englishContent = "english content";
        String englishTitle = "english title";

        String frenchContent = "french content";
        String frenchTitle = "french title";

        // Cleanup
        setup.rest().delete(referenceFR);
        setup.rest().delete(referenceEN);
        setup.rest().delete(referenceDEFAULT);

        // Create default version before setting the language settings
        // Ensure that we don't have a conflict window when creating the translations (cf. XWIKI-16299)
        ViewPage viewPage = setup.createPage(referenceDEFAULT, "", "");
        WikiEditPage editPage = viewPage.editWiki();
        editPage.setContent(originalContent);
        editPage.setTitle(originalTitle);
        viewPage = editPage.clickSaveAndView();
        assertEquals(originalContent, viewPage.getContent());
        assertEquals(originalTitle, viewPage.getDocumentTitle());

        // Set 2 locales
        setup.setWikiPreference("multilingual", "true");
        setup.setWikiPreference("languages", "en,fr");

        viewPage = setup.gotoPage(referenceDEFAULT);
        // Edit the page
        editPage = viewPage.editWiki();

        // First edition in multilingual is the edition of default document.
        assertEquals("", editPage.getMetaDataValue("locale"));
        assertEquals(new HashSet<>(Collections.singleton(Locale.FRENCH)), editPage.getNotExistingLocales());
        assertEquals(new HashSet<>(), editPage.getExistingLocales());

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
        setup.gotoPage(referenceDEFAULT, "view", "language=en");
        viewPage = new ViewPage();

        // Go back to default page editor to ensure we can still edit it
        editPage = viewPage.editWiki();
        assertEquals("en", editPage.getMetaDataValue("locale"));
        editPage = editPage.clickTranslate("default");
        assertEquals("", editPage.getMetaDataValue("locale"));
        assertEquals(originalContent, editPage.getContent());
        assertEquals(originalTitle, editPage.getTitle());

        // Make sure all have the right content
        Page page = setup.rest().get(referenceFR);
        assertEquals(frenchTitle, page.getTitle());
        assertEquals(frenchContent, page.getContent());
        page = setup.rest().get(referenceEN);
        assertEquals(englishTitle, page.getTitle());
        assertEquals(englishContent, page.getContent());
        page = setup.rest().get(referenceDEFAULT);
        assertEquals(originalTitle, page.getTitle());
        assertEquals(originalContent, page.getContent());

        // Make sure that URL with languages is working too
        setup.gotoPage(referenceDEFAULT, "view", "language=fr");
        viewPage = new ViewPage();
        assertEquals(frenchTitle, viewPage.getDocumentTitle());
        assertEquals(frenchContent, viewPage.getContent());
        setup.gotoPage(referenceDEFAULT, "view", "language=en");
        viewPage = new ViewPage();
        assertEquals(englishTitle, viewPage.getDocumentTitle());
        assertEquals(englishContent, viewPage.getContent());
        setup.gotoPage(referenceDEFAULT, "view", "language=");
        viewPage = new ViewPage();
        assertEquals(originalTitle, viewPage.getDocumentTitle());
        assertEquals(originalContent, viewPage.getContent());

        // Make sure three locales are listed for this page in the UI
        assertEquals(new HashSet<>(Arrays.asList(Locale.ROOT, Locale.ENGLISH, Locale.FRENCH)), new HashSet<>(viewPage.getLocales()));

        // Switch to en by switching language in the Drawer to test the feature
        viewPage.clickLocale(Locale.ENGLISH);

        // Verify edit mode informations in edit page
        setup.gotoPage(referenceDEFAULT, "edit", "language=");
        editPage = new WikiEditPage();

        assertEquals(new HashSet<>(), editPage.getNotExistingLocales());
        assertEquals(new HashSet<>(Arrays.asList(Locale.ENGLISH, Locale.FRENCH)), editPage.getExistingLocales());
    }

    /**
     * Tests that saving a panel always updates the default page translation even when another locale is specified,
     * because all panel information is stored on the default panel page translation using an xobject (which is shared
     * by all panel page translations but editable only through the default translation).
     */
    @Test
    @Order(2)
    @Disabled("It's possible to create a panel page translation from the UI, e.g. using the Wiki edit mode or from the"
        + " Information tab for user-created panels, but editing it doesn't update the panel meta data from the default"
        + " page translation, meaning that all changes are lost. See XWIKI-9617: Object properties are not saved when"
        + " editing a document translation inline")
    void testTranslatePanel(TestUtils setup, TestReference testReference) throws Exception
    {
        // Create a new Panel with a content and set the wiki as multilingual en/fr.
        setup.addObject(testReference, "Panels.PanelClass", "content", "custom panel");
        setup.setWikiPreference("multilingual", "true");
        setup.setWikiPreference("languages", "en,fr");

        setup.gotoPage(testReference, "edit", "language=en&editor=inline");
        InlinePage inlinePage = new InlinePage();
        assertEquals("custom panel", inlinePage.getValue("content"));
        inlinePage.setValue("content", "another value");
        inlinePage.clickSaveAndView();
        setup.gotoPage(testReference, "edit", "language=en&editor=inline");
        assertEquals("another value", inlinePage.getValue("content"));
    }
}
