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
package org.xwiki.blocknote.test.ui;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WindowType;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.administration.test.po.LocalizationAdministrationSectionPage;
import org.xwiki.blocknote.test.po.BlockNoteEditor;
import org.xwiki.blocknote.test.po.BlockNoteRichTextArea;
import org.xwiki.edit.test.po.InplaceEditablePage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.MultiUserTestUtils;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.ForceEditLockModal;
import org.xwiki.test.ui.po.editor.ForceEditLockPage;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Edit and save to check that the wiki syntax is preserved.
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
@UITest(
    properties = {
        // The Image Wizard needs this to be able to upload images.
        "xwikiCfgPlugins=com.xpn.xwiki.plugin.fileupload.FileUploadPlugin"
    },
    extraJARs = {
        // The WebSocket end-point implementation based on XWiki components needs to be installed as core extension.
        "org.xwiki.platform:xwiki-platform-websocket",

        // The macro service uses the extension index script service to get the list of uninstalled macros (from
        // extensions) which expects an implementation of the extension index. The extension index script service is a
        // core extension so we need to make the extension index also core.
        "org.xwiki.platform:xwiki-platform-extension-index",

        // Solr search is used to get suggestions for the link quick action.
        "org.xwiki.platform:xwiki-platform-search-solr-query"
    },
    servletEngineNetworkAliases = AbstractBlockNoteIT.XWIKI_ALIAS
)
class CollaborationIT extends AbstractBlockNoteIT
{
    @Test
    @Order(1)
    void editDifferentParagraphs(TestReference testReference, TestUtils setup, MultiUserTestUtils multiUserSetup)
    {
        //
        // First Tab
        //

        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "", "");

        new ViewPage().editWYSIWYG();
        BlockNoteEditor firstEditor = new BlockNoteEditor("content");
        BlockNoteRichTextArea firstTextArea = firstEditor.getRichTextArea();
        firstTextArea.click();
        firstTextArea.sendKeys("John:");

        //
        // Second Tab
        //

        String secondTabHandle = multiUserSetup.openNewBrowserTab(XWIKI_ALIAS);
        loginAsAlice(setup);

        setup.gotoPage(testReference);
        InplaceEditablePage secondEditPage = new InplaceEditablePage();
        editAndForceLock(secondEditPage, "John", setup);
        BlockNoteEditor secondEditor = new BlockNoteEditor("content");
        BlockNoteRichTextArea secondTextArea = secondEditor.getRichTextArea();

        // Each user types in their own paragraph.
        secondTextArea.waitUntilTextContains("John:");
        secondTextArea.appendParagraph("Alice:");

