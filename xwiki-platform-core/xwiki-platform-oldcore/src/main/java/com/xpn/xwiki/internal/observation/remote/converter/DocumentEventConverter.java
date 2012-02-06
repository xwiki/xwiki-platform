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
package com.xpn.xwiki.internal.observation.remote.converter;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.RemoteEventData;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Convert all document event to remote events and back to local events.
 * <p>
 * It also make sure the context contains the proper information like the user or the wiki.
 * 
 * @version $Id$
 * @since 2.0M3
 */
@Component
@Singleton
@Named("document")
public class DocumentEventConverter extends AbstractXWikiEventConverter
{
    /**
     * The events supported by this converter.
     */
    private static final Set<Class< ? extends Event>> EVENTS = new HashSet<Class< ? extends Event>>()
    {
        {
            add(DocumentDeletedEvent.class);
            add(DocumentCreatedEvent.class);
            add(DocumentUpdatedEvent.class);
        }
    };

    @Override
    public boolean toRemote(LocalEventData localEvent, RemoteEventData remoteEvent)
    {
        if (EVENTS.contains(localEvent.getEvent().getClass())) {
            // fill the remote event
            remoteEvent.setEvent((Serializable) localEvent.getEvent());
            remoteEvent.setSource(serializeXWikiDocument((XWikiDocument) localEvent.getSource()));
            remoteEvent.setData(serializeXWikiContext((XWikiContext) localEvent.getData()));

            return true;
        }

        return false;
    }

    @Override
    public boolean fromRemote(RemoteEventData remoteEvent, LocalEventData localEvent)
    {
        if (EVENTS.contains(remoteEvent.getEvent().getClass())) {
            // fill the local event
            XWikiContext context = unserializeXWikiContext(remoteEvent.getData());

            if (context != null) {
                localEvent.setEvent((Event) remoteEvent.getEvent());
                localEvent.setSource(unserializeDocument(remoteEvent.getSource()));
                localEvent.setData(unserializeXWikiContext(remoteEvent.getData()));
            }

            return true;
        }

        return false;
    }
}
