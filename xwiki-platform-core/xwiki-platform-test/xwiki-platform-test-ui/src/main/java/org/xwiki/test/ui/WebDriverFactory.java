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

import java.io.IOException;

import org.jboss.arquillian.phantom.resolver.ResolvingPhantomJSDriverService;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Create specific {@link WebDriver} instances for various Browsers.
 * 
 * @version $Id$
 * @since 3.5M1
 */
public class WebDriverFactory
{
    public XWikiWebDriver createWebDriver(String browserName)
    {
        WebDriver driver;
        if (browserName.startsWith("*firefox")) {
            // Native events are disabled by default for Firefox on Linux as it may cause tests which open many windows
            // in parallel to be unreliable. However, native events work quite well otherwise and are essential for some
            // of the new actions of the Advanced User Interaction. We need native events to be enable especially for
            // testing the WYSIWYG editor. See http://code.google.com/p/selenium/issues/detail?id=2331 .
            FirefoxProfile profile = new FirefoxProfile();
            profile.setEnableNativeEvents(true);
            // Make sure Firefox doesn't upgrade automatically on CI agents.
            profile.setPreference("app.update.auto", false);
            profile.setPreference("app.update.enabled", false);
            profile.setPreference("app.update.silent", false);
            driver = new FirefoxDriver(profile);

            // Hide the Add-on bar (from the bottom of the window, with "WebDriver" written on the right) because it can
            // prevent buttons or links from being clicked when they are beneath it and native events are used.
            // See https://groups.google.com/forum/#!msg/selenium-users/gBozOynEjs8/XDxxQNmUSCsJ
            // We need to load a page before sending the keys otherwise WebDriver throws ElementNotVisible exception.
            driver.get("data:text/plain;charset=utf-8,XWiki");
            driver.switchTo().activeElement().sendKeys(Keys.chord(Keys.CONTROL, "/"));
        } else if (browserName.startsWith("*iexplore")) {
            driver = new InternetExplorerDriver();
        } else if (browserName.startsWith("*chrome")) {
            driver = new ChromeDriver();
        } else if (browserName.startsWith("*phantomjs")) {
            DesiredCapabilities capabilities = DesiredCapabilities.phantomjs();
            capabilities.setCapability("handlesAlerts", true);
            try {
                driver = new PhantomJSDriver(ResolvingPhantomJSDriverService.createDefaultService(), capabilities);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("Unsupported browser name [" + browserName + "]");
        }

        // Maximize the browser window by default so that the page has a standard layout. Individual tests can resize
        // the browser window if they want to test how the page layout adapts to limited space. This reduces the
        // probability of a test failure caused by an unexpected layout (nested scroll bars, floating menu over links
        // and buttons and so on).
        driver.manage().window().maximize();

        return new XWikiWebDriver((RemoteWebDriver) driver);
    }
}
