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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Before;
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

    // Number of pages that have to be created in order for the notifications badge to show «X+»
    private static final int PAGES_TOP_CREATION_COUNT = 21;

    @Before
    public void setUpUsers() throws Exception
    {
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

        // The user 1 creates a new page, the user 2 shouldn’t recieve any notification
        getUtil().login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        getUtil().createPage(getTestClassName(), "WebHome", "Content from " + FIRST_USER_NAME, "Page title");
        getUtil().gotoPage(getTestClassName(), "WebHome");

        getUtil().login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        getUtil().gotoPage(getTestClassName(), "WebHome");

        tray = new NotificationsTrayPage();
        assertFalse(tray.areNotificationsAvailable());

        // The user 2 will now enable his notifications for new pages
        getUtil().login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        p = NotificationsUserProfilePage.gotoPage(SECOND_USER_NAME);
        p.setPageCreated(true);

        // We create a lot of pages in order to test the notification badge
        getUtil().login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        for (int i = 1; i < PAGES_TOP_CREATION_COUNT; i++) {
            getUtil().createPage(getTestClassName(), "Page" + i, "Simple content", "Simple title");
        }
        getUtil().createPage(getTestClassName(), "DTP", "Deletion test page", "Deletion test content");

        // Check that the badge is showing «20+»
        getUtil().login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        getUtil().gotoPage(getTestClassName(), "WebHome");
        tray = new NotificationsTrayPage();
        assertEquals(Integer.MAX_VALUE, tray.getNotificationsCount());

        // Ensure that the notification list is displaying the correct amount of unread notifications
        // (max 5 notifications by default)
        assertEquals(5, tray.getUnreadNotificationsCount());
        assertEquals(0, tray.getReadNotificationsCount());
        tray.markAsRead(0);
        assertEquals(4, tray.getUnreadNotificationsCount());
        assertEquals(1, tray.getReadNotificationsCount());

        // Ensure that a notification has a correct type
        assertEquals("[create]", tray.getNotificationType(0));

        // Reset the notifications count of the user 2
        tray.clearAllNotifications();
        assertEquals(0, tray.getNotificationsCount());
        assertFalse(tray.areNotificationsAvailable());

        // The user 2 will get notifications only for pages deletions
        p = NotificationsUserProfilePage.gotoPage(SECOND_USER_NAME);
        p.setPageCreated(false);
        p.setPageDeleted(true);

        // Delete the "Deletion test page" and test the notification
        getUtil().login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        getUtil().deletePage(getTestClassName(), "DTP");
        
        getUtil().login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        getUtil().gotoPage(getTestClassName(), "WebHome");
        tray = new NotificationsTrayPage();
        assertEquals(1, tray.getNotificationsCount());
        tray.clearAllNotifications();
    }
}
