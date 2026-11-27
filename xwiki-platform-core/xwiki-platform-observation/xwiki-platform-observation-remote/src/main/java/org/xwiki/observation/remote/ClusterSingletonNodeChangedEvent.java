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
 * Triggered when the current instance becomes or stops being the cluster singleton node (see
 * {@link ClusterSingletonManager}). It is a local event: it is never sent to the other members of the cluster.
 * <p>
 * The event is notified from a dedicated thread, never from the thread which received the network change, and only
 * when the answer of {@link ClusterSingletonManager#isSingletonNode()} differs from the previous one it gave.
 * <p>
 * The event also send the following parameters:
 * </p>
 * <ul>
 * <li>source: the {@link ClusterSingletonManager}</li>
 * <li>data: a {@link Boolean}, {@code true} when the current instance became the cluster singleton node and
 * {@code false} when it stopped being it</li>
 * </ul>
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Unstable
public class ClusterSingletonNodeChangedEvent implements Event
{
    /**
     * Creates an event matching all the changes of cluster singleton node.
     */
    public ClusterSingletonNodeChangedEvent()
    {
        // Nothing to initialize
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent instanceof ClusterSingletonNodeChangedEvent;
    }
}
