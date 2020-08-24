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
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.platform.notifications.test.po.NotificationsRSS;
import org.xwiki.platform.notifications.test.po.NotificationsTrayPage;
import org.xwiki.platform.notifications.test.po.NotificationsUserProfilePage;
import org.xwiki.platform.notifications.test.po.preferences.filters.NotificationFilterPreference;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.servletengine.ServletEngine;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.BootstrapSwitch;
import org.xwiki.test.ui.po.CommentsTab;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.ObjectEditPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests display of notifications.
 *
 * @version $Id$
 * @since 12.3RC1
 */
@UITest(
    properties = {
        "xwikiDbHbmCommonExtraMappings=notification-filter-preferences.hbm.xml"
    },
    extraJARs = {
        "org.xwiki.platform:xwiki-platform-notifications-filters-default",
        "org.xwiki.platform:xwiki-platform-eventstream-store-hibernate",
        // The Solr store is not ready yet to be installed as extension
        "org.xwiki.platform:xwiki-platform-eventstream-store-solr"
    }
)
public class NotificationsIT
{
    private static final String FIRST_USER_NAME = NotificationsIT.class.getSimpleName() + "user1";
    private static final String SECOND_USER_NAME = NotificationsIT.class.getSimpleName() + "user2";

    private static final String FIRST_USER_PASSWORD = "notificationsUser1";
    private static final String SECOND_USER_PASSWORD = "notificationsUser2";

    // Number of pages that have to be created in order for the notifications badge to show «X+»
    private static final int PAGES_TOP_CREATION_COUNT = 21;

    private static final String SYSTEM = "org.xwiki.platform";

    private static final String ALERT_FORMAT = "alert";

    private static final String ADD_COMMENT = "addComment";

    private static final String CREATE = "create";

    private static final String DELETE = "delete";

    private static final String UPDATE = "update";

    @BeforeEach
    public void setup(TestUtils testUtils) throws Exception
    {
        // Create the two users we will be using
        testUtils.createUser(FIRST_USER_NAME, FIRST_USER_PASSWORD, "", "");
        testUtils.createUser(SECOND_USER_NAME, SECOND_USER_PASSWORD, "", "");

        NotificationsUserProfilePage p;

        testUtils.login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
        // Make sure to wait until notifications are empty (in case of leftovers bing cleaned from a previous test)
        NotificationsTrayPage.waitOnNotificationCount("xwiki:XWiki." + FIRST_USER_NAME, "xwiki", 0);
        p.disableAllParameters();
        // Enable own filter
        p.getNotificationFilterPreferences().get(2).setEnabled(true);

        testUtils.login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        p = NotificationsUserProfilePage.gotoPage(SECOND_USER_NAME);
        // Make sure to wait until notifications are empty (in case of leftovers bing cleaned from a previous test)
        NotificationsTrayPage.waitOnNotificationCount("xwiki:XWiki." + SECOND_USER_NAME, "xwiki", 0);
        p.disableAllParameters();
    }

    @AfterEach
    public void tearDown(TestUtils testUtils)
    {
        testUtils.deletePage("XWiki", FIRST_USER_NAME);
        testUtils.deletePage("XWiki", SECOND_USER_NAME);
    }

