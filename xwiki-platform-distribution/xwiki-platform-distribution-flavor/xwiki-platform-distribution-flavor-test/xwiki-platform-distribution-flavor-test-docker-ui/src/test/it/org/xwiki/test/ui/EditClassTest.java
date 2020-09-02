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
package org.xwiki.test.ui;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.browser.IgnoreBrowser;
import org.xwiki.test.ui.po.FormContainerElement;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.ClassEditPage;
import org.xwiki.test.ui.po.editor.ObjectEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test XClass editing.
 *
 * @version $Id$
 * @since 12.8RC1
 */
@UITest
public class EditClassTest
{
    // @Rule
    // public AdminAuthenticationRule adminAuthenticationRule = new AdminAuthenticationRule(true, testUtils);

    
    
    @Test
    @Order(1)
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146")
    void testAddProperty(TestUtils testUtils, TestReference testReference)
    {
        // We verify that we can click on Edit Class from View Page (we need to test this at
        // least once to ensure the UI works).
        testUtils.gotoPage("Test", "EditObjectsTestClass", "view", "spaceRedirect=false");
        ClassEditPage cep = new ViewPage().editClass();

        // Create a class with a string property
        cep.addProperty("prop", "String");
        cep.clickSaveAndView();

        // Create object page
        ViewPage vp = testUtils.createPage("Test", "EditObjectsTestObject",
            "this is the content: {{velocity}}$doc.display('prop'){{/velocity}}", testReference.getName());

        // Add an object of the class created
        ObjectEditPage oep = vp.editObjects();
        FormContainerElement objectForm = oep.addObject("Test.EditObjectsTestClass");
        objectForm.setFieldValue(By.id("Test.EditObjectsTestClass_0_prop"), "testing value");
        vp = oep.clickSaveAndView();

        assertEquals("this is the content: testing value", vp.getContent());
    }

    @Test
    @Order(2)
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146")
    void testDeleteProperty(TestUtils testUtils, TestReference testReference)
    {
        // Create a class with two string properties
        testUtils.addClassProperty("Test", "EditObjectsTestClass", "prop1", "String");
        testUtils.addClassProperty("Test", "EditObjectsTestClass", "prop2", "String");

        // Create object page
        testUtils.createPage("Test", "EditObjectsTestObject",
            "this is the content: {{velocity}}$doc.display('prop1')/$doc.display('prop2')/" +
            "$!doc.getObject('Test.EditObjectsTestClass').getProperty('prop1').value{{/velocity}}",
            testReference.getName());

        testUtils.addObject("Test", "EditObjectsTestObject", "Test.EditObjectsTestClass",
            "prop1", "testing value 1", "prop2", "testing value 2");

        ViewPage vp = testUtils.gotoPage("Test", "EditObjectsTestObject");

        assertEquals("this is the content: testing value 1/testing value 2/testing value 1", vp.getContent());

        // Delete the first property from the class
        ClassEditPage cep = testUtils.editClass("Test", "EditObjectsTestClass");
        cep.deleteProperty("prop1");
        cep.clickSaveAndView();

        vp = testUtils.gotoPage("Test", "EditObjectsTestObject");
        assertEquals("this is the content: /testing value 2/testing value 1", vp.getContent());

        ObjectEditPage oep = vp.editObjects();
        XWikiWebDriver driver = testUtils.getDriver();
        assertNotNull(driver.findElement(By.className("deprecatedProperties")));
        assertNotNull(driver.findElement(By.cssSelector(".deprecatedProperties label")));
        assertEquals("prop1:", driver.findElement(By.cssSelector(".deprecatedProperties label")).getText());

        // Remove deprecated properties
        oep.removeAllDeprecatedProperties();
        vp = oep.clickSaveAndView();
        assertEquals("this is the content: /testing value 2/", vp.getContent());
    }

    @Test
    @Order(3)
    void addInvalidProperty(TestUtils testUtils)
    {
        ClassEditPage cep = testUtils.editClass("Test", "EditObjectsTestClass");
        cep.addPropertyWithoutWaiting("a<b c", "String");
        cep.waitForNotificationErrorMessage("Failed: Property names must follow these naming rules:");
    }
}
