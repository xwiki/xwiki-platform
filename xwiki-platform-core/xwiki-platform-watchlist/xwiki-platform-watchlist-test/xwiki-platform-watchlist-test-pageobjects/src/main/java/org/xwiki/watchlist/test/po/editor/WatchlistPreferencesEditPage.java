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
package org.xwiki.watchlist.test.po.editor;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;
import org.xwiki.test.ui.po.editor.EditPage;

/** User profile, the watchlist pane, edit mode. */
public class WatchlistPreferencesEditPage extends EditPage
{
    public static final String AUTOMATICWATCH_DEFAULT = "default";

    public static final String AUTOMATICWATCH_NONE = "NONE";

    public static final String AUTOMATICWATCH_ALL = "ALL";

    public static final String AUTOMATICWATCH_MAJOR = "MAJOR";

    public static final String AUTOMATICWATCH_NEW = "NEW";

    public static final String NOTIFIER_REALTIME = "realtime";

    public static final String NOTIFIER_HOURLY = "Scheduler.WatchListHourlyNotifier";

    public static final String NOTIFIER_DAILY = "Scheduler.WatchListDailyNotifier";

    public static final String NOTIFIER_WEEKLY = "Scheduler.WatchListWeeklyNotifier";

    @FindBy(name = "XWiki.WatchListClass_0_interval")
    private WebElement notifier;

    @FindBy(id = "XWiki.WatchListClass_0_automaticwatch")
    private WebElement automaticwatch;

    public void setAutomaticWatchDefault()
    {
        Select select = new Select(this.automaticwatch);
        select.selectByValue(AUTOMATICWATCH_DEFAULT);
    }

    public void setAutomaticWatchNone()
    {
        Select select = new Select(this.automaticwatch);
        select.selectByValue(AUTOMATICWATCH_NONE);
    }

    public void setAutomaticWatchAll()
    {
        Select select = new Select(this.automaticwatch);
        select.selectByValue(AUTOMATICWATCH_ALL);
    }

    public void setAutomaticWatchMajor()
    {
        Select select = new Select(this.automaticwatch);
        select.selectByValue(AUTOMATICWATCH_MAJOR);
    }

    public void setAutomaticWatchNew()
    {
        Select select = new Select(this.automaticwatch);
        select.selectByValue(AUTOMATICWATCH_NEW);
    }

    public void setNotifierRealtime()
    {
        Select select = new Select(this.notifier);
        select.selectByValue(NOTIFIER_REALTIME);
    }

    public void setNotifierHourly()
    {
        Select select = new Select(this.notifier);
        select.selectByValue(NOTIFIER_HOURLY);
    }

    public void setNotifierDaily()
    {
        Select select = new Select(this.notifier);
        select.selectByValue(NOTIFIER_DAILY);
    }

    public void setNotifierWeekly()
    {
        Select select = new Select(this.notifier);
        select.selectByValue(NOTIFIER_WEEKLY);
    }
}
