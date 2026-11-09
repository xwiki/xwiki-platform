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
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Keys;
import org.xwiki.blocknote.test.po.BlockNoteEditor;
import org.xwiki.blocknote.test.po.BlockNoteInplaceEditPage;
import org.xwiki.blocknote.test.po.BlockNoteRichTextArea;
import org.xwiki.blocknote.test.po.SlashMenu;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.repository.test.SolrTestUtils;
import org.xwiki.rest.model.jaxb.Attachment;
import org.xwiki.rest.model.jaxb.Attachments;
import org.xwiki.rest.resources.attachments.AttachmentsResource;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.editor.WikiEditPage;
import org.xwiki.wysiwyg.test.po.image.ImageDialogEditModal;
import org.xwiki.wysiwyg.test.po.image.ImageDialogSelectModal;
import org.xwiki.wysiwyg.test.po.image.edit.ImageDialogAdvancedEditForm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verify image related features of the BlockNote editor integration.
 *
 * @version $Id$
 * @since 18.3.0RC1
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
class ImageIT extends AbstractBlockNoteIT
{
    private static final String SENTENCE = "Please review before the meeting.";

    // Right after "review", so that the file lands in the middle of the sentence rather than at its end.
    private static final int OFFSET = 13;

    // The sentence with a neighbour on each side, so that a test can tell a block inserted next to it from one
    // inserted at the start or the end of the page.
    private static final String SURROUNDED_SENTENCE = "Before.\n\n" + SENTENCE + "\n\nAfter.";

    // Long enough to wrap over several lines, so that a drop can land below the vertical middle of its block.
    private static final String LONG_SENTENCE = SENTENCE.repeat(8);

    private static final String SENTENCE_WITH_LINK = "Please review[[attach:notes.txt]] before the meeting.";

    @Test
    @Order(1)
    void editImage(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.deletePage(testReference);
        setup.createPage(testReference, """
            before

            image:missing.png

            after""");
        
        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();

        BlockNoteEditor editor = page.getContentEditor();
        BlockNoteRichTextArea textArea = editor.getRichTextArea();

        AttachmentReference imageReference = new AttachmentReference("missing.png", testReference);
        assertThat(textArea.getImage(0).getDomAttribute("src"),
            endsWith(setup.getPath(imageReference, "download", "")));

        // Select the image and used the context toolbar to open the image edit modal.
        textArea.clickImage(0);
        ImageDialogEditModal imageEditModal = editor.getToolBar().editImage();

        // Change the image alignment and width.
        imageEditModal.switchToAdvancedTab().selectCenterAlignment().setWidth(127);
        imageEditModal.clickInsert();

        // Save and check the source.
        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("""
            before

            [[image:missing.png||width="127" data-xwiki-image-style-alignment="center"]]

            after""", wikiEditor.getContent());

        // Add some custom image parameters to verify they are preserved even when the image source is changed.
        wikiEditor.setContent("""
            before

            [[image:missing.png||class="test-image" width="127" id="I1234" data-xwiki-image-style-alignment="center" data-foo="bar"]]

            after""");
        wikiEditor.clickSaveAndView();
        
        // Edit again, to verify that the image modal shows the correct with and alignment values.
        page.editInplace();

        editor = page.getContentEditor();
        textArea = editor.getRichTextArea();

        textArea.clickImage(0);
        imageEditModal = editor.getToolBar().editImage();
        assertFalse(imageEditModal.switchToStandardTab().isCaptionCheckboxChecked());
        ImageDialogAdvancedEditForm advancedImageProperties = imageEditModal.switchToAdvancedTab();
        assertEquals("center", advancedImageProperties.getAlignment());
        assertEquals("127px", advancedImageProperties.getWidth());

        // Let's change the image now.
        ImageDialogSelectModal imageSelectModal = imageEditModal.clickChangeImage();
        imageSelectModal.switchToUploadTab().upload("/image.gif");
        imageEditModal = imageSelectModal.clickSelect();

        // Update the image alignment as well.
        imageEditModal.switchToAdvancedTab().selectEndAlignment();
        imageEditModal.clickInsert();

        // The image we just selected replaces the previous one, so we have to wait for it to be loaded before its
        // height reflects the configured width and the image aspect ratio.
        assertEquals("127", textArea.waitUntilImageIsLoaded(0).getDomProperty("offsetHeight"));

        // Save and check the source.
        page.save();
        wikiEditor = page.editWiki();
        assertEquals("""
            before

            [[image:image.gif||class="test-image" id="I1234" data-foo="bar" width="127" data-xwiki-image-style-alignment="end"]]

            after""", wikiEditor.getContent());
        
        // Verify the image caption is preserved when editing the image.
        wikiEditor.setContent("""
            before

            [[some caption>>image:image.gif||class="test-image" id="I1234" data-foo="bar" width="127" data-xwiki-image-style-alignment="end"]]

            after""");
        wikiEditor.clickSaveAndView();
        page.editInplace();

        editor = page.getContentEditor();
        textArea = editor.getRichTextArea();

        imageReference = new AttachmentReference("image.gif", testReference);
        assertThat(textArea.getImage(0).getDomAttribute("src"),
            containsString(setup.getPath(imageReference, "download", "width=127&rev=")));
        
        // Verify the image caption is visible.
        assertThat(textArea.getText(), containsString("some caption"));

        textArea.clickImage(0);
        imageEditModal = editor.getToolBar().editImage();
        assertTrue(imageEditModal.switchToStandardTab().isCaptionCheckboxChecked());
        imageEditModal.switchToAdvancedTab().removeAlignment();
        imageEditModal.clickInsert();

        // Verify the image caption is still visible.
        assertThat(textArea.getText(), containsString("some caption"));

        // Save and check the source.
        page.save();
        wikiEditor = page.editWiki();
        assertEquals("""
            before

            [[some caption>>image:image.gif||class="test-image" id="I1234" data-foo="bar" width="127"]]

            after""", wikiEditor.getContent());

        // Verify that the image was actually uploaded and attached to the page.
        // Verify the image is uploaded properly
        File file = setup.getResourceFile("/image.gif");
        byte[] uploadedAttachmentContent = setup.rest()
            .getAttachmentAsByteArray(new EntityReference("image.gif", EntityType.ATTACHMENT, testReference));
        assertArrayEquals(FileUtils.readFileToByteArray(file), uploadedAttachmentContent);
    }

