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
package org.xwiki.officeimporter.internal.openoffice;

import java.io.File;

import net.sf.jodconverter.OfficeDocumentConverter;
import net.sf.jodconverter.office.ManagedProcessOfficeManager;
import net.sf.jodconverter.office.OfficeException;
import net.sf.jodconverter.office.OfficeManager;

import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.officeimporter.openoffice.OpenOfficeServerConfiguration;
import org.xwiki.officeimporter.openoffice.OpenOfficeServerManager;
import org.xwiki.officeimporter.openoffice.OpenOfficeServerManagerException;

/**
 * Default implementation of {@link OpenOfficeServerManager} component.
 * 
 * @version $Id$
 * @since 1.8RC3
 */
public class DefaultOpenOfficeServerManager extends AbstractLogEnabled implements OpenOfficeServerManager,
    Initializable
{
    /**
     * The {@link OpenOfficeServerConfiguration} component.
     */
    private OpenOfficeServerConfiguration configuration;

    /**
     * Current oo server process state.
     */
    private ServerState currentState;

    /**
     * The {@link OfficeManager} used to control the openoffice server instance.
     */
    private OfficeManager officeManager;

    /**
     * Flag indicating whether the officeManager is initialized or not.
     */
    private boolean officeManagerInitialized;

    /**
     * The {@link OfficeDocumentConverter} used to convert office documents.
     */
    private OfficeDocumentConverter documentConverter;

    /**
     * {@inheritDoc}
     */
    public void initialize() throws InitializationException
    {
        currentState = ServerState.NOT_RUNNING;
        // Make sure there is no openoffice process left when XE shuts down.
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            public void run()
            {
                try {
                    stopServer();
                } catch (OpenOfficeServerManagerException ex) {
                    // Nothing to do.
                }
            }
        });
    }

    /**
     * Initializes the internal {@link OfficeManager}.
     */
    private void initializeOfficeManager() throws OpenOfficeServerManagerException
    {
        File officeHome = new File(getOfficeHome());
        File officeProfile = new File(getOfficeProfile());
        try {
            ManagedProcessOfficeManager managedProcessOfficeManager =
                new ManagedProcessOfficeManager(officeHome, officeProfile);
            managedProcessOfficeManager.setMaxTasksPerProcess(configuration.getMaxTasksPerProcess());
            managedProcessOfficeManager.setTaskExecutionTimeout(configuration.getTaskExecutionTimeout());
            this.officeManager = managedProcessOfficeManager;
            this.documentConverter = new OfficeDocumentConverter(officeManager);
            setOfficeManagerInitialized(true);
        } catch (IllegalArgumentException ex) {
            currentState = ServerState.CONF_ERROR;
            throw new OpenOfficeServerManagerException("Error while initializing OpenOffice server.", ex);
        }
    }

    /**
     * @return whether the officeManager is initialized or not.
     */
    private boolean isOfficeManagerInitialized()
    {
        return officeManagerInitialized;
    }

    /**
     * @param initialized state of the officeManager to be set.
     */
    private void setOfficeManagerInitialized(boolean initialized)
    {
        this.officeManagerInitialized = initialized;
    }

    /**
     * {@inheritDoc}
     */
    public String getOfficeHome()
    {
        return configuration.getHomePath();
    }

    /**
     * {@inheritDoc}
     */
    public String getOfficeProfile()
    {
        return configuration.getProfilePath();
    }

    /**
     * {@inheritDoc}
     */
    public ServerState getServerState()
    {
        return currentState;
    }

    /**
     * {@inheritDoc}
     */
    public void startServer() throws OpenOfficeServerManagerException
    {
        if (ServerState.UNKNOWN != currentState) {
            if (!isOfficeManagerInitialized()) {
                initializeOfficeManager();
            }
            try {
                officeManager.start();
                currentState = ServerState.RUNNING;
            } catch (OfficeException ex) {
                currentState = ServerState.UNKNOWN;
                throw new OpenOfficeServerManagerException("Error while starting OpenOffice server.", ex);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stopServer() throws OpenOfficeServerManagerException
    {
        if (ServerState.RUNNING == currentState) {
            try {
                officeManager.stop();
                currentState = ServerState.NOT_RUNNING;
            } catch (OfficeException ex) {
                currentState = ServerState.UNKNOWN;
                throw new OpenOfficeServerManagerException("Error while shutting down OpenOffice server.", ex);
            } finally {
                setOfficeManagerInitialized(false);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public OfficeDocumentConverter getDocumentConverter()
    {
        return this.documentConverter;
    }
}
