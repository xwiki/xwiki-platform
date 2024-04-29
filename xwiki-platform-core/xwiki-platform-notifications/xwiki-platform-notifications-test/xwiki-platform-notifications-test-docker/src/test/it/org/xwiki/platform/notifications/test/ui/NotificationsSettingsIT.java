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
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.index.tree.test.po.DocumentTreeElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.platform.notifications.test.po.AbstractNotificationsSettingsPage;
import org.xwiki.platform.notifications.test.po.NotificationsAdministrationPage;
import org.xwiki.platform.notifications.test.po.NotificationsTrayPage;
import org.xwiki.platform.notifications.test.po.NotificationsUserProfilePage;
import org.xwiki.platform.notifications.test.po.preferences.ApplicationPreferences;
import org.xwiki.platform.notifications.test.po.preferences.CustomNotificationFilterPreferencesLiveDataElement;
import org.xwiki.platform.notifications.test.po.preferences.filters.CustomNotificationFilterModal;
import org.xwiki.platform.notifications.test.po.preferences.filters.CustomNotificationFilterPreference;
import org.xwiki.platform.notifications.test.po.preferences.filters.SystemNotificationFilterPreference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.CopyOrRenameOrDeleteStatusPage;
import org.xwiki.test.ui.po.RenamePage;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.tree.test.po.TreeNodeElement;
import org.xwiki.user.test.po.AbstractUserProfilePage;
import org.xwiki.user.test.po.ProfileUserProfilePage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.xwiki.test.ui.po.BootstrapSwitch.State.OFF;
import static org.xwiki.test.ui.po.BootstrapSwitch.State.ON;
import static org.xwiki.test.ui.po.BootstrapSwitch.State.UNDETERMINED;

/**
 * Tests for the Notification settings UI.
 *
 * @version $Id$
 * @since 12.3RC1
 */
