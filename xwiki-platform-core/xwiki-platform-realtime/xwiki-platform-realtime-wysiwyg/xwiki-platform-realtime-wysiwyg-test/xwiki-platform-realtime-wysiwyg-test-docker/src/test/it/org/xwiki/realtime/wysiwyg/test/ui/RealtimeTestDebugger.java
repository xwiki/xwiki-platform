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
package org.xwiki.realtime.wysiwyg.test.ui;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.openqa.selenium.logging.LogType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.test.docker.internal.junit5.DockerTestUtils;
import org.xwiki.test.ui.XWikiWebDriver;

/**
 * Print debug information that can help debug failed real-time editing tests.
 * 
 * @version $Id$
 * @since 15.5.4
 * @since 15.10
 */
public class RealtimeTestDebugger implements TestExecutionExceptionHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(RealtimeTestDebugger.class);

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable
    {
        printDebugInfo(getDriver(context));

        throw throwable;
    }

    private XWikiWebDriver getDriver(ExtensionContext context)
    {
        return DockerTestUtils.getStore(context).get(XWikiWebDriver.class, XWikiWebDriver.class);
    }

    private void printDebugInfo(XWikiWebDriver driver)
    {
        String originalWindowHandle = driver.getWindowHandle();

        driver.getWindowHandles().forEach(handle -> {
            driver.switchTo().window(handle);
            LOGGER.info("Debug info for browser window {}:", handle);
            printBrowserLogs(driver);
            printRealtimeDebugInfo(driver);
        });

        driver.switchTo().window(originalWindowHandle);
    }

    private void printBrowserLogs(XWikiWebDriver driver)
    {
        LOGGER.info("Browser console logs:");
        try {
            driver.manage().logs().get(LogType.BROWSER).forEach(entry -> LOGGER.info(entry.toString()));
        } catch (Exception e) {
            // Not all browser drivers support getting the logs.
            LOGGER.warn("Failed to get browser console logs: " + e.getMessage());
        }
    }

    private void printRealtimeDebugInfo(XWikiWebDriver driver)
    {
        try {
            LOGGER.info("Realtime debug info: " + driver.executeScript("return JSON.stringify(window.REALTIME_DEBUG)"));
        } catch (Exception e) {
            LOGGER.warn("Failed to get realtime debug info: " + e.getMessage());
        }
    }
}
