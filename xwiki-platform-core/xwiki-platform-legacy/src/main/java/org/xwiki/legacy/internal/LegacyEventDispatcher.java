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
package org.xwiki.legacy.internal;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.DocumentDeleteEvent;
import org.xwiki.observation.event.DocumentSaveEvent;
import org.xwiki.observation.event.DocumentUpdateEvent;
import org.xwiki.observation.event.Event;

/**
 * An event listener that forwards received events to their corresponding legacy events. This allows depreciated events
 * to continue be supported.
 * 
 * @version $Id$
 */
@Component("legacyEventDispatcher")
public class LegacyEventDispatcher implements EventListener
{
    /**
     * Component manager, used to get access to the observation manager that we cannot get injected because of a cyclic
     * dependency.
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * Observation manager, used to notify legacy events.
     */
    private ObservationManager observationManager;

    /**
     * {@inheritDoc}
     */
    public String getName()
    {
        return "legacyEventDispatcher";
    }

    /**
     * {@inheritDoc}
     */
    public List<Event> getEvents()
    {
        return new ArrayList<Event>()
        {
            {
                add(new DocumentDeletedEvent());
                add(new DocumentCreatedEvent());
                add(new DocumentUpdatedEvent());
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof DocumentDeletedEvent) {
            this.getObservationManager().notify(
                new DocumentDeleteEvent(((DocumentDeletedEvent) event).getEventFilter()), source, data);
        } else if (event instanceof DocumentCreatedEvent) {
            this.getObservationManager().notify(new DocumentSaveEvent(((DocumentCreatedEvent) event).getEventFilter()),
                source, data);
        } else if (event instanceof DocumentUpdatedEvent) {
            this.getObservationManager().notify(
                new DocumentUpdateEvent(((DocumentUpdatedEvent) event).getEventFilter()), source, data);
        }
    }

    /**
     * Helper to lazily lookup the observation manager.
     * 
     * @return the observation manager
     */
    private ObservationManager getObservationManager()
    {
        if (this.observationManager != null) {
            return this.observationManager;
        }
        try {
            this.observationManager = this.componentManager.lookup(ObservationManager.class);
        } catch (ComponentLookupException e) {
            this.logger.error("Failed to lookup observation manager", e);
        }
        return this.observationManager;
    }

}
