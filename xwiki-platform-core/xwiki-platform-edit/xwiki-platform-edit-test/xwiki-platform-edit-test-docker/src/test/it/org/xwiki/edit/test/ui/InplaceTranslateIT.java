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
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.ckeditor.test.po.CKEditor;
import org.xwiki.ckeditor.test.po.RichTextAreaElement;
import org.xwiki.edit.test.po.InplaceEditablePage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.docker.junit5.MultiUserTestUtils;
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
@UITest(servletEngineNetworkAliases = InplaceTranslateIT.XWIKI_ALIAS)
class InplaceTranslateIT
{
    /**
     * Used to authenticate a second user, in a second browser tab, in order to check which document translation is
     * locked while the first user is editing.
     */
    public static final String XWIKI_ALIAS = "xwiki-alias";

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

    @AfterEach
    void closeBrowserTabs(TestUtils setup, MultiUserTestUtils multiUserSetup)
    {
        // Leave the edit mode on each tab, otherwise closing them can trigger the unsaved changes confirmation.
        setup.getDriver().getWindowHandles().forEach(handle -> {
            multiUserSetup.switchToBrowserTab(handle);
            setup.maybeLeaveEditMode();
        });
        multiUserSetup.closeTabs();
    }

    @AfterAll
    void tearDown(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();
        setup.setWikiPreference("multilingual", "false");
        setup.setWikiPreference("languages", "en");
    }

    @Test
    @Order(1)
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

    /**
     * The in-place editor has to lock the translation that is being created, and release the lock of the original
     * translation, no matter how the creation of the new translation was started.
     */
    @Test
    @Order(2)
    void lockTranslationToCreate(TestUtils setup, TestReference testReference, MultiUserTestUtils multiUserSetup)
    {
        // Alice is logged in on the first tab (see #setup). Log in Bob on a second tab, so that we can check which
        // translation is locked while Alice is creating a new one.
        String bobTab = multiUserSetup.openNewBrowserTab(XWIKI_ALIAS);
        setup.createUserAndLogin("bob", "pa$$word", "editor", "Wysiwyg");
        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());

        // Use case 1: create the translation directly from the Translate button, from view mode.
        setup.gotoPage(testReference, "view", "language=fr");
        InplaceEditablePage alicePage = new InplaceEditablePage();
        assertTrue(alicePage.hasTranslateButton());
        alicePage.translateInplace().waitForEditedLocale("fr");

        assertOnlyTranslationToCreateIsLocked(setup, testReference, multiUserSetup, bobTab);

