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
import org.xwiki.platform.notifications.test.po.NotificationsUserProfilePage;
import org.xwiki.test.ui.XWikiWebDriver;
import org.xwiki.test.ui.po.BootstrapSwitch;
import org.xwiki.test.ui.po.ConfirmationBox;

/**
 * Represent a livetable row describing a notification filter preference.
 *
 * @version $Id$
 * @since 10.8RC1
 * @since 9.11.8
 */
public class NotificationFilterPreference
{
    private static final String LIST_HTML_TAG = "li";

    private NotificationsUserProfilePage parentPage;

    private WebElement livetableRow;

    private String filterName;

    private String filterType;

    private List<String> eventTypes = new ArrayList<>();

    private List<String> formats = new ArrayList<>();

    private BootstrapSwitch enabledSwitch;

    /**
     * Construct a NotificationFilterPreference.
     * @param parentPage the user profile's page holding the livetable.
     * @param webElement the livetable row
     * @param driver the current webdriver in used
     */
    public NotificationFilterPreference(NotificationsUserProfilePage parentPage, WebElement webElement,
            XWikiWebDriver driver)
    {
        this.parentPage = parentPage;
        this.livetableRow = webElement;

        this.filterName = webElement.findElement(By.className("name")).getText();
        this.filterType = webElement.findElement(By.className("filterType")).getText();

        List<WebElement> eventTypeElements = webElement.findElement(By.className("eventTypes")).findElements(
                By.tagName(LIST_HTML_TAG));
        for (WebElement eventType : eventTypeElements) {
            String text = eventType.getText();
            if (!"-".equals(text)) {
                this.eventTypes.add(text);
            }
        }

        List<WebElement> formatElements = webElement.findElement(By.className("notificationFormats"))
                .findElements(By.tagName(LIST_HTML_TAG));
        for (WebElement format : formatElements) {
            this.formats.add(format.getText());
        }

        enabledSwitch = new BootstrapSwitch(
                webElement.findElement(By.className("isEnabled")).findElement(By.className("bootstrap-switch")),
                driver
        );
    }

    /**
     * @return the name of the filter
     */
    public String getFilterName()
    {
        return filterName;
    }

    /**
     * @return the type of the filter
     */
    public String getFilterType()
    {
        return filterType;
    }

    /**
     * @return the event types concerned by the filters (empty means "all").
     */
    public List<String> getEventTypes()
    {
        return eventTypes;
    }

    /**
     * @return the formats concerned by the filter
     */
    public List<String> getFormats()
    {
        return formats;
    }

    /**
     * @return either or not the preference is enabled
     */
    public boolean isEnabled()
    {
        return enabledSwitch.getState() == BootstrapSwitch.State.ON;
    }

    /**
     * Enable or disable the current filter.
     * @param enabled either or not the filter must be enabled
     * @throws Exception if the expected state cannot be set
     */
    public void setEnabled(boolean enabled) throws Exception
    {
        if (isEnabled() == enabled) {
            return;
        }

        this.enabledSwitch.setState(enabled ? BootstrapSwitch.State.ON : BootstrapSwitch.State.OFF);
        this.parentPage.waitForNotificationSuccessMessage("Filter preference saved!");
    }

    /**
     * @return the watched location if the current filter is a ScopeNotificationFilter.
     */
    public String getLocation()
    {
        return this.livetableRow.findElement(By.cssSelector("td.name ol")).getAttribute("data-entity");
    }

    /**
     * Delete the filter preference.
     */
    public void delete()
    {
        this.livetableRow.findElement(By.cssSelector("td.actions a.actiondelete")).click();
        ConfirmationBox confirmationBox = new ConfirmationBox();
        confirmationBox.clickYes();
        this.parentPage.waitForNotificationSuccessMessage("Filter preference deleted!");
    }

    @Override
    public String toString()
    {
        return String.format("NotificationFilterPreference{filterName='%s', filterType='%', evenTypes=%s, formats=%s,"
                + " enabled=%s", filterName, filterName, eventTypes, formats, isEnabled());
    }
}
