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

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.platform.notifications.test.po.NotificationsTrayPage;
import org.xwiki.platform.notifications.test.po.NotificationsUserProfilePage;
import org.xwiki.platform.notifications.test.po.preferences.ApplicationPreferences;
import org.xwiki.platform.notifications.test.po.preferences.filters.NotificationFilterPreference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.BootstrapSwitch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the Notification settings UI
 *
 * @version $Id$
 * @since 12.3RC1
 */
@UITest(
    properties = {
        "xwikiDbHbmCommonExtraMappings=notification-filter-preferences.hbm.xml",
    },
    extraJARs = {
        "org.xwiki.platform:xwiki-platform-notifications-filters-default",
        "org.xwiki.platform:xwiki-platform-eventstream-store-hibernate",
        // The Solr store is not ready yet to be installed as extension
        "org.xwiki.platform:xwiki-platform-eventstream-store-solr"
    }
)
public class NotificationsSettingsIT
{
    private static final String FIRST_USER_NAME = NotificationsSettingsIT.class.getSimpleName() + "user1";
    private static final String FIRST_USER_PASSWORD = "notificationsUser1";
    private static final String EMAIL_FORMAT = "email";
    private static final String SYSTEM = "org.xwiki.platform";
    private static final String CREATE = "create";

    private static final String DELETE = "delete";

    private static final String UPDATE = "update";
    private static final String ADD_COMMENT = "addComment";
    private static final String ALERT_FORMAT = "alert";

    @BeforeEach
    public void setup(TestUtils testUtils)
    {
        testUtils.createUser(FIRST_USER_NAME, FIRST_USER_PASSWORD, "", "");
    }

    @AfterEach
    public void tearDown(TestUtils testUtils)
    {
        testUtils.deletePage("XWiki", FIRST_USER_NAME);
    }

    @Test
    @Order(1)
    public void notificationsSwitches(TestUtils testUtils) throws Exception
    {
        testUtils.login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        NotificationsUserProfilePage p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);

        assertEquals(1, p.getApplicationPreferences().size());

        // Open system
        ApplicationPreferences system = p.getApplication(SYSTEM);
        assertTrue(system.isCollapsed());
        system.setCollapsed(false);
        assertFalse(system.isCollapsed());
        assertEquals("Pages", system.getApplicationName());
        assertEquals("A comment is posted", p.getEventType(SYSTEM, ADD_COMMENT).getEventTypeDescription());
        assertEquals("A new page is created", p.getEventType(SYSTEM, CREATE).getEventTypeDescription());
        assertEquals("A page is deleted", p.getEventType(SYSTEM, DELETE).getEventTypeDescription());
        assertEquals("A page is modified", p.getEventType(SYSTEM, UPDATE).getEventTypeDescription());

