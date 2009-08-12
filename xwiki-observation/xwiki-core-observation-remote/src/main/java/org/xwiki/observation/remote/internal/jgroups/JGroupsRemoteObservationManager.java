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
 *
 */
package org.xwiki.observation.remote.internal.jgroups;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.jgroups.ChannelException;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.Receiver;
import org.jgroups.conf.ConfiguratorFactory;
import org.jgroups.conf.ProtocolStackConfigurator;
import org.jgroups.conf.XmlConfigurator;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.container.Container;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.RemoteEventData;
import org.xwiki.observation.remote.RemoteEventException;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.observation.remote.RemoteObservationManager;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;
import org.xwiki.observation.remote.converter.EventConverterManager;
import org.xwiki.observation.remote.jgroups.JGroupsReceiver;

/**
 * JGoups based {@link RemoteObservationManager}. It's also the default implementation for now.
 * 
 * @version $Id$
 * @since 2.0M3
 */
@Component
public class JGroupsRemoteObservationManager extends AbstractLogEnabled implements RemoteObservationManager,
    Initializable
{
    /**
     * Relative path where to find jgroups channels configurations.
     */
    public static final String CONFIGURATION_PATH = "event/remote/jgroups/";

    /**
     * Access {@link RemoteObservationManager} configuration.
     */
    @Requirement
    private RemoteObservationManagerConfiguration configuration;

    /**
     * Used to lookup the receiver corresponding to the channel identifier.
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * Used to convert local event from and to remote event.
     */
    @Requirement
    private EventConverterManager eventConverterManager;

    /**
     * Used to inject event coming from network.
     */
    @Requirement
    private ObservationManager observationManager;

    /**
     * Used to set some extra informations about the current event injected to the local {@link ObservationManager}.
     */
    @Requirement
    private RemoteObservationManagerContext remoteEventManagerContext;

    /**
     * Used to initialize ExecutionContext for the remote->local thread.
     */
    @Requirement
    private Execution execution;

    /**
     * Used to initialize ExecutionContext for the remote->local thread.
     */
    @Requirement
    private ExecutionContextManager executionContextManager;

    /**
     * The container used to access configuration files.
     */
    @Requirement
    private Container container;

    /**
     * The network channels.
     */
    private Map<String, JChannel> channels = new HashMap<String, JChannel>();

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.phase.Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        // start configured channels
        for (String channelId : this.configuration.getChannels()) {
            try {
                startChannel(channelId);
            } catch (RemoteEventException e) {
                getLogger().error("Failed to start channel [" + channelId + "]", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.remote.RemoteObservationManager#notify(org.xwiki.observation.remote.LocalEventData)
     */
    public void notify(LocalEventData localEvent)
    {
        if (this.remoteEventManagerContext.isRemoteState()) {
            // the event is a remote event
            return;
        }

        // Convert local->remote
        RemoteEventData remoteEvent = this.eventConverterManager.createRemoteEventData(localEvent);

        // if remote event data is not filled it mean the message should not be sent to the network
        if (remoteEvent != null) {
            Message message = new Message(null, null, remoteEvent);

            // Send message to jgroups channels
            for (Map.Entry<String, JChannel> entry : this.channels.entrySet()) {
                try {
                    entry.getValue().send(message);
                } catch (Exception e) {
                    getLogger().error("Fail to send message to the channel [" + entry.getKey() + "]", e);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.remote.RemoteObservationManager#notify(org.xwiki.observation.remote.RemoteEventData)
     */
    public void notify(RemoteEventData remoteEvent)
    {
        // Make sure the Execution context is properly initialized
        initiContext();

        LocalEventData localEvent = this.eventConverterManager.createLocalEventData(remoteEvent);

        // send event
        if (localEvent != null) {
            // indicate all the following events are remote events
            this.remoteEventManagerContext.pushRemoteState();

            this.observationManager.notify(localEvent.getEvent(), localEvent.getSource(), localEvent.getData());

            // indicate all the following events are remote events
            this.remoteEventManagerContext.popRemoteState();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.remote.RemoteObservationManager#startChannel(java.lang.String)
     */
    public void startChannel(String channelId) throws RemoteEventException
    {
        if (this.channels.containsKey(channelId)) {
            throw new RemoteEventException(MessageFormat.format("Channel [{0}] already started", channelId));
        }

        JChannel channel;
        try {
            channel = createChannel(channelId);
            channel.connect("event");
            this.channels.put(channelId, channel);
        } catch (Exception e) {
            throw new RemoteEventException("Failed to create channel [" + channelId + "]", e);
        }

        getLogger().info(MessageFormat.format("Channel [{0}] started", channelId));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.remote.RemoteObservationManager#stopChannel(java.lang.String)
     */
    public void stopChannel(String channelId) throws RemoteEventException
    {
        JChannel channel = this.channels.get(channelId);

        if (channel == null) {
            throw new RemoteEventException(MessageFormat.format("Channel [{0}] is not started", channelId));
        }

        channel.close();

        this.channels.remove(channelId);

        getLogger().info(MessageFormat.format("Channel [{0}] stoped", channelId));
    }

    /**
     * Create a new channel.
     * 
     * @param channelId the identifier of the channel to create
     * @return the new channel
     * @throws ComponentLookupException failed to get default {@link JGroupsReceiver}
     * @throws ChannelException failed to create channel
     */
    private JChannel createChannel(String channelId) throws ComponentLookupException, ChannelException
    {
        // load configuration
        ProtocolStackConfigurator channelConf;
        try {
            channelConf = loadChannelConfiguration(channelId);
        } catch (IOException e) {
            throw new ChannelException("Failed to load configuration for the channel [" + channelId + "]", e);
        }

        // get Receiver
        Receiver channelReceiver;
        try {
            channelReceiver = this.componentManager.lookup(JGroupsReceiver.class, channelId);
        } catch (ComponentLookupException e) {
            channelReceiver = this.componentManager.lookup(JGroupsReceiver.class);
        }

        // create channel
        JChannel channel = new JChannel(channelConf);
        channel.setReceiver(channelReceiver);

        return channel;
    }

    /**
     * Load channel configuration.
     * 
     * @param channelId the identifier of the channel
     * @return the channel configuration
     * @throws IOException failed to load configuration file
     * @throws ChannelException failed to creation channel configuration
     */
    private ProtocolStackConfigurator loadChannelConfiguration(String channelId) throws IOException, ChannelException
    {
        ProtocolStackConfigurator configurator = null;

        String path = "/WEB-INF/" + CONFIGURATION_PATH + channelId + ".xml";

        InputStream is = this.container.getApplicationContext().getResourceAsStream(path);

        if (is != null) {
            configurator = XmlConfigurator.getInstance(is);
        } else {
            getLogger().warn(
                "Can't find a configuration for channel [" + channelId + "] at [" + path + "]. Using "
                    + JChannel.DEFAULT_PROTOCOL_STACK + " JGRoups default configuration.");

            configurator = ConfiguratorFactory.getStackConfigurator(JChannel.DEFAULT_PROTOCOL_STACK);
        }

        return configurator;
    }

    /**
     * Make sure an ExecutionContext initialized for remote->local thread.
     */
    private void initiContext()
    {
        if (this.execution.getContext() == null) {
            ExecutionContext context = new ExecutionContext();

            try {
                this.executionContextManager.initialize(context);
            } catch (ExecutionContextException e) {
                getLogger().error("failed to initialize execution context", e);
            }

            this.execution.setContext(context);
        }
    }
}
