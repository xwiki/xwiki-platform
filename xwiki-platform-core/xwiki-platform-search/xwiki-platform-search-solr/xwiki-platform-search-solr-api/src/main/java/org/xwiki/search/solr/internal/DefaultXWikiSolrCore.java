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
package org.xwiki.search.solr.internal;

import org.apache.solr.client.solrj.SolrClient;
import org.xwiki.search.solr.XWikiSolrCore;

/**
 * Default implementation of {@link XWikiSolrCore}.
 * 
 * @version $Id$
 * @since 16.2.0RC1
 */
public class DefaultXWikiSolrCore implements XWikiSolrCore
{
    private final String name;

    private final String solrName;

    private final SolrClient client;

    /**
     * @param name the name of the core from XWiki point of view (without the prefix/suffix specific to the setup)
     * @param solrName the real name of the core
     * @param client the client used to manipulate native Solr API
     */
    public DefaultXWikiSolrCore(String name, String solrName, SolrClient client)
    {
        this.name = name;
        this.solrName = solrName;
        this.client = client;
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public String getSolrName()
    {
        return this.solrName;
    }

    @Override
    public SolrClient getClient()
    {
        return this.client;
    }
}
