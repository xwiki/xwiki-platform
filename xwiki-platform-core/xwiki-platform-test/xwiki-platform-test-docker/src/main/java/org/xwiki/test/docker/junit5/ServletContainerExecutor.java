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

import static java.time.temporal.ChronoUnit.SECONDS;

/**
 * Create and execute the Docker Servlet engine container for the tests.
 *
 * @version $Id$
 * @since 10.9RC1
 */
public class ServletContainerExecutor
{
    /**
     * @param engine the servlet engine to build and start
     * @param sourceWARDirectory the location where the built WAR is located
     * @return the Docker container instance
     * @throws Exception if an error occurred during the build or start
     */
    public GenericContainer execute(ServletEngine engine, File sourceWARDirectory) throws Exception
    {
        GenericContainer servletContainer;
        switch (engine) {
            case TOMCAT:
                // Configure Tomcat logging for debugging
                File logFile = new File(sourceWARDirectory, "WEB-INF/classes/logging.properties");
                try (FileWriter writer = new FileWriter(logFile)) {
                    IOUtils.write("org.apache.catalina.core.ContainerBase.[Catalina].level = FINE\n"
                        + "org.apache.catalina.core.ContainerBase.[Catalina].handlers = "
                        + "java.util.logging.ConsoleHandler\n", writer);
                }

                // docker run --net=xwiki-nw --name xwiki -p 8080:8080 -v /my/own/xwiki:/usr/local/xwiki
                //     -e DB_USER=xwiki -e DB_PASSWORD=xwiki -e DB_DATABASE=xwiki -e DB_HOST=mysql-xwiki
                //     xwiki:mysql-tomcat
                // docker run --net=xwiki-nw --name xwiki -p 8080:8080 -v /my/own/xwiki:/usr/local/xwiki
                //     -e DB_USER=xwiki -e DB_PASSWORD=xwiki -e DB_DATABASE=xwiki -e DB_HOST=postgres-xwiki
                //     xwiki:postgres-tomcat
                servletContainer = new GenericContainer<>("tomcat:latest")
                    // Map the XWiki WAR inside Tomcat
                    .withFileSystemBind(sourceWARDirectory.toString(), "/usr/local/tomcat/webapps/xwiki");

                break;
            default:
                throw new RuntimeException(String.format("Servlet engine [%s] is not yet supported!", engine));
        }

        // Note: Testcontainers will wait for up to 60 seconds for the container's first mapped network port to start
        // listening.
        servletContainer
            .withNetwork(Network.SHARED)
            .withNetworkAliases("xwikiweb")
            .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(this.getClass())))
            .withExposedPorts(8080)
            .withEnv("CATALINA_OPTS", "-Xmx1024m "
                + "-Dorg.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true "
                + "-Dorg.apache.catalina.connector.CoyoteAdapter.ALLOW_BACKSLASH=true "
                + "-Dsecurerandom.source=file:/dev/urandom")
            .waitingFor(
                Wait.forHttp("/xwiki/bin/get/Main/WebHome")
                    .forStatusCode(200).withStartupTimeout(Duration.of(480, SECONDS)))
            .start();

        return servletContainer;
    }
}
