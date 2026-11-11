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
package org.xwiki.application.test.po;

import org.openqa.selenium.By;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents the Applications.WebHome page.
 *
 * @version $Id$
 * @since 10.8
 *
 */
public class ApplicationIndexHomePage extends ViewPage
{

    /**
     * Go to the home page of the Application Index application.
     *
     * @return the page object for the Application Index home page
     */
    public static ApplicationIndexHomePage gotoPage()
    {
        getUtil().gotoPage("Applications", "WebHome");
        return new ApplicationIndexHomePage();
    }

    private static By applicationElement(String applicationName)
    {
        return By.xpath(
                "//a/span[@class=\"application-label\" and contains(text(), '" + applicationName + "')]");
    }

    /**
     * @param applicationName the name of the application to look for
     * @return {@code true} if the application is listed in the Application Index, {@code false} otherwise
     */
    public boolean containsApplication(String applicationName)
    {
        return getDriver().hasElementWithoutWaiting(applicationElement(applicationName));
    }

    /**
     * Clicks on the given application in the Application Index.
     *
     * @param applicationName the name of the application to click on
     * @return the page object for the application's home page
     */
    public ViewPage clickApplication(String applicationName)
    {
        getDriver().findElementWithoutWaiting(applicationElement(applicationName)).click();
        return new ViewPage();
    }
}
