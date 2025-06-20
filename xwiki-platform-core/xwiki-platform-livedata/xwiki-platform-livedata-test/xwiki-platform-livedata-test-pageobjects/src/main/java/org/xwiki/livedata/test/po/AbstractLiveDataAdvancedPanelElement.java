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

package org.xwiki.livedata.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.InvalidElementStateException;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Abstract class representing different type of advanced panels in livedata.
 *
 * @version $Id$
 * @since 16.3.0RC1
 */
public abstract class AbstractLiveDataAdvancedPanelElement extends BaseElement
{
    protected final WebElement container;
    protected final LiveDataElement liveData;

    /**
     * Default constructor.
     * @param liveData the livedata the panel belongs to.
     * @param container the container of the panel.
     */
    public AbstractLiveDataAdvancedPanelElement(LiveDataElement liveData, WebElement container)
    {
        this.liveData = liveData;
        this.container = container;
    }

    /**
     * Close the panel.
     */
    public void closePanel()
    {
        getDriver().findElementWithoutWaiting(this.container, By.className("close-button")).click();
    }

    /**
     * Allow to click on links that are only visible when the mouse is on them (e.g. delete icons of filters)
     * @param linkIdentifiers the identifier to find the link element in the dom.
     */
    protected void clickOnMouseOverLinks(By linkContainer, By linkIdentifiers)
    {
        getDriver().findElementsWithoutWaiting(this.container, linkContainer)
            .forEach(webElement -> {
                // First we move to the link container to make the link appearing
                getDriver().createActions().moveToElement(webElement).build().perform();
                WebElement linkElement = getDriver().findElementWithoutWaiting(webElement, linkIdentifiers);
                if (!linkElement.isDisplayed()) {
                    throw new InvalidElementStateException(String.format("The link [%s] should be displayed.",
                        linkElement));
                }
                // Click on the actual link
                getDriver().createActions().moveToElement(linkElement).click().build().perform();
            });
    }
}