    @Test
    @Order(2)
    void pasteImage(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.deletePage(testReference);
        setup.createPage(testReference, "before");

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteRichTextArea textArea = page.getContentEditor().getRichTextArea();

        byte[] content = FileUtils.readFileToByteArray(setup.getResourceFile("/image.gif"));
        textArea.click().moveCaretTo(2).pasteFile("/image.gif");
        // Shown only once uploaded, since that's when the URL is known.
        textArea.waitUntilImageIsLoaded(0);

        page.save();
        String savedContent = page.editWiki().getContent();

        // Untyped reference, and renamed: browsers name a pasted file after its media type, so a second paste would
        // otherwise overwrite this attachment. The caret was in the middle of "before", yet the image goes after the
        // whole paragraph, since BlockNote inserts the file block after the current block instead of splitting it.
        assertThat(savedContent, matchesPattern("before\n\n\\[\\[image:\\d+-\\d+\\.gif\\]\\]"));

        String attachmentName = savedContent.replaceAll("(?s).*\\[\\[image:([^\\]]+)\\]\\].*", "$1");
        byte[] uploaded = setup.rest()
            .getAttachmentAsByteArray(new EntityReference(attachmentName, EntityType.ATTACHMENT, testReference));
        assertArrayEquals(content, uploaded);
    }

    @Test
    @Order(3)
    void dropFile(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.deletePage(testReference);
        setup.createPage(testReference, SENTENCE);

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteRichTextArea textArea = page.getContentEditor().getRichTextArea();

        byte[] content = FileUtils.readFileToByteArray(setup.getResourceFile("/notes.txt"));
        // A drop goes where the pointer is, so the caret stays at the start of the line to prove it.
        // Unlike a pasted file, a dropped file keeps the name the user gave it.
        textArea.click().moveCaretTo(0).dropFile("/notes.txt", OFFSET);
        // The link replaces the inserted block only once uploaded.
        textArea.waitUntilLinkIsPresent("notes.txt");

        page.save();

        // A file that is not an image is attached and linked inline where it landed, rather than taking a block of
        // its own.
        assertEquals(SENTENCE_WITH_LINK, page.editWiki().getContent());

        byte[] uploaded = setup.rest()
            .getAttachmentAsByteArray(new EntityReference("notes.txt", EntityType.ATTACHMENT, testReference));
        assertArrayEquals(content, uploaded);
    }

