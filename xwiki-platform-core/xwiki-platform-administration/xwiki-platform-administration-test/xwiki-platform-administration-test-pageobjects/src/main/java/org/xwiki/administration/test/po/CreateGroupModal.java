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
package org.xwiki.administration.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.xwiki.test.ui.po.BaseModal;

/**
 * Represents the Create Group modal.
 * 
 * @version $Id$
 * @since 10.9RC1
 */
public class CreateGroupModal extends BaseModal
{
    @FindBy(id = "createGroupModal-groupName")
    private WebElement groupNameInput;

    @FindBy(css = "#createGroupModal .btn-primary")
    private WebElement createGroupButton;

    /**
     * Default constructor.
     */
    public CreateGroupModal()
    {
        super(By.id("createGroupModal"));
    }

    public WebElement getGroupNameInput()
    {
        return this.groupNameInput;
    }

    public CreateGroupModal setGroupName(String groupName)
    {
        this.groupNameInput.clear();
        this.groupNameInput.sendKeys(groupName);
        return this;
    }

    public CreateGroupModal waitForValidationError(String message)
    {
        By validationErrorMessage = By.xpath(
            "//dd[contains(@class, 'has-error')]/span[contains(@class, 'help-block') and . = '" + message + "']");
        getDriver().waitUntilElementIsVisible(this.container, validationErrorMessage);
        return this;
    }

    public WebElement getCreateGroupButton()
    {
        return this.createGroupButton;
    }

    public void createGroup(String groupName)
    {
        setGroupName(groupName);
        // The create group button is enabled if the typed group name passes the validation.
        getDriver().waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                return createGroupButton.isEnabled();
            }
        });
        this.createGroupButton.click();
        waitForNotificationSuccessMessage("Group created");
    }
}
