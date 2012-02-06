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

import org.openqa.selenium.firefox.FirefoxDriver;
import org.xwiki.test.integration.XWikiExecutor;

/**
 * This is a container for holding all of the information which should persist throughout all of the tests.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class PersistentTestContext
{
    /** This starts and stops the wiki engine. */
    private final XWikiExecutor executor;

    private final XWikiWrappingDriver driver;

    private String currentTestName;

    /** Utility methods which should be available to tests and to pages. */
    private final TestUtils util = new TestUtils();

    public PersistentTestContext() throws Exception
    {
        this.executor = new XWikiExecutor(0);
        this.executor.start();

        // Use a wrapping driver to display more information when there are failures.
        // Note: If you wish to make Selenium use your default Firefox profile (for example to use your installed
        // extensions such as Firebug), simply uncomment the following line:
        // System.setProperty("webdriver.firefox.profile", "default");
        this.driver = new XWikiWrappingDriver(new FirefoxDriver(), getUtil());

        // Wait when trying to find elements on the page till the timeout expires
        getUtil().setDriverImplicitWait(this.driver);
    }

    public PersistentTestContext(XWikiExecutor executor) throws Exception
    {
        this.executor = executor;

        // Use a wrapping driver to display more information when there are failures.
        this.driver = new XWikiWrappingDriver(new FirefoxDriver(), getUtil());

        // Wait when trying to find elements on the page till the timeout expires
        getUtil().setDriverImplicitWait(this.driver);
    }

    public PersistentTestContext(PersistentTestContext toClone)
    {
        this.executor = toClone.executor;
        this.driver = toClone.driver;
    }

    public void setCurrentTestName(String currentTestName)
    {
        this.currentTestName = currentTestName;
    }

    public String getCurrentTestName()
    {
        return this.currentTestName;
    }

    public XWikiWrappingDriver getDriver()
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
        driver.close();
        executor.stop();
    }

    /**
     * Get a clone of this context which cannot be stopped by calling shutdown. this is needed so that individual tests
     * don't shutdown when AllTests ware being run.
     */
    public PersistentTestContext getUnstoppable()
    {
        return new PersistentTestContext(this)
        {
            public void shutdown()
            {
                // Do nothing, that's why it's unstoppable.
            }
        };
    }
}
