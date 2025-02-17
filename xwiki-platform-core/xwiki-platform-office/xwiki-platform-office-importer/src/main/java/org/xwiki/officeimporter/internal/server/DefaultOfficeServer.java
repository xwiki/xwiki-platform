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
package org.xwiki.officeimporter.internal.server;

import java.io.File;
import java.io.InputStream;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jodconverter.core.document.JsonDocumentFormatRegistry;
import org.jodconverter.core.office.OfficeManager;
import org.jodconverter.local.LocalConverter;
import org.jodconverter.local.filter.text.LinkedImagesEmbedderFilter;
import org.jodconverter.local.office.ExternalOfficeManager;
import org.jodconverter.local.office.LocalOfficeManager;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.environment.Environment;
import org.xwiki.officeimporter.converter.OfficeConverter;
import org.xwiki.officeimporter.internal.converter.DefaultOfficeConverter;
import org.xwiki.officeimporter.server.OfficeServer;
import org.xwiki.officeimporter.server.OfficeServerConfiguration;
import org.xwiki.officeimporter.server.OfficeServerException;
import org.xwiki.text.StringUtils;

/**
 * Default {@link OfficeServer} implementation.
 * 
 * @version $Id$
 * @since 5.0M2
 */
@Component
@Singleton
public class DefaultOfficeServer implements OfficeServer
{
    /**
     * The path to the file that can be used to configure the office document conversion.
     */
    private static final String DOCUMENT_FORMATS_PATH = "/document-formats.js";

    /**
     * The office server configuration.
     */
    @Inject
    private OfficeServerConfiguration config;

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
     * Internal {@link OfficeManager} used to control / connect the office server.
     */
    private OfficeManager jodManager;

    /**
     * Internal {@link LocalConverter} used to convert office documents.
     */
    private LocalConverter jodConverter;

    /**
     * Current office server process state.
     */
    private ServerState state;

    /**
     * Used for carrying out document conversion tasks.
     */
    private OfficeConverter converter;

    /**
     * Default constructor.
     */
    public DefaultOfficeServer()
    {
        setState(ServerState.NOT_CONNECTED);
    }

    /**
     * Initialize JodConverter.
     * 
     * @throws OfficeServerException when failed to initialize
     */
    public void initialize() throws OfficeServerException
    {
        if (this.config.getServerType() == OfficeServerConfiguration.SERVER_TYPE_INTERNAL) {
            initializeInternalServer();
        } else if (this.config.getServerType() == OfficeServerConfiguration.SERVER_TYPE_EXTERNAL_LOCAL
                || this.config.getServerType() == OfficeServerConfiguration.SERVER_TYPE_EXTERNAL_REMOTE) {
            initializeExternalServer();
        } else {
            setState(ServerState.CONF_ERROR);
            throw new OfficeServerException("Invalid office server configuration.");
        }

        this.jodConverter = null;

        // Try to use the JSON document format registry to configure the office document conversion.
        try (InputStream input = getClass().getResourceAsStream(DOCUMENT_FORMATS_PATH)) {
            if (input != null) {
                this.jodConverter = LocalConverter.builder().officeManager(this.jodManager)
                    .formatRegistry(JsonDocumentFormatRegistry.create(input))
                    .filterChain(new LinkedImagesEmbedderFilter()).build();
            } else {
                this.logger.debug("{} is missing. The default document format registry will be used instead.",
                    DOCUMENT_FORMATS_PATH);
            }
        } catch (Exception e) {
            this.logger.warn("Failed to parse {} . The default document format registry will be used instead.",
                DOCUMENT_FORMATS_PATH, e);
        }

        if (this.jodConverter == null) {
            // Use the default document format registry.
            this.jodConverter = LocalConverter.builder().officeManager(this.jodManager)
                .filterChain(new LinkedImagesEmbedderFilter()).build();
        }

        this.converter = new DefaultOfficeConverter(this.jodConverter, getWorkDir());
    }

