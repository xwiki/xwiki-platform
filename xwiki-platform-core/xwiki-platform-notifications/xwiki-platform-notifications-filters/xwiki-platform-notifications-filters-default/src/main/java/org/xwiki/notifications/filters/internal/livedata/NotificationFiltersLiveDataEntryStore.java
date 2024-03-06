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
package org.xwiki.notifications.filters.internal.livedata;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.script.ScriptContext;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.livedata.LiveData;
import org.xwiki.livedata.LiveDataEntryStore;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.internal.NotificationFilterPreferenceStore;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.query.internal.DefaultQueryParameter;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.TemplateManager;

import com.xpn.xwiki.XWikiContext;

/**
 * Dedicated {@link LiveDataEntryStore} for the {@link NotificationFiltersLiveDataSource}.
 * This component is in charge of performing the actual HQL queries to display the live data.
 *
 * @version $Id$
 */
@Component
@Singleton
@Named(NotificationFiltersLiveDataSource.NAME)
public class NotificationFiltersLiveDataEntryStore implements LiveDataEntryStore
{
    private static final String STARTS_WITH_OPERATOR = "startsWith";
    private static final String CONTAINS_OPERATOR = "contains";
    private static final String EQUALS_OPERATOR = "equals";
    private static final String EMAIL_FORMAT = "email";
    private static final String ALERT_FORMAT = "alert";
    private static final String LOCATION_TEMPLATE = "notification/filters/livedatalocation.vm";
    private static final String TARGET_SOURCE_PARAMETER = "target";
    private static final String WIKI_SOURCE_PARAMETER = "wiki";

    @Inject
    private NotificationFilterPreferenceStore notificationFilterPreferenceStore;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private NotificationFilterManager notificationFilterManager;

    @Inject
    private EntityReferenceResolver<String> entityReferenceResolver;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    @Named("html/5.0")
    private BlockRenderer blockRenderer;

    @Inject
    private QueryManager queryManager;

    @Inject
    private NotificationFilterLiveDataTranslationHelper translationHelper;

    @Inject
    private TemplateManager templateManager;

    @Inject
    private ScriptContextManager scriptContextManager;

    @Override
    public Optional<Map<String, Object>> get(Object entryId) throws LiveDataException
    {
        Optional<Map<String, Object>> result = Optional.empty();
        WikiReference wikiReference = contextProvider.get().getWikiReference();
        Optional<NotificationFilterPreference> filterPreferenceOpt;
        try {
            filterPreferenceOpt = notificationFilterPreferenceStore
                .getFilterPreference(String.valueOf(entryId), wikiReference);
            if (filterPreferenceOpt.isPresent()) {
                NotificationFilterPreference filterPreference = filterPreferenceOpt.get();
                result = Optional.of(getPreferenceInformation(filterPreference));
            }
        } catch (NotificationException e) {
            throw new LiveDataException(
                String.format("Error while retrieving LiveData entry for notification filter with id [%s]",
                entryId), e);
        }

        return result;
    }

    private Map<String, Object> getPreferenceInformation(NotificationFilterPreference filterPreference)
        throws NotificationException, LiveDataException
    {
        NotificationFiltersLiveDataConfigurationProvider.Scope scope = getScope(filterPreference);

        // FIXME: Check authorization?
        return Map.of(
            NotificationFiltersLiveDataConfigurationProvider.ID_FIELD, filterPreference.getId(),
            NotificationFiltersLiveDataConfigurationProvider.EVENT_TYPES_FIELD,
            this.displayEventTypes(filterPreference),
            NotificationFiltersLiveDataConfigurationProvider.NOTIFICATION_FORMATS_FIELD,
            displayNotificationFormats(filterPreference),
            NotificationFiltersLiveDataConfigurationProvider.SCOPE_FIELD, getScopeInfo(scope),
            NotificationFiltersLiveDataConfigurationProvider.LOCATION_FIELD,
            this.displayLocation(filterPreference, scope),
            NotificationFiltersLiveDataConfigurationProvider.DISPLAY_FIELD, this.renderDisplay(filterPreference),
            NotificationFiltersLiveDataConfigurationProvider.FILTER_TYPE_FIELD,
            this.translationHelper.getFilterTypeTranslation(filterPreference.getFilterType()),
            NotificationFiltersLiveDataConfigurationProvider.IS_ENABLED_FIELD, displayIsEnabled(filterPreference),
            "isEnabled_checked", filterPreference.isEnabled(),
            "isEnabled_data", Map.of(
                "preferenceId", filterPreference.getId()
            )
        );
    }

