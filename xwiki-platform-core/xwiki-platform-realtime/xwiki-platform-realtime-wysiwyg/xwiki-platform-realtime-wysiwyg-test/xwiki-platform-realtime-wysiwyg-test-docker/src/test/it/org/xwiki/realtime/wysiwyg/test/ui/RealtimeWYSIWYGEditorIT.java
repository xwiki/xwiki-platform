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
package org.xwiki.realtime.wysiwyg.test.ui;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Keys;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.administration.test.po.LocalizationAdministrationSectionPage;
import org.xwiki.ckeditor.test.po.AutocompleteDropdown;
import org.xwiki.ckeditor.test.po.CKEditor;
import org.xwiki.ckeditor.test.po.CKEditorToolBar;
import org.xwiki.ckeditor.test.po.MacroDialogEditModal;
import org.xwiki.ckeditor.test.po.RichTextAreaElement;
import org.xwiki.ckeditor.test.po.image.ImageDialogEditModal;
import org.xwiki.ckeditor.test.po.image.ImageDialogSelectModal;
import org.xwiki.edit.test.po.InplaceEditablePage;
import org.xwiki.flamingo.skin.test.po.EditConflictModal;
import org.xwiki.flamingo.skin.test.po.EditConflictModal.ConflictChoice;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.panels.test.po.DocumentInformationPanel;
import org.xwiki.realtime.test.RealtimeTestUtils;
import org.xwiki.realtime.test.po.CoeditorElement;
import org.xwiki.realtime.test.po.HistoryDropdown;
import org.xwiki.realtime.test.po.RealtimeEditToolbar;
import org.xwiki.realtime.test.po.RealtimeInplaceEditablePage;
import org.xwiki.realtime.test.po.SaveStatus;
import org.xwiki.realtime.test.po.SummaryModal;
import org.xwiki.realtime.test.po.VersionElement;
import org.xwiki.realtime.wysiwyg.test.po.RealtimeCKEditor;
import org.xwiki.realtime.wysiwyg.test.po.RealtimeRichTextAreaElement;
import org.xwiki.realtime.wysiwyg.test.po.RealtimeRichTextAreaElement.CoeditorPosition;
import org.xwiki.realtime.wysiwyg.test.po.RealtimeWYSIWYGEditPage;
import org.xwiki.test.docker.junit5.MultiUserTestUtils;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.DocumentSyntaxPicker;
import org.xwiki.test.ui.po.DocumentSyntaxPicker.SyntaxConversionConfirmationModal;
import org.xwiki.test.ui.po.HistoryPane;
import org.xwiki.test.ui.po.InformationPane;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import com.mchange.io.FileUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Functional tests for the real-time WYSIWYG editor.
 * 
 * @version $Id$
 * @since 15.5.4
 * @since 15.9
 */
@UITest(
    properties = {
        "xwikiDbHbmCommonExtraMappings=notification-filter-preferences.hbm.xml",
        // Required to upload files during the real-time editing session.
        "xwikiCfgPlugins=com.xpn.xwiki.plugin.fileupload.FileUploadPlugin"
    },
    extraJARs = {
        // The WebSocket end-point implementation based on XWiki components needs to be installed as core extension.
        "org.xwiki.platform:xwiki-platform-websocket",

        // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus
        // we need to provide the JAR inside WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-8271
        "org.xwiki.platform:xwiki-platform-notifications-filters-default",

        // The macro service uses the extension index script service to get the list of uninstalled macros (from
        // extensions) which expects an implementation of the extension index. The extension index script service is a
        // core extension so we need to make the extension index also core.
        "org.xwiki.platform:xwiki-platform-extension-index",

        // Solr search is used to get suggestions for the link quick action.
        "org.xwiki.platform:xwiki-platform-search-solr-query"
    },
    servletEngineNetworkAliases = RealtimeWYSIWYGEditorIT.XWIKI_ALIAS
)
class RealtimeWYSIWYGEditorIT extends AbstractRealtimeWYSIWYGEditorIT
{
    public static final String XWIKI_ALIAS = "xwiki-alias";

    @Test
    @Order(1)
    void editAlone(TestReference testReference, TestUtils setup)
    {
        // Start fresh.
        setup.deletePage(testReference);

        RealtimeWYSIWYGEditPage editPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor editor = editPage.getContenEditor();

        // Verify that we're connected to the realtime editing session.
        assertTrue(editPage.getToolbar().isCollaborating());

        // Verify that the Preview button is hidden.
        assertFalse(editPage.hasPreviewButton());

        // The Autosave checkbox is also hidden because autosave is done by the realtime editor (you can't disable it
        // while editing in realtime).
        assertFalse(editPage.getAutoSaveCheckbox().isDisplayed());

        // Verify that we're editing alone.
        assertTrue(editPage.getToolbar().isEditingAlone());

        // The Source button is now available.
        assertTrue(editor.getToolBar().canToggleSourceMode());

        RealtimeRichTextAreaElement textArea = editor.getRichTextArea();
        textArea.sendKeys("one");

        // Verify the cursor indicator on the left of the editing area.
        CoeditorPosition selfPosition = textArea.getCoeditorPosition(editPage.getToolbar().getUserId());
        assertEquals("John", selfPosition.getAvatarHint());
        assertTrue(selfPosition.getAvatarURL().contains("noavatar.png"),
            "Unexpected avatar URL: " + selfPosition.getAvatarURL());
        selfPosition.waitForLocation(new Point(4, 17));

        assertEquals(1, textArea.getCoeditorPositions().size());

        // Verify that the cursor indicator is updated when typing.
        textArea.sendKeys(Keys.ENTER, "two");
        selfPosition.waitForLocation(new Point(4, 47));

        // Verify save and cancel.
        editPage.getToolbar().sendSaveShortcutKey();
        textArea.sendKeys(Keys.ENTER, "three");
        setup.leaveEditMode();
        RealtimeInplaceEditablePage inplaceEditablePage = new RealtimeInplaceEditablePage();
        assertEquals("one\ntwo", inplaceEditablePage.getContent());

        // Edit again and verify the Save and View button.
        inplaceEditablePage.editInplace();
        new RealtimeCKEditor().getRichTextArea().sendKeys(Keys.ARROW_DOWN, Keys.END, Keys.ENTER, "three");
        inplaceEditablePage.done();
        assertEquals("one\ntwo\nthree", inplaceEditablePage.getContent());

        // Edit again to verify the autosave.
        inplaceEditablePage.editInplace();
        editor = new RealtimeCKEditor();
        textArea = editor.getRichTextArea();
        textArea.sendKeys(Keys.chord(Keys.SHIFT, Keys.END));
        textArea.sendKeys("zero");

        // Wait for auto-save.
        inplaceEditablePage.getToolbar().waitForSaveStatus(SaveStatus.UNSAVED);
        inplaceEditablePage.getToolbar().waitForSaveStatus(SaveStatus.SAVED);

        setup.leaveEditMode();
        assertEquals("zero\ntwo\nthree", inplaceEditablePage.getContent());

        // edit again and test the summarize & done
        inplaceEditablePage.editInplace();
        new RealtimeCKEditor().getRichTextArea().sendKeys(
            Keys.ARROW_DOWN,
            Keys.ARROW_DOWN,
            Keys.END,
            Keys.ENTER,
            "four");
        SummaryModal summaryModal = inplaceEditablePage.getToolbar().clickSummarizeAndDone();
        summaryModal.setSummary("Summarize changes");
        summaryModal.clickSave(true);
        inplaceEditablePage.waitForView();
        assertEquals("zero\ntwo\nthree\nfour", inplaceEditablePage.getContent());
        HistoryPane historyPane = inplaceEditablePage.openHistoryDocExtraPane().showMinorEdits();
        assertEquals(4, historyPane.getNumberOfVersions());
        assertEquals("Summarize changes", historyPane.getCurrentVersionComment());

        // Delete the page to test creation with summarize & done (regression test for XWIKI-23136).
        setup.deletePage(testReference);
        editPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        editor = editPage.getContenEditor();
        textArea = editor.getRichTextArea();
        textArea.sendKeys("New content");
        new WikiEditPage().setTitle("Dummy title");
        summaryModal = editPage.getToolbar().clickSummarizeAndDone();
        summaryModal.setSummary("Summarize changes 2");

        // Since it's a page creation, we're not in inplace editable page and so we'll have a reload.
        setup.getDriver().addPageNotYetReloadedMarker();
        summaryModal.clickSave(false);
        setup.getDriver().waitUntilPageIsReloaded();
        ViewPage viewPage = new ViewPage();
        assertEquals("New content", viewPage.getContent());
        assertEquals("Dummy title", viewPage.getDocumentTitle());
        historyPane = inplaceEditablePage.openHistoryDocExtraPane();
        assertEquals("Summarize changes 2", historyPane.getCurrentVersionComment());
    }

    @Test
    @Order(2)
    void editWithSelf(TestReference testReference, TestUtils setup, MultiUserTestUtils multiUserSetup)
    {
        //
        // First Tab
        //

        // Start fresh.
        setup.deletePage(testReference);

        // Edit the page in the first browser tab.
        RealtimeWYSIWYGEditPage firstEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor firstEditor = firstEditPage.getContenEditor();
        RealtimeRichTextAreaElement firstTextArea = firstEditor.getRichTextArea();
        String firstCoeditorId = firstEditPage.getToolbar().getUserId();

        // Focus the editing area to make sure the caret indicator is displayed.
        firstTextArea.click();

        //
        // Second Tab
        //

        String secondTabHandle = setup.getDriver().switchTo().newWindow(WindowType.TAB).getWindowHandle();

        // Edit the page in the second browser tab.
        RealtimeWYSIWYGEditPage secondEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor secondEditor = secondEditPage.getContenEditor();
        RealtimeRichTextAreaElement secondTextArea = secondEditor.getRichTextArea();
        String secondCoeditorId = secondEditPage.getToolbar().getUserId();

        // Verify the list of coeditors.
        CoeditorElement self = secondEditPage.getToolbar().waitForCoeditor(firstCoeditorId);
        assertTrue(self.isDisplayed());
        // The name is not visible when the user is displayed directly on the toolbar.
        assertEquals("", self.getName());
        assertEquals("xwiki:XWiki.John", self.getReference());
        assertTrue(setup.getURL(new DocumentReference("xwiki", "XWiki", "John")).endsWith(self.getURL()));
        assertTrue(self.getAvatarURL().contains("noavatar.png"), "Unexpected avatar URL: " + self.getAvatarURL());
        assertEquals("John", self.getAvatarHint());
        assertEquals("Jo", self.getAbbreviation());
        assertEquals(2, secondEditPage.getToolbar().getVisibleCoeditors().size());

        // Verify the placeholder text is present, because the content is empty.
        secondTextArea.waitForPlaceholder("Start typing here...");

        // Focus the editing area to make sure the caret indicator is displayed.
        secondTextArea.click();

        //
        // First Tab
        //

        // Switch back to the first tab and verify the list of coeditors.
        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
        self = firstEditPage.getToolbar().waitForCoeditor(secondCoeditorId);
        assertTrue(self.isDisplayed());
        // The name is not visible when the user is displayed directly on the toolbar.
        assertEquals("", self.getName());
        assertEquals("xwiki:XWiki.John", self.getReference());
        assertTrue(setup.getURL(new DocumentReference("xwiki", "XWiki", "John")).endsWith(self.getURL()));
        assertTrue(self.getAvatarURL().contains("noavatar.png"), "Unexpected avatar URL: " + self.getAvatarURL());
        assertEquals("John", self.getAvatarHint());
        assertEquals("Jo", self.getAbbreviation());
        assertEquals(2, firstEditPage.getToolbar().getVisibleCoeditors().size());

        // Type in the first tab to see that it gets propagated to the second tab.
        firstTextArea.sendKeys("one", Keys.ENTER, "two", Keys.ENTER, "three");

        //
        // Second Tab
        //

        // Switch to the second tab and verify the content.
        setup.getDriver().switchTo().window(secondTabHandle);
        secondTextArea.waitUntilTextContains("three");

        // There should be no placeholder text anymore because the content is not empty.
        secondTextArea.waitForPlaceholder(null);
        assertEquals("one\ntwo\nthree", secondTextArea.getText());

        secondTextArea.sendKeys(Keys.chord(Keys.SHIFT, Keys.END));
        secondTextArea.sendKeys("zero");

        // Verify the caret indicators on the left of the editing area.

        // The first user is on the third line (paragraph).
        CoeditorPosition firstPosition =
            secondTextArea.getCoeditorPosition(firstCoeditorId).waitForLocation(new Point(4, 77));
        assertEquals("John", firstPosition.getAvatarHint());
        assertTrue(firstPosition.getAvatarURL().contains("noavatar.png"),
            "Unexpected avatar URL: " + firstPosition.getAvatarURL());

        // The second user is on the first line (paragraph).
        CoeditorPosition secondPosition =
            secondTextArea.getCoeditorPosition(secondCoeditorId).waitForLocation(new Point(4, 17));
        assertEquals("John", secondPosition.getAvatarHint());
        assertTrue(secondPosition.getAvatarURL().contains("noavatar.png"),
            "Unexpected avatar URL: " + secondPosition.getAvatarURL());

        assertEquals(2, secondTextArea.getCoeditorPositions().size());

        // Verify that the caret indicator is updated on the first editor.
        secondTextArea.sendKeys(Keys.ARROW_DOWN, Keys.HOME);

        //
        // First Tab
        //

        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
        firstTextArea.getCoeditorPosition(secondCoeditorId).waitForLocation(new Point(4, 47));

        // Verify that clicking on the coeditor indicator scrolls the editing area to the coeditor position.
        // But first we need to add enough paragraphs to make the editing area scrollable.
        for (int i = 0; i < 20; i++) {
            firstTextArea.sendKeys(Keys.ENTER);
        }
        firstTextArea.sendKeys("end");

        //
        // Second Tab
        //

        // Switch to the second tab and click on the coeditor indicator.
        setup.getDriver().switchTo().window(secondTabHandle);
        secondTextArea.waitUntilTextContains("end");
        firstPosition = secondTextArea.getCoeditorPosition(firstCoeditorId).waitForLocation(new Point(4, 17 + 22 * 30));
        assertFalse(firstPosition.isVisible(), "The coeditor position is visible before scrolling.");
        secondEditPage.getToolbar().waitForCoeditor(firstCoeditorId).click();
        assertTrue(firstPosition.isVisible(), "The coeditor position is not visible after scrolling.");
    }

