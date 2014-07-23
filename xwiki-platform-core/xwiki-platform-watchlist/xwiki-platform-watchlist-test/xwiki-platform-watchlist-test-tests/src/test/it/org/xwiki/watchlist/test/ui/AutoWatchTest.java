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
package org.xwiki.watchlist.test.ui;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.browser.IgnoreBrowser;
import org.xwiki.watchlist.test.po.WatchlistUserProfilePage;

/**
 * Tests Watchlist application features.
 * 
 * @version $Id$
 */
public class AutoWatchTest extends AbstractTest
{
    private WatchlistUserProfilePage watchlistPage;

    private String testSpace;

    @Before
    public void setUp()
    {
        String userName = RandomStringUtils.randomAlphanumeric(5);

        getUtil().createUserAndLogin(userName, "password");
        WatchlistUserProfilePage profilePage = WatchlistUserProfilePage.gotoPage(userName);

        this.watchlistPage = profilePage.switchToWatchlist();

        this.testSpace = this.watchlistPage.getUsername() + "Test";
    }

    @Test
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See http://jira.xwiki.org/browse/XE-1177")
    public void testAutomaticWatchNewPage()
    {
        // create a new page
        getUtil().createPage(this.testSpace, "testpage", null, null);

        // go back to watchlist profile
        this.watchlistPage = WatchlistUserProfilePage.gotoPage(this.watchlistPage.getUsername());

        // check if it's registered in the watchlist
        Assert
            .assertTrue("Newly created page is not watched", this.watchlistPage.getWatchlistMacro().isWatched(this.testSpace, "testpage"));
    }
}
