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

import javax.mail.internet.MimeMessage;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.scheduler.test.po.SchedulerHomePage;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.test.ui.browser.IgnoreBrowser;
import org.xwiki.test.ui.browser.IgnoreBrowsers;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.watchlist.test.po.WatchlistUserProfilePage;
import org.xwiki.watchlist.test.po.editor.WatchlistPreferencesEditPage;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;

import static org.junit.Assert.assertEquals;

public class WatchThisPageAndWholeSpaceTest extends AbstractTest
{
    private static final String PASSWORD = "password";

    private GreenMail greenMail;

    private String testUserName = "TestUser";

    private String testUserName2 = "TestUser2";

    /**
     * Set the test user's email address to use a localhost domain so that the mail is caught by our GreenMail Mock mail
     * server.
     */
    private String testEmail = "test@localhost";

    @Rule
    public SuperAdminAuthenticationRule superAdminAuthenticationRule = new SuperAdminAuthenticationRule(getUtil());

    private String testSinglePageToWatchSpace = "Test";

    private String testSinglePageToWatch = "TestWatchThisPage";

    private String testSpaceToWatch = "TestWatchWholeSpace";

    private String testSpaceToWatchPage = "Test1";

    @Before
    public void setUp() throws Exception
    {
        // Set the SMTP port to the default port used by Greenmail (3025)
        getUtil().updateObject("Mail", "MailConfig", "Mail.SendMailConfigClass", 0, "host", "localhost", "port",
            "3025", "sendWaitTime", "0");

        // Start GreenMail test server
        this.greenMail = new GreenMail();
        this.greenMail.start();

        // Delete the users if they already exists.
        getUtil().deletePage("XWiki", testUserName);
        getUtil().deletePage("XWiki", testUserName2);

        // Delete the test pages.
        getUtil().deletePage(testSinglePageToWatchSpace, testSinglePageToWatch);
        getUtil().deletePage(testSpaceToWatch, testSpaceToWatchPage);

        // Create a user for the test.
        getUtil().createUserAndLogin(testUserName, PASSWORD, "email", testEmail);

        WatchlistUserProfilePage watchlistPage = WatchlistUserProfilePage.gotoPage(testUserName);

        // Disable auto watch.
        WatchlistPreferencesEditPage watchlistPreferences = watchlistPage.editPreferences();
        watchlistPreferences.setAutomaticWatchNone();
        watchlistPreferences.clickSaveAndContinue();
    }

    @After
    public void tearDown()
    {
        // Stop GreenMail test server
        this.greenMail.stop();

        // Make sure we can restore the settings, so we log back with superadmin to finish the work
        getUtil().loginAsSuperAdmin();

        // Remove the previous version that the setup has created.
        getUtil().deleteLatestVersion("Mail", "MailConfig");
    }

