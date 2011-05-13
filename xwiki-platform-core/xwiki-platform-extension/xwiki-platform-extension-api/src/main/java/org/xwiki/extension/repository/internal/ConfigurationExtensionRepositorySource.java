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
package org.xwiki.extension.repository.internal;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.ExtensionManagerConfiguration;
import org.xwiki.extension.repository.ExtensionRepositoryId;
import org.xwiki.extension.repository.ExtensionRepositorySource;

/**
 * Extensions repositories identifier stored in the configuration.
 * 
 * @version $Id$
 */
@Component
@Singleton
public class ConfigurationExtensionRepositorySource implements ExtensionRepositorySource
{
    /**
     * Used to get configuration properties containing repositories.
     */
    @Inject
    private ExtensionManagerConfiguration configuration;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.repository.ExtensionRepositorySource#getExtensionRepositories()
     */
    public List<ExtensionRepositoryId> getExtensionRepositories()
    {
        return configuration.getRepositories();
    }
}
