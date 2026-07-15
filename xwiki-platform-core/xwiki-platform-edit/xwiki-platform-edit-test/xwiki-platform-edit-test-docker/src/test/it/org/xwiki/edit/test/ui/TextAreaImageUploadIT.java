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
package org.xwiki.edit.test.ui;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.annotation.test.po.AnnotatableViewPage;
import org.xwiki.annotation.test.po.AnnotationsWindow;
import org.xwiki.ckeditor.test.po.CKEditor;
import org.xwiki.flamingo.skin.test.po.AttachmentsPane;
import org.xwiki.flamingo.skin.test.po.AttachmentsViewPage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.CommentsTab;
import org.xwiki.test.ui.po.EditablePropertyPane;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.ObjectEditPage;
import org.xwiki.wysiwyg.test.po.image.ImageDialogSelectModal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that images uploaded through the CKEditor image dialog are actually attached to the edited document when the
 * editor is embedded in a TextArea property (object editor and in-place property editing), in a comment or in an
 * annotation. This is a regression test for <a href="https://jira.xwiki.org/browse/XWIKI-24546">XWIKI-24546</a>: the
 * source document reference passed to the embedded editor was serialized with its (empty) locale parameters, which
 * resolved to a wrong document and left the uploaded images broken after save (the temporary attachment was never
 * persisted on the saved document).
 *
 * @version $Id$
 */
@UITest(
    extraJARs = {
        // The macro service uses the extension index script service to get the list of uninstalled macros (from
        // extensions) which expects an implementation of the extension index. The extension index script service is a
        // core extension so we need to make the extension index also core.
        "org.xwiki.platform:xwiki-platform-extension-index"
    },
    resolveExtraJARs = true
)
class TextAreaImageUploadIT
{
    private static final String CLASS_NAME = "TextAreaImageUploadIT.TextAreaClass";

    private static final DocumentReference CLASS_REFERENCE =
        new DocumentReference("xwiki", "TextAreaImageUploadIT", "TextAreaClass");

    private static final String PROPERTY_NAME = "textarea";

    private static final String IMAGE_NAME = "image.gif";

    @BeforeAll
    void beforeAll(TestUtils testUtils) throws Exception
    {
        // Use superadmin.
        testUtils.loginAsSuperAdmin();

        // Make WYSIWYG the default editor so that the TextArea property, the comment and the annotation are edited with
        // CKEditor.
        testUtils.setPropertyInXWikiPreferences("editor", "String", "Wysiwyg");

        // Create a class with a TextArea property.
        testUtils.rest().delete(CLASS_REFERENCE);
        testUtils.createPage(CLASS_REFERENCE, "", "TextAreaClass");
        testUtils.addClassProperty(CLASS_REFERENCE, PROPERTY_NAME, "TextArea");
    }

    /**
     * Upload an image while editing a TextArea property in the object editor, using basically the same setup as
     * {@code TextAreaIT}.
     */
    @Test
    @Order(1)
    void uploadImageInObjectProperty(TestUtils testUtils, TestReference testReference) throws Exception
    {
        testUtils.rest().delete(testReference);
        testUtils.rest().addObject(testReference, CLASS_NAME, PROPERTY_NAME, "");

        ObjectEditPage objectEditPage = ObjectEditPage.gotoPage(testReference);
        objectEditPage.getObjectsOfClass(CLASS_NAME);
        CKEditor ckeditor = new CKEditor(CLASS_NAME + "_0_" + PROPERTY_NAME).waitToLoad();

        uploadImage(ckeditor);

        objectEditPage.clickSaveAndView();

        assertImageAttached(testUtils, testReference);
    }

