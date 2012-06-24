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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.ApplicationStoppedEvent;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.NetworkAdapter;
import org.xwiki.observation.remote.RemoteEventData;
import org.xwiki.observation.remote.RemoteEventException;
import org.xwiki.observation.remote.RemoteObservationManager;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.observation.remote.converter.EventConverterManager;

/**
 * JGoups based {@link RemoteObservationManager}. It's also the default implementation for now.
 * 
 * @version $Id$
 * @since 2.0M3
 */
@Component
@Singleton
public class DefaultRemoteObservationManager implements RemoteObservationManager, Initializable
{
    /**
     * Access {@link RemoteObservationManager} configuration.
     */
    @Inject
    private RemoteObservationManagerConfiguration configuration;

    /**
     * Used to convert local event from and to remote event.
     */
    @Inject
    private EventConverterManager eventConverterManager;

    /**
     * Used to inject event coming from network.
     */
    @Inject
    private ObservationManager observationManager;

    /**
     * Used to set some extra information about the current event injected to the local {@link ObservationManager}.
     */
    @Inject
    private RemoteObservationManagerContext remoteEventManagerContext;

    /**
     * Used to initialize ExecutionContext for the remote->local thread.
     */
    @Inject
    private Execution execution;

    /**
     * Used to initialize ExecutionContext for the remote->local thread.
     */
    @Inject
    private ExecutionContextManager executionContextManager;

    /**
     * Used to lookup the network adapter.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * The network adapter to use to actually send and receive network messages.
     */
    private NetworkAdapter networkAdapter;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            String networkAdapterHint = this.configuration.getNetworkAdapter();
            this.networkAdapter = this.componentManager.getInstance(NetworkAdapter.class, networkAdapterHint);
        } catch (ComponentLookupException e) {
            throw new InitializationException("Failed to initialize network adapter ["
                + this.configuration.getNetworkAdapter() + "]", e);
        }

        // Start configured channels and register them against the JMX server
        for (String channelId : this.configuration.getChannels()) {
            try {
                startChannel(channelId);
            } catch (RemoteEventException e) {
                this.logger.error("Failed to start channel [" + channelId + "]", e);
            }
        }
    }

    @Override
    public void notify(LocalEventData localEvent)
    {
        if (this.remoteEventManagerContext.isRemoteState()) {
            // the event is a remote event
            return;
        }

        // Convert local->remote
        RemoteEventData remoteEvent = this.eventConverterManager.createRemoteEventData(localEvent);

        // if remote event data is not filled it means the message should not be sent to the network
        if (remoteEvent != null) {
            this.networkAdapter.send(remoteEvent);
        }

        if (localEvent.getEvent() instanceof ApplicationStoppedEvent) {
            try {
                this.networkAdapter.stopAllChannels();
            } catch (RemoteEventException e) {
                this.logger.error("Failed to stop channels", e);
            }
        }
    }

    @Override
    public void notify(RemoteEventData remoteEvent)
    {
        // Make sure the Execution context is properly initialized
        initializeContext();

        LocalEventData localEvent = this.eventConverterManager.createLocalEventData(remoteEvent);

        // send event
        if (localEvent != null) {
            // Indicate all the following events are remote events
            this.remoteEventManagerContext.pushRemoteState();

            try {
                this.observationManager.notify(localEvent.getEvent(), localEvent.getSource(), localEvent.getData());
            } finally {
                // Indicate all the following events are local events
                this.remoteEventManagerContext.popRemoteState();
            }
        }
    }

    @Override
    public void startChannel(String channelId) throws RemoteEventException
    {
        this.networkAdapter.startChannel(channelId);
    }

    @Override
    public void stopChannel(String channelId) throws RemoteEventException
    {
        this.networkAdapter.stopChannel(channelId);
    }

    /**
     * Make sure an ExecutionContext initialized for remote->local thread.
     */
    private void initializeContext()
    {
        if (this.execution.getContext() == null) {
            ExecutionContext context = new ExecutionContext();

            try {
                this.executionContextManager.initialize(context);
            } catch (Exception e) {
                this.logger.error("failed to initialize execution context", e);
            }
        }
    }
}
