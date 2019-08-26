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

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the actions possible on the Bootstrap-based date-time picker widget.
 * 
 * @version $Id$
 * @since 11.7RC1
 */
public class BootstrapDateTimePicker extends BaseElement
{
    @FindBy(className = "bootstrap-datetimepicker-widget")
    private WebElement container;

    public BootstrapDateTimePicker()
    {
        waitToLoad();
    }

    /**
     * @return the selected day
     */
    public String getSelectedDay()
    {
        return this.container.findElement(By.cssSelector(".day.active")).getText();
    }

    /**
     * @return the selected month
     */
    public String getSelectedMonth()
    {
        return this.container.findElement(By.cssSelector(".month.active")).getText();
    }

    /**
     * @return the selected year
     */
    public String getSelectedYear()
    {
        return this.container.findElement(By.cssSelector("[data-action='pickerSwitch'][title='Select Year']"))
            .getText();
    }

    /**
     * @param day
     * @return this date and time picker
     */
    public BootstrapDateTimePicker selectDay(String day)
    {
        this.container.findElement(By.xpath(".//*[@data-action = 'selectDay' and . = '" + day
            + "' and not(contains(@class, 'old')) and not(contains(@class, 'new'))]")).click();
        return this;
    }

    /**
     * Show the next month.
     * 
     * @return this date and time picker
     */
    public BootstrapDateTimePicker showNextMonth()
    {
        this.container.findElement(By.cssSelector("[data-action='next'] [title='Next Month']")).click();
        return this;
    }

    /**
     * Show the previous month.
     * 
     * @return this date and time picker
     */
    public BootstrapDateTimePicker showPreviousMonth()
    {
        this.container.findElement(By.cssSelector("[data-action='previous'] [title='Previous Month']")).click();
        return this;
    }

    /**
     * Opens the month and year picker.
     * 
     * @return this date and time picker
     */
    public BootstrapDateTimePicker changeMonthAndYear()
    {
        this.container.findElement(By.cssSelector("[data-action='pickerSwitch'][title='Select Month']")).click();
        return this;
    }

    /**
     * @param month the month to select
     * @return this date and time picker
     */
    public BootstrapDateTimePicker selectMonth(String month)
    {
        this.container.findElement(By.xpath(".//*[@data-action = 'selectMonth' and . = '" + month + "']")).click();
        return this;
    }

    /**
     * Show the next year.
     * 
     * @return this date and time picker
     */
    public BootstrapDateTimePicker showNextYear()
    {
        this.container.findElement(By.cssSelector("[data-action='next'] [title='Next Year']")).click();
        return this;
    }

    /**
     * Show the previous year.
     * 
     * @return this date and time picker
     */
    public BootstrapDateTimePicker showPreviousYear()
    {
        this.container.findElement(By.cssSelector("[data-action='previous'] [title='Previous Year']")).click();
        return this;
    }

    /**
     * Open the year picker.
     * 
     * @return this date and time picker
     */
    public BootstrapDateTimePicker changeYear()
    {
        this.container.findElement(By.cssSelector("[data-action='pickerSwitch'][title='Select Year']")).click();
        return this;
    }

    /**
     * Show the next decade.
     * 
     * @return this date and time picker
     */
    public BootstrapDateTimePicker showNextDecade()
    {
        this.container.findElement(By.cssSelector("[data-action='next'] [title='Next Decade']")).click();
        return this;
    }

    /**
     * Show the previous decade.
     * 
     * @return this date and time picker
     */
    public BootstrapDateTimePicker showPreviousDecade()
    {
        this.container.findElement(By.cssSelector("[data-action='previous'] [title='Previous Decade']")).click();
        return this;
    }

    /**
     * @param year the year to select
     * @return this date and time picker
     */
    public BootstrapDateTimePicker selectYear(String year)
    {
        this.container.findElement(By.xpath(".//*[@data-action = 'selectYear' and . = '" + year + "']")).click();
        return this;
    }

    /**
     * Open the decade picker.
     * 
     * @return this date and time picker
     */
    public BootstrapDateTimePicker changeDecade()
    {
        this.container.findElement(By.cssSelector("[data-action='pickerSwitch'][title='Select Decade']")).click();
        return this;
    }

    /**
     * @param decade the decade to select
     * @return this date and time picker
     */
    public BootstrapDateTimePicker selectDecade(String decade)
    {
        this.container.findElement(By.xpath(".//*[@data-action = 'selectDecade' and . = '" + decade + "']")).click();
        return this;
    }

    /**
     * Show the next century.
     * 
     * @return this date and time picker
     */
    public BootstrapDateTimePicker showNextCentury()
    {
        this.container.findElement(By.cssSelector("[data-action='next'] [title='Next Century']")).click();
        return this;
    }

    /**
     * Show the previous century.
     * 
     * @return this date and time picker
     */
    public BootstrapDateTimePicker showPreviousCentury()
    {
        this.container.findElement(By.cssSelector("[data-action='previous'] [title='Previous Century']")).click();
        return this;
    }

