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
package org.xwiki.administration.test.ui;

import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.xwiki.administration.test.po.RegistrationModal;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.AbstractRegistrationPage;
import org.xwiki.test.ui.po.DeletePageOutcomePage;
import org.xwiki.test.ui.po.RegistrationPage;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test the user registration feature.
 * <p>
 * The tests in this class are parametrized with the values:
 * <ul>
 *   <li>
 *     <b>isModal:</b> when {@code true} the user creation modal from the administration is used, otherwise the guest
 *     user registration form is used
 *   </li>
 *   <li>
 *       <b>closeWiki:</b> when {@code true} the wiki is set as private for guest user, otherwise it's readable for
 *       guest users
 *   </li>
 *   <li>
 *       <b>withRegistrationConfig</b> when {@code false} the {@code XWiki.RegistrationConfig} page is deleted to
 *       test fallbacks when it's not available
 *   </li>
 * </ul>
 * <p>
 *
 * @version $Id$
 * @since 13.4RC1
 * @since 12.10.8
 */
@UITest
class RegisterIT
{
    private AbstractRegistrationPage setUp(TestUtils testUtils, boolean isModal, boolean closeWiki,
        boolean withRegistrationConfig) throws Exception
    {
        // We create the admin user because it is expected to exist when testing the registration of an existing user.
        testUtils.loginAsSuperAdmin();
        if (closeWiki) {
            testUtils.setWikiPreference("authenticate_view", "1");
        }
        if (!withRegistrationConfig) {
            testUtils.deletePage("XWiki", "RegistrationConfig");
        }
        testUtils.createAdminUser();
        switchUser(testUtils, isModal);
        AbstractRegistrationPage registrationPage = this.getRegistrationPage(isModal);

        if (!closeWiki) {
            // The prepareName javascript function is the cause of endless flickering since it tries to suggest a username
            // every time the field is focused.
            testUtils.getDriver().executeJavascript("document.getElementById('xwikiname').onfocus = null;");
        }
        return registrationPage;
    }

    @AfterEach
    void afterEach(TestUtils testUtils) throws Exception
    {
        testUtils.loginAsSuperAdmin();
        testUtils.setWikiPreference("authenticate_view", "0");
        ViewPage viewPage = testUtils.gotoPage("XWiki", "RegistrationConfig");
        if (!viewPage.exists()) {
            // We try to restore the page instead of creating back the object to have same values that were in
            // the imported document.
            DeletePageOutcomePage deletePageOutcomePage = new DeletePageOutcomePage();
            deletePageOutcomePage.getDeletedTerminalPagesEntries().get(0).clickRestore();
        } else {
            testUtils.updateObject("XWiki", "RegistrationConfig", "XWiki.Registration", 0, "passwordLength", 6);
            testUtils.updateObject("XWiki", "RegistrationConfig", "XWiki.Registration", 0,
                "passwordRuleOneNumberEnabled", 0);
        }
        deleteJohnSmith(testUtils);
    }

    /**
     * Become the user needed for the test. Superadmin when testing the user creation from the administration modal,
     * guest when testing the user registration page.
     */
    private void switchUser(TestUtils testUtils, boolean isModal)
    {
        // The test of the standard registration for must be done with the guest user.
        // The test of the user creation in a modal from the administration must be done with an user that
        // has admin rights.
        if (!isModal) {
            // Fast Logout.
            testUtils.forceGuestUser();
        } else {
            testUtils.loginAsSuperAdmin();
        }
    }

    private AbstractRegistrationPage getRegistrationPage(boolean isModal)
    {
        // When testing the modal, we go the the user section of the administration.
        // Otherwise, we test the guest user registration form.
        if (isModal) {
            return RegistrationModal.gotoPage();
        } else {
            return RegistrationPage.gotoPage();
        }
    }

