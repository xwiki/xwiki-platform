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

import java.net.MalformedURLException;
import java.net.URL;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventFactory;
import org.xwiki.eventstream.EventStatus;
import org.xwiki.eventstream.internal.DefaultEventStatus;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;

/**
 * Default implementation to convert from and to LegacyEvents.
 * Set most of the values, but does not use param3, param4 and param5 which might be used for custom operations.
 *
 * @since 12.1RC1
 * @version $Id$
 */
public abstract class AbstractLegacyEventConverter implements LegacyEventConverter
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

    @Override
    public LegacyEvent convertEventToLegacyActivity(Event e)
    {
        LegacyEvent result = new LegacyEvent();
        result.setApplication(e.getApplication());
        result.setBody(e.getBody());
        result.setDate(e.getDate());
        result.setEventId(e.getId());
        if (e.getDocument() != null) {
            result.setPage(this.compactSerializer.serialize(e.getDocument(), e.getWiki()));
        }
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
        result.setHidden(e.getHidden());

        return result;
    }

    @Override
    public Event convertLegacyActivityToEvent(LegacyEvent e)
    {
        Event result = this.eventFactory.createRawEvent();
        result.setApplication(e.getApplication());
        result.setBody(e.getBody());
        result.setDate(e.getDate());
        if (StringUtils.isNotEmpty(e.getPage())) {
            result.setDocument(new DocumentReference(this.resolver.resolve(e.getPage(), EntityType.DOCUMENT,
                new WikiReference(e.getWiki()))));
        }
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
        Event.Importance importance = computePriorityFromLegacyEvent(e.getPriority());
        if (importance != null) {
            result.setImportance(importance);
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
        result.setHidden(e.isHidden());

        result.setTarget(e.getTarget());
        return result;
    }

    private Event.Importance computePriorityFromLegacyEvent(int originalPriority)
    {
        if (originalPriority > 0) {
            int priority = originalPriority / 10 - 1;
            if (priority >= 0 && priority < Event.Importance.values().length) {
                return Event.Importance.values()[priority];
            }
        }
        return null;
    }

    @Override
    public LegacyEventStatus convertEventStatusToLegacyActivityStatus(EventStatus eventStatus)
    {
        LegacyEventStatus legacyEventStatus = new LegacyEventStatus();
        legacyEventStatus.setActivityEvent(convertEventToLegacyActivity(eventStatus.getEvent()));
        legacyEventStatus.setEntityId(eventStatus.getEntityId());
        legacyEventStatus.setRead(eventStatus.isRead());
        return legacyEventStatus;
    }

    @Override
    public EventStatus convertLegacyActivityStatusToEventStatus(LegacyEventStatus eventStatus)
    {
        return new DefaultEventStatus(
            convertLegacyActivityToEvent(eventStatus.getActivityEvent()),
            eventStatus.getEntityId(),
            eventStatus.isRead()
        );
    }
}
