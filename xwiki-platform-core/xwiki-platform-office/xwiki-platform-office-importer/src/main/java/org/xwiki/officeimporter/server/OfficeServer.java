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
package org.xwiki.officeimporter.server;

import org.xwiki.component.annotation.Role;
import org.xwiki.officeimporter.converter.OfficeConverter;

/**
 * Component interface for managing the office server connection / process.
 * 
 * @version $Id$
 * @since 5.0M2
 */
@Role
public interface OfficeServer
{
    /**
     * Enumeration used to represent the office server state.
     */
    public enum ServerState
    {
        /**
         * Connected.
         */
        CONNECTED("Connected"),

        /**
         * Not connected.
         */
        NOT_CONNECTED("Not connected"),

        /**
         * Configuration error.
         */
        CONF_ERROR("Invalid configuration"),

        /**
         * Error.
         */
        ERROR("Error");

        /**
         * Description of current server state.
         */
        private String description;

        /**
         * Creates a new instance.
         * 
         * @param description description of current server state
         */
        private ServerState(String description)
        {
            this.description = description;
        }

        @Override
        public String toString()
        {
            return this.description;
        }
    }

    /**
     * @return current server state
     */
    ServerState getState();

    /**
     * If an internally managed office server is configured (xwiki.properties), this method will start an office server
     * process and connect to it. Otherwise this method will try to connect to an external office server instance
     * configured through xwiki.properties. Calling {@link #start()} on an already started / connected server has no
     * effect.
     * 
     * @throws OfficeServerException if the start operation fails
     */
    void start() throws OfficeServerException;

    /**
     * If an internally managed office server is configured (xwiki.properties), this method will disconnect from the
     * office server and terminate the server process. Otherwise this method will simply disconnect from the external
     * office server. Calling {@link #stop()} on an already stopped / disconnected server has no effect.
     * 
     * @throws OfficeServerException if stop operation fails
     */
    void stop() throws OfficeServerException;

    /**
     * @return {@link OfficeConverter} instance suitable for performing document conversion tasks
     */
    OfficeConverter getConverter();
}
