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

import net.sf.jodconverter.office.OfficeUtils;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationManager;
import org.xwiki.configuration.ConfigurationSourceCollection;
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
     * @see OpenOfficeConfiguration#getServerType()
     */
    private int serverType = SERVER_TYPE_INTERNAL;

    /**
     * @see OpenOfficeServerConfiguration#getServerPort();
     */
    private int serverPort = 8100;

    /**
     * @see OpenOfficeConfiguration#getHomePath()
     */
    private String homePath = OfficeUtils.getDefaultOfficeHome().getAbsolutePath();

    /**
     * @see OpenOfficeConfiguration#getProfilePath()
     */
    private String profilePath = OfficeUtils.getDefaultProfileDir().getAbsolutePath();

    /**
     * @see OpenOfficeConfiguration#isAutoStart()
     */
    private boolean autoStart = false;

    /**
     * @see OpenOfficeConfiguration#getMaxTasksPerProcess()
     */
    private int maxTasksPerProcess = 50;

    /**
     * @see OpenOfficeConfiguration#getTaskExecutionTimeout()
     */
    private long taskExecutionTimeout = 30000;

    /**
     * The {@link ConfigurationManager} component.
     */
    @Requirement
    private ConfigurationManager configurationManager;

    /**
     * The {@link ConfigurationSourceCollection} component.
     */
    @Requirement
    private ConfigurationSourceCollection sourceCollection;

    /**
     * {@inheritDoc}
     */
    public void initialize() throws InitializationException
    {
        this.configurationManager.initializeConfiguration(this, this.sourceCollection.getConfigurationSources(),
            "openoffice");
    }

    /**
     * {@inheritDoc}
     */
    public int getServerType()
    {
        return serverType;
    }

    /**
     * @param serverType the type of the openoffice server instance consumed by officeimporter.
     */
    public void setServerType(int serverType)
    {
        this.serverType = serverType;
    }

    /**
     * {@inheritDoc}
     */
    public int getServerPort()
    {
        return serverPort;
    }

    /**
     * @param serverPort port number used for connecting to the openoffice server instance.
     */
    public void setServerPort(int serverPort)
    {
        this.serverPort = serverPort;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isAutoStart()
    {
        return this.autoStart;
    }

    /**
     * @param autoStart if openoffice server should start automatically with XE.
     */
    public void setAutoStart(boolean autoStart)
    {
        this.autoStart = autoStart;
    }

    /**
     * {@inheritDoc}
     */
    public String getHomePath()
    {
        return homePath;
    }

    /**
     * @param homePath path to openoffice server installation.
     */
    public void setHomePath(String homePath)
    {
        this.homePath = homePath;
    }

    /**
     * {@inheritDoc}
     */
    public String getProfilePath()
    {
        return profilePath;
    }

    /**
     * @param profilePath path to openoffice execution profile.
     */
    public void setProfilePath(String profilePath)
    {
        this.profilePath = profilePath;
    }

    /**
     * {@inheritDoc}
     */
    public int getMaxTasksPerProcess()
    {
        return maxTasksPerProcess;
    }

    /**
     * @param maxTasksPerProcess maximum number of simultaneous conversion tasks to be handled by a single oo process
     *            instance.
     */
    public void setMaxTasksPerProcess(int maxTasksPerProcess)
    {
        this.maxTasksPerProcess = maxTasksPerProcess;
    }

    /**
     * {@inheritDoc}
     */
    public long getTaskExecutionTimeout()
    {
        return taskExecutionTimeout;
    }

    /**
     * @param taskExecutionTimeout timeout for document conversion tasks.
     */
    public void setTaskExecutionTimeout(long taskExecutionTimeout)
    {
        this.taskExecutionTimeout = taskExecutionTimeout;
    }
}
