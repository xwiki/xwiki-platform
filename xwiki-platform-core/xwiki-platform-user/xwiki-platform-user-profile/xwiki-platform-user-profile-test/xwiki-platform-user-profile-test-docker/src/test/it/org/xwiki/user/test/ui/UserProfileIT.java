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
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.xwiki.livedata.test.po.TableLayoutElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.WikisSource;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.HistoryPane;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.ClassEditPage;
import org.xwiki.test.ui.po.editor.EditPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;
import org.xwiki.user.test.po.ChangeAvatarPage;
import org.xwiki.user.test.po.GroupsUserProfilePage;
import org.xwiki.user.test.po.PreferencesEditPage;
import org.xwiki.user.test.po.PreferencesUserProfilePage;
import org.xwiki.user.test.po.ProfileEditPage;
import org.xwiki.user.test.po.ProfileUserProfilePage;
import org.xwiki.user.test.po.UserInactivePage;
import org.xwiki.user.test.po.UserProfileAdministrationSectionPage;

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
        "org.xwiki.platform:xwiki-platform-eventstream-store-solr",
        // The macro service uses the extension index script service to get the list of uninstalled macros (from
        // extensions) which expects an implementation of the extension index. The extension index script service is a
        // core extension so we need to make the extension index also core.
        "org.xwiki.platform:xwiki-platform-extension-index"
    }
)
class UserProfileIT
{
    private static final String IMAGE_NAME = "avatar.png";

    /**
     * The name sorts before {@link #IMAGE_NAME} on purpose: after an upload, the attachment picker selects the most
     * recent attachment rather than the uploaded one, and attachment dates have a precision of one second. When both
     * images are uploaded within the same second the tie is resolved by file name, so this name makes the second
     * upload the selected one. See XWIKI-25240: Uploading a file in the attachment picker can select another
     * attachment when both have the same upload second.
     */
    private static final String OTHER_IMAGE_NAME = "another-avatar.png";

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

    private static final String DEFAULT_EDITOR = "Text (Default)";

    private static final String SIMPLE_USER = "Simple";

    private static final String ADVANCED_USER = "Advanced";

    private static final String PARIS_TZ = "Europe/Paris";

    private static final String DEFAULT_PASSWORD = "testtest";

    private static final String CUSTOM_PROPERTY_NAME = "facebook";

    private static final String CUSTOM_PROPERTY_PRETTY_NAME = "Facebook";

    private static final String CUSTOM_PROPERTY_VALUE = "MyFacebookProfile";

    private static final String ACCOUNT_DISABLED_MESSAGE =
        "Your account has been disabled. Please contact the administrator if you think this is a mistake.";

    private static final String GROUPS_CLASS = "XWiki.XWikiGroups";

    private static final String GROUP_COLUMN = "Group";

    /**
     * The Page Index, the User Index and the Application Index, which are listed in the drawer, with their titles.
     */
    private static final Map<DocumentReference, String> INDEX_PAGES = Map.of(
        new DocumentReference("xwiki", "Main", "AllDocs"), "Pages on this Wiki",
        new DocumentReference("xwiki", "Main", "UserDirectory"), "User Index",
        new DocumentReference("xwiki", "Applications", "WebHome"), "Application Index");

    private String userName;

    @BeforeEach
    void setUp(TestUtils setup, TestReference testReference) throws Exception
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
        // The avatar image is only updated once the profile page is reloaded after the upload. Wait for the new image
        // before reading it, to avoid a stale element reference while the page is still reloading.
        userProfilePage.waitUntilAvatarImageName(IMAGE_NAME);
        assertEquals(IMAGE_NAME, userProfilePage.getAvatarImageName());

        // Select another image: it replaces the first one as the avatar.
        userProfilePage = new ProfileUserProfilePage(this.userName);
        changeAvatarImage = userProfilePage.changeAvatarImage();
        imageFile = new File(testConfiguration.getBrowser().getTestResourcesPath(), OTHER_IMAGE_NAME);
        changeAvatarImage.setAvatarImage(imageFile.getAbsolutePath());
        changeAvatarImage.submit();
        userProfilePage.waitUntilAvatarImageName(OTHER_IMAGE_NAME);
        assertEquals(OTHER_IMAGE_NAME, ProfileUserProfilePage.gotoPage(this.userName).getAvatarImageName());
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

    /**
     * Check that the content of the first comment isn't used as the "About" information in the user profile. See
     * XAADMINISTRATION-157.
     */
    @Test
    @Order(5)
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
    @Order(6)
    void ensureDashboardUIAddAnObjectAtFirstEdit()
    {
        ProfileUserProfilePage userProfilePage = ProfileUserProfilePage.gotoPage(this.userName);
        HistoryPane historyPane = userProfilePage.openHistoryDocExtraPane();
        assertEquals("Initialize default dashboard user setup", historyPane.getCurrentVersionComment());
        assertEquals("2.1", historyPane.getCurrentVersion());
    }

