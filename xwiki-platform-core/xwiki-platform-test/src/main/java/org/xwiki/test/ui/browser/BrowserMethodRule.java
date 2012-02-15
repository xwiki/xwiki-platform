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

import java.util.Arrays;
import java.util.List;

import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
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

    public BrowserMethodRule(WebDriver driver)
    {
        WebDriver nativeDriver = driver;
        if (driver instanceof XWikiWrappingDriver) {
            nativeDriver = ((XWikiWrappingDriver) driver).getWrappedDriver();
        }
        this.currentBrowserName = ((RemoteWebDriver) nativeDriver).getCapabilities().getBrowserName();
    }

    @Override
    public Statement apply(final Statement statement, final FrameworkMethod method, Object target)
    {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable
            {
                IgnoreBrowser ignoreBrowser = method.getAnnotation(IgnoreBrowser.class);
                if (ignoreBrowser != null) {
                    List<String> ignoreBrowserList = Arrays.asList(ignoreBrowser.value());
                    if (ignoreBrowserList.contains(currentBrowserName)) {
                        // We don't run the test since the current Browser is in the list of browsers to ignore.
                        // Returning an AssumptionViolatedException makes the test runner report the test as ignored!
                        throw new AssumptionViolatedException(ignoreBrowser.reason());
                    } else {
                        statement.evaluate();
                    }
                } else {
                    statement.evaluate();
                }
            }
        };
    }
}
