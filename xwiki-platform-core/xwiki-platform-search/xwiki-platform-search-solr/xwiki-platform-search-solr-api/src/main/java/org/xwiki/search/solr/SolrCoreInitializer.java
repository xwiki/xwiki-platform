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
package org.xwiki.search.solr;

import org.apache.solr.client.solrj.SolrClient;
import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * An extension point used to inject mandatory Solr cores.
 * 
 * @version $Id$
 * @since 12.2
 */
@Role
public interface SolrCoreInitializer
{
    /**
     * @return the name of Solr core to initialize.
     */
    String getCoreName();

    /**
     * Initialize the client after its creation.
     * 
     * @param client to manipulate the core
     * @throws SolrException when failing to initialize the core
     * @deprecated use {@link #initialize(XWikiSolrCore)} instead
     */
    @Deprecated(since = "16.1.0RC1")
    void initialize(SolrClient client) throws SolrException;

    /**
     * Initialize the client after its creation.
     * 
     * @param core to manipulate the core
     * @throws SolrException when failing to initialize the core
     * @since 16.2.0RC1
     */
    default void initialize(XWikiSolrCore core) throws SolrException
    {
        initialize(core.getClient());
    }

    /**
     * Indicate if the the core content is considered to be caching (it's possible to recreate it if the core is lost).
     * In practice it's mostly used to decide where to store the core data to make clear what it is when looking at the
     * permanent directory in case of embedded Solr.
     * 
     * @return true if the content of the core is considered to be cache
     * @since 12.10
     */
    default boolean isCache()
    {
        return false;
    }

    /**
     * @param sourceCore the core to copy from
     * @param targetCore the core to copy to
     * @throws SolrException when failing to migrate the core
     * @since 16.2.0RC1
     */
    @Unstable
    default void migrate(XWikiSolrCore sourceCore, XWikiSolrCore targetCore) throws SolrException
    {
        // Do nothing by default
    }
}
