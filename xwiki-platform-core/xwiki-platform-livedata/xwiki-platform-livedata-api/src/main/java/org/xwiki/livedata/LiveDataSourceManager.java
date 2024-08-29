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
package org.xwiki.livedata;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.xwiki.component.annotation.Role;
import org.xwiki.livedata.LiveDataQuery.Source;

/**
 * The interface used to access the available live data sources.
 * 
 * @version $Id$
 * @since 12.10
 */
@Role
public interface LiveDataSourceManager
{
    /**
     * Looks for a live data source in the specified namespace.
     * 
     * @param source indicates the live data source to return
     * @param namespace the namespace where to look for the specified live data source
     * @return the specified live data source, if found
     */
    Optional<LiveDataSource> get(Source source, String namespace);

    /**
     * Looks for a live data source in the current namespace.
     * 
     * @param source indicates the live data source to return
     * @return the specified live data source, if found
     */
    default Optional<LiveDataSource> get(Source source)
    {
        return get(source, "");
    }

    /**
     * Looks for a live data source in the specified namespace.
     * 
     * @param sourceHint indicates the live data source to return
     * @param namespace the namespace where to look for the specified live data source
     * @return the specified live data source, if found
     */
    default Optional<LiveDataSource> get(String sourceHint, String namespace)
    {
        Source source = new Source();
        source.setId(sourceHint);
        return get(source, namespace);
    }

    /**
     * Looks for a live data source in the current namespace.
     * 
     * @param sourceHint indicates the live data source to return
     * @return the specified live data source, if found
     */
    default Optional<LiveDataSource> get(String sourceHint)
    {
        return get(sourceHint, "");
    }

    /**
     * @param namespace the namespace where to look for live data sources
     * @return the list of live data sources available in the specified namespace, if the namespace exists
     */
    Optional<Collection<String>> getAvailableSources(String namespace);

    /**
     * @return the list of live data sources available in the current namespace
     */
    default Collection<String> getAvailableSources()
    {
        return getAvailableSources("").orElse(Collections.emptySet());
    }
}
