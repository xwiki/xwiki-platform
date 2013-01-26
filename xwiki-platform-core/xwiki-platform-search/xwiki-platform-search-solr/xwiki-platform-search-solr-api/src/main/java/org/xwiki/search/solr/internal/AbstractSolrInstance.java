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

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.core.CoreContainer;
import org.slf4j.Logger;
import org.xwiki.search.solr.internal.api.SolrInstance;

/**
 * Basic implementation for the wrapped instance.
 * 
 * @see SolrInstance
 * @version $Id$
 * @since 4.3M2
 */
public abstract class AbstractSolrInstance implements SolrInstance
{
    /**
     * Solr CoreContainer.
     */
    protected CoreContainer container;

    /**
     * Solr server instance corresponding, depending on the subclass.
     */
    protected SolrServer server;

    /**
     * Logging framework.
     */
    @Inject
    protected Logger logger;

    @Override
    public void shutDown()
    {
        if (this.server != null) {
            ((EmbeddedSolrServer) this.server).shutdown();
        }
        if (this.container != null) {
            container.shutdown();
        }
    }

    @Override
    public void add(SolrInputDocument solrDocument) throws SolrServerException, IOException
    {
        server.add(solrDocument);
    }

    @Override
    public void add(List<SolrInputDocument> solrDocuments) throws SolrServerException, IOException
    {
        server.add(solrDocuments);
    }

    @Override
    public void delete(String id) throws SolrServerException, IOException
    {
        server.deleteById(id);
    }

    @Override
    public void delete(List<String> ids) throws SolrServerException, IOException
    {
        server.deleteById(ids);
    }

    @Override
    public void deleteByQuery(String query) throws SolrServerException, IOException
    {
        server.deleteByQuery(query);
    }

    @Override
    public void commit() throws SolrServerException, IOException
    {
        server.commit();
    }

    @Override
    public void rollback() throws SolrServerException, IOException
    {
        server.rollback();
    }

    @Override
    public QueryResponse query(SolrParams solrParams) throws SolrServerException
    {
        QueryResponse result = server.query(solrParams);
        return result;
    }

    /**
     * Useful when testing.
     * 
     * @return the container
     */
    protected CoreContainer getContainer()
    {
        return container;
    }

    /**
     * Useful when testing.
     * 
     * @return the server
     */
    protected SolrServer getServer()
    {
        return server;
    }
}
