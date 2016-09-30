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
package org.xwiki.search.solr.internal.api;

import java.io.IOException;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.StreamingResponseCallback;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.SolrParams;
import org.xwiki.component.annotation.Role;
import org.xwiki.component.phase.Initializable;

/**
 * Component in charge of communicating with the actual Solr server. This is direct access and consistency is not
 * enforced at this level.
 * <p>
 * Note: This is also useful for testing since it can be replaced with a mock, this way allowing us to test just our
 * code.
 * 
 * @version $Id$
 * @since 4.3M2
 */
@Role
public interface SolrInstance extends Initializable
{
    /**
     * Add a {@link SolrInputDocument} to the Solr index.
     * <p>
     * Note: Does not apply until you call {@link #commit()}.
     * 
     * @param solrDocument the document.
     * @throws SolrServerException if problems occur.
     * @throws IOException if problems occur.
     */
    void add(SolrInputDocument solrDocument) throws SolrServerException, IOException;

    /**
     * Add a list of {@link SolrInputDocument} to the Solr index. This is a batch operation.
     * <p>
     * Note: Does not apply until you call {@link #commit()}.
     * 
     * @param solrDocuments the documents.
     * @throws SolrServerException if problems occur.
     * @throws IOException if problems occur.
     */
    void add(List<SolrInputDocument> solrDocuments) throws SolrServerException, IOException;

    /**
     * Delete a single entry from the Solr index.
     * <p>
     * Note: Does not apply until you call {@link #commit()}.
     * 
     * @param id the ID of the entry.
     * @throws SolrServerException if problems occur.
     * @throws IOException if problems occur.
     */
    void delete(String id) throws SolrServerException, IOException;

    /**
     * Delete a list of entries from the Solr index. This is a batch operation.
     * <p>
     * Note: Does not apply until you call {@link #commit()}.
     * 
     * @param ids the list of entry IDs
     * @throws SolrServerException if problems occur.
     * @throws IOException if problems occur.
     */
    void delete(List<String> ids) throws SolrServerException, IOException;

    /**
     * Delete entries from the index based on the result of the given query.
     * <p>
     * Note: Does not apply until you call {@link #commit()}.
     * 
     * @param query the Solr query.
     * @throws SolrServerException if problems occur.
     * @throws IOException if problems occur.
     */
    void deleteByQuery(String query) throws SolrServerException, IOException;

    /**
     * Commit the recent (uncommitted) changes to the Solr server.
     * 
     * @throws SolrServerException if problems occur.
     * @throws IOException if problems occur.
     */
    void commit() throws SolrServerException, IOException;

    /**
     * Cancel the local uncommitted changes that were not yet pushed to the Solr server.
     * 
     * @throws SolrServerException if problems occur.
     * @throws IOException if problems occur.
     */
    void rollback() throws SolrServerException, IOException;

    /**
     * Query the server's index using the Solr Query API.
     * 
     * @param solrParams Solr Query API.
     * @return the query result.
     * @throws SolrServerException if problems occur.
     * @throws IOException if there is an error on the server
     */
    QueryResponse query(SolrParams solrParams) throws SolrServerException, IOException;

    /**
     * Query solr, and stream the results. Unlike the standard query, this will send events for each Document rather
     * then add them to the {@link QueryResponse}.
     * <p>
     * Although this function returns a {@link QueryResponse} it should be used with care since it excludes anything
     * that was passed to callback. Also note that future version may pass even more info to the callback and may not
     * return the results in the {@link QueryResponse}.
     * 
     * @param params query parameters
     * @param callback the object to notify
     * @return the query result
     * @throws SolrServerException if problems occur
     * @throws IOException if problems occur
     */
    QueryResponse queryAndStreamResponse(SolrParams params, StreamingResponseCallback callback)
        throws SolrServerException, IOException;
}
