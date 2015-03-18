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
import org.xwiki.watchlist.test.po.editor.WatchlistPreferencesEditPage;

/**
 * Tests Watchlist application features.
 * 
 * @version $Id$
 */
public class AutoWatchTest extends AbstractTest
{
    private String testSpace;

    private String testUser;

    private String existingPageName;

    @Before
    public void setUp()
    {
        this.testUser = RandomStringUtils.randomAlphanumeric(5);
        this.testSpace = this.testUser + "Test";

        this.existingPageName = "existingPage";
        getUtil().loginAsSuperAdmin();
        getUtil().createPage(this.testSpace, existingPageName, null, null);

        getUtil().createUserAndLogin(this.testUser, "password");
    }

    @Test
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See http://jira.xwiki.org/browse/XE-1177")
    public void testAutomaticWatchDefaultAndNone()
    {
        /*
         * Scenario 1: 'Default' autowatch mode should watch new and modified documents.
         */
        WatchlistUserProfilePage watchlistPage = WatchlistUserProfilePage.gotoPage(this.testUser);

        String newPageName1 = "testpage";

        // Ensure the pages are not already watched.
        Assert.assertFalse("The test page should not be already watched when just starting", watchlistPage
            .getWatchlistMacro().isWatched(this.testSpace, newPageName1));
        Assert.assertFalse("The test page should not be already watched when just starting", watchlistPage
            .getWatchlistMacro().isWatched(this.testSpace, this.existingPageName));

        // Set to 'default' automatic watch mode.
        WatchlistPreferencesEditPage preferences = watchlistPage.editPreferences();
        preferences.setAutomaticWatchDefault();
        preferences.clickSaveAndContinue();

        // Create the new page and modify the existing one.
        getUtil().createPage(this.testSpace, newPageName1, null, null);
        getUtil().gotoPage(this.testSpace, this.existingPageName, "save", "content", "Test content");

        // Go back to watchlist profile.
        watchlistPage = WatchlistUserProfilePage.gotoPage(this.testUser);

        // Check if they are registered in the watchlist.
        Assert.assertTrue("Newly created page is not watched",
            watchlistPage.getWatchlistMacro().isWatched(this.testSpace, newPageName1));
        Assert.assertTrue("Newly created page is not watched",
            watchlistPage.getWatchlistMacro().isWatched(this.testSpace, this.existingPageName));

        /*
         * Scenario 2: 'None' autowatch mode should not watch new or modified documents.
         */
        String newPageName2 = "testpage2";

        // Cleanup from the previous test. Assume the existing page is unwatched.
        watchlistPage.getWatchlistMacro().unWatch(this.testSpace, this.existingPageName);

        // Ensure the pages are not already watched.
        Assert.assertFalse("The test page should not be already watched when just starting", watchlistPage
            .getWatchlistMacro().isWatched(this.testSpace, newPageName2));
        Assert.assertFalse("The test page should not be already watched when just starting", watchlistPage
            .getWatchlistMacro().isWatched(this.testSpace, this.existingPageName));

        // Set to 'none' automatic watch mode.
        preferences = watchlistPage.editPreferences();
        preferences.setAutomaticWatchNone();
        preferences.clickSaveAndContinue();

        // Create the new page and modify the existing one.
        getUtil().createPage(this.testSpace, newPageName2, null, null);
        getUtil().gotoPage(this.testSpace, this.existingPageName, "save", "content", "Test content");

        // Go back to watchlist profile
        watchlistPage = WatchlistUserProfilePage.gotoPage(this.testUser);

        // Check if it's registered in the watchlist
        Assert.assertFalse("Newly created page is watched even if autowatch is set to 'none'", watchlistPage
            .getWatchlistMacro().isWatched(this.testSpace, newPageName2));
        Assert.assertFalse("Modified page is watched even if autowatch is set to 'none'", watchlistPage
            .getWatchlistMacro().isWatched(this.testSpace, this.existingPageName));
    }
}
