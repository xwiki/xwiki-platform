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

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.xwiki.test.ui.po.ConfirmationModal;

/**
 * Represents the actions possible on the delete user confirmation modal.
 * 
 * @version $Id$
 * @since 10.9
 */
public class DeleteUserConfirmationModal extends ConfirmationModal
{
    public DeleteUserConfirmationModal()
    {
        super(By.id("deleteUserModal"));

        // Wait for the modal content to be loaded.
        waitUntilReady();
    }

    @Override
    public void clickOk()
    {
        super.clickOk();
        waitForNotificationSuccessMessage("User deleted");
    }

    /**
     * @return the href value of the message displayed when removing a user with script rights
     * @since 17.4.0RC1
     * @since 16.10.9
     */
    public String getScriptRightUserErrorMessageHrefValue()
    {
        return this.container.findElement(By.cssSelector(".errormessage.xform a")).getDomAttribute("href");
    }

    /**
     * @return the text of the warning displayed when the user being deleted has Script or Programming Rights and is
     *         the last author of one or more pages, or an empty string when no such warning is displayed
     * @since 18.7.0RC1
     */
    public String getWarningMessage()
    {
        List<WebElement> warnings =
            getDriver().findElementsWithoutWaiting(this.container, By.cssSelector(".errormessage.xform"));
        return warnings.isEmpty() ? "" : warnings.get(0).getText();
    }

    /**
     * The modal content is loaded asynchronously so we must wait for it.
     * 
     * @return this modal
     */
    private DeleteUserConfirmationModal waitUntilReady()
    {
        getDriver().waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                // The delete user button is enabled as soon as the modal content is loaded.
                return getDriver().findElementWithoutWaiting(DeleteUserConfirmationModal.this.container,
                    By.cssSelector(".modal-footer .btn-danger")).isEnabled();
            }
        });
        return this;
    }
}
