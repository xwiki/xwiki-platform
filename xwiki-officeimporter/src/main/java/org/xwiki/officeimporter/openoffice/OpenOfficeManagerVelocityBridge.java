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

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.logging.Logger;
import org.xwiki.context.Execution;

/**
 * A bridge between {@link OpenOfficeManager} and velocity scripts.
 * 
 * @version $Id$
 * @since 1.8RC3
 */
public class OpenOfficeManagerVelocityBridge
{
    /**
     * The key used to place any error messages while trying to control the oo server instance.
     */
    public static final String OFFICE_MANAGER_ERROR = "OFFICE_MANAGER_ERROR";

    /**
     * Error message used to indicate that openoffice server administration is restricted for main xwiki.
     */
    private static final String ERROR_FORBIDDEN = "OpenOffice server administration is forbidden for sub-wikis.";

    /**
     * Error message used to indicate that the current user does not have enough rights to perform the requested action.
     */
    private static final String ERROR_PRIVILEGES = "Inadequate privileges.";

    /**
     * Provides access to the request context.
     */
    private Execution execution;

    /**
     * The {@link OpenOfficeManager} component.
     */
    private OpenOfficeManager ooManager;

    /**
     * The {@link DocumentAccessBridge} component.
     */
    private DocumentAccessBridge docBridge;

    /**
     * The logger instance passed by the velocity context initializer.
     */
    private Logger logger;

    /**
     * Creates a new {@link OpenOfficeManagerVelocityBridge} with the provided {@link OpenOfficeManager} component.
     * 
     * @param oomanager openoffice manager component.
     * @param docBridge document access bridge component.
     * @param execution current execution.
     * @param logger logger.
     */
    public OpenOfficeManagerVelocityBridge(OpenOfficeManager oomanager, DocumentAccessBridge docBridge,
        Execution execution, Logger logger)
    {
        this.ooManager = oomanager;
        this.docBridge = docBridge;
        this.execution = execution;
        this.logger = logger;
    }

    /**
     * Tries to start the oo server process.
     * 
     * @return true if the operation succeeds, false otherwise.
     */
    public boolean startServer()
    {
        if (!isMainXWiki()) {
            setErrorMessage(ERROR_FORBIDDEN);
        } else if (!docBridge.hasProgrammingRights()) {
            setErrorMessage(ERROR_PRIVILEGES);
        } else {
            try {
                ooManager.start();
                return true;
            } catch (OpenOfficeManagerException ex) {
                logger.error(ex.getMessage(), ex);
                setErrorMessage(ex.getMessage());
            }
        }
        return false;
    }

    /**
     * Tries to stop the oo server process.
     * 
     * @return true if the operation succeeds, false otherwise.
     */
    public boolean stopServer()
    {
        if (!isMainXWiki()) {
            setErrorMessage(ERROR_FORBIDDEN);
        } else if (!docBridge.hasProgrammingRights()) {
            setErrorMessage(ERROR_PRIVILEGES);
        } else {
            try {
                ooManager.stop();
                return true;
            } catch (OpenOfficeManagerException ex) {
                logger.error(ex.getMessage(), ex);
                setErrorMessage(ex.getMessage());
            }
        }
        return false;
    }

    /**
     * @return current status of the oo server process as a string.
     */
    public String getServerState()
    {
        return ooManager.getState().toString();
    }

    /**
     * @return any error messages encountered.
     */
    public String getLastErrorMessage()
    {
        Object error = execution.getContext().getProperty(OFFICE_MANAGER_ERROR);
        return (error != null) ? (String) error : null;
    }

    /**
     * Sets an error message inside the execution context.
     * 
     * @param message error message.
     */
    private void setErrorMessage(String message)
    {
        execution.getContext().setProperty(OFFICE_MANAGER_ERROR, message);
    }

    /**
     * Utility method for checking if current context document is from main xwiki.
     * 
     * @return true if the current context document is from main xwiki.
     */
    private boolean isMainXWiki()
    {
        String currentWiki = docBridge.getCurrentWiki();
        // TODO: Remove the hard-coded main wiki name when a fix becomes available.
        return (currentWiki != null) && currentWiki.equals("xwiki");
    }
}
