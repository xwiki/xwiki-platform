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

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Page object representing a notification macro container with different events.
 * This is used for any call of notification macro: in the macro notification area, in the dashboard, etc.
 *
 * @version $Id$
 * @since 18.1.0RC1
 * @since 17.10.3
 * @since 17.4.9
 * @since 16.10.17
 */
public class NotificationsContainerElement extends BaseElement
{
    private static final String NOTIFICATION_MACRO_CONTAINER_CLASS = "notifications-macro";
    private final WebElement container;

    /**
     * Default constructor.
     * @param container the container representing the macro.
     */
    public NotificationsContainerElement(WebElement container)
    {
        this.container = container;
    }

    /**
     * @return an instance of {@link NotificationsContainerElement} for the first notification macro found in the page.
     */
    public static NotificationsContainerElement getElementForMacroInPage()
    {
        return new NotificationsContainerElement(
            getUtil().getDriver().findElement(By.className(NOTIFICATION_MACRO_CONTAINER_CLASS)));
    }

    /**
     * Test if the text "No notification available!" is displayed in the notification tray.
     *
     * @return true if the text is not displayed
     */
    public boolean areNotificationsAvailable()
    {
        return !getDriver().hasElement(this.container, By.className("noitems"));
    }

    private List<WebElement> getNotifications()
    {
        return getDriver().findElementsWithoutWaiting(this.container, By.className("notification-event"));
    }

    /**
     * Get the number of notifications displayed.
     *
     * @return number of notifications
     */
    public int getNotificationsListCount()
    {
        return this.getNotifications().size();
    }

    private void checkNotificationNumber(int notificationNumber)
    {
        if (notificationNumber < 0 || notificationNumber >= this.getNotificationsListCount()) {
            throw new IndexOutOfBoundsException();
        }
    }

    /**
     * Get the type of notification (bold text before notification content).
     *
     * @param notificationNumber index of the notification in the list
     * @return notification type
     */
    public String getNotificationType(int notificationNumber)
    {
        checkNotificationNumber(notificationNumber);

        return this.getNotifications().get(notificationNumber).getAttribute("data-eventtype");
    }

    /**
     * Get the content of a notification.
     *
     * @param notificationNumber index of the notification in the list
     * @return notification content
     */
    public String getNotificationContent(int notificationNumber)
    {
        checkNotificationNumber(notificationNumber);

        return this.getNotifications().get(notificationNumber).findElement(
            By.cssSelector(".notification-content")).getText();
    }

    /**
     * Get the page concerned by a notification (if any).
     *
     * @param notificationNumber index of the notification in the list
     * @return notification page
     */
    public String getNotificationPage(int notificationNumber)
    {
        checkNotificationNumber(notificationNumber);

        return this.getNotifications().get(notificationNumber).findElement(
            By.cssSelector(".notification-page")).getText();
    }

    /**
     * Check if the notification event page is coming from another wiki, by checking the presence of CSS class {@code
     * text-muted}.
     * @param notificationNumber index of the notification in the list
     * @return {@code true} if the notification event page has a text-muted CSS selector which by default contains
     * the name of the wiki where the page came from when related to another wiki.
     */
    public boolean isNotificationEventRelatedToOtherWiki(int notificationNumber)
    {
        checkNotificationNumber(notificationNumber);
        return getDriver().hasElement(
            this.getNotifications().get(notificationNumber), By.cssSelector(".notification-page .text-muted"));
    }

    /**
     * Get the description of a notification.
     *
     * @param notificationNumber index of the notification in the list
     * @return notification description
     */
    public String getNotificationDescription(int notificationNumber)
    {
        checkNotificationNumber(notificationNumber);

        return this.getNotifications().get(notificationNumber).findElement(
            By.cssSelector(".notification-description")).getText();
    }

    /**
     * Get the raw content of a notification.
     *
     * @param notificationNumber index of the notification in the list
     * @return the notification raw content
     */
    public String getNotificationRawContent(int notificationNumber)
    {
        checkNotificationNumber(notificationNumber);

        return this.getNotifications().get(notificationNumber).getText();
    }

    /**
     * Mark a notification as read.
     *
     * @param notificationNumber index of the notification in the list
     */
    public void markAsRead(int notificationNumber)
    {
        checkNotificationNumber(notificationNumber);

        getNotifications().get(notificationNumber)
            .findElement(By.cssSelector("button.notification-event-read-button")).click();
    }
}
