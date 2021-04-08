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

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * Represents the widget used to select a page template.
 * 
 * @version $Id$
 * @since 12.9RC1
 */
public class PageTypePicker extends BaseElement
{
    private WebElement container;

    public PageTypePicker()
    {
        this.container = getDriver().findElementByCssSelector(".xwiki-select.page-type");
        waitUntilReady();
    }

    public PageTypePicker(WebElement container)
    {
        this.container = container;
        waitUntilReady();
    }

    private List<WebElement> getAvailableTemplateInputs()
    {
        return getDriver().findElementsWithoutWaiting(this.container,
            By.xpath("//input[@name = 'type' and @data-type = 'template']"));
    }

    private List<WebElement> getAvailableTypeInputs()
    {
        return getDriver().findElementsWithoutWaiting(this.container, By.xpath("//input[@name = 'type']"));
    }

    public int countAvailableTemplates()
    {
        return getAvailableTemplateInputs().size();
    }

    public List<String> getAvailableTemplates()
    {
        List<String> availableTemplates = new ArrayList<String>();
        List<WebElement> templateInputs = getAvailableTemplateInputs();
        for (WebElement input : templateInputs) {
            if (input.getAttribute("value").length() > 0) {
                availableTemplates.add(input.getAttribute("value"));
            }
        }

        return availableTemplates;
    }

    public void selectTemplateByValue(String template)
    {
        List<WebElement> templates = getAvailableTemplateInputs();
        for (WebElement templateInput : templates) {
            if (templateInput.getAttribute("value").equals(template)) {
                // Get the label corresponding to the input so we can click on it
                WebElement label = getDriver()
                    .findElementWithoutWaiting(By.xpath("//label[@for = '" + templateInput.getAttribute("id") + "']"));
                label.click();
                return;
            }
        }
        throw new RuntimeException("Failed to find template [" + template + "]");
    }

    public void selectTypeByValue(String type)
    {
        List<WebElement> types = getAvailableTypeInputs();
        for (WebElement typeInput : types) {
            if (typeInput.getAttribute("value").equals(type)) {
                // Get the label corresponding to the input so we can click on it
                WebElement label = getDriver()
                    .findElementWithoutWaiting(By.xpath("//label[@for = '" + typeInput.getAttribute("id") + "']"));
                label.click();
                return;
            }
        }
        throw new RuntimeException("Failed to find type [" + type + "]");
    }

    protected void waitUntilReady()
    {
        getDriver().waitUntilCondition(ExpectedConditions.attributeToBe(this.container, "data-ready", "true"));
    }
}