    @Test
    @Order(7)
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
    @Order(8)
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
            assertEquals("Unexpected code [401], was expecting one of [[201, 202]]", e.getMessage());
            gotException = true;
        }
        // The disabled user gets a notice instead of the content of the pages listed in the drawer.
        for (DocumentReference indexPage : INDEX_PAGES.keySet()) {
            setup.gotoPage(indexPage);
            assertEquals(ACCOUNT_DISABLED_MESSAGE, new UserInactivePage().getMessage());
        }

        setup.loginAsSuperAdmin();
        ViewPage viewPage = setup.gotoPage(testReference);
        assertFalse(viewPage.exists());
        assertTrue(gotException);

        // Once enabled again, the user is logged in and sees the content of these pages instead of the notice, and
        // can edit a page.
        ProfileUserProfilePage.gotoPage(this.userName).clickEnable();
        setup.login(this.userName, DEFAULT_PASSWORD);
        for (Map.Entry<DocumentReference, String> indexPage : INDEX_PAGES.entrySet()) {
            viewPage = setup.gotoPage(indexPage.getKey());
            assertEquals(indexPage.getValue(), viewPage.getDocumentTitle());
            assertFalse(UserInactivePage.isDisplayed(),
                indexPage.getKey() + " still shows the account disabled notice");
            assertEquals(this.userName, viewPage.getCurrentUser());
        }
        WikiEditPage editPage = WikiEditPage.gotoPage(testReference);
        editPage.setContent("Edited after the account was enabled again");
        assertEquals("Edited after the account was enabled again", editPage.clickSaveAndView().getContent());
    }

    /**
     * A custom field added to the {@code XWiki.XWikiUsers} class and configured in a profile section is displayed
     * when viewing a user's profile.
     */
    @Test
    @Order(9)
    void extendUserProfile(TestUtils setup)
    {
        // Admin rights are required both to extend the XWikiUsers class and to configure the profile section.
        setup.loginAsSuperAdmin();

        // Step 1: add a new property to the XWiki.XWikiUsers class through the class editor.
        ClassEditPage classEditPage = ClassEditPage.gotoPage("XWiki", "XWikiUsers");
        classEditPage.addProperty(CUSTOM_PROPERTY_NAME, "TextArea").setPrettyName(CUSTOM_PROPERTY_PRETTY_NAME);
        classEditPage.clickSaveAndView();

        // Step 2: configure the "User Profile" administration section to display the new property in the "Personal"
        // section (object number 0).
        UserProfileAdministrationSectionPage adminSection = UserProfileAdministrationSectionPage.gotoPage();
        adminSection.appendPropertyToSection(0, CUSTOM_PROPERTY_NAME);
        adminSection.clickSave();

        // Step 3: set a value for the new property on the test user's profile. Log in as the user so that the value is
        // set on their own profile: this is both the realistic feature scenario and the reliable flow (editing another
        // user's profile as admin renders a different layout - e.g. the enable/disable buttons - whose edit pencil is
        // not exercised by any other test).
        setup.login(this.userName, DEFAULT_PASSWORD);
        ProfileUserProfilePage userProfilePage = ProfileUserProfilePage.gotoPage(this.userName);
        ProfileEditPage profileEditPage = userProfilePage.editProfile();
        profileEditPage.setUserCustomProperty(CUSTOM_PROPERTY_NAME, CUSTOM_PROPERTY_VALUE);
        profileEditPage.clickSaveAndView();

        // Step 4 (expected result): the new field is displayed when viewing the user's profile.
        userProfilePage = new ProfileUserProfilePage(this.userName);
        assertEquals(CUSTOM_PROPERTY_VALUE, userProfilePage.getUserCustomProperty(CUSTOM_PROPERTY_PRETTY_NAME));
    }

    /**
     * The Groups tab of the profile of a global user lists the groups of the main wiki and of the subwikis that the
     * user belongs to, directly or through another group.
     */
    @ParameterizedTest
    @WikisSource(mainWiki = false)
    @Order(10)
    void groupMembershipIncludesSubwikiGroups(WikiReference subwiki, TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();
        String userReference = "XWiki." + this.userName;
        DocumentReference firstGroup = new DocumentReference("xwiki", "XWiki", this.userName + "FirstGroup");
        DocumentReference secondGroup = new DocumentReference("xwiki", "XWiki", this.userName + "SecondGroup");
        DocumentReference parentGroup = new DocumentReference("xwiki", "XWiki", this.userName + "ParentGroup");
        DocumentReference subwikiGroup =
            new DocumentReference(subwiki.getName(), "XWiki", this.userName + "SubwikiGroup");
        setup.rest().addObject(firstGroup, GROUPS_CLASS, "member", userReference);
        setup.rest().addObject(secondGroup, GROUPS_CLASS, "member", userReference);
        setup.rest().addObject(parentGroup, GROUPS_CLASS, "member", "XWiki." + firstGroup.getName());
        setup.rest().addObject(subwikiGroup, GROUPS_CLASS, "member", "xwiki:" + userReference);

        List<DocumentReference> expectedGroups = List.of(new DocumentReference("xwiki", "XWiki", "XWikiAllGroup"),
            firstGroup, secondGroup, parentGroup, subwikiGroup);
        // Both an administrator and the user see all the groups of the user.
        assertGroups(expectedGroups, setup);
        setup.login(this.userName, DEFAULT_PASSWORD);
        assertGroups(expectedGroups, setup);
    }

    private void assertGroups(List<DocumentReference> expectedGroups, TestUtils setup)
    {
        GroupsUserProfilePage groupsPage = GroupsUserProfilePage.gotoPage(this.userName);
        assertEquals("Groups", groupsPage.getPreferencesTitle());
        TableLayoutElement tableLayout = groupsPage.getGroupsPaneLiveData().getTableLayout();
        assertEquals(expectedGroups.size(), tableLayout.countRows());
        for (DocumentReference group : expectedGroups) {
            tableLayout.assertCellWithLink(GROUP_COLUMN, group.getName(), setup.getURL(group));
        }
    }
}
