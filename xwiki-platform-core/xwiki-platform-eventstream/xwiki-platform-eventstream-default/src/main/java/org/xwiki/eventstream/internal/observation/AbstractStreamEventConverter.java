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
package org.xwiki.eventstream.internal.observation;

import java.util.Optional;

import javax.inject.Inject;

import jakarta.inject.Provider;

import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.observation.remote.converter.AbstractEventConverter;

/**
 * Base class helper to implement Event Stream related event converters.
 *
 * @version $Id$
 * @since 17.9.0RC1
 */
public abstract class AbstractStreamEventConverter extends AbstractEventConverter
{
    @Inject
    // Load the event store lazily because it can trigger a lot of things while the event converter is loaded very early
    // (but the change of it actually doing anything is very low during init)
    private Provider<EventStore> storeProvider;

    protected Optional<Event> getEvent(String eventId) throws EventStreamException
    {
        return this.storeProvider.get().getEvent(eventId);
    }
}
