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
package org.xwiki.observation.remote.internal.jgroups;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.management.MBeanServer;

import org.jgroups.BytesMessage;
import org.jgroups.Global;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.conf.ConfiguratorFactory;
import org.jgroups.conf.ProtocolStackConfigurator;
import org.jgroups.conf.XmlConfigurator;
import org.jgroups.jmx.JmxConfigurator;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.environment.Environment;
import org.xwiki.observation.remote.NetworkAdapter;
import org.xwiki.observation.remote.RemoteEventData;
import org.xwiki.observation.remote.RemoteEventException;
import org.xwiki.observation.remote.jgroups.JGroupsReceiver;

/**
 * JGroups based implementation of {@link NetworkAdapter}.
 *
 * @version $Id$
 * @since 2.0RC1
 */
@Component
@Named("jgroups")
@Singleton
public class JGroupsNetworkAdapter implements NetworkAdapter
{
    /**
     * Relative path where to find jgroups channels configurations.
     */
    public static final String CONFIGURATION_PATH = "observation/remote/jgroups/";

    /**
     * Used to lookup the receiver corresponding to the channel identifier.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * The network channels.
     */
    private Map<String, JChannel> channels = new ConcurrentHashMap<>();

    @Override
    public void send(RemoteEventData remoteEvent)
    {
        this.logger.debug("Send JGroups remote event [{}]", remoteEvent.toString());

        // Send the message to the whole group
        Message message = new BytesMessage(null,  remoteEvent);

        // Send message to JGroups channels
        for (Map.Entry<String, JChannel> entry : this.channels.entrySet()) {
            try {
                entry.getValue().send(message);
            } catch (Exception e) {
                this.logger.error("Failed to send message [{}] to the channel [{}]", remoteEvent.toString(),
                    entry.getKey(), e);
            }
        }
    }

    @Override
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

        // Register the channel against the JMX Server
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            JmxConfigurator.registerChannel(channel, mbs, channel.getClusterName());
        } catch (Exception e) {
            this.logger.warn("Failed to register channel [" + channelId + "] against the JMX Server", e);
        }

        this.logger.info("Channel [{}] started", channelId);
    }

    @Override
    public void stopChannel(String channelId) throws RemoteEventException
    {
        JChannel channel = this.channels.get(channelId);

        if (channel == null) {
            throw new RemoteEventException(MessageFormat.format("Channel [{0}] is not started", channelId));
        }

        channel.close();

        this.channels.remove(channelId);

        // Unregister the channel from the JMX Server
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            JmxConfigurator.unregister(channel, mbs, channel.getClusterName());
        } catch (Exception e) {
            this.logger.warn("Failed to unregister channel [{}] from the JMX Server", channelId, e);
        }

        this.logger.info("Channel [{}] stopped", channelId);
    }

    /**
     * Create a new channel.
     *
     * @param channelId the identifier of the channel to create
     * @return the new channel
     * @throws Exception failed to create new channel
     */
    private JChannel createChannel(String channelId) throws Exception
    {
        // load configuration
        ProtocolStackConfigurator channelConf = loadChannelConfiguration(channelId);

        // get Receiver
        JGroupsReceiver channelReceiver;
        try {
            channelReceiver = this.componentManager.getInstance(JGroupsReceiver.class, channelId);
        } catch (ComponentLookupException e) {
            channelReceiver = this.componentManager.getInstance(JGroupsReceiver.class);
        }

        // create channel
        JChannel channel = new JChannel(channelConf);

        channel.setReceiver(channelReceiver);
        channel.setDiscardOwnMessages(true);

        return channel;
    }

    /**
     * Load channel configuration.
     *
     * @param channelId the identifier of the channel
     * @return the channel configuration
     * @throws IOException failed to load configuration file
     */
    private ProtocolStackConfigurator loadChannelConfiguration(String channelId) throws IOException
    {
        try (InputStream stream = getChannelConfiguration(channelId)) {
            return XmlConfigurator.getInstance(stream);
        }
    }

    private InputStream getChannelConfiguration(String channelId) throws IOException
    {
        String channelFile = channelId + ".xml";
        String path = "/WEB-INF/" + CONFIGURATION_PATH + channelFile;

        InputStream is = null;
        try {
            Environment environment = this.componentManager.getInstance(Environment.class);
            is = environment.getResourceAsStream(path);
        } catch (ComponentLookupException e) {
            // Environment not found, continue by fallbacking on JGroups's standard configuration.
            this.logger.debug("Failed to lookup the Environment component.", e);
        }

        if (is == null) {
            // Fallback on JGroups standard configuration locations
            is = ConfiguratorFactory.getConfigStream(channelFile);

            if (is == null && !Global.DEFAULT_PROTOCOL_STACK.equals(channelFile)) {
                // Fallback on default JGroups configuration
                is = ConfiguratorFactory.getConfigStream(Global.DEFAULT_PROTOCOL_STACK);
            }
        }

        return is;
    }

    @Override
    public void stopAllChannels() throws RemoteEventException
    {
        for (Map.Entry<String, JChannel> channelEntry : this.channels.entrySet()) {
            channelEntry.getValue().close();
        }

        this.channels.clear();

        this.logger.info("All channels stopped");
    }
}
