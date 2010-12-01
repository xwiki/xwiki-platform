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
package org.xwiki.observation.remote.internal.converter;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.RemoteEventData;
import org.xwiki.observation.remote.converter.EventConverterManager;
import org.xwiki.observation.remote.converter.LocalEventConverter;
import org.xwiki.observation.remote.converter.RemoteEventConverter;

/**
 * Default implementation of {@link EventConverterManager}.
 * 
 * @version $Id$
 * @since 2.0M3
 */
@Component
public class DefaultEventConverterManager implements EventConverterManager, Initializable
{
    /**
     * The local events converters.
     */
    @Requirement
    private List<LocalEventConverter> localEventConverters;

    /**
     * The remote events converters.
     */
    @Requirement
    private List<RemoteEventConverter> remoteEventConverters;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.phase.Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        // sort local events converters by priority
        Collections.sort(this.localEventConverters, new Comparator<LocalEventConverter>()
        {
            public int compare(LocalEventConverter eventConverter1, LocalEventConverter eventConverter2)
            {
                return eventConverter1.getPriority() - eventConverter2.getPriority();
            }
        });

        // sort remote events converters by priority
        Collections.sort(this.remoteEventConverters, new Comparator<RemoteEventConverter>()
        {
            public int compare(RemoteEventConverter eventConverter1, RemoteEventConverter eventConverter2)
            {
                return eventConverter1.getPriority() - eventConverter2.getPriority();
            }
        });
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.remote.converter.EventConverterManager#getLocalEventConverters()
     */
    public List<LocalEventConverter> getLocalEventConverters()
    {
        return this.localEventConverters;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.remote.converter.EventConverterManager#getRemoteEventConverters()
     */
    public List<RemoteEventConverter> getRemoteEventConverters()
    {
        return this.remoteEventConverters;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.remote.converter.EventConverterManager#createRemoteEventData(org.xwiki.observation.remote.LocalEventData)
     */
    public RemoteEventData createRemoteEventData(LocalEventData localEvent)
    {
        RemoteEventData remoteEvent = new RemoteEventData();

        for (LocalEventConverter eventConverter : this.localEventConverters) {
            if (eventConverter.toRemote(localEvent, remoteEvent)) {
                break;
            }
        }

        if (remoteEvent.getEvent() == null) {
            remoteEvent = null;
        }

        return remoteEvent;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.remote.converter.EventConverterManager#createLocalEventData(org.xwiki.observation.remote.RemoteEventData)
     */
    public LocalEventData createLocalEventData(RemoteEventData remoteEvent)
    {
        LocalEventData localEvent = new LocalEventData();

        for (RemoteEventConverter eventConverter : this.remoteEventConverters) {
            if (eventConverter.fromRemote(remoteEvent, localEvent)) {
                break;
            }
        }

        if (localEvent.getEvent() == null) {
            localEvent = null;
        }

        return localEvent;
    }
}
