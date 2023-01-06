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
package org.xwiki.appwithinminutes.test.ui;

import java.time.Duration;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.xwiki.appwithinminutes.test.po.ApplicationClassEditPage;
import org.xwiki.appwithinminutes.test.po.ClassFieldEditPane;
import org.xwiki.appwithinminutes.test.po.EntryEditPage;
import org.xwiki.appwithinminutes.test.po.LongTextClassFieldEditPane;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.XWikiWebDriver;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.ObjectEditPage;
import org.xwiki.test.ui.po.editor.ObjectEditPane;
import org.xwiki.xclass.test.po.ClassSheetPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.xwiki.appwithinminutes.test.po.ApplicationClassEditPage.EMPTY_CANVAS_HINT;

/**
 * Tests the application class editor.
 *
 * @version $Id$
 * @since 14.10
 * @since 14.4.7
 * @since 13.10.10
 */
@UITest(properties = {
    // Exclude the AppWithinMinutes.ClassEditSheet and AppWithinMinutes.DynamicMessageTool from the PR checker since 
    // they use the groovy macro which requires PR rights.
    // TODO: Should be removed once XWIKI-20529 is closed.
    // Exclude AppWithinMinutes.LiveTableEditSheet because it calls com.xpn.xwiki.api.Document.saveWithProgrammingRights
    "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:AppWithinMinutes\\.(ClassEditSheet|DynamicMessageTool|LiveTableEditSheet)"
})
class ClassEditorIT
{
    @BeforeEach
    void setUp(TestUtils setup, TestReference testReference)
    {
        setup.loginAsSuperAdmin();
        setup.deleteSpace(testReference.getLastSpaceReference());
    }

    /**
     * Tests that the hint is displayed only when the canvas is empty.
     */
    @Test
    @Order(1)
    void emptyCanvasHint(TestReference testReference)
    {
        ApplicationClassEditPage editor = goToEditor(testReference);
        assertTrue(editor.getContent().contains(EMPTY_CANVAS_HINT));
        ClassFieldEditPane field = editor.addField("Short Text");
        assertFalse(editor.getContent().contains(EMPTY_CANVAS_HINT));
        field.delete().clickYes();
        assertTrue(editor.getContent().contains(EMPTY_CANVAS_HINT));
    }

    /**
     * Tests that the field display is updated when the configuration panel is closed.
     */
    @Test
    @Order(2)
    void applyConfigurationChanges(TestReference testReference)
    {
        ApplicationClassEditPage editor = goToEditor(testReference);

        LongTextClassFieldEditPane longTextField =
            new LongTextClassFieldEditPane(editor.addField("Long Text").getName());
        longTextField.openConfigPanel();
        longTextField.setRows(3);
        longTextField.setEditor("Wiki");
        longTextField.closeConfigPanel();
        assertEquals(3, longTextField.getPreviewRows());
    }

    /**
     * Tests that class fields can be deleted and that documents having objects of that class are updated.
     */
    @Test
    @Order(3)
    void deleteField(TestUtils setup, TestReference testReference)
    {
        ApplicationClassEditPage editor = goToEditor(testReference);
        // Add two fields.
        editor.addField("Boolean").setPrettyName("Available");
        editor.addField("Date").setPrettyName("Day");

        // Save and assert they are present.
        ViewPage classView = editor.clickSaveAndView();
        assertTrue(classView.getContent().contains("Available (boolean1: Boolean)"));
        assertTrue(classView.getContent().contains("Day (date1: Date)"));

        // Edit again and delete one of the fields.
        classView.edit();
        new ClassFieldEditPane("boolean1").delete().clickYes();

        // Save and check if the field was removed.
        classView = new ApplicationClassEditPage().clickSaveAndView();
        assertFalse(classView.getContent().contains("Available (boolean1: Boolean)"));
        assertTrue(classView.getContent().contains("Day (date1: Date)"));

        // Edit the class template and see if the deleted field is now deprecated.
        ObjectEditPage objectEditor = new ClassSheetPage().clickTemplateLink().editObjects();
        String className = setup.serializeReference(testReference.getLocalDocumentReference().getParent());
        List<ObjectEditPane> objectsOfClass = objectEditor.getObjectsOfClass(className);
        assertEquals(1, objectsOfClass.size());
        objectsOfClass.get(0).displayObject();
        assertTrue(objectEditor.isPropertyDeprecated(className, "boolean1"));
        assertFalse(objectEditor.isPropertyDeprecated(className, "date1"));
    }

