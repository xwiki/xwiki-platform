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

import java.io.File;
import java.util.Arrays;

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

        // The image suggestions execute a Solr query from XWiki.SuggestSolrService / XWiki.SuggestSolrMacros, which
        // requires programming right. These pages are authored by a user that has programming right in a normal wiki,
        // but the test wiki blocks programming right by default, so we need to explicitly exclude them.
        "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:XWiki\\.SuggestSolr(Service|Macros)"
    },
    extraJARs = {
        // The WebSocket end-point implementation based on XWiki components needs to be installed as core extension.
        "org.xwiki.platform:xwiki-platform-websocket",

        // The macro service uses the extension index script service to get the list of uninstalled macros (from
        // extensions) which expects an implementation of the extension index. The extension index script service is a
        // core extension so we need to make the extension index also core.
        "org.xwiki.platform:xwiki-platform-extension-index",

        // Solr search is used to get suggestions for the image quick action.
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
        // Start fresh.
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
        assertTrue(Arrays.equals(FileUtils.readFileToByteArray(file), uploadedAttachmentContent));
    }

    @Test
    @Order(2)
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

        // Save and check the source: the image is saved with its reference rather than with its URL.
        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("[[image:%s@%s]]".formatted(setup.serializeReference(testReference), imageName),
            wikiEditor.getContent());
    }

    @Test
    @Order(3)
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
}
