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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryExecutor;
import org.xwiki.search.solr.internal.api.FieldUtils;
import org.xwiki.search.solr.internal.api.SolrInstance;

/**
 * Executes Solr queries.
 * <p/>
 * For now, the result is the direct {@link QueryResponse}, in lack of a more expressive result type than the generic
 * List that the {@link #execute(Query)} method allows.
 * 
 * @version $Id$
 * @since 4.3M2
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
     * Logging framework.
     */
    @Inject
    protected Logger logger;

    /**
     * XWiki model bridge.
     */
    @Inject
    protected DocumentAccessBridge documentAccessBridge;

    /**
     * Used to retrieve user preference regarding hidden documents.
     */
    @Inject
    @Named("user")
    protected ConfigurationSource userPreferencesSource;

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

            // Overwrite offset and limit only if the query object explicitly says so, otherwise use whatever the query
            // statement says or the defaults
            if (query.getOffset() > 0) {
                solrQuery.setStart(query.getOffset());
            }
            if (query.getLimit() > 0) {
                solrQuery.setRows(query.getLimit());
            }

            // TODO: good idea? Any confusion? Do we really needs something like this?
            // Reuse the Query.getNamedParameters() map to get extra parameters.
            for (Entry<String, Object> entry : query.getNamedParameters().entrySet()) {
                if (entry.getValue() instanceof Collection) {
                    solrQuery.set(entry.getKey(), toStringArray((Collection) entry.getValue()));
                } else {
                    solrQuery.set(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }

            QueryResponse response = solrInstance.query(solrQuery);

            // Check access rights need to be checked before returning the response.
            // FIXME: this is not really the best way, mostly because at this point all grouping operations
            // have already been performed and any change on the result will not ensure that the grouping
            // information (facets, highlighting, maxScore, etc.) is still relevant.
            // A better way would be using a PostFilter as described in this article:
            // http://java.dzone.com/articles/custom-security-filtering-solr
            // Basically, we would be asking
            this.filterResponse(response);

            return (List<T>) Arrays.asList(response);
        } catch (Exception e) {
            throw new QueryException("Exception while executing query", query, e);
        }
    }

    /**
     * Converts the given collection of generic objects to a string array.
     * 
     * @param col the collection, must not be null
     * @return an array with the string representations of the collection's items
     */
    private String[] toStringArray(Collection col)
    {
        String[] args = new String[col.size()];
        int i = 0;
        for (Object obj : col) {
            args[i++] = String.valueOf(obj);
        }
        return args;
    }

    /**
     * Filter out results from the response that the current user does not have access to view.
     * 
     * @param response the Solr response to filter
     */
    protected void filterResponse(QueryResponse response)
    {
        SolrDocumentList results = response.getResults();
        long numFound = results.getNumFound();

        // Since we are modifying the results collection, we need to iterate over its copy.
        for (SolrDocument result : new ArrayList<SolrDocument>(results)) {
            try {
                DocumentReference resultDocumentReference =
                    new DocumentReference((String) result.get(FieldUtils.WIKI), (String) result.get(FieldUtils.SPACE),
                        (String) result.get(FieldUtils.NAME));

                if (!documentAccessBridge.exists(resultDocumentReference)
                    || !documentAccessBridge.isDocumentViewable(resultDocumentReference)) {

                    // Remove the current incompatible result.
                    results.remove(result);

                    // Decrement the number of results.
                    numFound--;

                    // FIXME: We should update maxScore as well when removing the top scored item. How do we do that?
                    // Sorting based on score might be a not so expensive option.

                    // FIXME: What about highlighting, facets and all the other data inside the QueryResponse?
                }
            } catch (Exception e) {
                this.logger.warn("Skipping bad result: {}", result, e);
            }
        }

        // Update the new number of results, excluding the filtered ones.
        if (numFound < 0) {
            // Lower bound guard for the total number of results.
            numFound = 0;
        }
        results.setNumFound(numFound);
    }
}
