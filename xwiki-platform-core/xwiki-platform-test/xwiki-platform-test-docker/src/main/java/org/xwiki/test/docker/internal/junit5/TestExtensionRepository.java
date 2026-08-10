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

import java.io.File;

import org.xwiki.extension.test.RepositoryUtils;
import org.xwiki.test.TestEnvironment;
import org.xwiki.test.docker.junit5.TestConfiguration;

/**
 * Generate the Maven repository containing the extensions declared in the resources of the module executing the test,
 * so that the test can ask XWiki to install them.
 * <p>
 * The extensions are the ones declared in the {@code packagefile} resources and the repository is initialized with the
 * content of the {@code repository/maven} resources (see {@link RepositoryUtils}).
 *
 * @version $Id$
 * @since 18.7.0RC1
 */
public final class TestExtensionRepository
{
    /**
     * The identifier of the repository, as configured in {@code xwiki.properties}.
     */
    public static final String ID = "maven-test";

    /**
     * The location of the repository inside the Servlet engine container, for the tests running XWiki in Docker (the
     * repository is copied there since XWiki cannot access the file system of the host).
     */
    public static final String CONTAINER_DIRECTORY = "/root/xwiki-test-extension-repository";

    private static final String OUTPUT_DIRECTORY = "test-extension-repository";

    private TestExtensionRepository()
    {
        // Utility class
    }

    /**
     * Generate the repository on the file system of the host.
     *
     * @param testConfiguration the configuration of the test, used to locate the output directory
     * @throws Exception if the extensions cannot be generated
     */
    public static void generate(TestConfiguration testConfiguration) throws Exception
    {
        File rootDirectory = getRootDirectory(testConfiguration);

        RepositoryUtils repositoryUtils = new RepositoryUtils();
        repositoryUtils.setup(new TestEnvironment()
        {
            @Override
            public File getPermanentDirectory()
            {
                // Generate the repository in the output directory of the test instead of a temporary directory, so
                // that it can be inspected when a test fails and so that "mvn clean" removes it.
                return rootDirectory;
            }
        });
    }

    /**
     * @param testConfiguration the configuration of the test, used to locate the output directory
     * @return the directory of the repository on the file system of the host
     */
    public static File getDirectory(TestConfiguration testConfiguration)
    {
        // Note: this is the location in which RepositoryUtils generates the Maven repository, relatively to the
        // permanent directory it's given.
        return new File(getRootDirectory(testConfiguration), "repositories/maven");
    }

    /**
     * @param testConfiguration the configuration of the test, used to locate the output directory and to know if XWiki
     *     runs inside a container
     * @return the URL of the repository, as seen by the XWiki instances
     */
    public static String getURL(TestConfiguration testConfiguration)
    {
        String path = testConfiguration.getServletEngine().isOutsideDocker()
            ? getDirectory(testConfiguration).getAbsolutePath() : CONTAINER_DIRECTORY;

        return String.format("file://%s", path);
    }

    private static File getRootDirectory(TestConfiguration testConfiguration)
    {
        return new File(testConfiguration.getOutputDirectory(), OUTPUT_DIRECTORY).getAbsoluteFile();
    }
}