    @Test
    @Order(3)
    void inplaceEditableMacro(TestReference testReference, TestUtils setup, MultiUserTestUtils multiUserSetup)
    {
        //
        // First Tab
        //

        // Start fresh.
        setup.deletePage(testReference);

        RealtimeWYSIWYGEditPage firstEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor firstEditor = firstEditPage.getContenEditor();
        RealtimeRichTextAreaElement firstTextArea = firstEditor.getRichTextArea();

        firstTextArea.sendKeys(Keys.ENTER, "one");

        //
        // Second Tab
        //

        String secondTabHandle = setup.getDriver().switchTo().newWindow(WindowType.TAB).getWindowHandle();

        RealtimeWYSIWYGEditPage secondEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor secondEditor = secondEditPage.getContenEditor();
        RealtimeRichTextAreaElement secondTextArea = secondEditor.getRichTextArea();

        secondTextArea.waitUntilTextContains("one");
        secondTextArea.sendKeys("/info");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/info", "Info Box");
        secondTextArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();
        secondTextArea.waitForContentRefresh();

        // Replace the default message text.
        secondTextArea.sendKeys(Keys.chord(Keys.SHIFT, Keys.END), Keys.BACK_SPACE);
        secondTextArea.sendKeys("my info");

        //
        // First Tab
        //

        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
        // Continue typing to verify that the selection is not lost in the second tab. Wait for the inserted macro to be
        // rendered server-side.
        firstTextArea.waitUntilTextContains("my");
        firstTextArea.waitForContentRefresh();
        firstTextArea.sendKeys(" two");

        //
        // Second Tab
        //

        setup.getDriver().switchTo().window(secondTabHandle);
        // Don't wait for content from the first tab because we want to check that the selection is preserved.
        secondTextArea.sendKeys(" tex");
        secondTextArea.sendKeys(Keys.chord(Keys.SHIFT, Keys.ARROW_LEFT, Keys.ARROW_LEFT, Keys.ARROW_LEFT));

        //
        // First Tab
        //

        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
        // Don't wait for content from the first tab because we want to check that the selection is preserved.
        firstTextArea.sendKeys(" three");

        //
        // Second Tab
        //

        setup.getDriver().switchTo().window(secondTabHandle);
        // Again, we don't wait for the content to be updated, because we don't need to. The content should be
        // synchronized while we are typing.
        secondTextArea.sendKeys("message");

        // Wait for the content to be synchronized before saving, otherwise we might save partial content and, more
        // importantly, we could trigger the leave confirmation (if the content is synchronized after the content dirty
        // flag is set to false by the action buttons listener).
        secondTextArea.waitUntilTextContains("three");

        // Save and check the result.
        ViewPage viewPage = secondEditPage.clickDone();
        assertEquals("Information\nmy info message\none two three", viewPage.getContent());
    }

    @Test
    @Order(4)
    void editDifferentParagraphs(TestReference testReference, TestUtils setup, MultiUserTestUtils multiUserSetup)
    {
        //
        // First Tab
        //

        // Start fresh.
        setup.deletePage(testReference);

        RealtimeWYSIWYGEditPage firstEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor firstEditor = firstEditPage.getContenEditor();
        RealtimeRichTextAreaElement firstTextArea = firstEditor.getRichTextArea();
        firstTextArea.sendKeys("Start.");

        //
        // Second Tab
        //

        String secondTabHandle = setup.getDriver().switchTo().newWindow(WindowType.TAB).getWindowHandle();

        RealtimeWYSIWYGEditPage secondEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor secondEditor = secondEditPage.getContenEditor();
        RealtimeRichTextAreaElement secondTextArea = secondEditor.getRichTextArea();

        // Each user types in their own paragraph.
        secondTextArea.waitUntilTextContains("Start.");
        secondTextArea.sendKeys(Keys.END, Keys.ENTER);

        String firstUserText =
            "The five boxing wizards jump quickly. The quick brown fox jumps over the lazy dog. First";
        String secondUserText =
            "The quick brown fox jumps over the lazy dog. The five boxing wizards jump quickly. Second";

        String[] firstUserWords = firstUserText.split(" ");
        String[] secondUserWords = secondUserText.split(" ");

        for (int i = 0; i < Math.min(firstUserWords.length, secondUserWords.length); i++) {
            //
            // First Tab
            //
            setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
            firstTextArea.sendKeys(" " + firstUserWords[i]);

            //
            // Second Tab
            //
            setup.getDriver().switchTo().window(secondTabHandle);
            secondTextArea.sendKeys(secondUserWords[i] + " ");
        }

        secondTextArea.sendKeys("End.");

        // Wait to receive all the content typed by the first user.
        secondTextArea.waitUntilTextContains("First");

        ViewPage viewPage = secondEditPage.clickDone();
        assertEquals("Start. " + firstUserText + "\n" + secondUserText + " End.", viewPage.getContent());
    }

    @Test
    @Order(5)
    void editSameParagraph(TestReference testReference, TestUtils setup, MultiUserTestUtils multiUserSetup)
    {
        //
        // First Tab
        //

        // Start fresh.
        setup.deletePage(testReference);

        RealtimeWYSIWYGEditPage firstEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor firstEditor = firstEditPage.getContenEditor();
        RealtimeRichTextAreaElement firstTextArea = firstEditor.getRichTextArea();
        firstTextArea.sendKeys("Separator.", Keys.HOME);

        //
        // Second Tab
        //

        String secondTabHandle = setup.getDriver().switchTo().newWindow(WindowType.TAB).getWindowHandle();

        RealtimeWYSIWYGEditPage secondEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor secondEditor = secondEditPage.getContenEditor();
        RealtimeRichTextAreaElement secondTextArea = secondEditor.getRichTextArea();

        // Each user types in their own paragraph.
        secondTextArea.waitUntilTextContains("Separator.");
        secondTextArea.sendKeys(Keys.END);

        String text = "The quick brown fox jumps over the lazy dog.";
        String[] words = text.split(" ");

        for (int i = 0; i < words.length; i++) {
            //
            // First Tab
            //
            setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
            firstTextArea.sendKeys(words[i] + " ");

            //
            // Second Tab
            //
            setup.getDriver().switchTo().window(secondTabHandle);
            secondTextArea.sendKeys(" " + words[i]);
        }

        //
        // First Tab
        //
        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
        firstTextArea.sendKeys("First. ");

        //
        // Second Tab
        //
        setup.getDriver().switchTo().window(secondTabHandle);
        // Wait to receive all the content typed by the first user.
        secondTextArea.waitUntilTextContains("First.");

        ViewPage viewPage = secondEditPage.clickDone();
        assertEquals(text + " First. Separator. " + text, viewPage.getContent());
    }

    @Test
    @Order(6)
    void applyInlineStylesOnTheSameParagraph(TestReference testReference, TestUtils setup,
        MultiUserTestUtils multiUserSetup)
    {
        //
        // First Tab
        //

        // Start fresh.
        setup.deletePage(testReference);

        RealtimeWYSIWYGEditPage firstEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor firstEditor = firstEditPage.getContenEditor();
        RealtimeRichTextAreaElement firstTextArea = firstEditor.getRichTextArea();
        firstTextArea.sendKeys("bold italic");

        //
        // Second Tab
        //

        String secondTabHandle = setup.getDriver().switchTo().newWindow(WindowType.TAB).getWindowHandle();

        RealtimeWYSIWYGEditPage secondEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor secondEditor = secondEditPage.getContenEditor();
        RealtimeRichTextAreaElement secondTextArea = secondEditor.getRichTextArea();

        secondTextArea.waitUntilTextContains("italic");
        secondTextArea.sendKeys(Keys.END, " underline");
        // Select the "underline" word.
        secondTextArea.sendKeys(Keys.chord(Keys.SHIFT, Keys.CONTROL, Keys.ARROW_LEFT));

        //
        // First Tab
        //

        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());

        // If we don't wait then the italic style might be applied before the "underline" word is retrieved, which leads
        // to the "underline" word being inserted inside the italic style.
        firstTextArea.waitUntilTextContains("underline");

        // Select the "italic" word and apply the italic style.
        firstTextArea.sendKeys(Keys.chord(Keys.SHIFT, Keys.CONTROL, Keys.ARROW_LEFT));
        firstTextArea.sendKeys(Keys.chord(Keys.CONTROL, "i"));

        //
        // Second Tab
        //

        setup.getDriver().switchTo().window(secondTabHandle);
        // Apply the underline style.
        secondTextArea.sendKeys(Keys.chord(Keys.CONTROL, "u"));

        //
        // First Tab
        //

        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
        // Select the "bold" word and apply the bold style.
        firstTextArea.sendKeys(Keys.ARROW_LEFT, Keys.ARROW_LEFT);
        firstTextArea.sendKeys(Keys.chord(Keys.SHIFT, Keys.CONTROL, Keys.ARROW_LEFT));
        firstTextArea.sendKeys(Keys.chord(Keys.CONTROL, "b"));

        //
        // Second Tab
        //

        setup.getDriver().switchTo().window(secondTabHandle);
        secondTextArea.sendKeys(Keys.ARROW_RIGHT);
        secondTextArea.sendKeys(Keys.chord(Keys.CONTROL, "u"));
        secondTextArea.sendKeys(" end");

        //
        // First Tab
        //

        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
        firstTextArea.sendKeys(Keys.ARROW_RIGHT, "er");