    private void initializeInternalServer() throws OfficeServerException
    {
        LocalOfficeManager.Builder configuration = LocalOfficeManager.builder();
        configuration.portNumbers(this.config.getServerPorts());

        String homePath = this.config.getHomePath();
        if (homePath != null) {
            configuration.officeHome(homePath);
        }

        String profilePath = this.config.getProfilePath();
        if (profilePath != null) {
            configuration.templateProfileDir(new File(profilePath));
        }

        configuration.maxTasksPerProcess(this.config.getMaxTasksPerProcess());
        configuration.taskExecutionTimeout(this.config.getTaskExecutionTimeout());

        try {
            this.jodManager = configuration.build();
        } catch (Exception e) {
            // Protect against exceptions raised by JodManager. For example if it cannot autodetect the office home,
            // it'll throw an java.lang.IllegalStateException exception...
            // We wrap this in an OfficeServerException in order to display some nicer message to the user.
            throw new OfficeServerException("Failed to start Office server. Reason: " + e.getMessage(), e);
        }
    }

    private void initializeExternalServer()
    {
        ExternalOfficeManager.Builder externalProcessOfficeManager = ExternalOfficeManager.builder();
        externalProcessOfficeManager.portNumbers(this.config.getServerPorts());
        externalProcessOfficeManager.connectOnStart(true);
        if (this.config.getServerType() == OfficeServerConfiguration.SERVER_TYPE_EXTERNAL_REMOTE) {
            externalProcessOfficeManager.hostName(config.getServerHost());
        }
        this.jodManager = externalProcessOfficeManager.build();
    }

    private File getWorkDir()
    {
        Optional<String> workDirFromConfiguration = this.config.getWorkDir();
        if (workDirFromConfiguration.isPresent() && StringUtils.isNotBlank(workDirFromConfiguration.get())) {
            File workDir = new File(workDirFromConfiguration.get());
            if (workDir.isDirectory() && workDir.canWrite()) {
                return workDir;
            }
        }
        return this.environment.getTemporaryDirectory();
    }

    @Override
    public void refreshState()
    {
        if (this.jodManager == null) {
            setState(ServerState.NOT_CONNECTED);
        } else {
            if (this.jodManager.isRunning()) {
                setState(ServerState.CONNECTED);
            } else {
                setState(ServerState.NOT_CONNECTED);
            }
        }
    }

    @Override
    public ServerState getState()
    {
        return this.state;
    }

    @Override
    public void start() throws OfficeServerException
    {
        // If the office server is running then stop it in order to restart the connection.
        stop();

        initialize();
        try {
            this.jodManager.start();
            setState(ServerState.CONNECTED);
            this.logger.info("Open Office instance started.");
        } catch (Exception e) {
            setState(ServerState.ERROR);
            throw new OfficeServerException("Error while connecting / starting the office server.", e);
        }
    }

    @Override
    public void stop() throws OfficeServerException
    {
        // We should try stopping the office server even if the status is not connected but we should not raise an
        // error if there is a failure to stop.
        boolean connected = checkState(ServerState.CONNECTED);
        try {
            this.jodManager.stop();
            setState(ServerState.NOT_CONNECTED);
            this.logger.info("Open Office instance stopped.");
        } catch (Exception e) {
            if (connected) {
                setState(ServerState.ERROR);
                throw new OfficeServerException("Error while disconnecting / shutting down the office server.", e);
            }
        }
    }

    @Override
    public OfficeConverter getConverter()
    {
        return this.converter;
    }

    /**
     * Utility method for setting the current office manager state.
     * 
     * @param newState new state.
     */
    private void setState(ServerState newState)
    {
        this.state = newState;
    }

    /**
     * Utility method for checking the office manager state.
     * 
     * @param expectedState expected state.
     * @return true if office manger is in given state, false otherwise.
     */
    private boolean checkState(ServerState expectedState)
    {
        return (this.state == expectedState);
    }
}
