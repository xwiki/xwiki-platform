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
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.administration.test.po.CreateGroupModal;
import org.xwiki.administration.test.po.EditGroupModal;
import org.xwiki.administration.test.po.GroupEditPage;
import org.xwiki.administration.test.po.GroupsPage;
import org.xwiki.administration.test.po.RegistrationModal;
import org.xwiki.administration.test.po.UsersAdministrationSectionPage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ConfirmationModal;
import org.xwiki.test.ui.po.EditRightsPane;
import org.xwiki.test.ui.po.LiveTableElement;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.RightsEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@UITest(properties = {
    // Add the RightsManagerPlugin needed by the test
    "xwikiCfgPlugins=com.xpn.xwiki.plugin.rightsmanager.RightsManagerPlugin"
})
public class UsersGroupsRightsManagementIT
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
    public void createAndDeleteGroup(TestUtils setup, TestReference testReference)
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
    public void editGroup(TestUtils setup, TestReference testReference) throws Exception
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
        assertTrue(groupsPage.getGroupsTable().hasRow("Group Name", devs), "devs group doesn't exist!");
        assertTrue(groupsPage.getGroupsTable().hasRow("Group Name", frontEndDevs), "frontEndDevs group doesn't exist!");
        assertTrue(groupsPage.getGroupsTable().hasRow("Group Name", backEndDevs), "backEndDevs group doesn't exist!");

        //
        // Work with the group page directly.
        //

        // Add members by editing the group page directly.
        GroupEditPage devsGroupPage = GroupEditPage.gotoPage(new DocumentReference("xwiki", "XWiki", devs));
        devsGroupPage.addGroups(frontEndDevs, backEndDevs).addUsers(alice, bob);

        // Verify that the members have been added to the live table.
        assertTrue(devsGroupPage.getMembersTable().hasRow("Member", frontEndDevs),
            "frontEndDevs group is not part of devs group!");
        assertTrue(devsGroupPage.getMembersTable().hasRow("Member", backEndDevs),
            "backEndDevs group is not part of devs group!");
        assertTrue(devsGroupPage.getMembersTable().hasRow("Member", alice), "Alice is not part of devs group!");
        assertTrue(devsGroupPage.getMembersTable().hasRow("Member", bob), "Bob is not part of devs group!");

        // Remove an user and a sub-group.
        devsGroupPage.removeMembers(alice, backEndDevs);

        // Verify that the live table has been updated.
        assertTrue(devsGroupPage.getMembersTable().hasRow("Member", frontEndDevs),
            "frontEndDevs group is not part of devs group!");
        assertFalse(devsGroupPage.getMembersTable().hasRow("Member", backEndDevs),
            "backEndDevs group is still part of devs group!");
        assertFalse(devsGroupPage.getMembersTable().hasRow("Member", alice), "Alice is still part of devs group!");
        assertTrue(devsGroupPage.getMembersTable().hasRow("Member", bob), "Bob is not part of devs group!");

        //
        // Work with the group edit modal from the administration.
        //

        // Edit the group from the administration, using the modal.
        groupsPage = GroupsPage.gotoPage();
        assertEquals("2", groupsPage.getMemberCount(devs));
        EditGroupModal devsGroupModal = groupsPage.clickEditGroup(devs);

        // Verify that the changes we did by editing the group page directly have been saved.
        assertTrue(devsGroupModal.getMembersTable().hasRow("Member", frontEndDevs),
            "frontEndDevs group is not part of devs group!");
        assertFalse(devsGroupModal.getMembersTable().hasRow("Member", backEndDevs),
            "backEndDevs group is part of devs group!");
        assertFalse(devsGroupModal.getMembersTable().hasRow("Member", alice), "Alice is part of devs group!");
        assertTrue(devsGroupModal.getMembersTable().hasRow("Member", bob), "Bob is not part of devs group!");

        // Add new members to the group.
        devsGroupModal.addUsers(alice).addGroups(backEndDevs);

        // Check if the group live table is updated.
        assertTrue(devsGroupModal.getMembersTable().hasRow("Member", backEndDevs),
            "backEndDevs group is not part of devs group!");
        assertTrue(devsGroupModal.getMembersTable().hasRow("Member", alice), "Alice is not part of devs group!");

        // Close the modal and wait for the groups live table to be reloaded.
        devsGroupModal.close();
        groupsPage.getGroupsTable().waitUntilReady();

        // Check the new group member count.
        assertEquals("4", groupsPage.getMemberCount(devs));

        // Edit the group again and remove some members.
        devsGroupModal = groupsPage.clickEditGroup(devs);
        devsGroupModal.removeMembers(bob, backEndDevs);

        // Verify that the live table is updated.
        assertTrue(devsGroupModal.getMembersTable().hasRow("Member", frontEndDevs),
            "frontEndDevs group is not part of devs group!");
        assertFalse(devsGroupModal.getMembersTable().hasRow("Member", backEndDevs),
            "backEndDevs group is still part of devs group!");
        assertTrue(devsGroupModal.getMembersTable().hasRow("Member", alice), "Alice is not part of devs group!");
        assertFalse(devsGroupModal.getMembersTable().hasRow("Member", bob), "Bob is still part of devs group!");

        // Close the modal and check the updated member count.
        devsGroupModal.close();
        groupsPage.getGroupsTable().waitUntilReady();
        assertEquals("2", groupsPage.getMemberCount(devs));
    }

    /**
     * Validate that administration show error when trying to create an existing group.
     */
    @Test
    @Order(3)
    public void createGroupWhenGroupAlreadyExists(TestUtils setup, TestReference testReference)
    {
        String testName = testReference.getLastSpaceReference().getName();
        setup.createPage("XWiki", testName, "", "");

        CreateGroupModal createGroupModal =
            GroupsPage.gotoPage().clickCreateGroup().setGroupName(testName)
                .waitForValidationError(testName +" cannot be used for the group name, "
                    + "as another page with this name already exists.");
        assertFalse(createGroupModal.getCreateGroupButton().isEnabled());
    }

    /**
     * <ul>
     * <li>Validate user creation.</li>
     * <li>Validate user disable/enable</li>
     * <li>Validate user deletion.</li>
     * <li>Validate groups automatically cleaned from deleted users.</li>
     * </ul>
     */
    @Test
    @Order(4)
    public void createAndDeleteUser(TestUtils setup, TestReference testReference)
    {
        String userName = testReference.getLastSpaceReference().getName();

        // ensure the user does not exist
        setup.deletePage("XWiki", userName);

        // Test user creation
        UsersAdministrationSectionPage usersPage = UsersAdministrationSectionPage.gotoPage();
        RegistrationModal registrationModal = usersPage.clickAddNewUser();
        registrationModal.fillRegisterForm("", "", userName, userName, userName, "");
        registrationModal.clickRegister();
        usersPage.waitForNotificationSuccessMessage("User created");
        usersPage.getUsersLiveTable().waitUntilReady();
        assertTrue(usersPage.getUsersLiveTable().hasRow("User", userName));

        // Verify that new users are automatically added to the XWikiAllGroup group.
        GroupsPage groupsPage = GroupsPage.gotoPage();
        assertEquals(1, groupsPage.clickEditGroup("XWikiAllGroup").filterMembers(userName).getRowCount());

        usersPage = UsersAdministrationSectionPage.gotoPage();

        // Verify the user is enabled and can only be edited or disabled
        assertFalse(usersPage.isUserDisabled(userName));
        assertFalse(usersPage.canDeleteUser(userName));
        assertFalse(usersPage.canEnableUser(userName));
        assertTrue(usersPage.canDisableUser(userName));
        assertTrue(usersPage.canEditUser(userName));

        // Verify that when the user is disabled it can be enabled back, deleted or edited
        usersPage = usersPage.disableUser(userName);
        assertTrue(usersPage.isUserDisabled(userName));
        assertTrue(usersPage.canDeleteUser(userName));
        assertTrue(usersPage.canEnableUser(userName));
        assertFalse(usersPage.canDisableUser(userName));
        assertTrue(usersPage.canEditUser(userName));

        // Delete the newly created user and see if groups are cleaned
        ConfirmationModal confirmation = usersPage.clickDeleteUser(userName);
        assertTrue(confirmation.getMessage().contains("Are you sure you want to proceed?"));
        confirmation.clickOk();
        usersPage.getUsersLiveTable().waitUntilReady();
        assertFalse(usersPage.getUsersLiveTable().hasRow("User", userName));

        // Verify that when a user is removed he's removed from the groups he belongs to.
        groupsPage = GroupsPage.gotoPage();
        assertEquals(0, groupsPage.clickEditGroup("XWikiAllGroup").filterMembers(userName).getRowCount());
    }

    /**
     * Test that the Ajax registration tool accepts non-ASCII symbols.
     */
    @Test
    @Order(5)
    public void createNonAsciiUser(TestUtils setup, TestReference testReference)
    {
        String userName = testReference.getLastSpaceReference().getName();
        String firstName = "a\u00e9b";
        String lastName = "c\u00e0d";

        // ensure the user does not exist
        setup.deletePage("XWiki", userName);

        // Test user creation
        UsersAdministrationSectionPage usersPage = UsersAdministrationSectionPage.gotoPage();
        RegistrationModal registrationModal = usersPage.clickAddNewUser();
        registrationModal.fillRegisterForm(firstName, lastName, userName, userName, userName, "");
        registrationModal.clickRegister();
        usersPage.waitForNotificationSuccessMessage("User created");
        usersPage.getUsersLiveTable().waitUntilReady();
        assertTrue(usersPage.getUsersLiveTable().hasRow("User", userName));
        assertTrue(usersPage.getUsersLiveTable().hasRow("First Name", firstName));
        assertTrue(usersPage.getUsersLiveTable().hasRow("Last Name", lastName));
    }

    /**
     * Validate group rights. Validate XWIKI-2375: Group and user access rights problem with a name which includes space
     * characters
     */
    @Test
    @Order(6)
    public void groupRights(TestUtils setup, TestReference testReference)
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
        assertEquals(1, editGroupModal.filterMembers(userName).getRowCount());
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
    public void testFilteringOnGroupSheet(TestUtils setup, TestReference testReference)
    {
        String groupName = testReference.getLastSpaceReference().getName();
        String userName = groupName+"User";

        // ensure the group and user doesn't exist yet
        setup.deletePage("XWiki", groupName);
        setup.deletePage("XWiki", userName);

        // create user, group and put the user has member of the group
        setup.createUser(userName, userName, "");
        GroupsPage groupsPage = GroupsPage.gotoPage();
        groupsPage = groupsPage.addNewGroup(groupName);
        groupsPage.clickEditGroup(groupName).addMember(userName, true).close();

        GroupEditPage groupEditPage = GroupEditPage.gotoPage(new DocumentReference("xwiki", "XWiki", groupName));
        LiveTableElement membersTable = groupEditPage.getMembersTable();
        assertEquals(1, membersTable.getRowCount());
        assertTrue(membersTable.hasRow("Member", userName));

        membersTable.filterColumn("xwiki-livetable-groupusers-filter-1", "zzz");
        assertEquals(0, membersTable.getRowCount());

        membersTable.filterColumn("xwiki-livetable-groupusers-filter-1", groupName.substring(2));
        assertEquals(1, membersTable.getRowCount());
        assertTrue(membersTable.hasRow("Member", userName));
    }
}
