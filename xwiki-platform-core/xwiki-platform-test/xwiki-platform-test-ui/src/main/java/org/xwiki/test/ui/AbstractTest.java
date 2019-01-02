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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.xwiki.component.annotation.ComponentAnnotationLoader;
import org.xwiki.component.annotation.ComponentDeclaration;
import org.xwiki.component.embed.EmbeddableComponentManager;
import org.xwiki.model.internal.DefaultModelConfiguration;
import org.xwiki.model.internal.reference.DefaultEntityReferenceProvider;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceResolver;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.DefaultSymbolScheme;
import org.xwiki.model.internal.reference.RelativeStringEntityReferenceResolver;
import org.xwiki.test.integration.XWikiExecutor;
import org.xwiki.test.ui.browser.BrowserTestRule;
import org.xwiki.test.ui.po.BaseElement;

import com.google.code.tempusfugit.concurrency.IntermittentTestRunner;

/**
 * To be extended by all Test Classes. Allows to start/stop the Web Driver and get access to it.
 * 
 * @version $Id$
 * @since 3.2M3
 */
@RunWith(IntermittentTestRunner.class)
public abstract class AbstractTest
{
    /**
     * The object used to access the name of the current test.
     */
    @Rule
    public final TestName testName = new TestName();

    /**
     * Used for ignoring tests that use {@link org.xwiki.test.ui.browser.IgnoreBrowser} and
     * {@link org.xwiki.test.ui.browser.IgnoreBrowsers} annotations.
     */
    @Rule
    public BrowserTestRule browserRule = new BrowserTestRule(getDriver());

    /**
     * Generates debugging information on test failure.
     */
    @Rule
    public TestDebugger testDebugger = new TestDebugger(getDriver());

    protected static PersistentTestContext context;

    protected static EmbeddableComponentManager componentManager;

    /** Used so that AllTests can set the persistent test context. */
    public static void initializeSystem(PersistentTestContext context) throws Exception
    {
        AbstractTest.setContext(context);
        BaseElement.setContext(context);
        TestUtils.setContext(context);
        AbstractTest.componentManager = new EmbeddableComponentManager();

        // Only load the minimal number of components required for the test framework, for both performance reasons
        // and for avoiding having to declare dependencies such as HttpServletRequest.
        ComponentAnnotationLoader loader = new ComponentAnnotationLoader();
        List<ComponentDeclaration> componentDeclarations = new ArrayList<>();
        componentDeclarations.add(new ComponentDeclaration(DefaultStringEntityReferenceResolver.class.getName()));
        componentDeclarations.add(new ComponentDeclaration(DefaultStringEntityReferenceSerializer.class.getName()));
        componentDeclarations.add(new ComponentDeclaration(RelativeStringEntityReferenceResolver.class.getName()));
        componentDeclarations.add(new ComponentDeclaration(DefaultEntityReferenceProvider.class.getName()));
        componentDeclarations.add(new ComponentDeclaration(DefaultModelConfiguration.class.getName()));
        componentDeclarations.add(new ComponentDeclaration(DefaultSymbolScheme.class.getName()));
        loader.initialize(AbstractTest.componentManager, AbstractTest.class.getClassLoader(), componentDeclarations);

        TestUtils.initializeComponent(AbstractTest.componentManager);
    }

    public static void setContext(PersistentTestContext testContext)
    {
        context = testContext;
    }

    public static PersistentTestContext getContext()
    {
        return context;
    }

    @BeforeClass
    public static void init() throws Exception
    {
        // This will not be null if we are in the middle of allTests
        if (context == null) {
            init(Arrays.asList(new XWikiExecutor(0)));
        }
    }

    public static void init(List<XWikiExecutor> executors) throws Exception
    {
        PersistentTestContext persistentTestContext = new PersistentTestContext(executors);
        initializeSystem(persistentTestContext);

        // Start XWiki
        persistentTestContext.start();

        // Cache the initial CSRF token since that token needs to be passed to all forms (this is done automatically
        // in TestUtils), including the login form. Whenever a new user logs in we need to recache.
        // Note that this requires a running XWiki instance.
        getUtil().recacheSecretToken();
    }

    @AfterClass
    public static void shutdown() throws Exception
    {
        // The context can be null if the XWiki Server couldn't start for example.
        if (context != null) {
            context.shutdown();
        }
    }

    @Before
    public void beforeTest()
    {
        // Make sure to start the test on first instance
        getUtil().switchExecutor(0);
    }

    protected String getTestMethodName()
    {
        return this.testName.getMethodName();
    }

    protected String getTestClassName()
    {
        return getClass().getSimpleName();
    }

    protected static XWikiWebDriver getDriver()
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
