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
package org.xwiki.test.ui.po.editor;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.BasePage;
import org.xwiki.test.ui.po.FormElement;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents the common actions possible on all Pages when using the "edit" action with the "class" editor.
 * 
 * @version $Id$
 * @since 3.2M3
 */
// TODO: Fix the fact that this class should extend EditPage but the id of the save action is incorrectly different...
public class ClassEditPage extends BasePage
{
    @FindBy(name = "action_saveandcontinue")
    private WebElement saveandcontinue;

    @FindBy(name = "action_propupdate")
    private WebElement saveandview;

    @FindBy(name = "action_cancel")
    private WebElement cancel;

    @FindBy(id = "propupdate")
    private WebElement propertyForm;

    @FindBy(id = "propname")
    private WebElement propertyNameField;

    @FindBy(id = "proptype")
    private WebElement propertyTypeField;

    @FindBy(name = "action_propadd")
    private WebElement propertySubmit;

    private FormElement form;

    /**
     * Go to the passed page in object edit mode.
     */
    public static ClassEditPage gotoPage(String space, String page)
    {
        getUtil().gotoPage(space, page, "edit", "editor=class");
        return new ClassEditPage();
    }

    public void addProperty(String propertyName, String propertyType)
    {
        addPropertyWithoutWaiting(propertyName, propertyType);

        // Make sure we wait for the element to appear since there's no page refresh.
        waitUntilElementIsVisible(By.id("xproperty_" + propertyName));
    }

    /**
     * @since 3.2M3
     */
    public void addPropertyWithoutWaiting(String propertyName, String propertyType)
    {
        getForm().setFieldValue(this.propertyNameField, propertyName);
        getForm().setFieldValue(this.propertyTypeField, propertyType);
        this.propertySubmit.click();
    }

    public void deleteProperty(String propertyName)
    {
        final By propertyLocator = By.id("xproperty_" + propertyName);
        final WebElement propertyContainer = getDriver().findElement(propertyLocator);
        WebElement deleteLink = propertyContainer.findElement(By.className("delete"));
        deleteLink.click();

        // Expect a confirmation box
        waitUntilElementIsVisible(By.className("xdialog-box-confirmation"));
        getDriver().findElement(By.cssSelector(".xdialog-box-confirmation input[value='Yes']")).click();
        waitUntilElementDisappears(propertyLocator);
    }

    private FormElement getForm()
    {
        if (this.form == null) {
            this.form = new FormElement(this.propertyForm);
        }
        return this.form;
    }

    public DatabaseListClassEditElement getDatabaseListClassEditElement(String propertyName)
    {
        // Make the element visible before returning it
        By locator = By.id("xproperty_" + propertyName + "_title");
        waitUntilElementIsVisible(locator);
        getDriver().findElement(locator).click();
        return new DatabaseListClassEditElement(getForm(), propertyName);
    }

    public StaticListClassEditElement getStaticListClassEditElement(String propertyName)
    {
        // Make the element visible before returning it
        By locator = By.id("xproperty_" + propertyName + "_title");
        waitUntilElementIsVisible(locator);
        getDriver().findElement(locator).click();
        return new StaticListClassEditElement(getForm(), propertyName);
    }

    public NumberClassEditElement getNumberClassEditElement(String propertyName)
    {
        // Make the element visible before returning it
        By locator = By.id("xproperty_" + propertyName + "_title");
        waitUntilElementIsVisible(locator);
        getDriver().findElement(locator).click();
        return new NumberClassEditElement(getForm(), propertyName);
    }

    public void clickSaveAndContinue()
    {
        this.saveandcontinue.click();

        // Wait until the page is really saved
        waitUntilElementIsVisible(By.xpath("//div[contains(@class,'xnotification-done') and text()='Saved']"));
    }

    public ViewPage clickSaveAndView()
    {
        this.saveandview.click();
        return new ViewPage();
    }

    public ViewPage clickCancel()
    {
        this.cancel.click();
        return new ViewPage();
    }
}
