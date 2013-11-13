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
package org.xwiki.wiki.internal.manager;

import org.xwiki.component.annotation.Role;
import org.xwiki.wiki.internal.descriptor.DefaultWikiDescriptor;

/**
 * Component that handle caching for wiki descriptors.
 * @version $Id$
 * @since 5.3M2
 */
@Role
public interface WikiDescriptorCache
{
    /**
     * Add a descriptor to the cache.
     *
     * @param descriptor descriptor to add
     */
    void add(DefaultWikiDescriptor descriptor);

    /**
     * Remove a descriptor from the cache.
     * @param descriptor descriptor to remove
     */
    void remove(DefaultWikiDescriptor descriptor);

    /**
     * Get a descriptor from the cache.
     *
     * @param wikiId Id of the wiki to get
     * @return the descriptor related to the id or null if there is no corresponding descriptor in the cache
     */
    DefaultWikiDescriptor getFromId(String wikiId);

    /**
     * Get a descriptor from the cache.
     *
     * @param wikiAlias Alias of the wiki to get
     * @return the descriptor related to the alias or null if there is no corresponding descriptor in the cache
     */
    DefaultWikiDescriptor getFromAlias(String wikiAlias);
}
