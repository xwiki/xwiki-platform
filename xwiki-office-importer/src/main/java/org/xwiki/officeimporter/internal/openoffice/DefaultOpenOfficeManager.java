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

import org.artofsolving.jodconverter.OfficeDocumentConverter;
import org.artofsolving.jodconverter.office.DefaultOfficeManagerConfiguration;
import org.artofsolving.jodconverter.office.ExternalOfficeManagerConfiguration;
import org.artofsolving.jodconverter.office.OfficeManager;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.container.Container;
import org.xwiki.officeimporter.openoffice.OpenOfficeConfiguration;
import org.xwiki.officeimporter.openoffice.OpenOfficeConverter;
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
    private OpenOfficeConfiguration conf;

    /**
     * Used to query global temporary working directory.
     */
    @Requirement
    private Container container;

    /**
     * Internal {@link OfficeManager} used to control / connect openoffice server.
     */
    private OfficeManager jodOOManager;

    /**
     * Internal {@link OfficeDocumentConverter} used to convert office documents.
     */
    private OfficeDocumentConverter jodConverter;

    /**
     * Current oo server process state.
     */
    private ManagerState state;

    /**
     * Used for carrying out document conversion tasks.
     */
    private OpenOfficeConverter converter;

    /**
     * Default constructor.
     */
    public DefaultOpenOfficeManager()
    {
        setState(ManagerState.NOT_CONNECTED);
    }

    /**
     * Initialize JodConverter.
     * 
     * @throws OpenOfficeManagerException when failed to initialize
     */
    public void initialize() throws OpenOfficeManagerException
    {
        if (this.conf.getServerType() == OpenOfficeConfiguration.SERVER_TYPE_INTERNAL) {
            File officeHome = new File(this.conf.getHomePath());
            File officeProfile = new File(this.conf.getProfilePath());
            DefaultOfficeManagerConfiguration configuration = new DefaultOfficeManagerConfiguration();
            configuration.setPortNumber(this.conf.getServerPort());
            configuration.setOfficeHome(officeHome);
            configuration.setTemplateProfileDir(officeProfile);
            configuration.setMaxTasksPerProcess(this.conf.getMaxTasksPerProcess());
            configuration.setTaskExecutionTimeout(this.conf.getTaskExecutionTimeout());
            this.jodOOManager = configuration.buildOfficeManager();
        } else if (conf.getServerType() == OpenOfficeConfiguration.SERVER_TYPE_EXTERNAL_LOCAL) {
            ExternalOfficeManagerConfiguration externalProcessOfficeManager = new ExternalOfficeManagerConfiguration();
            externalProcessOfficeManager.setPortNumber(this.conf.getServerPort());
            externalProcessOfficeManager.setConnectOnStart(true);
            this.jodOOManager = externalProcessOfficeManager.buildOfficeManager();
        } else {
            setState(ManagerState.CONF_ERROR);
            throw new OpenOfficeManagerException("Invalid openoffice server configuration.");
        }
        this.jodConverter = new OfficeDocumentConverter(this.jodOOManager);
        File workDir = container.getApplicationContext().getTemporaryDirectory();
        this.converter = new DefaultOpenOfficeConverter(this.jodConverter, workDir, getLogger());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.officeimporter.openoffice.OpenOfficeManager#getState()
     */
    public ManagerState getState()
    {
        return this.state;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.officeimporter.openoffice.OpenOfficeManager#start()
     */
    public void start() throws OpenOfficeManagerException
    {
        if (!checkState(ManagerState.CONNECTED)) {
            initialize();
            try {
                this.jodOOManager.start();
                setState(ManagerState.CONNECTED);
                getLogger().info("Open Office instance started.");
            } catch (Exception ex) {
                setState(ManagerState.ERROR);
                throw new OpenOfficeManagerException("Error while connecting / starting openoffice.", ex);
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.officeimporter.openoffice.OpenOfficeManager#stop()
     */
    public void stop() throws OpenOfficeManagerException
    {
        if (checkState(ManagerState.CONNECTED)) {
            try {
                this.jodOOManager.stop();
                setState(ManagerState.NOT_CONNECTED);
                getLogger().info("Open Office instance stopped.");
            } catch (Exception ex) {
                setState(ManagerState.ERROR);
                throw new OpenOfficeManagerException("Error while disconnecting / shutting down openoffice.", ex);
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.officeimporter.openoffice.OpenOfficeManager#getConverter()
     */
    public OpenOfficeConverter getConverter()
    {
        return this.converter;
    }

    /**
     * Utility method for setting the current OpenOffice manager state.
     * 
     * @param newState new state.
     */
    private void setState(ManagerState newState)
    {
        this.state = newState;
    }

    /**
     * Utility method for checking the OpenOffice manager state.
     * 
     * @param expectedState expected state.
     * @return true if OpenOffice manger is in given state, false otherwise.
     */
    private boolean checkState(ManagerState expectedState)
    {
        return (this.state == expectedState);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.officeimporter.openoffice.OpenOfficeManager#getDocumentConverter()
     */
    @Deprecated
    public OfficeDocumentConverter getDocumentConverter()
    {
        return this.jodConverter;
    }
}
