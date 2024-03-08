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
import java.util.Comparator;
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
import org.xwiki.notifications.filters.internal.DefaultNotificationFilterPreference;
import org.xwiki.notifications.filters.internal.NotificationFilterPreferenceStore;
import org.xwiki.notifications.filters.internal.livedata.NotificationFilterLiveDataTranslationHelper;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.query.internal.DefaultQueryParameter;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.template.TemplateManager;

import com.xpn.xwiki.XWikiContext;

/**
 * Dedicated {@link LiveDataEntryStore} for the {@link NotificationCustomFiltersLiveDataSource}.
 * This component is in charge of performing the actual HQL queries to display the live data.
 *
 * @version $Id$
 */
@Component
@Singleton
@Named(NotificationCustomFiltersLiveDataSource.NAME)
public class NotificationCustomFiltersLiveDataEntryStore implements LiveDataEntryStore
{
    private static final String WIKI = "wiki";
    private static final String STARTS_WITH_OPERATOR = "startsWith";
    private static final String CONTAINS_OPERATOR = "contains";
    private static final String EQUALS_OPERATOR = "equals";
    private static final String LOCATION_TEMPLATE = "notification/filters/livedatalocation.vm";
    private static final String TARGET_SOURCE_PARAMETER = "target";
    private static final String WIKI_SOURCE_PARAMETER = WIKI;
    private static final String UNAUTHORIZED_EXCEPTION_MSG = "You don't have rights to access those information.";
    private static final String AND = " and ";

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

    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @Override
    public Optional<Map<String, Object>> get(Object entryId) throws LiveDataException
    {
        Optional<Map<String, Object>> result = Optional.empty();
        XWikiContext context = contextProvider.get();
        WikiReference wikiReference = context.getWikiReference();
        Optional<NotificationFilterPreference> filterPreferenceOpt;
        try {
            filterPreferenceOpt = notificationFilterPreferenceStore
                .getFilterPreference(String.valueOf(entryId), wikiReference);
            if (filterPreferenceOpt.isPresent()) {
                DefaultNotificationFilterPreference filterPreference =
                    (DefaultNotificationFilterPreference) filterPreferenceOpt.get();
                if (this.contextualAuthorizationManager.hasAccess(Right.ADMIN)
                    || filterPreference.getOwner().equals(
                        this.entityReferenceSerializer.serialize(context.getUserReference()))) {
                    result = Optional.of(getPreferenceInformation(filterPreference));
                } else {
                    throw new LiveDataException(UNAUTHORIZED_EXCEPTION_MSG);
                }
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
        NotificationCustomFiltersLiveDataConfigurationProvider.Scope scope = getScope(filterPreference);

        Map<String, Object> result = new LinkedHashMap<>();
        // Map.of only accept 10 args
        result.put(NotificationCustomFiltersLiveDataConfigurationProvider.ID_FIELD, filterPreference.getId());
        result.put(NotificationCustomFiltersLiveDataConfigurationProvider.EVENT_TYPES_FIELD,
            this.displayEventTypes(filterPreference));
        result.put(NotificationCustomFiltersLiveDataConfigurationProvider.NOTIFICATION_FORMATS_FIELD,
            displayNotificationFormats(filterPreference));
        result.put(NotificationCustomFiltersLiveDataConfigurationProvider.SCOPE_FIELD, getScopeInfo(scope));
        result.put(NotificationCustomFiltersLiveDataConfigurationProvider.LOCATION_FIELD,
            this.displayLocation(filterPreference, scope));
        result.put(NotificationCustomFiltersLiveDataConfigurationProvider.DISPLAY_FIELD,
            this.renderDisplay(filterPreference));
        result.put(NotificationCustomFiltersLiveDataConfigurationProvider.FILTER_TYPE_FIELD,
            this.translationHelper.getFilterTypeTranslation(filterPreference.getFilterType()));
        result.put(NotificationCustomFiltersLiveDataConfigurationProvider.IS_ENABLED_FIELD,
            displayIsEnabled(filterPreference));
        result.put("isEnabled_checked", filterPreference.isEnabled());
        result.put("isEnabled_data", Map.of(
            "preferenceId", filterPreference.getId()
        ));
        // We don't care: if we access the LD we do have delete.
        result.put(NotificationCustomFiltersLiveDataConfigurationProvider.DOC_HAS_DELETE_FIELD, true);
        return result;
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
        NotificationCustomFiltersLiveDataConfigurationProvider.Scope scope)
    {
        EntityReference location;
        switch (scope) {
            case USER:
                location = this.entityReferenceResolver.resolve(filterPreference.getUser(), EntityType.DOCUMENT);
                break;

            case WIKI:
                location = this.entityReferenceResolver.resolve(filterPreference.getWiki(), EntityType.WIKI);
                break;

            case SPACE:
                location = this.entityReferenceResolver.resolve(filterPreference.getPage(), EntityType.SPACE);
                break;

            default:
            case PAGE:
                location = this.entityReferenceResolver.resolve(filterPreference.getPageOnly(), EntityType.DOCUMENT);
                break;
        }
        // FIXME: Do we need a new execution context?
        ScriptContext currentScriptContext = this.scriptContextManager.getCurrentScriptContext();
        currentScriptContext.setAttribute("location", location, ScriptContext.ENGINE_SCOPE);
        return this.templateManager.renderNoException(LOCATION_TEMPLATE);
    }

    private String getUnstyledList(List<String> items)
    {
        StringBuilder result = new StringBuilder("<ul class=\"list-unstyled\">");
        for (String item : items) {
            result.append("<li>");
            result.append(item);
            result.append("</li>");
        }
        result.append("</ul>");
        return result.toString();
    }

    private String displayEventTypes(NotificationFilterPreference filterPreference) throws LiveDataException
    {
        String result;
        List<String> eventTypes = new ArrayList<>(filterPreference.getEventTypes());
        // Ensure to always have same order
        eventTypes.sort(Comparator.naturalOrder());
        if (eventTypes.isEmpty()) {
            result = getUnstyledList(List.of(this.translationHelper.getAllEventTypesTranslation()));
        } else {
            List<String> items = new ArrayList<>();
            for (String eventType : eventTypes) {
                items.add(this.translationHelper.getEventTypeTranslation(eventType));
            }
            result = getUnstyledList(items);
        }
        return result;
    }

    private String displayNotificationFormats(NotificationFilterPreference filterPreference)
    {
        List<String> items = new ArrayList<>();
        List<NotificationFormat> notificationFormats = new ArrayList<>(filterPreference.getNotificationFormats());
        // Ensure to always have same order
        notificationFormats.sort(Comparator.comparing(NotificationFormat::name));
        for (NotificationFormat notificationFormat : notificationFormats) {
            items.add(this.translationHelper.getFormatTranslation(notificationFormat));
        }

        return getUnstyledList(items);
    }

    private Map<String, String> getScopeInfo(NotificationCustomFiltersLiveDataConfigurationProvider.Scope scope)
    {
        String icon;
        switch (scope) {
            case USER:
                icon = "user";
                break;

            case WIKI:
                icon = WIKI;
                break;

            case SPACE:
                icon = "chart-organisation";
                break;

            default:
            case PAGE:
                icon = "page";
                break;
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

    private NotificationCustomFiltersLiveDataConfigurationProvider.Scope getScope(NotificationFilterPreference
        filterPreference)
    {
        if (!StringUtils.isBlank(filterPreference.getUser())) {
            return NotificationCustomFiltersLiveDataConfigurationProvider.Scope.USER;
        } else if (!StringUtils.isBlank(filterPreference.getPageOnly())) {
            return NotificationCustomFiltersLiveDataConfigurationProvider.Scope.PAGE;
        } else if (!StringUtils.isBlank(filterPreference.getPage())) {
            return NotificationCustomFiltersLiveDataConfigurationProvider.Scope.SPACE;
        } else {
            return NotificationCustomFiltersLiveDataConfigurationProvider.Scope.WIKI;
        }
    }

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
            StringBuilder stringBuilder = new StringBuilder(AND);
            Iterator<String> iterator = queryWhereClauses.iterator();
            while (iterator.hasNext()) {
                stringBuilder.append(iterator.next());
                if (iterator.hasNext()) {
                    stringBuilder.append(AND);
                }
            }
            result.whereClause = stringBuilder.toString();
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
            queryWhereClauses.add(String.format("length(nfp.%s) > 0 ", scope.getFieldName()));
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
                queryWhereClauses.add("nfp.alertEnabled = 1");
            }
            if (NotificationFormat.EMAIL.name().equals(constraintValue)) {
                queryWhereClauses.add("nfp.emailEnabled = 1");
            }
        }
    }

    private void handleIsEnabledFilter(LiveDataQuery.Filter queryFilter, List<String> queryWhereClauses)
    {
        // We only check first constraint: if there's more they are either redundant or contradictory.
        LiveDataQuery.Constraint constraint = queryFilter.getConstraints().get(0);
        if (Boolean.parseBoolean(String.valueOf(constraint.getValue()))) {
            queryWhereClauses.add("nfp.enabled = 1");
        } else {
            queryWhereClauses.add("nfp.enabled = 0");
        }
    }

    private void handleEventTypeFilter(LiveDataQuery.Filter queryFilter, List<String> queryWhereClauses,
        FiltersHQLQuery result)
    {
        // We authorize only a single constraint here
        // FIXME: Actually maybe we should allow specifying multiple equals constraints?
        LiveDataQuery.Constraint constraint = queryFilter.getConstraints().get(0);
        if (EQUALS_OPERATOR.equals(constraint.getOperator())) {
            if (NotificationCustomFiltersLiveDataConfigurationProvider.ALL_EVENTS_OPTION_VALUE.equals(
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

    private void handleLocationFilter(LiveDataQuery.Filter locationFilter, List<String> queryWhereClauses,
        FiltersHQLQuery result)
    {
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
        StringBuilder result = new StringBuilder("(");
        String operatorAppender;
        if (filter.isMatchAll()) {
            operatorAppender = AND;
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

    private String handleSortEntries(List<LiveDataQuery.SortEntry> sortEntries)
    {
        List<String> clauses = new ArrayList<>();
        for (LiveDataQuery.SortEntry sortEntry : sortEntries) {
            String sortOperator = "asc";
            if (sortEntry.isDescending()) {
                sortOperator = "desc";
            }
            switch (sortEntry.getProperty()) {
                case NotificationCustomFiltersLiveDataConfigurationProvider.IS_ENABLED_FIELD ->
                    clauses.add(String.format("nfp.enabled %s", sortOperator));

                case NotificationCustomFiltersLiveDataConfigurationProvider.NOTIFICATION_FORMATS_FIELD -> {
                    if (sortEntry.isDescending()) {
                        clauses.add("nfp.alertEnabled desc, nfp.emailEnabled asc");
                    } else {
                        clauses.add("nfp.alertEnabled asc, nfp.emailEnabled desc");
                    }
                }

                case NotificationCustomFiltersLiveDataConfigurationProvider.LOCATION_FIELD,
                    NotificationCustomFiltersLiveDataConfigurationProvider.SCOPE_FIELD -> {
                    if (sortEntry.isDescending()) {
                        clauses.add("nfp.user desc, nfp.wiki desc, nfp.page desc, nfp.pageOnly desc");
                    } else {
                        clauses.add("nfp.pageOnly asc, nfp.page asc, nfp.wiki asc, nfp.user asc");
                    }
                }

                case NotificationCustomFiltersLiveDataConfigurationProvider.FILTER_TYPE_FIELD ->
                    clauses.add(String.format("nfp.filterType %s", sortOperator));

                case NotificationCustomFiltersLiveDataConfigurationProvider.EVENT_TYPES_FIELD ->
                    clauses.add(String.format("nfp.allEventTypes %s", sortOperator));

                default -> {
                }
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
        XWikiContext context = this.contextProvider.get();
        if (!this.contextualAuthorizationManager.hasAccess(Right.ADMIN)
            && !ownerReference.equals(context.getUserReference())) {
            throw new LiveDataException(UNAUTHORIZED_EXCEPTION_MSG);
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
