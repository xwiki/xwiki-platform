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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.xwiki.test.ui.browser.IgnoreBrowser;
import org.xwiki.test.ui.po.FormContainerElement;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.ClassEditPage;
import org.xwiki.test.ui.po.editor.ObjectEditPage;

/**
 * Test XClass editing.
 *
 * @version $Id$
 * @since 2.4RC1
 */
public class EditClassTest extends AbstractTest
{
    @Rule
    public AdminAuthenticationRule adminAuthenticationRule = new AdminAuthenticationRule(true, getUtil());

    @Before
    public void setUp() throws Exception
    {
        getUtil().rest().deletePage("Test", "EditObjectsTestClass");
        getUtil().rest().deletePage("Test", "EditObjectsTestObject");
    }

    @Test
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146")
    public void testAddProperty()
    {
        // We verify that we can click on Edit Class from View Page (we need to test this at
        // least once to ensure the UI works).
        getUtil().gotoPage("Test", "EditObjectsTestClass", "view", "spaceRedirect=false");
        ClassEditPage cep = new ViewPage().editClass();

        // Create a class with a string property
        cep.addProperty("prop", "String");
        cep.clickSaveAndView();

        // Create object page
        ViewPage vp = getUtil().createPage("Test", "EditObjectsTestObject",
            "this is the content: {{velocity}}$doc.display('prop'){{/velocity}}", getTestMethodName());

        // Add an object of the class created
        ObjectEditPage oep = vp.editObjects();
        FormContainerElement objectForm = oep.addObject("Test.EditObjectsTestClass");
        objectForm.setFieldValue(By.id("Test.EditObjectsTestClass_0_prop"), "testing value");
        vp = oep.clickSaveAndView();

        Assert.assertEquals("this is the content: testing value", vp.getContent());
    }

    @Test
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146")
    public void testDeleteProperty()
    {
        // Create a class with two string properties
        getUtil().addClassProperty("Test", "EditObjectsTestClass", "prop1", "String");
        getUtil().addClassProperty("Test", "EditObjectsTestClass", "prop2", "String");

        // Create object page
        ViewPage vp = getUtil().createPage("Test", "EditObjectsTestObject",
            "this is the content: {{velocity}}$doc.display('prop1')/$doc.display('prop2')/" +
            "$!doc.getObject('Test.EditObjectsTestClass').getProperty('prop1').value{{/velocity}}",
            getTestMethodName());

        // Add an object of the class created
        ObjectEditPage oep = vp.editObjects();
        FormContainerElement objectForm = oep.addObject("Test.EditObjectsTestClass");
        objectForm.setFieldValue(By.id("Test.EditObjectsTestClass_0_prop1"), "testing value 1");
        objectForm.setFieldValue(By.id("Test.EditObjectsTestClass_0_prop2"), "testing value 2");
        vp = oep.clickSaveAndView();

        Assert.assertEquals("this is the content: testing value 1/testing value 2/testing value 1", vp.getContent());

        // Delete the first property from the class
        ClassEditPage cep = getUtil().editClass("Test", "EditObjectsTestClass");
        cep.deleteProperty("prop1");
        cep.clickSaveAndView();

        vp = getUtil().gotoPage("Test", "EditObjectsTestObject");
        Assert.assertEquals("this is the content: /testing value 2/testing value 1", vp.getContent());

        oep = vp.editObjects();
        Assert.assertNotNull(getDriver().findElement(By.className("deprecatedProperties")));
        Assert.assertNotNull(getDriver().findElement(By.cssSelector(".deprecatedProperties label")));
        Assert.assertEquals("prop1:", getDriver().findElement(By.cssSelector(".deprecatedProperties label")).getText());

        // Remove deprecated properties
        oep.removeAllDeprecatedProperties();
        vp = oep.clickSaveAndView();
        Assert.assertEquals("this is the content: /testing value 2/", vp.getContent());
    }

    @Test
    public void addInvalidProperty()
    {
        ClassEditPage cep = getUtil().editClass("Test", "EditObjectsTestClass");
        cep.addPropertyWithoutWaiting("a<b c", "String");
        cep.waitForNotificationErrorMessage("Failed: Property names must follow these naming rules:");
    }
}
