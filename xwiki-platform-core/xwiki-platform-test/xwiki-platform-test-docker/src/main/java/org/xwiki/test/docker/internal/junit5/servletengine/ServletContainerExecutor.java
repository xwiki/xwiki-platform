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
import java.io.FileWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import org.xwiki.test.docker.internal.junit5.AbstractContainerExecutor;
import org.xwiki.test.docker.internal.junit5.DockerTestUtils;
import org.xwiki.test.docker.internal.junit5.XWikiLocalGenericContainer;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.database.Database;
import org.xwiki.test.docker.junit5.servletengine.ServletEngine;
import org.xwiki.test.integration.maven.ArtifactResolver;
import org.xwiki.test.integration.maven.MavenResolver;
import org.xwiki.test.integration.maven.RepositoryResolver;

import com.github.dockerjava.api.model.Image;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.xwiki.test.docker.internal.junit5.DockerTestUtils.isInAContainer;

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

    private static final String CLOVER_DATABASE = System.getProperty("maven.clover.cloverDatabase");

    private static final String ORACLE_TZ_WORKAROUND = "-Doracle.jdbc.timezoneAsRegion=false";

    private static final String OFFICE_IMAGE_VERSION_LABEL = "image-version";

    private JettyStandaloneExecutor jettyStandaloneExecutor;

    private RepositoryResolver repositoryResolver;

    private MavenResolver mavenResolver;

    private TestConfiguration testConfiguration;

    private GenericContainer<?> servletContainer;

    /**
     * @param testConfiguration the configuration to build (database, debug mode, etc)
     * @param artifactResolver the resolver to resolve artifacts from Maven repositories
     * @param mavenResolver the resolver to read Maven POMs
     * @param repositoryResolver the resolver to create Maven repositories and sessions
     */
    public ServletContainerExecutor(TestConfiguration testConfiguration, ArtifactResolver artifactResolver,
        MavenResolver mavenResolver, RepositoryResolver repositoryResolver)
    {
        this.mavenResolver = mavenResolver;
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
                configureTomcat(sourceWARDirectory);
                break;
            case JETTY:
                configureJetty(sourceWARDirectory);
                break;
            case WILDFLY:
                configureWildFly(sourceWARDirectory);
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
                    testConfiguration.getServletEngine()));
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

    private void configureWildFly(File sourceWARDirectory) throws Exception
    {
        this.servletContainer = createServletContainer();
        mountFromHostToContainer(this.servletContainer, sourceWARDirectory.toString(),
            "/opt/jboss/wildfly/standalone/deployments/xwiki");
    }

    private void configureJetty(File sourceWARDirectory) throws Exception
    {
        this.servletContainer = createServletContainer();
        mountFromHostToContainer(this.servletContainer, sourceWARDirectory.toString(),
            "/var/lib/jetty/webapps/xwiki");

        // When executing on the Oracle database, we get the following timezone error unless we pass a system
        // property to the Oracle JDBC driver:
        //   java.sql.SQLException: Cannot create PoolableConnectionFactory (ORA-00604: error occurred at
        //   recursive SQL level 1
        //   ORA-01882: timezone region not found
        if (this.testConfiguration.getDatabase().equals(Database.ORACLE)) {
            List<String> commandPartList =
                new ArrayList<>(Arrays.asList(this.servletContainer.getCommandParts()));
            commandPartList.add(ORACLE_TZ_WORKAROUND);
            this.servletContainer.setCommandParts(commandPartList.toArray(new String[0]));
        }
    }

    private void configureTomcat(File sourceWARDirectory) throws Exception
    {
        // Configure Tomcat logging for debugging. Create a logging.properties file
        File logFile = new File(sourceWARDirectory, "WEB-INF/classes/logging.properties");
        if (!logFile.createNewFile()) {
            throw new Exception(String.format("Failed to create logging configuration file at [%s]", logFile));
        }
        try (FileWriter writer = new FileWriter(logFile)) {
            IOUtils.write("org.apache.catalina.core.ContainerBase.[Catalina].level = FINE\n"
                + "org.apache.catalina.core.ContainerBase.[Catalina].handlers = "
                + "java.util.logging.ConsoleHandler\n", writer);
        }
        this.servletContainer = createServletContainer();
        mountFromHostToContainer(this.servletContainer, sourceWARDirectory.toString(),
            "/usr/local/tomcat/webapps/xwiki");

        List<String> catalinaOpts = new ArrayList<>();
        catalinaOpts.add("-Xmx1024m");
        catalinaOpts.add("-Dorg.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true");
        catalinaOpts.add("-Dorg.apache.catalina.connector.CoyoteAdapter.ALLOW_BACKSLASH=true");
        catalinaOpts.add("-Dsecurerandom.source=file:/dev/urandom");

        // If we're on debug mode, start XWiki in debug mode too so that we can attach a remote debugger to it
        // in order to debug.
        // Note: To attach the remote debugger, run "docker ps" to get the local mapped port for 5005, and use
        // "localhost" as the JVM host to connect to.
        if (this.testConfiguration.isDebug()) {
            catalinaOpts.add("-Xdebug");
            catalinaOpts.add("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005");
            catalinaOpts.add("-Xnoagent");
            catalinaOpts.add("-Djava.compiler=NONE");
        }

        // When executing on the Oracle database, we get the following timezone error unless we pass a system
        // property to the Oracle JDBC driver:
        //   java.sql.SQLException: Cannot create PoolableConnectionFactory (ORA-00604: error occurred at
        //   recursive SQL level 1
        //   ORA-01882: timezone region not found
        if (this.testConfiguration.getDatabase().equals(Database.ORACLE)) {
            catalinaOpts.add(ORACLE_TZ_WORKAROUND);
        }

        this.servletContainer.withEnv("CATALINA_OPTS", StringUtils.join(catalinaOpts, ' '));
    }

    private void startContainer() throws Exception
    {
        // Note: TestContainers will wait for up to 60 seconds for the container's first mapped network port to
        // start listening.
        this.servletContainer
            .withNetwork(Network.SHARED)
            .withNetworkAliases(this.testConfiguration.getServletEngine().getInternalIP())
            .waitingFor(
                Wait.forHttp("/xwiki/bin/get/Main/WebHome")
                    .forStatusCode(200).withStartupTimeout(Duration.of(480, SECONDS)));

        List<Integer> exposedPorts = new ArrayList<>();
        exposedPorts.add(this.testConfiguration.getServletEngine().getInternalPort());
        // In debug mode, we need to map the debug port so that the host can attach the remote debugger to the JVM
        // inside the container.
        if (this.testConfiguration.isDebug()) {
            exposedPorts.add(5005);
        }
        this.servletContainer.withExposedPorts(exposedPorts.toArray(new Integer[exposedPorts.size()]));

        // We want by default to have the local repository mounted, but this won't work in the DOOD use case.
        // For that to work we would need to copy the data instead of mounting the volume but the time
        // it would take would be too costly.
        if (!DockerTestUtils.isInAContainer()) {
            String repoLocation = this.repositoryResolver.getSession().getLocalRepository().getBasedir().toString();
            this.servletContainer.withFileSystemBind(repoLocation, "/root/.m2/repository");
        }

        // If the Clover database system property is setup, then copy or map the clover database location on the FS to
        // a path inside the container
        if (CLOVER_DATABASE != null && !this.testConfiguration.getServletEngine().isOutsideDocker()) {
            // Note 1: The Clover instrumentation puts the full path to the clover database location inside the java
            // source files which execute inside the docker container. Thus we need to make that exact same full path
            // available inside the container...
            // Note 2: For this to work in DOOD (Docker Outside Of Docker), the container in which this code is
            // running will need to have mapped the volume pointed to by the "maven.clover.cloverDatabase" system
            // property. It'll also need to make sure that it's removed before the build executes so that it doesn't
            // execute with Clover data from a previous run.
            // Note 3: The copy is done the other way around when the container is stopped so that the new added
            // Clover data is available from the Maven container for computing the Clover report.
            mountFromHostToContainer(this.servletContainer, CLOVER_DATABASE, CLOVER_DATABASE);
        }

        start(this.servletContainer, this.testConfiguration);
    }

    private String getDockerImageTag(TestConfiguration testConfiguration)
    {
        return testConfiguration.getServletEngineTag() != null ? testConfiguration.getServletEngineTag() : LATEST;
    }

    private GenericContainer createServletContainer() throws Exception
    {
        String baseImageName = String.format("%s:%s",
            this.testConfiguration.getServletEngine().getDockerImageName(), getDockerImageTag(this.testConfiguration));
        GenericContainer<?> container;

        if (this.testConfiguration.isOffice()) {
            // We only build the image once for performance reason.
            // So we provide a name to the image we will built and we check that the image does not exist yet.
            String imageName = String.format("xwiki-%s-office:%s",
                this.testConfiguration.getServletEngine().name().toLowerCase(), getDockerImageTag(testConfiguration));

            // We rebuild every time the LibreOffice version changes
            String officeVersion = this.mavenResolver.getPropertyFromCurrentPOM("libreoffice.version");
            String imageVersion = String.format("LO-%S", officeVersion);
            List<Image> imageSearchResults = DockerClientFactory.instance().client().listImagesCmd()
                .withImageNameFilter(imageName)
                .withLabelFilter(Collections.singletonMap(OFFICE_IMAGE_VERSION_LABEL, imageVersion))
                .exec();

            if (imageSearchResults.isEmpty()) {
                LOGGER.info("(*) Build a dedicated image embedding LibreOffice...");
                // The second argument of the ImageFromDockerfile is here to indicate we won't delete the image
                // at the end of the test container execution.
                container = new XWikiLocalGenericContainer(new ImageFromDockerfile(imageName, false)
                    .withDockerfileFromBuilder(builder -> {
                        builder
                            .from(baseImageName)
                            .user("root")
                            .env("LIBREOFFICE_VERSION", officeVersion)
                            .env("LIBREOFFICE_DOWNLOAD_URL", "https://downloadarchive.documentfoundation.org/"
                                + "libreoffice/old/$LIBREOFFICE_VERSION/deb/x86_64/"
                                + "LibreOffice_${LIBREOFFICE_VERSION}_Linux_x86-64_deb.tar.gz")
                            // Note that we expose libreoffice /usr/local/libreoffice so that it can be found by
                            // JODConverter: https://bit.ly/2w8B82Q
                            .run("apt-get update && "
                                + "apt-get --no-install-recommends -y install curl unzip procps libxinerama1 "
                                    + "libdbus-glib-1-2 libcairo2 libcups2 libsm6 && "
                                + "rm -rf /var/lib/apt/lists/* /var/cache/apt/* && "
                                + "wget --no-verbose -O /tmp/libreoffice.tar.gz $LIBREOFFICE_DOWNLOAD_URL && "
                                + "mkdir /tmp/libreoffice && "
                                + "tar -C /tmp/ -xvf /tmp/libreoffice.tar.gz && "
                                + "cd /tmp/LibreOffice_${LIBREOFFICE_VERSION}_Linux_x86-64_deb/DEBS && "
                                + "dpkg -i *.deb && "
                                + "ln -fs `ls -d /opt/libreoffice*` /opt/libreoffice")
                            // Increment the image version whenever a change is brought to the image so that it can
                            // reconstructed on all machines needing it.
                            .label(OFFICE_IMAGE_VERSION_LABEL, imageVersion);
                        if (this.testConfiguration.getServletEngine() == ServletEngine.JETTY) {
                            // Create the right jetty user directory since it doesn't exist
                            builder.run("mkdir -p /home/jetty && chown jetty:jetty /home/jetty")
                                // Put back the user as jetty since it's a best practice to not execute the container as
                                // root.
                                .user("jetty");
                        }

                        builder.build();
                    }));
            } else {
                container = new XWikiLocalGenericContainer(imageName);
            }
        } else {
            container = new GenericContainer<>(baseImageName);
        }

        return container;
    }

    /**
     * @throws Exception if an error occurred during the stop
     */
    public void stop() throws Exception
    {
        // Nothing else to do, TestContainers automatically stops the container
        if (ServletEngine.JETTY_STANDALONE == this.testConfiguration.getServletEngine()) {
            this.jettyStandaloneExecutor.stop();
        }

        // If the Clover database system property is setup, then copy back the data to the parent container if need be.
        if (CLOVER_DATABASE != null && !this.testConfiguration.getServletEngine().isOutsideDocker()
            && isInAContainer())
        {
            this.servletContainer.copyFileFromContainer(CLOVER_DATABASE, CLOVER_DATABASE);
        }
    }
}
