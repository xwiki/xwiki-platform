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
package org.xwiki.watchlist.test.ui;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.user.test.po.NetworkUserProfilePage;
import org.xwiki.user.test.po.ProfileUserProfilePage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests the ability to watch users.
 *
 * @version $Id$
 */
public class WatchUsersTest extends AbstractTest
{
    @Rule
    public SuperAdminAuthenticationRule superAdminAuthenticationRule = new SuperAdminAuthenticationRule(getUtil());

    @Test
    public void testFollowUser() throws Exception
    {
        // Create 2 test users and log in with Bob, the creepy guy :).
        String alice = "Alice";
        getUtil().deletePage("XWiki", alice);
        getUtil().createUser(alice, "password", null);

        String bob = "Bob";
        getUtil().deletePage("XWiki", bob);
        getUtil().createUserAndLogin(bob, "password");

        // Check that Bob does not follow anyone at the start.
        NetworkUserProfilePage networkPage = NetworkUserProfilePage.gotoPage(bob);
        List<String> followedUsers = networkPage.getFollowedUsers();
        assertEquals("The list of followed users should be empty in the start", 0, followedUsers.size());

        // Go to Alice's profile, check that she is not followed then start following her.
        ProfileUserProfilePage profilePage = ProfileUserProfilePage.gotoPage(alice);
        assertFalse("Another users should not be followed by default", profilePage.isFollowed());
        profilePage = profilePage.toggleFollowButton();
        assertTrue("After following, the UI should show the unfollow option", profilePage.isFollowed());

        // Go to Bob's Network profile tab and check that Alice is followed.
        networkPage = NetworkUserProfilePage.gotoPage(bob);
        followedUsers = networkPage.getFollowedUsers();
        assertEquals("There should be 1 user in the list of followed users", 1, followedUsers.size());
        assertTrue("The user Alice should be in the list of followed users", followedUsers.contains(alice));

        // Unfollow Alice.
        networkPage.unfollowUser(alice);
        followedUsers = networkPage.getFollowedUsers();
        assertEquals("There should be no more users in the list of followed users", 0, followedUsers.size());

        // Cleanup
        getUtil().deletePage("XWiki", alice);
        getUtil().deletePage("XWiki", bob);
    }
}
