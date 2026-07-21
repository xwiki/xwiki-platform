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
import org.xwiki.blocknote.test.po.BlockNoteEditor;
import org.xwiki.blocknote.test.po.BlockNoteRichTextArea;
import org.xwiki.edit.test.po.InplaceEditablePage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verify link related features of the BlockNote editor integration.
 *
 * @version $Id$
 * @since 18.6.0RC1
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
class LinkIT extends AbstractBlockNoteIT
{
    @Test
    @Order(1)
    void createLinkOnWordInTheMiddleOfALine(TestUtils setup, TestReference testReference)
    {
        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "First second third fourth");

        InplaceEditablePage page = new InplaceEditablePage().editInplace();

        BlockNoteEditor editor = new BlockNoteEditor("content");
        BlockNoteRichTextArea textArea = editor.getRichTextArea();

        // Select the word "second" using the keyboard.
        textArea.click();
        selectWord(textArea, 6, 6);

        // Create a link targeting a URL from the selection.
        editor.getToolBar().createLink().setTargetAndSubmit("https://xwiki.org");

        // The link must be inserted on the selected word without touching the rest of the line.
        textArea.waitUntilTextIs("First second third fourth");

        // Save and check the source.
        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("First [[second>>https://xwiki.org]] third fourth", wikiEditor.getContent());
    }

    @Test
    @Order(2)
    void editLinkTitle(TestUtils setup, TestReference testReference)
    {
        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "First [[second>>https://xwiki.org]] third fourth");

        InplaceEditablePage page = new InplaceEditablePage().editInplace();

        BlockNoteEditor editor = new BlockNoteEditor("content");
        BlockNoteRichTextArea textArea = editor.getRichTextArea();

        // Move the caret inside the link, using the keyboard, to trigger the link toolbar (we can't click the link
        // because the browser would follow it, and hovering it with synthetic mouse events is not reliable), then
        // change the link title.
        textArea.click();
        textArea.sendKeys(Keys.HOME);
        textArea.sendKeys(Keys.ARROW_RIGHT.toString().repeat(8));
        editor.getToolBar().editLink().setTitleAndSubmit("2nd");

        // The link title must be updated without touching the rest of the line.
        textArea.waitUntilTextIs("First 2nd third fourth");

        // Save and check the source.
        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("First [[2nd>>https://xwiki.org]] third fourth", wikiEditor.getContent());
    }

    @Test
    @Order(3)
    void editGeneratedLinkLabel(TestUtils setup, TestReference testReference)
    {
        // Start fresh.
        setup.deletePage(testReference);

        // Create a page with two links having generated labels. We'll edit only one of them to verify that generated
        // labels that are not modified are not persisted.
        setup.createPage(testReference, "one [[Users.Alice]] two [[Users.Bob]] three");

        InplaceEditablePage page = new InplaceEditablePage().editInplace();
        BlockNoteEditor editor = new BlockNoteEditor("content");
        BlockNoteRichTextArea textArea = editor.getRichTextArea();

        // Place the caret after the first link.
        textArea.click().sendKeys(Keys.HOME).sendKeys(Keys.chord(Keys.CONTROL, Keys.RIGHT, Keys.RIGHT));

        // Change the generated link label
        textArea.sendKeys(Keys.LEFT, Keys.BACK_SPACE, "z");

        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("one [[Alize>>Users.Alice]] two [[Users.Bob]] three", wikiEditor.getContent());
    }

    /**
     * Selects a word with the keyboard, assuming the caret is at the start of the line.
     *
     * @param textArea the rich text area to select in
     * @param wordOffset the number of characters between the start of the line and the word
     * @param wordLength the length of the word
     */
    private void selectWord(BlockNoteRichTextArea textArea, int wordOffset, int wordLength)
    {
        textArea.sendKeys(Keys.HOME);
        textArea.sendKeys(Keys.ARROW_RIGHT.toString().repeat(wordOffset));
        textArea.sendKeys(Keys.chord(Keys.SHIFT, Keys.ARROW_RIGHT.toString().repeat(wordLength)));
    }
}