        //
        // First Tab
        //

        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());
        firstTextArea.waitUntilTextContains("Alice:");

        String firstUserText = "The five boxing wizards jump quickly. The quick brown fox jumps over the lazy dog. End";
        String secondUserText =
            "The quick brown fox jumps over the lazy dog. The five boxing wizards jump quickly. Fin";

        String[] firstUserWords = firstUserText.split(" ");
        String[] secondUserWords = secondUserText.split(" ");

        for (int i = 0; i < Math.min(firstUserWords.length, secondUserWords.length); i++) {
            //
            // Second Tab
            //
            multiUserSetup.switchToBrowserTab(secondTabHandle);
            secondTextArea.waitUntilFocused().sendKeys(" " + secondUserWords[i]);

            //
            // First Tab
            //
            multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());
            firstTextArea.waitUntilFocused().sendKeys(" " + firstUserWords[i]);
        }

        //
        // Second Tab
        //

        multiUserSetup.switchToBrowserTab(secondTabHandle);
        // Wait to receive all the content typed by the first user.
        secondTextArea.waitUntilTextContains("End");

        ViewPage viewPage = secondEditPage.saveAndView();
        assertEquals("John: %s%nAlice: %s".formatted(firstUserText, secondUserText), viewPage.getContent());
    }

    @Test
    @Order(2)
    void editSameParagraph(TestReference testReference, TestUtils setup, MultiUserTestUtils multiUserSetup)
    {
        //
        // First Tab
        //

        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "", "");

        new InplaceEditablePage().editInplace();
        BlockNoteEditor firstEditor = new BlockNoteEditor("content");
        BlockNoteRichTextArea firstTextArea = firstEditor.getRichTextArea();
        firstTextArea.click();
        firstTextArea.sendKeys("Separator.", Keys.HOME);

        //
        // Second Tab
        //

        String secondTabHandle = setup.getDriver().switchTo().newWindow(WindowType.TAB).getWindowHandle();

        setup.gotoPage(testReference);
        InplaceEditablePage secondEditPage = new InplaceEditablePage().editInplace();
        BlockNoteEditor secondEditor = new BlockNoteEditor("content");
        BlockNoteRichTextArea secondTextArea = secondEditor.getRichTextArea();
        secondTextArea.waitUntilTextContains("Separator.");
        secondTextArea.click();
        secondTextArea.sendKeys(Keys.PAGE_UP, Keys.END);

        String text = "The quick brown fox jumps over the lazy dog.";
        String[] words = text.split(" ");

        for (int i = 0; i < words.length; i++) {
            //
            // First Tab
            //
            setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
            firstTextArea.waitUntilFocused().sendKeys(words[i] + " ");

            //
            // Second Tab
            //
            setup.getDriver().switchTo().window(secondTabHandle);
            secondTextArea.waitUntilFocused().sendKeys(" " + words[i]);
        }

        //
        // First Tab
        //

        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
        firstTextArea.waitUntilFocused().sendKeys("First. ");

        //
        // Second Tab
        //

        setup.getDriver().switchTo().window(secondTabHandle);
        // Wait to receive all the content typed by the first user.
        secondTextArea.waitUntilTextContains("First.");

        ViewPage viewPage = secondEditPage.saveAndView();
        assertEquals(text + " First. Separator. " + text, viewPage.getContent());
    }

    @Test
    @Order(3)
    void applyInlineStylesOnTheSameParagraph(TestReference testReference, TestUtils setup,
        MultiUserTestUtils multiUserSetup)
    {
        //
        // First Tab
        //

        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "", "");

        new InplaceEditablePage().editInplace();
        BlockNoteEditor firstEditor = new BlockNoteEditor("content");
        BlockNoteRichTextArea firstTextArea = firstEditor.getRichTextArea();
        firstTextArea.click();
        firstTextArea.sendKeys("bold italic");
        // Move the caret at the beginning of the "italic" word.
        firstTextArea.sendKeys(Keys.chord(Keys.CONTROL, Keys.LEFT));

        //
        // Second Tab
        //

        String secondTabHandle = setup.getDriver().switchTo().newWindow(WindowType.TAB).getWindowHandle();
        setup.gotoPage(testReference);
        InplaceEditablePage firstEditPage = new InplaceEditablePage().editInplace();
        BlockNoteEditor secondEditor = new BlockNoteEditor("content");
        BlockNoteRichTextArea secondTextArea = secondEditor.getRichTextArea();
        secondTextArea.waitUntilTextContains("italic");
        secondTextArea.click();
        secondTextArea.sendKeys(Keys.PAGE_UP, Keys.END, " underline");
        // Select the "underline" word.
        secondTextArea.sendKeys(Keys.chord(Keys.SHIFT, Keys.CONTROL, Keys.ARROW_LEFT));

        //
        // First Tab
        //

        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
        firstTextArea.waitUntilFocused();
        // Select the "italic" word and apply the italic style.
        firstTextArea.sendKeys(Keys.chord(Keys.SHIFT, Keys.CONTROL, Keys.ARROW_RIGHT));
        firstTextArea.sendKeys(Keys.chord(Keys.CONTROL, "i"));

        //
        // Second Tab
        //

        setup.getDriver().switchTo().window(secondTabHandle);
        // Apply the underline style.
        secondTextArea.waitUntilFocused().sendKeys(Keys.chord(Keys.CONTROL, "u"));

        //
        // First Tab
        //

        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
        firstTextArea.waitUntilFocused();
        // Select the "bold" word and apply the bold style.
        firstTextArea.sendKeys(Keys.ARROW_LEFT, Keys.ARROW_LEFT);
        firstTextArea.sendKeys(Keys.chord(Keys.SHIFT, Keys.CONTROL, Keys.ARROW_LEFT));
        firstTextArea.sendKeys(Keys.chord(Keys.CONTROL, "b"));

        //
        // Second Tab
        //

        setup.getDriver().switchTo().window(secondTabHandle);
        secondTextArea.waitUntilFocused().sendKeys(Keys.ARROW_RIGHT);
        secondTextArea.sendKeys(Keys.chord(Keys.CONTROL, "u"));
        secondTextArea.sendKeys(" end");

        //
        // First Tab
        //

        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
        firstTextArea.waitUntilFocused().sendKeys(Keys.ARROW_RIGHT, "er");
        firstTextArea.waitUntilTextContains("end");

        String source = firstEditPage.saveAndView().editWiki().getContent();
        assertTrue(source.contains("**bolder** //italic// __underline__ end"), "Unexpected content: " + source);
    }

    @Test
    @Order(4)
    void localUndoRedo(TestUtils setup, TestReference testReference, MultiUserTestUtils multiUserSetup)
    {
        //
        // First Tab
        //

        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "", "");

        // Edit the page in the first browser tab (in-place).
        new InplaceEditablePage().editInplace();
        BlockNoteEditor firstEditor = new BlockNoteEditor("content");
        BlockNoteRichTextArea firstTextArea = firstEditor.getRichTextArea();
        firstTextArea.click();
        firstTextArea.sendKeys("one |", Keys.LEFT);

        //
        // Second Tab
        //

        String secondTabHandle = setup.getDriver().switchTo().newWindow(WindowType.TAB).getWindowHandle();

        // Edit the page in the second browser tab (standalone).
        setup.gotoPage(testReference).editWYSIWYG();
        BlockNoteEditor secondEditor = new BlockNoteEditor("content");
        BlockNoteRichTextArea secondTextArea = secondEditor.getRichTextArea();
        secondTextArea.click();
        secondTextArea.waitUntilTextContains("|");
        secondTextArea.sendKeys(Keys.PAGE_UP, Keys.END, " red");

        //
        // First Tab
        //

        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
        firstTextArea.waitUntilTextContains("red");
        firstTextArea.waitUntilFocused().sendKeys("two ");

        //
        // Second Tab
        //

        setup.getDriver().switchTo().window(secondTabHandle);
        secondTextArea.waitUntilTextContains("two");
        secondTextArea.waitUntilFocused().sendKeys(" green");

        //
        // First Tab
        //

        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
        firstTextArea.waitUntilTextContains("green");
        firstTextArea.waitUntilFocused().sendKeys(Keys.HOME);
        // Select "one" and replace.
        firstTextArea.sendKeys(Keys.chord(Keys.CONTROL, Keys.SHIFT, Keys.RIGHT));
        firstTextArea.sendKeys("1");

        //
        // Second Tab
        //

        setup.getDriver().switchTo().window(secondTabHandle);
        secondTextArea.waitUntilTextContains("1").waitUntilFocused();
        secondTextArea.sendKeys(Keys.chord(Keys.CONTROL, Keys.LEFT, Keys.LEFT));
        // Select "red" and replace.
        secondTextArea.sendKeys(Keys.chord(Keys.CONTROL, Keys.SHIFT, Keys.RIGHT));
        secondTextArea.sendKeys("-");

        //
        // First Tab
        //

        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
        firstTextArea.waitUntilTextContains("-").waitUntilFocused();
        firstTextArea.sendKeys(Keys.chord(Keys.CONTROL, "z"));
        firstTextArea.waitUntilTextIs("one two | -%s green", "John");
        firstTextArea.sendKeys(Keys.chord(Keys.CONTROL, "z"));
        firstTextArea.waitUntilTextIs("one | -%s green", "John");
        firstTextArea.sendKeys(Keys.chord(Keys.CONTROL, Keys.SHIFT, "z"));
        firstTextArea.waitUntilTextIs("one two | -%s green", "John");

        //
        // Second Tab
        //

        setup.getDriver().switchTo().window(secondTabHandle);
        secondTextArea.waitUntilTextIs("one%s two | - green", "John").waitUntilFocused();
        secondTextArea.sendKeys(Keys.chord(Keys.CONTROL, "z"));
        secondTextArea.waitUntilTextIs("one%s two | red green", "John");
        secondTextArea.sendKeys(Keys.chord(Keys.CONTROL, "z"));
        secondTextArea.waitUntilTextIs("one%s two | red", "John");
        secondTextArea.sendKeys(Keys.chord(Keys.CONTROL, Keys.SHIFT, "z"));
        secondTextArea.waitUntilTextIs("one%s two | red green", "John");

        //
        // First Tab
        //

        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
        firstTextArea.waitUntilTextIs("one two | red green%s", "John");
        firstTextArea.waitUntilFocused().sendKeys("[");

        //
        // Second Tab
        //

        setup.getDriver().switchTo().window(secondTabHandle);
        secondTextArea.waitUntilTextContains("[").waitUntilFocused();

        // Verify we can still redo local changes after a remote change is received.
        secondTextArea.sendKeys(Keys.chord(Keys.CONTROL, "z"));
        secondTextArea.waitUntilTextIs("[%s two | red", "John");
        secondTextArea.sendKeys(Keys.chord(Keys.CONTROL, Keys.SHIFT, "z"));
        secondTextArea.waitUntilTextIs("[%s two | red green", "John");
        secondTextArea.sendKeys(Keys.chord(Keys.CONTROL, Keys.SHIFT, "z"));
        secondTextArea.waitUntilTextIs("[%s two | - green", "John");

        // Verify the care position after a redo/undo sequence.
        secondTextArea.sendKeys("]");
        secondTextArea.assertTextIs("[%s two | ] green", "John");
    }

    @Test
    @Order(5)
    void editPageTranslation(TestUtils setup, TestReference testReference, MultiUserTestUtils multiUserSetup)
    {
        //
        // First Tab
        //

        // Start fresh. We target the default English translation in order to make sure we delete all the translations.
        DocumentReference testReferenceEn = new DocumentReference(testReference, Locale.ENGLISH);
        setup.deletePage(testReferenceEn);
        setup.createPage(testReferenceEn, "One", "");

        // Edit the default page translation in the first browser tab (in-place).
        InplaceEditablePage firstEditPage = new InplaceEditablePage().editInplace();
        BlockNoteEditor firstEditor = new BlockNoteEditor("content");
        BlockNoteRichTextArea firstTextArea = firstEditor.getRichTextArea();
        firstTextArea.click().sendKeys(Keys.PAGE_UP, Keys.END, " two");

        //
        // Second Tab
        //

        // Enable multilingual support from a different tab, using the alias to be able to login as super admin and to
        // avoid changing the language of the first tab.
        String secondTabHandle = multiUserSetup.openNewBrowserTab(XWIKI_ALIAS);
        setup.loginAsSuperAdmin();
        // We also want to verify that language variants are properly handled.
        setMultiLingual(setup, true, "en", "de", "fr_CA");

        // Edit the default page translation in the second browser tab (standalone).
        setup.gotoPage(testReferenceEn).editWYSIWYG();
        new ForceEditLockPage().clickForceEdit();
        WYSIWYGEditPage secondEditPage = new WYSIWYGEditPage();
        BlockNoteEditor secondEditor = new BlockNoteEditor("content");
        BlockNoteRichTextArea secondTextArea = secondEditor.getRichTextArea();
        secondTextArea.click().waitUntilTextContains("two");
        secondTextArea.sendKeys(Keys.PAGE_UP, Keys.END, " three");

        //
        // First Tab
        //

        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());
        firstTextArea.waitUntilTextContains("three");
        firstTextArea.assertTextIs("One two three%s", "superadmin");

        // Leave and create the German translation.
        firstEditPage.cancel();
        DocumentReference testReferenceDe = new DocumentReference(testReference, Locale.GERMAN);
        setup.createPage(testReferenceDe, "Eins", "");
        firstEditPage = new InplaceEditablePage().editInplace();
        firstEditor = new BlockNoteEditor("content");
        firstTextArea = firstEditor.getRichTextArea();
        firstTextArea.click().sendKeys(Keys.PAGE_UP, Keys.END, " zwei");

        //
        // Second Tab
        //

        multiUserSetup.switchToBrowserTab(secondTabHandle);
        secondTextArea.waitUntilFocused().sendKeys(" four");
        secondTextArea.assertTextIs("One two three four");

        // Leave the default (English) translation and join the German translation.
        secondEditPage.clickCancel().openInformationDocExtraPane().clickTranslationLink(Locale.GERMAN);
        new ViewPage().editWYSIWYG();
        new ForceEditLockPage().clickForceEdit();
        secondEditPage = new WYSIWYGEditPage();
        secondEditor = new BlockNoteEditor("content");
        secondTextArea = secondEditor.getRichTextArea();
        secondTextArea.click().waitUntilTextContains("zwei");
        secondTextArea.sendKeys(Keys.PAGE_UP, Keys.END, " drei");

        //
        // First Tab
        //

        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());
        firstTextArea.waitUntilTextContains("drei");
        firstTextArea.assertTextIs("Eins zwei drei%s", "superadmin");

        // Leave and start editing the French Canadian translation (without saving).
        firstEditPage.cancel().openInformationDocExtraPane().clickTranslationLink(Locale.CANADA_FRENCH);
        firstEditor = new BlockNoteEditor("content");
        firstTextArea = firstEditor.getRichTextArea();
        firstTextArea.click().assertTextIs("One");
        firstTextArea.sendKeys(Keys.chord(Keys.CONTROL, "a")).sendKeys("Un deux");

        //
        // Second Tab
        //

        multiUserSetup.switchToBrowserTab(secondTabHandle);
        secondTextArea.waitUntilFocused().sendKeys(" vier");
        secondTextArea.assertTextIs("Eins zwei drei vier");

        // Leave the German translation and join the French Canadian translation.
        secondEditPage.clickCancel();
        setup.gotoPage(testReference, "edit", "language=fr_CA&editor=wysiwyg");
        secondEditor = new BlockNoteEditor("content");
        secondTextArea = secondEditor.getRichTextArea();
        secondTextArea.waitUntilTextContains("Un deux").click();
        secondTextArea.sendKeys(Keys.PAGE_UP, Keys.END, " trois");

        //
        // First Tab
        //

        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());
        firstTextArea.waitUntilTextContains("trois");
        firstTextArea.assertTextIs("Un deux trois%s", "superadmin");
    }

    @Test
    @Order(6)
    void restrictScriptMacroExecution(TestUtils setup, TestReference testReference, MultiUserTestUtils multiUserSetup)
    {
        //
        // First Tab
        //

        // Start fresh. We target the default English translation because the previous test has changed the current UI
        // language.
        DocumentReference testReferenceEn = new DocumentReference(testReference, Locale.ENGLISH);
        setup.deletePage(testReferenceEn);
        setup.createPage(testReferenceEn, """
            one

            {{velocity}}
            $xcontext.userReference.name
            {{/velocity}}
            """, "");

        // Edit the page in the first browser tab (in-place) as John (no script rights).
        InplaceEditablePage firstEditPage = new InplaceEditablePage().editInplace();
        BlockNoteEditor firstEditor = new BlockNoteEditor("content");
        BlockNoteRichTextArea firstTextArea = firstEditor.getRichTextArea();
        // The title field is focused by default. Move the focus to the content field.
        setup.getDriver().switchTo().activeElement().sendKeys(Keys.TAB);
        // John makes a change and becomes the effective author of the collaboration session.
        firstTextArea.sendKeys(Keys.PAGE_UP, Keys.END, " two");

        //
        // Second Tab
        //

        // Edit the page in the second browser tab (standalone) as superadmin (has script rights).
        String secondTabHandle = multiUserSetup.openNewBrowserTab(XWIKI_ALIAS);
        setup.loginAsSuperAdmin();
        setup.gotoPage(testReferenceEn).editWYSIWYG();
        new ForceEditLockPage().clickForceEdit();
        WYSIWYGEditPage secondEditPage = new WYSIWYGEditPage();
        BlockNoteEditor secondEditor = new BlockNoteEditor("content");
        BlockNoteRichTextArea secondTextArea = secondEditor.getRichTextArea();
        // Superadmin makes a change but John remains the effective author because the session script level cannot
        // increase.
        secondTextArea.waitUntilTextContains("two");
        secondTextArea.sendKeys(Keys.PAGE_UP, Keys.END, " three");

        // Save and check that the script macro is not executed because John is the effective author.
        ViewPage secondViewPage = secondEditPage.clickSaveAndView();
        String content = secondViewPage.getContent();
        assertTrue(content.contains("The execution of the [velocity] script macro is not allowed"),
            "Unexpected content: " + content);

        //
        // First Tab
        //

        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());
        firstTextArea.waitUntilTextContains("three");

        // Save and check that the script macro is not executed: John is still the effective author.
        content = firstEditPage.saveAndView().getContent();
        assertTrue(content.contains("The execution of the [velocity] script macro is not allowed"),
            "Unexpected content: " + content);

        //
        // Second Tab
        //

        multiUserSetup.switchToBrowserTab(secondTabHandle);
        // Start a new collaboration session.
        secondEditPage = secondViewPage.editWYSIWYG();
        secondEditor = new BlockNoteEditor("content");
        secondTextArea = secondEditor.getRichTextArea();
        // Superadmin makes a change and becomes the effective author of the new collaboration session.
        secondTextArea.sendKeys(Keys.PAGE_UP, Keys.END, " four");

        //
        // First Tab
        //

        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());
        // Join the new collaboration session.
        editAndForceLock(firstEditPage, "superadmin", setup);
        firstEditor = new BlockNoteEditor("content");
        firstTextArea = firstEditor.getRichTextArea();
        firstTextArea.waitUntilTextContains("four");

        // Save and check that the script macro is not executed: even though superadmin is the effective author, if the
        // user saving the page (John in this case) has less script rights, it becomes the effective script author for
        // that revision.
        content = firstEditPage.saveAndView().getContent();
        assertTrue(content.contains("The execution of the [velocity] script macro is not allowed"),
            "Unexpected content: " + content);

        //
        // Second Tab
        //

        multiUserSetup.switchToBrowserTab(secondTabHandle);
        secondTextArea.waitUntilFocused().sendKeys(" five");

        // Save (to check that the script macro is executed) but don't leave the edit mode yet. We want to keep the
        // collaboration session active for John to be able to rejoin.
        secondEditPage.clickSaveAndContinue();

        //
        // First Tab
        //

        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());
        // Re-join the collaboration session.
        editAndForceLock(firstEditPage, "superadmin", setup);
        firstEditor = new BlockNoteEditor("content");
        firstTextArea = firstEditor.getRichTextArea();
        firstTextArea.waitUntilTextContains("five");
        // John makes a change and becomes the effective author.
        firstTextArea.sendKeys(Keys.PAGE_UP, Keys.END, " six");

        //
        // Second Tab
        //

        multiUserSetup.switchToBrowserTab(secondTabHandle);
        secondTextArea.waitUntilTextContains("six");
        // Cancel and check that the script macro is executed after the last save.
        content = secondEditPage.clickCancel().getContent();
        assertTrue(content.contains("superadmin"), "Unexpected content: " + content);

        //
        // First Tab
        //

        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());
        // Save and check that the script macro is not executed since John is the effective author.
        content = firstEditPage.saveAndView().getContent();
        assertTrue(content.contains("The execution of the [velocity] script macro is not allowed"),
            "Unexpected content: " + content);
    }

    private void editAndForceLock(InplaceEditablePage inplaceEditablePage, String lockedBy, TestUtils setup)
    {
        inplaceEditablePage.edit();
        ForceEditLockModal forceEditLockModal = new ForceEditLockModal();
        setup.getDriver().waitUntilCondition(driver -> forceEditLockModal.isDisplayed());
        assertThat(forceEditLockModal.getMessage(), containsString("This page is currently locked by " + lockedBy));
        forceEditLockModal.clickOk();
        inplaceEditablePage.waitForInplaceEditor();
    }

    private void setMultiLingual(TestUtils setup, boolean isMultiLingual, String... supportedLanguages)
    {
        AdministrationPage adminPage = AdministrationPage.gotoPage();
        LocalizationAdministrationSectionPage sectionPage = adminPage.clickLocalizationSection();
        sectionPage.setMultiLingual(isMultiLingual);
        sectionPage.setDefaultLanguage("en");
        sectionPage.setSupportedLanguages(List.of(supportedLanguages));
        // The localization administration section doesn't save asynchronously so we can't wait for the save success
        // notification. There's no JavaScript involved in the form submit process so Selenium should wait for the page
        // to be reloaded. However, in practice we noticed that we can't always navigate to another page right after
        // saving the localization settings, probably because the browser is in the process of reloading the page.
        setup.getDriver().addPageNotYetReloadedMarker();
        sectionPage.clickSave();
        setup.getDriver().waitUntilPageIsReloaded();
    }
}
