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
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents the actions possible on the XWiki.ResetPassword page.
 *
 * @version $Id$
 * @since 7.0M2
 */
public class ResetPasswordPage extends ViewPage
{
    /**
     * Resource action used for the reset password handling.
     */
    public static final String RESET_PASSWORD_URL_RESOURCE = "authenticate/wiki/%s/resetpassword";

    @FindBy(id = "u")
    private WebElement userNameInput;

    @FindBy(css = "#resetPasswordForm input[type='submit']")
    private WebElement resetPasswordButton;

    @FindBy(css = ".xwikimessage")
    private WebElement messageBox;

    @FindBy(xpath = "//*[@class='panel-body']//a[contains(text(), 'Retry')]")
    private WebElement retryUserNameButton;

    public static String getResetPasswordURL()
    {
        return getUtil().getBaseURL() + String.format(RESET_PASSWORD_URL_RESOURCE, getUtil().getCurrentWiki());
    }

    public static ResetPasswordPage gotoPage()
    {
        getUtil().gotoPage(getResetPasswordURL());
        return new ResetPasswordPage();
    }

    public String getUserName()
    {
        return userNameInput.getAttribute("value");
    }

    public void setUserName(String userName)
    {
        this.userNameInput.sendKeys(userName);
    }

    public ResetPasswordPage clickResetPassword()
    {
        resetPasswordButton.click();
        return new ResetPasswordPage();
    }

    /**
     * This method only checks if the form was properly submitted and didn't return an error.
     * It does not mean that an email was necessarily sent.
     *
     * @return {@code true} if the form is properly submitted.
     */
    public boolean isFormSubmitted()
    {
        // If there is no form and we see an info box, then the request was sent.
        return !getDriver().hasElementWithoutWaiting(By.cssSelector("#resetPasswordForm"))
            && messageBox.getText().contains("An e-mail was sent to");
    }

    public String getMessage()
    {
        return messageBox.getText();
    }

    public ResetPasswordPage clickRetry()
    {
        retryUserNameButton.click();
        return new ResetPasswordPage();
    }
}
