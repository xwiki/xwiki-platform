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
import org.xwiki.test.ui.po.editor.BootstrapDateTimePicker;

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
        BootstrapDateTimePicker datePicker = dateField.openDatePicker();
        datePicker.changeMonthAndYear().changeYear().showPreviousDecade().selectYear("2015").selectMonth("Oct")
            .selectDay("13");
        datePicker.toggleTimePicker().changeHour().selectHour("08").changeMinute().selectMinute("15").incrementMinute()
            .changeSecond().selectSecond("40").decrementSecond().toggleTimePicker();
        String selectedDate = dateField.getDefaultValue();
        // Ignore the number of seconds.
        Assert.assertEquals(selectedDate, "13/10/2015 08:16:39");

        // Set the value of the date input and assert the date selected by the picker.
        dateField.setDefaultValue("17/03/2020 19:43:34");

        // We need to close and reopen the date picker in order to read the new value.
        datePicker.close();
        datePicker = dateField.openDatePicker();

        Assert.assertEquals("17", datePicker.getSelectedDay());

        datePicker.changeMonthAndYear();
        Assert.assertEquals("2020", datePicker.getSelectedYear());
        Assert.assertEquals("Mar", datePicker.getSelectedMonth());

        datePicker.toggleTimePicker();
        Assert.assertEquals("19", datePicker.getSelectedHour());
        Assert.assertEquals("43", datePicker.getSelectedMinute());
        Assert.assertEquals("34", datePicker.getSelectedSecond());
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
        BootstrapDateTimePicker datePicker = dateField.openDatePicker();
        // The current date format doesn't include time information.
        Assert.assertFalse(datePicker.hasTimePicker());
        datePicker.selectDay("22");
        Calendar now = Calendar.getInstance();
        now.set(Calendar.DAY_OF_MONTH, 22);
        Assert.assertEquals(new SimpleDateFormat(dateFormat).format(now.getTime()), dateField.getDefaultValue());

        // Test if the date picker knows how to parse dates with a custom date format.
        // Set the value of the date input and assert the date selected by the picker.
        dateField.setDefaultValue("2012.11.10");

        // We need to close and reopen the date picker in order to read the new value.
        datePicker.close();
        datePicker = dateField.openDatePicker();

        Assert.assertEquals("10", datePicker.getSelectedDay());
        Assert.assertFalse(datePicker.hasTimePicker());

        datePicker.changeMonthAndYear();
        Assert.assertEquals("2012", datePicker.getSelectedYear());
        Assert.assertEquals("Nov", datePicker.getSelectedMonth());
    }
}
