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
package org.xwiki.flamingo.test.ui;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.UnhandledAlertException;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.HistoryPane;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.ObjectEditPage;
import org.xwiki.test.ui.po.editor.ObjectEditPane;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the object editor.
 *
 * @since 12.4RC1
 * @version $Id$
 */
@UITest
public class ObjectEditorIT
{
    private final static String NUMBER_CLASS = "ObjectEditorIT.NumberClass";
    private final static String STRING_CLASS = "ObjectEditorIT.StringClass";

    @BeforeAll
    public void setup(TestUtils setup)
    {
        setup.loginAsSuperAdmin();

        setup.createPage("ObjectEditorIT", "NumberClass", "", "NumberClass");
        setup.addClassProperty("ObjectEditorIT", "NumberClass", "number", "Number");

        setup.createPage("ObjectEditorIT", "StringClass", "", "StringClass");
        setup.addClassProperty("ObjectEditorIT", "StringClass", "string", "String");
    }

    @Order(1)
    @Test
    public void preventUsersToLeaveTheEditorWithoutSaving(TestUtils testUtils, TestReference testReference)
    {
        // fixture
        testUtils.createPage(testReference, "Some content");
        testUtils.addObject(testReference, NUMBER_CLASS, "number", 42);

        ViewPage viewPage = testUtils.gotoPage(testReference);
        ObjectEditPage objectEditPage = viewPage.editObjects();
        List<ObjectEditPane> xobjects = objectEditPage.getObjectsOfClass(NUMBER_CLASS);
        assertEquals(1, xobjects.size());
        ObjectEditPane objectEditPane = xobjects.get(0);

        // check some values for the already saved xobject
        assertEquals(0, objectEditPane.getObjectNumber());
        assertTrue(objectEditPane.isDeleteLinkDisplayed());
        assertTrue(objectEditPane.isEditLinkDisplayed());

        // we should be able to leave the editor without any warning
        viewPage = testUtils.gotoPage(testReference);

        // come back to the editor and create a new object
        objectEditPage = viewPage.editObjects();
        objectEditPane = objectEditPage.addObject(NUMBER_CLASS);
        assertEquals(1, objectEditPane.getObjectNumber());
        assertTrue(objectEditPane.isDeleteLinkDisplayed());
        assertFalse(objectEditPane.isEditLinkDisplayed());

        try {
            // should open a confirmation modal for leaving since we didn't save
            testUtils.gotoPage(testReference);
            fail("A confirm alert should be triggered");
        } catch (UnhandledAlertException e) {
            Alert alert = testUtils.getDriver().switchTo().alert();
            alert.dismiss(); // remain on the page
        }

        objectEditPage.deleteObject(NUMBER_CLASS, 1);

        // State should be same as before adding
        xobjects = objectEditPage.getObjectsOfClass(NUMBER_CLASS);
        assertEquals(1, xobjects.size());
        objectEditPane = xobjects.get(0);
        assertEquals(0, objectEditPane.getObjectNumber());
        assertTrue(objectEditPane.isDeleteLinkDisplayed());
        assertTrue(objectEditPane.isEditLinkDisplayed());

        // We should be able to leave without warning now
        viewPage = testUtils.gotoPage(testReference);
        objectEditPage = viewPage.editObjects();

        // Ensure nothing has been saved previously
        xobjects = objectEditPage.getObjectsOfClass(NUMBER_CLASS);
        assertEquals(1, xobjects.size());

        // Delete the saved object
        objectEditPage.deleteObject(NUMBER_CLASS, 0);

        try {
            // should open a confirmation modal for leaving
            testUtils.gotoPage(testReference);
            fail("A confirm alert should be triggered");
        } catch (UnhandledAlertException e) {
            Alert alert = testUtils.getDriver().switchTo().alert();
            alert.dismiss();
        }

        objectEditPage.clickSaveAndContinue();

        // we should be able to leave the editor without any warning now
        viewPage = testUtils.gotoPage(testReference);

        // Ensure the changes have been properly saved.
        objectEditPage = viewPage.editObjects();
        xobjects = objectEditPage.getObjectsOfClass(NUMBER_CLASS);

        assertTrue(xobjects.isEmpty());
    }