        // Release the lock on the French translation before testing the second use case.
        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());
        alicePage.cancel();

        // Use case 2: start editing the original translation and then switch to creating the new translation.
        setup.gotoPage(testReference, "view", "language=fr");
        alicePage = new InplaceEditablePage().editInplace().waitForEditedLocale("");
        alicePage.translateInplace().waitForEditedLocale("fr");

        assertOnlyTranslationToCreateIsLocked(setup, testReference, multiUserSetup, bobTab);
    }

    @Test
    @Order(3)
    void translateFromInformationTab(TestUtils setup, TestReference testReference)
    {
        // Translate both a non-terminal page and a terminal page.
        updateAndTranslateFromInformationTab(setup, testReference);
        DocumentReference terminalPageReference =
            new DocumentReference("Terminal", testReference.getLastSpaceReference(), Locale.ENGLISH);
        setup.createPage(terminalPageReference, "content EN", "title EN");
        updateAndTranslateFromInformationTab(setup, terminalPageReference);
    }

    private void updateAndTranslateFromInformationTab(TestUtils setup, DocumentReference reference)
    {
        // Update the original translation (English).
        setup.gotoPage(reference, "view", "language=en");
        InplaceEditablePage viewPage = new InplaceEditablePage().editInplace();
        viewPage.setDocumentTitle("English title");
        setContent("English content");
        viewPage.saveAndView();
        assertEquals("English title", viewPage.getDocumentTitle());
        assertEquals("English content", viewPage.getContent());

        // Create the French translation from the Information tab.
        viewPage.openInformationDocExtraPane().clickTranslationLink("French");
        viewPage = new InplaceEditablePage().waitForInplaceEditor();
        viewPage.setDocumentTitle("Titre français");
        setContent("Contenu français");
        viewPage.saveAndView("Enregistré");
        assertEquals("Titre français", viewPage.getDocumentTitle());
        assertEquals("Contenu français", viewPage.getContent());

        assertLanguageSwitch(viewPage, "English title", "English content");
    }

    @Test
    @Order(4)
    void translateWithFrenchUI(TestUtils setup, TestReference testReference) throws Exception
    {
        // The French translations of these hints may be missing, in which case the English ones are used.
        Map<String, String> frenchUI = Map.of("outputSyntax", "plain", "language", "fr");
        String expectedTranslateHint = setup.executeWiki("{{velocity}}$services.localization.render("
            + "'core.menu.translate.hint', [$xcontext.locale.getDisplayName($xcontext.locale)]){{/velocity}}",
            Syntax.XWIKI_2_1, frenchUI);
        String expectedTitleHint = setup.executeWiki("{{velocity}}$services.localization.render("
            + "'edit.inplace.page.translate.messageAfter', [$xcontext.locale.getDisplayName($xcontext.locale)])"
            + "{{/velocity}}", Syntax.XWIKI_2_1, frenchUI);

        // The page has no French translation yet, so the French user interface offers to translate it.
        setup.gotoPage(testReference, "view", "language=fr");
        InplaceEditablePage viewPage = new InplaceEditablePage();
        assertEquals(expectedTranslateHint, viewPage.getTranslateButtonHint());

        viewPage.translateInplace();
        assertEquals(expectedTitleHint, viewPage.getDocumentTitleHint());
        viewPage.setDocumentTitle("Titre français");
        setContent("Contenu français");
        viewPage.saveAndView("Enregistré");
        assertEquals("Titre français", viewPage.getDocumentTitle());
        assertEquals("Contenu français", viewPage.getContent());

        assertLanguageSwitch(viewPage, "title EN", "content EN");
    }

    private void setContent(String content)
    {
        RichTextAreaElement richTextArea = new CKEditor("content").getRichTextArea();
        richTextArea.clear();
        richTextArea.sendKeys(content);
    }

    /**
     * Switches between the original (English) and the French translations of the current page using the Languages
     * menu of the drawer, and checks that each translation shows its own title and content.
     */
    private void assertLanguageSwitch(InplaceEditablePage frenchPage, String englishTitle, String englishContent)
    {
        InplaceEditablePage englishPage = frenchPage.switchToLocale(Locale.ENGLISH);
        assertEquals(englishTitle, englishPage.getDocumentTitle());
        assertEquals(englishContent, englishPage.getContent());

        frenchPage = englishPage.switchToLocale(Locale.FRENCH);
        assertEquals("Titre français", frenchPage.getDocumentTitle());
        assertEquals("Contenu français", frenchPage.getContent());
    }

    /**
     * Checks, as Bob, that the translation Alice is creating is locked while the original translation is not.
     */
    private void assertOnlyTranslationToCreateIsLocked(TestUtils setup, TestReference testReference,
        MultiUserTestUtils multiUserSetup, String bobTab)
    {
        multiUserSetup.switchToBrowserTab(bobTab);

        // The translation being created is locked, so Bob is asked to confirm before taking over the lock.
        setup.gotoPage(testReference, "view", "language=fr");
        InplaceEditablePage bobPage = new InplaceEditablePage();
        bobPage.clickTranslate();
        assertTrue(bobPage.waitForEditLockConfirmation().contains("alice"),
            "Expected the French translation to be locked by alice.");

        // The original translation is not locked, so Bob can edit it without being asked to confirm.
        setup.gotoPage(testReference, "view", "language=en");
        new InplaceEditablePage().editInplace().cancel();
    }
}
