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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.test.ui.po.BaseElement;
import org.xwiki.test.ui.po.DateRangePicker;
import org.xwiki.test.ui.po.FormContainerElement;
import org.xwiki.test.ui.po.SuggestInputElement;
import org.xwiki.text.StringUtils;

import static java.util.Collections.singletonList;
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
     * Option for {@link #filterColumn(String, String, boolean, Map)} to wait for selectize fields suggestions before
     * continuing. The default behavior being to use the typed text as the selected value without waiting.
     */
    public static final String FILTER_COLUMN_SELECTIZE_WAIT_FOR_SUGGESTIONS = "selectized.waitForSuggestions";

    private static final String INNER_HTML_ATTRIBUTE = "innerHTML";

    private static final String CLASS_HTML_ATTRIBUTE = "class";

    private final LiveDataElement liveData;

    private static final Pattern PAGINATION_SENTENCE_PATTERN =
        Pattern.compile("^Entries (?<firstEntry>\\d+) - (?<lastEntry>\\d+) out of (?<totalEntries>\\d+)$");

    /**
     * @return the list of rows {@link WebElement}s
     * @since 16.1.0RC1
     */
    public List<WebElement> getRows()
    {
        return getDriver().findElementsWithoutWaiting(getRoot(), By.cssSelector("tbody > tr"));
    }

    /**
     * A matcher for the cell containing links. The matcher assert of a given {@link WebElement} contains a {@code a}
     * tag with the expected text and link.
     */
    private static class CellWithLinkMatcher extends TypeSafeMatcher<WebElement>
    {
        private final String text;

        private final String link;

        private final URI linkUri;

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
            this.linkUri = URI.create(this.link);
        }

        @Override
        protected boolean matchesSafely(WebElement item)
        {
            String hrefAttribute = "href";
            return item.findElements(By.tagName("a"))
                .stream()
                .anyMatch(aTag -> {
                    String href = aTag.getAttribute(hrefAttribute);
                    return Objects.equals(aTag.getText(), this.text)
                        && (Objects.equals(linkUri, URI.create(href))
                        || Objects.equals(linkUri, removeTrailingSlash(href)));
                });
        }

        private URI removeTrailingSlash(String href)
        {
            URI initialUri = URI.create(href);
            URIBuilder uriBuilder = new URIBuilder(initialUri);
            List<String> pathSegments = uriBuilder.getPathSegments();
            URI uri;
            if (pathSegments.get(pathSegments.size() - 1).equals("")) {
                pathSegments.remove(pathSegments.size() - 1);
                try {
                    uri = uriBuilder.setPathSegments(pathSegments).build();
                } catch (URISyntaxException e) {
                    uri = initialUri;
                }
            } else {
                uri = initialUri;
            }

            return uri;
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

    private static final class DatePatternMatcher extends TypeSafeMatcher<WebElement>
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

    /**
     * Default constructor. Initializes a live data table layout page object.
     *
     * @param liveData the live data object corresponding to this table layout
     * @since 15.5
     */
    public TableLayoutElement(LiveDataElement liveData)
    {
        this.liveData = liveData;
    }

    /**
     * Assert if the column contains a value. See {@link #assertRow(String, Matcher)} to use another matcher on the
     * column values.
     *
     * @param columnLabel a column label (for instance {@code Title})
     * @param value the value to be found in the column
     * @see #assertRow(String, Matcher)
     * @since 12.10.9
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
     * @since 13.5RC1
     * @since 13.4.1
     * @since 12.10.9
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
     * Assert if the column contains a delete action.
     *
     * @param columnName the column name
     * @param entityReference the entity reference subject of the delete action
     * @since 13.5RC1
     */
    public void assertCellWithDeleteAction(String columnName, EntityReference entityReference)
    {
        assertCellWithLink(columnName, "Delete", urlWithoutFormToken(entityReference, "delete"));
    }

    /**
     * Assert if the column contains an edit action.
     *
     * @param columnName the column name
     * @param entityReference the entity reference subject of the edit action
     * @since 13.5RC1
     */
    public void assertCellWithEditAction(String columnName, EntityReference entityReference)
    {
        assertCellWithLink(columnName, "Edit", urlWithoutFormToken(entityReference, "edit"));
    }

    /**
     * Assert if the column contains an edit action.
     *
     * @param columnName the column name
     * @param entity the entity reference subject of the edit action
     * @since 13.5RC1
     */
    public void assertCellWithCopyAction(String columnName, EntityReference entity)
    {
        assertCellWithLink(columnName, "Copy", getUtil().getURL(entity, "view", "xpage=copy"));
    }

    /**
     * Assert if the column contains an edit action.
     *
     * @param columnName the column name
     * @param entityReference the entity reference subject of the edit action
     * @since 13.5RC1
     */
    public void assertCellWithRenameAction(String columnName, EntityReference entityReference)
    {
        assertCellWithLink(columnName, "Rename", getUtil().getURL(entityReference, "view", "xpage=rename&step=1"));
    }

    /**
     * Assert if the column contains an edit action.
     *
     * @param columnName the column name
     * @param entityReference the entity reference subject of the edit action
     * @since 13.5RC1
     */
    public void assertCellWithRightsAction(String columnName, EntityReference entityReference)
    {
        DocumentReference webPreferencesReference = new DocumentReference("WebPreferences",
            (SpaceReference) entityReference.extractReference(EntityType.SPACE));
        assertCellWithLink(columnName, "Rights",
            urlWithoutFormToken(
                getUtil().getURL(webPreferencesReference, "admin", "editor=spaceadmin&section=PageRights")));
    }

    /**
     * Waits until the table has content displayed and loaded. If you expect the Live Data to be displayed without
     * content, see {@link #waitUntilReady(boolean)}.
     *
     * @see #waitUntilReady(boolean)
     * @since 12.10.9
     */
    public void waitUntilReady()
    {
        waitUntilReady(true);
    }

    /**
     * Waits until the table is loaded. Use {@link #waitUntilReady()} for the default behavior.
     *
     * @param expectRows when {@code true} waits for rows to be loaded, when {@code false} continue
     *     without waiting for the content
     * @see #waitUntilReady()
     * @since 12.10.9
     */
    public void waitUntilReady(boolean expectRows)
    {
        // Waits for all the live data to be loaded and the cells to be finished loading.
        getDriver().waitUntilCondition(webDriver -> {
            WebElement root;
            try {
                root = getRoot();
            } catch (StaleElementReferenceException e) {
                // If the root element is stale, this means Vue is mounting itself and the table is not ready yet.
                return false;
            }
            List<String> layoutLoaderClasses =
                Arrays.asList(getClasses(root.findElement(By.cssSelector(".layout-loader"))));
            boolean isWaiting = layoutLoaderClasses.contains("waiting");
            if (isWaiting) {
                return false;
            }
            if (!noFiltering()) {
                return false;
            }
            return !expectRows || !layoutLoaderClasses.contains("loading") && areCellsLoaded();
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
            if (isEmpty() || !areCellsLoaded()) {
                return false;
            }
            // And the count of row is greater than the expected count.
            int count = countRows();
            LOGGER.info("TableLayoutElement#waitUntilRowCountGreaterThan/refresh(): count = [{}]", count);
            return count >= minimalExpectedRowCount;
        }, timeout);
    }

    /**
     * Waits until the number of rows displayed in the live data matches the expected count.
     *
     * @param expectedRowCount the number of expected rows
     * @see #waitUntilRowCountEqualsTo(int, int) if you want to define a custom timeout
     */
    public void waitUntilRowCountEqualsTo(int expectedRowCount)
    {
        waitUntilRowCountEqualsTo(expectedRowCount, getDriver().getTimeout());
    }

    /**
     * Waits until the number of rows displayed in the live data matches the expected count.
     *
     * @param expectedRowCount the number of expected rows
     * @param refresh when {@code true}, the Live Data is reloaded before each count
     * @see #waitUntilRowCountEqualsTo(int, int) if you want to define a custom timeout
     */
    public void waitUntilRowCountEqualsTo(int expectedRowCount, boolean refresh)
    {
        waitUntilRowCountEqualsTo(expectedRowCount, getDriver().getTimeout(), refresh);
    }

    /**
     * Waits until the number of rows displayed in the live data matches the expected count.
     *
     * @param expectedRowCount the number of expected rows
     * @param timeout a custom timeout before stopping the wait and raising an error
     */
    public void waitUntilRowCountEqualsTo(int expectedRowCount, int timeout)
    {
        waitUntilRowCountEqualsTo(expectedRowCount, timeout, false);
    }

    /**
     * Waits until the number of rows displayed in the live data matches the expected count.
     *
     * @param expectedRowCount the number of expected rows
     * @param timeout a custom timeout before stopping the wait and raising an error
     * @param refresh if {@code true}, the
     */
    public void waitUntilRowCountEqualsTo(int expectedRowCount, int timeout, boolean refresh)
    {
        getDriver().waitUntilCondition(webDriver -> {
            if (refresh) {
                this.liveData.refresh();
            }

            // Cells are displayed. And they are loaded.
            if (isEmpty() || !areCellsLoaded()) {
                return false;
            }

            // And the count of row is greater than the expected count.
            int count = countRows();
            LOGGER.info("TableLayoutElement#waitUntilRowCountEqualsTo/refresh(): count = [{}]", count);
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
        filterColumn(columnLabel, content, wait, Map.of());
    }

    /**
     * Set the value in the filter of a column. Waits for the new filtered values to be displayed before continuing when
     * {@code waits} is {@code true}.
     *
     * @param columnLabel the label of the column to filter, for instance {@code "Creation Date"}
     * @param content the content to set on the filter
     * @param wait when {@code true} waits for the filtered results to be displayed before continuing, otherwise
     *     continues without waiting (useful when updating several filters in a row).
     * @param options additional options that are only relevant for specific type of fields (e.g., for selectize
     *     based fields only)
     * @see #filterColumn(String, String)
     * @see #filterColumn(String, String, boolean)
     * @since 14.8RC1
     */
    public void filterColumn(String columnLabel, String content, boolean wait, Map<String, Object> options)
    {
        WebElement element = getFilter(columnLabel);

        List<String> classes = Arrays.asList(getClasses(element));
        if (classes.contains("filter-list")) {
            if (element.getAttribute(CLASS_HTML_ATTRIBUTE).contains("selectized")) {
                SuggestInputElement suggestInputElement = new SuggestInputElement(element);
                // Wait for the suggestions on selectize fields only if this is explicitly asked.
                suggestInputElement.clearSelectedSuggestions().sendKeys(content);
                if (Objects.equals(options.get(FILTER_COLUMN_SELECTIZE_WAIT_FOR_SUGGESTIONS), Boolean.TRUE)) {
                    suggestInputElement.waitForSuggestions().selectByVisibleText(content);
                } else {
                    suggestInputElement.selectTypedText();
                }
            } else {
                new Select(element).selectByVisibleText(content);
            }
        } else if (classes.contains("filter-text")) {
            element.clear();
            if (content.isEmpty()) {
                // Make sure we generate some actual key events so LD notices the empty filter.
                element.sendKeys(" ", Keys.BACK_SPACE);
            } else {
                element.sendKeys(content);
            }
        } else if (classes.contains("filter-date")) {
            element.click();
            DateRangePicker picker = new DateRangePicker(element);
            if (StringUtils.isNotBlank(content)) {
                element.clear();
                element.sendKeys(content);
                picker.applyRange();
            } else {
                picker.clearRange();
            }
        }

        if (wait) {
            waitUntilReady();
        }
    }

    /**
     * Sort by the specified column and wait for the data to load.
     *
     * @param columnLabel the label of the column to sort
     * @since 15.9RC1
     */
    public void sortBy(String columnLabel)
    {
        int columnIndex = findColumnIndex(columnLabel);

        WebElement element = getRoot().findElement(By.cssSelector(String.format(".column-header-names > th:nth-child"
            + "(%d) .handle", columnIndex)));
        element.click();

        waitUntilReady();
    }

    /**
     * Return the current values of the filter of a given column. We return a list of values because some filters can
     * have several values (e.g., a selectized field).
     *
     * @param columnLabel the label of the column (for instance, {@code Name})
     * @return the current values of the filter
     * @since 13.9
     * @since 13.10RC1
     * @since 13.4.4
     */
    public List<String> getFilterValues(String columnLabel)
    {
        int columnIndex = findColumnIndex(columnLabel);
        WebElement element = getRoot()
            .findElement(By.cssSelector(String.format(".column-filters > th:nth-child(%d) input", columnIndex)));
        List<String> classes = Arrays.asList(getClasses(element));
        List<String> ret;
        if (classes.contains("filter-list")) {
            if (element.getAttribute(CLASS_HTML_ATTRIBUTE).contains("selectized")) {
                ret = new SuggestInputElement(element)
                    .getSelectedSuggestions()
                    .stream()
                    .map(SuggestInputElement.SuggestionElement::getLabel)
                    .collect(Collectors.toList());
            } else {
                ret = new Select(element)
                    .getAllSelectedOptions()
                    .stream()
                    .map(WebElement::getText)
                    .collect(Collectors.toList());
            }
        } else if (classes.contains("filter-text")) {
            ret = singletonList(element.getText());
        } else {
            ret = Collections.emptyList();
        }

        return ret;
    }

    /**
     * @return the number of rows currently displayed in the live data
     * @since 12.10.9
     */
    public int countRows()
    {
        return getDriver().findElementsWithoutWaiting(getRoot(), By.cssSelector("tbody tr td:first-child")).size();
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
     * Return the list of {@link WebElement} of a column by its label.
     *
     * @param columnLabel the label of the column to get, for instance {@code "Title"}
     * @return the list of {@link WebElement} of the request column
     * @since 15.9RC1
     * @since 15.5.3
     */
    public List<WebElement> getAllCells(String columnLabel)
    {
        int columnNumber = findColumnIndex(columnLabel);
        return getRoot().findElements(By.cssSelector(String.format("tbody tr td:nth-child(%d)", columnNumber)));
    }

    /**
     * Get the 1-based row index of an element, relative to the number of currently displayed rows.
     *
     * @param by the selector of the searched element
     * @return the 1-based row index where the element was found, or 0 if it doesn't exist
     * @since 14.8RC1
     */
    public int getRowIndexForElement(By by)
    {
        WebElement rowElement = getDriver().findElementWithoutWaiting(getRoot(), by);
        if (rowElement.isDisplayed()) {
            // Count the preceding rows without waiting.
            return getDriver().findElementsWithoutWaiting(rowElement,
                By.xpath("./ancestor::tr[1]/preceding-sibling::tr")).size() + 1;
        }
        return 0;
    }

    /**
     * Get the filter for the given column.
     *
     * @param columnLabel the label of the column to get the filter element for, for instance {@code "Title"}
     * @return The {@link WebElement} for the given column filter.
     * @since 13.10RC1
     */
    public WebElement getFilter(String columnLabel)
    {
        int columnIndex = findColumnIndex(columnLabel);
        By cssSelector = By.cssSelector(String.format(
            ".column-filters > th:nth-child(%1$d) input, "
            + ".column-filters > th:nth-child(%1$d) select", columnIndex));
        return getRoot().findElement(cssSelector);
    }

    /**
     * @return the list of pagination size choices proposed by the pagination select field
     * @since 14.7RC1
     */
    public Set<String> getPaginationSizes()
    {
        return new Select(getRoot().findElement(By.cssSelector(".pagination-page-size select"))).getOptions().stream()
            .map(it -> it.getAttribute("value")).collect(Collectors.toSet());
    }

    private String getPaginationEntriesString()
    {
        return getRoot().findElement(By.className("pagination-current-entries")).getText().trim();
    }

    private java.util.regex.Matcher getPaginationMatcher()
    {
        return PAGINATION_SENTENCE_PATTERN.matcher(getPaginationEntriesString());
    }

    public long getTotalEntries()
    {
        java.util.regex.Matcher paginationMatcher = getPaginationMatcher();
        if (!paginationMatcher.matches()) {
            throw new IllegalStateException("Matcher does not match: " + getPaginationEntriesString());
        }
        return Long.parseLong(paginationMatcher.group("totalEntries"));
    }

    /**
     * Clicks on an action button identified by its name, on a given row.
     *
     * @param rowNumber the row number, for instance 3 for the third row
     * @param actionName the name of the action button to click on
     */
    public void clickAction(int rowNumber, String actionName)
    {
        clickAction(rowNumber, getActionSelector(actionName));
    }

    /**
     * @param rowNumber the row number to inspect
     * @param actionName the expected action
     * @return {@code true} if the expected action is found on the row, {@code false} otherwise
     * @since 16.2.0RC1
     */
    public boolean hasAction(int rowNumber, String actionName)
    {
        return !findElementsInRow(rowNumber, getActionSelector(actionName)).isEmpty();
    }

    /**
     * Clicks on an action based on a row and the provided selector.
     *
     * @param rowNumber the row number, for instance 3 for the third row
     * @param selector the selector to find the action element in the row
     */
    public void clickAction(int rowNumber, By selector)
    {
        findElementInRow(rowNumber, selector).click();
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
        internalEdit(columnLabel, rowNumber, fieldName, newValue, true);
    }

    /**
     * Starts editing a cell, input a value, but cancel the edition (by pressing escape) instead of saving.
     *
     * @param columnLabel the label of the column
     * @param rowNumber the number of the row to update (the first line is number 1)
     * @param fieldName the name of the field to edit, in other word the name of the corresponding XClass property
     * @param newValue the new value set of the field, but never saved because we cancel the edition
     * @since 13.5RC1
     * @since 13.4.1
     */
    public void editAndCancel(String columnLabel, int rowNumber, String fieldName, String newValue)
    {
        internalEdit(columnLabel, rowNumber, fieldName, newValue, false);
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
        return getRowElement(rowNumber).findElement(by);
    }

    /**
     * Return a list of elements matching a given selector in a row.
     *
     * @param rowNumber the number of the row to search on (starting at index 1)
     * @param by the selector of the elements to search for
     * @return the list of matched elements
     * @since 16.2.0RC1
     */
    public List<WebElement> findElementsInRow(int rowNumber, By by)
    {
        return getRowElement(rowNumber).findElements(by);
    }

    private WebElement getRowElement(int rowNumber)
    {
        return getRoot().findElement(By.cssSelector(String.format("tbody tr:nth-child(%d)", rowNumber)));
    }

    /**
     * Return a hamcrest {@link Matcher} on the text of a {@link WebElement}. For instance the {@link Matcher} will
     * match on the web element containing the following html source {@code <p>Te<i>st</i></p>} for the value {@code
     * "Test"}.
     *
     * @param value the expected value of the text returned by {@link WebElement#getText()}
     * @return a matcher instance
     * @since 13.5RC1
     * @since 13.4.1
     * @since 12.10.9
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
     * @since 13.4.1
     */
    public Matcher<WebElement> getWebElementCellWithLinkMatcher(String text, String link)
    {
        return new CellWithLinkMatcher(text, link);
    }

    /**
     * @param columnLabel The label of the column to check for
     * @return If the given column exists
     * @since 15.9RC1
     */
    public boolean hasColumn(String columnLabel)
    {
        return findColumnIndex(columnLabel) >= 0;
    }

    /**
     * Return the {@link WebElement} of the dropdown button.
     *
     * @return the {@link WebElement} of the dropdown button
     * @since 13.10RC1
     * @since 13.4.5
     */
    public WebElement getDropDownButton()
    {
        return getRoot().findElement(By.cssSelector("a.dropdown-toggle"));
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
     * @return {@code false} if the live data contains some result lines, {@code true} otherwise
     */
    private boolean isEmpty()
    {
        return getRoot().findElements(By.cssSelector("tbody tr td.cell .livedata-displayer")).isEmpty();
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
        return getDriver().findElement(By.id(this.liveData.getId()));
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
     * Does the steps for the edition of a cell and then returning the cell to view mode either by clicking on a h1
     * tag or by pressing escape if {@code save} is false.
     *
     * @param columnLabel the label of the column
     * @param rowNumber the number of the row to update (the first line is number 1)
     * @param fieldName the name of the field to edit, in other word the name of the corresponding XClass property
     * @param newValue the new value set of the field, but never saved because we cancel the edition
     * @param save if the edit shall be saved (if false, the edit is cancelled)
     */
    private void internalEdit(String columnLabel, int rowNumber, String fieldName, String newValue, boolean save)
    {
        int columnIndex = getColumnIndex(columnLabel);
        WebElement element = getCellsByColumnIndex(columnIndex).get(rowNumber - 1);

        // Hover on the property and click on the edit button on the displayed popover. We move slightly at the right of
        // the center of the targeted element, then slightly to the left, towards the center of the element. This
        // simulates the mouse trajectory of a real user hovering the cell above the one he/she wants to edit.
        new Actions(getDriver().getWrappedDriver())
            .moveToElement(element, 50, 0)
            .moveToElement(element, 0, 0)
            .perform();
        By editActionSelector = By.cssSelector(".displayer-action-list span[title='Edit']");
        // Waits to have at least one popover visible and click on the edit action of the last one. While it does not
        // seem to be possible in normal conditions, using selenium and moveToElement, several popover can be visible
        // at the same time (especially on Chrome). We select the latest edit action, which is the one of the targeted
        // property because the popover actions are appended at the end of the document.
        getDriver().waitUntilCondition(input -> !getDriver().findElementsWithoutWaiting(editActionSelector).isEmpty());
        // Does not use findElementsWithoutWaiting to let a chance for the cursor to move to the targeted cell, and for
        // its edit action popover to be displayed.
        List<WebElement> popoverActions = getDriver().findElements(editActionSelector);
        popoverActions.get(popoverActions.size() - 1).click();
        
        // Selector of the edited field.
        By selector = By.cssSelector(String.format("[name$='_%s']", fieldName));

        // Waits for the text input to be displayed.
        getDriver().waitUntilElementIsVisible(element, selector);

        // Reuse the FormContainerElement to avoid code duplication of the interaction with the form elements 
        // displayed in the live data (they are the same as the one of the inline edit mode).
        new FormContainerElement(By.cssSelector(".livedata-displayer .edit"))
            .setFieldValue(element.findElement(selector), newValue);

        if (save) {
            // Clicks somewhere outside the edited cell. We use the h1 tag because it is present on all pages.
            new Actions(getDriver().getWrappedDriver()).click(getDriver().findElement(By.tagName("h1"))).perform();
        } else {
            // Press escape to cancel the edition.
            new Actions(getDriver().getWrappedDriver()).sendKeys(Keys.ESCAPE).build().perform();
        }

        // Waits for the field to disappear.
        getDriver().waitUntilCondition(input -> {
            // The edited field is not displayed anymore.
            return element.findElements(selector).isEmpty();
        });

        // Wait for reload after saving.
        if (save) {
            waitUntilReady();
        }
    }

    private String[] getClasses(WebElement element)
    {
        return element.getAttribute(CLASS_HTML_ATTRIBUTE).split("\\s+");
    }

    private String urlWithoutFormToken(EntityReference entityReference, String action)
    {
        return urlWithoutFormToken(getUtil().getURL(entityReference, action, ""));
    }

    /**
     * Remove the {@code form_token} path parameter from an url.
     *
     * @param url the url to clean up
     * @return the cleanup url, without the {@code form_token} path parameter
     */
    private String urlWithoutFormToken(String url)
    {
        URI initialUri = URI.create(url);
        URIBuilder uriBuilder = new URIBuilder(initialUri);
        List<NameValuePair> cleanedUpParams = uriBuilder.getQueryParams().stream()
            .filter(it -> !Objects.equals(it.getName(), "form_token"))
            .collect(Collectors.toList());
        uriBuilder.setParameters(cleanedUpParams);
        URI uri;
        try {
            uri = uriBuilder.build();
        } catch (URISyntaxException e) {
            uri = initialUri;
        }
        return uri.toASCIIString();
    }

    private static By getActionSelector(String actionName)
    {
        return By.cssSelector(String.format(".actions-container .action.action_%s", actionName));
    }
}
