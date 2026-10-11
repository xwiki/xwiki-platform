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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the wasteful-wait detection in {@link XWikiWebDriver}.
 *
 * @version $Id$
 */
class XWikiWebDriverTest
{
    // Only capture WARN and above so that the detection warnings are the only thing we assert on.
    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private XWikiWebDriver createDriver()
    {
        // Deep stubs so that the constructor's manage().timeouts().implicitlyWait(...) call doesn't NPE.
        return new XWikiWebDriver(mock(RemoteWebDriver.class, RETURNS_DEEP_STUBS));
    }

    @Test
    void warnIfWastefulWaitLogsWhenReachingTheTimeout()
    {
        XWikiWebDriver driver = createDriver();

        // Default timeout is 10 seconds, so the threshold is 10000ms.
        driver.warnIfWastefulWait(By.id("missing"), 10000L);

        assertEquals(1, this.logCapture.size());
        String message = this.logCapture.getMessage(0);
        assertTrue(message.contains("wasted [10000] ms"), message);
        assertTrue(message.contains("By.id: missing"), message);
        assertTrue(message.contains("findElementWithoutWaiting()"), message);
    }

    @Test
    void warnIfWastefulWaitSilentBelowTheTimeout()
    {
        XWikiWebDriver driver = createDriver();

        // Default timeout is 10 seconds, so 9999ms is just below the 10000ms threshold.
        driver.warnIfWastefulWait(By.id("found-fast"), 9999L);

        assertEquals(0, this.logCapture.size());
    }

    @Test
    void findElementWarnsWhenNotFoundAfterWaiting()
    {
        XWikiWebDriver driver = createDriver();
        // Use a 0s timeout so that any elapsed time (>= 0) is considered a full wait, making the test independent from
        // the real duration of the (mocked) lookup.
        driver.setTimeout(0);
        By by = By.id("missing");
        when(driver.getWrappedDriver().findElement(by)).thenThrow(new NoSuchElementException("nope"));

        assertThrows(NoSuchElementException.class, () -> driver.findElement(by));

        assertEquals(1, this.logCapture.size());
        assertTrue(this.logCapture.getMessage(0).contains("was not found"), this.logCapture.getMessage(0));
    }

    @Test
    void findElementDoesNotWarnWhenFound()
    {
        XWikiWebDriver driver = createDriver();
        driver.setTimeout(0);
        By by = By.id("present");
        WebElement element = mock(WebElement.class);
        when(driver.getWrappedDriver().findElement(by)).thenReturn(element);

        assertSame(element, driver.findElement(by));

        assertEquals(0, this.logCapture.size());
    }

    @Test
    void findElementWithoutScrollingWarnsWhenNotFoundAfterWaiting()
    {
        XWikiWebDriver driver = createDriver();
        driver.setTimeout(0);
        By by = By.id("missing");
        when(driver.getWrappedDriver().findElement(by)).thenThrow(new NoSuchElementException("nope"));

        assertThrows(NoSuchElementException.class, () -> driver.findElementWithoutScrolling(by));

        assertEquals(1, this.logCapture.size());
        assertTrue(this.logCapture.getMessage(0).contains("was not found"), this.logCapture.getMessage(0));
    }

    @Test
    void findElementsWarnsWhenEmptyAfterWaiting()
    {
        XWikiWebDriver driver = createDriver();
        driver.setTimeout(0);
        By by = By.className("missing");
        when(driver.getWrappedDriver().findElements(by)).thenReturn(List.of());

        assertTrue(driver.findElements(by).isEmpty());

        assertEquals(1, this.logCapture.size());
        assertTrue(this.logCapture.getMessage(0).contains("was not found"), this.logCapture.getMessage(0));
    }

    @Test
    void findElementsDoesNotWarnWhenNotEmpty()
    {
        XWikiWebDriver driver = createDriver();
        driver.setTimeout(0);
        By by = By.className("present");
        when(driver.getWrappedDriver().findElements(by)).thenReturn(List.of(mock(WebElement.class)));

        assertEquals(1, driver.findElements(by).size());

        assertEquals(0, this.logCapture.size());
    }

    @Test
    void truncateToLastXWikiFrameDropsTrailingNonXWikiFramesButKeepsInterleavedOnes()
    {
        StackTraceElement driverFrame =
            new StackTraceElement("org.xwiki.test.ui.XWikiWebDriver", "findElement", "XWikiWebDriver.java", 715);
        StackTraceElement interleaved = new StackTraceElement(
            "org.openqa.selenium.support.pagefactory.DefaultElementLocator", "findElement", null, 59);
        StackTraceElement testFrame = new StackTraceElement(
            "org.xwiki.administration.test.ui.RegisterIT", "administrationModalUserCreation", "RegisterIT.java", 177);
        StackTraceElement selenium =
            new StackTraceElement("org.openqa.selenium.support.ui.SlowLoadableComponent", "get", null, 48);
        StackTraceElement jdk = new StackTraceElement("jdk.proxy1.$Proxy70", "click", null, -1);

        StackTraceElement[] result = XWikiWebDriver.truncateToLastXWikiFrame(
            new StackTraceElement[] {driverFrame, interleaved, testFrame, selenium, jdk});

        // Everything up to and including the last org.xwiki frame (testFrame) is kept, the trailing selenium/jdk frames
        // are dropped, and the non-xwiki frame sitting between two xwiki frames is preserved.
        assertArrayEquals(new StackTraceElement[] {driverFrame, interleaved, testFrame}, result);
    }

    @Test
    void truncateToLastXWikiFrameReturnsInputWhenNoXWikiFrame()
    {
        StackTraceElement[] stackTrace = new StackTraceElement[] {
            new StackTraceElement("org.openqa.selenium.remote.RemoteWebDriver", "findElement", null, 100),
            new StackTraceElement("jdk.proxy1.$Proxy70", "click", null, -1)
        };

        assertSame(stackTrace, XWikiWebDriver.truncateToLastXWikiFrame(stackTrace));
    }

    @Test
    void truncateToLastXWikiFrameKeepsWholeTraceWhenLastFrameIsXWiki()
    {
        StackTraceElement[] stackTrace = new StackTraceElement[] {
            new StackTraceElement("org.xwiki.test.ui.XWikiWebDriver", "findElement", "XWikiWebDriver.java", 715),
            new StackTraceElement("org.xwiki.administration.test.ui.RegisterIT", "test", "RegisterIT.java", 177)
        };

        assertArrayEquals(stackTrace, XWikiWebDriver.truncateToLastXWikiFrame(stackTrace));
    }
}
