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
package org.xwiki.test.docker.junit5;

import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WindowType;
import org.xwiki.test.ui.TestUtils;

/**
 * Utility methods for multi-user tests using multiple tabs of a single browser instance.
 *
 * @version $Id$
 * @since 15.10.12
 * @since 16.4.1
 * @since 16.6.0RC1
 */
public class MultiUserTestUtils
{
    private final TestUtils setup;

    private final TestConfiguration testConfiguration;

    private final String firstTabHandle;

    private final Map<String, String> baseURLByTab = new HashMap<>();

    private final Map<String, String> secretTokenByTab = new HashMap<>();

    /**
     * Creates a new instance wrapping the given test configuration and test setup.
     * 
     * @param setup the test setup
     * @param testConfiguration the test configuration
     */
    public MultiUserTestUtils(TestUtils setup, TestConfiguration testConfiguration)
    {
        this.setup = setup;
        this.testConfiguration = testConfiguration;

        // Store the ID of the current browser tab because we're going to open new tabs which will have to be closed
        // after each test, and we need this to avoid closing all tabs.
        this.firstTabHandle = setup.getDriver().getWindowHandle();
    }

    /**
     * @return the handle that can be used to switch to the first browser tab
     */
    public String getFirstTabHandle()
    {
        return this.firstTabHandle;
    }

    /**
     * Open a new browser tab and associate a base URL with it. By using different base URLs on different browser tabs
     * we can authenticate multiple XWiki users at the same time (in different tabs).
     *
     * @param xwikiDomain the domain used to access XWiki on the newly created tab
     * @return the handle of the new tab
     * @see TestConfiguration#getServletEngineNetworkAliases()
     */
    public String openNewBrowserTab(String xwikiDomain)
    {
        beforeTabSwitch();

        String tabHandle = this.setup.getDriver().switchTo().newWindow(WindowType.TAB).getWindowHandle();

        String baseURL = String.format("http://%s:%s/xwiki", xwikiDomain,
            this.testConfiguration.getServletEngine().getInternalPort());
        this.baseURLByTab.put(tabHandle, baseURL);
        afterTabSwitch();

        return tabHandle;
    }

    /**
     * Switch to another (existing) browser tab, restoring the XWiki URL prefix associated with that tab on
     * {@link TestUtils}. By using different XWiki URL prefixes on different browser tabs we can authenticate multiple
     * XWiki users at the same time (in different tabs).
     *
     * @param tabHandle the tab to switch to
     */
    public void switchToBrowserTab(String tabHandle)
    {
        if (!tabHandle.equals(this.setup.getDriver().getWindowHandle())) {
            beforeTabSwitch();
            setup.getDriver().switchTo().window(tabHandle);
            afterTabSwitch();
        }
    }

    /**
     * Close all browser tabs except the first one. Before calling this method, make sure that closing the browser tabs
     * doesn't trigger an alert (e.g. like the one shown when leaving the edit mode with unsaved changes).
     */
    public void closeTabs()
    {
        // Close all tabs except the first one.
        this.setup.getDriver().getWindowHandles().stream().filter(handle -> !handle.equals(this.firstTabHandle))
            .forEach(handle -> {
                this.setup.getDriver().switchTo().window(handle).close();
                // Clean up the state associated with the closed tab.
                this.baseURLByTab.remove(handle);
                this.secretTokenByTab.remove(handle);
            });

        // Switch back to the first tab.
        this.setup.getDriver().switchTo().window(this.firstTabHandle);
        afterTabSwitch();
    }

    private void beforeTabSwitch()
    {
        WebDriver driver = this.setup.getDriver();
        String currentTabHandle = driver.getWindowHandle();
        this.baseURLByTab.put(currentTabHandle, this.setup.getBaseURL());
        this.secretTokenByTab.put(currentTabHandle, this.setup.getSecretToken());
    }

    private void afterTabSwitch()
    {
        WebDriver driver = this.setup.getDriver();
        String currentTabHandle = driver.getWindowHandle();
        if (this.baseURLByTab.containsKey(currentTabHandle)) {
            TestUtils.setURLPrefix(this.baseURLByTab.get(currentTabHandle));
            // The secret token is cached in the test setup so we need to make sure the new tab (that uses a different
            // domain to access XWiki) doesn't use the secret token that was cached for the previous tab.
            if (this.secretTokenByTab.containsKey(currentTabHandle)) {
                // Restore the secret token associated with the current tab.
                this.setup.setSecretToken(this.secretTokenByTab.get(currentTabHandle));
            } else {
                // Find what the secret token is and cache it for the current tab.
                this.setup.recacheSecretToken();
            }
        }
    }
}
