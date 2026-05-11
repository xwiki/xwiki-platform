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

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WindowType;
import org.xwiki.blocknote.test.po.BlockNoteEditor;
import org.xwiki.blocknote.test.po.BlockNoteRichTextArea;
import org.xwiki.edit.test.po.InplaceEditablePage;
import org.xwiki.test.docker.junit5.MultiUserTestUtils;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.ForceEditLockModal;

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
        secondTextArea.click();
        secondTextArea.sendKeys(Keys.PAGE_DOWN, "Alice:");

        //
        // First Tab
        //

        setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
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
            setup.getDriver().switchTo().window(secondTabHandle);
            secondTextArea.waitUntilFocused().sendKeys(" " + secondUserWords[i]);

            //
            // First Tab
            //
            setup.getDriver().switchTo().window(multiUserSetup.getFirstTabHandle());
            firstTextArea.waitUntilFocused().sendKeys(" " + firstUserWords[i]);
        }

        //
        // Second Tab
        //

        setup.getDriver().switchTo().window(secondTabHandle);
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

    private void editAndForceLock(InplaceEditablePage inplaceEditablePage, String lockedBy, TestUtils setup)
    {
        inplaceEditablePage.edit();
        ForceEditLockModal forceEditLockModal = new ForceEditLockModal();
        setup.getDriver().waitUntilCondition(driver -> forceEditLockModal.isDisplayed());
        assertThat(forceEditLockModal.getMessage(), containsString("This page is currently locked by " + lockedBy));
        forceEditLockModal.clickOk();
        inplaceEditablePage.waitForInplaceEditor();
    }
}
