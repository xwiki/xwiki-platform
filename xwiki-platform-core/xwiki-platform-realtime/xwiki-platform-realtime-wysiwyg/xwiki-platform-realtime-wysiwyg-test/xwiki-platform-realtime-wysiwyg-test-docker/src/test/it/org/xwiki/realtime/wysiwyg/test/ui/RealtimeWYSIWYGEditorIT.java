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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Keys;
import org.openqa.selenium.Point;
import org.openqa.selenium.WindowType;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.administration.test.po.LocalizationAdministrationSectionPage;
import org.xwiki.ckeditor.test.po.AutocompleteDropdown;
import org.xwiki.ckeditor.test.po.MacroDialogEditModal;
import org.xwiki.ckeditor.test.po.image.ImageDialogEditModal;
import org.xwiki.ckeditor.test.po.image.ImageDialogSelectModal;
import org.xwiki.edit.test.po.InplaceEditablePage;
import org.xwiki.flamingo.skin.test.po.EditConflictModal;
import org.xwiki.flamingo.skin.test.po.EditConflictModal.ConflictChoice;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.realtime.wysiwyg.test.po.RealtimeCKEditor;
import org.xwiki.realtime.wysiwyg.test.po.RealtimeCKEditorToolBar.Coeditor;
import org.xwiki.realtime.wysiwyg.test.po.RealtimeRichTextAreaElement;
import org.xwiki.realtime.wysiwyg.test.po.RealtimeRichTextAreaElement.CoeditorPosition;
import org.xwiki.realtime.wysiwyg.test.po.RealtimeWYSIWYGEditPage;
import org.xwiki.test.docker.junit5.MultiUserTestUtils;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

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

        // Verify that the Allow Realtime Collaboration checkbox is checked.
        assertTrue(editPage.isRealtimeEditing());

        // Verify that the Preview button is hidden.
        assertFalse(editPage.hasPreviewButton());

        // The Autosave checkbox is also hidden because autosave is done by the realtime editor (you can't disable it
        // while editing in realtime).
        assertFalse(editPage.getAutoSaveCheckbox().isDisplayed());

        // Verify that we're editing alone.
        assertTrue(editor.getToolBar().isEditingAlone());

        // The Source button is currently disabled.
        assertFalse(editor.getToolBar().canToggleSourceMode());

        RealtimeRichTextAreaElement textArea = editor.getRichTextArea();
        textArea.sendKeys("one");

        // Verify the cursor indicator on the left of the editing area.
        CoeditorPosition selfPosition = textArea.getCoeditorPosition(editor.getToolBar().getUserId());
        assertEquals("John", selfPosition.getAvatarHint());
        assertTrue(selfPosition.getAvatarURL().contains("noavatar.png"),
            "Unexpected avatar URL: " + selfPosition.getAvatarURL());
        selfPosition.waitForLocation(new Point(4, 18));

        assertEquals(1, textArea.getCoeditorPositions().size());

        // Verify that the cursor indicator is updated when typing.
        textArea.sendKeys(Keys.ENTER, "two");
        selfPosition.waitForLocation(new Point(4, 48));

        // Verify the action buttons (Save and Cancel).
        editPage.clickSaveAndContinue();
        textArea.sendKeys(Keys.ENTER, "three");
        ViewPage viewPage = editPage.clickCancel();
        assertEquals("one\ntwo", viewPage.getContent());

        // Edit again and verify the Save and View button.
        viewPage.edit();
        InplaceEditablePage inplaceEditablePage = new InplaceEditablePage();
        new RealtimeCKEditor().getRichTextArea().sendKeys(Keys.ARROW_DOWN, Keys.END, Keys.ENTER, "three");
        viewPage = inplaceEditablePage.saveAndView();
        assertEquals("one\ntwo\nthree", viewPage.getContent());

        // Edit again to verify the autosave.
        viewPage.edit();
        inplaceEditablePage = new InplaceEditablePage();
        editor = new RealtimeCKEditor();
        textArea = editor.getRichTextArea();
        textArea.sendKeys(Keys.chord(Keys.SHIFT, Keys.END));
        textArea.sendKeys("zero");

        // Wait for auto-save.
        String saveStatus = editor.getToolBar().waitForAutoSave();
        assertTrue(saveStatus.startsWith("Saved:"), "Unexpected save status: " + saveStatus);

        viewPage = inplaceEditablePage.cancel();
        assertEquals("zero\ntwo\nthree", viewPage.getContent());
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
        String firstCoeditorId = firstEditor.getToolBar().getUserId();

        //
        // Second Tab
        //

        String secondTabHandle = setup.getDriver().switchTo().newWindow(WindowType.TAB).getWindowHandle();

        // Edit the page in the second browser tab.
        RealtimeWYSIWYGEditPage secondEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor secondEditor = secondEditPage.getContenEditor();
        RealtimeRichTextAreaElement secondTextArea = secondEditor.getRichTextArea();
        String secondCoeditorId = secondEditor.getToolBar().getUserId();

        // Verify the list of coeditors.
        List<Coeditor> coeditors = secondEditor.getToolBar().waitForCoeditor(firstCoeditorId).getCoeditors();
        assertEquals(1, coeditors.size());
        Coeditor self = coeditors.get(0);
        assertEquals("John", self.getName());
        assertEquals("xwiki:XWiki.John", self.getReference());
        assertTrue(setup.getURL(new DocumentReference("xwiki", "XWiki", "John")).endsWith(self.getURL()));
        assertTrue(self.getAvatarURL().contains("noavatar.png"), "Unexpected avatar URL: " + self.getAvatarURL());
        assertEquals("John", self.getAvatarHint());

        // Verify the placeholder text is present, because the content is empty.
        secondTextArea.waitForPlaceholder("Start typing here...");

        //
        // First Tab
        //

        // Switch back to the first tab and verify the list of coeditors.
        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
        coeditors = firstEditor.getToolBar().waitForCoeditor(secondCoeditorId).getCoeditors();
        assertEquals(1, coeditors.size());
        self = coeditors.get(0);
        assertEquals("John", self.getName());
        assertEquals("xwiki:XWiki.John", self.getReference());
        assertTrue(setup.getURL(new DocumentReference("xwiki", "XWiki", "John")).endsWith(self.getURL()));
        assertTrue(self.getAvatarURL().contains("noavatar.png"), "Unexpected avatar URL: " + self.getAvatarURL());
        assertEquals("John", self.getAvatarHint());

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
            secondTextArea.getCoeditorPosition(firstCoeditorId).waitForLocation(new Point(4, 78));
        assertEquals("John", firstPosition.getAvatarHint());
        assertTrue(firstPosition.getAvatarURL().contains("noavatar.png"),
            "Unexpected avatar URL: " + firstPosition.getAvatarURL());

        // The second user is on the first line (paragraph).
        CoeditorPosition secondPosition =
            secondTextArea.getCoeditorPosition(secondCoeditorId).waitForLocation(new Point(4, 18));
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
        firstTextArea.getCoeditorPosition(secondCoeditorId).waitForLocation(new Point(4, 48));

        // Verify that clicking on the coeditor indicator scrolls the editing area to the coeditor position.
        // But first we need to add enough paragraphs to make the editing area scrollable.
        for (int i = 0; i< 20; i++) {
            firstTextArea.sendKeys(Keys.ENTER);
        }
        firstTextArea.sendKeys("end");

        //
        // Second Tab
        //

        // Switch to the second tab and click on the coeditor indicator.
        setup.getDriver().switchTo().window(secondTabHandle);
        secondTextArea.waitUntilTextContains("end");
        firstPosition = secondTextArea.getCoeditorPosition(firstCoeditorId).waitForLocation(new Point(4, 18 + 22 * 30));
        assertFalse(firstPosition.isVisible(), "The coeditor position is visible before scrolling.");
        secondEditor.getToolBar().getCoeditor(firstCoeditorId).click();
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
        secondTextArea.waitUntilMacrosAreRendered();

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
        firstTextArea.waitUntilMacrosAreRendered();
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
        ViewPage viewPage = secondEditPage.clickSaveAndView();
        assertEquals("my info message\none two three", viewPage.getContent());
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

        String firstUserText = "The five boxing wizards jump quickly. The quick brown fox jumps over the lazy dog. First";
        String secondUserText = "The quick brown fox jumps over the lazy dog. The five boxing wizards jump quickly. Second";

        String[] firstUserWords = firstUserText.split(" ");
        String[] secondUserWords = secondUserText.split(" ");

        for(int i = 0; i < Math.min(firstUserWords.length, secondUserWords.length); i++) {
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

        ViewPage viewPage = secondEditPage.clickSaveAndView();
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

        for(int i = 0; i < words.length; i++) {
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

        ViewPage viewPage = secondEditPage.clickSaveAndView();
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
    void imageWithCaption(TestReference testReference, TestUtils setup, MultiUserTestUtils multiUserSetup) throws Exception
    {
        // Start fresh.
        setup.deletePage(testReference);

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
        firstEditPage.clickSaveAndView();
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
        firstTextArea.waitUntilMacrosAreRendered();

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
        firstMacroEditModal.clickSubmit();
        firstTextArea.waitUntilMacrosAreRendered();

        // Move to the information box title field and type something.
        firstTextArea.sendKeys(Keys.ARROW_UP, Keys.END, " title");

        //
        // Second Tab
        //

        setup.getDriver().switchTo().window(secondTabHandle);

        secondTextArea.waitUntilTextContains("Some title");
        // Verify that the remote change (which included a macro parameter update) didn't steal the focus.
        secondMacroEditModal.getMacroParameterInput("cssClass").sendKeys("a");
        secondMacroEditModal.clickSubmit();
        secondTextArea.waitUntilMacrosAreRendered();

        // Move to the information box title field and type something.
        secondTextArea.sendKeys(Keys.ARROW_UP, Keys.HOME);
        secondTextArea.sendKeys(Keys.chord(Keys.CONTROL, Keys.ARROW_RIGHT));
        secondTextArea.sendKeys(" cool");

        //
        // First Tab
        //

        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
        firstTextArea.waitUntilTextContains("Some cool title");

        // Edit again the macro an verify that we have the correct parameter value.
        firstMacroEditModal = firstEditor.getBalloonToolBar().editMacro();
        assertEquals("bar", firstMacroEditModal.getMacroParameter("cssClass"));
        firstMacroEditModal.clickCancel();

        firstEditPage.clickSaveAndView();
        assertEquals("{{info cssClass=\"bar\" title=\"Some cool title\"}}\ntwo one\n{{/info}}",
            WikiEditPage.gotoPage(testReference).getContent());
    }

    @Test
    @Order(9)
    void reloadEditorsMergeConflictManualSave(TestReference testReference, TestUtils setup,
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
        firstEditPage.clickSaveAndContinue();

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

        thirdEditPage.leaveRealtimeEditing();

        thirdTextArea.sendKeys(Keys.END, " Third");
        thirdEditPage.clickSaveAndContinue();

        //
        // Second tab
        //

        setup.getDriver().switchTo().window(secondTabHandle);
        secondTextArea.sendKeys(Keys.END, " Second");
        secondEditPage.clickSaveAndContinue(false);

        EditConflictModal editConflictModal = new EditConflictModal();
        editConflictModal.makeChoiceAndSubmit(ConflictChoice.RELOAD, false);

        secondTextArea.waitUntilTextContains("Third");

        //
        // First tab
        //

        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
        firstTextArea.waitUntilTextContains("Third");
        assertEquals("First Third", firstTextArea.getText());
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
        firstEditPage.clickSaveAndContinue();

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

        thirdEditPage.leaveRealtimeEditing();

        thirdTextArea.sendKeys(Keys.END, Keys.ENTER, "Third");
        thirdEditPage.clickSaveAndContinue();

        //
        // Second tab
        //

        setup.getDriver().switchTo().window(secondTabHandle);
        secondEditPage.clickSaveAndContinue();
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
        firstTextArea.waitUntilMacrosAreRendered();

        // Select the default information message and delete it.
        firstTextArea.sendKeys(Keys.chord(Keys.SHIFT, Keys.END), Keys.BACK_SPACE);

        // Insert a nested error box.
        firstTextArea.sendKeys("inside", Keys.ENTER, "/err");
        qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/err", "Error Box");
        firstTextArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();
        firstTextArea.waitUntilMacrosAreRendered();

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
        firstEditPage.leaveRealtimeEditing();

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

        // Insert an empty Velocity macro.
        secondTextArea.sendKeys("before", Keys.RETURN);
        secondTextArea.sendKeys("/velo");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/velo", "Velocity");
        secondTextArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();
        secondTextArea.waitUntilMacrosAreRendered();
        String text = secondTextArea.getText();
        assertFalse(text.contains("Failed"), "Unexpected text content: " + text);

        // Edit the inserted Velocity macro.
        secondTextArea.sendKeys(Keys.ENTER);
        new MacroDialogEditModal().waitUntilReady().setMacroContent("$xcontext.userReference.name").clickSubmit();
        secondTextArea.waitUntilTextContains("superadmin");
        text = secondTextArea.getText();
        assertFalse(text.contains("Failed"), "Unexpected text content: " + text);

        //
        // First Tab
        //

        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());
        // Even if John didn't make any changes yet, the script macro is executed with the minimum script rights
        // between the current user (John) and the script author associated with the realtime session (superadmin
        // currently).
        firstTextArea.waitUntilTextContains("Failed to execute the [velocity] macro.");

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
        // This time the script macro is not executed because John has been associated as script author of the realtime
        // session.
        secondTextArea.waitUntilTextContains("Failed to execute the [velocity] macro.");
        text = secondTextArea.getText();
        assertFalse(text.contains("User: superadmin"), "Unexpected text content: " + text);

        secondTextArea.sendKeys(Keys.ARROW_LEFT, Keys.chord(Keys.CONTROL, Keys.SHIFT, Keys.ARROW_LEFT));
        secondTextArea.sendKeys("lunch");

        //
        // First Tab
        //

        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());
        firstTextArea.waitUntilTextContains("lunch");

        // Try to inject a script macro.
        firstTextArea.sendKeys(Keys.HOME);
        firstTextArea.sendKeys(Keys.chord(Keys.CONTROL, Keys.ARROW_RIGHT));
        firstTextArea.sendKeys(Keys.ENTER, Keys.ENTER, Keys.ARROW_UP, "/velo");
        qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/velo", "Velocity");
        firstTextArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();
        firstTextArea.waitUntilMacrosAreRendered();

        // Edit the inserted Velocity macro to add some script.
        firstTextArea.sendKeys(Keys.ARROW_RIGHT, Keys.ENTER);
        new MacroDialogEditModal().waitUntilReady().setMacroContent("injected").clickSubmit();
        firstTextArea.waitUntilMacrosAreRendered();
        text = firstTextArea.getText();
        assertFalse(text.contains("injected"), "Unexpected text content: " + text);

        // Leave the edit mode to see that the script level associated with the realtime session remains the same.
        firstEditPage.clickCancel();

        //
        // Second Tab
        //

        multiUserSetup.switchToBrowserTab(secondTabHandle);

        secondTextArea.waitUntilTextContains("before\nFailed to execute the [velocity] macro.");
        text = secondTextArea.getText();
        assertFalse(text.contains("injected"), "Unexpected text content: " + text);

        // Edit again the macro to see that the script level doesn't change.
        secondTextArea.sendKeys(Keys.ARROW_RIGHT, Keys.ENTER);
        new MacroDialogEditModal().waitUntilReady().setMacroContent("Current: $xcontext.userReference").clickSubmit();
        secondTextArea.waitUntilMacrosAreRendered();
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

        RealtimeWYSIWYGEditPage firstEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor firstEditor = firstEditPage.getContenEditor();
        RealtimeRichTextAreaElement firstTextArea = firstEditor.getRichTextArea();

        firstTextArea.sendKeys("default content");

        // Save the page so that we can translate it.
        firstTextArea.sendKeys(Keys.chord(Keys.ALT, Keys.SHIFT, "s"));

        //
        // Second Tab
        //

        // Translate the created page in a new tab using the alias so that we don't change the locale of the first tab.
        String secondTabHandle = multiUserSetup.openNewBrowserTab(XWIKI_ALIAS);

        // But first we need to enabled multilingual support.
        setup.loginAsSuperAdmin();
        setMultiLingual(true, "en", "fr", "de");

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
        secondTextArea.waitUntilMacrosAreRendered();

        // Edit the inserted Velocity macro.
        secondTextArea.sendKeys(Keys.ENTER);
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
        firstTextArea.waitUntilTextContains("default content\nFailed to execute the [velocity] macro.");

        // Verify that we're editing alone.
        assertTrue(firstEditor.getToolBar().isEditingAlone());

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
        assertTrue(secondEditor.getToolBar().isEditingAlone());

        assertEquals("French content\nUser: superadmin", secondEditPage.clickSaveAndView().getContent());

        //
        // First Tab
        //

        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());

        firstTextArea.sendKeys(Keys.ARROW_UP, Keys.HOME);
        firstTextArea.sendKeys(Keys.chord(Keys.CONTROL, Keys.SHIFT, Keys.ARROW_RIGHT));
        firstTextArea.sendKeys("English");

        String content = firstEditPage.clickSaveAndView().getContent();
        assertTrue(content.startsWith("English content\nFailed to execute the [velocity] macro."),
            "Unexpected content: " + content);

        // Now edit the same (German) translation.
        setup.gotoPage(testReference, "view", "language=de");
        new InplaceEditablePage().translateInplace();

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

        assertEquals("superadmin", firstEditor.getToolBar().getCoeditors().stream().map(Coeditor::getName)
            .reduce((a, b) -> a + ", " + b).get());
    }

    private void setMultiLingual(boolean isMultiLingual, String... supportedLanguages)
    {
        AdministrationPage adminPage = AdministrationPage.gotoPage();
        LocalizationAdministrationSectionPage sectionPage = adminPage.clickLocalizationSection();
        sectionPage.setMultiLingual(isMultiLingual);
        sectionPage.setDefaultLanguage("en");
        sectionPage.setSupportedLanguages(List.of(supportedLanguages));
        sectionPage.clickSave();
    }
}