    @Test
    @Order(1)
    public void simpleNotifications(TestUtils testUtils, TestReference testReference) throws Exception
    {
        NotificationsUserProfilePage p;
        NotificationsTrayPage tray;

        // The user 1 creates a new page, the user 2 shouldn’t receive any notification
        testUtils.login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        testUtils.createPage(testReference.getLastSpaceReference().getName(), 
            "WebHome", "Content from " + FIRST_USER_NAME, "Page title");

        testUtils.login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        testUtils.gotoPage(testReference.getLastSpaceReference().getName(), "WebHome");

        tray = new NotificationsTrayPage();
        assertFalse(tray.areNotificationsAvailable());

        // The user 2 will now enable his notifications for new pages
        p = NotificationsUserProfilePage.gotoPage(SECOND_USER_NAME);
        p.getApplication(SYSTEM).setCollapsed(false);
        p.setEventTypeState(SYSTEM, CREATE, ALERT_FORMAT, BootstrapSwitch.State.ON);

        // Yes we wait on a timer, but it is to be sure the following events will be stored AFTER the settings have been
        // changed.
        Thread.sleep(1000);

        // We create a lot of pages in order to test the notification badge
        testUtils.login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        for (int i = 1; i < PAGES_TOP_CREATION_COUNT; i++) {
            testUtils.createPage(testReference.getLastSpaceReference().getName(), 
                "Page" + i, "Simple content", "Simple title");
        }
        testUtils.createPage(testReference.getLastSpaceReference().getName(), 
            "DTP", "Deletion test page", "Deletion test content");

        // Check that the badge is showing «20+»
        testUtils.login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        testUtils.gotoPage(testReference.getLastSpaceReference().getName(), "WebHome");
        NotificationsTrayPage.waitOnNotificationCount("xwiki:XWiki." + SECOND_USER_NAME, "xwiki",
            PAGES_TOP_CREATION_COUNT);
        testUtils.gotoPage(testReference.getLastSpaceReference().getName(), "WebHome");
        tray = new NotificationsTrayPage();
        assertEquals(Integer.MAX_VALUE, tray.getNotificationsCount());

        // Ensure that the notification list is displaying the correct amount of unread notifications
        // (max 10 notifications by default)
        assertEquals(10, tray.getUnreadNotificationsCount());
        assertEquals(0, tray.getReadNotificationsCount());
        tray.markAsRead(0);
        assertEquals(9, tray.getUnreadNotificationsCount());
        assertEquals(1, tray.getReadNotificationsCount());

        // Make sure it's still OK after a refresh (change the page so we are sure it refreshes)
        testUtils.gotoPage("Main", "WebHome");
        tray = new NotificationsTrayPage();
        assertEquals(20, tray.getNotificationsCount());
        assertEquals(9, tray.getUnreadNotificationsCount());
        assertEquals(1, tray.getReadNotificationsCount());

        // Ensure that a notification has a correct type
        assertEquals("create", tray.getNotificationType(0));

        // Reset the notifications count of the user 2
        tray.clearAllNotifications();
        assertEquals(0, tray.getNotificationsCount());
        assertFalse(tray.areNotificationsAvailable());

        // The user 2 will get notifications only for pages deletions
        p = NotificationsUserProfilePage.gotoPage(SECOND_USER_NAME);
        p.getApplication(SYSTEM).setCollapsed(false);
        p.setEventTypeState(SYSTEM, CREATE, ALERT_FORMAT, BootstrapSwitch.State.OFF);
        p.setEventTypeState(SYSTEM, DELETE, ALERT_FORMAT, BootstrapSwitch.State.ON);

        // Yes we wait on a timer, but it is to be sure the following events will be stored AFTER the settings have been
        // changed.
        Thread.sleep(1000);

        // Delete the "Deletion test page" and test the notification
        testUtils.login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        testUtils.deletePage(testReference.getLastSpaceReference().getName(), "DTP");

        testUtils.login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        testUtils.gotoPage(testReference.getLastSpaceReference().getName(), "WebHome");
        tray = new NotificationsTrayPage();
        assertEquals(1, tray.getNotificationsCount());
    }

