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

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.Event.Importance;
import org.xwiki.eventstream.EventQuery;
import org.xwiki.eventstream.EventSearchResult;
import org.xwiki.eventstream.EventStatus;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.PageableEventQuery;
import org.xwiki.eventstream.SimpleEventQuery;
import org.xwiki.eventstream.SimpleEventQuery.CompareQueryCondition;
import org.xwiki.eventstream.SimpleEventQuery.CompareQueryCondition.CompareType;
import org.xwiki.eventstream.internal.AbstractAsynchronousEventStore;
import org.xwiki.eventstream.internal.DefaultEvent;
import org.xwiki.eventstream.internal.StreamEventSearchResult;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
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
    private static class CompareQueryConditionRange
    {
        final String property;

        CompareQueryCondition less;

        CompareQueryCondition greater;

        CompareQueryConditionRange(String property)
        {
            this.property = property;
        }
    }

    @Inject
    private Solr solr;

    @Inject
    private SolrUtils utils;

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
    protected Optional<EventStatus> syncDeleteEventStatus(EventStatus status) throws EventStreamException
    {
        saveEventStatus(status.getEvent().getId(), status.getEntityId(), false, false);

        return Optional.of(status);
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

        document.setField(EventsSolrCoreInitializer.SOLR_FIELD_READLISTENERS,
            Collections.singletonMap(read ? "add-distinct" : "remove", entityId));

        Map<String, Object> unreadModifier = new HashMap<>(1);
        unreadModifier.put(read ? "add-distinct" : "remove", entityId);
        document.setField(EventsSolrCoreInitializer.SOLR_FIELD_UNREADLISTENERS, unreadModifier);

        try {
            this.client.add(document);
        } catch (Exception e) {
            throw new EventStreamException(
                String.format("Failed to update the event status for event [%s] and entity id [%s]", eventId, entityId),
                e);
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
        this.utils.setString(Event.FIELD_DOCUMENT, event.getDocument(), document);
        this.utils.set(Event.FIELD_DOCUMENTTITLE, event.getDocumentTitle(), document);
        this.utils.set(Event.FIELD_DOCUMENTVERSION, event.getDocumentVersion(), document);
        this.utils.set(Event.FIELD_GROUPID, event.getGroupId(), document);
        this.utils.set(Event.FIELD_HIDDEN, event.getHidden(), document);
        this.utils.setString(Event.FIELD_IMPORTANCE, event.getImportance(), document);
        this.utils.setString(Event.FIELD_RELATEDENTITY, event.getRelatedEntity(), document);
        this.utils.setString(Event.FIELD_SPACE, event.getSpace(), document);
        this.utils.set(Event.FIELD_STREAM, event.getStream(), document);
        this.utils.set(Event.FIELD_TARGET, event.getTarget(), document);
        this.utils.set(Event.FIELD_TITLE, event.getTitle(), document);
        this.utils.set(Event.FIELD_TYPE, event.getType(), document);
        this.utils.setString(Event.FIELD_URL, event.getUrl(), document);
        this.utils.setString(Event.FIELD_USER, event.getUser(), document);
        this.utils.setString(Event.FIELD_WIKI, event.getWiki(), document);

        this.utils.setMap(EventsSolrCoreInitializer.SOLR_FIELD_PROPERTIES, event.getParameters(), document);

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

    @Override
    public Optional<Event> getEvent(String eventId) throws EventStreamException
    {
        SolrDocument document;
        try {
            document = this.client.getById(eventId);
        } catch (Exception e) {
            throw new EventStreamException("Failed to get Solr document with id [" + eventId + "]", e);
        }

        return Optional.ofNullable(toEvent(document));
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
        event.setDocument(this.utils.get(Event.FIELD_DOCUMENT, document, DocumentReference.class));
        event.setDocumentTitle(this.utils.get(Event.FIELD_DOCUMENTTITLE, document));
        event.setDocumentVersion(this.utils.get(Event.FIELD_DOCUMENTVERSION, document));
        event.setGroupId(this.utils.get(Event.FIELD_GROUPID, document));
        event.setHidden(this.utils.get(Event.FIELD_HIDDEN, document));
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

        event.setParameters(this.utils.getMap(EventsSolrCoreInitializer.SOLR_FIELD_PROPERTIES, document));

        return event;
    }

    private SolrQuery toSolrQuery(EventQuery query)
    {
        SolrQuery solrQuery = new SolrQuery();

        if (query instanceof PageableEventQuery) {
            PageableEventQuery pageableQuery = (PageableEventQuery) query;

            if (pageableQuery.getOffset() > 0) {
                solrQuery.setStart((int) pageableQuery.getOffset());
            }

            if (pageableQuery.getLimit() > 0) {
                solrQuery.setRows((int) pageableQuery.getLimit());
            }

            if (pageableQuery instanceof SimpleEventQuery) {
                Map<String, CompareQueryConditionRange> ranges = new HashMap<>();

                for (CompareQueryCondition condition : ((SimpleEventQuery) pageableQuery).getConditions()) {

                    if (EventsSolrCoreInitializer.KNOWN_FIELDS.contains(condition.getProperty())) {
                        if (condition.getType() == CompareType.EQUALS) {
                            StringBuilder builder = new StringBuilder();
                            builder.append(condition.getProperty());
                            builder.append(':');
                            builder.append(this.utils.toFilterQueryString(condition.getValue()));
                            solrQuery.addFilterQuery(builder.toString());
                        } else {
                            // Optimize ranges to have one instead of two since Solr is based on a range syntax (no
                            // lower/greater syntax)
                            CompareQueryConditionRange range = ranges.computeIfAbsent(condition.getProperty(),
                                k -> new CompareQueryConditionRange(condition.getProperty()));

                            switch (condition.getType()) {
                                case LESS:
                                case LESS_OR_EQUALS:
                                    range.less = condition;
                                    break;

                                case GREATER:
                                case GREATER_OR_EQUALS:
                                    range.greater = condition;
                                    break;
                            }

                            break;
                        }
                    } else {
                        // TODO: add support for custom properties
                    }
                }

                // Add ranges to the filter query
                for (CompareQueryConditionRange range : ranges.values()) {
                    StringBuilder builder = new StringBuilder();
                    builder.append(range.property);
                    builder.append(':');
                    builder.append(toFilterQueryStringRange(range.greater, range.less));

                    solrQuery.addFilterQuery(builder.toString());
                }
            }
        }

        return solrQuery;
    }

    public String toFilterQueryStringRange(CompareQueryCondition greater, CompareQueryCondition less)
    {
        StringBuilder builder = new StringBuilder();

        if (greater != null) {
            if (greater.getType() == CompareType.GREATER) {
                builder.append('{');
            } else {
                builder.append('[');
            }

            builder.append(this.utils.toFilterQueryString(greater.getValue()));
        } else {
            builder.append("[*");
        }

        builder.append(" TO ");

        if (less != null) {
            builder.append(this.utils.toFilterQueryString(less.getValue()));

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
        SolrQuery solrQuery = toSolrQuery(query);

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
