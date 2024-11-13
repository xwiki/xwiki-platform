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
package org.xwiki.user.test.po;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.xwiki.test.ui.po.BasePage;

/**
 * User profile, change password action.
 */
public class ChangePasswordPage extends BasePage
{
    private static final String ERROR_MESSAGE_SELECTOR = "span.box.errormessage";

    private static final String VALIDATION_ERROR_MESSAGE_SELECTOR = "span.LV_validation_message.LV_invalid";

    private static final String SUCCESS_MESSAGE_SELECTOR = "span.box.infomessage";

    @FindBy(xpath = "//input[@id='xwikioriginalpassword']")
    private WebElement originalPassword;

    @FindBy(xpath = "//input[@id='xwikipassword']")
    private WebElement password1;

    @FindBy(xpath = "//input[@id='xwikipassword2']")
    private WebElement password2;

    @FindBy(xpath = "//input[@value='Save']")
    private WebElement changePassword;

    @FindBy(css = "a.secondary.button")
    private WebElement cancelPasswordChange;

    @FindBy(css = ERROR_MESSAGE_SELECTOR)
    private WebElement errorMessage;

    @FindBy(css = VALIDATION_ERROR_MESSAGE_SELECTOR)
    private WebElement validationErrorMessage;

    @FindBy(css = SUCCESS_MESSAGE_SELECTOR)
    private WebElement successMessage;

    /**
     * Fill the change password form with the original password, the new password and the confirmation of the new
     * password.
     *
     * @param originalPassword the original password
     * @param password the new password
     * @param password2 the confirmation of the new password
     * @see #changePasswordAsAdmin(String, String)
     */
    public void changePassword(String originalPassword, String password, String password2)
    {
        this.originalPassword.clear();
        this.originalPassword.sendKeys(originalPassword);
        this.password1.clear();
        this.password1.sendKeys(password);
        this.password2.clear();
        this.password2.sendKeys(password2);
    }

    /**
     * Fill the change password form when the current user is an Admin.
     *
     * @param password the new password
     * @param password2 the confirmation of the new password
     * @see #changePassword(String, String, String)
     */
    public void changePasswordAsAdmin(String password, String password2)
    {
        this.password1.clear();
        this.password1.sendKeys(password);
        this.password2.clear();
        this.password2.sendKeys(password2);
        getDriver().waitUntilElementHasNonEmptyAttributeValue(By.xpath("//input[@id='xwikipassword2']"),"class");
    }

    /**
     * @return the text of the change password form error message
     */
    public String getErrorMessage()
    {
        return this.errorMessage.getText();
    }

    /**
     * @return the text of the change password form validation error message
     */
    public String getValidationErrorMessage()
    {
        return this.validationErrorMessage.getText();
    }

    /**
     * @return the text of the change password form success message
     */
    public String getSuccessMessage()
    {
        return this.successMessage.getText();
    }

    /**
     * Submit the change password form.
     *
     * @return the new {@link ChangePasswordPage} after submission.
     */
    public ChangePasswordPage submit()
    {
        this.changePassword.click();

        // We cannot wait on a page reload because of the live error messages,
        // so we wait on the various kind of messages we can have, to avoid getting
        // StaleElementReference afterwards.
        getDriver().waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(@Nullable WebDriver webDriver)
            {
                return isDisplayed(By.cssSelector(VALIDATION_ERROR_MESSAGE_SELECTOR))
                    || isDisplayed(By.cssSelector(ERROR_MESSAGE_SELECTOR))
                    || isDisplayed(By.cssSelector(SUCCESS_MESSAGE_SELECTOR));
            }

            private boolean isDisplayed(By target)
            {
                boolean liveErrorIsDisplayed = false;
                try {
                    // Fails fast with findElementWithoutWaiting when the targeted element is staled,
                    // because isDisplayed fails too but much more slowly, increasing the chance to see 
                    // waitUntilCondition timeout even if the expected message is finally displayed. 
                    WebElement elementWithoutWaiting = getDriver().findElementWithoutWaiting(target);
                    liveErrorIsDisplayed = elementWithoutWaiting.isDisplayed();
                } catch (StaleElementReferenceException | NoSuchElementException e) {
                }
                return liveErrorIsDisplayed;
            }
        });
        return new ChangePasswordPage();
    }

    /**
     * Click on the cancel button of the change password form.
     */
    public void cancel()
    {
        this.cancelPasswordChange.click();
    }

    /**
     * Wait until the validation error message has the expected text. {@link TimeoutException} is thrown if the expected
     * text is not displayed withing a given time limit.
     *
     * @param expectedText the expected text of the validation error message
     * @since 14.4RC1
     * @since 13.10.6
     */
    public void assertValidationErrorMessage(String expectedText)
    {
        getDriver().waitUntilElementHasTextContent(By.cssSelector(VALIDATION_ERROR_MESSAGE_SELECTOR), expectedText);
    }

    /**
     * Wait until the success message has the expected text. {@link TimeoutException} is thrown if the expected text is
     * not displayed withing a given time limit.
     *
     * @param expectedText the expected text of the success message
     * @since 14.4RC1
     * @since 13.10.6
     */
    public void assertSuccessMessage(String expectedText)
    {
        getDriver().waitUntilElementHasTextContent(By.cssSelector(SUCCESS_MESSAGE_SELECTOR), expectedText);
    }

    /**
     * Wait until the error message has the expected text. {@link TimeoutException} is thrown if the expected text is
     * not displayed withing a given time limit.
     *
     * @param expectedText the expected text of the error message
     * @since 14.4RC1
     * @since 13.10.6
     */
    public void assertErrorMessage(String expectedText)
    {
        getDriver().waitUntilElementHasTextContent(By.cssSelector(ERROR_MESSAGE_SELECTOR), expectedText);
    }
}
