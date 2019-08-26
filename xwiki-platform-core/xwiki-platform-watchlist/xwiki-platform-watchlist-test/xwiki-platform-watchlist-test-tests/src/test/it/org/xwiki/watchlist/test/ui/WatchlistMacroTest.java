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

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.watchlist.test.po.WatchlistMacro;

/**
 * Tests Watchlist Macro features.
 * 
 * @version $Id$
 */
public class WatchlistMacroTest extends AbstractTest
{
    private WatchlistMacro watchlistMacro;
    private String testSpace;
    private String testPage;

    @Before
    public void setUp() {
        getUtil().loginAsSuperAdmin();
        String userName = RandomStringUtils.randomAlphanumeric(5);
        getUtil().createUserAndLogin(userName, "password");

        this.watchlistMacro = new WatchlistMacro();
        this.testSpace = userName + "Test";
        this.testPage = "TestWatchlist";
    }

    @Test
    public void testWatchAndRemoveEntries() {
        // create a new page
        ViewPage testPageView = getUtil().createPage(this.testSpace, this.testPage, "{{watchlist /}}", "Watch list");

        // check if new page is registered in the watchlist, but not its space
        Assert.assertTrue("Newly created page is not watched",
                this.watchlistMacro.isWatched(this.testSpace, this.testPage));
        Assert.assertFalse("Newly created space is watched", this.watchlistMacro.isWatched(this.testSpace));
        Assert.assertFalse("Complete wiki is watched", this.watchlistMacro.isWikiWatched());

        testPageView.watchSpace();
        testPageView.watchWiki();

        // now need to reload the watchlist; doing so by going away from the page and then back
        getUtil().gotoPage("Main", "WebHome");
        testPageView = getUtil().gotoPage(this.testSpace, this.testPage);

        Assert.assertTrue("Newly created page is not watched",
                this.watchlistMacro.isWatched(this.testSpace, this.testPage));
        Assert.assertTrue("Test space is not watched", this.watchlistMacro.isWatched(this.testSpace));
        Assert.assertTrue("Complete wiki is not watched", this.watchlistMacro.isWikiWatched());

        this.watchlistMacro.unWatch(null, null);
        this.watchlistMacro.unWatch(this.testSpace, this.testPage);

        // change should have taken effect immediately, but instead reload page to avoid timing effects:
        getUtil().gotoPage("Main", "WebHome");
        testPageView = getUtil().gotoPage(this.testSpace, this.testPage);

        Assert.assertFalse("Newly created page is still watched",
                this.watchlistMacro.isWatched(this.testSpace, this.testPage));
        Assert.assertTrue("Test space is not watched", this.watchlistMacro.isWatched(this.testSpace));
        Assert.assertFalse("Complete wiki is still watched", this.watchlistMacro.isWikiWatched());

        this.watchlistMacro.unWatch(this.testSpace, null);

        // next page reload .... see above
        getUtil().gotoPage("Main", "WebHome");
        testPageView = getUtil().gotoPage(this.testSpace, this.testPage);
        Assert.assertFalse("Test space is still watched", this.watchlistMacro.isWatched(this.testSpace));
    }
}
