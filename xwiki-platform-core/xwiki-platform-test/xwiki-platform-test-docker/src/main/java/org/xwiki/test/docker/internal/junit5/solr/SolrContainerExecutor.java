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
package org.xwiki.test.docker.internal.junit5.solr;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import jakarta.inject.Named;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.solr.SolrContainer;
import org.testcontainers.utility.MountableFile;
import org.xwiki.component.annotation.Component;
import org.xwiki.search.solr.SolrCoreInitializer;
import org.xwiki.search.solr.internal.DefaultSolrConfiguration;
import org.xwiki.search.solr.internal.search.SearchCoreInitializer;
import org.xwiki.test.docker.internal.junit5.AbstractContainerExecutor;
import org.xwiki.test.docker.junit5.DockerTestException;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.solr.SolrMode;
import org.xwiki.test.integration.maven.MavenResolver;

/**
 * Create and execute the Docker Solr container for the tests.
 *
 * @version $Id$
 * @since 18.3.0RC1
 */
public class SolrContainerExecutor extends AbstractContainerExecutor
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SolrContainerExecutor.class);

    private static final String NETWORK_ALIAS = "xwikisolr";

    /**
     * @param testConfiguration the configuration to build (Solr mode, debug mode, etc)
     * @param mavenResolver the Maven resolver to use to resolve Solr core artifacts
     * @throws Exception if the container fails to start
     */
    public void start(TestConfiguration testConfiguration, MavenResolver mavenResolver) throws Exception
    {
        SolrMode solrMode = testConfiguration.getSolrMode();
        if (solrMode == null) {
            // Default to embedded
            return;
        }

        switch (solrMode) {
            case EMBEDDED:
                // No container needed for embedded mode
                break;
            case REMOTE:
                startSolrContainer(testConfiguration, mavenResolver);
                break;
            default:
                throw new DockerTestException(String.format("Solr mode [%s] is not supported!", solrMode));
        }
    }

    /**
     * @param testConfiguration the configuration to build (blob store, debug mode, etc)
     */
    public void stop(TestConfiguration testConfiguration)
    {
        // Note that we don't need to stop the container as this is taken care of by TestContainers
    }

    private void startSolrContainer(TestConfiguration testConfiguration, MavenResolver mavenResolver) throws Exception
    {
        SolrContainer solrContainer;
        String solrTag = testConfiguration.getRemoteSolrTag();
        if (StringUtils.isNotBlank(solrTag)) {
            solrContainer = new SolrContainer(String.format("solr:%s", solrTag));
        } else {
            // No tag specified, use "latest"
            solrContainer = new SolrContainer("solr:latest");
        }

        String solrCoresPath = testConfiguration.getOutputDirectory() + "/solr/";

        // Resolve the cores artifacts
        Map<String, File> coreArtifacts =
            Map.of(SearchCoreInitializer.CORE_NAME, getCoreArtifact(SearchCoreInitializer.CORE_NAME, mavenResolver),
                "minimal", getCoreArtifact("minimal", mavenResolver));

        // Create the cores
        for (String coreName : findAllCores()) {
            addCoreToContainer(solrContainer, coreName);
        }

        // Configure the container
        solrContainer.withNetwork(Network.SHARED).withNetworkAliases(NETWORK_ALIAS)
            .withCopyFileToContainer(MountableFile.forHostPath(solrCoresPath), "/var/solr/");

        start(solrContainer, testConfiguration);
    }

    private void addCoreToContainer(SolrContainer solrContainer, String coreName, String solrCoresPath) throws Exception
    {
        String coreDirectoryPath = solrCoresPath + coreName;
        File coreDirectory = new File(coreDirectoryPath);
        if (!coreDirectory.exists()) {
            coreDirectory.mkdirs();
        }

        try (InputStream defaultCoreContent =
            getClass().getResourceAsStream("/xwiki-platform-search-solr-server-core.zip")) {
            copyCoreConfiguration(defaultCoreContent, coreDirectory.toPath());
        }
    }

    private void copyCoreConfiguration(InputStream stream, Path corePath) throws IOException
    {
        try (ZipInputStream zstream = new ZipInputStream(stream)) {
            for (ZipEntry entry = zstream.getNextEntry(); entry != null; entry = zstream.getNextEntry()) {
                Path targetPath = corePath.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                } else {
                    FileUtils.copyInputStreamToFile(CloseShieldInputStream.wrap(zstream), targetPath.toFile());
                }
            }
        }
    }

    private File getCoreArtifact(String coreName, MavenResolver mavenResolver) throws Exception
    {
        mavenResolver
            .getArtifactResolver().resolveArtifact(new DefaultArtifact("org.xwiki.platform",
                "xwiki-platform-tool-configuration-resources", "zip", mavenResolver.getPlatformVersion()))
            .getArtifact().getFile();
    }

    private String getCoreArtifactId(String coreName)
    {
        return SearchCoreInitializer.CORE_NAME.equals(coreName) ? DefaultSolrConfiguration.CORE_ARTIFACTID_SEARCH
            : DefaultSolrConfiguration.CORE_ARTIFACTID_MINIMAL;
    }

    private Set<String> findAllCores()
    {
        Set<String> namedValues = new HashSet<>();
        Reflections reflections = new Reflections("org.xwiki");

        Set<Class<? extends SolrCoreInitializer>> implementations =
            reflections.getSubTypesOf(SolrCoreInitializer.class);

        for (Class<? extends SolrCoreInitializer> implClass : implementations) {
            addComponentRoleHint(implClass, namedValues);
        }

        return namedValues;
    }

    private void addComponentRoleHint(Class<?> componentClass, Set<String> hints)
    {
        Named namedAnnotation = componentClass.getAnnotation(Named.class);
        if (namedAnnotation != null) {
            hints.add(namedAnnotation.value());
        }

        javax.inject.Named javaxAnnotation = componentClass.getAnnotation(javax.inject.Named.class);
        if (javaxAnnotation != null) {
            hints.add(javaxAnnotation.value());
        }

        Component componentAnnotation = componentClass.getAnnotation(Component.class);
        if (componentAnnotation != null) {
            hints.addAll(List.of(componentAnnotation.hints()));
        }
    }
}
