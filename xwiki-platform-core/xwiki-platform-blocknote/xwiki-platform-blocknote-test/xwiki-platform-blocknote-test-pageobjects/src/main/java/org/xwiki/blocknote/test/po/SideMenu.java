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
package org.xwiki.blocknote.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the side menu displayed next to the block hovered in the BlockNote rich text area.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
public class SideMenu extends BaseElement
{
    private static final By SIDE_MENU = By.className("bn-side-menu");

    private final WebElement container;

    /**
     * Waits for the side menu of the specified block to be displayed at its final position.
     *
     * @param hoveredBlock the block the side menu belongs to, which is hovered again while waiting
     */
    public SideMenu(WebElement hoveredBlock)
    {
        waitUntilStable(hoveredBlock);
        this.container = getDriver().findElement(SIDE_MENU);
    }

    /**
     * @return the side menu container element
     */
    public WebElement getContainer()
    {
        return this.container;
    }

    private void waitUntilStable(WebElement hoveredBlock)
    {
        // The side menu is faded in and positioned asynchronously, so waiting for it to be visible is not enough to
        // avoid flaky screenshots: we wait for it to be at the same position for two consecutive ticks. We hover the
        // block again on each tick because the editor removes the side menu as soon as it considers that the mouse
        // left the block, which happens on its own when the block is re-rendered. We move in two steps so that a
        // mouse move event is fired even when the mouse is already at the target point.
        Point[] previousPosition = new Point[] {null};
        getDriver().waitUntilCondition(driver -> {
            getDriver().createActions().moveToElement(hoveredBlock, 1, 1).moveToElement(hoveredBlock).perform();
            Point position = getPosition();
            boolean stable = position != null && position.equals(previousPosition[0]);
            previousPosition[0] = position;
            return stable;
        });
    }

    /**
     * @return the position of the side menu, or {@code null} if it is not displayed (anymore)
     */
    private Point getPosition()
    {
        try {
            WebElement sideMenu = getDriver().findElementWithoutWaiting(SIDE_MENU);
            return sideMenu.isDisplayed() ? sideMenu.getLocation() : null;
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            // The editor has just removed or re-created the side menu, so it's not stable yet.
            return null;
        }
    }
}
