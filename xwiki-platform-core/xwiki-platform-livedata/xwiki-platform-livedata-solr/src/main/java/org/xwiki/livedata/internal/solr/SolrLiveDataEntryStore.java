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

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.livedata.LiveData;
import org.xwiki.livedata.LiveDataEntryStore;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.livedata.LiveDataQuery.Filter;
import org.xwiki.livedata.WithParameters;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.query.SecureQuery;
import org.xwiki.search.solr.SolrUtils;
import org.xwiki.search.solr.internal.api.FieldUtils;
import org.xwiki.security.SecurityConfiguration;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserPropertiesResolver;

/**
 * {@link LiveDataEntryStore} implementation backed by a Solr search query, exposing the matching documents as live
 * data entries.
 * <p>
 * Columns are referenced through {@code doc.*} property identifiers (the same ones used by the other live data sources
 * and the default configuration), which are mapped internally to the corresponding Solr fields; callers never deal
 * with raw Solr field names. The live data column filters and the sort are translated into Solr filter queries and
 * sort clauses.
 * <p>
 * The entity type to list is controlled by the {@link #TYPE_PARAMETER} source parameter, which defaults to
 * {@code document}. Only {@code document} is supported for now (support for other entity types such as attachments or
 * objects, and for object properties, is left for a later version); an unsupported value is rejected. This parameter is
 * the forward-compatible extension point: new types can be added without changing the {@code solr} source contract.
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
     * The source parameter holding the entity type to list. Defaults to {@link #DEFAULT_TYPE}; only {@code document}
     * is supported for now. This is the extension point for supporting other entity types later.
     */
    public static final String TYPE_PARAMETER = "type";

    /**
     * The live data property exposing the title of the matching document, rendered as a link.
     */
    protected static final String TITLE_PROPERTY = "doc.title";

    /**
     * The live data property exposing the view URL of the matching document. It is always added to each entry so that
     * the {@code link} displayer of the {@link #TITLE_PROPERTY} (and of {@link #FULLNAME_PROPERTY}) can build the link.
     */
    protected static final String URL_PROPERTY = "doc.url";

    /**
     * The live data property exposing the location (breadcrumb) of the matching document, rendered as HTML. It is a
     * derived column (not backed by a Solr field): its value is built from the document reference.
     */
    protected static final String LOCATION_PROPERTY = "doc.location";

    /**
     * The live data property holding the view URL of the last author's profile, used by the {@code link} displayer of
     * the {@code doc.author} column.
     */
    protected static final String AUTHOR_URL_PROPERTY = "doc.author_url";

    /**
     * The live data property holding the view URL of the creator's profile, used by the {@code link} displayer of the
     * {@code doc.creator} column.
     */
    protected static final String CREATOR_URL_PROPERTY = "doc.creator_url";

    /**
     * The live data property exposing the display name of the last author of the matching document.
     */
    protected static final String AUTHOR_PROPERTY = "doc.author";

    /**
     * The live data property exposing the display name of the creator of the matching document.
     */
    protected static final String CREATOR_PROPERTY = "doc.creator";

    /**
     * The live data property holding the unique identifier of the matching entry. Its value is the Solr document id
     * (unique per indexed document, including across wikis and per translation), so it is used as the
     * {@code idProperty} of this source and always added to each entry (even when not among the requested columns) for
     * the live data widget to be able to identify the rows.
     */
    protected static final String ID_PROPERTY = "doc.id";

    /**
     * The live data property holding the full name of the matching document, available as a (local, friendly) display
     * column when requested.
     */
    protected static final String FULLNAME_PROPERTY = "doc.fullName";

    /**
     * Maps the supported {@code doc.*} live data property identifiers to the Solr field holding their value.
     */
    protected static final Map<String, String> SOLR_FIELDS;

    /**
     * Maps the sortable {@code doc.*} live data property identifiers to the Solr field to sort on (a dedicated
     * {@code _sort} field for tokenized fields, the field itself for single-valued fields such as dates).
     */
    protected static final Map<String, String> SOLR_SORT_FIELDS;

    /**
     * The {@code doc.*} live data property identifiers backed by a Solr date field.
     */
    protected static final List<String> DATE_PROPERTIES;

    /**
     * The {@code doc.*} live data property identifier backed by a Solr boolean field.
     */
    protected static final String HIDDEN_PROPERTY = "doc.hidden";

    /**
     * The Solr query language id used to run the query (the {@code solr} query language registered by the Solr query
     * executor). Kept as a separate constant from {@link #ROLE_HINT} on purpose: even though their values currently
     * coincide, the component hint and the underlying query language id are independent concerns.
     */
    private static final String SOLR_QUERY_LANGUAGE = ROLE_HINT;

    private static final String DEFAULT_QUERY = "*:*";

    /**
     * The only entity type supported for now (see {@link #TYPE_PARAMETER}), also the default.
     */
    private static final String DEFAULT_TYPE = "document";

    /**
     * The Solr {@code type} field token matching the documents (see {@code FieldUtils#TYPE}).
     */
    private static final String DOCUMENT_SOLR_TYPE = "DOCUMENT";

    /**
     * The number of entries returned when the query does not specify a limit. A {@code null} limit must not be passed
     * as {@code 0} to Solr: the Solr query executor would then leave the {@code rows} parameter unset and fall back to
     * Solr's own small default (10), silently truncating the result. We default instead to the same page size as the
     * live data base configuration ({@code liveDataConfiguration.json}).
     */
    private static final int DEFAULT_LIMIT = 15;

    private static final String DOC_CREATION_DATE = "doc.creationDate";

    private static final String DOC_DATE = "doc.date";

    /**
     * Maps the {@code doc.*} user columns to the Solr field holding the serialized user reference, used to build the
     * link to the user profile.
     */
    private static final Map<String, String> USER_REFERENCE_FIELDS =
        Map.of(AUTHOR_PROPERTY, FieldUtils.AUTHOR, CREATOR_PROPERTY, FieldUtils.CREATOR);

    /**
     * Maps the {@code doc.*} user columns to the live data property holding their profile URL.
     */
    private static final Map<String, String> USER_URL_PROPERTIES =
        Map.of(AUTHOR_PROPERTY, AUTHOR_URL_PROPERTY, CREATOR_PROPERTY, CREATOR_URL_PROPERTY);

    // Live data filter operators (see the live data filter widgets).
    private static final String CONTAINS_OPERATOR = "contains";

    private static final String EQUALS_OPERATOR = "equals";

    private static final String STARTS_WITH_OPERATOR = "startsWith";

    private static final String BEFORE_OPERATOR = "before";

    private static final String AFTER_OPERATOR = "after";

    private static final String BETWEEN_OPERATOR = "between";

    private static final String WILDCARD = "*";

    private static final String AND_JOIN = " AND ";

    private static final String OR_JOIN = " OR ";

    private static final String OPEN_GROUP = "(";

    private static final String CLOSE_GROUP = ")";

    static {
        Map<String, String> fields = new LinkedHashMap<>();
        // The generic "title_" field aggregates the title of all locales (copyField of title_*) and uses the generic
        // tokenizer, so it can be queried/filtered without depending on the document locale.
        fields.put(TITLE_PROPERTY, FieldUtils.TITLE + FieldUtils.USCORE);
        fields.put("doc.name", FieldUtils.NAME);
        fields.put(FULLNAME_PROPERTY, FieldUtils.FULLNAME);
        fields.put(AUTHOR_PROPERTY, FieldUtils.AUTHOR_DISPLAY);
        fields.put(CREATOR_PROPERTY, FieldUtils.CREATOR_DISPLAY);
        fields.put(DOC_CREATION_DATE, FieldUtils.CREATIONDATE);
        fields.put(DOC_DATE, FieldUtils.DATE);
        fields.put(HIDDEN_PROPERTY, FieldUtils.HIDDEN);
        SOLR_FIELDS = Map.copyOf(fields);

        Map<String, String> sortFields = new LinkedHashMap<>();
        sortFields.put(TITLE_PROPERTY, FieldUtils.TITLE_SORT);
        // The author has a dedicated sort field (copyField author_display -> author_display_sort); the creator does
        // not, so it is intentionally left non-sortable.
        sortFields.put(AUTHOR_PROPERTY, FieldUtils.AUTHOR_DISPLAY_SORT);
        sortFields.put(DOC_CREATION_DATE, FieldUtils.CREATIONDATE);
        sortFields.put(DOC_DATE, FieldUtils.DATE);
        SOLR_SORT_FIELDS = Map.copyOf(sortFields);

        DATE_PROPERTIES = List.of(DOC_CREATION_DATE, DOC_DATE);
    }

    @Inject
    private QueryManager queryManager;

    /**
     * Builds the display values that need more than the raw Solr field value (view URL, location breadcrumb, formatted
     * dates, user profile URLs).
     */
    @Inject
    private SolrLiveDataDocumentFormatter documentFormatter;

    @Inject
    private UserPropertiesResolver userPropertiesResolver;

    @Inject
    private SolrUtils solrUtils;

    @Inject
    private SecurityConfiguration securityConfiguration;

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

            // Validate the requested entity type up-front (only "document" is supported for now).
            String typeClause = getTypeClause(parameters);

            String solrQueryString =
                StringUtils.defaultIfBlank((String) parameters.get(QUERY_PARAMETER), DEFAULT_QUERY);

            Query solrQuery = this.queryManager.createQuery(solrQueryString, SOLR_QUERY_LANGUAGE);
            // Enforce view rights: the Solr query executor removes from the result the documents the current user
            // cannot see.
            ((SecureQuery) solrQuery).checkCurrentUser(true);

            solrQuery.bindValue("fq", getFilterQueries(query, typeClause));
            List<String> sortClauses = getSortClauses(query);
            if (!sortClauses.isEmpty()) {
                solrQuery.bindValue("sort", StringUtils.join(sortClauses, ", "));
            }
            solrQuery.setOffset(query.getOffset() == null ? 0 : query.getOffset().intValue());
            solrQuery.setLimit(validateAndGetLimit(query.getLimit()));

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

    /**
     * Validates the requested limit against the configured maximum number of items a query is allowed to return, to
     * avoid denial of service through arbitrarily large result requests (mirrors the REST resources behavior).
     *
     * @param limit the limit carried by the query (may be {@code null})
     * @return the limit to use, never {@code 0} (see {@link #DEFAULT_LIMIT})
     * @throws LiveDataException if the requested limit is negative or exceeds the configured maximum
     */
    private int validateAndGetLimit(Integer limit) throws LiveDataException
    {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        int configuredLimit = this.securityConfiguration.getQueryItemsLimit();
        if (configuredLimit >= 0 && (limit < 0 || limit > configuredLimit)) {
            throw new LiveDataException(String.format(
                "Invalid limit value [%s]. The limit must be a positive integer lower than or equal to [%s].",
                limit, configuredLimit));
        }
        return limit;
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
     * Resolves the {@code type} source parameter into the Solr {@code type} filter query, rejecting any value other
     * than the supported {@link #DEFAULT_TYPE}. This is the extension point for supporting other entity types later.
     */
    private String getTypeClause(Map<String, Object> parameters) throws LiveDataException
    {
        String type = StringUtils.defaultIfBlank((String) parameters.get(TYPE_PARAMETER), DEFAULT_TYPE);
        if (!DEFAULT_TYPE.equals(type)) {
            throw new LiveDataException(
                String.format("Unsupported entity type [%s]. Only [%s] is supported.", type, DEFAULT_TYPE));
        }
        return FieldUtils.TYPE + ":(\"" + DOCUMENT_SOLR_TYPE + "\")";
    }

    /**
     * Builds the list of Solr filter queries ({@code fq}) from the live data column filters. Each filter becomes a
     * single {@code fq} clause (filters are combined with a logical AND), and the constraints inside a filter are
     * combined with AND or OR depending on {@link Filter#isMatchAll()}.
     */
    private List<String> getFilterQueries(LiveDataQuery query, String typeClause)
    {
        List<String> filterQueries = new ArrayList<>();
        // Restrict to the requested entity type (only documents for now).
        filterQueries.add(typeClause);
        // Exclude the hidden documents (e.g. technical pages) unless the current user chose to display them, so that
        // the result is consistent with the rest of XWiki (document index, live table, search).
        if (!this.userPropertiesResolver.resolve(CurrentUserReference.INSTANCE).displayHiddenDocuments()) {
            filterQueries.add(FieldUtils.HIDDEN + ":(false)");
        }

        if (query.getFilters() != null) {
            for (Filter filter : query.getFilters()) {
                toFilterQuery(filter).ifPresent(filterQueries::add);
            }
        }
        return filterQueries;
    }

    private Optional<String> toFilterQuery(Filter filter)
    {
        if (!SOLR_FIELDS.containsKey(filter.getProperty())) {
            // Unknown / unsupported property (e.g. an object property): ignore the filter for now.
            return Optional.empty();
        }
        List<String> clauses = filter.getConstraints().stream()
            .map(constraint -> toClause(filter.getProperty(), constraint.getOperator(), constraint.getValue()))
            .filter(StringUtils::isNotEmpty)
            .toList();
        if (clauses.isEmpty()) {
            return Optional.empty();
        }
        String operator = filter.isMatchAll() ? AND_JOIN : OR_JOIN;
        return Optional.of(OPEN_GROUP + StringUtils.join(clauses, operator) + CLOSE_GROUP);
    }

    /**
     * Returns the Solr field(s) a text filter on the passed property must match. Most properties map to a single field
     * ({@link #SOLR_FIELDS}); {@link #FULLNAME_PROPERTY} matches the tokenized, lowercased {@code name} field (for a
     * case-insensitive match on the page name) <em>and</em> the {@code fullname} {@code string} field (so the full
     * space path still matches, case-sensitively).
     */
    private List<String> getTextFilterFields(String property)
    {
        if (FULLNAME_PROPERTY.equals(property)) {
            return List.of(FieldUtils.NAME, FieldUtils.FULLNAME);
        }
        return List.of(SOLR_FIELDS.get(property));
    }

    /**
     * Translates a single live data filter constraint into a Solr clause, taking the constraint operator and the
     * property type (text, date or boolean) into account.
     */
    private String toClause(String property, String operator, Object value)
    {
        if (value == null || StringUtils.isEmpty(value.toString())) {
            return null;
        }
        String effectiveOperator = StringUtils.defaultString(operator);
        if (DATE_PROPERTIES.contains(property)) {
            return toDateClause(SOLR_FIELDS.get(property), effectiveOperator, value.toString());
        } else if (HIDDEN_PROPERTY.equals(property)) {
            // Boolean field: an exact match on true/false.
            return SOLR_FIELDS.get(property) + ':' + this.solrUtils.toFilterQueryString(value);
        } else {
            return toTextClause(getTextFilterFields(property), effectiveOperator, value);
        }
    }

    private String toTextClause(List<String> fields, String operator, Object value)
    {
        if (EQUALS_OPERATOR.equals(operator)) {
            // Exact match: a phrase query per field so the indexed tokens must appear in the exact same order.
            String phrase = escapePhrase(value.toString());
            return joinFields(fields, field -> field + ":\"" + phrase + '"');
        }
        // "contains" surrounds each typed word with wildcards (substring match), "startsWith" only appends a trailing
        // wildcard (prefix match). Wildcard queries bypass the tokenizer, so we apply them per word (the words are
        // split on whitespace); on tokenized text fields Solr lowercases the wildcard terms through its multiterm
        // analyzer, so the match is case-insensitive there. Each word is escaped through SolrUtils.
        boolean contains = !STARTS_WITH_OPERATOR.equals(operator);
        List<String> wordClauses = new ArrayList<>();
        for (String word : value.toString().trim().split("\\s+")) {
            String escaped = this.solrUtils.toFilterQueryString(word);
            if (StringUtils.isNotEmpty(escaped)) {
                String wildcard = contains ? WILDCARD + escaped + WILDCARD : escaped + WILDCARD;
                wordClauses.add(joinFields(fields, field -> field + ':' + wildcard));
            }
        }
        if (wordClauses.isEmpty()) {
            return null;
        }
        // The words are AND-ed together. No extra grouping parentheses are added here: each (possibly multi-field) word
        // clause is already self-contained, the caller (toFilterQuery) wraps the whole constraint, and Lucene gives AND
        // a higher precedence than the OR that joins the fields and the constraints.
        return StringUtils.join(wordClauses, AND_JOIN);
    }

    /**
     * Builds the clause matching any of the passed fields: a single field is emitted bare (e.g. {@code title_:*ba*}),
     * several fields are OR-ed inside a parenthesized group (e.g. {@code (name:*ba* OR fullname:*ba*)}) so the OR does
     * not leak into the surrounding AND precedence.
     */
    private String joinFields(List<String> fields, Function<String, String> clauseBuilder)
    {
        if (fields.size() == 1) {
            return clauseBuilder.apply(fields.get(0));
        }
        return OPEN_GROUP + StringUtils.join(fields.stream().map(clauseBuilder).toList(), OR_JOIN) + CLOSE_GROUP;
    }

    /**
     * Translates a date filter constraint into a Solr range query. The live data date filter serializes its value as
     * an ISO 8601 instant (or, for the {@code between} operator, two instants separated by a {@code /}); Solr expects
     * UTC instants, so the values are normalized before being injected in a range query.
     */
    private String toDateClause(String field, String operator, String value)
    {
        String effectiveOperator = StringUtils.isEmpty(operator) ? BETWEEN_OPERATOR : operator;
        if (BETWEEN_OPERATOR.equals(effectiveOperator)) {
            String[] bounds = value.split("/", 2);
            return toRangeClause(field, toSolrDate(bounds[0]), bounds.length > 1 ? toSolrDate(bounds[1]) : WILDCARD);
        } else if (BEFORE_OPERATOR.equals(effectiveOperator)) {
            return toRangeClause(field, WILDCARD, toSolrDate(value));
        } else if (AFTER_OPERATOR.equals(effectiveOperator)) {
            return toRangeClause(field, toSolrDate(value), WILDCARD);
        }
        // Unsupported date operator: ignore the constraint rather than produce an invalid Solr query.
        return null;
    }

    private String toRangeClause(String field, String from, String to)
    {
        if (from == null || to == null) {
            return null;
        }
        return field + ":[" + from + " TO " + to + ']';
    }

    private String toSolrDate(String isoDate)
    {
        if (WILDCARD.equals(isoDate)) {
            return WILDCARD;
        }
        try {
            // The live data date filter sends ISO 8601 instants, possibly with a timezone offset; Solr expects UTC.
            return OffsetDateTime.parse(isoDate).toInstant().toString();
        } catch (DateTimeParseException e) {
            try {
                // Some configurations send a local date-time without a timezone offset: assume UTC rather than dropping
                // the bound (which would silently turn the whole date filter into a no-op).
                return LocalDateTime.parse(isoDate).atOffset(ZoneOffset.UTC).toInstant().toString();
            } catch (DateTimeParseException nested) {
                // The value is not a parseable date-time: drop the bound rather than emit an invalid range.
                return null;
            }
        }
    }

    private String escapePhrase(String value)
    {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
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

        // The URL is always provided so that the "link" displayer of the title (and full name) can build the link.
        entry.put(URL_PROPERTY, this.documentFormatter.getDocumentUrl(document));
        // The Solr document id is the idProperty of this source, so it is always provided (even when not a requested
        // column) for the live data widget to be able to uniquely identify the rows.
        entry.put(ID_PROPERTY, document.getFirstValue(FieldUtils.ID));

        List<String> requestedProperties =
            (properties == null || properties.isEmpty()) ? List.of(TITLE_PROPERTY) : properties;
        for (String property : requestedProperties) {
            addPropertyValue(entry, document, property);
        }
        return entry;
    }

    /**
     * Adds to the entry the value of a single requested column, plus any side property its displayer needs (the
     * profile URL for the user columns). Unsupported properties are silently skipped: the live data resolver provides
     * a default descriptor for them so the (empty) column still renders.
     */
    private void addPropertyValue(Map<String, Object> entry, SolrDocument document, String property)
    {
        if (LOCATION_PROPERTY.equals(property)) {
            entry.put(property, this.documentFormatter.buildLocationHtml(document));
        } else if (USER_REFERENCE_FIELDS.containsKey(property)) {
            // The display name is read from the *_display Solr field; the profile URL is built from the serialized
            // user reference stored in the author/creator field.
            entry.put(property, document.getFirstValue(SOLR_FIELDS.get(property)));
            entry.put(USER_URL_PROPERTIES.get(property),
                this.documentFormatter.getUserProfileUrl(document.getFirstValue(USER_REFERENCE_FIELDS.get(property))));
        } else if (DATE_PROPERTIES.contains(property)) {
            // Format the date with the wiki date format so that the column shows a proper date and not a raw number.
            entry.put(property, this.documentFormatter.formatDate(document.getFirstValue(SOLR_FIELDS.get(property))));
        } else {
            String field = SOLR_FIELDS.get(property);
            // The title is read from the generic "title_" field (no name/fullName fallback): Solr indexes the
            // rendered title, so a blank title is intentional and the document name would be a misleading title.
            if (field != null) {
                entry.put(property, document.getFirstValue(field));
            }
        }
    }
}
