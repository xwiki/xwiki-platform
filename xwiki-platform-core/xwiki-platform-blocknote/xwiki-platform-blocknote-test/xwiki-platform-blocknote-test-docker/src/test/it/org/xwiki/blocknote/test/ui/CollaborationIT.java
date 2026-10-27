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

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WindowType;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.administration.test.po.LocalizationAdministrationSectionPage;
import org.xwiki.blocknote.test.po.BlockNoteEditor;
import org.xwiki.blocknote.test.po.BlockNoteInplaceEditPage;
import org.xwiki.blocknote.test.po.BlockNoteRichTextArea;
import org.xwiki.blocknote.test.po.BlockNoteStandaloneEditPage;
import org.xwiki.edit.test.po.InplaceEditablePage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.realtime.test.po.CoeditorElement;
import org.xwiki.realtime.test.po.HistoryDropdown;
import org.xwiki.realtime.test.po.RealtimeEditToolbar;
import org.xwiki.realtime.test.po.SaveStatus;
import org.xwiki.realtime.test.po.SummaryModal;
import org.xwiki.realtime.test.po.VersionElement;
import org.xwiki.test.docker.junit5.MultiUserTestUtils;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.HistoryPane;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.ForceEditLockModal;
import org.xwiki.test.ui.po.editor.ForceEditLockPage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

        BlockNoteInplaceEditPage secondEditPage = BlockNoteInplaceEditPage.gotoPage(testReference);
        joinSession(secondEditPage, setup);
        BlockNoteEditor secondEditor = secondEditPage.getContentEditor();
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

        ViewPage viewPage = secondEditPage.done();
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

        BlockNoteInplaceEditPage secondEditPage = BlockNoteInplaceEditPage.gotoPage(testReference).editInplace();
        BlockNoteEditor secondEditor = secondEditPage.getContentEditor();
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

        ViewPage viewPage = secondEditPage.done();
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

        BlockNoteInplaceEditPage firstEditPage = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteEditor firstEditor = firstEditPage.getContentEditor();
        BlockNoteRichTextArea firstTextArea = firstEditor.getRichTextArea();
        firstTextArea.click();
        firstTextArea.sendKeys("bold italic");
        // Move the caret at the beginning of the "italic" word.
        firstTextArea.sendKeys(Keys.chord(Keys.CONTROL, Keys.LEFT));

        //
        // Second Tab
        //

        String secondTabHandle = setup.getDriver().switchTo().newWindow(WindowType.TAB).getWindowHandle();
        BlockNoteEditor secondEditor =
            BlockNoteInplaceEditPage.gotoPage(testReference).editInplace().getContentEditor();
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

        String source = firstEditPage.done().editWiki().getContent();
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
        BlockNoteInplaceEditPage firstEditPage = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteEditor firstEditor = firstEditPage.getContentEditor();
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
        BlockNoteStandaloneEditPage secondEditPage = new BlockNoteStandaloneEditPage();
        BlockNoteEditor secondEditor = secondEditPage.getContentEditor();
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
        firstEditPage = new BlockNoteInplaceEditPage().editInplace();
        firstEditor = firstEditPage.getContentEditor();
        firstTextArea = firstEditor.getRichTextArea();
        firstTextArea.click().sendKeys(Keys.PAGE_UP, Keys.END, " zwei");

        //
        // Second Tab
        //

        multiUserSetup.switchToBrowserTab(secondTabHandle);
        secondTextArea.waitUntilFocused().sendKeys(" four");
        secondTextArea.assertTextIs("One two three four");

        // Leave the default (English) translation and join the German translation.
        secondEditPage.cancel().openInformationDocExtraPane().clickTranslationLink(Locale.GERMAN);
        new ViewPage().editWYSIWYG();
        secondEditPage = new BlockNoteStandaloneEditPage();
        secondEditor = secondEditPage.getContentEditor();
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
        firstEditor = firstEditPage.getContentEditor();
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
        secondEditPage.cancel();
        setup.gotoPage(testReference, "edit", "language=fr_CA&editor=wysiwyg");
        secondEditPage = new BlockNoteStandaloneEditPage();
        secondEditor = secondEditPage.getContentEditor();
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
        BlockNoteInplaceEditPage firstEditPage = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteEditor firstEditor = firstEditPage.getContentEditor();
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
        // John, the content author, doesn't have script right, so superadmin is warned that editing the page would
        // allow the velocity macro to be executed.
        new ForceEditLockPage().clickForceEdit();
        BlockNoteStandaloneEditPage secondEditPage = new BlockNoteStandaloneEditPage();
        BlockNoteEditor secondEditor = secondEditPage.getContentEditor();
        BlockNoteRichTextArea secondTextArea = secondEditor.getRichTextArea();
        // Superadmin makes a change but John remains the effective author because the session script level cannot
        // increase.
        secondTextArea.waitUntilTextContains("two");
        secondTextArea.sendKeys(Keys.PAGE_UP, Keys.END, " three");

        // Save and check that the script macro is not executed because John is the effective author.
        ViewPage secondViewPage = secondEditPage.clickDone();
        String content = secondViewPage.getContent();
        assertTrue(content.contains("The execution of the [velocity] script macro is not allowed"),
            "Unexpected content: " + content);

        //
        // First Tab
        //

        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());
        firstTextArea.waitUntilTextContains("three");

        // Save and check that the script macro is not executed: John is still the effective author.
        content = firstEditPage.done().getContent();
        assertTrue(content.contains("The execution of the [velocity] script macro is not allowed"),
            "Unexpected content: " + content);

        //
        // Second Tab
        //

        multiUserSetup.switchToBrowserTab(secondTabHandle);
        // Start a new collaboration session.
        secondViewPage.editWYSIWYG();
        secondEditPage = new BlockNoteStandaloneEditPage();
        secondEditor = secondEditPage.getContentEditor();
        secondTextArea = secondEditor.getRichTextArea();
        // Superadmin makes a change and becomes the effective author of the new collaboration session.
        secondTextArea.sendKeys(Keys.PAGE_UP, Keys.END, " four");

        //
        // First Tab
        //

        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());
        // Join the new collaboration session.
        joinSession(firstEditPage, setup);
        firstEditor = firstEditPage.getContentEditor();
        firstTextArea = firstEditor.getRichTextArea();
        firstTextArea.waitUntilTextContains("four");

        // Save and check that the script macro is not executed: even though superadmin is the effective author, if the
        // user saving the page (John in this case) has less script rights, it becomes the effective script author for
        // that revision.
        content = firstEditPage.done().getContent();
        assertTrue(content.contains("The execution of the [velocity] script macro is not allowed"),
            "Unexpected content: " + content);

        //
        // Second Tab
        //

        multiUserSetup.switchToBrowserTab(secondTabHandle);
        secondTextArea.waitUntilFocused().sendKeys(" five");

        // Save (to check that the script macro is executed) but don't leave the edit mode yet. We want to keep the
        // collaboration session active for John to be able to rejoin.
        secondEditPage.save();

        //
        // First Tab
        //

        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());
        // Re-join the collaboration session. Superadmin's save made them the content author, so John is warned that
        // editing the page may break the velocity macro because he doesn't have script right.
        joinSessionConfirmingRequiredRightsWarning(firstEditPage);
        firstEditor = firstEditPage.getContentEditor();
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
        content = secondEditPage.cancel().getContent();
        assertTrue(content.contains("superadmin"), "Unexpected content: " + content);

        //
        // First Tab
        //

        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());
        // Save and check that the script macro is not executed since John is the effective author.
        content = firstEditPage.done().getContent();
        assertTrue(content.contains("The execution of the [velocity] script macro is not allowed"),
            "Unexpected content: " + content);
    }

    @Test
    @Order(7)
    void editAlone(TestUtils setup, TestReference testReference)
    {
        // Start fresh.
        setup.deletePage(testReference);

        // Create the page with the standalone editor.
        BlockNoteStandaloneEditPage editPage = BlockNoteStandaloneEditPage.gotoPage(testReference);
        RealtimeEditToolbar toolbar = editPage.getToolbar();

        // Verify that we're editing alone, in realtime.
        assertTrue(toolbar.isCollaborating());
        assertTrue(toolbar.isEditingAlone());

        // There are no versions yet.
        assertTrue(toolbar.getHistoryDropdown().open().getVersions().isEmpty());
        toolbar.getHistoryDropdown().close();

        // Verify the save status and the Save & Continue shortcut key.
        BlockNoteRichTextArea textArea = editPage.getContentEditor().getRichTextArea();
        textArea.click().sendKeys("one");
        toolbar.waitForSaveStatus(SaveStatus.UNSAVED);
        editPage.save();
        assertEquals(SaveStatus.SAVED, toolbar.getSaveStatus());

        // The created version is listed in the history dropdown, with its author.
        List<VersionElement> versions = toolbar.getHistoryDropdown().waitForVersion("1.1").open().getVersions();
        assertEquals(1, versions.size());
        assertEquals("1.1", versions.get(0).getNumber());
        assertEquals("Jo", versions.get(0).getAuthor().getAbbreviation());
        toolbar.getHistoryDropdown().close();

        // Verify the Done button.
        textArea.sendKeys(Keys.ENTER, "two");
        assertEquals("one\ntwo", editPage.clickDone().getContent());

        // Edit in-place: the realtime toolbar replaces the in-place edit buttons.
        BlockNoteInplaceEditPage inplaceEditablePage = new BlockNoteInplaceEditPage().editInplace();
        toolbar = inplaceEditablePage.getToolbar();
        assertTrue(toolbar.isEditingAlone());
        textArea = inplaceEditablePage.getContentEditor().getRichTextArea();
        textArea.appendParagraph("three").waitUntilTextContains("three");

        // Verify the Summarize & Done action.
        SummaryModal summaryModal = toolbar.clickSummarizeAndDone();
        summaryModal.setSummary("Summarize changes");
        summaryModal.clickSave(true);
        inplaceEditablePage.waitForView();
        assertEquals("one\ntwo\nthree", inplaceEditablePage.getContent());
        HistoryPane historyPane = inplaceEditablePage.openHistoryDocExtraPane().showMinorEdits();
        assertEquals(3, historyPane.getNumberOfVersions());
        assertEquals("Summarize changes", historyPane.getCurrentVersionComment());
    }

    @Test
    @Order(8)
    void editWithSelf(TestUtils setup, TestReference testReference, MultiUserTestUtils multiUserSetup)
    {
        //
        // First Tab
        //

        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "", "");

        BlockNoteInplaceEditPage firstEditPage = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteRichTextArea firstTextArea = firstEditPage.getContentEditor().getRichTextArea();
        String firstCoeditorId = firstEditPage.getToolbar().getUserId();

        //
        // Second Tab
        //

        String secondTabHandle = setup.getDriver().switchTo().newWindow(WindowType.TAB).getWindowHandle();
        BlockNoteStandaloneEditPage secondEditPage = BlockNoteStandaloneEditPage.gotoPage(testReference);
        BlockNoteRichTextArea secondTextArea = secondEditPage.getContentEditor().getRichTextArea();
        String secondCoeditorId = secondEditPage.getToolbar().getUserId();

        // The same user joined twice, so there are two coeditors with distinct identifiers.
        assertNotEquals(firstCoeditorId, secondCoeditorId);
        CoeditorElement self = secondEditPage.getToolbar().waitForCoeditor(firstCoeditorId);
        assertTrue(self.isDisplayed());
        assertEquals("xwiki:XWiki.John", self.getReference());
        assertTrue(setup.getURL(new DocumentReference("xwiki", "XWiki", "John")).endsWith(self.getURL()));
        assertEquals("John", self.getAvatarHint());
        assertEquals("Jo", self.getAbbreviation());
        assertEquals(2, secondEditPage.getToolbar().getVisibleCoeditorCount());

        secondTextArea.click().sendKeys("second");

        //
        // First Tab
        //

        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());
        self = firstEditPage.getToolbar().waitForCoeditor(secondCoeditorId);
        assertEquals("xwiki:XWiki.John", self.getReference());
        assertEquals("Jo", self.getAbbreviation());
        assertEquals(2, firstEditPage.getToolbar().getVisibleCoeditorCount());
        firstTextArea.waitUntilTextContains("second");

        //
        // Second Tab
        //

        // The coeditor list is updated when one of them leaves.
        multiUserSetup.switchToBrowserTab(secondTabHandle);
        assertEquals("second", secondEditPage.clickDone().getContent());

        //
        // First Tab
        //

        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());
        firstEditPage.getToolbar().waitUntilEditingAlone();
        assertEquals("second", firstEditPage.done().getContent());
    }

    @Test
    @Order(9)
    void preventEmptyRevisions(TestUtils setup, TestReference testReference, MultiUserTestUtils multiUserSetup)
    {
        //
        // First Tab
        //

        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "", "");

        // Edit the page in the first browser tab (in-place).
        BlockNoteInplaceEditPage firstEditPage = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteRichTextArea firstTextArea = firstEditPage.getContentEditor().getRichTextArea();
        HistoryDropdown historyDropdown = firstEditPage.getToolbar().getHistoryDropdown();

        // The history dropdown lists the initial version.
        List<VersionElement> versions = historyDropdown.open().getVersions();
        assertEquals(1, versions.size());
        assertEquals("1.1", versions.get(0).getNumber());
        assertEquals("Jo", versions.get(0).getAuthor().getAbbreviation());
        historyDropdown.close();

        // Save without making any changes. No new version should be created.
        firstEditPage.save();
        assertEquals(1, historyDropdown.open().getVersions().size());
        historyDropdown.close();

        //
        // Second Tab
        //

        // Join the editing session as another user (standalone).
        String secondTabHandle = multiUserSetup.openNewBrowserTab(XWIKI_ALIAS);
        loginAsAlice(setup);
        BlockNoteStandaloneEditPage secondEditPage = BlockNoteStandaloneEditPage.gotoPage(testReference);
        BlockNoteRichTextArea secondTextArea = secondEditPage.getContentEditor().getRichTextArea();
        secondTextArea.click().sendKeys("one");
        secondEditPage.save();

        // The new version is listed in the history dropdown, with its author.
        versions = secondEditPage.getToolbar().getHistoryDropdown().waitForVersion("1.2").open().getVersions();
        assertEquals(2, versions.size());
        assertEquals("1.2", versions.get(1).getNumber());
        assertEquals("Al", versions.get(1).getAuthor().getAbbreviation());
        secondEditPage.getToolbar().getHistoryDropdown().close();

        //
        // First Tab
        //

        // The new version is propagated to the other users, with its author.
        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());
        firstTextArea.waitUntilTextContains("one");
        versions = historyDropdown.waitForVersion("1.2").open().getVersions();
        assertEquals(2, versions.size());
        assertEquals("Al", versions.get(1).getAuthor().getAbbreviation());
        historyDropdown.close();

        // Leave the editing session without making any changes. No new version should be created.
        HistoryPane historyPane = firstEditPage.done().openHistoryDocExtraPane().showMinorEdits();
        assertEquals(2, historyPane.getNumberOfVersions());
        assertEquals("1.2", historyPane.getCurrentVersion());

        //
        // Second Tab
        //

        multiUserSetup.switchToBrowserTab(secondTabHandle);
        // Force an empty version by specifying a version summary.
        SummaryModal summaryModal = secondEditPage.getToolbar().getHistoryDropdown().open().summarizeChanges();
        summaryModal.setSummary("An empty version");
        summaryModal.clickSave(true);
        versions = secondEditPage.getToolbar().getHistoryDropdown().waitForVersion("1.3").open().getVersions();
        assertEquals(3, versions.size());
        assertEquals("1.3", versions.get(2).getNumber());
        secondEditPage.getToolbar().getHistoryDropdown().close();

        historyPane = secondEditPage.clickDone().openHistoryDocExtraPane().showMinorEdits();
        assertEquals(3, historyPane.getNumberOfVersions());
        assertEquals("An empty version", historyPane.getCurrentVersionComment());
    }

    @Test
    @Order(10)
    void saveAndViewNoMergeConflict(TestUtils setup, TestReference testReference,
        MultiUserTestUtils multiUserSetup)
    {
        //
        // First Tab
        //

        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "", "");

        // Edit the page in the first browser tab (standalone).
        BlockNoteStandaloneEditPage firstEditPage = BlockNoteStandaloneEditPage.gotoPage(testReference);
        BlockNoteRichTextArea firstTextArea = firstEditPage.getContentEditor().getRichTextArea();

        //
        // Second Tab
        //

        // Edit the page in the second browser tab (in-place).
        String secondTabHandle = setup.getDriver().switchTo().newWindow(WindowType.TAB).getWindowHandle();
        BlockNoteInplaceEditPage secondEditPage = BlockNoteInplaceEditPage.gotoPage(testReference).editInplace();
        BlockNoteRichTextArea secondTextArea = secondEditPage.getContentEditor().getRichTextArea();
        secondTextArea.click().sendKeys("second");
        secondEditPage.done();

        //
        // First Tab
        //

        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());
        firstTextArea.waitUntilTextContains("second");
        firstTextArea.click().sendKeys(Keys.HOME, "first ");
        // Use the shortcut key so that we don't lose the caret position.
        firstEditPage.save();

        //
        // Second Tab
        //

        // Edit again, after the first user saved.
        multiUserSetup.switchToBrowserTab(secondTabHandle);
        secondEditPage.editInplace();
        secondTextArea = secondEditPage.getContentEditor().getRichTextArea();
        // The caret of the first user is displayed where they typed.
        secondTextArea.waitUntilTextIs("first %ssecond", "John");

        //
        // First Tab
        //

        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());
        firstTextArea.waitUntilFocused().sendKeys("then ");
        firstEditPage.clickDone();

        //
        // Second Tab
        //

        // Save again, after the first user left.
        multiUserSetup.switchToBrowserTab(secondTabHandle);
        // The caret of the first user is removed when they leave.
        secondTextArea.waitUntilTextIs("first then second");
        secondTextArea.click().sendKeys(Keys.END, " done");
        assertEquals("first then second done", secondEditPage.done().getContent());
    }

    @Test
    @Order(11)
    void mandatoryVersionSummary(TestUtils setup, TestReference testReference) throws Exception
    {
        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "", "");

        // Make the version summary mandatory.
        setup.loginAsSuperAdmin();
        setup.setWikiPreference("editcomment", "1");
        setup.setWikiPreference("editcomment_mandatory", "1");
        try {
            loginAsJohn(setup);
            BlockNoteInplaceEditPage editPage = BlockNoteInplaceEditPage.gotoPage(testReference).editInplace();
            RealtimeEditToolbar toolbar = editPage.getToolbar();

            // The Done button opens the summary modal so the "Summarize & Done" action is not available.
            assertFalse(toolbar.hasSummarizeAndDoneAction());
            assertTrue(toolbar.getHistoryDropdown().hasSummarizeChangesAction());

            BlockNoteRichTextArea textArea = editPage.getContentEditor().getRichTextArea();
            textArea.click().sendKeys("one");

            // The version summary is required.
            toolbar.clickDone();
            SummaryModal summaryModal = new SummaryModal();
            summaryModal.clickSave(false);
            summaryModal.waitUntilSummaryIsFocused();
            assertFalse(summaryModal.isSummaryValid());

            summaryModal.setSummary("first version summary");
            summaryModal.clickSave(true);
            editPage.waitForView();
            assertEquals("one", editPage.getContent());

            HistoryPane historyPane = editPage.openHistoryDocExtraPane().showMinorEdits();
            assertEquals(2, historyPane.getNumberOfVersions());
            assertEquals("first version summary", historyPane.getCurrentVersionComment());
        } finally {
            setup.loginAsSuperAdmin();
            setup.setWikiPreference("editcomment_mandatory", "0");
            loginAsJohn(setup);
        }
    }

    @Test
    @Order(12)
    void editWhileCollaborationServerIsUnreachable(TestUtils setup, TestReference testReference,
        MultiUserTestUtils multiUserSetup)
    {
        //
        // First Tab
        //

        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "one", "");

        // The editor is loaded even if it can't connect to the collaboration server.
        BlockNoteInplaceEditPage firstEditPage = new BlockNoteInplaceEditPage();
        firstEditPage.simulateUnreachableCollaborationServer().edit();
        firstEditPage.waitForInplaceEditor(false);
        RealtimeEditToolbar firstToolbar = firstEditPage.getToolbar();
        assertFalse(firstToolbar.isConnected());
        // The auto-save is started even if we're disconnected.
        firstToolbar.waitForSaveStatus(SaveStatus.SAVED);
        // We're identified in the realtime editing session even before we connect to it.
        String firstCoeditorId = firstToolbar.getUserId();

        // The user can edit and save while disconnected.
        BlockNoteRichTextArea firstTextArea = firstEditPage.getContentEditor().getRichTextArea();
        firstTextArea.appendParagraph("two").waitUntilTextContains("two");
        firstToolbar.waitForSaveStatus(SaveStatus.UNSAVED);
        firstEditPage.save();
        assertEquals(SaveStatus.SAVED, firstToolbar.getSaveStatus());
        assertFalse(firstToolbar.isConnected());

        // The created version is listed in the history dropdown.
        List<VersionElement> versions =
            firstToolbar.getHistoryDropdown().waitForVersion("1.2").open().getVersions();
        assertEquals(2, versions.size());
        assertEquals("1.2", versions.get(1).getNumber());
        firstToolbar.getHistoryDropdown().close();

        // The editor connects to the realtime editing session as soon as the collaboration server is reachable, and
        // the changes made while disconnected are shared.
        firstEditPage.restoreCollaborationServer();
        firstToolbar.waitUntilEditingAlone();
        firstTextArea.appendParagraph("three").waitUntilTextContains("three");

        //
        // Second Tab
        //

        String secondTabHandle = multiUserSetup.openNewBrowserTab(XWIKI_ALIAS);
        loginAsAlice(setup);
        BlockNoteInplaceEditPage secondEditPage = BlockNoteInplaceEditPage.gotoPage(testReference);
        joinSession(secondEditPage, setup);
        BlockNoteRichTextArea secondTextArea = secondEditPage.getContentEditor().getRichTextArea();
        secondTextArea.waitUntilTextContains("three");
        RealtimeEditToolbar secondToolbar = secondEditPage.getToolbar();
        String secondCoeditorId = secondToolbar.getUserId();
        // The user who edited while disconnected is listed among the coeditors.
        secondToolbar.waitForCoeditor(firstCoeditorId);
        assertEquals(2, secondToolbar.getVisibleCoeditorCount());
        secondTextArea.appendParagraph("four").waitUntilTextContains("four");

        //
        // First Tab
        //

        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());
        firstTextArea.waitUntilTextContains("four");
        firstToolbar.waitForCoeditor(secondCoeditorId);
        assertEquals(2, firstToolbar.getVisibleCoeditorCount());
        assertEquals("one\ntwo\nthree\nfour", firstEditPage.done().getContent());

        //
        // Second Tab
        //

        multiUserSetup.switchToBrowserTab(secondTabHandle);
        secondToolbar.waitUntilEditingAlone();
        assertEquals("one\ntwo\nthree\nfour", secondEditPage.done().getContent());
    }

    @Test
    @Order(13)
    void macroOutputIsNotShared(TestUtils setup, TestReference testReference, MultiUserTestUtils multiUserSetup)
    {
        //
        // First Tab
        //

        // Start fresh. The macro output depends on the current user, and it's not supposed to be shared with the other
        // users editing the page.
        setup.loginAsSuperAdmin();
        setup.deletePage(testReference);
        setup.createPage(testReference, """
            Hello {{velocity}}$xcontext.userReference.name{{/velocity}}!

            {{velocity}}
            Welcome $xcontext.userReference.name
            {{/velocity}}
            """, "");

        // Edit the page in-place as superadmin, who starts the collaboration session.
        BlockNoteInplaceEditPage firstEditPage = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteRichTextArea firstTextArea = firstEditPage.getContentEditor().getRichTextArea();
        firstTextArea.waitUntilTextContains("Welcome superadmin");
        assertThat(firstTextArea.getText(), containsString("Hello superadmin!"));

        //
        // Second Tab
        //

        // Join the collaboration session as John. The superadmin content author has script right, but John doesn't.
        String secondTabHandle = multiUserSetup.openNewBrowserTab(XWIKI_ALIAS);
        loginAsJohn(setup);
        BlockNoteInplaceEditPage secondEditPage = BlockNoteInplaceEditPage.gotoPage(testReference);
        joinSessionConfirmingRequiredRightsWarning(secondEditPage);
        BlockNoteRichTextArea secondTextArea = secondEditPage.getContentEditor().getRichTextArea();
        // John gets the macro calls but not their output (which was rendered for superadmin).
        secondTextArea.waitUntilTextContains("Hello macro:velocity!");
        String secondText = secondTextArea.getText();
        assertEquals(2, StringUtils.countMatches(secondText, "macro:velocity"), "Unexpected content: " + secondText);
        assertThat(secondText, not(containsString("superadmin")));

        //
        // First Tab
        //

        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());
        // Superadmin keeps seeing the macro output while editing.
        firstTextArea.appendParagraph("end").waitUntilTextContains("end");
        String firstText = firstTextArea.getText();
        assertThat(firstText, containsString("Hello superadmin!"));
        assertThat(firstText, containsString("Welcome superadmin"));

        //
        // Second Tab
        //

        multiUserSetup.switchToBrowserTab(secondTabHandle);
        secondTextArea.waitUntilTextContains("end");
        assertThat(secondTextArea.getText(), not(containsString("superadmin")));
        // Leave without making changes, so that superadmin remains the effective author of the collaboration session.
        secondEditPage.cancel();

        //
        // First Tab
        //

        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());
        firstEditPage.getToolbar().waitUntilEditingAlone();
        // The macro calls are saved even though the macro output is not part of the edited content.
        assertEquals("Hello superadmin!\nWelcome superadmin\nend", firstEditPage.done().getContent());
    }

    /**
     * Joins an existing collaboration session, checking that the user is not asked to confirm that they want to take
     * over the edit lock held by the users that are already in the session.
     *
     * @param inplaceEditablePage the page to edit in place
     * @param setup the test setup
     */
    private void joinSession(BlockNoteInplaceEditPage inplaceEditablePage, TestUtils setup)
    {
        inplaceEditablePage.edit();
        // If the user was asked to confirm that they want to take over the edit lock then the in-place editor
        // wouldn't be loaded and this would time out.
        inplaceEditablePage.waitForInplaceEditor();
        assertTrue(setup.getDriver().findElementsWithoutWaiting(By.className("force-edit-lock-modal")).isEmpty(),
            "The user was asked to force the edit lock instead of joining the collaboration session.");
    }

    /**
     * Joins the collaboration session after confirming the required rights warning that is displayed when the current
     * user doesn't have a right that the content author of the edited page has.
     *
     * @param inplaceEditablePage the page to edit in-place
     */
    private void joinSessionConfirmingRequiredRightsWarning(BlockNoteInplaceEditPage inplaceEditablePage)
    {
        inplaceEditablePage.edit();
        ForceEditLockModal requiredRightsWarning = new ForceEditLockModal();
        String message = requiredRightsWarning.getMessage();
        // Being asked to take over the edit lock would mean that the user doesn't join the collaboration session, so
        // the warning must only be about the missing rights.
        assertTrue(message.contains("you are missing the following rights"), "Unexpected warning: " + message);
        assertFalse(message.contains("locked"), "Unexpected warning: " + message);
        requiredRightsWarning.clickOk();
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
