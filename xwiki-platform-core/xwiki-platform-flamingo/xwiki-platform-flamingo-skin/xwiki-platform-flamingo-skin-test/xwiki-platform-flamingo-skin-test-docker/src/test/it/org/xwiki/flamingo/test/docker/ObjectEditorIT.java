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

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.UnhandledAlertException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.FormContainerElement;
import org.xwiki.test.ui.po.HistoryPane;
import org.xwiki.test.ui.po.SuggestInputElement;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.BootstrapDateTimePicker;
import org.xwiki.test.ui.po.editor.ClassEditPage;
import org.xwiki.test.ui.po.editor.ObjectEditPage;
import org.xwiki.test.ui.po.editor.ObjectEditPane;
import org.xwiki.test.ui.po.editor.StaticListClassEditElement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for the object editor.
 *
 * @since 12.4RC1
 * @version $Id$
 */
@UITest
class ObjectEditorIT
{
    // We're using classes with a nested space to ensure class names with multiple dots are not causing problems.
    private static final String NUMBER_CLASS = "ObjectEditorIT.NestedSpace.NumberClass";
    private static final String STRING_CLASS = "ObjectEditorIT.NestedSpace.StringClass";

    @BeforeAll
    public void setup(TestUtils setup)
    {
        setup.loginAsSuperAdmin();

        DocumentReference numberClassReference =
            new DocumentReference("xwiki", Arrays.asList("ObjectEditorIT","NestedSpace"), "NumberClass");
        DocumentReference stringClassReference =
            new DocumentReference("xwiki", Arrays.asList("ObjectEditorIT","NestedSpace"), "StringClass");

        setup.createPage(numberClassReference, "", "NumberClass");
        setup.addClassProperty(numberClassReference, "number", "Number");

        setup.createPage(stringClassReference, "", "StringClass");
        setup.addClassProperty(stringClassReference, "string", "String");
    }
    
    @Test
    @Order(1)
    void preventUsersToLeaveTheEditorWithoutSaving(TestUtils testUtils, TestReference testReference)
    {
        Assumptions.assumeFalse(StringUtils.equalsIgnoreCase("firefox",
                testUtils.getDriver().getCapabilities().getBrowserName()),
            "Alert handling in Firefox currently isn't working, see also https://jira.xwiki.org/browse/XWIKI-22282");

        // fixture
        testUtils.deletePage(testReference);
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

    @Test
    @Order(2)
    void addingAndDeletingMultipleObjects(TestUtils testUtils, TestReference testReference)
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
        List<ObjectEditPane> numberObjects = objectEditPage.getObjectsOfClass(NUMBER_CLASS, false);
        assertEquals(2, numberObjects.size());

        number1 = numberObjects.get(0);
        number1.displayObject();
        assertEquals(0, number1.getObjectNumber());
        assertTrue(number1.isDeleteLinkDisplayed());
        assertTrue(number1.isEditLinkDisplayed());
        assertEquals("24", number1.getFieldValue(number1.byPropertyName("number")));

        number2 = numberObjects.get(1);
        number2.displayObject();
        assertEquals(1, number2.getObjectNumber());
        assertTrue(number2.isDeleteLinkDisplayed());
        assertTrue(number2.isEditLinkDisplayed());
        assertEquals("42", number2.getFieldValue(number2.byPropertyName("number")));

        List<ObjectEditPane> stringObjects = objectEditPage.getObjectsOfClass(STRING_CLASS);
        assertEquals(1, stringObjects.size());

        string1 = stringObjects.get(0);
        string1.displayObject();
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
        number1.displayObject();
        assertEquals(0, number1.getObjectNumber());
        assertTrue(number1.isDeleteLinkDisplayed());
        assertTrue(number1.isEditLinkDisplayed());
        assertEquals("84", number1.getFieldValue(number1.byPropertyName("number")));

        stringObjects = objectEditPage.getObjectsOfClass(STRING_CLASS);
        assertEquals(2, stringObjects.size());

        string1 = stringObjects.get(0);
        string1.displayObject();
        assertEquals(0, string1.getObjectNumber());
        assertTrue(string1.isDeleteLinkDisplayed());
        assertTrue(string1.isEditLinkDisplayed());
        assertEquals("foobar", string1.getFieldValue(string1.byPropertyName("string")));

        string2 = stringObjects.get(1);
        string2.displayObject();
        assertEquals(1, string2.getObjectNumber());
        assertTrue(string2.isDeleteLinkDisplayed());
        assertTrue(string2.isEditLinkDisplayed());
        assertEquals("something else", string2.getFieldValue(string2.byPropertyName("string")));

        // check version
        viewPage = testUtils.gotoPage(testReference);
        historyPane = viewPage.openHistoryDocExtraPane();
        assertEquals("2.1", historyPane.getCurrentVersion());
    }

