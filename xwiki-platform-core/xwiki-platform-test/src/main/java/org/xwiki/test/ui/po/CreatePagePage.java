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
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.pagefactory.ByChained;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;

/**
 * Represents the actions possible on the Create Page template page.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class CreatePagePage extends ViewPage
{
    @FindBy(id = "space")
    private WebElement spaceTextField;

    @FindBy(id = "page")
    private WebElement pageTextField;

    public static CreatePagePage gotoPage()
    {
        getUtil().gotoPage("Main", "WebHome", "create");
        return new CreatePagePage();
    }

    public String getSpace()
    {
        return spaceTextField.getAttribute("value");
    }

    public void setSpace(String space)
    {
        this.spaceTextField.clear();
        this.spaceTextField.sendKeys(space);
    }

    public String getPage()
    {
        return pageTextField.getAttribute("value");
    }

    public void setPage(String page)
    {
        this.pageTextField.clear();
        this.pageTextField.sendKeys(page);
    }

    /**
     * @since 3.2M3
     */
    public int getAvailableTemplateSize()
    {
        // When there's no template available a hidden input with a blank value remains.
        return getDriver().findElements(By.name("templateprovider")).size() - 1;
    }

    public List<String> getAvailableTemplates()
    {
        List<String> availableTemplates = new ArrayList<String>();
        List<WebElement> templateInputs = getDriver().findElements(By.name("templateprovider"));
        for (WebElement input : templateInputs) {
            if (input.getAttribute("value").length() > 0) {
                availableTemplates.add(input.getAttribute("value"));
            }
        }

        return availableTemplates;
    }

    public void setTemplate(String template)
    {
        // Select the correct radio element corresponding to the passed template name.
        // TODO: For some reason the following isn't working. Find out why.
        // List<WebElement> templates = getDriver().findElements(
        // new ByChained(By.name("template"), By.tagName("input")));
        List<WebElement> templates = getDriver().findElements(By.name("templateprovider"));
        for (WebElement templateInput : templates) {
            if (templateInput.getAttribute("value").equals(template)) {
                templateInput.click();
                return;
            }
        }
        throw new RuntimeException("Failed to find template [" + template + "]");
    }

    public void clickCreate()
    {
        this.pageTextField.submit();
    }

    public WYSIWYGEditPage createPage(String spaceValue, String pageValue)
    {
        setSpace(spaceValue);
        setPage(pageValue);
        clickCreate();
        return new WYSIWYGEditPage();
    }

    public WYSIWYGEditPage createPageFromTemplate(String spaceValue, String pageValue, String templateValue)
    {
        setSpace(spaceValue);
        setPage(pageValue);
        setTemplate(templateValue);
        clickCreate();
        return new WYSIWYGEditPage();
    }

    /**
     * Waits for a global error message in the page.
     *
     * @since 3.2M3
     */
    public void waitForErrorMessage()
    {
        getDriver().waitUntilElementIsVisible(By.className("errormessage"));
    }

    /**
     * Waits for a validation error in a field.
     *
     * @since 3.2M3
     */
    public void waitForFieldErrorMessage()
    {
        getDriver().waitUntilElementIsVisible(new ByChained(By.className("LV_invalid")));
    }
}
