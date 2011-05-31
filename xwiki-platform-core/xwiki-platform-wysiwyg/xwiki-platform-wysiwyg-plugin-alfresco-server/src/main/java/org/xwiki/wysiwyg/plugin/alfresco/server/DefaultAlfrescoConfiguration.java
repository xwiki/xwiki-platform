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
package org.xwiki.wysiwyg.plugin.alfresco.server;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.configuration.ConfigurationSource;

/**
 * Default implementation of {@link AlfrescoConfiguration}.
 * 
 * @version $Id$
 */
@Component
public class DefaultAlfrescoConfiguration implements AlfrescoConfiguration
{
    /**
     * The configuration source.
     */
    @Requirement("all")
    private ConfigurationSource configurationSource;

    /**
     * {@inheritDoc}
     * 
     * @see AlfrescoConfiguration#getServerURL()
     */
    public String getServerURL()
    {
        return configurationSource.getProperty("alfresco.serverURL", "http://localhost:8080");
    }

    /**
     * {@inheritDoc}
     * 
     * @see AlfrescoConfiguration#getUserName()
     */
    public String getUserName()
    {
        return configurationSource.getProperty("alfresco.username", "Admin");
    }

    /**
     * {@inheritDoc}
     * 
     * @see AlfrescoConfiguration#getPassword()
     */
    public String getPassword()
    {
        return configurationSource.getProperty("alfresco.password", "admin");
    }

    /**
     * {@inheritDoc}
     * 
     * @see AlfrescoConfiguration#getDefaultNodeReference()
     */
    public String getDefaultNodeReference()
    {
        return configurationSource.getProperty("alfresco.defaultNodeRef",
            "workspace://SpacesStore/e47e3e7d-c345-4558-97f7-1e846453dd4b");
    }
}
