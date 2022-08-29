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
package org.xwiki.ckeditor.test.ui;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Keys;
import org.xwiki.ckeditor.test.po.CKEditor;
import org.xwiki.ckeditor.test.po.ImageDialogEditModal;
import org.xwiki.ckeditor.test.po.ImageDialogSelectModal;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.model.jaxb.Object;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test of the CKEditor Image Plugin.
 *
 * @version $Id$
 * @since 14.7RC1
 */
@UITest
class ImagePluginIT
{
    @BeforeEach
    void setUp(TestUtils setup) throws Exception
    {
        // Activate the new image dialog.
        activateImageDialog(setup);

        // Run the tests as a normal user. We make the user advanced only to enable the Edit drop down menu.
        setup.createUserAndLogin("alice", "pa$$word", "editor", "Wysiwyg", "usertype", "Advanced");
    }

    @AfterEach
    void tearDown(TestUtils setup) throws Exception
    {
        // Deactivate the new image dialog.
        deactivateImageDialog(setup);
    }

    @Test
    void insertImage(TestUtils setup, TestReference testReference) throws Exception
    {
        String attachmentName = "image.gif";
        AttachmentReference attachmentReference = new AttachmentReference(attachmentName, testReference);
        ViewPage newPage = setup.createPage(testReference, "", "");
        setup.attachFile(testReference, attachmentName,
            getClass().getResourceAsStream("/ImagePlugin/" + attachmentName), false);

        // Move to the WYSIWYG edition page.
        WYSIWYGEditPage wysiwygEditPage = newPage.editWYSIWYG();
        CKEditor editor = new CKEditor("content").waitToLoad();

        // Insert a first image.
        ImageDialogSelectModal imageDialogSelectModal = editor.clickImageButton();
        imageDialogSelectModal.selectAttachment(attachmentReference);
        ImageDialogEditModal imageDialogEditModal = imageDialogSelectModal.clickSelect();
        imageDialogEditModal.clickInsert();
        // Move the focus out of the newly inserted image widget.
        editor.getRichTextArea().sendKeys(Keys.RIGHT);
        // Insert a second image, with a caption.
        imageDialogSelectModal = editor.clickImageButton();
        imageDialogSelectModal.selectAttachment(attachmentReference);
        imageDialogEditModal = imageDialogSelectModal.clickSelect();
        imageDialogEditModal.clickCaptionCheckbox();
        imageDialogEditModal.clickInsert();

        ViewPage savedPage = wysiwygEditPage.clickSaveAndView();

        // Verify that the content matches what we did using CKEditor.
        assertEquals("[[image:attach:image.gif]]\n"
            + "\n"
            + "[[Caption>>image:attach:image.gif]]", savedPage.editWiki().getContent());
    }

    private static void activateImageDialog(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();
        DocumentReference configPageDocumentReference = getConfigPageDocumentReference(setup);
        setup.rest().delete(configPageDocumentReference);
        Page page = setup.rest().page(configPageDocumentReference);
        setup.rest().save(page);
        Object object = setup.rest()
            .object(configPageDocumentReference, "CKEditor.ConfigClass");
        // Update the configuration to activate xwiki-image (by removing it from the removed plugins).
        object.withProperties(TestUtils.RestTestUtils.property("removePlugins",
            List.of("bidi", "colorbutton", "font", "justify", "save", "sourcearea")));
        setup.rest().add(object);
    }

    private void deactivateImageDialog(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();
        setup.rest().delete(getConfigPageDocumentReference(setup));
    }

    private static DocumentReference getConfigPageDocumentReference(TestUtils setup)
    {
        return new DocumentReference(setup.getCurrentWiki(), "CKEditor", "Config");
    }
}
