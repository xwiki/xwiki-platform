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
    void translateInplace(TestUtils setup, TestReference testReference) throws Exception
    {
        // The hints displayed when creating a translation with the Translate button are in the language of the user
        // interface, German in the use case below.
        String expectedTranslateHint = renderTranslateHint(setup, "core.menu.translate.hint", "de");
        String expectedTitleHint = renderTranslateHint(setup, "edit.inplace.page.translate.messageAfter", "de");

        //
        // Verify the original translation (English).
        //

        setup.gotoPage(testReference, "view", "language=en");
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

        viewPage.setDocumentTitle("title FR");
        setContent("content FR");
        viewPage.saveAndView("Enregistr\u00E9");
        assertEquals("title FR", viewPage.getDocumentTitle());
        assertEquals("content FR", viewPage.getContent());
        assertFalse(viewPage.getTranslateButton().isDisplayed());

        // The Edit button should target the created translation now.
        viewPage.editInplace();
        assertEquals("title FR", viewPage.getDocumentTitle());

        viewPage.setDocumentTitle("French Title").saveAndView("Enregistr\u00E9");
        assertEquals("French Title", viewPage.getDocumentTitle());

        viewPage = assertLanguageSwitch(viewPage, "title EN", "content EN", "French Title", "content FR");

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
        assertEquals(expectedTranslateHint, viewPage.getTranslateButtonHint());

        viewPage.translateInplace();
        assertEquals(expectedTitleHint, viewPage.getDocumentTitleHint());
        assertFalse(viewPage.getTranslateButton().isDisplayed());
        assertEquals("title EN", viewPage.getDocumentTitle());

        viewPage.setDocumentTitle("title DE");
        setContent("content DE");
        viewPage.saveAndView("Gespeichert");
        assertEquals("title DE", viewPage.getDocumentTitle());
        assertEquals("content DE", viewPage.getContent());
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

        viewPage.setDocumentTitle("Original title");
        setContent("Original content");
        viewPage.saveAndView();
        assertEquals("Original title", viewPage.getDocumentTitle());
        assertEquals("Original content", viewPage.getContent());

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

    /**
     * The creation of a translation from the Information tab works the same for a terminal page.
     */
    @Test
    @Order(3)
    void translateTerminalPageFromInformationTab(TestUtils setup, TestReference testReference)
    {
        DocumentReference terminalPageReference =
            new DocumentReference("Terminal", testReference.getLastSpaceReference(), Locale.ENGLISH);
        setup.deletePage(terminalPageReference);
        setup.createPage(terminalPageReference, "content EN", "title EN");
        setup.gotoPage(terminalPageReference, "view", "language=en");

        new InplaceEditablePage().openInformationDocExtraPane().clickTranslationLink("French");
        InplaceEditablePage viewPage = new InplaceEditablePage().waitForInplaceEditor();
        assertEquals("title EN", viewPage.getDocumentTitle());
        viewPage.setDocumentTitle("title FR");
        setContent("content FR");
        viewPage.saveAndView("Enregistr\u00E9");
        assertEquals("title FR", viewPage.getDocumentTitle());
        assertEquals("content FR", viewPage.getContent());

        assertLanguageSwitch(viewPage, "title EN", "content EN", "title FR", "content FR");
    }

    private String renderTranslateHint(TestUtils setup, String translationKey, String language) throws Exception
    {
        // The hint takes as parameter the name of the language of the user interface, in that language. The
        // translation may be missing for the given language, in which case the English one is used.
        return setup.executeWiki("{{velocity}}$services.localization.render('" + translationKey
            + "', [$xcontext.locale.getDisplayName($xcontext.locale)]){{/velocity}}", Syntax.XWIKI_2_1,
            Map.of("outputSyntax", "plain", "language", language));
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
     *
     * @return the French translation of the page, in view mode
     */
    private InplaceEditablePage assertLanguageSwitch(InplaceEditablePage frenchPage, String englishTitle,
        String englishContent, String frenchTitle, String frenchContent)
    {
        InplaceEditablePage englishPage = frenchPage.switchToLocale(Locale.ENGLISH);
        assertEquals(englishTitle, englishPage.getDocumentTitle());
        assertEquals(englishContent, englishPage.getContent());

        InplaceEditablePage newFrenchPage = englishPage.switchToLocale(Locale.FRENCH);
        assertEquals(frenchTitle, newFrenchPage.getDocumentTitle());
        assertEquals(frenchContent, newFrenchPage.getContent());
        return newFrenchPage;
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
