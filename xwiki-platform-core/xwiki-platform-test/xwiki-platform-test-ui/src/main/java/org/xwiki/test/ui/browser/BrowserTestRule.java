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
package org.xwiki.test.ui.browser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Allows ignoring some tests for a given browser.
 *
 * <pre>
 * public class MyTestClass
 * {
 *  &#064;Rule
 *  public BrowserTestRule browseTestRule = new BrowserTestRule(getDriver());
 *
 * 	&#064;Test
 *  &#064;IgnoreBrowser(value = {"firefox"}, reason="some reason for ignoring the test...")
 * 	public void myTest()
 * 	{
 * 	...
 * 	}
 * }
 * </pre>
 *
 * @version $Id$
 * @since 3.5M1
 */
public class BrowserTestRule implements TestRule
{
    private String currentBrowserName;
    private String currentBrowserVersion;

    public BrowserTestRule(WebDriver driver)
    {
        Capabilities capability = ((RemoteWebDriver) driver).getCapabilities();
        // We get the name of the current user Browser
        this.currentBrowserName = capability.getBrowserName();
        // We get the version of the current used Browser
        this.currentBrowserVersion = capability.getBrowserVersion();
    }

    @Override
    public Statement apply(final Statement base, final Description description)
    {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable
            {
                // The full List with ignored browsers, taken from both annotations
                List<IgnoreBrowser> ignoredBrowsersList = new ArrayList<IgnoreBrowser>();

                // We check if there is a IgnoreBrowser annotation
                IgnoreBrowser ignoreBrowser = description.getAnnotation(IgnoreBrowser.class);
                if (ignoreBrowser != null) {
                    ignoredBrowsersList.add(ignoreBrowser);
                }

                // We check if there is a IgnoreBrowsers annotation compound
                IgnoreBrowsers ignoreBrowsers = description.getAnnotation(IgnoreBrowsers.class);
                if (ignoreBrowsers != null) {
                    ignoredBrowsersList.addAll(Arrays.asList(ignoreBrowsers.value()));
                }

                // We iterate through the list of BrowserIgnore annotations
                for (IgnoreBrowser ignoredBrowser : ignoredBrowsersList) {
                    Pattern browserNamePattern = Pattern.compile(ignoredBrowser.value());
                    Pattern browserVersionPattern = Pattern.compile(ignoredBrowser.version());

                    if (browserNamePattern.matcher(currentBrowserName).matches()
                        && (ignoredBrowser.version().isEmpty()
                            || browserVersionPattern.matcher(currentBrowserVersion).matches()))
                    {
                        throw new AssumptionViolatedException(ignoredBrowser.reason());
                    }
                }
                base.evaluate();
            }
        };
    }
}