    /**
     * Switch between date and time picker.
     * 
     * @return this date and time picker
     */
    public BootstrapDateTimePicker toggleTimePicker()
    {
        this.container.findElement(By.cssSelector("[data-action='togglePicker'][title='Select Time']")).click();

        // Wait for collapse animation to end.
        WebElement datePickerParent = this.container.findElement(By.xpath(".//*[@class = 'datepicker']/parent::*"));
        WebElement timePickerParent = this.container.findElement(By.xpath(".//*[@class = 'timepicker']/parent::*"));
        getDriver().waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                boolean datePickerShown = "collapse in".equals(datePickerParent.getAttribute("class"));
                boolean timePickerShown = "collapse in".equals(timePickerParent.getAttribute("class"));
                return datePickerShown ^ timePickerShown;
            }
        });
        return this;
    }

    /**
     * @return {@code true} if the time picker is available
     */
    public boolean hasTimePicker()
    {
        return getDriver().findElementsWithoutWaiting(this.container,
            By.cssSelector("[data-action='togglePicker'][title='Select Time']")).size() > 0;
    }

    /**
     * Open the hour picker.
     * 
     * @return this date and time picker
     */
    public BootstrapDateTimePicker changeHour()
    {
        this.container.findElement(By.cssSelector("[data-action='showHours']")).click();
        return this;
    }

    /**
     * Open the minute picker.
     * 
     * @return this date and time picker
     */
    public BootstrapDateTimePicker changeMinute()
    {
        this.container.findElement(By.cssSelector("[data-action='showMinutes']")).click();
        return this;
    }

    /**
     * Open the second picker.
     * 
     * @return this date and time picker
     */
    public BootstrapDateTimePicker changeSecond()
    {
        this.container.findElement(By.cssSelector("[data-action='showSeconds']")).click();
        return this;
    }

    /**
     * @param hour the hour to select
     * @return this date and time picker
     */
    public BootstrapDateTimePicker selectHour(String hour)
    {
        this.container.findElement(By.xpath(".//*[@data-action = 'selectHour' and . = '" + hour + "']")).click();
        return this;
    }

    /**
     * @param minute the minute to select
     * @return this date and time picker
     */
    public BootstrapDateTimePicker selectMinute(String minute)
    {
        this.container.findElement(By.xpath(".//*[@data-action = 'selectMinute' and . = '" + minute + "']")).click();
        return this;
    }

    /**
     * @param second the second to select
     * @return this date and time picker
     */
    public BootstrapDateTimePicker selectSecond(String second)
    {
        this.container.findElement(By.xpath(".//*[@data-action = 'selectSecond' and . = '" + second + "']")).click();
        return this;
    }

    /**
     * Increments the hour.
     * 
     * @return this date and time picker
     */
    public BootstrapDateTimePicker incrementHour()
    {
        this.container.findElement(By.cssSelector("[data-action='incrementHours']")).click();
        return this;
    }

    /**
     * Increments the minutes.
     * 
     * @return this date and time picker
     */
    public BootstrapDateTimePicker incrementMinute()
    {
        this.container.findElement(By.cssSelector("[data-action='incrementMinutes']")).click();
        return this;
    }

    /**
     * Increments the seconds.
     * 
     * @return this date and time picker
     */
    public BootstrapDateTimePicker incrementSecond()
    {
        this.container.findElement(By.cssSelector("[data-action='incrementSeconds']")).click();
        return this;
    }

    /**
     * Decrements the hours.
     * 
     * @return this date and time picker
     */
    public BootstrapDateTimePicker decrementHour()
    {
        this.container.findElement(By.cssSelector("[data-action='decrementHours']")).click();
        return this;
    }

    /**
     * Decrements the minutes.
     * 
     * @return this date and time picker
     */
    public BootstrapDateTimePicker decrementMinute()
    {
        this.container.findElement(By.cssSelector("[data-action='decrementMinutes']")).click();
        return this;
    }

    /**
     * Decrements the seconds.
     * 
     * @return this date and time picker
     */
    public BootstrapDateTimePicker decrementSecond()
    {
        this.container.findElement(By.cssSelector("[data-action='decrementSeconds']")).click();
        return this;
    }

    /**
     * @return the selected hour
     */
    public String getSelectedHour()
    {
        return this.container.findElement(By.className("timepicker-hour")).getText();
    }

    /**
     * @return the selected minute
     */
    public String getSelectedMinute()
    {
        return this.container.findElement(By.className("timepicker-minute")).getText();
    }

    /**
     * @return the selected second
     */
    public String getSelectedSecond()
    {
        return this.container.findElement(By.className("timepicker-second")).getText();
    }

    /**
     * Wait for the widget to appear.
     */
    public BootstrapDateTimePicker waitToLoad()
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
     * Closes the date time picker.
     */
    public void close()
    {
        this.container.findElement(By.xpath("./preceding-sibling::input")).sendKeys(Keys.ESCAPE);
    }
}
