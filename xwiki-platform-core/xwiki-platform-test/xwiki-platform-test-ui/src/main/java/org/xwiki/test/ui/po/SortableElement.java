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
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;

/**
 * Represents a sortable element that is using, e.g., the "sortable" plugin of "jquery-ui".
 *
 * @version $Id$
 * @since 15.6RC1
 * @since 15.5.1
 * @since 14.10.15
 */
public class SortableElement extends BaseElement
{
    private final WebElement container;

    /**
     * Creates a new instance.
     *
     * @param container the element that wraps the sortable elements
     */
    public SortableElement(WebElement container)
    {
        super();

        this.container = container;
    }

    /**
     * Move a given element before another one.
     *
     * @param toMove the selector for the element to move inside the sortable
     * @param before the selector for the element before which the element to move should be moved
     */
    public void moveBefore(By toMove, By before)
    {
        WebElement elementToMove = this.container.findElement(toMove);
        WebElement elementBefore = this.container.findElement(before);
        Point target = elementBefore.getLocation();
        Point source = elementToMove.getLocation();

        // The drag & drop of the "sortable" plugin of "jquery-ui" is very sensitive so we need to script the
        // moves of the mouse precisely if we don't want to have flickers.

        // First, we click and hold the item we want to move.
        Actions actions = getDriver().createActions().clickAndHold(elementToMove);
        // Then we move into the position of the targeted item so jquery-ui can register we want to take its place.
        actions.moveByOffset(target.getX() - source.getX(), target.getY() - source.getY());
        // Now we do a little move on top left so jquery-ui understand we want to be *before* the other item and
        // put a blank place instead of the other app.
        actions.moveByOffset(-4, -4);
        actions.perform();

        // Before releasing the click, check that jquery-ui has moved the other item to let the place free.
        getDriver().waitUntilCondition((ExpectedCondition<Object>) webDriver -> {
            Point newTarget = elementBefore.getLocation();
            Point newSource = elementToMove.getLocation();
            return newTarget.getX() > newSource.getX() + 5 || newTarget.getY() > newSource.getY() + 5;
        });

        // Now we can release the selection
        actions = new Actions(getDriver().getWrappedDriver());
        actions.release();
        actions.perform();
    }
}
