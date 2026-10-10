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

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.BasePage;

/**
 * Represents the page displayed to a user whose account is not active yet because the email address has not been
 * validated. It holds a form to enter the validation key received by email.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
public class InactiveAccountPage extends BasePage
{
    @FindBy(css = "#mainContentArea p.xwikimessage")
    private WebElement message;

    @FindBy(id = "validKey")
    private WebElement validationKeyInput;

    @FindBy(css = "#mainContentArea form.xformInline input[type='submit']")
    private WebElement confirmAccountButton;

    /**
     * @return the message explaining why the account cannot be used
     */
    public String getMessage()
    {
        return this.message.getText();
    }

    /**
     * Submit the validation key received by email.
     *
     * @param validationKey the validation key
     * @return the page displaying the result of the account validation
     */
    public AccountValidationPage confirmAccount(String validationKey)
    {
        this.validationKeyInput.clear();
        this.validationKeyInput.sendKeys(validationKey);
        getDriver().addPageNotYetReloadedMarker();
        this.confirmAccountButton.click();
        getDriver().waitUntilPageIsReloaded();
        return new AccountValidationPage();
    }
}
