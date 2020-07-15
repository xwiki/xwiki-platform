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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseElement.class);

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
            // The notification message may disappear before we get to click on it.
            getDriver().findElementWithoutWaiting(notificationMessageLocator).click();
        } catch (WebDriverException e) {
            // Ignore.
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
     * Waits for the javascript libraries and their plugins that need to load before the UI's elements can be used
     * safely.
     * <p>
     * Subclassed should override this method and add additional checks needed by their logic.
     *
     * @since 12.5RC1
     */
    public void waitUntilPageJSIsLoaded()
    {
        // Prototype
        getDriver().waitUntilJavascriptCondition("return window.Prototype != null && window.Prototype.Version != null");

        // JQuery and dependencies
        // JQuery dropdown plugin needed for the edit button's dropdown menu.
        // TODO: We seem to have a flicker possibly caused by this check taking more than the default 10s timeout from
        // time to time, see https://jira.xwiki.org/browse/XCOMMONS-1865. Testing this hypothesis by waiting a first
        // time and if it fails waiting again and logging some message. Remove if the increased timeout doesn't help.
        // If it helps, then we might need to dive deeper and understand why it can take more than 10s (underpowered
        // machine, etc).
        try {
            getDriver()
                .waitUntilJavascriptCondition("return window.jQuery != null && window.jQuery().dropdown != null");
        } catch (TimeoutException e) {
            LOGGER.error("Wait for JQuery took more than [{}] seconds", getDriver().getTimeout(), e);
            getDriver()
                .waitUntilJavascriptCondition("return window.jQuery != null && window.jQuery().dropdown != null");
        }

        // Make sure all asynchronous elements have been executed
        getDriver().waitUntilJavascriptCondition("return !document.getElementsByClassName('xwiki-async').length");

        // Make sure the shortcuts are loaded
        getDriver().waitUntilJavascriptCondition("return shortcut != null && shortcut != undefined");
    }
}
