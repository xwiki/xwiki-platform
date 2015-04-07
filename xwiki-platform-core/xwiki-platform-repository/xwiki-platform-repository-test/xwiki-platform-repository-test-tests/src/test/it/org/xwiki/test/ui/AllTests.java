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

import java.io.File;
import java.util.List;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.test.RepositoryUtils;
import org.xwiki.repository.test.RepositoryTestUtils;
import org.xwiki.repository.test.SolrTestUtils;
import org.xwiki.test.integration.XWikiExecutor;
import org.xwiki.test.integration.XWikiExecutorSuite;

/**
 * Runs all functional tests found in the classpath.
 * 
 * @version $Id$
 */
@RunWith(PageObjectSuite.class)
public class AllTests
{
    protected static final Logger LOGGER = LoggerFactory.getLogger(AllTests.class);

    private static RepositoryUtils repositoryUtil;

    @XWikiExecutorSuite.PreStart
    public void preStart(List<XWikiExecutor> executors) throws Exception
    {
        XWikiExecutor executor = executors.get(0);

        repositoryUtil = new RepositoryUtils();

        LOGGER.info("Adding maven repository to xwiki.properties");

        PropertiesConfiguration properties = executor.loadXWikiPropertiesConfiguration();
        // Put self as extensions repository
        properties.setProperty("extension.repositories", "maven-test:maven:"
            + repositoryUtil.getMavenRepository().toURI());
        executor.saveXWikiProperties(properties);
    }

    @PageObjectSuite.PostStart
    public void postStart(PersistentTestContext context) throws Exception
    {
        // Import XR
        if (!context.getUtil().pageExists("Extension", "WebHome")) {
            System.out.println("Importing xwiki-platform-repository-server-ui.xar...");

            context.getDriver().get(
                context.getUtil().getURLToLoginAsAdminAndGotoPage(context.getUtil().getURLToNonExistentPage()));
            context.getUtil().recacheSecretToken();
            context.getUtil().setDefaultCredentials(TestUtils.ADMIN_CREDENTIALS);
            context.getUtil().importXar(new File("target/dependency/xwiki-platform-repository-server-ui.xar"));
        }

        // Import ratings
        if (!context.getUtil().pageExists("XWiki", "Ratings")) {
            System.out.println("Importing xwiki-platform-ratings-ui.xar...");

            context.getDriver().get(
                context.getUtil().getURLToLoginAsAdminAndGotoPage(context.getUtil().getURLToNonExistentPage()));
            context.getUtil().recacheSecretToken();
            context.getUtil().importXar(new File("target/dependency/xwiki-platform-ratings-ui.xar"));
        }

        // Initialize extensions and repositories
        initExtensions(context);
    }

    public static void initExtensions(PersistentTestContext context) throws Exception
    {
        // Initialize extensions and repositories
        RepositoryTestUtils repositoryTestUtil =
            new RepositoryTestUtils(context.getUtil(), repositoryUtil, new SolrTestUtils(context.getUtil()));
        repositoryTestUtil.init();

        // Set integration repository and extension utils.
        context.getProperties().put(RepositoryTestUtils.PROPERTY_KEY, repositoryTestUtil);

        // Populate maven repository
        File extensionFile =
            repositoryTestUtil.getTestExtension(new ExtensionId("emptyjar", "1.0"), "jar").getFile().getFile();
        FileUtils.copyFile(extensionFile, new File(repositoryTestUtil.getRepositoryUtil().getMavenRepository(),
            "maven/extension/1.0/extension-1.0.jar"));
        FileUtils.copyFile(extensionFile, new File(repositoryTestUtil.getRepositoryUtil().getMavenRepository(),
            "maven/extension/2.0/extension-2.0.jar"));
        FileUtils.copyFile(extensionFile, new File(repositoryTestUtil.getRepositoryUtil().getMavenRepository(),
            "maven/oldextension/0.9/oldextension-0.9.jar"));
        FileUtils.copyFile(extensionFile, new File(repositoryTestUtil.getRepositoryUtil().getMavenRepository(),
            "maven/dependency/version/dependency-version.jar"));
    }
}
