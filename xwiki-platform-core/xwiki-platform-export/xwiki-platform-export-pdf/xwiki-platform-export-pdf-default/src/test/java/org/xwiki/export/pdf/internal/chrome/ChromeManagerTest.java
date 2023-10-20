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
package org.xwiki.export.pdf.internal.chrome;

import java.util.Arrays;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mock;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.export.pdf.PDFExportConfiguration;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.github.kklisura.cdt.protocol.commands.Browser;
import com.github.kklisura.cdt.protocol.commands.Target;
import com.github.kklisura.cdt.protocol.types.browser.Version;
import com.github.kklisura.cdt.services.ChromeDevToolsService;
import com.github.kklisura.cdt.services.ChromeService;
import com.github.kklisura.cdt.services.exceptions.ChromeServiceException;
import com.github.kklisura.cdt.services.types.ChromeTab;
import com.github.kklisura.cdt.services.types.ChromeVersion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ChromeManager}.
 * 
 * @version $Id$
 */
@ComponentTest
class ChromeManagerTest
{
    @InjectMockComponents
    private ChromeManager chromeManager;

    @MockComponent
    private ChromeServiceFactory chromeServiceFactory;

    @MockComponent
    private PDFExportConfiguration configuration;

    @Mock
    private ChromeService chromeService;

    @Mock
    private ChromeDevToolsService browserDevToolsService;

    @Mock
    private ChromeVersion chromeVersion;

    @BeforeEach
    void configure() throws Exception
    {
        when(this.configuration.getChromeRemoteDebuggingTimeout()).thenReturn(1);

        when(this.chromeService.getVersion()).thenReturn(this.chromeVersion);
        when(this.chromeServiceFactory.createBrowserDevToolsService(this.chromeVersion))
            .thenReturn(this.browserDevToolsService);

        Browser browser = mock(Browser.class);
        when(this.browserDevToolsService.getBrowser()).thenReturn(browser);
        when(browser.getVersion()).thenReturn(new Version());
    }

    @Test
    void connectAndReconnect() throws Exception
    {
        assertFalse(this.chromeManager.isConnected());

        when(this.chromeServiceFactory.createChromeService("localhost", 9222)).thenReturn(this.chromeService);

        this.chromeManager.connect("localhost", 9222);

        assertTrue(this.chromeManager.isConnected());

        try {
            this.chromeManager.connect("localhost", 9223);
            fail();
        } catch (IllegalStateException e) {
            assertEquals(
                "Chrome is already connected. Please close the current connection before establishing a new one.",
                e.getMessage());
        }

        this.chromeManager.close();
        assertFalse(this.chromeManager.isConnected());

        when(this.chromeServiceFactory.createChromeService("localhost", 9223)).thenReturn(this.chromeService);
        this.chromeManager.connect("localhost", 9223);
        assertTrue(this.chromeManager.isConnected());
    }

    @Test
    void createIncognitoTab() throws Exception
    {
        try {
            this.chromeManager.createIncognitoTab();
            fail();
        } catch (IllegalStateException e) {
            assertEquals("The Chrome web browser is not connected.", e.getMessage());
        }

        when(this.chromeServiceFactory.createChromeService("localhost", 9222)).thenReturn(this.chromeService);
        this.chromeManager.connect("localhost", 9222);

        Target browserTarget = mock(Target.class, "browser");
        when(this.browserDevToolsService.getTarget()).thenReturn(browserTarget);
        when(browserTarget.createBrowserContext(true, null, null)).thenReturn("browserContextId");
        when(browserTarget.createTarget("", null, null, "browserContextId", false, false, false)).thenReturn("second");

        ChromeTab firstTab = new ChromeTab();
        ReflectionUtils.setFieldValue(firstTab, "id", "first");

        ChromeTab secondTab = new ChromeTab();
        ReflectionUtils.setFieldValue(firstTab, "id", "second");

        when(this.chromeService.getTabs()).thenReturn(Arrays.asList(firstTab, secondTab));

        assertNotNull(this.chromeManager.createIncognitoTab());
    }

    @Test
    void dispose() throws Exception
    {
        this.chromeManager.dispose();

        assertFalse(this.chromeManager.isConnected());

        try {
            this.chromeManager.createIncognitoTab();
            fail("The Chrome Manager shouldn't be able to create a new incognito tab after being disposed.");
        } catch (IllegalStateException e) {
            assertEquals("The Chrome web browser is not connected.", e.getMessage());
        }

        try {
            this.chromeManager.connect("localhost", 9222);
            fail("The Chrome Manager shouldn't be able to connect after being disposed.");
        } catch (IllegalStateException e) {
            assertEquals("The Chrome Manager must be initialized before making a connection.", e.getMessage());
        }
    }

    @Test
    void connectWithTimeout() throws Exception
    {
        // Increase the timeout so that it tries multiple times (it waits 2s before retrying).
        when(this.configuration.getChromeRemoteDebuggingTimeout()).thenReturn(3);

        when(this.chromeServiceFactory.createChromeService("localhost", 9222)).thenReturn(this.chromeService);
        when(this.chromeService.getVersion()).thenThrow(new ChromeServiceException("Remote Chrome is not available."));

        try {
            this.chromeManager.connect("localhost", 9222);
            fail("We shouldn't be able to connect if we can't get the browser version.");
        } catch (TimeoutException e) {
            assertEquals("Timeout waiting for Chrome remote debugging to become available."
                + " Waited [4] seconds. Root cause: [ChromeServiceException: Remote Chrome is not available.]", e.getMessage());
        }

        // It should have tried twice to get the version.
        verify(this.chromeService, times(2)).getVersion();
    }

    @Test
    void isConnectedWithTimeout() throws Exception {
        // First we connect to the Chrome remote debugging and then we simulate that it doesn't respond before the
        // configured timeout.
        when(this.chromeServiceFactory.createChromeService("localhost", 9222)).thenReturn(this.chromeService);
        this.chromeManager.connect("localhost", 9222);
        int delay = 5000;
        doAnswer(AdditionalAnswers.answersWithDelay(delay, invocation -> this.chromeVersion)).when(this.chromeService)
            .getVersion();

        long start = System.currentTimeMillis();
        assertFalse(this.chromeManager.isConnected());
        long waitTime = System.currentTimeMillis() - start;
        // The timeout is 1 second (less than the delay) so it should abort the request before it gets a response.
        assertTrue(waitTime < delay, "The timeout was ignored.");
    }
}
