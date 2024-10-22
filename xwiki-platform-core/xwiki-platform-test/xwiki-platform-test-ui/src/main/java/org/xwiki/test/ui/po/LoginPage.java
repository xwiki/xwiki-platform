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

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.ui.TestUtils;

/**
 * Represents the actions possible on the Login page.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class LoginPage extends ViewPage
{
    @FindBy(id = "j_username")
    private WebElement usernameText;

    @FindBy(id = "j_password")
    private WebElement passwordText;

    @FindBy(id = "rememberme")
    private WebElement rememberMeCheckbox;

    @FindBy(xpath = "//input[@type='submit' and @value='Log-in']")
    private WebElement submitButton;

    private static final LocalDocumentReference LOCAL_DOCUMENT_REFERENCE =
        new LocalDocumentReference("XWiki", "XWikiLogin");

    public static LoginPage gotoPage()
    {
        getUtil().gotoPage(LOCAL_DOCUMENT_REFERENCE, "login");
        return new LoginPage();
    }

    public void assertOnPage()
    {
        getUtil().assertOnPage(LOCAL_DOCUMENT_REFERENCE);
    }

    public void loginAsAdmin()
    {
        loginAs(TestUtils.ADMIN_CREDENTIALS.getUserName(), TestUtils.ADMIN_CREDENTIALS.getPassword(), true);
    }

    public void loginAs(String username, String password, boolean rememberMe)
    {
        // In order to have good performance, don't log in again if the user is already logged-in.
        if (!isAuthenticated() || !getCurrentUser().equals(username)) {
            this.usernameText.sendKeys(username);
            this.passwordText.sendKeys(password);
            if (rememberMe) {
                this.rememberMeCheckbox.click();
            }
            this.submitButton.click();
        }
    }

    public void loginAs(String username, String password)
    {
        loginAs(username, password, false);
    }

    public boolean hasInvalidCredentialsErrorMessage()
    {
        return getErrorMessages().contains("Error: Invalid credentials");
    }

    /**
     * @since 11.6RC1
     */
    public boolean hasCaptchaErrorMessage()
    {
        return getErrorMessages().contains("Error: Please fill the captcha form to login.");
    }

    /**
     * @since 11.6RC1
     */
    public String getErrorMessages()
    {
        StringBuilder messages = new StringBuilder();
        for (WebElement element : getDriver().findElements(By.xpath("//div[@class='box errormessage']"))) {
            messages.append(element.getText());
        }
        return messages.toString();
    }

    public boolean hasCaptchaChallenge()
    {
        return getDriver().hasElementWithoutWaiting(By.className("captcha-challenge"));
    }
}
