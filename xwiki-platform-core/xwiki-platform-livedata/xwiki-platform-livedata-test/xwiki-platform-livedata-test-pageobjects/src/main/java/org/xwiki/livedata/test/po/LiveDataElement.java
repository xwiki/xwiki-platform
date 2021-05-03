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
import org.xwiki.test.ui.po.BaseElement;

/**
 * Live Data page object. Provides the operations to interact with a live data macro.
 *
 * @version $Id$
 * @since 13.4RC1
 */
public class LiveDataElement extends BaseElement
{
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
     * @param columnName the column name
     * @param value the value to be found in the column
     * @return {@code true} if the value is found in the column, {@code false} otherwise
     */
    public boolean hasRow(String columnName, String value)
    {
        // TODO: currently only equality of the cell's text value is supported, this might not be convenient for some
        // cells types.
        return getValues(columnName)
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
        getDriver().waitUntilCondition(webDriver -> {
            WebElement liveData = getLiveData();
            // Cells are displayed.
            boolean hasLines = !liveData.findElements(By.cssSelector("tbody tr td.cell .livedata-displayer")).isEmpty();
            // And they are loaded.
            boolean linesAreLoaded = liveData.findElements(By.cssSelector("tbody tr td.cell .xwiki-loader")).isEmpty();
            return hasLines && linesAreLoaded;
        });
    }

    /**
     * Gets the values currently displayed in the given column.
     *
     * @param columnName the column name
     * @return the list of values found in the column
     */
    private List<WebElement> getValues(String columnName)
    {
        int columnIndex = getColumnIndex(columnName);
        return getLiveData().findElements(By.cssSelector(String.format("tr td:nth-child(%d)", columnIndex)));
    }

    /**
     * Returns the column index of the given column. The indexes start at {@code 1}, corresponding to the leftest
     * column.
     *
     * @param columnName a column name
     * @return the index of the given column
     */
    private int getColumnIndex(String columnName)
    {
        return getColumnsNames().indexOf(columnName) + 1;
    }

    /**
     * @return the text of the columns currently displayed in the live data
     */
    private List<String> getColumnsNames()
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
