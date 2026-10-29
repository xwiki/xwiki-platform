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

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.blocknote.test.po.BlockNoteEditor;
import org.xwiki.blocknote.test.po.BlockNoteInplaceEditPage;
import org.xwiki.blocknote.test.po.BlockNoteRichTextArea;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.EntityReference;
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
class ImageIT extends AbstractBlockNoteIT
{
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
        textArea.click().pasteFile("/image.gif");
        // Shown only once uploaded, since that's when the URL is known.
        textArea.waitUntilImageIsLoaded(0);

        page.save();
        String savedContent = page.editWiki().getContent();

        // Untyped reference, and renamed: browsers name a pasted file after its media type, so a second paste would
        // otherwise overwrite this attachment. A block of its own, after the paragraph that held the caret, since
        // BlockNote inserts the file block after the current block instead of splitting it.
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
        setup.createPage(testReference, "before");

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteRichTextArea textArea = page.getContentEditor().getRichTextArea();

        byte[] content = FileUtils.readFileToByteArray(setup.getResourceFile("/notes.txt"));
        // Unlike a pasted file, a dropped file keeps the name the user gave it.
        textArea.click().dropFile("/notes.txt");
        // The link replaces the inserted block only once uploaded.
        textArea.waitUntilLinkIsPresent("notes.txt");

        page.save();

        // The file block is saved as a macro call, but that macro doesn't exist yet, so the file becomes an
        // attachment link instead, in a block of its own after the paragraph that held the caret.
        assertEquals("""
            before

            [[attach:notes.txt]]""", page.editWiki().getContent());

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
        setup.createPage(testReference, "before");
        byte[] before = "before".getBytes(StandardCharsets.UTF_8);
        setup.rest().attachFile(new EntityReference("notes.txt", EntityType.ATTACHMENT, testReference),
            new ByteArrayInputStream(before), true);

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteRichTextArea textArea = page.getContentEditor().getRichTextArea();

        byte[] after = FileUtils.readFileToByteArray(setup.getResourceFile("/notes.txt"));
        textArea.click().dropFile("/notes.txt");
        textArea.waitUntilLinkIsPresent("notes.txt");

        page.save();

        // A taken name overwrites the existing attachment, like CKEditor and the attachment UI do.
        assertEquals("""
            before

            [[attach:notes.txt]]""", page.editWiki().getContent());

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
        setup.createPage(testReference, "before");

        // The editor loads even when it can't reach the collaboration server, which is also what happens for a user
        // who can't edit the document (e.g. with Change Request).
        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage();
        page.simulateUnreachableCollaborationServer().edit();
        page.waitForInplaceEditor(false);
        assertFalse(page.getToolbar().isConnected());

        BlockNoteRichTextArea textArea = page.getContentEditor().getRichTextArea();
        byte[] content = FileUtils.readFileToByteArray(setup.getResourceFile("/notes.txt"));
        textArea.click().dropFile("/notes.txt");
        textArea.waitUntilLinkIsPresent("notes.txt");

        // Out of a realtime session the file waits in the temporary store, so it isn't attached yet.
        Attachments beforeSave = setup.rest().get(AttachmentsResource.class, testReference);
        assertEquals(0, beforeSave.getAttachments().size());

        page.save();

        assertEquals("""
            before

            [[attach:notes.txt]]""", page.editWiki().getContent());

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
    void pasteImageInsideText(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.deletePage(testReference);
        setup.createPage(testReference, "before");

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteRichTextArea textArea = page.getContentEditor().getRichTextArea();

        textArea.click().moveCaretTo(2);
        textArea.pasteFile("/image.gif");
        textArea.waitUntilImageIsLoaded(0);

        page.save();

        // The caret was in the middle of "before", yet the image goes after the whole paragraph.
        assertThat(page.editWiki().getContent(),
            matchesPattern("before\n\n\\[\\[image:\\d+-\\d+\\.gif\\]\\]"));
    }

    @Test
    @Order(8)
    void pasteFileInsideText(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.deletePage(testReference);
        setup.createPage(testReference, "before");

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteRichTextArea textArea = page.getContentEditor().getRichTextArea();

        textArea.click().moveCaretTo(2);
        textArea.pasteFile("/notes.txt");
        textArea.waitUntilLinkIsPresent();

        page.save();

        // Same for the link: the caret was inside the text, the link lands in a paragraph of its own.
        assertThat(page.editWiki().getContent(),
            matchesPattern("before\n\n\\[\\[attach:\\d+-\\d+\\.txt\\]\\]"));
    }
}
