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
package org.xwiki.tour.test.po;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.editor.EditPage;
import org.xwiki.text.StringUtils;

/**
 * @version $Id$
 * @since 15.9RC1
 */
public class StepEditModal extends EditPage
{
    @FindBy(xpath = "(//div[@id='stepForm']//input[@type='text'])[1]")
    private WebElement elementField;

    @FindBy(xpath = "(//div[@id='stepForm']//input[@type='text'])[2]")
    private WebElement titleField;

    @FindBy(xpath = "//div[@id='stepForm']//textarea")
    private WebElement contentField;

    @FindBy(xpath = "(//div[@id='stepForm']//select)[1]")
    private WebElement placementField;

    @FindBy(xpath = "(//div[@id='stepForm']//select)[2]")
    private WebElement orderField;

    @FindBy(xpath = "(//div[@id='stepForm']//input[@type='checkbox'])[1]")
    private WebElement backdropField;

    @FindBy(xpath = "(//div[@id='stepForm']//input[@type='text'])[3]")
    private WebElement targetPageField;

    @FindBy(id = "saveStepBtn")
    private WebElement saveButton;

    @FindBy(className = "xdialog-close")
    private WebElement closeButton;

    public String getElement()
    {
        return elementField.getAttribute("value");
    }

    public void setElement(String element)
    {
        elementField.clear();
        elementField.sendKeys(element);
    }

    public String getTitle()
    {
        return titleField.getAttribute("value");
    }

    public void setTitle(String title)
    {
        titleField.clear();
        titleField.sendKeys(title);
    }

    public String getContent()
    {
        return contentField.getText();
    }

    public void setContent(String content)
    {
        contentField.clear();
        contentField.sendKeys(content);
    }

    public String getPlacement()
    {
        return placementField.getAttribute("value");
    }

    public void setPlacement(String placement)
    {
        List<WebElement> options = placementField.findElements(By.tagName("option"));
        for (WebElement option : options) {
            if (StringUtils.equals(option.getAttribute("value"), placement)) {
                option.click();
            }
        }
    }

    public int getOrder()
    {
        return Integer.parseInt(orderField.getAttribute("value")) + 1;
    }

    public void setOrder(int order)
    {
        String orderInString = String.valueOf(order - 1);
        List<WebElement> options = orderField.findElements(By.tagName("option"));
        for (WebElement option : options) {
            if (StringUtils.equals(option.getAttribute("value"), orderInString)) {
                option.click();
            }
        }
    }

    public boolean isBackdropEnabled()
    {
        return StringUtils.isNotEmpty(backdropField.getAttribute("checked"));
    }

    public void setBackdrop(boolean enabled)
    {
        if (isBackdropEnabled() != enabled) {
            backdropField.click();
        }
    }

    public String getTargetPage()
    {
        return targetPageField.getAttribute("value");
    }

    public void setTargetPage(String targetPage)
    {
        targetPageField.clear();
        targetPageField.sendKeys(targetPage);
    }

    public void close()
    {
        getDriver().scrollTo(closeButton);
        closeButton.click();
        getDriver().waitUntilElementDisappears(By.id("stepForm"));
    }

    public void save()
    {
        saveButton.click();
        getDriver().waitUntilElementDisappears(By.id("stepForm"));
    }

}
