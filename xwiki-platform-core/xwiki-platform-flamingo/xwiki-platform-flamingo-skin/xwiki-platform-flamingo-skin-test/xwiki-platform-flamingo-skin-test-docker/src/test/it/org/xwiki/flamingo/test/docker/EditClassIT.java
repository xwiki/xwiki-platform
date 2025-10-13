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
package org.xwiki.flamingo.test.docker;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.FormContainerElement;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.ClassEditPage;
import org.xwiki.test.ui.po.editor.ObjectEditPage;
import org.xwiki.test.ui.po.editor.ObjectEditPane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test XClass editing.
 *
 * @version $Id$
 * @since 14.0RC1
 */
@UITest
class EditClassIT
{
    @BeforeEach
    public void setUp(TestUtils setup, TestReference reference) throws Exception
    {
        setup.loginAsSuperAdmin();
        setup.deletePage(reference.getLastSpaceReference(), true);
    }

    @Test
    @Order(1)
    void addProperty(TestUtils setup, TestReference reference)
    {
        DocumentReference editObjectsTestClass = getTestClassDocumentReference(reference);
        DocumentReference editObjectsTestObject = getTestObjectDocumentReference(reference);
        String className = setup.serializeReference(editObjectsTestClass.getLocalDocumentReference());

        // We verify that we can click on Edit Class from View Page (we need to test this at least once to ensure the UI
        // works).
        setup.gotoPage(editObjectsTestClass, "view", "spaceRedirect=false");
        ClassEditPage cep = new ViewPage().editClass();

        // Create a class with a string property
        cep.addProperty("prop", "String");
        cep.clickSaveAndView();

        // Create object page
        ViewPage vp = setup.createPage(editObjectsTestObject,
            "this is the content: {{velocity}}$doc.display('prop'){{/velocity}}", editObjectsTestClass.getName());

        // Add an object of the class created
        ObjectEditPage oep = vp.editObjects();
        FormContainerElement objectForm = oep.addObject(className);
        objectForm.setFieldValue(By.id(className + "_0_prop"), "testing value");
        vp = oep.clickSaveAndView();

        assertEquals("this is the content: testing value", vp.getContent());
    }

    @Test
    @Order(2)
    void deleteProperty(TestUtils setup, TestReference reference)
    {
        DocumentReference editObjectsTestClass = getTestClassDocumentReference(reference);
        DocumentReference editObjectsTestObject = getTestObjectDocumentReference(reference);
        String className = setup.serializeReference(editObjectsTestClass.getLocalDocumentReference());

        // Create a class with two string properties
        setup.addClassProperty(editObjectsTestClass, "prop1", "String");
        setup.addClassProperty(editObjectsTestClass, "prop2", "String");

        // Create object page
        setup.createPage(editObjectsTestObject,
            "this is the content: {{velocity}}$doc.display('prop1')/$doc.display('prop2')/" 
                + "$!doc.getObject('" + className + "').getProperty('prop1').value{{/velocity}}",
            "");

        setup.addObject(editObjectsTestObject, className,
            "prop1", "testing value 1", "prop2", "testing value 2");

        ViewPage vp = setup.gotoPage(editObjectsTestObject);

        assertEquals("this is the content: testing value 1/testing value 2/testing value 1", vp.getContent());

        // Delete the first property from the class
        ClassEditPage cep = setup.editClass(editObjectsTestClass);
        cep.deleteProperty("prop1");
        cep.clickSaveAndView();

        vp = setup.gotoPage(editObjectsTestObject);
        assertEquals("this is the content: /testing value 2/testing value 1", vp.getContent());

        ObjectEditPage oep = vp.editObjects();
        List<ObjectEditPane> objectsOfClass = oep.getObjectsOfClass(className);
        assertEquals(1, objectsOfClass.size());
        objectsOfClass.get(0).displayObject();
        assertNotNull(setup.getDriver().findElement(By.className("deprecatedProperties")));
        assertNotNull(setup.getDriver().findElement(By.cssSelector(".deprecatedProperties label")));
        assertEquals("prop1:", setup.getDriver().findElement(By.cssSelector(".deprecatedProperties label")).getText());

        // Remove deprecated properties
        oep.removeAllDeprecatedProperties();
        vp = oep.clickSaveAndView();
        assertEquals("this is the content: /testing value 2/", vp.getContent());
    }

    @Test
    @Order(3)
    void addInvalidProperty(TestUtils setup, TestReference reference)
    {
        DocumentReference editObjectsTestClass = getTestClassDocumentReference(reference);
        ClassEditPage cep = setup.editClass(editObjectsTestClass);
        cep.addPropertyWithoutWaiting("a<b c", "String");
        String errorMessage =
        setup.getDriver().findElement(By.xpath("//div[contains(@class,'xnotification-error')]")).getText();
        assertEquals("Failed:Property names must follow these naming rules:\n"
            + "Names can contain letters, numbers, and the following characters: \"., -, _, :\"\n"
            + "Names must not start with a number or punctuation character.\n"
            + "Names must not start with the letters xml (or XML, or Xml, etc).\n"
            + "Names cannot contain spaces.", errorMessage);
        /*cep.waitForNotificationErrorMessage("Failed:Property names must follow these naming rules:<br/>"
            + "Names can contain letters, numbers, and the following characters: \"., -, _, :\"<br/>"
            + "Names must not start with a number or punctuation character.<br/>"
            + "Names must not start with the letters xml (or XML, or Xml, etc).<br/>"
            + "Names cannot contain spaces.");*/
    }

    /**
     * Test adding and removing a property without leaving the class editor UI.
     */
    @Test
    @Order(4)
    void addAndDeleteProperty(TestUtils setup, TestReference reference) throws Exception
    {
        setup.rest().savePage(reference, "Some content", "");
        ClassEditPage classEditPage = setup.editClass(reference);
        classEditPage.addProperty("age", "Number");
        classEditPage.addProperty("color", "String");
        classEditPage.deleteProperty("color");
        classEditPage.clickSaveAndView();

        classEditPage = setup.editClass(reference);
        assertTrue(classEditPage.hasProperty("age"));
        assertFalse(classEditPage.hasProperty("color"));
    }

    @Test
    @Order(5)
    void reorderProperty(TestUtils setup, TestReference reference) throws Exception
    {
        setup.rest().savePage(reference, "Some content", "");
        ClassEditPage classEditPage = setup.editClass(reference);
        classEditPage.addProperty("testA", "Number");
        classEditPage.addProperty("testB", "Number");
        classEditPage.addProperty("testC", "Number");
        classEditPage.clickSaveAndView();

        classEditPage = setup.editClass(reference);
        assertEquals(List.of("testA", "testB", "testC"), classEditPage.getProperties());

        classEditPage.movePropertyBefore("testC", "testB");
        assertEquals(List.of("testA", "testC", "testB"), classEditPage.getProperties());
        classEditPage.clickSaveAndView();

        classEditPage = setup.editClass(reference);
        assertEquals(List.of("testA", "testC", "testB"), classEditPage.getProperties());
    }

    private DocumentReference getTestClassDocumentReference(TestReference reference)
    {
        return new DocumentReference("TestClass", reference.getLastSpaceReference());
    }

    private DocumentReference getTestObjectDocumentReference(TestReference reference)
    {
        return new DocumentReference("TestObjects", reference.getLastSpaceReference());
    }
}
