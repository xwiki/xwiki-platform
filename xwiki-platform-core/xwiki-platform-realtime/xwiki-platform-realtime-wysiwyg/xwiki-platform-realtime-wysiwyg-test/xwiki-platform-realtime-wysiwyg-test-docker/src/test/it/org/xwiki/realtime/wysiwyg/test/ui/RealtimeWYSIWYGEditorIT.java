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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Keys;
import org.openqa.selenium.Point;
import org.openqa.selenium.WindowType;
import org.xwiki.ckeditor.test.po.AutocompleteDropdown;
import org.xwiki.ckeditor.test.po.image.ImageDialogEditModal;
import org.xwiki.ckeditor.test.po.image.ImageDialogSelectModal;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.realtime.wysiwyg.test.po.RealtimeCKEditor;
import org.xwiki.realtime.wysiwyg.test.po.RealtimeCKEditorToolBar.Coeditor;
import org.xwiki.realtime.wysiwyg.test.po.RealtimeRichTextAreaElement;
import org.xwiki.realtime.wysiwyg.test.po.RealtimeRichTextAreaElement.CoeditorPosition;
import org.xwiki.realtime.wysiwyg.test.po.RealtimeWYSIWYGEditPage;
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
        "xwikiDbHbmCommonExtraMappings=notification-filter-preferences.hbm.xml"
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
    }
)
class RealtimeWYSIWYGEditorIT extends AbstractRealtimeWYSIWYGEditorIT
{
    @Test
    @Order(1)
    void editAlone(TestReference testReference, TestUtils setup) throws Exception
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
        List<CoeditorPosition> coeditorPositions = textArea.getCoeditorPositions();
        assertEquals(1, coeditorPositions.size());

        CoeditorPosition selfPosition = coeditorPositions.get(0);
        assertEquals("John", selfPosition.getAvatarHint());
        assertTrue(selfPosition.getAvatarURL().contains("noavatar.png"),
            "Unexpected avatar URL: " + selfPosition.getAvatarURL());
        selfPosition.waitForLocation(new Point(3, 18));

        // Verify that the cursor indicator is updated when typing.
        textArea.sendKeys(Keys.ENTER, "two");
        selfPosition.waitForLocation(new Point(3, 48));

        // Verify the action buttons (Save and Cancel).
        editPage.clickSaveAndContinue();
        textArea.sendKeys(Keys.ENTER, "three");
        ViewPage viewPage = editPage.clickCancel();
        assertEquals("one\ntwo", viewPage.getContent());

        // Edit again and verify the Save and View button.
        viewPage.edit();
        editPage = new RealtimeWYSIWYGEditPage();
        editPage.getContenEditor().getRichTextArea().sendKeys(Keys.ARROW_DOWN, Keys.END, Keys.ENTER, "three");
        viewPage = editPage.clickSaveAndView();
        assertEquals("one\ntwo\nthree", viewPage.getContent());

        // Edit again to verify the autosave.
        viewPage.edit();
        editPage = new RealtimeWYSIWYGEditPage();
        textArea = editPage.getContenEditor().getRichTextArea();
        textArea.sendKeys(Keys.chord(Keys.SHIFT, Keys.END));
        textArea.sendKeys("zero");

        // Wait for auto-save.
        String saveStatus = editor.getToolBar().waitForAutoSave();
        assertTrue(saveStatus.startsWith("Saved:"), "Unexpected save status: " + saveStatus);

