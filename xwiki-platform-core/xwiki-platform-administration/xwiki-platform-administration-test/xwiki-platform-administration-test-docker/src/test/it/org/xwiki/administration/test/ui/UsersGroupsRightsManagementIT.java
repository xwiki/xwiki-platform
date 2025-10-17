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
package org.xwiki.administration.test.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.administration.test.po.CreateGroupModal;
import org.xwiki.administration.test.po.DeleteUserConfirmationModal;
import org.xwiki.administration.test.po.EditGroupModal;
import org.xwiki.administration.test.po.GroupEditPage;
import org.xwiki.administration.test.po.GroupsPage;
import org.xwiki.administration.test.po.RegistrationModal;
import org.xwiki.administration.test.po.UsersAdministrationSectionPage;
import org.xwiki.livedata.test.po.TableLayoutElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.WikisSource;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ConfirmationModal;
import org.xwiki.test.ui.po.CopyOrRenameOrDeleteStatusPage;
import org.xwiki.test.ui.po.DeletePageOutcomePage;
import org.xwiki.test.ui.po.EditRightsPane;
import org.xwiki.test.ui.po.RenamePage;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.RightsEditPage;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@UITest(properties = {
    // Add the RightsManagerPlugin needed by the test
    "xwikiCfgPlugins=com.xpn.xwiki.plugin.rightsmanager.RightsManagerPlugin",
    // Programming rights are required to disable/enable user profiles (cf. XWIKI-21238)
    "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:XWiki\\.XWikiUserProfileSheet",
    "xwikiDbHbmCommonExtraMappings=notification-filter-preferences.hbm.xml"
    },
    extraJARs = {
    // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus,
    // we need to provide the JAR inside WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-19932
    "org.xwiki.platform:xwiki-platform-notifications-filters-default"
    }
)
class UsersGroupsRightsManagementIT
{
    @BeforeEach
    public void setup(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
    }

    /**
     * <ul>
     * <li>Validate group creation.</li>
     * <li>Validate groups administration print "0" members for empty group.</li>
     * <li>Validate group deletion.</li>
     * <li>Validate rights automatically cleaned from deleted groups.</li>
     * </ul>
     */
    @Test
    @Order(1)
    void createAndDeleteGroup(TestUtils setup, TestReference testReference)
    {
        String groupName = testReference.getLastSpaceReference().getName();

        // Make sure the group doesn't exist.
        setup.deletePage("XWiki", groupName);
        GroupsPage groupsPage = GroupsPage.gotoPage();
        groupsPage.addNewGroup(groupName);

        // Validate XWIKI-1903: Empty group shows 1 member.
        String memberCount = groupsPage.getMemberCount(groupName);
        assertEquals("0", memberCount);

        // Give "view" global right to the new group on wiki
        AdministrationPage administrationPage = AdministrationPage.gotoPage();
        EditRightsPane editRightsPane = administrationPage.clickGlobalRightsSection().getEditRightsPane();
        editRightsPane.switchToGroups();
        assertTrue(editRightsPane.hasEntity(groupName));
        editRightsPane.setRight(groupName, EditRightsPane.Right.VIEW, EditRightsPane.State.ALLOW);

        // Give "comment" page right to XWikiNewGroup on Test.TestCreateAndDeleteGroup page
        setup.createPage(testReference, "whatever", testReference.getName());
        RightsEditPage rightsEditPage = RightsEditPage.gotoPage(testReference.getLastSpaceReference().getName(),
            testReference.getName());
        rightsEditPage.switchToGroups();
        assertTrue(rightsEditPage.hasEntity(groupName));
        rightsEditPage.setRight(groupName, EditRightsPane.Right.COMMENT, EditRightsPane.State.ALLOW);

        // Delete the newly created group and see if rights are cleaned
        groupsPage = GroupsPage.gotoPage();
        groupsPage.deleteGroup(groupName);

        // Validate XWIKI-2304: When a user or a group is removed it's not removed from rights objects
        AdministrationPage.gotoPage();
        editRightsPane = administrationPage.clickGlobalRightsSection().getEditRightsPane();
        editRightsPane.switchToGroups();
        assertFalse(editRightsPane.hasEntity(groupName));

        rightsEditPage = RightsEditPage.gotoPage(testReference.getLastSpaceReference().getName(),
            testReference.getName());
        rightsEditPage.switchToGroups();
        assertFalse(rightsEditPage.hasEntity(groupName));
    }