    @Order(2)
    @Test
    public void addingAndDeletingMultipleObjects(TestUtils testUtils, TestReference testReference)
    {
        ViewPage viewPage = testUtils.createPage(testReference, "Some content"); // version 1.1
        ObjectEditPage objectEditPage = viewPage.editObjects();

        assertTrue(objectEditPage.getObjectsOfClass(NUMBER_CLASS).isEmpty());
        assertTrue(objectEditPage.getObjectsOfClass(STRING_CLASS).isEmpty());

        // Adding a first object number and ensure it's all ok
        ObjectEditPane number1 = objectEditPage.addObject(NUMBER_CLASS);
        assertEquals(0, number1.getObjectNumber());
        assertTrue(number1.isDeleteLinkDisplayed());
        assertFalse(number1.isEditLinkDisplayed());
        number1.setPropertyValue("number", "24");

        // Add a second one: check that the delete link of the first one is now hidden.
        ObjectEditPane number2 = objectEditPage.addObject(NUMBER_CLASS);
        assertEquals(1, number2.getObjectNumber());
        assertTrue(number2.isDeleteLinkDisplayed());
        assertFalse(number2.isEditLinkDisplayed());
        assertFalse(number1.isDeleteLinkDisplayed());
        number2.setPropertyValue("number", "42");

        ObjectEditPane string1 = objectEditPage.addObject(STRING_CLASS);
        assertEquals(0, string1.getObjectNumber());
        assertTrue(string1.isDeleteLinkDisplayed());
        assertFalse(string1.isEditLinkDisplayed());
        string1.setPropertyValue("string", "foobar");

        // When we save, we also display all links that were hidden
        objectEditPage.clickSaveAndContinue(); // version 1.2

        assertTrue(number1.isDeleteLinkDisplayed());
        assertTrue(number1.isEditLinkDisplayed());
        assertTrue(number2.isDeleteLinkDisplayed());
        assertTrue(number2.isEditLinkDisplayed());
        assertTrue(string1.isDeleteLinkDisplayed());
        assertTrue(string1.isEditLinkDisplayed());

        // ensure that deleting an object and adding a new one of same type erase the content
        objectEditPage.deleteObject(STRING_CLASS, 0);
        string1 = objectEditPage.addObject(STRING_CLASS);
        assertEquals(0, string1.getObjectNumber());
        assertTrue(string1.isDeleteLinkDisplayed());
        assertFalse(string1.isEditLinkDisplayed());
        assertTrue(string1.getFieldValue(string1.byPropertyName("string")).isEmpty());

        // Check behaviour of adding objects with inline links
        ObjectEditPane string2 = objectEditPage.addObjectFromInlineLink(STRING_CLASS);
        assertEquals(1, string2.getObjectNumber());
        assertTrue(string2.isDeleteLinkDisplayed());
        assertFalse(string2.isEditLinkDisplayed());
        assertFalse(string1.isDeleteLinkDisplayed());

        ObjectEditPane string3 = objectEditPage.addObjectFromInlineLink(STRING_CLASS);
        assertEquals(2, string3.getObjectNumber());
        assertTrue(string3.isDeleteLinkDisplayed());
        assertFalse(string3.isEditLinkDisplayed());
        assertFalse(string2.isDeleteLinkDisplayed());
        assertFalse(string1.isDeleteLinkDisplayed());

        // ensure cancel works and don't save anything
        number1.setPropertyValue("number", "84");
        viewPage = objectEditPage.clickCancel();

        // check version
        HistoryPane historyPane = viewPage.openHistoryDocExtraPane();
        assertEquals("1.2", historyPane.getCurrentVersion());

        // check the state of the editor when we come back to it
        objectEditPage = viewPage.editObjects();
        List<ObjectEditPane> numberObjects = objectEditPage.getObjectsOfClass(NUMBER_CLASS);
        assertEquals(2, numberObjects.size());

        number1 = numberObjects.get(0);
        assertEquals(0, number1.getObjectNumber());
        assertTrue(number1.isDeleteLinkDisplayed());
        assertTrue(number1.isEditLinkDisplayed());
        assertEquals("24", number1.getFieldValue(number1.byPropertyName("number")));

        number2 = numberObjects.get(1);
        assertEquals(1, number2.getObjectNumber());
        assertTrue(number2.isDeleteLinkDisplayed());
        assertTrue(number2.isEditLinkDisplayed());
        assertEquals("42", number2.getFieldValue(number2.byPropertyName("number")));

        List<ObjectEditPane> stringObjects = objectEditPage.getObjectsOfClass(STRING_CLASS);
        assertEquals(1, stringObjects.size());

        string1 = stringObjects.get(0);
        assertEquals(0, string1.getObjectNumber());
        assertTrue(string1.isDeleteLinkDisplayed());
        assertTrue(string1.isEditLinkDisplayed());
        assertEquals("foobar", string1.getFieldValue(string1.byPropertyName("string")));

        objectEditPage.deleteObject(NUMBER_CLASS, 1);
        number1.setPropertyValue("number", "84");

        string2 = objectEditPage.addObjectFromInlineLink(STRING_CLASS);
        string2.setPropertyValue("string", "something else");

        // check save&view
        viewPage = objectEditPage.clickSaveAndView(); // version 2.1

        objectEditPage = viewPage.editObjects();
        numberObjects = objectEditPage.getObjectsOfClass(NUMBER_CLASS);
        assertEquals(1, numberObjects.size());
        number1 = numberObjects.get(0);
        assertEquals(0, number1.getObjectNumber());
        assertTrue(number1.isDeleteLinkDisplayed());
        assertTrue(number1.isEditLinkDisplayed());
        assertEquals("84", number1.getFieldValue(number1.byPropertyName("number")));

        stringObjects = objectEditPage.getObjectsOfClass(STRING_CLASS);
        assertEquals(2, stringObjects.size());

        string1 = stringObjects.get(0);
        assertEquals(0, string1.getObjectNumber());
        assertTrue(string1.isDeleteLinkDisplayed());
        assertTrue(string1.isEditLinkDisplayed());
        assertEquals("foobar", string1.getFieldValue(string1.byPropertyName("string")));

        string2 = stringObjects.get(1);
        assertEquals(1, string2.getObjectNumber());
        assertTrue(string2.isDeleteLinkDisplayed());
        assertTrue(string2.isEditLinkDisplayed());
        assertEquals("something else", string2.getFieldValue(string2.byPropertyName("string")));

        // check version
        viewPage = testUtils.gotoPage(testReference);
        historyPane = viewPage.openHistoryDocExtraPane();
        assertEquals("2.1", historyPane.getCurrentVersion());
    }
}
