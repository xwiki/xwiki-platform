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

import javax.inject.Inject;

import org.artofsolving.jodconverter.office.OfficeUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.officeimporter.openoffice.OpenOfficeConfiguration;

/**
 * Default implementation of {@link OpenOfficeConfiguration}.
 * 
 * @version $Id$
 * @since 1.8RC3
 */
@Component
public class DefaultOpenOfficeConfiguration implements OpenOfficeConfiguration, Initializable
{
    /**
     * Prefix for configuration keys for the OpenOffice module.
     */
    private static final String PREFIX = "openoffice.";

    /**
     * @see OpenOfficeConfiguration#getServerType()
     */
    private static final int DEFAULT_SERVER_TYPE = SERVER_TYPE_INTERNAL;

    /**
     * @see OpenOfficeConfiguration#getServerPort()
     */
    private static final int DEFAULT_SERVER_PORT = 8100;

    /**
     * @see OpenOfficeConfiguration#isAutoStart()
     */
    private static final boolean DEFAULT_AUTO_START = false;

    /**
     * @see OpenOfficeConfiguration#getMaxTasksPerProcess()
     */
    private static final int DEFAULT_MAX_TASKS_PER_PROCESS = 50;

    /**
     * @see OpenOfficeConfiguration#getTaskExecutionTimeout()
     */
    private static final long DEFAULT_TASK_EXECUTION_TIMEOUT = 30000L;

    /**
     * @see OpenOfficeConfiguration#getHomePath()
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
        return this.configuration.getProperty(PREFIX + "homePath", this.defaultHomePath);
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
        File defaultHomeFolder = OfficeUtils.getDefaultOfficeHome();
        this.defaultHomePath = defaultHomeFolder != null ? defaultHomeFolder.getAbsolutePath() : null;
    }
}
