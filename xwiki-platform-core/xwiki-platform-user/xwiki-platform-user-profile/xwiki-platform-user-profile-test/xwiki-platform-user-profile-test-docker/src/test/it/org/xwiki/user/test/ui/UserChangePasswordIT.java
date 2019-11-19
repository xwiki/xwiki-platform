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
package org.xwiki.user.test.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.integration.junit.LogCaptureConfiguration;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.user.test.po.ChangePasswordPage;
import org.xwiki.user.test.po.PreferencesUserProfilePage;
import org.xwiki.user.test.po.ProfileUserProfilePage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UI Tests related to password change.
 *
 * @since 11.10
 * @version $Id$
 */
@UITest
public class UserChangePasswordIT
{
    private static final String DEFAULT_PASSWORD = "testtest";

    private static final String NEW_PASSWORD = "newPassword";

    private static final String PASSWORD_1 = "password1";

    private static final String PASSWORD_2 = "password2";

    private String userName;

    @BeforeEach
    public void setUp(TestUtils setup, TestReference testReference, LogCaptureConfiguration logCaptureConfiguration)
        throws Exception
    {
        this.userName = testReference.getName() + testReference.getLastSpaceReference().getName();
        setup.loginAsSuperAdmin();
        setup.rest().deletePage("XWiki", this.userName);
        setup.createUserAndLogin(this.userName, DEFAULT_PASSWORD);

        // The following error happened from time-to-time:
        // JavaScript error: http://localhost:8080/xwiki/resources/js/xwiki/meta.js?cache-version=1571475464000,
        // line 1: TypeError: f is undefined
        //
        // It looks like it's not a problem for the tests, nevertheless it should be fixed in the future.
        // Best would be to migrate those as docker tests.
        logCaptureConfiguration.registerExcludes("TypeError: f is undefined");
    }

    /** Functionality check: changing the password. */
    @Test
    @Order(1)
    public void changePassword(TestUtils setup, TestReference testReference)
    {
        ProfileUserProfilePage userProfilePage = ProfileUserProfilePage.gotoPage(this.userName);
        // Change the password
        PreferencesUserProfilePage preferencesPage = userProfilePage.switchToPreferences();
        ChangePasswordPage changePasswordPage = preferencesPage.changePassword();
        changePasswordPage.changePassword(DEFAULT_PASSWORD, NEW_PASSWORD, NEW_PASSWORD);
        changePasswordPage.submit();

        // Logout
        setup.forceGuestUser();

        // Login with the new password and navigate to a page to verify that the user is logged in
        setup.loginAndGotoPage(this.userName, NEW_PASSWORD, setup.getURL(testReference, "view", null));
        ViewPage vp = new ViewPage();
        assertTrue(vp.isAuthenticated());
        setup.recacheSecretToken();

        //Reset the password
        setup.loginAsSuperAdmin();

        userProfilePage = ProfileUserProfilePage.gotoPage(this.userName);
        preferencesPage = userProfilePage.switchToPreferences();
        changePasswordPage = preferencesPage.changePassword();
        changePasswordPage.changePasswordAsAdmin(DEFAULT_PASSWORD, DEFAULT_PASSWORD);
        changePasswordPage.submit();
        assertEquals("Your password has been successfully changed.", changePasswordPage.getSuccessMessage());
    }

    @Test
    @Order(2)
    public void changePasswordWithTwoDifferentPasswords()
    {
        ProfileUserProfilePage userProfilePage = ProfileUserProfilePage.gotoPage(this.userName);
        PreferencesUserProfilePage preferencesPage = userProfilePage.switchToPreferences();
        ChangePasswordPage changePasswordPage = preferencesPage.changePassword();
        changePasswordPage.changePassword(DEFAULT_PASSWORD, PASSWORD_1, PASSWORD_2);
        changePasswordPage.submit();
        assertEquals("The two passwords do not match.", changePasswordPage.getValidationErrorMessage());
    }

    @Test
    @Order(3)
    public void changePasswordWithoutEnteringPasswords()
    {
        ChangePasswordPage changePasswordPage = ProfileUserProfilePage.gotoPage(this.userName)
            .switchToPreferences().changePassword();
        changePasswordPage.submit();
        assertEquals("This field is required.", changePasswordPage.getValidationErrorMessage());
    }

    @Test
    @Order(4)
    public void changePasswordOfAnotherUserWithTwoDifferentPasswords(TestUtils setup)
    {
        // Login as superadmin (to have Admin rights) and change the password of another user.
        setup.loginAsSuperAdmin();
        ProfileUserProfilePage userProfilePage = ProfileUserProfilePage.gotoPage(this.userName);

        PreferencesUserProfilePage preferencesPage = userProfilePage.switchToPreferences();
        ChangePasswordPage changePasswordPage = preferencesPage.changePassword();
        changePasswordPage.changePasswordAsAdmin(PASSWORD_1, PASSWORD_2);
        changePasswordPage.submit();
        assertEquals("The two passwords do not match.", changePasswordPage.getValidationErrorMessage());
    }

    @Test
    @Order(5)
    public void changePasswordWithWrongOriginalPassword()
    {
        ProfileUserProfilePage userProfilePage = ProfileUserProfilePage.gotoPage(this.userName);
        PreferencesUserProfilePage preferencesPage = userProfilePage.switchToPreferences();
        ChangePasswordPage changePasswordPage = preferencesPage.changePassword();
        changePasswordPage.changePassword("badPassword", PASSWORD_1, PASSWORD_1);
        changePasswordPage.submit();
        assertEquals("Current password is invalid.", changePasswordPage.getErrorMessage());
    }

    @Test
    @Order(6)
    public void changePasswordWhenPolicyIsLength8AndNumberMandatory(TestUtils setup)
    {
        // Update password policy to enforce password with 8 characters and a mandatory number in it
        setup.updateObject("XWiki", "RegistrationConfig", "XWiki.Registration", 0,
            "passwordLength", "8",
            "passwordRuleOneNumberEnabled", "1");
        try {
            setup.loginAsSuperAdmin();
            ProfileUserProfilePage userProfilePage = ProfileUserProfilePage.gotoPage(this.userName);
            PreferencesUserProfilePage preferencesPage = userProfilePage.switchToPreferences();
            ChangePasswordPage changePasswordPage = preferencesPage.changePassword();
            changePasswordPage.changePasswordAsAdmin("foo", "foo");
            changePasswordPage = changePasswordPage.submit();
            assertEquals("Your new password must be at least 8 characters long.",
                changePasswordPage.getValidationErrorMessage());
            changePasswordPage.changePasswordAsAdmin("foofoofoo", "foofoofoo");
            changePasswordPage = changePasswordPage.submit();
            assertEquals("The password must contain at least one number.",
                changePasswordPage.getValidationErrorMessage());
            changePasswordPage.changePasswordAsAdmin("foofoofoo42", "foofoofoo42");
            changePasswordPage = changePasswordPage.submit();
            assertEquals("Your password has been successfully changed.", changePasswordPage.getSuccessMessage());
        } finally {
            // put back standard config
            setup.updateObject("XWiki", "RegistrationConfig", "XWiki.Registration", 0,
                "passwordLength", "6",
                "passwordRuleOneNumberEnabled", "0");
        }
    }
}