    private String displayIsEnabled(NotificationFilterPreference filterPreference)
    {
        String checked = (filterPreference.isEnabled()) ? "checked=\"checked\"" : "";
        String disabled = (filterPreference.getId().startsWith("watchlist_")) ? "disabled=\"disabled\"" : "";
        String html = "<input type=\"checkbox\" class=\"notificationFilterPreferenceCheckbox\" "
            + "data-preferenceId=\"%s\" %s %s />";
        return String.format(html, filterPreference.getId(), checked, disabled);
    }

    private String displayLocation(NotificationFilterPreference filterPreference,
        NotificationFiltersLiveDataConfigurationProvider.Scope scope)
    {
        EntityReference location = null;
        switch (scope) {
            case WIKI -> location = this.entityReferenceResolver.resolve(filterPreference.getWiki(), EntityType.WIKI);
            case SPACE -> location = this.entityReferenceResolver.resolve(filterPreference.getPage(), EntityType.SPACE);
            case PAGE -> location = this.entityReferenceResolver.resolve(filterPreference.getPageOnly(),
                EntityType.DOCUMENT);
            case USER -> location = this.entityReferenceResolver.resolve(filterPreference.getUser(),
                EntityType.DOCUMENT);
        }
        // FIXME: Do we need a new execution context?
        ScriptContext currentScriptContext = this.scriptContextManager.getCurrentScriptContext();
        currentScriptContext.setAttribute("location", location, ScriptContext.ENGINE_SCOPE);
        return this.templateManager.renderNoException(LOCATION_TEMPLATE);
    }

    private String displayEventTypes(NotificationFilterPreference filterPreference) throws LiveDataException
    {
        StringBuilder result = new StringBuilder("<ul class=\"list-unstyled\">");
        Set<String> eventTypes = filterPreference.getEventTypes();
        if (eventTypes.isEmpty()) {
            result.append("<li>");
            result.append(this.translationHelper.getAllEventTypesTranslation());
            result.append("<li>");
        } else {
            for (String eventType : eventTypes) {
                result.append("<li>");
                result.append(this.translationHelper.getEventTypeTranslation(eventType));
                result.append("</li>");
            }
        }
        result.append("</ul>");
        return result.toString();
    }

    private String displayNotificationFormats(NotificationFilterPreference filterPreference)
    {
        StringBuilder result = new StringBuilder("<ul class=\"list-unstyled\">");
        String translationPrefix = "notifications.format.";

        for (NotificationFormat notificationFormat : filterPreference.getNotificationFormats()) {
            result.append("<li>");
            result.append(this.translationHelper.getTranslationWithPrefix(translationPrefix,
                notificationFormat.name().toLowerCase()));
            result.append("</li>");
        }
        result.append("</ul>");

        return result.toString();
    }

    private Map<String, String> getScopeInfo(NotificationFiltersLiveDataConfigurationProvider.Scope scope)
    {
        String icon = "";
        switch (scope) {
            case WIKI -> icon = "wiki";
            case SPACE -> icon = "chart-organisation";
            case PAGE -> icon = "page";
            case USER -> icon = "user";
        }
        return Map.of("icon", icon, "name", this.translationHelper.getScopeTranslation(scope));
    }

