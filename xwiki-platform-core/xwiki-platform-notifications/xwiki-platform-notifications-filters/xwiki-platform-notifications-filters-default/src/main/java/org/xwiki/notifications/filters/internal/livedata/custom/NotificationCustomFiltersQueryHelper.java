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

package org.xwiki.notifications.filters.internal.livedata.custom;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.query.internal.DefaultQueryParameter;

/**
 * Helper to perform DB queries for retrieving notification custom filters with live data filters and sort.
 * TODO: maybe this could be improved with some APIs to put in NotificationFilterPreferenceStore.
 *
 * @version $Id$
 * @since 16.3.0RC1
 */
@Component(roles = NotificationCustomFiltersQueryHelper.class)
@Singleton
public class NotificationCustomFiltersQueryHelper
{
    private static final String BASE_QUERY = "select nfp from DefaultNotificationFilterPreference nfp "
        + "where owner = :owner";
    private static final String OWNER_BINDING = "owner";
    private static final String STARTS_WITH_OPERATOR = "startsWith";
    private static final String CONTAINS_OPERATOR = "contains";
    private static final String EQUALS_OPERATOR = "equals";
    private static final String AND = " and ";

    @Inject
    private QueryManager queryManager;

    private static final class FiltersHQLQuery
    {
        private String whereClause;
        private final Map<String, Object> bindings = new LinkedHashMap<>();
    }

    private Optional<FiltersHQLQuery> handleFilter(List<LiveDataQuery.Filter> queryFilters)
    {
        FiltersHQLQuery result = new FiltersHQLQuery();
        List<String> queryWhereClauses = new ArrayList<>();
        for (LiveDataQuery.Filter queryFilter : queryFilters) {
            switch (queryFilter.getProperty()) {
                case NotificationCustomFiltersLiveDataConfigurationProvider.IS_ENABLED_FIELD ->
                    this.handleIsEnabledFilter(queryFilter, queryWhereClauses);

                case NotificationCustomFiltersLiveDataConfigurationProvider.NOTIFICATION_FORMATS_FIELD ->
                    this.handleNotificationFormatsFilter(queryFilter, queryWhereClauses);

                case NotificationCustomFiltersLiveDataConfigurationProvider.SCOPE_FIELD ->
                    this.handleScopeFilter(queryFilter, queryWhereClauses);

                case NotificationCustomFiltersLiveDataConfigurationProvider.FILTER_TYPE_FIELD ->
                    this.handleFilterTypeFilter(queryFilter, queryWhereClauses, result);

                case NotificationCustomFiltersLiveDataConfigurationProvider.LOCATION_FIELD ->
                    this.handleLocationFilter(queryFilter, queryWhereClauses, result);

                case NotificationCustomFiltersLiveDataConfigurationProvider.EVENT_TYPES_FIELD ->
                    this.handleEventTypeFilter(queryFilter, queryWhereClauses, result);

                default -> {
                }
            }
        }
        if (!queryWhereClauses.isEmpty()) {
            result.whereClause = queryWhereClauses.stream().collect(Collectors.joining(AND, AND, ""));
            return Optional.of(result);
        } else {
            return Optional.empty();
        }
    }

    private void handleFilterTypeFilter(LiveDataQuery.Filter queryFilter, List<String> queryWhereClauses,
        FiltersHQLQuery result)
    {
        // We authorize only a single constraint here
        LiveDataQuery.Constraint constraint = queryFilter.getConstraints().get(0);
        if (EQUALS_OPERATOR.equals(constraint.getOperator())) {
            queryWhereClauses.add("nfp.filterType = :filterType");
            result.bindings.put("filterType",
                NotificationFilterType.valueOf(String.valueOf(constraint.getValue())));
        }
    }

    private void handleScopeFilter(LiveDataQuery.Filter queryFilter, List<String> queryWhereClauses)
    {
        // We authorize only a single constraint here
        LiveDataQuery.Constraint constraint = queryFilter.getConstraints().get(0);
        if (EQUALS_OPERATOR.equals(constraint.getOperator())
            && !StringUtils.isBlank(String.valueOf(constraint.getValue()))) {
            NotificationCustomFiltersLiveDataConfigurationProvider.Scope scope =
                NotificationCustomFiltersLiveDataConfigurationProvider.Scope.valueOf(
                    String.valueOf(constraint.getValue()));
            queryWhereClauses.add(String.format("length(nfp.%s) > 0", scope.getFieldName()));
        }
    }

