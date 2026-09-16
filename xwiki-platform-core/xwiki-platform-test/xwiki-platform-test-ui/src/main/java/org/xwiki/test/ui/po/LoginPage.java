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
            // For some reason, the WebDriver is not always waiting for the login form to be submitted (even if there is
            // no JavaScript involved) and the web page to be reloaded (either the same page, in case of a login error,
            // or a different one, where the user is redirected after a successful login).
            getDriver().addPageNotYetReloadedMarker();
            this.submitButton.click();
            getDriver().waitUntilPageIsReloaded();
        }

        synchronizeRESTCredentials(username, password);
    }

    public void loginAs(String username, String password)
    {
        loginAs(username, password, false);
    }

    /**
     * Make the REST client authenticate as the user the browser was just logged in as, so that the two don't drift
     * apart. The browser session and the credentials used for REST calls are independent states, and a test that logs
     * in through this page object would otherwise keep performing its REST calls as whoever was set before, typically
     * the superadmin the {@link TestUtils} constructor defaults to.
     * <p>
     * This method is only ever called with a displayed page, so the login state it reads is the one the server just
     * rendered. A page that shows the requested user as logged in is a definitive answer: only a page the server
     * rendered for that user can show them as logged in. The opposite isn't definitive, since the displayed page may
     * not show any login state at all (the login may have failed, but the redirect may also have landed on a page
     * that isn't skinned), so the credentials are left untouched in that case rather than guessed. Leaving them
     * untouched is also what the tests that log in with wrong credentials on purpose need.
     *
     * @param username the user the browser was asked to log in as
     * @param password the password the browser was asked to log in with
     */
    private void synchronizeRESTCredentials(String username, String password)
    {
        if (username.equals(getUtil().getLoggedInUserName())) {
            getUtil().setDefaultCredentials(username, password);
        }
    }

    public boolean hasInvalidCredentialsErrorMessage()
    {
        return getErrorMessages().contains("Error\nInvalid credentials");
    }

    /**
     * @since 11.6RC1
     */
    public boolean hasCaptchaErrorMessage()
    {
        return getErrorMessages().contains("Error\nPlease fill the captcha form to login.");
    }

    /**
     * @since 11.6RC1
     */
    public String getErrorMessages()
    {
        StringBuilder messages = new StringBuilder();
        for (WebElement element : getDriver().findElements(By.xpath("//div[contains(@class, 'errormessage')]"))) {
            messages.append(element.getText());
        }
        return messages.toString();
    }

    public boolean hasCaptchaChallenge()
    {
        return getDriver().hasElementWithoutWaiting(By.className("captcha-challenge"));
    }
}
