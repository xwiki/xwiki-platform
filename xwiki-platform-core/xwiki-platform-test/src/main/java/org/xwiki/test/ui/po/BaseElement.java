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
}
