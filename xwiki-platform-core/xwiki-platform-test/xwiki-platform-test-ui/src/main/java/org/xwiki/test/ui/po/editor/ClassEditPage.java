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
import org.xwiki.test.ui.po.FormContainerElement;

/**
 * Represents the common actions possible on all Pages when using the "edit" action with the "class" editor.
 *
 * @version $Id$
 * @since 3.2M3
 */
public class ClassEditPage extends EditPage
{
    @FindBy(name = "action_propupdate")
    private WebElement saveandview;

    @FindBy(id = "propupdate")
    private WebElement propertyForm;

    @FindBy(id = "propname")
    private WebElement propertyNameField;

    @FindBy(id = "proptype")
    private WebElement propertyTypeField;

    @FindBy(name = "action_propadd")
    private WebElement propertySubmit;

    private FormContainerElement form;

    /**
     * Go to the passed page in object edit mode.
     */
    public static ClassEditPage gotoPage(String space, String page)
    {
        getUtil().gotoPage(space, page, "edit", "editor=class");
        return new ClassEditPage();
    }

    public ClassPropertyEditPane addProperty(String propertyName, String propertyType)
    {
        addPropertyWithoutWaiting(propertyName, propertyType);

        // The following call waits for the element to appear since there's no page refresh.
        return getPropertyEditPane(propertyName);
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
        getDriver().waitUntilElementIsVisible(By.className("xdialog-box-confirmation"));
        getDriver().findElement(By.cssSelector(".xdialog-box-confirmation input[value='Yes']")).click();
        getDriver().waitUntilElementDisappears(propertyLocator);
    }

    private FormContainerElement getForm()
    {
        if (this.form == null) {
            this.form = new FormContainerElement(By.id("propupdate"));
        }
        return this.form;
    }

    /**
     * Use this method if you need to set generic meta-properties (common to all XClass property types). For specific
     * meta-properties use the methods dedicated to each XClass property type.
     *
     * @param propertyName the name of a property of this class
     * @return the pane used to edit the specified property
     * @since 4.5
     */
    public ClassPropertyEditPane getPropertyEditPane(String propertyName)
    {
        return new ClassPropertyEditPane(getForm(), propertyName).expand();
    }

    public DatabaseListClassEditElement getDatabaseListClassEditElement(String propertyName)
    {
        return (DatabaseListClassEditElement) new DatabaseListClassEditElement(getForm(), propertyName).expand();
    }

    public StaticListClassEditElement getStaticListClassEditElement(String propertyName)
    {
        return (StaticListClassEditElement) new StaticListClassEditElement(getForm(), propertyName).expand();
    }

    public NumberClassEditElement getNumberClassEditElement(String propertyName)
    {
        return (NumberClassEditElement) new NumberClassEditElement(getForm(), propertyName).expand();
    }

    @Override
    public WebElement getSaveAndViewButton()
    {
        return saveandview;
    }
}