    @Test
    @Order(4)
    void dropFileWithExistingName(TestUtils setup, TestReference testReference) throws Exception
    {
        // An attachment the dropped file is going to collide with.
        setup.deletePage(testReference);
        setup.createPage(testReference, SENTENCE);
        byte[] before = "before".getBytes(StandardCharsets.UTF_8);
        setup.rest().attachFile(new EntityReference("notes.txt", EntityType.ATTACHMENT, testReference),
            new ByteArrayInputStream(before), true);

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteRichTextArea textArea = page.getContentEditor().getRichTextArea();

        byte[] after = FileUtils.readFileToByteArray(setup.getResourceFile("/notes.txt"));
        textArea.click().moveCaretTo(0).dropFile("/notes.txt", OFFSET);
        textArea.waitUntilLinkIsPresent("notes.txt");

        page.save();

        // A taken name overwrites the existing attachment, like CKEditor and the attachment UI do.
        assertEquals(SENTENCE_WITH_LINK, page.editWiki().getContent());

        // A single attachment, so the upload overwrote the existing one rather than adding another.
        Attachments attachments = setup.rest().get(AttachmentsResource.class, testReference);
        assertEquals(1, attachments.getAttachments().size());
        Attachment notes = attachments.getAttachments().get(0);
        assertEquals("notes.txt", notes.getName());
        assertEquals("1.2", notes.getVersion());

        byte[] uploaded = setup.rest()
            .getAttachmentAsByteArray(new EntityReference("notes.txt", EntityType.ATTACHMENT, testReference));
        assertArrayEquals(after, uploaded);
    }

    @Test
    @Order(5)
    void dropFileWithoutRealtime(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.deletePage(testReference);
        setup.createPage(testReference, SENTENCE);

        // The editor loads even when it can't reach the collaboration server, which is also what happens for a user
        // who can't edit the document (e.g. with Change Request).
        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage();
        page.simulateUnreachableCollaborationServer().edit();
        page.waitForInplaceEditor(false);
        assertFalse(page.getToolbar().isConnected());

        BlockNoteRichTextArea textArea = page.getContentEditor().getRichTextArea();
        byte[] content = FileUtils.readFileToByteArray(setup.getResourceFile("/notes.txt"));
        textArea.click().moveCaretTo(0).dropFile("/notes.txt", OFFSET);
        textArea.waitUntilLinkIsPresent("notes.txt");

        // Out of a realtime session the file waits in the temporary store, so it isn't attached yet.
        Attachments beforeSave = setup.rest().get(AttachmentsResource.class, testReference);
        assertEquals(0, beforeSave.getAttachments().size());

        page.save();

        assertEquals(SENTENCE_WITH_LINK, page.editWiki().getContent());

        // The save is what attaches the file.
        Attachments attachments = setup.rest().get(AttachmentsResource.class, testReference);
        assertEquals(1, attachments.getAttachments().size());
        assertEquals("notes.txt", attachments.getAttachments().get(0).getName());
        assertArrayEquals(content, setup.rest()
            .getAttachmentAsByteArray(new EntityReference("notes.txt", EntityType.ATTACHMENT, testReference)));
    }

    @Test
    @Order(6)
    void missingImageShowsItsName(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.deletePage(testReference);
        setup.createPage(testReference, "[[image:missing.png]]");

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteRichTextArea textArea = page.getContentEditor().getRichTextArea();

        // The image can't be displayed, so the editor labels it with its file name, otherwise the block would look
        // empty and there would be nothing to select.
        assertEquals("missing.png", textArea.getImage(0).getDomAttribute("alt"));

        page.save();

        // The label is only displayed, so nothing is added to the source: an alt is saved only when the user sets it.
        assertEquals("[[image:missing.png]]", page.editWiki().getContent());
    }

    @Test
    @Order(7)
    void pasteFile(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.deletePage(testReference);
        setup.createPage(testReference, SENTENCE);

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteRichTextArea textArea = page.getContentEditor().getRichTextArea();

        textArea.click().moveCaretTo(OFFSET).pasteFile("/notes.txt");
        textArea.waitUntilLinkIsPresent();

        page.save();

        // The link goes where the caret was, splitting the text, rather than after the paragraph that held it.
        assertThat(page.editWiki().getContent(),
            matchesPattern("Please review\\[\\[attach:\\d+-\\d+\\.txt\\]\\] before the meeting\\."));
    }

