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
package org.xwiki.test.wysiwyg;

import java.util.Date;

import org.junit.Test;
import org.xwiki.test.wysiwyg.framework.AbstractWysiwygTestCase;

import static org.junit.Assert.*;

/**
 * Functional tests for in-line editing using the WYSIWYG editor.
 * 
 * @version $Id$
 */
public class EditInlineTest extends AbstractWysiwygTestCase
{
    /**
     * Tests if a property whose name contains the underscore character can be edited properly.
     * 
     * @see XWIKI-4746: GWT-editor wont render field's content if it has underscores in it's name.
     */
    @Test
    public void testEditPropertyWithUnderscore()
    {
        StringBuffer spaceName = new StringBuffer(this.getClass().getSimpleName());
        spaceName.insert(spaceName.length() / 2, "_0_");

        StringBuffer pageName = new StringBuffer(getTestMethodName());
        pageName.insert(pageName.length() / 2, "_17_");

        // Create a class with a property that has '_' in its name.
        open(spaceName.toString(), pageName.toString(), "edit", "editor=class");
        String propertyName = "my_1_property";
        addWysiwygProperty(propertyName);
        clickEditSaveAndContinue();

        // Create an object of the previously created class.
        open(spaceName.toString(), pageName.toString(), "edit", "editor=object");
        addObject(pageName.toString());
        String propertyValue = String.valueOf(new Date().getTime());
        setFieldValue(spaceName + "." + pageName + "_0_" + propertyName, propertyValue);
        clickEditSaveAndContinue();

        // Display the object.
        open(spaceName.toString(), pageName.toString(), "edit", "editor=wiki");
        setFieldValue("content", display(pageName.toString(), propertyName));
        clickEditSaveAndView();
        assertTextPresent(propertyValue);

        // Edit the object in-line.
        open(spaceName.toString(), pageName.toString(), "edit", "editor=inline");
        waitForEditorToLoad();
        assertEquals(propertyValue, getRichTextArea().getText());

        // Change the property value.
        propertyValue = new StringBuffer(propertyValue).reverse().toString();
        setContent(propertyValue);
        clickEditSaveAndView();
        assertTextPresent(propertyValue);
    }

    /**
     * Tests if the initial content of the editor is taken from the template when creating a new document from a
     * template.
     * 
     * @see XWIKI-4814: WYSIWYG does not preserve TextArea property values when creating a new document from a class
     *      template
     */
    @Test
    public void testEditorInitialContentWhenCreatingDocumentFromTemplate()
    {
        String spaceName = this.getClass().getSimpleName();
        String pageName = getTestMethodName();
        String className = pageName + "Class";
        String templateName = pageName + "Template";
        String sheetName = pageName + "Sheet";
        String propertyName = "myproperty";

        // Create the class.
        deletePage(spaceName, className);
        open(spaceName, className, "edit", "editor=class");
        addWysiwygProperty(propertyName);
        clickEditSaveAndContinue();

        // Create the sheet.
        deletePage(spaceName, sheetName);
        open(spaceName, sheetName, "edit", "editor=wiki");
        setFieldValue("content", display(className, propertyName));
        clickEditSaveAndContinue();

        // Create the template.
        // Add the object.
        deletePage(spaceName, templateName);
        open(spaceName, templateName, "edit", "editor=object");
        addObject(className);
        String propertyValue = String.valueOf(new Date().getTime());
        setFieldValue(spaceName + "." + className + "_0_" + propertyName, propertyValue);
        clickEditSaveAndContinue();
        // Include the sheet.
        open(spaceName, templateName, "edit", "editor=wiki");
        setFieldValue("content", "{{include document=\"" + sheetName + "\"/}}");
        clickEditSaveAndView();
        assertTextPresent(propertyValue);

        // Create a new page from template.
        open(spaceName, pageName, "edit", "editor=inline&template=" + templateName);
        waitForEditorToLoad();
        assertEquals(propertyValue, getRichTextArea().getText());
    }

    /**
     * Adds a {@code TextArea} property with the specified name and sets its preferred editor to WYSIWYG.
     * 
     * @param propertyName the name of the property to add
     */
    private void addWysiwygProperty(String propertyName)
    {
        setFieldValue("propname", propertyName);
        getSelenium().select("proptype", "TextArea");
        clickEditAddProperty();
        // Expand the added XClass property to modify its 'editor' meta property.
        getSelenium().click("xproperty_" + propertyName);
        getSelenium().select(propertyName + "_editor", "Wysiwyg");
    }

    /**
     * Adds an object of the specified class to the current page.
     * 
     * @param className the class name
     */
    private void addObject(String className)
    {
        getSelenium().select("classname", className);
        clickEditAddObject();
    }

    /**
     * @param className the name of a XWiki class
     * @param propertyName which property of the specified class to display
     * @return the code to display the specified property
     */
    private String display(String className, String propertyName)
    {
        StringBuffer code = new StringBuffer();
        code.append("{{velocity}}\n");
        code.append("$doc.use(\"" + className + "\")\n");
        code.append("$doc.display(\"" + propertyName + "\")\n");
        code.append("{{/velocity}}");
        return code.toString();
    }
}
