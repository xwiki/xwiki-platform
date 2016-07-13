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
    @FindBy(id = "u")
    private WebElement userNameInput;

    @FindBy(css = ".xcontent form input[type='submit']")
    private WebElement resetPasswordButton;

    @FindBy(css = ".xcontent .box")
    private WebElement messageBox;

    @FindBy(xpath = "//.[@class='xcontent']//a[contains(text(), 'Retry')]")
    private WebElement retryUserNameButton;

    public static ResetPasswordPage gotoPage()
    {
        getUtil().gotoPage("XWiki", "ResetPassword");
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

    public boolean isResetPasswordSent()
    {
        // If there is no form and we see an info box, then the request was sent.
        return !getDriver().hasElementWithoutWaiting(By.cssSelector(".xcontent form"))
            && messageBox.getAttribute("class").contains("infomessage");
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
