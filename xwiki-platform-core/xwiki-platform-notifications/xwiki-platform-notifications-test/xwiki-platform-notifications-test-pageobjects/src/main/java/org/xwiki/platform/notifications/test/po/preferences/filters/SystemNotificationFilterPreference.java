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

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.platform.notifications.test.po.AbstractNotificationsSettingsPage;
import org.xwiki.test.ui.XWikiWebDriver;

/**
 * Represents a System notification filter that can only be enabled or disabled.
 *
 * @version $Id$
 * @since 13.2RC1
 */
public class SystemNotificationFilterPreference extends AbstractNotificationFilterPreference
{
    /**
     * Default constructor.
     * @param parentPage the page where the settings are displayed.
     * @param row the row of the livetable for this filter.
     * @param webDriver the webdriver to initialize the switches.
     */
    public SystemNotificationFilterPreference(AbstractNotificationsSettingsPage parentPage, WebElement row,
        XWikiWebDriver webDriver)
    {
        super(parentPage, row, webDriver);
    }

    /**
     * @return the name of the filter.
     */
    public String getName()
    {
        return getRow().findElement(By.cssSelector("td[data-title='Name'] .view")).getText();
    }

    /**
     * @return the description of the filter.
     */
    public String getDescription()
    {
        return getRow().findElement(By.cssSelector("td[data-title='Description'] .view")).getText();
    }
}
