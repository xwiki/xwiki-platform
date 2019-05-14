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
package org.xwiki.test.ui;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.administration.test.po.GlobalRightsAdministrationSectionPage;
import org.xwiki.test.ui.browser.IgnoreBrowser;
import org.xwiki.test.ui.browser.IgnoreBrowsers;
import org.xwiki.test.ui.po.LoginPage;
import org.xwiki.test.ui.po.ResubmissionPage;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

/**
 * Test the Login feature.
 *
 * @version $Id$
 * @since 2.3M1
 */
public class LoginTest extends AbstractTest
{
    private ViewPage vp;

    private String nonExistentPageURL;

    @Before
    public void setUp()
    {
        // Force log out (we're using the fast way since this is not part of what we want to test)
        getUtil().forceGuestUser();

        // Go to any page in view mode. We choose to go to a nonexisting page so that it loads as fast as possible
        // Note: since the page doesn't exist, we need to disable the space redirect feature so that we end up on the
        // terminal page and not on WebHome in the space.
        getUtil().gotoPage("NonExistentSpace", "NonExistentPage", "view", "spaceRedirect=false");
        this.vp = new ViewPage();
        this.nonExistentPageURL = getDriver().getCurrentUrl();
    }

    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    })
    public void testLoginLogoutAsAdmin()
    {
        LoginPage loginPage = this.vp.login();
        loginPage.loginAsAdmin();

        // Verify that after logging in we're redirected to the page on which the login button was clicked, i.e. the
        // non existent page here.
        Assert.assertEquals(this.nonExistentPageURL, getDriver().getCurrentUrl());

        Assert.assertTrue(this.vp.isAuthenticated());
        Assert.assertEquals("Administrator", this.vp.getCurrentUser());

        // Test Logout and verify we stay on the same page
        this.vp.logout();
        Assert.assertFalse(this.vp.isAuthenticated());
        Assert.assertEquals(this.nonExistentPageURL, getDriver().getCurrentUrl());
    }

    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    })
    public void testLoginWithInvalidCredentials()
    {
        LoginPage loginPage = this.vp.login();
        loginPage.loginAs("Admin", "wrong password");
        Assert.assertTrue(loginPage.hasInvalidCredentialsErrorMessage());
    }

    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    })
    public void testLoginWithInvalidUsername()
    {
        LoginPage loginPage = this.vp.login();
        loginPage.loginAs("non existent user", "admin");
        Assert.assertTrue(loginPage.hasInvalidCredentialsErrorMessage());
    }

    /**
     * Verify that the initial URL is not lost after logging in when the session has expired.
     * See XWIKI-5317.
     */
    @Test
    public void testRedirectBackAfterLogin()
    {
        try {
            // Test setup: disallow view right for unauthenticated users. We need to be logged as admin in order to
            // do that. Since this is not what we are testing use the fast way to log in
            GlobalRightsAdministrationSectionPage grasp = new GlobalRightsAdministrationSectionPage();
            getDriver().get(getUtil().getURLToLoginAsAdminAndGotoPage(grasp.getURL()));
            getUtil().recacheSecretToken();
            getUtil().setDefaultCredentials(TestUtils.ADMIN_CREDENTIALS);
            grasp.forceAuthenticatedView();

            // Go to a page, log out and expire session by removing cookies, log in again and verify that the user is
            // redirected to the initial page.
            ViewPage page = getUtil().gotoPage("SomeSpace", "SomePage");
            page.logout();
            // Since view is disallowed for unauthenticated users, at this point we see a log in page.
            LoginPage loginPage = new LoginPage();
            // Remove all cookie to simulate a session expiry
            getDriver().manage().deleteAllCookies();
            loginPage.loginAsAdmin();

            // We use startsWith since the URL contains a jsessionid and a srid.
            Assert.assertTrue(getDriver().getCurrentUrl().startsWith(getUtil().getURL("SomeSpace", "SomePage")));
        } finally {
            GlobalRightsAdministrationSectionPage grasp = GlobalRightsAdministrationSectionPage.gotoPage();
            if (!grasp.isAuthenticated()) {
                getDriver().get(getUtil().getURLToLoginAsAdminAndGotoPage(grasp.getURL()));
            }
            grasp.unforceAuthenticatedView();
        }
    }

    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    })
    public void testLogoutDuringEditDisplayLoginModal()
    {
        String test = "Test string " + System.currentTimeMillis();
        final String space = "Main";
        final String page = "POSTTest";
        LoginPage loginPage = this.vp.login();
        loginPage.loginAsAdmin();
        // start editing a page
        WikiEditPage editPage = WikiEditPage.gotoPage(space, page);
        editPage.setTitle(test);
        editPage.setContent(test);
        // emulate expired session: delete the cookies
        getDriver().manage().deleteAllCookies();
        // try to save
        editPage.clickSaveAndView(false);
        Assert.assertTrue(editPage.loginModalDisplayed());
        String mainWindow = getDriver().getWindowHandle();
        loginPage = editPage.clickModalLoginLink();
        loginPage.loginAsAdmin();
        getDriver().switchTo().window(mainWindow);
        editPage = new WikiEditPage();
        editPage.closeLoginModal();
        ViewPage viewPage = editPage.clickSaveAndView();

        Assert.assertEquals(test, viewPage.getDocumentTitle());
        Assert.assertEquals(test, viewPage.getContent());
    }

    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    })
    public void testCorrectUrlIsAccessedAfterLogin()
    {
        // We will choose the Scheduler.WebHome page to make our testing
        // since it can't be viewed without being logged in
        getUtil().gotoPage("Scheduler", "WebHome");
        LoginPage loginPage = new LoginPage();
        Assert.assertFalse(loginPage.isAuthenticated());
        loginPage.loginAsAdmin();
        // We should be redirected back to Scheduler.WebHome
        Assert.assertTrue(getDriver().getCurrentUrl().contains("/xwiki/bin/view/Scheduler/WebHome"));
        Assert.assertTrue(getDriver().getTitle().contains("Job Scheduler"));
    }

    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    })
    public void testDataIsPreservedAfterLogin()
    {
        getUtil().gotoPage("Test", "TestData", "save", "content=this+should+not+be+saved");
        getUtil().gotoPage("Test", "TestData", "save", "content=this+should+be+saved+instead&parent=Main.WebHome");
        LoginPage loginPage = new LoginPage();
        loginPage.loginAsAdmin();
        // we switched to another user, CSRF protection (if enabled) will ask for confirmation
        ResubmissionPage resubmissionPage = new ResubmissionPage();
        if (resubmissionPage.isOnResubmissionPage()) {
            resubmissionPage.resubmit();
        }
        Assert.assertTrue(getDriver().getCurrentUrl().contains("/xwiki/bin/view/Test/TestData"));
        ViewPage viewPage = new ViewPage();
        Assert.assertEquals("this should be saved instead", viewPage.getContent());

        // Expected logs
        this.validateConsole.getLogCaptureConfiguration().registerExpected(
            "CSRFToken: Secret token verification failed, token:");
    }
}
