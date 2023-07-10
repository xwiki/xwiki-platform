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
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.WebElement;

/**
 * Represents the actions possible on the date range picker widget.
 *
 * @version $Id$
 * @since 15.5
 * @since 14.10.14
 */
public class DateRangePicker extends BaseElement
{
    private WebElement container;

    /**
     * Get the date range picker corresponding to the given element.
     *
     * @param element an element that has a date range picker assigned
     */
    public DateRangePicker(WebElement element)
    {
        this.container = (WebElement) ((JavascriptExecutor) getDriver()).executeScript(
            "return jQuery(arguments[0]).data('daterangepicker').container[0];", element);
        waitUntilReady();
    }

    /**
     * Apply the selected range.
     */
    public void applyRange()
    {
        this.container.findElement(By.className("applyBtn")).click();
    }

    /**
     * Clear the selected range.
     */
    public void clearRange()
    {
        this.container.findElement(By.className("cancelBtn")).click();
    }

    /**
     * Wait for the widget to appear.
     */
    private DateRangePicker waitUntilReady()
    {
        getDriver().waitUntilCondition(driver -> {
            try {
                return container.isDisplayed();
            } catch (NotFoundException e) {
                return false;
            }
        });
        return this;
    }
}
