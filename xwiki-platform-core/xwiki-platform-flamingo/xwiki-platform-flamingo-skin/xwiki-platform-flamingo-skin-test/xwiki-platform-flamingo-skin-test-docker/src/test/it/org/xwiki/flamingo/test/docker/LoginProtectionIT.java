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
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.xwiki.administration.test.po.AuthenticationAdministrationSectionPage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.integration.junit.LogCaptureConfiguration;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.LoginPage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test related to login protection on flamingo skin (captcha appearing after N attempts, account disabled, etc).
 *
 * @version $Id$
 * @since 11.6RC1
 */
@UITest(properties = "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:Test\\.Execute\\..*")
class LoginProtectionIT
{
    private static final DocumentReference AUTHENTICATION_CONFIGURATION =
        new DocumentReference("xwiki", Arrays.asList("XWiki", "Authentication"), "Configuration");

    private static final String USERNAME = "repeatedAuthenticationFailure";

    private static final String PASSWORD = "password";

    private static final DocumentReference CAPTCHA_CONFIGURATION =
        new DocumentReference("xwiki", Arrays.asList("XWiki", "Captcha"), "Configuration");

    private static final String CAPTCHA_CONFIGURATION_CLASS = "XWiki.Captcha.ConfigurationClass";

    /**
     * Hint of the test CAPTCHA registered in {@link #setup(TestUtils)}.
     */
    private static final String FIXED_ANSWER_CAPTCHA = "fixedAnswer";

    private static final String CAPTCHA_ANSWER = "xwiki";

    private static final String DISABLED_ACCOUNT_MESSAGE =
        "This account has been disabled. Please ask the administrator to enable it back.";

    @BeforeAll
    void setup(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();
        // The default CAPTCHA (JCaptcha) displays an image that a test cannot solve. Register a CAPTCHA whose answer
        // is known, to be able to verify that a user who solves the CAPTCHA can log in.
        setup.executeWikiPlain("""
            {{groovy}}
            import org.xwiki.captcha.Captcha
            import org.xwiki.component.descriptor.DefaultComponentDescriptor
            import org.xwiki.component.manager.ComponentManager

            class FixedAnswerCaptcha implements Captcha
            {
              String display() { display(null) }
              String display(Map<String, Object> parameters)
              {
                '<span class="captcha-challenge">%2$s</span><input type="text" name="captchaAnswer" value=""/>'
              }
              boolean isValid() { false }
              boolean isValid(Map<String, Object> parameters)
              {
                // The values are the ones of the request parameter map.
                def answer = parameters?.captchaAnswer
                (answer instanceof String[] ? answer.find() : answer) == '%2$s'
              }
            }

            def descriptor = new DefaultComponentDescriptor()
            descriptor.roleType = Captcha
            descriptor.roleHint = '%1$s'
            services.component.getInstance(ComponentManager).registerComponent(descriptor, new FixedAnswerCaptcha())
            {{/groovy}}
            """.formatted(FIXED_ANSWER_CAPTCHA, CAPTCHA_ANSWER), Syntax.XWIKI_2_1);
        setup.createPage(AUTHENTICATION_CONFIGURATION, "");
        setup.addObject(AUTHENTICATION_CONFIGURATION, "XWiki.Authentication.ConfigurationClass",
            "failureStrategy", "captcha",
            "maxAuthorizedAttempts", 3,
            "timeWindowAttempts", 300,
            "isAuthenticationSecurityEnabled", true);
    }

    @AfterAll
    void tearDown(TestUtils setup) throws Exception
    {
        // Just to be safe reset the session before logging in as super admin to avoid being in a session with login
        // failures which would block the superadmin user.
        setup.forceGuestUser();
        setup.loginAsSuperAdmin();
        setup.deletePage(AUTHENTICATION_CONFIGURATION);
        setup.updateObject(CAPTCHA_CONFIGURATION, CAPTCHA_CONFIGURATION_CLASS, 0, "captcha", "jcaptcha");
        setup.executeWikiPlain("""
            {{groovy}}
            import org.xwiki.captcha.Captcha
            import org.xwiki.component.manager.ComponentManager

            services.component.getInstance(ComponentManager).unregisterComponent(Captcha, '%s')
            {{/groovy}}
            """.formatted(FIXED_ANSWER_CAPTCHA), Syntax.XWIKI_2_1);
    }

