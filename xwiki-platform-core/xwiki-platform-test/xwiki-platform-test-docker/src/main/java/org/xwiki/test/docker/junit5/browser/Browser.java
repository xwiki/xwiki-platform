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
package org.xwiki.test.docker.junit5.browser;

import java.util.logging.Level;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.UnexpectedAlertBehaviour;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

/**
 * The browser to use for the UI tests.
 *
 * @version $Id$
 * @since 10.6RC1
 */
public enum Browser
{
    /**
     * The Firefox Browser.
     */
    FIREFOX(new FirefoxOptions()),

    /**
     * The Chrome Browser.
     */
    CHROME(new ChromeOptions());

    /**
     * The path where to store the test-resources on the browser container.
     */
    private static final String TEST_RESOURCES_PATH = "/tmp/test-resources";

    private Capabilities capabilities;

    Browser()
    {
        // Capability will be resolved at runtime
    }

    Browser(Capabilities capabilities)
    {
        this.capabilities = capabilities;
        this.forceDefaultCapabilities();
    }

    /**
     * Ensure that some capabilities are set as expected for our tests.
     */
    private void forceDefaultCapabilities()
    {
        DesiredCapabilities desiredCapabilities = new DesiredCapabilities();
        LoggingPreferences logPrefs = new LoggingPreferences();
        logPrefs.enable(LogType.BROWSER, Level.ALL);
        // By default we want to be able to handle alerts.
        desiredCapabilities.setCapability(CapabilityType.SUPPORTS_ALERTS, true);
        desiredCapabilities.setCapability(CapabilityType.UNEXPECTED_ALERT_BEHAVIOUR, UnexpectedAlertBehaviour.IGNORE);
        desiredCapabilities.setCapability(CapabilityType.UNHANDLED_PROMPT_BEHAVIOUR, UnexpectedAlertBehaviour.IGNORE);
        desiredCapabilities.setCapability(CapabilityType.LOGGING_PREFS, logPrefs);
        this.capabilities.merge(desiredCapabilities);

        if (this.capabilities instanceof FirefoxOptions) {
            FirefoxOptions firefoxOptions = (FirefoxOptions) this.capabilities;
            // Create the profile on the fly, mostly for test.
            if (firefoxOptions.getProfile() == null) {
                firefoxOptions.setProfile(new FirefoxProfile());
            }
            // We want to ensure that those events are taking into account.
            firefoxOptions.getProfile().setPreference("dom.disable_beforeunload", false);
        } else if (this.capabilities instanceof ChromeOptions) {
            ChromeOptions chromeOptions = (ChromeOptions) this.capabilities;
            chromeOptions.addArguments(
                "--whitelisted-ips",
                "--no-sandbox",
                "--disable-extensions"
            );
        }
    }

    /**
     * @return the Selenium capability object for the selected browser.
     */
    public Capabilities getCapabilities()
    {
        return this.capabilities;
    }

    /**
     * @return the path of the directory containing the test resources on the browser container.
     * @since 10.11RC1
     */
    public String getTestResourcesPath()
    {
        return TEST_RESOURCES_PATH;
    }
}