    /**
     * Verify the following group editing features, from 2 locations: from the Admin UI Group page and from the
     * Group page itself (in inline edit mode):
     * <ul>
     * <li>Validate adding users as group members.</li>
     * <li>Validate adding sub-groups</li>
     * <li>Validate removing user members.</li>
     * <li>Validate removing sub-groups.</li>
     * </ul>
     */
    @Test
    @Order(2)
    void editGroup(TestUtils setup, TestReference testReference) throws Exception
    {
        //
        // Setup
        //

        String testName = testReference.getLastSpaceReference().getName();
        String devs = String.format("%s_%s", testName, "devs");
        String frontEndDevs = String.format("%s_%s", testName, "frontEndDevs");
        String backEndDevs = String.format("%s_%s", testName, "backEndDevs");
        String alice = String.format("%s_%s", testName, "Alice");
        String bob = String.format("%s_%s", testName, "Bob");

        // Clean-up
        setup.rest().deletePage("XWiki", devs);
        setup.rest().deletePage("XWiki", frontEndDevs);
        setup.rest().deletePage("XWiki", backEndDevs);
        setup.rest().deletePage("XWiki", alice);
        setup.rest().deletePage("XWiki", bob);

        // Create the groups & the users.
        setup.createUser(alice, alice, "", "first_name", "", "last_name", "");
        setup.createUser(bob, bob, "", "first_name", "", "last_name", "");
        GroupsPage groupsPage = GroupsPage.gotoPage();
        groupsPage.addNewGroup(frontEndDevs).addNewGroup(backEndDevs).addNewGroup(devs);

        // Test that the groups have been successfully added.
        groupsPage.getGroupsTable().assertRow("Group Name", devs);
        groupsPage.getGroupsTable().assertRow("Group Name", frontEndDevs);
        groupsPage.getGroupsTable().assertRow("Group Name", backEndDevs);

        //
        // Work with the group page directly.
        //

        // Add members by editing the group page directly.
        GroupEditPage devsGroupPage = GroupEditPage.gotoPage(new DocumentReference("xwiki", "XWiki", devs));
        devsGroupPage.addGroups(frontEndDevs, backEndDevs).addUsers(alice, bob);

        // Verify that the members have been added to the live table.
        devsGroupPage.getMembersTable().assertRow("Member", frontEndDevs);
        devsGroupPage.getMembersTable().assertRow("Member", backEndDevs);
        devsGroupPage.getMembersTable().assertRow("Member", alice);
        devsGroupPage.getMembersTable().assertRow("Member", bob);

        // Remove an user and a sub-group.
        devsGroupPage.removeMembers(alice, backEndDevs);

        // Verify that the live table has been updated.
        TableLayoutElement membersTable = devsGroupPage.getMembersTable();
        membersTable.assertRow("Member", frontEndDevs);
        membersTable.assertRow("Member",
            not(hasItem(devsGroupPage.getMembersTable().getWebElementTextMatcher(backEndDevs))));
        membersTable.assertRow("Member", not(hasItem(devsGroupPage.getMembersTable().getWebElementTextMatcher(alice))));
        membersTable.assertRow("Member", bob);

        //
        // Work with the group edit modal from the administration.
        //

        // Edit the group from the administration, using the modal.
        groupsPage = GroupsPage.gotoPage();
        assertEquals("2", groupsPage.getMemberCount(devs));
        EditGroupModal devsGroupModal = groupsPage.clickEditGroup(devs);

        // Verify that the changes we did by editing the group page directly have been saved.
        devsGroupModal.getMembersTable().assertRow("Member", frontEndDevs);
        devsGroupModal.getMembersTable()
            .assertRow("Member", not(hasItem(devsGroupModal.getMembersTable().getWebElementTextMatcher(backEndDevs))));
        devsGroupModal.getMembersTable()
            .assertRow("Member", not(hasItem(devsGroupModal.getMembersTable().getWebElementTextMatcher(alice))));
        devsGroupModal.getMembersTable().assertRow("Member", bob);

        // Add new members to the group.
        devsGroupModal.addUsers(alice).addGroups(backEndDevs);

        // Check if the group live table is updated.
        devsGroupModal.getMembersTable().assertRow("Member", backEndDevs);
        devsGroupModal.getMembersTable().assertRow("Member", alice);

        // Close the modal and wait for the groups live table to be reloaded.
        devsGroupModal.close();

        // Check the new group member count.
        assertEquals("4", groupsPage.getMemberCount(devs));

        // Edit the group again and remove some members.
        devsGroupModal = groupsPage.clickEditGroup(devs);
        devsGroupModal.removeMembers(bob, backEndDevs);

        // Verify that the live table is updated.
        devsGroupModal.getMembersTable().assertRow("Member", frontEndDevs);
        TableLayoutElement tableLayoutElement = devsGroupModal.getMembersTable();
        tableLayoutElement.assertRow("Member", not(hasItem(tableLayoutElement.getWebElementTextMatcher(backEndDevs))));
        devsGroupModal.getMembersTable().assertRow("Member", alice);
        devsGroupModal.getMembersTable()
            .assertRow("Member", not(hasItem(devsGroupModal.getMembersTable().getWebElementTextMatcher(bob))));

        // Close the modal and check the updated member count.
        devsGroupModal.close();
        assertEquals("2", groupsPage.getMemberCount(devs));
    }

