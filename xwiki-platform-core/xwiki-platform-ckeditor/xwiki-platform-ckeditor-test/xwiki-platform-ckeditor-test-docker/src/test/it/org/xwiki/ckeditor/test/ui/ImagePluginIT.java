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
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.xwiki.ckeditor.test.po.CKEditor;
import org.xwiki.ckeditor.test.po.image.ImageDialogEditModal;
import org.xwiki.ckeditor.test.po.image.ImageDialogSelectModal;
import org.xwiki.ckeditor.test.po.image.select.ImageDialogIconSelectForm;
import org.xwiki.ckeditor.test.po.image.select.ImageDialogUrlSelectForm;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.model.jaxb.Object;
import org.xwiki.rest.model.jaxb.Property;
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
    void setUp(TestUtils setup, TestReference testReference)
    {
        // Run the tests as a normal user. We make the user advanced only to enable the Edit drop down menu.
        createAndLoginStandardUser(setup);
        setup.deletePage(testReference);
    }

    @Test
    @Order(1)
    void insertImage(TestUtils setup, TestReference testReference) throws Exception
    {
        String attachmentName = "image.gif";
        AttachmentReference attachmentReference = new AttachmentReference(attachmentName, testReference);
        ViewPage newPage = uploadAttachment(setup, testReference, attachmentName);

        // Move to the WYSIWYG edition page.
        WYSIWYGEditPage wysiwygEditPage = newPage.editWYSIWYG();
        CKEditor editor = new CKEditor("content").waitToLoad();

        // Insert a first image.
        ImageDialogSelectModal imageDialogSelectModal = editor.clickImageButton();
        imageDialogSelectModal.switchToTreeTab().selectAttachment(attachmentReference);
        ImageDialogEditModal imageDialogEditModal = imageDialogSelectModal.clickSelect();
        imageDialogEditModal.clickInsert();
        // Move the focus out of the newly inserted image widget.
        editor.getRichTextArea().sendKeys(Keys.RIGHT);
        // Insert a second image, with a caption.
        imageDialogSelectModal = editor.clickImageButton();
        imageDialogSelectModal.switchToTreeTab().selectAttachment(attachmentReference);
        imageDialogEditModal = imageDialogSelectModal.clickSelect();
        imageDialogEditModal.clickCaptionCheckbox();
        imageDialogEditModal.clickInsert();

        ViewPage savedPage = wysiwygEditPage.clickSaveAndView();

        // Verify that the content matches what we did using CKEditor.
        assertEquals("[[image:image.gif]]\n"
            + "\n"
            + "[[Caption>>image:image.gif]]", savedPage.editWiki().getContent());
    }

    @Test
    @Order(2)
    void insertImageWithStyle(TestUtils setup, TestReference testReference) throws Exception
    {
        // Create the image style as an admin.
        setup.loginAsSuperAdmin();
        DocumentReference borderedStyleDocumentReference =
            new DocumentReference(setup.getCurrentWiki(), List.of("Image", "Style", "Code",
                "ImageStyles"), "bordered");
        setup.rest().delete(borderedStyleDocumentReference);
        setup.rest().savePage(borderedStyleDocumentReference);
        Object styleObject =
            setup.rest().object(borderedStyleDocumentReference, "Image.Style.Code.ImageStyleClass");
        Property borderedProperty = new Property();
        borderedProperty.setName("prettyName");
        borderedProperty.setValue("Bordered");
        Property typeProperty = new Property();
        typeProperty.setName("type");
        typeProperty.setValue("bordered");
        styleObject.withProperties(borderedProperty, typeProperty);
        setup.rest().add(styleObject);

        // Then test the image styles on the image dialog as a standard user.
        createAndLoginStandardUser(setup);
        String attachmentName = "image.gif";
        AttachmentReference attachmentReference = new AttachmentReference(attachmentName, testReference);
        ViewPage newPage = uploadAttachment(setup, testReference, attachmentName);

        // Move to the WYSIWYG edition page.
        WYSIWYGEditPage wysiwygEditPage = newPage.editWYSIWYG();
        CKEditor editor = new CKEditor("content").waitToLoad();

        // Insert a first image.
        ImageDialogSelectModal imageDialogSelectModal = editor.clickImageButton();
        imageDialogSelectModal.switchToTreeTab().selectAttachment(attachmentReference);
        ImageDialogEditModal imageDialogEditModal = imageDialogSelectModal.clickSelect();
        // Assert the available image styles as well as the one currently selected.
        assertEquals(Set.of("", "bordered"), imageDialogEditModal.getListImageStyles());
        assertEquals("", imageDialogEditModal.getCurrentImageStyle());
        imageDialogEditModal.setImageStyle("Bordered");
        imageDialogEditModal.clickInsert();

        ViewPage savedPage = wysiwygEditPage.clickSaveAndView();

        // Verify that the content matches what we did using CKEditor.
        assertEquals("[[image:image.gif||data-xwiki-image-style=\"bordered\"]]",
            savedPage.editWiki().getContent());

        // Re-edit the page.
        savedPage.editWYSIWYG();
        editor = new CKEditor("content").waitToLoad();

        // Focus on the image to edit.
        editor.executeOnIframe(() -> setup.getDriver().findElement(By.id("Iimage.gif")).click());

        imageDialogEditModal = editor.clickImageButtonWhenImageExists();
        assertEquals(Set.of("", "bordered"), imageDialogEditModal.getListImageStyles());
        assertEquals("bordered", imageDialogEditModal.getCurrentImageStyle());

        // Re-insert and save the page to avoid triggering a javascript alert for unsaved page.
        imageDialogEditModal.clickInsert();
        wysiwygEditPage.clickSaveAndView();
    }

    @Test
    @Order(3)
    void insertIcon(TestUtils setup, TestReference testReference)
    {
        setup.deletePage(testReference);
        ViewPage newPage = setup.gotoPage(testReference);

        // Move to the WYSIWYG edition page.
        WYSIWYGEditPage wysiwygEditPage = newPage.editWYSIWYG();
        CKEditor editor = new CKEditor("content").waitToLoad();

        // Insert a first image.
        ImageDialogSelectModal imageDialogSelectModal = editor.clickImageButton();
        ImageDialogIconSelectForm imageDialogIconSelectForm = imageDialogSelectModal.switchToIconTab();
        imageDialogIconSelectForm.setIconValue("accept");
        ImageDialogEditModal imageDialogEditModal = imageDialogSelectModal.clickSelect();
        imageDialogEditModal.clickInsert();
        ViewPage savedPage = wysiwygEditPage.clickSaveAndView();

        // Verify that the content matches what we did using CKEditor.
        assertEquals("[[image:icon:accept]]", savedPage.editWiki().getContent());
    }

    @Test
    @Order(4)
    void insertUrl(TestUtils setup, TestReference testReference)
    {
        ViewPage newPage = setup.gotoPage(testReference);

        // Move to the WYSIWYG edition page.
        WYSIWYGEditPage wysiwygEditPage = newPage.editWYSIWYG();
        CKEditor editor = new CKEditor("content").waitToLoad();

        // Insert a first image.
        ImageDialogSelectModal imageDialogSelectModal = editor.clickImageButton();
        ImageDialogUrlSelectForm imageDialogUrlSelectForm = imageDialogSelectModal.switchToUrlTab();
        imageDialogUrlSelectForm.setUrlValue("http://mysite.com/myimage.png");
        ImageDialogEditModal imageDialogEditModal = imageDialogSelectModal.clickSelect();
        imageDialogEditModal.clickInsert();
        ViewPage savedPage = wysiwygEditPage.clickSaveAndView();

        // Verify that the content matches what we did using CKEditor.
        assertEquals("[[image:http://mysite.com/myimage.png]]", savedPage.editWiki().getContent());
    }

    private static void createAndLoginStandardUser(TestUtils setup)
    {
        setup.createUserAndLogin("alice", "pa$$word", "editor", "Wysiwyg", "usertype", "Advanced");
    }

    private ViewPage uploadAttachment(TestUtils setup, TestReference testReference, String attachmentName)
        throws Exception
    {
        ViewPage newPage = setup.createPage(testReference, "", "");
        setup.attachFile(testReference, attachmentName,
            getClass().getResourceAsStream("/ImagePlugin/" + attachmentName), false);
        return newPage;
    }
}
