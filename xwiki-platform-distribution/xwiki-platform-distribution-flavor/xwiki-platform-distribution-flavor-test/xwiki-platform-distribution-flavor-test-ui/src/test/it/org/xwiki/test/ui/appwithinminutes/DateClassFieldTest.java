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
package org.xwiki.test.ui.appwithinminutes;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.junit.Assert;
import org.junit.Test;
import org.xwiki.appwithinminutes.test.po.DateClassFieldEditPane;
import org.xwiki.test.ui.browser.IgnoreBrowser;
import org.xwiki.test.ui.browser.IgnoreBrowsers;
import org.xwiki.test.ui.po.editor.DatePicker;

/**
 * Special class editor tests that address only the Date class field type.
 * 
 * @version $Id$
 * @since 3.5
 */
public class DateClassFieldTest extends AbstractClassEditorTest
{
    /**
     * Tests that the user can select a date using the date picker.
     */
    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    })
    public void testDatePicker()
    {
        // First select a date using the picker and assert the value of the date input.
        DateClassFieldEditPane dateField = new DateClassFieldEditPane(editor.addField("Date").getName());
        DatePicker datePicker = dateField.openDatePicker();
        datePicker.setYear("2011");
        datePicker.setMonth("October");
        datePicker.setDay("13");
        datePicker.setHour("8 AM");
        datePicker.setMinute("15");
        String selectedDate = dateField.getDefaultValue();
        // Ignore the number of seconds.
        Assert.assertTrue(String.format("The selected date [%s] doesn't start with [13/10/2011 08:15:].", selectedDate),
            selectedDate.startsWith("13/10/2011 08:15:"));

        // Set the value of the date input and assert the date selected by the picker.
        dateField.setDefaultValue("17/03/2020 19:43:34");

        // Currently the date picker doesn't know how to parse a date with a specified format. The workaround is to pass
        // the date time stamp when generating the date input, but for this the page needs to be saved and reloaded.
        editor.clickSaveAndView().edit();
        datePicker = new DateClassFieldEditPane("date1").openDatePicker();

        Assert.assertEquals("2020", datePicker.getYear());
        Assert.assertEquals("March", datePicker.getMonth());
        Assert.assertEquals("17", datePicker.getDay());
        Assert.assertEquals("7 PM", datePicker.getHour());
        Assert.assertEquals("40", datePicker.getMinute());
    }

    /**
     * Tests that the date picker can parse dates using the specified date format and that the selected date is
     * serialized using the specified date format.
     */
    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    })
    public void testDateFormat()
    {
        // Add a date field and change the date format.
        DateClassFieldEditPane dateField = new DateClassFieldEditPane(editor.addField("Date").getName());
        dateField.openConfigPanel();
        String dateFormat = "yyyy.MM.dd";
        dateField.setDateFormat(dateFormat);

        // Close the configuration panel to refresh the date field preview.
        dateField.closeConfigPanel();

        // Select a date using the date picker.
        DatePicker datePicker = dateField.openDatePicker();
        // The current date format doesn't include time information.
        Assert.assertFalse(datePicker.hasHourSelector());
        datePicker.setDay("22");
        Calendar now = Calendar.getInstance();
        now.set(Calendar.DAY_OF_MONTH, 22);
        Assert.assertEquals(new SimpleDateFormat(dateFormat).format(now.getTime()), dateField.getDefaultValue());

        // Test if the date picker knows how to parse dates with a custom date format.
        // Set the value of the date input and assert the date selected by the picker.
        dateField.setDefaultValue("2012.11.10");

        // Currently the date picker doesn't know how to parse a date with a specified format. The workaround is to pass
        // the date time stamp when generating the date input, but for this the page needs to be saved and reloaded.
        editor.clickSaveAndView().edit();
        datePicker = new DateClassFieldEditPane("date1").openDatePicker();

        Assert.assertEquals("2012", datePicker.getYear());
        Assert.assertEquals("November", datePicker.getMonth());
        Assert.assertEquals("10", datePicker.getDay());
        Assert.assertFalse(datePicker.hasHourSelector());
    }
}
