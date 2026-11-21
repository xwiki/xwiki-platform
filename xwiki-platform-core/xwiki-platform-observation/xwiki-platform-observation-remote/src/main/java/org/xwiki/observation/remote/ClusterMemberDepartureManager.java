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

import java.util.Collection;
import java.util.Date;
import java.util.Set;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Tells which members of the cluster left it for good, so that the work they left pending can be taken over by
 * another member (generally the cluster singleton node, see {@link ClusterSingletonManager}).
 * <p>
 * Some work is attached to the identifier of the member which produced it (see
 * {@link RemoteObservationManagerConfiguration#getId()}) and is only handled by that member. A member can't tell from
 * the network whether another member is restarting or gone for good, so a member identifier is considered
 * <em>departed</em> once it has been absent from every running channel for at least
 * {@link RemoteObservationManagerConfiguration#getReclaimDelay()} seconds. The absence is measured in memory by each
 * member, from the moment it sees the identifier leave a channel, or from the moment it's asked to
 * {@link #watch(Collection) watch} an identifier found in stored data (for example an identifier which left before the
 * current instance started). When an identifier becomes departed a {@link ClusterMemberDepartedEvent} is notified, and
 * when it comes back it's not departed anymore.
 * <p>
 * Nothing is ever departed when remote observation is disabled, when the delay is negative, when the instance is
 * stopping, or when no channel is running (a channel which failed to start or is reconnecting). A departure is also
 * postponed while the network view of a running channel contains a member (other than the current instance) whose
 * identifier is not known yet, since it could be the absent member. Nothing is persisted and no message is exchanged
 * with the other members: a restarted instance measures the absences again, so the departure can be noticed later
 * than expected but never earlier. For an identifier which was never seen (only {@link #watch(Collection) watched}),
 * the delay starts when it's watched, so it must also cover the time all the members need to join the cluster after
 * a full start of the cluster.
 * <p>
 * A member which is alone in its view (because the other members left, or because it's cut from them) is a network
 * partition of one: no quorum is required, so that the remaining member of a cluster scaled down to one member takes
 * over the work of the others. During a network partition which lasts longer than the delay, the members of each
 * partition consider the members of the other partitions departed: the work taken over must be idempotent or accept
 * to be done twice in that case.
 * <p>
 * The departures are tracked by identifier: when several members share an identifier (a misconfiguration), the
 * identifier is absent only once its last member left, and a member which can't tell the members sharing its own
 * identifier from unidentified members postpones the departures.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Role
@Unstable
public interface ClusterMemberDepartureManager
{
    /**
     * Gives the identifiers of the current members of the cluster, as known by the current instance.
     *
     * <p>
     * The result is the union of the identified members of all the running channels, which can differ from the
     * members among which the cluster singleton node is elected (see {@link ClusterSingletonManager}).
     *
     * @return the identifiers of the members of the running channels (see {@link NetworkChannel#getMembers()}),
     *         always including the identifier of the current instance (only that one when remote observation is
     *         disabled or no channel is running); the members whose identifier is not known yet are not included
     */
    Set<String> getMemberIds();

    /**
     * Reports member identifiers found in data waiting to be processed by those members, for example in a store
     * scanned when the application is ready or when the current instance becomes the cluster singleton node.
     * <p>
     * Each identifier which is not the identifier of a current member is watched: its absence is measured from now,
     * or from the moment it was seen leaving if that's earlier, and a {@link ClusterMemberDepartedEvent} is notified
     * once it's departed. If the identifier is already departed the event is notified again right away (from another
     * thread), so the caller can take over the work it just found. The call itself never blocks on the network and
     * the events are never notified from the calling thread.
     *
     * @param memberIds the member identifiers found in the data, {@code null} elements and the identifier of the
     *            current instance are ignored
     */
    void watch(Collection<String> memberIds);

    /**
     * Indicates if the passed identifier is departed, checked again against the current members of the cluster (a
     * member which came back is not departed anymore, even if the notification of its return was not processed yet).
     *
     * @param memberId the identifier of a member, for example {@code xwiki-web-1}
     * @return {@code true} if the member identified by the passed identifier has been absent from every running channel
     *         for at least {@link RemoteObservationManagerConfiguration#getReclaimDelay()} seconds, {@code false}
     *         otherwise (including when the identifier is {@code null}, unknown, when nothing can be departed, or
     *         while the departures are postponed, see {@link ClusterMemberDepartureManager})
     */
    boolean isDeparted(String memberId);

    /**
     * Gives the date since which a member is absent, as seen by the current instance. Work produced by a departed
     * member before that date was left pending by it, and work produced after that date (by a member which came back
     * with the same identifier) belongs to that member again.
     *
     * @param memberId the identifier of a member, for example {@code xwiki-web-1}
     * @return the date since which the member is absent from every running channel (the moment the current instance
     *         saw it leave, or the moment it was first {@link #watch(Collection) watched}), or {@code null} if the
     *         member is present, unknown, if nothing can be departed, or while the departures are postponed (see
     *         {@link ClusterMemberDepartureManager})
     */
    Date getDepartureDate(String memberId);
}
