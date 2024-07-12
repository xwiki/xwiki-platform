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
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.pagefactory.AjaxElementLocatorFactory;
import org.openqa.selenium.support.pagefactory.ElementLocatorFactory;
import org.xwiki.test.ui.PersistentTestContext;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.XWikiWebDriver;

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
        ElementLocatorFactory finder = new AjaxElementLocatorFactory(getDriver(), getDriver().getTimeout());
        PageFactory.initElements(finder, this);
    }

    protected XWikiWebDriver getDriver()
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
     * @since 3.2M3
     */
    public void waitForNotificationErrorMessage(String message)
    {
        waitForNotificationMessage("error", message);
    }

    /**
     * @since 3.2M3
     */
    public void waitForNotificationWarningMessage(String message)
    {
        waitForNotificationMessage("warning", message);
    }

    /**
     * @since 3.2M3
     */
    public void waitForNotificationSuccessMessage(String message)
    {
        waitForNotificationMessage("done", message);
    }

    /**
     * @since 11.3RC1
     */
    public void waitForNotificationInProgressMessage(String message)
    {
        waitForNotificationMessage("inprogress", message);
    }

    /**
     * Waits for a notification message of the specified type with the given message to be displayed.
     * 
     * @param level the notification type (one of error, warning, done)
     * @param message the notification message
     * @see 4.3
     */
    private void waitForNotificationMessage(String level, String message)
    {
        By notificationMessageLocator =
            By.xpath(String.format("//div[contains(@class,'xnotification-%s') and contains(., '%s')]", level, message));
        getDriver().waitUntilElementIsVisible(notificationMessageLocator);
        // In order to improve test speed, clicking on the notification will make it disappear. This also ensures that
        // this method always waits for the last notification message of the specified level.
        try {
            getDriver().findElementWithoutWaiting(notificationMessageLocator).click();
        } catch (WebDriverException e) {
            // The notification message may disappear before we get to click on it and thus we ignore in case there's
            // an error.
        }
    }

    /**
     * @since 8.4.5
     * @since 9.0RC1
     */
    protected boolean isElementVisible(By by)
    {
        return getDriver().findElementWithoutWaiting(by).isDisplayed();
    }

    /**
     * Wait until the page is ready for user interaction. The page is ready when there are no pending HTTP requests
     * (e.g. to load resources or data) and no pending promises (e.g. no asynchronous code that waits to be executed).
     * 
     * @since 14.2RC1
     */
    public void waitUntilPageIsReady()
    {
        By htmlTagLocator = By.tagName("html");
        try {
            getDriver().waitUntilElementHasAttributeValue(htmlTagLocator, "data-xwiki-page-ready", "true");
        } catch (TimeoutException e) {
            // Gather debug information by getting the reasons why the page didn't become ready, e.g., which requests
            // it is waiting for.
            // As require could be async, there is no easy way to return the value directly.
            // So set it in an attribute and get it later.
            getDriver().executeJavascript("require(['xwiki-page-ready'], function(pageReady) {"
                + "  document.documentElement.dataset.debugPendingDelays"
                + " = JSON.stringify(Object.fromEntries(pageReady.getPendingDelays().entries()));"
                + "});");
            // Wait for the attribute to be set and retrieve it.
            String attributeName = "data-debug-pending-delays";
            getDriver().waitUntilElementHasNonEmptyAttributeValue(htmlTagLocator, attributeName);
            // Get the value of the attribute.
            String pendingDelays = getDriver().findElement(htmlTagLocator).getAttribute(attributeName);
            throw new TimeoutException(
                "Page did not become ready within the timeout. Pending delays: " + pendingDelays + ".", e);
        }
    }
}
