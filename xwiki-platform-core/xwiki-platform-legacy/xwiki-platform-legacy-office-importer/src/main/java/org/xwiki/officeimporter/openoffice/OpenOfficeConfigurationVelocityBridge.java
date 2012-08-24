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
package org.xwiki.officeimporter.openoffice;

import org.xwiki.officeimporter.internal.openoffice.script.OpenOfficeManagerScriptService;
import org.xwiki.script.service.ScriptService;

/**
 * A link between {@link OpenOfficeConfiguration} and velocity scripts.
 * 
 * @version $Id$
 * @since 1.9RC1
 * @deprecated since 4.1M1 use the {@link ScriptService} with hint "officemanager" instead; the office manager script
 *             service has a method to retrieve the configuration, {@link OpenOfficeManagerScriptService#getConfig()}
 */
@Deprecated
public class OpenOfficeConfigurationVelocityBridge
{
    /**
     * The {@link OpenOfficeConfiguration} component passed by the velocity context initializer.
     */
    private OpenOfficeConfiguration ooConfig;

    /**
     * Creates a new {@link OpenOfficeConfigurationVelocityBridge} instance.
     * 
     * @param ooConfig the {@link OpenOfficeConfiguration} component.
     */
    public OpenOfficeConfigurationVelocityBridge(OpenOfficeConfiguration ooConfig)
    {
        this.ooConfig = ooConfig;
    }

    /**
     * @return type of the openoffice server used by officeimporter.
     * @see OpenOfficeConfiguration#getServerType()
     */
    public int getServerType()
    {
        return this.ooConfig.getServerType();
    }

    /**
     * @return port number used for connecting to the openoffice server instance.
     * @see OpenOfficeConfiguration#getServerPort()
     */
    public int getServerPort()
    {
        return this.ooConfig.getServerPort();
    }

    /**
     * @return whether office server should be started / connected automatically with XWiki Enterprise
     */
    public boolean isAutoStart()
    {
        return this.ooConfig.isAutoStart();
    }

    /**
     * @return path to openoffice server installation.
     * @see OpenOfficeConfiguration#getHomePath()
     */
    public String getHomePath()
    {
        return this.ooConfig.getHomePath();
    }

    /**
     * @return path to openoffice execution profile.
     * @see OpenOfficeConfiguration#getProfilePath()
     */
    public String getProfilePath()
    {
        return this.ooConfig.getProfilePath();
    }

    /**
     * @return maximum number of simultaneous conversion tasks to be handled by a single oo process instance.
     * @see OpenOfficeConfiguration#getMaxTasksPerProcess()
     */
    public int getMaxTasksPerProcess()
    {
        return this.ooConfig.getMaxTasksPerProcess();
    }

    /**
     * @return timeout for document conversion tasks.
     * @see OpenOfficeConfiguration#getTaskExecutionTimeout()
     */
    public long getTaskExecutionTimeout()
    {
        return this.ooConfig.getTaskExecutionTimeout();
    }
}
