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
package org.xwiki.wiki.user.internal;

import org.xwiki.component.annotation.Role;
import org.xwiki.wiki.user.WikiUserConfiguration;
import org.xwiki.wiki.user.WikiUserManagerException;

/**
 * Component to load and save wiki user configuration in a wiki.
 *
 * @version $Id$
 * @since 5.3M2
 */
@Role
public interface WikiUserConfigurationHelper
{
    /**
     * Get the configuration of the specified wiki.
     * @param wikiId id of the wiki
     * @return the configuration of that wiki
     * @throws WikiUserManagerException if problems occur
     */
    WikiUserConfiguration getConfiguration(String wikiId) throws WikiUserManagerException;

    /**
     * Save the configuration of the specified wiki.
     * @param configuration configuration to save
     * @param wikiId id of the wiki concerned by this configuration
     * @throws WikiUserManagerException if problems occur
     */
    void saveConfiguration(WikiUserConfiguration configuration, String wikiId) throws WikiUserManagerException;
}
