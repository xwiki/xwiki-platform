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
import java.util.Iterator;
import java.util.List;

import org.junit.internal.AssumptionViolatedException;
import org.junit.internal.builders.IgnoredBuilder;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.xwiki.test.ui.XWikiWrappingDriver;

/**
 * Allows ignoring some tests for a given browser.
 *
 * <pre>
 * public class MyTestClass
 * {
 *  &#064;Rule
 *  public BrowserMethodRule browseMethodRule = new BrowserMethodRule(getDriver());
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
public class BrowserMethodRule implements MethodRule
{
    private String currentBrowserName;
    private String currentBrowserVersion;

    public BrowserMethodRule(WebDriver driver)
    {
        WebDriver nativeDriver = driver;
        if (driver instanceof XWikiWrappingDriver) {
            nativeDriver = ((XWikiWrappingDriver) driver).getWrappedDriver();
        }

        Capabilities capability = ((RemoteWebDriver) nativeDriver).getCapabilities();
        // We get the name of the current user Browser
        this.currentBrowserName = capability.getBrowserName();
        // We get the version of the current used Browser
        this.currentBrowserVersion = capability.getVersion();

    }

    @Override
    public Statement apply(final Statement statement, final FrameworkMethod method, Object target)
    {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable
            {
                // The full List with ignored browsers, taken from both annotations
                List<IgnoreBrowser> ignoreBrowsersList = new ArrayList<IgnoreBrowser>();

                // We check if there is a IgnoreBrowser annotation
                IgnoreBrowser ignoreBrowser = method.getAnnotation(IgnoreBrowser.class);
                if (ignoreBrowser != null) {
                    ignoreBrowsersList.add(ignoreBrowser);
                }
                // We check if t here is a IgnoreBrowsers annotation compound
                IgnoreBrowsers ignoreBrowsers = method.getAnnotation(IgnoreBrowsers.class);
                if (ignoreBrowsers != null) {
                    ignoreBrowsersList.addAll(Arrays.asList(ignoreBrowsers.value()));
                }

                // We iterate through the array of annotations
                for (IgnoreBrowser ignoredBrowser : ignoreBrowsersList) {
                    if (ignoredBrowser.value().equals(currentBrowserName)
                        && (ignoredBrowser.version().isEmpty() || ignoredBrowser.version()
                            .equals(currentBrowserVersion))) {
                        throw new AssumptionViolatedException(ignoredBrowser.reason());
                    }
                }
                statement.evaluate();
            }
        };
    }
}
