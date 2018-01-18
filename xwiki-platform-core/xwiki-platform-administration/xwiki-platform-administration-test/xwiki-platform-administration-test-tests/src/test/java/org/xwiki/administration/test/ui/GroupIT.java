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

import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.administration.test.po.GroupEditPage;
import org.xwiki.administration.test.po.GroupsPage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;

public class GroupIT extends AbstractTest
{
    private static final String TESTER = "tester";

    private static final String SUB_GROUP = "subGroup";

    private static final String BIG_GROUP = "bigGroup";

    @Rule
    public SuperAdminAuthenticationRule authenticationRule = new SuperAdminAuthenticationRule(getUtil());

    @Test
    public void addUserAndSubgroupToGroup() throws Exception
    {
        // Clean-up
        getUtil().rest().deletePage("XWiki", BIG_GROUP);
        getUtil().rest().deletePage("XWiki", SUB_GROUP);
        getUtil().rest().deletePage("XWiki", TESTER);

        // Create the groups & the user
        getUtil().createUser(TESTER, TESTER, "", "first_name", "", "last_name", "");
        GroupsPage groupsPage = GroupsPage.gotoPage();
        groupsPage.addNewGroup(SUB_GROUP);
        groupsPage.addNewGroup(BIG_GROUP);

        // Test that the 2 groups have been successfully added
        assertTrue("bigGroup doesn't exist!", groupsPage.getGroupsTable().hasRow("Group Name", BIG_GROUP));
        assertTrue("subGroup doesn't exist!", groupsPage.getGroupsTable().hasRow("Group Name", SUB_GROUP));

        // Add SUB_GROUP & TESTER as members of BIG_GROUP
        GroupEditPage bigGroupPage = GroupEditPage.gotoPage(new DocumentReference("xwiki", "XWiki", BIG_GROUP));
        bigGroupPage.addMemberToGroup(SUB_GROUP, false);
        bigGroupPage.addMemberToGroup(TESTER, true);

        // Test that SUB_GROUP is a member of BIG_GROUP
        bigGroupPage.filterMembers(SUB_GROUP);
        assertTrue("subGroup is not part of bigGroup!",
            bigGroupPage.getMembersTable().hasRow("Member", String.format("%s (XWiki.%s)", SUB_GROUP, SUB_GROUP)));

        // Test that TESTER is a member of BIG_GROUP
        bigGroupPage.filterMembers(TESTER);
        assertTrue("tester is not part of bigGroup!",
            bigGroupPage.getMembersTable().hasRow("Member", String.format("%s (XWiki.%s)", TESTER, TESTER)));
    }
}
