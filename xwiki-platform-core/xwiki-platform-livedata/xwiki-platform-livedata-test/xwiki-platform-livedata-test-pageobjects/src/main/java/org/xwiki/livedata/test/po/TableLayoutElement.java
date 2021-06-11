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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.test.ui.po.BaseElement;
import org.xwiki.test.ui.po.FormContainerElement;
import org.xwiki.test.ui.po.SuggestInputElement;

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
    private static final String INNER_HTML_ATTRIBUTE = "innerHTML";

    private static final String CLASS_HTML_ATTRIBUTE = "class";

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
            String hrefAttribute = "href";
            return item.findElements(By.tagName("a"))
                .stream()
                .anyMatch(aTag -> Objects.equals(aTag.getText(), this.text)
                    && (Objects.equals(aTag.getAttribute(hrefAttribute), this.link)
                    || Objects.equals(aTag.getAttribute(hrefAttribute), this.link + '/')));
        }

        @Override
        protected void describeMismatchSafely(WebElement item, Description mismatchDescription)
        {
            mismatchDescription.appendText(item.getAttribute(INNER_HTML_ATTRIBUTE));
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

    private static class WebElementTextMatcher extends TypeSafeMatcher<WebElement>
    {
        private final String value;

        WebElementTextMatcher(String value)
        {
            this.value = value;
        }

        @Override
        protected boolean matchesSafely(WebElement item)
        {
            return item.getText().equals(this.value);
        }

        @Override
        protected void describeMismatchSafely(WebElement item, Description mismatchDescription)
        {
            mismatchDescription.appendText(item.getAttribute(INNER_HTML_ATTRIBUTE));
        }

        @Override
        public void describeTo(Description description)
        {
            description.appendValue(this.value);
        }
    }

    private static class DatePatternMatcher extends TypeSafeMatcher<WebElement>
    {
        private static final String REGEX = "\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}";

        @Override
        protected boolean matchesSafely(WebElement item)
        {
            return item.getText().matches(REGEX);
        }

        @Override
        public void describeTo(Description description)
        {
            description.appendValue(String.format("Regex %s", REGEX));
        }

        @Override
        protected void describeMismatchSafely(WebElement item, Description mismatchDescription)
        {
            mismatchDescription.appendText(item.getAttribute(INNER_HTML_ATTRIBUTE));
        }
    }

    private static final String SELECT_CELLS_BY_COLUMN_INDEX = "tr td:nth-child(%d)";

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
     * Assert if the column contains a value. See {@link #assertRow(String, Matcher)} to use another matcher on the
     * column values.
     *
     * @param columnLabel a column label (for instance {@code Title})
     * @param value the value to be found in the column
     * @see #assertRow(String, Matcher)
     */
    public void assertRow(String columnLabel, String value)
    {
        assertRow(columnLabel, hasItem(getWebElementTextMatcher(value)));
    }

    /**
     * Calls a {@code Matcher} on the column values.
     *
     * @param columnLabel a column label (for instance {@code Title})
     * @param matcher the matcher to apply on the values of the column
     * @see #assertRow(String, String)
     */
    public void assertRow(String columnLabel, Matcher<Iterable<? super WebElement>> matcher)
    {
        assertThat(getValues(columnLabel), matcher);
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
        assertThat(getValues(columnName), hasItem(getWebElementCellWithLinkMatcher(text, link)));
    }

    /**
     * Waits until the table has content displayed and loaded. If you expect the Live Data to be displayed without
     * content, see {@link #waitUntilReady(boolean)}.
     *
     * @see #waitUntilReady(boolean)
     */
    public void waitUntilReady()
    {
        waitUntilReady(true);
    }

    /**
     * Waits until the table has content displayed and loaded. Use {@link #waitUntilReady()} for the default behavior.
     *
     * @param expectRows when {@code true} waits for rows to be displayed and loaded, when {@code false} continue
     *     without waiting for the content
     * @see #waitUntilReady()
     */
    public void waitUntilReady(boolean expectRows)
    {
        // Waits for all the live data to be loaded and the cells to be finished loading.
        getDriver().waitUntilCondition(webDriver -> {
            boolean isWaiting =
                Arrays.asList(getClasses(getRoot().findElement(By.cssSelector(".layout-loader")))).contains("waiting");
            if (isWaiting) {
                return false;
            }
            if (!noFiltering()) {
                return false;
            }
            return !expectRows || hasLines() && areCellsLoaded();
        }, 20);
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
        int columnIndex = findColumnIndex(columnLabel);
        WebElement element = getRoot()
            .findElement(By.cssSelector(String.format(".column-filters > th:nth-child(%d) > input", columnIndex)));

        List<String> classes = Arrays.asList(getClasses(element));
        if (classes.contains("filter-list")) {
            if (element.getAttribute(CLASS_HTML_ATTRIBUTE).contains("selectized")) {
                SuggestInputElement suggestInputElement = new SuggestInputElement(element);
                suggestInputElement.sendKeys(content).selectTypedText();
            } else {
                new Select(element).selectByVisibleText(content);
            }
        } else if (classes.contains("filter-text")) {
            element.clear();
            element.sendKeys(content);
        }

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
     * Set a new value to a field in the nth cell of a column. Waits for the cell to be successfully edited before
     * continuing.
     *
     * @param columnLabel the label of the column
     * @param rowNumber the number of the row to update (the first line is number 1)
     * @param fieldName the name of the field to edit, in other word the name of the corresponding XClass property
     * @param newValue the new value of the field
     */
    public void editCell(String columnLabel, int rowNumber, String fieldName, String newValue)
    {
        internalEdit(columnLabel, rowNumber, fieldName, newValue, () -> {
            // Clicks somewhere outside the edited cell. We use the h1 tag because it is present on all pages.
            new Actions(getDriver().getWrappedDriver()).click(getDriver().findElement(By.tagName("h1"))).perform();
        });
    }

    /**
     * Starts editing a cell, input a value, but cancel the edition (by pressing escape) instead of saving.
     *
     * @param columnLabel the label of the column
     * @param rowNumber the number of the row to update (the first line is number 1)
     * @param fieldName the name of the field to edit, in other word the name of the corresponding XClass property
     * @param newValue the new value set of the field, but never saved because we cancel the edition
     * @since 13.5RC1
     */
    public void editAndCancel(String columnLabel, int rowNumber, String fieldName, String newValue)
    {
        internalEdit(columnLabel, rowNumber, fieldName, newValue, () -> {
            // Press escape to cancel the edition.
            new Actions(getDriver().getWrappedDriver()).sendKeys(Keys.ESCAPE).build().perform();
        });
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
     * Return a hamcrest {@link Matcher} on the text of a {@link WebElement}. For instance the {@link Matcher} will
     * match on the web element containing the following html source {@code <p>Te<i>st</i></p>} for the value {@code
     * "Test"}.
     *
     * @param value the expected value of the text returned by {@link WebElement#getText()}
     * @return a matcher instance
     * @since 13.5RC1
     */
    public Matcher<WebElement> getWebElementTextMatcher(String value)
    {
        return new WebElementTextMatcher(value);
    }

    /**
     * Return a hamcrest {@link Matcher}. This matcher assert that the text of a {@link WebElement} matches the date
     * pattern {@code YYYY/MM/DD HH:mm}.
     *
     * @return 13.5RC1
     */
    public Matcher<WebElement> getDatePatternMatcher()
    {
        return new DatePatternMatcher();
    }

    /**
     * Return a hamcrest {@link Matcher} on the links of a {@link WebElement}. This matcher matches when a link is found
     * on the {@link WebElement} with the expected text and link. For instance, the {@link Matcher} will match on the
     * web element containing the following html source {@code <p>Links: <a href="/path">label</a>, <a
     * href="/path2">label2</a></p>} for the text {@code "label"} and the link {@code "/path"}.
     *
     * @param text the expected text
     * @param link the expected link
     * @return a matcher instance
     * @since 13.5RC1
     */
    public Matcher<WebElement> getWebElementCellWithLinkMatcher(String text, String link)
    {
        return new CellWithLinkMatcher(text, link);
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
        return getRoot().findElements(By.cssSelector(String.format(SELECT_CELLS_BY_COLUMN_INDEX, columnIndex)));
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

    private List<WebElement> getCellsByColumnIndex(int columnIndex)
    {
        return getRoot().findElements(By.cssSelector(String.format(SELECT_CELLS_BY_COLUMN_INDEX, columnIndex)));
    }

    /**
     * Does the steps for the edition of a cell, until the {@code newValue} is set on the requested field. Then call an
     * {@code userAction} (for instance a click outside of the cell, or pressing escape). The {@code userAction} is
     * expected to switch the Live Data back to the view mode (i.e., not cells are edited). Finally,
     * waits for the result of the user action to be completed before continuing.
     *
     * @param columnLabel the label of the column
     * @param rowNumber the number of the row to update (the first line is number 1)
     * @param fieldName the name of the field to edit, in other word the name of the corresponding XClass property
     * @param newValue the new value set of the field, but never saved because we cancel the edition
     * @param userAction an user action to perform after the field values has been set. This action is expected to
     *     bring the Live Data back to the view mode
     */
    private void internalEdit(String columnLabel, int rowNumber, String fieldName, String newValue, Runnable userAction)
    {
        int columnIndex = getColumnIndex(columnLabel);
        WebElement element = getCellsByColumnIndex(columnIndex).get(rowNumber - 1);

        // Double click on the cell.
        new Actions(getDriver().getWrappedDriver()).doubleClick(element).perform();

        // Selector of the edited field.
        By selector = By.cssSelector(String.format("[name$='_%s']", fieldName));

        // Waits for the text input to be displayed.
        getDriver().waitUntilCondition(input -> !element.findElements(selector).isEmpty());

        // Reuse the FormContainerElement to avoid code duplication of the interaction with the form elements 
        // displayed in the live data (they are the same as the one of the inline edit mode).
        new FormContainerElement(By.cssSelector(".livedata-displayer.edit"))
            .setFieldValue(element.findElement(selector), newValue);

        userAction.run();

        // Waits for the field to be reload before continuing.
        getDriver().waitUntilCondition(input -> {
            // Nothing is loading.
            boolean noLoader = element.findElements(By.cssSelector(".xwiki-loader")).isEmpty();
            // And the edited field is not displayed anymore.
            boolean noInput = element.findElements(selector).isEmpty();
            return noLoader && noInput;
        });
    }

    private String[] getClasses(WebElement element)
    {
        return element.getAttribute(CLASS_HTML_ATTRIBUTE).split("\\s+");
    }
}
