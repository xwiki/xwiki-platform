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
package org.xwiki.filter.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents the Filter Application home page.
 *
 * @version $Id$
 * @since 16.2.0RC1
 * @since 15.10.7
 */
public class ApplicationFilterHomePage extends ViewPage
{
    @FindBy(id = "filter_input_type")
    private WebElement inputType;

    @FindBy(id = "filter_output_type")
    private WebElement outType;

    @FindBy(name = "convert")
    private WebElement convertButton;

    /**
     * Go to the home page of the Application Index application.
     */
    public static ApplicationFilterHomePage gotoPage()
    {
        getUtil().gotoPage("Filter", "WebHome");

        return new ApplicationFilterHomePage();
    }

    public void setInputFilter(String typeId)
    {
        new Select(this.inputType).selectByValue(typeId);
    }

    public void setOutputFilter(String typeId)
    {
        new Select(this.outType).selectByValue(typeId);
    }

    public WebElement getInputField(String propertyName)
    {
        return getDriver().findElement(By.id("filter_input_properties_descriptor_" + propertyName));
    }

    public WebElement getOutputField(String propertyName)
    {
        return getDriver().findElement(By.id("filter_output_properties_descriptor_" + propertyName));
    }

    public void setInputProperty(String propertyName, String value)
    {
        WebElement inputElement = getInputField(propertyName);
        inputElement.sendKeys(value);
    }

    public void setOutputProperty(String propertyName, String value)
    {
        WebElement outputElement = getOutputField(propertyName);
        outputElement.sendKeys(value);
    }

    public void setSource(String source)
    {
        setInputProperty("source", source);
    }

    public void setTarget(String target)
    {
        setOutputProperty("target", target);
    }

    public void convert()
    {
        // Click convert
        this.convertButton.click();

        // Wait until the conversion job is finished
        getDriver().waitUntilElementDisappears(By.className("ui-progress"));
    }
}
