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

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.StreamingResponseCallback;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.SolrParams;
import org.slf4j.Logger;
import org.xwiki.search.solr.XWikiSolrCore;
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
     * Solr server instance corresponding, depending on the subclass.
     */
    protected volatile XWikiSolrCore server;

    /**
     * Logging framework.
     */
    @Inject
    protected Logger logger;

    @Override
    public void add(SolrInputDocument solrDocument) throws SolrServerException, IOException
    {
        this.logger.debug("Add Solr document [{}] to index", solrDocument);

        getClient().add(solrDocument);
    }

    @Override
    public void add(List<SolrInputDocument> solrDocuments) throws SolrServerException, IOException
    {
        // An update request without any document has no body, which a remote Solr rejects with a
        // "missing content stream" error. There is also nothing to send, so skip the request entirely.
        if (solrDocuments.isEmpty()) {
            return;
        }

        this.logger.debug("Add Solr documents [{}] to index", solrDocuments);

        getClient().add(solrDocuments);
    }

    @Override
    public SolrDocument get(String id) throws IOException, SolrServerException
    {
        return getClient().getById(id);
    }

    @Override
    public void delete(String id) throws SolrServerException, IOException
    {
        this.logger.debug("Delete Solr document [{}] from index", id);

        getClient().deleteById(id);
    }

    @Override
    public void delete(List<String> ids) throws SolrServerException, IOException
    {
        this.logger.debug("Delete Solr documents [{}] from index", ids);

        getClient().deleteById(ids);
    }

    @Override
    public void deleteByQuery(String query) throws SolrServerException, IOException
    {
        this.logger.debug("Delete Solr documents from index based on query [{}]", query);

        getClient().deleteByQuery(query);
    }

    @Override
    public void commit() throws SolrServerException, IOException
    {
        this.logger.debug("Commit changes to Solr");

        getClient().commit();
    }

    @Override
    public void rollback() throws SolrServerException, IOException
    {
        this.logger.debug("Rollback changes to Solr");

        getClient().rollback();
    }

    @Override
    public QueryResponse query(SolrParams solrParams) throws SolrServerException, IOException
    {
        this.logger.debug("Execute Solr query [{}]", solrParams);

        return getClient().query(solrParams);
    }

    @Override
    public QueryResponse queryAndStreamResponse(SolrParams params, StreamingResponseCallback callback)
        throws SolrServerException, IOException
    {
        this.logger.debug("Execute Solr query and stream response [{}]", params);

        return getClient().queryAndStreamResponse(params, callback);
    }

    /**
     * Useful when testing.
     * <p>
     * With a remote Solr server, {@link #server} is {@code null} until the Solr core is resolved, so this method
     * returns {@code null} until then: operations should use {@link #getClient()}, which resolves the core when
     * needed.
     * 
     * @return the client of {@link #server}, or {@code null} if the Solr core is not resolved yet
     */
    protected SolrClient getServer()
    {
        XWikiSolrCore solrCore = this.server;

        return solrCore != null ? solrCore.getClient() : null;
    }

    /**
     * Gets the client of the Solr core used by this instance. The default implementation returns the client of
     * {@link #server}.
     *
     * @return the client of the Solr core
     * @throws SolrServerException when the Solr core is not available
     * @since 18.10.0RC1
     */
    protected SolrClient getClient() throws SolrServerException
    {
        return this.server.getClient();
    }
}
