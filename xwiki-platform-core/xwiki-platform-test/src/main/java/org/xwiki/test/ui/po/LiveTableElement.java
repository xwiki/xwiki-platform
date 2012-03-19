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

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;

/**
 * Represents the actions possible on a livetable.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class LiveTableElement extends BaseElement
{
    private String livetableId;

    public LiveTableElement(String livetableId)
    {
        this.livetableId = livetableId;
    }

    /**
     * @return if the livetable has finished displaying and is ready for service
     */
    public boolean isReady()
    {
        Object result = executeJavascript("return Element.hasClassName('" + livetableId + "-ajax-loader','hidden')");
        return result instanceof Boolean ? (Boolean) result : false;
    }

    /**
     * Wait till the livetable has finished displaying all its rows (so that we can then assert the livetable content
     * without running the risk that the rows have not been updated yet).
     */
    public void waitUntilReady()
    {
        long t1 = System.currentTimeMillis();
        while ((System.currentTimeMillis() - t1 < getUtil().getTimeout() * 1000L)) {
            if (isReady()) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Do nothing just break out
                break;
            }
        }
        getUtil().takeScreenshot();
        throw new TimeoutException("Livetable isn't ready after the timeout has expired. Source ["
            + getDriver().getPageSource() + "]");
    }

    public boolean hasColumn(String columnTitle)
    {
        List<WebElement> elements =
            getUtil().findElementsWithoutWaiting(
                getDriver(),
                By.xpath("//th[contains(@class, 'xwiki-livetable-display-header-text') and normalize-space(.) = '"
                    + columnTitle + "']"));
        return elements.size() > 0;
    }

    public void filterColumn(String inputId, String filterValue)
    {
        // Make extra sure selenium can't go quicker than the livetable status by forcing it before filtering
        executeJavascript("return $('" + livetableId + "-ajax-loader').removeClassName('hidden')");

        WebElement element = getDriver().findElement(By.id(inputId));
        element.sendKeys(filterValue);
        waitUntilReady();
    }

    /**
     * @param inputId the filter input identifier
     * @return the value of the filter input for the specified column
     * @see #filterColumn(String, String)
     */
    public String getFilterValue(String inputId)
    {
        return getDriver().findElement(By.id(inputId)).getAttribute("value");
    }

    /**
     * @param columnTitle the title of live table column
     * @return the 0-based index of the specified column
     */
    public int getColumnIndex(String columnTitle)
    {
        WebElement liveTable = getDriver().findElement(By.id(livetableId));

        String escapedColumnTitle = columnTitle.replace("\\", "\\\\").replace("'", "\\'");
        String columnXPath = "//thead[@class = 'xwiki-livetable-display-header']//th[normalize-space(.) = '%s']";
        WebElement column = liveTable.findElement(By.xpath(String.format(columnXPath, escapedColumnTitle)));

        return ((Long) getDriver().executeScript("return arguments[0].cellIndex;", column)).intValue();
    }

    /**
     * Checks if there is a row that has the given value for the specified column.
     * 
     * @param columnTitle the title of live table column
     * @param columnValue the value to match rows against
     * @return {@code true} if there is a row that matches the given value for the specified column, {@code false}
     *         otherwise
     */
    public boolean hasRow(String columnTitle, String columnValue)
    {
        String escapedColumnValue = columnValue.replace("\\", "\\\\").replace("'", "\\'");
        String cellXPath = "//tr/td[position() = %s and . = '%s']";

        WebElement liveTableBody = getDriver().findElement(By.id(livetableId + "-display"));
        return liveTableBody.findElements(
            By.xpath(String.format(cellXPath, getColumnIndex(columnTitle) + 1, escapedColumnValue))).size() > 0;
    }

    /**
     * @since 3.2M3
     */
    public void waitUntilRowCountGreaterThan(final int minimalExpectedRowCount)
    {
        final By by = By.xpath("//tbody[@id = '" + this.livetableId + "-display']//tr");
        getUtil().waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            public Boolean apply(WebDriver driver)
            {
                return driver.findElements(by).size() >= minimalExpectedRowCount;
            }
        });
    }
}
