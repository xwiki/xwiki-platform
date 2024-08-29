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
package org.xwiki.eventstream.store.solr.internal;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.eventstream.EntityEvent;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.Event.Importance;
import org.xwiki.eventstream.EventQuery;
import org.xwiki.eventstream.EventSearchResult;
import org.xwiki.eventstream.EventStatus;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.internal.AbstractAsynchronousEventStore;
import org.xwiki.eventstream.internal.DefaultEvent;
import org.xwiki.eventstream.internal.DefaultEventStatus;
import org.xwiki.eventstream.internal.StreamEventSearchResult;
import org.xwiki.eventstream.query.AbstractPropertyQueryCondition;
import org.xwiki.eventstream.query.CompareQueryCondition;
import org.xwiki.eventstream.query.CompareQueryCondition.CompareType;
import org.xwiki.eventstream.query.GroupQueryCondition;
import org.xwiki.eventstream.query.InQueryCondition;
import org.xwiki.eventstream.query.MailEntityQueryCondition;
import org.xwiki.eventstream.query.PageableEventQuery;
import org.xwiki.eventstream.query.QueryCondition;
import org.xwiki.eventstream.query.SimpleEventQuery;
import org.xwiki.eventstream.query.SortableEventQuery;
import org.xwiki.eventstream.query.SortableEventQuery.SortClause;
import org.xwiki.eventstream.query.SortableEventQuery.SortClause.Order;
import org.xwiki.eventstream.query.StatusQueryCondition;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.SolrUtils;

/**
 * Solr based implementation of {@link EventStore}.
 *
 * @version $Id$
 * @since 12.4RC1
 */
@Component
@Singleton
@Named("solr")
public class SolrEventStore extends AbstractAsynchronousEventStore
{
    private static final Map<String, SearchFieldMapping> SEARCH_FIELD_MAPPING = new HashMap<>();

    private static class SearchFieldMapping
    {
        String solrFieldName;

        Type type;

        SearchFieldMapping(String solrFieldName)
        {
            this.solrFieldName = solrFieldName;
        }

        SearchFieldMapping(Type type)
        {
            this.type = type;
        }

        SearchFieldMapping(String solrFieldName, Type type)
        {
            this.solrFieldName = solrFieldName;
            this.type = type;
        }
    }

    static {
        SEARCH_FIELD_MAPPING.put(Event.FIELD_DOCUMENT,
            new SearchFieldMapping(EventsSolrCoreInitializer.FIELD_DOCUMENT_INDEX));
        SEARCH_FIELD_MAPPING.put(Event.FIELD_SPACE,
            new SearchFieldMapping(EventsSolrCoreInitializer.FIELD_SPACE_INDEX));
        SEARCH_FIELD_MAPPING.put(Event.FIELD_RELATEDENTITY, new SearchFieldMapping(EntityReference.class));
        SEARCH_FIELD_MAPPING.put(Event.FIELD_IMPORTANCE, new SearchFieldMapping(Importance.class));
        SEARCH_FIELD_MAPPING.put(Event.FIELD_USER, new SearchFieldMapping(DocumentReference.class));
    }

    @Inject
    private Solr solr;

    @Inject
    private SolrUtils utils;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    @Named("compact")
    private EntityReferenceSerializer<String> compact;

    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactwiki;

    private SolrClient client;

    @Override
    public void initialize() throws InitializationException
    {
        initialize(100, false, true);

        try {
            this.client = this.solr.getClient(EventsSolrCoreInitializer.NAME);
        } catch (SolrException e) {
            throw new InitializationException("Failed to get the events Solr core", e);
        }
    }

    @Override
    protected void afterTasks(List<EventStoreTask<?, ?>> tasks)
    {
        try {
            commit();
        } catch (EventStreamException e) {
            this.logger.error("Failed to commit", e);
        }

        super.afterTasks(tasks);
    }

