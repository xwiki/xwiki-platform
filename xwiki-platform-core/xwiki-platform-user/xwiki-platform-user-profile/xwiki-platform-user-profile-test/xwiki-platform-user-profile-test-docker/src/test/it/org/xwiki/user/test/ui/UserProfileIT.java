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

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.HistoryPane;
import org.xwiki.test.ui.po.LiveTableElement;
import org.xwiki.test.ui.po.editor.EditPage;
import org.xwiki.user.test.po.ChangeAvatarPage;
import org.xwiki.user.test.po.GroupsUserProfilePage;
import org.xwiki.user.test.po.PreferencesEditPage;
import org.xwiki.user.test.po.PreferencesUserProfilePage;
import org.xwiki.user.test.po.ProfileEditPage;
import org.xwiki.user.test.po.ProfileUserProfilePage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test the User Profile.
 * 
 * @version $Id$
 * @since 11.10
 */
@UITest(extraJARs = {
    // The Solr store is not ready yet to be installed as an extension so we need to add it to WEB-INF/lib manually
    "org.xwiki.platform:xwiki-platform-eventstream-store-solr"
})
public class UserProfileIT
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

    private static final String DEFAULT_EDITOR = "Text (Default)";

    private static final String SIMPLE_USER = "Simple";

    private static final String ADVANCED_USER = "Advanced";

    private static final String PARIS_TZ = "Europe/Paris";

    private static final String DEFAULT_PASSWORD = "testtest";

    private String userName;

    @BeforeEach
    public void setUp(TestUtils setup, TestReference testReference)
        throws Exception
    {
        this.userName = testReference.getLastSpaceReference().getName();
        setup.loginAsSuperAdmin();
        setup.rest().deletePage("XWiki", this.userName);
        setup.createUserAndLogin(this.userName, DEFAULT_PASSWORD);

        // At first edition the Dashboard is saving the doc to insert a new object, so we need to be sure
        // this has been done before performing our other test, to avoid getting stale element references.
        setup.gotoPage("XWiki", this.userName, "edit");
        new EditPage();
    }

    /** Functionality check: changing profile information. */
    @Test
    @Order(1)
    public void editProfile()
    {
        ProfileUserProfilePage userProfilePage = ProfileUserProfilePage.gotoPage(this.userName);
        ProfileEditPage profileEditPage = userProfilePage.editProfile();
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

        userProfilePage = new ProfileUserProfilePage(this.userName);
        // Check that the information was updated
        assertEquals(USER_FIRST_NAME, userProfilePage.getUserFirstName());
        assertEquals(USER_LAST_NAME, userProfilePage.getUserLastName());
        assertEquals(USER_COMPANY, userProfilePage.getUserCompany());
        assertEquals(USER_ABOUT, userProfilePage.getUserAbout());
        assertEquals(USER_EMAIL, userProfilePage.getUserEmail());
        assertEquals(USER_PHONE, userProfilePage.getUserPhone());
        assertEquals(USER_ADDRESS, userProfilePage.getUserAddress());
        assertEquals(USER_BLOG, userProfilePage.getUserBlog());
        assertEquals(USER_BLOGFEED, userProfilePage.getUserBlogFeed());
    }

    /** Functionality check: changing the profile picture. */
    @Test
    @Order(2)
    public void changeAvatarImage(TestConfiguration testConfiguration)
    {
        ProfileUserProfilePage userProfilePage = ProfileUserProfilePage.gotoPage(this.userName);
        ChangeAvatarPage changeAvatarImage = userProfilePage.changeAvatarImage();
        File imageFile = new File(testConfiguration.getBrowser().getTestResourcesPath(), IMAGE_NAME);
        changeAvatarImage.setAvatarImage(imageFile.getAbsolutePath());
        changeAvatarImage.submit();
        assertEquals(IMAGE_NAME, userProfilePage.getAvatarImageName());
    }

    /** Functionality check: changing the user type. */
    @Test
    @Order(3)
    public void changeUserProfile()
    {
        ProfileUserProfilePage userProfilePage = ProfileUserProfilePage.gotoPage(this.userName);
        PreferencesUserProfilePage preferencesPage = userProfilePage.switchToPreferences();
        assertEquals("", preferencesPage.getTimezone());

        // Setting to Simple user and setting the timezone to Europe/Paris
        PreferencesEditPage preferencesEditPage = preferencesPage.editPreferences();
        preferencesEditPage.setSimpleUserType();
        preferencesEditPage.setTimezone(PARIS_TZ);
        preferencesEditPage.clickSaveAndView();

        userProfilePage = new ProfileUserProfilePage(this.userName);
        preferencesPage = userProfilePage.switchToPreferences();
        assertEquals(SIMPLE_USER, preferencesPage.getUserType());
        assertEquals(PARIS_TZ, preferencesPage.getTimezone());

        // Setting to Advanced user
        preferencesEditPage = preferencesPage.editPreferences();
        preferencesEditPage.setAdvancedUserType();
        preferencesEditPage.clickSaveAndView();

        userProfilePage = new ProfileUserProfilePage(this.userName);
        userProfilePage.switchToPreferences();
        assertEquals(ADVANCED_USER, preferencesPage.getUserType());
    }

    /** Functionality check: changing the default editor. */
    @Test
    @Order(4)
    public void changeDefaultEditor()
    {
        ProfileUserProfilePage userProfilePage = ProfileUserProfilePage.gotoPage(this.userName);
        PreferencesUserProfilePage preferencesPage = userProfilePage.switchToPreferences();

        // Setting to Text Editor
        PreferencesEditPage preferencesEditPage = preferencesPage.editPreferences();
        preferencesEditPage.setDefaultEditorText();
        preferencesEditPage.clickSaveAndView();

        userProfilePage = new ProfileUserProfilePage(this.userName);
        preferencesPage = userProfilePage.switchToPreferences();
        assertEquals(TEXT_EDITOR, preferencesPage.getDefaultEditor());

        // Setting to WYSIWYG Editor
        userProfilePage = ProfileUserProfilePage.gotoPage(this.userName);
        preferencesPage = userProfilePage.switchToPreferences();
        preferencesEditPage = preferencesPage.editPreferences();
        preferencesEditPage.setDefaultEditorWysiwyg();
        preferencesEditPage.clickSaveAndView();

        userProfilePage = new ProfileUserProfilePage(this.userName);
        preferencesPage = userProfilePage.switchToPreferences();
        assertEquals(WYSIWYG_EDITOR, preferencesPage.getDefaultEditor());

        // Setting to Default Editor
        userProfilePage = ProfileUserProfilePage.gotoPage(this.userName);
        preferencesPage = userProfilePage.switchToPreferences();
        preferencesEditPage = preferencesPage.editPreferences();
        preferencesEditPage.setDefaultEditorDefault();
        preferencesEditPage.clickSaveAndView();

        userProfilePage = new ProfileUserProfilePage(this.userName);
        preferencesPage = userProfilePage.switchToPreferences();
        assertEquals(DEFAULT_EDITOR, preferencesPage.getDefaultEditor());
    }

    /**
     * Check that the content of the first comment isn't used as the "About" information in the user profile. See
     * XAADMINISTRATION-157.
     */
    @Test
    @Order(5)
    public void commentDoesntOverrideAboutInformation(TestUtils setup)
    {
        ProfileUserProfilePage userProfilePage = ProfileUserProfilePage.gotoPage(this.userName);
        String commentContent = "this is from a comment";

        int commentId = userProfilePage.openCommentsDocExtraPane().postComment(commentContent, true);
        setup.getDriver().navigate().refresh();
        assertFalse(userProfilePage.getContent().contains(commentContent),
            "Comment content was used as profile information");

        if (commentId != -1) {
            userProfilePage.openCommentsDocExtraPane().deleteCommentByID(commentId);
        }
    }

    @Test
    @Order(6)
    public void ensureDashboardUIAddAnObjectAtFirstEdit()
    {
        ProfileUserProfilePage userProfilePage = ProfileUserProfilePage.gotoPage(this.userName);
        HistoryPane historyPane = userProfilePage.openHistoryDocExtraPane();
        assertEquals("Initialize default dashboard user setup", historyPane.getCurrentVersionComment());
        assertEquals("2.1", historyPane.getCurrentVersion());
    }

    @Test
    @Order(7)
    public void verifyGroupTab()
    {
        GroupsUserProfilePage preferencesPage = GroupsUserProfilePage.gotoPage(this.userName);

        assertEquals("Groups", preferencesPage.getPreferencesTitle());
        LiveTableElement groupsPaneLiveTable = preferencesPage.getGroupsPaneLiveTable();

        assertEquals(1, groupsPaneLiveTable.getRowCount());
        assertEquals("XWikiAllGroup", groupsPaneLiveTable.getCell(1, 1).getText());
    }

    @Test
    @Order(8)
    public void toggleEnableDisable(TestUtils setup)
    {
        ProfileUserProfilePage userProfilePage = ProfileUserProfilePage.gotoPage(this.userName);
        // We are already logged in with this user, so we shouldn't be able to change the status
        assertFalse(userProfilePage.isDisableButtonAvailable());
        assertFalse(userProfilePage.isEnableButtonAvailable());

        // Buttons should be available with super admin
        setup.loginAsSuperAdmin();
        userProfilePage = ProfileUserProfilePage.gotoPage(this.userName);
        assertTrue(userProfilePage.isDisableButtonAvailable());
        assertFalse(userProfilePage.isEnableButtonAvailable());

        // Ensure that we can disable the user and buttons are switching
        userProfilePage.clickDisable();
        assertFalse(userProfilePage.isDisableButtonAvailable());
        assertTrue(userProfilePage.isEnableButtonAvailable());

        // Ensure that the state has been saved
        userProfilePage = ProfileUserProfilePage.gotoPage(this.userName);
        assertFalse(userProfilePage.isDisableButtonAvailable());
        assertTrue(userProfilePage.isEnableButtonAvailable());

        // Enable back
        userProfilePage.clickEnable();
        assertTrue(userProfilePage.isDisableButtonAvailable());
        assertFalse(userProfilePage.isEnableButtonAvailable());

        userProfilePage = ProfileUserProfilePage.gotoPage(this.userName);
        assertTrue(userProfilePage.isDisableButtonAvailable());
        assertFalse(userProfilePage.isEnableButtonAvailable());
    }

}
