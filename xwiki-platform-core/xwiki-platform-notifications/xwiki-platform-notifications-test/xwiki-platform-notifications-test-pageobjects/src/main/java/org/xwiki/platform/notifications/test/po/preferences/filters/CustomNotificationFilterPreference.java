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
package org.xwiki.platform.notifications.test.po.preferences.filters;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.platform.notifications.test.po.AbstractNotificationsSettingsPage;
import org.xwiki.test.ui.XWikiWebDriver;
import org.xwiki.test.ui.po.ConfirmationBox;

import static org.xwiki.platform.notifications.test.po.preferences.filters.CustomNotificationFilterPreference.FilterAction.IGNORE_EVENT;
import static org.xwiki.platform.notifications.test.po.preferences.filters.CustomNotificationFilterPreference.FilterAction.NOTIFY_EVENT;

/**
 * Represents a custom notification filter: i.e., a scope notification filter created by the user or through the watch
 * settings.
 *
 * @version $Id$
 * @since 13.2RC1
 */
public class CustomNotificationFilterPreference extends AbstractNotificationFilterPreference
{
    /**
     * Represents the possible actions for such filters.
     *
     * @version $Id$
     */
    public enum FilterAction
    {
        /**
         * Ignore event is displayed when it's an exclusive filter.
         */
        IGNORE_EVENT,

        /**
         * Notify event is displayed when it's an inclusive filter.
         */
        NOTIFY_EVENT
    }

    private final List<String> eventTypes = new ArrayList<>();

    private final FilterAction filterAction;

    /**
     * Default constructor.
     *
     * @param parentPage the page where the settings are displayed.
     * @param row the row of the livetable for this filter.
     * @param webDriver the webdriver to initialize the switches.
     */
    public CustomNotificationFilterPreference(AbstractNotificationsSettingsPage parentPage, WebElement row,
        XWikiWebDriver webDriver)
    {
        super(parentPage, row, webDriver);

        List<WebElement> eventTypeElements =
            row.findElement(By.cssSelector("td[data-title='Events']")).findElements(By.tagName("li"));
        for (WebElement eventType : eventTypeElements) {
            String text = eventType.getText();
            if (!text.startsWith("All events")) {
                this.eventTypes.add(text);
            }
        }

        String filterType = row.findElement(By.cssSelector("td[data-title='Filter Action'] .view")).getText();
        if (filterType.startsWith("Notify")) {
            this.filterAction = NOTIFY_EVENT;
        } else {
            this.filterAction = IGNORE_EVENT;
        }
    }

    /**
     * @return the scope of the filter.
     */
    public String getScope()
    {
        return getRow().findElement(By.cssSelector("td[data-title='Scope']")).getText();
    }

    /**
     * @return the event types or an empty list in case of all events
     */
    public List<String> getEventTypes()
    {
        return this.eventTypes;
    }

    /**
     * @return the watched location
     */
    public String getLocation()
    {
        return getRow().findElement(By.cssSelector("td[data-title='Location'] .html-wrapper ol"))
            .getAttribute("data-entity");
    }

    /**
     * @return the action performed by the filter
     */
    public FilterAction getFilterAction()
    {
        return this.filterAction;
    }

    /**
     * Delete the filter preference.
     */
    public void delete()
    {
        this.getRow().findElement(By.cssSelector(".actions-container .action_delete")).click();
        ConfirmationBox confirmationBox = new ConfirmationBox();
        confirmationBox.clickYes();
        this.getParentPage().waitForNotificationSuccessMessage("Filter preference deleted!");
    }
}