    private String renderDisplay(NotificationFilterPreference filterPreference) throws LiveDataException
    {
        String result = "";
        WikiPrinter printer = new DefaultWikiPrinter();
        String missingComponentExceptionMessage = String.format("Cannot find NotificationFilter component for "
            + "preference named [%s]", filterPreference.getFilterName());
        if (this.componentManager.hasComponent(NotificationFilter.class, filterPreference.getFilterName())) {
            try {
                NotificationFilter filter =
                    this.componentManager.getInstance(NotificationFilter.class, filterPreference.getFilterName());
                Block block = this.notificationFilterManager.displayFilter(filter, filterPreference);
                blockRenderer.render(block, printer);
                result = printer.toString();
            } catch (Exception e) {
                throw new LiveDataException("Error while rendering a block for notification filter", e);
            }
        } else {
            throw new LiveDataException(missingComponentExceptionMessage);
        }

        return result;
    }

    private NotificationFiltersLiveDataConfigurationProvider.Scope getScope(NotificationFilterPreference
        filterPreference)
    {
        if (!StringUtils.isBlank(filterPreference.getUser())) {
            return NotificationFiltersLiveDataConfigurationProvider.Scope.USER;
        } else if (!StringUtils.isBlank(filterPreference.getPageOnly())) {
            return NotificationFiltersLiveDataConfigurationProvider.Scope.PAGE;
        } else if (!StringUtils.isBlank(filterPreference.getPage())) {
            return NotificationFiltersLiveDataConfigurationProvider.Scope.SPACE;
        } else {
            return NotificationFiltersLiveDataConfigurationProvider.Scope.WIKI;
        }
    }

    private static class FiltersHQLQuery
    {
        String whereClause;
        Map<String, Object> bindings = new LinkedHashMap<>();
    }

