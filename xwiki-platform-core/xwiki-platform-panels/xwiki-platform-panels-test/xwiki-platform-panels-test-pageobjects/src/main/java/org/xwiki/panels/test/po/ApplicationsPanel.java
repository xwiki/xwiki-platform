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
package org.xwiki.panels.test.po;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents actions for the Panels.Applications panel.
 *
 * @version $Id$
 * @since 4.3M2
 */
public class ApplicationsPanel extends ViewPage
{
    /**
     * @return the page representing the Application Panels page
     */
    public static ApplicationsPanel gotoPage()
    {
        getUtil().gotoPage("Panels", "Applications");
        return new ApplicationsPanel();
    }

    public static String getURL()
    {
        return getUtil().getURL("Panels", "Applications");
    }

    public boolean containsApplication(String applicationName)
    {
        return getDriver().findElementsWithoutWaiting(By.xpath(
            "//a/span[@class=\"application-label\" and contains(text(), '" + applicationName + "')]")).size() == 1;
    }

    public ViewPage clickApplication(String applicationName)
    {
        getDriver().findElementWithoutWaiting(By.xpath(
            "//a/span[@class=\"application-label\" and contains(text(), '" + applicationName + "')]")).click();
        return new ViewPage();
    }

    public List<String> getApplications()
    {
        List<String> applications = new ArrayList<>();

        for (WebElement elem : getDriver().findElementsWithoutWaiting(By.cssSelector(".application-label"))) {
            if (elem.isDisplayed()) {
                applications.add(elem.getText());
            }
        }

        return applications;
    }

    /**
     * Expands the "More applications" list of the panel, so that its entries (e.g. the AppWithinMinutes "Create your
     * own!" entry) can be clicked.
     *
     * @return this panel
     * @since 18.9.0RC1
     */
    public ApplicationsPanel clickMoreApplications()
    {
        WebElement moreButton = getDriver().findElementWithoutWaiting(By.className("applicationPanelMoreButton"));
        moreButton.click();
        getDriver().waitUntilCondition(driver -> "true".equals(moreButton.getDomAttribute("aria-expanded")));
        return this;
    }

    /**
     * @param applicationName the label of the application entry
     * @return the HTML of the icon displayed for the specified application entry
     * @since 18.9.0RC1
     */
    public String getApplicationIcon(String applicationName)
    {
        return getDriver().findElementWithoutWaiting(By.xpath("//a[span[@class=\"application-label\" and "
            + "contains(text(), '" + applicationName + "')]]/span[@class=\"application-img\"]"))
            .getDomProperty("innerHTML").trim();
    }
}
