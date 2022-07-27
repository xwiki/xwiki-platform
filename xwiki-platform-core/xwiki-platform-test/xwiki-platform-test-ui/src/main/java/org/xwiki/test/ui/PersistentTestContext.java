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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private List<XWikiExecutor> executors;

    private XWikiWebDriver driver;

    /** Utility methods which should be available to tests and to pages. */
    private TestUtils util;

    private Map<String, Object> properties = new HashMap<>();

    /**
     * Starts an XWiki instance if not already started.
     */
    public PersistentTestContext() throws Exception
    {
        this(Arrays.asList(new XWikiExecutor(0)));
    }

    /**
     * Don't start an XWiki instance, instead use an existing started instance.
     */
    public PersistentTestContext(List<XWikiExecutor> executors)
    {
        initialize(executors);
        // Note: If you wish to make Selenium use your default Firefox profile (for example to use your installed
        // extensions such as Firebug), simply uncomment the following line:
        // System.setProperty("webdriver.firefox.profile", "default");
        WebDriverFactory webDriverFactory = new WebDriverFactory();
        setDriver(webDriverFactory.createWebDriver(BROWSER_NAME_SYSTEM_PROPERTY));
    }

    public PersistentTestContext(List<XWikiExecutor> executors, XWikiWebDriver driver)
    {
        initialize(executors);
        setDriver(driver);
    }

    private void initialize(List<XWikiExecutor> executors)
    {
        this.executors = executors;

        this.util = new TestUtils();
        this.util.setExecutors(executors);
    }

    public PersistentTestContext(PersistentTestContext toClone)
    {
        this.executors = toClone.executors;
        this.util = toClone.util;
        setDriver(toClone.driver);
        this.properties.putAll(toClone.properties);
    }

    public XWikiWebDriver getDriver()
    {
        return this.driver;
    }

    public void setDriver(XWikiWebDriver driver)
    {
        this.driver = driver;
    }

    public List<XWikiExecutor> getExecutors()
    {
        return executors;
    }

    /**
     * @return Utility class with functions not specific to any test or element.
     */
    public TestUtils getUtil()
    {
        return this.util;
    }

    public void start() throws Exception
    {
        for (XWikiExecutor executor : this.executors) {
            executor.start();
        }
    }

    public void shutdown() throws Exception
    {
        if (getDriver() != null) {
            getDriver().quit();
        }
        for (XWikiExecutor executor : this.executors) {
            executor.stop();
        }
    }

    public Map<String, Object> getProperties()
    {
        return this.properties;
    }

    /**
     * Get a clone of this context which cannot be stopped by calling shutdown. this is needed so that individual tests
     * don't shutdown when AllTests are being run.
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