    /**
     * Validate that administration show error when trying to create an existing group.
     */
    @Test
    @Order(3)
    void createGroupWhenGroupAlreadyExists(TestUtils setup, TestReference testReference)
    {
        String testName = testReference.getLastSpaceReference().getName();
        setup.createPage("XWiki", testName, "", "");

        CreateGroupModal createGroupModal =
            GroupsPage.gotoPage().clickCreateGroup().setGroupName(testName)
                .waitForValidationError(testName + " cannot be used for the group name, "
                    + "as another page with this name already exists.");
        assertFalse(createGroupModal.getCreateGroupButton().isEnabled());
    }

    /**
     * <ul>
     * <li>Validate user creation.</li>
     * <li>Validate user disable/enable</li>
     * <li>Validate user deletion.</li>
     * <li>Validate groups automatically cleaned from deleted users.</li>
     * <li>Validate default groups are updated when a deleted user is restored.</li>
     * </ul>
     */
    @Test
    @Order(4)
    void createAndDeleteUser(TestUtils setup, TestReference testReference)
    {
        String userName = testReference.getLastSpaceReference().getName();

        // ensure the user does not exist
        setup.deletePage("XWiki", userName);

        // Test user creation
        UsersAdministrationSectionPage usersPage = UsersAdministrationSectionPage.gotoPage();
        RegistrationModal registrationModal = usersPage.clickAddNewUser();
        registrationModal.fillRegisterForm("", "", userName, userName, userName, "");
        registrationModal.waitForLiveValidationSuccess();
        registrationModal.clickRegister();
        usersPage.waitForNotificationSuccessMessage("User created");
        usersPage.getUsersLiveData().getTableLayout().assertRow("User", userName);

        // Verify that new users are automatically added to the XWikiAllGroup group.
        GroupsPage groupsPage = GroupsPage.gotoPage();
        assertEquals(1, groupsPage.clickEditGroup("XWikiAllGroup").filterMembers(userName).countRows());

        usersPage = UsersAdministrationSectionPage.gotoPage();

        int rowNumber = usersPage.getRowNumberByUsername(userName);

        // Verify the user is enabled and can only be edited or disabled
        assertFalse(usersPage.isUserDisabled(rowNumber));
        assertFalse(usersPage.canDeleteUser(rowNumber));
        assertFalse(usersPage.canEnableUser(rowNumber));
        assertTrue(usersPage.canDisableUser(rowNumber));
        assertTrue(usersPage.canEditUser(rowNumber));

        // Verify that when the user is disabled it can be enabled back, deleted or edited
        usersPage = usersPage.disableUser(rowNumber);
        assertTrue(usersPage.isUserDisabled(rowNumber));
        assertTrue(usersPage.canDeleteUser(rowNumber));
        assertTrue(usersPage.canEnableUser(rowNumber));
        assertFalse(usersPage.canDisableUser(rowNumber));
        assertTrue(usersPage.canEditUser(rowNumber));

        // Delete the newly created user and see if groups are cleaned
        ConfirmationModal confirmation = usersPage.clickDeleteUser(rowNumber);
        assertTrue(confirmation.getMessage().contains("Are you sure you want to proceed?"));
        confirmation.clickOk();
        usersPage.getUsersLiveData().getTableLayout().assertRow("User",
            not(hasItem(usersPage.getUsersLiveData().getTableLayout().getWebElementTextMatcher(userName))));

        // Verify that when a user is removed, they are removed from the groups they belong to.
        groupsPage = GroupsPage.gotoPage();
        assertEquals(0, groupsPage.clickEditGroup("XWikiAllGroup").filterMembers(userName).countRows());

        // Verify that when a user is restored, it's put back in the default group
        setup.gotoPage("XWiki", userName);
        DeletePageOutcomePage deletePageOutcomePage = new DeletePageOutcomePage();
        deletePageOutcomePage.getDeletedTerminalPagesEntries().get(0).clickRestore();
        groupsPage = GroupsPage.gotoPage();
        assertEquals(1, groupsPage.clickEditGroup("XWikiAllGroup").filterMembers(userName).countRows());
    }