        viewPage = editPage.clickCancel();
        assertEquals("zero\ntwo\nthree", viewPage.getContent());
    }

    @Test
    @Order(2)
    void editWithSelf(TestReference testReference, TestUtils setup)
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
        String firstCoeditorId = firstTextArea.getCoeditorPositions().get(0).getCoeditorId();

        //
        // Second Tab
        //

        String secondTabHandle = setup.getDriver().switchTo().newWindow(WindowType.TAB).getWindowHandle();

        // Edit the page in the second browser tab.
        RealtimeWYSIWYGEditPage secondEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor secondEditor = secondEditPage.getContenEditor();
        RealtimeRichTextAreaElement secondTextArea = secondEditor.getRichTextArea();

        // FIXME: We need a simpler method to get the user ID in the real-time session.
        String secondCoeditorId = secondTextArea.getCoeditorPositions().stream()
            .filter(position -> !firstCoeditorId.equals(position.getCoeditorId())).findFirst().get().getCoeditorId();

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
        assertEquals("Start typing here...", secondTextArea.getPlaceholder());

        //
        // First Tab
        //

        // Switch back to the first tab and verify the list of coeditors.
        setup.getDriver().switchTo().window(firstTabHandle);
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
        secondTextArea.waitUntilContentContains("three");

        // There should be no placeholder text anymore because the content is not empty.
        assertNull(secondTextArea.getPlaceholder());
        assertEquals("one\ntwo\nthree", secondTextArea.getText());

        secondTextArea.sendKeys(Keys.chord(Keys.SHIFT, Keys.END));
        secondTextArea.sendKeys("zero");

        // Verify the caret indicators on the left of the editing area.

        // The first user is on the third line (paragraph).
        CoeditorPosition firstPosition =
            secondTextArea.getCoeditorPosition(firstCoeditorId).waitForLocation(new Point(3, 78));
        assertEquals("John", firstPosition.getAvatarHint());
        assertTrue(firstPosition.getAvatarURL().contains("noavatar.png"),
            "Unexpected avatar URL: " + firstPosition.getAvatarURL());

        // The second user is on the first line (paragraph).
        CoeditorPosition secondPosition =
            secondTextArea.getCoeditorPosition(secondCoeditorId).waitForLocation(new Point(3, 18));
        assertEquals("John", secondPosition.getAvatarHint());
        assertTrue(secondPosition.getAvatarURL().contains("noavatar.png"),
            "Unexpected avatar URL: " + secondPosition.getAvatarURL());

        assertEquals(2, secondTextArea.getCoeditorPositions().size());

        // Verify that the caret indicator is updated on the first editor.
        secondTextArea.sendKeys(Keys.ARROW_DOWN, Keys.HOME);

        //
        // First Tab
        //

        setup.getDriver().switchTo().window(firstTabHandle);
        firstTextArea.getCoeditorPosition(secondCoeditorId).waitForLocation(new Point(3, 48));

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
        secondTextArea.waitUntilContentContains("end");
        firstPosition = secondTextArea.getCoeditorPosition(firstCoeditorId).waitForLocation(new Point(3, 18 + 22 * 30));
        assertFalse(firstPosition.isVisible(), "The coeditor position is visible before scrolling.");
        secondEditor.getToolBar().getCoeditor(firstCoeditorId).click();
        assertTrue(firstPosition.isVisible(), "The coeditor position is not visible after scrolling.");
    }

    @Test
    @Order(3)
    void inplaceEditableMacro(TestReference testReference, TestUtils setup)
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

        secondTextArea.waitUntilContentContains("one");
        secondTextArea.sendKeys("/info");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/info", "Info Box");
        secondTextArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        // The content is reloaded when a macro is inserted.
        secondTextArea = secondEditor.getRichTextArea();
        secondTextArea.waitUntilContentEditable();
        // Replace the default message text.
        secondTextArea.sendKeys(Keys.chord(Keys.SHIFT, Keys.END), Keys.BACK_SPACE);
        secondTextArea.sendKeys("my info tex");
        secondTextArea.sendKeys(Keys.chord(Keys.SHIFT, Keys.ARROW_LEFT, Keys.ARROW_LEFT, Keys.ARROW_LEFT));

        //
        // First Tab
        //

        setup.getDriver().switchTo().window(firstTabHandle);

        // Continue typing to verify that the selection is not lost in the second tab.
        // Notice that we don't wait for the content to be updated because we want to verfiy that this can happen while
        // we are typing.
        firstTextArea.sendKeys(" two");

        //
        // Second Tab
        //

        setup.getDriver().switchTo().window(secondTabHandle);

        // Again, we don't wait for the content to be updated, because we don't need to. The content should be
        // synchronized while we are typing.
        secondTextArea.sendKeys("message");

        //
        // First Tab
        //

        setup.getDriver().switchTo().window(firstTabHandle);

        firstTextArea.sendKeys(" three");

        //
        // Second Tab
        //

        setup.getDriver().switchTo().window(secondTabHandle);

        // Wait for the content to be synchronized before saving, otherwise we might save partial content and, more
        // importantly, we could trigger the leave confirmation (if the content is synchronized after the content dirty
        // flag is set to false by the action buttons listener).
        secondTextArea.waitUntilContentContains("three");

        // Save and check the result.
        ViewPage viewPage = secondEditPage.clickSaveAndView();
        assertEquals("my info message\none two three", viewPage.getContent());
    }

    @Test
    @Order(4)
    void editDifferentParagraphs(TestReference testReference, TestUtils setup)
    {
        //
        // First Tab
        //

        // Start fresh.
        setup.deletePage(testReference);

        RealtimeWYSIWYGEditPage firstEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor firstEditor = firstEditPage.getContenEditor();
        RealtimeRichTextAreaElement firstTextArea = firstEditor.getRichTextArea();
        firstTextArea.sendKeys("Start. ");

        //
        // Second Tab
        //

        String secondTabHandle = setup.getDriver().switchTo().newWindow(WindowType.TAB).getWindowHandle();

        RealtimeWYSIWYGEditPage secondEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor secondEditor = secondEditPage.getContenEditor();
        RealtimeRichTextAreaElement secondTextArea = secondEditor.getRichTextArea();

        // Each user types in their own paragraph.
        secondTextArea.waitUntilContentContains("Start. ");
        secondTextArea.sendKeys(Keys.END, Keys.ENTER);

        String firstUserText = "The five boxing wizards jump quickly. The quick brown fox jumps over the lazy dog. First";
        String secondUserText = "The quick brown fox jumps over the lazy dog. The five boxing wizards jump quickly. Second";

        String[] firstUserWords = firstUserText.split(" ");
        String[] secondUserWords = secondUserText.split(" ");

        for(int i = 0; i < Math.min(firstUserWords.length, secondUserWords.length); i++) {
            //
            // First Tab
            //
            setup.getDriver().switchTo().window(firstTabHandle);
            firstTextArea.sendKeys(firstUserWords[i] + " ");

            //
            // Second Tab
            //
            setup.getDriver().switchTo().window(secondTabHandle);
            secondTextArea.sendKeys(secondUserWords[i] + " ");
        }

        // Wait to receive all the content typed by the first user.
        secondTextArea.waitUntilContentContains("First");

        ViewPage viewPage = secondEditPage.clickSaveAndView();
        assertEquals("Start. " + firstUserText + "\n" + secondUserText, viewPage.getContent());
    }

    @Test
    @Order(5)
    void editSameParagraph(TestReference testReference, TestUtils setup)
    {
        //
        // First Tab
        //

        // Start fresh.
        setup.deletePage(testReference);

        RealtimeWYSIWYGEditPage firstEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor firstEditor = firstEditPage.getContenEditor();
        RealtimeRichTextAreaElement firstTextArea = firstEditor.getRichTextArea();
        firstTextArea.sendKeys("Separator. ", Keys.HOME);

        //
        // Second Tab
        //

        String secondTabHandle = setup.getDriver().switchTo().newWindow(WindowType.TAB).getWindowHandle();

        RealtimeWYSIWYGEditPage secondEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor secondEditor = secondEditPage.getContenEditor();
        RealtimeRichTextAreaElement secondTextArea = secondEditor.getRichTextArea();

        // Each user types in their own paragraph.
        secondTextArea.waitUntilContentContains("Separator. ");
        secondTextArea.sendKeys(Keys.END);

        String text = "The quick brown fox jumps over the lazy dog.";
        String[] words = text.split(" ");

        for(int i = 0; i < words.length; i++) {
            //
            // First Tab
            //
            setup.getDriver().switchTo().window(firstTabHandle);
            firstTextArea.sendKeys(words[i] + " ");

            //
            // Second Tab
            //
            setup.getDriver().switchTo().window(secondTabHandle);
            secondTextArea.sendKeys(words[i] + " ");
        }

        //
        // First Tab
        //
        setup.getDriver().switchTo().window(firstTabHandle);
        firstTextArea.sendKeys("First. ");

        //
        // Second Tab
        //
        setup.getDriver().switchTo().window(secondTabHandle);
        // Wait to receive all the content typed by the first user.
        secondTextArea.waitUntilContentContains("First.");

        ViewPage viewPage = secondEditPage.clickSaveAndView();
        assertEquals(text + " First. Separator. " + text, viewPage.getContent());
    }

    @Test
    @Order(6)
    void applyInlineStylesOnTheSameParagraph(TestReference testReference, TestUtils setup)
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

        secondTextArea.waitUntilContentContains("italic");
        secondTextArea.sendKeys(Keys.END, " underline");
        // Select the "underline" word.
        secondTextArea.sendKeys(Keys.chord(Keys.SHIFT, Keys.CONTROL, Keys.ARROW_LEFT));

        //
        // First Tab
        //

        setup.getDriver().switchTo().window(firstTabHandle);

        // If we don't wait then the italic style might be applied before the "underline" word is retrieved, which leads
        // to the "underline" word being inserted inside the italic style.
        firstTextArea.waitUntilContentContains("underline");

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

        setup.getDriver().switchTo().window(firstTabHandle);
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

        setup.getDriver().switchTo().window(firstTabHandle);
        firstTextArea.sendKeys(Keys.ARROW_RIGHT, "er");

        firstTextArea.waitUntilContentContains("end");
        String content = firstTextArea.getContent();
        assertTrue(content.contains("<strong>bolder</strong> <em>italic</em> <ins>underline</ins> end"),
            "Unexpected content: " + content);
    }

    @Test
    @Order(7)
    void imageWithCaption(TestReference testReference, TestUtils setup) throws Exception
    {
        // Start fresh.
        setup.deletePage(testReference);

        // Upload an image to test with.
        String attachmentName = "image.gif";
        AttachmentReference attachmentReference = new AttachmentReference(attachmentName, testReference);
        setup.createPage(testReference, "", "");
        setup.attachFile(testReference, attachmentName, getClass().getResourceAsStream("/" + attachmentName), false);

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

        String secondTabHandle = setup.getDriver().switchTo().newWindow(WindowType.TAB).getWindowHandle();

        RealtimeWYSIWYGEditPage secondEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor secondEditor = secondEditPage.getContenEditor();
        RealtimeRichTextAreaElement secondTextArea = secondEditor.getRichTextArea();
        secondTextArea.waitUntilContentContains("before");
        secondTextArea.sendKeys(Keys.END, Keys.ENTER);

        // Insert the image with caption.
        ImageDialogSelectModal imageDialogSelectModal = secondEditor.getToolBar().insertImage();
        imageDialogSelectModal.switchToTreeTab().selectAttachment(attachmentReference);
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

        setup.getDriver().switchTo().window(firstTabHandle);
        firstTextArea.waitUntilContentContains("Tree");
        firstTextArea.sendKeys(Keys.ARROW_DOWN, Keys.HOME, "Small ");

        //
        // Second Tab
        //

        setup.getDriver().switchTo().window(secondTabHandle);
        secondTextArea.waitUntilContentContains("Small");
        secondTextArea.sendKeys(Keys.chord(Keys.CONTROL, Keys.SHIFT, Keys.ARROW_LEFT));
        secondTextArea.sendKeys(Keys.chord(Keys.CONTROL, "b"));

        //
        // First Tab
        //

        setup.getDriver().switchTo().window(firstTabHandle);
        firstTextArea.sendKeys(Keys.ARROW_LEFT, "est");
        firstTextArea.waitUntilContentContains("<strong>Tree</strong>");
        firstEditPage.clickSaveAndView();
        assertEquals("before\n\n[[Smallest **Tree**>>image:image.gif]]\n\n ",
            WikiEditPage.gotoPage(testReference).getContent());
    }
}
