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
package org.xwiki.security.requiredrights.test.po;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Page object for the Required Rights pre-check message.
 *
 * @version $Id$
 * @since 15.9RC1
 */
public class RequiredRightsPreEditCheckElement extends BaseElement
{
    private static final String PANEL_BODY_CLASS = "panel-body";

    private List<WebElement> results;

    private List<WebElement> titles;

    /**
     * @return the number of results displayed on the current pre-check page
     */
    public int count()
    {
        return getResults().size();
    }

    /**
     * @param index the index of the result to retrieve the title of (the first result is index 0)
     * @return the title of the nth result on the current pre-check page
     */
    public String getSummary(int index)
    {
        return getResults().get(index).findElement(By.className("panel-heading")).getText();
    }

    /**
     * Toggle the detailed message of the nth result by clicking on the title.
     *
     * @param index the index of the result to toggle (the first result is index 0)
     */
    public void toggleDetailedMessage(int index)
    {
        WebElement result = getResults().get(index);
        result.findElement(By.className("panel-title")).click();
        getDriver().waitUntilElementIsVisible(result, By.className("panel-collapse"));
    }

    /**
     * @param index the index of the result to retrieve the detailed message of (the first result is index 0)
     * @return the detailed message of the nth result on the current pre-check page
     * @see #toggleDetailedMessage(int) to make the detailed message visible
     */
    public String getDetailedMessage(int index)
    {
        return getResults().get(index).findElement(By.className(PANEL_BODY_CLASS)).getText();
    }

    /**
     * Expand/Fold the details.
     *
     * @return the current object
     * @since 15.10RC1
     */
    public RequiredRightsPreEditCheckElement toggleDetails()
    {
        getDriver().findElement(By.className("required-rights-advanced-toggle")).click();
        return this;
    }

    /**
     * Waits for the content of the given detailed message to be equal to the provided expected value. This is required
     * as calling get text can return partial value when the toggle transition from {@link #toggleDetails()} is not
     * finished when calling {@link WebElement#getText()} on the panel body.
     *
     * @param index the index of the result to retrieve the detailed message of (the first result is index 0)
     * @param expectedMessage the expected detailed message of the expected message
     * @since 15.10RC1
     */
    public void waitForDetailedMessage(int index, String expectedMessage)
    {
        getDriver()
            .waitUntilElementHasTextContent(() -> getResults().get(index).findElement(By.className(PANEL_BODY_CLASS)),
                expectedMessage);
    }

    /**
     * @param index the index of the title (the first result is index 0)
     * @return the href element of the title link
     * @since 16.0.0RC1
     * @since 15.10.5
     */
    public String getTitleHref(int index)
    {
        return getTitles().get(index).findElement(By.cssSelector("a")).getAttribute("href");
    }

    /**
     * Get the list of results and store them in {@link #results} the first time the method is called.
     *
     * @return the list of results
     */
    private List<WebElement> getResults()
    {
        if (this.results == null) {
            this.results = getRoot().findElements(By.className("panel-group"));
        }
        return this.results;
    }

    private List<WebElement> getTitles()
    {
        if (this.titles == null) {
            this.titles = getRoot().findElements(By.cssSelector("h3.group-title"));
        }
        return this.titles;
    }

    /**
     * @return the root element of the required rights pre-check results
     */
    private WebElement getRoot()
    {
        return getDriver().findElementWithoutWaiting(By.className("required-rights-results"));
    }
}
