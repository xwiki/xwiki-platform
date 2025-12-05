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
package org.xwiki.test.docker.junit5.servletengine;

import org.testcontainers.containers.GenericContainer;

/**
 * The Servlet Engine to use for the UI tests.
 *
 * @version $Id$
 * @since 10.9
 */
public enum ServletEngine
{
    /**
     * Represents the Tomcat Servlet engine.
     */
    TOMCAT("tomcat"),

    /**
     * Represents the Jetty Servlet engine (running inside Docker).
     */
    JETTY("jetty"),

    /**
     * Represents the Jetty Servlet engine but running outside of Docker.
     */
    JETTY_STANDALONE,

    /**
     * Represents the JBoss Wildfly engine.
     */
    WILDFLY("jboss/wildfly"),

    /**
     * Represents an external Servlet Engine already configured and running (we won't start or stop it).
     */
    EXTERNAL;

    private static final String LOCALHOST = "localhost";

    private boolean isOutsideDocker;

    private String dockerImageName;

    /**
     * Constructor for a Servlet Engine using a Docker image. This image must be available on DockerHub.
     * @param dockerImageName the name of the docker image to use.
     */
    ServletEngine(String dockerImageName)
    {
        this.isOutsideDocker = false;
        this.dockerImageName = dockerImageName;
    }

    /**
     * Constructor for an external ServletEngine: in that case, it is running outside docker.
     */
    ServletEngine()
    {
        this.isOutsideDocker = true;
    }

    /**
     * @return true if the Servlet engine is meant to be running on the host and not in a Docker container
     */
    public boolean isOutsideDocker()
    {
        return this.isOutsideDocker;
    }

    /**
     * @return the IP to the host from inside the Servlet Engine
     * @since 10.11RC1
     */
    public String getHostIP()
    {
        return isOutsideDocker() ? LOCALHOST : GenericContainer.INTERNAL_HOST_HOSTNAME;
    }

    /**
     * @return the location of the XWiki permanent directory inside the Docker container for the Servlet Engine.
     * @since 10.11RC1
     */
    public String getPermanentDirectory()
    {
        // Choose directories that can be created by the default user in the related containers.
        if (name().equals("JETTY")) {
            return "/var/lib/jetty/xwiki-data";
        } else if (name().equals("TOMCAT")) {
            return "/usr/local/tomcat/xwiki-data";
        } else if (name().equals("WILDFLY")) {
            return "/opt/jboss/xwiki-data";
        } else {
            throw new RuntimeException(String.format("Permanent directory not supported for [%s]", name()));
        }
    }

    /**
     * @return the Docker image name for this Servlet Engine. This image must be available on DockerHub.
     * @since 10.11RC1
     */
    public String getDockerImageName()
    {
        return this.dockerImageName;
    }
}