    @Test
    @Order(8)
    void insertImageFromSuggestions(TestUtils setup, TestReference testReference) throws Exception
    {
        // Start fresh, with a page holding an image, and wait for the image to be indexed, since the image suggestions
        // are based on Solr indexation.
        setup.deletePage(testReference);
        setup.createPage(testReference, "", "");
        String imageName = "image.gif";
        setup.attachFile(testReference, imageName, getClass().getResourceAsStream('/' + imageName), false);
        new SolrTestUtils(setup).waitEmptyQueue();

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteEditor editor = page.getContentEditor();
        BlockNoteRichTextArea textArea = editor.getRichTextArea();

        // The image quick action shows the image suggestions.
        textArea.click().sendKeys("/");
        SlashMenu slashMenu = new SlashMenu();
        textArea.sendKeys("image");
        slashMenu.waitForItemSelected("Image");
        textArea.sendKeys(Keys.ENTER);
        slashMenu.waitForItemSubmitted();

        // Search the image by its name (as a real user would), but pick the suggestion using its location, since other
        // tests running in the same wiki may also attach an image with the same name.
        textArea.sendKeys(imageName);
        new SlashMenu().clickItem(imageName, testReference.getLastSpaceReference().getName());
        textArea.waitUntilImageIsLoaded(0);

        // Save and check the source: the image is saved with its reference rather than with its URL, relative to the
        // edited page.
        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("[[image:%s]]".formatted(imageName), wikiEditor.getContent());
    }

    @Test
    @Order(9)
    void insertImageFromImageWizard(TestUtils setup, TestReference testReference) throws Exception
    {
        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "", "");

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteEditor editor = page.getContentEditor();
        BlockNoteRichTextArea textArea = editor.getRichTextArea();

        // The image quick action shows the action to browse the images first, selected.
        textArea.click().sendKeys("/");
        SlashMenu slashMenu = new SlashMenu();
        textArea.sendKeys("image");
        slashMenu.waitForItemSelected("Image");
        textArea.sendKeys(Keys.ENTER);
        slashMenu.waitForItemSubmitted();
        slashMenu = new SlashMenu().waitForItemSelected("Browse images...");
        textArea.sendKeys(Keys.ENTER);
        slashMenu.waitForItemSubmitted();

        // Browsing the images opens the image wizard, where we upload a new image and insert it.
        ImageDialogSelectModal imageSelectModal = new ImageDialogSelectModal();
        imageSelectModal.switchToUploadTab().upload("/image.gif");
        imageSelectModal.clickSelect().clickInsert();
        textArea.waitUntilImageIsLoaded(0);