    private Optional<FiltersHQLQuery> handleFilter(List<LiveDataQuery.Filter> queryFilters)
    {
        FiltersHQLQuery result = new FiltersHQLQuery();
        List<String> queryWhereClauses = new ArrayList<>();
        for (LiveDataQuery.Filter queryFilter : queryFilters) {
            switch (queryFilter.getProperty()) {
                case NotificationFiltersLiveDataConfigurationProvider.IS_ENABLED_FIELD -> {
                    // We only check first constraint: if there's more they are either redundant or contradictory.
                    LiveDataQuery.Constraint constraint = queryFilter.getConstraints().get(0);
                    if (Boolean.parseBoolean(String.valueOf(constraint.getValue()))) {
                        queryWhereClauses.add("nfp.enabled = 1");
                    } else {
                        queryWhereClauses.add("nfp.enabled = 0");
                    }
                }

                case NotificationFiltersLiveDataConfigurationProvider.NOTIFICATION_FORMATS_FIELD -> {
                    handleFormatFilter(queryFilter, queryWhereClauses);
                }

                case NotificationFiltersLiveDataConfigurationProvider.SCOPE_FIELD -> {
                    // We authorize only a single constraint here
                    LiveDataQuery.Constraint constraint = queryFilter.getConstraints().get(0);
                    if (EQUALS_OPERATOR.equals(constraint.getOperator())
                        && !StringUtils.isBlank(String.valueOf(constraint.getValue()))) {
                        NotificationFiltersLiveDataConfigurationProvider.Scope scope =
                            NotificationFiltersLiveDataConfigurationProvider.Scope.valueOf(
                                String.valueOf(constraint.getValue()));
                        queryWhereClauses.add(String.format("length(nfp.%s) > 0 ", scope.getFieldName()));
                    }
                }

                case NotificationFiltersLiveDataConfigurationProvider.FILTER_TYPE_FIELD -> {
                    // We authorize only a single constraint here
                    LiveDataQuery.Constraint constraint = queryFilter.getConstraints().get(0);
                    if (EQUALS_OPERATOR.equals(constraint.getOperator())) {
                        queryWhereClauses.add("nfp.filterType = :filterType");
                        result.bindings.put("filterType",
                            NotificationFilterType.valueOf(String.valueOf(constraint.getValue())));
                    }
                }

                case NotificationFiltersLiveDataConfigurationProvider.LOCATION_FIELD -> {
                    Optional<FiltersHQLQuery> locationQueryOpt = this.handleLocationFilter(queryFilter);
                    if (locationQueryOpt.isPresent()) {
                        FiltersHQLQuery locationHQLQuery = locationQueryOpt.get();
                        queryWhereClauses.add(locationHQLQuery.whereClause);
                        result.bindings.putAll(locationHQLQuery.bindings);
                    }
                }

                case NotificationFiltersLiveDataConfigurationProvider.EVENT_TYPES_FIELD -> {
                    // We authorize only a single constraint here
                    // FIXME: Actually maybe we should allow specifying multiple equals constraints?
                    LiveDataQuery.Constraint constraint = queryFilter.getConstraints().get(0);
                    if (EQUALS_OPERATOR.equals(constraint.getOperator())) {
                        if (NotificationFiltersLiveDataConfigurationProvider.ALL_EVENTS_OPTION_VALUE.equals(
                            constraint.getValue()))
                        {
                            queryWhereClauses.add("length(nfp.allEventTypes) = 0 ");
                        } else {
                            queryWhereClauses.add("nfp.allEventTypes like :eventTypes");
                            DefaultQueryParameter queryParameter = new DefaultQueryParameter(null);
                            queryParameter.anyChars().literal(String.valueOf(constraint.getValue())).anyChars();
                            result.bindings.put("eventTypes", queryParameter);
                        }
                    }
                }
            }
        }
        if (!queryWhereClauses.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder(" and ");
            Iterator<String> iterator = queryWhereClauses.iterator();
            while (iterator.hasNext()) {
                stringBuilder.append(iterator.next());
                if (iterator.hasNext()) {
                    stringBuilder.append(" and ");
                }
            }
            result.whereClause = stringBuilder.toString();
            return Optional.of(result);
        } else {
            return Optional.empty();
        }
    }

    private Optional<FiltersHQLQuery> handleLocationFilter(LiveDataQuery.Filter locationFilter)
    {
        FiltersHQLQuery filtersHQLQuery = new FiltersHQLQuery();
        List<String> clauses = new ArrayList<>();
        int clauseCounter = 0;
        String clauseValue = "(nfp.pageOnly like :constraint_%1$s or nfp.page like :constraint_%1$s or "
            + "nfp.wiki like :constraint_%1$s or nfp.user like :constraint_%1$s)";
        String constraintName = "constraint_%s";
        for (LiveDataQuery.Constraint constraint : locationFilter.getConstraints()) {
            if (EQUALS_OPERATOR.equals(constraint.getOperator())) {
                clauses.add(String.format("(nfp.pageOnly = :constraint_%1$s or nfp.page = :constraint_%1$s or "
                    + "nfp.wiki = :constraint_%1$s or nfp.user = :constraint_%1$s)", clauseCounter));
                DefaultQueryParameter queryParameter = new DefaultQueryParameter(null);
                queryParameter.literal(String.valueOf(constraint.getValue()));
                filtersHQLQuery.bindings.put(String.format(constraintName, clauseCounter), queryParameter);
            } else if (STARTS_WITH_OPERATOR.equals(constraint.getOperator())) {
                clauses.add(String.format(clauseValue, clauseCounter));
                DefaultQueryParameter queryParameter = new DefaultQueryParameter(null);
                queryParameter.literal(String.valueOf(constraint.getValue())).anyChars();
                filtersHQLQuery.bindings.put(String.format(constraintName, clauseCounter), queryParameter);
            } else if (CONTAINS_OPERATOR.equals(constraint.getOperator())) {
                clauses.add(String.format(clauseValue, clauseCounter));
                DefaultQueryParameter queryParameter = new DefaultQueryParameter(null);
                queryParameter.anyChars().literal(String.valueOf(constraint.getValue())).anyChars();
                filtersHQLQuery.bindings.put(String.format(constraintName, clauseCounter), queryParameter);
            }
            clauseCounter++;
        }
        if (!clauses.isEmpty()) {
            filtersHQLQuery.whereClause = buildQueryClause(locationFilter, clauses);
            return Optional.of(filtersHQLQuery);
        } else {
            return Optional.empty();
        }
    }

