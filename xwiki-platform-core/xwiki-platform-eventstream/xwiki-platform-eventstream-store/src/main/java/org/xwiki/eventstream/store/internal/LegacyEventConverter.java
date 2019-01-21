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
package org.xwiki.eventstream.store.internal;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventFactory;
import org.xwiki.eventstream.EventStatus;
import org.xwiki.eventstream.internal.DefaultEventStatus;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Internal helper that convert some objects from the Event Stream module to objects of the Activity Stream module
 * (which is used for the storage) and the opposite.
 *
 * @version $Id$
 * @since 11.0RC1
 */
@Component(roles = LegacyEventConverter.class)
@Singleton
public class LegacyEventConverter
{
    /** Needed for creating raw events. */
    @Inject
    private EventFactory eventFactory;

    /** Needed for serializing document names. */
    @Inject
    private EntityReferenceSerializer<String> serializer;

    /** Needed for serializing the wiki and space references. */
    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactSerializer;

    /** Needed for deserializing document names. */
    @Inject
    private EntityReferenceResolver<String> resolver;

    /** Needed for deserializing related entities. */
    @Inject
    @Named("explicit")
    private EntityReferenceResolver<String> explicitResolver;

    /**
     * Converts a new {@link Event} to the old {@link LegacyEvent}.
     *
     * @param e the event to transform
     * @return the equivalent activity event
     */
    public LegacyEvent convertEventToLegacyActivity(Event e)
    {
        LegacyEvent result = new LegacyEvent();
        result.setApplication(e.getApplication());
        result.setBody(e.getBody());
        result.setDate(e.getDate());
        result.setEventId(e.getId());
        result.setPage(this.compactSerializer.serialize(e.getDocument(), e.getWiki()));
        if (e.getDocumentTitle() != null) {
            result.setParam1(e.getDocumentTitle());
        }
        if (e.getRelatedEntity() != null) {
            result.setParam2(this.serializer.serialize(e.getRelatedEntity()));
        }
        result.setPriority((e.getImportance().ordinal() + 1) * 10);

        result.setRequestId(e.getGroupId());
        result.setSpace(this.compactSerializer.serialize(e.getSpace(), e.getWiki()));
        result.setStream(e.getStream());
        result.setTitle(e.getTitle());
        result.setType(e.getType());
        if (e.getUrl() != null) {
            result.setUrl(e.getUrl().toString());
        }
        result.setUser(this.serializer.serialize(e.getUser()));
        result.setVersion(e.getDocumentVersion());
        result.setWiki(this.serializer.serialize(e.getWiki()));

        result.setTarget(e.getTarget());

        return result;
    }

    /**
     * Convert an old {@link LegacyEvent} to the new {@link Event}.
     *
     * @param e the activity event to transform
     * @return the equivalent event
     */
    public Event convertLegacyActivityToEvent(LegacyEvent e)
    {
        Event result = this.eventFactory.createRawEvent();
        result.setApplication(e.getApplication());
        result.setBody(e.getBody());
        result.setDate(e.getDate());
        result.setDocument(new DocumentReference(this.resolver.resolve(e.getPage(), EntityType.DOCUMENT,
                new WikiReference(e.getWiki()))));
        result.setId(e.getEventId());
        result.setDocumentTitle(e.getParam1());
        if (StringUtils.isNotEmpty(e.getParam2())) {
            if (StringUtils.endsWith(e.getType(), "Attachment")) {
                result.setRelatedEntity(this.explicitResolver.resolve(e.getParam2(), EntityType.ATTACHMENT,
                        result.getDocument()));
            } else if (StringUtils.endsWith(e.getType(), "Comment")
                    || StringUtils.endsWith(e.getType(), "Annotation")) {
                result.setRelatedEntity(this.explicitResolver.resolve(e.getParam2(), EntityType.OBJECT,
                        result.getDocument()));
            }
        }
        result.setImportance(Event.Importance.MEDIUM);
        if (e.getPriority() > 0) {
            int priority = e.getPriority() / 10 - 1;
            if (priority >= 0 && priority < Event.Importance.values().length) {
                result.setImportance(Event.Importance.values()[priority]);
            }
        }

        result.setGroupId(e.getRequestId());
        result.setStream(e.getStream());
        result.setTitle(e.getTitle());
        result.setType(e.getType());
        if (StringUtils.isNotBlank(e.getUrl())) {
            try {
                result.setUrl(new URL(e.getUrl()));
            } catch (MalformedURLException ex) {
                // Should not happen
            }
        }
        result.setUser(new DocumentReference(this.resolver.resolve(e.getUser(), EntityType.DOCUMENT)));
        result.setDocumentVersion(e.getVersion());


        result.setTarget(e.getTarget());
        return result;
    }

    /**
     * Convert an {@link EventStatus} to an {@link LegacyEventStatus}.
     *
     * @param eventStatus the status to transform
     * @return the equivalent activity event status
     */
    public LegacyEventStatus convertEventStatusToLegacyActivityStatus(EventStatus eventStatus)
    {
        LegacyEventStatus legacyEventStatus = new LegacyEventStatus();
        legacyEventStatus.setActivityEvent(convertEventToLegacyActivity(eventStatus.getEvent()));
        legacyEventStatus.setEntityId(eventStatus.getEntityId());
        legacyEventStatus.setRead(eventStatus.isRead());
        return legacyEventStatus;
    }

    /**
     * Convert an {@link LegacyEventStatus} to an {@link EventStatus}.
     *
     * @param eventStatus the activity event status to transform
     * @return the equivalent event status
     */
    public EventStatus convertLegacyActivityStatusToEventStatus(LegacyEventStatus eventStatus)
    {
        return new DefaultEventStatus(
                convertLegacyActivityToEvent(eventStatus.getActivityEvent()),
                eventStatus.getEntityId(),
                eventStatus.isRead()
        );
    }
}
