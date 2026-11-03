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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
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
import org.xwiki.observation.remote.ClusterSingletonManager;
import org.xwiki.observation.remote.ClusterSingletonNodeChangedEvent;
import org.xwiki.observation.remote.NetworkChannel;
import org.xwiki.observation.remote.NetworkMember;
import org.xwiki.observation.remote.RemoteObservationManager;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;
import org.xwiki.observation.remote.internal.jgroups.JGroupsNetworkChannel;

/**
 * Default implementation of {@link ClusterSingletonManager}, based on the members of the channels of the
 * {@link RemoteObservationManager}.
 * <p>
 * The answer is cached: it's computed the first time it's asked and then again each time the members of a channel
 * change, the application starts or stops (see {@link ClusterSingletonNodeListener}). Each change of the answer is
 * logged and notified with a {@link ClusterSingletonNodeChangedEvent}.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Component
@Singleton
public class DefaultClusterSingletonManager implements ClusterSingletonManager, Disposable
{
    /**
     * How long to wait for the pending notifications when the component is disposed, in seconds.
     */
    private static final long DISPOSE_TIMEOUT = 5;

    private static final String NO_RUNNING_CHANNEL = "no running channel";

    private record Election(boolean singletonNode, String reason, NetworkChannel channel,
        List<? extends NetworkMember> members)
    {
    }

    @Inject
    private RemoteObservationManagerConfiguration configuration;

    /**
     * Not injected directly since initializing the remote observation manager starts the channels, which should not
     * happen when remote observation is disabled.
     */
    @Inject
    private Provider<RemoteObservationManager> remoteObservationManagerProvider;

    @Inject
    private Provider<ObservationManager> observationManagerProvider;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

    private volatile boolean stopping;

    /**
     * The current answer, {@code null} until it's computed for the first time.
     */
    private volatile Boolean singletonNode;

    /**
     * Guards the changes of {@link #singletonNode} and the logs. Never held while notifying the listeners or waiting for
     * anything else (the listeners act while holding their own lock and may call {@link #isSingletonNode()}).
     */
    private final Object lock = new Object();

    private boolean stateLogged;

    private boolean noRunningChannelWarned;

    /**
     * Used to update the answer from a dedicated thread (the thread is only created when the first task is submitted)
     * when the members of a channel change, so that the thread receiving the network changes is never blocked.
     */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "XWiki cluster singleton node");
        thread.setDaemon(true);
        return thread;
    });

    @Override
    public boolean isSingletonNode()
    {
        if (this.stopping) {
            return false;
        }

        if (!this.configuration.isEnabled()) {
            // No cluster: this instance does everything
            return true;
        }

        Boolean current = this.singletonNode;
        if (current == null) {
            // First call: compute the answer (not logged nor notified, the listener takes care of it)
            Election election = elect(getChannels());
            synchronized (this.lock) {
                if (this.singletonNode == null) {
                    this.singletonNode = election.singletonNode();
                }
                current = this.singletonNode;
            }
        }

        return current;
    }

    /**
     * Compute the answer again, from a dedicated thread.
     */
    public void updateAsync()
    {
        try {
            this.executor.execute(new ExecutionContextRunnable(this::update, this.componentManager));
        } catch (RejectedExecutionException e) {
            // The component is disposed: the instance is stopping and it's not the singleton node anymore
            this.logger.debug("Ignoring a cluster singleton node update requested after the component was disposed",
                e);
        }
    }

    /**
     * Compute the answer again, log it and notify the listeners if it changed. Called from the dedicated thread.
     */
    public void update()
    {
        // Initializing the remote observation manager can take time (connecting the channels): don't hold the lock
        Collection<NetworkChannel> channels = getChannels();

        boolean changed;
        Election election;
        synchronized (this.lock) {
            election = elect(channels);
            changed = setSingletonNode(election);
        }

        if (changed) {
            notifyChange(election.singletonNode());
        }
    }

    /**
     * Indicate that the instance is stopping: it's not the singleton node anymore. The listeners are notified
     * synchronously if the instance was the singleton node.
     */
    public void stop()
    {
        // Without cluster the instance was always the singleton node, otherwise only the computed answer matters (it
        // was not given to anyone if it was never computed)
        if (this.stopping) {
            return;
        }

        boolean wasSingletonNode = !this.configuration.isEnabled() || Boolean.TRUE.equals(this.singletonNode);

        this.stopping = true;

        boolean changed;
        synchronized (this.lock) {
            this.singletonNode = false;
            changed = wasSingletonNode;
            if (changed && this.configuration.isEnabled()) {
                this.logger.info("This instance is no longer the cluster singleton node (the instance is stopping)");
            }
        }

        if (changed) {
            notifyChange(false);
        }
    }

    private boolean setSingletonNode(Election election)
    {
        if (NO_RUNNING_CHANNEL.equals(election.reason()) && !this.noRunningChannelWarned) {
            this.logger.warn("Remote observation is enabled but none of the configured channels {} is running:"
                + " this instance runs the tasks executed only once in the cluster.", this.configuration.getChannels());
            this.noRunningChannelWarned = true;
        }

        Boolean previous = this.singletonNode;
        this.singletonNode = election.singletonNode();

        boolean changed = previous != null && previous != election.singletonNode();
        if (changed) {
            if (election.singletonNode()) {
                this.logger.info("This instance is now the cluster singleton node ({})", describe(election));
            } else {
                this.logger.info("This instance is no longer the cluster singleton node ({})", describe(election));
            }
        } else if (!this.stateLogged) {
            if (election.singletonNode()) {
                this.logger.info("This instance is the cluster singleton node ({})", describe(election));
            } else {
                this.logger.info("This instance is not the cluster singleton node ({})", describe(election));
            }
        }
        this.stateLogged = true;

        return changed;
    }

    private Collection<NetworkChannel> getChannels()
    {
        if (this.stopping || this.configuration.getSingletonPriority() < 0) {
            // No need for the channels
            return List.of();
        }

        try {
            return this.remoteObservationManagerProvider.get().getChannels();
        } catch (Exception e) {
            this.logger.debug("Failed to get the remote observation channels", e);

            return List.of();
        }
    }

    private Election elect(Collection<NetworkChannel> channels)
    {
        if (this.stopping) {
            return new Election(false, "the instance is stopping", null, List.of());
        }

        if (this.configuration.getSingletonPriority() < 0) {
            return new Election(false, "negative singleton priority", null, List.of());
        }

        for (String channelId : this.configuration.getChannels()) {
            for (NetworkChannel channel : channels) {
                if (channelId.equals(channel.getId())) {
                    List<? extends NetworkMember> members = getMembers(channel);
                    if (members.stream().anyMatch(NetworkMember::isLocal)) {
                        return elect(channel, members);
                    }
                }
            }
        }

        // An isolated instance keeps doing everything
        return new Election(true, NO_RUNNING_CHANNEL, null, List.of());
    }

    private Election elect(NetworkChannel channel, List<? extends NetworkMember> members)
    {
        // The highest priority wins, the members are sorted from the oldest to the youngest so the oldest member wins
        // in case of tie
        NetworkMember elected = null;
        for (NetworkMember member : members) {
            if (elected == null || member.getSingletonPriority() > elected.getSingletonPriority()) {
                elected = member;
            }
        }

        // A negative priority means never
        return new Election(elected != null && elected.getSingletonPriority() >= 0 && elected.isLocal(), null,
            channel, members);
    }

    /**
     * @return the members of the channel from the oldest to the youngest, based on the network view when the channel
     *         exposes it (the identifiers of the members are not needed for the election)
     */
    private List<? extends NetworkMember> getMembers(NetworkChannel channel)
    {
        if (channel instanceof JGroupsNetworkChannel jgroupsChannel) {
            return jgroupsChannel.getViewMembers();
        }

        return List.copyOf(channel.getMembers());
    }

    private String describe(Election election)
    {
        if (election.channel() == null) {
            return election.reason();
        }

        StringBuilder builder = new StringBuilder();
        builder.append("channel [").append(election.channel().getId()).append("], members [");
        for (int i = 0; i < election.members().size(); i++) {
            NetworkMember member = election.members().get(i);
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(member.getId()).append(" (priority ").append(member.getSingletonPriority());
            if (member.isLocal()) {
                builder.append(", this instance");
            }
            builder.append(')');
        }
        builder.append(']');

        return builder.toString();
    }

    private void notifyChange(boolean newSingletonNode)
    {
        // Called without holding any lock: the listeners act while holding their own lock (see
        // AbstractClusterSingletonListener) and the stopping thread waits for them
        this.observationManagerProvider.get().notify(new ClusterSingletonNodeChangedEvent(), this, newSingletonNode);
    }

    @Override
    public void dispose()
    {
        this.executor.shutdown();
        try {
            if (!this.executor.awaitTermination(DISPOSE_TIMEOUT, TimeUnit.SECONDS)) {
                this.executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            this.executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
