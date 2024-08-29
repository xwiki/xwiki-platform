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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.velocity.VelocityContext;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private ArtifactResolver artifactResolver;

    private MavenResolver mavenResolver;

    private TestConfiguration testConfiguration;

    /**
     * Used to start and stop the Jetty instance. We need to use the same instance to stop it since otherwise the stop
     * won't do anything as there would be no state about the instance having been started fine.
     */
    private XWikiExecutor executor;

    /**
     * @param testConfiguration the configuration to build (database, debug mode, etc)
     * @param artifactResolver the resolver to resolve artifacts from Maven repositories
     * @param mavenResolver the resolver to read Maven POMs
     */
    public JettyStandaloneExecutor(TestConfiguration testConfiguration, ArtifactResolver artifactResolver,
        MavenResolver mavenResolver)
    {
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
     * @throws Exception when an error occurs
     */
    public void start() throws Exception
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
            System.setProperty("xwikiExecutionDirectory", getJettyDirectory());
            this.executor = new XWikiExecutor(0);
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
        return String.format("%s/jetty", directory);
    }
}
