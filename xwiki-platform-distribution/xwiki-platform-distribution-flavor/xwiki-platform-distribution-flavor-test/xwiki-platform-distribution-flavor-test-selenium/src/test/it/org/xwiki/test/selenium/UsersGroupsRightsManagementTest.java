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
package org.xwiki.test.selenium;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.xwiki.administration.test.po.AdministrationMenu;
import org.xwiki.administration.test.po.CreateGroupModal;
import org.xwiki.administration.test.po.EditGroupModal;
import org.xwiki.administration.test.po.GroupEditPage;
import org.xwiki.administration.test.po.GroupsPage;
import org.xwiki.administration.test.po.RegistrationModal;
import org.xwiki.administration.test.po.UsersAdministrationSectionPage;
import org.xwiki.test.selenium.framework.AbstractXWikiTestCase;
import org.xwiki.test.ui.po.ConfirmationModal;
import org.xwiki.test.ui.po.SuggestInputElement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verify the Users, Groups and Rights Management features of XWiki.
 *
 * @version $Id$
 */
public class UsersGroupsRightsManagementTest extends AbstractXWikiTestCase
{
    private AdministrationMenu administrationMenu = new AdministrationMenu();

    /**
     * <ul>
     * <li>Validate group creation.</li>
     * <li>Validate groups administration print "0" members for empty group.</li>
     * <li>Validate group deletion.</li>
     * <li>Validate rights automatically cleaned from deleted groups.</li>
     * </ul>
     */
    @Test
    public void testCreateAndDeleteGroup()
    {
        // Make sure there's no XWikiNewGroup before we try to create it
        deleteGroup("XWikiNewGroup", true);
        createGroup("XWikiNewGroup");

        // Validate XWIKI-1903: Empty group shows 1 member.
        assertEquals("Group XWikiNewGroup which is empty print more than 0 members", "0",
            new GroupsPage().getMemberCount("XWikiNewGroup"));

        // Give "view" global right to XWikiNewGroup on wiki
        openGlobalRightsPage();
        clickGroupsRadioButton();
        clickViewRightsCheckbox("XWikiNewGroup", "allow");

        // Give "comment" page right to XWikiNewGroup on Test.TestCreateAndDeleteGroup page
        createPage("Test", "TestCreateAndDeleteGroup", "whatever");
        clickEditPageAccessRights();
        clickGroupsRadioButton();
        clickCommentRightsCheckbox("XWikiNewGroup", "allow");

        // Delete the newly created group and see if rights are cleaned
        deleteGroup("XWikiNewGroup", false);

        // Validate XWIKI-2304: When a user or a group is removed it's not removed from rights objects
        open("XWiki", "XWikiPreferences", "edit", "editor=object");
        assertTextNotPresent("XWikiNewGroup");
        open("Test", "TestCreateAndDeleteGroup", "edit", "editor=object");
        assertTextNotPresent("XWikiNewGroup");
    }

    /**
     * Validate that administration show error when trying to create an existing group.
     */
    @Test
    public void testCreateGroupWhenGroupAlreadyExists()
    {
        open("XWiki", "testCreateGroupWhenGroupAlreadyExists", "edit", "editor=wiki");
        clickEditSaveAndView();
        CreateGroupModal createGroupModal =
            GroupsPage.gotoPage().clickCreateGroup().setGroupName("testCreateGroupWhenGroupAlreadyExists")
                .waitForValidationError("testCreateGroupWhenGroupAlreadyExists cannot be used for the group name, "
                    + "as another page with this name already exists.");
        assertFalse(createGroupModal.getCreateGroupButton().isEnabled());
    }

    /**
     * <ul>
     * <li>Validate user creation.</li>
     * <li>Validate user deletion.</li>
     * <li>Validate groups automatically cleaned from deleted users.</li>
     * </ul>
     */
    @Test
    public void testCreateAndDeleteUser()
    {
        // Make sure there's no XWikiNewUser user before we try to create it
        deleteUser("XWikiNewUser", true);
        createUser("XWikiNewUser", "XWikiNewUser");

        // Verify that new users are automatically added to the XWikiAllGroup group.
        open("XWiki", "XWikiAllGroup");
        waitForGroupUsersLiveTable();
        assertTextPresent("XWikiNewUser");

        // Delete the newly created user and see if groups are cleaned
        deleteUser("XWikiNewUser", false);

        // Verify that when a user is removed he's removed from the groups he belongs to.
        open("XWiki", "XWikiAllGroup");
        waitForGroupUsersLiveTable();
        assertTextNotPresent("XWikiNewUser");
    }

    /**
     * Test that the Ajax registration tool accepts non-ASCII symbols.
     */
    @Test
    public void testCreateNonAsciiUser()
    {
        // Make sure there's no AccentUser user before we try to create it
        deleteUser("AccentUser", true);

        // Use ISO-8859-1 symbols to make sure that the test works both in ISO-8859-1 and UTF8
        createUser("AccentUser", "AccentUser", "a\u00e9b", "c\u00e0d");

        // Verify that the user is present in the table
        assertTextPresent("AccentUser");
        // Verify that the correct symbols appear
        assertTextPresent("a\u00e9b");
        assertTextPresent("c\u00e0d");
    }