    /**
     * Upload an image while editing a TextArea property in-place, through the {@link EditablePropertyPane}. The
     * document content displays the property using the structure expected by that page object.
     */
    @Test
    @Order(2)
    void uploadImageInEditableProperty(TestUtils testUtils, TestReference testReference) throws Exception
    {
        String content = """
            {{velocity}}
            #set ($object = $doc.getObject('%1$s'))
            #set ($discard = $xwiki.jsfx.use('uicomponents/edit/editableProperty.js', {
              'forceSkinAction': true,
              'language': $xcontext.locale
            }))
            #set ($discard = $doc.use($object))
            #set ($editing = $xcontext.action == 'edit')
            {{html wiki="true" clean="false"}}
            <div class="xform">
              <dl>
                <dt #if (!$editing && $hasEdit)
                    class="editableProperty"
                    data-property="$escapetool.xml($services.model.serialize($object.getPropertyReference('%2$s')))"
                    data-property-type="object"#end>
                  <label#if ($editing) for="%1$s_0_%2$s"#end>
                    $escapetool.xml($doc.displayPrettyName('%2$s', false, false))
                  </label>
                </dt>
                <dd>$doc.display('%2$s')</dd>
              </dl>
            </div>
            {{/html}}
            {{/velocity}}
            """.formatted(CLASS_NAME, PROPERTY_NAME);

        testUtils.rest().delete(testReference);
        testUtils.rest().savePage(testReference, content, null);
        testUtils.rest().addObject(testReference, CLASS_NAME, PROPERTY_NAME, "");

        testUtils.gotoPage(testReference);

        // The editable property displayer serializes the object property reference relative to its document, i.e.
        // without the wiki and document parts (see also ClassSheetIT), so we build the same local reference here.
        String propertyReference = CLASS_NAME + "[0]." + PROPERTY_NAME;
        EditablePropertyPane<String> propertyPane = new EditablePropertyPane<>(propertyReference);
        propertyPane.clickEdit();

        CKEditor ckeditor = new CKEditor(CLASS_NAME + "_0_" + PROPERTY_NAME).waitToLoad();
        uploadImage(ckeditor);

        propertyPane.clickSave();

        assertImageAttached(testUtils, testReference);
    }

    /**
     * Upload an image while adding a comment. The comment content is also a TextArea property, edited with CKEditor,
     * and it is one of the scenarios originally reported in XWIKI-24546.
     */
    @Test
    @Order(3)
    void uploadImageInComment(TestUtils testUtils, TestReference testReference) throws Exception
    {
        testUtils.deletePage(testReference);
        ViewPage viewPage = testUtils.createPage(testReference, "");

        CommentsTab commentsTab = viewPage.openCommentsDocExtraPane();
        commentsTab.openCommentForm();
        CKEditor ckeditor = new CKEditor("XWiki.XWikiComments_comment").waitToLoad();

        uploadImage(ckeditor);

        commentsTab.getAddCommentForm().clickSubmit();

        assertImageAttached(testUtils, testReference);
    }

    /**
     * Upload an image while adding an annotation. The annotation comment is also a TextArea property, edited with
     * CKEditor, and it is the scenario originally reported in XWIKI-24546.
     */
    @Test
    @Order(4)
    void uploadImageInAnnotation(TestUtils testUtils, TestReference testReference) throws Exception
    {
        testUtils.rest().delete(testReference);
        ViewPage viewPage = testUtils.createPage(testReference, "one two three");

        AnnotatableViewPage annotatableViewPage = new AnnotatableViewPage(viewPage);
        AnnotationsWindow annotationsWindow = annotatableViewPage.beginAddAnnotation("two");

        // The annotation comment is a TextArea property that is edited with CKEditor when the WYSIWYG editor is the
        // default one. The corresponding editor instance name is just the property name because the annotation form
        // displays the property without the usual "Class_objectNumber_" prefix.
        CKEditor ckeditor = new CKEditor("comment").waitToLoad();
        uploadImage(ckeditor);

        annotationsWindow.clickSaveAnnotation();

        // Wait for the annotation to be saved before reloading the page to check the uploaded image.
        viewPage.waitForNotificationSuccessMessage("Annotation added");

        assertImageAttached(testUtils, testReference);
    }

    /**
     * Insert an image in the given editor by uploading it through the image dialog (as opposed to selecting an existing
     * attachment), which is the code path fixed in XWIKI-24546.
     *
     * @param ckeditor the editor to insert the image into
     * @throws Exception in case of error while uploading the image
     */
    private void uploadImage(CKEditor ckeditor) throws Exception
    {
        ImageDialogSelectModal imageDialogSelectModal = ckeditor.getToolBar().insertImage();
        imageDialogSelectModal.switchToUploadTab().upload('/' + IMAGE_NAME);
        imageDialogSelectModal.clickSelect().clickInsert();
    }

    /**
     * Assert that the uploaded image has been persisted as an attachment on the given document.
     *
     * @param testUtils the test utilities, used to reload the document
     * @param testReference the reference of the edited document
     */
    private void assertImageAttached(TestUtils testUtils, TestReference testReference)
    {
        // Reload the page to make sure we look at the persisted state and open the attachments tab.
        testUtils.gotoPage(testReference);
        AttachmentsPane attachmentsPane = new AttachmentsViewPage().openAttachmentsDocExtraPane();
        assertEquals(1, attachmentsPane.getNumberOfAttachments());
        assertTrue(attachmentsPane.attachmentIsDisplayedByFileName(IMAGE_NAME),
            String.format("The uploaded image [%s] was not attached to the document.", IMAGE_NAME));
    }
}