    @Override
    protected Event syncSaveEvent(Event event) throws EventStreamException
    {
        try {
            this.client.add(toSolrInputDocument(event));
        } catch (Exception e) {
            throw new EventStreamException("Failed to save event", e);
        }

        return event;
    }

    @Override
    protected EventStatus syncSaveEventStatus(EventStatus status) throws EventStreamException
    {
        saveEventStatus(status.getEvent().getId(), status.getEntityId(), status.isRead(), !status.isRead());

        return status;
    }

    @Override
    protected EntityEvent syncSaveMailEntityEvent(EntityEvent event) throws EventStreamException
    {
        saveMailEntityEvent(event.getEvent().getId(), event.getEntityId(), true);

        return event;
    }

    @Override
    protected Optional<EventStatus> syncDeleteEventStatus(EventStatus status) throws EventStreamException
    {
        saveEventStatus(status.getEvent().getId(), status.getEntityId(), false, false);

        return Optional.of(status);
    }

    @Override
    protected Void syncDeleteEventStatuses(String entityId, Date date) throws EventStreamException
    {
        SimpleEventQuery query = new SimpleEventQuery();
        query.withStatus(entityId);
        if (date != null) {
            query.lessOrEq(Event.FIELD_DATE, date);
        }
        EventSearchResult results = search(query, Collections.singleton(Event.FIELD_ID));

        for (Iterator<Event> it = results.stream().iterator(); it.hasNext();) {
            Event event = it.next();

            saveEventStatus(event.getId(), entityId, false, false);
        }

        return null;
    }

    @Override
    protected Optional<EntityEvent> syncDeleteMailEntityEvent(EntityEvent event) throws EventStreamException
    {
        saveMailEntityEvent(event.getEvent().getId(), event.getEntityId(), false);

        return Optional.of(event);
    }

    @Override
    protected Event syncPrefilterEvent(Event event) throws EventStreamException
    {
        SolrInputDocument document = new SolrInputDocument();

        this.utils.set(EventsSolrCoreInitializer.SOLR_FIELD_ID, event.getId(), document);

        this.utils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET, Event.FIELD_PREFILTERED, true, document);

        try {
            this.client.add(document);
        } catch (Exception e) {
            throw new EventStreamException(
                String.format("Failed to to set the event [%s] as prefiltered", event.getId()), e);
        }

        // Update the event so that we return something with the right value
        if (event instanceof DefaultEvent) {
            event.setPrefiltered(true);
        }

