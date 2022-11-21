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
package org.xwiki.eventstream.internal;

import java.util.Date;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventFactory;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;
import org.xwiki.model.reference.EntityReferenceResolver;

/**
 * Default implementation of the {@link EventFactory}, creating {@link DefaultEvent} objects as {@link Event events}.
 * 
 * @version $Id$
 */
@Component
@Singleton
public class DefaultEventFactory implements EventFactory
{
    /** The key used to store the current event group ID in the execution context. */
    private static final String EVENT_GROUP_ID = Event.class.getCanonicalName().concat("_groupId");

    /** Needed for storing the current event group ID. */
    @Inject
    private Execution execution;

    /** Needed for converting the current username into a proper reference. */
    @Inject
    private EntityReferenceResolver<String> resolver;

    /** Needed for retrieving the current user. */
    @Inject
    private DocumentAccessBridge bridge;

    @Inject
    private RemoteObservationManagerConfiguration remoteObservation;

    @Override
    public Event createEvent()
    {
        DefaultEvent result = new DefaultEvent();
        result.setId(UUID.randomUUID().toString());
        result.setGroupId(getCurrentGroupId());
        result.setUser(new DocumentReference(this.resolver.resolve(this.bridge.getCurrentUser(), EntityType.DOCUMENT)));
        result.setDate(new Date());
        result.setRemoteObservationId(this.remoteObservation.getId());
        return result;
    }

    @Override
    public Event createRawEvent()
    {
        DefaultEvent result =  new DefaultEvent();
        result.setRemoteObservationId(this.remoteObservation.getId());

        return result;
    }

    /**
     * Retrieves the event group ID from the execution context. If there's no group ID in the context, generate a random
     * one and store it in the context.
     * 
     * @return the current event group ID
     */
    private String getCurrentGroupId()
    {
        String currentGroup = (String) this.execution.getContext().getProperty(EVENT_GROUP_ID);
        if (currentGroup == null) {
            currentGroup = UUID.randomUUID().toString();
            this.execution.getContext().setProperty(EVENT_GROUP_ID, currentGroup);
        }
        return currentGroup;
    }
}
