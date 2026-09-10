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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.platform.notifications.test.po.GroupedNotificationElementPage;
import org.xwiki.platform.notifications.test.po.NotificationWatchButtonElement;
import org.xwiki.platform.notifications.test.po.NotificationsContainerElement;
import org.xwiki.platform.notifications.test.po.NotificationsRSS;
import org.xwiki.platform.notifications.test.po.NotificationsTrayPage;
import org.xwiki.platform.notifications.test.po.NotificationsUserProfilePage;
import org.xwiki.platform.notifications.test.po.NotificationsWatchModal;
import org.xwiki.platform.notifications.test.po.preferences.filters.SystemNotificationFilterPreference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.WikisSource;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.BootstrapSwitch;
import org.xwiki.test.ui.po.CommentsTab;

import com.rometools.rome.feed.synd.SyndEntry;

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

    // Number of times the page of compositeNotifications is updated before the comment is added. The comment itself
    // updates the page, so the update composite event holds one more event than that.
    private static final int PAGE_UPDATE_COUNT = 21;

    private static final String SYSTEM = "org.xwiki.platform";

    private static final String ALERT_FORMAT = "alert";

    private static final String ADD_COMMENT = "addComment";

    private static final String CREATE = "create";

    private static final String DELETE = "delete";

    private static final String UPDATE = "update";

    @BeforeEach
    public void setup(TestUtils setup) throws Exception
    {
        // The REST client acts as superadmin, which is allowed to create the users, without the browser having to
        // log in as superadmin.
        setup.setDefaultCredentials(TestUtils.SUPER_ADMIN_CREDENTIALS);
        // Create the two users we will be using
        setup.rest().createUser(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        setup.rest().createUser(SECOND_USER_NAME, SECOND_USER_PASSWORD);

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
    public void tearDown(TestUtils setup) throws Exception
    {
        setup.setDefaultCredentials(TestUtils.SUPER_ADMIN_CREDENTIALS);
        setup.rest().deletePage("XWiki", FIRST_USER_NAME);
        setup.rest().deletePage("XWiki", SECOND_USER_NAME);
        setup.forceGuestUser();
    }

    @Test
    @Order(1)
    void simpleNotifications(TestUtils setup, TestReference testReference) throws Exception
    {
        NotificationsUserProfilePage p;
        NotificationsTrayPage tray;

        // The user 1 creates a new page, the user 2 shouldn’t receive any notification. Only the user 2 logs in:
        // the pages of the user 1 are created over REST, since the subject of this test is the notification tray of
        // the user 2, not the page creation itself.
        String space = testReference.getLastSpaceReference().getName();
        setup.rest().runAs(FIRST_USER_NAME, FIRST_USER_PASSWORD,
            rest -> rest.savePage(new LocalDocumentReference(space, "WebHome"),
                "Content from " + FIRST_USER_NAME, "Page title"));

        setup.login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        setup.gotoPage(space, "WebHome");

        tray = new NotificationsTrayPage();
        assertFalse(tray.areNotificationsAvailable());

        // The user 2 will now enable his notifications for new pages
        p = NotificationsUserProfilePage.gotoPage(SECOND_USER_NAME);
        p.getApplication(SYSTEM).setCollapsed(false);
        p.setEventTypeState(SYSTEM, CREATE, ALERT_FORMAT, BootstrapSwitch.State.ON);

        NotificationWatchButtonElement watchButtonElement = new NotificationWatchButtonElement();
        assertTrue(watchButtonElement.isNotSet());
        // And he will watch the entire wiki.
        NotificationsWatchModal notificationsWatchModal = watchButtonElement.openModal();
        assertEquals(List.of(
            NotificationsWatchModal.WatchOptions.WATCH_PAGE,
            NotificationsWatchModal.WatchOptions.WATCH_WIKI
        ), notificationsWatchModal.getAvailableOptions());
        notificationsWatchModal.selectOptionAndSave(NotificationsWatchModal.WatchOptions.WATCH_WIKI);

        // We create a lot of pages in order to test the notification badge
        setup.rest().runAs(FIRST_USER_NAME, FIRST_USER_PASSWORD, rest -> {
            for (int i = 1; i < PAGES_TOP_CREATION_COUNT; i++) {
                LocalDocumentReference page = new LocalDocumentReference(space, "Page" + i);
                // Make sure the page is created, and not updated, so that a "create" event is sent.
                rest.delete(page);
                rest.savePage(page, "Simple content", "Simple title");
            }
            rest.savePage(new LocalDocumentReference(space, "DTP"), "Deletion test page", "Deletion test content");
        });

        // Check that the badge is showing «20+»
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
        setup.rest().runAs(FIRST_USER_NAME, FIRST_USER_PASSWORD,
            rest -> rest.delete(new LocalDocumentReference(space, "DTP")));

        setup.gotoPage(space, "WebHome");
        // Ensure the notification has been received.
        NotificationsTrayPage.waitOnNotificationCount("xwiki:XWiki." + SECOND_USER_NAME, "xwiki", 1);
        tray = new NotificationsTrayPage();
        assertEquals(1, tray.getNotificationsCount());
    }

    @Test
    @Order(2)
    void compositeNotifications(TestUtils setup, TestReference testReference) throws Exception
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
        NotificationWatchButtonElement watchButtonElement = new NotificationWatchButtonElement();
        assertTrue(watchButtonElement.isNotSet());
        // And he will watch the entire wiki.
        NotificationsWatchModal notificationsWatchModal = watchButtonElement.openModal();
        assertEquals(List.of(
            NotificationsWatchModal.WatchOptions.WATCH_PAGE,
            NotificationsWatchModal.WatchOptions.WATCH_SPACE,
            NotificationsWatchModal.WatchOptions.WATCH_WIKI
        ), notificationsWatchModal.getAvailableOptions());
        notificationsWatchModal.selectOptionAndSave(NotificationsWatchModal.WatchOptions.WATCH_WIKI);

        tray = new NotificationsTrayPage();
        tray.showNotificationTray();
        tray.clearAllNotifications();


        // Create a page, edit it several times, and finally add a comment. What this test is about is how the
        // resulting events are grouped in the tray of the user 2, so the user 1 produces them over REST rather than
        // through the editor.
        setup.rest().runAs(FIRST_USER_NAME, FIRST_USER_PASSWORD, rest -> {
            StringBuilder content = new StringBuilder("Linux is a part of GNU/Linux - it's the kernel");
            rest.savePage(testReference, content.toString(), "Linux as a title");

            for (int i = 0; i < PAGE_UPDATE_COUNT; i++) {
                content.append(String.format("%nAdding some content iteration %s", i));
                rest.savePage(testReference, content.toString(), "Linux as a title");
            }
            CommentsTab.restPostComment(setup, testReference, "Linux is a great OS");
        });

        // Check that events have been grouped together (see: https://jira.xwiki.org/browse/XWIKI-14114)
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
        assertEquals(PAGE_UPDATE_COUNT + 1, groupedNotificationsPage.getNumberOfElements(1));

        NotificationsRSS notificationsRSS = tray.getNotificationRSS(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        notificationsRSS.loadEntries(setup);
        assertEquals(2, notificationsRSS.getEntries().size());

        // Both RSS feeds must be served with a feed media type so that browsers and feed readers recognize them as
        // feeds instead of rendering the XML as HTML (see XWIKI-24543). The menu feed is served by the
        // NotificationRSSService wiki page (application/xml) while the macro feed is served by the /notifications/rss
        // REST endpoint (application/rss+xml).
        assertTrue(notificationsRSS.getContentType().startsWith("application/xml"),
            "Menu RSS feed Content-Type was: " + notificationsRSS.getContentType());

        String macroRSSURL = String.format(
            "%srest/notifications/rss?userId=%s&useUserPreferences=false&count=10&displayOwnEvents=true",
            setup.getBaseURL(), URLEncoder.encode("xwiki:XWiki." + SECOND_USER_NAME, StandardCharsets.UTF_8));
        NotificationsRSS macroRSS = new NotificationsRSS(macroRSSURL, SECOND_USER_NAME, SECOND_USER_PASSWORD);
        macroRSS.loadEntries(setup);
        assertTrue(macroRSS.getContentType().startsWith("application/rss+xml"),
            "Macro (REST) RSS feed Content-Type was: " + macroRSS.getContentType());

        // The comment event and the page-update composite event can share the same timestamp (posting a
        // comment also updates the page), so the RSS feed may return them in either order (see XWIKI-21059).
        // Match the entries by title rather than by position.
        SyndEntry commentEntry = getEntryByTitle(notificationsRSS,
            "A comment has been added to the page \"Linux as a title\"");
        getEntryByTitle(notificationsRSS, "The page \"Linux as a title\" has been modified");
        String descriptionValue = commentEntry.getDescription().getValue();
        assertTrue(descriptionValue.contains("<strong>Pages</strong>"), "Value was: " + descriptionValue);
        assertTrue(descriptionValue.contains("Linux as a title"), "Value was: " + descriptionValue);
        assertTrue(descriptionValue.contains("edited by " + FIRST_USER_NAME), "Value was: " + descriptionValue);

        tray.clearAllNotifications();
    }

    @Test
    @Order(3)
    void notificationDisplayerClass(TestUtils setup, TestReference testReference) throws Exception
    {
        String space = testReference.getLastSpaceReference().getName();
        LocalDocumentReference modifiedPage = new LocalDocumentReference(space, "ARandomPageThatShouldBeModified");
        LocalDocumentReference displayerPage = new LocalDocumentReference(space, "NotificationDisplayerClassTest");
        try {
            // Create the pages and a custom displayer for "update" events. None of this is asserted by the test, so
            // it is done over REST as superadmin rather than by logging the browser in as superadmin and going
            // through the object editor.
            setup.setDefaultCredentials(TestUtils.SUPER_ADMIN_CREDENTIALS);
            setup.rest().savePage(modifiedPage,
                "Page used for the tests of the NotificationDisplayerClass XObject.", "Test page");
            setup.rest().savePage(displayerPage,
                "Page used for the tests of the NotificationDisplayerClass XObject.", "Test page 2");
            setup.rest().addObject(displayerPage, "XWiki.Notifications.Code.NotificationDisplayerClass",
                "eventType", "update",
                "notificationTemplate", "This is a test template");

            // Login as first user, and enable notifications on document updates
            setup.login(FIRST_USER_NAME, FIRST_USER_PASSWORD);

            NotificationsUserProfilePage p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
            p.getApplication(SYSTEM).setCollapsed(false);
            p.setEventTypeState(SYSTEM, UPDATE, ALERT_FORMAT, BootstrapSwitch.State.ON);

            // Watch the entire wiki so that we receive notifications
            NotificationWatchButtonElement watchButtonElement = new NotificationWatchButtonElement();
            assertTrue(watchButtonElement.isNotSet());
            // And he will watch the entire wiki.
            NotificationsWatchModal notificationsWatchModal = watchButtonElement.openModal();
            assertEquals(List.of(
                NotificationsWatchModal.WatchOptions.WATCH_PAGE,
                NotificationsWatchModal.WatchOptions.WATCH_WIKI
            ), notificationsWatchModal.getAvailableOptions());
            notificationsWatchModal.selectOptionAndSave(NotificationsWatchModal.WatchOptions.WATCH_WIKI);

            // Modify ARandomPageThatShouldBeModified as the second user over REST, so that the browser stays
            // logged in as the first user, whose notification tray is the subject of this test.
            setup.rest().runAs(SECOND_USER_NAME, SECOND_USER_PASSWORD,
                rest -> rest.savePage(modifiedPage, "Something", "Test page"));

            // Ensure that the notification is displayed with a custom template
            setup.gotoPage(space, "WebHome");

            // Ensure the notification has been received.
            NotificationsTrayPage.waitOnNotificationCount("xwiki:XWiki." + FIRST_USER_NAME, "xwiki", 1);
            NotificationsTrayPage tray = new NotificationsTrayPage();
            assertEquals("This is a test template", tray.getNotificationRawContent(0));
        } finally {
            setup.setDefaultCredentials(TestUtils.SUPER_ADMIN_CREDENTIALS);
            setup.rest().delete(displayerPage);
            setup.rest().delete(modifiedPage);
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
            NotificationWatchButtonElement watchButtonElement = new NotificationWatchButtonElement();
            assertTrue(watchButtonElement.isNotSet());
            // And he will watch the entire wiki.
            NotificationsWatchModal notificationsWatchModal = watchButtonElement.openModal();
            assertEquals(List.of(
                NotificationsWatchModal.WatchOptions.WATCH_PAGE,
                NotificationsWatchModal.WatchOptions.WATCH_WIKI
            ), notificationsWatchModal.getAvailableOptions());
            notificationsWatchModal.selectOptionAndSave(NotificationsWatchModal.WatchOptions.WATCH_WIKI);

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

    @ParameterizedTest
    @Order(6)
    @WikisSource(mainWiki = false, extensions = { "org.xwiki.platform:xwiki-platform-notifications-ui" })
    void displayNotificationsOnSubwikis(WikiReference wikiReference, TestUtils setup, TestReference testReference)
        throws Exception
    {
        String notificationMacroDashboard = """
            {{notifications useUserPreferences="false" displayOwnEvents="true" displayRSSLink="true" /}}
            """;
        setup.login(FIRST_USER_NAME, FIRST_USER_PASSWORD);

        DocumentReference mainWikiDashboard = new DocumentReference("Dashboard", testReference.getLastSpaceReference());
        SpaceReference subWikiSpace =
            testReference.getLastSpaceReference().replaceParent(new WikiReference("xwiki"), wikiReference);
        DocumentReference subWikiDashboard = new DocumentReference("Dashboard", subWikiSpace);

        // We perform waits to ensure of the order of the events for next asserts
        // TODO: we should probably implement an ordering of the events strictly based on the moment the event is
        //  triggered to avoid having to rely on this kind of hack...
        setup.rest().savePage(testReference, "Some content", "Test Notif Main");
        Thread.sleep(1000);
        setup.rest().savePage(testReference.replaceParent(new WikiReference("xwiki"), wikiReference),
            "Some content", "Test Notif Subwiki");
        Thread.sleep(1000);
        setup.rest().savePage(mainWikiDashboard, notificationMacroDashboard, "Main Wiki Dashboard");
        Thread.sleep(1000);
        setup.rest().savePage(subWikiDashboard, notificationMacroDashboard, "Sub Wiki Dashboard");

        setup.forceGuestUser();
        setup.gotoPage(subWikiDashboard);
        // Events are processed asynchronously, so wait until the macro displays the two expected notifications.
        NotificationsContainerElement notificationsContainerElement =
            NotificationsContainerElement.waitUntilNotificationCount(2);

        for (int i = 0; i < notificationsContainerElement.getNotificationsListCount(); i++) {
            assertFalse(notificationsContainerElement.isNotificationEventRelatedToOtherWiki(i),
                String.format("Found notifications for another wiki with page [%s]",
                    notificationsContainerElement.getNotificationPage(i)));
        }
        assertEquals(2, notificationsContainerElement.getNotificationsListCount());
        assertEquals("Sub Wiki Dashboard", notificationsContainerElement.getNotificationPage(0));
        assertEquals("Test Notif Subwiki", notificationsContainerElement.getNotificationPage(1));

        setup.gotoPage(mainWikiDashboard);
        // This test should have produced 6 events, but more were produced with previous tests. Wait until at least
        // the 6 events of this test are displayed.
        notificationsContainerElement = NotificationsContainerElement.waitUntilNotificationCount(6);

        assertTrue(notificationsContainerElement.getNotificationsListCount() >= 6);
        assertEquals("Sub Wiki Dashboard (wiki1)", notificationsContainerElement.getNotificationPage(0));
        assertEquals("Main Wiki Dashboard", notificationsContainerElement.getNotificationPage(1));
        assertEquals("Test Notif Subwiki (wiki1)", notificationsContainerElement.getNotificationPage(2));
        assertEquals("Test Notif Main", notificationsContainerElement.getNotificationPage(3));

        assertTrue(notificationsContainerElement.getNotificationPage(4).startsWith("Profile of "));
        assertTrue(notificationsContainerElement.getNotificationPage(5).startsWith("Profile of "));
    }

    private SyndEntry getEntryByTitle(NotificationsRSS rss, String title)
    {
        return rss.getEntries().stream()
            .filter(entry -> title.equals(entry.getTitle()))
            .findFirst()
            .orElseThrow(() -> new AssertionError(String.format("No RSS entry with title [%s]. Titles: %s",
                title, rss.getEntries().stream().map(SyndEntry::getTitle).toList())));
    }
}
