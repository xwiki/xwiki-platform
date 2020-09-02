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
package org.xwiki.test.ui;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.browser.IgnoreBrowser;
import org.xwiki.test.ui.browser.IgnoreBrowsers;
import org.xwiki.test.ui.po.AbstractRegistrationPage;
import org.xwiki.test.ui.po.RegistrationPage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test the user registration feature.
 *
 * @version $Id$
 * @since 12.8RC1
 */
@UITest
public class RegisterTest
{
    protected AbstractRegistrationPage registrationPage;

    @BeforeEach
    void setUp(TestUtils testUtils) throws Exception
    {
        deleteUser("JohnSmith", testUtils);
        testUtils.updateObject("XWiki", "RegistrationConfig", "XWiki.Registration", 0, "liveValidation_enabled",
            useLiveValidation());
        switchUser(testUtils);
        testUtils.recacheSecretToken();
        this.registrationPage = this.getRegistrationPage();
        // The prepareName javascript function is the cause of endless flickering
        // since it trys to suggest a username every time the field is focused.
        testUtils.getDriver().executeJavascript("document.getElementById('xwikiname').onfocus = null;");
        this.registrationPage.fillInJohnSmithValues();
    }

    /** Become the user needed for the test. Guest for RegisterTest. */
    protected void switchUser(TestUtils testUtils)
    {
        // Fast Logout.
        testUtils.forceGuestUser();
    }

    /** To put the registration page someplace else, subclass this class and change this method. */
    protected AbstractRegistrationPage getRegistrationPage()
    {
        return RegistrationPage.gotoPage();
    }

    /** To test without javascript validation, subclass this class and change this method. */
    protected boolean useLiveValidation()
    {
        return true;
    }

    @Test
    @Order(1)
    @IgnoreBrowsers({
        @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason = "See https://jira.xwiki.org/browse/XE-1146"),
        @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See https://jira.xwiki.org/browse/XE-1177")
    })
    void testRegisterJohnSmith(TestUtils testUtils)
    {
        assertTrue(validateAndRegister(testUtils));
        tryToLogin("JohnSmith", "WeakPassword", testUtils);
    }

    @Test
    @Order(2)
    @IgnoreBrowsers({
        @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason = "See https://jira.xwiki.org/browse/XE-1146"),
        @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See https://jira.xwiki.org/browse/XE-1177")
    })
    void testRegisterExistingUser(TestUtils testUtils)
    {
        this.registrationPage.fillRegisterForm(null, null, "Admin", null, null, null);
        // Can't use validateAndRegister here because user existence is not checked by LiveValidation.
        assertFalse(tryToRegister(testUtils));
        assertTrue(this.registrationPage.validationFailureMessagesInclude("User already exists."));
    }

    @Test
    @Order(3)
    @IgnoreBrowsers({
        @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason = "See https://jira.xwiki.org/browse/XE-1146"),
        @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See https://jira.xwiki.org/browse/XE-1177")
    })
    void testRegisterPasswordTooShort(TestUtils testUtils)
    {
        this.registrationPage.fillRegisterForm(null, null, null, "short", "short", null);
        assertFalse(validateAndRegister(testUtils));
        assertTrue(this.registrationPage.validationFailureMessagesInclude(
            "Your new password must be at least 6 characters long."));
    }

    @Test
    @Order(4)
    @IgnoreBrowsers({
        @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason = "See https://jira.xwiki.org/browse/XE-1146"),
        @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See https://jira.xwiki.org/browse/XE-1177")
    })
    void testRegisterDifferentPasswords(TestUtils testUtils)
    {
        this.registrationPage.fillRegisterForm(null, null, null, null, "DifferentPassword", null);
        assertFalse(validateAndRegister(testUtils));
        assertTrue(this.registrationPage.validationFailureMessagesInclude("The two passwords do not match."));
    }

    @Test
    @Order(5)
    @IgnoreBrowsers({
        @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason = "See https://jira.xwiki.org/browse/XE-1146"),
        @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See https://jira.xwiki.org/browse/XE-1177")
    })
    void testRegisterEmptyPassword(TestUtils testUtils)
    {
        this.registrationPage.fillRegisterForm(null, null, null, "", "", null);
        assertFalse(validateAndRegister(testUtils));
        assertTrue(this.registrationPage.validationFailureMessagesInclude("This field is required."));
    }

    @Test
    @Order(6)
    @IgnoreBrowsers({
        @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason = "See https://jira.xwiki.org/browse/XE-1146"),
        @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See https://jira.xwiki.org/browse/XE-1177")
    })
    void testRegisterEmptyUserName(TestUtils testUtils)
    {
        // A piece of javascript fills in the username with the first and last names so we will empty them.
        this.registrationPage.fillRegisterForm("", "", "", null, null, null);
        assertFalse(validateAndRegister(testUtils));
        assertTrue(this.registrationPage.validationFailureMessagesInclude("This field is required."));
    }

    @Test
    @Order(7)
    @IgnoreBrowsers({
        @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason = "See https://jira.xwiki.org/browse/XE-1146"),
        @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See https://jira.xwiki.org/browse/XE-1177")
    })
    void testRegisterInvalidEmail(TestUtils testUtils)
    {
        this.registrationPage.fillRegisterForm(null, null, null, null, null, "not an email address");
        assertFalse(validateAndRegister(testUtils));
        assertTrue(this.registrationPage.validationFailureMessagesInclude("Please enter a valid email address."));
    }

    /**
     * If LiveValidation is enabled then it will check that there are no failures with that. If no failures then hits
     * register button, it then asserts that hitting the register button did not reveal any failures not caught by
     * LiveValidation. If LiveValidation is disabled then just hits the register button.
     */
    protected boolean validateAndRegister(TestUtils testUtils)
    {
        if (useLiveValidation()) {
            this.registrationPage.triggerLiveValidation();
            if (!this.registrationPage.getValidationFailureMessages().isEmpty()) {
                return false;
            }
            boolean result = tryToRegister(testUtils);

            assertTrue(this.registrationPage.getValidationFailureMessages().isEmpty(),
                "LiveValidation did not show a failure message but clicking on the register button did.");

            return result;
        }
        return tryToRegister(testUtils);
    }

    protected boolean tryToRegister(TestUtils testUtils)
    {
        this.registrationPage.clickRegister();

        List<WebElement> infos = testUtils.getDriver().findElements(By.className("infomessage"));
        for (WebElement info : infos) {
            if (info.getText().contains("Registration successful.")) {
                return true;
            }
        }
        return false;
    }

    /** Deletes specified user if it exists, leaves the driver on undefined page. **/
    private void deleteUser(String userName, TestUtils testUtils) throws Exception
    {
        TestUtils.Session s = testUtils.getSession();
        testUtils.forceGuestUser();
        testUtils.getDriver().get(testUtils.getURLToLoginAsAdminAndGotoPage(testUtils.getURLToNonExistentPage()));
        testUtils.recacheSecretToken();
        testUtils.setDefaultCredentials(TestUtils.ADMIN_CREDENTIALS);
        testUtils.rest().deletePage("XWiki", userName);
        testUtils.setSession(s);
    }

    protected void tryToLogin(String username, String password, TestUtils testUtils)
    {
        // Fast logout.
        testUtils.forceGuestUser();
        testUtils.getDriver().get(testUtils.getURLToLoginAs(username, password));
        assertTrue(this.registrationPage.isAuthenticated());
        testUtils.recacheSecretToken();
        testUtils.setDefaultCredentials(username, password);
    }
}
