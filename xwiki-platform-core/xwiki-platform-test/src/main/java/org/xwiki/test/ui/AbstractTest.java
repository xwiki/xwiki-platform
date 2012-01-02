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

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.MethodRule;
import org.junit.rules.TestName;
import org.junit.rules.TestWatchman;
import org.junit.runner.RunWith;
import org.junit.runners.model.FrameworkMethod;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.test.ui.po.BaseElement;

import com.google.code.tempusfugit.concurrency.IntermittentTestRunner;

/**
 * To be extended by all Test Classes. Allows to start/stop the Web Driver and get access to it.
 * 
 * @version $Id$
 * @since 3.2M3
 */
@RunWith(IntermittentTestRunner.class)
public class AbstractTest
{
    /**
     * The object used to access the name of the current test.
     */
    @Rule
    public final TestName testName = new TestName();

    /**
     * The object used to watch tests and log when they start and succeed/fail.
     * <p>
     * The reason we need this is simply to overcome a deficiency in error reporting in Jenkins. The reason is that
     * Jenkins bases its test reporting on the Maven Surefire plugin reporting which itself is using a file to report
     * test status. Since ui-tests are using a test suite, {@link PageObjectSuite}, there's only a single file generated and
     * it's only generated when all tests have finished executing. Thus if a test hangs there won't be any file
     * generated and looking at the Jenkins UI it won't be possible to see which tests have executed.
     * <p>
     * Normally each JUnit Test Runner knows what test is executing and when it's finished and thus can report them in
     * its own console (as this is the case for IDEs for example). Again the issue here is that Jenkins doesn't have any
     * JUnit Test Runner but instead is calling JUnit by delegation to the Maven Surefire plugin.
     */
    @Rule
    public final MethodRule watchman = new TestWatchman()
    {
        @Override
        public void starting(FrameworkMethod method)
        {
            logger.info("{} started", method.getName());
        }

        @Override
        public void succeeded(FrameworkMethod method)
        {
            logger.info("{} succeeded", method.getName());
        }

        @Override
        public void failed(Throwable e, FrameworkMethod method)
        {
            logger.info("{} failed", method.getName());
        }
    };

    protected static PersistentTestContext context;

    /** The object used to log an info message when the test starts and ends. */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** Used so that AllTests can set the persistent test context. */
    public static void setContext(PersistentTestContext context)
    {
        AbstractTest.context = context;
        BaseElement.setContext(context);
        TestUtils.setContext(context);

        // Cache the initial CSRF token since that token needs to be passed to all forms (this is done automatically
        // in TestUtils), including the login form. Whenever a new user logs in we need to recache
        getUtil().recacheSecretToken();
    }

    @Before
    public void setTestName()
    {
        context.setCurrentTestName(getClass().getSimpleName() + "-" + getTestMethodName());
    }

    @BeforeClass
    public static void init() throws Exception
    {
        // This will not be null if we are in the middle of allTests
        if (context == null) {
            setContext(new PersistentTestContext());
        }
    }

    @AfterClass
    public static void shutdown() throws Exception
    {
        context.shutdown();
    }

    protected String getTestMethodName()
    {
        return this.testName.getMethodName();
    }

    protected String getTestClassName()
    {
        return getClass().getSimpleName();
    }

    protected static WebDriver getDriver()
    {
        return context.getDriver();
    }

    /**
     * @return Utility class with functions not specific to any test or element.
     */
    protected static TestUtils getUtil()
    {
        return context.getUtil();
    }
}
