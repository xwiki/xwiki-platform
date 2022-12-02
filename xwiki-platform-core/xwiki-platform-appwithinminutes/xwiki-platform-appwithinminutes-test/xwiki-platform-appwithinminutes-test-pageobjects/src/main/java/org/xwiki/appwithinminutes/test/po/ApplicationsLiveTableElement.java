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
package org.xwiki.appwithinminutes.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.ConfirmationPage;
import org.xwiki.test.ui.po.LiveTableElement;

/**
 * Represents the live table that lists the existing applications on the AppWithinMinutes home page.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class ApplicationsLiveTableElement extends LiveTableElement
{
    /**
     * Identifies the application name filter input.
     */
    private static final String APP_NAME_FILTER_ID = "xwiki-livetable-livetable-filter-1";

    /**
     * The title of the live table column that displays the application name.
     */
    private static final String APP_NAME_COLUMN_TITLE = "Application";

    /**
     * Creates a new instance.
     */
    public ApplicationsLiveTableElement()
    {
        super("livetable");
    }

    /**
     * @param appName the name of an application
     * @return {@code true} if the specified application is listed, {@code false} otherwise
     */
    public boolean isApplicationListed(String appName)
    {
        return hasRow(APP_NAME_COLUMN_TITLE, appName);
    }

    public ApplicationHomePage viewApplication(String appName)
    {
        String escapedAppName = getUtil().escapeXPath(appName);
        String xpath = "//td[contains(@class, 'doc_title')]/a[. = " + escapedAppName + "]";
        getDriver().findElementWithoutWaiting(By.xpath(xpath)).click();
        return new ApplicationHomePage();
    }

    /**
     * Clicks on the link to delete the specified application.
     * 
     * @param appName the name of the application to delete
     */
    public ConfirmationPage clickDeleteApplication(String appName)
    {
        clickAction(appName, "delete");
        return new ConfirmationPage();
    }

    /**
     * @param appName the name of an application
     * @return {@code true} if the delete link is displayed for the specified application
     */
    public boolean canDeleteApplication(String appName)
    {
        return hasAction(appName, "delete");
    }

    /**
     * Clicks on the link to edit the specified application.
     * 
     * @param appName the name of the application to delete.
     */
    public ApplicationClassEditPage clickEditApplication(String appName)
    {
        clickAction(appName, "edit");
        return new ApplicationClassEditPage();
    }

    /**
     * @param appName the name of an application
     * @return {@code true} if the edit link is displayed for the specified application
     */
    public boolean canEditApplication(String appName)
    {
        return hasAction(appName, "edit");
    }

    /**
     * Clicks one of the action links corresponding to the specified application.
     * 
     * @param appName the action target
     * @param action the action name
     */
    protected void clickAction(String appName, String action)
    {
        String escapedAppName = appName.replace("\\", "\\\\").replace("'", "\\'");
        String actionLinkXPath =
            "//tr[td[contains(@class, 'doc_title') and . = '" + escapedAppName
                + "']]/td[@class = 'actions']//a[contains(@class, 'action" + action + "')]";
        WebElement liveTableBody = getDriver().findElement(By.id("livetable-display"));
        liveTableBody.findElement(By.xpath(actionLinkXPath)).click();
    }

    /**
     * @return {@code true} if the given action is listed for the specified application, {@code false} otherwise
     */
    protected boolean hasAction(String appName, String action)
    {
        String escapedAppName = appName.replace("\\", "\\\\").replace("'", "\\'");
        String actionLinkXPath =
            "//tr[td[contains(@class, 'doc_title') and . = '" + escapedAppName
                + "']]/td[@class = 'actions']//a[contains(@class, 'action" + action + "')]";
        WebElement liveTableBody = getDriver().findElement(By.id("livetable-display"));
        // Don't wait as this needs significant time when the action doesn't exist and actions should be available
        // after waiting for the LiveTable to be ready.
        return !getDriver().findElementsWithoutWaiting(liveTableBody, By.xpath(actionLinkXPath)).isEmpty();
    }

    /**
     * Filters by application name.
     * 
     * @param appNameFilter the string to filter the application names with
     */
    public void filterApplicationName(String appNameFilter)
    {
        super.filterColumn(APP_NAME_FILTER_ID, appNameFilter);
    }

    public String getApplicationNameFilter()
    {
        return super.getFilterValue(APP_NAME_FILTER_ID);
    }
}
