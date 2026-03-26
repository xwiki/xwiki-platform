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
package org.xwiki.extension.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the pagination filter.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class PaginationFilterPane extends BaseElement
{
    /**
     * The class name of the next page button.
     */
    private static final String NEXT_PAGE_CLASS_NAME = "nextPagination";

    /**
     * The class name of the previous page button.
     */
    private static final String PREVIOUS_PAGE_CLASS_NAME = "prevPagination";

    /**
     * The elements that displays the total number of results.
     */
    @FindBy(className = "totalResultsNo")
    private WebElement resultsCount;

    /**
     * The element that displays the index of the first and last result from the current page.
     */
    @FindBy(className = "currentResultsNo")
    private WebElement currentRange;

    /**
     * The element that displays the index of the current page.
     */
    @FindBy(className = "currentPage")
    private WebElement currentPageIndex;

    /**
     * The index of the last page.
     */
    @FindBy(xpath = "//ul[@class = 'pagination-list']/*[last()]")
    private WebElement lastPageIndex;

    /**
     * The button used to navigate to the previous page.
     */
    @FindBy(className = PREVIOUS_PAGE_CLASS_NAME)
    private WebElement previousPageButton;

    /**
     * The button used to navigate to the next page.
     */
    @FindBy(className = NEXT_PAGE_CLASS_NAME)
    private WebElement nextPageButton;

    /**
     * @return the total number of results
     */
    public int getResultsCount()
    {
        return Integer.parseInt(resultsCount.getText());
    }

    /**
     * @return the index of the first and last result on the current page, using this format: {@code start - end}
     */
    public String getCurrentRange()
    {
        return currentRange.getText();
    }

    /**
     * @return the index of the current page
     */
    public int getCurrentPageIndex()
    {
        return Integer.parseInt(currentPageIndex.getText());
    }

    /**
     * @return the number of pages
     */
    public int getPageCount()
    {
        return Integer.parseInt(lastPageIndex.getText());
    }

    /**
     * Navigates to the next page.
     * 
     * @return the new pagination filter matching the next page
     */
    public PaginationFilterPane nextPage()
    {
        nextPageButton.click();
        return new PaginationFilterPane();
    }

    /**
     * @return {@code true} if the previous page button is active, {@code false} otherwise
     */
    public boolean hasNextPage()
    {
        return getDriver().findElements(By.className(NEXT_PAGE_CLASS_NAME)).size() > 0;
    }

    /**
     * Navigates to the previous page.
     * 
     * @return the new pagination filter matching the previous page
     */
    public PaginationFilterPane previousPage()
    {
        previousPageButton.click();
        return new PaginationFilterPane();
    }

    /**
     * @return {@code true} if the previous page button is active, {@code false} otherwise
     */
    public boolean hasPreviousPage()
    {
        return getDriver().findElements(By.className(PREVIOUS_PAGE_CLASS_NAME)).size() > 0;
    }

    /**
     * Loads the specified page of results.
     * 
     * @param index the page index
     * @return the specified page
     */
    public PaginationFilterPane gotoPage(int index)
    {
        getDriver().findElement(By.xpath("//ul[@class = 'pagination-list']/li/a[. = '" + index + "']")).click();
        return new PaginationFilterPane();
    }
}
