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
package org.xwiki.test.ui.po.editor;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.Select;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the actions possible on the date picker.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class DatePicker extends BaseElement
{
    /**
     * The element wrapping the date picker.
     */
    @FindBy(className = "calendar_date_select")
    private WebElement container;

    public DatePicker()
    {
        this.waitToLoad();
    }

    /**
     * Selects the specified year.
     * 
     * @param year the year to select
     * @return this date picker
     */
    public DatePicker setYear(String year)
    {
        Select yearSelector = new Select(container.findElement(By.className("year")));
        yearSelector.selectByVisibleText(year);
        return this;
    }

    /**
     * @return the selected year
     */
    public String getYear()
    {
        Select yearSelector = new Select(container.findElement(By.className("year")));
        return yearSelector.getFirstSelectedOption().getText();
    }

    /**
     * Selects the specified month.
     * 
     * @param month the month to select
     * @return this date picker
     */
    public DatePicker setMonth(String month)
    {
        Select monthSelector = new Select(container.findElement(By.className("month")));
        monthSelector.selectByVisibleText(month);
        return this;
    }

    /**
     * @return the selected month
     */
    public String getMonth()
    {
        Select monthSelector = new Select(container.findElement(By.className("month")));
        return monthSelector.getFirstSelectedOption().getText();
    }

    /**
     * Selects the specified day from the current month.
     * 
     * @param day the day to select
     * @return this date picker
     */
    public DatePicker setDay(String day)
    {
        container.findElement(By.xpath("//*[@class = 'cds_body']//tbody//div[. = '" + day + "' and not(@class)]"))
            .click();
        return this;
    }

    /**
     * @return the selected day, if any, otherwise {@code null}
     */
    public String getDay()
    {
        String xpath = "//*[@class = 'cds_body']//tbody//*[contains(@class, 'selected')]";
        List<WebElement> selected = container.findElements(By.xpath(xpath));
        if (selected.size() == 1) {
            return selected.get(0).getText();
        }
        return null;
    }

    /**
     * Selects the specified hour.
     * 
     * @param hour the hour to select
     * @return this data picker
     */
    public DatePicker setHour(String hour)
    {
        Select hourSelector = new Select(container.findElement(By.className("hour")));
        hourSelector.selectByVisibleText(hour);
        return this;
    }

    /**
     * @return the selected hour
     */
    public String getHour()
    {
        Select hourSelector = new Select(container.findElement(By.className("hour")));
        return hourSelector.getFirstSelectedOption().getText();
    }

    /**
     * Selects the specified minute.
     * 
     * @param minute the minute to select
     * @return this date picker
     */
    public DatePicker setMinute(String minute)
    {
        Select minuteSelector = new Select(container.findElement(By.className("minute")));
        if (minuteSelector.getFirstSelectedOption().getText().equals(minute)) {
            // The specified minute is already selected but that doesn't mean the date text input has exactly the
            // specified minute. The minute selector has only multiples of 5 so you can have '15' selected while the
            // actual minutes in the date input is '17'. The date picker rounds down the actual minutes to the closest
            // multiple of 5 before initializing the minute selector. We need to force a selection change event in order
            // to update the date text input with the specified minute.
            minuteSelector.selectByIndex(0);
            if (minuteSelector.getFirstSelectedOption().getText().equals(minute)) {
                minuteSelector.selectByIndex(1);
            }
        }
        minuteSelector.selectByVisibleText(minute);
        return this;
    }

    /**
     * @return the selected minute
     */
    public String getMinute()
    {
        Select minuteSelector = new Select(container.findElement(By.className("minute")));
        return minuteSelector.getFirstSelectedOption().getText();
    }

    /**
     * @return {@code true} if the date picker allows you to select the hour, {@code false} otherwise
     */
    public boolean hasHourSelector()
    {
        return getDriver().findElementsWithoutWaiting(container, By.className("hour")).size() > 0;
    }

    /**
     * Waits for the DatePicker popup to load.
     * 
     * @since 6.3M2
     */
    public DatePicker waitToLoad()
    {
        getDriver().waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                try {
                    // Since container is a proxy WebElement, any method on it would call findElement internally. We
                    // chose isDisplayed since it is the most self-describing, even if it's practically useless.
                    return container.isDisplayed();
                } catch (NotFoundException e) {
                    return false;
                }
            }
        });
        return this;
    }

    /**
     * Close the date picker.
     *
     * @return this date picker
     * @since 10.6RC1
     */
    public DatePicker close()
    {
        this.container.findElement(By.partialLinkText("OK")).click();
        return this;
    }
}
