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

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WebDriverException;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the actions possible on the section of a page which allows the user to previw an unsent message or
 * view an old sent message.
 *
 * @version $Id$
 * @since 4.2M1
 */
public class InvitationMessageDisplayElement extends BaseElement
{
    private WebElement displayElement;

    public InvitationMessageDisplayElement(WebElement displayElement)
    {
        super();
        this.displayElement = displayElement;
    }

    public List<WebElement> getAllRecipients()
    {
        return displayElement.findElement(By.id("preview-to-field")).findElements(By.tagName("span"));
    }

    public List<WebElement> getValidRecipients()
    {
        return displayElement.findElement(By.id("preview-to-field")).findElements(By.className("valid-address"));
    }

    public List<WebElement> getInvalidRecipients()
    {
        return displayElement.findElement(By.id("preview-to-field")).findElements(By.className("invalid-address"));
    }

    public boolean isRecipientValid(String email)
    {
        for(WebElement recip : getAllRecipients()) {
            if (recip.getText().contains(email)) {
                return recip.getAttribute("class").contains("valid-address");
            }
        }
        throw new WebDriverException("Recipient name (" + email + ") not found.");
    }

    public String getInvalidAddressMessage()
    {
        return displayElement.findElement(By.id("invalid-address-message")).getText();
    }

    public String getSubjectLine()
    {
        return displayElement.findElement(By.id("preview-subjectline-field")).getText();
    }

    public String getMessageBody()
    {
        return displayElement.findElement(By.id("preview-messagebody-field")).getText();
    }
}
