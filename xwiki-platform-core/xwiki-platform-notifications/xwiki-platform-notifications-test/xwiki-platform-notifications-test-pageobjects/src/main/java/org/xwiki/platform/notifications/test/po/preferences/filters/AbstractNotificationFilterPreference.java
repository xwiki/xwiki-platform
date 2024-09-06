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
import org.xwiki.test.ui.po.BootstrapSwitch;

import static org.xwiki.test.ui.po.BootstrapSwitch.State.OFF;
import static org.xwiki.test.ui.po.BootstrapSwitch.State.ON;

/**
 * Abstract representation of the notification filter preferences which covers common needs for
 * {@link SystemNotificationFilterPreference} and {@link CustomNotificationFilterPreference}.
 *
 * @version $Id$
 * @since 13.2RC1
 */
public abstract class AbstractNotificationFilterPreference
{
    private final XWikiWebDriver webDriver;

    private AbstractNotificationsSettingsPage parentPage;

    private WebElement row;

    private List<String> formats = new ArrayList<>();

    private BootstrapSwitch enabledSwitch;

    /**
     * Default constructor.
     *
     * @param parentPage the page where the settings are displayed.
     * @param row the row of the Live Data for this filter
     * @param webDriver the webdriver to initialize the switches.
     */
    public AbstractNotificationFilterPreference(AbstractNotificationsSettingsPage parentPage, WebElement row,
        XWikiWebDriver webDriver)
    {
        this.parentPage = parentPage;
        this.row = row;
        List<WebElement> formatElements = getFormatsElement(row)
            .findElements(By.tagName("li"));
        for (WebElement format : formatElements) {
            this.formats.add(format.getText());
        }
        this.webDriver = webDriver;
        this.enabledSwitch = new BootstrapSwitch(getBootstrapSwitchElement(row), webDriver);
    }

    /**
     * @param row the row to get the formats from
     * @return the {@link WebElement} containing the formats of the row
     */
    private WebElement getFormatsElement(WebElement row)
    {
        return row.findElement(By.cssSelector("td[data-title='Formats'] .view"));
    }

    /**
     * @param row the row to get the switch from
     * @return the {@link WebElement} containing the switch element of the row
     */
    private WebElement getBootstrapSwitchElement(WebElement row)
    {
        this.webDriver.waitUntilElementIsVisible(row, By.cssSelector(".displayer-toggle .bootstrap-switch"));
        return row.findElement(By.className("displayer-toggle")).findElement(By.className("bootstrap-switch"));
    }

    /**
     * @return the parent page where the settings are displayed.
     */
    public AbstractNotificationsSettingsPage getParentPage()
    {
        return parentPage;
    }

    /**
     * @return the row
     */
    public WebElement getRow()
    {
        return row;
    }

    /**
     * @return the formats of the filter.
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
        return enabledSwitch.getState() == ON;
    }

    /**
     * Enable or disable the current filter.
     *
     * @param enabled either or not the filter must be enabled
     * @throws Exception if the expected state cannot be set
     */
    public void setEnabled(boolean enabled) throws Exception
    {
        if (isEnabled() == enabled) {
            return;
        }

        this.enabledSwitch.setState(enabled ? ON : OFF);
        this.parentPage.waitForNotificationSuccessMessage("Filter preference saved!");
    }
}