    /**
     * Validate group rights. Validate XWIKI-2375: Group and user access rights problem with a name which includes space
     * characters
     */
    @Test
    public void testGroupRights()
    {
        String username = "TestUser";
        // Voluntarily put a space in the group name.
        String groupname = "Test Group";

        // Make sure there's no "TestUser" user and no "Test Group" user before we try to create it
        deleteUser(username, true);
        deleteGroup(groupname, true);

        // Create a new user, a new group, make the user part of that group and create a new page
        createUser(username, username);
        createGroup(groupname);
        addUserToGroup(username, groupname);
        createPage("Test", "TestGroupRights", "Some content");

        // Deny view rights to the group on the newly created page
        open("Test", "TestGroupRights", "edit", "editor=rights");
        clickGroupsRadioButton();
        // Click a first time to allow view and a second time to deny it.
        clickViewRightsCheckbox(groupname, "allow");
        clickViewRightsCheckbox(groupname, "deny1");

        // Make sure that Admins can still view the page
        open("Test", "TestGroupRights");
        assertTextPresent("Some content");

        // And ensure that the newly created user cannot view it
        login(username, username, false);
        open("Test", "TestGroupRights");
        assertTextPresent("not allowed");

        // Cleanup
        loginAsAdmin();
        deleteUser(username, false);
        deleteGroup(groupname, false);
    }

    /**
     * Test adding a group to a group. Specifically, assert that the group is added as a member itself, not adding all
     * its members one by one.
     */
    @Test
    public void testAddGroupToGroup()
    {
        String group = "GroupWithGroup";
        deleteGroup(group, true);
        createGroup(group);

        GroupsPage groupsPage = GroupsPage.gotoPage();
        groupsPage.filterGroups(group);
        assertEquals(1, groupsPage.getGroupsTable().getRowCount());
        assertTrue(groupsPage.getGroupsTable().hasRow("Members", "0"));

        EditGroupModal editGroupModal = groupsPage.clickEditGroup(group);
        assertEquals(0, editGroupModal.getMembersTable().getRowCount());
        editGroupModal.addMember("XWikiAllGroup", false);
        assertEquals(1, editGroupModal.getMembersTable().getRowCount());
        assertTrue(editGroupModal.getMembersTable().hasRow("Member", "XWikiAllGroup"));
        editGroupModal.close();
        groupsPage.getGroupsTable().waitUntilReady();

        assertEquals(1, groupsPage.getGroupsTable().getRowCount());
        assertTrue(groupsPage.getGroupsTable().hasRow("Members", "1"));

        // Now do the same by editing the group page in Inline Form edit mode.
        clickLinkWithText(group);
        clickEditPageInlineForm();
        assertTrue(new GroupEditPage().addMemberToGroup("XWikiAdminGroup", false).getMembersTable().hasRow("Member",
            "XWikiAdminGroup"));
    }

    /**
     * Validate adding a member to a group via the administration.
     */
    @Test
    public void testAddUserToGroup()
    {
        // Make sure there's no XWikiNewUser user before we try to create it
        deleteUser("XWikiTestUser", true);
        createUser("XWikiTestUser", "XWikiTestUser");

        addUserToGroup("XWikiTestUser", "XWikiAdminGroup");

        deleteUser("XWikiTestUser", true);
    }

    /**
     * Validate member filtering on group sheet.
     */
    @Test
    public void testFilteringOnGroupSheet()
    {
        GroupsPage.gotoPage();
        String rowXPath = "//td[contains(@class, 'member')]//a[@href='/xwiki/bin/view/XWiki/Admin']";
        this.clickLinkWithText("XWikiAdminGroup");
        this.waitForCondition("selenium.isElementPresent(\"" + rowXPath + "\")");

        this.getSelenium().focus("member");
        this.getSelenium().typeKeys("member", "zzz");
        this.waitForCondition("!selenium.isElementPresent(\"" + rowXPath + "\")");

        this.getSelenium().focus("member");
        // Type Backspace 3 times to delete the previous text.
        this.getSelenium().typeKeys("member", "\b\b\bAd");
        this.waitForCondition("selenium.isElementPresent(\"" + rowXPath + "\")");
    }

    // Helper methods

    private void createGroup(String groupName)
    {
        GroupsPage.gotoPage().clickCreateGroup().createGroup(groupName);

        // The groups live table is refreshed.
        waitForLiveTable("groupstable");
    }