    private String buildQueryClause(LiveDataQuery.Filter filter, List<String> clauses)
    {
        StringBuilder result = new StringBuilder("(");
        String operatorAppender;
        if (filter.isMatchAll()) {
            operatorAppender = " and ";
        } else {
            operatorAppender = " or ";
        }
        Iterator<String> iterator = clauses.iterator();
        while (iterator.hasNext()) {
            result.append(iterator.next());
            if (iterator.hasNext()) {
                result.append(operatorAppender);
            }
        }
        result.append(")");
        return result.toString();
    }

    private void handleFormatFilter(LiveDataQuery.Filter formatFilter, List<String> queryWhereClauses)
    {
        List<String> clauses = new ArrayList<>();
        for (LiveDataQuery.Constraint constraint : formatFilter.getConstraints()) {
            boolean isEmailEnabled = false;
            boolean isAlertEnabled = false;
            String constraintValue = String.valueOf(constraint.getValue());
            if (StringUtils.isNotBlank(constraintValue)) {
                if (STARTS_WITH_OPERATOR.equals(constraint.getOperator())) {
                    isEmailEnabled = EMAIL_FORMAT.startsWith(constraintValue);
                    isAlertEnabled = ALERT_FORMAT.startsWith(constraintValue);
                } else if (CONTAINS_OPERATOR.equals(constraint.getOperator())) {
                    isEmailEnabled = EMAIL_FORMAT.contains(constraintValue);
                    isAlertEnabled = ALERT_FORMAT.contains(constraintValue);
                } else if (EQUALS_OPERATOR.equals(constraint.getOperator())) {
                    isEmailEnabled = EMAIL_FORMAT.equals(constraintValue);
                    isAlertEnabled = ALERT_FORMAT.equals(constraintValue);
                }
                if (isEmailEnabled) {
                    clauses.add("nfp.emailEnabled = 1");
                } else {
                    clauses.add("nfp.emailEnabled = 0");
                }
                if (isAlertEnabled) {
                    clauses.add("nfp.alertEnabled = 1");
                } else {
                    clauses.add("nfp.alertEnabled = 0");
                }
            }
        }
        if (!clauses.isEmpty()) {
            queryWhereClauses.add(buildQueryClause(formatFilter, clauses));
        }
    }

