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
package org.xwiki.platform.notifications.test.ui;

import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.xwiki.platform.notifications.test.po.NotificationsTrayPage;
import org.xwiki.platform.notifications.test.po.NotificationsUserProfilePage;
import org.xwiki.test.ui.AbstractTest;

/**
 * Perform tests on the notifications module.
 * 
 * @version $Id$
 * @since 9.4RC1
 */
public class NotificationsTest extends AbstractTest
{
    private static final String FIRST_USER_NAME = "user1";
    private static final String SECOND_USER_NAME = "user2";

    private static final String FIRST_USER_PASSWORD = "notificationsUser1";
    private static final String SECOND_USER_PASSWORD = "notificationsUser2";

    private void setUpUsers() throws Exception
    {
        // Remove any previous registered users
        getUtil().rest().deletePage("XWiki", FIRST_USER_NAME);
        getUtil().rest().deletePage("XWiki", FIRST_USER_NAME);

        // Create the two users we will be using
        getUtil().createUser(FIRST_USER_NAME, FIRST_USER_PASSWORD, "", "");
        getUtil().createUser(SECOND_USER_NAME, SECOND_USER_PASSWORD, "", "");
        
        NotificationsUserProfilePage p;

        getUtil().login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
        p.disableAllParameters();

        getUtil().login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        p = NotificationsUserProfilePage.gotoPage(SECOND_USER_NAME);
        p.disableAllParameters();
    }

    @Test
    public void testNotifications() throws Exception
    {
        NotificationsUserProfilePage p;
        NotificationsTrayPage tray;

        // Create the users
        this.setUpUsers();

        // The user 1 creates a new page, the user 2 shouldn’t recieve any notification
        getUtil().login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        getUtil().createPage("Main", "WebHome", "Content from " + FIRST_USER_NAME, "Page title");
        getUtil().gotoPage("Main", "WebHome");

        getUtil().login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        getUtil().gotoPage("Main", "WebHome");

        tray = new NotificationsTrayPage();
        assertFalse(tray.areNotificationsAvailable());

        // The user 2 will now enable his notifications for new pages
        getUtil().login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        p = NotificationsUserProfilePage.gotoPage(SECOND_USER_NAME);
        p.setPageCreated(true);

        getUtil().login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        getUtil().createPage("Main", "Page2", "Second content from " + FIRST_USER_NAME, "Second page title");

        getUtil().login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        getUtil().gotoPage("Main", "WebHome");

        tray = new NotificationsTrayPage();
        // assertTrue(tray.areNotificationsAvailable());

        // The user 2 will get notifications only for pages deletions
        getUtil().login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        p = NotificationsUserProfilePage.gotoPage(SECOND_USER_NAME);
        p.setPageCreated(false);
        p.setPageDeleted(true);

        getUtil().login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        getUtil().deletePage("Main", "Page2");

        getUtil().login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        getUtil().gotoPage("Main", "WebHome");
    }
}
