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

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.editor.EditPage;

/**
 * @version $Id$
 * @since 15.9RC1
 */
public class TourEditPage extends EditPage
{
    @FindBy(id = "TourCode.TourClass_0_description")
    private WebElement description;

    @FindBy(id = "TourCode.TourClass_0_isActive")
    private WebElement isActive;

    @FindBy(id = "TourCode.TourClass_0_targetPage")
    private WebElement targetPage;

    @FindBy(id = "TourCode.TourClass_0_targetClass")
    private WebElement targetClass;

    @FindBy(xpath = "//div[@id='stepsBlk']//a[@id='addStep']")
    private WebElement addStepButton;

    @FindBy(id = "stepsContainer")
    private WebElement stepsContainer;

    public void setDescription(String description)
    {
        this.description.clear();
        this.description.sendKeys(description);
    }

    public void setIsActive(boolean isActive)
    {
        if (!this.isActive.getAttribute("checked").isEmpty() != isActive) {
            this.isActive.click();
        }
    }

    public void setTargetPage(String targetPage)
    {
        this.targetPage.clear();
        this.targetPage.sendKeys(targetPage);
    }

    public void setTargetClass(String targetClass)
    {
        this.targetClass.clear();
        this.targetClass.sendKeys(targetClass);
    }

    public StepEditModal newStep()
    {
        addStepButton.click();
        getDriver().waitUntilElementIsVisible(By.id("stepForm"));
        return new StepEditModal();
    }

    public StepEditModal editStep(int number)
    {
        WebElement stepEditLink =
            stepsContainer.findElement(By.xpath(String.format("(//a[contains(@class, 'editStep')])[%d]", number)));
        stepEditLink.click();
        getDriver().waitUntilElementIsVisible(By.id("stepForm"));
        return new StepEditModal();
    }

    public void deleteStep(int number, boolean confirm)
    {
        WebElement deleteLink =
            stepsContainer.findElement(By.xpath(String.format("(//a[contains(@class, 'deleteStep')])[%d]", number)));
        deleteLink.click();
        getDriver().waitUntilElementIsVisible(By.className("xdialog-box-confirmation"));
        WebElement confirmBox = getDriver().findElement(By.className("xdialog-content"));
        if (confirm) {
            confirmBox.findElement(By.xpath("//input[@value = 'Yes']")).click();
        } else {
            confirmBox.findElement(By.xpath("//input[@value = 'No']")).click();
        }
        getDriver().waitUntilElementDisappears(By.className("xdialog-box-confirmation"));
        waitForNotificationSuccessMessage("Delete step done!");

    }
}
