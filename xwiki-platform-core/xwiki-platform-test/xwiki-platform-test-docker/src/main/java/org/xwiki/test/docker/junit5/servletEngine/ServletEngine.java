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

import org.xwiki.test.docker.junit5.TestConfiguration;

/**
 * The Servlet Engine to use for the UI tests.
 *
 * @version $Id$
 * @since 10.9
 */
public enum ServletEngine
{
    /**
     * The browser is selected based on a system property value.
     * @see TestConfiguration
     */
    SYSTEM,

    /**
     * Represents the Tomcat Servlet engine.
     */
    TOMCAT,

    /**
     * Represents the Jetty Servlet engine (running inside Docker).
     */
    JETTY,

    /**
     * Represents the Jetty Servlet engine but running outside of Docker.
     */
    JETTY_STANDALONE(true),

    /**
     * Represents the JBoss Wildfly engine.
     */
    WILDFLY,

    /**
     * Represents an external Servlet Engine already configured and running (we won't start or stop it).
     */
    EXTERNAL(true);

    private static final String PERMANENT_DIRECTORY = "/var/local/xwiki";

    private static final String LOCALHOST = "localhost";

    private static final String HOST_INTERNAL = "host.testcontainers.internal";

    private boolean isOutsideDocker;

    private String ip;

    private int port;

    ServletEngine()
    {
        // By default all servlet engines run inside docker containers.
    }

    ServletEngine(boolean isOutsideDocker)
    {
        this.isOutsideDocker = isOutsideDocker;
    }

    /**
     * @return true if the Servlet engine is meant to be running on the host and not in a Docker container
     */
    public boolean isOutsideDocker()
    {
        return this.isOutsideDocker;
    }

    /**
     * @param ip see {@link #getIP()}
     * @since 10.11RC1
     */
    public void setIP(String ip)
    {
        this.ip = ip;
    }

    /**
     * @return the IP address to use to connect to the Servlet Engine from the outside
     *         (it is different if it runs locally or in a Docker container).
     * @since 10.11RC1
     */
    public String getIP()
    {
        return this.ip;
    }

    /**
     * @param port see {@link #getPort()}
     * @since 10.11RC1
     */
    public void setPort(int port)
    {
        this.port = port;
    }

    /**
     * @return the port to use to connect to the Servlet Engine from the outside
     *         (it is different if it runs locally or in a Docker container)
     * @since 10.11RC1
     */
    public int getPort()
    {
        return this.port;
    }

    /**
     * @return the IP to the host from inside the Servlet Engine
     * @since 10.11RC1
     */
    public String getHostIP()
    {
        return isOutsideDocker() ? LOCALHOST : HOST_INTERNAL;
    }

    /**
     * @return the IP of the container from inside itself (it is different if it runs locally or in a Docker container)
     * @since 10.11RC1
     */
    public String getInternalIP()
    {
        return isOutsideDocker ? HOST_INTERNAL : "xwikiweb";
    }

    /**
     * @return the port of the container from inside itself (it is different if it runs locally or in a Docker
     *         container)
     * @since 10.11RC1
     */
    public int getInternalPort()
    {
        return 8080;
    }

    /**
     * @return the location of the XWiki permanent directory inside the Docker container for the Servlet Engine.
     * @since 10.11RC1
     */
    public String getPermanentDirectory()
    {
        return PERMANENT_DIRECTORY;
    }
}
