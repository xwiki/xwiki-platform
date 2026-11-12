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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Keys;
import org.xwiki.blocknote.test.po.BlockNoteEditor;
import org.xwiki.blocknote.test.po.BlockNoteInplaceEditPage;
import org.xwiki.blocknote.test.po.BlockNoteLinkModal;
import org.xwiki.blocknote.test.po.BlockNoteRichTextArea;
import org.xwiki.blocknote.test.po.SlashMenu;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.repository.test.SolrTestUtils;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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

        // The page, attachment and image suggestions execute a Solr query from XWiki.SuggestSolrService /
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

        // Solr search is used to get suggestions for the link and image quick actions.
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
        setup.createPage(testReference, "first second third", "");

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();

        BlockNoteEditor editor = page.getContentEditor();
        BlockNoteRichTextArea textArea = editor.getRichTextArea();

        // Select the word "second" using the keyboard.
        textArea.click().select(6, 6);

        // Create a link targeting a URL from the selection.
        editor.getToolBar().createLink().setTargetAndSubmit("https://xwiki.org");

        // Save and check the source: the link is inserted on the selected word, without touching the rest of the line.
        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("first [[second>>https://xwiki.org]] third", wikiEditor.getContent());
    }

    @Test
    @Order(2)
    void editLinkTitle(TestUtils setup, TestReference testReference)
    {
        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "first [[second>>https://xwiki.org]] third");

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteEditor editor = page.getContentEditor();
        BlockNoteRichTextArea textArea = editor.getRichTextArea();

        // Move the caret inside the link, using the keyboard, to trigger the link toolbar (we can't click the link
        // because the browser would follow it, and hovering it with synthetic mouse events is not reliable), then
        // change the link title.
        textArea.click().moveCaretTo(8);
        editor.getToolBar().editLink().setTitleAndSubmit("2nd");

        // The link title must be updated without touching the rest of the line.
        textArea.waitUntilTextIs("first 2nd third");
        // The edited content is focused back, so that keyboard users can keep typing.
        textArea.waitUntilFocused();

        // Save and check the source.
        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("first [[2nd>>https://xwiki.org]] third", wikiEditor.getContent());
    }

    @Test
    @Order(3)
    void cancelLinkCreation(TestUtils setup, TestReference testReference)
    {
        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "first second third");

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();

        BlockNoteEditor editor = page.getContentEditor();
        BlockNoteRichTextArea textArea = editor.getRichTextArea();

        // Select the word "second" using the keyboard.
        textArea.click().select(6, 6);

        // Open the link modal, fill it in, but cancel it instead of submitting.
        BlockNoteLinkModal linkModal = editor.getToolBar().createLink();
        linkModal.selectTargetType("URL");
        linkModal.setTarget("https://xwiki.org");

        // Clicking outside the link modal doesn't close it, so that what was typed in it is not lost by accident.
        linkModal.clickOutside();
        assertEquals("https://xwiki.org", linkModal.getTarget());

        linkModal.cancel();

        // The content must be left untouched.
        textArea.waitUntilTextIs("first second third");

        // Save and check the source: since nothing was actually changed in the editor, the page content is left
        // untouched (i.e. it is not even round-tripped through the editor, hence the lack of style annotation that a
        // real edit would introduce).
        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("first second third", wikiEditor.getContent());
    }

    @Test
    @Order(4)
    void cancelLinkEdition(TestUtils setup, TestReference testReference)
    {
        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "first [[second>>https://xwiki.org]] third");

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();

        BlockNoteEditor editor = page.getContentEditor();
        BlockNoteRichTextArea textArea = editor.getRichTextArea();

        // Move the caret inside the link, using the keyboard, to trigger the link toolbar, then open the link modal,
        // change the title, but cancel the modal instead of submitting.
        textArea.click().moveCaretTo(8);
        BlockNoteLinkModal linkModal = editor.getToolBar().editLink();
        linkModal.setDisplayText("2nd");
        linkModal.cancel();

        // The link title must be left untouched.
        textArea.waitUntilTextIs("first second third");
        // The edited content is focused back, so that keyboard users can keep typing.
        textArea.waitUntilFocused();

        // Save and check the source: since nothing was actually changed in the editor, the page content is left
        // untouched (i.e. it is not even round-tripped through the editor, hence the lack of style annotation that a
        // real edit would introduce).
        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("first [[second>>https://xwiki.org]] third", wikiEditor.getContent());
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
        setup.createPage(testReference, "first second third");

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();

        BlockNoteEditor editor = page.getContentEditor();
        BlockNoteRichTextArea textArea = editor.getRichTextArea();

        // Select the word "second" using the keyboard.
        textArea.click().select(6, 6);

        // Create a link targeting a page from the selection. Search by title since suggestions are matched (and
        // rendered) using the page title, not its reference.
        editor.getToolBar().createLink().setPageTargetAndSubmit("Page Link Target", "Page Link Target");

        // Save and check the source: the link is inserted on the selected word, without touching the rest of the line.
        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("first [[second>>doc:PageLinkTarget]] third", wikiEditor.getContent());
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
        setup.createPage(targetPage, "", "");
        String attachmentName = "image.gif";
        setup.attachFile(targetPage, attachmentName, getClass().getResourceAsStream('/' + attachmentName), false);
        new SolrTestUtils(setup).waitEmptyQueue();

        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "first second third", "");

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();

        BlockNoteEditor editor = page.getContentEditor();
        BlockNoteRichTextArea textArea = editor.getRichTextArea();

        // Select the word "second" using the keyboard.
        textArea.click().select(6, 6);

        // Create a link targeting an attachment from the selection. Search by filename (as a real user would), but
        // disambiguate the suggestion to select using the test page name, since other tests running in the same wiki
        // may also attach a file with the same name.
        editor.getToolBar().createLink().setAttachmentTargetAndSubmit(attachmentName, attachmentName,
            testReference.getLastSpaceReference().getName());

        // Save and check the source: the link is inserted on the selected word, without touching the rest of the line.
        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("first [[second>>attach:AttachmentLinkTarget@image.gif]] third", wikiEditor.getContent());
    }

    @Test
    @Order(7)
    void createEmailLink(TestUtils setup, TestReference testReference)
    {
        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "first second third");

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();

        BlockNoteEditor editor = page.getContentEditor();
        BlockNoteRichTextArea textArea = editor.getRichTextArea();

        // Select the word "second" using the keyboard.
        textArea.click().select(6, 6);

        // Create a link targeting an e-mail address from the selection.
        editor.getToolBar().createLink().setEmailTargetAndSubmit("second@xwiki.org");

        // Save and check the source: the link is inserted on the selected word, without touching the rest of the line.
        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("first [[second>>mailto:second@xwiki.org]] third", wikiEditor.getContent());
    }

    @Test
    @Order(9)
    void switchTargetTypeWhenEditingLink(TestUtils setup, TestReference testReference)
    {
        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "first [[second>>https://xwiki.org]] third");

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();

        BlockNoteEditor editor = page.getContentEditor();
        BlockNoteRichTextArea textArea = editor.getRichTextArea();

        // Move the caret inside the link, using the keyboard, to trigger the link toolbar, then open the link modal
        // and switch the target type from URL to E-mail.
        textArea.click().moveCaretTo(8);
        editor.getToolBar().editLink().setEmailTargetAndSubmit("second@xwiki.org");

        // Save and check the source: the link text is left untouched.
        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("first [[second>>mailto:second@xwiki.org]] third", wikiEditor.getContent());
    }

    @Test
    @Order(10)
    void editURLLink(TestUtils setup, TestReference testReference)
    {
        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "first [[second>>https://xwiki.org]] third");

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();

        BlockNoteEditor editor = page.getContentEditor();
        BlockNoteRichTextArea textArea = editor.getRichTextArea();

        // Move the caret inside the link, using the keyboard, to trigger the link toolbar, then open the link modal
        // and change the target to a relative URL.
        textArea.click().moveCaretTo(8);
        editor.getToolBar().editLink().setTargetAndSubmit("/xwiki/bin/view/Main/");

        // Save and check the source (which also shows that the link label is left untouched): the relative URL is kept
        // as typed, as a path reference, since a URL reference needs a scheme.
        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("first [[second>>path:/xwiki/bin/view/Main/]] third", wikiEditor.getContent());

        // Edit the link again: it's now a path link.
        page = BlockNoteInplaceEditPage.gotoPage(testReference).editInplace();
        editor = page.getContentEditor();
        textArea = editor.getRichTextArea();
        textArea.click().moveCaretTo(8);
        BlockNoteLinkModal linkModal = editor.getToolBar().editLink();
        assertEquals("Path", linkModal.getTargetType());
        assertEquals("/xwiki/bin/view/Main/", linkModal.getTarget());

        // Change it to a link to a URL without a scheme: the URL is completed, as in the address bar of a browser.
        linkModal.setTargetAndSubmit("www.xwiki.org");

        page.save();
        wikiEditor = page.editWiki();
        assertEquals("first [[second>>https://www.xwiki.org]] third", wikiEditor.getContent());
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
            "first [[second>>doc:%s]] third".formatted(setup.serializeReference(oldTargetPage)));

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();

        BlockNoteEditor editor = page.getContentEditor();
        BlockNoteRichTextArea textArea = editor.getRichTextArea();

        // Move the caret inside the link, using the keyboard, to trigger the link toolbar, then open the link modal
        // and change the target page. Search by title since suggestions are matched (and rendered) using the page
        // title, not its reference.
        textArea.click().moveCaretTo(8);
        editor.getToolBar().editLink().setPageTargetAndSubmit("New Page Link Target", "New Page Link Target");

        // Save and check the source: the link text is left untouched.
        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("first [[second>>doc:NewPageLinkTarget]] third", wikiEditor.getContent());
    }

    @Test
    @Order(12)
    void editAttachmentLink(TestUtils setup, TestReference testReference) throws Exception
    {
        // Create the page holding the attachment we're going to switch the link to (the "new" target), and wait for it
        // be indexed, since the attachment link suggestions are based on Solr indexation.
        String attachmentName = "image.gif";
        DocumentReference oldTargetPage =
            new DocumentReference("OldAttachmentLinkTarget", testReference.getLastSpaceReference());
        DocumentReference newTargetPage =
            new DocumentReference("NewAttachmentLinkTarget", testReference.getLastSpaceReference());
        setup.deletePage(newTargetPage);
        setup.createPage(newTargetPage, "", "");
        setup.attachFile(newTargetPage, attachmentName, getClass().getResourceAsStream('/' + attachmentName), false);
        new SolrTestUtils(setup).waitEmptyQueue();

        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference,
            "first [[second>>attach:%s@%s]] third".formatted(setup.serializeReference(oldTargetPage), attachmentName));

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();

        BlockNoteEditor editor = page.getContentEditor();
        BlockNoteRichTextArea textArea = editor.getRichTextArea();

        // Move the caret inside the link, using the keyboard, to trigger the link toolbar, then open the link modal
        // and change the target attachment. Search by filename (as a real user would), but disambiguate the suggestion
        // to select using the test page name, since other tests running in the same wiki may also attach a file with
        // the same name.
        textArea.click().moveCaretTo(8);
        editor.getToolBar().editLink().setAttachmentTargetAndSubmit(attachmentName, attachmentName,
            testReference.getLastSpaceReference().getName());

        // Save and check the source: the link text is left untouched.
        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("first [[second>>attach:NewAttachmentLinkTarget@image.gif]] third", wikiEditor.getContent());
    }

    @Test
    @Order(13)
    void editEmailLink(TestUtils setup, TestReference testReference)
    {
        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "first [[second>>mailto:second@xwiki.org]] third");

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();

        BlockNoteEditor editor = page.getContentEditor();
        BlockNoteRichTextArea textArea = editor.getRichTextArea();

        // Move the caret inside the link, using the keyboard, to trigger the link toolbar, then open the link modal
        // and change the target e-mail address.
        textArea.click().moveCaretTo(8);
        editor.getToolBar().editLink().setEmailTargetAndSubmit("other@xwiki.org");

        // Save and check the source: the link text is left untouched.
        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("first [[second>>mailto:other@xwiki.org]] third", wikiEditor.getContent());
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

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteEditor editor = page.getContentEditor();
        BlockNoteRichTextArea textArea = editor.getRichTextArea();

        // Place the caret after the first link.
        textArea.click().moveCaretTo(0).sendKeys(Keys.chord(Keys.CONTROL, Keys.RIGHT, Keys.RIGHT));

        // Change the generated link label
        textArea.sendKeys(Keys.LEFT, Keys.BACK_SPACE, "z");

        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("one [[Alize>>Users.Alice]] two [[Users.Bob]] three", wikiEditor.getContent());
    }

    @Test
    @Order(15)
    void preserveLinkParametersWhenChangingTarget(TestUtils setup, TestReference testReference) throws Exception
    {
        // Create the page we're going to switch the link to and wait for it to be indexed, since the page link
        // suggestions are based on Solr indexation.
        DocumentReference newTargetPage = new DocumentReference("NewLinkTarget", testReference.getLastSpaceReference());
        setup.deletePage(newTargetPage);
        setup.createPage(newTargetPage, "", "New Link Target");
        new SolrTestUtils(setup).waitEmptyQueue();

        // Start fresh, with a link that has custom parameters. They are not part of the BlockNote schema so they are
        // kept aside, bound to the link, and must survive a change of the link target.
        setup.deletePage(testReference);
        setup.createPage(testReference, "first [[second>>doc:Users.Alice||class=\"foo\"]] third");

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteEditor editor = page.getContentEditor();
        BlockNoteRichTextArea textArea = editor.getRichTextArea();

        // Move the caret inside the link, using the keyboard, to trigger the link toolbar, then open the link modal
        // and change the target page.
        textArea.click().moveCaretTo(8);
        editor.getToolBar().editLink().setPageTargetAndSubmit("New Link Target", "New Link Target");

        // Save and check the source: the link text is left untouched.
        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("first [[second>>doc:NewLinkTarget||class=\"foo\"]] third", wikiEditor.getContent());
    }

    @Test
    @Order(16)
    void insertLinkFromSlashMenu(TestUtils setup, TestReference testReference) throws Exception
    {
        // Create the page to link to, with an attachment, and wait for them to be indexed, since the link suggestions
        // are based on Solr indexation.
        DocumentReference targetPage =
            new DocumentReference("SlashMenuLinkTarget", testReference.getLastSpaceReference());
        setup.deletePage(targetPage);
        setup.createPage(targetPage, "", "Slash Menu Link Target");
        // The file name is searched by its words, which are indexed only when shorter than 14 characters.
        String attachmentName = "slashmenu-file.txt";
        setup.attachFile(targetPage, attachmentName,
            new ByteArrayInputStream("content".getBytes(StandardCharsets.UTF_8)), false);
        new SolrTestUtils(setup).waitEmptyQueue();

        // Start fresh, with a page that already has a link, edited in a realtime session.
        setup.deletePage(testReference);
        setup.createPage(testReference, "first [[second>>doc:Users.Alice]] third");

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteEditor editor = page.getContentEditor();
        BlockNoteRichTextArea textArea = editor.getRichTextArea();

        // Open the link modal from the link suggestions of the slash menu, in a new paragraph.
        textArea.appendParagraph("/");
        SlashMenu slashMenu = new SlashMenu();
        textArea.sendKeys("link");
        slashMenu.waitForItemSelected("Link");
        textArea.sendKeys(Keys.ENTER);
        slashMenu.waitForItemSubmitted();
        slashMenu = new SlashMenu().waitForItemSelected("Link to...");
        textArea.sendKeys(Keys.ENTER);
        slashMenu.waitForItemSubmitted();

        // The link modal shows the link type and the link target, which is focused, so that keyboard users can type
        // the target right away.
        BlockNoteLinkModal linkModal = new BlockNoteLinkModal();
        assertEquals("Page", linkModal.getTargetType());
        linkModal.typeInFocusedField("Slash");
        assertEquals("Slash", linkModal.getTarget());

        // Pick the page to link to, leaving the display text empty: the link label is generated from the target.
        linkModal.setPageReference("Slash Menu Link Target", "Slash Menu Link Target");
        linkModal.submit();
        textArea.waitUntilLinkIsPresent("Slash Menu Link Target");

        // Typing the start of a link in XWiki syntax suggests the pages to link to: search the page by its title (as a
        // real user would), but pick the suggestion using its location, since other tests running in the same wiki
        // may also create a page with the same title. The link label is generated from the page.
        // The trigger is sent on its own: the editor recognizes it only when typed, not when inserted along with other
        // text in one go (as Firefox inserts the keys sent at once by WebDriver).
        String location = testReference.getLastSpaceReference().getName();
        textArea.sendKeys(" ");
        textArea.sendKeys("[[");
        textArea.sendKeys("Slash");
        new SlashMenu().clickItem("Slash Menu Link Target", location);

        // The attachments to link to are suggested as well, and the link label is generated from the attachment.
        textArea.sendKeys(" ");
        textArea.sendKeys("[[");
        textArea.sendKeys("slashmenu");
        new SlashMenu().clickItem(attachmentName, location);
        textArea.waitUntilLinkIsPresent(attachmentName);

        // Save and check the source: the links are saved with the reference of the linked page or attachment, relative
        // to the edited page, rather than with its URL, and without their generated label, while the existing link is
        // left untouched.
        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("first [[second>>doc:Users.Alice]] third\n\n[[doc:SlashMenuLinkTarget]] "
            + "[[doc:SlashMenuLinkTarget]] [[attach:SlashMenuLinkTarget@%s]]".formatted(attachmentName),
            wikiEditor.getContent());
    }

    @Test
    @Order(17)
    void editLinkQueryStringAndAnchor(TestUtils setup, TestReference testReference)
    {
        // Start fresh, with a page link that has an anchor and a custom parameter.
        setup.deletePage(testReference);
        setup.createPage(testReference, "first [[second>>doc:QueryStringTarget||anchor=\"HOld\" class=\"foo\"]] third");

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteEditor editor = page.getContentEditor();
        BlockNoteRichTextArea textArea = editor.getRichTextArea();

        // Move the caret inside the link, using the keyboard, to trigger the link toolbar, then open the link modal.
        // The page search box is focused, which lists the pages matching the current reference: close these
        // suggestions, which cover the link options.
        textArea.click().moveCaretTo(8);
        BlockNoteLinkModal linkModal = editor.getToolBar().editLink();
        linkModal.closeSuggestions();

        // The link modal shows the anchor of the link.
        linkModal.expandOptions();
        assertEquals("HOld", linkModal.getAnchor());
        assertEquals("", linkModal.getQueryString());

        // Change the anchor and add a query string, without changing the target page.
        linkModal.setAnchor("HNew");
        linkModal.setQueryString("a=b");
        linkModal.submit();

        // Save and check the source: the anchor is updated, the query string is added and the custom parameter is
        // preserved.
        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("first [[second>>doc:QueryStringTarget||queryString=\"a=b\" anchor=\"HNew\" class=\"foo\"]] third",
            wikiEditor.getContent());

        // Edit the link again: the link modal shows the new anchor and query string.
        page = BlockNoteInplaceEditPage.gotoPage(testReference).editInplace();
        editor = page.getContentEditor();
        textArea = editor.getRichTextArea();
        textArea.click().moveCaretTo(8);
        linkModal = editor.getToolBar().editLink();
        linkModal.closeSuggestions();
        linkModal.expandOptions();
        assertEquals("HNew", linkModal.getAnchor());
        assertEquals("a=b", linkModal.getQueryString());
        linkModal.cancel();
    }

    @Test
    @Order(18)
    void changeTargetOfLinkWithGeneratedLabel(TestUtils setup, TestReference testReference)
    {
        // Start fresh, with a link whose label is generated from its target.
        setup.deletePage(testReference);
        setup.createPage(testReference, "first [[Users.Alice]] third");

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteEditor editor = page.getContentEditor();
        BlockNoteRichTextArea textArea = editor.getRichTextArea();

        // Move the caret inside the link, using the keyboard, to trigger the link toolbar, then open the link modal:
        // the display text is empty, since the link label is generated.
        textArea.click().moveCaretTo(8);
        BlockNoteLinkModal linkModal = editor.getToolBar().editLink();
        assertEquals("", linkModal.getDisplayText());

        // Type the reference of the new target page, rather than picking a suggestion, then close the suggestions with
        // the Escape key: this must not close the link modal.
        linkModal.setTarget("Users.Bob");
        linkModal.closeSuggestions();
        assertEquals("Users.Bob", linkModal.getTarget());
        linkModal.submit();

        // The link label is generated from the new target.
        textArea.waitUntilTextIs("first Bob third");

        // Save and check the source: the link is still saved without its generated label.
        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("first [[doc:Users.Bob]] third", wikiEditor.getContent());
    }

    @Test
    @Order(19)
    void submitLinkWithoutTarget(TestUtils setup, TestReference testReference)
    {
        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "first second third", "");

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteEditor editor = page.getContentEditor();
        BlockNoteRichTextArea textArea = editor.getRichTextArea();

        // Select the word "second" using the keyboard, then open the link modal and submit it without a target
        // attachment: the browser reports the missing target and the link modal stays open.
        textArea.click().select(6, 6);
        BlockNoteLinkModal linkModal = editor.getToolBar().createLink();
        linkModal.selectTargetType("Attachment");
        linkModal.trySubmit();
        assertFalse(linkModal.getTargetValidationMessage().isEmpty());

        // The same goes for a link to a URL.
        linkModal.selectTargetType("URL");
        linkModal.trySubmit();
        assertFalse(linkModal.getTargetValidationMessage().isEmpty());

        // Once a target is set, the link modal can be submitted.
        linkModal.setTargetAndSubmit("https://xwiki.org");

        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("first [[second>>https://xwiki.org]] third", wikiEditor.getContent());
    }

    @Test
    @Order(20)
    void createLinkWithDisplayTextOnFormattedSelection(TestUtils setup, TestReference testReference)
    {
        // Start fresh, with a bold word.
        setup.deletePage(testReference);
        setup.createPage(testReference, "first **second** third", "");

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteEditor editor = page.getContentEditor();
        BlockNoteRichTextArea textArea = editor.getRichTextArea();

        // Select the bold word using the keyboard, then create a link from the selection, changing its display text.
        textArea.click().select(6, 6);
        BlockNoteLinkModal linkModal = editor.getToolBar().createLink();
        linkModal.setDisplayText("2nd");
        linkModal.setTargetAndSubmit("https://xwiki.org");

        // Save and check the source: the selected word is replaced with the display text, which keeps its formatting.
        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("first [[**2nd**>>https://xwiki.org]] third", wikiEditor.getContent());
    }

    @Test
    @Order(21)
    void editTargetOfLinkWithFormattedText(TestUtils setup, TestReference testReference)
    {
        // Start fresh, with a link whose text is partly bold.
        setup.deletePage(testReference);
        setup.createPage(testReference, "first [[se**con**d>>https://xwiki.org]] third", "");

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteEditor editor = page.getContentEditor();
        BlockNoteRichTextArea textArea = editor.getRichTextArea();

        // Move the caret inside the link, using the keyboard, to trigger the link toolbar, then open the link modal
        // and change only the link target.
        textArea.click().moveCaretTo(8);
        editor.getToolBar().editLink().setTargetAndSubmit("https://www.xwiki.org");

        // Save and check the source: the link text keeps its formatting.
        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("first [[se**con**d>>https://www.xwiki.org]] third", wikiEditor.getContent());
    }

    @Test
    @Order(22)
    void typeDisplayTextOfLinkWithGeneratedLabel(TestUtils setup, TestReference testReference)
    {
        // Start fresh, with a link whose label is generated from its target.
        setup.deletePage(testReference);
        setup.createPage(testReference, "first [[Users.Alice]] third");

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteEditor editor = page.getContentEditor();
        BlockNoteRichTextArea textArea = editor.getRichTextArea();

        // Move the caret inside the link, using the keyboard, to trigger the link toolbar, then open the link modal.
        // The page search box is focused, which lists the pages matching the current reference: close these
        // suggestions, which cover the link options, then type the display text, the same as the generated label.
        textArea.click().moveCaretTo(8);
        BlockNoteLinkModal linkModal = editor.getToolBar().editLink();
        linkModal.closeSuggestions();
        linkModal.setTitleAndSubmit("Alice");

        // Save and check the source: the link label chosen by the user is saved.
        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("first [[Alice>>Users.Alice]] third", wikiEditor.getContent());
    }

    @Test
    @Order(23)
    void createLinkWithEmptyDisplayTextOnSelection(TestUtils setup, TestReference testReference)
    {
        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "first second third", "");

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteEditor editor = page.getContentEditor();
        BlockNoteRichTextArea textArea = editor.getRichTextArea();

        // Select the word "second" using the keyboard, then create a link from the selection, emptying its display
        // text: the selection is replaced with the label generated from the link target.
        textArea.click().select(6, 6);
        BlockNoteLinkModal linkModal = editor.getToolBar().createLink();
        linkModal.setDisplayText("");
        linkModal.setTargetAndSubmit("https://xwiki.org");
        textArea.waitUntilTextIs("first https://xwiki.org third");

        // Save and check the source: the link is saved without its generated label.
        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("first [[https://xwiki.org]] third", wikiEditor.getContent());
    }

    @Test
    @Order(24)
    void createLinkToSectionOfCurrentPage(TestUtils setup, TestReference testReference)
    {
        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "first second third", "");

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteEditor editor = page.getContentEditor();
        BlockNoteRichTextArea textArea = editor.getRichTextArea();

        // Select the word "second" using the keyboard, then create a link from the selection, leaving the page
        // reference empty and setting only the anchor.
        textArea.click().select(6, 6);
        BlockNoteLinkModal linkModal = editor.getToolBar().createLink();
        linkModal.setAnchor("HSection");
        linkModal.submit();

        // Save and check the source: the link has an empty reference, which designates the current page.
        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("first [[second>>||anchor=\"HSection\"]] third", wikiEditor.getContent());

        // Edit the link again: the page reference is still empty.
        page = BlockNoteInplaceEditPage.gotoPage(testReference).editInplace();
        editor = page.getContentEditor();
        textArea = editor.getRichTextArea();
        textArea.click().moveCaretTo(8);
        linkModal = editor.getToolBar().editLink();
        assertEquals("Page", linkModal.getTargetType());
        assertEquals("", linkModal.getTarget());
        linkModal.expandOptions();
        assertEquals("HSection", linkModal.getAnchor());

        // Change the anchor, add a query string and clear the display text: the reference is kept empty, and the link
        // label is generated from the current page.
        linkModal.setAnchor("HOther");
        linkModal.setQueryString("a=b");
        linkModal.setDisplayText("");
        linkModal.submit();
        textArea.waitUntilTextIs("first createLinkToSectionOfCurrentPage third");

        // Save and check the source: the link is saved without its generated label.
        page.save();
        wikiEditor = page.editWiki();
        assertEquals("first [[||queryString=\"a=b\" anchor=\"HOther\"]] third", wikiEditor.getContent());

        // The link is still there when editing the page again, with its generated label.
        page = BlockNoteInplaceEditPage.gotoPage(testReference).editInplace();
        page.getContentEditor().getRichTextArea().waitUntilLinkIsPresent("createLinkToSectionOfCurrentPage");
    }

    @Test
    @Order(25)
    void createLinkToCurrentPage(TestUtils setup, TestReference testReference)
    {
        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "first second third", "");

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteEditor editor = page.getContentEditor();
        BlockNoteRichTextArea textArea = editor.getRichTextArea();

        // Insert, from the slash menu in a new paragraph, a link to the current page, leaving all the fields of the
        // link modal empty: the link label is generated from the current page.
        textArea.appendParagraph("/");
        SlashMenu slashMenu = new SlashMenu();
        textArea.sendKeys("link");
        slashMenu.waitForItemSelected("Link");
        textArea.sendKeys(Keys.ENTER);
        slashMenu.waitForItemSubmitted();
        slashMenu = new SlashMenu().waitForItemSelected("Link to...");
        textArea.sendKeys(Keys.ENTER);
        slashMenu.waitForItemSubmitted();
        new BlockNoteLinkModal().submit();
        textArea.waitUntilLinkIsPresent("createLinkToCurrentPage");

        // Select the word "second" of the first paragraph using the keyboard, then create another link to the current
        // page from the selection, leaving the page reference, the query string and the anchor empty.
        textArea.clickBlock(0).select(6, 6);
        editor.getToolBar().createLink().submit();
        textArea.waitUntilLinkIsPresent("second");

        // Save and check the source: both links have an empty reference, which designates the current page, and the
        // one in the new paragraph has no label either.
        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("first [[second>>]] third\n\n[[]]", wikiEditor.getContent());

        // The link is still there when editing the page again, with its generated label.
        page = BlockNoteInplaceEditPage.gotoPage(testReference).editInplace();
        page.getContentEditor().getRichTextArea().waitUntilLinkIsPresent("createLinkToCurrentPage");
    }
}
