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

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;
import org.xwiki.test.ui.po.LiveTableElement;

/**
 * Represents a WatchList macro visible on the current page
 * 
 * @version $Id$
 * @since 6.0M1
 */
public class WatchlistMacro extends BaseElement
{

    /**
     * a formatter to create a XPath expression to a "remove from watchlist" link. The arguments to pass to this
     * formatter are: removal action and reference to removed.
     */
    private static final String WATCHLIST_REMOVE_BUTTON_PATTERN_CSS 
    	= "tbody#mywatchlist-display>tr>td>a[href$='?xpage=watch&do=%s&reference=%s']";

    public LiveTableElement getWatchList() {
        LiveTableElement liveTableElement = new LiveTableElement("mywatchlist");
        liveTableElement.waitUntilReady();

        return liveTableElement;
    }

    public boolean isWatched(String space, String page) {
        // Make sure the livetable is loaded
        getWatchList();

        return getUtil().hasElement(removeLink(space, page));
    }

    public boolean isWatched(String space) {
        return this.isWatched(space, null);
    }

    public boolean isWikiWatched() {
        return this.isWatched(null, null);
    }

    /**
     * Unregister a document or a space from the watchlist.
     * 
     * @param space
     *            the space name, if null the method tries to unregister the main wiki
     * @param page
     *            the page name, if null the methods tries to unregister a space
     * @return true if something has been unregistered, false otherwise
     */
    public boolean unWatch(String space, String page) {
        getWatchList();

        List<WebElement> links = getDriver().findElements(removeLink(space, page));
        if (links.isEmpty()) {
            return false;
        }

        links.get(0).click();
        // FIXME: now we should wait for the ajax call to finish and the livetable to reload
        // this would be easier if we get any feedback about removing items from the watchlist
        // waitForNotificationSuccessMessage(message);
        return true;
    }

    private By removeLink(String space, String page) {
        By removeLink;
        if (space == null) {
            removeLink = By.cssSelector(String.format(WATCHLIST_REMOVE_BUTTON_PATTERN_CSS, "removewiki", "xwiki" ));
        } else if (page == null) {
            removeLink = By.cssSelector(String.format(WATCHLIST_REMOVE_BUTTON_PATTERN_CSS, "removespace", "xwiki%3A"+space));
        } else {
            removeLink = By.cssSelector(String.format(WATCHLIST_REMOVE_BUTTON_PATTERN_CSS, "removedocument", "xwiki%3A"+space+'.'+ page ));
        }
        return removeLink;
    }

}
