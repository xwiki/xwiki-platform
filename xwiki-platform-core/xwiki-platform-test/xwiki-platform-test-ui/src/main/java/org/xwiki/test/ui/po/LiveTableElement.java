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

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the actions possible on a livetable.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class LiveTableElement extends BaseElement
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LiveTableElement.class);

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
        try {
            Object result = getDriver().executeJavascript("return (Element.hasClassName) && Element.hasClassName('"
                + StringEscapeUtils.escapeEcmaScript(livetableId) + "-ajax-loader','hidden')");
            return result instanceof Boolean ? (Boolean) result : false;
        } catch (JavascriptException e) {
            return false;
        }
    }

    /**
     * Wait till the livetable has finished displaying all its rows (so that we can then assert the livetable content
     * without running the risk that the rows have not been updated yet).
     */
    public void waitUntilReady()
    {
        long t1 = System.currentTimeMillis();
        while ((System.currentTimeMillis() - t1 < getDriver().getTimeout() * 1000L)) {
            if (isReady()) {
                return;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // Do nothing just break out
                break;
            }
        }
        throw new TimeoutException("Livetable isn't ready after the timeout has expired.");
    }

    public boolean hasColumn(String columnTitle)
    {
        List<WebElement> elements = getDriver().findElementsWithoutWaiting(
            By.xpath("//th[contains(@class, 'xwiki-livetable-display-header-text') and normalize-space(.) = '"
                + columnTitle + "']"));
        return elements.size() > 0;
    }

    public void filterColumn(String inputId, String filterValue)
    {
        // Make extra sure Selenium can't go quicker than the live table status by forcing it before filtering.
        getDriver().executeJavascript("return $('" + StringEscapeUtils.escapeEcmaScript(livetableId)
            + "-ajax-loader').removeClassName('hidden')");

        WebElement element = getDriver().findElement(By.id(livetableId)).findElement(By.id(inputId));
        if ("select".equals(element.getTagName())) {
            if (element.getAttribute("class").contains("selectized")) {
                SuggestInputElement suggestInputElement = new SuggestInputElement(element);
                suggestInputElement.sendKeys(filterValue).selectTypedText();
            } else {
                new Select(element).selectByVisibleText(filterValue);
            }
        } else {
            element.clear();
            element.sendKeys(filterValue);
        }
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

        return ((Long) ((JavascriptExecutor) getDriver()).executeScript("return arguments[0].cellIndex;", column))
            .intValue();
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
        List<WebElement> elements = getRows(columnTitle);

        boolean result = elements.size() > 0;
        boolean match = false;
        if (result) {
            for (WebElement element : elements) {
                match = element.getText().equals(columnValue);
                if (match) {
                    break;
                }
            }
        }

        return result && match;
    }

    /**
     * Checks if there are as many rows as there are passed values and check that the values match.
     *
     * @since 7.2M2
     */
    public boolean hasExactRows(String columnTitle, List<String> columnValues)
    {
        List<WebElement> elements = getRows(columnTitle);

        boolean result = elements.size() == columnValues.size();
        if (result) {
            for (int i = 0; i < elements.size(); i++) {
                result = result && elements.get(i).getText().equals(columnValues.get(i));
                if (!result) {
                    break;
                }
            }
        }

        return result;
    }

    private List<WebElement> getRows(String columnTitle)
    {
        String cellXPath = String.format(".//tr/td[position() = %s]", getColumnIndex(columnTitle) + 1);
        WebElement liveTableBody = getDriver().findElement(By.id(this.livetableId + "-display"));
        // Don't wait as rows should be available after the LiveTable is ready.
        return getDriver().findElementsWithoutWaiting(liveTableBody, By.xpath(cellXPath));
    }

    /**
     * @return the number of rows in the live table
     */
    public int getRowCount()
    {
        WebElement liveTableBody = getDriver().findElementWithoutWaiting(By.id(this.livetableId + "-display"));
        // We use XPath because we're interested only in the direct children.
        return getDriver().findElementsWithoutWaiting(liveTableBody, By.xpath("tr")).size();
    }

    /**
     * @since 5.3RC1
     */
    public WebElement getRow(int rowNumber)
    {
        WebElement liveTableBody = getDriver().findElementWithoutWaiting(By.id(this.livetableId + "-display"));
        return getDriver().findElementWithoutWaiting(liveTableBody, By.xpath("tr[" + rowNumber + "]"));
    }

    /**
     * Get the row index of an element, relative to the number of currently displayed rows.
     *
     * @param by the selector of the searched element
     * @return the row index where the element was found
     * @since 14.4.2
     * @since 14.5
     */
    public int getRowNumberForElement(By by)
    {
        WebElement livetableRowElement =
            getDriver().findElement(By.xpath("//tbody[@id='" + this.livetableId + "-display']/tr/td")).findElement(by);
        if (livetableRowElement.isDisplayed()) {
            // Count the preceding rows.
            return livetableRowElement.findElements(By.xpath("./ancestor::tr[1]/preceding-sibling::tr")).size() + 1;
        }
        return 0;
    }

    /**
     * @since 5.3RC1
     */
    public WebElement getCell(WebElement rowElement, int columnNumber)
    {
        return getDriver().findElementWithoutWaiting(rowElement, By.xpath("td[" + columnNumber + "]"));
    }

    /**
     * @since 11.6RC1
     * @since 11.5
     */
    public WebElement getCell(int rowNumber, int columnNumber)
    {
        return getCell(getRow(rowNumber), columnNumber);
    }

    /**
     * @since 5.3RC1
     */
    public ViewPage clickCell(int rowNumber, int columnNumber)
    {
        WebElement tdElement = getCell(getRow(rowNumber), columnNumber);
        // First scroll the element into view, if needed, by moving the mouse to the top left corner of the element.
        new Actions(getDriver().getWrappedDriver()).moveToElement(tdElement, 0, 0).perform();
        // Find the first A element and click it!
        tdElement.findElement(By.tagName("a")).click();
        return new ViewPage();
    }

    /**
     * @since 3.2M3
     */
    public void waitUntilRowCountGreaterThan(int minimalExpectedRowCount)
    {
        final By by = By.xpath("//tbody[@id = '" + this.livetableId + "-display']//tr");
        // TODO: Remove the try/catch and the logging once we understand the issue. This is done to debug an issue
        //  with the MailIT test where the timeout is not the issue since the CI screenshots shows that the number of
        //  rows displayed is correct and yet this method returns less than the expected count. Thus we suppose that
        //  the refresh() might not be working. We thus also try to navigate to the same page in the hope to get better
        //  results.
        try {
            getDriver().waitUntilCondition(driver -> {
                // Refresh the current page since we need the livetable to fetch the JSON again
                driver.navigate().refresh();
                this.waitUntilReady();
                int count = driver.findElements(by).size();
                LOGGER.info("LiveTableElement#waitUntilRowCountGreaterThan/refresh(): count = [{}]", count);
                return count >= minimalExpectedRowCount;
            });
        } catch (TimeoutException e) {
            // Try again but this time with driver.get(), in case refresh() doesn't work.
            getDriver().waitUntilCondition(driver -> {
                // Refresh the current page since we need the livetable to fetch the JSON again
                driver.get(driver.getCurrentUrl());
                this.waitUntilReady();
                int count = driver.findElements(by).size();
                LOGGER.info("LiveTableElement#waitUntilRowCountGreaterThan/get(): count = [{}]", count);
                return count >= minimalExpectedRowCount;
            });
        }
    }

    /**
     * Same as {@link #waitUntilRowCountGreaterThan(int, int)} but with a specific timeout (ie not using the default
     * timeout)
     *
     * @since 9.1RC1
     */
    // We need to decide if it's better to introduce this method or to globally increase the default timeout.
    public void waitUntilRowCountGreaterThan(int minimalExpectedRowCount, int timeout)
    {
        int originalTimeout = getDriver().getTimeout();
        getDriver().setTimeout(timeout);
        try {
            waitUntilRowCountGreaterThan(minimalExpectedRowCount);
        } finally {
            getDriver().setTimeout(originalTimeout);
        }
    }

    /**
     * Sorts the live table on the specified column.
     *
     * @param columnTitle the column to sort on
     * @since 9.7RC1
     */
    public void sortBy(String columnTitle)
    {
        // Make extra sure Selenium can't go quicker than the live table status by forcing it before sorting.
        getDriver().executeJavascript("return $('" + StringEscapeUtils.escapeEcmaScript(livetableId)
            + "-ajax-loader').removeClassName('hidden')");

        getHeaderByColumnTitle(columnTitle).click();

        waitUntilReady();
    }

    /**
     * Sorts the live table on the specified column, by ascending order.
     *
     * @param columnTitle the column to sort on
     * @since 13.3RC1
     * @since 12.10.6
     */
    public void sortAscending(String columnTitle)
    {
        WebElement element = getHeaderByColumnTitle(columnTitle);
        List<String> strings = Arrays.asList(element.getAttribute("class").split("\\w+"));
        boolean isSelected = strings.contains("selected");
        boolean isAsc = strings.contains("asc");

        /*
         * isSelected indicates if the column is the one currently sorted.
         * If the column is the one currently sorted, and is in ascending order, we do nothing.
         * If the column is already sorted in descending order, or if it is not the one currently sorted, but was
         * previously sorted in ascending order, we sort only once.
         * If the column is not already sorted, and was previously sorted in descending order, clicking sorting twice
         * is required to sort in ascending order.
         */
        if (isSelected && !isAsc || (!isSelected && isAsc)) {
            sortBy(columnTitle);
        } else if (!isSelected) {
            sortBy(columnTitle);
            sortBy(columnTitle);
        }
    }

    /**
     * Sorts the live table on the specified column, by ascending order.
     *
     * @param columnTitle the column to sort on
     * @since 13.3RC1
     * @since 12.10.6
     */
    public void sortDescending(String columnTitle)
    {
        WebElement element = getHeaderByColumnTitle(columnTitle);
        List<String> strings = Arrays.asList(element.getAttribute("class").split("\\w+"));
        boolean isSelected = strings.contains("selected");
        boolean isDesc = strings.contains("desc");

        /*
         * isSelected indicates if the column is the one currently sorted.
         * If the column is the one currently sorted, and is in descending order, we do nothing.
         * If the column is already sorted in ascending order, or if it is not the one currently sorted, but was
         * previously sorted in descending order, we sort only once.
         * If the column is not already sorted, and was previously sorted in ascending order, clicking sorting twice
         * is required to sort in descending order.
         */
        if (isSelected && !isDesc || (!isSelected && isDesc)) {
            sortBy(columnTitle);
        } else if (!isSelected) {
            sortBy(columnTitle);
            sortBy(columnTitle);
        }
    }

    /**
     * Clicks a form element (usually a button) with the passed name at the passed row number.
     *
     * @param actionName the HTML form element name attribute value
     * @param rowNumber the LT row number (starts at 1)
     * @since 12.10
     */
    public void clickAction(String actionName, int rowNumber)
    {
        WebElement rowElement = getRow(rowNumber);
        rowElement.findElement(By.xpath("td/form//input[@name = '" + actionName + "']")).click();
    }

    private WebElement getHeaderByColumnTitle(String columnTitle)
    {
        String xpath = String.format("//table[@id = '%s']//th[contains(@class, 'xwiki-livetable-display-header-text')"
            + " and contains(@class, 'sortable') and normalize-space(.) = '%s']", this.livetableId, columnTitle);
        return getDriver().findElement(By.xpath(xpath));
    }
}