@UITest(
    properties = {
        "xwikiDbHbmCommonExtraMappings=notification-filter-preferences.hbm.xml",
        // Programming rights are required to use scheduling operations.
        "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=xwiki:Scheduler.WebHome",
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
        NotificationsUserProfilePage notificationsUserProfilePage =
            NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);

        assertEquals(1, notificationsUserProfilePage.getApplicationPreferences().size());

        // Open system
        ApplicationPreferences system = notificationsUserProfilePage.getApplication(SYSTEM);
        assertTrue(system.isCollapsed());
        system.setCollapsed(false);
        assertFalse(system.isCollapsed());
        assertEquals("Pages", system.getApplicationName());
        assertEquals("A comment is posted",
            notificationsUserProfilePage.getEventType(SYSTEM, ADD_COMMENT).getEventTypeDescription());
        assertEquals("A new page is created",
            notificationsUserProfilePage.getEventType(SYSTEM, CREATE).getEventTypeDescription());
        assertEquals("A page is deleted",
            notificationsUserProfilePage.getEventType(SYSTEM, DELETE).getEventTypeDescription());
        assertEquals("A page is modified",
            notificationsUserProfilePage.getEventType(SYSTEM, UPDATE).getEventTypeDescription());

        // Check default
        assertEquals(ON, notificationsUserProfilePage.getApplicationState(SYSTEM, ALERT_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getApplicationState(SYSTEM, EMAIL_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, ADD_COMMENT, ALERT_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, ADD_COMMENT, EMAIL_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, CREATE, ALERT_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, CREATE, EMAIL_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, DELETE, ALERT_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, DELETE, EMAIL_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, UPDATE, ALERT_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, UPDATE, EMAIL_FORMAT));

        // Disable email on system
        notificationsUserProfilePage.setApplicationState(SYSTEM, EMAIL_FORMAT, OFF);
        assertEquals(ON, notificationsUserProfilePage.getApplicationState(SYSTEM, ALERT_FORMAT));
        assertEquals(OFF, notificationsUserProfilePage.getApplicationState(SYSTEM, EMAIL_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, ADD_COMMENT, ALERT_FORMAT));
        assertEquals(OFF, notificationsUserProfilePage.getEventTypeState(SYSTEM, ADD_COMMENT, EMAIL_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, CREATE, ALERT_FORMAT));
        assertEquals(OFF, notificationsUserProfilePage.getEventTypeState(SYSTEM, CREATE, EMAIL_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, DELETE, ALERT_FORMAT));
        assertEquals(OFF, notificationsUserProfilePage.getEventTypeState(SYSTEM, DELETE, EMAIL_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, UPDATE, ALERT_FORMAT));
        assertEquals(OFF, notificationsUserProfilePage.getEventTypeState(SYSTEM, UPDATE, EMAIL_FORMAT));

        // Disable alert on "update"
        notificationsUserProfilePage.setEventTypeState(SYSTEM, UPDATE, ALERT_FORMAT, OFF);
        assertEquals(UNDETERMINED, notificationsUserProfilePage.getApplicationState(SYSTEM, ALERT_FORMAT));
        assertEquals(OFF, notificationsUserProfilePage.getApplicationState(SYSTEM, EMAIL_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, ADD_COMMENT, ALERT_FORMAT));
        assertEquals(OFF, notificationsUserProfilePage.getEventTypeState(SYSTEM, ADD_COMMENT, EMAIL_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, CREATE, ALERT_FORMAT));
        assertEquals(OFF, notificationsUserProfilePage.getEventTypeState(SYSTEM, CREATE, EMAIL_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, DELETE, ALERT_FORMAT));
        assertEquals(OFF, notificationsUserProfilePage.getEventTypeState(SYSTEM, DELETE, EMAIL_FORMAT));
        assertEquals(OFF, notificationsUserProfilePage.getEventTypeState(SYSTEM, UPDATE, ALERT_FORMAT));
        assertEquals(OFF, notificationsUserProfilePage.getEventTypeState(SYSTEM, UPDATE, EMAIL_FORMAT));

        // Enable email on "delete"
        notificationsUserProfilePage.setEventTypeState(SYSTEM, DELETE, EMAIL_FORMAT, ON);
        assertEquals(UNDETERMINED, notificationsUserProfilePage.getApplicationState(SYSTEM, ALERT_FORMAT));
        assertEquals(UNDETERMINED, notificationsUserProfilePage.getApplicationState(SYSTEM, EMAIL_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, ADD_COMMENT, ALERT_FORMAT));
        assertEquals(OFF, notificationsUserProfilePage.getEventTypeState(SYSTEM, ADD_COMMENT, EMAIL_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, CREATE, ALERT_FORMAT));
        assertEquals(OFF, notificationsUserProfilePage.getEventTypeState(SYSTEM, CREATE, EMAIL_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, DELETE, ALERT_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, DELETE, EMAIL_FORMAT));
        assertEquals(OFF, notificationsUserProfilePage.getEventTypeState(SYSTEM, UPDATE, ALERT_FORMAT));
        assertEquals(OFF, notificationsUserProfilePage.getEventTypeState(SYSTEM, UPDATE, EMAIL_FORMAT));

        // Forget it
        notificationsUserProfilePage.disableAllParameters();
    }

    @Test
    @Order(2)
    void notificationFiltersDefaultValues(TestUtils testUtils)
    {
        testUtils.login(FIRST_USER_NAME, FIRST_USER_PASSWORD);

        // Verify the default state of the filters
        NotificationsUserProfilePage notificationsUserProfilePage =
            NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
        List<SystemNotificationFilterPreference> preferences =
            notificationsUserProfilePage.getSystemNotificationFilterPreferences();
        assertEquals(6, preferences.size());

        // Filter 0
        SystemNotificationFilterPreference filter0 = preferences.get(0);
        assertEquals("Read Event Filter (Alert)", filter0.getName());
        assertEquals("Hide notifications that you have marked as read", filter0.getDescription());
        assertEquals(List.of("Alert"), filter0.getFormats());
        assertFalse(filter0.isEnabled());

        // Filter 1
        SystemNotificationFilterPreference filter1 = preferences.get(1);
        assertEquals("Read Event Filter (Email)", filter1.getName());
        assertEquals("Hide notifications that you have marked as read", filter1.getDescription());
        assertEquals(List.of("Email"), filter1.getFormats());
        assertFalse(filter1.isEnabled());

        // Filter 2
        SystemNotificationFilterPreference filter2 = preferences.get(2);
        assertEquals("Minor Event (Alert)", filter2.getName());
        assertEquals("Hide notifications concerning minor changes on pages", filter2.getDescription());
        assertEquals(List.of("Alert"), filter2.getFormats());
        assertTrue(filter2.isEnabled());

        // Filter 3
        SystemNotificationFilterPreference filter3 = preferences.get(3);
        assertEquals("Minor Event (Email)", filter3.getName());
        assertEquals("Hide notifications concerning minor changes on pages", filter3.getDescription());
        assertEquals(List.of("Email"), filter3.getFormats());
        assertTrue(filter3.isEnabled());

        // Filter 4
        SystemNotificationFilterPreference filter4 = preferences.get(4);
        assertEquals("Own Events Filter", filter4.getName());
        assertEquals("Hide notifications about your own activity unless the event specifically targets you",
            filter4.getDescription());
        assertEquals(List.of("Alert", "Email"), filter4.getFormats());
        assertTrue(filter4.isEnabled());

        // Filter 5
        SystemNotificationFilterPreference filter5 = preferences.get(5);
        assertEquals("System Filter", filter5.getName());
        assertEquals("Hide notifications from the System user unless the event specifically targets you",
            filter5.getDescription());
        assertEquals(List.of("Alert", "Email"), filter5.getFormats());
        assertTrue(filter5.isEnabled());
    }

    @Test
    @Order(3)
    void filterAndWatchedPage(TestUtils testUtils, TestReference testReference) throws Exception
    {
        testUtils.login(FIRST_USER_NAME, FIRST_USER_PASSWORD);

        try {
            NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);

            // Create a page
            testUtils.createPage(testReference, "", "");
            NotificationsTrayPage trayPage = new NotificationsTrayPage();
            trayPage.showNotificationTray();

            // Check if the page is watched
            assertTrue(trayPage.isPageOnlyWatched());
            assertFalse(trayPage.arePageAndChildrenWatched());
            assertFalse(trayPage.isWikiWatched());

            // Go back to the preferences to ensure the filter has been created
            NotificationsUserProfilePage notificationsUserProfilePage =
                NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
            CustomNotificationFilterPreferencesLiveDataElement customPrefLiveData =
                notificationsUserProfilePage.getCustomNotificationFilterPreferencesLiveData();
            List<CustomNotificationFilterPreference> preferences =
                customPrefLiveData.getCustomNotificationFilterPreferences();
            assertEquals(1, preferences.size());

            String pageName =
                testUtils.serializeReference(testReference.removeParent(new WikiReference("xwiki")));

            // Filter 0
            assertEquals("Page only", preferences.get(0).getScope());
            assertEquals(pageName, preferences.get(0).getLocation());
            assertEquals(CustomNotificationFilterPreference.FilterAction.NOTIFY_EVENT,
                preferences.get(0).getFilterAction());
            assertTrue(preferences.get(0).getEventTypes().isEmpty());
            assertTrue(preferences.get(0).getFormats().containsAll(List.of("Email", "Alert")));
            assertTrue(preferences.get(0).isEnabled());

            // back to the page
            testUtils.gotoPage(testReference);
            trayPage = new NotificationsTrayPage();
            // Unwatch the page
            trayPage.setPageOnlyWatchedState(false);
            // Verify all other buttons are updated
            assertFalse(trayPage.isPageOnlyWatched());
            assertFalse(trayPage.arePageAndChildrenWatched());
            assertFalse(trayPage.isWikiWatched());

            // Go back to the preferences to ensure the filter has been deleted
            notificationsUserProfilePage = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
            customPrefLiveData =
                notificationsUserProfilePage.getCustomNotificationFilterPreferencesLiveData();
            preferences = customPrefLiveData.getCustomNotificationFilterPreferences();
            assertTrue(preferences.isEmpty());

            // back to the page
            testUtils.gotoPage(testReference);
            trayPage = new NotificationsTrayPage();
            trayPage.showNotificationTray();

            // Watch the space
            trayPage.setPageAndChildrenWatchedState(true);
            // Verify the other button is updated
            assertTrue(trayPage.isPageOnlyWatched());

            // Go back to the preferences to ensure the filter has been created
            notificationsUserProfilePage = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
            customPrefLiveData =
                notificationsUserProfilePage.getCustomNotificationFilterPreferencesLiveData();
            preferences = customPrefLiveData.getCustomNotificationFilterPreferences();
            assertEquals(1, preferences.size());

            // Filter 1
            assertEquals("Page and children", preferences.get(0).getScope());
            assertEquals(pageName, preferences.get(0).getLocation());
            assertEquals(CustomNotificationFilterPreference.FilterAction.NOTIFY_EVENT,
                preferences.get(0).getFilterAction());
            assertTrue(preferences.get(0).getEventTypes().isEmpty());
            assertTrue(preferences.get(0).getFormats().containsAll(List.of("Email", "Alert")));
            assertTrue(preferences.get(0).isEnabled());

            // back to the page
            testUtils.gotoPage(testReference);
            trayPage = new NotificationsTrayPage();
            trayPage.showNotificationTray();

            // Unwatch the page
            trayPage.setPageOnlyWatchedState(false);
            // Verify the whole status
            assertFalse(trayPage.isPageOnlyWatched());
            assertTrue(trayPage.arePageAndChildrenWatched());
            assertFalse(trayPage.isWikiWatched());

            // Go back to the preferences
            notificationsUserProfilePage = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
            customPrefLiveData =
                notificationsUserProfilePage.getCustomNotificationFilterPreferencesLiveData();
            preferences = customPrefLiveData.getCustomNotificationFilterPreferences();
            assertEquals(2, preferences.size());

            // Filter 2
            assertEquals("Page only", preferences.get(0).getScope());
            assertEquals(pageName, preferences.get(0).getLocation());
            assertEquals(CustomNotificationFilterPreference.FilterAction.IGNORE_EVENT,
                preferences.get(0).getFilterAction());
            assertTrue(preferences.get(0).getEventTypes().isEmpty());
            assertTrue(preferences.get(0).getFormats().containsAll(List.of("Email", "Alert")));
            assertTrue(preferences.get(0).isEnabled());

            // Disable filter 2
            preferences.get(0).setEnabled(false);
            // Refresh the page
            notificationsUserProfilePage = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
            customPrefLiveData =
                notificationsUserProfilePage.getCustomNotificationFilterPreferencesLiveData();
            preferences = customPrefLiveData.getCustomNotificationFilterPreferences();
            // Verify the change have been saved
            assertFalse(preferences.get(0).isEnabled());

            // Go back to the page to check how it impacts the watch
            testUtils.gotoPage(testReference);
            trayPage = new NotificationsTrayPage();
            trayPage.showNotificationTray();
            // Verify the whole status
            assertTrue(trayPage.isPageOnlyWatched());
            assertTrue(trayPage.arePageAndChildrenWatched());
            assertFalse(trayPage.isWikiWatched());

            // Delete the filters
            notificationsUserProfilePage = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
            customPrefLiveData =
                notificationsUserProfilePage.getCustomNotificationFilterPreferencesLiveData();
            preferences = customPrefLiveData.getCustomNotificationFilterPreferences();
            // The livetable page is refreshed so we need to load back the filter preferences
            preferences.get(1).delete();
            preferences.get(0).delete();

            // Verify it's all like the beginning
            testUtils.gotoPage(testReference);
            trayPage = new NotificationsTrayPage();
            trayPage.showNotificationTray();
            assertFalse(trayPage.isPageOnlyWatched());
            assertFalse(trayPage.arePageAndChildrenWatched());
            assertFalse(trayPage.isWikiWatched());

            // Go back to the preferences
            notificationsUserProfilePage = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
            customPrefLiveData =
                notificationsUserProfilePage.getCustomNotificationFilterPreferencesLiveData();
            preferences = customPrefLiveData.getCustomNotificationFilterPreferences();
            assertTrue(preferences.isEmpty());
        } finally {
            // Clean up
            testUtils.rest().delete(testReference);

            NotificationsUserProfilePage p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
            CustomNotificationFilterPreferencesLiveDataElement customPrefLiveData =
                p.getCustomNotificationFilterPreferencesLiveData();
            List<CustomNotificationFilterPreference> preferences =
                customPrefLiveData.getCustomNotificationFilterPreferences();
            while (!preferences.isEmpty()) {
                preferences.get(preferences.size() - 1).delete();
                preferences = customPrefLiveData.getCustomNotificationFilterPreferences();
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
        AbstractNotificationsSettingsPage notificationsUserProfilePage =
            NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
        ApplicationPreferences system = notificationsUserProfilePage.getApplication(SYSTEM);
        assertTrue(system.isCollapsed());
        system.setCollapsed(false);

        // Check default values
        assertEquals(ON, notificationsUserProfilePage.getApplicationState(SYSTEM, ALERT_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getApplicationState(SYSTEM, EMAIL_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, ADD_COMMENT, ALERT_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, ADD_COMMENT, EMAIL_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, CREATE, ALERT_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, CREATE, EMAIL_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, DELETE, ALERT_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, DELETE, EMAIL_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, UPDATE, ALERT_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, UPDATE, EMAIL_FORMAT));

        assertEquals(NotificationsUserProfilePage.EmailDiffType.STANDARD,
            notificationsUserProfilePage.getNotificationEmailDiffType());
        assertEquals(NotificationsUserProfilePage.AutowatchMode.MAJOR,
            notificationsUserProfilePage.getAutoWatchModeValue());

        notificationsUserProfilePage.setEventTypeState(SYSTEM, CREATE, ALERT_FORMAT, OFF);
        notificationsUserProfilePage.setAutoWatchMode(NotificationsUserProfilePage.AutowatchMode.NONE);

        // Second step: login with superadmin and set global notification settings.
        testUtils.loginAsSuperAdmin();
        notificationsUserProfilePage = NotificationsAdministrationPage.gotoPage();

        system = notificationsUserProfilePage.getApplication(SYSTEM);
        assertTrue(system.isCollapsed());
        system.setCollapsed(false);

        // Check default values
        assertEquals(ON, notificationsUserProfilePage.getApplicationState(SYSTEM, ALERT_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getApplicationState(SYSTEM, EMAIL_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, ADD_COMMENT, ALERT_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, ADD_COMMENT, EMAIL_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, CREATE, ALERT_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, CREATE, EMAIL_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, DELETE, ALERT_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, DELETE, EMAIL_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, UPDATE, ALERT_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, UPDATE, EMAIL_FORMAT));

        assertEquals(NotificationsUserProfilePage.EmailDiffType.STANDARD,
            notificationsUserProfilePage.getNotificationEmailDiffType());
        assertEquals(NotificationsUserProfilePage.AutowatchMode.MAJOR,
            notificationsUserProfilePage.getAutoWatchModeValue());

        notificationsUserProfilePage.setEventTypeState(SYSTEM, UPDATE, ALERT_FORMAT, OFF);
        notificationsUserProfilePage.setAutoWatchMode(NotificationsUserProfilePage.AutowatchMode.ALL);
        notificationsUserProfilePage.setNotificationEmailDiffType(NotificationsUserProfilePage.EmailDiffType.NOTHING);

        // Third step: create an user, and check his settings with superadmin and perform some changes on it.
        String secondUser = testReference.getName() + "Foo";
        testUtils.deletePage("XWiki", secondUser);
        testUtils.createUser(secondUser, "foo", null);

        testUtils.gotoPage("XWiki", secondUser);
        AbstractUserProfilePage profilePage = new AbstractUserProfilePage(secondUser);
        assertTrue(profilePage.getAvailableCategories().contains("Notifications"));

        notificationsUserProfilePage = NotificationsUserProfilePage.gotoPage(secondUser);
        system = notificationsUserProfilePage.getApplication(SYSTEM);
        assertTrue(system.isCollapsed());
        system.setCollapsed(false);

        // All should be true except update alert
        assertEquals(UNDETERMINED, notificationsUserProfilePage.getApplicationState(SYSTEM, ALERT_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getApplicationState(SYSTEM, EMAIL_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, ADD_COMMENT, ALERT_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, ADD_COMMENT, EMAIL_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, CREATE, ALERT_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, CREATE, EMAIL_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, DELETE, ALERT_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, DELETE, EMAIL_FORMAT));
        assertEquals(OFF, notificationsUserProfilePage.getEventTypeState(SYSTEM, UPDATE, ALERT_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, UPDATE, EMAIL_FORMAT));

        assertEquals(NotificationsUserProfilePage.EmailDiffType.NOTHING,
            notificationsUserProfilePage.getNotificationEmailDiffType());
        assertEquals(NotificationsUserProfilePage.AutowatchMode.ALL,
            notificationsUserProfilePage.getAutoWatchModeValue());

        // perform some changes with superadmin
        notificationsUserProfilePage.setEventTypeState(SYSTEM, ADD_COMMENT, ALERT_FORMAT, OFF);
        notificationsUserProfilePage.setAutoWatchMode(NotificationsUserProfilePage.AutowatchMode.NEW);

        // Fourth step: still with superadmin, go to first user notification settings, check them and perform changes.
        testUtils.gotoPage("XWiki", FIRST_USER_NAME);
        profilePage = new AbstractUserProfilePage(FIRST_USER_NAME);
        assertTrue(profilePage.getAvailableCategories().contains("Notifications"));

        notificationsUserProfilePage = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
        system = notificationsUserProfilePage.getApplication(SYSTEM);
        assertTrue(system.isCollapsed());
        system.setCollapsed(false);

        // All should be true except for the create alert, specified in first step.
        assertEquals(UNDETERMINED, notificationsUserProfilePage.getApplicationState(SYSTEM, ALERT_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getApplicationState(SYSTEM, EMAIL_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, ADD_COMMENT, ALERT_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, ADD_COMMENT, EMAIL_FORMAT));
        assertEquals(OFF, notificationsUserProfilePage.getEventTypeState(SYSTEM, CREATE, ALERT_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, CREATE, EMAIL_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, DELETE, ALERT_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, DELETE, EMAIL_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, UPDATE, ALERT_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, UPDATE, EMAIL_FORMAT));

        // We didn't change the email settings value in first step, so we obtain here the global settings until
        // the user decide which settings she'd want for herself.
        assertEquals(NotificationsUserProfilePage.EmailDiffType.NOTHING,
            notificationsUserProfilePage.getNotificationEmailDiffType());
        assertEquals(NotificationsUserProfilePage.AutowatchMode.NONE,
            notificationsUserProfilePage.getAutoWatchModeValue());

        // perform some changes with superadmin
        notificationsUserProfilePage.setEventTypeState(SYSTEM, ADD_COMMENT, EMAIL_FORMAT, OFF);
        notificationsUserProfilePage.setNotificationEmailDiffType(NotificationsUserProfilePage.EmailDiffType.STANDARD);

        // Fifth step: login with first user and check notification settings
        testUtils.login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        notificationsUserProfilePage = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);

        system = notificationsUserProfilePage.getApplication(SYSTEM);
        assertTrue(system.isCollapsed());
        system.setCollapsed(false);

        assertEquals(UNDETERMINED, notificationsUserProfilePage.getApplicationState(SYSTEM, ALERT_FORMAT));
        assertEquals(UNDETERMINED, notificationsUserProfilePage.getApplicationState(SYSTEM, EMAIL_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, ADD_COMMENT, ALERT_FORMAT));
        assertEquals(OFF, notificationsUserProfilePage.getEventTypeState(SYSTEM, ADD_COMMENT, EMAIL_FORMAT));
        assertEquals(OFF, notificationsUserProfilePage.getEventTypeState(SYSTEM, CREATE, ALERT_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, CREATE, EMAIL_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, DELETE, ALERT_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, DELETE, EMAIL_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, UPDATE, ALERT_FORMAT));
        assertEquals(ON, notificationsUserProfilePage.getEventTypeState(SYSTEM, UPDATE, EMAIL_FORMAT));

        assertEquals(NotificationsUserProfilePage.EmailDiffType.STANDARD,
            notificationsUserProfilePage.getNotificationEmailDiffType());
        assertEquals(NotificationsUserProfilePage.AutowatchMode.NONE,
            notificationsUserProfilePage.getAutoWatchModeValue());

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
     *   - Add several new preferences and check the modal sort/filter operations
     */
    @Test
    @Order(5)
    void customFiltersAndLiveData(TestUtils testUtils) throws Exception
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
        CustomNotificationFilterPreferencesLiveDataElement customPrefLiveData =
            administrationPage.getCustomNotificationFilterPreferencesLiveData();
        List<CustomNotificationFilterPreference> customNotificationFilterPreferences =
            customPrefLiveData.getCustomNotificationFilterPreferences();
        assertTrue(customNotificationFilterPreferences.isEmpty());

        CustomNotificationFilterModal customNotificationFilterModal = administrationPage.clickAddCustomFilter();

        // check default values of the modal
        assertFalse(customNotificationFilterModal.isSubmitEnabled());
        assertEquals(Set.of(CustomNotificationFilterModal.NotificationFormat.ALERT,
                CustomNotificationFilterModal.NotificationFormat.EMAIL),
            customNotificationFilterModal.getSelectedFormats());
        assertTrue(customNotificationFilterModal.getEventsSelector().getAllSelectedOptions().isEmpty());
        DocumentTreeElement locations = customNotificationFilterModal.getLocations();
        assertTrue(locations.getSelectedNodeIDs().isEmpty());
        assertEquals(CustomNotificationFilterPreference.FilterAction.NOTIFY_EVENT,
            customNotificationFilterModal.getSelectedAction());

        locations = locations.openToDocument(NotificationsSettingsIT.class.getSimpleName(), "WebHome");
        locations.getDocumentNode(NotificationsSettingsIT.class.getSimpleName(), "WebHome").select();
        customNotificationFilterModal.getEventsSelector().selectByValue(UPDATE);
        customNotificationFilterModal.selectFormats(Set.of(CustomNotificationFilterModal.NotificationFormat.EMAIL));
        assertTrue(customNotificationFilterModal.isSubmitEnabled());
        customNotificationFilterModal.clickSubmit();

        // check newly created filter
        customNotificationFilterPreferences =
            customPrefLiveData.getCustomNotificationFilterPreferences();
        assertEquals(1, customNotificationFilterPreferences.size());
        CustomNotificationFilterPreference filterPreference =
            customNotificationFilterPreferences.get(0);

        assertEquals("Page and children", filterPreference.getScope());
        assertEquals(NotificationsSettingsIT.class.getSimpleName() + ".WebHome", filterPreference.getLocation());
        assertEquals(CustomNotificationFilterPreference.FilterAction.NOTIFY_EVENT, filterPreference.getFilterAction());
        assertEquals(List.of("Email"), filterPreference.getFormats());
        assertEquals(List.of("A page is modified"), filterPreference.getEventTypes());

        // create new user
        String secondUserUsername = NotificationsSettingsIT.class.getSimpleName() + "user2";
        String secondUserPassword = "notificationsUser2";
        // Ensure the user does not exist
        testUtils.deletePage("XWiki", secondUserUsername);
        testUtils.createUser(secondUserUsername, secondUserPassword, null);

        // go to notification settings of new user and check that the custom filter exists there too
        NotificationsUserProfilePage notificationsUserProfilePage =
            NotificationsUserProfilePage.gotoPage(secondUserUsername);
        customPrefLiveData = notificationsUserProfilePage.getCustomNotificationFilterPreferencesLiveData();
        customNotificationFilterPreferences = customPrefLiveData.getCustomNotificationFilterPreferences();

        assertEquals(1, customNotificationFilterPreferences.size());
        filterPreference = customNotificationFilterPreferences.get(0);

        assertEquals("Page and children", filterPreference.getScope());
        assertEquals(NotificationsSettingsIT.class.getSimpleName() + ".WebHome", filterPreference.getLocation());
        assertEquals(CustomNotificationFilterPreference.FilterAction.NOTIFY_EVENT, filterPreference.getFilterAction());
        assertEquals(List.of("Email"), filterPreference.getFormats());
        assertEquals(List.of("A page is modified"), filterPreference.getEventTypes());

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
        customNotificationFilterPreferences = customPrefLiveData.getCustomNotificationFilterPreferences();
        assertEquals(2, customNotificationFilterPreferences.size());

        // Filters are ordered in descending order of creation
        filterPreference = customNotificationFilterPreferences.get(0);

        assertEquals("Page only", filterPreference.getScope());
        assertEquals(NotificationsSettingsIT.class.getSimpleName() + ".SubSpace.SubPage",
            filterPreference.getLocation());
        assertEquals(CustomNotificationFilterPreference.FilterAction.IGNORE_EVENT, filterPreference.getFilterAction());
        assertEquals(List.of("Alert", "Email"), filterPreference.getFormats());
        assertTrue(filterPreference.getEventTypes().isEmpty());

        // login with the user
        testUtils.login(secondUserUsername, secondUserPassword);

        // check the filters
        notificationsUserProfilePage =
            NotificationsUserProfilePage.gotoPage(secondUserUsername);
        customPrefLiveData = notificationsUserProfilePage.getCustomNotificationFilterPreferencesLiveData();
        customNotificationFilterPreferences =
            customPrefLiveData.getCustomNotificationFilterPreferences();
        assertEquals(2, customNotificationFilterPreferences.size());

        filterPreference =
            customNotificationFilterPreferences.get(0);

        assertEquals("Page only", filterPreference.getScope());
        assertEquals(NotificationsSettingsIT.class.getSimpleName() + ".SubSpace.SubPage",
            filterPreference.getLocation());
        assertEquals(CustomNotificationFilterPreference.FilterAction.IGNORE_EVENT, filterPreference.getFilterAction());
        assertEquals(List.of("Alert", "Email"), filterPreference.getFormats());
        assertTrue(filterPreference.getEventTypes().isEmpty());

        filterPreference = customNotificationFilterPreferences.get(1);

        assertEquals("Page and children", filterPreference.getScope());
        assertEquals(NotificationsSettingsIT.class.getSimpleName() + ".WebHome", filterPreference.getLocation());
        assertEquals(CustomNotificationFilterPreference.FilterAction.NOTIFY_EVENT, filterPreference.getFilterAction());
        assertEquals(List.of("Email"), filterPreference.getFormats());
        assertEquals(List.of("A page is modified"), filterPreference.getEventTypes());

        // add a final filter: it will actually create 2 filters since we select several locations
        customNotificationFilterModal = notificationsUserProfilePage.clickAddCustomFilter();
        customNotificationFilterModal.selectAction(CustomNotificationFilterPreference.FilterAction.NOTIFY_EVENT);
        locations = customNotificationFilterModal.getLocations();
        locations = locations.openToDocument(NotificationsSettingsIT.class.getSimpleName(), "WebHome");
        TreeNodeElement node1 = locations.getDocumentNode(NotificationsSettingsIT.class.getSimpleName(), "Page1");
        TreeNodeElement node2 = locations.getDocumentNode(NotificationsSettingsIT.class.getSimpleName(), "Page2");
        locations.selectNodes(node1, node2);

        customNotificationFilterModal.selectFormats(Set.of(CustomNotificationFilterModal.NotificationFormat.ALERT));
        customNotificationFilterModal.getEventsSelector().selectByValue(UPDATE);
        customNotificationFilterModal.getEventsSelector().selectByValue(DELETE);
        customNotificationFilterModal.clickSubmit(2);

        // check created filters
        customNotificationFilterPreferences = customPrefLiveData.getCustomNotificationFilterPreferences();
        assertEquals(4, customNotificationFilterPreferences.size());

        filterPreference = customNotificationFilterPreferences.get(0);

        assertEquals("Page only", filterPreference.getScope());
        assertEquals(NotificationsSettingsIT.class.getSimpleName() + ".Page2", filterPreference.getLocation());
        assertEquals(CustomNotificationFilterPreference.FilterAction.NOTIFY_EVENT, filterPreference.getFilterAction());
        assertEquals(List.of("Alert"), filterPreference.getFormats());
        // core.events.delete before core.events.update
        assertEquals(List.of("A page is deleted","A page is modified"), filterPreference.getEventTypes());

        filterPreference = customNotificationFilterPreferences.get(1);

        assertEquals("Page only", filterPreference.getScope());
        assertEquals(NotificationsSettingsIT.class.getSimpleName() + ".Page1", filterPreference.getLocation());
        assertEquals(CustomNotificationFilterPreference.FilterAction.NOTIFY_EVENT, filterPreference.getFilterAction());
        assertEquals(List.of("Alert"), filterPreference.getFormats());
        // core.events.delete before core.events.update
        assertEquals(List.of("A page is deleted", "A page is modified"), filterPreference.getEventTypes());

        // follow a user
        ProfileUserProfilePage userProfilePage = ProfileUserProfilePage.gotoPage(FIRST_USER_NAME);
        assertFalse(userProfilePage.isFollowed());
        userProfilePage.toggleFollowButton();

        notificationsUserProfilePage =
            NotificationsUserProfilePage.gotoPage(secondUserUsername);
        notificationsUserProfilePage.setAutoWatchMode(AbstractNotificationsSettingsPage.AutowatchMode.NEW);

        SpaceReference newPagesSpaceRef = new SpaceReference("NewPages", lastSpaceReference);
        // Create multiple pages
        for (int i = 0; i < 15; i++) {
            DocumentReference pageRef = new DocumentReference("Page_" + i, newPagesSpaceRef);
            testUtils.rest().savePage(pageRef, "Content of page " + i, "Title of page " + i );
        }
        String pageSpaceName =
            NotificationsSettingsIT.class.getSimpleName() + "." + newPagesSpaceRef.getName();

        notificationsUserProfilePage =
            NotificationsUserProfilePage.gotoPage(secondUserUsername);
        customPrefLiveData = notificationsUserProfilePage.getCustomNotificationFilterPreferencesLiveData();

        // 4 filters created before following a user
        // +1 when following the user
        // +15 when creating the pages after setting autowatch
        assertEquals(20, customPrefLiveData.getTableLayout().getTotalEntries());

        customPrefLiveData.filterScope("USER");
        assertEquals(1, customPrefLiveData.getTableLayout().getTotalEntries());
        filterPreference = customPrefLiveData.getCustomNotificationFilterPreferences().get(0);
        assertEquals("User", filterPreference.getScope());
        assertEquals(List.of("Alert", "Email"), filterPreference.getFormats());
        assertEquals(CustomNotificationFilterPreference.FilterAction.NOTIFY_EVENT, filterPreference.getFilterAction());
        assertTrue(filterPreference.getEventTypes().isEmpty());
        assertEquals("XWiki." + FIRST_USER_NAME, filterPreference.getLocation());

        customPrefLiveData.clearAllFilters();
        customPrefLiveData.filterLocation(newPagesSpaceRef.getName());
        // Click twice to order descending
        customPrefLiveData.sortLocation();
        customPrefLiveData.sortLocation();
        assertEquals(15, customPrefLiveData.getTableLayout().getTotalEntries());
        filterPreference = customPrefLiveData.getCustomNotificationFilterPreferences().get(0);
        assertEquals("Page only", filterPreference.getScope());
        assertEquals(List.of("Alert", "Email"), filterPreference.getFormats());
        assertEquals(CustomNotificationFilterPreference.FilterAction.NOTIFY_EVENT, filterPreference.getFilterAction());
        assertTrue(filterPreference.getEventTypes().isEmpty());
        // Order is alphabetical so Page_9 is > Page_15
        assertEquals(pageSpaceName + ".Page_9", filterPreference.getLocation());

        customPrefLiveData.clearAllFilters();
        customPrefLiveData.clearAllSort();
        assertEquals(20, customPrefLiveData.getTableLayout().getTotalEntries());
        customPrefLiveData.filterFilterAction("INCLUSIVE");
        assertEquals(19, customPrefLiveData.getTableLayout().getTotalEntries());
        customPrefLiveData.filterLocation("2");
        // 3 possibles matches: Page2,Page_2,Page_12
        assertEquals(3, customPrefLiveData.getTableLayout().getTotalEntries());
        customPrefLiveData.sortFormats();
        customPrefLiveData.sortFormats();
        filterPreference = customPrefLiveData.getCustomNotificationFilterPreferences().get(0);
        assertEquals("Page only", filterPreference.getScope());
        assertEquals(NotificationsSettingsIT.class.getSimpleName() + ".Page2", filterPreference.getLocation());
        assertEquals(CustomNotificationFilterPreference.FilterAction.NOTIFY_EVENT, filterPreference.getFilterAction());
        assertEquals(List.of("Alert"), filterPreference.getFormats());
        // core.events.delete before core.events.update
        assertEquals(List.of("A page is deleted","A page is modified"), filterPreference.getEventTypes());

        customPrefLiveData.filterFormat("EMAIL");
        assertEquals(2, customPrefLiveData.getTableLayout().getTotalEntries());
        customPrefLiveData.sortLocation();

        filterPreference = customPrefLiveData.getCustomNotificationFilterPreferences().get(0);
        assertEquals("Page only", filterPreference.getScope());
        assertEquals(List.of("Alert", "Email"), filterPreference.getFormats());
        assertEquals(CustomNotificationFilterPreference.FilterAction.NOTIFY_EVENT, filterPreference.getFilterAction());
        assertTrue(filterPreference.getEventTypes().isEmpty());
        assertEquals(pageSpaceName + ".Page_12", filterPreference.getLocation());
    }

    @Test
    @Order(6)
    void watchAndRename(TestUtils testUtils, TestReference testReference)
    {
        testUtils.login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        ViewPage viewPage = testUtils.createPage(testReference, "Content", "Title");
        NotificationsTrayPage notificationsTrayPage = new NotificationsTrayPage();
        notificationsTrayPage.showNotificationTray();
        // Autowatch is enabled for created page
        assertTrue(notificationsTrayPage.isPageOnlyWatched());
        RenamePage renamePage = viewPage.rename();
        renamePage.getDocumentPicker().setTitle(testReference.getName() + "Renamed");
        CopyOrRenameOrDeleteStatusPage statusPage = renamePage.clickRenameButton().waitUntilFinished();
        statusPage.gotoNewPage();
        notificationsTrayPage = new NotificationsTrayPage();
        notificationsTrayPage.showNotificationTray();
        assertTrue(notificationsTrayPage.isPageOnlyWatched());

        testUtils.gotoPage(testReference);
        notificationsTrayPage = new NotificationsTrayPage();
        notificationsTrayPage.showNotificationTray();
        assertFalse(notificationsTrayPage.isPageOnlyWatched());
    }
}