    /**
     * Tests that class fields can be reordered.
     */
    @Test
    @Order(4)
    void reorderFields(TestReference testReference)
    {
        ApplicationClassEditPage editor = goToEditor(testReference);
        // Add two class fields.
        editor.addField("Date").setPrettyName("Start Date");
        editor.addField("Date").setPrettyName("End Date");

        // Save and edit the class template in in-line edit mode.
        editor.clickSaveAndView();
        new ClassSheetPage().clickTemplateLink().edit();

        // Assert the order of the form fields.
        List<String> fieldNames = new EntryEditPage().getFieldNames();
        assertEquals("date1", fieldNames.get(0));
        assertEquals("date2", fieldNames.get(1));

        // Go back to the class editor.
        editor = goToEditor(testReference);

        // Change the order of the class fields.
        editor.moveFieldBefore("date2", "date1");

        // Save and edit the class template again.
        editor.clickSaveAndView();
        new ClassSheetPage().clickTemplateLink().edit();

        // Assert the order of the form fields.
        fieldNames = new EntryEditPage().getFieldNames();
        assertEquals("date2", fieldNames.get(0));
        assertEquals("date1", fieldNames.get(1));
    }

    /**
     * Tests that class fields can be renamed.
     */
    @Test
    @Order(5)
    void renameField(TestUtils setup, TestReference testReference)
    {
        ApplicationClassEditPage editor = goToEditor(testReference);
        // Add a class field.
        editor.addField("Number").setDefaultValue("13");

        // Save and edit the class template.
        editor.clickSaveAndView();
        new ClassSheetPage().clickTemplateLink().edit();

        // Change the field value.
        EntryEditPage inlineEditor = new EntryEditPage();
        assertEquals("13", inlineEditor.getValue("number1"));
        inlineEditor.setValue("number1", "27");

        // Save and edit again the class.
        inlineEditor.clickSaveAndView();
        setup.gotoPage(testReference.getParent());
        new ViewPage().edit();

        // Rename the class field.
        ClassFieldEditPane field = new ClassFieldEditPane("number1");
        field.openConfigPanel();
        field.setName("age");

        // Save and edit again the class template.
        new ApplicationClassEditPage().clickSaveAndView();
        new ClassSheetPage().clickTemplateLink().edit();
        assertEquals("27", new EntryEditPage().getValue("age"));
    }

    /**
     * Tests that invalid field names are not allowed.
     */
    @Test
    @Order(6)
    void invalidFieldName(TestUtils setup, TestReference testReference)
    {
        ApplicationClassEditPage editor = goToEditor(testReference);
        String invalidFieldNameErrorMessage = "Property names must follow these naming rules:";

        ClassFieldEditPane field = editor.addField("Static List");
        field.openConfigPanel();
        field.setName("3times");
        // Save the page and expect the error.
        editor.getSaveAndViewButton().click();
        waitForPageSourceContains(setup, invalidFieldNameErrorMessage);

        editor = goToEditor(testReference);
        field = editor.addField("User");
        field.openConfigPanel();
        // Unfortunately we don't allow Unicode letters because they are not fully supported in tag names.
        // See XWIKI-7306: The class editor doesn't validate properly the field names
        field.setName("\u021Bar\u0103");
        // Save the page and expect the error.
        editor.getSaveAndViewButton().click();
        waitForPageSourceContains(setup, invalidFieldNameErrorMessage);

        editor = goToEditor(testReference);
        field = editor.addField("Group");
        field.openConfigPanel();
        field.setName("alice>bob");
        // Save the page and expect the error.
        editor.getSaveAndViewButton().click();
        waitForPageSourceContains(setup, invalidFieldNameErrorMessage);
    }

    /**
     * Tests that two class fields can't have the same name.
     */
    @Test
    @Order(7)
    void duplicateFieldName(TestUtils setup, TestReference testReference)
    {
        ApplicationClassEditPage editor = goToEditor(testReference);
        ClassFieldEditPane field = editor.addField("Short Text");
        field.setPrettyName("Alice");
        field.openConfigPanel();
        field.setName("carol");

        field = editor.addField("Short Text");
        field.setPrettyName("Bob");
        field.openConfigPanel();
        field.setName("carol");

        // Save the page and expect the error.
        editor.getSaveAndViewButton().click();
        waitForPageSourceContains(setup, "The class has two fields with the same name: carol");
    }

    /**
     * Tests that swapping field names is not allowed.
     */
    @Test
    @Order(8)
    void swapFieldNames(TestUtils setup, TestReference testReference)
    {
        ApplicationClassEditPage editor = goToEditor(testReference);
        ClassFieldEditPane field = editor.addField("Short Text");
        field.openConfigPanel();
        field.setName("alice");

        field = editor.addField("Number");
        field.openConfigPanel();
        field.setName("bob");

        editor.clickSaveAndView().edit();
        editor = new ApplicationClassEditPage();

        field = new ClassFieldEditPane("alice");
        field.openConfigPanel();
        field.setName("bob");

        field = new ClassFieldEditPane("bob");
        field.openConfigPanel();
        field.setName("alice");

        // Save the page and expect the error.
        editor.getSaveAndViewButton().click();
        waitForPageSourceContains(setup, "The class has two fields with the same name: alice");
    }

