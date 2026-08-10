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
package org.xwiki.test.ui.po;

import java.util.List;
import java.util.Optional;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Represents the actions possible on the Registration Page
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class RegistrationPage extends AbstractRegistrationPage
{
    /** The locator of the message displayed when the registration succeeds. */
    private static final By SUCCESS_MESSAGE_LOCATOR = By.xpath(
        "//*[(contains(@class, 'infomessage') or contains(@class, 'registration-success-headline')) and "
            + "(contains(., 'Registration successful.') or contains(., 'Welcome '))]");

    /** The locator of a server-side error (e.g. the user already exists on a closed wiki). */
    private static final By ERROR_MESSAGE_LOCATOR = By.cssSelector("div.errormessage");

    /** The locator of a live validation error (e.g. the user already exists on an open wiki). */
    private static final By LIVE_VALIDATION_ERROR_LOCATOR =
        By.cssSelector("dd > span.LV_validation_message.LV_invalid");

    @FindBy(css = "form#register input[type='submit']")
    private WebElement submitButton;

    /**
     * To put the registration page someplace else, subclass this class and change this method.
     */
    public static RegistrationPage gotoPage()
    {
        getUtil().gotoPage("XWiki", "Register", "register");
        return new RegistrationPage();
    }

    @Override
    public void clickRegister()
    {
        this.submitButton.click();
    }

    /**
     * @since 14.10.17
     * @since 15.5.3
     * @since 15.8RC1
     *
     * @return the registration success message if present after submitting the registration form
     */
    public Optional<String> getRegistrationSuccessMessage()
    {
        // First wait for the form submission to produce an outcome (success, server-side error or live validation
        // error). This lets us then look for the success message without waiting: on failure it's legitimately absent
        // and a plain findElements() would otherwise block for the full Selenium implicit-wait timeout.
        getDriver().waitUntilElementsAreVisible(
            new By[] { SUCCESS_MESSAGE_LOCATOR, ERROR_MESSAGE_LOCATOR, LIVE_VALIDATION_ERROR_LOCATOR }, false);

        List<WebElement> infos = getDriver().findElementsWithoutWaiting(SUCCESS_MESSAGE_LOCATOR);
        if (!infos.isEmpty()) {
            return Optional.of(infos.getFirst().getText().replaceAll("\n", " "));
        }

        return Optional.empty();
    }
}
