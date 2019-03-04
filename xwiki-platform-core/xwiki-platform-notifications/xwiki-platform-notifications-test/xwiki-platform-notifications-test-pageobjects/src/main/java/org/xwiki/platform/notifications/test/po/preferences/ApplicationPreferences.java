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

import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BootstrapSwitch;
import org.xwiki.test.ui.XWikiWebDriver;

/**
 * Wrap the notification preferences of a given application.
 *
 * @version $Id$
 * @since 9.7RC1
 */
public class ApplicationPreferences extends AbstractNotificationPreferences
{
    private static final String EVENT_TYPE_ROW = "rowEventType";

    private String applicationId;

    private String applicationName;

    private WebElement collapseButton;

    private Map<String, EventTypePreferences> eventTypePreferences = new HashMap();

    /**
     * Construct an ApplicationPreferences.
     *
     * @param webElement table body of the application
     * @param driver the web driver
     */
    public ApplicationPreferences(WebElement webElement, XWikiWebDriver driver)
    {
        super(webElement, driver);
        this.applicationId   = webElement.getAttribute("data-applicationId");
        this.applicationName = webElement.findElement(By.cssSelector("tr > th")).getText();
        this.collapseButton  = webElement.findElement(By.className("collapseButton"));

        for (WebElement element : webElement.findElements(By.className(EVENT_TYPE_ROW))) {
            EventTypePreferences pref = new EventTypePreferences(element, driver);
            eventTypePreferences.put(pref.getEventType(), pref);
        }
    }

    /**
     * @return the application id
     */
    public String getApplicationId()
    {
        return applicationId;
    }

    /**
     * @return the application name
     */
    public String getApplicationName()
    {
        return applicationName;
    }

    /**
     * @return if the application details are collapsed (hidden)
     */
    public boolean isCollapsed()
    {
        return !webElement.findElement(By.className(EVENT_TYPE_ROW)).isDisplayed();
    }

    /**
     * Set if the application details should be collapsed (hidden).
     * @param wantedState expected state
     */
    public void setCollapsed(boolean wantedState)
    {
        while (wantedState != isCollapsed()) {
            this.collapseButton.click();
            driver.waitUntilCondition(webDriver -> wantedState == isCollapsed());
        }
    }

    @Override
    protected BootstrapSwitch getSwitch(String format)
    {
        return new BootstrapSwitch(
                webElement.findElement(
                        By.cssSelector(
                                String.format(
                                        "td.notificationAppCell[data-format='%s'] .bootstrap-switch",
                                        format
                                )
                        )
                ),
                driver
        );
    }

    /**
     * @return a map of preferences for each event type of the application
     */
    public Map<String, EventTypePreferences> getEventTypePreferences()
    {
        return eventTypePreferences;
    }
}
