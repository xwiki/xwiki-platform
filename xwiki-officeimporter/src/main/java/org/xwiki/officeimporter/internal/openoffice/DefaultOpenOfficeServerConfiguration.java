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

import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationManager;
import org.xwiki.configuration.ConfigurationSourceCollection;
import org.xwiki.officeimporter.openoffice.OpenOfficeServerConfiguration;

/**
 * Default implementation of {@link OpenOfficeServerConfiguration}.
 * 
 * @version $Id$
 * @since 1.8RC3
 */
public class DefaultOpenOfficeServerConfiguration implements OpenOfficeServerConfiguration, Initializable
{
    /**
     * Path to openoffice server installation.
     */
    private String homePath = OfficeUtils.getDefaultOfficeHome().getAbsolutePath();

    /**
     * Path to openoffice execution profile.
     */
    private String profilePath = OfficeUtils.getDefaultProfileDir().getAbsolutePath();

    /**
     * A flag indicating if openoffice server should automatically start with XE.
     */
    private boolean autoStart = false;
    
    /**
     * Maximum number of simultaneous conversion tasks to be handled by a single oo process instance.
     */
    private int maxTasksPerProcess = 50;

    /**
     * Timeout for document conversion tasks.
     */
    private long taskExecutionTimeout = 30000;

    /**
     * The {@link ConfigurationManager} component.
     */
    private ConfigurationManager configurationManager;

    /**
     * The {@link ConfigurationSourceCollection} component.
     */
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
}
