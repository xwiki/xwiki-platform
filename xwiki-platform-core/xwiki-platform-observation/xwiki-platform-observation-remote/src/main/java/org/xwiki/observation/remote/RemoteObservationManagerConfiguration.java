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

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Provide configuration for remote observation manager.
 *
 * @version $Id$
 * @since 2.0M3
 */
@Role
public interface RemoteObservationManagerConfiguration
{
    /**
     * @return indicate if the remote observation manager is enabled
     */
    boolean isEnabled();

    /**
     * @return the channels to start at init
     */
    List<String> getChannels();

    /**
     * @return the identifier of the network adapter implementation to use to actually send and receive network messages
     */
    String getNetworkAdapter();

    /**
     * Gives the priority of the current instance in the election of the cluster singleton node (see
     * {@link ClusterSingletonManager}): the member with the highest priority runs the tasks executed only once in the
     * cluster and, when several members have that priority, the oldest one runs them. It's generally used to make a
     * member dedicated to background tasks the singleton node, the other members taking over (or not, with a negative
     * priority) when it's not part of the cluster.
     *
     * @return the singleton priority of the current instance, {@code 0} by default, a negative value means the current
     *         instance is never the singleton node
     * @since 18.10.0RC1
     */
    @Unstable
    default int getSingletonPriority()
    {
        return 0;
    }

    /**
     * Gives how long a member must be absent from the cluster before it's considered gone for good (see
     * {@link ClusterMemberDepartureManager}), so that the work it left pending can be taken over by another member. It
     * should be longer than the time a member needs to restart, since a member which comes back handles its own
     * pending work, and longer than the time every member needs to join the cluster after a full start of the cluster,
     * since the absence of a member which was never seen is measured from the moment its pending work is found.
     *
     * @return the delay in seconds, {@code 900} (15 minutes) by default, a negative value means that a member is never
     *         considered gone for good
     * @since 18.10.0RC1
     */
    @Unstable
    default long getReclaimDelay()
    {
        return 900;
    }

    /**
     * Gives the maximum age of the pending work of a member gone for good which is taken over by another member (see
     * {@link ClusterMemberDepartureManager}): the work produced more than this before the member left is not taken
     * over (for example, to avoid notifying the users about very old events when the identifier of a member changed
     * long ago). The members also forget a member gone for good once it has been absent for the reclaim delay plus
     * this maximum age.
     *
     * @return the maximum age in seconds, {@code 86400} (1 day) by default, a negative value means no limit (the
     *         members then remember every member gone for good until they restart)
     * @since 18.10.0RC1
     */
    @Unstable
    default long getReclaimMaxAge()
    {
        return 86400;
    }

    /**
     * @return the unique identifier of the instance in the cluster, or {@code null} if the component does not support
     *     provisioning an identifier (e.g., a component implemented before 14.1RC1)
     * @since 14.1RC1
     */
    default String getId()
    {
        return null;
    }
}
