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
package org.xwiki.test.ui.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Represents the common actions possible on all Pages when using the "inline" action.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class InlinePage extends ViewPage
{
    /**
     * The XPath that locates a form field.
     */
    private static final String FIELD_XPATH_FORMAT = "//*[substring(@name, string-length(@name) - %s - 2) = '_0_%s']";

    @FindBy(name = "action_preview")
    private WebElement preview;

    @FindBy(name = "action_saveandcontinue")
    private WebElement saveandcontinue;

    @FindBy(name = "action_save")
    private WebElement save;

    @FindBy(name = "action_cancel")
    private WebElement cancel;

    @FindBy(id = "inline")
    private WebElement form;

    public void clickPreview()
    {
        preview.click();
    }

    public void clickSaveAndContinue()
    {
        this.saveandcontinue.click();

        // Wait until the page is really saved.
        waitForNotificationSuccessMessage("Saved");
    }

    public <T extends ViewPage> T clickSaveAndView()
    {
        save.click();
        return createViewPage();
    }

    public <T extends ViewPage> T clickCancel()
    {
        cancel.click();
        return createViewPage();
    }

    /**
     * Can be overridden to return extended {@link ViewPage}.
     */
    protected <T extends ViewPage> T createViewPage()
    {
        return (T) new ViewPage();
    }

    @Override
    public String getContent()
    {
        return form.getText();
    }

    /**
     * @return the form element
     */
    public WebElement getForm()
    {
        return form;
    }

    /**
     * Retrieves the value of the specified form field
     *
     * @param fieldName the name of a form field
     * @return the value of the specified form field
     * @since 7.0RC1
     */
    public String getValue(String fieldName)
    {
        String xpath = String.format(FIELD_XPATH_FORMAT, fieldName.length(), fieldName);
        return new FormElement(getForm()).getFieldValue(By.xpath(xpath));
    }

    /**
     * Sets the value of the specified form field
     *
     * @param fieldName the name of a form field
     * @param fieldValue the new value for the specified form field
     * @since 7.0RC1
     */
    public void setValue(String fieldName, String fieldValue)
    {
        String xpath = String.format(FIELD_XPATH_FORMAT, fieldName.length(), fieldName);
        WebElement field = getForm().findElement(By.xpath(xpath));
        if (field.getAttribute("name").equals(field.getAttribute("id"))) {
            new FormElement(getForm()).setFieldValue(field, fieldValue);
        } else {
            xpath = String.format("//*[@name = '%s' and @value = '%s']", field.getAttribute("name"), fieldValue);
            new FormElement(getForm()).setCheckBox(By.xpath(xpath), true);
        }
    }
}
