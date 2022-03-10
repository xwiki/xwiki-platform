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

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.xwiki.administration.test.po.RegistrationModal;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.AbstractRegistrationPage;
import org.xwiki.test.ui.po.RegistrationPage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test the user registration feature.
 * <p>
 * The tests in this class are parametrized with the values:
 * <ul>
 *   <li><b>useLiveValidation:</b> when {@code true} activates the client side validation of the registration form</li>
 *   <li>
 *     <b>isModal:</b> when {@code true} the user creation modal from the administration is used, otherwise the guest
 *     user registration form is used
 *   </li>
 * </ul>
 * <p>
 * Three combinations of these parameters are tested:
 * <ul>
 *   <li>useLiveValidation + !isModal</li>
 *   <li>!useLiveValidation + !isModal</li>
 *   <li>useLiveValidation + isModal</li>
 * </ul>
 *
 * @version $Id$
 * @since 13.4RC1
 * @since 12.10.8
 */
@UITest
class RegisterIT
{
    /**
     * Returns a stream of combinations of parameters to test. The first value is {@code useLiveValidation} and the
     * second {@code isModal}.
     *
     * @return the tested combination of {@code useLiveValidation} and {@code isModal}
     */
    private static Stream<Arguments> testsParameters()
    {
        return Stream.of(
            Arguments.of(true, false),
            Arguments.of(false, false),
            Arguments.of(true, true)
        );
    }

    @ParameterizedTest
    @MethodSource("testsParameters")
    @Order(1)
    void registerJohnSmith(boolean useLiveValidation, boolean isModal, TestUtils testUtils) throws Exception
    {
        AbstractRegistrationPage registrationPage = setUp(testUtils, useLiveValidation, isModal);
        assertTrue(validateAndRegister(testUtils, useLiveValidation, isModal, registrationPage));
        tryToLoginAsJohnSmith(testUtils, registrationPage);
    }

    @ParameterizedTest
    @MethodSource("testsParameters")
    @Order(2)
    void registerExistingUser(boolean useLiveValidation, boolean isModal, TestUtils testUtils) throws Exception
    {
        AbstractRegistrationPage registrationPage = setUp(testUtils, useLiveValidation, isModal);

        // Uses the empty string instead of the null value to empty the form fields (the null value just keep the value filled from the previously run test).
        registrationPage.fillRegisterForm("", "", "Admin", "password", "password", "");
        // Can't use validateAndRegister here because user existence is not checked by LiveValidation.
        assertFalse(tryToRegister(testUtils, registrationPage, isModal));
        assertTrue(registrationPage.validationFailureMessagesInclude("User already exists."));
    }

    @ParameterizedTest
    @MethodSource("testsParameters")
    @Order(3)
    void registerPasswordTooShort(boolean useLiveValidation, boolean isModal, TestUtils testUtils) throws Exception
    {
        AbstractRegistrationPage registrationPage = setUp(testUtils, useLiveValidation, isModal);
        registrationPage.fillRegisterForm(null, null, null, "short", "short", null);
        assertFalse(validateAndRegister(testUtils, useLiveValidation, isModal, registrationPage));
        assertTrue(
            registrationPage.validationFailureMessagesInclude("Your new password must be at least 6 characters long."));
    }

    @ParameterizedTest
    @MethodSource("testsParameters")
    @Order(4)
    void registerDifferentPasswords(boolean useLiveValidation, boolean isModal, TestUtils testUtils)
        throws Exception
    {
        AbstractRegistrationPage registrationPage = setUp(testUtils, useLiveValidation, isModal);
        registrationPage.fillRegisterForm(null, null, null, null, "DifferentPassword", null);
        assertFalse(validateAndRegister(testUtils, useLiveValidation, isModal, registrationPage));
        assertTrue(registrationPage.validationFailureMessagesInclude("The two passwords do not match."));
    }

    @ParameterizedTest
    @MethodSource("testsParameters")
    @Order(5)
    void registerEmptyPassword(boolean useLiveValidation, boolean isModal, TestUtils testUtils) throws Exception
    {
        AbstractRegistrationPage registrationPage = setUp(testUtils, useLiveValidation, isModal);
        registrationPage.fillRegisterForm(null, null, null, "", "", null);
        assertFalse(validateAndRegister(testUtils, useLiveValidation, isModal, registrationPage));
        assertTrue(registrationPage.validationFailureMessagesInclude("This field is required."));
    }

