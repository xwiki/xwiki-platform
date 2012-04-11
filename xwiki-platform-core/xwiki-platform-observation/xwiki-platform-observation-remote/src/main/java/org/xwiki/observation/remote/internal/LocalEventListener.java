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
package org.xwiki.observation.remote.internal;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.AllEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.RemoteObservationManager;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;

/**
 * Register to {@link org.xwiki.observation.ObservationManager} for all events and send them to
 * {@link RemoteObservationManager}.
 * 
 * @version $Id$
 * @since 2.0M3
 */
@Component
@Named("observation.remote")
@Singleton
public class LocalEventListener implements EventListener
{
    /**
     * The name of the listener.
     */
    private static final String NAME = "observation.remote";

    /**
     * Used to know if remote observation manager is enabled.
     */
    @Inject
    private RemoteObservationManagerConfiguration configuration;

    /**
     * Used to lookup for {@link RemoteObservationManager} if it's enabled. To avoid initializing it if it's not needed.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * We don't inject {@link RemoteObservationManager} automatically to load it only when necessary (when remote
     * observation manager is enabled).
     */
    private RemoteObservationManager remoteObservationManager;

    @Override
    public List<Event> getEvents()
    {
        List<Event> events;

        if (this.configuration.isEnabled()) {
            events = Collections.<Event> singletonList(AllEvent.ALLEVENT);
        } else {
            events = Collections.emptyList();
        }

        return events;
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (this.remoteObservationManager == null) {
            try {
                // Make sure to not receive events until RemoteObservationManager is ready
                ObservationManager om = this.componentManager.getInstance(ObservationManager.class);
                om.removeListener(getName());
                this.remoteObservationManager = this.componentManager.getInstance(RemoteObservationManager.class);
                om.addListener(this);

                this.remoteObservationManager.notify(new LocalEventData(event, source, data));
            } catch (ComponentLookupException e) {
                this.logger.error("Failed to initialize the Remote Observation Manager", e);
            }
        } else {
            this.remoteObservationManager.notify(new LocalEventData(event, source, data));
        }
    }
}
