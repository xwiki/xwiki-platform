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

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Keyboard;
import org.openqa.selenium.interactions.Mouse;
import org.openqa.selenium.remote.CommandExecutor;
import org.openqa.selenium.remote.ErrorHandler;
import org.openqa.selenium.remote.FileDetector;
import org.openqa.selenium.remote.RemoteStatus;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps a {@link org.openqa.selenium.WebDriver} instance and adds new APIs useful for XWiki tests.
 *
 * @version $Id$
 * @since 7.0M2
 */
public class XWikiWebDriver extends RemoteWebDriver
{
    private final static Logger LOGGER = LoggerFactory.getLogger(XWikiWebDriver.class);

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
        manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
        try {
            return this.wrappedDriver.findElement(by);
        } finally {
            setDriverImplicitWait();
        }
    }

    public List<WebElement> findElementsWithoutWaiting(By by)
    {
        // Temporarily remove the implicit wait on the driver since we're doing our own waits...
        manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
        try {
            return this.wrappedDriver.findElements(by);
        } finally {
            setDriverImplicitWait();
        }
    }

    public WebElement findElementWithoutWaiting(WebElement element, By by)
    {
        // Temporarily remove the implicit wait on the driver since we're doing our own waits...
        manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
        try {
            return element.findElement(by);
        } finally {
            setDriverImplicitWait();
        }
    }

    public List<WebElement> findElementsWithoutWaiting(WebElement element, By by)
    {
        // Temporarily remove the implicit wait on the driver since we're doing our own waits...
        manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
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

    public <T> void waitUntilCondition(ExpectedCondition<T> condition)
    {
        // Temporarily remove the implicit wait on the driver since we're doing our own waits...
        manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
        Wait<WebDriver> wait = new WebDriverWait(this, getTimeout());
        try {
            wait.until(condition);
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
        manage().timeouts().implicitlyWait(timeout, TimeUnit.SECONDS);
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
        waitUntilElementsAreVisible(new By[] {locator}, true);
    }

    /**
     * Wait until the element given by the locator is displayed. Give up after specified timeout (in seconds).
     * <p/>
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
     * Wait until one or all of a array of element locators are displayed.
     *
     * @param locators the array of element locators to look for.
     * @param all if true then don't return until all elements are found. Otherwise return after finding one.
     */
    public void waitUntilElementsAreVisible(final By[] locators, final boolean all)
    {
        waitUntilCondition(new ExpectedCondition<WebElement>()
        {
            @Override
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
        waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            @Override
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
        waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                try {
                    WebElement element = driver.findElement(locator);
                    return !element.getAttribute(attributeName).isEmpty();
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
     * Waits until the given element has a certain value for an attribute.
     *
     * @param locator the element to wait on
     * @param attributeName the name of the attribute to check
     * @param expectedValue the attribute value to wait for
     */
    public void waitUntilElementHasAttributeValue(final By locator, final String attributeName,
        final String expectedValue)
    {
        waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            @Override
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
        waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            @Override
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
        waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                WebElement element = driver.findElement(locator);
                return Boolean.valueOf(expectedValue.equals(element.getText()));
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
     * <p/>
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
        waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                boolean result = false;

                Object rawResult = executeJavascript(booleanExpression, arguments);
                if (rawResult instanceof Boolean) {
                    result = (Boolean) rawResult;
                } else {
                    throw new IllegalArgumentException("The executed javascript does not return a boolean value");
                }

                return result;
            }
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

    @Override
    public WebElement findElement(By by)
    {
        return this.wrappedDriver.findElement(by);
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
    public RemoteStatus getRemoteStatus()
    {
        return this.wrappedDriver.getRemoteStatus();
    }

    @Override
    public <X> X getScreenshotAs(OutputType<X> outputType) throws WebDriverException
    {
        return this.wrappedDriver.getScreenshotAs(outputType);
    }

    @Override
    public WebElement findElementById(String using)
    {
        return this.wrappedDriver.findElementById(using);
    }

    @Override
    public List<WebElement> findElementsById(String using)
    {
        return this.wrappedDriver.findElementsById(using);
    }

    @Override
    public WebElement findElementByLinkText(String using)
    {
        return this.wrappedDriver.findElementByLinkText(using);
    }

    @Override
    public List<WebElement> findElementsByLinkText(String using)
    {
        return this.wrappedDriver.findElementsByLinkText(using);
    }

    @Override
    public WebElement findElementByPartialLinkText(String using)
    {
        return this.wrappedDriver.findElementByPartialLinkText(using);
    }

    @Override
    public List<WebElement> findElementsByPartialLinkText(String using)
    {
        return this.wrappedDriver.findElementsByPartialLinkText(using);
    }

    @Override
    public WebElement findElementByTagName(String using)
    {
        return this.wrappedDriver.findElementByTagName(using);
    }

    @Override
    public List<WebElement> findElementsByTagName(String using)
    {
        return this.wrappedDriver.findElementsByTagName(using);
    }

    @Override
    public WebElement findElementByName(String using)
    {
        return this.wrappedDriver.findElementByName(using);
    }

    @Override
    public List<WebElement> findElementsByName(String using)
    {
        return this.wrappedDriver.findElementsByName(using);
    }

    @Override
    public WebElement findElementByClassName(String using)
    {
        return this.wrappedDriver.findElementByClassName(using);
    }

    @Override
    public List<WebElement> findElementsByClassName(String using)
    {
        return this.wrappedDriver.findElementsByClassName(using);
    }

    @Override
    public WebElement findElementByCssSelector(String using)
    {
        return this.wrappedDriver.findElementByCssSelector(using);
    }

    @Override
    public List<WebElement> findElementsByCssSelector(String using)
    {
        return this.wrappedDriver.findElementsByCssSelector(using);
    }

    @Override
    public WebElement findElementByXPath(String using)
    {
        return this.wrappedDriver.findElementByXPath(using);
    }

    @Override
    public List<WebElement> findElementsByXPath(String using)
    {
        return this.wrappedDriver.findElementsByXPath(using);
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
    public Keyboard getKeyboard()
    {
        return this.wrappedDriver.getKeyboard();
    }

    @Override
    public Mouse getMouse()
    {
        return this.wrappedDriver.getMouse();
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
}
