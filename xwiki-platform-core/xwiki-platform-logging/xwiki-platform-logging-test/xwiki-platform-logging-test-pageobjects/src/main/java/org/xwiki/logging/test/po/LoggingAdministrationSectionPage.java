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
package org.xwiki.logging.test.po;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.xwiki.administration.test.po.AdministrationSectionPage;
import org.xwiki.livedata.test.po.LiveDataElement;
import org.xwiki.livedata.test.po.TableLayoutElement;

/**
 * Represents the Logging administration section, where the level of each registered logger can be viewed and changed
 * (or reset to its default). The loggers are displayed in a Live Data table with one row per logger; each row exposes,
 * in its "Actions" column, a form to select a new level and submit it.
 *
 * @version $Id$
 * @since 17.10.10
 * @since 18.4.3
 * @since 18.5.0RC1
 */
public class LoggingAdministrationSectionPage extends AdministrationSectionPage
{
    private static final String SECTION_ID = "Logging";

    private static final String LIVE_DATA_ID = "logging";

    /**
     * Live Data column label (English) for the logger name.
     */
    private static final String COLUMN_LOGGER = "Logger";

    /**
     * Live Data column label (English) for the logger level.
     */
    private static final String COLUMN_LEVEL = "Level";

    /**
     * Live Data column label (English) for the actions form. This column is only displayed when the current user has
     * programming right.
     */
    private static final String COLUMN_ACTIONS = "Actions";

    private static final By LEVEL_SELECT = By.cssSelector("select[name='logger_level']");

    private static final By SET_BUTTON = By.cssSelector("input[name='loggeraction_set']");

    /**
     * Open the Logging administration section.
     *
     * @return the Logging administration section
     */
    public static LoggingAdministrationSectionPage gotoPage()
    {
        AdministrationSectionPage.gotoPage(SECTION_ID);
        return new LoggingAdministrationSectionPage();
    }

    public LoggingAdministrationSectionPage()
    {
        super(SECTION_ID);
    }

    /**
     * @return the table layout of the Live Data listing the loggers
     */
    public TableLayoutElement getLiveDataTable()
    {
        return new LiveDataElement(LIVE_DATA_ID).getTableLayout();
    }

    /**
     * @param rowNumber the row number, starting at 1
     * @return the logger name displayed in the given row
     */
    public String getLogger(int rowNumber)
    {
        return getLiveDataTable().getCell(COLUMN_LOGGER, rowNumber).getText().trim();
    }

    /**
     * @param rowNumber the row number, starting at 1
     * @return the level displayed in the given row (empty when the logger uses its default level)
     */
    public String getLevel(int rowNumber)
    {
        return getLiveDataTable().getCell(COLUMN_LEVEL, rowNumber).getText().trim();
    }

    /**
     * @return {@code true} if the {@code logger}, {@code level} and {@code actions} columns are all displayed
     */
    public boolean hasAllColumns()
    {
        TableLayoutElement table = getLiveDataTable();
        return table.hasColumn(COLUMN_LOGGER) && table.hasColumn(COLUMN_LEVEL) && table.hasColumn(COLUMN_ACTIONS);
    }

    /**
     * Set the level of the logger displayed in the given row and submit the form. The page is reloaded as a result, so
     * any previously obtained Live Data page object becomes stale.
     *
     * @param rowNumber the row number, starting at 1
     * @param level the level value to select (e.g. {@code "DEBUG"}, or {@code ""} to reset the logger to its default
     *            level). The selected value must be one of the options offered by the form: the option matching the
     *            logger's current level is not present, and the "default" option (empty value) is only present when a
     *            level is currently set.
     */
    public void setLevel(int rowNumber, String level)
    {
        WebElement actionsCell = getLiveDataTable().getCell(COLUMN_ACTIONS, rowNumber);
        new Select(actionsCell.findElement(LEVEL_SELECT)).selectByValue(level);
        getDriver().addPageNotYetReloadedMarker();
        actionsCell.findElement(SET_BUTTON).click();
        getDriver().waitUntilPageIsReloaded();
    }

    /**
     * Locate the row of a logger by its exact name, filtering the Live Data on the logger column first so that the
     * logger is displayed even if it isn't on the first page.
     *
     * @param loggerName the exact logger name to look for
     * @return the row number (starting at 1) of the logger
     * @throws NoSuchElementException if no row matches the logger name exactly
     */
    public int findRowForLogger(String loggerName)
    {
        TableLayoutElement table = getLiveDataTable();
        table.filterColumn(COLUMN_LOGGER, loggerName);
        List<WebElement> cells = table.getAllCells(COLUMN_LOGGER);
        for (int i = 0; i < cells.size(); i++) {
            if (loggerName.equals(cells.get(i).getText().trim())) {
                return i + 1;
            }
        }
        throw new NoSuchElementException(String.format("No logger row found for [%s]", loggerName));
    }

    /**
     * @return the text of the on-page success message displayed after a level has been set or unset
     */
    public String getSuccessMessage()
    {
        return getDriver().findElement(By.cssSelector(".box.successmessage")).getText().trim();
    }
}
