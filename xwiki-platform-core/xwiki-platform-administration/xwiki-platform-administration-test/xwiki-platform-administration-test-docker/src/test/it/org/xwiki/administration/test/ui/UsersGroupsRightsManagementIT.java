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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.administration.test.po.EditGroupModal;
import org.xwiki.administration.test.po.GroupEditPage;
import org.xwiki.administration.test.po.GroupsPage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.EditRightsPane;
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
    @BeforeAll
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

    @Test
    public void addUserAndSubgroupToGroup(TestUtils setup, TestReference testReference) throws Exception
    {
        String testName = testReference.getLastSpaceReference().getName();
        String BIG_GROUP = String.format("%s_%s", testName, "biggroup");
        String SUB_GROUP = String.format("%s_%s", testName, "subgroup");
        String TESTER = String.format("%s_%s", testName, "tester");
        String TESTER2 = String.format("%s_%s", testName, "anothertester");

        // Clean-up
        setup.rest().deletePage("XWiki", BIG_GROUP);
        setup.rest().deletePage("XWiki", SUB_GROUP);
        setup.rest().deletePage("XWiki", TESTER);
        setup.rest().deletePage("XWiki", TESTER2);

        // Create the groups & the user
        setup.createUser(TESTER, TESTER, "", "first_name", "", "last_name", "");
        setup.createUser(TESTER2, TESTER2, "", "first_name", "", "last_name", "");
        GroupsPage groupsPage = GroupsPage.gotoPage();
        groupsPage.addNewGroup(SUB_GROUP);
        groupsPage.addNewGroup(BIG_GROUP);

        // Test that the 2 groups have been successfully added
        assertTrue(groupsPage.getGroupsTable().hasRow("Group Name", BIG_GROUP), "bigGroup doesn't exist!");
        assertTrue(groupsPage.getGroupsTable().hasRow("Group Name", SUB_GROUP), "subGroup doesn't exist!");

        // Add SUB_GROUP & TESTER as members of BIG_GROUP
        GroupEditPage bigGroupPage = GroupEditPage.gotoPage(new DocumentReference("xwiki", "XWiki", BIG_GROUP));
        bigGroupPage.addMemberToGroup(SUB_GROUP, false);
        bigGroupPage.addMemberToGroup(TESTER, true);

        // Test that SUB_GROUP is a member of BIG_GROUP
        bigGroupPage.filterMembers(SUB_GROUP);
        assertTrue(bigGroupPage.getMembersTable().hasRow("Member", SUB_GROUP), "subGroup is not part of bigGroup!");

        // Test that TESTER is a member of BIG_GROUP
        bigGroupPage.filterMembers(TESTER);
        assertTrue(bigGroupPage.getMembersTable().hasRow("Member", TESTER), "tester is not part of bigGroup!");

        // Test adding an user through the modal edit
        groupsPage = GroupsPage.gotoPage();
        EditGroupModal editGroupModal = groupsPage.clickEditGroup(BIG_GROUP);
        assertFalse(editGroupModal.getMembersTable().hasRow("Member", TESTER2), "anotherTester is part of bigGroup!");
        editGroupModal.addMember(TESTER2, true);
        editGroupModal.close();
        // Wait for the groups live table to be reloaded.
        groupsPage.getGroupsTable().waitUntilReady();
        editGroupModal = groupsPage.clickEditGroup(BIG_GROUP);
        assertTrue(editGroupModal.getMembersTable().hasRow("Member", TESTER2), "anotherTester is not part of bigGroup!");
    }
}
