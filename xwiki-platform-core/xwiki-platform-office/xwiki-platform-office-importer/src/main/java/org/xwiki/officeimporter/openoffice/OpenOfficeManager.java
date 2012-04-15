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
package org.xwiki.officeimporter.openoffice;

import org.xwiki.component.annotation.Role;

/**
 * Component interface for managing openoffice server connection / process.
 * 
 * @version $Id$
 * @since 1.8RC3
 */
@Role
public interface OpenOfficeManager
{
    /**
     * Enum type used to represent the state of {@link OpenOfficeManager}.
     * 
     * @version $Id$
     * @since 1.9M1
     */
    public enum ManagerState
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
        private String stateDescription;

        /**
         * Enum constructor.
         * 
         * @param stateDescription description of current server state.
         */
        private ManagerState(String stateDescription)
        {
            this.stateDescription = stateDescription;
        }

        @Override
        public String toString()
        {
            return this.stateDescription;
        }
    }

    /**
     * @return current state of {@link OpenOfficeManager}.
     */
    ManagerState getState();

    /**
     * If an internally managed openoffice server is configured (xwiki.properties), this method will start an openoffice
     * server process and connect to it. Otherwise this method will try to connect to an external openoffice server
     * instance configured through xwiki.properties. Calling {@link OpenOfficeManager#start()} on an already started /
     * connected {@link OpenOfficeManager} has no effect.
     * 
     * @throws OpenOfficeManagerException if the start operation fails.
     */
    void start() throws OpenOfficeManagerException;

    /**
     * If an internally managed openoffice server is configured (xwiki.properties), this method will disconnect from the
     * openoffice server and terminate the server process. Otherwise this method will simply disconnect from the
     * external openoffice server. Calling {@link OpenOfficeManager#stop()} on an already stopped / disconnected
     * {@link OpenOfficeManager} has no effect.
     * 
     * @throws OpenOfficeManagerException if stop operation fails.
     */
    void stop() throws OpenOfficeManagerException;

    /**
     * @return {@link OpenOfficeConverter} instance suitable for performing document conversion tasks.
     */
    OpenOfficeConverter getConverter();
}