    @ParameterizedTest
    @MethodSource("testsParameters")
    @Order(6)
    void registerEmptyUserName(boolean useLiveValidation, boolean isModal, TestUtils testUtils) throws Exception
    {
        AbstractRegistrationPage registrationPage = setUp(testUtils, useLiveValidation, isModal);
        // A piece of javascript fills in the username with the first and last names so we will empty them.
        registrationPage.fillRegisterForm("", "", "", null, null, null);
        assertFalse(validateAndRegister(testUtils, useLiveValidation, isModal, registrationPage));
        assertTrue(registrationPage.validationFailureMessagesInclude("This field is required."));
    }

    @ParameterizedTest
    @MethodSource("testsParameters")
    @Order(7)
    void registerInvalidEmail(boolean useLiveValidation, boolean isModal, TestUtils testUtils) throws Exception
    {
        AbstractRegistrationPage registrationPage = setUp(testUtils, useLiveValidation, isModal);
        registrationPage.fillRegisterForm(null, null, null, null, null, "not an email address");
        assertFalse(validateAndRegister(testUtils, useLiveValidation, isModal, registrationPage));
        assertTrue(registrationPage.validationFailureMessagesInclude("Please enter a valid email address."));
    }

    private AbstractRegistrationPage setUp(TestUtils testUtils, boolean useLiveValidation, boolean isModal)
        throws Exception
    {
        // We create the admin user because it is expected to exist when testing the registration of an existing user.
        testUtils.loginAsSuperAdmin();
        testUtils.createAdminUser();
        deleteJohnSmith(testUtils);
        testUtils.updateObject("XWiki", "RegistrationConfig", "XWiki.Registration", 0, "liveValidation_enabled",
            useLiveValidation);
        switchUser(testUtils, isModal);
        testUtils.recacheSecretToken();
        AbstractRegistrationPage registrationPage = this.getRegistrationPage(isModal);
        // The prepareName javascript function is the cause of endless flickering since it tries to suggest a username
        // every time the field is focused.
        testUtils.getDriver().executeJavascript("document.getElementById('xwikiname').onfocus = null;");
        registrationPage.fillInJohnSmithValues();
        return registrationPage;
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
    private boolean validateAndRegister(TestUtils testUtils, boolean useLiveValidation, boolean isModal,
        AbstractRegistrationPage registrationPage)
    {
        if (useLiveValidation) {
            registrationPage.triggerLiveValidation();
            if (!registrationPage.getValidationFailureMessages().isEmpty()) {
                return false;
            }
            boolean result = tryToRegister(testUtils, registrationPage, isModal);

            assertTrue(registrationPage.getValidationFailureMessages().isEmpty(),
                "LiveValidation did not show a failure message but clicking on the register button did.");

            return result;
        }
        return tryToRegister(testUtils, registrationPage, isModal);
    }

    private boolean tryToRegister(TestUtils testUtils, AbstractRegistrationPage registrationPage, boolean isModal)
    {
        if (isModal) {
            return administrationModalUserCreation(testUtils, registrationPage);
        } else {
            return guestUserRegistration(testUtils, registrationPage);
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

    private boolean guestUserRegistration(TestUtils testUtils, AbstractRegistrationPage registrationPage)
    {
        registrationPage.clickRegister();

        List<WebElement> infos = testUtils.getDriver().findElements(By.className("infomessage"));
        for (WebElement info : infos) {
            if (info.getText().contains("Registration successful.")) {
                return true;
            }
        }
        return false;
    }

    private void tryToLoginAsJohnSmith(TestUtils testUtils, AbstractRegistrationPage registrationPage)
    {
        // Fast logout.
        testUtils.forceGuestUser();
        testUtils.getDriver().get(testUtils.getURLToLoginAs("JohnSmith", "WeakPassword"));
        assertTrue(registrationPage.isAuthenticated());
        testUtils.recacheSecretToken();
        testUtils.setDefaultCredentials("JohnSmith", "WeakPassword");
    }

    /**
     * Deletes JohnSmith if it exists, leaves the driver on undefined page.
     */
    private void deleteJohnSmith(TestUtils testUtils) throws Exception
    {
        testUtils.loginAsSuperAdmin();
        testUtils.rest().deletePage("XWiki", "JohnSmith");
    }
}
