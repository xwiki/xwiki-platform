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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.management.MBeanServer;

import org.jgroups.Address;
import org.jgroups.Global;
import org.jgroups.JChannel;
import org.jgroups.blocks.ReplicatedHashMap;
import org.jgroups.conf.ConfiguratorFactory;
import org.jgroups.conf.ProtocolStackConfigurator;
import org.jgroups.conf.XmlConfigurator;
import org.jgroups.jmx.JmxConfigurator;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.environment.Environment;
import org.xwiki.observation.remote.NetworkChannel;
import org.xwiki.observation.remote.NetworkMember;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;
import org.xwiki.observation.remote.jgroups.JGroupsReceiver;

/**
 * JGroups implementation of {@link NetworkChannel}.
 * 
 * @version $Id$
 * @since 17.8.0RC1
 */
@Component(roles = JGroupsNetworkChannel.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class JGroupsNetworkChannel implements NetworkChannel
{
    /**
     * Relative path where to find JGroups channels configurations.
     */
    public static final String CONFIGURATION_PATH = "observation/remote/jgroups/";

    @Inject
    private RemoteObservationManagerConfiguration configuration;

    /**
     * Used to lookup the receiver corresponding to the channel identifier.
     */
    @Inject
    private ComponentManager componentManager;

    @Inject
    private JGroupsNetworkChannels channels;

    @Inject
    private Logger logger;

    private String id;

    private JChannel jchannel;

    private ReplicatedHashMap<String, String> membersIdMap;

    private Map<String, JGroupsNetworkMember> members;

    private JGroupsNetworkMember leader;

    /**
     * Initialize and start the channel.
     * 
     * @param id the identifier of the channel
     * @throws Exception when failing to start the channel
     */
    @SuppressWarnings("resource")
    public void start(String id) throws Exception
    {
        this.id = id;

        // Load configuration
        ProtocolStackConfigurator channelConf = loadChannelConfiguration(this.id);

        // Get the Receiver
        JGroupsReceiver channelReceiver;
        try {
            channelReceiver = this.componentManager.getInstance(JGroupsReceiver.class, this.id);
        } catch (ComponentLookupException e) {
            channelReceiver = this.componentManager.getInstance(JGroupsReceiver.class);
        }

        // Create tyhe channel
        this.jchannel = new JChannel(channelConf);
        this.jchannel.setReceiver(channelReceiver);
        this.jchannel.setDiscardOwnMessages(true);

        // Register the channel
        this.channels.getChannels().put(id, this);

        // Register the channel against the JMX Server
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            JmxConfigurator.registerChannel(this.jchannel, mbs, this.jchannel.getClusterName());
        } catch (Exception e) {
            this.logger.warn("Failed to register channel [{}] against the JMX Server", this.id, e);
        }

        // Start the channel
        this.jchannel.connect("event");

        this.membersIdMap = new ReplicatedHashMap<>(this.jchannel);
        this.membersIdMap.put(this.jchannel.getAddress().toString(), this.configuration.getId());
    }

    /**
     * Close the channel.
     */
    public void stop()
    {
        // Chose the channel
        this.jchannel.close();

        // Unregister the channel from the JMX Server
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            JmxConfigurator.unregister(this.jchannel, mbs, this.jchannel.getClusterName());
        } catch (Exception e) {
            this.logger.warn("Failed to unregister channel [{}] from the JMX Server", this.id, e);
        }
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

    /**
     * @return the identifier of the channel
     */
    @Override
    public String getId()
    {
        return this.id;
    }

    /**
     * @return the JGroups channel
     */
    public JChannel getJChannel()
    {
        return this.jchannel;
    }

    /**
     * Called when the view of the JGroups channel has been modified.
     */
    public void onViewChanged()
    {
        // Get the current JGroups members
        List<Address> jmembers = this.jchannel.getView().getMembers();

        // Create the member's map
        Map<String, JGroupsNetworkMember> newMembers = new HashMap<>();
        for (Address jmember : jmembers) {
            String remoteId = this.membersIdMap.get(jmember.toString());
            JGroupsNetworkMember member = new JGroupsNetworkMember(remoteId, jmember);

            newMembers.put(remoteId, member);

            if (this.leader == null) {
                // Get the leader
                this.leader = member;
            }
        }
        this.members = Collections.unmodifiableMap(newMembers);
    }

    @Override
    public Collection<NetworkMember> getMembers()
    {
        return (Collection) this.members.values();
    }

    @Override
    public NetworkMember getLeader()
    {
        return this.leader;
    }
}