    /**
     * Test that the Ajax registration tool accepts non-ASCII symbols.
     */
    @Test
    @Order(5)
    void createNonAsciiUser(TestUtils setup, TestReference testReference)
    {
        String userName = testReference.getLastSpaceReference().getName();
        String firstName = "aéb";
        String lastName = "càd";

        // ensure the user does not exist
        setup.deletePage("XWiki", userName);

        // Test user creation
        UsersAdministrationSectionPage usersPage = UsersAdministrationSectionPage.gotoPage();
        RegistrationModal registrationModal = usersPage.clickAddNewUser();
        registrationModal.fillRegisterForm(firstName, lastName, userName, userName, userName, "");
        registrationModal.clickRegister();
        usersPage.waitForNotificationSuccessMessage("User created");

        TableLayoutElement tableLayout = usersPage.getUsersLiveData().getTableLayout();
        tableLayout.assertRow("User", userName);
        tableLayout.assertRow("First Name", firstName);
        tableLayout.assertRow("Last Name", lastName);
    }

    /**
     * Validate group rights. Validate XWIKI-2375: Group and user access rights problem with a name which includes space
     * characters
     */
    @Test
    @Order(6)
    void groupRights(TestUtils setup, TestReference testReference)
    {
        String userName = testReference.getLastSpaceReference().getName();
        // Voluntarily put a space in the group name.
        String groupname = "Test Group";

        DocumentReference testDocument = new DocumentReference("TestGroupsRights",
            testReference.getLastSpaceReference());

        // Make sure there's no "TestUser" user and no "Test Group" user before we try to create it
        setup.deletePage(testDocument);
        setup.deletePage("XWiki", userName);
        setup.deletePage("XWiki", groupname);

        // Create a new user, a new group, make the user part of that group and create a new page
        setup.createUser(userName, userName, "");
        GroupsPage groupsPage = GroupsPage.gotoPage();
        groupsPage = groupsPage.addNewGroup(groupname);
        EditGroupModal editGroupModal = groupsPage.clickEditGroup(groupname).addMember(userName, true);
        assertEquals(1, editGroupModal.filterMembers(userName).countRows());
        editGroupModal.close();

        // Create a page and deny view to it
        setup.createPage(testDocument, "Some content");
        setup.addObject(testDocument, "XWiki.XWikiRights",
            "levels", "view",
            "groups", "XWiki." + groupname,
            "allow", "Deny");

        // Make sure that Admins can still view the page
        ViewPage viewPage = setup.gotoPage(testDocument);
        assertEquals("Some content", viewPage.getContent());

        // And ensure that the newly created user cannot view it
        setup.login(userName, userName);
        viewPage = setup.gotoPage(testDocument);
        assertTrue(viewPage.isForbidden());
    }

    /**
     * Validate member filtering on group sheet.
     */
    @Test
    @Order(7)
    void testFilteringOnGroupSheet(TestUtils setup, TestReference testReference)
    {
        String groupName = testReference.getLastSpaceReference().getName();
        String userName = groupName + "User";

        // ensure the group and user doesn't exist yet
        setup.deletePage("XWiki", groupName);
        setup.deletePage("XWiki", userName);

        // create user, group and put the user has member of the group
        setup.createUser(userName, userName, "");
        GroupsPage groupsPage = GroupsPage.gotoPage();
        groupsPage = groupsPage.addNewGroup(groupName);
        groupsPage.clickEditGroup(groupName).addMember(userName, true).close();

        GroupEditPage groupEditPage = GroupEditPage.gotoPage(new DocumentReference("xwiki", "XWiki", groupName));
        assertEquals(1, groupEditPage.getMembersTable().countRows());
        groupEditPage.getMembersTable().assertRow("Member", userName);

        groupEditPage.getMembersTable().filterColumn("Member", "zzz");
        assertEquals(0, groupEditPage.getMembersTable().countRows());

        groupEditPage.getMembersTable().filterColumn("Member", groupName.substring(2));
        assertEquals(1, groupEditPage.getMembersTable().countRows());
        groupEditPage.getMembersTable().assertRow("Member", userName);
    }

