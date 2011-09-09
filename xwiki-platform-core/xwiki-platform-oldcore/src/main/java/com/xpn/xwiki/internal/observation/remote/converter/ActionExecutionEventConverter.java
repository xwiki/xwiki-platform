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

import java.util.HashSet;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.event.ActionExecutedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.RemoteEventData;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Convert and filter action events for the network.
 * <p>
 * Currently only "upload" action is send (for attachments modifications).
 * 
 * @todo make the list of actions to send configurable
 * @version $Id$
 * @since 2.0M4
 */
@Component
@Singleton
@Named("action")
public class ActionExecutionEventConverter extends AbstractXWikiEventConverter
{
    /**
     * The events supported by this converter.
     */
    private Set<String> actions = new HashSet<String>()
    {
        {
            add("upload");
        }
    };

    @Override
    public boolean toRemote(LocalEventData localEvent, RemoteEventData remoteEvent)
    {
        if (localEvent.getEvent() instanceof ActionExecutedEvent) {
            ActionExecutedEvent event = (ActionExecutedEvent) localEvent.getEvent();

            if (this.actions.contains(event.getActionName())) {
                // fill the remote event
                remoteEvent.setEvent(event);
                remoteEvent.setSource(serializeXWikiDocument((XWikiDocument) localEvent.getSource()));
                remoteEvent.setData(serializeXWikiContext((XWikiContext) localEvent.getData()));
            }

            return true;
        }

        return false;
    }

    @Override
    public boolean fromRemote(RemoteEventData remoteEvent, LocalEventData localEvent)
    {
        if (remoteEvent.getEvent() instanceof ActionExecutedEvent) {
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
