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

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Represents the common actions possible after trying to copy a page over an existing page.
 *
 * @version $Id$
 * @since 5.3M1
 */
public class CopyOverwritePromptPage extends ViewPage
{
    /**
     * The warning message.
     */
    @FindBy(className = "warningmessage")
    private WebElement warningMessage;

    /**
     * The copy button.
     */
    @FindBy(xpath = "//input[@class = 'button' and @value = 'Copy']")
    private WebElement copyButton;

    /**
     * The change target button.
     */
    @FindBy(xpath = "//a[@class = 'secondary button' and normalize-space(.) = 'Change the target page']")
    private WebElement changeButton;

    /**
     * The cancel button.
     */
    @FindBy(xpath = "//a[@class = 'secondary button' and text()='Cancel']")
    private WebElement cancelButton;

    /**
     * Submit the copy overwrite prompt page form.
     * @return the copy confirmation page.
     */
    public CopyOrRenameOrDeleteStatusPage clickCopyButton()
    {
        this.copyButton.submit();
        return new CopyOrRenameOrDeleteStatusPage();
    }

    /**
     * Click the change target link.
     * @return the copy page.
     */
    public CopyPage clickChangeTargetButton()
    {
        this.changeButton.click();
        return new CopyPage();
    }

    /**
     * Click the cancel link.
     * @return the view page of the originaly copied document.
     */
    public ViewPage clickCancelButton()
    {
        this.cancelButton.click();
        return new CopyPage();
    }

    /**
     * @return the overwrite warning message.
     */
    public String getWarningMessage()
    {
        return this.warningMessage.getText();
    }
}
