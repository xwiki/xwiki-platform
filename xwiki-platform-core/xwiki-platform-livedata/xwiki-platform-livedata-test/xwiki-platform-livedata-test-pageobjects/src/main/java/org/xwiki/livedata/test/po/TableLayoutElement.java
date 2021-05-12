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
import java.util.stream.Collectors;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.test.ui.po.BaseElement;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Provides the operations to interact with a Live Data when displayed with a table layout.
 *
 * @version $Id$
 * @since 13.4RC1
 */
public class TableLayoutElement extends BaseElement
{
    /**
     * A matcher for the cell containing links. The matcher assert of a given {@link WebElement} contains a {@code a}
     * tag with the expected text and link.
     */
    private static class CellWithLinkMatcher extends TypeSafeMatcher<WebElement>
    {
        private final String text;

        private final String link;

        /**
         * Initializes the matcher with the expected text and link.
         *
         * @param text the expected text of the {@code a} tag
         * @param link the expected link of the {@code a} tag
         */
        CellWithLinkMatcher(String text, String link)
        {
            this.text = text;
            this.link = link;
        }

        @Override
        protected boolean matchesSafely(WebElement item)
        {
            boolean hasExpectedText = item.getText().equals(this.text);
            // URL equality, possibly adding a trailing / to the passed link if missing.
            String hrefValue = item.findElement(By.tagName("a")).getAttribute("href");
            boolean hasExpectedLink = hrefValue.equals(this.link) || hrefValue.equals(this.link + '/');
            return hasExpectedText && hasExpectedLink;
        }

        @Override
        protected void describeMismatchSafely(WebElement item, Description mismatchDescription)
        {
            mismatchDescription.appendText(item.getAttribute("innerHTML"));
        }

