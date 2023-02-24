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
package org.xwiki.observation.remote.internal.converter;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
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
@Singleton
public class DefaultEventConverterManager implements EventConverterManager, Initializable
{
    /**
     * The local events converters.
     */
    @Inject
    private List<LocalEventConverter> localEventConverters;

    /**
     * The remote events converters.
     */
    @Inject
    private List<RemoteEventConverter> remoteEventConverters;

    @Inject
    private Logger logger;

    @Override
    public void initialize() throws InitializationException
    {
        // sort local events converters by priority
        Collections.sort(this.localEventConverters, new Comparator<LocalEventConverter>()
        {
            @Override
            public int compare(LocalEventConverter eventConverter1, LocalEventConverter eventConverter2)
            {
                return eventConverter1.getPriority() - eventConverter2.getPriority();
            }
        });

        // sort remote events converters by priority
        Collections.sort(this.remoteEventConverters, new Comparator<RemoteEventConverter>()
        {
            @Override
            public int compare(RemoteEventConverter eventConverter1, RemoteEventConverter eventConverter2)
            {
                return eventConverter1.getPriority() - eventConverter2.getPriority();
            }
        });
    }

    @Override
    public List<LocalEventConverter> getLocalEventConverters()
    {
        return this.localEventConverters;
    }

    @Override
    public List<RemoteEventConverter> getRemoteEventConverters()
    {
        return this.remoteEventConverters;
    }

    @Override
    public RemoteEventData createRemoteEventData(LocalEventData localEvent)
    {
        RemoteEventData remoteEvent = new RemoteEventData();

        for (LocalEventConverter eventConverter : this.localEventConverters) {
            try {
                if (eventConverter.toRemote(localEvent, remoteEvent)) {
                    break;
                }
            } catch (Exception e) {
                this.logger.error("Failed to convert local event [{}]", localEvent, e);
            }
        }

        if (remoteEvent.getEvent() == null) {
            remoteEvent = null;
        }

        return remoteEvent;
    }

    @Override
    public LocalEventData createLocalEventData(RemoteEventData remoteEvent)
    {
        LocalEventData localEvent = new LocalEventData();

        for (RemoteEventConverter eventConverter : this.remoteEventConverters) {
            try {
                if (eventConverter.fromRemote(remoteEvent, localEvent)) {
                    break;
                }
            } catch (Exception e) {
                this.logger.error("Failed to convert remote event [{}]", remoteEvent, e);
            }
        }

        if (localEvent.getEvent() == null) {
            localEvent = null;
        }

        return localEvent;
    }
}
