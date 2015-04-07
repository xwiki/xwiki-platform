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

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.browser.IgnoreBrowser;
import org.xwiki.test.ui.browser.IgnoreBrowsers;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.user.test.po.ChangeAvatarPage;
import org.xwiki.user.test.po.ChangePasswordPage;
import org.xwiki.user.test.po.PreferencesEditPage;
import org.xwiki.user.test.po.PreferencesUserProfilePage;
import org.xwiki.user.test.po.ProfileEditPage;
import org.xwiki.user.test.po.ProfileUserProfilePage;

/**
 * Test the User Profile.
 * 
 * @version $Id$
 * @since 2.4
 */
public class UserProfileTest extends AbstractTest
{
    private static final String IMAGE_NAME = "avatar.png";

    private static final String USER_FIRST_NAME = "User";

    private static final String USER_LAST_NAME = "of this Wiki";

    private static final String USER_COMPANY = "XWiki.org";

    private static final String USER_ABOUT = "This is some example text to type into the text area";

    private static final String USER_EMAIL = "webmaster@xwiki.org";

    private static final String USER_PHONE = "0000-000-000";

    private static final String USER_ADDRESS = "1600 No Street";

    private static final String USER_BLOG = "http://xwiki.org/";

    private static final String USER_BLOGFEED = "http://xwiki.org/feed";

    private static final String WYSIWYG_EDITOR = "Wysiwyg";

    private static final String TEXT_EDITOR = "Text";

    private static final String DEFAULT_EDITOR = "-";

    private static final String SIMPLE_USER = "Simple";

    private static final String ADVANCED_USER = "Advanced";

    private static final String PASSWORD_1 = "password1";

    private static final String PASSWORD_2 = "password2";

    private static final String DEFAULT_PASSWORD = "testtest";

    private ProfileUserProfilePage customProfilePage;

    private String userName;

    @BeforeClass
    public static void globalSetUp()
    {
        getUtil().loginAsSuperAdminAndGotoPage(getUtil().getURL("XWiki", "ChangePassword"));
        ChangePasswordPage changePasswordPage = new ChangePasswordPage();
        changePasswordPage.edit();
        changePasswordPage.clickSaveAndView();
        getDriver().get(getUtil().getURLToLogout());
    }

    @Before
    public void setUp()
    {
        getUtil().recacheSecretToken();
        this.userName = getTestClassName() + getTestMethodName();
        getUtil().createUserAndLogin(this.userName, DEFAULT_PASSWORD);

        this.customProfilePage = ProfileUserProfilePage.gotoPage(this.userName);
    }

    /** Functionality check: changing profile information. */
    @Test
    @IgnoreBrowsers({
        @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See http://jira.xwiki.org/browse/XE-1146"),
        @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See http://jira.xwiki.org/browse/XE-1177")
    })
    public void testEditProfile()
    {
        ProfileEditPage profileEditPage = this.customProfilePage.editProfile();
        profileEditPage.setUserFirstName(USER_FIRST_NAME);
        profileEditPage.setUserLastName(USER_LAST_NAME);
        profileEditPage.setUserCompany(USER_COMPANY);
        profileEditPage.setUserAbout(USER_ABOUT);
        profileEditPage.setUserEmail(USER_EMAIL);
        profileEditPage.setUserPhone(USER_PHONE);
        profileEditPage.setUserAddress(USER_ADDRESS);
        profileEditPage.setUserBlog(USER_BLOG);
        profileEditPage.setUserBlogFeed(USER_BLOGFEED);
        profileEditPage.clickSaveAndView();

        // Check that the information was updated
        Assert.assertEquals(USER_FIRST_NAME, this.customProfilePage.getUserFirstName());
        Assert.assertEquals(USER_LAST_NAME, this.customProfilePage.getUserLastName());
        Assert.assertEquals(USER_COMPANY, this.customProfilePage.getUserCompany());
        Assert.assertEquals(USER_ABOUT, this.customProfilePage.getUserAbout());
        Assert.assertEquals(USER_EMAIL, this.customProfilePage.getUserEmail());
        Assert.assertEquals(USER_PHONE, this.customProfilePage.getUserPhone());
        Assert.assertEquals(USER_ADDRESS, this.customProfilePage.getUserAddress());
        Assert.assertEquals(USER_BLOG, this.customProfilePage.getUserBlog());
        Assert.assertEquals(USER_BLOGFEED, this.customProfilePage.getUserBlogFeed());
    }

