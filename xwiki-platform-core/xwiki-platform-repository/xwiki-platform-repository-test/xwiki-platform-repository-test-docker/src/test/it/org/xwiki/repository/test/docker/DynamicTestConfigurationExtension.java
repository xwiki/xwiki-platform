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
package org.xwiki.repository.test.docker;

import java.io.File;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.test.RepositoryUtils;
import org.xwiki.test.TestEnvironment;
import org.xwiki.test.docker.junit5.TestConfiguration;

/**
 * Inject a dynamically-generated {@link TestConfiguration} so that the {@code maven-test} extension repository (whose
 * location is a randomly-generated temporary directory) is configured in {@code xwiki.properties} <em>before</em> XWiki
 * is started.
 * <p>
 * The generated {@link RepositoryUtils} is shared with {@link RepositoryIT} (through {@link #getRepositoryUtils()}) so
 * that both the configured maven repository path and the generated test extension files match (required for the file
 * size assertions performed by the test).
 *
 * @version $Id$
 * @since 18.5.0RC1
 */
public class DynamicTestConfigurationExtension implements BeforeAllCallback
{
    private static RepositoryUtils repositoryUtils;

    /**
     * @return the {@link RepositoryUtils} initialized before XWiki started, holding the maven test repository and the
     *         generated test extension files
     */
    public static RepositoryUtils getRepositoryUtils()
    {
        return repositoryUtils;
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception
    {
        // Generate the test extensions and copy the maven repository resources into a (randomly located) temporary
        // directory.
        repositoryUtils = new RepositoryUtils();
        repositoryUtils.setup(new TestEnvironment());

        // Populate the maven repository with the empty extension file, exactly as the legacy AllIT.initExtensions did.
        File mavenRepository = repositoryUtils.getMavenRepository();
        File extensionFile = repositoryUtils.getExtensionPackager().getExtensionFile(new ExtensionId("emptyjar", "1.0"));
        FileUtils.copyFile(extensionFile, new File(mavenRepository, "maven/extension/1.0/extension-1.0.jar"));
        FileUtils.copyFile(extensionFile, new File(mavenRepository, "maven/extension/2.0/extension-2.0.jar"));
        FileUtils.copyFile(extensionFile, new File(mavenRepository, "maven/oldextension/0.9/oldextension-0.9.jar"));
        FileUtils.copyFile(extensionFile,
            new File(mavenRepository, "maven/oldextension/10.0/oldversionnedextension-10.0.jar"));
        FileUtils.copyFile(extensionFile, new File(mavenRepository, "maven/dependency/version/dependency-version.jar"));

        // Save a TestConfiguration in the global test context so that it's merged in XWikiDockerExtension. We:
        // - configure the maven test repository (its location is only known at runtime)
        // - disable core extension resolve because Jetty is not ready when it starts
        ExtensionContext.Store globalStore = extensionContext.getRoot().getStore(ExtensionContext.Namespace.GLOBAL);
        TestConfiguration configuration = new TestConfiguration();
        Properties properties = new Properties();
        properties.setProperty("xwikiPropertiesAdditionalProperties",
            String.format("extension.repositories = maven-test:maven:%s%nextension.core.resolve = false",
                mavenRepository.toURI()));
        configuration.setProperties(properties);
        globalStore.put(TestConfiguration.class, configuration);
    }
}