        // Save and check the source: the image is saved with the reference provided by the image wizard.
        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("[[image:image.gif]]", wikiEditor.getContent());
    }

    @Test
    @Order(10)
    void dropFiles(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.deletePage(testReference);
        setup.createPage(testReference, SENTENCE);

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteRichTextArea textArea = page.getContentEditor().getRichTextArea();

        textArea.click().moveCaretTo(0).dropFiles(OFFSET, "/notes.txt", "/agenda.txt");
        // The last one to be uploaded, so waiting for it waits for both.
        textArea.waitUntilLinkIsPresent("agenda.txt");

        page.save();

        // The links keep the order of the transfer, each one after the previous separated by a space, rather than
        // all at the drop point.
        assertEquals("Please review[[attach:notes.txt]] [[attach:agenda.txt]] before the meeting.",
            page.editWiki().getContent());

        Attachments attachments = setup.rest().get(AttachmentsResource.class, testReference);
        assertEquals(List.of("agenda.txt", "notes.txt"),
            attachments.getAttachments().stream().map(Attachment::getName).sorted().toList());
    }

    @Test
    @Order(11)
    void pasteFiles(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.deletePage(testReference);
        setup.createPage(testReference, SENTENCE);

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteRichTextArea textArea = page.getContentEditor().getRichTextArea();

        textArea.click().moveCaretTo(OFFSET).pasteFiles("/notes.txt", "/agenda.txt");
        textArea.waitUntilLinkCountIs(2);

        page.save();

        // A paste lands at the caret rather than under the pointer, but is otherwise placed like a drop: both links
        // inline, in the order of the transfer, separated by a space. Both files are renamed, as any pasted file is.
        assertThat(page.editWiki().getContent(),
            matchesPattern("Please review\\[\\[attach:\\d+-\\d+\\.txt\\]\\] "
                + "\\[\\[attach:\\d+-\\d+\\.txt\\]\\] before the meeting\\."));
    }

    @Test
    @Order(12)
    void dropImageAndFile(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.deletePage(testReference);
        setup.createPage(testReference, SURROUNDED_SENTENCE);

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteRichTextArea textArea = page.getContentEditor().getRichTextArea();

        textArea.clickBlock(1).moveCaretTo(0).dropFiles(OFFSET, "/image.gif", "/notes.txt");
        textArea.waitUntilImageIsLoaded(0);
        textArea.waitUntilLinkIsPresent("notes.txt");

        page.save();

        // Once an image is in the transfer, BlockNote gives every file a block of its own, next to the paragraph
        // the files landed in rather than at the start of the page. A drop in the upper half of that paragraph
        // puts them above it. The file that is not an image gets a paragraph, since a link needs a block to live in.
        assertEquals("""
            Before.

            [[image:image.gif]]

            [[attach:notes.txt]]

            Please review before the meeting.

            After.""", page.editWiki().getContent());
    }

    @Test
    @Order(13)
    void pasteImageAndFile(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.deletePage(testReference);
        setup.createPage(testReference, SURROUNDED_SENTENCE);

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteRichTextArea textArea = page.getContentEditor().getRichTextArea();

        textArea.clickBlock(1).moveCaretTo(OFFSET).pasteFiles("/image.gif", "/notes.txt");
        textArea.waitUntilImageIsLoaded(0);
        textArea.waitUntilLinkIsPresent();

        page.save();

        // Both files land next to the paragraph the caret was in, as they do for a drop. A paste has no pointer,
        // so BlockNote always puts them after it, and each file is inserted after that same paragraph, which leaves
        // them in the reverse order of the transfer. Both are renamed, as any pasted file is.
        assertThat(page.editWiki().getContent(),
            matchesPattern("Before\\.\n\nPlease review before the meeting\\.\n\n"
                + "\\[\\[attach:\\d+-\\d+\\.txt\\]\\]\n\n\\[\\[image:\\d+-\\d+\\.gif\\]\\]"
                + "\n\nAfter\\."));
    }

    @Test
    @Order(14)
    void dropImages(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.deletePage(testReference);
        setup.createPage(testReference, SURROUNDED_SENTENCE);

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteRichTextArea textArea = page.getContentEditor().getRichTextArea();

        textArea.clickBlock(1).moveCaretTo(0).dropFiles(OFFSET, "/image.gif", "/picture.png");
        textArea.waitUntilImageIsLoaded(1);

        page.save();

        // Each image gets a block of its own, above the paragraph they were dropped in since the pointer was in
        // its upper half, and in the order of the transfer.
        assertEquals("""
            Before.

            [[image:image.gif]]

            [[image:picture.png]]

            Please review before the meeting.

            After.""", page.editWiki().getContent());
    }

    @Test
    @Order(15)
    void pasteImages(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.deletePage(testReference);
        setup.createPage(testReference, SURROUNDED_SENTENCE);

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteRichTextArea textArea = page.getContentEditor().getRichTextArea();

        textArea.clickBlock(1).moveCaretTo(OFFSET).pasteFiles("/image.gif", "/picture.png");
        textArea.waitUntilImageIsLoaded(1);

        page.save();

        // Both images get a block after the paragraph the caret was in, in the reverse order of the transfer since
        // each one is inserted after that same paragraph. Both are renamed, as any pasted file is.
        assertThat(page.editWiki().getContent(),
            matchesPattern("Before\\.\n\nPlease review before the meeting\\.\n\n"
                + "\\[\\[image:\\d+-\\d+\\.png\\]\\]\n\n\\[\\[image:\\d+-\\d+\\.gif\\]\\]"
                + "\n\nAfter\\."));
    }

    @Test
    @Order(16)
    void dropImageAndFileBelowTheMiddle(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.deletePage(testReference);
        setup.createPage(testReference, "Before.\n\n" + LONG_SENTENCE + "\n\nAfter.");

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteRichTextArea textArea = page.getContentEditor().getRichTextArea();

        // Drop on the last line of a paragraph that wraps, so that the pointer is below the middle of its block.
        textArea.clickBlock(1).dropFiles(LONG_SENTENCE.length() - 2, "/image.gif", "/notes.txt");
        textArea.waitUntilImageIsLoaded(0);
        textArea.waitUntilLinkIsPresent("notes.txt");

        page.save();

        // Dropped below the middle of the paragraph the blocks follow it, where a drop in its upper half puts them
        // above it, and they come out in the reverse order of the transfer: each file is inserted after that same
        // paragraph, so the next one lands between them.
        assertEquals("Before.\n\n" + LONG_SENTENCE + "\n\n[[attach:notes.txt]]\n\n[[image:image.gif]]\n\nAfter.",
            page.editWiki().getContent());
    }
}