    /**
     * XWIKI-5832: Cannot create groups or add members to existing groups using the object editor.
     */
    @Test
    @Order(3)
    void createEmptyGroup(TestUtils testUtils, TestReference testReference)
    {
        ViewPage vp = testUtils.createPage(testReference, "this is the content");

        // Add an XWikiGroups object
        ObjectEditPage oep = vp.editObjects();
        oep.addObject("XWiki.XWikiGroups");
        vp = oep.clickSaveAndView();

        oep = vp.editObjects();
        assertEquals(1, oep.getObjectsOfClass("XWiki.XWikiGroups").size());
    }

    @Test
    @Order(4)
    void changeMultiselectProperty(TestUtils testUtils, TestReference testReference)
    {
        String dedicatedSpace = testReference.getLastSpaceReference().getName();
        String testClassPage = "TestClass";
        String testClassFullName = String.format("%s.%s", dedicatedSpace, testClassPage);
        ViewPage page = testUtils.createPage(dedicatedSpace, testClassPage, "", "");
        // Create a class with a database list property set to return all documents
        ClassEditPage cep = page.editClass();
        cep.addProperty("prop", "DBList");
        cep.getDatabaseListClassEditElement("prop").setHibernateQuery(
            String.format("select doc.fullName from XWikiDocument doc where doc.space = '%s'", dedicatedSpace));
        cep.clickSaveAndView();

        // Create a second page to hold the Object and set its content
        String testObjectPage = "TestObject";
        ViewPage vp = testUtils.createPage(dedicatedSpace, testObjectPage, "this is the content", "");

        // Add an object of the class created and set the value to be the test page
        ObjectEditPage oep = vp.editObjects();
        FormContainerElement objectForm = oep.addObject(testClassFullName);
        objectForm.setFieldValue(By.id(String.format("%s_0_prop", testClassFullName)), testClassFullName);
        oep.clickSaveAndView();

        // Set multiselect to true
        cep = ClassEditPage.gotoPage(dedicatedSpace, testClassPage);
        cep.getDatabaseListClassEditElement("prop").setMultiSelect(true);
        cep.clickSaveAndView();

        // Select a second document in the DB list select field.
        oep = ObjectEditPage.gotoPage(dedicatedSpace, testObjectPage);
        ObjectEditPane objectEditPane = oep.getObjectsOfClass(testClassFullName).get(0);
        objectEditPane.displayObject();
        objectEditPane.setFieldValue(
            By.id(String.format("%s_0_prop", testClassFullName)),
            String.format("%s.%s", dedicatedSpace, testObjectPage));
        vp = oep.clickSaveAndView();

        assertEquals("this is the content", vp.getContent());
    }

    @Test
    @Order(5)
    void changeNumberType(TestUtils testUtils, TestReference testReference)
    {
        String dedicatedSpace = testReference.getLastSpaceReference().getName();
        String testClassPage = "TestClass";
        String testClassFullName = String.format("%s.%s", dedicatedSpace, testClassPage);
        String testObjectPage = "TestObject";
        String propertyClassId = String.format("%s_0_prop", testClassFullName);
        // Create class page
        ViewPage vp = testUtils.createPage(dedicatedSpace, testClassPage, "this is the content", "");

        // Add class
        ClassEditPage cep = vp.editClass();
        cep.addProperty("prop", "Number");
        cep.getNumberClassEditElement("prop").setNumberType("integer");
        vp = cep.clickSaveAndView();
        assertEquals("this is the content", vp.getContent());

        // Create object page
        vp = testUtils.createPage(dedicatedSpace, testObjectPage,
            "this is the content: {{velocity}}$doc.display('prop'){{/velocity}}", "");

        // Add object
        ObjectEditPage oep = vp.editObjects();
        FormContainerElement objectForm = oep.addObject(testClassFullName);
        objectForm.setFieldValue(By.id(propertyClassId), "3");
        vp = oep.clickSaveAndView();
        assertEquals("this is the content: 3", vp.getContent());

        // Change number to double type
        cep = ClassEditPage.gotoPage(dedicatedSpace, testClassPage);
        cep.getNumberClassEditElement("prop").setNumberType("double");
        vp = cep.clickSaveAndView();
        assertEquals("this is the content", vp.getContent());

        // Verify conversion
        oep = ObjectEditPage.gotoPage(dedicatedSpace, testObjectPage);
        ObjectEditPane objectEditPane = oep.getObjectsOfClass(testClassFullName).get(0);
        objectEditPane.displayObject();
        objectEditPane.setFieldValue(By.id(propertyClassId), "2.5");
        vp = oep.clickSaveAndView();
        assertEquals("this is the content: 2.5", vp.getContent());

        // Change number to long type
        cep = ClassEditPage.gotoPage(dedicatedSpace, testClassPage);
        cep.getNumberClassEditElement("prop").setNumberType("long");
        vp = cep.clickSaveAndView();
        assertEquals("this is the content", vp.getContent());

        // Verify conversion
        oep = ObjectEditPage.gotoPage(dedicatedSpace, testObjectPage);
        vp = oep.clickSaveAndView();
        assertEquals("this is the content: 2", vp.getContent());
    }

