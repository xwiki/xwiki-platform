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
package org.xwiki.test.ui.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * Represents the Drawer Menu available on all pages.
 *
 * @version $Id$
 * @since 15.2RC1
 */
public class DrawerMenu extends BaseElement
{
    @FindBy(id = "xwikimaincontainer")
    private WebElement mainContainerDiv;

    @FindBy(id = "tmDrawerActivator")
    private WebElement activator;
    
    @FindBy(id = "tmDrawer")
    private WebElement drawerContainer;

    public void toggle()
    {
        if (isVisible()) {
            hide();
        } else {
            show();
        }
    }

    public boolean isVisible()
    {
        return this.drawerContainer.isDisplayed();
    }

    public boolean show()
    {
        if (!isVisible()) {
            // Open the drawer.
            this.activator.click();
            waitForDrawer(true);
            return true;
        }
        return false;
    }

    /**
     * @return true if the drawer used to be displayed
     */
    public boolean hide()
    {
        if (isVisible()) {
            // Close the drawer by clicking outside.
            // We don't perform directly a click since it could lead to a
            // org.openqa.selenium.ElementClickInterceptedException because of a drawer-overlay above it.
            // The click through action is performed with a move and click, which is what we really want.
            getDriver().createActions().click(this.mainContainerDiv).perform();
            waitForDrawer(false);
            return true;
        }
        return false;
    }

    /**
     * Click the passed menu entry link.
     *
     * @param entryName the displayed name of the entry link
     */
    public void clickEntry(String entryName)
    {
        show();
        getDriver().findElement(By.xpath(getEntryXPath(entryName))).click();
    }

    /**
     * @param entryName the displayed name of the entry link
     * @return true if the entry exists or false otherwise
     */
    public boolean hasEntry(String entryName)
    {
        show();
        return getDriver().findElementsWithoutWaiting(By.xpath(getEntryXPath(entryName))).size() > 0;
    }

    private String getEntryXPath(String entryName)
    {
        return String.format("//a[contains(@class, 'drawer-menu-item')]/*[text() = \"%s\"]", entryName);
    }

    private void waitForDrawer(boolean visible)
    {
        // We always want the transition to be ended in the wait condition.
        ExpectedCondition<Boolean> transitionEnded = ExpectedConditions.not(
            ExpectedConditions.attributeContains(this.drawerContainer, "class", "drawer-transitioning")
        );
        if (visible) {
            getDriver().waitUntilCondition(ExpectedConditions.and(
                ExpectedConditions.visibilityOf(this.drawerContainer),
                transitionEnded
            ));
        } else {
            getDriver().waitUntilCondition(ExpectedConditions.and(
                ExpectedConditions.invisibilityOf(this.drawerContainer),
                transitionEnded
            ));
        }
    }
}
