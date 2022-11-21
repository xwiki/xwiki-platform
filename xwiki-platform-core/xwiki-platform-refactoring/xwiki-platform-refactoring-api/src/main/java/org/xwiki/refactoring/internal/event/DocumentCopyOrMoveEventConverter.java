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
package org.xwiki.refactoring.internal.event;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.RemoteEventData;
import org.xwiki.observation.remote.converter.AbstractEventConverter;
import org.xwiki.refactoring.event.DocumentCopiedEvent;
import org.xwiki.refactoring.event.DocumentCopyingEvent;
import org.xwiki.refactoring.event.DocumentRenamedEvent;
import org.xwiki.refactoring.event.DocumentRenamingEvent;

/**
 * Convert all document event to remote events and back to local events.
 * <p>
 * It also make sure the context contains the proper information like the user or the wiki.
 *
 * @version $Id$
 * @since 14.5
 * @since 13.10.7
 * @since 14.4.2
 */
@Component
@Singleton
@Named("DocumentCopyOrMoveEvent")
public class DocumentCopyOrMoveEventConverter extends AbstractEventConverter
{
    /**
     * The events supported by this converter.
     */
    private static final Set<Class<? extends Event>> EVENTS = new HashSet<>(Arrays.asList(DocumentRenamingEvent.class,
        DocumentRenamedEvent.class, DocumentCopyingEvent.class, DocumentCopiedEvent.class));

    @Override
    public boolean toRemote(LocalEventData localEvent, RemoteEventData remoteEvent)
    {
        if (EVENTS.contains(localEvent.getEvent().getClass())) {
            // fill the remote event
            remoteEvent.setEvent((Serializable) localEvent.getEvent());
            remoteEvent.setData((Serializable) localEvent.getData());

            // The source these events is not serializables

            return true;
        }

        return false;
    }

    @Override
    public boolean fromRemote(RemoteEventData remoteEvent, LocalEventData localEvent)
    {
        if (EVENTS.contains(remoteEvent.getEvent().getClass())) {
            localEvent.setEvent((Event) remoteEvent.getEvent());
            localEvent.setData(remoteEvent.getData());

            // The source these events is not serializables

            return true;
        }

        return false;
    }
}