        // Check default
        assertEquals(BootstrapSwitch.State.OFF, p.getApplicationState(SYSTEM, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getApplicationState(SYSTEM, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, ADD_COMMENT, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, ADD_COMMENT, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, CREATE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, CREATE, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, DELETE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, DELETE, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, UPDATE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, UPDATE, EMAIL_FORMAT));

        // Enable alert on system
        p.setApplicationState(SYSTEM, ALERT_FORMAT, BootstrapSwitch.State.ON);
        assertEquals(BootstrapSwitch.State.ON , p.getApplicationState(SYSTEM, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getApplicationState(SYSTEM, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.ON , p.getEventTypeState(SYSTEM, ADD_COMMENT, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, ADD_COMMENT, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.ON , p.getEventTypeState(SYSTEM, CREATE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, CREATE, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.ON , p.getEventTypeState(SYSTEM, DELETE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, DELETE, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.ON , p.getEventTypeState(SYSTEM, UPDATE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, UPDATE, EMAIL_FORMAT));

        // Enable email on system
        p.setApplicationState(SYSTEM, EMAIL_FORMAT, BootstrapSwitch.State.ON);
        assertEquals(BootstrapSwitch.State.ON, p.getApplicationState(SYSTEM, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.ON, p.getApplicationState(SYSTEM, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.ON, p.getEventTypeState(SYSTEM, ADD_COMMENT, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.ON, p.getEventTypeState(SYSTEM, ADD_COMMENT, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.ON, p.getEventTypeState(SYSTEM, CREATE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.ON, p.getEventTypeState(SYSTEM, CREATE, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.ON, p.getEventTypeState(SYSTEM, DELETE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.ON, p.getEventTypeState(SYSTEM, DELETE, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.ON, p.getEventTypeState(SYSTEM, UPDATE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.ON, p.getEventTypeState(SYSTEM, UPDATE, EMAIL_FORMAT));

        // Disable email on system
        p.setApplicationState(SYSTEM, EMAIL_FORMAT, BootstrapSwitch.State.OFF);
        assertEquals(BootstrapSwitch.State.ON , p.getApplicationState(SYSTEM, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getApplicationState(SYSTEM, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.ON , p.getEventTypeState(SYSTEM, ADD_COMMENT, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, ADD_COMMENT, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.ON , p.getEventTypeState(SYSTEM, CREATE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, CREATE, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.ON , p.getEventTypeState(SYSTEM, DELETE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, DELETE, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.ON , p.getEventTypeState(SYSTEM, UPDATE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, UPDATE, EMAIL_FORMAT));

        // Disable alert on "update"
        p.setEventTypeState(SYSTEM, UPDATE, ALERT_FORMAT, BootstrapSwitch.State.OFF);
        assertEquals(BootstrapSwitch.State.UNDETERMINED , p.getApplicationState(SYSTEM, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getApplicationState(SYSTEM, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.ON , p.getEventTypeState(SYSTEM, ADD_COMMENT, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, ADD_COMMENT, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.ON , p.getEventTypeState(SYSTEM, CREATE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, CREATE, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.ON , p.getEventTypeState(SYSTEM, DELETE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, DELETE, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, UPDATE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, UPDATE, EMAIL_FORMAT));

        // Enable email on "delete"
        p.setEventTypeState(SYSTEM, DELETE, EMAIL_FORMAT, BootstrapSwitch.State.ON);
        assertEquals(BootstrapSwitch.State.UNDETERMINED , p.getApplicationState(SYSTEM, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.UNDETERMINED, p.getApplicationState(SYSTEM, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.ON , p.getEventTypeState(SYSTEM, ADD_COMMENT, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, ADD_COMMENT, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.ON , p.getEventTypeState(SYSTEM, CREATE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, CREATE, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.ON , p.getEventTypeState(SYSTEM, DELETE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.ON , p.getEventTypeState(SYSTEM, DELETE, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, UPDATE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, UPDATE, EMAIL_FORMAT));

        // Forget it
        p.disableAllParameters();
    }

    @Test
    @Order(2)
    public void notificationFiltersDefaultValues(TestUtils testUtils) throws Exception
    {
        testUtils.login(FIRST_USER_NAME, FIRST_USER_PASSWORD);

        // Verify the default state of the filters
        NotificationsUserProfilePage p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
        List<NotificationFilterPreference> preferences = p.getNotificationFilterPreferences();
        assertEquals(6, preferences.size());

        // Filter 0
        assertEquals("Minor Event (Alert)", preferences.get(0).getFilterName());
        assertEquals("Hide notifications concerning minor changes on pages", preferences.get(0).getFilterType());
        assertTrue(preferences.get(0).getEventTypes().isEmpty());
        assertEquals(Arrays.asList("Alert"), preferences.get(0).getFormats());
        assertTrue(preferences.get(0).isEnabled());

        // Filter 1
        assertEquals("Minor Event (Email)", preferences.get(1).getFilterName());
        assertEquals("Hide notifications concerning minor changes on pages", preferences.get(1).getFilterType());
        assertTrue(preferences.get(1).getEventTypes().isEmpty());
        assertEquals(Arrays.asList("Email"), preferences.get(1).getFormats());
        assertTrue(preferences.get(1).isEnabled());

        // Filter 2
        assertEquals("Own Events Filter", preferences.get(2).getFilterName());
        assertEquals("Hide notifications about your own activity", preferences.get(2).getFilterType());
        assertTrue(preferences.get(2).getEventTypes().isEmpty());
        assertEquals(Arrays.asList("Alert", "Email"), preferences.get(2).getFormats());
        assertTrue(preferences.get(2).isEnabled());

        // Filter 3
        assertEquals("Read Event Filter (Alert)", preferences.get(3).getFilterName());
        assertEquals("Hide notifications that you have marked as read", preferences.get(3).getFilterType());
        assertTrue(preferences.get(3).getEventTypes().isEmpty());
        assertEquals(Arrays.asList("Alert"), preferences.get(3).getFormats());
        assertFalse(preferences.get(3).isEnabled());

        // Filter 4
        assertEquals("Read Event Filter (Email)", preferences.get(4).getFilterName());
        assertEquals("Hide notifications that you have marked as read", preferences.get(4).getFilterType());
        assertTrue(preferences.get(4).getEventTypes().isEmpty());
        assertEquals(Arrays.asList("Email"), preferences.get(4).getFormats());
        assertFalse(preferences.get(4).isEnabled());

        // Filter 5
        assertEquals("System Filter", preferences.get(5).getFilterName());
        assertEquals("Hide notifications from the System user", preferences.get(5).getFilterType());
        assertTrue(preferences.get(5).getEventTypes().isEmpty());
        assertEquals(Arrays.asList("Alert", "Email"), preferences.get(5).getFormats());
        assertTrue(preferences.get(5).isEnabled());
    }

    @Test
    @Order(3)
    public void filterAndWatchedPage(TestUtils testUtils, TestReference testReference) throws Exception
    {
        testUtils.login(FIRST_USER_NAME, FIRST_USER_PASSWORD);

        try {
            NotificationsUserProfilePage p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);

            // Enable the notifications
            p.setApplicationState(SYSTEM, "alert", BootstrapSwitch.State.ON);

            // Create a page
            testUtils.createPage(testReference.getLastSpaceReference().getName(), testReference.getName(), "", "");
            NotificationsTrayPage trayPage = new NotificationsTrayPage();
            trayPage.showNotificationTray();

            // Check if the page is watched
            assertTrue(trayPage.isPageOnlyWatched());
            assertTrue(trayPage.arePageAndChildrenWatched());
            assertTrue(trayPage.isWikiWatched());

            // Unwatch the wiki
            trayPage.setWikiWatchedState(false);
            // Verify all other buttons are updated
            assertFalse(trayPage.isPageOnlyWatched());
            assertFalse(trayPage.arePageAndChildrenWatched());

            // Go back to the preferences to ensure the filter has been created
            p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
            List<NotificationFilterPreference> preferences = p.getNotificationFilterPreferences();
            assertEquals(7, preferences.size());

            // Filter 6
            assertTrue(preferences.get(6).getFilterName().contains("Wiki"));
            assertEquals("", preferences.get(6).getLocation());
            assertEquals("Exclusive", preferences.get(6).getFilterType());
            assertTrue(preferences.get(6).getEventTypes().isEmpty());;
            assertTrue(preferences.get(6).getFormats().containsAll(Arrays.asList("Email", "Alert")));
            assertTrue(preferences.get(6).isEnabled());

            // back to the page
            testUtils.gotoPage(testReference.getLastSpaceReference().getName(), testReference.getName());
            trayPage = new NotificationsTrayPage();
            trayPage.showNotificationTray();

            // Watch the space
            trayPage.setPageAndChildrenWatchedState(true);
            // Verify the other button is updated
            assertTrue(trayPage.isPageOnlyWatched());

            // Go back to the preferences to ensure the filter has been created
            p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
            preferences = p.getNotificationFilterPreferences();
            assertEquals(8, preferences.size());

            // Filter 7
            assertTrue(preferences.get(7).getFilterName().contains("Page and children"),
                String.format("Name: [%s] ID: [%s]", preferences.get(7).getFilterName(), preferences.get(7).getID()));
            assertEquals(testReference.getLastSpaceReference().getName() + ".WebHome", preferences.get(7).getLocation());
            assertEquals("Inclusive", preferences.get(7).getFilterType());
            assertTrue(preferences.get(7).getEventTypes().isEmpty());
            assertTrue(preferences.get(7).getFormats().containsAll(Arrays.asList("Email", "Alert")));
            assertTrue(preferences.get(7).isEnabled());

            // back to the page
            testUtils.gotoPage(testReference.getLastSpaceReference().getName(), testReference.getName());
            trayPage = new NotificationsTrayPage();
            trayPage.showNotificationTray();

            // Unwatch the page
            trayPage.setPageOnlyWatchedState(false);
            // Verify the whole status
            assertFalse(trayPage.isPageOnlyWatched());
            assertTrue(trayPage.arePageAndChildrenWatched());
            assertFalse(trayPage.isWikiWatched());

            // Go back to the preferences
            p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
            preferences = p.getNotificationFilterPreferences();
            assertEquals(9, preferences.size());

            // Filter 8
            assertTrue(preferences.get(8).getFilterName().contains("Page only"));
            assertEquals(testReference.getLastSpaceReference().getName() + "." + testReference.getName(), preferences.get(8).getLocation());
            assertEquals("Exclusive", preferences.get(8).getFilterType());
            assertTrue(preferences.get(8).getEventTypes().isEmpty());
            assertTrue(preferences.get(8).getFormats().containsAll(Arrays.asList("Email", "Alert")));
            assertTrue(preferences.get(8).isEnabled());

            // Disable filter 6
            preferences.get(6).setEnabled(false);
            // Refresh the page
            p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
            preferences = p.getNotificationFilterPreferences();
            // Verify the change have been saved
            assertFalse(preferences.get(6).isEnabled());

            // Delete these new filters
            preferences.get(8).delete();
            // The livetable page is refreshed so we need to load back the filter preferences
            p.getNotificationFilterPreferences().get(7).delete();
            p.getNotificationFilterPreferences().get(6).delete();

            // Verify it's all like the beginning
            testUtils.gotoPage(testReference.getLastSpaceReference().getName(), testReference.getName());
            trayPage = new NotificationsTrayPage();
            trayPage.showNotificationTray();
            assertTrue(trayPage.isPageOnlyWatched());
            assertTrue(trayPage.arePageAndChildrenWatched());
            assertTrue(trayPage.isWikiWatched());

            // Go back to the preferences
            p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
            preferences = p.getNotificationFilterPreferences();
            assertEquals(6, preferences.size());

        } finally {
            // Clean up
            testUtils.rest().deletePage(testReference.getLastSpaceReference().getName(), testReference.getName());

            NotificationsUserProfilePage p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
            List<NotificationFilterPreference> preferences = p.getNotificationFilterPreferences();
            while (preferences.size() > 6) {
                preferences.get(preferences.size() - 1).delete();
                // Reload teh livetable
                preferences = p.getNotificationFilterPreferences();
            }
        }
    }
}
