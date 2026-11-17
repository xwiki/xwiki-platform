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
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.CommonParams;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.SecureQuery;
import org.xwiki.query.internal.AbstractQueryExecutor;
import org.xwiki.search.solr.internal.api.FieldUtils;
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
public class SolrQueryExecutor extends AbstractQueryExecutor
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
     * The request handlers that can be selected using the {@code qt} parameter: the search request handlers of the
     * search core.
     */
    private static final List<String> ALLOWED_REQUEST_HANDLERS = List.of("/select", "/query", "/spell");

    /**
     * The parameter that controls whether Solr expands the macros in the parameter values.
     */
    private static final String EXPAND_MACROS = "expandMacros";

    /**
     * The separator between the prefix of the parameters of a nested query and their names.
     */
    private static final char PARAMETER_PREFIX_SEPARATOR = '.';

    private static final String NESTED_FIELD_LIST_SUFFIX = PARAMETER_PREFIX_SEPARATOR + CommonParams.FL;

    private static final String NESTED_REQUEST_HANDLER_SUFFIX = PARAMETER_PREFIX_SEPARATOR + CommonParams.QT;

    /**
     * The name of the parameters of the JSON request API, which Solr merges into the other parameters (e.g., the
     * fields of the JSON request are added to the field list), also when prefixed for a nested query.
     */
    private static final String JSON = "json";

    /**
     * The parameters of the JSON request API that are supported: they only control how the response is serialized or
     * which facets are computed.
     */
    private static final List<String> SUPPORTED_JSON_PARAMETERS = List.of("json.nl", "json.wrf", "json.facet");

    /**
     * Provider for the {@link SolrInstance} that allows communication with the Solr server.
     */
    @Inject
    protected SolrInstance solrInstance;

    /**
     * Used to retrieve the configured supported locales.
     */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * Used to remove the documents that the users to check aren't allowed to view from the responses.
     */
    @Inject
    private Provider<SolrResponseRightsFilter> responseRightsFilterProvider;

    @Inject
    private JobProgressManager progress;

    @Inject
    private SafeSolrQueryValidator validator;

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> execute(Query query) throws QueryException
    {
        this.progress.startStep(query, "query.solr.progress.execute", "Execute Solr query [{}]", query);
        this.progress.pushLevelProgress(3, query);

        try {
            this.progress.startStep(query, "query.solr.progress.execute.prepare", "Prepare");

            SolrQuery solrQuery = createSolrQuery(filterQuery(query, SOLR));

            this.progress.startStep(query, "query.solr.progress.execute.execute", "Execute");

            QueryResponse response = this.solrInstance.query(solrQuery);

            this.progress.startStep(query, "query.solr.progress.execute.filter", "Filter");

            // Check access rights need to be checked before returning the response.
            // FIXME: this is not really the best way, mostly because at this point all grouping operations
            // have already been performed and any change on the result will not ensure that the grouping
            // information (facets, highlighting, maxScore, etc.) is still relevant.
            // A better way would be using a PostFilter as described in this article:
            // http://java.dzone.com/articles/custom-security-filtering-solr
            List<DocumentReference> usersToCheck = new ArrayList<>(2);
            if (query instanceof SecureQuery secureQuery) {
                if (secureQuery.isCurrentUserChecked()) {
                    usersToCheck.add(xcontextProvider.get().getUserReference());
                }
                if (secureQuery.isCurrentAuthorChecked()) {
                    usersToCheck.add(xcontextProvider.get().getAuthorReference());
                }
            } else {
                usersToCheck.add(xcontextProvider.get().getUserReference());
                usersToCheck.add(xcontextProvider.get().getAuthorReference());
            }
            if (!usersToCheck.isEmpty()) {
                this.responseRightsFilterProvider.get().filter(response, usersToCheck);
            }

            return filterResults(query, (List<T>) List.of(response));
        } catch (Exception e) {
            throw new QueryException("Exception while executing query", query, e);
        } finally {
            this.progress.popLevelProgress(query);
            this.progress.endStep(query);
        }
    }

    private SolrQuery createSolrQuery(Query query) throws QueryException
    {
        SolrQuery solrQuery = new SolrQuery(query.getStatement());

        // Overwrite offset and limit only if the query object explicitly says so, otherwise use whatever the named
        // parameters say or the defaults. This is done before setting the named parameters so that the "start" and
        // "rows" parameters take precedence over the offset and limit of the query object.
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

            if (value instanceof Iterable iterable) {
                solrQuery.set(entry.getKey(), toStringArray(iterable));
            } else if (value != null && value.getClass().isArray()) {
                solrQuery.set(entry.getKey(), toStringArray(value));
            } else {
                solrQuery.set(entry.getKey(), String.valueOf(value));
            }
        }

        checkRequestHandlers(solrQuery, query);
        checkJSONParameters(solrQuery, query);
        checkFieldLists(solrQuery, query);

        // Solr replaces the ${name} and ${name:default} macros in the parameter values with the values of other
        // parameters, after the values have been validated or escaped (e.g., a quoted value is no longer a phrase if
        // it contains ${q}). Expand macros only when the query asks for it explicitly.
        if (solrQuery.get(EXPAND_MACROS) == null) {
            solrQuery.set(EXPAND_MACROS, false);
        }

        // The sections of the response that are keyed by document id (e.g., highlighting) are filtered based on the
        // ids of the documents that are kept in the results, so make sure the id is returned. Solr merges the values
        // of the field list parameter.
        if (solrQuery.getParams(CommonParams.FL) != null) {
            solrQuery.add(CommonParams.FL, FieldUtils.ID);
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
     * The query executor only supports search request handlers, also for the nested queries (e.g., {@code x.qt} for
     * {@code fl=x:[subquery]}).
     */
    private void checkRequestHandlers(SolrQuery solrQuery, Query query) throws QueryException
    {
        for (String name : solrQuery.getParameterNames()) {
            if (CommonParams.QT.equals(name) || name.endsWith(NESTED_REQUEST_HANDLER_SUFFIX)) {
                for (String requestHandler : solrQuery.getParams(name)) {
                    if (!ALLOWED_REQUEST_HANDLERS.contains(requestHandler)) {
                        throw new QueryException(
                            String.format("Unsupported request handler [%s], only %s are supported", requestHandler,
                                ALLOWED_REQUEST_HANDLERS),
                            query, null);
                    }
                }
            }
        }
    }

    /**
     * Solr merges the parameters of the JSON request API into the other parameters without the checks done here, so
     * only the parameters that don't affect the documents of the response are supported.
     */
    private void checkJSONParameters(SolrQuery solrQuery, Query query) throws QueryException
    {
        for (String name : solrQuery.getParameterNames()) {
            List<String> segments = List.of(StringUtils.split(name, PARAMETER_PREFIX_SEPARATOR));
            int jsonIndex = segments.indexOf(JSON);
            if (jsonIndex >= 0 && !SUPPORTED_JSON_PARAMETERS
                .contains(StringUtils.join(segments.subList(jsonIndex, segments.size()), PARAMETER_PREFIX_SEPARATOR))) {
                throw new QueryException(String.format("Unsupported parameter [%s], of the JSON request parameters "
                    + "only %s are supported", name, SUPPORTED_JSON_PARAMETERS), query, null);
            }
        }
    }

    /**
     * The response filter decides which documents to keep based on the values of some of their fields, so the field
     * lists must not replace these values. This includes the field lists of the nested queries, e.g., {@code x.fl} for
     * {@code fl=x:[subquery]}.
     */
    private void checkFieldLists(SolrQuery solrQuery, Query query) throws QueryException
    {
        for (String name : solrQuery.getParameterNames()) {
            if (CommonParams.FL.equals(name) || name.endsWith(NESTED_FIELD_LIST_SUFFIX)) {
                for (String fieldList : solrQuery.getParams(name)) {
                    if (!this.validator.isFieldListKeepingFields(fieldList, SolrResponseRightsFilter.CHECKED_FIELDS)) {
                        throw new QueryException(String.format(
                            "Unsupported field list [%s]: the fields %s can only be returned with their own values",
                            fieldList, new TreeSet<>(SolrResponseRightsFilter.CHECKED_FIELDS)), query, null);
                    }
                }
            }
        }
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
        List<String> args = new ArrayList<>();
        for (Object obj : iterable) {
            args.add(String.valueOf(obj));
        }
        return args.toArray(new String[args.size()]);
    }
}