    @Test
    @Order(8)
    void deleteUserWithScriptRights(TestUtils setup, TestReference testReference)
    {
        setup.loginAsSuperAdmin();
        String scriptUserName = testReference.getLastSpaceReference().getName();
        String scriptUserPassword = "password";
        setup.createUser(scriptUserName, scriptUserPassword, "");
        setup.setGlobalRights( "", "XWiki.%s".formatted(scriptUserName), "script", true);
        setup.login(scriptUserName, scriptUserPassword);
        setup.createPage(testReference, "");
        setup.loginAsSuperAdmin();
        UsersAdministrationSectionPage usersPage = UsersAdministrationSectionPage.gotoPage();
        usersPage.getUsersLiveData().getTableLayout().filterColumn("User", scriptUserName);
        usersPage.disableUser(1);
        DeleteUserConfirmationModal deleteUserConfirmationModal = usersPage.clickDeleteUser(1);
        assertEquals("/xwiki/bin/view/Main/AllDocs?doc.author=XWiki.%s".formatted(scriptUserName),
            deleteUserConfirmationModal.getScriptRightUserErrorMessageHrefValue());
        deleteUserConfirmationModal.clickOk();
        assertEquals(0, usersPage.getUsersLiveData().getTableLayout().countRows());
    }

    @ParameterizedTest
    @Order(9)
    @WikisSource(extensions = { "org.xwiki.platform:xwiki-platform-administration-ui" })
    void renameUserUpdatesGroupAndRights(WikiReference wiki, TestUtils setup)
    {
        setup.loginAsSuperAdmin();
        setup.setCurrentWiki(wiki.getName());
        String userName = "userToRename";
        String newUserName = "renamedUser";
        String groupName = "groupForRenamedUser";
        DocumentReference userRef = new DocumentReference(wiki.getName(), "XWiki", userName);
        // Ensure the user and group doesn't exist yet.
        setup.deletePage("XWiki", userName);
        setup.deletePage("XWiki", newUserName);
        setup.deletePage("XWiki", groupName);

        // Create user, group and put the user has member of the group.
        setup.createUser(userName, userName, "");
        GroupsPage groupsPage = GroupsPage.gotoPage();
        groupsPage = groupsPage.addNewGroup(groupName);
        groupsPage.clickEditGroup(groupName).addMember(userName, true).close();

        // Give "view" global right to the user on wiki.
        AdministrationPage administrationPage = AdministrationPage.gotoPage();
        EditRightsPane editRightsPane = administrationPage.clickGlobalRightsSection().getEditRightsPane();
        editRightsPane.switchToUsers();
        assertTrue(editRightsPane.hasEntity(userName));
        editRightsPane.setRight(userName, EditRightsPane.Right.VIEW, EditRightsPane.State.ALLOW);

        // Rename the user.
        ViewPage userProfile = setup.gotoPage(userRef);
        RenamePage rename = userProfile.rename();
        rename.getDocumentPicker().setTitle(newUserName);
        rename.setTerminal(true);
        CopyOrRenameOrDeleteStatusPage renameStatusPage = rename.clickRenameButton();
        renameStatusPage.waitUntilFinished();

        // Verify the user has been renamed.
        UsersAdministrationSectionPage usersPage = UsersAdministrationSectionPage.gotoPage();
        TableLayoutElement usersTable = usersPage.getUsersLiveData().getTableLayout();
        usersTable.filterColumn("User", userName);
        assertEquals(0, usersTable.countRows());
        usersTable.filterColumn("User", newUserName);
        usersTable.assertRow("User", newUserName);

        // Verify the group has been updated with the new user name.
        groupsPage = GroupsPage.gotoPage();
        EditGroupModal editGroupModal = groupsPage.clickEditGroup(groupName);
        TableLayoutElement membersTable = editGroupModal.getMembersTable();
        membersTable.filterColumn("Member", userName);
        assertEquals(0, membersTable.countRows());
        membersTable.filterColumn("Member", newUserName);
        membersTable.assertRow("Member", newUserName);
        editGroupModal.close();

        // Verify the global rights have been updated with the new user name.
        administrationPage = AdministrationPage.gotoPage();
        editRightsPane = administrationPage.clickGlobalRightsSection().getEditRightsPane();
        editRightsPane.switchToUsers();
        assertFalse(editRightsPane.hasEntity(userName));
        assertTrue(editRightsPane.hasEntity(newUserName));
        assertEquals(EditRightsPane.State.ALLOW, editRightsPane.getRight(newUserName, EditRightsPane.Right.VIEW));
        // Reset the right to avoid interference with other tests.
        editRightsPane.setRight(newUserName, EditRightsPane.Right.VIEW, EditRightsPane.State.NONE);
    }

