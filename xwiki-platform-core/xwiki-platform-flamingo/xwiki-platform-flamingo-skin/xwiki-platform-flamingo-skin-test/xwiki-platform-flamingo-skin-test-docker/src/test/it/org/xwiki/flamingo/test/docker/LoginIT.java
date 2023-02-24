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
package org.xwiki.flamingo.test.docker;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.administration.test.po.GlobalRightsAdministrationSectionPage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.integration.junit.LogCaptureConfiguration;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.XWikiWebDriver;
import org.xwiki.test.ui.po.LoginPage;
import org.xwiki.test.ui.po.ResubmissionPage;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test the Login feature.
 *
 * @version $Id$
 * @since 2.3M1
 */
@UITest(
    properties = {
        // The RightsManagerPlugin is needed to change rights in the UI
        "xwikiCfgPlugins=com.xpn.xwiki.plugin.rightsmanager.RightsManagerPlugin"
    }
)
class LoginIT
{
    @BeforeAll
    static void beforeAll(TestUtils setup)
    {
        // By default, the minimal distribution used for the tests doesn't have any rights set up. For this test class
        // we'll need:
        // - Make sure that only authenticated users can edit/save content (for test dataIsPreservedAfterLogin below).
        // - Create an Admin user.
        setup.loginAsSuperAdmin();
        setup.setGlobalRights("XWiki.XWikiAllGroup", "", "edit", true);
        setup.createAdminUser();
    }

    @Test
    @Order(1)
    void loginLogoutAsAdmin(TestUtils setup, TestReference testReference)
    {
        // Go to any page in view mode. We choose to go to a non-existing page so that it loads as fast as possible
        // Note: since the page doesn't exist, we need to disable the space redirect feature so that we end up on the
        // terminal page and not on WebHome in the space.
        setup.deletePage(testReference);
        setup.gotoPage(testReference, "view", "spaceRedirect=false");

        // Force log out (we're using the fast way since this is not part of what we want to test)
        setup.forceGuestUser();

        // Test the click on the login button in the UI and log in as Admin
        ViewPage vp = new ViewPage();
        LoginPage loginPage = vp.login();
        loginPage.loginAsAdmin();

        // Verify that after logging in we're redirected to the page on which the login button was clicked, i.e. the
        // non existent page here.
        setup.assertOnPage(testReference);
        assertTrue(vp.isAuthenticated());

        // Also verify that we display the logged-in user name in the UI (in the drawer menu)
        assertEquals("Admin", vp.getCurrentUser());

        // Test Logout and verify we stay on the same page
        vp.logout();
        assertFalse(vp.isAuthenticated());
        setup.assertOnPage(testReference);
    }

    @Test
    @Order(2)
    void loginWithInvalidCredentials(LogCaptureConfiguration logCaptureConfiguration)
    {
        LoginPage.gotoPage();
        LoginPage loginPage = new LoginPage();
        loginPage.loginAs("Admin", "wrong password");
        assertTrue(loginPage.hasInvalidCredentialsErrorMessage());
        logCaptureConfiguration.registerExpected("Authentication failure with login [Admin]");

        loginPage.loginAs("non existent user", "admin");
        assertTrue(loginPage.hasInvalidCredentialsErrorMessage());
        logCaptureConfiguration.registerExpected("Authentication failure with login [non existent user]");
    }

    /**
     * Verify that the initial URL is not lost after logging in when the session has expired.
     * See XWIKI-5317.
     */
    @Test
    @Order(3)
    void redirectBackAfterLogin(TestUtils setup, XWikiWebDriver driver, TestReference testReference)
    {
        try {
            // Test setup: disallow view right for unauthenticated users. Note that we use the UI to perform this
            // since this allows us to verify that the UI works.
            setup.loginAsSuperAdmin();
            GlobalRightsAdministrationSectionPage grasp = GlobalRightsAdministrationSectionPage.gotoPage();
            grasp.forceAuthenticatedView();

            // Go to a page, log out and expire session by removing cookies, log in again and verify that the user is
            // redirected to the initial page.
            setup.deletePage(testReference);
            ViewPage page = setup.gotoPage(testReference);
            page.logout();
            // Since view is disallowed for unauthenticated users, at this point we see a log in page.
            LoginPage loginPage = new LoginPage();
            loginPage.assertOnPage();
            // Remove all cookies to simulate a session expiry
            driver.manage().deleteAllCookies();

            // Log in again and verify we're redirected to the right page.
            loginPage.loginAsAdmin();
            setup.assertOnPage(testReference);
        } finally {
            // Make sure we're logged-in since the test could fail when we're not logged in and we need to be admin
            // to go to the Rights UI.
            setup.loginAsSuperAdmin();
            GlobalRightsAdministrationSectionPage grasp = GlobalRightsAdministrationSectionPage.gotoPage();
            grasp.unforceAuthenticatedView();
            // Make sure to log out to leave a deterministic state for the following tests.
            setup.gotoPage(setup.getURLToLogout());
        }
    }

    @Test
    @Order(4)
    void correctUrlIsAccessedAfterLogin(TestUtils setup, TestReference testReference)
    {
        // Create a page that can only be viewed by logged-in users.
        setup.loginAsAdmin();
        setup.deletePage(testReference);
        setup.createPage(testReference, "whatever");
        setup.setRights(testReference, null, "XWiki.XWikiGuest", "view", false);
        setup.forceGuestUser();

        // Navigate to it and verify we're asked to log in and that we're not logged-in.
        setup.gotoPage(testReference);
        LoginPage loginPage = new LoginPage();
        loginPage.assertOnPage();
        assertFalse(loginPage.isAuthenticated());

        // Log in
        loginPage.loginAsAdmin();

        // We should be redirected back to the page. Verify we're on the right page.
        setup.assertOnPage(testReference);
    }

    @Test
    @Order(5)
    void dataIsPreservedAfterLogin(TestUtils setup, TestReference testReference,
        LogCaptureConfiguration logCaptureConfiguration)
    {
        // Force guest user so that the save will redirect to the login page
        setup.forceGuestUser();
        setup.createPage(testReference, "some content");
        LoginPage loginPage = new LoginPage();
        loginPage.assertOnPage();

        // Now login
        loginPage.loginAsAdmin();

        // Since we switched user (from guest to Admin), the CSRF protection will ask for confirmation
        ResubmissionPage resubmissionPage = new ResubmissionPage();
        resubmissionPage.resubmit();

        // Verify that the page we tried to create is now created (thanks to the automatic redirect) with the proper
        // content.
        ViewPage viewPage = new ViewPage();
        setup.assertOnPage(testReference);
        assertEquals("some content", viewPage.getContent());

        // Since we got a CSRF warning, we expect it to be in the logs too.
        logCaptureConfiguration.registerExpected("CSRFToken: Secret token verification failed, token:");
    }
}
