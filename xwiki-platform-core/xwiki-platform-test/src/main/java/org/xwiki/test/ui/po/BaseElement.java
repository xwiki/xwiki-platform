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

import org.openqa.selenium.By;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.pagefactory.AjaxElementLocatorFactory;
import org.openqa.selenium.support.pagefactory.ElementLocatorFactory;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.xwiki.test.ui.PersistentTestContext;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.XWikiWrappingDriver;

/**
 * Represents all elements which include web pages as well as parts of web pages.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class BaseElement
{
    private static PersistentTestContext context;

    /** Used so that AllTests can set the persistent test context. */
    public static void setContext(PersistentTestContext context)
    {
        BaseElement.context = context;
    }

    public BaseElement()
    {
        ElementLocatorFactory finder =
            new AjaxElementLocatorFactory(getDriver().getWrappedDriver(), getUtil().getTimeout());
        PageFactory.initElements(finder, this);
    }

    protected XWikiWrappingDriver getDriver()
    {
        return context.getDriver();
    }

    /**
     * @return Utility class with functions not specific to any test or element.
     */
    protected static TestUtils getUtil()
    {
        return context.getUtil();
    }

    /**
     * Wait until the element given by the locator is displayed. Give up after timeout seconds.
     * 
     * @param locator the locator for the element to look for.
     */
    public void waitUntilElementIsVisible(final By locator)
    {
        waitUntilElementsAreVisible(new By[] {locator}, true);
    }

    /**
     * Wait until one or all of a array of element locators are displayed.
     * 
     * @param locators the array of element locators to look for.
     * @param all if true then don't return until all elements are found. Otherwise return after finding one.
     */
    public void waitUntilElementsAreVisible(final By[] locators, final boolean all)
    {
        getUtil().waitUntilCondition(new ExpectedCondition<WebElement>()
        {
            public WebElement apply(WebDriver driver)
            {
                WebElement element = null;
                for (int i = 0; i < locators.length; i++) {
                    try {
                        element = driver.findElement(locators[i]);
                    } catch (NotFoundException e) {
                        // This exception is caught by WebDriverWait
                        // but it returns null which is not necessarily what we want.
                        if (all) {
                            return null;
                        }
                        continue;
                    }
                    // At this stage it's possible the element is no longer valid (for example if the DOM has
                    // changed). If it's no longer attached to the DOM then consider we haven't found the element
                    // yet.
                    try {
                        if (element.isDisplayed()) {
                            if (!all) {
                                return element;
                            }
                        } else if (all) {
                            return null;
                        }
                    } catch (StaleElementReferenceException e) {
                        // Consider we haven't found the element yet
                        return null;
                    }
                }
                return element;
            }
        });
    }

    /**
     * Waits until the given element is either hidden or deleted.
     * 
     * @param locator
     */
    public void waitUntilElementDisappears(final By locator)
    {
        getUtil().waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            public Boolean apply(WebDriver driver)
            {
                try {
                    WebElement element = driver.findElement(locator);
                    return !element.isDisplayed();
                } catch (NotFoundException e) {
                    return Boolean.TRUE;
                } catch (StaleElementReferenceException e) {
                    // The element was removed from DOM in the meantime
                    return Boolean.TRUE;
                }
            }
        });
    }

    /**
     * Shows hidden elements, as if they would be shown on hover. Currently implemented using JavaScript. Will throw a
     * {@link RuntimeException} if the web driver does not support JavaScript or JavaScript is disabled.
     * 
     * @param locator locator used to find the element, in case multiple elements are found, the first is used
     */
    public void makeElementVisible(By locator)
    {
        makeElementVisible(getDriver().findElement(locator));
    }

    public void makeElementVisible(WebElement element)
    {
        // RenderedWebElement.hover() don't seem to work, workarounded using JavaScript call
        executeJavascript("arguments[0].style.visibility='visible'", element);
    }

    /**
     * Waits until the given element has a certain value for an attribute.
     * 
     * @param locator the element to wait on
     * @param attributeName the name of the attribute to check
     * @param expectedValue the attribute value to wait for
     */
    public void waitUntilElementHasAttributeValue(final By locator, final String attributeName,
        final String expectedValue)
    {
        getUtil().waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            public Boolean apply(WebDriver driver)
            {
                try {
                    WebElement element = driver.findElement(locator);
                    return expectedValue.equals(element.getAttribute(attributeName));
                } catch (NotFoundException e) {
                    return false;
                } catch (StaleElementReferenceException e) {
                    // The element was removed from DOM in the meantime
                    return false;
                }
            }
        });
    }

    /**
     * Waits until the given element ends with a certain value for an attribute.
     * 
     * @param locator the element to wait on
     * @param attributeName the name of the attribute to check
     * @param expectedValue the attribute value to wait for
     */
    public void waitUntilElementEndsWithAttributeValue(final By locator, final String attributeName,
        final String expectedValue)
    {
        getUtil().waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            public Boolean apply(WebDriver driver)
            {
                try {
                    WebElement element = driver.findElement(locator);
                    return element.getAttribute(attributeName).endsWith(expectedValue);
                } catch (NotFoundException e) {
                    return false;
                } catch (StaleElementReferenceException e) {
                    // The element was removed from DOM in the meantime
                    return false;
                }
            }
        });
    }

    /**
     * Waits until the given element has a certain value as its inner text.
     * 
     * @param locator the element to wait on
     * @param expectedValue the content value to wait for
     * @since 3.2M3
     */
    public void waitUntilElementHasTextContent(final By locator, final String expectedValue)
    {
        getUtil().waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            public Boolean apply(WebDriver driver)
            {
                WebElement element = driver.findElement(locator);
                return Boolean.valueOf(expectedValue.equals(element.getText()));
            }
        });
    }

    public Object executeJavascript(String javascript, Object... arguments)
    {
        return getDriver().executeScript(javascript, arguments);
    }

    /**
     * There is no easy support for alert/confirm window methods yet, see -
     * http://code.google.com/p/selenium/issues/detail?id=27 -
     * http://www.google.com/codesearch/p?hl=en#2tHw6m3DZzo/branches
     * /merge/common/test/java/org/openqa/selenium/AlertsTest.java The aim is : <code>
     * Alert alert = this.getDriver().switchTo().alert();
     * alert.accept();
     * </code> Until then, the following hack does override the confirm method in Javascript to return the given value.
     * 
     * @param {@code true} to accept the confirmation dialog, {@code false} to cancel it
     */
    public void makeConfirmDialogSilent(boolean accept)
    {
        String script = String.format("window.confirm = function() { return %s; }", accept);
        getDriver().executeScript(script);
    }

    /**
     * @see #makeConfirmDialogSilent(boolean)
     * @since 3.2M3
     */
    public void makeAlertDialogSilent()
    {
        getDriver().executeScript("window.alert = function() { return true; }");
    }

    /**
     * @since 3.2M3
     */
    public void waitForNotificationErrorMessage(String message)
    {
        waitUntilElementIsVisible(By.xpath("//div[contains(@class,'xnotification-error') " + "and contains(text(), '"
            + message + "')]"));
        // in order to improve test speed, clicking on the notification will make it disappear
        getDriver().findElement(
            By.xpath("//div[contains(@class,'xnotification-error') " + "and contains(text(), '" + message + "')]"))
            .click();
    }

    /**
     * @since 3.2M3
     */
    public void waitForNotificationWarningMessage(String message)
    {
        waitUntilElementIsVisible(By.xpath("//div[contains(@class,'xnotification-warning') " + "and contains(text(), '"
            + message + "')]"));
        // in order to improve test speed, clicking on the notification will make it disappear
        getDriver().findElement(
            By.xpath("//div[contains(@class,'xnotification-warning') " + "and contains(text(), '" + message + "')]"))
            .click();
    }

    /**
     * @since 3.2M3
     */
    public void waitForNotificationSuccessMessage(String message)
    {
        waitUntilElementIsVisible(By.xpath("//div[contains(@class,'xnotification-done') " + "and contains(text(), '"
            + message + "')]"));
        // in order to improve test speed, clicking on the notification will make it disappear
        getDriver().findElement(
            By.xpath("//div[contains(@class,'xnotification-done') " + "and contains(text(), '" + message + "')]"))
            .click();
    }
}