    /**
     * Ensure that the repeated authentication failure mechanism is triggered.
     */
    @Test
    @Order(1)
    void repeatedAuthenticationFailure(TestUtils setup, TestInfo testInfo, TestReference testReference,
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

        // Switch to the CAPTCHA whose answer is known, so that it can be solved below.
        setup.forceGuestUser();
        setup.loginAsSuperAdmin();
        setup.updateObject(CAPTCHA_CONFIGURATION, CAPTCHA_CONFIGURATION_CLASS, 0, "captcha", FIXED_ANSWER_CAPTCHA);

        // Reset the session to verify that we still cannot login.
        setup.forceGuestUser();
        loginPage = LoginPage.gotoPage();
        loginPage.loginAs(USERNAME, PASSWORD);
        loginPage = new LoginPage();
        assertTrue(loginPage.hasInvalidCredentialsErrorMessage());
        assertTrue(loginPage.hasCaptchaErrorMessage());
        assertTrue(loginPage.hasCaptchaChallenge());

        // A wrong answer to the CAPTCHA is refused, even with the right password.
        loginPage.setCaptchaAnswer("wrong");
        loginPage.loginAs(USERNAME, PASSWORD);
        loginPage = new LoginPage();
        assertTrue(loginPage.hasInvalidCredentialsErrorMessage());
        assertTrue(loginPage.hasCaptchaErrorMessage());
        assertTrue(loginPage.hasCaptchaChallenge());

        // The right answer to the CAPTCHA, with the right password, logs the user in. Use the other user, since the
        // CAPTCHA is required for the whole session: logging in as the first user would reset its authentication
        // failures, and the next test needs it to be still blocked.
        loginPage.setCaptchaAnswer(CAPTCHA_ANSWER);
        loginPage.loginAs(username2, PASSWORD);
        assertEquals(username2, setup.getLoggedInUserName());

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

    /**
     * Ensure that the "Disable account" strategy, configured from the administration, disables the account of a user
     * after too many authentication failures, and that the user can log in again once an administrator enabled the
     * account back.
     */
    @Test
    @Order(3)
    void disableAccountStrategy(TestUtils setup)
    {
        String username = "disableAccountStrategy";
        setup.forceGuestUser();
        setup.createUser(username, PASSWORD, setup.getBaseURL());

        // Configure the strategy from the administration (the security was disabled by the previous test).
        setup.loginAsSuperAdmin();
        AuthenticationAdministrationSectionPage administrationPage = AuthenticationAdministrationSectionPage.gotoPage();
        administrationPage.setAuthenticationSecurityEnabled(true);
        administrationPage.setFailureStrategies("disableAccount");
        administrationPage.setMaxAuthorizedAttempts(4);
        administrationPage.clickSave();

        administrationPage = AuthenticationAdministrationSectionPage.gotoPage();
        assertTrue(administrationPage.isAuthenticationSecurityEnabled());
        assertEquals(List.of("disableAccount"), administrationPage.getFailureStrategies());
        assertEquals(4, administrationPage.getMaxAuthorizedAttempts());

        // The first 3 failures are only reported as invalid credentials.
        setup.forceGuestUser();
        LoginPage loginPage = LoginPage.gotoPage();
        for (int i = 0; i < 3; i++) {
            loginPage.loginAs(username, "foo");
            loginPage = new LoginPage();
            assertTrue(loginPage.hasInvalidCredentialsErrorMessage());
            assertThat(loginPage.getErrorMessages(), not(containsString(DISABLED_ACCOUNT_MESSAGE)));
        }

        // The 4th failure disables the account.
        loginPage.loginAs(username, "foo");
        loginPage = new LoginPage();
        assertThat(loginPage.getErrorMessages(), containsString(DISABLED_ACCOUNT_MESSAGE));

        // The right password is now refused, even from a new session.
        setup.forceGuestUser();
        loginPage = LoginPage.gotoPage();
        loginPage.loginAs(username, PASSWORD);
        loginPage = new LoginPage();
        assertTrue(loginPage.hasInvalidCredentialsErrorMessage());
        assertThat(loginPage.getErrorMessages(), containsString(DISABLED_ACCOUNT_MESSAGE));

        // An administrator enables the account back. This is what the Enable button of the user profile does (the
        // user profile UI is not part of this test's distribution and its button is tested in UserProfileIT), and it
        // resets the authentication failures of the user.
        setup.forceGuestUser();
        setup.loginAsSuperAdmin();
        setup.updateObject("XWiki", username, "XWiki.XWikiUsers", 0, "active", 1);

        setup.forceGuestUser();
        loginPage = LoginPage.gotoPage();
        loginPage.loginAs(username, PASSWORD);
        assertEquals(username, setup.getLoggedInUserName());
    }
}
