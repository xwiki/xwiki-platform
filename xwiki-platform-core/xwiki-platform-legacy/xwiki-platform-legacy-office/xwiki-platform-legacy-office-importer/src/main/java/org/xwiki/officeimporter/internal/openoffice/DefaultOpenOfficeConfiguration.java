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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.officeimporter.openoffice.OpenOfficeConfiguration;
import org.xwiki.officeimporter.server.OfficeServerConfiguration;

/**
 * Default {@link OpenOfficeConfiguration} implementation.
 * 
 * @version $Id$
 * @since 1.8RC3
 * @deprecated since 5.0M2, use {@link OfficeServerConfiguration} instead
 */
@Component
@Singleton
@Deprecated
public class DefaultOpenOfficeConfiguration implements OpenOfficeConfiguration
{
    /**
     * The actual configuration.
     */
    @Inject
    private OfficeServerConfiguration config;

    @Override
    public int getServerType()
    {
        return config.getServerType();
    }

    @Override
    public int getServerPort()
    {
        return config.getServerPort();
    }

    @Override
    public boolean isAutoStart()
    {
        return config.isAutoStart();
    }

    @Override
    public String getHomePath()
    {
        return config.getHomePath();
    }

    @Override
    public String getProfilePath()
    {
        return config.getProfilePath();
    }

    @Override
    public int getMaxTasksPerProcess()
    {
        return config.getMaxTasksPerProcess();
    }

    @Override
    public long getTaskExecutionTimeout()
    {
        return config.getTaskExecutionTimeout();
    }
}
