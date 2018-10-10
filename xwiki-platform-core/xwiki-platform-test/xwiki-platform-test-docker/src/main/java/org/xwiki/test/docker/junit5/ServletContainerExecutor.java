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
 * @since 10.9RC1
 */
public class ServletContainerExecutor
{
    private static final String TOMCAT_WEBAPP_DIR = "/usr/local/tomcat/webapps/xwiki";

    /**
     * @param configuration the configuration to build (servlet engine, debug mode, etc)
     * @param sourceWARDirectory the location where the built WAR is located
     * @return the Docker container instance
     * @throws Exception if an error occurred during the build or start
     */
    public GenericContainer execute(UITest configuration, File sourceWARDirectory) throws Exception
    {
        GenericContainer servletContainer;
        switch (configuration.servletEngine()) {
            case TOMCAT:
                // Configure Tomcat logging for debugging
                File logFile = new File(sourceWARDirectory, "WEB-INF/classes/logging.properties");
                try (FileWriter writer = new FileWriter(logFile)) {
                    IOUtils.write("org.apache.catalina.core.ContainerBase.[Catalina].level = FINE\n"
                        + "org.apache.catalina.core.ContainerBase.[Catalina].handlers = "
                        + "java.util.logging.ConsoleHandler\n", writer);
                }

                servletContainer = new GenericContainer<>("tomcat:latest");

                // File mounting is awfully slow on Mac OSX. For example starting Tomcat with XWiki mounted takes
                // 45s+, while doing a COPY first and then starting Tomcat takes 8s (+5s for the copy).
                String osName = System.getProperty("os.name").toLowerCase();
                if (osName.startsWith("mac os x")) {
                    MountableFile xwikiDirectory = MountableFile.forHostPath(sourceWARDirectory.toString());
                    servletContainer.withCopyFileToContainer(xwikiDirectory, TOMCAT_WEBAPP_DIR);
                } else {
                    servletContainer.withFileSystemBind(sourceWARDirectory.toString(), TOMCAT_WEBAPP_DIR);
                }

                break;
            default:
                throw new RuntimeException(String.format("Servlet engine [%s] is not yet supported!",
                    configuration.servletEngine()));
        }

        // Note: Testcontainers will wait for up to 60 seconds for the container's first mapped network port to start
        // listening.
        servletContainer
            .withNetwork(Network.SHARED)
            .withNetworkAliases("xwikiweb")
            .withExposedPorts(8080)
            .withEnv("CATALINA_OPTS", "-Xmx1024m "
                + "-Dorg.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true "
                + "-Dorg.apache.catalina.connector.CoyoteAdapter.ALLOW_BACKSLASH=true "
                + "-Dsecurerandom.source=file:/dev/urandom")
            .waitingFor(
                Wait.forHttp("/xwiki/bin/get/Main/WebHome")
                    .forStatusCode(200).withStartupTimeout(Duration.of(480, SECONDS)));

        if (configuration.debug()) {
            servletContainer.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(this.getClass())));
        }

        servletContainer.start();

        return servletContainer;
    }
}
