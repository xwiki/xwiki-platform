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
package org.xwiki.platform.flavor;

import java.util.Collection;
import java.util.Collections;

import org.xwiki.component.annotation.Role;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.repository.result.IterableResult;
import org.xwiki.extension.repository.search.SearchException;
import org.xwiki.stability.Unstable;

/**
 * Find the flavors into the repositories, applying filters according to the configuration.
 * 
 * @version $Id$
 * @since 7.1M2
 */
@Role
@Unstable
public interface FlavorManager
{
    /**
     * Get all flavors matching a query.
     * 
     * @param query query to execute
     * @return flavors matching the query
     * @throws SearchException error when trying to search provided query
     * @deprecated since 8.0RC1, use {@link #searchFlavors(FlavorQuery)} instead
     */
    @Deprecated
    IterableResult<Extension> getFlavors(FlavorQuery query) throws SearchException;

    /**
     * Get all flavors matching a query.
     * 
     * @param query query to execute
     * @return flavors matching the query
     * @throws SearchException error when trying to search provided query
     * @since 8.0RC1
     */
    IterableResult<Extension> searchFlavors(FlavorQuery query) throws SearchException;

    /**
     * Get the flavor installed on a given wiki.
     * 
     * @param wikiId id of the wiki
     * @return the id of the flavor installed on the given wiki or null if there is no flavor installed
     */
    ExtensionId getFlavorOfWiki(String wikiId);

    /**
     * Get the flavor know that the distribution.
     * <p>
     * Each {@link ExtensionId} always contains the flavor id but version might be null.
     * 
     * @return the already known flavors
     * @since 8.1M1
     */
    default Collection<ExtensionId> getKnownFlavors()
    {
        return Collections.emptyList();
    }
}