    /**
     * @param deleteOnlyIfExists if true then only delete the group if it exists
     */
    private void deleteGroup(String groupName, boolean deleteOnlyIfExists)
    {
        if (!deleteOnlyIfExists || (deleteOnlyIfExists && isExistingPage("XWiki", groupName))) {
            GroupsPage groupsPage = GroupsPage.gotoPage();
            assertTrue(groupsPage.getGroupsTable().hasRow("Group Name", groupName));
            ConfirmationModal confirmation = groupsPage.clickDeleteGroup(groupName);
            assertEquals("The group XWiki." + groupName + " will be deleted. Are you sure you want to proceed?",
                confirmation.getMessage());
            confirmation.clickOk();
            groupsPage.getGroupsTable().waitUntilReady();
            assertFalse(groupsPage.getGroupsTable().hasRow("Group Name", groupName));
        }
    }

    private void createUser(String login, String pwd)
    {
        createUser(login, pwd, "New", "User");
    }

    private void createUser(String login, String pwd, String fname, String lname)
    {
        UsersAdministrationSectionPage usersPage = openUsersPage();
        RegistrationModal registrationModal = usersPage.clickAddNewUser();
        registrationModal.fillRegisterForm(fname, lname, login, pwd, pwd, "new.user@xwiki.org");
        registrationModal.clickRegister();
        usersPage.waitForNotificationSuccessMessage("User created");
        usersPage.getUsersLiveTable().waitUntilReady();
        assertTrue(usersPage.getUsersLiveTable().hasRow("User", login));
    }

    private void deleteUser(String login, boolean deleteOnlyIfExists)
    {
        if (!deleteOnlyIfExists || (deleteOnlyIfExists && isExistingPage("XWiki", login))) {
            UsersAdministrationSectionPage usersPage = openUsersPage();
            ConfirmationModal confirmation = usersPage.clickDeleteUser(login);
            assertEquals("The user XWiki." + login + " will be deleted and removed from all groups he belongs to. "
                + "Are you sure you want to proceed?", confirmation.getMessage());
            confirmation.clickOk();
            usersPage.getUsersLiveTable().waitUntilReady();
            assertFalse(usersPage.getUsersLiveTable().hasRow("User", login));
        }
    }

    private void addUserToGroup(String user, String group)
    {
        GroupsPage groupsPage = GroupsPage.gotoPage();
        EditGroupModal editGroupModal = groupsPage.clickEditGroup(group);
        editGroupModal.addMember(user, true);
        assertTrue(editGroupModal.getMembersTable().hasRow("Member", "New User" + user));
        editGroupModal.close();

        open("XWiki", group);
        waitForGroupUsersLiveTable();
        assertTextPresent(user);
    }

    private void setSuggestInputValue(String id, String value)
    {
        SuggestInputElement suggester = new SuggestInputElement(getDriver().findElementWithoutWaiting(By.id(id)));
        suggester.clearSelectedSuggestions().sendKeys(value).waitForSuggestions().sendKeys(Keys.ENTER)
            .hideSuggestions();
    }

    private void clickGroupsRadioButton()
    {
        clickLinkWithXPath("//input[@name='uorg' and @value='groups']", false);
    }

    private void openGlobalRightsPage()
    {
        openAdministrationPage();
        administrationMenu.expandCategoryWithName("Users & Rights").getSectionByName("Users & Rights", "Rights")
            .click();
    }

    private UsersAdministrationSectionPage openUsersPage()
    {
        // Note: We could have used the following command instead:
        // open("XWiki", "XWikiUsers", "admin", "editor=users")
        // However we haven't done it since we also want to verify that clicking on the "Users" tab works.
        openAdministrationPage();
        administrationMenu.expandCategoryWithName("Users & Rights").getSectionByName("Users & Rights", "Users").click();
        return new UsersAdministrationSectionPage().waitUntilPageIsLoaded();
    }

    /**
     * @param actionToVerify the action that the click is supposed to have done. Valid values are "allow", "deny1" or
     *            "none".
     */
    private void clickViewRightsCheckbox(String groupOrUserName, String actionToVerify)
    {
        clickRightsCheckbox(groupOrUserName, actionToVerify, 2);
    }

    /**
     * @param actionToVerify the action that the click is supposed to have done. Valid values are "allow", "deny1" or
     *            "none".
     */
    private void clickCommentRightsCheckbox(String groupOrUserName, String actionToVerify)
    {
        clickRightsCheckbox(groupOrUserName, actionToVerify, 3);
    }

    private void clickRightsCheckbox(String groupOrUserName, String actionToVerify, int positionInTd)
    {
        String xpath = "//tbody/tr[td/a='" + groupOrUserName + "']/td[" + positionInTd + "]/img";
        clickLinkWithXPath(xpath, false);
        // Wait till it has been clicked since this can take some time.
        waitForCondition("selenium.isElementPresent(\"" + xpath + "[contains(@src, '" + actionToVerify + ".png')]\")");
    }

    /**
     * Waits for the live table that lists group users to load.
     */
    private void waitForGroupUsersLiveTable()
    {
        waitForLiveTable("groupusers");
    }
}
