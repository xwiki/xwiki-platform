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

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Indicates which member of the cluster must run the tasks executed only once in the cluster, for example the tasks
 * working on a resource shared by all the members (the database, a remote Solr server) which would otherwise be
 * duplicated by each member.
 * <p>
 * The singleton node is elected among the members of the first running channel listed in
 * {@link RemoteObservationManagerConfiguration#getChannels()} (a channel is running when its members include the
 * current instance, see {@link NetworkMember#isLocal()}): the member with the highest
 * {@link NetworkMember#getSingletonPriority() singleton priority} wins, and when several members share that priority
 * the oldest one (the {@link NetworkChannel#getLeader() leader} of the channel) wins. A member with a negative priority
 * is never elected. Every member computes the same answer from the same list of members, so no message is exchanged.
 * During a network partition, each partition has its own singleton node.
 * <p>
 * A {@link NetworkAdapter} whose members don't implement {@link NetworkMember#isLocal()} never has a running channel,
 * so every member is the singleton node, as if this election did not exist.
 * <p>
 * Each change of the answer for the current instance is notified with a {@link ClusterSingletonNodeChangedEvent}.
 * Listeners whose work must run on the singleton node only can extend
 * {@link org.xwiki.observation.event.AbstractClusterSingletonListener}.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Role
@Unstable
public interface ClusterSingletonManager
{
    /**
     * Indicates if the current instance is the one which must run the tasks executed only once in the cluster.
     * <p>
     * The answer can change at any time, when a member joins or leaves the cluster. The rules are applied in this
     * order:
     * <ol>
     * <li>{@code false} once the instance is stopping ({@code ApplicationStoppedEvent} was received)</li>
     * <li>{@code true} when remote observation is disabled</li>
     * <li>{@code false} when the {@link RemoteObservationManagerConfiguration#getSingletonPriority() singleton priority}
     * of the current instance is negative</li>
     * <li>{@code true} when no channel is running (the instance is alone)</li>
     * <li>otherwise the result of the election described in {@link ClusterSingletonManager}</li>
     * </ol>
     *
     * @return {@code true} if the current instance is the cluster singleton node
     */
    boolean isSingletonNode();
}
