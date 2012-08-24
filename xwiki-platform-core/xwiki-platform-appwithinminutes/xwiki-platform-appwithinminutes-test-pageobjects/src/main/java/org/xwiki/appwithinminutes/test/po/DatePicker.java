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
package org.xwiki.appwithinminutes.test.po;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
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

    /**
     * Selects the specified year.
     * 
     * @param year the year to select
     */
    public void setYear(String year)
    {
        Select yearSelector = new Select(container.findElement(By.className("year")));
        yearSelector.selectByVisibleText(year);
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
     */
    public void setMonth(String month)
    {
        Select monthSelector = new Select(container.findElement(By.className("month")));
        monthSelector.selectByVisibleText(month);
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
     */
    public void setDay(String day)
    {
        container.findElement(By.xpath("//*[@class = 'cds_body']//tbody//div[. = '" + day + "' and not(@class)]"))
            .click();
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
     */
    public void setHour(String hour)
    {
        Select hourSelector = new Select(container.findElement(By.className("hour")));
        hourSelector.selectByVisibleText(hour);
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
     */
    public void setMinute(String minute)
    {
        Select minuteSelector = new Select(container.findElement(By.className("minute")));
        minuteSelector.selectByVisibleText(minute);
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
        return getUtil().findElementsWithoutWaiting(getDriver(), container, By.className("hour")).size() > 0;
    }
}
