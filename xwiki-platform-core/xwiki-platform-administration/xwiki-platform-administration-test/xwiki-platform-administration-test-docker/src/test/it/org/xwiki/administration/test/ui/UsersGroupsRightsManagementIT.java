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

import org.junit.jupiter.api.Test;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.administration.test.po.GroupsPage;
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
    /**
     * <ul>
     * <li>Validate group creation.</li>
     * <li>Validate groups administration print "0" members for empty group.</li>
     * <li>Validate group deletion.</li>
     * <li>Validate rights automatically cleaned from deleted groups.</li>
     * </ul>
     */
    @Test
    public void testCreateAndDeleteGroup(TestUtils setup, TestReference testReference)
    {
        setup.loginAsSuperAdmin();
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
}
