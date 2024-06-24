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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrClient;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.XWikiSolrCore;
import org.xwiki.search.solr.internal.api.SolrConfiguration;

/**
 * Default implementation of {@link Solr} which dispatch on the right component (embedded, remote, etc.).
 * 
 * @version $Id$
 * @since 12.2
 */
@Component
@Singleton
public class DefaultSolr implements Solr, Initializable
{
    @Inject
    private SolrConfiguration configuration;

    @Inject
    private ComponentManager componentManager;

    private Solr configuredSolr;

    @Override
    public void initialize() throws InitializationException
    {
        String type = this.configuration.getServerType();
        try {
            this.configuredSolr = this.componentManager.getInstance(Solr.class, type);
        } catch (ComponentLookupException e) {
            throw new InitializationException(String.format("Failed to lookup configured Solr type [%s]", type), e);
        }
    }

    @Override
    @Deprecated
    public SolrClient getClient(String name) throws SolrException
    {
        return this.configuredSolr.getClient(name);
    }

    @Override
    public XWikiSolrCore getCore(String name) throws SolrException
    {
        return this.configuredSolr.getCore(name);
    }
}
