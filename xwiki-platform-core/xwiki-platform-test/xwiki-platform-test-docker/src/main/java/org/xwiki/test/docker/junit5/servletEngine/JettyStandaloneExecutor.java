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
package org.xwiki.test.docker.junit5.servletEngine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.velocity.VelocityContext;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.test.docker.junit5.ArtifactResolver;
import org.xwiki.test.docker.junit5.DockerTestUtils;
import org.xwiki.test.docker.junit5.MavenResolver;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.integration.XWikiExecutor;

/**
 * Create a Jetty Standalone packaging on the file system and start/stop Jetty.
 *
 * @version $Id$
 * @since 10.10RC1
 */
public class JettyStandaloneExecutor
{
    private static final Logger LOGGER = LoggerFactory.getLogger(JettyStandaloneExecutor.class);

    private static final String XWIKI_SUBDIR = "xwiki";

    private static final String DATA_SUBDIR = "data";

    private static final String PARENT_DIR = "..";

    private ArtifactResolver artifactResolver;

    private MavenResolver mavenResolver;

    private TestConfiguration testConfiguration;

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
     * Create a Jetty Standalone packaging on the file system and start Jetty.
     *
     * @throws Exception when an error occurs
     */
    public void start() throws Exception
    {
        // For performance reason, skip creating the jetty packaging if it already exists
        File jettyDirectory = new File(getJettyDirectory());
        if (!jettyDirectory.exists()) {
            // Step: Resolve the jetty resources
            String xwikiVersion = this.mavenResolver.getPlatformVersion();
            Artifact jettyArtifact =
                new DefaultArtifact("org.xwiki.platform", "xwiki-platform-tool-jetty-resources", "zip", xwikiVersion);
            File jettyArtifactFile = this.artifactResolver.resolveArtifact(jettyArtifact).getArtifact().getFile();

            // Step: Unzip
            DockerTestUtils.unzip(jettyArtifactFile, jettyDirectory);

            // Step: Replace properties in start shell scripts
            Collection<File> startFiles = org.apache.commons.io.FileUtils.listFiles(jettyDirectory,
                new WildcardFileFilter("start_xwiki*.*"), null);

            VelocityContext velocityContext = createVelocityContext();
            for (File startFile : startFiles) {
                LOGGER.info(String.format("Replacing variables in [%s]...", startFile));
                String content = org.apache.commons.io.FileUtils.readFileToString(startFile, StandardCharsets.UTF_8);
                try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(startFile))) {
                    writer.write(replaceProperty(content, velocityContext));
                }
            }

            // Step: Link the generated WAR to the Jetty webapp dir.
            Path webapps = jettyDirectory.toPath().resolve("webapps");
            Path link = webapps.resolve(XWIKI_SUBDIR);
            Files.createDirectories(webapps);
            Path target = Paths.get(PARENT_DIR, PARENT_DIR, XWIKI_SUBDIR);
            Files.createSymbolicLink(link, target);

            // Step: Remove root webapp since we don't need it
            FileUtils.forceDelete(new File(jettyDirectory, "jetty/contexts/root.xml"));
        }

        // Step: Remove data directory since we will provision again the extensions to account for source changes.
        FileUtils.deleteDirectory(new File(jettyDirectory, DATA_SUBDIR));

        // Step: Start Jetty
        // Don't check if XWiki is started since this is done already in XWikiDockerExtension
        System.setProperty("xwiki.test.verifyRunningXWikiAtStart", "false");
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
        System.setProperty("xwikiExecutionDirectory", getJettyDirectory());
        XWikiExecutor executor = new XWikiExecutor(0);
        return executor;
    }

    private VelocityContext createVelocityContext()
    {
        Properties properties = new Properties();
        properties.setProperty("xwikiDataDir", DATA_SUBDIR);
        VelocityContext context = new VelocityContext(properties);
        return context;
    }

    private String replaceProperty(String content, VelocityContext velocityContext)
    {
        String result = content;
        for (Object key : velocityContext.getKeys()) {
            Object value = velocityContext.get(key.toString());
            result = StringUtils.replace(result, String.format("${%s}", key.toString()), value.toString());
        }
        return result;
    }

    private String getJettyDirectory()
    {
        return String.format("%s/jetty", this.testConfiguration.getOutputDirectory());
    }
}
