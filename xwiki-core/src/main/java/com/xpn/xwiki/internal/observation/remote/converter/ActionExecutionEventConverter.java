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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.xwiki.component.annotation.Component;
import org.xwiki.observation.event.ActionExecutionEvent;
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
 * @since 2.0RC1
 */
@Component("action")
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

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.remote.converter.LocalEventConverter#toRemote(org.xwiki.observation.remote.LocalEventData,
     *      org.xwiki.observation.remote.RemoteEventData)
     */
    public boolean toRemote(LocalEventData localEvent, RemoteEventData remoteEvent)
    {
        if (localEvent.getEvent() instanceof ActionExecutionEvent) {
            ActionExecutionEvent event = (ActionExecutionEvent) localEvent.getEvent();

            if (this.actions.contains(event.getActionName())) {
                HashMap<String, Serializable> remoteData = new HashMap<String, Serializable>();

                // serialize document
                serializeXWikiDocument((XWikiDocument) localEvent.getSource(), remoteData);

                // save some context informations
                serializeXWikiContext((XWikiContext) localEvent.getData(), remoteData);

                // fill the remote event
                remoteEvent.setEvent((Serializable) localEvent.getEvent());
                remoteEvent.setData(remoteData);

                return true;
            }
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
        if (remoteEvent.getEvent() instanceof ActionExecutionEvent) {
            Map<String, Serializable> remoteData = (Map<String, Serializable>) remoteEvent.getData();

            // set some context information
            XWikiContext context = unserializeXWikiContext(remoteData);

            // restore document
            XWikiDocument document = unserializeDocument(remoteData);

            // fill the local event
            localEvent.setEvent((Event) remoteEvent.getEvent());
            localEvent.setSource(document);
            localEvent.setData(context);

            return true;
        }

        return false;
    }
}
