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
package org.xwiki.invitation.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the actions possible on the confirmation dialog for the invitation appliaction.
 *
 * @version $Id$
 * @since 4.2M1
 */
public class InvitationActionConfirmationElement extends BaseElement
{
    @FindBy(className = "action-confirm")
    private WebElement actionConfirm;

    public void setMemo(String memo)
    {
        actionConfirm.findElements(By.name("memo")).get(0).sendKeys(memo);
    }

    public String getLabel()
    {
        return actionConfirm.findElements(By.tagName("label")).get(0).getText();
    }

    /** Returns the message given after clicking confirm. */
    public String confirm()
    {
        actionConfirm.findElements(By.className("button")).get(0).click();
        return getDriver().findElement(By.id("invitation-action-message")).getText();
    }
}
