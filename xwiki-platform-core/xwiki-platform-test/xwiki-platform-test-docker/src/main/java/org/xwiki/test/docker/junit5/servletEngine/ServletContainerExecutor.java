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
import java.io.FileWriter;
import java.time.Duration;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.xwiki.test.docker.junit5.AbstractContainerExecutor;
import org.xwiki.test.docker.junit5.ArtifactResolver;
import org.xwiki.test.docker.junit5.MavenResolver;
import org.xwiki.test.docker.junit5.RepositoryResolver;
import org.xwiki.test.docker.junit5.TestConfiguration;

import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.SearchItem;

import static java.time.temporal.ChronoUnit.SECONDS;

/**
 * Create and execute the Docker Servlet engine container for the tests.
 *
 * @version $Id$
 * @since 10.9
 */
public class ServletContainerExecutor extends AbstractContainerExecutor
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ServletContainerExecutor.class);

    private static final String LATEST = "latest";

    private JettyStandaloneExecutor jettyStandaloneExecutor;

    private RepositoryResolver repositoryResolver;

    private TestConfiguration testConfiguration;

    private GenericContainer servletContainer;

    /**
     * @param testConfiguration the configuration to build (database, debug mode, etc)
     * @param artifactResolver the resolver to resolve artifacts from Maven repositories
     * @param mavenResolver the resolver to read Maven POMs
     * @param repositoryResolver the resolver to create Maven repositories and sessions
     */
    public ServletContainerExecutor(TestConfiguration testConfiguration, ArtifactResolver artifactResolver,
        MavenResolver mavenResolver, RepositoryResolver repositoryResolver)
    {
        this.testConfiguration = testConfiguration;
        this.jettyStandaloneExecutor = new JettyStandaloneExecutor(testConfiguration, artifactResolver, mavenResolver);
        this.repositoryResolver = repositoryResolver;
    }

    /**
     * @param sourceWARDirectory the location where the built WAR is located
     * @throws Exception if an error occurred during the build or start
     */
    public void start(File sourceWARDirectory) throws Exception
    {
        String xwikiIPAddress = "localhost";
        int xwikiPort = 8080;

        switch (this.testConfiguration.getServletEngine()) {
            case TOMCAT:
                // Configure Tomcat logging for debugging. Create a logging.properties file
                File logFile = new File(sourceWARDirectory, "WEB-INF/classes/logging.properties");
                logFile.createNewFile();
                try (FileWriter writer = new FileWriter(logFile)) {
                    IOUtils.write("org.apache.catalina.core.ContainerBase.[Catalina].level = FINE\n"
                        + "org.apache.catalina.core.ContainerBase.[Catalina].handlers = "
                        + "java.util.logging.ConsoleHandler\n", writer);
                }
                servletContainer = getServletContainerImage(testConfiguration);
                mountFromHostToContainer(this.servletContainer, sourceWARDirectory.toString(),
                    "/usr/local/tomcat/webapps/xwiki");

                this.servletContainer.withEnv("CATALINA_OPTS", "-Xmx1024m "
                    + "-Dorg.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true "
                    + "-Dorg.apache.catalina.connector.CoyoteAdapter.ALLOW_BACKSLASH=true "
                    + "-Dsecurerandom.source=file:/dev/urandom");

                break;
            case JETTY:
                this.servletContainer = getServletContainerImage(testConfiguration);
                mountFromHostToContainer(this.servletContainer, sourceWARDirectory.toString(),
                    "/var/lib/jetty/webapps/xwiki");

                break;
            case WILDFLY:
                this.servletContainer = getServletContainerImage(testConfiguration);
                mountFromHostToContainer(this.servletContainer, sourceWARDirectory.toString(),
                    "/opt/jboss/wildfly/standalone/deployments/xwiki");

                break;
            case JETTY_STANDALONE:
                // Resolve and unzip the xwiki-platform-tool-jetty-resources zip artifact and configure Jetty to
                // use the custom WAR that we generated. Then start jetty from the command line shell script.
                // Note that we could have decided to embed Jetty (see
                // http://www.eclipse.org/jetty/documentation/9.4.x/embedding-jetty.html) but we decided against that
                // as it would mean maintaining 2 Jetty configurations (one for the Jetty standalone packaging and
                // one for Jetty in embedded mode).
                this.jettyStandaloneExecutor.start();
                break;
            default:
                throw new RuntimeException(String.format("Servlet engine [%s] is not yet supported!",
                    this.testConfiguration.getServletEngine()));
        }

        if (this.servletContainer != null) {

            startContainer();

            xwikiIPAddress = this.servletContainer.getContainerIpAddress();
            xwikiPort =
                this.servletContainer.getMappedPort(this.testConfiguration.getServletEngine().getInternalPort());
        }

        this.testConfiguration.getServletEngine().setIP(xwikiIPAddress);
        this.testConfiguration.getServletEngine().setPort(xwikiPort);
    }

    private void startContainer() throws Exception
    {
        // Note: TestContainers will wait for up to 60 seconds for the container's first mapped network port to
        // start listening.
        this.servletContainer
            .withNetwork(Network.SHARED)
            .withNetworkAliases(this.testConfiguration.getServletEngine().getInternalIP())
            .withExposedPorts(this.testConfiguration.getServletEngine().getInternalPort())
            .waitingFor(
                Wait.forHttp("/xwiki/bin/get/Main/WebHome")
                    .forStatusCode(200).withStartupTimeout(Duration.of(480, SECONDS)));

        if (this.testConfiguration.isOffline()) {
            String repoLocation = this.repositoryResolver.getSession().getLocalRepository().getBasedir().toString();
            this.servletContainer.withFileSystemBind(repoLocation, "/root/.m2/repository");
        }

        start(this.servletContainer, this.testConfiguration);
    }

    private GenericContainer getServletContainerImage(TestConfiguration testConfiguration)
    {
        final String baseImageName;

        switch (testConfiguration.getServletEngine()) {
            case TOMCAT:
                baseImageName = String.format("tomcat:%s", testConfiguration.getServletEngineTag() != null
                    ? testConfiguration.getServletEngineTag() : LATEST);
                break;

            case JETTY:
                baseImageName = String.format("jetty:%s", testConfiguration.getServletEngineTag() != null
                    ? testConfiguration.getServletEngineTag() : LATEST);
                break;

            case WILDFLY:
                baseImageName = String.format("jboss/wildfly:%s", testConfiguration.getServletEngineTag() != null
                    ? testConfiguration.getServletEngineTag() : LATEST);
                break;

            default:
                throw new RuntimeException(String.format("Servlet engine [%s] is not yet supported!",
                    testConfiguration.getServletEngine()));
        }

        // we want to use libreoffice so we need a custom image
        if (testConfiguration.isOffice()) {
            // name of the image that we will create
            String imageName = StringUtils.join("xwiki", "_", baseImageName, "_", "office").replace(':', '_');

            // we won't delete the image, so it's possible that the image already exists: it would avoid us to create
            // it again
            List<Image> imageSearchResults = DockerClientFactory.instance().client().listImagesCmd()
                .withImageNameFilter(imageName).exec();

            // the image does not exist
            if (imageSearchResults.isEmpty()) {

                LOGGER.info("(*) Build a dedicated image embedding libre office...");
                // let's create it with the appropriate name
                // the flag indicates that we don't want it to be deleted
                return new GenericContainer(new ImageFromDockerfile(imageName, false)
                    .withDockerfileFromBuilder(builder ->
                        builder
                            .from(baseImageName)
                            .user("root")
                            .run("apt-get update && apt-get install -y"
                                + " curl"
                                + " libreoffice"
                                + " unzip"
                                + " procps")
                            .build()));
            } else {
                return new GenericContainer(imageName);
            }
        }
        return new GenericContainer(baseImageName);
    }

    /**
     * @throws Exception if an error occurred during the stop
     */
    public void stop() throws Exception
    {
        switch (this.testConfiguration.getServletEngine()) {
            case JETTY_STANDALONE:
                this.jettyStandaloneExecutor.stop();
                break;
            default:
                // Nothing else to do, TestContainers automatically stops the container
        }
    }
}
