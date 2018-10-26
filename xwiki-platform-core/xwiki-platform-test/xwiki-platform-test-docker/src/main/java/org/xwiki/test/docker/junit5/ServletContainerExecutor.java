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
package org.xwiki.test.docker.junit5;

import java.io.File;
import java.io.FileWriter;
import java.time.Duration;

import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import static java.time.temporal.ChronoUnit.SECONDS;

/**
 * Create and execute the Docker Servlet engine container for the tests.
 *
 * @version $Id$
 * @since 10.9
 */
public class ServletContainerExecutor
{
    /**
     * @param testConfiguration the configuration to build (servlet engine, debug mode, etc)
     * @param sourceWARDirectory the location where the built WAR is located
     * @return the Docker container instance
     * @throws Exception if an error occurred during the build or start
     */
    public GenericContainer execute(TestConfiguration testConfiguration, File sourceWARDirectory) throws Exception
    {
        GenericContainer servletContainer;
        switch (testConfiguration.getServletEngine()) {
            case TOMCAT:
                // Configure Tomcat logging for debugging. Create a logging.properties file
                File logFile = new File(sourceWARDirectory, "WEB-INF/classes/logging.properties");
                logFile.createNewFile();
                try (FileWriter writer = new FileWriter(logFile)) {
                    IOUtils.write("org.apache.catalina.core.ContainerBase.[Catalina].level = FINE\n"
                        + "org.apache.catalina.core.ContainerBase.[Catalina].handlers = "
                        + "java.util.logging.ConsoleHandler\n", writer);
                }

                servletContainer = new GenericContainer<>("tomcat:latest");
                setWebappMount(servletContainer, sourceWARDirectory, "/usr/local/tomcat/webapps/xwiki");

                servletContainer.withEnv("CATALINA_OPTS", "-Xmx1024m "
                    + "-Dorg.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true "
                    + "-Dorg.apache.catalina.connector.CoyoteAdapter.ALLOW_BACKSLASH=true "
                    + "-Dsecurerandom.source=file:/dev/urandom");

                break;
            case JETTY:
                servletContainer = new GenericContainer<>("jetty:latest");
                setWebappMount(servletContainer, sourceWARDirectory, "/var/lib/jetty/webapps/xwiki");

                break;
            default:
                throw new RuntimeException(String.format("Servlet engine [%s] is not yet supported!",
                    testConfiguration.getServletEngine()));
        }

        // Note: TestContainers will wait for up to 60 seconds for the container's first mapped network port to start
        // listening.
        servletContainer
            .withNetwork(Network.SHARED)
            .withNetworkAliases("xwikiweb")
            .withExposedPorts(8080)
            .waitingFor(
                Wait.forHttp("/xwiki/bin/get/Main/WebHome")
                    .forStatusCode(200).withStartupTimeout(Duration.of(480, SECONDS)));

        if (testConfiguration.isDebug()) {
            servletContainer.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(this.getClass())));
        }

        servletContainer.start();

        return servletContainer;
    }

    private void setWebappMount(GenericContainer servletContainer, File sourceWARDirectory, String webappDirectory)
    {
        // File mounting is awfully slow on Mac OSX. For example starting Tomcat with XWiki mounted takes
        // 45s+, while doing a COPY first and then starting Tomcat takes 8s (+5s for the copy).
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.startsWith("mac os x")) {
            MountableFile xwikiDirectory = MountableFile.forHostPath(sourceWARDirectory.toString());
            servletContainer.withCopyFileToContainer(xwikiDirectory, webappDirectory);
        } else {
            servletContainer.withFileSystemBind(sourceWARDirectory.toString(), webappDirectory);
        }
    }
}
