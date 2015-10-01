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
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WebDriverException;

/**
 * Represents the actions possible on a static table.
 *
 * @version $Id$
 * @since 3.2M3
 */
public class TableElement extends BaseElement
{
    private final WebElement table;

    // Empty, numberOfColumns and numberOfRows are stored to cut down on processing.
    // Empty -1 = unknown, 0 = no, 1 = yes.
    private int empty = -1;

    private int numberOfColumns = -1;

    private int numberOfRows = -1;

    public TableElement(WebElement table)
    {
        super();
        if (!table.getTagName().toLowerCase().equals("table")) {
            throw new WebDriverException("You can only create a TableElement using a <table> web element,"
                                         + "you passed a <" + table.getTagName() + "> element");
        }
        if (!table.findElements(By.tagName("tbody")).isEmpty()) {
            this.table = table.findElements(By.tagName("tbody")).get(0);
        } else {
            this.table = table;
        }
    }

    /** @return true if there are no colums or no rows in the table. True if the table only contains headings. */
    public boolean isEmpty()
    {
        if (empty == -1) {
            for (WebElement row : table.findElements(By.tagName("tr"))) {
                if (!row.findElements(By.tagName("td")).isEmpty()) {
                    empty = 0;
                    return false;
                }
            }
            empty = 1;
        }
        return (empty == 1);
    }

    /** @return the number of columns in the table. */
    public int numberOfColumns()
    {
        if (numberOfRows() == 0) {
            return 0;
        }
        if (numberOfColumns == -1) {
            numberOfColumns = getRow(0).size();
        }
        return numberOfColumns;
    }

    /** @return The number of rows in the table. */
    public int numberOfRows()
    {
        if (numberOfRows == -1) {
            numberOfRows = table.findElements(By.tagName("tr")).size();
        }
        return numberOfRows;
    }

    /**
     * @param firstEntry text content of the element in the first row (usually a heading).
     * @return each WebElement in that column.
     */
    public List<WebElement> getColumn(String firstEntry)
    {
        return getColumn(getColumnNumber(firstEntry));
    }

    /**
     * @param firstEntry text content of the element in the first row (usually a heading).
     * @return number of that column (0 indexed)
     */
    public int getColumnNumber(String firstEntry)
    {
        if (numberOfRows() == 0) {
            return -1;
        }
        List<WebElement> headers = getRow(0);
        for (WebElement header : headers) {
            if (header.getText().equals(firstEntry)) {
                return headers.indexOf(header);
            }
        }
        return -1;
    }

    /**
     * @param columnNumber zero indexed number of the column.
     * @return each WebElement in that column.
     */
    public List<WebElement> getColumn(int columnNumber)
    {
        if (numberOfRows() == 0 || numberOfColumns() < columnNumber || columnNumber < 0) {
            return null;
        }
        // xpaths are 1 indexed.
        return table.findElements(By.xpath("//tr/th[" + (columnNumber + 1) + "]"
                                               + " | //tr/td[" + (columnNumber + 1) + "]"));
    }

    /**
     * @param firstEntry text content of the element in the first column
     *                   (a heading if the table has left side headings).
     * @return each WebElement in that row.
     */
    public List<WebElement> getRow(String firstEntry)
    {
        return getRow(getRowNumber(firstEntry));
    }

    /**
     * @param firstEntry text content of the element in the first column
     *                   (a heading if the table has left side headings).
     * @return numeric index of that row or -1 if not found.
     */
    public int getRowNumber(String firstEntry)
    {
        if (numberOfColumns() == 0) {
            return -1;
        }
        List<WebElement> headers = getColumn(0);
        for (WebElement header : headers) {
            if (header.getText().equals(firstEntry)) {
                return headers.indexOf(header);
            }
        }
        return -1;
    }

    /**
     * @param rowNumber zero indexed number of the row.
     * @return each WebElement in that row.
     */
    public List<WebElement> getRow(int rowNumber)
    {
        if (numberOfRows() <= rowNumber || rowNumber < 0) {
            return null;
        }
        return table.findElements(By.xpath("//tr[" + (rowNumber + 1) + "]/th"
                                               + " | //tr[" + (rowNumber + 1) + "]/td"));
    }
}