        @Override
        public void describeTo(Description description)
        {
            description.appendText("a link with text ");
            description.appendValue(this.text);
            description.appendText(" and link ");
            description.appendValue(this.link);
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(TableLayoutElement.class);

    private final String liveDataId;

    /**
     * Default constructor. Initializes a live data table layout page object.
     *
     * @param liveDataId the live data id
     */
    public TableLayoutElement(String liveDataId)
    {
        this.liveDataId = liveDataId;
    }

    /**
     * Assert if the a column contains a value.
     *
     * @param columnLabel a column label (for instance {@code Title})
     * @param value the value to be found in the column
     */
    public void assertRow(String columnLabel, String value)
    {
        // TODO: currently only equality of the cell's text value is supported, this might not be convenient for some
        List<String> columnTextValues = getValues(columnLabel)
            .stream()
            .map(WebElement::getText)
            .collect(Collectors.toList());
        assertThat(columnTextValues, hasItem(value));
    }

    /**
     * Assert if the column contains a link with the given name and url.
     *
     * @param columnName the column name
     * @param text the text of the link to be found in the column
     * @param link the href value of the link to be found in the column
     */
    public void assertCellWithLink(String columnName, String text, String link)
    {
        assertThat(getValues(columnName), hasItem(new CellWithLinkMatcher(text, link)));
    }

    /**
     * Waits until the table has content displayed and loaded. Do not use this method if you expect the livedata to be
     * displayed without content.
     */
    public void waitUntilReady()
    {
        // Waits for all the live data to be loaded and the cells to be finished loading.
        getDriver().waitUntilCondition(webDriver -> (!hasLines() || hasLines() && areCellsLoaded()) && noFiltering(),
            20);
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
     * Set the value in the filter of a column and wait for the filtered results to be displayed. See {@link
     * #filterColumn(String, String, boolean)} to filter without waiting.
     *
     * @param columnLabel the label of the column to filter, for instance {@code "Title"}
     * @param content the content to set on the filter
     * @see #filterColumn(String, String, boolean)
     */
    public void filterColumn(String columnLabel, String content)
    {
        filterColumn(columnLabel, content, true);
    }

    /**
     * Set the value in the filter of a column. Waits for the new filtered values to be displayed before continuing when
     * {@code waits} is {@code true}.
     *
     * @param columnLabel the label of the column to filter, for instance {@code "Creation Date"}
     * @param content the content to set on the filter
     * @param wait when {@code true} waits for the filtered results to be displayed before continuing, otherwise
     *     continues without waiting (useful when updating several filters in a row).
     * @see #filterColumn(String, String)
     */
    public void filterColumn(String columnLabel, String content, boolean wait)
    {
        // TODO: adapt for other types of filters
        int columnIndex = findColumnIndex(columnLabel);
        WebElement element = getRoot()
            .findElement(By.cssSelector(String.format(".column-filters > th:nth-child(%d) > input", columnIndex)));
        element.clear();
        element.sendKeys(content);
        if (wait) {
            waitUntilReady();
        }
    }

    /**
     * @return the number of rows currently displayed in the live data
     */
    public int countRows()
    {
        return getRoot().findElements(By.cssSelector("tbody tr td:first-child")).size();
    }

    /**
     * Return the {@link WebElement} of a cell by its row and column numbers. For instance the second row of the first
     * column is {@code (2, 1)}
     *
     * @param columnLabel the label of the column to get, for instance {@code "Title"}
     * @param rowNumber the cell row number to get, starting at 1. For instance the second column has the number 2
     * @return the {@link WebElement} of the requested cell
     */
    public WebElement getCell(String columnLabel, int rowNumber)
    {
        int columnNumber = findColumnIndex(columnLabel);
        return getRoot().findElement(
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
        getRoot().findElement(By.cssSelector(
            String.format("tbody tr:nth-child(%d) [name='%s']", rowNumber, actionName)))
            .click();
    }

    /**
     * Returns a single {@link WebElement} found by passing {@code by} to {@link WebElement#findElement(By)} on the
     * {@link WebElement} of the requested row.
     *
     * @param rowNumber the requested row by its row number, the first row has the number {@code 1}
     * @param by the selector to apply on the row web element
     * @return the {@link WebElement} found on the row
     */
    public WebElement findElementInRow(int rowNumber, By by)
    {
        return getRoot().findElement(By.cssSelector(String.format("tbody tr:nth-child(%d)", rowNumber)))
            .findElement(by);
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
        return getRoot().findElements(By.cssSelector(String.format("tr td:nth-child(%d)", columnIndex)));
    }

    /**
     * @return {@code true} if all the cells of the live data are loaded, {@code false} otherwise
     */
    private boolean areCellsLoaded()
    {
        return getRoot().findElements(By.cssSelector("tbody tr td.cell .xwiki-loader")).isEmpty();
    }

    /**
     * @return {@code true} if the live data contains some result lines, {@code false} otherwise
     */
    private boolean hasLines()
    {
        return !getRoot().findElements(By.cssSelector("tbody tr td.cell .livedata-displayer")).isEmpty();
    }

    /**
     * @return {@code true} if no filters is currently in a filtering state (in other words, when the value currently
     *     set in the filter has not been taken into account), otherwise {@code false}
     */
    private boolean noFiltering()
    {
        return getRoot().findElements(By.cssSelector(".column-filters .livedata-filter.filtering")).isEmpty();
    }

    /**
     * @return the text of the columns currently displayed in the live data
     */
    private List<String> getColumnsLabels()
    {
        return getRoot().findElements(By.cssSelector(".column-name .property-name"))
            .stream()
            .map(WebElement::getText)
            .collect(Collectors.toList());
    }

    private WebElement getRoot()
    {
        return getDriver().findElementById(this.liveDataId);
    }

    private int findColumnIndex(String columnLabel)
    {
        List<WebElement> elements = getRoot().findElements(By.cssSelector("thead tr th .property-name"));
        int index = -1;
        for (int i = 0; i < elements.size(); i++) {
            if (elements.get(i).getText().equals(columnLabel)) {
                index = i + 1;
                break;
            }
        }
        return index;
    }
}
