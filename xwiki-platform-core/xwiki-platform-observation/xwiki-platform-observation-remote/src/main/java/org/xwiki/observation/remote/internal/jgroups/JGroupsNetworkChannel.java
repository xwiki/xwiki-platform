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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.management.MBeanServer;

import org.jgroups.Address;
import org.jgroups.Global;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ObjectMessage;
import org.jgroups.Receiver;
import org.jgroups.View;
import org.jgroups.blocks.MessageDispatcher;
import org.jgroups.blocks.RequestHandler;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.conf.ConfiguratorFactory;
import org.jgroups.conf.ProtocolStackConfigurator;
import org.jgroups.conf.XmlConfigurator;
import org.jgroups.jmx.JmxConfigurator;
import org.jgroups.util.Rsp;
import org.jgroups.util.RspList;
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
 * @since 17.9.0RC1
 */
@Component(roles = JGroupsNetworkChannel.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
// It's unfortunately not so easy to reduce the class fan out complexity (which is currently just above the accepted
// threshold)
@SuppressWarnings("checkstyle:ClassFanOutComplexity")
public class JGroupsNetworkChannel implements NetworkChannel, Receiver
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
    private JGroupsNetworkChannelNotifier notifier;

    @Inject
    private JGroupsReceiver channelReceiver;

    @Inject
    private Logger logger;

    private String id;

    private JChannel jchannel;

    private MessageDispatcher memberIdDispatcher;

    // <jid, xid>
    private Map<String, String> membersIdMap;

    // <xid, M>
    private Map<String, JGroupsNetworkMember> members;

    private JGroupsNetworkMember leader;

    private final class JGroupsMemberHandler implements RequestHandler
    {
        @Override
        public Object handle(Message msg) throws Exception
        {
            Object obj = msg.getObject();

            // Member id update
            if (obj instanceof String srcId) {
                // Add the id to the mapping
                membersIdMap.put(msg.getSrc().toString(), srcId);

                // Re-evaluate members
                updateMembers(true);

                // Return the id of the current member
                return configuration.getId();
            }

            // Async messages
            if (obj instanceof Message asyncMessage) {
                channelReceiver.receive(asyncMessage);
            }

            return null;
        }
    }

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
        try {
            this.channelReceiver = this.componentManager.getInstance(JGroupsReceiver.class, this.id);
        } catch (ComponentLookupException e) {
            this.channelReceiver = this.componentManager.getInstance(JGroupsReceiver.class);
        }

        // Create the channel
        this.jchannel = new JChannel(channelConf);
        this.jchannel.setDiscardOwnMessages(true);

        // Register the channel
        this.channels.getChannels().put(getId(), this);

        // Register the member handler dispatcher
        this.memberIdDispatcher = new MessageDispatcher(this.jchannel, new JGroupsMemberHandler());
        this.memberIdDispatcher.setReceiver(this);

        // Start the channel
        this.jchannel.connect("event");

        String currentMemberId = this.configuration.getId();

        // Register the channel against the JMX Server
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            JmxConfigurator.registerChannel(this.jchannel, mbs, this.jchannel.getClusterName() + '-' + currentMemberId);
        } catch (Exception e) {
            this.logger.warn("Failed to register channel [" + getId() + "] against the JMX Server", e);
        }

        // Create the mapping between JGroups member address and XWiki observation id
        this.membersIdMap = new ConcurrentHashMap<>();
        // Add current instance to the maping
        // Need to be done after starting the channel to know the address
        this.membersIdMap.put(this.jchannel.getAddress().toString(), currentMemberId);
        this.members =
            Map.of(currentMemberId, new JGroupsNetworkMember(this, currentMemberId, this.jchannel.getAddress()));

        // Send the message to other members and wait for their ids in response (wait for 1min max).
        RspList<String> responses = this.memberIdDispatcher.castMessage(null, new ObjectMessage(null, currentMemberId),
            new RequestOptions(ResponseMode.GET_ALL, 60000));
        for (Map.Entry<Address, Rsp<String>> response : responses.entrySet()) {
            this.membersIdMap.put(response.getKey().toString(), response.getValue().getValue());
        }

        // Initialize the channel
        // It's actually the second time that #updateMembers is called, but the first time we did not had enough
        // information for it to do much
        updateMembers(false);
    }

    /**
     * Close the channel.
     */
    public void stop()
    {
        String currentMemberId = this.configuration.getId();

        // Unregister the channel from the JMX Server
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            JmxConfigurator.unregisterChannel(this.jchannel, mbs,
                this.jchannel.getClusterName() + '-' + currentMemberId);
        } catch (Exception e) {
            this.logger.warn("Failed to unregister channel [{}] from the JMX Server", this.id, e);
        }

        // Unregister the channel
        this.channels.getChannels().remove(getId());

        // Close the channel
        this.jchannel.close();
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
     * Asynchronously send a message to all group members.
     * 
     * @param message the message to send
     * @throws Exception when failing to send the message
     */
    public void send(Message message) throws Exception
    {
        this.memberIdDispatcher.castMessage(null, new ObjectMessage(null, message), RequestOptions.ASYNC());
    }

    @Override
    public void viewAccepted(View view)
    {
        if (this.membersIdMap == null) {
            // Not ready
            return;
        }

        // Check if a member has been removed
        for (JGroupsNetworkMember member : this.members.values()) {
            if (!this.jchannel.getView().containsMember(member.getAddress())) {
                // At least one member left, update the members cache
                updateMembers(true);
            }
        }
    }

    private synchronized void updateMembers(boolean notifyChanges)
    {
        if (this.membersIdMap == null) {
            // Not ready
            return;
        }

        // Get the current JGroups members
        List<Address> jmembers = this.jchannel.getView().getMembers();

        // Create the member's map
        Map<String, JGroupsNetworkMember> previousMembers = this.members;
        Map<String, JGroupsNetworkMember> newMembers = new LinkedHashMap<>();
        JGroupsNetworkMember currentLeader = this.leader;
        JGroupsNetworkMember newLeader = null;
        for (Address jmember : jmembers) {
            String remoteId = this.membersIdMap.get(jmember.toString());
            JGroupsNetworkMember member = new JGroupsNetworkMember(this, remoteId, jmember);

            newMembers.put(remoteId, member);

            // Update the leader if it changed
            if (newLeader == null) {
                // Get the leader
                newLeader = member;
            }
        }
        this.leader = newLeader;
        this.members = Collections.unmodifiableMap(newMembers);

        if (notifyChanges) {
            // Notify listeners about changes made
            notifyChanges(currentLeader, previousMembers);
        }
    }

    private void notifyChanges(JGroupsNetworkMember previousLeader, Map<String, JGroupsNetworkMember> previousMembers)
    {
        // Check and notify if the leader changed
        if (previousLeader == null || !previousLeader.getAddress().equals(this.leader.getAddress())) {
            this.notifier.notifyChannelLeaderChanged(this);
        }

        // Check and notify if new member(s) joined
        for (Map.Entry<String, JGroupsNetworkMember> entry : this.members.entrySet()) {
            if (!previousMembers.containsKey(entry.getKey())) {
                this.notifier.notifyChannelMemberJoined(this, entry.getValue());
            }
        }

        // Check and notify if old member(s) left
        for (Map.Entry<String, JGroupsNetworkMember> entry : previousMembers.entrySet()) {
            if (!this.members.containsKey(entry.getKey())) {
                this.notifier.notifychannelMemberLeft(this, previousLeader);
            }
        }
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
