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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryExecutor;
import org.xwiki.query.SecureQuery;
import org.xwiki.search.solr.internal.api.SolrInstance;

import com.xpn.xwiki.XWikiContext;

/**
 * Executes Solr queries.
 * <p>
 * For now, the result is the direct {@link QueryResponse}, in lack of a more expressive result type than the generic
 * List that the {@link #execute(Query)} method allows.
 * 
 * @version $Id$
 * @since 4.3M2
 */
@Component
@Named(SolrQueryExecutor.SOLR)
@Singleton
public class SolrQueryExecutor implements QueryExecutor
{
    /**
     * Query language ID.
     */
    public static final String SOLR = "solr";

    /**
     * The parameter that specifies the list of supported locales. This is used to add generic (unlocalized) aliases for
     * localized query fields (e.g. 'title' alias for 'title_en' query field).
     */
    private static final String PARAM_SUPPORTED_LOCALES = "xwiki.supportedLocales";

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
     * Provider for the {@link SolrInstance} that allows communication with the Solr server.
     */
    @Inject
    protected Provider<SolrInstance> solrInstanceProvider;

    /**
     * Used to retrieve the configured supported locales.
     */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * Used to extract a {@link DocumentReference} from a {@link SolrDocument}.
     */
    @Inject
    private DocumentReferenceResolver<SolrDocument> solrDocumentReferenceResolver;

    @Override
    public <T> List<T> execute(Query query) throws QueryException
    {
        // TODO: make it less restrictive, see http://jira.xwiki.org/browse/XWIKI-9386
        if (query instanceof SecureQuery && ((SecureQuery) query).isCurrentAuthorChecked()
            && !this.documentAccessBridge.hasProgrammingRights()) {
            throw new QueryException("Solr query require programming right", query, null);
        }

        try {
            SolrInstance solrInstance = solrInstanceProvider.get();
            SolrQuery solrQuery = createSolrQuery(query);
            QueryResponse response = solrInstance.query(solrQuery);

            // Check access rights need to be checked before returning the response.
            // FIXME: this is not really the best way, mostly because at this point all grouping operations
            // have already been performed and any change on the result will not ensure that the grouping
            // information (facets, highlighting, maxScore, etc.) is still relevant.
            // A better way would be using a PostFilter as described in this article:
            // http://java.dzone.com/articles/custom-security-filtering-solr
            // Basically, we would be asking
            if (!(query instanceof SecureQuery) || ((SecureQuery) query).isCurrentUserChecked()) {
                this.filterResponse(response);
            }

            return (List<T>) Arrays.asList(response);
        } catch (Exception e) {
            throw new QueryException("Exception while executing query", query, e);
        }
    }

    private SolrQuery createSolrQuery(Query query)
    {
        SolrQuery solrQuery = new SolrQuery(query.getStatement());

        // Overwrite offset and limit only if the query object explicitly says so, otherwise use whatever the query
        // statement says or the defaults.
        if (query.getOffset() > 0) {
            solrQuery.setStart(query.getOffset());
        }
        if (query.getLimit() > 0) {
            solrQuery.setRows(query.getLimit());
        }

        // TODO: good idea? Any confusion? Do we really needs something like this?
        // Reuse the Query.getNamedParameters() map to get extra parameters.
        for (Entry<String, Object> entry : query.getNamedParameters().entrySet()) {
            Object value = entry.getValue();

            if (value instanceof Iterable) {
                solrQuery.set(entry.getKey(), toStringArray((Iterable) value));
            } else if (value != null && value.getClass().isArray()) {
                solrQuery.set(entry.getKey(), toStringArray(value));
            } else {
                solrQuery.set(entry.getKey(), String.valueOf(value));
            }
        }

        // Make sure the list of supported locales is set so the names of the fields that are indexed in multiple
        // languages are expanded in the search query. For instance, the query "title:text" will be expanded to
        // "title__:text OR title_en:text OR title_fr:text" if the list of supported locales is [en, fr].
        if (!solrQuery.getParameterNames().contains(PARAM_SUPPORTED_LOCALES)) {
            XWikiContext xcontext = this.xcontextProvider.get();
            solrQuery.set(PARAM_SUPPORTED_LOCALES,
                StringUtils.join(xcontext.getWiki().getAvailableLocales(xcontext), ","));
        }

        return solrQuery;
    }

    /**
     * Converts an arbitrary array to an array containing its string representations.
     * 
     * @param array an array of arbitrary type, must not be null
     * @return an array with the string representations of the passed array's items
     */
    private String[] toStringArray(Object array)
    {
        int length = Array.getLength(array);
        String[] args = new String[length];
        for (int i = 0; i < length; i++) {
            args[i] = String.valueOf(Array.get(array, i));
        }

        return args;
    }

    /**
     * Converts the given iterable object to an array containing its string representations.
     * 
     * @param iterable the iterable object, must not be null
     * @return an array with the string representations of the passed iterable's items
     */
    private String[] toStringArray(Iterable iterable)
    {
        List<String> args = new ArrayList<String>();
        for (Object obj : iterable) {
            args.add(String.valueOf(obj));
        }
        return args.toArray(new String[args.size()]);
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
                DocumentReference resultDocumentReference = this.solrDocumentReferenceResolver.resolve(result);

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
