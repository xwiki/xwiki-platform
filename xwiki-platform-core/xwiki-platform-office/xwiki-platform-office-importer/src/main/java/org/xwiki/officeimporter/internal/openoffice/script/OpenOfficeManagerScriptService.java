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
package org.xwiki.officeimporter.internal.openoffice.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.ModelContext;
import org.xwiki.officeimporter.openoffice.OpenOfficeConfiguration;
import org.xwiki.officeimporter.openoffice.OpenOfficeManager;
import org.xwiki.officeimporter.openoffice.OpenOfficeManagerException;
import org.xwiki.script.service.ScriptService;

/**
 * Exposes the office manager APIs to server-side scripts.
 * 
 * @version $Id$
 * @since 4.1M1
 */
@Component
@Named("officemanager")
@Singleton
public class OpenOfficeManagerScriptService implements ScriptService
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
     * The object used to log messages.
     */
    @Inject
    private Logger logger;

    /**
     * Provides access to the request context.
     */
    @Inject
    private Execution execution;

    /**
     * The component used to access the current wiki.
     */
    @Inject
    private ModelContext modelContext;

    /**
     * The {@link OpenOfficeManager} component.
     */
    @Inject
    private OpenOfficeManager ooManager;

    /**
     * The {@link DocumentAccessBridge} component.
     */
    @Inject
    private DocumentAccessBridge docBridge;

    /**
     * The office server configuration.
     */
    @Inject
    private OpenOfficeConfiguration config;

    /**
     * Tries to start the oo server process.
     * 
     * @return true if the operation succeeds, false otherwise
     */
    public boolean startServer()
    {
        if (!isMainXWiki()) {
            setErrorMessage(ERROR_FORBIDDEN);
        } else if (!this.docBridge.hasProgrammingRights()) {
            setErrorMessage(ERROR_PRIVILEGES);
        } else {
            try {
                this.ooManager.start();
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
     * @return true if the operation succeeds, false otherwise
     */
    public boolean stopServer()
    {
        if (!isMainXWiki()) {
            setErrorMessage(ERROR_FORBIDDEN);
        } else if (!this.docBridge.hasProgrammingRights()) {
            setErrorMessage(ERROR_PRIVILEGES);
        } else {
            try {
                this.ooManager.stop();
                return true;
            } catch (OpenOfficeManagerException ex) {
                logger.error(ex.getMessage(), ex);
                setErrorMessage(ex.getMessage());
            }
        }
        return false;
    }

    /**
     * @return current status of the oo server process as a string
     */
    public String getServerState()
    {
        return this.ooManager.getState().toString();
    }

    /**
     * @return the office server configuration
     */
    public OpenOfficeConfiguration getConfig()
    {
        return config;
    }

    /**
     * @return any error messages encountered
     */
    public String getLastErrorMessage()
    {
        Object error = this.execution.getContext().getProperty(OFFICE_MANAGER_ERROR);
        return (error != null) ? (String) error : null;
    }

    /**
     * Sets an error message inside the execution context.
     * 
     * @param message error message
     */
    private void setErrorMessage(String message)
    {
        this.execution.getContext().setProperty(OFFICE_MANAGER_ERROR, message);
    }

    /**
     * Utility method for checking if current context document is from main xwiki.
     * 
     * @return true if the current context document is from main xwiki
     */
    private boolean isMainXWiki()
    {
        String currentWiki = this.modelContext.getCurrentEntityReference().getName();
        // TODO: Remove the hard-coded main wiki name when a fix becomes available.
        return (currentWiki != null) && currentWiki.equals("xwiki");
    }
}