        firstTextArea.waitUntilTextContains("end");
        // Normalize the spaces because not all browsers behave the same (where some browser inserts a space another may
        // insert a non-breaking space).
        String content = firstTextArea.getContent().replace("&nbsp;", " ");
        assertTrue(content.contains("<strong>bolder</strong> <em>italic</em> <ins>underline</ins> end"),
            "Unexpected content: " + content);
    }

    @Test
    @Order(7)
    void imageWithCaption(TestReference testReference, TestUtils setup, MultiUserTestUtils multiUserSetup)
        throws Exception
    {
        // Start fresh.
        setup.deletePage(testReference);
        // Create the page with the current user so the current user can delete it when running the test multiple times.
        setup.createPage(testReference, "");

        //
        // First Tab
        //

        RealtimeWYSIWYGEditPage firstEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor firstEditor = firstEditPage.getContenEditor();
        RealtimeRichTextAreaElement firstTextArea = firstEditor.getRichTextArea();
        firstTextArea.sendKeys("before");

        //
        // Second Tab
        //

        String secondTabHandle = multiUserSetup.openNewBrowserTab(XWIKI_ALIAS);
        loginAsBob(setup);

        RealtimeWYSIWYGEditPage secondEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor secondEditor = secondEditPage.getContenEditor();
        RealtimeRichTextAreaElement secondTextArea = secondEditor.getRichTextArea();
        secondTextArea.waitUntilTextContains("before");
        secondTextArea.sendKeys(Keys.END, Keys.ENTER);

        // Insert the image with caption.
        ImageDialogSelectModal imageDialogSelectModal = secondEditor.getToolBar().insertImage();
        imageDialogSelectModal.switchToUploadTab().upload("/image.gif");
        ImageDialogEditModal imageDialogEditModal = imageDialogSelectModal.clickSelect();
        imageDialogEditModal.switchToStandardTab().clickCaptionCheckbox();
        imageDialogEditModal.clickInsert();

        // Verify the image is uploaded properly
        File file = setup.getResourceFile("/image.gif");
        byte[] uploadedAttachmentContent = setup.rest()
            .getAttachmentAsByteArray(new EntityReference("image.gif", EntityType.ATTACHMENT, testReference));
        assertTrue(Arrays.equals(FileUtils.getBytes(file), uploadedAttachmentContent));

        // Focus the caption and edit it.
        secondTextArea.sendKeys(Keys.ARROW_DOWN, Keys.ARROW_UP);
        secondTextArea.sendKeys(Keys.chord(Keys.SHIFT, Keys.END));
        secondTextArea.sendKeys("Tree");

        //
        // First Tab
        //

        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());
        firstTextArea.waitUntilTextContains("Tree");
        firstTextArea.sendKeys(Keys.ARROW_DOWN, Keys.HOME, "Small ");

        // Verify that the image uploaded from the second tab is visible in the first tab.
        firstTextArea.verifyContent(content -> {
            Dimension imageSize = content.getImages().get(0).getSize();
            assertEquals(20, imageSize.width);
            assertEquals(20, imageSize.height);
        });

        //
        // Second Tab
        //

        multiUserSetup.switchToBrowserTab(secondTabHandle);
        secondTextArea.waitUntilTextContains("Small");
        secondTextArea.sendKeys(Keys.chord(Keys.CONTROL, Keys.SHIFT, Keys.ARROW_LEFT));
        secondTextArea.sendKeys(Keys.chord(Keys.CONTROL, "b"));

        //
        // First Tab
        //

        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());
        firstTextArea.sendKeys(Keys.ARROW_LEFT, "est");
        firstTextArea.waitUntilContentContains("<strong>Tree</strong>");
        firstEditPage.clickDone();
        assertEquals("before\n\n[[Smallest **Tree**>>image:image.gif]]\n\n ",
            WikiEditPage.gotoPage(testReference).getContent());
    }

    @Test
    @Order(8)
    void editSameMacro(TestReference testReference, TestUtils setup, MultiUserTestUtils multiUserSetup)
    {
        //
        // First Tab
        //

        // Start fresh.
        setup.deletePage(testReference);

        RealtimeWYSIWYGEditPage firstEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor firstEditor = firstEditPage.getContenEditor();
        RealtimeRichTextAreaElement firstTextArea = firstEditor.getRichTextArea();

        firstTextArea.sendKeys("/info");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/info", "Info Box");
        firstTextArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();
        firstTextArea.waitForContentRefresh();

        // Replace the default message text.
        firstTextArea.sendKeys(Keys.chord(Keys.SHIFT, Keys.END), Keys.BACK_SPACE);
        firstTextArea.sendKeys("one");

        MacroDialogEditModal firstMacroEditModal = firstEditor.getBalloonToolBar().editMacro();
        firstMacroEditModal.setMacroParameter("title", "Some");
        firstMacroEditModal.setMacroParameter("cssClass", "foo");

        //
        // Second Tab
        //

        String secondTabHandle = setup.getDriver().switchTo().newWindow(WindowType.TAB).getWindowHandle();

        RealtimeWYSIWYGEditPage secondEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor secondEditor = secondEditPage.getContenEditor();
        RealtimeRichTextAreaElement secondTextArea = secondEditor.getRichTextArea();

        secondTextArea.waitUntilTextContains("one");
        // Click on the information box content to be able to type inside it.
        secondTextArea.click(By.xpath("//p[. = 'one']"));
        secondTextArea.sendKeys(Keys.HOME, "two ");

        MacroDialogEditModal secondMacroEditModal = secondEditor.getBalloonToolBar().editMacro();
        // We want to verify that remote changes don't steal the focus from the modal.
        secondMacroEditModal.setMacroParameter("cssClass", "br", Keys.ARROW_LEFT);

        //
        // First Tab
        //

        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
        // Wait for the content to be synchronized before applying the macro parameter changes, otherwise we might
        // overwrite the text typed in the second tab.
        firstTextArea.waitUntilTextContains("two");
        firstMacroEditModal.setMacroContent("one two");
        firstMacroEditModal.clickSubmit();
        firstTextArea.waitForContentRefresh();

        // Move to the information box title field and type something.
        firstTextArea.sendKeys(Keys.ARROW_UP, Keys.END, " title");

        //
        // Second Tab
        //

        setup.getDriver().switchTo().window(secondTabHandle);

        secondTextArea.waitUntilTextContains("Some title");
        // Verify that the remote change (which included a macro parameter update) didn't steal the focus.
        secondMacroEditModal.getMacroParameterInput("cssClass").sendKeys("a");
        secondMacroEditModal.setMacroParameter("title", "Some title");
        secondMacroEditModal.clickSubmit();
        // The content is refreshed 3 times: after the macro is inserted, after the macro is updated from the first tab
        // and after the macro is updated from the second tab.
        secondTextArea.waitForContentRefresh("3");

        // Move to the information box title field and type something.
        secondTextArea.sendKeys(Keys.ARROW_UP, Keys.HOME);
        secondTextArea.sendKeys(Keys.chord(Keys.CONTROL, Keys.ARROW_RIGHT));
        secondTextArea.sendKeys(" cool");

        //
        // First Tab
        //

        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
        firstTextArea.waitUntilTextContains("Some cool title");
        firstTextArea.waitForContentRefresh();

        // Edit again the macro an verify that we have the correct parameter value.
        firstMacroEditModal = firstEditor.getBalloonToolBar().editMacro();
        assertEquals("bar", firstMacroEditModal.getMacroParameter("cssClass"));
        firstMacroEditModal.clickCancel();

        firstEditPage.clickDone();
        assertEquals("{{info cssClass=\"bar\" title=\"Some cool title\"}}\ntwo one\n{{/info}}",
            WikiEditPage.gotoPage(testReference).getContent());
    }

    @Test
    @Order(9)
    void reloadEditorsMergeConflictManualSave(TestReference testReference, TestUtils setup,
        MultiUserTestUtils multiUserSetup) throws Exception
    {
        //
        // First Tab
        //

        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "before\n\n[[image:image.gif||width=\"50px\"]]");
        setup.attachFile(testReference, "image.gif", getClass().getResourceAsStream("/image.gif"), false);

        RealtimeWYSIWYGEditPage firstEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor firstEditor = firstEditPage.getContenEditor();
        RealtimeRichTextAreaElement firstTextArea = firstEditor.getRichTextArea();
        firstTextArea.sendKeys(Keys.END, " first");

        // We plan to edit the same page in another tab outside the realtime session in order to trigger a merge
        // conflict. We have to save now to prevent the autosave from triggering (it doesn't trigger if there are no
        // local changes), because we want to control when the merge conflict modal is shown (moreover, we're going to
        // handle the merge conflict in a second browser tab).
        firstEditPage.getToolbar().sendSaveShortcutKey();

        //
        // Second Tab
        //

        String secondTabHandle = setup.getDriver().switchTo().newWindow(WindowType.TAB).getWindowHandle();

        RealtimeWYSIWYGEditPage secondEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor secondEditor = secondEditPage.getContenEditor();
        RealtimeRichTextAreaElement secondTextArea = secondEditor.getRichTextArea();

        // Only wait for the synchronization. We're not making any changes yet because we want to save the page outside
        // the realtime session first in order to trigger a merge conflict.
        secondTextArea.waitUntilTextContains("first");

        //
        // Third Tab
        //

        setup.getDriver().switchTo().newWindow(WindowType.TAB).getWindowHandle();
        RealtimeWYSIWYGEditPage thirdEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor thirdEditor = thirdEditPage.getContenEditor();
        RealtimeRichTextAreaElement thirdTextArea = thirdEditor.getRichTextArea();
        thirdTextArea.waitUntilTextContains("first");

        // Make some changes outside the realtime session and save in order to trigger a merge conflict in the realtime
        // session.
        thirdEditPage.getToolbar().leaveCollaboration();
        thirdTextArea.sendKeys(Keys.END, " third");
        thirdEditPage.clickSaveAndContinue();

        //
        // Second tab
        //

        setup.getDriver().switchTo().window(secondTabHandle);
        secondTextArea.sendKeys(Keys.END, " second");
        secondEditPage.getToolbar().sendSaveShortcutKey(false);

        EditConflictModal editConflictModal = new EditConflictModal();
        editConflictModal.makeChoiceAndSubmit(ConflictChoice.RELOAD, false);

        secondTextArea.waitUntilTextContains("third");
        // Verify that the image is still properly rendered.
        secondTextArea.verifyContent(content -> {
            WebElement image = content.getImages().get(0);
            AttachmentReference attachmentReference = new AttachmentReference("image.gif", testReference);
            assertEquals(setup.getURL(attachmentReference, "download", "width=50&rev=1.1"),
                image.getDomProperty("src"));
            Dimension imageSize = image.getSize();

            if (imageSize.width != 50 && "20".equals(image.getDomProperty("naturalWidth"))) {
                // FIXME: The image appears as broken / missing in Chrome sometimes even though:
                // - the image URL is the right one (we just asserted it above)
                // - the image naturalWidth = 20 (which indicates that the image is loaded and displayed)
                // This happens only when running the test with the servlet engine (Tomcat) in a Docker container. I
                // haven't been able to reproduce when testing manually on the same XWiki test instance. We force Chrome
                // to reload the image in this case.
                reloadImage(image, setup);
                // Get the updated image size.
                imageSize = image.getSize();
            }

            assertEquals(50, imageSize.width);
            assertEquals(50, imageSize.height);
        });

        //
        // First tab
        //

        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
        firstTextArea.waitUntilTextContains("third");
        assertEquals("before first third", firstTextArea.getText());
    }

    private void reloadImage(WebElement image, TestUtils setup)
    {
        StringBuilder script = new StringBuilder();
        script.append("let image = arguments[0];\n");
        script.append("let src = image.src;\n");
        script.append("image.src = '';\n");
        script.append("image.src = src;\n");
        setup.getDriver().executeScript(script.toString(), image);
        setup.getDriver().waitUntilCondition(ExpectedConditions.domPropertyToBe(image, "complete", "true"));
    }

    @Test
    @Order(10)
    void reloadEditorsSilentMergeConflictManualSave(TestReference testReference, TestUtils setup,
        MultiUserTestUtils multiUserSetup)
    {
        //
        // First Tab
        //

        // Start fresh.
        setup.deletePage(testReference);

        RealtimeWYSIWYGEditPage firstEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor firstEditor = firstEditPage.getContenEditor();
        RealtimeRichTextAreaElement firstTextArea = firstEditor.getRichTextArea();
        firstTextArea.sendKeys("First");
        firstEditPage.getToolbar().sendSaveShortcutKey();

        //
        // Second Tab
        //

        String secondTabHandle = setup.getDriver().switchTo().newWindow(WindowType.TAB).getWindowHandle();

        RealtimeWYSIWYGEditPage secondEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor secondEditor = secondEditPage.getContenEditor();
        RealtimeRichTextAreaElement secondTextArea = secondEditor.getRichTextArea();

        secondTextArea.waitUntilTextContains("First");

        //
        // Third Tab
        //

        setup.getDriver().switchTo().newWindow(WindowType.TAB).getWindowHandle();
        RealtimeWYSIWYGEditPage thirdEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor thirdEditor = thirdEditPage.getContenEditor();
        RealtimeRichTextAreaElement thirdTextArea = thirdEditor.getRichTextArea();
        thirdTextArea.waitUntilTextContains("First");

        thirdEditPage.getToolbar().leaveCollaboration();

        thirdTextArea.sendKeys(Keys.END, Keys.ENTER, "Third");
        thirdEditPage.clickSaveAndContinue();

        //
        // Second tab
        //

        setup.getDriver().switchTo().window(secondTabHandle);
        // Save to get the auto-merged content.
        secondEditPage.getToolbar().sendSaveShortcutKey(false);
        secondTextArea.waitUntilTextContains("Third");

        //
        // First tab
        //

        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
        firstTextArea.waitUntilTextContains("Third");
        assertEquals("First\nThird", firstTextArea.getText());
    }

    @Test
    @Order(11)
    void removeAllContent(TestReference testReference, TestUtils setup, MultiUserTestUtils multiUserSetup)
    {
        //
        // First Tab
        //

        // Start fresh.
        setup.deletePage(testReference);

        RealtimeWYSIWYGEditPage firstEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor firstEditor = firstEditPage.getContenEditor();
        RealtimeRichTextAreaElement firstTextArea = firstEditor.getRichTextArea();

        // Type some text and insert an information box.
        firstTextArea.sendKeys("before", Keys.ENTER, "/info");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/info", "Info Box");
        firstTextArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();
        firstTextArea.waitForContentRefresh();

        // Select the default information message and delete it.
        firstTextArea.sendKeys(Keys.chord(Keys.SHIFT, Keys.END), Keys.BACK_SPACE);

        // Insert a nested error box.
        firstTextArea.sendKeys("inside", Keys.ENTER, "/err");
        qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/err", "Error Box");
        firstTextArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();
        firstTextArea.waitForContentRefresh();

        // Replace the default error message.
        firstTextArea.sendKeys(Keys.chord(Keys.SHIFT, Keys.END), Keys.BACK_SPACE);
        firstTextArea.sendKeys("nested");

        // Type some text after the information box.
        firstTextArea.sendKeys(Keys.ARROW_DOWN, "after");

        //
        // Second Tab
        //

        setup.getDriver().switchTo().newWindow(WindowType.TAB).getWindowHandle();
        RealtimeWYSIWYGEditPage secondEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor secondEditor = secondEditPage.getContenEditor();
        RealtimeRichTextAreaElement secondTextArea = secondEditor.getRichTextArea();

        secondTextArea.waitUntilTextContains("after");
        // Overwrite the entire content.
        secondTextArea.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        secondTextArea.sendKeys(Keys.BACK_SPACE, "end");

        //
        // First Tab
        //

        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
        firstTextArea.waitUntilTextContains("end");
        firstTextArea.sendKeys(Keys.END, "ing");
        assertEquals("ending", firstTextArea.getText());
    }

    @Test
    @Order(12)
    void noLockWarningSameEditor(TestUtils setup, TestReference testReference, MultiUserTestUtils multiUserSetup)
    {
        //
        // First Tab
        //

        // We are already logged-in as John, edit the page as John, effectively locking it.
        RealtimeWYSIWYGEditPage firstEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor firstEditor = firstEditPage.getContenEditor();
        RealtimeRichTextAreaElement firstTextArea = firstEditor.getRichTextArea();

        //
        // Second Tab
        //
        multiUserSetup.openNewBrowserTab(XWIKI_ALIAS);
        loginAsBob(setup);

        // Edit the same page as Bob, John still has the lock, but because he is active in the realtime session
        // no warning message should be handled.
        RealtimeWYSIWYGEditPage secondEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);

        // If we get the editor, that means there was no warning.
        RealtimeCKEditor secondEditor = secondEditPage.getContenEditor();

        // Write some text to check that we joined the same session.
        RealtimeRichTextAreaElement secondTextArea = secondEditor.getRichTextArea();
        secondTextArea.sendKeys("Hello from Bob!");

        //
        // First Tab
        //

        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());
        firstTextArea.waitUntilTextContains("Bob!");
        assertEquals("Hello from Bob!", firstTextArea.getText());
    }

    @Test
    @Order(13)
    void lockWarningSameEditor(TestUtils setup, TestReference testReference, MultiUserTestUtils multiUserSetup)
    {
        //
        // First Tab
        //

        // We are already logged-in as John, edit the page as John, effectively locking it.
        RealtimeWYSIWYGEditPage firstEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        firstEditPage.getContenEditor();

        // Leaving the realtime session should not release the lock.
        firstEditPage.getToolbar().leaveCollaboration();

        //
        // Second Tab
        //
        multiUserSetup.openNewBrowserTab(XWIKI_ALIAS);
        loginAsBob(setup);

        // Edit the same page as Bob, John still has the lock, but because he is not active in the realtime session
        // a warning message should appear.
        setup.gotoPage(testReference, "edit", "editor=wysiwyg");

        // Check that we did not get to the edit page.
        assertFalse(setup.isInWYSIWYGEditMode());
    }

    @Test
    @Order(14)
    void lockWarningWysiwygAndWikiEditors(TestUtils setup, TestReference testReference,
        MultiUserTestUtils multiUserSetup)
    {
        //
        // First Tab
        //

        // We are already logged-in as John, edit the page as John, effectively locking it.
        RealtimeWYSIWYGEditPage.gotoPage(testReference).getContenEditor();

        //
        // Second Tab
        //
        multiUserSetup.openNewBrowserTab(XWIKI_ALIAS);
        loginAsBob(setup);

        // Edit the same page as Bob, John still has the lock, but because he is using a different editor
        // a warning message should appear.
        setup.gotoPage(testReference, "edit", "editor=wiki");

        // Check that we did not get to the edit page.
        assertFalse(setup.isInWYSIWYGEditMode());
    }

    @Test
    @Order(15)
    void restrictScriptMacroExecution(TestUtils setup, TestReference testReference, MultiUserTestUtils multiUserSetup)
    {
        //
        // First Tab
        //

        // We are already logged in as John.
        RealtimeWYSIWYGEditPage firstEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor firstEditor = firstEditPage.getContenEditor();
        RealtimeRichTextAreaElement firstTextArea = firstEditor.getRichTextArea();

        //
        // Second Tab
        //

        String secondTabHandle = multiUserSetup.openNewBrowserTab(XWIKI_ALIAS);

        setup.loginAsSuperAdmin();
        RealtimeWYSIWYGEditPage secondEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor secondEditor = secondEditPage.getContenEditor();
        RealtimeRichTextAreaElement secondTextArea = secondEditor.getRichTextArea();

        // Insert a Velocity macro.
        secondTextArea.sendKeys("before", Keys.RETURN);
        secondTextArea.sendKeys("/velo");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/velo", "Velocity");
        secondTextArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();
        new MacroDialogEditModal().waitUntilReady().setMacroContent("$xcontext.userReference.name").clickSubmit();
        secondTextArea.waitForContentRefresh();
        String text = secondTextArea.getText();
        assertTrue(text.contains("superadmin"));
        assertFalse(text.contains("Failed"), "Unexpected text content: " + text);
        String secondRefreshCounter = secondTextArea.getRefreshCounter();

        //
        // First Tab
        //

        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());
        // The content is re-rendered twice, first time when the Velocity macro is inserted and a second time when the
        // Velocity macro is edited.
        firstTextArea.waitForContentRefresh(secondRefreshCounter);

        // Even if John didn't make any changes yet, the script macro is executed with the minimum script rights
        // between the current user (John) and the script author associated with the realtime session (superadmin
        // currently).
        text = firstTextArea.getText();
        assertTrue(text.contains("Failed to execute the [velocity] macro."));

        // Change the content (without modifying the script macro).
        firstTextArea.sendKeys(Keys.END, " dinner");

        //
        // Second Tab
        //

        multiUserSetup.switchToBrowserTab(secondTabHandle);

        // Wait for the change made by John to be sure that the script author associated with the realtime session has
        // been updated.
        secondTextArea.waitUntilTextContains(" dinner");

        // Edit the macro again.
        secondTextArea.sendKeys(Keys.ENTER);
        new MacroDialogEditModal().waitUntilReady().setMacroContent("User: $xcontext.userReference.name").clickSubmit();
        secondTextArea.waitForContentRefresh();
        text = secondTextArea.getText();
        // This time the script macro is not executed because John has been associated as script author of the realtime
        // session.
        assertTrue(text.contains("Failed to execute the [velocity] macro."));
        assertFalse(text.contains("User: superadmin"), "Unexpected text content: " + text);

        secondTextArea.sendKeys(Keys.ARROW_LEFT, Keys.chord(Keys.CONTROL, Keys.SHIFT, Keys.ARROW_LEFT));
        secondTextArea.sendKeys("lunch");

        //
        // First Tab
        //

        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());
        // The second user has modified the Velocity macro, which triggered a re-rendering of the content on this tab.
        firstTextArea.waitForContentRefresh();
        firstTextArea.waitUntilTextContains("lunch");

        // Try to inject a script macro.
        firstTextArea.sendKeys(Keys.HOME);
        firstTextArea.sendKeys(Keys.chord(Keys.CONTROL, Keys.ARROW_RIGHT));
        firstTextArea.sendKeys(Keys.ENTER, Keys.ENTER, Keys.ARROW_UP, "/velo");
        qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/velo", "Velocity");
        firstTextArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();
        new MacroDialogEditModal().waitUntilReady().setMacroContent("injected").clickSubmit();
        firstTextArea.waitForContentRefresh();
        text = firstTextArea.getText();
        assertFalse(text.contains("injected"), "Unexpected text content: " + text);
        String firstRefreshCounter = firstTextArea.getRefreshCounter();

        // Leave the edit mode to see that the script level associated with the realtime session remains the same.
        setup.leaveEditMode();

        //
        // Second Tab
        //

        multiUserSetup.switchToBrowserTab(secondTabHandle);

        // The content is re-rendered twice because the first user has inserted and modified the Velocity macro.
        secondTextArea.waitForContentRefresh(firstRefreshCounter);
        text = secondTextArea.getText();
        assertTrue(text.contains("before\nFailed to execute the [velocity] macro."));
        assertFalse(text.contains("injected"), "Unexpected text content: " + text);

        // Edit again the macro to see that the script level doesn't change.
        secondTextArea.sendKeys(Keys.ARROW_RIGHT, Keys.ENTER);
        new MacroDialogEditModal().waitUntilReady().setMacroContent("Current: $xcontext.userReference").clickSubmit();
        secondTextArea.waitForContentRefresh();
        text = secondTextArea.getText();
        assertFalse(text.contains("Current: superadmin"), "Unexpected text content: " + text);
        assertTrue(text.contains("Failed to execute the [velocity] macro."), "Unexpected text content: " + text);
    }

    @Test
    @Order(16)
    void editPageTranslations(TestUtils setup, TestReference testReference, MultiUserTestUtils multiUserSetup)
    {
        //
        // First Tab
        //

        // Start fresh.
        setup.deletePage(testReference);

        // Force the English locale, in case this test is run multiple times (it switches to German locale at some
        // point).
        DocumentReference testReferenceEN = new DocumentReference(testReference, Locale.ENGLISH);
        RealtimeWYSIWYGEditPage firstEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReferenceEN);
        RealtimeCKEditor firstEditor = firstEditPage.getContenEditor();
        RealtimeRichTextAreaElement firstTextArea = firstEditor.getRichTextArea();

        firstTextArea.sendKeys("default content");

        // Save the page so that we can translate it.
        firstEditPage.getToolbar().sendSaveShortcutKey();

        //
        // Second Tab
        //

        // Translate the created page in a new tab using the alias so that we don't change the locale of the first tab.
        String secondTabHandle = multiUserSetup.openNewBrowserTab(XWIKI_ALIAS);

        // But first we need to enabled multilingual support.
        setup.loginAsSuperAdmin();
        setMultiLingual(setup, true, "en", "fr", "de");

        // Switch to French locale and start editing the French translation.
        setup.gotoPage(testReference, "edit", "editor=wysiwyg&language=fr");

        RealtimeWYSIWYGEditPage secondEditPage = new RealtimeWYSIWYGEditPage();
        RealtimeCKEditor secondEditor = secondEditPage.getContenEditor();
        RealtimeRichTextAreaElement secondTextArea = secondEditor.getRichTextArea();

        // The translated content is initialized with the default content.
        secondTextArea.waitUntilTextContains("default content");

        // Replace the default content with a script macro call. We want to check two things:
        // * different channels are used to synchronize the content of different translations
        // * each translation has its own script author
        secondTextArea.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        secondTextArea.sendKeys("French content", Keys.ENTER, "/velo");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/velo", "Velocity");
        secondTextArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();
        new MacroDialogEditModal().waitUntilReady().setMacroContent("$xcontext.userReference.name").clickSubmit();
        secondTextArea.waitUntilTextContains("superadmin");

        //
        // First Tab
        //

        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());

        // Try to insert a script macro. The current user doesn't have script right.
        firstTextArea.sendKeys(Keys.ENTER, "/velo");
        qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/velo", "Velocity");
        firstTextArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();
        new MacroDialogEditModal().waitUntilReady().setMacroContent(" ").clickSubmit();
        firstTextArea.waitUntilTextContains("default content\nFailed to execute the [velocity] macro.");

        // Verify that we're editing alone.
        assertTrue(firstEditPage.getToolbar().isEditingAlone());

        //
        // Second Tab
        //

        multiUserSetup.switchToBrowserTab(secondTabHandle);

        // Edit the inserted Velocity macro to verify that the script author for this translation didn't change after
        // John modified the default translation in the first tab.
        secondTextArea.sendKeys(Keys.ENTER);
        new MacroDialogEditModal().waitUntilReady().setMacroContent("User: $xcontext.userReference.name").clickSubmit();
        secondTextArea.waitUntilTextContains("User: superadmin");

        // Verify that we're editing alone.
        assertTrue(secondEditPage.getToolbar().isEditingAlone());

        assertEquals("French content\nUser: superadmin", secondEditPage.clickDone().getContent());

        //
        // First Tab
        //

        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());

        firstTextArea.sendKeys(Keys.ARROW_UP, Keys.HOME);
        firstTextArea.sendKeys(Keys.chord(Keys.CONTROL, Keys.SHIFT, Keys.ARROW_RIGHT));
        firstTextArea.sendKeys("English");

        String content = firstEditPage.clickDone().getContent();
        assertTrue(content.startsWith("English content\nFailed to execute the [velocity] macro."),
            "Unexpected content: " + content);

        // Now edit the same (German) translation.
        setup.gotoPage(testReference, "view", "language=de");
        new RealtimeInplaceEditablePage().translateInplace();

        firstEditPage = new RealtimeWYSIWYGEditPage();
        firstEditor = firstEditPage.getContenEditor();
        firstTextArea = firstEditor.getRichTextArea();

        // Replace the default (English) content.
        firstTextArea.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        firstTextArea.sendKeys("German content");

        //
        // Second Tab
        //

        multiUserSetup.switchToBrowserTab(secondTabHandle);

        setup.gotoPage(testReference, "edit", "editor=wysiwyg&language=de");

        secondEditPage = new RealtimeWYSIWYGEditPage();
        secondEditor = secondEditPage.getContenEditor();
        secondTextArea = secondEditor.getRichTextArea();

        secondTextArea.waitUntilTextContains("German content");
        secondTextArea.sendKeys(Keys.chord(Keys.CONTROL, Keys.SHIFT, Keys.ARROW_RIGHT));
        secondTextArea.sendKeys("Deutsch");

        //
        // First Tab
        //

        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());
        firstTextArea.waitUntilTextContains("Deutsch");
        assertEquals("Deutsch content", firstTextArea.getText());

        assertEquals("John, superadmin", firstEditPage.getToolbar().getVisibleCoeditors().stream()
            .map(CoeditorElement::getAvatarHint).reduce((a, b) -> a + ", " + b).get());
    }

    @Test
    @Order(17)
    void editSource(TestUtils setup, TestReference testReference, MultiUserTestUtils multiUserSetup)
    {
        //
        // First Tab
        //

        // Start fresh.
        loginAsJohn(setup);
        setup.deletePage(testReference);

        // Edit the page in the first browser tab.
        RealtimeWYSIWYGEditPage firstEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor firstEditor = firstEditPage.getContenEditor();
        CKEditorToolBar firstEditorToolbar = firstEditor.getToolBar();
        RealtimeRichTextAreaElement firstTextArea = firstEditor.getRichTextArea();

        // Check that the source button is available.
        assertTrue(firstEditorToolbar.canToggleSourceMode());

        //
        // Second Tab
        //

        String secondTabHandle = setup.getDriver().switchTo().newWindow(WindowType.TAB).getWindowHandle();

        // Edit the page in the second browser tab.
        RealtimeWYSIWYGEditPage secondEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor secondEditor = secondEditPage.getContenEditor();
        CKEditorToolBar secondEditorToolbar = secondEditor.getToolBar();
        RealtimeRichTextAreaElement secondTextArea = secondEditor.getRichTextArea();

        // Check that the source button is available.
        assertTrue(secondEditorToolbar.canToggleSourceMode());

        //
        // First Tab
        //

        // Switch back to the first tab
        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());

        // Type in the first tab to have some content.
        firstTextArea.sendKeys("one", Keys.ENTER, "two", Keys.ENTER, "three");

        //
        // Second Tab
        //

        // Switch to the second tab and verify the content.
        setup.getDriver().switchTo().window(secondTabHandle);
        secondTextArea.waitUntilTextContains("three");

        assertEquals("one\ntwo\nthree", secondTextArea.getText());

        // Switch to source mode and check that we are not in the realtime session anymore.
        secondEditorToolbar.toggleSourceMode();
        assertFalse(secondEditPage.getToolbar().isCollaborating());
        assertFalse(secondEditPage.getToolbar().canJoinCollaboration());

        // Check that we can still switch back to wysiwyg mode.
        assertTrue(secondEditorToolbar.canToggleSourceMode());

        // Check the contents of the source mode.
        assertEquals("one\n\ntwo\n\nthree", secondEditor.getSourceTextArea().getDomProperty("value"));

        // Make some changes to verify the merge. We try to cover two cases here:
        // * modify a paragraph that is not modified inside the realtime session
        // * insert a new paragraph in the same place where the realtime session inserts a new paragraph
        secondEditor.getSourceTextArea().sendKeys(Keys.UP, Keys.UP, Keys.END, " 2", Keys.DOWN, Keys.DOWN, Keys.END,
            Keys.ENTER, Keys.ENTER, "four");

        //
        // First Tab
        //

        // Switch to the first tab and make more changes, while the second user is in source mode.
        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());

        // Insert a new paragraph in the same place where the second user is inserting a new paragraph.
        // We also modify a paragraph that the second user didn't modify.
        firstTextArea.sendKeys(Keys.ENTER, "quattro", Keys.PAGE_UP, Keys.END, " 1");

        // Wait for the changes to be propagated in order to be sure that the new "quattro" paragraph is inserted before
        // the new "four" paragraph coming from the second user (source mode).
        firstTextArea.waitUntilLocalChangesArePushed();

        //
        // Second Tab
        //

        // Switch to the second tab.
        setup.getDriver().switchTo().window(secondTabHandle);

        // Switch back to WYSIWYG mode.
        secondEditorToolbar.toggleSourceMode();
        secondEditPage.getToolbar().waitUntilConnected();
        secondTextArea = secondEditor.getRichTextArea();

        // Check that the second user re-joined the realtime editing session.
        assertTrue(secondEditPage.getToolbar().isCollaborating());
        assertTrue(secondEditorToolbar.canToggleSourceMode());
        // Wait for the changes done in the realtime session while we were offline.
        secondTextArea.waitUntilContentContains("quattro");
        // Verify that the content has been merged (3-way).
        assertEquals("one 1\ntwo 2\nthree\nquattro\nfour", secondTextArea.getText());

        // Check that we can still switch to source mode.
        assertTrue(secondEditorToolbar.canToggleSourceMode());

        // Verify local changes are propagated after we re-joined the realtime session.
        secondTextArea.sendKeys(" 4");

        //
        // First Tab
        //

        // Verify what happens when both users switch to Source mode (i.e. no one else remains in the realtime session).
        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());

        firstTextArea.waitUntilContentContains("4");
        assertEquals("one 1\ntwo 2\nthree\nquattro\nfour 4", firstTextArea.getText());

        firstEditorToolbar.toggleSourceMode();
        assertFalse(firstEditPage.getToolbar().isCollaborating());
        assertEquals("one 1\n\ntwo 2\n\nthree\n\nquattro\n\nfour 4",
            secondEditor.getSourceTextArea().getDomProperty("value"));

        // Make some changes to verify the merge. We try to cover two cases here:
        // * modify a paragraph that is not touched by the other user
        // * modify a paragraph that is also modified by the other user
        firstEditor.getSourceTextArea().sendKeys(Keys.BACK_SPACE, "uno", Keys.DOWN, Keys.DOWN, Keys.DOWN, Keys.DOWN,
            Keys.END, " 3");

        //
        // Second Tab
        //

        // Switch to the second tab and switch back to Source mode.
        setup.getDriver().switchTo().window(secondTabHandle);
        secondEditorToolbar.toggleSourceMode();
        assertFalse(secondEditPage.getToolbar().isCollaborating());

        // At this point both users are editing outside the realtime session, and no one else is left in the realtime
        // session.

        // Make some changes. We modify two paragraphs:
        // * one that the other user is also modifying
        // * one that the other user is not modifying
        secondEditor.getSourceTextArea().sendKeys(Keys.HOME, Keys.BACK_SPACE, Keys.BACK_SPACE, " ", Keys.UP, Keys.UP, Keys.END,
            " tre", Keys.UP, Keys.UP, Keys.END, Keys.BACK_SPACE, "due");

        //
        // First Tab
        //

        // Switch back to WYSIWYG mode.
        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
        firstEditorToolbar.toggleSourceMode();
        firstEditPage.getToolbar().waitUntilConnected();
        firstTextArea = firstEditor.getRichTextArea();

        // Check that the second user did not re-join the realtime editing session yet.
        assertTrue(firstEditPage.getToolbar().isEditingAlone());
        String firstUserId = firstEditPage.getToolbar().getUserId();

        // Verify that the changes made in source mode have been merged.
        firstTextArea.waitUntilContentContains("3");
        assertEquals("one uno\ntwo 2\nthree 3\nquattro\nfour 4", firstTextArea.getText());

        // Wait for local changes to be propagated in order to ensure that "3" inserted by the first user comes before
        // "tre" inserted by the second user.
        firstTextArea.waitUntilLocalChangesArePushed();

        //
        // Second Tab
        //

        // Switch to the second tab and switch back to WYSIWYG mode.
        setup.getDriver().switchTo().window(secondTabHandle);
        secondEditorToolbar.toggleSourceMode();
        secondEditPage.getToolbar().waitUntilConnected();
        secondTextArea = secondEditor.getRichTextArea();

        // Verify that the content has been merged.
        secondTextArea.waitUntilContentContains("3");
        assertEquals("one uno\ntwo due\nthree 3 tre\nquattro four 4", secondTextArea.getText());

        // Check that both users have now joined.
        secondEditPage.getToolbar().waitForCoeditor(firstUserId);
        assertFalse(secondEditPage.getToolbar().isEditingAlone());

        // Verify local changes are propagated after we re-joined the realtime session.
        secondTextArea.sendKeys(" deux");

        //
        // First Tab
        //

        // Switch to the first tab and verify the merged content.
        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
        firstTextArea.waitUntilContentContains("deux");
        assertEquals("one uno\ntwo due deux\nthree 3 tre\nquattro four 4", firstTextArea.getText());
    }

    @Test
    @Order(18)
    void saveAndContinueDoesNotHideToolbar(TestUtils setup, TestReference testReference)
    {
        // Save the page first so that we can edit it inplace.
        setup.createPage(testReference, "one\n\n{{info}}two{{/info}}");

        // The default edit mode is inplace.
        RealtimeInplaceEditablePage inplaceEditablePage = new RealtimeInplaceEditablePage();
        inplaceEditablePage.editInplace();

        RealtimeCKEditor editor = new RealtimeCKEditor();
        CKEditorToolBar toolbar = editor.getToolBar();
        RealtimeRichTextAreaElement textArea = editor.getRichTextArea();

        // Move the focus from the main editable to the nested editable.
        textArea.sendKeys("1", Keys.ARROW_DOWN, Keys.HOME, "2");

        // Save and continue (using the shortcut so that the editor doesn't lose the focus).
        inplaceEditablePage.getToolbar().sendSaveShortcutKey();

        // Verify that the toolbar is still visible.
        assertTrue(toolbar.canToggleSourceMode());

        // Verify that Done doesn't ask for leave confirmation.
        textArea.sendKeys("3");
        inplaceEditablePage.done();
        assertEquals("1one\nInformation\n23two", inplaceEditablePage.getContent());
    }

    @Test
    @Order(19)
    void saveAndViewNoMergeConflict(TestUtils setup, TestReference testReference,  MultiUserTestUtils multiUserSetup)
    {
        //
        // First Tab
        //

        // Start fresh.
        setup.deletePage(testReference);

        // Edit the page in the first browser tab.
        RealtimeWYSIWYGEditPage firstEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor firstEditor = firstEditPage.getContenEditor();
        RealtimeRichTextAreaElement firstTextArea = firstEditor.getRichTextArea();

        //
        // Second Tab
        //

        String secondTabHandle = setup.getDriver().switchTo().newWindow(WindowType.TAB).getWindowHandle();

        // Edit the page in the second browser tab.
        RealtimeWYSIWYGEditPage secondEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor secondEditor = secondEditPage.getContenEditor();
        RealtimeRichTextAreaElement secondTextArea = secondEditor.getRichTextArea();

        secondTextArea.sendKeys("second");
        secondEditPage.clickDone();

        //
        // First Tab
        //

        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
        firstTextArea.waitUntilTextContains("second");
        firstTextArea.sendKeys("first ");
        // Use the shortcut key so that we don't lose the caret position.
        firstEditPage.getToolbar().sendSaveShortcutKey();

        //
        // Second Tab
        //

        setup.getDriver().switchTo().window(secondTabHandle);

        // Edit again, this time inplace.
        RealtimeInplaceEditablePage secondInplaceEditPage = new RealtimeInplaceEditablePage().editInplace();
        secondEditPage = new RealtimeWYSIWYGEditPage();
        secondEditor = secondEditPage.getContenEditor();
        secondTextArea = secondEditor.getRichTextArea();
        secondTextArea.waitUntilTextContains("first second");

        //
        // First Tab
        //

        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
        firstTextArea.sendKeys("then ");
        firstEditPage.clickDone();

        //
        // Second Tab
        //

        setup.getDriver().switchTo().window(secondTabHandle);
        secondTextArea.waitUntilTextContains("first then second");
        secondTextArea.sendKeys(Keys.END, " and");
        // Use the shortcut key so that we don't lose the caret position.
        secondEditPage.getToolbar().sendSaveShortcutKey();

        //
        // First Tab
        //

        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());

        // Edit again, this time inplace.
        RealtimeInplaceEditablePage firstInplaceEditPage = new RealtimeInplaceEditablePage().editInplace();
        firstEditPage = new RealtimeWYSIWYGEditPage();
        firstEditor = firstEditPage.getContenEditor();
        firstTextArea = firstEditor.getRichTextArea();

        firstTextArea.waitUntilTextContains("first then second and");
        firstTextArea.sendKeys(Keys.END, " done");
        firstInplaceEditPage.done();

        //
        // Second Tab
        //

        setup.getDriver().switchTo().window(secondTabHandle);
        secondTextArea.waitUntilTextContains("first then second and done");
        secondTextArea.sendKeys(" almost");
        secondInplaceEditPage.done();

        assertEquals("first then second and almost done", secondInplaceEditPage.getContent());
    }

    @Test
    @Order(20)
    void dragAndDropFilesAtTheSameTime(TestUtils setup, TestReference testReference, MultiUserTestUtils multiUserSetup)
        throws Exception
    {
        //
        // First Tab
        //

        // Start fresh.
        setup.deletePage(testReference);

        // Edit the page in the first browser tab.
        RealtimeWYSIWYGEditPage firstEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor firstEditor = firstEditPage.getContenEditor();
        RealtimeRichTextAreaElement firstTextArea = firstEditor.getRichTextArea();

        firstTextArea.sendKeys("f");

        //
        // Second Tab
        //

        String secondTabHandle = setup.getDriver().switchTo().newWindow(WindowType.TAB).getWindowHandle();

        // Edit the page in the second browser tab.
        RealtimeWYSIWYGEditPage secondEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor secondEditor = secondEditPage.getContenEditor();
        RealtimeRichTextAreaElement secondTextArea = secondEditor.getRichTextArea();

        secondTextArea.waitUntilTextContains("f");
        secondTextArea.sendKeys(Keys.END, Keys.ENTER, "second ");

        //
        // First Tab
        //

        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
        firstTextArea.sendKeys("irst end");
        firstTextArea.sendKeys(Keys.chord(Keys.CONTROL, Keys.LEFT));
        // Drop the file without waiting for the upload to finish because we want to simulate two users dropping files
        // at the same time.
        firstTextArea.dropFile("/realtimeWysiwygEditor.webm", false);

        //
        // Second Tab
        //

        setup.getDriver().switchTo().window(secondTabHandle);
        secondTextArea.dropFile("/source-button.mp4", false);
        secondTextArea.sendKeys(Keys.RIGHT, " blue ");

        //
        // First Tab
        //

        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
        firstTextArea.sendKeys(Keys.RIGHT, " red ");
        firstTextArea.dropFile("/ckeditor-source.png", false);

        //
        // Second Tab
        //

        setup.getDriver().switchTo().window(secondTabHandle);
        secondTextArea.dropFile("/realtimeWysiwygEditor.png", false);
        secondTextArea.sendKeys(Keys.RIGHT, " green");

        //
        // First Tab
        //

        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
        firstTextArea.sendKeys(Keys.RIGHT, " yellow ");

        //
        // Second Tab
        //

        setup.getDriver().switchTo().window(secondTabHandle);
        secondTextArea.waitUntilTextContains("yellow");
        secondTextArea.waitForUploadsToFinish();

        // Verify the result.
        secondEditor.getToolBar().toggleSourceMode();
        assertEquals("""
            first [[attach:realtimeWysiwygEditor.webm||target="_blank"]] red [[image:ckeditor-source.png]] yellow end

            second\u00A0[[attach:source-button.mp4||target="_blank"]] blue\u00A0[[image:realtimeWysiwygEditor.png]] green""",
            secondEditor.getSourceTextArea().getDomProperty("value"));
    }

    @Test
    @Order(21)
    void failGracefullyIfWeCannotConnect(TestUtils setup, TestReference testReference) throws Exception
    {
        RealtimeTestUtils.simulateFailedWebSocketConnection(setup, () -> {
            // Start fresh.
            setup.deletePage(testReference);

            loginAsJohn(setup);

            // Verify the standalone editor.
            WYSIWYGEditPage editPage = WYSIWYGEditPage.gotoPage(testReference);
            editPage.waitForNotificationErrorMessage("Failed to join the realtime collaboration.");

            RealtimeEditToolbar realtimeToolbar = new RealtimeEditToolbar();
            assertFalse(realtimeToolbar.isCollaborating());
            assertFalse(realtimeToolbar.canJoinCollaboration());

            CKEditor editor = new CKEditor("content").waitToLoad();
            RichTextAreaElement textArea = editor.getRichTextArea();
            textArea.sendKeys("alone");
            editor.getToolBar().toggleSourceMode();
            assertEquals("alone", editor.getSourceTextArea().getDomProperty("value"));
            editPage.clickSaveAndView();

            // Verify the in-place editor.
            InplaceEditablePage inplaceEditablePage = new InplaceEditablePage();
            assertEquals("alone", inplaceEditablePage.getContent());

            inplaceEditablePage.editInplace();
            inplaceEditablePage.waitForNotificationErrorMessage("Failed to join the realtime collaboration.");

            assertFalse(realtimeToolbar.isCollaborating());
            assertFalse(realtimeToolbar.canJoinCollaboration());

            new CKEditor("content").waitToLoad().getRichTextArea().sendKeys("edited ");
            inplaceEditablePage.save();
            assertEquals("edited alone", inplaceEditablePage.getContent());

            return null;
        });
    }

    @Test
    @Order(22)
    void editFullScreen(TestUtils setup, TestReference testReference, MultiUserTestUtils multiUserSetup)
    {
        //
        // First Tab
        //

        // Start fresh.
        loginAsJohn(setup);
        setup.deletePage(testReference);

        // Edit the page in the first browser tab (standalone).
        RealtimeWYSIWYGEditPage firstEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor firstEditor = firstEditPage.getContenEditor();
        RealtimeRichTextAreaElement firstTextArea = firstEditor.getRichTextArea();

        // Standalone fullscreen.
        firstEditor.getToolBar().toggleFullScreenMode();
        firstTextArea.sendKeys("one");

        // Save the page in order to be able to edit in in-place in the second browser tab.
        firstEditPage.getToolbar().sendSaveShortcutKey();

        //
        // Second Tab
        //

        String secondTabHandle = setup.getDriver().switchTo().newWindow(WindowType.TAB).getWindowHandle();

        // Edit the page in the second browser tab (in-place).
        RealtimeInplaceEditablePage inplaceEditablePage = RealtimeInplaceEditablePage.gotoPage(testReference);
        inplaceEditablePage.editInplace();
        RealtimeCKEditor secondEditor = new RealtimeCKEditor();
        RealtimeRichTextAreaElement secondTextArea = secondEditor.getRichTextArea();

        // Focus the editing area to get the floating toolbar.
        secondTextArea.click();

        // In-place fullscreen.
        secondEditor.getToolBar().toggleFullScreenMode();
        secondTextArea.sendKeys(Keys.END, Keys.ENTER, "two");

        //
        // First Tab
        //

        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
        firstTextArea.waitUntilTextContains("two");
        firstTextArea.sendKeys(" 1");

        // Switch to Source mode.
        firstEditor.getToolBar().toggleSourceMode();
        firstEditor.getSourceTextArea().sendKeys(" 3");

        //
        // Second Tab
        //

        setup.getDriver().switchTo().window(secondTabHandle);
        secondTextArea.waitUntilTextContains("1");
        inplaceEditablePage.getToolbar().waitForConcurrentEditingWarning();
        inplaceEditablePage.getToolbar().leaveCollaboration();
        secondTextArea.sendKeys(" 2");

        //
        // First Tab
        //

        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
        // Back to WYSIWYG mode.
        firstEditor.getToolBar().toggleSourceMode();
        firstEditPage.getToolbar().waitUntilConnected();
        firstTextArea = firstEditor.getRichTextArea();
        firstTextArea.sendKeys(" 5");

        // The outcome is different depending on whether these changes are pushed before the second user joins back, so
        // we enforce an order to avoid test flakiness.
        firstTextArea.waitUntilLocalChangesArePushed();

        //
        // Second Tab
        //

        setup.getDriver().switchTo().window(secondTabHandle);
        // Join again the realtime collaboration.
        inplaceEditablePage.getToolbar().joinCollaboration();
        secondTextArea.sendKeys(" 4");
        // Switch to Source mode.
        secondEditor.getToolBar().toggleSourceMode();
        // Exit full screen mode.
        secondEditor.getToolBar().toggleFullScreenMode();
        assertTrue(secondEditor.getToolBar().isInSourceMode());
        // Back to WYSIWYG mode.
        secondEditor.getToolBar().toggleSourceMode();
        inplaceEditablePage.getToolbar().waitUntilConnected();
        // Back to full screen mode.
        secondEditor.getToolBar().toggleFullScreenMode();
        assertFalse(secondEditor.getToolBar().isInSourceMode());

        //
        // First Tab
        //

        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
        // Exit full screen mode.
        firstEditor.getToolBar().toggleFullScreenMode();
        firstTextArea.waitUntilTextContains("4");
        // Back to Source mode.
        firstEditor.getToolBar().toggleSourceMode();
        // Back to full screen mode.
        firstEditor.getToolBar().toggleFullScreenMode();

        // Save (from full screen mode) and edit again (still standalone).
        firstEditPage.clickSaveAndView();
        firstEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        firstEditor = firstEditPage.getContenEditor();
        firstTextArea = firstEditor.getRichTextArea();

        // Enter full screen mode again.
        firstEditor.getToolBar().toggleFullScreenMode();
        firstTextArea.sendKeys(Keys.END, " 7");

        //
        // Second Tab
        //

        setup.getDriver().switchTo().window(secondTabHandle);
        secondTextArea.waitUntilTextContains("7");
        // Exit full screen mode.
        secondEditor.getToolBar().toggleFullScreenMode();
        // Leave collaboration.
        inplaceEditablePage.getToolbar().leaveCollaboration();
        // Back to full screen mode.
        secondEditor.getToolBar().toggleFullScreenMode();
        secondTextArea.sendKeys(" 6");
        // Join collaboration.
        inplaceEditablePage.getToolbar().joinCollaboration();
        secondTextArea.sendKeys(" 8");

        //
        // First Tab
        //

        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
        firstTextArea.waitUntilTextContains("8");
        // Exit full screen mode.
        firstEditor.getToolBar().toggleFullScreenMode();
        // Switch to Source mode.
        firstEditor.getToolBar().toggleSourceMode();
        // Back to full screen mode.
        firstEditor.getToolBar().toggleFullScreenMode();
        firstEditor.getSourceTextArea().sendKeys(" 9");
        // Back to WYSIWYG mode.
        firstEditor.getToolBar().toggleSourceMode();
        firstEditPage.getToolbar().waitUntilConnected();
        firstTextArea = firstEditor.getRichTextArea();
        firstTextArea.sendKeys(" 11");

        //
        // Second Tab
        //

        setup.getDriver().switchTo().window(secondTabHandle);
        secondTextArea.waitUntilTextContains("11");
        // This will cause a merge conflict because the first user saved from Source mode.
        inplaceEditablePage.getToolbar().clickDone();

        // Resolve the merge conflict caused by the fact that we saved from Source mode.
        EditConflictModal editConflictModal = new EditConflictModal();
        editConflictModal.submitCurrentChoice(true);
        assertEquals("one 1 3 5 7 9 11\ntwo 2 4 6 8", inplaceEditablePage.getContent());
    }

    @Test
    @Order(23)
    void restoreUnsavedChanges(TestUtils setup, TestReference testReference, MultiUserTestUtils multiUserSetup)
    {
        //
        // First Tab
        //

        // Start fresh.
        loginAsJohn(setup);
        setup.deletePage(testReference);

        // Edit the page in the first browser tab (standalone).
        RealtimeWYSIWYGEditPage firstEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeRichTextAreaElement firstTextArea = firstEditPage.getContenEditor().getRichTextArea();

        // Type something to have unsaved changes.
        firstTextArea.sendKeys("one");

        // Leave the page by clicking on the breadcrumb link.
        new ViewPage().getBreadcrumb().clickPathElement(testReference.getLastSpaceReference().getName());
        // Edit again the page.
        firstEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        firstTextArea = firstEditPage.getContenEditor().getRichTextArea();

        // Verify that the unsaved changes have been restored.
        firstTextArea.waitForOwnNotificationInfoMessage("Unsaved changes restored");
        firstTextArea.waitUntilTextContains("one");
        assertEquals(SaveStatus.UNSAVED, firstEditPage.getToolbar().getSaveStatus());

        // Leave the page again, same way.
        new ViewPage().getBreadcrumb().clickPathElement(testReference.getLastSpaceReference().getName());
        // Edit again the page.
        firstEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        firstTextArea = firstEditPage.getContenEditor().getRichTextArea();

        // Verify that the unsaved changes have been restored.
        firstTextArea.waitForOwnNotificationInfoMessage("Unsaved changes restored");
        firstTextArea.waitUntilTextContains("one");
        assertEquals(SaveStatus.UNSAVED, firstEditPage.getToolbar().getSaveStatus());

        // Cancel the unsaved changes.
        setup.leaveEditMode();

        // Edit again the page. This time unsaved changes should not be restored.
        firstEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        firstTextArea = firstEditPage.getContenEditor().getRichTextArea();

        // Save in order to be able to edit in-place in the second tab.
        firstEditPage.getToolbar().sendSaveShortcutKey();

        // Verify the case when unsaved changes can't be restored because the merge fails.
        firstTextArea.sendKeys("two");

        // Leave the page again, but this time edit it in a separate tab.
        new ViewPage().getBreadcrumb().clickPathElement(testReference.getLastSpaceReference().getName());

        //
        // Second Tab
        //

        String secondTabHandle = setup.getDriver().switchTo().newWindow(WindowType.TAB).getWindowHandle();

        // Edit the page in the second browser tab (in-place).
        RealtimeInplaceEditablePage inplaceEditablePage = RealtimeInplaceEditablePage.gotoPage(testReference);
        inplaceEditablePage.editInplace();
        RealtimeRichTextAreaElement secondTextArea = new RealtimeCKEditor().getRichTextArea();

        secondTextArea.sendKeys("three");
        secondTextArea.waitUntilLocalChangesArePushed();

        //
        // First Tab
        //

        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
        firstEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        firstTextArea = firstEditPage.getContenEditor().getRichTextArea();

        firstTextArea.waitForOwnNotificationWarningMessage("Failed to restore unsaved changes");
        firstTextArea.waitUntilTextContains("three");
        assertEquals(SaveStatus.UNSAVED, firstEditPage.getToolbar().getSaveStatus());

        // Leave edit mode to test how unsaved changes are restored on the second tab using the in-place edit mode.
        setup.leaveEditMode();

        //
        // Second Tab
        //

        setup.getDriver().switchTo().window(secondTabHandle);
        // Leave the edit mode with unsaved changes, by clicking on the breadcrumb link.
        inplaceEditablePage.getBreadcrumb().clickPathElement(testReference.getLastSpaceReference().getName());
        // Edit again the page.
        inplaceEditablePage.editInplace();
        secondTextArea = new RealtimeCKEditor().getRichTextArea();

        // Verify that the unsaved changes have been restored.
        secondTextArea.waitForOwnNotificationInfoMessage("Unsaved changes restored");
        secondTextArea.waitUntilTextContains("three");
        assertEquals(SaveStatus.UNSAVED, inplaceEditablePage.getToolbar().getSaveStatus());

        // Try again.
        inplaceEditablePage.getBreadcrumb().clickPathElement(testReference.getLastSpaceReference().getName());
        inplaceEditablePage.editInplace();
        secondTextArea = new RealtimeCKEditor().getRichTextArea();

        // Verify that the unsaved changes have been restored.
        secondTextArea.waitForOwnNotificationInfoMessage("Unsaved changes restored");
        secondTextArea.waitUntilTextContains("three");
        assertEquals(SaveStatus.UNSAVED, inplaceEditablePage.getToolbar().getSaveStatus());

        // This time cancel the unsaved changes.
        setup.leaveEditMode();

        // Edit again the page. This time unsaved changes should not be restored.
        inplaceEditablePage.editInplace();
        secondTextArea = new RealtimeCKEditor().getRichTextArea();

        // Let's verify a case where unsaved changes can be merged.
        secondTextArea.sendKeys("four");
        inplaceEditablePage.getToolbar().sendSaveShortcutKey();
        secondTextArea.sendKeys(" five");

        // Leave the edit mode with unsaved changes and edit the page in the first tab.
        inplaceEditablePage.getBreadcrumb().clickPathElement(testReference.getLastSpaceReference().getName());

        //
        // First Tab
        //

        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
        firstEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        firstTextArea = firstEditPage.getContenEditor().getRichTextArea();
        firstTextArea.waitUntilTextContains("four");
        firstTextArea.sendKeys("zero ");
        firstTextArea.waitUntilLocalChangesArePushed();

        //
        // Second Tab
        //

        setup.getDriver().switchTo().window(secondTabHandle);
        inplaceEditablePage.editInplace();
        secondTextArea = new RealtimeCKEditor().getRichTextArea();

        // Verify that the unsaved changes have been restored and merged.
        secondTextArea.waitForOwnNotificationInfoMessage("Unsaved changes restored");
        secondTextArea.waitUntilTextContains("zero four five");
        assertEquals(SaveStatus.UNSAVED, inplaceEditablePage.getToolbar().getSaveStatus());

        //
        // First Tab
        //

        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
        firstTextArea.waitUntilTextContains("zero four five");
    }

    @Test
    @Order(24)
    void syntaxChange(TestUtils setup, TestReference testReference, MultiUserTestUtils multiUserSetup)
    {
        // Start fresh.
        loginAsJohn(setup);
        setup.deletePage(testReference);
        setup.createPage(testReference, "[[label>>#target]]", "", "xwiki/2.0");

        //
        // Perform the syntax change from the standalone editor.
        //

        RealtimeWYSIWYGEditPage firstEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor firstEditor = firstEditPage.getContenEditor();
        RealtimeRichTextAreaElement firstTextArea = firstEditor.getRichTextArea();

        // Type some text to verify that it isn't lost when we change the syntax.
        // First move the cursor to the start before entering the text to ensure it is outside the link, being
        // outside the link is the initial state in Chrome but not in Firefox.
        firstTextArea.sendKeys(Keys.HOME, "before ");

        // Change the syntax.
        DocumentSyntaxPicker documentSyntaxPicker = new DocumentInformationPanel().getSyntaxPicker();
        assertEquals("xwiki/2.0", documentSyntaxPicker.getSelectedSyntax());

        // Confirm the syntax conversion.
        SyntaxConversionConfirmationModal syntaxConfirmationModal = documentSyntaxPicker.selectSyntaxById("xwiki/2.1");
        assertTrue(syntaxConfirmationModal.getMessage()
            .contains("from the previous XWiki 2.0 syntax to the selected XWiki 2.1 syntax?"));
        syntaxConfirmationModal.confirmSyntaxConversion();
        // Wait for the syntax conversion.
        firstEditPage.waitForNotificationSuccessMessage("Syntax converted");
        // Wait for the editor to be reloaded (recreated).
        firstEditPage.waitForNotificationSuccessMessage("WYSIWYG editor updated");

        // The editor was recreated.
        firstEditor = firstEditPage.getContenEditor();
        // Verify that we are still connected to the realtime collaboration session.
        assertTrue(firstEditPage.getToolbar().isCollaborating());
        firstEditor.getToolBar().toggleSourceMode();
        String source = firstEditor.getSourceTextArea().getDomProperty("value").replace('\u00A0', ' ');
        assertEquals("before [[label>>||anchor=\"target\"]]", source);

        firstEditPage.clickCancel();

        //
        // Perform the syntax change from the in-place editor.
        //

        // Open the document information tab before editing in-place because both change the document fragment (anchor)
        // in the current URL. We need the #edit fragment in the URL in order to detect when the user leaves the edit
        // mode. See TestUtils#leaveEditMode().
        RealtimeInplaceEditablePage inplaceEditablePage = new RealtimeInplaceEditablePage();
        InformationPane informationPane = inplaceEditablePage.openInformationDocExtraPane();

        // Start editing in-place.
        inplaceEditablePage.editInplace();
        firstEditor = new RealtimeCKEditor();
        firstTextArea = firstEditor.getRichTextArea();
        // Type some text to verify that it isn't lost when we change the syntax.
        firstTextArea.sendKeys(Keys.END, " after");

        // Change the syntax.
        documentSyntaxPicker = informationPane.editSyntax().getSyntaxPicker();
        syntaxConfirmationModal = documentSyntaxPicker.selectSyntaxById("xwiki/2.1");
        // Confirm the syntax conversion.
        assertTrue(syntaxConfirmationModal.getMessage()
            .contains("from the previous XWiki 2.0 syntax to the selected XWiki 2.1 syntax?"));
        syntaxConfirmationModal.confirmSyntaxConversion();
        // Wait for the editor to be reloaded (recreated).
        firstEditPage.waitForNotificationSuccessMessage("WYSIWYG editor updated");

        // The editor was re-created.
        firstEditor = new RealtimeCKEditor();
        firstTextArea = firstEditor.getRichTextArea();
        // Verify that we are still connected to the realtime collaboration session.
        assertTrue(firstEditPage.getToolbar().isCollaborating());
        // Focus the editing area to get the floating toolbar and access the Source button.
        firstTextArea.click();
        firstEditor.getToolBar().toggleSourceMode();
        source = firstEditor.getSourceTextArea().getDomProperty("value").replace('\u00A0', ' ');
        assertEquals("[[label>>||anchor=\"target\"]] after", source);
    }

    @Test
    @Order(25)
    void preventEmptyRevisions(TestUtils setup, TestReference testReference, MultiUserTestUtils multiUserSetup)
    {
        //
        // First Tab
        //

        // Start fresh.
        setup.loginAsSuperAdmin();
        setup.deletePage(testReference);

        // Edit the page in the first browser tab (standalone).
        RealtimeWYSIWYGEditPage firstEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);

        // There are no revisions yet.
        HistoryDropdown historyDropdown = firstEditPage.getToolbar().getHistoryDropdown();
        assertTrue(historyDropdown.open().getVersions().isEmpty());

        // Even if there is no change, the document is new so the Done button should create the first revision.
        firstEditPage.clickDone();

        //
        // Second Tab
        //

        String secondTabHandle = multiUserSetup.openNewBrowserTab(XWIKI_ALIAS);
        loginAsJohn(setup);

        // Edit the page in the second browser tab (in-place).
        RealtimeInplaceEditablePage inplaceEditablePage = RealtimeInplaceEditablePage.gotoPage(testReference);
        inplaceEditablePage.editInplace();
        RealtimeCKEditor secondEditor = new RealtimeCKEditor();
        RealtimeRichTextAreaElement secondTextArea = secondEditor.getRichTextArea();

        // Verify that the history dropdown contains only one entry (the initial revision).
        List<VersionElement> versions = historyDropdown.open().getVersions();
        assertEquals(1, versions.size());
        assertEquals("1.1", versions.get(0).getNumber());
        assertEquals("Su", versions.get(0).getAuthor().getAbbreviation());

        // Try to save without making any changes. Even if the author is different, no new revision should be created.
        inplaceEditablePage.getToolbar().sendSaveShortcutKey();

        // Verify that the history dropdown still contains only one entry (the initial revision).
        versions = historyDropdown.open().getVersions();
        assertEquals(1, versions.size());
        assertEquals("1.1", versions.get(0).getNumber());
        assertEquals("Su", versions.get(0).getAuthor().getAbbreviation());

        //
        // First Tab
        //

        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());
        // Join the realtime collaboration.
        firstEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor firstEditor = firstEditPage.getContenEditor();
        RealtimeRichTextAreaElement firstTextArea = firstEditor.getRichTextArea();

        //
        // Second Tab
        //

        multiUserSetup.switchToBrowserTab(secondTabHandle);
        secondTextArea.sendKeys("one");
        inplaceEditablePage.getToolbar().sendSaveShortcutKey();

        // Verify that the history dropdown contains two versions.
        versions = historyDropdown.open().getVersions();
        assertEquals(2, versions.size());
        assertEquals("1.1", versions.get(0).getNumber());
        assertEquals("Su", versions.get(0).getAuthor().getAbbreviation());
        assertEquals("1.2", versions.get(1).getNumber());
        assertEquals("Jo", versions.get(1).getAuthor().getAbbreviation());
        historyDropdown.close();

        //
        // First Tab
        //

        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());
        firstTextArea.waitUntilTextContains("one");

        // Verify that the new version was propagated.
        versions = historyDropdown.waitForVersion("1.2").open().getVersions();
        assertEquals(2, versions.size());
        assertEquals("1.1", versions.get(0).getNumber());
        assertEquals("Su", versions.get(0).getAuthor().getAbbreviation());
        assertEquals("1.2", versions.get(1).getNumber());
        assertEquals("Jo", versions.get(1).getAuthor().getAbbreviation());

        // Try to save without making any changes. No new revision should be created.
        firstEditPage.getToolbar().sendSaveShortcutKey();

        // Verify that the history dropdown still contains only two entries.
        versions = historyDropdown.open().getVersions();
        assertEquals(2, versions.size());

        firstTextArea.sendKeys(Keys.END, " two");
        assertEquals("one two", firstEditPage.clickDone().getContent());

        //
        // Second Tab
        //

        multiUserSetup.switchToBrowserTab(secondTabHandle);
        secondTextArea.waitUntilTextContains("two");

        // Verify that the new version was propagated.
        versions = historyDropdown.waitForVersion("2.1").open().getVersions();
        assertEquals(3, versions.size());
        assertEquals("1.1", versions.get(0).getNumber());
        assertEquals("Su", versions.get(0).getAuthor().getAbbreviation());
        assertEquals("1.2", versions.get(1).getNumber());
        assertEquals("Jo", versions.get(1).getAuthor().getAbbreviation());
        assertEquals("2.1", versions.get(2).getNumber());
        assertEquals("Su", versions.get(2).getAuthor().getAbbreviation());

        // Leave the editing session without making any changes. No new revision should be created.
        HistoryPane historyPane = inplaceEditablePage.done().openHistoryDocExtraPane().showMinorEdits();
        assertEquals(3, historyPane.getNumberOfVersions());
        assertEquals("2.1", historyPane.getCurrentVersion());
        assertEquals("superadmin", historyPane.getCurrentAuthor());

        //
        // First Tab
        //

        // Edit again to check that we can force a new empty revision by specifying a version summary.
        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());
        firstEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);

        assertEquals(1, historyDropdown.open().getVersions().size());

        SummaryModal summaryModal = historyDropdown.summarizeChanges();
        summaryModal.setSummary("An empty revision");
        summaryModal.clickSave(true);

        versions = historyDropdown.open().getVersions();
        assertEquals(2, versions.size());
        assertEquals("2.2", versions.get(1).getNumber());
        assertEquals("Su", versions.get(1).getAuthor().getAbbreviation());

        // Try again, this time without a summary.
        summaryModal = historyDropdown.summarizeChanges();
        summaryModal.setSummary("");
        summaryModal.clickSave(false);
        firstEditPage.getToolbar().waitForSaveStatus(SaveStatus.SAVED);

        assertEquals(2, historyDropdown.open().getVersions().size());

        // Leave the editing session with a summary. This should create a new revision.
        summaryModal = firstEditPage.getToolbar().clickSummarizeAndDone();
        summaryModal.setSummary("Another empty revision");
        summaryModal.clickSave(true);

        historyPane = new ViewPage().openHistoryDocExtraPane().showMinorEdits();
        assertEquals(5, historyPane.getNumberOfVersions());
        assertEquals("3.1", historyPane.getCurrentVersion());
        assertEquals("superadmin", historyPane.getCurrentAuthor());
        assertEquals("Another empty revision", historyPane.getCurrentVersionComment());

        //
        // Second Tab
        //

        multiUserSetup.switchToBrowserTab(secondTabHandle);
        // Edit again to verify that the latest version is returned even when there are no changes. The plan is to save
        // the same document state with both users, before making any changes and after making the same changes.
        inplaceEditablePage = RealtimeInplaceEditablePage.gotoPage(testReference);
        inplaceEditablePage.editInplace();
        secondEditor = new RealtimeCKEditor();
        secondTextArea = secondEditor.getRichTextArea();

        //
        // First Tab
        //

        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());
        firstEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        firstEditor = firstEditPage.getContenEditor();
        firstTextArea = firstEditor.getRichTextArea();

        // Verify that there is a single revision listed in the history dropdown.
        versions = historyDropdown.open().getVersions();
        assertEquals(1, versions.size());
        assertEquals("3.1", versions.get(0).getNumber());
        assertEquals("Su", versions.get(0).getAuthor().getAbbreviation());

        //
        // Second Tab
        //

        multiUserSetup.switchToBrowserTab(secondTabHandle);
        // Focus the editing area to get the floating toolbar.
        secondTextArea.click();
        secondEditor.getToolBar().toggleSourceMode();
        // Close the concurrent editing warning popover because it may cover other UI elements (like the Save button if
        // the window width is too small for the toolbar to fit in a single line).
        inplaceEditablePage.getToolbar().waitForConcurrentEditingWarning();
        // Save without making any changes.
        inplaceEditablePage.save();

        //
        // First Tab
        //

        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());
        // Save without making any changes.
        firstEditPage.getToolbar().sendSaveShortcutKey();
        // Verify that we have two revisions listed in the history dropdown.
        versions = historyDropdown.open().getVersions();
        assertEquals(2, versions.size());
        assertEquals("3.2", versions.get(1).getNumber());
        // The save action doesn't return the latest revision author so we assume the current user is the author.
        assertEquals("Su", versions.get(1).getAuthor().getAbbreviation());

        //
        // Second Tab
        //

        multiUserSetup.switchToBrowserTab(secondTabHandle);
        // Make some changes and save. We'll do the same changes in the first tab afterwards.
        secondEditor.getSourceTextArea().sendKeys(Keys.END, " three");
        inplaceEditablePage.save();

        //
        // First Tab
        //

        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());
        // Make the same changes and save.
        firstTextArea.sendKeys(Keys.END, " three");
        firstEditPage.getToolbar().sendSaveShortcutKey();
        // Verify that we have three revisions listed in the history dropdown.
        versions = historyDropdown.open().getVersions();
        assertEquals(3, versions.size());
        assertEquals("3.3", versions.get(2).getNumber());
        // Again, the current user is not the real author of the new revision but the save action doesn't help us.
        assertEquals("Su", versions.get(2).getAuthor().getAbbreviation());

        // Make some more changes to verify we don't get a merge conflict.
        firstTextArea.clear();
        firstTextArea.sendKeys("final");
        historyPane = firstEditPage.clickDone().openHistoryDocExtraPane().showMinorEdits();
        assertEquals(8, historyPane.getNumberOfVersions());
        assertEquals("4.1", historyPane.getCurrentVersion());
        assertEquals("superadmin", historyPane.getCurrentAuthor());
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
