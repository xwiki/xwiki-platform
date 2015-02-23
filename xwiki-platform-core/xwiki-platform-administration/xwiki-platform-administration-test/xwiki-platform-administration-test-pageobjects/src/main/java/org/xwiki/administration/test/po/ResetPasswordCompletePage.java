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
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.LoginPage;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents the actions possible on the XWiki.ResetPasswordComplete page, where a user lands by using a reset password
 * link provided by mail by the XWiki.ResetPassword page.
 * 
 * @version $Id$
 */
public class ResetPasswordCompletePage extends ViewPage
{
    @FindBy(css = ".xcontent .box")
    private WebElement messageBox;

    @FindBy(id = "p")
    private WebElement newPasswordField;

    @FindBy(id = "p2")
    private WebElement newPasswordConfirmationField;

    @FindBy(css = ".xcontent form input[type='submit']")
    private WebElement saveButton;

    @FindBy(xpath = "//.[@class='xcontent']//a[contains(@href, 'login')]")
    private WebElement loginButton;

    /**
     * To be called the first time the page is opened, using a password reset link.
     * 
     * @return true if the reset link is valid and the form to change the password is being displayed, false otherwise.
     */
    public boolean isResetLinkValid()
    {
        // If we see the form, the the link is valid.
        return getDriver().hasElementWithoutWaiting(By.cssSelector(".xcontent form"));
    }

    public String getPassowrd()
    {
        return this.newPasswordField.getAttribute("value");
    }

    public void setPassword(String newPassword)
    {
        this.newPasswordField.sendKeys(newPassword);
    }

    public String getPasswordConfirmation()
    {
        return this.newPasswordConfirmationField.getAttribute("value");
    }

    public void setPasswordConfirmation(String newPasswordConfirmation)
    {
        this.newPasswordConfirmationField.sendKeys(newPasswordConfirmation);
    }

    public ResetPasswordCompletePage clickSave()
    {
        saveButton.click();
        return new ResetPasswordCompletePage();
    }

    public boolean isPasswordSuccessfullyReset()
    {
        // success = no form and a message that is not error or warning.
        return !getDriver().hasElementWithoutWaiting(By.cssSelector(".xcontent form"))
            && messageBox.getAttribute("class").contains("infomessage");
    }

    public String getMessage()
    {
        return messageBox.getText();
    }

    public LoginPage clickLogin()
    {
        loginButton.click();
        return new LoginPage();
    }
}
