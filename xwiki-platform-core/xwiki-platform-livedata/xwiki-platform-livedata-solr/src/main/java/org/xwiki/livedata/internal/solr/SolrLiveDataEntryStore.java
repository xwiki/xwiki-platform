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
package org.xwiki.livedata.internal.solr;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.livedata.LiveData;
import org.xwiki.livedata.LiveDataEntryStore;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.livedata.LiveDataQuery.Filter;
import org.xwiki.livedata.WithParameters;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.query.SecureQuery;
import org.xwiki.search.solr.SolrUtils;
import org.xwiki.search.solr.internal.api.FieldUtils;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserPropertiesResolver;

/**
 * {@link LiveDataEntryStore} implementation that runs a Solr search query and exposes the matching documents as live
 * data entries.
 * <p>
 * The Solr query string is taken from the {@code query} source parameter (defaulting to {@code *:*}). Columns are
 * referenced through {@code doc.*} property identifiers (the same ones used by the other live data sources and the
 * default configuration), which are mapped internally to the corresponding Solr fields; callers never deal with raw
 * Solr field names. The live data column filters and the sort are translated into Solr filter queries and sort
 * clauses. View rights are enforced by the underlying {@code solr} query executor, which removes from the result the
 * documents the current user cannot see.
 * <p>
 * Only document entities are exposed for now ({@code type:DOCUMENT}). Support for other entity types (attachments,
 * objects) and for object properties is left for a later version.
 *
 * @version $Id$
 * @since 18.5.0RC1
 */
