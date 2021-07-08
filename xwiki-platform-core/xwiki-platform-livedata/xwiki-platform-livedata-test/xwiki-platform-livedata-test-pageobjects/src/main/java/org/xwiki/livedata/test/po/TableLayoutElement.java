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
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Provides the operations to interact with a Live Data when displayed with a table layout.
 *
 * @version $Id$
 * @since 13.4RC1
 * @since 12.10.9
 */
public class TableLayoutElement extends BaseElement
{
    private static final String INNER_HTML_ATTRIBUTE = "innerHTML";

    private static final String CLASS_HTML_ATTRIBUTE = "class";

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

    private static final String SELECT_CELLS_BY_COLUMN_INDEX = "tr td:nth-child(%d)";

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
            try {
                return !expectRows || hasLines() && areCellsLoaded();
            } catch (StaleElementReferenceException e) {
                // The Live Data root element can be staled when Vue takes over the root element introduced by the 
                // Live Data macro.
                return false;
            }
        }, 20);
    }

    /**
     * @return the number of rows currently displayed in the live data
     */
    public int countRows()
    {
        return getRoot().findElements(By.cssSelector("tbody tr td:first-child")).size();
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
     * @return the text of the columns currently displayed in the live data
     */
    private List<String> getColumnsLabels()
    {
        return getRoot().findElements(By.cssSelector(".column-name > span:nth-child(2)"))
            .stream()
            .map(WebElement::getText)
            .collect(Collectors.toList());
    }

    private WebElement getRoot()
    {
        return getDriver().findElementById(this.liveDataId);
    }
}
