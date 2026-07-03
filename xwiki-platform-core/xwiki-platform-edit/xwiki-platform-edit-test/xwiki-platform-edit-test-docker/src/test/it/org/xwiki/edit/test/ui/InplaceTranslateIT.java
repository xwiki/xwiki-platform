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
package org.xwiki.edit.test.ui;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.edit.test.po.InplaceEditablePage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.InformationPane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests in-place page translating.
 * 
 * @version $Id$
 * @since 12.10.6
 * @since 13.2RC1
 */
@UITest
class InplaceTranslateIT
{
    @BeforeEach
    void setup(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.loginAsSuperAdmin();
        setup.setWikiPreference("multilingual", "true");
        setup.setWikiPreference("languages", "de,en,fr,it");
        setup.setWikiPreference("default_language", "en");

        setup.createUserAndLogin("alice", "pa$$word", "editor", "Wysiwyg");
        // Make sure we create the page with English as default locale.
        DocumentReference originalTranslationReference = new DocumentReference(testReference, Locale.ENGLISH);
        setup.deletePage(originalTranslationReference);
        setup.createPage(originalTranslationReference, "content EN", "title EN");
    }

    @AfterAll
    void tearDown(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();
        setup.setWikiPreference("multilingual", "false");
        setup.setWikiPreference("languages", "en");
    }

    @Test
    void translateInplace(TestUtils setup, TestReference testReference)
    {
        //
        // Verify the original translation (English).
        //

        InplaceEditablePage viewPage = new InplaceEditablePage();
        assertFalse(viewPage.hasTranslateButton());

        InformationPane infoPane = viewPage.openInformationDocExtraPane();
        assertEquals("English", infoPane.getLocale());
        assertTrue(infoPane.isOriginalLocale());
        assertEquals(Collections.emptyList(), infoPane.getAvailableTranslations());
        assertEquals(Arrays.asList("German", "French", "Italian"), infoPane.getMissingTranslations());

        //
        // Create translation from the Information tab link.
        //

        infoPane.clickTranslationLink("French");
        viewPage = new InplaceEditablePage().waitForInplaceEditor();
        assertFalse(viewPage.getTranslateButton().isDisplayed());
        assertEquals("title EN", viewPage.getDocumentTitle());

        viewPage.setDocumentTitle("title FR").saveAndView("Enregistr\u00E9");
        assertEquals("title FR", viewPage.getDocumentTitle());
        assertFalse(viewPage.getTranslateButton().isDisplayed());

        // The Edit button should target the created translation now.
        viewPage.editInplace();
        assertEquals("title FR", viewPage.getDocumentTitle());

        viewPage.setDocumentTitle("French Title").saveAndView("Enregistr\u00E9");
        assertEquals("French Title", viewPage.getDocumentTitle());

        infoPane = viewPage.openInformationDocExtraPane();
        assertEquals("français", infoPane.getLocale());
        assertEquals("anglais", infoPane.getOriginalLocale());
        assertEquals(Collections.emptyList(), infoPane.getAvailableTranslations());
        assertEquals(Arrays.asList("allemand", "italien"), infoPane.getMissingTranslations());

        //
        // Create translation from the Translate button.
        //

        infoPane.clickTranslationLink("allemand");
        viewPage = new InplaceEditablePage().waitForInplaceEditor();
        assertEquals("title EN", viewPage.getDocumentTitle());

        // Cancel because we want to use the Translate button.
        viewPage.cancel();
        assertTrue(viewPage.getTranslateButton().isDisplayed());

        viewPage.translateInplace();
        assertFalse(viewPage.getTranslateButton().isDisplayed());
        assertEquals("title EN", viewPage.getDocumentTitle());

        viewPage.setDocumentTitle("title DE").saveAndView("Gespeichert");
        assertEquals("title DE", viewPage.getDocumentTitle());
        assertFalse(viewPage.getTranslateButton().isDisplayed());

        infoPane = viewPage.openInformationDocExtraPane();
        assertEquals("Deutsch", infoPane.getLocale());
        assertEquals("Englisch", infoPane.getOriginalLocale());
        assertEquals(Arrays.asList("Französisch"), infoPane.getAvailableTranslations());
        assertEquals(Arrays.asList("Italienisch"), infoPane.getMissingTranslations());

        //
        // Create translation with Edit + Translate
        //

        infoPane.clickTranslationLink("Italienisch");
        viewPage = new InplaceEditablePage().waitForInplaceEditor().cancel();
        assertEquals("title EN", viewPage.getDocumentTitle());

        // Edit the original translation first and then create the missing translation.
        viewPage.editInplace();
        assertTrue(viewPage.getTranslateButton().isDisplayed());
        assertEquals("title EN", viewPage.getDocumentTitle());

        viewPage.setDocumentTitle("English Title").save();
        // This changes the document locale.
        viewPage.getTranslateButton().click();
        viewPage.waitForInplaceEditor();
        assertEquals("English Title", viewPage.getDocumentTitle());

        viewPage.setDocumentTitle("title IT").saveAndView();
        assertEquals("title IT", viewPage.getDocumentTitle());

        infoPane = viewPage.openInformationDocExtraPane();
        assertEquals("italiano", infoPane.getLocale());
        assertEquals("inglese", infoPane.getOriginalLocale());
        assertEquals(Arrays.asList("tedesco", "francese"), infoPane.getAvailableTranslations());
        assertEquals(Collections.emptyList(), infoPane.getMissingTranslations());

        //
        // Edit existing translation.
        //

        // The links to the available translations should go to view mode.
        infoPane.clickTranslationLink("tedesco");
        viewPage = new InplaceEditablePage();
        assertFalse(viewPage.hasTranslateButton());
        assertEquals("title DE", viewPage.getDocumentTitle());

        viewPage.editInplace().setDocumentTitle("Deutsch Title").saveAndView("Gespeichert");
        assertEquals("Deutsch Title", viewPage.getDocumentTitle());

        //
        // Final check on the original locale.
        //

        infoPane = viewPage.openInformationDocExtraPane();
        infoPane.clickTranslationLink("Englisch");
        viewPage = new InplaceEditablePage();
        assertEquals("English Title", viewPage.getDocumentTitle());
        assertFalse(viewPage.hasTranslateButton());

        viewPage.editInplace();
        assertEquals("English Title", viewPage.getDocumentTitle());

        viewPage.setDocumentTitle("Original title").saveAndView();
        assertEquals("Original title", viewPage.getDocumentTitle());

        infoPane = viewPage.openInformationDocExtraPane();
        assertEquals("English", infoPane.getLocale());
        assertTrue(infoPane.isOriginalLocale());
        assertEquals(Arrays.asList("German", "French", "Italian"), infoPane.getAvailableTranslations());
        assertEquals(Collections.emptyList(), infoPane.getMissingTranslations());
    }
}
