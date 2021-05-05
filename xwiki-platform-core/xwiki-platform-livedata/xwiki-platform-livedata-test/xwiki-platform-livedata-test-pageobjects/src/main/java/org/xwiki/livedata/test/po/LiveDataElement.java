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

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Live Data page object. Provides the operations to interact with a live data macro.
 *
 * @version $Id$
 * @since 13.4RC1
 */
public class LiveDataElement extends BaseElement
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LiveDataElement.class);

    private final String id;

    /**
     * Lazily initialized the first time {@link #getLiveData()} is called.
     */
    private WebElement livedataRoot;

    /**
     * Default constructor. Initializes a livedata page object by its id.
     *
     * @param id the live data id
     */
    public LiveDataElement(String id)
    {
        this.id = id;
    }

    /**
     * Checks if the a column contains a value.
     *
     * @param columnLabel a column label (for instance {@code Title})
     * @param value the value to be found in the column
     * @return {@code true} if the value is found in the column, {@code false} otherwise
     */
    public boolean hasRow(String columnLabel, String value)
    {
        // TODO: currently only equality of the cell's text value is supported, this might not be convenient for some
        // cells types.
        return getValues(columnLabel)
            .stream()
            .map(WebElement::getText)
            .anyMatch(it -> it.equals(value));
    }

    /**
     * Waits until the table has content displayed and loaded. Do not use this method if you expect the livedata to be
     * displayed without content.
     */
    public void waitUntilHasContentReady()
    {
        // Waits for all the live data to be loaded and the cells to be finished loading.
        getDriver().waitUntilCondition(webDriver -> hasLines() && areCellsLoaded(), 20);
    }

    /**
     * Waits until a minimal number of rows are displayed in the live data.
     *
     * @param minimalExpectedRowCount the minimal number of expected rows
     * @see #waitUntilRowCountGreaterThan(int, int) if you want to define a custom timeout
     */
    public void waitUntilRowCountGreaterThan(int minimalExpectedRowCount)
    {
        waitUntilRowCountGreaterThan(minimalExpectedRowCount, getDriver().getTimeout());
    }

    /**
     * Waits until a minimal number of rows are displayed in the live data.
     *
     * @param minimalExpectedRowCount the minimal number of expected rows
     * @param timeout a custom timeout before stopping the wait and raising an error
     */
    public void waitUntilRowCountGreaterThan(int minimalExpectedRowCount, int timeout)
    {
        getDriver().waitUntilCondition(webDriver -> {
            // Cells are displayed and they are loaded.
            if (!hasLines() || !areCellsLoaded()) {
                return false;
            }
            // And the count of row is greater than the expected count.
            int count = countRows();
            LOGGER.info("LiveTableElement#waitUntilRowCountGreaterThan/refresh(): count = [{}]", count);
            return count >= minimalExpectedRowCount;
        }, timeout);
    }

    /**
     * Waits until a the number of rows displayed in the live data matches the expected count.
     *
     * @param expectedRowCount the number of expected rows
     * @see #waitUntilRowCountEqualsTo(int, int) if you want to define a custom timeout
     */
    public void waitUntilRowCountEqualsTo(int expectedRowCount)
    {
        waitUntilRowCountEqualsTo(expectedRowCount, getDriver().getTimeout());
    }

    /**
     * Waits until a the number of rows displayed in the live data matches the expected count.
     *
     * @param expectedRowCount the number of expected rows
     * @param timeout a custom timeout before stopping the wait and raising an error
     */
    public void waitUntilRowCountEqualsTo(int expectedRowCount, int timeout)
    {
        getDriver().waitUntilCondition(webDriver -> {
            // Cells are displayed. And they are loaded.
            if (!hasLines() || !areCellsLoaded()) {
                return false;
            }
            // And the count of row is greater than the expected count.
            int count = countRows();
            LOGGER.info("LiveTableElement#waitUntilRowCountEqualsTo/refresh(): count = [{}]", count);
            return count == expectedRowCount;
        }, timeout);
    }

    /**
     * Set the value in the filter of a column.
     *
     * @param columnIndex the index of the column to filter, for instance 2 for the second column
     * @param content the content to set on the filter
     */
    public void filterColumn(int columnIndex, String content)
    {
        // .column-filters > th:nth-child(4) > input:nth-child(1)
        // TODO: adapt for other types of filters
        WebElement element = getLiveData()
            .findElement(By.cssSelector(String.format(".column-filters > th:nth-child(%d) > input", columnIndex)));
        element.clear();
        element.sendKeys(content);
    }

    /**
     * @return the number of rows currently displayed in the live data
     */
    public int countRows()
    {
        return getLiveData().findElements(By.cssSelector("tbody tr td:first-child")).size();
    }

    /**
     * Return the {@link WebElement} of a cell by its row and column numbers. For instance the second row of the first
     * column is {@code (2, 1)}
     *
     * @param rowNumber the cell row number to get, starting at 1. For instance the second column has the number 2
     * @param columnNumber the cell column number to get, starting at 1. For instance the third row has the number
     *     3
     * @return the {@link WebElement} of the requested cell
     */
    public WebElement getCell(int rowNumber, int columnNumber)
    {
        return getLiveData().findElement(
            By.cssSelector(String.format("tbody tr:nth-child(%d) td:nth-child(%d)", rowNumber, columnNumber)));
    }

    /**
     * Clicks on an action button identified by its name, on a given row.
     *
     * @param rowNumber the row number, for instance 3 for the third row
     * @param actionName the name of the action button to click on
     */
    public void clickAction(int rowNumber, String actionName)
    {
        getLiveData().findElement(By.cssSelector(
            String.format("tbody tr:nth-child(%d) [name='%s']", rowNumber, actionName)))
            .click();
    }

    /**
     * Returns the column index of the given column. The indexes start at {@code 1}, corresponding to the leftest
     * column.
     *
     * @param columnLabel a column label (for instance {@code Title}).
     * @return the index of the given column
     */
    private int getColumnIndex(String columnLabel)
    {
        return getColumnsLabels().indexOf(columnLabel) + 1;
    }

    /**
     * Gets the values currently displayed in the given column.
     *
     * @param columnLabel a column label (for instance {@code Title})
     * @return the list of values found in the column
     */
    private List<WebElement> getValues(String columnLabel)
    {
        int columnIndex = getColumnIndex(columnLabel);
        return getLiveData().findElements(By.cssSelector(String.format("tr td:nth-child(%d)", columnIndex)));
    }

    /**
     * @return {@code true} if all the cells of the live data are loaded, {@code false} otherwise
     */
    private boolean areCellsLoaded()
    {
        return getLiveData().findElements(By.cssSelector("tbody tr td.cell .xwiki-loader")).isEmpty();
    }

    /**
     * @return {@code true} if the live data contains some result lines, {@code false} otherwise
     */
    private boolean hasLines()
    {
        return !getLiveData().findElements(By.cssSelector("tbody tr td.cell .livedata-displayer")).isEmpty();
    }

    /**
     * @return the text of the columns currently displayed in the live data
     */
    private List<String> getColumnsLabels()
    {
        return getLiveData().findElements(By.cssSelector(".column-name .property-name"))
            .stream()
            .map(WebElement::getText)
            .collect(Collectors.toList());
    }

    /**
     * Get the livedata of this page object. The corresponding {@link WebElement} is initialized on the first call to
     * this method and stored in a field. Subsequents calls to this methods returns the value of the field.
     *
     * @return the livedata of this page object
     */
    @Nonnull
    private WebElement getLiveData()
    {
        if (this.livedataRoot != null) {
            this.livedataRoot = getDriver().findElementById(this.id);
        }
        return Objects.requireNonNull(this.livedataRoot);
    }
}
