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
import org.xwiki.livedata.test.po.TableLayoutElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.CommentsTab;
import org.xwiki.test.ui.po.HistoryPane;
import org.xwiki.test.ui.po.InformationPane;
import org.xwiki.test.ui.po.ViewPage;
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
@UITest(properties = {
    // We need the notifications feature because the User Profile UI draws the Notifications Macro used in the user
    // profile for the user's activity stream. As a consequence, when a user is created in the test, the
    // UserAddedEventListener is called and global default user notifications filters are copied for the new user,
    // requiring the notifications HBM mapping file.
    "xwikiDbHbmCommonExtraMappings=notification-filter-preferences.hbm.xml",
    // Remove once https://jira.xwiki.org/browse/XWIKI-21238 is fixed. Right now XWikiUserProfileSheet requires
    // Programming Rights to enable/disable a user.
    "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:XWiki\\.XWikiUserProfileSheet"
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
class UserProfileIT
{
    private static final String IMAGE_NAME = "avatar.png";

    private static final String USER_FIRST_NAME = "User";

    private static final String USER_LAST_NAME = "of this Wiki";

    private static final String USER_COMPANY = "XWiki.org";

    private static final String USER_ABOUT = "This is some example text to type into the text area";

    private static final String USER_EMAIL = "webmaster@xwiki.org";

    private static final String USER_EMAIL_OBFUSCATED = "w...@xwiki.org";

    private static final String USER_PHONE = "0000-000-000";

    private static final String USER_ADDRESS = "1600 No Street";

    private static final String USER_BLOG = "http://xwiki.org/";

    private static final String USER_BLOGFEED = "http://xwiki.org/feed";

    private static final String WYSIWYG_EDITOR = "Wysiwyg";

    private static final String TEXT_EDITOR = "Text";

    private static final String NEW_SHORTCUT_VALUE = "B";

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
    void editProfile(TestUtils setup)
    {
        // Turn on email Obfuscation to verify that the email displayed in the user profile is obfuscated.
        setup.loginAsSuperAdmin();
        setup.updateObject("Mail", "MailConfig", "Mail.GeneralMailConfigClass", 0, "obfuscate", "1");
        setup.login(this.userName, DEFAULT_PASSWORD);

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
        assertEquals(USER_EMAIL_OBFUSCATED, userProfilePage.getUserEmail());
        assertEquals(USER_PHONE, userProfilePage.getUserPhone());
        assertEquals(USER_ADDRESS, userProfilePage.getUserAddress());
        assertEquals(USER_BLOG, userProfilePage.getUserBlog());
        assertEquals(USER_BLOGFEED, userProfilePage.getUserBlogFeed());

        // Turn of email obfuscation and verify that the displayed email is not obfuscated anymore.
        setup.loginAsSuperAdmin();
        setup.updateObject("Mail", "MailConfig", "Mail.GeneralMailConfigClass", 0, "obfuscate", "0");
        setup.login(this.userName, DEFAULT_PASSWORD);

        userProfilePage = ProfileUserProfilePage.gotoPage(this.userName);
        assertEquals(USER_EMAIL, userProfilePage.getUserEmail());
    }

    /** Functionality check: changing the profile picture. */
    @Test
    @Order(2)
    void changeAvatarImage(TestConfiguration testConfiguration)
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
    void changeUserProfile()
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
    void changeDefaultEditor()
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

    /** Functionality check: changing the shortcut for the default edit mode. */
    @Test
    @Order(5)
    void changeShortcutViewEdit(TestUtils setup)
    {
        ProfileUserProfilePage userProfilePage = ProfileUserProfilePage.gotoPage(this.userName);
        PreferencesUserProfilePage preferencesPage = userProfilePage.switchToPreferences();
        PreferencesEditPage preferencesEditPage = preferencesPage.editPreferences();
        // Overriding the default shortcut value (E)
        preferencesEditPage.setShortcutViewEdit(NEW_SHORTCUT_VALUE);
        preferencesEditPage.clickSaveAndView();

        userProfilePage = new ProfileUserProfilePage(this.userName);
        preferencesPage = userProfilePage.switchToPreferences();
        assertEquals(NEW_SHORTCUT_VALUE, preferencesPage.getViewEditShortcut());

        // Testing that the updated shortcut preference works as intended
        setup.getDriver().addPageNotYetReloadedMarker();
        setup.getDriver().createActions().sendKeys(NEW_SHORTCUT_VALUE).perform();
        setup.getDriver().waitUntilPageIsReloaded();
        preferencesEditPage = new PreferencesEditPage();
        // Reset the preference
        preferencesEditPage.setShortcutViewEdit("");
        preferencesEditPage.clickSaveAndView();
    }

    /**
     * Check that the content of the first comment isn't used as the "About" information in the user profile. See
     * XAADMINISTRATION-157.
     */
    @Test
    @Order(6)
    void commentDoesntOverrideAboutInformation(TestUtils setup)
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
    @Order(7)
    void ensureDashboardUIAddAnObjectAtFirstEdit()
    {
        ProfileUserProfilePage userProfilePage = ProfileUserProfilePage.gotoPage(this.userName);
        HistoryPane historyPane = userProfilePage.openHistoryDocExtraPane();
        assertEquals("Initialize default dashboard user setup", historyPane.getCurrentVersionComment());
        assertEquals("2.1", historyPane.getCurrentVersion());
    }

    @Test
    @Order(8)
    void verifyGroupTab(TestUtils setup)
    {
        GroupsUserProfilePage preferencesPage = GroupsUserProfilePage.gotoPage(this.userName);

        assertEquals("Groups", preferencesPage.getPreferencesTitle());
        TableLayoutElement tableLayout = preferencesPage.getGroupsPaneLiveData().getTableLayout();

        assertEquals(1, tableLayout.countRows());
        tableLayout.assertCellWithLink("Group", "XWikiAllGroup",
            setup.getURL(new DocumentReference("xwiki", "XWiki", "XWikiAllGroup")));
    }

    @Test
    @Order(9)
    void toggleEnableDisable(TestUtils setup)
    {
        ProfileUserProfilePage userProfilePage = ProfileUserProfilePage.gotoPage(this.userName);
        // We are already logged in with this user, so we shouldn't be able to change the status
        assertFalse(userProfilePage.isDisableButtonAvailable());
        assertFalse(userProfilePage.isEnableButtonAvailable());

        // Buttons should be available with a user having admin rights (which is the case for superadmin)
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

    @Test
    @Order(10)
    void disabledUserTest(TestUtils setup, TestReference testReference)
    {
        setup.loginAsSuperAdmin();
        setup.setGlobalRights("", "XWiki.XWikiGuest", "edit", false);
        ProfileUserProfilePage userProfilePage = ProfileUserProfilePage.gotoPage(this.userName);
        userProfilePage.clickDisable();

        setup.login(this.userName, DEFAULT_PASSWORD);
        boolean gotException = false;
        try {
            setup.rest().savePage(testReference, "Some content", "A title");
        } catch (Throwable e) {
            assertTrue(e.getMessage().startsWith("Unexpected code [401], was expecting one of [[201, 202]] for "));
            gotException = true;
        }
        setup.loginAsSuperAdmin();
        ViewPage viewPage = setup.gotoPage(testReference);
        assertFalse(viewPage.exists());
        assertTrue(gotException);
    }

    @Test
    @Order(11)
    void changeShortcutInformation(TestUtils setup, TestReference testReference)
    {
        ProfileUserProfilePage userProfilePage = ProfileUserProfilePage.gotoPage(this.userName);
        PreferencesUserProfilePage preferencesPage = userProfilePage.switchToPreferences();

        // Overriding the default shortcut value (I)
        PreferencesEditPage preferencesEditPage = preferencesPage.editPreferences();
        preferencesEditPage.setShortcutInformation(NEW_SHORTCUT_VALUE);
        preferencesEditPage.clickSaveAndView();

        ViewPage viewPage = setup.createPage(testReference, "one **two** three", "");
        InformationPane infoPane = viewPage.openInformationDocExtraPane();
        CommentsTab commentsPane = viewPage.openCommentsDocExtraPane();
        assertTrue(commentsPane.isOpened());
        assertFalse(infoPane.isOpened());
        // We try using the default shortcut. We expect it to not work, that is, to still have the commentsTab opened.
        setup.getDriver().createActions().sendKeys("i").perform();
        assertTrue(commentsPane.isOpened());
        assertFalse(infoPane.isOpened());
        // We now use the user preference defined shortcut to open it instead.
        setup.getDriver().createActions().sendKeys(NEW_SHORTCUT_VALUE).perform();
        viewPage.waitForDocExtraPaneActive("information");
        assertFalse(commentsPane.isOpened());
        assertTrue(infoPane.isOpened());
        // We try using the default shortcut to get back to the comments tab. We expect this one to work without change.
        viewPage.useShortcutKeyForCommentPane();
        assertTrue(commentsPane.isOpened());
        assertFalse(infoPane.isOpened());

        // Reset the preference
        userProfilePage = new ProfileUserProfilePage(this.userName);
        preferencesPage = userProfilePage.switchToPreferences();
        preferencesEditPage = new PreferencesEditPage();
        preferencesEditPage.setShortcutInformation("");
        preferencesEditPage.clickSaveAndView();
    }
}
