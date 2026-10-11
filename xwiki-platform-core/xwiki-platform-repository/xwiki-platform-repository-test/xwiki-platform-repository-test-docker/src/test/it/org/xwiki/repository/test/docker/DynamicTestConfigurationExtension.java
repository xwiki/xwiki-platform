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
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.nginx.NginxContainer;
import org.testcontainers.utility.MountableFile;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.test.RepositoryUtils;
import org.xwiki.test.TestEnvironment;
import org.xwiki.test.docker.internal.junit5.UITestTestConfigurationResolver;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.UITest;

/**
 * Serve the {@code maven-test} extension repository over HTTP from an nginx container and inject a
 * dynamically-generated {@link TestConfiguration} so that this repository is configured in {@code xwiki.properties}
 * <em>before</em> XWiki is started.
 * <p>
 * The maven repository is generated in a (randomly located) temporary directory on the host. XWiki itself can run
 * inside a Docker container and therefore can't access that directory through a {@code file://} URL (this is especially
 * true in the DOOD - Docker out of Docker - setup used on the CI). Instead, the repository is copied into an nginx
 * container (copy, not volume mount, so that it also works with DOOD) which serves it over HTTP on the shared network.
 * <p>
 * The generated {@link RepositoryUtils} is shared with {@link RepositoryIT} (through {@link #getRepositoryUtils()}) so
 * that both the configured maven repository content and the generated test extension files match (required for the file
 * size assertions performed by the test).
 *
 * @version $Id$
 * @since 18.5.0RC1
 */
public class DynamicTestConfigurationExtension implements BeforeAllCallback
{
    /**
     * The network alias under which the nginx container serving the maven repository is reachable from the XWiki
     * container (both are connected to the shared network).
     */
    private static final String REPOSITORY_HOST = "maven-test-repository";

    private static final String NGINX_IMAGE = "nginx:1.27-alpine";

    /**
     * The document root served by the stock nginx image. We copy the maven repository content there so that it's served
     * over HTTP without needing a custom nginx configuration.
     */
    private static final String NGINX_DOCUMENT_ROOT = "/usr/share/nginx/html";

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
        // beforeAll is called both for the top-level test class and for the @Nested test class, so make sure to perform
        // the (expensive) setup only once.
        if (repositoryUtils != null) {
            return;
        }

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

        // Resolve the test configuration (the same way the docker test framework does) so that we know whether XWiki
        // will run inside a container or on the host (it influences how the repository is reachable by XWiki). The
        // @UITest annotation is on the top-level test class, so we resolve from there.
        Class<?> topLevelTestClass = extensionContext.getRequiredTestClass();
        while (topLevelTestClass.getEnclosingClass() != null) {
            topLevelTestClass = topLevelTestClass.getEnclosingClass();
        }
        TestConfiguration resolvedConfiguration =
            new UITestTestConfigurationResolver().resolve(topLevelTestClass.getAnnotation(UITest.class));

        // Serve the maven repository over HTTP from a container reachable by XWiki.
        String repositoryURL = startRepositoryContainer(extensionContext, mavenRepository, resolvedConfiguration);

        // Save a TestConfiguration in the global test context so that it's merged in XWikiDockerExtension. We:
        // - configure the maven test repository served over HTTP by the nginx container
        // - disable core extension resolve because Jetty is not ready when it starts
        ExtensionContext.Store globalStore = extensionContext.getRoot().getStore(ExtensionContext.Namespace.GLOBAL);
        TestConfiguration configuration = new TestConfiguration();
        Properties properties = new Properties();
        properties.setProperty("xwikiPropertiesAdditionalProperties", String.format(
            "extension.repositories = maven-test:maven:%s%n"
                + "extension.core.resolve = false",
            repositoryURL));
        configuration.setProperties(properties);
        globalStore.put(TestConfiguration.class, configuration);
    }

    /**
     * Start the nginx container serving the maven repository over HTTP.
     *
     * @return the URL at which the repository is reachable <em>by XWiki</em>: through the shared network alias when
     *         XWiki runs inside a container, or through the mapped port on the host when XWiki runs outside Docker
     *         (e.g. with the default Jetty standalone servlet engine)
     */
    private String startRepositoryContainer(ExtensionContext extensionContext, File mavenRepository,
        TestConfiguration testConfiguration)
    {
        // Note: we copy the files into the container instead of mounting a volume so that this also works with DOOD
        // (Docker out of Docker), where volume mounts from the host don't work.
        NginxContainer nginx = new NginxContainer(NGINX_IMAGE)
            .withNetwork(Network.SHARED)
            .withNetworkAliases(REPOSITORY_HOST)
            .withExposedPorts(80)
            .withCopyFileToContainer(MountableFile.forHostPath(mavenRepository.toPath()), NGINX_DOCUMENT_ROOT);
        nginx.setWaitStrategy(Wait.forListeningPort());
        nginx.start();

        // Make sure the container is stopped at the end of the test run.
        extensionContext.getRoot().getStore(ExtensionContext.Namespace.GLOBAL)
            .put(NginxContainer.class, (ExtensionContext.Store.CloseableResource) nginx::stop);

        if (testConfiguration.getServletEngine().isOutsideDocker()) {
            // XWiki runs on the host: reach the repository through the port mapped on the host.
            return String.format("http://%s:%d/", nginx.getHost(), nginx.getMappedPort(80));
        } else {
            // XWiki runs in a container connected to the shared network: reach the repository through its alias.
            return String.format("http://%s/", REPOSITORY_HOST);
        }
    }
}
