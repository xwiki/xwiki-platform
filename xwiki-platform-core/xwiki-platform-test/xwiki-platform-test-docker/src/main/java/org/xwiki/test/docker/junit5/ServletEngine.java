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
    JETTY_STANDALONE(true);

    private boolean isOutsideDocker;

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
}
