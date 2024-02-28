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
import org.apache.solr.core.SolrCore;
import org.xwiki.component.annotation.Role;
import org.xwiki.search.solr.internal.DefaultXWikiSolrCore;
import org.xwiki.stability.Unstable;

/**
 * The central entry point to access a Solr core.
 * 
 * @version $Id$
 * @since 12.2
 */
@Role
public interface Solr
{
    /**
     * @param name the name of the Solr core
     * @return the cached {@link SolrClient} instance to use to manipulate the core
     * @throws SolrException when failing to create the solr client
     * @deprecated use {@link #getCore(String)} instead
     */
    @Deprecated(since = "16.1.0RC1")
    SolrClient getClient(String name) throws SolrException;

    /**
     * @param name the name of the core form XWiki point of view (so without potential prefix/suffix part of the real
     *            solr core specific to the setup)
     * @return the cached {@link SolrCore} instance to use to manipulate the core
     * @throws SolrException when failing to create the solr client
     * @since 16.2.0RC1
     */
    @Unstable
    default XWikiSolrCore getCore(String name) throws SolrException
    {
        return new DefaultXWikiSolrCore(name, name, getClient(name));
    }
}
