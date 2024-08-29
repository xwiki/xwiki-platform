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

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;

import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptException;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.CommandExecutor;
import org.openqa.selenium.remote.ErrorHandler;
import org.openqa.selenium.remote.FileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Wraps a {@link org.openqa.selenium.WebDriver} instance and adds new APIs useful for XWiki tests.
 *
 * @version $Id$
 * @since 7.0M2
 */
public class XWikiWebDriver extends RemoteWebDriver
{
    private RemoteWebDriver wrappedDriver;

    /**
     * How long to wait (in seconds) before failing a test because an element cannot be found. Can be overridden with
     * setTimeout.
     */
    private int timeout = 10;

    public XWikiWebDriver(RemoteWebDriver wrappedDriver)
    {
        this.wrappedDriver = wrappedDriver;

        // Wait when trying to find elements till the timeout expires
        setDriverImplicitWait();
    }

    // XWiki-specific APIs

    public WebElement findElementWithoutWaiting(By by)
    {
        // Temporarily remove the implicit wait on the driver since we're doing our own waits...
        manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
        try {
            return findElement(by);
        } finally {
            setDriverImplicitWait();
        }
    }

    /**
     * Same as {@link #findElementWithoutWaiting(By)} but don't scroll to make the element visible. Useful for example
     * whenverifying that the page has finished loading (and thus there's no element visible and we cannot scroll to
     * it).
     *
     * @since 10.8.1
     * @since 10.9
     */
    public WebElement findElementWithoutWaitingWithoutScrolling(By by)
    {
        // Temporarily remove the implicit wait on the driver since we're doing our own waits...
        // Trying to use another unit in case there is a conflict when calling implicitlyWait both here
        // and in waitUntilCondition.
        manage().timeouts().implicitlyWait(Duration.ofMillis(1));
        try {
            return findElementWithoutScrolling(by);
        } finally {
            setDriverImplicitWait();
        }
    }

    public List<WebElement> findElementsWithoutWaiting(By by)
    {
        // Temporarily remove the implicit wait on the driver since we're doing our own waits...
        manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
        try {
            return findElements(by);
        } finally {
            setDriverImplicitWait();
        }
    }

    public WebElement findElementWithoutWaiting(WebElement element, By by)
    {
        // Temporarily remove the implicit wait on the driver since we're doing our own waits...
        manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
        try {
            return element.findElement(by);
        } finally {
            setDriverImplicitWait();
        }
    }

    public List<WebElement> findElementsWithoutWaiting(WebElement element, By by)
    {
        // Temporarily remove the implicit wait on the driver since we're doing our own waits...
        manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
        try {
            return element.findElements(by);
        } finally {
            setDriverImplicitWait();
        }
    }

