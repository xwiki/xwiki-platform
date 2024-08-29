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

import java.util.Arrays;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.ckeditor.test.po.CKEditor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.ObjectEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the TextArea property editor.
 *
 * @version $Id$
 */
@UITest
class TextAreaIT
{
    private static final String TEXTAREA_CLASS = "TextAreaIT.NestedSpace.TextAreaClass";

    private static final DocumentReference TEXTAREA_CLASS_REFERENCE =
        new DocumentReference("xwiki", Arrays.asList("TextAreaIT", "NestedSpace"), "TextAreaClass");

    @BeforeAll
    public void beforeEach(TestUtils testUtils)
    {
        // Use superadmin
        testUtils.loginAsSuperAdmin();

        // Make WYSIWYG the default editor
        testUtils.setPropertyInXWikiPreferences("editor", "String", "Wysiwyg");

        // Create a class with a textarea
        testUtils.createPage(TEXTAREA_CLASS_REFERENCE, "", "TextAreaClass");
        testUtils.addClassProperty(TEXTAREA_CLASS_REFERENCE, "textarea", "TextArea");
    }

    @Test
    @Order(1)
    void restricted(TestUtils testUtils, TestReference testReference)
    {
        // Cleanup
        testUtils.deletePage(testReference);

        testUtils.createPage(testReference, "");
        testUtils.addObject(testReference, TEXTAREA_CLASS, "textarea", "{{velocity}}OK{{/velocity}}");

        ViewPage viewPage = testUtils.gotoPage(testReference);
        ObjectEditPage objectEditPage = viewPage.editObjects();
        objectEditPage.getObjectsOfClass(TEXTAREA_CLASS);
        CKEditor ckeditor = new CKEditor(TEXTAREA_CLASS + "_0_textarea").waitToLoad();

        assertEquals("OK", ckeditor.getRichTextArea().getText());

        // Make the textarea restricted
        testUtils.updateClassProperty(TEXTAREA_CLASS_REFERENCE, "textarea_restricted", "1");

        viewPage = testUtils.gotoPage(testReference);
        objectEditPage = viewPage.editObjects();
        objectEditPage.getObjectsOfClass(TEXTAREA_CLASS);
        ckeditor = new CKEditor(TEXTAREA_CLASS + "_0_textarea").waitToLoad();

        assertEquals("Failed to execute the [velocity] macro. "
            + "Cause: [The execution of the [velocity] script macro is not allowed in ["
            + testUtils.serializeReference(testReference) + "]. "
            + "Check the rights of its last author or the parameters if it's rendered from another script.]. "
            + "Click on this message for details.", ckeditor.getRichTextArea().getText());
    }
}