    /** Functionality check: changing the profile picture. */
    @Test
    @IgnoreBrowsers({
        @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See http://jira.xwiki.org/browse/XE-1146"),
        @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See http://jira.xwiki.org/browse/XE-1177")
    })
    public void testChangeAvatarImage()
    {
        ChangeAvatarPage changeAvatarImage = this.customProfilePage.changeAvatarImage();
        changeAvatarImage.setAvatarImage(IMAGE_NAME);
        changeAvatarImage.submit();
        Assert.assertEquals(IMAGE_NAME, this.customProfilePage.getAvatarImageName());
    }

    /** Functionality check: changing the password. */
    @Test
    @IgnoreBrowsers({
        @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See http://jira.xwiki.org/browse/XE-1146"),
        @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See http://jira.xwiki.org/browse/XE-1177")
    })
    public void testChangePassword()
    {
        // Change the password
        PreferencesUserProfilePage preferencesPage = this.customProfilePage.switchToPreferences();
        ChangePasswordPage changePasswordPage = preferencesPage.changePassword();
        String newPassword = RandomStringUtils.randomAlphanumeric(6);
        changePasswordPage.changePassword(DEFAULT_PASSWORD, newPassword, newPassword);
        changePasswordPage.submit();

        // Logout
        getUtil().forceGuestUser();

        // Login with the new password and navigate to a page to verify that the user is logged in
        getUtil().loginAndGotoPage(this.userName, newPassword,
            getUtil().getURL(getTestClassName(), getTestMethodName()));
        ViewPage vp = new ViewPage();
        Assert.assertTrue(vp.isAuthenticated());
        getUtil().recacheSecretToken();
        
        //Reset the password
        getUtil().loginAsSuperAdminAndGotoPage(this.customProfilePage.getURL());

        preferencesPage = this.customProfilePage.switchToPreferences();
        changePasswordPage = preferencesPage.changePassword();
        changePasswordPage.changePasswordAsAdmin(DEFAULT_PASSWORD, DEFAULT_PASSWORD);
        changePasswordPage.submit();
        Assert.assertEquals("Your password has been successfully changed.", changePasswordPage.getSuccessMessage());
    }

    /** Functionality check: changing the user type. */
    @Test
    @IgnoreBrowsers({
        @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See http://jira.xwiki.org/browse/XE-1146"),
        @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See http://jira.xwiki.org/browse/XE-1177")
    })
    public void testChangeUserProfile()
    {
        String timezone = "Europe/Paris";
        PreferencesUserProfilePage preferencesPage = this.customProfilePage.switchToPreferences();
        Assert.assertEquals("", preferencesPage.getTimezone());

        // Setting to Simple user and setting the timezone to Europe/Paris
        PreferencesEditPage preferencesEditPage = preferencesPage.editPreferences();
        preferencesEditPage.setSimpleUserType();
        preferencesEditPage.setTimezone(timezone);
        preferencesEditPage.clickSaveAndView();
        preferencesPage = this.customProfilePage.switchToPreferences();
        Assert.assertEquals(SIMPLE_USER, preferencesPage.getUserType());
        Assert.assertEquals(timezone, preferencesPage.getTimezone());

        // Setting to Advanced user
        preferencesEditPage = preferencesPage.editPreferences();
        preferencesEditPage.setAdvancedUserType();
        preferencesEditPage.clickSaveAndView();
        this.customProfilePage.switchToPreferences();
        Assert.assertEquals(ADVANCED_USER, preferencesPage.getUserType());
    }

