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
 *
 */
package com.xpn.xwiki.internal.observation.remote.converter;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.xwiki.bridge.event.WikiCreatedEvent;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.RemoteEventData;

import com.xpn.xwiki.XWikiContext;

/**
 * Convert all document event to remote events and back to local events.
 * <p>
 * It also make sure the context contains the proper information like the user or the wiki.
 * 
 * @version $Id: DocumentEventConverter.java 33349 2010-12-11 10:37:21Z jvelociter $
 * @since 2.7.1
 */
@Component("wiki")
public class WikiEventConverter extends AbstractXWikiEventConverter
{
    /**
     * The events supported by this converter.
     */
    private Set<Class< ? extends Event>> events = new HashSet<Class< ? extends Event>>()
    {
        {
            add(WikiDeletedEvent.class);
            add(WikiCreatedEvent.class);
        }
    };

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.remote.converter.LocalEventConverter#toRemote(org.xwiki.observation.remote.LocalEventData,
     *      org.xwiki.observation.remote.RemoteEventData)
     */
    public boolean toRemote(LocalEventData localEvent, RemoteEventData remoteEvent)
    {
        if (this.events.contains(localEvent.getEvent().getClass())) {
            // fill the remote event
            remoteEvent.setEvent((Serializable) localEvent.getEvent());
            remoteEvent.setSource((String) localEvent.getSource());
            remoteEvent.setData(serializeXWikiContext((XWikiContext) localEvent.getData()));

            return true;
        }

        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.remote.converter.RemoteEventConverter#fromRemote(org.xwiki.observation.remote.RemoteEventData,
     *      org.xwiki.observation.remote.LocalEventData)
     */
    public boolean fromRemote(RemoteEventData remoteEvent, LocalEventData localEvent)
    {
        if (this.events.contains(remoteEvent.getEvent().getClass())) {
            // fill the local event
            XWikiContext context = unserializeXWikiContext(remoteEvent.getData());

            if (context != null) {
                localEvent.setEvent((Event) remoteEvent.getEvent());
                localEvent.setSource(remoteEvent.getSource());
                localEvent.setData(unserializeXWikiContext(remoteEvent.getData()));
            }

            return true;
        }

        return false;
    }
}
