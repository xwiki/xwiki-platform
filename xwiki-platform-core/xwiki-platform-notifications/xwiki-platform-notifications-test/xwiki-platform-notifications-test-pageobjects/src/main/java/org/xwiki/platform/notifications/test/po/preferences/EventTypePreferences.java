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
package org.xwiki.platform.notifications.test.po.preferences;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.XWikiWebDriver;
import org.xwiki.test.ui.po.BootstrapSwitch;

/**
 * Wrap the notification preferences of a given event type.
 *
 * @version $Id$
 * @since 9.7RC1
 */
public class EventTypePreferences extends AbstractNotificationPreferences
{
    private String eventType;

    /**
     * Construct an EventTypePreferences.
     * @param webElement table row of the event type
     * @param driver the xwiki web driver
     */
    public EventTypePreferences(WebElement webElement, XWikiWebDriver driver)
    {
        super(webElement, driver);
        this.eventType   = webElement.getAttribute("data-eventtype");
    }

    /**
     * @return the id of the event type
     */
    public String getEventType()
    {
        return eventType;
    }

    /**
     * @return the description of the event type
     * @throws Exception if the event type is not displayed
     */
    public String getEventTypeDescription() throws Exception
    {
        if (!webElement.isDisplayed()) {
            throw new Exception(String.format(
                    "The event type [%s] is not displayed, so we cannot read its description.", eventType));
        }
        return webElement.findElement(By.cssSelector("td:first-child")).getText();
    }

    @Override
    protected BootstrapSwitch getSwitch(String format)
    {
        return new BootstrapSwitch(
                webElement.findElement(
                        By.cssSelector(
                                String.format(
                                        "td.notificationTypeCell[data-format='%s'] .bootstrap-switch",
                                        format
                                )
                        )
                ),
                driver
        );
    }
}