    /** Functionality check: changing the default editor. */
    @Test
    @IgnoreBrowsers({
        @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See http://jira.xwiki.org/browse/XE-1146"),
        @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See http://jira.xwiki.org/browse/XE-1177")
    })
    public void testChangeDefaultEditor()
    {
        PreferencesUserProfilePage preferencesPage = this.customProfilePage.switchToPreferences();

        // Setting to Text Editor
        PreferencesEditPage preferencesEditPage = preferencesPage.editPreferences();
        preferencesEditPage.setDefaultEditorText();
        preferencesEditPage.clickSaveAndView();
        preferencesPage = this.customProfilePage.switchToPreferences();
        Assert.assertEquals(TEXT_EDITOR, preferencesPage.getDefaultEditor());

        // Setting to WYSIWYG Editor
        this.customProfilePage = ProfileUserProfilePage.gotoPage(this.userName);
        preferencesPage = this.customProfilePage.switchToPreferences();
        preferencesEditPage = preferencesPage.editPreferences();
        preferencesEditPage.setDefaultEditorWysiwyg();
        preferencesEditPage.clickSaveAndView();
        preferencesPage = this.customProfilePage.switchToPreferences();
        Assert.assertEquals(WYSIWYG_EDITOR, preferencesPage.getDefaultEditor());

        // Setting to Default Editor
        this.customProfilePage = ProfileUserProfilePage.gotoPage(this.userName);
        preferencesPage = this.customProfilePage.switchToPreferences();
        preferencesEditPage = preferencesPage.editPreferences();
        preferencesEditPage.setDefaultEditorDefault();
        preferencesEditPage.clickSaveAndView();
        preferencesPage = this.customProfilePage.switchToPreferences();
        Assert.assertEquals(DEFAULT_EDITOR, preferencesPage.getDefaultEditor());
    }

    /**
     * Check that the content of the first comment isn't used as the "About" information in the user profile. See
     * XAADMINISTRATION-157.
     */
    @Test
    public void testCommentDoesntOverrideAboutInformation()
    {
        String commentContent = "this is from a comment";

        int commentId = this.customProfilePage.openCommentsDocExtraPane().postComment(commentContent, true);
        getDriver().navigate().refresh();
        Assert.assertFalse("Comment content was used as profile information", this.customProfilePage.getContent()
            .contains(commentContent));

        if (commentId != -1) {
            this.customProfilePage.openCommentsDocExtraPane().deleteCommentByID(commentId);
        }
    }

    @Test
    @IgnoreBrowsers({
        @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See http://jira.xwiki.org/browse/XE-1146"),
        @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See http://jira.xwiki.org/browse/XE-1177")
    })
    public void testChangePasswordWithTwoDifferentPasswords()
    {
        PreferencesUserProfilePage preferencesPage = this.customProfilePage.switchToPreferences();
        ChangePasswordPage changePasswordPage = preferencesPage.changePassword();
        changePasswordPage.changePassword(DEFAULT_PASSWORD, PASSWORD_1, PASSWORD_2);
        changePasswordPage.submit();
        Assert.assertEquals("The two passwords do not match.", changePasswordPage.getValidationErrorMessage());
    }

    @Test
    @IgnoreBrowsers({
        @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See http://jira.xwiki.org/browse/XE-1146"),
        @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See http://jira.xwiki.org/browse/XE-1177")
    })
    public void testChangePasswordWithoutEnteringPasswords()
    {
        ChangePasswordPage changePasswordPage = this.customProfilePage.switchToPreferences().changePassword();
        changePasswordPage.submit();
        Assert.assertEquals("Your new password should be at least 6 characters long.", changePasswordPage.getValidationErrorMessage());
    }

    @Test
    @IgnoreBrowsers({
        @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See http://jira.xwiki.org/browse/XE-1146"),
        @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See http://jira.xwiki.org/browse/XE-1177")
    })
    public void testChangePasswordOfAnotherUserWithTwoDifferentPasswords()
    {
        // Login as superadmin (to have Admin rights) and change the password of another user.
        getUtil().loginAsSuperAdminAndGotoPage(this.customProfilePage.getURL());

        PreferencesUserProfilePage preferencesPage = this.customProfilePage.switchToPreferences();
        ChangePasswordPage changePasswordPage = preferencesPage.changePassword();
        changePasswordPage.changePasswordAsAdmin(PASSWORD_1, PASSWORD_2);
        changePasswordPage.submit();
        Assert.assertEquals("The two passwords do not match.", changePasswordPage.getValidationErrorMessage());
    }
    
    @Test
    @IgnoreBrowsers({
        @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See http://jira.xwiki.org/browse/XE-1146"),
        @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See http://jira.xwiki.org/browse/XE-1177")
    })
    public void testChangePasswordWithWrongOriginalPassword()
    {
        PreferencesUserProfilePage preferencesPage = this.customProfilePage.switchToPreferences();
        ChangePasswordPage changePasswordPage = preferencesPage.changePassword();
        changePasswordPage.changePassword("badPassword", PASSWORD_1, PASSWORD_1);
        changePasswordPage.submit();
        Assert.assertEquals("Current password is invalid.", changePasswordPage.getErrorMessage());
    }
}
