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
import net.sf.jodconverter.office.ExternalProcessOfficeManager;
import net.sf.jodconverter.office.ManagedProcessOfficeManager;
import net.sf.jodconverter.office.ManagedProcessOfficeManagerConfiguration;
import net.sf.jodconverter.office.OfficeConnectionMode;
import net.sf.jodconverter.office.OfficeException;
import net.sf.jodconverter.office.OfficeManager;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.officeimporter.openoffice.OpenOfficeConfiguration;
import org.xwiki.officeimporter.openoffice.OpenOfficeManager;
import org.xwiki.officeimporter.openoffice.OpenOfficeManagerException;

/**
 * Default implementation of {@link OpenOfficeManager} component.
 * 
 * @version $Id$
 * @since 1.8RC3
 */
@Component
public class DefaultOpenOfficeManager extends AbstractLogEnabled implements OpenOfficeManager
{
    /**
     * The {@link OpenOfficeConfiguration} component.
     */
    @Requirement
    private OpenOfficeConfiguration ooConfig;

    /**
     * The {@link OfficeManager} used to control / connect openoffice server.
     */
    private OfficeManager officeManager;
    
    /**
     * Current oo server process state.
     */
    private ManagerState currentState = ManagerState.NOT_CONNECTED;

    /**
     * Flag indicating whether the officeManager is initialized or not.
     */
    private boolean officeManagerInitialized;

    /**
     * The {@link OfficeDocumentConverter} used to convert office documents.
     */    
    private OfficeDocumentConverter documentConverter;

    /**
     * Initializes the internal {@link OfficeManager}.
     */
    private void initializeOfficeManager() throws OpenOfficeManagerException
    {
        OfficeConnectionMode connectionMode = OfficeConnectionMode.socket(ooConfig.getServerPort());
        if (ooConfig.getServerType() == OpenOfficeConfiguration.SERVER_TYPE_INTERNAL) {
            File officeHome = new File(ooConfig.getHomePath());
            File officeProfile = new File(ooConfig.getProfilePath());
            ManagedProcessOfficeManagerConfiguration configuration =
                new ManagedProcessOfficeManagerConfiguration(connectionMode);
            configuration.setOfficeHome(officeHome);
            configuration.setTemplateProfileDir(officeProfile);
            configuration.setMaxTasksPerProcess(ooConfig.getMaxTasksPerProcess());
            configuration.setTaskExecutionTimeout(ooConfig.getTaskExecutionTimeout());
            this.officeManager = new ManagedProcessOfficeManager(configuration);
        } else if (ooConfig.getServerType() == OpenOfficeConfiguration.SERVER_TYPE_EXTERNAL_LOCAL) {
            ExternalProcessOfficeManager externalProcessOfficeManager =
                new ExternalProcessOfficeManager(connectionMode);
            externalProcessOfficeManager.setConnectOnStart(true);
            this.officeManager = externalProcessOfficeManager;
        } else {
            this.currentState = ManagerState.CONF_ERROR;
            throw new OpenOfficeManagerException("Invalid configuration.");
        }
        this.documentConverter = new OfficeDocumentConverter(officeManager);
        this.officeManagerInitialized = true;
    }

    /**
     * {@inheritDoc}
     */
    public ManagerState getState()
    {
        return currentState;
    }

    /**
     * {@inheritDoc}
     */
    public OfficeDocumentConverter getDocumentConverter()
    {
        return this.documentConverter;
    }

    /**
     * {@inheritDoc}
     */
    public void start() throws OpenOfficeManagerException
    {
        if (ManagerState.CONNECTED != currentState) {
            if (!officeManagerInitialized) {
                initializeOfficeManager();
            }
            try {
                officeManager.start();
                currentState = ManagerState.CONNECTED;
                getLogger().info("Open Office instance started.");
            } catch (OfficeException ex) {
                currentState = ManagerState.ERROR;
                throw new OpenOfficeManagerException("Error while connecting / starting openoffice.", ex);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stop() throws OpenOfficeManagerException
    {
        if (ManagerState.CONNECTED == currentState) {
            try {
                officeManager.stop();
                currentState = ManagerState.NOT_CONNECTED;
                getLogger().info("Open Office instance stopped.");
            } catch (OfficeException ex) {
                currentState = ManagerState.ERROR;
                throw new OpenOfficeManagerException("Error while disconnecting / shutting down openoffice.", ex);
            } finally {
                this.officeManagerInitialized = false;
            }
        }
    }
}
