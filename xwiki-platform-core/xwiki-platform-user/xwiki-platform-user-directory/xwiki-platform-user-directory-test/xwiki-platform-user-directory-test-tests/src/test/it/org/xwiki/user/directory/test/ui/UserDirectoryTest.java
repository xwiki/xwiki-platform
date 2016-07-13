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
package org.xwiki.user.directory.test.ui;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.test.ui.po.LiveTableElement;
import org.xwiki.user.directory.test.po.UserDirectoryPage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests the User Directory feature.
 *
 * @version $Id$
 * @since 5.1M1
 */
public class UserDirectoryTest extends AbstractTest
{
    @Rule
    public SuperAdminAuthenticationRule authenticationRule = new SuperAdminAuthenticationRule(getUtil());

    @Test
    public void verifyUserIsListed()
    {
        // Delete possible existing user
        getUtil().deletePage("XWiki", "test");

        UserDirectoryPage page = UserDirectoryPage.gotoPage();

        // Verify that the user directory is empty when there's no user in the wiki
        LiveTableElement liveTableElement = page.getUserDirectoryLiveTable();
        assertEquals(0, liveTableElement.getRowCount());

        // Add a user and verify it's visible in the livetable
        getUtil().createUserAndLogin("test", "testtest", "first_name", "John", "last_name", "Doe");
        // Go back to the user directory page since the user creation navigated to another page
        page = UserDirectoryPage.gotoPage();
        assertEquals(1, liveTableElement.getRowCount());
        assertTrue(liveTableElement.hasRow("User ID", "test"));
        assertTrue(liveTableElement.hasRow("First Name", "John"));
        assertTrue(liveTableElement.hasRow("Last Name", "Doe"));

        // Log out to verify the livetable works in guest view too
        getUtil().forceGuestUser();
        assertEquals(1, liveTableElement.getRowCount());
        assertTrue(liveTableElement.hasRow("User ID", "test"));
        assertTrue(liveTableElement.hasRow("First Name", "John"));
        assertTrue(liveTableElement.hasRow("Last Name", "Doe"));
    }
}
