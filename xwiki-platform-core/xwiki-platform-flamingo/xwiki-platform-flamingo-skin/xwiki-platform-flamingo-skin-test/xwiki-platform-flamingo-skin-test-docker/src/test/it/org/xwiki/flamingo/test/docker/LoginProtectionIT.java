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

import java.util.Arrays;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.integration.junit.LogCaptureConfiguration;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.LoginPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test related to login protection on flamingo skin (captcha appearing after N attempts, etc).
 *
 * @version $Id$
 * @since 11.6RC1
 */
@UITest
public class LoginProtectionIT
{
    private static final DocumentReference AUTHENTICATION_CONFIGURATION =
        new DocumentReference("xwiki", Arrays.asList("XWiki", "Authentication"), "Configuration");

    private static final String USERNAME = "repeatedAuthenticationFailure";

    private static final String PASSWORD = "password";

    @BeforeAll
    public void setup(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
        setup.createPage(AUTHENTICATION_CONFIGURATION, "");
        setup.addObject(AUTHENTICATION_CONFIGURATION, "XWiki.Authentication.ConfigurationClass",
            "failureStrategy", "captcha",
            "maxAuthorizedAttempts", 3,
            "timeWindowAttempts", 300,
            "isAuthenticationSecurityEnabled", true);
    }

    @AfterAll
    public void tearDown(TestUtils setup)
    {
        // Just to be safe reset the session before logging in as super admin to avoid being in a session with login
        // failures which would block the superadmin user.
        setup.forceGuestUser();
        setup.loginAsSuperAdmin();
        setup.deletePage(AUTHENTICATION_CONFIGURATION);
    }

    /**
     * Ensure that the repeated authentication failure mechanism is triggered.
     */
    @Test
    @Order(1)
    public void repeatedAuthenticationFailure(TestUtils setup, TestInfo testInfo, TestReference testReference,
        LogCaptureConfiguration logCaptureConfiguration)
    {
        // fixture:
        // create login and fails login with it: we don't want Admin to be blocked for authentication in
        // further tests.
        String username2 = USERNAME + "2";

        // We don't need to be logged in for that.
        setup.forceGuestUser();
        setup.createUser(USERNAME, PASSWORD, setup.getBaseURL());
        setup.createUser(username2, PASSWORD, setup.getBaseURL());
        LoginPage loginPage = LoginPage.gotoPage();

        // first wrong auth
        loginPage.loginAs(USERNAME, "foo");
        loginPage = new LoginPage();
        assertTrue(loginPage.hasInvalidCredentialsErrorMessage());
        assertFalse(loginPage.hasCaptchaErrorMessage());

        // second wrong auth
        loginPage.loginAs(USERNAME, "foo");
        loginPage = new LoginPage();
        assertTrue(loginPage.hasInvalidCredentialsErrorMessage());
        assertFalse(loginPage.hasCaptchaErrorMessage());

        // third wrong auth: captcha is triggered
        loginPage.loginAs(USERNAME, "foo");
        loginPage = new LoginPage();
        assertTrue(loginPage.hasInvalidCredentialsErrorMessage());
        assertTrue(loginPage.hasCaptchaErrorMessage());
        assertTrue(loginPage.hasCaptchaChallenge());

        // fourth good auth: captcha is still triggered
        loginPage.loginAs(USERNAME, PASSWORD);
        loginPage = new LoginPage();
        assertTrue(loginPage.hasInvalidCredentialsErrorMessage());
        assertTrue(loginPage.hasCaptchaErrorMessage());
        assertTrue(loginPage.hasCaptchaChallenge());

        // trying with another login: captcha is still triggered because it's on the same session
        loginPage.loginAs(username2, PASSWORD);
        loginPage = new LoginPage();
        assertTrue(loginPage.hasInvalidCredentialsErrorMessage());
        assertTrue(loginPage.hasCaptchaErrorMessage());
        assertTrue(loginPage.hasCaptchaChallenge());

        // Reset the session to verify that we still cannot login.
        setup.forceGuestUser();
        loginPage = LoginPage.gotoPage();
        loginPage.loginAs(USERNAME, PASSWORD);
        loginPage = new LoginPage();
        assertTrue(loginPage.hasInvalidCredentialsErrorMessage());
        assertTrue(loginPage.hasCaptchaErrorMessage());
        assertTrue(loginPage.hasCaptchaChallenge());

        logCaptureConfiguration.registerExpected(
            "Authentication failure with login [repeatedAuthenticationFailure]");
    }

    /**
     * Ensure that when the protection mechanism is disabled a user can directly login.
     * Note that if this test is failing it might impact the whole test suite since superadmin might be prevented to
     * login.
     */
    @Test
    @Order(2)
    void canLoginWhenSecurityIsDisabled(TestUtils setup)
    {
        // Clean the session to reset the session protection mechanism.
        setup.forceGuestUser();
        setup.loginAsSuperAdmin();
        // We disable the security mechanism
        setup.updateObject(AUTHENTICATION_CONFIGURATION, "XWiki.Authentication.ConfigurationClass", 0,
            "isAuthenticationSecurityEnabled", false);

        // Verify that we can login again as the user who was previously blocked.
        setup.forceGuestUser();
        setup.login(USERNAME, PASSWORD);

        setup.gotoPage("Main", "WebHome");
        assertEquals(USERNAME, setup.getLoggedInUserName());
    }
}