    /**
     * If LiveValidation is enabled then it will check that there are no failures with that. If no failures then hits
     * register button, it then asserts that hitting the register button did not reveal any failures not caught by
     * LiveValidation. If LiveValidation is disabled then just hits the register button.
     */
    private boolean validateAndRegister(TestUtils testUtils, boolean isModal, AbstractRegistrationPage registrationPage)
    {
        registrationPage.triggerLiveValidation();
        if (!registrationPage.getValidationFailureMessages().isEmpty()) {
            return false;
        }
        boolean result = tryToRegister(testUtils, registrationPage, isModal);

        assertTrue(registrationPage.getValidationFailureMessages().isEmpty(),
            "LiveValidation did not show a failure message but clicking on the register button did.");

        return result;
    }

    private boolean tryToRegister(TestUtils testUtils, AbstractRegistrationPage registrationPage, boolean isModal)
    {
        if (isModal) {
            return administrationModalUserCreation(testUtils, registrationPage);
        } else {
            return guestUserRegistration(registrationPage);
        }
    }

    private boolean administrationModalUserCreation(TestUtils testUtils, AbstractRegistrationPage registrationPage)
    {
        registrationPage.clickRegister();

        // Wait until one of the following happens:
        testUtils.getDriver().waitUntilElementsAreVisible(new By[] {
            // A live validation error message appears.
            By.cssSelector("dd > span.LV_validation_message.LV_invalid"),
            // The operation fails on the server.
            By.cssSelector(".xnotification-error"),
            // The operation succeeds.
            By.cssSelector(".xnotification-done")
        }, false);

        try {
            // Try to hide the success message by clicking on it.
            testUtils.getDriver().findElementWithoutWaiting(
                By.xpath("//div[contains(@class,'xnotification-done') and contains(., 'User created')]")).click();
            // If we get here it means the registration was successful.
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    private boolean guestUserRegistration(AbstractRegistrationPage registrationPage)
    {
        registrationPage.clickRegister();

        return ((RegistrationPage) registrationPage).getRegistrationSuccessMessage().isPresent();
    }

    private void tryToLoginAsJohnSmith(TestUtils testUtils, String password, AbstractRegistrationPage registrationPage)
    {
        // Fast logout.
        testUtils.forceGuestUser();
        testUtils.loginAndGotoPage(AbstractRegistrationPage.JOHN_SMITH_USERNAME, password,
            testUtils.getDriver().getCurrentUrl());
        assertTrue(registrationPage.isAuthenticated());
    }

    /**
     * Deletes JohnSmith if it exists, leaves the driver on undefined page.
     */
    private void deleteJohnSmith(TestUtils testUtils) throws Exception
    {
        testUtils.loginAsSuperAdmin();
        testUtils.rest().deletePage("XWiki", AbstractRegistrationPage.JOHN_SMITH_USERNAME);
    }

    /**
     * Returns a stream of combinations of parameters to test. The first value is {@code isModal}, the
     * second {@code closedWiki} and the last {@code withRegistrationConfig}.
     *
     * @return the tested combination of {@code isModal}, {@code closedWiki} and {@code withRegistrationConfig}
     */
    private static Stream<Arguments> testsParameters()
    {
        return Stream.of(
            // Note: modal true and closedWiki true doesn't make sense here: we don't care if the wiki is closed or not
            // when checking the registration modal from administration.
            Arguments.of(false, false, true),
            Arguments.of(false, true, true),
            Arguments.of(true, false, true),
            Arguments.of(false, false, false),
            Arguments.of(false, true, false),
            Arguments.of(true, false, false)
        );
    }

    @ParameterizedTest()
    @MethodSource("testsParameters")
    @Order(1)
    void registerJohnSmith(boolean isModal, boolean closedWiki, boolean withRegistrationConfig, TestUtils testUtils)
        throws Exception
    {
        AbstractRegistrationPage registrationPage = setUp(testUtils, isModal, closedWiki, withRegistrationConfig);
        registrationPage.fillInJohnSmithValues();
        assertTrue(validateAndRegister(testUtils, isModal, registrationPage), String.format("isModal: %s close "
                + "wiki: %s withRegistrationConfig: %s", isModal, closedWiki, withRegistrationConfig));
        tryToLoginAsJohnSmith(testUtils, AbstractRegistrationPage.JOHN_SMITH_PASSWORD, registrationPage);
    }

    @ParameterizedTest()
    @MethodSource("testsParameters")
    @Order(2)
    void registerExistingUser(boolean isModal, boolean closedWiki, boolean withRegistrationConfig,
        TestUtils testUtils) throws Exception
    {
        AbstractRegistrationPage registrationPage = setUp(testUtils, isModal, closedWiki, withRegistrationConfig);

        // Uses the empty string instead of the null value to empty the form fields (the null value just keep the value filled from the previously run test).
        registrationPage.fillRegisterForm("", "", "Admin", "password", "password", "");
        // Can't use validateAndRegister here because user existence is not checked by LiveValidation.
        assertFalse(tryToRegister(testUtils, registrationPage, isModal));
        if (closedWiki) {
            assertTrue(registrationPage.errorMessageInclude("Error: User already exists."));
        } else {
            assertTrue(registrationPage.validationFailureMessagesInclude("User already exists."));
        }
    }

    @ParameterizedTest()
    @MethodSource("testsParameters")
    @Order(3)
    void registerPasswordTooShort(boolean isModal, boolean closedWiki, boolean withRegistrationConfig,
        TestUtils testUtils) throws Exception
    {
        AbstractRegistrationPage registrationPage = setUp(testUtils, isModal, closedWiki, withRegistrationConfig);
        registrationPage.fillRegisterForm(null, null, null, "short", "short", null);
        assertFalse(validateAndRegister(testUtils, isModal, registrationPage));
        assertTrue(
            registrationPage.validationFailureMessagesInclude("Your new password must be at least 6 characters long."));
    }

    @ParameterizedTest()
    @MethodSource("testsParameters")
    @Order(4)
    void registerDifferentPasswords(boolean isModal, boolean closedWiki, boolean withRegistrationConfig,
        TestUtils testUtils) throws Exception
    {
        AbstractRegistrationPage registrationPage = setUp(testUtils, isModal, closedWiki, withRegistrationConfig);
        registrationPage.fillRegisterForm(null, null, null, null, "DifferentPassword", null);
        assertFalse(validateAndRegister(testUtils, isModal, registrationPage));
        assertTrue(registrationPage.validationFailureMessagesInclude("The two passwords do not match."));
    }

    @ParameterizedTest()
    @MethodSource("testsParameters")
    @Order(5)
    void registerEmptyPassword(boolean isModal, boolean closedWiki, boolean withRegistrationConfig,
        TestUtils testUtils) throws Exception
    {
        AbstractRegistrationPage registrationPage = setUp(testUtils, isModal, closedWiki, withRegistrationConfig);
        registrationPage.fillRegisterForm(null, null, null, "", "", null);
        assertFalse(validateAndRegister(testUtils, isModal, registrationPage));
        assertTrue(registrationPage.validationFailureMessagesInclude("This field is required."));
    }

    @ParameterizedTest()
    @MethodSource("testsParameters")
    @Order(6)
    void registerEmptyUserName(boolean isModal, boolean closedWiki, boolean withRegistrationConfig,
        TestUtils testUtils) throws Exception
    {
        AbstractRegistrationPage registrationPage = setUp(testUtils, isModal, closedWiki, withRegistrationConfig);
        // A piece of javascript fills in the username with the first and last names so we will empty them.
        registrationPage.fillRegisterForm("", "", "", null, null, null);
        assertFalse(validateAndRegister(testUtils, isModal, registrationPage));
        assertTrue(registrationPage.validationFailureMessagesInclude("This field is required."));
    }

    @ParameterizedTest()
    @MethodSource("testsParameters")
    @Order(7)
    void registerInvalidEmail(boolean isModal, boolean closedWiki, boolean withRegistrationConfig,
        TestUtils testUtils) throws Exception
    {
        AbstractRegistrationPage registrationPage = setUp(testUtils, isModal, closedWiki, withRegistrationConfig);
        registrationPage.fillRegisterForm(null, null, null, null, null, "not an email address");
        assertFalse(validateAndRegister(testUtils, isModal, registrationPage));
        assertTrue(registrationPage.validationFailureMessagesInclude("Please enter a valid email address."));
    }

    @ParameterizedTest()
    @MethodSource("testsParameters")
    @Order(8)
    void registerWikiSyntaxName(boolean isModal, boolean closedWiki, boolean withRegistrationConfig,
        TestUtils testUtils) throws Exception
    {
        // We don't really care of executing this test within the modal since it's about checking the success message
        // and in case of modal there's no success message with the user information.
        if (!isModal) {
            AbstractRegistrationPage registrationPage = setUp(testUtils, isModal, closedWiki, withRegistrationConfig);
            String firstName = "]]{{/html}}{{html clean=false}}HT&amp;ML";
            String lastName = "]]{{/html}}";
            String password = AbstractRegistrationPage.JOHN_SMITH_PASSWORD;
            registrationPage.fillRegisterForm(firstName, lastName,
                AbstractRegistrationPage.JOHN_SMITH_USERNAME, password, password, "wiki@example.com");
            assertTrue(validateAndRegister(testUtils, isModal, registrationPage), String.format("isModal: %s close "
                + "wiki: %s withRegistrationConfig: %s", isModal, closedWiki, withRegistrationConfig));
            // TODO: looks like a pretty strange behavior, there might be a message box title missing somewhere
            String messagePrefix = closedWiki ? "" : "Information ";
            messagePrefix = !closedWiki&&withRegistrationConfig ? "Welcome ": messagePrefix;
            // TODO: clean up this test with a better final assertion. 
            //  As of now, the string retrieved changes a lot depending on the test parameters
            // The assertion should be less strong so that we can clearly show these differences.
            assertEquals(String.format("%s%s %s (%s)%s", messagePrefix, firstName, lastName,
                    AbstractRegistrationPage.JOHN_SMITH_USERNAME, 
                    !closedWiki&&withRegistrationConfig ? "" : ": Registration successful."),
                ((RegistrationPage) registrationPage).getRegistrationSuccessMessage().orElseThrow());
        }
    }

    @ParameterizedTest()
    @MethodSource("testsParameters")
    @Order(9)
    void registerWithCustomPasswordPolicy(boolean isModal, boolean closedWiki, boolean withRegistrationConfig,
        TestUtils testUtils) throws Exception
    {
        // There's no point of running this one without registration config.
        if (withRegistrationConfig) {
            testUtils.loginAsSuperAdmin();
            // Enforce using a password of 10 characters and a symbol
            testUtils.updateObject("XWiki", "RegistrationConfig", "XWiki.Registration", 0, "passwordLength", 10);
            testUtils.updateObject("XWiki", "RegistrationConfig", "XWiki.Registration", 0,
                "passwordRuleOneNumberEnabled", 1);

            AbstractRegistrationPage registrationPage = setUp(testUtils, isModal, closedWiki, withRegistrationConfig);

            String password = "password";
            registrationPage.fillRegisterForm("John", "Smith", AbstractRegistrationPage.JOHN_SMITH_USERNAME,
                password, password, "johnsmith@xwiki.org");
            assertFalse(validateAndRegister(testUtils, isModal, registrationPage));
            assertTrue(registrationPage.validationFailureMessagesInclude(
                "Your new password must be at least 10 characters long."));

            password = "passwordpassword";
            registrationPage.fillRegisterForm("John", "Smith", AbstractRegistrationPage.JOHN_SMITH_USERNAME,
                password, password, "johnsmith@xwiki.org");
            assertFalse(validateAndRegister(testUtils, isModal, registrationPage));
            assertTrue(registrationPage.validationFailureMessagesInclude(
                "The password must contain at least one number."));

            password = "password4password";
            registrationPage.fillRegisterForm("John", "Smith", AbstractRegistrationPage.JOHN_SMITH_USERNAME,
                password, password, "johnsmith@xwiki.org");
            assertTrue(validateAndRegister(testUtils, isModal, registrationPage), String.format("isModal: %s close "
                + "wiki: %s withRegistrationConfig: %s", isModal, closedWiki, withRegistrationConfig));
            tryToLoginAsJohnSmith(testUtils, password, registrationPage);
        }
    }


}
