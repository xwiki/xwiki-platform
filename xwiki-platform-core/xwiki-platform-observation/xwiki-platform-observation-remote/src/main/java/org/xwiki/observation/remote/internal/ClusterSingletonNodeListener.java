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

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.observation.event.ApplicationStoppedEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.ClusterSingletonManager;
import org.xwiki.observation.remote.NetworkChannelLeaderChangedEvent;
import org.xwiki.observation.remote.NetworkMemberJoinedEvent;
import org.xwiki.observation.remote.NetworkMemberLeftEvent;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;

/**
 * Updates the {@link DefaultClusterSingletonManager} when the members of a channel change and when the application
 * starts and stops.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Component
@Named(ClusterSingletonNodeListener.NAME)
@Singleton
public class ClusterSingletonNodeListener extends AbstractEventListener
{
    /**
     * The name of the listener.
     */
    public static final String NAME = "org.xwiki.observation.remote.internal.ClusterSingletonNodeListener";

    @Inject
    private RemoteObservationManagerConfiguration configuration;

    @Inject
    private ClusterSingletonManager singletonManager;

    /**
     * Default constructor.
     */
    public ClusterSingletonNodeListener()
    {
        super(NAME, new NetworkChannelLeaderChangedEvent(), new NetworkMemberJoinedEvent(),
            new NetworkMemberLeftEvent(), new ApplicationStartedEvent(), new ApplicationStoppedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (this.singletonManager instanceof DefaultClusterSingletonManager manager) {
            if (event instanceof ApplicationStoppedEvent) {
                manager.stop();
            } else if (this.configuration.isEnabled()) {
                // The channel events are received from the network thread, which must not be blocked
                manager.updateAsync();
            }
        }
    }
}