    @Test
    @Order(6)
    void changeListMultipleSelect(TestUtils testUtils, TestReference testReference)
    {
        String dedicatedSpace = testReference.getLastSpaceReference().getName();
        String testClassPage = "TestClass";
        String testClassFullName = String.format("%s.%s", dedicatedSpace, testClassPage);
        String testObjectPage = "TestObject";
        String propertyClassId = String.format("%s_0_prop", testClassFullName);

        // Create class page
        ViewPage vp = testUtils.createPage(dedicatedSpace, testClassPage, "this is the content", "");

        // Add class
        ClassEditPage cep = vp.editClass();
        cep.addProperty("prop", "StaticList");
        StaticListClassEditElement slcee = cep.getStaticListClassEditElement("prop");
        slcee.setMultiSelect(false);
        slcee.setValues("choice 1|choice 2|choice 3|choice 4|choice 5");
        vp = cep.clickSaveAndView();
        assertEquals("this is the content", vp.getContent());

        // Create object page
        vp = testUtils.createPage(dedicatedSpace, testObjectPage,
            "this is the content: {{velocity}}$doc.display('prop'){{/velocity}}", "");

        // Add object
        ObjectEditPage oep = vp.editObjects();
        FormContainerElement objectForm = oep.addObject(testClassFullName);
        objectForm.setFieldValue(By.id(propertyClassId), "choice 3");
        vp = oep.clickSaveAndView();
        assertEquals("this is the content: choice 3", vp.getContent());

        // Change list to a multiple select.
        cep = ClassEditPage.gotoPage(dedicatedSpace, testClassPage);
        cep.getStaticListClassEditElement("prop").setMultiSelect(true);
        vp = cep.clickSaveAndView();
        assertEquals("this is the content", vp.getContent());

        // Verify conversion
        oep = ObjectEditPage.gotoPage(dedicatedSpace, testObjectPage);
        ObjectEditPane objectEditPane = oep.getObjectsOfClass(testClassFullName).get(0);
        objectEditPane.displayObject();
        objectEditPane.setFieldValue(By.id(propertyClassId), "choice 3");
        objectEditPane.setFieldValue(By.id(propertyClassId), "choice 4");
        vp = oep.clickSaveAndView();
        assertEquals("this is the content: choice 3 choice 4", vp.getContent());
    }

    @Test
    @Order(7)
    void changeListTypeRelationalStorage(TestUtils testUtils, TestReference testReference)
    {
        String dedicatedSpace = testReference.getLastSpaceReference().getName();
        String testClassPage = "TestClass";
        String testClassFullName = String.format("%s.%s", dedicatedSpace, testClassPage);
        String testObjectPage = "TestObject";
        String propertyClassId = String.format("%s_0_prop", testClassFullName);

        // Create class page
        ViewPage vp = testUtils.createPage(dedicatedSpace, testClassPage, "this is the content", "");

        // Add class
        ClassEditPage cep = vp.editClass();
        cep.addProperty("prop", "StaticList");
        StaticListClassEditElement slcee = cep.getStaticListClassEditElement("prop");
        slcee.setMultiSelect(true);
        slcee.setDisplayType(StaticListClassEditElement.DisplayType.INPUT);
        vp = cep.clickSaveAndView();
        assertEquals("this is the content", vp.getContent());

        // Create object page
        vp = testUtils.createPage(dedicatedSpace, testObjectPage,
            "this is the content: {{velocity}}$doc.display('prop'){{/velocity}}", "");

        // Add object
        ObjectEditPage oep = vp.editObjects();
        FormContainerElement objectForm = oep.addObject(testClassFullName);
        objectForm.setFieldValue(By.id(propertyClassId), "this|that|other");
        vp = oep.clickSaveAndView();
        assertEquals("this is the content: this that other", vp.getContent());

        // Change list to relational storage.
        cep = ClassEditPage.gotoPage(dedicatedSpace, testClassPage);
        cep.getStaticListClassEditElement("prop").setRelationalStorage(true);
        vp = cep.clickSaveAndView();
        assertEquals("this is the content", vp.getContent());

        // Make sure we can still edit the object.
        oep = ObjectEditPage.gotoPage(dedicatedSpace, testObjectPage);
        ObjectEditPane objectEditPane = oep.getObjectsOfClass(testClassFullName).get(0);
        objectEditPane.displayObject();
        objectEditPane.setFieldValue(
            By.id(propertyClassId), "this|other");
        vp = oep.clickSaveAndView();
        assertEquals("this is the content: this other", vp.getContent());

        // Change list to non-relational storage.
        cep = ClassEditPage.gotoPage(dedicatedSpace, testClassPage);
        cep.getStaticListClassEditElement("prop").setRelationalStorage(false);
        vp = cep.clickSaveAndView();
        assertEquals("this is the content", vp.getContent());

        // Make sure we can still edit the object.
        oep = ObjectEditPage.gotoPage(dedicatedSpace, testObjectPage);
        objectEditPane = oep.getObjectsOfClass(testClassFullName).get(0);
        objectEditPane.displayObject();
        objectEditPane.setFieldValue(
            By.id(propertyClassId), "that|other");
        vp = oep.clickSaveAndView();
        assertEquals("this is the content: that other", vp.getContent());
    }