    @ParameterizedTest
    @Order(10)
    @WikisSource(extensions = { "org.xwiki.platform:xwiki-platform-administration-ui" })
    void renameGroupUpdatesGroupsAndRights(WikiReference wiki, TestUtils setup)
    {
        setup.loginAsSuperAdmin();
        setup.setCurrentWiki(wiki.getName());
        String groupName = "groupToRename";
        String newGroupName = "renamedGroupName";
        String parentGroupName = "parentGroupForRenamedGroup";
        DocumentReference groupRef = new DocumentReference(wiki.getName(), "XWiki", groupName);
        // Ensure the group doesn't exist yet.
        setup.deletePage("XWiki", groupName);
        setup.deletePage("XWiki", newGroupName);
        setup.deletePage("XWiki", parentGroupName);

        // create groups and put one group as member of the other group.
        GroupsPage groupsPage = GroupsPage.gotoPage();
        groupsPage.addNewGroup(groupName).addNewGroup(parentGroupName);
        groupsPage.clickEditGroup(parentGroupName).addMember(groupName, false).close();

        // Give "view" global right to the group on wiki.
        AdministrationPage administrationPage = AdministrationPage.gotoPage();
        EditRightsPane editRightsPane = administrationPage.clickGlobalRightsSection().getEditRightsPane();
        editRightsPane.switchToGroups();
        assertTrue(editRightsPane.hasEntity(groupName));
        editRightsPane.setRight(groupName, EditRightsPane.Right.VIEW, EditRightsPane.State.ALLOW);

        // Rename the group.
        ViewPage groupPage = setup.gotoPage(groupRef);
        RenamePage rename = groupPage.rename();
        rename.getDocumentPicker().setTitle(newGroupName);
        rename.setTerminal(true);
        CopyOrRenameOrDeleteStatusPage renameStatusPage = rename.clickRenameButton();
        renameStatusPage.waitUntilFinished();

        // Verify the group has been renamed.
        groupsPage = GroupsPage.gotoPage();
        TableLayoutElement groupsTable = groupsPage.getGroupsTable();
        groupsTable.filterColumn("Group Name", groupName);
        assertEquals(0, groupsTable.countRows());
        groupsTable.filterColumn("Group Name", newGroupName);
        groupsTable.assertRow("Group Name", newGroupName);

        // Verify the global rights have been updated with the new group name.
        administrationPage = AdministrationPage.gotoPage();
        editRightsPane = administrationPage.clickGlobalRightsSection().getEditRightsPane();
        editRightsPane.switchToGroups();
        assertFalse(editRightsPane.hasEntity(groupName));
        assertTrue(editRightsPane.hasEntity(newGroupName));
        assertEquals(EditRightsPane.State.ALLOW, editRightsPane.getRight(newGroupName, EditRightsPane.Right.VIEW));
        // Reset the right to avoid interference with other tests.
        editRightsPane.setRight(newGroupName, EditRightsPane.Right.VIEW, EditRightsPane.State.NONE);

        // Verify the parent group has been updated with the new group name.
        groupsPage = GroupsPage.gotoPage();
        EditGroupModal editGroupModal = groupsPage.clickEditGroup(parentGroupName);
        TableLayoutElement membersTable = editGroupModal.getMembersTable();
        membersTable.filterColumn("Member", groupName);
        assertEquals(0, membersTable.countRows());
        membersTable.filterColumn("Member", newGroupName);
        membersTable.assertRow("Member", newGroupName);
        editGroupModal.close();
    }
}
