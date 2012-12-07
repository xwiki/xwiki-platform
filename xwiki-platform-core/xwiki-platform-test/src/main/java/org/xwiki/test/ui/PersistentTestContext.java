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

import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.WebDriver;
import org.xwiki.test.integration.XWikiExecutor;

/**
 * This is a container for holding all of the information which should persist throughout all of the tests.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class PersistentTestContext
{
    /**
     * Decide on which browser to run the tests and defaults to Firefox if no system property is defined (useful for
     * running in your IDE for example).
     */
    private static final String BROWSER_NAME_SYSTEM_PROPERTY = System.getProperty("browser", "*firefox");

    /** This starts and stops the wiki engine. */
    private final XWikiExecutor executor;

    private final WebDriver driver;

    /** Utility methods which should be available to tests and to pages. */
    private final TestUtils util = new TestUtils();

    private WebDriverFactory webDriverFactory = new WebDriverFactory();

    private final Map<String, Object> properties = new HashMap<String, Object>();

    /**
     * Starts an XWiki instance if not already started.
     */
    public PersistentTestContext() throws Exception
    {
        this(new XWikiExecutor(0));
        this.executor.start();
    }

    /**
     * Don't start an XWiki instance, instead use an existing started instance.
     */
    public PersistentTestContext(XWikiExecutor executor) throws Exception
    {
        this.executor = executor;

        // Note: If you wish to make Selenium use your default Firefox profile (for example to use your installed
        // extensions such as Firebug), simply uncomment the following line:
        // System.setProperty("webdriver.firefox.profile", "default");
        this.driver = this.webDriverFactory.createWebDriver(BROWSER_NAME_SYSTEM_PROPERTY);

        // Wait when trying to find elements on the page till the timeout expires
        getUtil().setDriverImplicitWait(this.driver);
    }

    public PersistentTestContext(PersistentTestContext toClone)
    {
        this.executor = toClone.executor;
        this.driver = toClone.driver;
        this.properties.putAll(toClone.properties);
    }

    public WebDriver getDriver()
    {
        return this.driver;
    }

    /**
     * @return Utility class with functions not specific to any test or element.
     */
    public TestUtils getUtil()
    {
        return this.util;
    }

    public void shutdown() throws Exception
    {
        this.driver.quit();
        this.executor.stop();
    }

    public Map<String, Object> getProperties()
    {
        return this.properties;
    }

    /**
     * Get a clone of this context which cannot be stopped by calling shutdown. this is needed so that individual tests
     * don't shutdown when AllTests ware being run.
     */
    public PersistentTestContext getUnstoppable()
    {
        return new PersistentTestContext(this)
        {
            @Override
            public void shutdown()
            {
                // Do nothing, that's why it's unstoppable.
            }
        };
    }
}
