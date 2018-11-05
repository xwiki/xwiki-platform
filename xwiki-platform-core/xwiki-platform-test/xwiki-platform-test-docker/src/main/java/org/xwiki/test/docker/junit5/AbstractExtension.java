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
package org.xwiki.test.docker.junit5;

import java.util.Arrays;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.VncRecordingContainer;
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
public abstract class AbstractExtension
{
    private static final ExtensionContext.Namespace NAMESPACE =
        ExtensionContext.Namespace.create(XWikiDockerExtension.class);

    private static ExtensionContext.Store getStore(ExtensionContext context)
    {
        return context.getRoot().getStore(NAMESPACE);
    }

    protected void saveXWikiWebDriver(ExtensionContext context, XWikiWebDriver xwikiWebDriver)
    {
        ExtensionContext.Store store = getStore(context);
        store.put(XWikiWebDriver.class, xwikiWebDriver);
    }

    protected XWikiWebDriver loadXWikiWebDriver(ExtensionContext context)
    {
        ExtensionContext.Store store = getStore(context);
        return store.get(XWikiWebDriver.class, XWikiWebDriver.class);
    }

    protected void saveVNC(ExtensionContext context, VncRecordingContainer vnc)
    {
        ExtensionContext.Store store = getStore(context);
        store.put(VncRecordingContainer.class, vnc);
    }

    protected VncRecordingContainer loadVNC(ExtensionContext context)
    {
        ExtensionContext.Store store = getStore(context);
        return store.get(VncRecordingContainer.class, VncRecordingContainer.class);
    }

    protected void saveBrowserWebDriverContainer(ExtensionContext context, BrowserWebDriverContainer container)
    {
        ExtensionContext.Store store = getStore(context);
        store.put(BrowserWebDriverContainer.class, container);
    }

    protected void saveXWikiURL(ExtensionContext context, String xwikiURL)
    {
        ExtensionContext.Store store = getStore(context);
        store.put(String.class, xwikiURL);
    }

    protected BrowserWebDriverContainer loadBrowserWebDriverContainer(ExtensionContext context)
    {
        ExtensionContext.Store store = getStore(context);
        return store.get(BrowserWebDriverContainer.class, BrowserWebDriverContainer.class);
    }

    protected String loadXWikiURL(ExtensionContext context)
    {
        ExtensionContext.Store store = getStore(context);
        return store.get(String.class, String.class);
    }

    protected void savePersistentTestContext(ExtensionContext context, PersistentTestContext testContext)
    {
        ExtensionContext.Store store = getStore(context);
        store.put(PersistentTestContext.class, testContext);
    }

    protected PersistentTestContext loadPersistentTestContext(ExtensionContext context)
    {
        ExtensionContext.Store store = getStore(context);
        return store.get(PersistentTestContext.class, PersistentTestContext.class);
    }

    protected void saveTestConfiguration(ExtensionContext context, TestConfiguration configuration)
    {
        ExtensionContext.Store store = getStore(context);
        store.put(TestConfiguration.class, configuration);
    }

    protected TestConfiguration loadTestConfiguration(ExtensionContext context)
    {
        ExtensionContext.Store store = getStore(context);
        return store.get(TestConfiguration.class, TestConfiguration.class);
    }

    protected void saveServletContainerExecutor(ExtensionContext context, ServletContainerExecutor executor)
    {
        ExtensionContext.Store store = getStore(context);
        store.put(ServletContainerExecutor.class, executor);
    }

    protected ServletContainerExecutor loadServletContainerExecutor(ExtensionContext context)
    {
        ExtensionContext.Store store = getStore(context);
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

}
