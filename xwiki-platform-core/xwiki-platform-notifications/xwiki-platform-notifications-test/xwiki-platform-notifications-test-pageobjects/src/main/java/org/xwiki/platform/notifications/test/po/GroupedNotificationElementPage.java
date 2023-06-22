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

import static org.openqa.selenium.By.cssSelector;

/**
 * Represent generic operations on the notification tray groups.
 *
 * @version $Id$
 * @since 12.8RC1
 */
public class GroupedNotificationElementPage extends BaseElement
{
    protected static final String NOTIFICATION_EVENT_SELECTOR = "div.notification-event";

    protected static final String TEXT_SELECTOR = "tr td.description";

    private static final String GROUP_SELECTOR =
        "button.toggle-notification-event-details";

    private static final String EMITTER_SELECTOR =
        "tr td span.notification-event-user";

    private final WebElement rootElement;

    /**
     * Default constructor.
     * @param rootElement The root element of the notification blocks.
     */
    public GroupedNotificationElementPage(WebElement rootElement)
    {

        this.rootElement = rootElement;
    }

    /**
     * Opens a notification group.
     * @param notificationNumber The notification index.
     */
    public void openGroup(int notificationNumber)
    {
        WebElement notifications =
            findNotifications().get(notificationNumber);
        WebElement group = notifications.findElement(cssSelector(GROUP_SELECTOR));
        group.click();
    }

    /**
     * Finds the text of a notification.
     * @param notificationNumber The notification group number.
     * @param groupIndex The index of the notification in the group.
     * @return The mention text.
     */
    public String getText(int notificationNumber, int groupIndex)
    {
        WebElement notifications = findNotifications().get(notificationNumber);
        WebElement element = notifications.findElements(cssSelector(TEXT_SELECTOR)).get(groupIndex);
        return element.getText();
    }

    /**
     *
     * @param notificationNumber The notification group number.
     * @param groupIndex The index of the notification in the group.
     * @return The mention emitter.
     */
    public String getEmitter(int notificationNumber, int groupIndex)
    {
        WebElement notifications = findNotifications().get(notificationNumber);
        WebElement emitter = notifications.findElements(cssSelector(EMITTER_SELECTOR)).get(groupIndex);
        return emitter.findElement(By.tagName("a")).getText();
    }

    /**
     * Get the number of notification details.
     * @param notificationNumber The notification group number.
     * @return the number of details row contained in the group
     * @since 15.5
     */
    public int getNumberOfElements(int notificationNumber)
    {
        return findNotifications().get(notificationNumber).findElements(cssSelector(TEXT_SELECTOR)).size();
    }

    protected List<WebElement> findNotifications()
    {
        return this.rootElement.findElements(cssSelector(NOTIFICATION_EVENT_SELECTOR));
    }
}
