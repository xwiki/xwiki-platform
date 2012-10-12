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
package org.xwiki.query.solr.internal;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryExecutor;
import org.xwiki.search.solr.SolrInstance;

/**
 * Executes Solr queries.
 * <p/>
 * For now, the result is the direct {@link QueryResponse}, in lack of a more expressive result type than the generic
 * List that the {@link #execute(Query)} method allows.
 * 
 * @version $Id$
 */
@Component
@Named(SolrQueryExecutor.SOLR)
public class SolrQueryExecutor implements QueryExecutor
{
    /**
     * Query language ID.
     */
    public static final String SOLR = "solr";

    /**
     * Provider for the {@link SolrInstance} that allows communication with the Solr server.
     */
    @Inject
    protected Provider<SolrInstance> solrInstanceProvider;

    @Override
    public <T> List<T> execute(Query query) throws QueryException
    {
        try {
            SolrInstance solrInstance = solrInstanceProvider.get();

            SolrQuery solrQuery = new SolrQuery(query.getStatement());

            solrQuery.setStart(query.getOffset());
            solrQuery.setRows(query.getLimit());

            // TODO: good idea? Any confusion? Do we really needs something like this?
            // Reuse the Query.getNamedParameters() map to get extra parameters.
            for (Entry<String, Object> entry : query.getNamedParameters().entrySet()) {
                solrQuery.set(entry.getKey(), String.valueOf(entry.getValue()));
            }

            QueryResponse response = solrInstance.query(solrQuery);

            return (List<T>) Arrays.asList(response);
        } catch (Exception e) {
            throw new QueryException("Exception while execute query", query, e);
        }
    }
}
