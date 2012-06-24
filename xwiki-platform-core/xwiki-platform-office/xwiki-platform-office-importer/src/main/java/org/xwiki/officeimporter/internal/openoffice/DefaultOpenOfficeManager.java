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
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.artofsolving.jodconverter.OfficeDocumentConverter;
import org.artofsolving.jodconverter.document.JsonDocumentFormatRegistry;
import org.artofsolving.jodconverter.office.DefaultOfficeManagerConfiguration;
import org.artofsolving.jodconverter.office.ExternalOfficeManagerConfiguration;
import org.artofsolving.jodconverter.office.OfficeManager;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.environment.Environment;
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
@Singleton
public class DefaultOpenOfficeManager implements OpenOfficeManager
{
    /**
     * The path to the file that can be used to configure the office document conversion.
     */
    private static final String DOCUMENT_FORMATS_PATH = "/document-formats.js";

    /**
     * The {@link OpenOfficeConfiguration} component.
     */
    @Inject
    private OpenOfficeConfiguration conf;

    /**
     * Used to query global temporary working directory.
     */
    @Inject
    private Environment environment;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

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
            DefaultOfficeManagerConfiguration configuration = new DefaultOfficeManagerConfiguration();
            configuration.setPortNumber(this.conf.getServerPort());

            String homePath = this.conf.getHomePath();
            if (homePath != null) {
                configuration.setOfficeHome(homePath);
            }

            String profilePath = this.conf.getProfilePath();
            if (profilePath != null) {
                configuration.setTemplateProfileDir(new File(profilePath));
            }

            configuration.setMaxTasksPerProcess(this.conf.getMaxTasksPerProcess());
            configuration.setTaskExecutionTimeout(this.conf.getTaskExecutionTimeout());
            this.jodOOManager = configuration.buildOfficeManager();
        } else if (this.conf.getServerType() == OpenOfficeConfiguration.SERVER_TYPE_EXTERNAL_LOCAL) {
            ExternalOfficeManagerConfiguration externalProcessOfficeManager = new ExternalOfficeManagerConfiguration();
            externalProcessOfficeManager.setPortNumber(this.conf.getServerPort());
            externalProcessOfficeManager.setConnectOnStart(true);
            this.jodOOManager = externalProcessOfficeManager.buildOfficeManager();
        } else {
            setState(ManagerState.CONF_ERROR);
            throw new OpenOfficeManagerException("Invalid openoffice server configuration.");
        }

        this.jodConverter = null;
        // Try to use the JSON document format registry to configure the office document conversion.
        InputStream input = getClass().getResourceAsStream(DOCUMENT_FORMATS_PATH);
        if (input != null) {
            try {
                this.jodConverter =
                    new OfficeDocumentConverter(this.jodOOManager, new JsonDocumentFormatRegistry(input));
            } catch (Exception e) {
                this.logger.warn("Failed to parse {} . The default document format registry will be used instead.",
                    DOCUMENT_FORMATS_PATH, e);
            }
        } else {
            this.logger.debug("{} is missing. The default document format registry will be used instead.",
                DOCUMENT_FORMATS_PATH);
        }
        if (this.jodConverter == null) {
            // Use the default document format registry.
            this.jodConverter = new OfficeDocumentConverter(this.jodOOManager);
        }

        File workDir = this.environment.getTemporaryDirectory();
        this.converter = new DefaultOpenOfficeConverter(this.jodConverter, workDir);
    }

    @Override
    public ManagerState getState()
    {
        return this.state;
    }

    @Override
    public void start() throws OpenOfficeManagerException
    {
        // If the OpenOffice server is running then stop it in order to restart the connection.
        stop();

        initialize();
        try {
            this.jodOOManager.start();
            setState(ManagerState.CONNECTED);
            this.logger.info("Open Office instance started.");
        } catch (Exception e) {
            setState(ManagerState.ERROR);
            throw new OpenOfficeManagerException("Error while connecting / starting openoffice.", e);
        }
    }

    @Override
    public void stop() throws OpenOfficeManagerException
    {
        // We should try stopping the OpenOffice server even if the status is not connected but we should not raise an
        // error if there is a failure to stop.
        boolean connected = checkState(ManagerState.CONNECTED);
        try {
            this.jodOOManager.stop();
            setState(ManagerState.NOT_CONNECTED);
            this.logger.info("Open Office instance stopped.");
        } catch (Exception e) {
            if (connected) {
                setState(ManagerState.ERROR);
                throw new OpenOfficeManagerException("Error while disconnecting / shutting down openoffice.", e);
            }
        }
    }

    @Override
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
}
