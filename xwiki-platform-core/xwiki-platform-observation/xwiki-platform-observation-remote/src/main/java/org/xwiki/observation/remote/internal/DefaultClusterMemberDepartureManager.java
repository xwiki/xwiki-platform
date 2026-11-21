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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Disposable;
import org.xwiki.context.concurrent.ExecutionContextRunnable;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.remote.ClusterMemberDepartedEvent;
import org.xwiki.observation.remote.ClusterMemberDepartureManager;
import org.xwiki.observation.remote.NetworkMember;
import org.xwiki.observation.remote.RemoteObservationManager;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;
import org.xwiki.observation.remote.internal.jgroups.JGroupsNetworkChannel;
import org.xwiki.observation.remote.internal.jgroups.JGroupsNetworkChannels;

/**
 * Default implementation of {@link ClusterMemberDepartureManager}, based on the members of the running JGroups
 * channels (the channels of {@link RemoteObservationManager#getChannels()}). The channels are read from their
 * registry so that reading them never initializes the {@link RemoteObservationManager} (which starts the channels).
 * <p>
 * Everything is done by a single task, the sweep, run from a dedicated thread (never from the thread which received
 * the network change nor from the thread calling {@link #watch(Collection)}) each time the members of a channel
 * change, the application starts, identifiers are watched, and when the next absent identifier is due. The sweep
 * compares the members with the ones seen during the previous sweep (the identifiers which are not listed anymore
 * are absent from then on, and the absent identifiers which are listed again are present), registers the watched
 * identifiers, and notifies the {@link ClusterMemberDepartedEvent} for the identifiers absent for longer than the
 * delay. At most one sweep is scheduled at any time, and only while some identifier is waiting for its departure.
 * <p>
 * A departure is postponed while a member of the network view of a running channel (other than the current instance)
 * has no known identifier, since that member could be the absent one (for example a member which came back after a
 * network partition, or whose identifier could not be exchanged yet).
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Component
@Singleton
public class DefaultClusterMemberDepartureManager implements ClusterMemberDepartureManager, Disposable
{
    /**
     * How long to wait for the running task when the component is disposed, in seconds.
     */
    private static final long DISPOSE_TIMEOUT = 5;

    /**
     * The maximum delay before sweeping again when the members can't be known, in seconds.
     */
    private static final long MAX_RETRY_DELAY = 60;

    /**
     * The maximum value of the configured durations (about 100 years, in seconds), to avoid overflows.
     */
    private static final long MAX_DURATION = 3_155_760_000L;

    /**
     * The members of the running channels.
     *
     * @param ids the identifiers of the members of the running channels, including the current instance
     * @param unidentified {@code true} if the network view of a running channel contains a member (other than the
     *            current instance) whose identifier is not known
     */
    private record Members(Set<String> ids, boolean unidentified)
    {
    }

    @Inject
    private RemoteObservationManagerConfiguration configuration;

    @Inject
    private JGroupsNetworkChannels channels;

    @Inject
    private Provider<ObservationManager> observationManagerProvider;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

    /**
     * Guards the state below. Never held while notifying the listeners or accessing the network.
     */
    private final Object lock = new Object();

    /**
     * The date since which each absent member identifier is absent.
     */
    private final Map<String, Instant> absentSince = new LinkedHashMap<>();

    /**
     * The absent member identifiers whose departure was notified.
     */
    private final Set<String> departed = new HashSet<>();

    /**
     * The absent member identifiers whose departure is postponed because some members are not identified (logged once).
     */
    private final Set<String> postponed = new HashSet<>();

    /**
     * The identifiers passed to {@link #watch(Collection)} and not processed yet by a sweep.
     */
    private final Set<String> pendingWatches = new LinkedHashSet<>();

    /**
     * The identifiers of the members seen during the previous sweep.
     */
    private Set<String> knownMemberIds = Set.of();

    /**
     * The next scheduled sweep, if any.
     */
    private Future<?> nextSweep;

    /**
     * The time of the next scheduled sweep, if any.
     */
    private Instant nextSweepTime;

    /**
     * The executor running the sweeps, created when the first one is submitted.
     */
    private ScheduledExecutorService executor;

    /**
     * The source of the current time (replaced in the tests).
     */
    private Clock clock = Clock.systemUTC();

    private volatile boolean stopping;

    @Override
    public Set<String> getMemberIds()
    {
        Members members = isRemoteObservationEnabled() ? getCurrentMembers() : null;
        if (members == null) {
            String currentId = this.configuration.getId();
            return currentId != null ? Set.of(currentId) : Set.of();
        }

        return Collections.unmodifiableSet(members.ids());
    }

    @Override
    public void watch(Collection<String> memberIds)
    {
        if (memberIds != null && isEnabled()) {
            // Copy the identifiers since the caller might modify its collection
            List<String> ids = memberIds.stream().filter(Objects::nonNull).distinct().toList();
            if (!ids.isEmpty()) {
                synchronized (this.lock) {
                    this.pendingWatches.addAll(ids);
                }
                execute(() -> sweep(null));
            }
        }
    }

    @Override
    public boolean isDeparted(String memberId)
    {
        Instant since = getAbsentSince(memberId);

        return since != null && !now().isBefore(since.plusSeconds(getDelay()));
    }

    @Override
    public Date getDepartureDate(String memberId)
    {
        Instant since = getAbsentSince(memberId);

        return since != null ? Date.from(since) : null;
    }

    /**
     * @return the date since which the passed member is absent, or {@code null} if it's present, not known as absent,
     *         if nothing can be departed, or if some members are not identified
     */
    private Instant getAbsentSince(String memberId)
    {
        if (memberId == null || !isEnabled()) {
            return null;
        }

        Instant since;
        synchronized (this.lock) {
            since = this.absentSince.get(memberId);
        }

        if (since != null) {
            // Check again the current members, in case the member came back and the sweep did not run yet
            Members members = getCurrentMembers();
            if (members == null || members.unidentified() || members.ids().contains(memberId)) {
                return null;
            }
        }

        return since;
    }

    /**
     * Compare the current members with the ones seen previously, from a dedicated thread.
     *
     * @param leftMemberId the identifier of a member which was reported as having left a channel, or {@code null}
     */
    public void updateAsync(String leftMemberId)
    {
        if (isEnabled()) {
            execute(() -> sweep(leftMemberId));
        }
    }

    /**
     * Indicate that the instance is stopping: nothing is departed or notified anymore.
     */
    public void stop()
    {
        this.stopping = true;

        synchronized (this.lock) {
            cancelSweep();
        }
    }

    /**
     * Compare the current members with the ones seen during the previous sweep (the members which are not listed
     * anymore, and the passed member if it's not listed, are absent from now on, and the absent members which are
     * listed again are present), then register the watched identifiers and notify the departures which are due.
     *
     * @param leftMemberId the identifier of a member which was reported as having left a channel, or {@code null}
     */
    private void sweep(String leftMemberId)
    {
        if (!isEnabled()) {
            return;
        }

        Members members = getCurrentMembers();

        List<String> toNotify = new ArrayList<>();
        synchronized (this.lock) {
            if (members == null) {
                // No running channel: nothing can be told about the other members, try again later if something is
                // waiting
                if (!this.pendingWatches.isEmpty() || this.departed.size() < this.absentSince.size()) {
                    scheduleSweep(now().plusSeconds(getRetryDelay()));
                }
            } else {
                Instant now = now();
                refresh(members.ids(), leftMemberId, now);
                processWatches(members.ids(), now, toNotify);
                processDepartures(members.unidentified(), now, toNotify);
            }
        }

        // Called without holding the lock: the listeners may call this component
        for (String memberId : toNotify) {
            notifyDeparted(memberId);
        }
    }

    private void scheduledSweep()
    {
        // The sweeps are scheduled only from the executor thread, which is the one running this one, so the scheduled
        // sweep is this one
        synchronized (this.lock) {
            this.nextSweep = null;
            this.nextSweepTime = null;
        }

        sweep(null);
    }

    /**
     * Must be called while holding the lock.
     */
    private void refresh(Set<String> currentMemberIds, String leftMemberId, Instant now)
    {
        // The absent members which came back
        for (Iterator<String> it = this.absentSince.keySet().iterator(); it.hasNext();) {
            String memberId = it.next();
            if (currentMemberIds.contains(memberId)) {
                it.remove();
                this.postponed.remove(memberId);
                if (this.departed.remove(memberId)) {
                    this.logger.info("Cluster member [{}] came back after being considered gone for good", memberId);
                } else {
                    this.logger.info("Cluster member [{}] came back", memberId);
                }
            }
        }

        // The members which left
        Set<String> candidates = new LinkedHashSet<>(this.knownMemberIds);
        if (leftMemberId != null) {
            candidates.add(leftMemberId);
        }
        for (String memberId : candidates) {
            if (!currentMemberIds.contains(memberId) && this.absentSince.putIfAbsent(memberId, now) == null) {
                this.logger.info("Cluster member [{}] left, its pending work will be taken over in [{}] seconds if it"
                    + " does not come back", memberId, getDelay());
            }
        }

        this.knownMemberIds = currentMemberIds;

        prune(now);
    }

    /**
     * Must be called while holding the lock.
     */
    private void processWatches(Set<String> currentMemberIds, Instant now, List<String> toNotify)
    {
        for (String memberId : this.pendingWatches) {
            if (!currentMemberIds.contains(memberId) && !memberId.equals(this.configuration.getId())) {
                if (this.departed.contains(memberId)) {
                    // Notify again, so that the caller can take over the work it found
                    toNotify.add(memberId);
                } else if (this.absentSince.putIfAbsent(memberId, now) == null) {
                    this.logger.info("Cluster member [{}] is not part of the cluster, its pending work will be taken"
                        + " over in [{}] seconds if it does not join", memberId, getDelay());
                }
            }
        }
        this.pendingWatches.clear();
    }

    /**
     * Must be called while holding the lock.
     */
    private void processDepartures(boolean unidentified, Instant now, List<String> toNotify)
    {
        long delay = getDelay();
        Instant next = null;
        for (Map.Entry<String, Instant> entry : this.absentSince.entrySet()) {
            String memberId = entry.getKey();
            if (!this.departed.contains(memberId)) {
                Instant departure = entry.getValue().plusSeconds(delay);
                if (now.isBefore(departure)) {
                    next = min(next, departure);
                } else if (unidentified) {
                    // The absent member might be the member which is not identified: wait
                    if (this.postponed.add(memberId)) {
                        this.logger.warn("Cluster member [{}] has been absent for more than [{}] seconds but is not"
                            + " considered gone for good yet because some members of the cluster are not identified",
                            memberId, delay);
                    }
                    next = min(next, now.plusSeconds(getRetryDelay()));
                } else {
                    this.departed.add(memberId);
                    this.postponed.remove(memberId);
                    toNotify.add(memberId);
                    this.logger.info("Cluster member [{}] is considered gone for good (absent since [{}])", memberId,
                        Date.from(entry.getValue()));
                }
            }
        }

        if (next != null) {
            scheduleSweep(next);
        }
    }

    private static Instant min(Instant instant1, Instant instant2)
    {
        return instant1 == null || instant2.isBefore(instant1) ? instant2 : instant1;
    }

    /**
     * Forget the members which are absent for longer than the delay plus the maximum age of the work to take over.
     * Must be called while holding the lock.
     */
    private void prune(Instant now)
    {
        long maxAge = getMaxAge();
        if (maxAge >= 0) {
            long delay = getDelay();
            for (Iterator<Map.Entry<String, Instant>> it = this.absentSince.entrySet().iterator(); it.hasNext();) {
                Map.Entry<String, Instant> entry = it.next();
                if (entry.getValue().plusSeconds(delay).plusSeconds(maxAge).isBefore(now)) {
                    it.remove();
                    this.departed.remove(entry.getKey());
                    this.postponed.remove(entry.getKey());
                    this.logger.debug("Forgetting cluster member [{}], absent since [{}]", entry.getKey(),
                        Date.from(entry.getValue()));
                }
            }
        }
    }

    private void notifyDeparted(String memberId)
    {
        if (!this.stopping) {
            this.observationManagerProvider.get().notify(new ClusterMemberDepartedEvent(), this, memberId);
        }
    }

    /**
     * @return the members of the running channels, or {@code null} when no channel is running
     */
    private Members getCurrentMembers()
    {
        Set<String> memberIds = null;
        boolean unidentified = false;
        for (JGroupsNetworkChannel channel : this.channels.getChannels().values()) {
            // The network view tells if the channel is running and if some members are not identified, independently
            // from the exchange of the identifiers (several members can share the same identifier)
            List<? extends NetworkMember> viewMembers = channel.getViewMembers();
            if (viewMembers.stream().anyMatch(NetworkMember::isLocal)) {
                if (memberIds == null) {
                    memberIds = new HashSet<>();
                }
                channel.getMembers().stream().map(NetworkMember::getId).filter(Objects::nonNull)
                    .forEach(memberIds::add);
                unidentified |= viewMembers.stream().anyMatch(member -> !member.isLocal() && member.getId() == null);
            }
        }

        if (memberIds == null) {
            return null;
        }

        String currentId = this.configuration.getId();
        if (currentId != null) {
            memberIds.add(currentId);
        }

        return new Members(memberIds, unidentified);
    }

    private boolean isRemoteObservationEnabled()
    {
        return !this.stopping && this.configuration.isEnabled();
    }

    private boolean isEnabled()
    {
        return isRemoteObservationEnabled() && this.configuration.getReclaimDelay() >= 0;
    }

    private long getDelay()
    {
        return Math.min(this.configuration.getReclaimDelay(), MAX_DURATION);
    }

    private long getMaxAge()
    {
        return Math.min(this.configuration.getReclaimMaxAge(), MAX_DURATION);
    }

    private long getRetryDelay()
    {
        return Math.max(1, Math.min(getDelay(), MAX_RETRY_DELAY));
    }

    private Instant now()
    {
        return this.clock.instant();
    }

    /**
     * Make sure a sweep runs at the passed time at the latest. Must be called while holding the lock.
     */
    private void scheduleSweep(Instant time)
    {
        if (this.nextSweepTime != null && !time.isBefore(this.nextSweepTime)) {
            // An earlier sweep is already scheduled, it will schedule the next one
            return;
        }

        cancelSweep();
        Future<?> sweep = schedule(this::scheduledSweep, Duration.between(now(), time));
        if (sweep != null) {
            this.nextSweep = sweep;
            this.nextSweepTime = time;
        }
    }

    /**
     * Must be called while holding the lock.
     */
    private void cancelSweep()
    {
        if (this.nextSweep != null) {
            this.nextSweep.cancel(false);
        }
        this.nextSweep = null;
        this.nextSweepTime = null;
    }

    private void execute(Runnable runnable)
    {
        try {
            getExecutor().execute(new ExecutionContextRunnable(runnable, this.componentManager));
        } catch (RejectedExecutionException e) {
            // The component is disposed: the instance is stopping
            this.logger.debug("Ignoring a cluster member departure task submitted after the component was disposed",
                e);
        }
    }

    private Future<?> schedule(Runnable runnable, Duration delay)
    {
        if (this.stopping) {
            return null;
        }

        try {
            return getExecutor().schedule(new ExecutionContextRunnable(runnable, this.componentManager),
                Math.max(0, delay.toMillis()), TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            // The component is disposed: the instance is stopping
            this.logger.debug("Ignoring a cluster member departure check scheduled after the component was disposed",
                e);

            return null;
        }
    }

    private ScheduledExecutorService getExecutor()
    {
        synchronized (this.lock) {
            if (this.executor == null) {
                ScheduledThreadPoolExecutor newExecutor = new ScheduledThreadPoolExecutor(1, runnable -> {
                    Thread thread = new Thread(runnable, "XWiki cluster member departure");
                    thread.setDaemon(true);
                    return thread;
                });
                newExecutor.setRemoveOnCancelPolicy(true);
                newExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
                this.executor = newExecutor;
            }

            return this.executor;
        }
    }

    @Override
    public void dispose()
    {
        this.stopping = true;

        ScheduledExecutorService currentExecutor;
        synchronized (this.lock) {
            currentExecutor = this.executor;
        }

        if (currentExecutor != null) {
            currentExecutor.shutdown();
            try {
                if (!currentExecutor.awaitTermination(DISPOSE_TIMEOUT, TimeUnit.SECONDS)) {
                    currentExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                currentExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
