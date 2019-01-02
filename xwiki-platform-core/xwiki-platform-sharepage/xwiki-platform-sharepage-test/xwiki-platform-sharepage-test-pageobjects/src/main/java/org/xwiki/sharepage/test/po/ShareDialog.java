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
package org.xwiki.sharepage.test.po;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.BaseElement;
import org.xwiki.test.ui.po.SuggestInputElement;

/**
 * Represents actions that can be done on the Share Page by Email dialog box.
 *
 * @version $Id$
 * @since 7.0RC1
 */
public class ShareDialog extends BaseElement
{
    @FindBy(id = "shareTarget")
    private WebElement emailField;

    @FindBy(xpath = "//textarea[@name = 'message']")
    private WebElement emailMessage;

    @FindBy(xpath = "//input[@type = 'submit' and @class = 'button']")
    private WebElement sendButton;

    public void setEmailField(String email)
    {
        new SuggestInputElement(this.emailField).sendKeys(email).waitForSuggestions().selectTypedText();
    }

    public void setMessage(String message)
    {
        this.emailMessage.clear();
        this.emailMessage.sendKeys(message);
    }

    public ShareResultDialog sendMail()
    {
        this.sendButton.click();
        return new ShareResultDialog();
    }
}
