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
    private List<WebElement> results;

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
        getResults().get(index).findElement(By.className("panel-title")).click();
    }

    /**
     * @param index the index of the result to retrieve the detailed message of (the first result is index 0)
     * @return the detailed message of the nth result on the current pre-check page
     * @see #toggleDetailedMessage(int) to make the detailed message visible
     */
    public String getDetailedMessage(int index)
    {
        return getResults().get(index).findElement(By.className("panel-body")).getText();
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

    /**
     * @return the root element of the required rights pre-check results
     */
    private WebElement getRoot()
    {
        return getDriver().findElementWithoutWaiting(By.className("required-rights-results"));
    }
}
