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

import net.sf.jodconverter.OfficeDocumentConverter;

/**
 * Component interface for managing the oo server process.
 * 
 * @version $Id$
 * @since 1.8RC3
 */
public interface OpenOfficeServerManager
{
    /**
     * This component's role, used when code needs to look it up.
     */
    String ROLE = OpenOfficeServerManager.class.getName();

    /**
     * Enum type used to represent the state of the oo server process.
     * 
     * @version $Id$
     * @since 1.9M1
     */
    enum ServerState
    {
        /**
         * Running.
         */
        RUNNING("Running"),

        /**
         * Not running.
         */
        NOT_RUNNING("Not Running"),

        /**
         * Configuration error.
         */
        CONF_ERROR("Invalid configuration"),
        
        /**
         * Unknown server state.
         */
        UNKNOWN("Unknown");
        
        /**
         * Description of current server state.
         */
        private String stateDescription;
        
        /**
         * Enum constructor.
         * 
         * @param stateDescription description of current server state.
         */
        private ServerState(String stateDescription)
        {
            this.stateDescription = stateDescription;
        }
        
        /**
         * {@inheritDoc}
         */
        public String toString()
        {
            return this.stateDescription;
        }
    }

    /**
     * @return path to openoffice server installation.
     */
    String getOfficeHome();

    /**
     * @return path to openoffice execution profile.
     */
    String getOfficeProfile();

    /**
     * @return current state of oo server process.
     */
    ServerState getServerState();

    /**
     * Starts the oo server with configuration details from xwiki.properties file (or default values).
     * 
     * @throws OOServerManagerException if OpenOfficeServerManagerExceptioner start operation fails.
     */
    void startServer() throws OpenOfficeServerManagerException;

    /**
     * Terminates the oo server instance.
     * 
     * @throws OOServerManagerException if OpenOfficeServerManagerExceptioner stop operation fails.
     */
    void stopServer() throws OpenOfficeServerManagerException;
    
    /**
     * @return a {@link OfficeDocumentConverter} associated with this oo server process.
     */
    OfficeDocumentConverter getDocumentConverter();
}
