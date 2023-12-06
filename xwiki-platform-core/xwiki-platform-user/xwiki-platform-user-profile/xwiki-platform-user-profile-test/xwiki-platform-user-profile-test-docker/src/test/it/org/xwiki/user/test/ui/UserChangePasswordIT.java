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
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.EditPage;
import org.xwiki.user.test.po.ChangePasswordPage;
import org.xwiki.user.test.po.PreferencesUserProfilePage;
import org.xwiki.user.test.po.ProfileUserProfilePage;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UI Tests related to password change.
 *
 * @since 11.10
 * @version $Id$
 */
@UITest(properties = {
    // We need the notifications feature because the User Profile UI draws the Notifications Macro used in the user
    // profile for the user's activity stream. As a consequence, when a user is created in the test, the
    // UserAddedEventListener is called and global default user notifications filters are copied for the new user,
    // requiring the notifications HBM mapping file.
    "xwikiDbHbmCommonExtraMappings=notification-filter-preferences.hbm.xml",
    },
    extraJARs = {
        // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus,
        // we need to provide the JAR inside WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-19932
        "org.xwiki.platform:xwiki-platform-notifications-filters-default",
        // The Solr store is not ready yet to be installed as an extension, so we need to add it to WEB-INF/lib
        // manually. See https://jira.xwiki.org/browse/XWIKI-21594
        "org.xwiki.platform:xwiki-platform-eventstream-store-solr"
    }
)
class UserChangePasswordIT
{
    private static final String DEFAULT_PASSWORD = "testtest";

    private static final String NEW_PASSWORD = "newPassword";

    private static final String PASSWORD_1 = "password1";

    private static final String PASSWORD_2 = "password2";

    private String userName;

    @BeforeEach
    public void setUp(TestUtils setup, TestReference testReference)
        throws Exception
    {
        this.userName = testReference.getName() + testReference.getLastSpaceReference().getName();
        setup.loginAsSuperAdmin();
        setup.rest().deletePage("XWiki", this.userName);
        setup.createUserAndLogin(this.userName, DEFAULT_PASSWORD);

        // At first edition the Dashboard is saving the doc to insert a new object, so we need to be sure
        // this has been done before performing our other test, to avoid getting stale element references.
        setup.gotoPage("XWiki", this.userName, "edit");
        // Ensure JS has been loaded
        new EditPage();
    }

    /** Functionality check: changing the password. */
    @Test
    @Order(1)
    void changePassword(TestUtils setup, TestReference testReference)
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
        changePasswordPage = changePasswordPage.submit();
        changePasswordPage.assertSuccessMessage("Your password has been successfully changed.");
    }

    @Test
    @Order(2)
    void changePasswordWithTwoDifferentPasswords()
    {
        ProfileUserProfilePage userProfilePage = ProfileUserProfilePage.gotoPage(this.userName);
        PreferencesUserProfilePage preferencesPage = userProfilePage.switchToPreferences();
        ChangePasswordPage changePasswordPage = preferencesPage.changePassword();
        changePasswordPage.changePassword(DEFAULT_PASSWORD, PASSWORD_1, PASSWORD_2);
        changePasswordPage = changePasswordPage.submit();
        changePasswordPage.assertValidationErrorMessage("The two passwords do not match.");
    }

    @Test
    @Order(3)
    void changePasswordWithoutEnteringPasswords()
    {
        ChangePasswordPage changePasswordPage = ProfileUserProfilePage.gotoPage(this.userName)
            .switchToPreferences().changePassword();
        changePasswordPage = changePasswordPage.submit();
        changePasswordPage.assertValidationErrorMessage("This field is required.");
    }

    @Test
    @Order(4)
    void changePasswordOfAnotherUserWithTwoDifferentPasswords(TestUtils setup)
    {
        // Login as superadmin (to have Admin rights) and change the password of another user.
        setup.loginAsSuperAdmin();
        ProfileUserProfilePage userProfilePage = ProfileUserProfilePage.gotoPage(this.userName);

        PreferencesUserProfilePage preferencesPage = userProfilePage.switchToPreferences();
        ChangePasswordPage changePasswordPage = preferencesPage.changePassword();
        changePasswordPage.changePasswordAsAdmin(PASSWORD_1, PASSWORD_2);
        changePasswordPage = changePasswordPage.submit();
        changePasswordPage.assertValidationErrorMessage("The two passwords do not match.");
    }

    @Test
    @Order(5)
    void changePasswordWithWrongOriginalPassword()
    {
        ProfileUserProfilePage userProfilePage = ProfileUserProfilePage.gotoPage(this.userName);
        PreferencesUserProfilePage preferencesPage = userProfilePage.switchToPreferences();
        ChangePasswordPage changePasswordPage = preferencesPage.changePassword();
        changePasswordPage.changePassword("badPassword", PASSWORD_1, PASSWORD_1);
        changePasswordPage = changePasswordPage.submit();
        changePasswordPage.assertErrorMessage("Current password is invalid.");
    }

    @Test
    @Order(6)
    void changePasswordWhenPolicyIsLength8AndNumberMandatory(TestUtils setup)
    {
        // Update password policy to enforce password with 8 characters and a mandatory number in it
        setup.updateObject("XWiki", "RegistrationConfig", "XWiki.Registration", 0,
            "passwordLength", "8",
            "passwordRuleOneNumberEnabled", "1");

        setup.loginAsSuperAdmin();
        ProfileUserProfilePage userProfilePage = ProfileUserProfilePage.gotoPage(this.userName);
        PreferencesUserProfilePage preferencesPage = userProfilePage.switchToPreferences();
        ChangePasswordPage changePasswordPage = preferencesPage.changePassword();
        changePasswordPage.changePasswordAsAdmin("foo", "foo");
        changePasswordPage = changePasswordPage.submit();
        changePasswordPage.assertValidationErrorMessage("Your new password must be at least 8 characters long.");
        changePasswordPage.changePasswordAsAdmin("foofoofoo", "foofoofoo");
        changePasswordPage = changePasswordPage.submit();
        changePasswordPage.assertValidationErrorMessage("The password must contain at least one number.");
        changePasswordPage.changePasswordAsAdmin("foofoofoo42", "foofoofoo42");
        changePasswordPage = changePasswordPage.submit();
        changePasswordPage.assertSuccessMessage("Your password has been successfully changed.");
    }
}
