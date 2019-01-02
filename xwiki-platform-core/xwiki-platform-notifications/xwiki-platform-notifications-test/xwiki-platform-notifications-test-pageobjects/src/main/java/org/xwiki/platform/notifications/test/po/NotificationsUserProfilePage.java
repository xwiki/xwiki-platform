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
package org.xwiki.platform.notifications.test.po;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.platform.notifications.test.po.preferences.AbstractNotificationPreferences;
import org.xwiki.platform.notifications.test.po.preferences.ApplicationPreferences;
import org.xwiki.platform.notifications.test.po.preferences.EventTypePreferences;
import org.xwiki.platform.notifications.test.po.preferences.filters.NotificationFilterPreference;
import org.xwiki.stability.Unstable;
import org.xwiki.test.ui.po.BootstrapSwitch;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents the user profile's Notifications tab.
 * 
 * @version $Id$
 * @since 9.4RC1
 */
public class NotificationsUserProfilePage extends ViewPage
{
    private static final String SAVED_NOTIFICATION_TEXT = "Saved!";

    private static final String ALERT_FORMAT = "alert";

    private static final String EMAIL_FORMAT = "email";

    @FindBy(id = "notificationsPane")
    private WebElement notificationsPane;

    @FindBy(className = "notificationAutoWatchMode")
    private WebElement notificationAutoWatchModeSelect;

    private Map<String, ApplicationPreferences> applicationPreferences = new HashMap<>();

    @FindBy(id = "notificationFilterPreferencesLiveTable-display")
    private WebElement notificationFilterPreferencesLivetable;

    /**
     * Construct a NotificationsUserProfilePage (and for the browser page to be fully loaded).
     */
    public NotificationsUserProfilePage()
    {
        getDriver().waitUntilElementIsVisible(notificationsPane, By.cssSelector(".notifPreferences .bootstrap-switch"));

        for (WebElement element : getDriver().findElements(By.cssSelector("tbody.applicationElem"))) {
            ApplicationPreferences pref = new ApplicationPreferences(element, getDriver());
            applicationPreferences.put(pref.getApplicationId(), pref);
        }
    }

    /**
     * @return a map of the preferences for each application
     * @since 9.7RC1
     */
    public Map<String, ApplicationPreferences> getApplicationPreferences()
    {
        return applicationPreferences;
    }

    /**
     * @param username the user profile document name
     * @return the notifications profile tab page object
     * @since 9.7RC1
     */
    public static NotificationsUserProfilePage gotoPage(String username)
    {
        getUtil().gotoPage("XWiki", username, "view", "category=notifications");
        return new NotificationsUserProfilePage();
    }

    /**
     * @param applicationId id of the application
     * @return the preferences for the given application
     * @throws Exception if the application cannot be found
     * @since 9.7RC1
     */
    public ApplicationPreferences getApplication(String applicationId) throws Exception
    {
        ApplicationPreferences pref = applicationPreferences.get(applicationId);
        if (pref == null) {
            throw new Exception(String.format("Application [%s] is not present.", applicationId));
        }
        return pref;
    }

    /**
     * @param applicationId id of the application
     * @param eventType name of the event type
     * @return the preferences for the given event type of the given application
     * @throws Exception if the event type cannot be found
     * @since 9.7RC1
     */
    public EventTypePreferences getEventType(String applicationId, String eventType) throws Exception
    {
        EventTypePreferences pref = getApplication(applicationId).getEventTypePreferences().get(eventType);
        if (pref == null) {
            throw new Exception(
                    String.format("Event Type [%s] is not present in the application [%s].", eventType, applicationId));
        }
        return pref;
    }

    /**
     * @param applicationId id of the application
     * @param format the format of the notification
     * @return the state of the switch related to given application and format
     * @throws Exception if the application cannot be found
     * @since 9.7RC1
     */
    public BootstrapSwitch.State getApplicationState(String applicationId, String format) throws Exception
    {
        ApplicationPreferences pref = getApplication(applicationId);
        if (ALERT_FORMAT.equals(format)) {
            return pref.getAlertState();
        } else {
            return pref.getEmailState();
        }
    }

    /**
     * @param applicationId id of the application
     * @param eventType name of the event type
     * @param format the format of the notification
     * @return the state of the switch related to given event type and format
     * @throws Exception if the event type cannot be found
     * @since 9.7RC1
     */
    public BootstrapSwitch.State getEventTypeState(String applicationId, String eventType, String format)
            throws Exception
    {
        EventTypePreferences pref = getEventType(applicationId, eventType);
        if (ALERT_FORMAT.equals(format)) {
            return pref.getAlertState();
        } else {
            return pref.getEmailState();
        }
    }

    /**
     * Set the state of the given application for the given format.
     * @param applicationId id of the application
     * @param format the format of the notification
     * @param state the state to set
     * @throws Exception if the application is not found
     * @since 9.7RC1
     */
    public void setApplicationState(String applicationId, String format, BootstrapSwitch.State state) throws Exception
    {
        AbstractNotificationPreferences pref = getApplication(applicationId);
        setState(format, state, pref);
    }

    /**
     * Set the state of the event type for the given format.
     * @param applicationId id of the application
     * @param eventType name of the event type
     * @param format the format of the notification
     * @param state the state to set
     * @throws Exception if the event type cannot be found
     * @since 9.7RC1
     */
    public void setEventTypeState(String applicationId, String eventType, String format, BootstrapSwitch.State state)
            throws Exception
    {
        EventTypePreferences pref = getEventType(applicationId, eventType);
        setState(format, state, pref);
    }

    private void setState(String format, BootstrapSwitch.State state, AbstractNotificationPreferences pref)
            throws Exception
    {
        boolean wait = false;
        if (ALERT_FORMAT.equals(format)) {
            if (pref.getAlertState() != state) {
                pref.setAlertState(state);
                wait = true;
            }
        } else {
            if (pref.getEmailState() != state) {
                pref.setEmailState(state);
                wait = true;
            }
        }
        if (wait) {
            this.waitForNotificationSuccessMessage(SAVED_NOTIFICATION_TEXT);
        }
    }

    /**
     * Disable every notification parameters.
     */
    public void disableAllParameters()
    {
        WebElement never = notificationAutoWatchModeSelect.findElement(By.xpath("option[@value='NONE']"));
        never.click();
        try {
            for (ApplicationPreferences app : applicationPreferences.values()) {
                if (app.getAlertState() != BootstrapSwitch.State.OFF) {
                    app.setAlertState(BootstrapSwitch.State.OFF);
                    this.waitForNotificationSuccessMessage(SAVED_NOTIFICATION_TEXT);
                }
                if (app.getEmailState() != BootstrapSwitch.State.OFF) {
                    app.setEmailState(BootstrapSwitch.State.OFF);
                    this.waitForNotificationSuccessMessage(SAVED_NOTIFICATION_TEXT);
                }
            }
        } catch (Exception e) {
            // Do nothing, the exception is only triggered if we try to use an invalid state
        }
    }

    /**
     * @return the notification filter preferences, as they are described in the corresponding livetable
     *
     * @since 10.8RC1
     * @since 9.11.8
     */
    public List<NotificationFilterPreference> getNotificationFilterPreferences()
    {

        List<NotificationFilterPreference> preferences = new ArrayList<>();
        for (WebElement row : this.notificationFilterPreferencesLivetable.findElements(By.tagName("tr"))) {
            preferences.add(new NotificationFilterPreference(this, row, this.getDriver()));
        }
        return preferences;
    }
}
