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
package org.xwiki.wiki.test.ui;

import java.io.File;
import java.util.List;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.test.RepositoryUtils;
import org.xwiki.test.integration.XWikiExecutor;
import org.xwiki.test.integration.XWikiExecutorSuite;
import org.xwiki.test.ui.PageObjectSuite;
import org.xwiki.test.ui.PersistentTestContext;

/**
 * Runs all functional tests found in the classpath.
 *
 * @version $Id$
 * @since 5.4RC1
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

        LOGGER.info("Adding repository to xwiki.properties");

        PropertiesConfiguration properties = executor.loadXWikiPropertiesConfiguration();

        // Put maven as extensions repository
        properties.setProperty(
                "extension.repositories","maven-test:maven:" + repositoryUtil.getMavenRepository().toURI());
        // Disable core extension resolve because Jetty is not ready when it starts
        properties.setProperty("extension.core.resolve", false);

        executor.saveXWikiProperties(properties);
    }

    @PageObjectSuite.PostStart
    public void postStart(PersistentTestContext context) throws Exception
    {
        repositoryUtil.setup();

        // Populate maven repository
        ExtensionId extensionId = new ExtensionId("fakeextension", "1.0");
        File extensionFile = repositoryUtil.getExtensionPackager().getExtensionFile(extensionId);
        FileUtils.copyFile(extensionFile,
                new File(repositoryUtil.getMavenRepository(), "maven/fakeextension/1.0/fakeextension-1.0.xar"));
    }
}