    @Test
    @Order(8)
    void objectAddAndRemove(TestReference testReference)
    {
        String dedicatedSpace = testReference.getLastSpaceReference().getName();
        String testObjectPage = "TestObject";
        String property = String.format("%s_%%s_string", STRING_CLASS);

        ObjectEditPage oep = ObjectEditPage.gotoPage(dedicatedSpace, testObjectPage);
        FormContainerElement object = oep.addObject(STRING_CLASS);
        object.setFieldValue(By.id(String.format(property, 0)), "John");

        // Add another object
        FormContainerElement object2 = oep.addObject(STRING_CLASS);

        // Check that the unsaved value from the first object wasn't lost
        assertEquals("John", object.getFieldValue(By.id(String.format(property, 0))));
        // Check that the value from the second object is unset
        assertEquals("", object2.getFieldValue(By.id(String.format(property, 1))));

        // Delete the second object
        oep.deleteObject(STRING_CLASS, 1);

        // Let's save the form and check that changes were persisted.
        oep = oep.clickSaveAndView().editObjects();
        List<ObjectEditPane> xwikiStringsForms = oep.getObjectsOfClass(STRING_CLASS);
        assertEquals(1, xwikiStringsForms.size());
        ObjectEditPane objectEditPane = xwikiStringsForms.get(0);
        objectEditPane.displayObject();
        assertEquals("John", objectEditPane.getFieldValue(By.id(String.format(property, 0))));
    }

    @Test
    @Order(9)
    void inlineObjectAddButton(TestReference testReference)
    {
        String dedicatedSpace = testReference.getLastSpaceReference().getName();
        String testObjectPage = "TestObject";
        ObjectEditPage oep = ObjectEditPage.gotoPage(dedicatedSpace, testObjectPage);
        oep.addObject(STRING_CLASS);
        oep.addObjectFromInlineLink(STRING_CLASS);
        assertEquals(2, oep.getObjectsOfClass(STRING_CLASS).size());

        // Save & view to avoid getting an alert when moving to next test.
        oep.clickSaveAndView();
    }

    @Test
    @Order(10)
    void propertyDisplayersForNewObjects(TestUtils testUtils, TestReference testReference) throws Exception
    {
        String dedicatedSpace = testReference.getLastSpaceReference().getName();
        String testClassPage = "TestClass";
        String testClassFullName = String.format("%s.%s", dedicatedSpace, testClassPage);
        testUtils.rest().deletePage(dedicatedSpace, testClassPage);
        testUtils.createAdminUser();
        testUtils.loginAsSuperAdmin();

        // Create a class with two properties: a date and a list of users.
        ClassEditPage classEditor = ClassEditPage.gotoPage(dedicatedSpace, testClassPage);
        classEditor.addProperty("date", "Date");
        classEditor.addProperty("author", "Users");

        // Add an object of this class and set its properties.
        ObjectEditPage objectEditor = ObjectEditPage.gotoPage(dedicatedSpace, testClassPage);
        ObjectEditPane object = objectEditor.addObject(testClassFullName);
        object.openDatePicker("date").selectDay("15").close();
        object.getSuggestInput("author").sendKeys("ad").waitForSuggestions().selectByVisibleText("Admin");

        // Save, edit again and check the values.
        object = objectEditor.clickSaveAndView().editObjects().getObjectsOfClass(testClassFullName).get(0);
        object.displayObject();
        BootstrapDateTimePicker datePicker = object.openDatePicker("date");
        assertEquals("15", datePicker.getSelectedDay());
        datePicker.close();
        SuggestInputElement.SuggestionElement author = object.getSuggestInput("author").getSelectedSuggestions().get(0);
        assertEquals("Admin", author.getLabel());
        assertEquals("XWiki.Admin", author.getValue());
    }
}
