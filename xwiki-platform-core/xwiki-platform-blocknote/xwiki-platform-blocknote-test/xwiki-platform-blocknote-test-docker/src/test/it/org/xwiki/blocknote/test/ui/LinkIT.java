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
import org.xwiki.blocknote.test.po.BlockNoteLinkModal;
import org.xwiki.blocknote.test.po.BlockNoteRichTextArea;
import org.xwiki.edit.test.po.InplaceEditablePage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.repository.test.SolrTestUtils;
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
        "xwikiCfgPlugins=com.xpn.xwiki.plugin.fileupload.FileUploadPlugin",

        // The page and attachment link suggestions execute a Solr query from XWiki.SuggestSolrService /
        // XWiki.SuggestSolrMacros, which requires programming right. These pages are authored by a user that has
        // programming right in a normal wiki, but the test wiki blocks programming right by default, so we need to
        // explicitly exclude them.
        "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:XWiki\\.SuggestSolr(Service|Macros)"
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
        // change the link title. We wait for the rich text area to be focused before sending the keys, otherwise
        // they can be silently dropped (e.g. the HOME key) if sent right after the click, before the click's focus
        // has actually settled.
        textArea.click();
        textArea.waitUntilFocused();
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
    void cancelLinkCreation(TestUtils setup, TestReference testReference)
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

        // Open the link modal, fill it in, but cancel it instead of submitting.
        BlockNoteLinkModal linkModal = editor.getToolBar().createLink();
        linkModal.selectTargetType("URL");
        linkModal.setUrl("https://xwiki.org");
        linkModal.cancel();

        // The content must be left untouched.
        textArea.waitUntilTextIs("First second third fourth");

        // Save and check the source: since nothing was actually changed in the editor, the page content is left
        // untouched (i.e. it is not even round-tripped through the editor, hence the lack of style annotation that a
        // real edit would introduce).
        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("First second third fourth", wikiEditor.getContent());
    }

    @Test
    @Order(4)
    void cancelLinkEdition(TestUtils setup, TestReference testReference)
    {
        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "First [[second>>https://xwiki.org]] third fourth");

        InplaceEditablePage page = new InplaceEditablePage().editInplace();

        BlockNoteEditor editor = new BlockNoteEditor("content");
        BlockNoteRichTextArea textArea = editor.getRichTextArea();

        // Move the caret inside the link, using the keyboard, to trigger the link toolbar, then open the link modal,
        // change the title, but cancel the modal instead of submitting. We wait for the rich text area to be
        // focused before sending the keys, otherwise they can be silently dropped (e.g. the HOME key) if sent right
        // after the click, before the click's focus has actually settled.
        textArea.click();
        textArea.waitUntilFocused();
        textArea.sendKeys(Keys.HOME);
        textArea.sendKeys(Keys.ARROW_RIGHT.toString().repeat(8));
        BlockNoteLinkModal linkModal = editor.getToolBar().editLink();
        linkModal.setDisplayText("2nd");
        linkModal.cancel();

        // The link title must be left untouched.
        textArea.waitUntilTextIs("First second third fourth");

        // Save and check the source: since nothing was actually changed in the editor, the page content is left
        // untouched (i.e. it is not even round-tripped through the editor, hence the lack of style annotation that a
        // real edit would introduce).
        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("First [[second>>https://xwiki.org]] third fourth", wikiEditor.getContent());
    }

    @Test
    @Order(5)
    void createPageLink(TestUtils setup, TestReference testReference) throws Exception
    {
        // Create the page to link to and wait for it to be indexed, since the page link suggestions are based on
        // Solr indexation.
        DocumentReference targetPage = new DocumentReference("PageLinkTarget", testReference.getLastSpaceReference());
        setup.deletePage(targetPage);
        setup.createPage(targetPage, "", "Page Link Target");
        new SolrTestUtils(setup).waitEmptyQueue();

        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "First second third fourth");

        InplaceEditablePage page = new InplaceEditablePage().editInplace();

        BlockNoteEditor editor = new BlockNoteEditor("content");
        BlockNoteRichTextArea textArea = editor.getRichTextArea();

        // Select the word "second" using the keyboard.
        textArea.click();
        selectWord(textArea, 6, 6);

        // Create a link targeting a page from the selection. Search by title since suggestions are matched (and
        // rendered) using the page title, not its reference.
        editor.getToolBar().createLink().setPageTargetAndSubmit("Page Link Target", "Page Link Target");

        // The link must be inserted on the selected word without touching the rest of the line.
        textArea.waitUntilTextIs("First second third fourth");

        // Save and check the source.
        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("First [[second>>doc:%s]] third fourth".formatted(setup.serializeReference(targetPage)),
            wikiEditor.getContent());
    }

    @Test
    @Order(6)
    void createAttachmentLink(TestUtils setup, TestReference testReference) throws Exception
    {
        // Create the page holding the attachment to link to and wait for it to be indexed, since the attachment
        // link suggestions are based on Solr indexation.
        DocumentReference targetPage =
            new DocumentReference("AttachmentLinkTarget", testReference.getLastSpaceReference());
        setup.deletePage(targetPage);
        setup.createPage(targetPage, "", "Attachment Link Target");
        String attachmentName = "image.gif";
        setup.attachFile(targetPage, attachmentName, getClass().getResourceAsStream('/' + attachmentName), false);
        new SolrTestUtils(setup).waitEmptyQueue();

        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "First second third fourth");

        InplaceEditablePage page = new InplaceEditablePage().editInplace();

        BlockNoteEditor editor = new BlockNoteEditor("content");
        BlockNoteRichTextArea textArea = editor.getRichTextArea();

        // Select the word "second" using the keyboard.
        textArea.click();
        selectWord(textArea, 6, 6);

        // Create a link targeting an attachment from the selection. Search by filename (as a real user would), but
        // disambiguate the suggestion to select using the target page name, since other tests running in the same
        // wiki may also attach a file with the same name.
        editor.getToolBar().createLink().setAttachmentTargetAndSubmit(attachmentName, targetPage.getName());

        // The link must be inserted on the selected word without touching the rest of the line.
        textArea.waitUntilTextIs("First second third fourth");

        // Save and check the source.
        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("First [[second>>attach:%s@%s]] third fourth".formatted(setup.serializeReference(targetPage),
            attachmentName), wikiEditor.getContent());
    }

    @Test
    @Order(7)
    void createEmailLink(TestUtils setup, TestReference testReference)
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

        // Create a link targeting an e-mail address from the selection.
        editor.getToolBar().createLink().setEmailTargetAndSubmit("second@xwiki.org");

        // The link must be inserted on the selected word without touching the rest of the line.
        textArea.waitUntilTextIs("First second third fourth");

        // Save and check the source.
        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("First [[second>>mailto:second@xwiki.org]] third fourth", wikiEditor.getContent());
    }

    @Test
    @Order(9)
    void switchTargetTypeWhenEditingLink(TestUtils setup, TestReference testReference)
    {
        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "First [[second>>https://xwiki.org]] third fourth");

        InplaceEditablePage page = new InplaceEditablePage().editInplace();

        BlockNoteEditor editor = new BlockNoteEditor("content");
        BlockNoteRichTextArea textArea = editor.getRichTextArea();

        // Move the caret inside the link, using the keyboard, to trigger the link toolbar, then open the link modal
        // and switch the target type from URL to E-mail. We wait for the rich text area to be focused before
        // sending the keys, otherwise they can be silently dropped (e.g. the HOME key) if sent right after the
        // click, before the click's focus has actually settled.
        textArea.click();
        textArea.waitUntilFocused();
        textArea.sendKeys(Keys.HOME);
        textArea.sendKeys(Keys.ARROW_RIGHT.toString().repeat(8));
        editor.getToolBar().editLink().setEmailTargetAndSubmit("second@xwiki.org");

        // The link text must be left untouched.
        textArea.waitUntilTextIs("First second third fourth");

        // Save and check the source.
        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("First [[second>>mailto:second@xwiki.org]] third fourth", wikiEditor.getContent());
    }

    @Test
    @Order(10)
    void editUrlLink(TestUtils setup, TestReference testReference)
    {
        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "First [[second>>https://xwiki.org]] third fourth");

        InplaceEditablePage page = new InplaceEditablePage().editInplace();

        BlockNoteEditor editor = new BlockNoteEditor("content");
        BlockNoteRichTextArea textArea = editor.getRichTextArea();

        // Move the caret inside the link, using the keyboard, to trigger the link toolbar, then open the link modal
        // and change the target URL. We wait for the rich text area to be focused before sending the keys,
        // otherwise they can be silently dropped (e.g. the HOME key) if sent right after the click, before the
        // click's focus has actually settled.
        textArea.click();
        textArea.waitUntilFocused();
        textArea.sendKeys(Keys.HOME);
        textArea.sendKeys(Keys.ARROW_RIGHT.toString().repeat(8));
        editor.getToolBar().editLink().setTargetAndSubmit("https://example.org");

        // The link text must be left untouched.
        textArea.waitUntilTextIs("First second third fourth");

        // Save and check the source.
        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("First [[second>>https://example.org]] third fourth", wikiEditor.getContent());
    }

    @Test
    @Order(11)
    void editPageLink(TestUtils setup, TestReference testReference) throws Exception
    {
        // Create the page currently linked to (the "old" target) and the page we're going to switch the link to
        // (the "new" target), and wait for the new one to be indexed, since the page link suggestions are based on
        // Solr indexation.
        DocumentReference oldTargetPage =
            new DocumentReference("OldPageLinkTarget", testReference.getLastSpaceReference());
        setup.deletePage(oldTargetPage);
        setup.createPage(oldTargetPage, "", "Old Page Link Target");
        DocumentReference newTargetPage =
            new DocumentReference("NewPageLinkTarget", testReference.getLastSpaceReference());
        setup.deletePage(newTargetPage);
        setup.createPage(newTargetPage, "", "New Page Link Target");
        new SolrTestUtils(setup).waitEmptyQueue();

        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference,
            "First [[second>>doc:%s]] third fourth".formatted(setup.serializeReference(oldTargetPage)));

        InplaceEditablePage page = new InplaceEditablePage().editInplace();

        BlockNoteEditor editor = new BlockNoteEditor("content");
        BlockNoteRichTextArea textArea = editor.getRichTextArea();

        // Move the caret inside the link, using the keyboard, to trigger the link toolbar, then open the link modal
        // and change the target page. Search by title since suggestions are matched (and rendered) using the page
        // title, not its reference. We wait for the rich text area to be focused before sending the keys, otherwise
        // they can be silently dropped (e.g. the HOME key) if sent right after the click, before the click's focus
        // has actually settled.
        textArea.click();
        textArea.waitUntilFocused();
        textArea.sendKeys(Keys.HOME);
        textArea.sendKeys(Keys.ARROW_RIGHT.toString().repeat(8));
        editor.getToolBar().editLink().setPageTargetAndSubmit("New Page Link Target", "New Page Link Target");

        // The link text must be left untouched.
        textArea.waitUntilTextIs("First second third fourth");

        // Save and check the source.
        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("First [[second>>doc:%s]] third fourth".formatted(setup.serializeReference(newTargetPage)),
            wikiEditor.getContent());
    }

    @Test
    @Order(12)
    void editAttachmentLink(TestUtils setup, TestReference testReference) throws Exception
    {
        // Create the page holding the attachment currently linked to (the "old" target) and the page holding the
        // attachment we're going to switch the link to (the "new" target), and wait for the new one to be indexed,
        // since the attachment link suggestions are based on Solr indexation.
        String attachmentName = "image.gif";
        DocumentReference oldTargetPage =
            new DocumentReference("OldAttachmentLinkTarget", testReference.getLastSpaceReference());
        setup.deletePage(oldTargetPage);
        setup.createPage(oldTargetPage, "", "Old Attachment Link Target");
        setup.attachFile(oldTargetPage, attachmentName, getClass().getResourceAsStream('/' + attachmentName), false);
        DocumentReference newTargetPage =
            new DocumentReference("NewAttachmentLinkTarget", testReference.getLastSpaceReference());
        setup.deletePage(newTargetPage);
        setup.createPage(newTargetPage, "", "New Attachment Link Target");
        setup.attachFile(newTargetPage, attachmentName, getClass().getResourceAsStream('/' + attachmentName), false);
        new SolrTestUtils(setup).waitEmptyQueue();

        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference,
            "First [[second>>attach:%s@%s]] third fourth".formatted(setup.serializeReference(oldTargetPage), attachmentName));

        InplaceEditablePage page = new InplaceEditablePage().editInplace();

        BlockNoteEditor editor = new BlockNoteEditor("content");
        BlockNoteRichTextArea textArea = editor.getRichTextArea();

        // Move the caret inside the link, using the keyboard, to trigger the link toolbar, then open the link modal
        // and change the target attachment. Search by filename (as a real user would), but disambiguate the
        // suggestion to select using the target page name, since other tests running in the same wiki may also
        // attach a file with the same name. We wait for the rich text area to be focused before sending the keys,
        // otherwise they can be silently dropped (e.g. the HOME key) if sent right after the click, before the
        // click's focus has actually settled.
        textArea.click();
        textArea.waitUntilFocused();
        textArea.sendKeys(Keys.HOME);
        textArea.sendKeys(Keys.ARROW_RIGHT.toString().repeat(8));
        editor.getToolBar().editLink().setAttachmentTargetAndSubmit(attachmentName, newTargetPage.getName());

        // The link text must be left untouched.
        textArea.waitUntilTextIs("First second third fourth");

        // Save and check the source.
        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("First [[second>>attach:%s@%s]] third fourth".formatted(setup.serializeReference(newTargetPage),
            attachmentName), wikiEditor.getContent());
    }

    @Test
    @Order(13)
    void editEmailLink(TestUtils setup, TestReference testReference)
    {
        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "First [[second>>mailto:second@xwiki.org]] third fourth");

        InplaceEditablePage page = new InplaceEditablePage().editInplace();

        BlockNoteEditor editor = new BlockNoteEditor("content");
        BlockNoteRichTextArea textArea = editor.getRichTextArea();

        // Move the caret inside the link, using the keyboard, to trigger the link toolbar, then open the link modal
        // and change the target e-mail address. We wait for the rich text area to be focused before sending the
        // keys, otherwise they can be silently dropped (e.g. the HOME key) if sent right after the click, before
        // the click's focus has actually settled.
        textArea.click();
        textArea.waitUntilFocused();
        textArea.sendKeys(Keys.HOME);
        textArea.sendKeys(Keys.ARROW_RIGHT.toString().repeat(8));
        editor.getToolBar().editLink().setEmailTargetAndSubmit("other@xwiki.org");

        // The link text must be left untouched.
        textArea.waitUntilTextIs("First second third fourth");

        // Save and check the source.
        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("First [[second>>mailto:other@xwiki.org]] third fourth", wikiEditor.getContent());
    }

    @Test
    @Order(14)
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
        // Wait for the rich text area to be focused before sending the keys, otherwise they can be silently dropped
        // (e.g. the HOME key) if sent right after the click, before the click's focus has actually settled.
        textArea.waitUntilFocused();
        textArea.sendKeys(Keys.HOME);
        textArea.sendKeys(Keys.ARROW_RIGHT.toString().repeat(wordOffset));
        textArea.sendKeys(Keys.chord(Keys.SHIFT, Keys.ARROW_RIGHT.toString().repeat(wordLength)));
    }
}