    /**
     * Tests the options to update the class sheet and the class template.
     */
    @Test
    @Order(9)
    void updateSheetAndTemplate(TestReference testReference)
    {
        ApplicationClassEditPage editor = goToEditor(testReference);
        // The options panel is not displayed if the class template and sheet don't exists.
        assertFalse(editor.getContent().contains("Update class template"));

        // Add a class field.
        editor.addField("Number");

        // Save and edit again.
        editor.clickSaveAndView().edit();
        editor = new ApplicationClassEditPage();

        // Set default value for the previously added field.
        new ClassFieldEditPane("number1").setDefaultValue("9");

        // Add a new field
        editor.addField("Database List");

        // The options panel should be displayed now.
        editor.setUpdateClassSheet(false);

        // Save and edit the template.
        editor.clickSaveAndView();
        new ClassSheetPage().clickTemplateLink().edit();

        // The sheet should display only the first field.
        EntryEditPage inlineEditor = new EntryEditPage();
        List<String> fieldNames = inlineEditor.getFieldNames();
        assertEquals(1, fieldNames.size());
        assertEquals("number1", fieldNames.get(0));

        // Assert the value of the first field. The class template should have been updated.
        assertEquals("9", inlineEditor.getValue("number1"));
    }

    /**
     * Tests the Save & Continue button.
     */
    @Test
    @Order(10)
    void saveAndContinue(TestReference testReference)
    {
        ApplicationClassEditPage editor = goToEditor(testReference);
        editor.addField("Date");
        editor.clickSaveAndContinue();

        // Check if the field was added.
        ViewPage viewer = editor.clickCancel();
        assertTrue(viewer.getContent().contains("Date (date1: Date)"));

        // Edit again. This time check the error message.
        viewer.edit();
        editor = new ApplicationClassEditPage();

        // Try to set the field name to an invalid value.
        ClassFieldEditPane field = new ClassFieldEditPane("date1");
        field.openConfigPanel();
        field.setName("-delta");

        editor.clickSaveAndContinue(false);
        editor.waitForNotificationErrorMessage("Failed to save the page.");

        // Double check that the field wasn't renamed.
        assertTrue(editor.clickCancel().getContent().contains("Date (date1: Date)"));
    }

    /**
     * Tests that fields names are auto-generated properly.
     */
    @Test
    @Order(11)
    void fieldNameAutoGeneration(TestReference testReference)
    {
        ApplicationClassEditPage editor = goToEditor(testReference);
        // Add a class field and set its name to an auto-generated field name for a different type.
        ClassFieldEditPane field = editor.addField("Short Text");
        field.openConfigPanel();
        field.setName("number1");

        editor.clickSaveAndContinue();

        // Add a new field of the type implied by the name set to the previous field.
        field = editor.addField("Number");
        field.openConfigPanel();
        assertEquals("number2", field.getName());

        // Save and assert both fields have been added.
        ViewPage viewer = editor.clickSaveAndView();
        assertTrue(viewer.getContent().contains("Short Text (number1: String)"));
        assertTrue(viewer.getContent().contains("Number (number2: Number)"));
    }

    /**
     * Test that Save And Continue supports field renames.
     */
    @Test
    @Order(12)
    void renameWithSaveAndContinue(TestReference testReference)
    {
        ApplicationClassEditPage editor = goToEditor(testReference);
        ClassFieldEditPane field = editor.addField("Short Text");
        editor.clickSaveAndContinue();

        // Rename the field.
        field.openConfigPanel();
        field.setName("title");
        editor.clickSaveAndContinue();

        // Rename the field for a second time.
        // NOTE: The IDs have been changed so we must recreate the class field edit pane.
        new ClassFieldEditPane("title").setName("city");

        // Save and assert the field was added with the right name.
        assertTrue(editor.clickSaveAndView().getContent().contains("Short Text (city: String)"));
    }

    /**
     * Waits until the page source contains the given text or the timeout expires.
     * <p>
     * NOTE: Normally we shouldn't need to use this method, i.e. we should be able to assert the source page directly
     * because Selenium should wait until the page is loaded but this doesn't happen all the time for some reason...
     */
    private void waitForPageSourceContains(TestUtils setup, String text)
    {
        XWikiWebDriver driver = setup.getDriver();
        new WebDriverWait(driver, Duration.ofSeconds(driver.getTimeout()))
            .until(d -> StringUtils.contains(driver.getPageSource(), text));
    }

    private ApplicationClassEditPage goToEditor(TestReference testReference)
    {
        return ApplicationClassEditPage.goToEditor(testReference.getLastSpaceReference());
    }
}