    @Test
    @IgnoreBrowsers({
        @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason = "See http://jira.xwiki.org/browse/XE-1146"),
        @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See http://jira.xwiki.org/browse/XE-1177")})
    public void testWatchThisPageAndWholeSpace() throws Exception
    {
        // Clear the list of watched documents and spaces
        getUtil().updateObject("XWiki", this.testUserName, "XWiki.WatchListClass", 0, "spaces", "", "documents", "");

        // Watch Test.TestWatchThisPage
        ViewPage page =
            getUtil().createPage(testSinglePageToWatchSpace, testSinglePageToWatch, "TestWatchThisPage ui", null);
        page.watchDocument();

        // Watch TestWatchWholeSpace.Test1
        page = getUtil().createPage(testSpaceToWatch, testSpaceToWatchPage, "TestWatchWholeSpace ui", null);
        page.watchSpace();

        // Verify that the watched page & space are present in the watchlist manager
        WatchlistUserProfilePage watchlistPage = WatchlistUserProfilePage.gotoPage(this.testUserName);
        Assert.assertTrue(watchlistPage.getWatchlistMacro()
            .isWatched(testSinglePageToWatchSpace, testSinglePageToWatch));
        Assert.assertTrue(watchlistPage.getWatchlistMacro().isWatched(testSpaceToWatch));

        // Ensure that the watchlist notified is set to Daily since we're going to trigger that notifier scheduler job
        WatchlistPreferencesEditPage watchlistPreferences = watchlistPage.editPreferences();
        watchlistPreferences.setNotifierDaily();
        watchlistPreferences.clickSaveAndContinue();

        // Switch to superadmin user and go to the scheduler home page
        SchedulerHomePage schedulerHomePage = new SchedulerHomePage();
        getUtil().loginAsSuperAdminAndGotoPage(schedulerHomePage.getURL());

        // Trigger the notification for the Daily job
        schedulerHomePage.clickJobActionTrigger("WatchList daily notifier");

        // Wait for the email with a timeout
        Assert.assertTrue("Scheduled notification mail not received", this.greenMail.waitForIncomingEmail(70000, 1));

        // Verify email content
        MimeMessage[] receivedMails = this.greenMail.getReceivedMessages();
        assertEquals(1, receivedMails.length);
        String messageFromXWiki = GreenMailUtil.getBody(receivedMails[0]).replaceAll("=\r?\n", "");
        Assert.assertFalse("should have no exception in " + messageFromXWiki, messageFromXWiki.contains("Exception"));
        Assert.assertTrue("should have test page in the message " + messageFromXWiki,
            messageFromXWiki.contains(testSinglePageToWatch));
        Assert.assertTrue("should have test space in the message " + messageFromXWiki,
            messageFromXWiki.contains(testSpaceToWatch));

        // // Clear the mock inbox for the following step.
        // GreenMailUser mailUser = this.greenMail.getManagers().getUserManager().getUser("admin@localhost");
        // this.greenMail.getManagers().getImapHostManager().deleteMailbox(mailUser, "INBOX");
        // TODO: we might need the commented code above to make sure the mail is destined for our current test user and
        // not other users created by previous tests.

        /*
         * Realtime notifications.
         */

        // Log back in with the user to test realtime notifications.
        getUtil().login(testUserName, PASSWORD);

        // Set the notifier to 'Realtime'.
        WatchlistUserProfilePage profilePage = WatchlistUserProfilePage.gotoPage(testUserName);
        watchlistPage = profilePage.switchToWatchlist();

        watchlistPreferences = watchlistPage.editPreferences();
        watchlistPreferences.setNotifierRealtime();
        watchlistPreferences.clickSaveAndContinue();

        // Make a change in a watched document.
        // Note: Taking a shortcut and just using the save action.
        String content = "New content that watchlist should ignore.";
        getUtil().gotoPage(testSinglePageToWatchSpace, testSinglePageToWatch, "save", "content", content);

        // Wait for an email that should never come.
        Assert.assertFalse("Mail should not be received for own changes", this.greenMail.waitForIncomingEmail(3000, 2));

        // Create a second user that should trigger a realtime notification to the first user.
        getUtil().createUserAndLogin(testUserName2, PASSWORD);

        // Make a change in a document watched by the first user.
        // Note: Taking a shortcut and just using the save action.
        String newContent = "New content that watchlist should notify about.";
        content += "\n" + newContent;
        getUtil().gotoPage(testSinglePageToWatchSpace, testSinglePageToWatch, "save", "content", content);

        // Wait for the email with a timeout.
        Assert.assertTrue("Realtime notification mail not received", this.greenMail.waitForIncomingEmail(70000, 2));

        // Verify email content.
        receivedMails = this.greenMail.getReceivedMessages();
        assertEquals(2, receivedMails.length);
        messageFromXWiki = GreenMailUtil.getBody(receivedMails[1]).replaceAll("=\r?\n", "");
        Assert.assertFalse("should have no exception in " + messageFromXWiki, messageFromXWiki.contains("Exception"));
        Assert.assertTrue("should have test page in the message " + messageFromXWiki,
            messageFromXWiki.contains("TestWatchThisPage"));
        Assert.assertTrue("should have test content in the message " + messageFromXWiki,
            messageFromXWiki.contains(newContent));
    }
}
