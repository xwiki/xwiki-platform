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

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents the actions possible on the App Within Minutes home page.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class AppWithinMinutesHomePage extends ViewPage
{
    @FindBy(xpath = "//a[@class = 'button' and . = 'Create Application']")
    private WebElement createAppButton;

    /**
     * The live table that lists the existing applications.
     */
    private ApplicationsLiveTableElement appsLiveTable = new ApplicationsLiveTableElement();

    /**
     * Opens the App Within Minutes home page.
     * 
     * @return the App Within Minutes home page
     */
    public static AppWithinMinutesHomePage gotoPage()
    {
        getUtil().gotoPage("AppWithinMinutes", "WebHome");
        return new AppWithinMinutesHomePage();
    }

    /**
     * @return the URL of the App Within Minutes home page
     */
    public String getURL()
    {
        return getUtil().getURL("AppWithinMinutes", "WebHome");
    }

    /**
     * Clicks on the Create Application button.
     * 
     * @return the page that represents the first step of the App Within Minutes wizard
     */
    public ApplicationCreatePage clickCreateApplication()
    {
        createAppButton.click();
        return new ApplicationCreatePage();
    }

    /**
     * @return the live table that list existing applications
     */
    public ApplicationsLiveTableElement getAppsLiveTable()
    {
        this.appsLiveTable.waitUntilReady();
        return this.appsLiveTable;
    }

    /**
     * Delete the specified application.
     *
     * @param appName the application name
     * @return this page
     */
    public AppWithinMinutesHomePage deleteApplication(String appName)
    {
        this.appsLiveTable.waitUntilReady();
        this.appsLiveTable.filterApplicationName(appName);
        if (this.appsLiveTable.isApplicationListed(appName)) {
            getAppsLiveTable().clickDeleteApplication(appName).clickYes();
        }
        return this;
    }
}