    @Test
    @Order(2)
    public void compositeNotifications(TestUtils testUtils, TestReference testReference,
        TestConfiguration testConfiguration) throws Exception
    {
        NotificationsUserProfilePage p;
        NotificationsTrayPage tray;
        // Now we enable "create", "update" and "comment" for user 2
        testUtils.login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        p = NotificationsUserProfilePage.gotoPage(SECOND_USER_NAME);
        p.getApplication(SYSTEM).setCollapsed(false);
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, CREATE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, UPDATE, ALERT_FORMAT));
        p.setEventTypeState(SYSTEM, UPDATE, ALERT_FORMAT, BootstrapSwitch.State.ON);

        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, ADD_COMMENT, ALERT_FORMAT));
        p.setEventTypeState(SYSTEM, ADD_COMMENT, ALERT_FORMAT, BootstrapSwitch.State.ON);
        testUtils.gotoPage("Main", "WebHome");
        tray = new NotificationsTrayPage();
        tray.clearAllNotifications();

        // Create a page, edit it twice, and finally add a comment
        testUtils.login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        testUtils.createPage(testReference.getLastSpaceReference().getName(), 
            "Linux", "Simple content", "Linux as a title");
        ViewPage page = testUtils.gotoPage(testReference.getLastSpaceReference().getName(), "Linux");
        page.edit();
        WikiEditPage edit = new WikiEditPage();
        edit.setContent("Linux is a part of GNU/Linux");
        edit.clickSaveAndView(true);
        page = new ViewPage();
        page.edit();
        edit = new WikiEditPage();
        edit.setContent("Linux is a part of GNU/Linux - it's the kernel");
        edit.clickSaveAndView(true);
        page = testUtils.gotoPage(testReference.getLastSpaceReference().getName(), "Linux");
        CommentsTab commentsTab = page.openCommentsDocExtraPane();
        commentsTab.postComment("Linux is a great OS", true);

        // Check that events have been grouped together (see: https://jira.xwiki.org/browse/XWIKI-14114)
        testUtils.login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        testUtils.gotoPage(testReference.getLastSpaceReference().getName(), "WebHome");
        tray = new NotificationsTrayPage();
        assertEquals(2, tray.getNotificationsCount());
        assertEquals("Linux as a title", tray.getNotificationPage(0));
        String expectedComment = String.format("commented by %s", FIRST_USER_NAME);
        String obtainedComment = tray.getNotificationDescription(0);
        assertTrue(obtainedComment.startsWith(expectedComment), String.format("Expected description start: [%s]. "
            + "Actual description: [%s]", expectedComment, obtainedComment));
        assertEquals("Linux as a title", tray.getNotificationPage(1));
        assertEquals("update", tray.getNotificationType(1));
        expectedComment = String.format("edited by %s", FIRST_USER_NAME);
        obtainedComment = tray.getNotificationDescription(1);
        assertTrue(obtainedComment.startsWith(expectedComment), String.format("Expected description start: [%s]. "
            + "Actual description: [%s]", expectedComment, obtainedComment));

        NotificationsRSS notificationsRSS = tray.getNotificationRSS(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        ServletEngine servletEngine = testConfiguration.getServletEngine();
            notificationsRSS.loadEntries(
                String.format("%s:%s", servletEngine.getInternalIP(), servletEngine.getInternalPort()),
                String.format("%s:%s", servletEngine.getIP(), servletEngine.getPort()));
        assertEquals(2, notificationsRSS.getEntries().size());
        assertEquals("A comment has been added to the page \"Linux as a title\"",
                notificationsRSS.getEntries().get(0).getTitle());
        assertTrue(notificationsRSS.getEntries().get(0).getDescription().getValue().contains(
                "<strong>Pages: [addComment]</strong>"));
        assertEquals("The page \"Linux as a title\" has been modified",
                notificationsRSS.getEntries().get(1).getTitle());

        tray.clearAllNotifications();
    }

    @Test
    @Order(3)
    public void notificationDisplayerClass(TestUtils testUtils, TestReference testReference) throws Exception
    {
        try {
            // Create the pages and a custom displayer for "update" events
            testUtils.loginAsSuperAdmin();

            testUtils.gotoPage(testReference.getLastSpaceReference().getName(), "WebHome");
            testUtils.createPage(testReference.getLastSpaceReference().getName(), "ARandomPageThatShouldBeModified",
                "Page used for the tests of the NotificationDisplayerClass XObject.", "Test page");

            testUtils.createPage(testReference.getLastSpaceReference().getName(), "NotificationDisplayerClassTest",
                "Page used for the tests of the NotificationDisplayerClass XObject.", "Test page 2");

            Map<String, String> notificationDisplayerParameters = new HashMap<String, String>()
            {{
                put("XWiki.Notifications.Code.NotificationDisplayerClass_0_eventType", "update");
                put("XWiki.Notifications.Code.NotificationDisplayerClass_0_notificationTemplate",
                    "This is a test template");
            }};

            ObjectEditPage editObjects = testUtils.editObjects(testReference.getLastSpaceReference().getName(),
                "NotificationDisplayerClassTest");
            editObjects.addObject("XWiki.Notifications.Code.NotificationDisplayerClass");
            editObjects.getObjectsOfClass("XWiki.Notifications.Code.NotificationDisplayerClass")
                .get(0).fillFieldsByName(notificationDisplayerParameters);
            editObjects.clickSaveAndContinue(true);

            // Login as first user, and enable notifications on document updates
            testUtils.login(FIRST_USER_NAME, FIRST_USER_PASSWORD);

            NotificationsUserProfilePage p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
            p.getApplication(SYSTEM).setCollapsed(false);
            p.setEventTypeState(SYSTEM, UPDATE, ALERT_FORMAT, BootstrapSwitch.State.ON);

            // Login as second user and modify ARandomPageThatShouldBeModified
            testUtils.login(SECOND_USER_NAME, SECOND_USER_PASSWORD);

            ViewPage viewPage =
                testUtils.gotoPage(testReference.getLastSpaceReference().getName(), "ARandomPageThatShouldBeModified");
            viewPage.edit();
            WikiEditPage editPage = new WikiEditPage();
            editPage.setContent("Something");
            editPage.clickSaveAndView(true);

            // Login as the first user, ensure that the notification is displayed with a custom template
            testUtils.login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
            testUtils.gotoPage(testReference.getLastSpaceReference().getName(), "WebHome");

            NotificationsTrayPage tray = new NotificationsTrayPage();
            assertEquals("This is a test template", tray.getNotificationRawContent(0));
        } finally {
            testUtils.loginAsSuperAdmin();
            testUtils.deletePage(testReference.getLastSpaceReference().getName(), "NotificationDisplayerClassTest");
            testUtils.deletePage(testReference.getLastSpaceReference().getName(), "ARandomPageThatShouldBeModified");
        }
    }


    @Test
    @Order(4)
    public void ownEventNotifications(TestUtils testUtils, TestReference testReference) throws Exception
    {
        testUtils.login(FIRST_USER_NAME, FIRST_USER_PASSWORD);

        DocumentReference page2 = new DocumentReference("page2", testReference.getLastSpaceReference());
        try {
            NotificationsUserProfilePage p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
            List<NotificationFilterPreference> preferences = p.getNotificationFilterPreferences();

            // Now let's do some changes (own even filter)
            p.setApplicationState(SYSTEM, "alert", BootstrapSwitch.State.ON);
            assertEquals("Own Events Filter", preferences.get(2).getFilterName());
            preferences.get(2).setEnabled(false);
            testUtils.createPage(testReference, "", "");
            // Refresh that page
            testUtils.gotoPage(testReference);
            NotificationsTrayPage notificationsTrayPage = new NotificationsTrayPage();
            assertEquals(1, notificationsTrayPage.getNotificationsCount());
            assertEquals(String.format("created by %s\n" + "moments ago", FIRST_USER_NAME),
                    notificationsTrayPage.getNotificationDescription(0));
            assertEquals(testReference.getLastSpaceReference().getName(), notificationsTrayPage.getNotificationPage(0));

            // Go back to enable the own even filter
            p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
            preferences = p.getNotificationFilterPreferences();
            assertEquals("Own Events Filter", preferences.get(2).getFilterName());
            assertFalse(preferences.get(2).isEnabled());
            preferences.get(2).setEnabled(true);
            testUtils.createPage(page2, "", "Page 2");
            testUtils.gotoPage(testReference.getLastSpaceReference().getName(), testReference.getName());
            notificationsTrayPage = new NotificationsTrayPage();

            // Ensure we only have the previous notification.
            assertEquals(1, notificationsTrayPage.getNotificationsCount());
            assertEquals(String.format("created by %s\n" + "moments ago", FIRST_USER_NAME),
                notificationsTrayPage.getNotificationDescription(0));
            assertEquals(testReference.getLastSpaceReference().getName(), notificationsTrayPage.getNotificationPage(0));
        } finally {
            // Clean up
            testUtils.rest().deletePage(testReference.getLastSpaceReference().getName(), testReference.getName());
            testUtils.rest().deletePage(testReference.getLastSpaceReference().getName(), "page2");
        }
    }

    @Test
    @Order(5)
    public void guestUsersDontSeeNotificationPanel(TestUtils testUtils)
    {
        testUtils.login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        testUtils.gotoPage("Main", "WebHome");
        assertTrue(new NotificationsTrayPage().isNotificationMenuVisible());
        testUtils.forceGuestUser();
        assertFalse(new NotificationsTrayPage().isNotificationMenuVisible());
    }
}