        return event;
    }

    private void saveEventStatus(String eventId, String entityId, boolean read, boolean unread)
        throws EventStreamException
    {
        SolrInputDocument document = new SolrInputDocument();

        this.utils.set(EventsSolrCoreInitializer.SOLR_FIELD_ID, eventId, document);

        this.utils.setAtomic(
            read ? SolrUtils.ATOMIC_UPDATE_MODIFIER_ADD_DISTINCT : SolrUtils.ATOMIC_UPDATE_MODIFIER_REMOVE,
            EventsSolrCoreInitializer.SOLR_FIELD_READLISTENERS, entityId, document);
        this.utils.setAtomic(
            unread ? SolrUtils.ATOMIC_UPDATE_MODIFIER_ADD_DISTINCT : SolrUtils.ATOMIC_UPDATE_MODIFIER_REMOVE,
            EventsSolrCoreInitializer.SOLR_FIELD_UNREADLISTENERS, entityId, document);

        try {
            this.client.add(document);
        } catch (Exception e) {
            throw new EventStreamException(
                String.format("Failed to update the event status for event [%s] and entity id [%s]", eventId, entityId),
                e);
        }
    }

    private void saveMailEntityEvent(String eventId, String entityId, boolean add) throws EventStreamException
    {
        SolrInputDocument document = new SolrInputDocument();

        this.utils.set(EventsSolrCoreInitializer.SOLR_FIELD_ID, eventId, document);

        this.utils.setAtomic(
            add ? SolrUtils.ATOMIC_UPDATE_MODIFIER_ADD_DISTINCT : SolrUtils.ATOMIC_UPDATE_MODIFIER_REMOVE,
            EventsSolrCoreInitializer.SOLR_FIELD_MAILLISTENERS, entityId, document);

        try {
            this.client.add(document);
        } catch (Exception e) {
            throw new EventStreamException(String.format(
                "Failed to update the event mail status for event [%s] and entity id [%s]", eventId, entityId), e);
        }
    }

    private SolrInputDocument toSolrInputDocument(Event event)
    {
        if (event == null) {
            return null;
        }

        SolrInputDocument document = new SolrInputDocument();

        this.utils.set(EventsSolrCoreInitializer.SOLR_FIELD_ID, event.getId(), document);

        this.utils.set(Event.FIELD_APPLICATION, event.getApplication(), document);
        this.utils.set(Event.FIELD_BODY, event.getBody(), document);
        this.utils.set(Event.FIELD_DATE, event.getDate(), document);
        this.utils.set(Event.FIELD_REMOTE_OBSERVATION_ID, event.getRemoteObservationId(), document);
        this.utils.setString(Event.FIELD_DOCUMENT, event.getDocument(), document);
        this.utils.set(Event.FIELD_DOCUMENTTITLE, event.getDocumentTitle(), document);
        this.utils.set(Event.FIELD_DOCUMENTVERSION, event.getDocumentVersion(), document);
        this.utils.set(Event.FIELD_GROUPID, event.getGroupId(), document);
        this.utils.set(Event.FIELD_HIDDEN, event.getHidden(), document);
        this.utils.set(Event.FIELD_PREFILTERED, event.isPrefiltered(), document);
        this.utils.setString(Event.FIELD_IMPORTANCE, event.getImportance(), document);
        this.utils.setString(Event.FIELD_RELATEDENTITY, event.getRelatedEntity(), EntityReference.class, document);
        this.utils.setString(Event.FIELD_SPACE, event.getSpace(), document);
        this.utils.set(Event.FIELD_STREAM, event.getStream(), document);
        this.utils.set(Event.FIELD_TARGET, event.getTarget(), document);
        this.utils.set(Event.FIELD_TITLE, event.getTitle(), document);
        this.utils.set(Event.FIELD_TYPE, event.getType(), document);
        this.utils.setString(Event.FIELD_URL, event.getUrl(), document);
        this.utils.setString(Event.FIELD_USER, event.getUser(), document);
        this.utils.setString(Event.FIELD_WIKI, event.getWiki(), document);

        this.utils.setMap(EventsSolrCoreInitializer.SOLR_FIELD_PROPERTIES, event.getCustom(), document);

        // Support various relative forms of the reference fields
        if (event.getDocument() != null) {
            this.utils.set(EventsSolrCoreInitializer.FIELD_DOCUMENT_INDEX,
                Arrays.asList(this.serializer.serialize(event.getDocument()),
                    this.compactwiki.serialize(event.getDocument(), event.getWiki()),
                    this.compact.serialize(event.getDocument(), event.getSpace())),
                document);
        }
        if (event.getSpace() != null) {
            this.utils.set(EventsSolrCoreInitializer.FIELD_SPACE_INDEX,
                Arrays.asList(this.serializer.serialize(event.getSpace()),
                    this.compact.serialize(event.getSpace(), event.getWiki())),
                document);
        }

        return document;
    }

    @Override
    protected Optional<Event> syncDeleteEvent(String eventId) throws EventStreamException
    {
        Optional<Event> event = getEvent(eventId);

        if (event.isPresent()) {
            deleteById(eventId);
        }

        return event;
    }

    @Override
    protected Optional<Event> syncDeleteEvent(Event event) throws EventStreamException
    {
        return syncDeleteEvent(event.getId());
    }

    private void deleteById(String eventId) throws EventStreamException
    {
        try {
            this.client.deleteById(eventId);
        } catch (Exception e) {
            throw new EventStreamException("Failed to delete the event", e);
        }
    }

    private void commit() throws EventStreamException
    {
        try {
            this.client.commit();
        } catch (Exception e) {
            throw new EventStreamException("Failed to commit", e);
        }
    }

    public SolrDocument getEventDocument(String eventId) throws SolrServerException, IOException
    {
        return this.client.getById(eventId);
    }

    @Override
    public Optional<Event> getEvent(String eventId) throws EventStreamException
    {
        SolrDocument document;
        try {
            document = getEventDocument(eventId);
        } catch (Exception e) {
            throw new EventStreamException("Failed to get Solr document with id [" + eventId + "]", e);
        }

        return Optional.ofNullable(toEvent(document));
    }

    @Override
    public List<EventStatus> getEventStatuses(Collection<Event> events, Collection<String> entityIds) throws Exception
    {
        SolrQuery solrQuery = new SolrQuery();

        solrQuery.addFilterQuery(serializeInCondition(EventsSolrCoreInitializer.SOLR_FIELD_ID,
            events.stream().map(Event::getId).collect(Collectors.toList())));

        solrQuery.addFilterQuery(serializeInCondition(EventsSolrCoreInitializer.SOLR_FIELD_READLISTENERS, entityIds)
            + " OR " + serializeInCondition(EventsSolrCoreInitializer.SOLR_FIELD_UNREADLISTENERS, entityIds));

        // Without it the query will only return 10 first results.
        solrQuery.setRows(events.size());

        QueryResponse response;
        try {
            response = this.client.query(solrQuery);
        } catch (Exception e) {
            throw new EventStreamException("Failed to execute Solr query", e);
        }

        SolrDocumentList documents = response.getResults();

        List<EventStatus> statuses = new ArrayList<>(documents.size() * entityIds.size());
        for (SolrDocument solrDocument : documents) {
            Event event = toEvent(solrDocument);

            Set<String> readListeners =
                this.utils.getSet(EventsSolrCoreInitializer.SOLR_FIELD_READLISTENERS, solrDocument);
            Set<String> unreadListeners =
                this.utils.getSet(EventsSolrCoreInitializer.SOLR_FIELD_UNREADLISTENERS, solrDocument);

            for (String entityId : entityIds) {
                boolean read = readListeners != null && readListeners.contains(entityId);
                boolean unread = unreadListeners != null && unreadListeners.contains(entityId);

                if (read || unread) {
                    statuses.add(new DefaultEventStatus(event, entityId, read));
                }
            }
        }

        return statuses;
    }

    private Event toEvent(SolrDocument document)
    {
        if (document == null) {
            return null;
        }

        DefaultEvent event = new DefaultEvent();

        event.setId(this.utils.get(EventsSolrCoreInitializer.SOLR_FIELD_ID, document));

        event.setApplication(this.utils.get(Event.FIELD_APPLICATION, document));
        event.setBody(this.utils.get(Event.FIELD_BODY, document));
        event.setDate(this.utils.get(Event.FIELD_DATE, document));
        event.setRemoteObservationId(this.utils.get(Event.FIELD_REMOTE_OBSERVATION_ID, document));
        event.setDocument(this.utils.get(Event.FIELD_DOCUMENT, document, DocumentReference.class));
        event.setDocumentTitle(this.utils.get(Event.FIELD_DOCUMENTTITLE, document));
        event.setDocumentVersion(this.utils.get(Event.FIELD_DOCUMENTVERSION, document));
        event.setGroupId(this.utils.get(Event.FIELD_GROUPID, document));
        event.setImportance(this.utils.get(Event.FIELD_IMPORTANCE, document, Importance.class));
        event.setRelatedEntity(this.utils.get(Event.FIELD_RELATEDENTITY, document, EntityReference.class));
        event.setSpace(this.utils.get(Event.FIELD_SPACE, document, SpaceReference.class));
        event.setStream(this.utils.get(Event.FIELD_STREAM, document));
        event.setTarget(this.utils.getSet(Event.FIELD_TARGET, document));
        event.setTitle(this.utils.get(Event.FIELD_TITLE, document));
        event.setType(this.utils.get(Event.FIELD_TYPE, document));
        event.setUrl(this.utils.get(Event.FIELD_URL, document, URL.class));
        event.setUser(this.utils.get(Event.FIELD_USER, document, DocumentReference.class));
        event.setWiki(this.utils.get(Event.FIELD_WIKI, document, WikiReference.class));

        event.setHidden(this.utils.get(Event.FIELD_HIDDEN, document, false));
        event.setPrefiltered(this.utils.get(Event.FIELD_PREFILTERED, document, false));

        event.setParameters(this.utils.getMap(EventsSolrCoreInitializer.SOLR_FIELD_PROPERTIES, document));

        return event;
    }

    private SolrQuery toSolrQuery(EventQuery query, Set<String> fields)
    {
        SolrQuery solrQuery = new SolrQuery();

        if (CollectionUtils.isNotEmpty(fields)) {
            fields.forEach(solrQuery::setFields);
        }

        if (query instanceof PageableEventQuery) {
            PageableEventQuery pageableQuery = (PageableEventQuery) query;

            if (pageableQuery.getOffset() > 0) {
                solrQuery.setStart((int) pageableQuery.getOffset());
            }

            // FIXME: this should probably be fixed in the future, we shouldn't allow to try retrieving unlimited
            // results since it's not allowed by Solr API.
            if (pageableQuery.getLimit() >= 0) {
                solrQuery.setRows((int) pageableQuery.getLimit());
            } else {
                solrQuery.setRows(Integer.MAX_VALUE - 1);
            }
        }

        if (query instanceof SortableEventQuery) {
            for (SortClause sort : ((SortableEventQuery) query).getSorts()) {
                solrQuery.addSort(toSolrFieldName(sort), sort.getOrder() == Order.ASC ? ORDER.asc : ORDER.desc);
            }
        }

        if (query instanceof SimpleEventQuery) {
            SimpleEventQuery simpleQuery = (SimpleEventQuery) query;

            addConditions(simpleQuery.isOr() && simpleQuery.getConditions().size() > 1
                ? Collections.singletonList(simpleQuery) : simpleQuery.getConditions(), solrQuery);
        }

        return solrQuery;
    }

    private void addConditions(List<QueryCondition> conditions, SolrQuery solrQuery)
    {
        for (QueryCondition condition : conditions) {
            String conditionString = serializeCondition(condition);
            if (conditionString != null) {
                if (condition.isReversed()) {
                    solrQuery.addFilterQuery('-' + conditionString);
                } else {
                    solrQuery.addFilterQuery(conditionString);
                }
            }
        }
    }

    private String serializeCondition(QueryCondition condition)
    {
        String conditionString;

        if (condition instanceof CompareQueryCondition) {
            conditionString = serializeCompareCondition((CompareQueryCondition) condition);
        } else if (condition instanceof StatusQueryCondition) {
            conditionString = serializeStatusCondition((StatusQueryCondition) condition);
        } else if (condition instanceof MailEntityQueryCondition) {
            conditionString = serializeMailCondition((MailEntityQueryCondition) condition);
        } else if (condition instanceof InQueryCondition) {
            conditionString = serializeInCondition((InQueryCondition) condition);
        } else if (condition instanceof GroupQueryCondition) {
            conditionString = serializeGroupCondition((GroupQueryCondition) condition);
        } else {
            conditionString = null;
        }

        return conditionString;
    }

    private String serializeStatusCondition(StatusQueryCondition condition)
    {
        // Filter on status
        if (condition.getStatusRead() != null) {
            StringBuilder builder = new StringBuilder();
            if (condition.getStatusEntityId() != null) {
                builder.append(condition.getStatusRead() ? EventsSolrCoreInitializer.SOLR_FIELD_READLISTENERS
                    : EventsSolrCoreInitializer.SOLR_FIELD_UNREADLISTENERS);
                builder.append(':');
                builder.append(this.utils.toCompleteFilterQueryString(condition.getStatusEntityId()));
            } else {
                builder.append('(');
                builder.append(EventsSolrCoreInitializer.SOLR_FIELD_READLISTENERS);
                builder.append(':');
                builder.append("[* TO *]");
                builder.append(" OR ");
                builder.append(EventsSolrCoreInitializer.SOLR_FIELD_UNREADLISTENERS);
                builder.append(':');
                builder.append("[* TO *]");
                builder.append(')');
            }

            return builder.toString();
        } else if (condition.getStatusEntityId() != null) {
            StringBuilder builder = new StringBuilder();
            builder.append('(');
            builder.append(EventsSolrCoreInitializer.SOLR_FIELD_READLISTENERS);
            builder.append(':');
            builder.append(this.utils.toCompleteFilterQueryString(condition.getStatusEntityId()));
            builder.append(" OR ");
            builder.append(EventsSolrCoreInitializer.SOLR_FIELD_UNREADLISTENERS);
            builder.append(':');
            builder.append(this.utils.toCompleteFilterQueryString(condition.getStatusEntityId()));
            builder.append(')');

            return builder.toString();
        }

        return null;
    }

    private String serializeMailCondition(MailEntityQueryCondition condition)
    {
        // Filter on status
        if (condition.getStatusEntityId() != null) {
            StringBuilder builder = new StringBuilder();
            builder.append(EventsSolrCoreInitializer.SOLR_FIELD_MAILLISTENERS);
            builder.append(':');
            builder.append(this.utils.toCompleteFilterQueryString(condition.getStatusEntityId()));

            return builder.toString();
        }

        return null;
    }

    private String serializeInCondition(InQueryCondition condition)
    {
        return serializeInCondition(toSearchFieldName(toSolrFieldName(condition)), condition.getValues());
    }

    private String serializeInCondition(String filedName, Collection<?> values)
    {
        StringBuilder builder = new StringBuilder();

        builder.append(filedName);

        builder.append(':');

        if (values.isEmpty()) {
            builder.append("[* TO *]");

            return "(-" + builder.toString() + ')';
        } else {
            builder.append('(');
            builder.append(
                StringUtils.join(values.stream().map(this.utils::toCompleteFilterQueryString).iterator(), " OR "));
            builder.append(')');

            return builder.toString();
        }
    }

    private String serializeGroupCondition(GroupQueryCondition group)
    {
        if (group.getConditions().isEmpty()) {
            return null;
        } else if (group.getConditions().size() == 1) {
            // Optimize useless groups
            return serializeCondition(group.getConditions().get(0));
        }

        StringBuilder builder = new StringBuilder();

        builder.append('(');

        for (QueryCondition condition : group.getConditions()) {
            if (builder.length() > 1) {
                if (group.isOr()) {
                    builder.append(" OR ");
                } else {
                    builder.append(" AND ");
                }
            }

            String conditionString = serializeCondition(condition);
            if (conditionString != null) {
                if (condition.isReversed()) {
                    builder.append('-');
                }
                builder.append(conditionString);
            }
        }

        builder.append(')');

        return builder.toString();
    }

    private String getMapFieldName(String key, Type type)
    {
        return this.utils.getMapFieldName(key, EventsSolrCoreInitializer.SOLR_FIELD_PROPERTIES, type);
    }

    private String toSolrFieldName(AbstractPropertyQueryCondition condition)
    {
        if (condition.isCustom()) {
            Type customType = condition.getCustomType();
            if (customType == null) {
                Object value = null;
                if (condition instanceof CompareQueryCondition) {
                    value = ((CompareQueryCondition) condition).getValue();
                } else if (condition instanceof InQueryCondition) {
                    List<Object> values = ((InQueryCondition) condition).getValues();
                    if (values != null && !values.isEmpty()) {
                        value = values.get(0);
                    }
                }
                if (value != null) {
                    customType = value.getClass();
                }
            }

            // It's a custom parameter
            return getMapFieldName(condition.getProperty(), customType);
        } else {
            return condition.getProperty();
        }
    }

    private String toSolrFieldName(SortClause sort)
    {
        if (sort.isCustom()) {
            // It's a custom parameter
            return getMapFieldName(sort.getProperty(), sort.getCustomType());
        } else {
            return sort.getProperty();
        }
    }

    private String serializeCompareCondition(CompareQueryCondition condition)
    {
        StringBuilder builder = new StringBuilder();
        builder.append(toSearchFieldName(toSolrFieldName(condition)));
        builder.append(':');

        switch (condition.getType()) {
            case EQUALS:
                String queryString = toCompleteFilterQueryString(condition.getProperty(), condition.getValue());
                if (queryString == null) {
                    // Negative queries in Solr can only remove results so a simple negative field existence query
                    // doesn't match anything. Thus, use the more compatible query "all events except for those where
                    // the field exists". See also https://stackoverflow.com/a/28859224
                    builder.insert(0, "(*:* NOT ");
                    builder.append("*)");
                } else {
                    builder.append(queryString);
                }
                break;

            case LESS, LESS_OR_EQUALS:
                builder.append(toFilterQueryStringRange(null, condition));
                break;

            case GREATER, GREATER_OR_EQUALS:
                builder.append(toFilterQueryStringRange(condition, null));
                break;

            case STARTS_WITH:
                builder.append(this.utils.toFilterQueryString(condition.getValue()));
                builder.append('*');
                break;

            case ENDS_WITH:
                builder.append('*');
                builder.append(this.utils.toFilterQueryString(condition.getValue()));
                break;

            case CONTAINS:
                builder.append('*');
                builder.append(this.utils.toFilterQueryString(condition.getValue()));
                builder.append('*');
                break;
        }

        return builder.toString();
    }

    private String toCompleteFilterQueryString(String propertyName, Object propertyValue)
    {
        SearchFieldMapping mapping = SEARCH_FIELD_MAPPING.get(propertyName);

        if (mapping != null && mapping.type != null) {
            return this.utils.toCompleteFilterQueryString(propertyValue, mapping.type);
        } else {
            return this.utils.toCompleteFilterQueryString(propertyValue);
        }
    }

    private String toSearchFieldName(String property)
    {
        SearchFieldMapping mapping = SEARCH_FIELD_MAPPING.get(property);

        if (mapping != null && mapping.solrFieldName != null) {
            return mapping.solrFieldName;
        }

        return property;
    }

    private String toFilterQueryStringRange(CompareQueryCondition greater, CompareQueryCondition less)
    {
        StringBuilder builder = new StringBuilder();

        if (greater != null) {
            if (greater.getType() == CompareType.GREATER) {
                builder.append('{');
            } else {
                builder.append('[');
            }

            builder.append(this.utils.toCompleteFilterQueryString(greater.getValue()));
        } else {
            builder.append("[*");
        }

        builder.append(" TO ");

        if (less != null) {
            builder.append(this.utils.toCompleteFilterQueryString(less.getValue()));

            if (less.getType() == CompareType.LESS) {
                builder.append('}');
            } else {
                builder.append(']');
            }
        } else {
            builder.append("*]");
        }

        return builder.toString();
    }

    @Override
    public EventSearchResult search(EventQuery query) throws EventStreamException
    {
        return search(query, null);
    }

    @Override
    public EventSearchResult search(EventQuery query, Set<String> fields) throws EventStreamException
    {
        SolrQuery solrQuery = toSolrQuery(query, fields);

        QueryResponse response;
        try {
            response = this.client.query(solrQuery);
        } catch (Exception e) {
            throw new EventStreamException("Failed to execute Solr query", e);
        }

        SolrDocumentList documents = response.getResults();

        return new StreamEventSearchResult(documents.getNumFound(), documents.getStart(), documents.size(),
            documents.stream().map(this::toEvent));
    }
}