    private String handleSortEntries(List<LiveDataQuery.SortEntry> sortEntries)
    {
        List<String> clauses = new ArrayList<>();
        for (LiveDataQuery.SortEntry sortEntry : sortEntries) {
            String sortOperator = "asc";
            if (sortEntry.isDescending()) {
                sortOperator = "desc";
            }
            switch (sortEntry.getProperty()) {
                case NotificationFiltersLiveDataConfigurationProvider.IS_ENABLED_FIELD ->
                    clauses.add(String.format("nfp.enabled %s", sortOperator));

                case NotificationFiltersLiveDataConfigurationProvider.NOTIFICATION_FORMATS_FIELD -> {
                    if (sortEntry.isDescending()) {
                        clauses.add("nfp.alertEnabled desc, nfp.emailEnabled asc");
                    } else {
                        clauses.add("nfp.alertEnabled asc, nfp.emailEnabled desc");
                    }
                }

                case NotificationFiltersLiveDataConfigurationProvider.LOCATION_FIELD,
                    NotificationFiltersLiveDataConfigurationProvider.SCOPE_FIELD -> {
                    if (sortEntry.isDescending()) {
                        clauses.add("nfp.user desc, nfp.wiki desc, nfp.page desc, nfp.pageOnly desc");
                    } else {
                        clauses.add("nfp.pageOnly asc, nfp.page asc, nfp.wiki asc, nfp.user asc");
                    }
                }

                case NotificationFiltersLiveDataConfigurationProvider.FILTER_TYPE_FIELD ->
                    clauses.add(String.format("nfp.filterType %s", sortOperator));

                case NotificationFiltersLiveDataConfigurationProvider.EVENT_TYPES_FIELD ->
                    clauses.add(String.format("nfp.allEventTypes %s", sortOperator));
            }
        }
        if (!clauses.isEmpty()) {
            StringBuilder result = new StringBuilder(" order by ");
            Iterator<String> iterator = clauses.iterator();
            while (iterator.hasNext()) {
                result.append(iterator.next());
                if (iterator.hasNext()) {
                    result.append(", ");
                }
            }
            return result.toString();
        } else {
            return "";
        }
    }

    @Override
    public LiveData get(LiveDataQuery query) throws LiveDataException
    {
        if (query.getOffset() > Integer.MAX_VALUE) {
            throw new LiveDataException("Currently only integer offsets are supported.");
        }
        Map<String, Object> sourceParameters = query.getSource().getParameters();
        if (!sourceParameters.containsKey(TARGET_SOURCE_PARAMETER)) {
            throw new LiveDataException("The target source parameter is mandatory.");
        }
        String target = String.valueOf(sourceParameters.get(TARGET_SOURCE_PARAMETER));
        EntityReference ownerReference;
        if (WIKI_SOURCE_PARAMETER.equals(target)) {
            ownerReference =
                this.entityReferenceResolver.resolve(String.valueOf(sourceParameters.get(WIKI_SOURCE_PARAMETER)),
                EntityType.WIKI);
        } else {
            ownerReference = this.entityReferenceResolver.resolve(String.valueOf(sourceParameters.get(target)),
                EntityType.DOCUMENT);
        }
        String serializedOwner = this.entityReferenceSerializer.serialize(ownerReference);

        LiveData liveData = new LiveData();
        String baseQuery = "select nfp from DefaultNotificationFilterPreference nfp where owner = :owner";
        Optional<FiltersHQLQuery> optionalFiltersHQLQuery = handleFilter(query.getFilters());
        if (optionalFiltersHQLQuery.isPresent()) {
            baseQuery += optionalFiltersHQLQuery.get().whereClause;
        }
        baseQuery += handleSortEntries(query.getSort());
        try {
            Query hqlQuery = this.queryManager.createQuery(baseQuery, Query.HQL)
                .bindValue("owner", serializedOwner)
                .setWiki(ownerReference.extractReference(EntityType.WIKI).getName());
            if (optionalFiltersHQLQuery.isPresent()) {
                for (Map.Entry<String, Object> binding : optionalFiltersHQLQuery.get().bindings.entrySet()) {
                    hqlQuery = hqlQuery.bindValue(binding.getKey(), binding.getValue());
                }
            }
            List<NotificationFilterPreference> notificationFilterPreferences = hqlQuery
                .setLimit(query.getLimit())
                .setOffset(query.getOffset().intValue())
                .execute();
            liveData.setCount(notificationFilterPreferences.size());
            List<Map<String, Object>> entries = liveData.getEntries();
            for (NotificationFilterPreference notificationFilterPreference : notificationFilterPreferences) {
                entries.add(getPreferenceInformation(notificationFilterPreference));
            }
        } catch (QueryException | NotificationException e) {
            throw new LiveDataException("Error when querying notification filter preferences", e);
        }
        return liveData;
    }
}
