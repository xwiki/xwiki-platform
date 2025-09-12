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
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
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
public class DefaultEventConverterManager implements EventConverterManager
{
    @Inject
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

    /**
     * The local events converters.
     */
    private List<LocalEventConverter> localEventConverters;

    /**
     * The remote events converters.
     */
    private List<RemoteEventConverter> remoteEventConverters;

    private <T> List<T> loadConverters(Class<T> converterType, Comparator<T> c)
    {
        // Load converters lazily to avoid cycles if any of those inject directly or indirectly the converter manager
        try {
            List<T> converters = this.componentManager.getInstanceList(converterType);
            Collections.sort(converters, c);
            return converters;
        } catch (ComponentLookupException e) {
            this.logger.error("Failed to lookup event converters", e);
        }

        return List.of();
    }

    @Override
    public List<LocalEventConverter> getLocalEventConverters()
    {
        if (this.localEventConverters == null) {
            this.localEventConverters =
                loadConverters(LocalEventConverter.class, (c1, c2) -> c1.getPriority() - c2.getPriority());
        }

        return this.localEventConverters;
    }

    @Override
    public List<RemoteEventConverter> getRemoteEventConverters()
    {
        if (this.localEventConverters == null) {
            this.remoteEventConverters =
                loadConverters(RemoteEventConverter.class, (c1, c2) -> c1.getPriority() - c2.getPriority());
        }

        return this.remoteEventConverters;
    }

    @Override
    public RemoteEventData createRemoteEventData(LocalEventData localEvent)
    {
        RemoteEventData remoteEvent = new RemoteEventData();

        for (LocalEventConverter eventConverter : getLocalEventConverters()) {
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

        for (RemoteEventConverter eventConverter : getRemoteEventConverters()) {
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
