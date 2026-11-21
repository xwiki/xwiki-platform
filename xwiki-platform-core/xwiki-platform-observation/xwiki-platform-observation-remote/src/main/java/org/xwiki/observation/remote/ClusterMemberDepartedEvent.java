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
package org.xwiki.observation.remote;

import org.xwiki.observation.event.Event;
import org.xwiki.stability.Unstable;

/**
 * Triggered when a member of the cluster is considered departed: it has been absent from every running channel for
 * at least {@link RemoteObservationManagerConfiguration#getReclaimDelay()} seconds (see
 * {@link ClusterMemberDepartureManager}). It is a local event: it is never sent to the other members of the cluster,
 * and each member notifies it on its own.
 * <p>
 * The event is notified from a dedicated thread, once when the member becomes departed, and again each time a
 * departed member is {@link ClusterMemberDepartureManager#watch(java.util.Collection) watched}. The listeners must not
 * block, and the work they take over must be idempotent. The listeners which take over the pending work of the
 * departed member generally extend {@link org.xwiki.observation.event.AbstractClusterSingletonListener}, so that only
 * the cluster singleton node does it.
 * <p>
 * The event also send the following parameters:
 * </p>
 * <ul>
 * <li>source: the {@link ClusterMemberDepartureManager}</li>
 * <li>data: the identifier of the departed member, as a {@link String}</li>
 * </ul>
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Unstable
public class ClusterMemberDepartedEvent implements Event
{
    /**
     * Creates an event matching the departure of any member.
     */
    public ClusterMemberDepartedEvent()
    {
        // Nothing to initialize
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent instanceof ClusterMemberDepartedEvent;
    }
}
