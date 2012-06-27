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
package org.xwiki.watchlist.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.administration.test.po.AbstractUserProfilePage;
import org.xwiki.test.ui.po.LiveTableElement;
import org.xwiki.watchlist.test.po.editor.WatchlistPreferencesEditPage;

/**
 * Represents the User Profile Watchlist Tab.
 * 
 * @version $Id$
 */
public class WatchlistUserProfilePage extends AbstractUserProfilePage
{
    @FindBy(xpath = "//div[@id='watchlistPane']//div[@class='editProfileCategory']/a")
    private WebElement editPreferences;

    @FindBy(xpath = "//div[@class='watchlistManagement']/dl[1]/dd[1]")
    private WebElement notifier;

    @FindBy(xpath = "//div[@class='watchlistManagement']/dl[1]/dd[2]")
    private WebElement automaticwatch;

    public static WatchlistUserProfilePage gotoPage(String username)
    {
        WatchlistUserProfilePage page = new WatchlistUserProfilePage(username);
        getUtil().gotoPage("XWiki", page.getUsername(), "view", "category=watchlist");
        return page;
    }

    public WatchlistUserProfilePage(String username)
    {
        super(username);
    }

    public String getNotifier()
    {
        return this.notifier.getText();
    }

    public String getAutomaticWatch()
    {
        return this.automaticwatch.getText();
    }

    public LiveTableElement getWatchList()
    {
        LiveTableElement liveTableElement = new LiveTableElement("mywatchlist");
        liveTableElement.waitUntilReady();

        return liveTableElement;
    }

    /**
     * Unregister a document or a space from the watchlist.
     * 
     * @param space the space name, can't be null
     * @param page the page name, if null the methods tries to unregister a space
     * @return true if something has been unregistered, false otherwise
     */
    public boolean unWatch(String space, String page)
    {
        return false;
    }

    public boolean isWatched(String space, String page)
    {
        // Make sure the livetable is loaded
        getWatchList();

        // TODO: only looks in the first livetable page currently
        // TODO: move a generic version of that in LiveTableElement
        return getUtil().hasElement(
            By.xpath("//tbody[@id='mywatchlist-display']/tr/td/a[@href='/xwiki/bin/view/" + space + "/" + page + "']"));
    }

    public boolean isWatched(String space)
    {
        // TODO: only looks in the first livetable page currently
        // TODO: move a generic version of that in LiveTableElement
        return getUtil().hasElement(
            By.xpath("//tbody[@id='mywatchlist-display']/tr/td/a[@href='/xwiki/bin/view/" + space + "/WebHome']"));
    }

    public WatchlistPreferencesEditPage editPreferences()
    {
        this.editPreferences.click();
        return new WatchlistPreferencesEditPage();
    }
}
