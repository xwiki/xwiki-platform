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
package org.xwiki.flamingo.test.ui;

import java.util.Arrays;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.integration.junit.LogCaptureConfiguration;
import org.xwiki.test.integration.junit.LogCaptureValidator;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.LoginPage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test related to login form on flamingo skin.
 *
 * @version $Id$
 * @since 11.6RC1
 */
@UITest
public class LoginIT
{
    private static DocumentReference AUTHENTICATION_CONFIGURATION =
        new DocumentReference("xwiki", Arrays.asList("XWiki", "Authentication"), "Configuration");

    @BeforeAll
    public void setup(TestUtils setup)
    {
        setup.createPage(AUTHENTICATION_CONFIGURATION, "");
        setup.addObject(AUTHENTICATION_CONFIGURATION, "XWiki.Authentication.ConfigurationClass",
            "failureStrategy", "captcha",
            "maxAuthorizedAttempts", 3,
            "timeWindowAttempts", 300);
    }

    @AfterAll
    public void tearDown(TestUtils setup)
    {
        setup.deletePage(AUTHENTICATION_CONFIGURATION);
    }

    /**
     * Ensure that the repeated authentication failure mechanism is triggered.
     */
    @Test
    public void repeatedAuthenticationFailure(TestUtils setup, TestInfo testInfo, TestReference testReference,
        LogCaptureConfiguration logCaptureConfiguration)
    {
        // fixture:
        // create login and fails login with it: we don't want Admin to be blocked for authentication in
        // further tests.
        String username = testInfo.getTestMethod().get().getName();
        String password = testInfo.getTestMethod().get().getName();

        // We don't need to be logged in for that.
        setup.forceGuestUser();
        setup.createUser(username, password, setup.getBaseURL());
        LoginPage loginPage = LoginPage.gotoPage();

        // first wrong auth
        loginPage.loginAs(username, "foo");
        loginPage = new LoginPage();
        assertTrue(loginPage.hasInvalidCredentialsErrorMessage());
        assertFalse(loginPage.hasCaptchaErrorMessage());

        // second wrong auth
        loginPage.loginAs(username, "foo");
        loginPage = new LoginPage();
        assertTrue(loginPage.hasInvalidCredentialsErrorMessage());
        assertFalse(loginPage.hasCaptchaErrorMessage());

        // third wrong auth: captcha is triggered
        loginPage.loginAs(username, "foo");
        loginPage = new LoginPage();
        assertTrue(loginPage.hasInvalidCredentialsErrorMessage());
        assertTrue(loginPage.hasCaptchaErrorMessage());
        assertTrue(loginPage.hasCaptchaChallenge());

        // fourth good auth: captcha is still triggered
        loginPage.loginAs(username, password);
        loginPage = new LoginPage();
        assertTrue(loginPage.hasInvalidCredentialsErrorMessage());
        assertTrue(loginPage.hasCaptchaErrorMessage());
        assertTrue(loginPage.hasCaptchaChallenge());

        logCaptureConfiguration.registerExpected(
            "Authentication failure with login [repeatedAuthenticationFailure]");
    }
}
