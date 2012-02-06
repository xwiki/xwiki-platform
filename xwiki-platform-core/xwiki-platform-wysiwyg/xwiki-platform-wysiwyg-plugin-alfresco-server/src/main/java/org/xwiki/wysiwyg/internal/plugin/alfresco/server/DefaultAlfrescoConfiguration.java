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
package org.xwiki.wysiwyg.internal.plugin.alfresco.server;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.wysiwyg.plugin.alfresco.server.AlfrescoConfiguration;

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
    @Inject
    @Named("all")
    private ConfigurationSource configurationSource;

    @Override
    public String getServerURL()
    {
        return configurationSource.getProperty("alfresco.serverURL", "http://localhost:8080");
    }

    @Override
    public String getUserName()
    {
        return configurationSource.getProperty("alfresco.username", "Admin");
    }

    @Override
    public String getPassword()
    {
        return configurationSource.getProperty("alfresco.password", "admin");
    }

    @Override
    public String getDefaultNodeReference()
    {
        return configurationSource.getProperty("alfresco.defaultNodeRef",
            "workspace://SpacesStore/e47e3e7d-c345-4558-97f7-1e846453dd4b");
    }

    @Override
    public String getAuthenticatorHint()
    {
        return configurationSource.getProperty("alfresco.authenticatorHint", "siteMinder");
    }
}
