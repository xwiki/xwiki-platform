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
package org.xwiki.test.cluster;

import java.util.List;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.extension.test.ExtensionTestUtils;
import org.xwiki.extension.test.RepositoryUtils;
import org.xwiki.test.integration.XWikiExecutor;
import org.xwiki.test.ui.PageObjectSuite;
import org.xwiki.test.ui.PersistentTestContext;

/**
 * Runs all functional tests found in the classpath and start/stop XWiki before/after the tests (only once).
 * 
 * @version $Id$
 */
@RunWith(PageObjectSuite.class)
@PageObjectSuite.Executors(2)
public class AllTests
{
    protected static final Logger LOGGER = LoggerFactory.getLogger(AllTests.class);

    private static RepositoryUtils repositoryUtil;

    @PageObjectSuite.PreStart
    public void preStart(List<XWikiExecutor> executors) throws Exception
    {
        setupChannel(executors.get(0), "tcp");
        setupChannel(executors.get(1), "tcp");

        repositoryUtil = new RepositoryUtils();
        repositoryUtil.setup();

        setupRepositories(executors.get(0));
        setupRepositories(executors.get(1));
    }

    private void setupChannel(XWikiExecutor executor, String channelName) throws Exception
    {
        if (executor.getExecutionDirectory() != null) {
            PropertiesConfiguration properties = executor.loadXWikiPropertiesConfiguration();
            properties.setProperty("observation.remote.enabled", "true");
            properties.setProperty("observation.remote.channels", channelName);
            executor.saveXWikiProperties();

            setupExecutor(executor);
        }
    }

    public static void setupExecutor(XWikiExecutor executor)
    {
        // Force bind_addr since tcp jgroups configuration expect cluster members to listen localhost by default
        executor.setXWikiOpts("-Djgroups.bind_addr=localhost -Xmx512m -XX:MaxPermSize=128m");
    }

    private void setupRepositories(XWikiExecutor executor) throws Exception
    {
        LOGGER.info("Adding repository to xwiki.properties");

        if (executor.getExecutionDirectory() != null) {
            PropertiesConfiguration properties = executor.loadXWikiPropertiesConfiguration();

            // Put self and Maven as extensions repository
            properties.setProperty("extension.repositories",
                "maven-test:maven:" + repositoryUtil.getMavenRepository().toURI());

            executor.saveXWikiProperties();
        }
    }

    @PageObjectSuite.PostStart
    public void postStart(PersistentTestContext context) throws Exception
    {
        initExtensionTestUtils(context);
    }

    public static ExtensionTestUtils initExtensionTestUtils(PersistentTestContext context) throws Exception
    {
        // Initialize extensions and repositories
        ExtensionTestUtils extensionTestUtil = new ExtensionTestUtils(context.getUtil());

        // Set integration extension utils.
        context.getProperties().put(ExtensionTestUtils.PROPERTY_KEY, extensionTestUtil);

        return extensionTestUtil;
    }
}
