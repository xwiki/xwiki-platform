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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jodconverter.office.LocalOfficeUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.officeimporter.server.OfficeServerConfiguration;

/**
 * Default {@link OfficeServerConfiguration} implementation.
 * 
 * @version $Id$
 * @since 5.0M2
 */
@Component
@Singleton
public class DefaultOfficeServerConfiguration implements OfficeServerConfiguration, Initializable
{
    /**
     * Prefix for configuration keys for the office module.
     */
    private static final String PREFIX = "openoffice.";

    /**
     * @see OfficeServerConfiguration#getServerType()
     */
    private static final int DEFAULT_SERVER_TYPE = SERVER_TYPE_INTERNAL;

    /**
     * @see OfficeServerConfiguration#getServerPort()
     */
    private static final int DEFAULT_SERVER_PORT = 8100;

    /**
     * @see OfficeServerConfiguration#isAutoStart()
     */
    private static final boolean DEFAULT_AUTO_START = false;

    /**
     * @see OfficeServerConfiguration#getMaxTasksPerProcess()
     */
    private static final int DEFAULT_MAX_TASKS_PER_PROCESS = 50;

    /**
     * @see OfficeServerConfiguration#getTaskExecutionTimeout()
     */
    private static final long DEFAULT_TASK_EXECUTION_TIMEOUT = 30000L;

    /**
     * @see OfficeServerConfiguration#getHomePath()
     */
    private String defaultHomePath;

    /**
     * Defines from where to read the rendering configuration data.
     */
    @Inject
    private ConfigurationSource configuration;

    @Override
    public int getServerType()
    {
        return this.configuration.getProperty(PREFIX + "serverType", DEFAULT_SERVER_TYPE);
    }

    @Override
    public int getServerPort()
    {
        return this.configuration.getProperty(PREFIX + "serverPort", DEFAULT_SERVER_PORT);
    }

    @Override
    public boolean isAutoStart()
    {
        return this.configuration.getProperty(PREFIX + "autoStart", DEFAULT_AUTO_START);
    }

    @Override
    public String getHomePath()
    {
        String homePath = this.configuration.getProperty(PREFIX + "homePath");
        if (homePath == null) {
            // Fallback to the environment variable so anybody can set it for the execution of the functional tests,
            // in accord with their system.
            homePath = System.getenv("XWIKI_OFFICE_HOME");
            if (homePath == null) {
                // Finally fallback to the default value
                homePath = this.defaultHomePath;
            }
        }
        
        return homePath;
    }

    @Override
    public String getProfilePath()
    {
        return this.configuration.getProperty(PREFIX + "profilePath");
    }

    @Override
    public int getMaxTasksPerProcess()
    {
        return this.configuration.getProperty(PREFIX + "maxTasksPerProcess", DEFAULT_MAX_TASKS_PER_PROCESS);
    }

    @Override
    public long getTaskExecutionTimeout()
    {
        return this.configuration.getProperty(PREFIX + "taskExecutionTimeout", DEFAULT_TASK_EXECUTION_TIMEOUT);
    }

    @Override
    public void initialize() throws InitializationException
    {
        File defaultHomeFolder = LocalOfficeUtils.getDefaultOfficeHome();
        this.defaultHomePath = defaultHomeFolder != null ? defaultHomeFolder.getAbsolutePath() : null;
    }
}
