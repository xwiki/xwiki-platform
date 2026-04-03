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
package org.xwiki.test.docker.internal.junit5.servletengine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.velocity.VelocityContext;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.integration.XWikiExecutor;
import org.xwiki.test.integration.maven.ArtifactResolver;
import org.xwiki.test.integration.maven.MavenResolver;

import static org.xwiki.test.docker.internal.junit5.FileTestUtils.unzip;

/**
 * Create a Jetty Standalone packaging on the file system and start/stop Jetty.
 *
 * @version $Id$
 * @since 10.10RC1
 */
public class JettyStandaloneExecutor
{
    private static final Logger LOGGER = LoggerFactory.getLogger(JettyStandaloneExecutor.class);

    private static final String DATA_SUBDIR = "data";

    private static final String XWIKI_OPTS = "XWIKI_OPTS";

    /**
     * The memory option which start_xwiki.sh sets when {@code XWIKI_OPTS} is not defined.
     */
    private static final String DEFAULT_MEMORY_OPTION = "-Xmx1024m";

    private final int index;

    private ArtifactResolver artifactResolver;

    private MavenResolver mavenResolver;

    private TestConfiguration testConfiguration;

    /**
     * Used to start and stop the Jetty instance. We need to use the same instance to stop it since otherwise the stop
     * won't do anything as there would be no state about the instance having been started fine.
     */
    private XWikiExecutor executor;

    /**
     * @param index the index of the executor
     * @param testConfiguration the configuration to build (database, debug mode, etc)
     * @param artifactResolver the resolver to resolve artifacts from Maven repositories
     * @param mavenResolver the resolver to read Maven POMs
     * @since 18.3.0RC1
     */
    public JettyStandaloneExecutor(int index, TestConfiguration testConfiguration, ArtifactResolver artifactResolver,
        MavenResolver mavenResolver)
    {
        this.index = index;
        this.testConfiguration = testConfiguration;
        this.artifactResolver = artifactResolver;
        this.mavenResolver = mavenResolver;
    }

    /**
     * @return the directory where the exploded XWiki WAR will be created
     */
    public File getWARDirectory()
    {
        return new File(new File(getJettyDirectory(), "webapps"), "xwiki");
    }

    /**
     * Create a Jetty Standalone packaging on the file system and start Jetty.
     *
     * @return the executor used to start Jetty
     * @throws Exception when an error occurs
     * @since 18.3.0RC1
     */
    public XWikiExecutor start() throws Exception
    {
        // For performance reason, skip creating the jetty packaging if it already exists
        File jettyDirectory = new File(getJettyDirectory());
        File jettyJettyDirectory = new File(getJettyDirectory(getJettyDirectory()));
        if (!jettyJettyDirectory.exists()) {
            // Step: Resolve the jetty resources
            String xwikiVersion = this.mavenResolver.getPlatformVersion();
            Artifact jettyArtifact =
                new DefaultArtifact("org.xwiki.platform", "xwiki-platform-tool-jetty-resources", "zip", xwikiVersion);
            File jettyArtifactFile = this.artifactResolver.resolveArtifact(jettyArtifact).getArtifact().getFile();

            // Step: Unzip
            unzip(jettyArtifactFile, jettyDirectory);

            // Step: Replace properties in start shell scripts
            Collection<File> startFiles = org.apache.commons.io.FileUtils.listFiles(jettyDirectory,
                new WildcardFileFilter("start_xwiki*.*"), null);

            VelocityContext velocityContext = createVelocityContext();
            for (File startFile : startFiles) {
                LOGGER.info("Replacing variables in [{}]...", startFile);
                String content = org.apache.commons.io.FileUtils.readFileToString(startFile, StandardCharsets.UTF_8);
                try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(startFile))) {
                    writer.write(replaceProperty(content, velocityContext));
                }
            }
        }

        // Step: Remove data directory since we will provision again the extensions to account for source changes.
        FileUtils.deleteDirectory(new File(jettyDirectory, DATA_SUBDIR));

        // Step: Start Jetty

        // Don't check if XWiki is started since this is done already in XWikiDockerExtension
        System.setProperty("xwiki.test.verifyRunningXWikiAtStart", "false");

        // If we're on debug mode, start XWiki in debug mode too so that we can attach a remote debugger to it in order
        // to debug.
        if (this.testConfiguration.isDebug()) {
            System.setProperty("debug", "true");
        }

        getExecutor().start();

        return this.executor;
    }

    /**
     * Stops a running Jetty instance.
     *
     * @throws Exception when an error occurs
     */
    public void stop() throws Exception
    {
        getExecutor().stop();
    }

    private XWikiExecutor getExecutor()
    {
        if (this.executor == null) {
            // Note: the property is suffixed with the index since each XWiki instance has its own Jetty packaging (the
            // property without index is a static in XWikiExecutor and thus cannot be changed for each instance).
            System.setProperty("xwikiExecutionDirectory" + this.index, getJettyDirectory());
            // Note: the browser runs in a container and thus reaches the instance, which runs on the host, through
            // the host name exposing the host. Each instance listens on its own port.
            this.executor = new XWikiExecutor(this.index, GenericContainer.INTERNAL_HOST_HOSTNAME,
                XWikiExecutor.resolvePort(this.index));

            // Tell the instance where to find the other members of the cluster (when there are several instances)
            List<String> clusterOptions = RemoteObservationJavaOptions.get(this.index, this.testConfiguration);
            if (!clusterOptions.isEmpty()) {
                // Note: start_xwiki.sh only sets the default memory option when XWIKI_OPTS is not set, so it needs to
                // be passed along with the cluster options.
                String currentOptions = System.getenv(XWIKI_OPTS);
                List<String> xwikiOptions = new ArrayList<>();
                xwikiOptions.add(currentOptions == null || currentOptions.isBlank() ? DEFAULT_MEMORY_OPTION
                    : currentOptions);
                xwikiOptions.addAll(clusterOptions);
                this.executor.addEnvironmentVariable(XWIKI_OPTS, String.join(" ", xwikiOptions));
            }
        }

        return this.executor;
    }

    private VelocityContext createVelocityContext()
    {
        Map<String, Object> properties = new HashMap<>();
        properties.put("xwikiDataDir", DATA_SUBDIR);
        return new VelocityContext(properties);
    }

    private String replaceProperty(String content, VelocityContext velocityContext)
    {
        String result = content;
        for (Object key : velocityContext.getKeys()) {
            Object value = velocityContext.get(key.toString());
            result = StringUtils.replace(result, String.format("${%s}", key), value.toString());
        }
        return result;
    }

    private String getJettyDirectory()
    {
        return getJettyDirectory(this.testConfiguration.getOutputDirectory());
    }

    private String getJettyDirectory(String directory)
    {
        // Each XWiki instance gets its own Jetty packaging since each of them has its own configuration (ports, data
        // directory, etc). The first instance keeps the standard location to make it easier to find.
        return this.index > 0 ? String.format("%s/jetty-%s", directory, this.index)
            : String.format("%s/jetty", directory);
    }
}