    private void handleNotificationFormatsFilter(LiveDataQuery.Filter queryFilter, List<String> queryWhereClauses)
    {
        // We authorize only a single constraint here
        LiveDataQuery.Constraint constraint = queryFilter.getConstraints().get(0);
        String constraintValue = String.valueOf(constraint.getValue());
        if (EQUALS_OPERATOR.equals(constraint.getOperator())
            && !StringUtils.isEmpty(constraintValue)) {
            if (NotificationFormat.ALERT.name().equals(constraintValue)) {
                queryWhereClauses.add("nfp.alertEnabled = true");
            }
            if (NotificationFormat.EMAIL.name().equals(constraintValue)) {
                queryWhereClauses.add("nfp.emailEnabled = true");
            }
        }
    }

    private void handleIsEnabledFilter(LiveDataQuery.Filter queryFilter, List<String> queryWhereClauses)
    {
        // We only check first constraint: if there's more they are either redundant or contradictory.
        LiveDataQuery.Constraint constraint = queryFilter.getConstraints().get(0);
        if (Boolean.parseBoolean(String.valueOf(constraint.getValue()))) {
            queryWhereClauses.add("nfp.enabled = true");
        } else {
            queryWhereClauses.add("nfp.enabled = false");
        }
    }

    private void handleEventTypeFilter(LiveDataQuery.Filter queryFilter, List<String> queryWhereClauses,
        FiltersHQLQuery result)
    {
        // We authorize only a single constraint here
        LiveDataQuery.Constraint constraint = queryFilter.getConstraints().get(0);
        if (EQUALS_OPERATOR.equals(constraint.getOperator())) {
            if (NotificationCustomFiltersLiveDataConfigurationProvider.ALL_EVENTS_OPTION_VALUE.equals(
                constraint.getValue()))
            {
                queryWhereClauses.add("length(nfp.allEventTypes) = 0");
            } else {
                queryWhereClauses.add("nfp.allEventTypes like :eventTypes");
                DefaultQueryParameter queryParameter = new DefaultQueryParameter(null);
                queryParameter.anyChars().literal(String.valueOf(constraint.getValue())).anyChars();
                result.bindings.put("eventTypes", queryParameter);
            }
        }
    }

    private void handleLocationFilter(LiveDataQuery.Filter locationFilter, List<String> queryWhereClauses,
        FiltersHQLQuery result)
    {
        List<String> clauses = new ArrayList<>();
        int clauseCounter = 0;

        // for searching in location we actually need to look in 4 columns in DB, so we reuse same constraint binding
        // in those 4 columns, and since we might have multiple constraints we use a counter to name those bindings
        String clauseValue = "(nfp.pageOnly like :constraint_%1$s or nfp.page like :constraint_%1$s or "
            + "nfp.wiki like :constraint_%1$s or nfp.user like :constraint_%1$s)";
        String constraintName = "constraint_%s";

        for (LiveDataQuery.Constraint constraint : locationFilter.getConstraints()) {
            if (EQUALS_OPERATOR.equals(constraint.getOperator())) {
                clauses.add(String.format("(nfp.pageOnly = :constraint_%1$s or nfp.page = :constraint_%1$s or "
                    + "nfp.wiki = :constraint_%1$s or nfp.user = :constraint_%1$s)", clauseCounter));
                DefaultQueryParameter queryParameter = new DefaultQueryParameter(null);
                queryParameter.literal(String.valueOf(constraint.getValue()));
                result.bindings.put(String.format(constraintName, clauseCounter), queryParameter);
            } else if (STARTS_WITH_OPERATOR.equals(constraint.getOperator())) {
                clauses.add(String.format(clauseValue, clauseCounter));
                DefaultQueryParameter queryParameter = new DefaultQueryParameter(null);
                queryParameter.literal(String.valueOf(constraint.getValue())).anyChars();
                result.bindings.put(String.format(constraintName, clauseCounter), queryParameter);
            } else if (CONTAINS_OPERATOR.equals(constraint.getOperator())) {
                clauses.add(String.format(clauseValue, clauseCounter));
                DefaultQueryParameter queryParameter = new DefaultQueryParameter(null);
                queryParameter.anyChars().literal(String.valueOf(constraint.getValue())).anyChars();
                result.bindings.put(String.format(constraintName, clauseCounter), queryParameter);
            }
            clauseCounter++;
        }
        if (!clauses.isEmpty()) {
            queryWhereClauses.add(buildQueryClause(locationFilter, clauses));
        }
    }

    private String buildQueryClause(LiveDataQuery.Filter filter, List<String> clauses)
    {
        String operatorAppender;
        if (filter.isMatchAll()) {
            operatorAppender = AND;
        } else {
            operatorAppender = " or ";
        }
        return clauses.stream().collect(Collectors.joining(operatorAppender, "(", ")"));
    }

