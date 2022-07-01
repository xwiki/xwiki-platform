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
package org.xwiki.test.docker.internal.junit5;

import java.util.Arrays;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.VncRecordingContainer;
import org.xwiki.test.docker.internal.junit5.servletengine.ServletContainerExecutor;
import org.xwiki.test.docker.junit5.DockerTestException;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.integration.XWikiExecutor;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.PersistentTestContext;
import org.xwiki.test.ui.XWikiWebDriver;

/**
 * Provides methods to save/store objects from the JUnit5 store.
 *
 * @version $Id$
 * @since 10.10RC1
 */
public abstract class AbstractExtension implements BeforeAllCallback, AfterAllCallback,
    BeforeEachCallback, AfterEachCallback, ParameterResolver, TestExecutionExceptionHandler, ExecutionCondition
{
    protected void saveXWikiWebDriver(ExtensionContext context, XWikiWebDriver xwikiWebDriver)
    {
        ExtensionContext.Store store = DockerTestUtils.getStore(context);
        store.put(XWikiWebDriver.class, xwikiWebDriver);
    }

    protected XWikiWebDriver loadXWikiWebDriver(ExtensionContext context)
    {
        ExtensionContext.Store store = DockerTestUtils.getStore(context);
        return store.get(XWikiWebDriver.class, XWikiWebDriver.class);
    }

    protected void saveVNC(ExtensionContext context, VncRecordingContainer vnc)
    {
        ExtensionContext.Store store = DockerTestUtils.getStore(context);
        store.put(VncRecordingContainer.class, vnc);
    }

    protected VncRecordingContainer loadVNC(ExtensionContext context)
    {
        ExtensionContext.Store store = DockerTestUtils.getStore(context);
        return store.get(VncRecordingContainer.class, VncRecordingContainer.class);
    }

    protected void saveBrowserWebDriverContainer(ExtensionContext context, BrowserWebDriverContainer<?> container)
    {
        ExtensionContext.Store store = DockerTestUtils.getStore(context);
        store.put(BrowserWebDriverContainer.class, container);
    }

    protected void saveXWikiURL(ExtensionContext context, String xwikiURL)
    {
        DockerTestUtils.setXWikiURL(context, xwikiURL);
    }

    protected BrowserWebDriverContainer loadBrowserWebDriverContainer(ExtensionContext context)
    {
        ExtensionContext.Store store = DockerTestUtils.getStore(context);
        return store.get(BrowserWebDriverContainer.class, BrowserWebDriverContainer.class);
    }

    protected String loadXWikiURL(ExtensionContext context)
    {
        return DockerTestUtils.getXWikiURL(context);
    }

    protected void savePersistentTestContext(ExtensionContext context, PersistentTestContext testContext)
    {
        ExtensionContext.Store store = DockerTestUtils.getStore(context);
        store.put(PersistentTestContext.class, testContext);
    }

    protected PersistentTestContext loadPersistentTestContext(ExtensionContext context)
    {
        ExtensionContext.Store store = DockerTestUtils.getStore(context);
        return store.get(PersistentTestContext.class, PersistentTestContext.class);
    }

    protected void saveTestConfiguration(ExtensionContext context, TestConfiguration configuration)
    {
        DockerTestUtils.setTestConfiguration(context, configuration);
    }

    protected TestConfiguration loadTestConfiguration(ExtensionContext context)
    {
        return DockerTestUtils.getTestConfiguration(context);
    }

    protected void saveServletContainerExecutor(ExtensionContext context, ServletContainerExecutor executor)
    {
        ExtensionContext.Store store = DockerTestUtils.getStore(context);
        store.put(ServletContainerExecutor.class, executor);
    }

    protected ServletContainerExecutor loadServletContainerExecutor(ExtensionContext context)
    {
        ExtensionContext.Store store = DockerTestUtils.getStore(context);
        return store.get(ServletContainerExecutor.class, ServletContainerExecutor.class);
    }

    protected PersistentTestContext initializePersistentTestContext(XWikiWebDriver driver)
    {
        PersistentTestContext testContext;

        try {
            PersistentTestContext initialTestContext =
                new PersistentTestContext(Arrays.asList(new XWikiExecutor(0)), driver);
            testContext = initialTestContext.getUnstoppable();
            AbstractTest.initializeSystem(testContext);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize PersistentTestContext", e);
        }

        return testContext;
    }

    protected void shutdownPersistentTestContext(PersistentTestContext testContext)
    {
        if (testContext != null) {
            try {
                testContext.shutdown();
            } catch (Exception e) {
                throw new RuntimeException("Failed to shutdown PersistentTestContext", e);
            }
        }
    }

    protected void mergeTestConfigurationInGlobalContext(TestConfiguration testConfiguration, ExtensionContext context)
    {
        // Allow extensions to contribute a dynamically-generated TestConfiguration by storing it in the GLOBAL
        // test context. This allows test writers to provide dynamically-generated configuration before XWiki is
        // started (e.g. to set the URL and port for an ElasticSearch instance for active installs).
        ExtensionContext.Store globalStore = context.getStore(ExtensionContext.Namespace.GLOBAL);
        if (globalStore.get(TestConfiguration.class) != null) {
            try {
                testConfiguration.merge((TestConfiguration) globalStore.get(TestConfiguration.class));
            } catch (DockerTestException e) {
                throw new RuntimeException("Failed to merge Test Configuration from the global test context store", e);
            }
        }
    }
}
