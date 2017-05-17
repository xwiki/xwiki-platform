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

import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.platform.notifications.test.po.NotificationsTrayPage;
import org.xwiki.platform.notifications.test.po.NotificationsUserProfilePage;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.ObjectEditPage;
import org.xwiki.test.ui.po.editor.ObjectEditPane;

import com.sun.xml.internal.fastinfoset.tools.FI_StAX_SAX_Or_XML_SAX_SAXEvent;

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
        p.disableAllStandardParameters();

        getUtil().login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        p = NotificationsUserProfilePage.gotoPage(SECOND_USER_NAME);
        p.disableAllStandardParameters();
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
        p = NotificationsUserProfilePage.gotoPage(SECOND_USER_NAME);
        p.disableAllStandardParameters();

        // Now, let’s test the XObject based notification types
        /* Process :
           - As FIRST_USER_NAME, create a page with a XObject «XWiki.TagClass»
             (no particular reason, it should work with any XObject)
           - Add the PageNotificationEventDescriptorClass XObject in another page, listening for events that come from a
             DocumentUpdatedEvent
           - Check that the notification preference is correctly displayed with SECOND_USER_NAME
           - Update the document with FIST_USER_NAME
           - Check that the SECOND_USER_NAME did not get any notifications, then enable the preference
           - Update the document with FIRST_USER_NAME
           - Check that SECOND_USER_NAME get the notification
           - Delete the first XObject, then update the document
           - Check that SECOND_USER_NAME did not get any notifications
           - Delete the PageNotificationEventDescriptorClass XObject
           - Check that the notification preference is not visible anymore
         */

        // Create a page with a XWiki.TagClass XObject
        getUtil().login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        ViewPage page1 = getUtil().createPage(getTestClassName(), "Page1", "Page1", "XObjectTest1");
        ObjectEditPage page1XObjects = page1.editObjects();
        page1XObjects.addObject("XWiki.TagClass");
        page1XObjects.clickSaveAndView();

        ViewPage page2 = getUtil().createPage(getTestClassName(), "Page2", "Page2", "XObjectTest2");
        ObjectEditPage page2XObjects = page2.editObjects();
        page2XObjects.addObject("XWiki.Notifications.Code.PageNotificationEventDescriptorClass");
        List<ObjectEditPane> xObjects = page2XObjects
                .getObjectsOfClass("XWiki.Notifications.Code.PageNotificationEventDescriptorClass");
        xObjects.get(0).fillFieldsByName(new HashMap<String, String>()
        {{
            put("applicationName", "Notification Tests");
            put("eventId", "test-xobject-notification");
            put("eventPrettyName", "Test for XObject based notifications");
            put("eventIcon", "rss");
            put("eventType", "org.xwiki.bridge.event.DocumentUpdatedEvent");
            put("objectType", "XWiki.TagClass");
            put("notificationTemplate", "Static template");
        }});
        page2XObjects.clickSaveAndView();

        // Check that the notification preference is correctly displayed with SECOND_USER_NAME
        getUtil().login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        p = NotificationsUserProfilePage.gotoPage(SECOND_USER_NAME);

        // Should throw a NoSuchElementFoundException if the element does not exists
        p.findNotificationParameterRow("test-xobject-notification");

        // Now update the first page with FIRST_USER_NAME
        getUtil().login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        getUtil().gotoPage(getTestClassName(), "Page1").editWiki().setContent("Updated content in Page1");

        getUtil().login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        tray = new NotificationsTrayPage();
        assertEquals(0, tray.getNotificationsCount());
        p = NotificationsUserProfilePage.gotoPage(SECOND_USER_NAME);
        p.enablePreference(p.findNotificationParameterRow("test-xobject-notification"));

        getUtil().login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        getUtil().gotoPage(getTestClassName(), "Page1").editWiki().setContent("Again, updated content in Page1");

        getUtil().login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        tray = new NotificationsTrayPage();
        assertEquals(1, tray.getNotificationsCount());

    }
}