@Component
@Named(SolrLiveDataEntryStore.ROLE_HINT)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class SolrLiveDataEntryStore extends WithParameters implements LiveDataEntryStore
{
    /**
     * The hint of this component implementation.
     */
    public static final String ROLE_HINT = "solr";

    /**
     * The source parameter holding the base Solr query string.
     */
    public static final String QUERY_PARAMETER = "query";

    /**
     * The live data property exposing the title of the matching document, rendered as a link.
     */
    protected static final String TITLE_PROPERTY = "doc.title";

    /**
     * The live data property exposing the view URL of the matching document. It is always added to each entry so that
     * the {@code link} displayer of the {@link #TITLE_PROPERTY} can build the link.
     */
    protected static final String URL_PROPERTY = "url";

    /**
     * The live data property holding the full name of the matching document. It is the {@code idProperty} of the
     * {@code solr} source configuration, so it is always added to each entry (even when not among the requested
     * columns) for the live data widget to be able to identify the rows.
     */
    protected static final String FULLNAME_PROPERTY = "doc.fullName";

    /**
     * Maps the supported {@code doc.*} live data property identifiers to the Solr field holding their value. The title
     * is handled separately because it is a localized field (see {@link #getTitle(SolrDocument)}).
     */
    protected static final Map<String, String> SOLR_FIELDS;

    /**
     * Maps the sortable {@code doc.*} live data property identifiers to the Solr field to sort on (a dedicated
     * {@code _sort} field for tokenized fields, the field itself for single-valued fields such as dates).
     */
    protected static final Map<String, String> SOLR_SORT_FIELDS;

    /**
     * The Solr query language id used to run the query. It happens to be the same as this component's role hint.
     */
    private static final String SOLR = ROLE_HINT;

    private static final String DEFAULT_QUERY = "*:*";

    /**
     * The number of entries returned when the query does not specify a limit. A {@code null} limit must not be passed
     * as {@code 0} to Solr: the Solr query executor would then leave the {@code rows} parameter unset and fall back to
     * Solr's own small default (10), silently truncating the result. We default instead to the same page size as the
     * live data base configuration ({@code liveDataConfiguration.json}).
     */
    private static final int DEFAULT_LIMIT = 15;

    private static final String DOC_CREATION_DATE = "doc.creationDate";

    private static final String DOC_DATE = "doc.date";

    static {
        Map<String, String> fields = new LinkedHashMap<>();
        // The generic "title_" field aggregates the title of all locales (copyField of title_*) and uses the generic
        // tokenizer, so it can be queried/filtered without depending on the document locale.
        fields.put(TITLE_PROPERTY, FieldUtils.TITLE + FieldUtils.USCORE);
        fields.put("doc.name", FieldUtils.NAME);
        fields.put(FULLNAME_PROPERTY, FieldUtils.FULLNAME);
        fields.put("doc.author", FieldUtils.AUTHOR_DISPLAY);
        fields.put("doc.creator", FieldUtils.CREATOR_DISPLAY);
        fields.put(DOC_CREATION_DATE, FieldUtils.CREATIONDATE);
        fields.put(DOC_DATE, FieldUtils.DATE);
        fields.put("doc.hidden", FieldUtils.HIDDEN);
        SOLR_FIELDS = Map.copyOf(fields);

        Map<String, String> sortFields = new LinkedHashMap<>();
        sortFields.put(TITLE_PROPERTY, FieldUtils.TITLE_SORT);
        sortFields.put(DOC_CREATION_DATE, FieldUtils.CREATIONDATE);
        sortFields.put(DOC_DATE, FieldUtils.DATE);
        SOLR_SORT_FIELDS = Map.copyOf(sortFields);
    }

    @Inject
    private QueryManager queryManager;

    @Inject
    private DocumentReferenceResolver<SolrDocument> solrDocumentReferenceResolver;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private UserPropertiesResolver userPropertiesResolver;

    @Inject
    private SolrUtils solrUtils;

    @Override
    public Optional<Map<String, Object>> get(Object entryId)
    {
        // Retrieving a single entry by its identifier is not supported: this source is a read-only list of documents
        // matching a Solr query.
        throw new UnsupportedOperationException();
    }

    @Override
    public LiveData get(LiveDataQuery query) throws LiveDataException
    {
        try {
            Map<String, Object> parameters = mergeParameters(query);

            String solrQueryString =
                StringUtils.defaultIfBlank((String) parameters.get(QUERY_PARAMETER), DEFAULT_QUERY);

            Query solrQuery = this.queryManager.createQuery(solrQueryString, SOLR);
            // Enforce view rights: the Solr query executor removes from the result the documents the current user
            // cannot see.
            ((SecureQuery) solrQuery).checkCurrentUser(true);

            solrQuery.bindValue("fq", getFilterQueries(query));
            List<String> sortClauses = getSortClauses(query);
            if (!sortClauses.isEmpty()) {
                solrQuery.bindValue("sort", StringUtils.join(sortClauses, ", "));
            }
            solrQuery.setOffset(query.getOffset() == null ? 0 : query.getOffset().intValue());
            solrQuery.setLimit(query.getLimit() == null ? DEFAULT_LIMIT : query.getLimit());

            QueryResponse response = execute(solrQuery);
            SolrDocumentList documents = response.getResults();

            LiveData liveData = new LiveData();
            // Note: the count is the number of documents matched by Solr, before the view rights filtering, so it can
            // be slightly higher than the number of entries actually returned (same limitation as the REST search).
            liveData.setCount(documents.getNumFound());
            List<String> properties = query.getProperties();
            for (SolrDocument document : documents) {
                liveData.getEntries().add(toEntry(document, properties));
            }
            return liveData;
        } catch (LiveDataException e) {
            throw e;
        } catch (Exception e) {
            throw new LiveDataException("Failed to execute the Solr live data query.", e);
        }
    }

    private QueryResponse execute(Query solrQuery) throws QueryException, LiveDataException
    {
        List<Object> results = solrQuery.execute();
        if (results.isEmpty() || !(results.getFirst() instanceof QueryResponse)) {
            throw new LiveDataException("The Solr query executor returned an unexpected result.");
        }
        return (QueryResponse) results.getFirst();
    }

    /**
     * Merges the parameters of this entry store (coming from the live data source configuration) with the parameters
     * carried by the query source, the latter taking precedence.
     */
    private Map<String, Object> mergeParameters(LiveDataQuery query)
    {
        Map<String, Object> parameters = new LinkedHashMap<>(getParameters());
        var source = query.getSource();
        if (source != null) {
            parameters.putAll(source.getParameters());
        }
        return parameters;
    }

    /**
     * Builds the list of Solr filter queries ({@code fq}) from the live data column filters. Each filter becomes a
     * single {@code fq} clause (filters are combined with a logical AND), and the constraints inside a filter are
     * combined with AND or OR depending on {@link Filter#isMatchAll()}.
     */
    private List<String> getFilterQueries(LiveDataQuery query)
    {
        List<String> filterQueries = new ArrayList<>();
        // Only list documents (not objects, attachments, etc.).
        filterQueries.add(FieldUtils.TYPE + ":(\"DOCUMENT\")");
        // Exclude the hidden documents (e.g. technical pages) unless the current user chose to display them, so that
        // the result is consistent with the rest of XWiki (document index, live table, search).
        if (!this.userPropertiesResolver.resolve(CurrentUserReference.INSTANCE).displayHiddenDocuments()) {
            filterQueries.add(FieldUtils.HIDDEN + ":(false)");
        }

        if (query.getFilters() != null) {
            for (Filter filter : query.getFilters()) {
                String filterQuery = toFilterQuery(filter);
                if (filterQuery != null) {
                    filterQueries.add(filterQuery);
                }
            }
        }
        return filterQueries;
    }

    private String toFilterQuery(Filter filter)
    {
        String field = SOLR_FIELDS.get(filter.getProperty());
        if (field == null) {
            // Unknown / unsupported property (e.g. an object property): ignore the filter for now.
            return null;
        }
        List<String> clauses = filter.getConstraints().stream()
            .map(constraint -> toClause(field, constraint.getValue()))
            .filter(StringUtils::isNotEmpty)
            .toList();
        if (clauses.isEmpty()) {
            return null;
        }
        String operator = filter.isMatchAll() ? " AND " : " OR ";
        return "(" + StringUtils.join(clauses, operator) + ")";
    }

    private String toClause(String field, Object value)
    {
        if (value == null) {
            return null;
        }
        // Use a tokenized match through SolrUtils rather than raw wildcards: wildcards bypass the Solr tokenizer and
        // lead to surprising results (e.g. multi-word queries not matching). The value is escaped and analyzed by the
        // field's query analyzer.
        return field + ':' + this.solrUtils.toFilterQueryString(value);
    }

    /**
     * Translates the live data sort entries into Solr {@code sort} clauses (e.g. {@code title_sort asc}).
     */
    private List<String> getSortClauses(LiveDataQuery query)
    {
        List<String> sortClauses = new ArrayList<>();
        if (query.getSort() != null) {
            for (var sortEntry : query.getSort()) {
                String field = SOLR_SORT_FIELDS.get(sortEntry.getProperty());
                if (field != null) {
                    sortClauses.add(field + (sortEntry.isDescending() ? " desc" : " asc"));
                }
            }
        }
        return sortClauses;
    }

    private Map<String, Object> toEntry(SolrDocument document, List<String> properties)
    {
        Map<String, Object> entry = new LinkedHashMap<>();

        DocumentReference reference = this.solrDocumentReferenceResolver.resolve(document);
        // The URL is always provided so that the "link" displayer of the title can build the link.
        entry.put(URL_PROPERTY, this.documentAccessBridge.getDocumentURL(reference, "view", null, null));
        // The full name is the idProperty of this source, so it is always provided (even when not a requested column)
        // for the live data widget to be able to identify the rows.
        entry.put(FULLNAME_PROPERTY, document.getFirstValue(SOLR_FIELDS.get(FULLNAME_PROPERTY)));

        List<String> requestedProperties =
            (properties == null || properties.isEmpty()) ? List.of(TITLE_PROPERTY) : properties;
        for (String property : requestedProperties) {
            String field = SOLR_FIELDS.get(property);
            // Unknown / unsupported properties (e.g. object properties) are ignored for now.
            // The title is read from the generic "title_" field (no name/fullName fallback): Solr indexes the
            // rendered title, so a blank title is intentional and the document name would be a misleading title.
            if (field != null) {
                entry.put(property, document.getFirstValue(field));
            }
        }
        return entry;
    }
}