    private Query getHQLQuery(LiveDataQuery query, boolean isCount, String owner, WikiReference wikiReference)
        throws QueryException, LiveDataException
    {
        String baseQuery = (isCount) ? "select count(nfp.id) "
            + "from DefaultNotificationFilterPreference nfp where owner = :owner" : BASE_QUERY;
        Optional<FiltersHQLQuery> optionalFiltersHQLQuery = handleFilter(query.getFilters());
        if (optionalFiltersHQLQuery.isPresent()) {
            baseQuery += optionalFiltersHQLQuery.get().whereClause;
        }
        if (!isCount) {
            baseQuery += handleSortEntries(query.getSort());
        }
        Query hqlQuery = this.queryManager.createQuery(baseQuery, Query.HQL)
            .bindValue(OWNER_BINDING, owner)
            .setWiki(wikiReference.getName());
        if (optionalFiltersHQLQuery.isPresent()) {
            for (Map.Entry<String, Object> binding : optionalFiltersHQLQuery.get().bindings.entrySet()) {
                hqlQuery = hqlQuery.bindValue(binding.getKey(), binding.getValue());
            }
        }
        return hqlQuery;
    }

    /**
     * Count the total number of filter preferences for given owner on given wiki.
     *
     * @param query the query to use for applying the filters
     * @param owner the owner for which to count all filter preferences
     * @param wikiReference the wiki where to count all filter preferences
     * @return the total number of filter preferences
     * @throws QueryException in case of problem to perform the query
     * @throws LiveDataException in case of problem with the livedata query
     */
    public long countTotalFilters(LiveDataQuery query, String owner, WikiReference wikiReference)
        throws QueryException, LiveDataException
    {
        return getHQLQuery(query, true, owner, wikiReference)
            .<Long>execute()
            .get(0);
    }

    private String handleSortEntries(List<LiveDataQuery.SortEntry> sortEntries) throws LiveDataException
    {
        List<String> clauses = new ArrayList<>();
        for (LiveDataQuery.SortEntry sortEntry : sortEntries) {
            String sortOperator = (sortEntry.isDescending()) ? "desc" : "asc";
            String clause = switch (sortEntry.getProperty()) {
                case NotificationCustomFiltersLiveDataConfigurationProvider.IS_ENABLED_FIELD ->
                    String.format("nfp.enabled %s", sortOperator);

                case NotificationCustomFiltersLiveDataConfigurationProvider.NOTIFICATION_FORMATS_FIELD ->
                    handleNotificationFormatSort(sortEntry);

                case NotificationCustomFiltersLiveDataConfigurationProvider.LOCATION_FIELD,
                    NotificationCustomFiltersLiveDataConfigurationProvider.SCOPE_FIELD ->
                    handleLocationSort(sortEntry);

                case NotificationCustomFiltersLiveDataConfigurationProvider.FILTER_TYPE_FIELD ->
                    String.format("nfp.filterType %s", sortOperator);

                case NotificationCustomFiltersLiveDataConfigurationProvider.EVENT_TYPES_FIELD ->
                    String.format("nfp.allEventTypes %s", sortOperator);

                case NotificationCustomFiltersLiveDataConfigurationProvider.ID_FIELD ->
                    String.format("nfp.internalId %s", sortOperator);

                default -> throw new LiveDataException("Unexpected sort value: " + sortEntry.getProperty());
            };
            clauses.add(clause);
        }
        if (!clauses.isEmpty()) {
            return clauses.stream().collect(Collectors.joining(", ", " order by ", ""));
        } else {
            return "";
        }
    }

    private String handleLocationSort(LiveDataQuery.SortEntry sortEntry)
    {
        if (sortEntry.isDescending()) {
            return "nfp.user desc, nfp.wiki desc, nfp.page desc, nfp.pageOnly desc";
        } else {
            return "nfp.pageOnly asc, nfp.page asc, nfp.wiki asc, nfp.user asc";
        }
    }

    private String handleNotificationFormatSort(LiveDataQuery.SortEntry sortEntry)
    {
        // We use on purpose asc/desc in combination to ensure having both formats in the middle of the data
        // since it's the most common: by doing that we'll have [only alerts, both formats, only emails]
        if (sortEntry.isDescending()) {
            return "nfp.emailEnabled asc, nfp.alertEnabled desc";
        } else {
            return "nfp.alertEnabled asc, nfp.emailEnabled desc";
        }
    }

    /**
     * Retrieve all filter preferences matching the live data query for the given owner on the given wiki.
     *
     * @param query the livedata query containing filters and sort criteria
     * @param owner the owner of the filter preferences to retrieve
     * @param wikiReference the wiki where to retrieve the filter preferences
     * @return the list of filter preferences
     * @throws QueryException in case of problem to perform the query
     * @throws LiveDataException in case of problem with the livedata query
     */
    public List<NotificationFilterPreference> getFilterPreferences(LiveDataQuery query, String owner,
        WikiReference wikiReference) throws QueryException, LiveDataException
    {
        return getHQLQuery(query, false, owner, wikiReference)
            .setLimit(query.getLimit())
            .setOffset(query.getOffset().intValue())
            .execute();
    }
}