    /**
     * Should be used when the result is supposed to be true (otherwise you'll incur the timeout and an error will be
     * raised!).
     */
    public boolean hasElement(By by)
    {
        try {
            findElement(by);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public boolean hasElementWithoutWaiting(By by)
    {
        try {
            findElementWithoutWaiting(by);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Same as {@link #hasElementWithoutWaiting(By)} but don't scroll to make the element visible. Useful for example
     * whenverifying that the page has finished loading (and thus there's no element visible and we cannot scroll to
     * it).
     *
     * @since 10.8.1
     * @since 10.9
     */
    public boolean hasElementWithoutWaitingWithoutScrolling(By by)
    {
        try {
            findElementWithoutWaitingWithoutScrolling(by);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public boolean hasElementWithoutWaiting(WebElement element, By by)
    {
        try {
            findElementWithoutWaiting(element, by);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Should be used when the result is supposed to be true (otherwise you'll incur the timeout).
     */
    public boolean hasElement(WebElement element, By by)
    {
        try {
            element.findElement(by);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public <T> T waitUntilCondition(ExpectedCondition<T> condition, int timeout)
    {
        int currentTimeout = getTimeout();

        try {
            setTimeout(timeout);

            return waitUntilCondition(condition);
        } finally {
            setTimeout(currentTimeout);
        }
    }

    public <T> T waitUntilCondition(ExpectedCondition<T> condition)
    {
        // Temporarily remove the implicit wait on the driver since we're doing our own waits...
        manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
        Wait<WebDriver> wait = new WebDriverWait(this, Duration.ofSeconds(getTimeout()));
        try {
            return wait.until(condition::apply);
        } finally {
            // Reset timeout
            setDriverImplicitWait();
        }
    }

    /**
     * Forces the driver to wait for a {@link #getTimeout()} number of seconds when looking up page elements
     * before declaring that it cannot find them.
     */
    public void setDriverImplicitWait()
    {
        setDriverImplicitWait(getTimeout());
    }

    /**
     * Forces the driver to wait for passed number of seconds when looking up page elements before declaring that it
     * cannot find them.
     */
    public void setDriverImplicitWait(int timeout)
    {
        manage().timeouts().implicitlyWait(Duration.ofSeconds(timeout));
    }

    public int getTimeout()
    {
        return this.timeout;
    }

    /**
     * @param timeout the number of seconds after which we consider the action to have failed
     */
    public void setTimeout(int timeout)
    {
        this.timeout = timeout;
    }

    /**
     * Wait until the element given by the locator is displayed. Give up after timeout seconds.
     *
     * @param locator the locator for the element to look for.
     */
    public void waitUntilElementIsVisible(final By locator)
    {
        waitUntilElementIsVisible(null, locator);
    }

    /**
     * Wait until the element specified by the locator is displayed. Give up after timeout seconds.
     *
     * @param parentElement where to look for the specified element, {@code null} to look everywhere
     * @param locator the locator for the element to look for
     * @since 7.2
     */
    public void waitUntilElementIsVisible(WebElement parentElement, final By locator)
    {
        waitUntilElementsAreVisible(parentElement, new By[] { locator }, true);
    }

    /**
     * Wait until the element given by the locator is displayed. Give up after specified timeout (in seconds).
     * <p>
     * Only use this API if you absolutely need a longer timeout than the default, otherwise use
     * {@link #waitUntilElementIsVisible(org.openqa.selenium.By)}.
     *
     * @param locator the locator for the element to look for
     * @param timeout the timeout after which to give up
     * @since 5.4RC1
     */
    public void waitUntilElementIsVisible(final By locator, int timeout)
    {
        int currentTimeout = getTimeout();
        try {
            setTimeout(timeout);
            waitUntilElementsAreVisible(new By[] {locator}, true);
        } finally {
            setTimeout(currentTimeout);
        }
    }

    /**
     * Wait until one or all of an array of element locators are displayed.
     *
     * @param locators the array of element locators to look for
     * @param all if true then don't return until all elements are found. Otherwise return after finding one
     */
    public void waitUntilElementsAreVisible(final By[] locators, final boolean all)
    {
        waitUntilElementsAreVisible(null, locators, all);
    }

    /**
     * Wait until one or all of an array of element locators are displayed.
     *
     * @param parentElement where to look for the specified elements, {@code null} to look everywhere
     * @param locators the array of element locators to look for
     * @param all if true then don't return until all elements are found. Otherwise return after finding one
     * @since 7.2
     */
    public void waitUntilElementsAreVisible(final WebElement parentElement, final By[] locators, final boolean all)
    {
        waitUntilCondition(driver -> {
            WebElement element = null;
            for (By locator : locators) {
                try {
                    if (parentElement != null) {
                        // Use the locator from the passed parent element.
                        element = parentElement.findElement(locator);
                    } else {
                        // Use the locator from the root.
                        element = driver.findElement(locator);
                    }
                // We might obtain a StaleElementReferenceException in some edge cases when looking
                // for the same notifications several times for example.
                } catch (NotFoundException|StaleElementReferenceException e) {
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
        });
    }

    /**
     * Waits until the given locator corresponds to either a hidden or a deleted element.
     *
     * @param locator the locator to wait for
     */
    public void waitUntilElementDisappears(final By locator)
    {
        waitUntilElementDisappears(null, locator);
    }

    /**
     * Waits until the given locator corresponds to either a hidden or a deleted element.
     *
     * @param parentElement the element from which to start the search
     * @param locator the locator to wait for
     */
    public void waitUntilElementDisappears(final WebElement parentElement, final By locator)
    {
        waitUntilCondition(driver -> {
            try {
                WebElement element = null;
                // Note: Make sure to perform the find operation without waiting, since if the element is already
                // gone (what we really want here) there is no point to wait for it.
                if (parentElement != null) {
                    // Use the locator from the passed parent element.
                    element = findElementWithoutWaiting(parentElement, locator);
                } else {
                    // Use the locator from the root.
                    element = findElementWithoutWaiting(locator);
                }
                return !element.isDisplayed();
            } catch (NotFoundException e) {
                return Boolean.TRUE;
            } catch (StaleElementReferenceException e) {
                // The element was removed from DOM in the meantime
                return Boolean.TRUE;
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
        makeElementVisible(findElement(locator));
    }

    public void makeElementVisible(WebElement element)
    {
        // RenderedWebElement.hover() don't seem to work, workarounded using JavaScript call
        executeJavascript("arguments[0].style.visibility='visible'", element);
    }

    /**
     * Waits until the given element has a non-empty value for an attribute.
     *
     * @param locator the element to wait on
     * @param attributeName the name of the attribute to check
     */
    public void waitUntilElementHasNonEmptyAttributeValue(final By locator, final String attributeName)
    {
        waitUntilCondition(driver -> {
            try {
                WebElement element = driver.findElement(locator);
                return !element.getAttribute(attributeName).isEmpty();
            } catch (NotFoundException e) {
                return false;
            } catch (StaleElementReferenceException e) {
                // The element was removed from DOM in the meantime
                return false;
            }
        });
    }

    /**
     * Waits until the given element is enabled.
     *
     * @param element the element to wait on
     * @since 12.9RC1
     */
    public void waitUntilElementIsEnabled(WebElement element)
    {
        waitUntilCondition(element, WebElement::isEnabled);
    }

    /**
     * Waits until the given element is disabled.
     *
     * @param element the element to wait on
     * @since 15.6RC1
     * @since 15.5.1
     * @since 14.10.15
     */
    public void waitUntilElementIsDisabled(WebElement element)
    {
        waitUntilCondition(element, Predicate.not(WebElement::isEnabled));
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
        waitUntilCondition(driver -> {
            try {
                WebElement element = driver.findElement(locator);
                return expectedValue.equals(element.getAttribute(attributeName));
            } catch (NotFoundException e) {
                return false;
            } catch (StaleElementReferenceException e) {
                // The element was removed from DOM in the meantime
                return false;
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
        waitUntilCondition(driver -> {
            try {
                WebElement element = driver.findElement(locator);
                return element.getAttribute(attributeName).endsWith(expectedValue);
            } catch (NotFoundException e) {
                return false;
            } catch (StaleElementReferenceException e) {
                // The element was removed from DOM in the meantime
                return false;
            }
        });
    }

    /**
     * Waits until the given element contain a certain value for an attribute.
     * @param locator the element to wait on.
     * @param attributeName the name of attribute to check.
     * @param expectedValue the value that should be contained in the attribute.
     * @since 11.1RC1
     */
    public void waitUntilElementContainsAttributeValue(final By locator, final String attributeName,
        final String expectedValue)
    {
        waitUntilCondition(driver -> {
            try {
                WebElement element = driver.findElement(locator);
                return element.getAttribute(attributeName).contains(expectedValue);
            } catch (NotFoundException e) {
                return false;
            } catch (StaleElementReferenceException e) {
                // The element was removed from DOM in the meantime
                return false;
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
        waitUntilElementHasTextContent(() -> findElement(locator), expectedValue);
    }

    /**
     * Waits until the given element has a certain value as its inner text.
     *
     * @param getElement an arbitrary supplier for the element to wait on. {@link WebElement#getText()} is called on
     *     the returned value and compared to the expected value
     * @param expectedValue the content value to wait for
     * @since 15.10RC1
     */
    public void waitUntilElementHasTextContent(Supplier<WebElement> getElement, String expectedValue)
    {
        waitUntilCondition(driver -> {
            try {
                WebElement element = getElement.get();
                return Objects.equals(expectedValue, element.getText());
            } catch (NotFoundException | StaleElementReferenceException e) {
                // In case of NotFoundException, the element is not yet present in the DOM.
                // In case of StaleElementReferenceException, the element was removed from the DOM between the result of
                // findElement and the call to getText.
                return false;
            }
        });
    }

    public Object executeJavascript(String javascript, Object... arguments)
    {
        return executeScript(javascript, arguments);
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
     * @param accept {@code true} to accept the confirmation dialog, {@code false} to cancel it
     */
    public void makeConfirmDialogSilent(boolean accept)
    {
        String script = String.format("window.confirm = function() { return %s; }", accept);
        executeScript(script);
    }

    /**
     * @see #makeConfirmDialogSilent(boolean)
     * @since 3.2M3
     */
    public void makeAlertDialogSilent()
    {
        executeScript("window.alert = function() { return true; }");
    }

    /**
     * Waits until the provided javascript expression returns {@code true}.
     * <p>
     * The wait is done while the expression returns {@code false}.
     *
     * @param booleanExpression the javascript expression to wait for to return {@code true}. The expression must have a
     *            {@code return} statement on the last line, e.g. {@code "return window.jQuery != null"}
     * @param arguments any arguments passed to the javascript expression
     * @throws IllegalArgumentException if the evaluated expression does not return a boolean result
     * @see #executeJavascript(String, Object...)
     * @since 6.2
     */
    public void waitUntilJavascriptCondition(final String booleanExpression, final Object... arguments)
        throws IllegalArgumentException
    {
        waitUntilCondition(driver -> {
            boolean result = false;

            try {
                Object rawResult = executeJavascript(booleanExpression, arguments);
                if (rawResult instanceof Boolean) {
                    result = (Boolean) rawResult;
                } else {
                    throw new IllegalArgumentException(String.format(
                        "The executed javascript expression [%s] called with the arguments [%s] does not return a"
                            + " boolean value [%s]",
                        booleanExpression, Arrays.toString(arguments), rawResult));
                }
            } catch (JavascriptException e) {
                // We might obtain reference error when checking the presence of some properties during a wait.
                if (!e.getMessage().contains("ReferenceError")) {
                    throw e;
                }
            }

            return result;
        });
    }

    // WebDriver APIs

    @Override
    public void get(String s)
    {
        this.wrappedDriver.get(s);
    }

    @Override
    public String getCurrentUrl()
    {
        return this.wrappedDriver.getCurrentUrl();
    }

    @Override
    public String getTitle()
    {
        return this.wrappedDriver.getTitle();
    }

    @Override
    public List<WebElement> findElements(By by)
    {
        return this.wrappedDriver.findElements(by);
    }

    // Make sure the element is visible by scrolling it into view. Otherwise it's possible for example  that the
    // visible floating save bar would hide the element.
    public WebElement scrollTo(WebElement element)
    {
        executeScript("arguments[0].scrollIntoView();", element);
        return element;
    }

    /**
     * Instantaneously scrolls to the given coordinates inside the web page. If you want to scroll to a specific {@link
     * WebElement}, see {@link #scrollTo(WebElement)}.
     *
     * @param xCoord is the pixel along the horizontal axis of the web page that you want displayed in the upper
     *     left
     * @param yCoord is the pixel along the vertical axis of the web page that you want displayed in the upper left
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/Element/scrollTo">MDN Web Docs -
     *     Element.scrollTo()</a>
     * @see #scrollTo(WebElement)
     * @since 13.3RC1
     * @since 12.10.7
     */
    public void scrollTo(int xCoord, int yCoord)
    {
        // This action is instantaneous, allowing to continue testing while knowing precisely the browser's view of the 
        // web page.
        executeScript(String.format("window.scrollTo(%d, %d)", xCoord, yCoord));
    }

    /**
     * Overwrites {@link WebDriver#findElement(By)} to make sure the found element is visible by scrolling it into view.
     * This means that calling this method can have side effects on the User Interface. If the element you're looking
     * for doesn't have to be visible in the viewport then you should use {@link #findElementWithoutScrolling(By)}
     * instead.
     * <p>
     * Also node that this method is called internally by APIs such as
     * {@code ExpectedConditions#presenceOfElementLocated()} so if you don't want the scrolling then you should
     * implement your own {@link ExpectedCondition} using {@link #findElementWithoutScrolling(By)}.
     */
    @Override
    public WebElement findElement(By by)
    {
        WebElement element = this.wrappedDriver.findElement(by);
        return this.scrollTo(element);
    }

    @Override
    public String getPageSource()
    {
        return this.wrappedDriver.getPageSource();
    }

    @Override
    public void close()
    {
        this.wrappedDriver.close();
    }

    @Override
    public void quit()
    {
        this.wrappedDriver.quit();
    }

    @Override
    public Set<String> getWindowHandles()
    {
        return this.wrappedDriver.getWindowHandles();
    }

    @Override
    public String getWindowHandle()
    {
        return this.wrappedDriver.getWindowHandle();
    }

    @Override
    public TargetLocator switchTo()
    {
        return this.wrappedDriver.switchTo();
    }

    @Override
    public Navigation navigate()
    {
        return this.wrappedDriver.navigate();
    }

    @Override
    public Options manage()
    {
        return this.wrappedDriver.manage();
    }

    // Remote WebDriver APIs

    @Override
    public void setFileDetector(FileDetector detector)
    {
        this.wrappedDriver.setFileDetector(detector);
    }

    @Override
    public SessionId getSessionId()
    {
        return this.wrappedDriver.getSessionId();
    }

    @Override
    public ErrorHandler getErrorHandler()
    {
        return this.wrappedDriver.getErrorHandler();
    }

    @Override
    public void setErrorHandler(ErrorHandler handler)
    {
        this.wrappedDriver.setErrorHandler(handler);
    }

    @Override
    public CommandExecutor getCommandExecutor()
    {
        return this.wrappedDriver.getCommandExecutor();
    }

    @Override
    public Capabilities getCapabilities()
    {
        return this.wrappedDriver.getCapabilities();
    }

    @Override
    public <X> X getScreenshotAs(OutputType<X> outputType) throws WebDriverException
    {
        return this.wrappedDriver.getScreenshotAs(outputType);
    }

    @Override
    public Object executeScript(String script, Object... args)
    {
        return this.wrappedDriver.executeScript(script, args);
    }

    @Override
    public Object executeAsyncScript(String script, Object... args)
    {
        return this.wrappedDriver.executeAsyncScript(script, args);
    }

    @Override
    public void setLogLevel(Level level)
    {
        this.wrappedDriver.setLogLevel(level);
    }

    @Override
    public FileDetector getFileDetector()
    {
        return this.wrappedDriver.getFileDetector();
    }

    @Override
    public String toString()
    {
        return this.wrappedDriver.toString();
    }

    /**
     * Compared to using clear() + sendKeys(), this method ensures that an "input" event is triggered on the JavaScript
     * side for an empty ("") value. Without this, the clear() method triggers just a "change" event.
     *
     * @param textInputElement an element accepting text input
     * @param newTextValue the new text value to set
     * @see <a href="https://code.google.com/p/selenium/issues/detail?id=214">Issue 214</a>
     * @since 7.2M3
     */
    public void setTextInputValue(WebElement textInputElement, String newTextValue)
    {
        textInputElement.clear();
        textInputElement.sendKeys(newTextValue);
        // To be sure the right events are sent.
        textInputElement.sendKeys(Keys.TAB);
    }

    /**
     * Adds a marker in the DOM of the browser that will only be available until we leave or reload the current page.
     * <p>
     * To be used mainly before {@link #waitUntilPageIsReloaded()}.
     *
     * @since 7.4M2
     */
    public void addPageNotYetReloadedMarker()
    {
        StringBuilder markerJs = new StringBuilder();
        markerJs.append("new function () {");
        markerJs.append("  var marker = document.createElement('div');");
        markerJs.append("  marker.style.display='none';");
        markerJs.append("  marker.id='pageNotYetReloadedMarker';");
        markerJs.append("  document.body.appendChild(marker);");
        markerJs.append("}()");

        executeJavascript(markerJs.toString());
    }

    /**
     * Waits until the previously added marker is no longer found on the current page, signaling that the page has been
     * changed or reloaded. Useful when the page loading is done by jJavaScript and Selenium can not help in telling us
     * when we have left the old page.
     * <p>
     * To be used always after {@link #addPageNotYetReloadedMarker()}.
     *
     * @since 7.4M2
     */
    public void waitUntilPageIsReloaded()
    {
        waitUntilCondition(input -> {
            // Note: make sure we don't scroll since we're looking for an element that's not here anymore and
            // thus it would produce a StaleElementException!
            return !hasElementWithoutWaitingWithoutScrolling(By.id("pageNotYetReloadedMarker"));
        });
    }

    /**
     * Same as {@link #findElement(By)} but don't scroll to make the element visible. Useful for example when
     * verifying that the page has finished loading (and thus there's no element visible and we cannot scroll to it).
     *
     * @since 10.8.1
     * @since 10.9
     */
    public WebElement findElementWithoutScrolling(By by)
    {
        return this.wrappedDriver.findElement(by);
    }

    /**
     * @return the original {@link RemoteWebDriver} created for selenium tests.
     *          The original driver should be used for custom {@link org.openqa.selenium.interactions.Actions}.
     * @since 11.3RC1
     */
    public RemoteWebDriver getWrappedDriver()
    {
        return this.wrappedDriver;
    }

    /**
     * Utility method to perform a drag &amp; drop by using the appropriate WebDriver.
     * @param source the element to drag
     * @param target the element where to drop
     *
     * @since 11.3RC1
     */
    public void dragAndDrop(WebElement source, WebElement target)
    {
        createActions().dragAndDrop(source, target).perform();
    }

    /**
     * Utility method to build a proper instance of {@link Actions}.
     * @return a new instance of {@link Actions}.
     * @since 11.9RC1
     */
    public Actions createActions()
    {
        return new Actions(getWrappedDriver());
    }

    /**
     * Same as {@link Actions#moveToElement(WebElement, int, int)} except that the target is the top-left corner of the
     * target, instead of the center.
     * @param target the element for which we want to reach the offset from the top-left corner.
     * @param offsetX the offset on the right of the top-left corner to move to
     * @param offsetY the offset on the bottom of the top-left corner to move to
     * @param chainFrom the existing actions to be chain to, or null to create a dedicated chain of actions.
     * @return an actions with the right move.
     * @since 11.9RC1
     */
    public Actions moveToTopLeftCornerOfTargetWithOffset(WebElement target, int offsetX, int offsetY, Actions chainFrom)
    {
        Dimension containerDimension = target.getSize();
        int newOffsetX = - containerDimension.getWidth() / 2 + offsetX;
        int newOffsetY = - containerDimension.getHeight() / 2 + offsetY;

        if (chainFrom == null) {
            return createActions().moveToElement(target, newOffsetX, newOffsetY);
        } else {
            return chainFrom.moveToElement(target, newOffsetX, newOffsetY);
        }
    }

    private void waitUntilCondition(WebElement element, Predicate<WebElement> condition)
    {
        waitUntilCondition(driver -> {
            try {
                return condition.test(element);
            } catch (NotFoundException e) {
                return false;
            } catch (StaleElementReferenceException e) {
                // The element was removed from DOM in the meantime
                return false;
            }
        });
    }
}
