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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.index.tree.test.po.DocumentTreeElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.platform.notifications.test.po.AbstractNotificationsSettingsPage;
import org.xwiki.platform.notifications.test.po.NotificationsAdministrationPage;
import org.xwiki.platform.notifications.test.po.NotificationsTrayPage;
import org.xwiki.platform.notifications.test.po.NotificationsUserProfilePage;
import org.xwiki.platform.notifications.test.po.preferences.ApplicationPreferences;
import org.xwiki.platform.notifications.test.po.preferences.filters.CustomNotificationFilterModal;
import org.xwiki.platform.notifications.test.po.preferences.filters.CustomNotificationFilterPreference;
import org.xwiki.platform.notifications.test.po.preferences.filters.SystemNotificationFilterPreference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.BootstrapSwitch;
import org.xwiki.tree.test.po.TreeNodeElement;
import org.xwiki.user.test.po.AbstractUserProfilePage;

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
        // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus
        // we need to provide the JAR inside WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-8271
        "org.xwiki.platform:xwiki-platform-notifications-filters-default",
        // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus
        // we need to provide the JAR inside WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-8271
        "org.xwiki.platform:xwiki-platform-eventstream-store-hibernate",
        // The Solr store is not ready yet to be installed as extension
        "org.xwiki.platform:xwiki-platform-eventstream-store-solr"
    }
)
class NotificationsSettingsIT
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
    void setup(TestUtils testUtils)
    {
        testUtils.createUser(FIRST_USER_NAME, FIRST_USER_PASSWORD, "", "");
    }

    @AfterEach
    void tearDown(TestUtils testUtils)
    {
        testUtils.deletePage("XWiki", FIRST_USER_NAME);
    }

    @Test
    @Order(1)
    void notificationsSwitches(TestUtils testUtils) throws Exception
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
    void notificationFiltersDefaultValues(TestUtils testUtils) throws Exception
    {
        testUtils.login(FIRST_USER_NAME, FIRST_USER_PASSWORD);

        // Verify the default state of the filters
        NotificationsUserProfilePage p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
        List<SystemNotificationFilterPreference> preferences = p.getSystemNotificationFilterPreferences();
        assertEquals(6, preferences.size());

        // Filter 0
        assertEquals("Minor Event (Alert)", preferences.get(0).getFilterName());
        assertEquals("Hide notifications concerning minor changes on pages", preferences.get(0).getDescription());
        assertEquals(Arrays.asList("Alert"), preferences.get(0).getFormats());
        assertTrue(preferences.get(0).isEnabled());

        // Filter 1
        assertEquals("Minor Event (Email)", preferences.get(1).getFilterName());
        assertEquals("Hide notifications concerning minor changes on pages", preferences.get(1).getDescription());
        assertEquals(Arrays.asList("Email"), preferences.get(1).getFormats());
        assertTrue(preferences.get(1).isEnabled());

        // Filter 2
        assertEquals("Own Events Filter", preferences.get(2).getFilterName());
        assertEquals("Hide notifications about your own activity unless the event specifically targets you",
            preferences.get(2).getDescription());
        assertEquals(Arrays.asList("Alert", "Email"), preferences.get(2).getFormats());
        assertTrue(preferences.get(2).isEnabled());

        // Filter 3
        assertEquals("Read Event Filter (Alert)", preferences.get(3).getFilterName());
        assertEquals("Hide notifications that you have marked as read", preferences.get(3).getDescription());
        assertEquals(Arrays.asList("Alert"), preferences.get(3).getFormats());
        assertFalse(preferences.get(3).isEnabled());

        // Filter 4
        assertEquals("Read Event Filter (Email)", preferences.get(4).getFilterName());
        assertEquals("Hide notifications that you have marked as read", preferences.get(4).getDescription());
        assertEquals(Arrays.asList("Email"), preferences.get(4).getFormats());
        assertFalse(preferences.get(4).isEnabled());

        // Filter 5
        assertEquals("System Filter", preferences.get(5).getFilterName());
        assertEquals("Hide notifications from the System user unless the event specifically targets you",
            preferences.get(5).getDescription());
        assertEquals(Arrays.asList("Alert", "Email"), preferences.get(5).getFormats());
        assertTrue(preferences.get(5).isEnabled());
    }

    @Test
    @Order(3)
    void filterAndWatchedPage(TestUtils testUtils, TestReference testReference) throws Exception
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
            List<CustomNotificationFilterPreference> preferences = p.getCustomNotificationFilterPreferences();
            assertEquals(1, preferences.size());

            // Filter 0
            assertTrue(preferences.get(0).getFilterName().contains("Wiki"));
            assertEquals("", preferences.get(0).getLocation());
            assertEquals(CustomNotificationFilterPreference.FilterAction.IGNORE_EVENT,
                preferences.get(0).getFilterAction());
            assertTrue(preferences.get(0).getEventTypes().isEmpty());;
            assertTrue(preferences.get(0).getFormats().containsAll(Arrays.asList("Email", "Alert")));
            assertTrue(preferences.get(0).isEnabled());

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
            preferences = p.getCustomNotificationFilterPreferences();
            assertEquals(2, preferences.size());

            // Filter 1
            assertTrue(preferences.get(1).getFilterName().contains("Page and children"));
            assertEquals(testReference.getLastSpaceReference().getName() + ".WebHome",
                preferences.get(1).getLocation());
            assertEquals(CustomNotificationFilterPreference.FilterAction.NOTIFY_EVENT,
                preferences.get(1).getFilterAction());
            assertTrue(preferences.get(1).getEventTypes().isEmpty());
            assertTrue(preferences.get(1).getFormats().containsAll(Arrays.asList("Email", "Alert")));
            assertTrue(preferences.get(1).isEnabled());

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
            preferences = p.getCustomNotificationFilterPreferences();
            assertEquals(3, preferences.size());

            // Filter 2
            assertTrue(preferences.get(2).getFilterName().contains("Page only"));
            assertEquals(testReference.getLastSpaceReference().getName() + "." + testReference.getName(),
                preferences.get(2).getLocation());
            assertEquals(CustomNotificationFilterPreference.FilterAction.IGNORE_EVENT,
                preferences.get(2).getFilterAction());
            assertTrue(preferences.get(2).getEventTypes().isEmpty());
            assertTrue(preferences.get(2).getFormats().containsAll(Arrays.asList("Email", "Alert")));
            assertTrue(preferences.get(2).isEnabled());

            // Disable filter 6
            preferences.get(0).setEnabled(false);
            // Refresh the page
            p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
            preferences = p.getCustomNotificationFilterPreferences();
            // Verify the change have been saved
            assertFalse(preferences.get(0).isEnabled());

            // Delete these new filters
            preferences.get(2).delete();
            // The livetable page is refreshed so we need to load back the filter preferences
            p.getCustomNotificationFilterPreferences().get(1).delete();
            p.getCustomNotificationFilterPreferences().get(0).delete();

            // Verify it's all like the beginning
            testUtils.gotoPage(testReference.getLastSpaceReference().getName(), testReference.getName());
            trayPage = new NotificationsTrayPage();
            trayPage.showNotificationTray();
            assertTrue(trayPage.isPageOnlyWatched());
            assertTrue(trayPage.arePageAndChildrenWatched());
            assertTrue(trayPage.isWikiWatched());

            // Go back to the preferences
            p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
            preferences = p.getCustomNotificationFilterPreferences();
            assertTrue(preferences.isEmpty());

        } finally {
            // Clean up
            testUtils.rest().deletePage(testReference.getLastSpaceReference().getName(), testReference.getName());

            NotificationsUserProfilePage p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
            List<CustomNotificationFilterPreference> preferences = p.getCustomNotificationFilterPreferences();
            while (!preferences.isEmpty()) {
                preferences.get(preferences.size() - 1).delete();
                // Reload teh livetable
                preferences = p.getCustomNotificationFilterPreferences();
            }
        }
    }

    /**
     * Check global settings behaviour and notification settings for another user.
     */
    @Test
    @Order(4)
    void globalAndOtherUserSettings(TestUtils testUtils, TestReference testReference) throws Exception
    {
        // First step: login with first user, and set some notification settings.
        testUtils.login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        AbstractNotificationsSettingsPage p =
            NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
        ApplicationPreferences system = p.getApplication(SYSTEM);
        assertTrue(system.isCollapsed());
        system.setCollapsed(false);

        // Check default values
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

        assertEquals(NotificationsUserProfilePage.EmailDiffType.STANDARD, p.getNotificationEmailDiffType());
        assertEquals(NotificationsUserProfilePage.AutowatchMode.MAJOR, p.getAutoWatchModeValue());

        p.setEventTypeState(SYSTEM, CREATE, ALERT_FORMAT, BootstrapSwitch.State.ON);
        p.setAutoWatchMode(NotificationsUserProfilePage.AutowatchMode.NONE);

        // Second step: login with superadmin and set global notification settings.
        testUtils.loginAsSuperAdmin();
        p = NotificationsAdministrationPage.gotoPage();

        system = p.getApplication(SYSTEM);
        assertTrue(system.isCollapsed());
        system.setCollapsed(false);
        
        // Check default values
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

        assertEquals(NotificationsUserProfilePage.EmailDiffType.STANDARD, p.getNotificationEmailDiffType());
        assertEquals(NotificationsUserProfilePage.AutowatchMode.MAJOR, p.getAutoWatchModeValue());

        p.setEventTypeState(SYSTEM, UPDATE, ALERT_FORMAT, BootstrapSwitch.State.ON);
        p.setAutoWatchMode(NotificationsUserProfilePage.AutowatchMode.ALL);
        p.setNotificationEmailDiffType(NotificationsUserProfilePage.EmailDiffType.NOTHING);

        // Third step: create an user, and check his settings with superadmin and perform some changes on it.
        String secondUser = testReference.getName() + "Foo";
        testUtils.deletePage("XWiki", secondUser);
        testUtils.createUser(secondUser, "foo", null);

        testUtils.gotoPage("XWiki", secondUser);
        AbstractUserProfilePage profilePage = new AbstractUserProfilePage(secondUser);
        assertTrue(profilePage.getAvailableCategories().contains("Notifications"));

        p = NotificationsUserProfilePage.gotoPage(secondUser);
        system = p.getApplication(SYSTEM);
        assertTrue(system.isCollapsed());
        system.setCollapsed(false);

        // All should be false except update alert
        assertEquals(BootstrapSwitch.State.UNDETERMINED, p.getApplicationState(SYSTEM, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getApplicationState(SYSTEM, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, ADD_COMMENT, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, ADD_COMMENT, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, CREATE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, CREATE, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, DELETE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, DELETE, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.ON, p.getEventTypeState(SYSTEM, UPDATE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, UPDATE, EMAIL_FORMAT));

        assertEquals(NotificationsUserProfilePage.EmailDiffType.NOTHING, p.getNotificationEmailDiffType());
        assertEquals(NotificationsUserProfilePage.AutowatchMode.ALL, p.getAutoWatchModeValue());

        // perform some changes with superadmin
        p.setEventTypeState(SYSTEM, ADD_COMMENT, ALERT_FORMAT, BootstrapSwitch.State.ON);
        p.setAutoWatchMode(NotificationsUserProfilePage.AutowatchMode.NEW);

        // Fourth step: still with superadmin, go to first user notification settings, check them and perform changes.
        testUtils.gotoPage("XWiki", FIRST_USER_NAME);
        profilePage = new AbstractUserProfilePage(FIRST_USER_NAME);
        assertTrue(profilePage.getAvailableCategories().contains("Notifications"));

        p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
        system = p.getApplication(SYSTEM);
        assertTrue(system.isCollapsed());
        system.setCollapsed(false);

        // All should be false except for the create alert, specified in first step.
        assertEquals(BootstrapSwitch.State.UNDETERMINED, p.getApplicationState(SYSTEM, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getApplicationState(SYSTEM, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, ADD_COMMENT, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, ADD_COMMENT, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.ON, p.getEventTypeState(SYSTEM, CREATE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, CREATE, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, DELETE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, DELETE, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, UPDATE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, UPDATE, EMAIL_FORMAT));

        // We didn't change the email settings value in first step, so we obtain here the global settings until
        // the user decide which settings she'd want for herself.
        assertEquals(NotificationsUserProfilePage.EmailDiffType.NOTHING, p.getNotificationEmailDiffType());
        assertEquals(NotificationsUserProfilePage.AutowatchMode.NONE, p.getAutoWatchModeValue());

        // perform some changes with superadmin
        p.setEventTypeState(SYSTEM, ADD_COMMENT, EMAIL_FORMAT, BootstrapSwitch.State.ON);
        p.setNotificationEmailDiffType(NotificationsUserProfilePage.EmailDiffType.STANDARD);

        // Fifth step: login with first user and check notification settings
        testUtils.login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);

        system = p.getApplication(SYSTEM);
        assertTrue(system.isCollapsed());
        system.setCollapsed(false);

        assertEquals(BootstrapSwitch.State.UNDETERMINED, p.getApplicationState(SYSTEM, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.UNDETERMINED, p.getApplicationState(SYSTEM, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, ADD_COMMENT, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.ON, p.getEventTypeState(SYSTEM, ADD_COMMENT, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.ON, p.getEventTypeState(SYSTEM, CREATE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, CREATE, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, DELETE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, DELETE, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, UPDATE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, UPDATE, EMAIL_FORMAT));

        assertEquals(NotificationsUserProfilePage.EmailDiffType.STANDARD, p.getNotificationEmailDiffType());
        assertEquals(NotificationsUserProfilePage.AutowatchMode.NONE, p.getAutoWatchModeValue());

        // sixth step: check if first user can access notification settings of the other user.
        testUtils.gotoPage("XWiki", secondUser);
        profilePage = new AbstractUserProfilePage(secondUser);
        assertFalse(profilePage.getAvailableCategories().contains("Notifications"));
    }

    /**
     * This test performs the following scenario:
     *   - add a new filters in global administration and check it
     *   - create a new user and check that the filter exist for it
     *   - add a new filter to the user with admin user
     *   - login with the new user and add another new filter
     */
    @Test
    @Order(5)
    void addCustomFilters(TestUtils testUtils)
    {
        // Create pages for the filter locations
        SpaceReference lastSpaceReference = new SpaceReference("xwiki", NotificationsSettingsIT.class.getSimpleName());
        DocumentReference page1 = new DocumentReference("Page1", lastSpaceReference);
        testUtils.createPage(page1, "", "Page1");
        DocumentReference page2 = new DocumentReference("Page2", lastSpaceReference);
        testUtils.createPage(page2, "", "Page2");
        SpaceReference subSpace = new SpaceReference("SubSpace", lastSpaceReference);
        DocumentReference subpageRef = new DocumentReference("SubPage", subSpace);
        testUtils.createPage(subpageRef, "", "SubPage");

        testUtils.loginAsSuperAdmin();
        NotificationsAdministrationPage administrationPage = NotificationsAdministrationPage.gotoPage();
        List<CustomNotificationFilterPreference> customNotificationFilterPreferences =
            administrationPage.getCustomNotificationFilterPreferences();
        assertTrue(customNotificationFilterPreferences.isEmpty());

        CustomNotificationFilterModal customNotificationFilterModal = administrationPage.clickAddCustomFilter();

        // check default values of the modal
        assertFalse(customNotificationFilterModal.isSubmitEnabled());
        assertEquals(new HashSet<>(Arrays.asList(
            CustomNotificationFilterModal.NotificationFormat.ALERT,
            CustomNotificationFilterModal.NotificationFormat.EMAIL)),
            customNotificationFilterModal.getSelectedFormats());
        assertTrue(customNotificationFilterModal.getEventsSelector().getAllSelectedOptions().isEmpty());
        DocumentTreeElement locations = customNotificationFilterModal.getLocations();
        assertTrue(locations.getSelectedNodeIDs().isEmpty());
        assertEquals(CustomNotificationFilterPreference.FilterAction.NOTIFY_EVENT,
            customNotificationFilterModal.getSelectedAction());

        locations = locations.openToDocument(NotificationsSettingsIT.class.getSimpleName(), "WebHome");
        locations.getDocumentNode(NotificationsSettingsIT.class.getSimpleName(), "WebHome").select();
        customNotificationFilterModal.getEventsSelector().selectByValue(UPDATE);
        customNotificationFilterModal.selectFormats(Collections.singleton(
            CustomNotificationFilterModal.NotificationFormat.EMAIL));
        assertTrue(customNotificationFilterModal.isSubmitEnabled());
        customNotificationFilterModal.clickSubmit();

        // check newly created filter
        customNotificationFilterPreferences =
            administrationPage.getCustomNotificationFilterPreferences();
        assertEquals(1, customNotificationFilterPreferences.size());
        CustomNotificationFilterPreference filterPreference =
            customNotificationFilterPreferences.get(0);

        assertTrue(filterPreference.getFilterName().contains("Page and children"));
        assertEquals(NotificationsSettingsIT.class.getSimpleName() + ".WebHome", filterPreference.getLocation());
        assertEquals(CustomNotificationFilterPreference.FilterAction.NOTIFY_EVENT, filterPreference.getFilterAction());
        assertEquals(Collections.singletonList("Email"), filterPreference.getFormats());
        assertEquals(Collections.singletonList("A page is modified"), filterPreference.getEventTypes());

        // create new user
        String secondUserUsername = NotificationsSettingsIT.class.getSimpleName() + "user2";
        String secondUserPassword = "notificationsUser2";
        // Ensure the user does not exist
        testUtils.deletePage("XWiki", secondUserUsername);
        testUtils.createUser(secondUserUsername, secondUserPassword, null);

        // go to notification settings of new user and check that the custom filter exists there too
        NotificationsUserProfilePage notificationsUserProfilePage =
            NotificationsUserProfilePage.gotoPage(secondUserUsername);
        customNotificationFilterPreferences =
            notificationsUserProfilePage.getCustomNotificationFilterPreferences();

        assertEquals(1, customNotificationFilterPreferences.size());
        filterPreference =
            customNotificationFilterPreferences.get(0);

        assertTrue(filterPreference.getFilterName().contains("Page and children"));
        assertEquals(NotificationsSettingsIT.class.getSimpleName() + ".WebHome", filterPreference.getLocation());
        assertEquals(CustomNotificationFilterPreference.FilterAction.NOTIFY_EVENT, filterPreference.getFilterAction());
        assertEquals(Collections.singletonList("Email"), filterPreference.getFormats());
        assertEquals(Collections.singletonList("A page is modified"), filterPreference.getEventTypes());

        // create a new custom filter for this user
        customNotificationFilterModal = notificationsUserProfilePage.clickAddCustomFilter();
        assertTrue(customNotificationFilterModal.isDisplayed());
        locations = customNotificationFilterModal.getLocations();
        locations = locations.openToDocument(NotificationsSettingsIT.class.getSimpleName(), "SubSpace", "SubPage");
        locations.getDocumentNode(NotificationsSettingsIT.class.getSimpleName(), "SubSpace", "SubPage").select();
        customNotificationFilterModal.selectAction(CustomNotificationFilterPreference.FilterAction.IGNORE_EVENT);
        assertTrue(customNotificationFilterModal.isSubmitEnabled());
        assertTrue(customNotificationFilterModal.isDisplayed());
        customNotificationFilterModal.clickSubmit();

        // Check the newly created filter
        customNotificationFilterPreferences =
            notificationsUserProfilePage.getCustomNotificationFilterPreferences();
        assertEquals(2, customNotificationFilterPreferences.size());

        filterPreference =
            customNotificationFilterPreferences.get(1);

        assertTrue(filterPreference.getFilterName().contains("Page"));
        assertFalse(filterPreference.getFilterName().contains("Page and children"));
        assertEquals(NotificationsSettingsIT.class.getSimpleName() + ".SubSpace.SubPage",
            filterPreference.getLocation());
        assertEquals(CustomNotificationFilterPreference.FilterAction.IGNORE_EVENT, filterPreference.getFilterAction());
        assertEquals(Arrays.asList("Alert", "Email"), filterPreference.getFormats());
        assertTrue(filterPreference.getEventTypes().isEmpty());

        // login with the user
        testUtils.login(secondUserUsername, secondUserPassword);

        // check the filters
        notificationsUserProfilePage =
            NotificationsUserProfilePage.gotoPage(secondUserUsername);
        customNotificationFilterPreferences =
            notificationsUserProfilePage.getCustomNotificationFilterPreferences();
        assertEquals(2, customNotificationFilterPreferences.size());

        filterPreference =
            customNotificationFilterPreferences.get(0);

        assertTrue(filterPreference.getFilterName().contains("Page and children"));
        assertEquals(NotificationsSettingsIT.class.getSimpleName() + ".WebHome", filterPreference.getLocation());
        assertEquals(CustomNotificationFilterPreference.FilterAction.NOTIFY_EVENT, filterPreference.getFilterAction());
        assertEquals(Collections.singletonList("Email"), filterPreference.getFormats());
        assertEquals(Collections.singletonList("A page is modified"), filterPreference.getEventTypes());

        filterPreference =
            customNotificationFilterPreferences.get(1);

        assertTrue(filterPreference.getFilterName().contains("Page"));
        assertFalse(filterPreference.getFilterName().contains("Page and children"));
        assertEquals(NotificationsSettingsIT.class.getSimpleName() + ".SubSpace.SubPage",
            filterPreference.getLocation());
        assertEquals(CustomNotificationFilterPreference.FilterAction.IGNORE_EVENT, filterPreference.getFilterAction());
        assertEquals(Arrays.asList("Alert", "Email"), filterPreference.getFormats());
        assertTrue(filterPreference.getEventTypes().isEmpty());

        // add a final filter: it will actually create 2 filters since we select several locations
        customNotificationFilterModal = notificationsUserProfilePage.clickAddCustomFilter();
        customNotificationFilterModal.selectAction(CustomNotificationFilterPreference.FilterAction.NOTIFY_EVENT);
        locations = customNotificationFilterModal.getLocations();
        locations = locations.openToDocument(NotificationsSettingsIT.class.getSimpleName(),  "WebHome");
        TreeNodeElement node1 = locations.getDocumentNode(NotificationsSettingsIT.class.getSimpleName(), "Page1");
        TreeNodeElement node2 = locations.getDocumentNode(NotificationsSettingsIT.class.getSimpleName(), "Page2");
        locations.selectNodes(node1, node2);

        customNotificationFilterModal.selectFormats(
            Collections.singleton(CustomNotificationFilterModal.NotificationFormat.ALERT));
        customNotificationFilterModal.getEventsSelector().selectByValue(UPDATE);
        customNotificationFilterModal.getEventsSelector().selectByValue(DELETE);
        customNotificationFilterModal.clickSubmit();

        // check created filters
        customNotificationFilterPreferences =
            notificationsUserProfilePage.getCustomNotificationFilterPreferences();
        assertEquals(4, customNotificationFilterPreferences.size());

        filterPreference =
            customNotificationFilterPreferences.get(2);

        assertTrue(filterPreference.getFilterName().contains("Page"));
        assertFalse(filterPreference.getFilterName().contains("Page and children"));
        assertEquals(NotificationsSettingsIT.class.getSimpleName() + ".Page1", filterPreference.getLocation());
        assertEquals(CustomNotificationFilterPreference.FilterAction.NOTIFY_EVENT, filterPreference.getFilterAction());
        assertEquals(Collections.singletonList("Alert"), filterPreference.getFormats());
        assertEquals(Arrays.asList("A page is modified", "A page is deleted"), filterPreference.getEventTypes());

        filterPreference =
            customNotificationFilterPreferences.get(3);

        assertTrue(filterPreference.getFilterName().contains("Page"));
        assertFalse(filterPreference.getFilterName().contains("Page and children"));
        assertEquals(NotificationsSettingsIT.class.getSimpleName() + ".Page2", filterPreference.getLocation());
        assertEquals(CustomNotificationFilterPreference.FilterAction.NOTIFY_EVENT, filterPreference.getFilterAction());
        assertEquals(Collections.singletonList("Alert"), filterPreference.getFormats());
        assertEquals(Arrays.asList("A page is modified", "A page is deleted"), filterPreference.getEventTypes());
    }
}
