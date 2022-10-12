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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.github.kklisura.cdt.protocol.commands.Browser;
import com.github.kklisura.cdt.protocol.commands.Target;
import com.github.kklisura.cdt.protocol.types.browser.Version;
import com.github.kklisura.cdt.services.ChromeDevToolsService;
import com.github.kklisura.cdt.services.ChromeService;
import com.github.kklisura.cdt.services.types.ChromeTab;
import com.github.kklisura.cdt.services.types.ChromeVersion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
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

    @Mock
    private ChromeService chromeService;

    @Mock
    private ChromeDevToolsService browserDevToolsService;

    @Mock
    private ChromeVersion chromeVersion;

    @BeforeEach
    void configure() throws Exception
    {
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
}
