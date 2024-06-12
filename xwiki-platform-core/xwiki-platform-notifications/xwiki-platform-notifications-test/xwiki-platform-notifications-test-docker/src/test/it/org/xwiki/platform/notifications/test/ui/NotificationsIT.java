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

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.platform.notifications.test.po.GroupedNotificationElementPage;
import org.xwiki.platform.notifications.test.po.NotificationsRSS;
import org.xwiki.platform.notifications.test.po.NotificationsTrayPage;
import org.xwiki.platform.notifications.test.po.NotificationsUserProfilePage;
import org.xwiki.platform.notifications.test.po.preferences.filters.SystemNotificationFilterPreference;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.servletengine.ServletEngine;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.BootstrapSwitch;
import org.xwiki.test.ui.po.CommentsTab;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.ObjectEditPage;
import org.xwiki.test.ui.po.editor.ObjectEditPane;
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
        // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus,
        // we need to provide the JAR inside WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-19932
        "org.xwiki.platform:xwiki-platform-notifications-filters-default",
        // The Solr store is not ready yet to be installed as an extension, so we need to add it to WEB-INF/lib
        // manually. See https://jira.xwiki.org/browse/XWIKI-21594
        "org.xwiki.platform:xwiki-platform-eventstream-store-solr"
    }
)
class NotificationsIT
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
    public void setup(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();
        // Create the two users we will be using
        setup.createUser(FIRST_USER_NAME, FIRST_USER_PASSWORD, "", "");
        setup.createUser(SECOND_USER_NAME, SECOND_USER_PASSWORD, "", "");

        NotificationsUserProfilePage p;

        setup.login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
        // Make sure to wait until notifications are empty (in case of leftovers bing cleaned from a previous test)
        NotificationsTrayPage.waitOnNotificationCount("xwiki:XWiki." + FIRST_USER_NAME, "xwiki", 0);
        // The page should have been refreshed but we want to be sure to not have stale elements.
        p = new NotificationsUserProfilePage();
        p.disableAllParameters();
        // Enable own filter
        p.getSystemNotificationFilterPreferences().get(2).setEnabled(true);

        setup.login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        NotificationsUserProfilePage.gotoPage(SECOND_USER_NAME);
        // Make sure to wait until notifications are empty (in case of leftovers bing cleaned from a previous test)
        NotificationsTrayPage.waitOnNotificationCount("xwiki:XWiki." + SECOND_USER_NAME, "xwiki", 0);
        // The page should have been refreshed but we want to be sure to not have stale elements.
        p = new NotificationsUserProfilePage();
        p.disableAllParameters();
    }

    @AfterEach
    public void tearDown(TestUtils setup)
    {
        setup.deletePage("XWiki", FIRST_USER_NAME);
        setup.deletePage("XWiki", SECOND_USER_NAME);
        setup.forceGuestUser();
    }

    @Test
    @Order(1)
    void simpleNotifications(TestUtils setup, TestReference testReference) throws Exception
    {
        NotificationsUserProfilePage p;
        NotificationsTrayPage tray;

        // The user 1 creates a new page, the user 2 shouldn’t receive any notification
        setup.login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        String space = testReference.getLastSpaceReference().getName();
        setup.createPage(space, "WebHome", "Content from " + FIRST_USER_NAME, "Page title");

        setup.login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        setup.gotoPage(space, "WebHome");

        tray = new NotificationsTrayPage();
        assertFalse(tray.areNotificationsAvailable());

        // The user 2 will now enable his notifications for new pages
        p = NotificationsUserProfilePage.gotoPage(SECOND_USER_NAME);
        p.getApplication(SYSTEM).setCollapsed(false);
        p.setEventTypeState(SYSTEM, CREATE, ALERT_FORMAT, BootstrapSwitch.State.ON);

        tray.showNotificationTray();
        assertFalse(tray.isPageOnlyWatched());
        assertFalse(tray.arePageAndChildrenWatched());
        assertFalse(tray.isWikiWatched());
        // And he will watch the entire wiki.
        tray.setWikiWatchedState(true);

        // We create a lot of pages in order to test the notification badge
        setup.login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        for (int i = 1; i < PAGES_TOP_CREATION_COUNT; i++) {
            setup.deletePage(space, "Page" + i);
            setup.createPage(space, "Page" + i, "Simple content", "Simple title");
        }
        setup.createPage(space, "DTP", "Deletion test page", "Deletion test content");

        // Check that the badge is showing «20+»
        setup.login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        setup.gotoPage(space, "WebHome");
        NotificationsTrayPage.waitOnNotificationCount("xwiki:XWiki." + SECOND_USER_NAME, "xwiki",
            PAGES_TOP_CREATION_COUNT);
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
        setup.gotoPage("Main", "WebHome");
        // Marking the notification as read is done async, so we need to wait to be sure it has been taken into account.
        NotificationsTrayPage.waitOnNotificationCount("xwiki:XWiki." + SECOND_USER_NAME, "xwiki", 20);
        tray = new NotificationsTrayPage();
        assertEquals(20, tray.getNotificationsCount());
        assertEquals(9, tray.getUnreadNotificationsCount());
        assertEquals(1, tray.getReadNotificationsCount());

        // Ensure that a notification has a correct type
        assertEquals("create", tray.getNotificationType(0));

        // Reset the notifications count of the user 2
        tray.clearAllNotifications();
        // Clearing the notifications is done async, so we need to wait to be sure it has been taken into account.
        NotificationsTrayPage.waitOnNotificationCount("xwiki:XWiki." + SECOND_USER_NAME, "xwiki", 0);
        tray = new NotificationsTrayPage();
        assertEquals(0, tray.getNotificationsCount());
        assertFalse(tray.areNotificationsAvailable());

        // The user 2 will get notifications only for pages deletions
        p = NotificationsUserProfilePage.gotoPage(SECOND_USER_NAME);
        p.getApplication(SYSTEM).setCollapsed(false);
        p.setEventTypeState(SYSTEM, CREATE, ALERT_FORMAT, BootstrapSwitch.State.OFF);
        p.setEventTypeState(SYSTEM, DELETE, ALERT_FORMAT, BootstrapSwitch.State.ON);

        // Delete the "Deletion test page" and test the notification
        setup.login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        setup.deletePage(space, "DTP");

        setup.login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        setup.gotoPage(space, "WebHome");
        // Ensure the notification has been received.
        NotificationsTrayPage.waitOnNotificationCount("xwiki:XWiki." + SECOND_USER_NAME, "xwiki", 1);
        tray = new NotificationsTrayPage();
        assertEquals(1, tray.getNotificationsCount());
    }

    @Test
    @Order(2)
    void compositeNotifications(TestUtils setup, TestReference testReference,
        TestConfiguration testConfiguration) throws Exception
    {
        NotificationsUserProfilePage p;
        NotificationsTrayPage tray;
        // We enable "create", "update" and "comment" for user 2
        setup.login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        p = NotificationsUserProfilePage.gotoPage(SECOND_USER_NAME);
        p.getApplication(SYSTEM).setCollapsed(false);
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, CREATE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, UPDATE, ALERT_FORMAT));
        p.setEventTypeState(SYSTEM, UPDATE, ALERT_FORMAT, BootstrapSwitch.State.ON);

        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, ADD_COMMENT, ALERT_FORMAT));
        p.setEventTypeState(SYSTEM, ADD_COMMENT, ALERT_FORMAT, BootstrapSwitch.State.ON);

        List<SystemNotificationFilterPreference> minorEvent = p.getSystemNotificationFilterPreferences()
            .stream()
            .filter(fp -> fp.getName().equals("Minor Event (Alert)"))
            .toList();

        assertEquals(1, minorEvent.size());
        minorEvent.get(0).setEnabled(false);
        setup.gotoPage("Main", "WebHome");
        tray = new NotificationsTrayPage();
        tray.showNotificationTray();
        tray.clearAllNotifications();
        // Watch the entire wiki so that we receive notifications
        tray.setWikiWatchedState(true);

        // Create a page, edit it 20 times, and finally add a comment
        setup.login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        ViewPage page = setup.createPage(testReference, "Simple content", "Linux as a title");
        page.edit();
        WikiEditPage edit = new WikiEditPage();
        StringBuilder originalContent = new StringBuilder("Linux is a part of GNU/Linux - it's the kernel");
        edit.setContent(originalContent.toString());
        page = edit.clickSaveAndView();
        page.edit();
        edit = new WikiEditPage();

        for (int i = 0; i < 20; i++) {
            String newContent = String.format("\nAdding some content iteration %s", i);
            originalContent.append(newContent);
            edit.setContent(originalContent.toString());
            edit.clickSaveAndContinue();
        }
        page = edit.clickSaveAndView();
        CommentsTab commentsTab = page.openCommentsDocExtraPane();
        commentsTab.postComment("Linux is a great OS", true);

        // Check that events have been grouped together (see: https://jira.xwiki.org/browse/XWIKI-14114)
        setup.login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        setup.gotoPage("Main", "WebHome");
        NotificationsTrayPage.waitOnNotificationCount("xwiki:XWiki." + SECOND_USER_NAME, "xwiki", 2);
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
        GroupedNotificationElementPage groupedNotificationsPage = tray.getGroupedNotificationsPage();
        groupedNotificationsPage.openGroup(1);
        assertEquals(22, groupedNotificationsPage.getNumberOfElements(1));

        NotificationsRSS notificationsRSS = tray.getNotificationRSS(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        ServletEngine servletEngine = testConfiguration.getServletEngine();
        notificationsRSS.loadEntries(
            String.format("%s:%s", servletEngine.getInternalIP(), servletEngine.getInternalPort()),
            String.format("%s:%s", servletEngine.getIP(), servletEngine.getPort()));
        assertEquals(2, notificationsRSS.getEntries().size());

        // FIXME: This needs to be enabled back once XWIKI-21059 is fixed.
        //assertEquals("A comment has been added to the page \"Linux as a title\"",
        //        notificationsRSS.getEntries().get(0).getTitle());
        //assertTrue(notificationsRSS.getEntries().get(0).getDescription().getValue().contains(
        //        "<strong>Pages: [addComment]</strong>"));
        //assertEquals("The page \"Linux as a title\" has been modified",
        //        notificationsRSS.getEntries().get(1).getTitle());

        tray.clearAllNotifications();
    }

    @Test
    @Order(3)
    void notificationDisplayerClass(TestUtils setup, TestReference testReference) throws Exception
    {
        try {
            // Create the pages and a custom displayer for "update" events
            setup.loginAsSuperAdmin();

            setup.gotoPage(testReference.getLastSpaceReference().getName(), "WebHome");
            setup.createPage(testReference.getLastSpaceReference().getName(), "ARandomPageThatShouldBeModified",
                "Page used for the tests of the NotificationDisplayerClass XObject.", "Test page");

            setup.createPage(testReference.getLastSpaceReference().getName(), "NotificationDisplayerClassTest",
                "Page used for the tests of the NotificationDisplayerClass XObject.", "Test page 2");

            Map<String, String> notificationDisplayerParameters = Map.of(
                "XWiki.Notifications.Code.NotificationDisplayerClass_0_eventType", "update",
                "XWiki.Notifications.Code.NotificationDisplayerClass_0_notificationTemplate",
                    "This is a test template"
            );

            ObjectEditPage editObjects = setup.editObjects(testReference.getLastSpaceReference().getName(),
                "NotificationDisplayerClassTest");
            editObjects.addObject("XWiki.Notifications.Code.NotificationDisplayerClass");
            ObjectEditPane objectEditPane =
                editObjects.getObjectsOfClass("XWiki.Notifications.Code.NotificationDisplayerClass").get(0);
            objectEditPane.displayObject();
            objectEditPane.fillFieldsByName(notificationDisplayerParameters);
            editObjects.clickSaveAndContinue(true);

            // Login as first user, and enable notifications on document updates
            setup.login(FIRST_USER_NAME, FIRST_USER_PASSWORD);

            NotificationsUserProfilePage p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
            p.getApplication(SYSTEM).setCollapsed(false);
            p.setEventTypeState(SYSTEM, UPDATE, ALERT_FORMAT, BootstrapSwitch.State.ON);

            // Watch the entire wiki so that we receive notifications
            NotificationsTrayPage tray = new NotificationsTrayPage();
            tray.showNotificationTray();
            tray.setWikiWatchedState(true);

            // Login as second user and modify ARandomPageThatShouldBeModified
            setup.login(SECOND_USER_NAME, SECOND_USER_PASSWORD);

            ViewPage viewPage =
                setup.gotoPage(testReference.getLastSpaceReference().getName(), "ARandomPageThatShouldBeModified");
            viewPage.edit();
            WikiEditPage editPage = new WikiEditPage();
            editPage.setContent("Something");
            editPage.clickSaveAndView(true);

            // Login as the first user, ensure that the notification is displayed with a custom template
            setup.login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
            setup.gotoPage(testReference.getLastSpaceReference().getName(), "WebHome");

            // Ensure the notification has been received.
            NotificationsTrayPage.waitOnNotificationCount("xwiki:XWiki." + FIRST_USER_NAME, "xwiki", 1);
            tray = new NotificationsTrayPage();
            assertEquals("This is a test template", tray.getNotificationRawContent(0));
        } finally {
            setup.loginAsSuperAdmin();
            setup.deletePage(testReference.getLastSpaceReference().getName(), "NotificationDisplayerClassTest");
            setup.deletePage(testReference.getLastSpaceReference().getName(), "ARandomPageThatShouldBeModified");
        }
    }

    @Test
    @Order(4)
    void ownEventNotifications(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.login(FIRST_USER_NAME, FIRST_USER_PASSWORD);

        DocumentReference page2 = new DocumentReference("page2", testReference.getLastSpaceReference());
        try {
            int filterPreferenceNumber = 4;
            NotificationsUserProfilePage p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
            List<SystemNotificationFilterPreference> preferences = p.getSystemNotificationFilterPreferences();

            // Now let's do some changes (own even filter)
            SystemNotificationFilterPreference filterPreference = preferences.get(filterPreferenceNumber);
            p.setApplicationState(SYSTEM, "alert", BootstrapSwitch.State.ON);
            assertEquals("Own Events Filter", filterPreference.getName());
            filterPreference.setEnabled(false);
            setup.gotoPage(page2);
            p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
            assertFalse(p.getSystemNotificationFilterPreferences().get(filterPreferenceNumber).isEnabled());

            // Watch the entire wiki so that we receive notifications
            NotificationsTrayPage tray = new NotificationsTrayPage();
            tray.showNotificationTray();
            tray.setWikiWatchedState(true);

            setup.createPage(testReference, "", "");
            // Ensure the notification has been received.
            NotificationsTrayPage.waitOnNotificationCount("xwiki:XWiki." + FIRST_USER_NAME, "xwiki", 1);
            NotificationsTrayPage notificationsTrayPage = new NotificationsTrayPage();
            assertEquals(1, notificationsTrayPage.getNotificationsCount());
            assertEquals(String.format("created by %s\n" + "moments ago", FIRST_USER_NAME),
                notificationsTrayPage.getNotificationDescription(0));
            assertEquals(testReference.getLastSpaceReference().getName(), notificationsTrayPage.getNotificationPage(0));

            // Go back to enable the own even filter
            p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
            preferences = p.getSystemNotificationFilterPreferences();
            filterPreference = preferences.get(filterPreferenceNumber);
            assertEquals("Own Events Filter", filterPreference.getName());
            assertFalse(filterPreference.isEnabled());
            filterPreference.setEnabled(true);
            setup.createPage(page2, "", "Page 2");
            setup.gotoPage(testReference.getLastSpaceReference().getName(), testReference.getName());
            notificationsTrayPage = new NotificationsTrayPage();

            // Ensure we only have the previous notification.
            assertEquals(1, notificationsTrayPage.getNotificationsCount());
            assertEquals(String.format("created by %s\n" + "moments ago", FIRST_USER_NAME),
                notificationsTrayPage.getNotificationDescription(0));
            assertEquals(testReference.getLastSpaceReference().getName(), notificationsTrayPage.getNotificationPage(0));
        } finally {
            // Clean up
            setup.rest().delete(testReference);
            setup.rest().delete(page2);
        }
    }

    @Test
    @Order(5)
    void guestUsersDontSeeNotificationMenu(TestUtils setup)
    {
        setup.login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        // Move to any page in view mode so that the notification menu is visible. Note that we use a non-existing page
        // for improved test performance.
        setup.gotoPage("NotExistingSpace", "NotExistingPage");
        assertTrue(new NotificationsTrayPage().isNotificationMenuVisible());
        setup.forceGuestUser();
        assertFalse(new NotificationsTrayPage().isNotificationMenuVisible());
    }
}
