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

import javax.inject.Inject;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.Event.Importance;
import org.xwiki.eventstream.EventStatus;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.internal.DefaultEvent;
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
 * @since 12.3RC1
 */
public class SolrEventStore implements EventStore, Initializable
{
    @Inject
    private Solr solr;

    @Inject
    private SolrUtils utils;

    private SolrClient client;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.client = this.solr.getClient(EventsSolrCoreInitializer.NAME);
        } catch (SolrException e) {
            throw new InitializationException("Failed to get the events Solr core", e);
        }
    }

    @Override
    public void saveEvent(Event event) throws EventStreamException
    {
        try {
            this.client.add(toSolrInputDocument(event));
            this.client.commit();
        } catch (Exception e) {
            throw new EventStreamException("Failed to save event", e);
        }
    }

    private SolrInputDocument toSolrInputDocument(Event event)
    {
        SolrInputDocument document = new SolrInputDocument();

        this.utils.set(EventsSolrCoreInitializer.SOLR_FIELD_ID, event.getId(), document);
        this.utils.set(EventsSolrCoreInitializer.SOLR_FIELD_APPLICATION, event.getApplication(), document);
        this.utils.set(EventsSolrCoreInitializer.SOLR_FIELD_BODY, event.getBody(), document);
        this.utils.set(EventsSolrCoreInitializer.SOLR_FIELD_DATE, event.getDate(), document);
        this.utils.setString(EventsSolrCoreInitializer.SOLR_FIELD_DOCUMENT, event.getDocument(), document);
        this.utils.set(EventsSolrCoreInitializer.SOLR_FIELD_DOCUMENTTITLE, event.getDocumentTitle(), document);
        this.utils.set(EventsSolrCoreInitializer.SOLR_FIELD_DOCUMENTVERSION, event.getDocumentVersion(), document);
        this.utils.set(EventsSolrCoreInitializer.SOLR_FIELD_GROUPID, event.getGroupId(), document);
        this.utils.set(EventsSolrCoreInitializer.SOLR_FIELD_HIDDEN, event.getHidden(), document);
        this.utils.setString(EventsSolrCoreInitializer.SOLR_FIELD_IMPORTANCE, event.getImportance(), document);
        this.utils.setString(EventsSolrCoreInitializer.SOLR_FIELD_RELATEDENTITY, event.getRelatedEntity(), document);
        this.utils.setString(EventsSolrCoreInitializer.SOLR_FIELD_SPACE, event.getSpace(), document);
        this.utils.set(EventsSolrCoreInitializer.SOLR_FIELD_STREAM, event.getStream(), document);
        this.utils.set(EventsSolrCoreInitializer.SOLR_FIELD_TARGET, event.getTarget(), document);
        this.utils.set(EventsSolrCoreInitializer.SOLR_FIELD_TITLE, event.getTitle(), document);
        this.utils.set(EventsSolrCoreInitializer.SOLR_FIELD_TYPE, event.getType(), document);
        this.utils.setString(EventsSolrCoreInitializer.SOLR_FIELD_URL, event.getUrl(), document);
        this.utils.setString(EventsSolrCoreInitializer.SOLR_FIELD_USER, event.getUser(), document);
        this.utils.setString(EventsSolrCoreInitializer.SOLR_FIELD_WIKI, event.getWiki(), document);

        this.utils.setMap(EventsSolrCoreInitializer.SOLR_FIELD_PROPERTIES, event.getParameters(), document);

        return document;
    }

    @Override
    public void deleteEvent(String eventId) throws EventStreamException
    {
        try {
            this.client.deleteById(eventId);
        } catch (Exception e) {
            throw new EventStreamException("Failed to delete the event", e);
        }
    }

    @Override
    public Event getEvent(String eventId) throws EventStreamException
    {
        SolrDocument document;
        try {
            document = this.client.getById(eventId);
        } catch (Exception e) {
            throw new EventStreamException("Failed to get Solr document with id [" + eventId + "]", e);
        }

        DefaultEvent event = new DefaultEvent();

        event.setId(eventId);

        event.setApplication(this.utils.get(EventsSolrCoreInitializer.SOLR_FIELD_APPLICATION, document));
        event.setBody(this.utils.get(EventsSolrCoreInitializer.SOLR_FIELD_BODY, document));
        event.setDate(this.utils.get(EventsSolrCoreInitializer.SOLR_FIELD_DATE, document));
        event.setDocument(
            this.utils.get(EventsSolrCoreInitializer.SOLR_FIELD_DOCUMENT, document, DocumentReference.class));
        event.setDocumentTitle(this.utils.get(EventsSolrCoreInitializer.SOLR_FIELD_DOCUMENTTITLE, document));
        event.setDocumentVersion(this.utils.get(EventsSolrCoreInitializer.SOLR_FIELD_DOCUMENTVERSION, document));
        event.setGroupId(this.utils.get(EventsSolrCoreInitializer.SOLR_FIELD_GROUPID, document));
        event.setHidden(this.utils.get(EventsSolrCoreInitializer.SOLR_FIELD_HIDDEN, document));
        event
            .setImportance(this.utils.get(EventsSolrCoreInitializer.SOLR_FIELD_IMPORTANCE, document, Importance.class));
        event.setRelatedEntity(
            this.utils.get(EventsSolrCoreInitializer.SOLR_FIELD_RELATEDENTITY, document, EntityReference.class));
        event.setSpace(this.utils.get(EventsSolrCoreInitializer.SOLR_FIELD_SPACE, document, SpaceReference.class));
        event.setStream(this.utils.get(EventsSolrCoreInitializer.SOLR_FIELD_STREAM, document));
        event.setTarget(this.utils.getSet(EventsSolrCoreInitializer.SOLR_FIELD_TARGET, document));
        event.setTitle(this.utils.get(EventsSolrCoreInitializer.SOLR_FIELD_TITLE, document));
        event.setType(this.utils.get(EventsSolrCoreInitializer.SOLR_FIELD_TYPE, document));
        event.setUrl(this.utils.get(EventsSolrCoreInitializer.SOLR_FIELD_URL, document, URL.class));
        event.setUser(this.utils.get(EventsSolrCoreInitializer.SOLR_FIELD_USER, document, DocumentReference.class));
        event.setWiki(this.utils.get(EventsSolrCoreInitializer.SOLR_FIELD_WIKI, document, WikiReference.class));

        event.setParameters(this.utils.getMap(EventsSolrCoreInitializer.SOLR_FIELD_PROPERTIES, document));

        return event;
    }

    @Override
    public void saveEventStatus(EventStatus status) throws EventStreamException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteEventStatus(EventStatus status) throws EventStreamException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public EventStatus getEventStatus(String eventId, String entity) throws EventStreamException
    {
        // TODO Auto-generated method stub
        return null;
    }
}
