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
package com.xpn.xwiki.plugin.scheduler.internal;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.observation.event.AbstractClusterSingletonListener;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;

import com.xpn.xwiki.plugin.scheduler.SchedulerPlugin;

/**
 * Runs the scheduler only on the cluster singleton node (see
 * {@link org.xwiki.observation.remote.ClusterSingletonManager}) when remote observation and
 * {@code scheduler.clusterSingleton} are enabled: starts it when the application is ready if the current instance is
 * the singleton node, starts it when the current instance becomes the singleton node (the jobs are restored from their
 * pages), and stops it when the current instance stops being the singleton node. A singleton node whose scheduler is
 * disabled ({@code scheduler.enabled=false}) logs a warning, since no member runs the scheduler jobs then.
 * <p>
 * The {@link SchedulerPlugin} is not a component: it registers itself and this listener when it's initialized.
 * Otherwise (no cluster, or {@code scheduler.clusterSingleton} disabled), the plugin starts the scheduler as soon as
 * it's initialized when {@code scheduler.enabled} is true, and this listener is not registered.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Component(roles = SchedulerClusterSingletonListener.class)
@Singleton
public class SchedulerClusterSingletonListener extends AbstractClusterSingletonListener
{
    /**
     * The name of the listener.
     */
    public static final String NAME = "com.xpn.xwiki.plugin.scheduler.internal.SchedulerClusterSingletonListener";

    /**
     * The name of the property which enables running the scheduler on the cluster singleton node only.
     */
    public static final String PROPERTY_CLUSTER_SINGLETON = "scheduler.clusterSingleton";

    @Inject
    private RemoteObservationManagerConfiguration remoteObservationConfiguration;

    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configuration;

    @Inject
    private Logger logger;

    private volatile SchedulerPlugin schedulerPlugin;

    /**
     * Listen to the event which starts the scheduler on the cluster singleton node.
     */
    public SchedulerClusterSingletonListener()
    {
        super(NAME, new ApplicationReadyEvent());
    }

    /**
     * @param schedulerPlugin the scheduler plugin to start and stop
     */
    public void setSchedulerPlugin(SchedulerPlugin schedulerPlugin)
    {
        this.schedulerPlugin = schedulerPlugin;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The scheduler must run on the singleton node only when remote observation and {@code scheduler.clusterSingleton}
     * are enabled.
     *
     * @see org.xwiki.observation.event.AbstractClusterSingletonListener#isSingletonNodeRequired()
     */
    @Override
    public boolean isSingletonNodeRequired()
    {
        return this.remoteObservationConfiguration.isEnabled()
            && this.configuration.getProperty(PROPERTY_CLUSTER_SINGLETON, false);
    }

    @Override
    protected void processSingletonEvent(Event event, Object source, Object data)
    {
        // The listener is only registered when isSingletonNodeRequired() is true, but the base class calls this method
        // on every member when it's false
        if (isSingletonNodeRequired()) {
            start("this instance is the cluster singleton node");
        }
    }

    @Override
    protected void onSingletonNodeGained()
    {
        start("this instance became the cluster singleton node");
    }

    @Override
    protected void onSingletonNodeLost()
    {
        SchedulerPlugin plugin = this.schedulerPlugin;
        if (plugin != null && plugin.isEnabled()) {
            this.logger.info("Stopping the scheduler: this instance is no longer the cluster singleton node");
            plugin.stopScheduler();
        }
    }

    private void start(String reason)
    {
        SchedulerPlugin plugin = this.schedulerPlugin;
        if (plugin != null) {
            if (plugin.isEnabled()) {
                this.logger.info("Starting the scheduler: {}", reason);
                plugin.startScheduler();
            } else {
                this.logger.warn("This instance is the cluster singleton node but its scheduler is disabled"
                    + " (scheduler.enabled=false): no cluster member runs the scheduler jobs. Enable the scheduler on"
                    + " this instance, or give a higher observation.remote.singletonPriority to a member where it's"
                    + " enabled.");
            }
        }
    }
}
