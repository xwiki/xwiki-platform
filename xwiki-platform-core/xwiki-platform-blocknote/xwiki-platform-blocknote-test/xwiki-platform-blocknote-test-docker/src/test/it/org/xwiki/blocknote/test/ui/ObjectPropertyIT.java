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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.blocknote.test.po.BlockNoteEditor;
import org.xwiki.blocknote.test.po.BlockNoteRichTextArea;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.rest.model.jaxb.Property;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.InlinePage;
import org.xwiki.test.ui.po.editor.ObjectEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verify that BlockNote can be used to edit the TextArea properties of an object, along with the page content, each
 * BlockNote field editing its own content even though they all take part in the same realtime collaboration session.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
class ObjectPropertyIT extends AbstractBlockNoteIT
{
    private static final String CLASS_NAME = "ObjectPropertyIT.TextAreaClass";

    private static final DocumentReference CLASS_REFERENCE =
        new DocumentReference("xwiki", "ObjectPropertyIT", "TextAreaClass");

    private static final DocumentReference SHEET_REFERENCE =
        new DocumentReference("xwiki", "ObjectPropertyIT", "TextAreaSheet");

    private static final String FIRST = "first";

    private static final String SECOND = "second";

    private static final String OBJECT_EDITOR = "editor=object";

    private static final String INLINE_EDITOR = "editor=inline";

    private static final String EDIT = "edit";

    private static final String CONTENT = "content";

    @BeforeAll
    static void createClassAndSheet(TestUtils setup) throws Exception
    {
        // The class and its sheet are created by a user with script right, because the sheet uses Velocity.
        setup.loginAsSuperAdmin();

        // A class with two TextArea properties, edited with the WYSIWYG editor (BlockNote) because that's the editor
        // preferred by the user editing them.
        setup.rest().delete(CLASS_REFERENCE);
        setup.createPage(CLASS_REFERENCE, "", "TextAreaClass");
        setup.addClassProperty(CLASS_REFERENCE, FIRST, "TextArea");
        setup.addClassProperty(CLASS_REFERENCE, SECOND, "TextArea");

        // A sheet editing the page content together with the first property, in inline form edit mode. It has no title
        // so that the title of the pages using it is not replaced by the title of the sheet.
        setup.rest().delete(SHEET_REFERENCE);
        setup.createPage(SHEET_REFERENCE, """
            {{velocity}}
            #if ($xcontext.action == 'edit')
              {{html clean="false"}}
              $!services.edit.syntaxContent.wysiwyg($tdoc.content, $tdoc.syntax, {'id': 'content', 'name': 'content'})
              {{/html}}
            #end

            $doc.display('first')
            {{/velocity}}""", "");
        setup.addObject(CLASS_REFERENCE, "XWiki.ClassSheetBinding", "sheet", "ObjectPropertyIT.TextAreaSheet");

        setup.login("John", "pass");
    }

    @Test
    @Order(1)
    void editPropertiesInObjectEditor(TestUtils setup, TestReference testReference) throws Exception
    {
        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "");
        setup.addObject(testReference, CLASS_NAME, FIRST, "First value", SECOND, "Second value");

        ObjectEditPage objectEditPage = editObjects(setup, testReference);

        // Each field loads its own value.
        BlockNoteRichTextArea firstTextArea = getRichTextArea(FIRST);
        BlockNoteRichTextArea secondTextArea = getRichTextArea(SECOND);
        firstTextArea.waitUntilTextIs("First value");
        secondTextArea.waitUntilTextIs("Second value");

        // Each field keeps what is typed in it.
        firstTextArea.appendParagraph("Added to the first");
        secondTextArea.appendParagraph("Added to the second");
        firstTextArea.waitUntilTextIs("First value\nAdded to the first");
        secondTextArea.waitUntilTextIs("Second value\nAdded to the second");

        // Each property is saved with its own value.
        objectEditPage.clickSaveAndView();
        org.xwiki.rest.model.jaxb.Object object = getObject(setup, testReference);
        assertEquals("First value\n\nAdded to the first", getPropertyValue(object, FIRST));
        assertEquals("Second value\n\nAdded to the second", getPropertyValue(object, SECOND));

        // Each field loads its saved value when editing again.
        editObjects(setup, testReference);
        getRichTextArea(FIRST).waitUntilTextIs("First value\nAdded to the first");
        getRichTextArea(SECOND).waitUntilTextIs("Second value\nAdded to the second");
    }

    @Test
    @Order(2)
    void editContentAndPropertyInInlineForm(TestUtils setup, TestReference testReference) throws Exception
    {
        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "Page content");
        setup.addObject(testReference, CLASS_NAME, FIRST, "Property value");

        setup.gotoPage(testReference, EDIT, INLINE_EDITOR);
        InlinePage inlinePage = new InlinePage();

        // The page content and the property are edited each in its own BlockNote field.
        BlockNoteRichTextArea contentTextArea = new BlockNoteEditor(CONTENT).waitToLoad().getRichTextArea();
        BlockNoteRichTextArea propertyTextArea = getRichTextArea(FIRST);
        contentTextArea.waitUntilTextIs("Page content");
        propertyTextArea.waitUntilTextIs("Property value");

        contentTextArea.appendParagraph("Added to the content");
        propertyTextArea.appendParagraph("Added to the property");
        contentTextArea.waitUntilTextIs("Page content\nAdded to the content");
        propertyTextArea.waitUntilTextIs("Property value\nAdded to the property");

        // The page content and the property are saved each with its own value.
        inlinePage.clickSaveAndView();
        Page page = setup.rest().get(testReference);
        assertEquals("Page content\n\nAdded to the content", page.getContent());
        assertEquals("Property value\n\nAdded to the property", getPropertyValue(getObject(setup, testReference),
            FIRST));
    }

    private ObjectEditPage editObjects(TestUtils setup, DocumentReference documentReference)
    {
        setup.gotoPage(documentReference, EDIT, OBJECT_EDITOR);
        ObjectEditPage objectEditPage = new ObjectEditPage();
        // The objects are collapsed by default, and their properties are loaded (and their editors created) only when
        // they are expanded.
        objectEditPage.getObjectsOfClass(CLASS_NAME);
        return objectEditPage;
    }

    private BlockNoteRichTextArea getRichTextArea(String propertyName)
    {
        return new BlockNoteEditor(CLASS_NAME + "_0_" + propertyName).waitToLoad().getRichTextArea();
    }

    private org.xwiki.rest.model.jaxb.Object getObject(TestUtils setup, DocumentReference documentReference)
        throws Exception
    {
        return setup.rest().get(new ObjectReference(CLASS_NAME + "[0]", documentReference));
    }

    private String getPropertyValue(org.xwiki.rest.model.jaxb.Object object, String propertyName)
    {
        return object.getProperties().stream().filter(property -> propertyName.equals(property.getName()))
            .map(Property::getValue).findFirst().orElse(null);
    }
}
