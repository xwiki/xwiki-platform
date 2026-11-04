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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
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

    /**
     * How long to wait for the other members to send their identifier (in milliseconds).
     */
    private static final long ID_TIMEOUT = 60000;

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

    // <JGroups address, XWiki id>
    private Map<Address, String> membersIdMap;

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
                registerMemberId(msg.getSrc(), srcId);

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
            this.logger.warn("Failed to register channel [{}] against the JMX Server", getId(), e);
        }

        // Create the mapping between JGroups member address and XWiki observation id
        Map<Address, String> idMap = new ConcurrentHashMap<>();
        // Add current instance to the mapping
        // Need to be done after starting the channel to know the address
        idMap.put(this.jchannel.getAddress(), currentMemberId);
        this.members =
            Map.of(currentMemberId, new JGroupsNetworkMember(this, currentMemberId, this.jchannel.getAddress()));

        // Send the message to other members and wait for their ids in response (wait for 1min max).
        RspList<String> responses = this.memberIdDispatcher.castMessage(null, new ObjectMessage(null, currentMemberId),
            new RequestOptions(ResponseMode.GET_ALL, ID_TIMEOUT));
        for (Map.Entry<Address, Rsp<String>> response : responses.entrySet()) {
            Rsp<String> rsp = response.getValue();
            // A member which did not answer in time is asked again below
            if (rsp.wasReceived() && rsp.getValue() != null) {
                idMap.put(response.getKey(), rsp.getValue());
            }
        }

        // Expose the mapping only now that it's complete, so that a concurrent view change does not notify listeners
        // about a partial list of members
        this.membersIdMap = idMap;

        // Initialize the members based on the current view
        View view = this.jchannel.getView();
        updateMembers(view, false, false);

        // Ask their id to the members which did not answer or joined in the meantime
        requestMissingIds(view);
    }

    /**
     * Remember the identifier of a member and update the members accordingly.
     * 
     * @param address the JGroups address of the member
     * @param memberId the XWiki identifier of the member
     */
    private void registerMemberId(Address address, String memberId)
    {
        if (memberId.equals(this.configuration.getId()) && !address.equals(this.jchannel.getAddress())) {
            this.logger.warn("The member [{}] of channel [{}] uses the same identifier [{}] as this instance. Each"
                + " member must have its own observation.remote.id (or its own permanent directory, where the generated"
                + " identifier is stored).", address, getId(), memberId);
        }

        Map<Address, String> idMap = this.membersIdMap;
        if (idMap != null) {
            // Add the id to the mapping
            idMap.put(address, memberId);

            // Re-evaluate members
            updateMembers(true);
        }
        // Otherwise the channel is still starting and the id of this member is part of the handshake responses, or
        // will be requested when the handshake is over
    }

    /**
     * Ask their identifier to the members of the view which don't have a known one yet, for example a member which
     * was wrongly suspected (long garbage collection pause, network partition) and comes back with a merge view after
     * its identifier was forgotten. Both sides learn the identifier of the other: the request handler stores ours and
     * the responses give us theirs.
     * 
     * @param view the view to check
     */
    private void requestMissingIds(View view)
    {
        Map<Address, String> idMap = this.membersIdMap;
        if (idMap == null) {
            // Not ready
            return;
        }

        List<Address> unknownMembers = new ArrayList<>();
        for (Address jmember : view.getMembers()) {
            if (!idMap.containsKey(jmember)) {
                unknownMembers.add(jmember);
            }
        }

        if (!unknownMembers.isEmpty()) {
            try {
                // Don't block the JGroups thread (the responses are received through it): handle them when they arrive
                CompletableFuture<RspList<String>> responses = this.memberIdDispatcher.castMessageWithFuture(
                    unknownMembers, new ObjectMessage(null, this.configuration.getId()),
                    new RequestOptions(ResponseMode.GET_ALL, ID_TIMEOUT));
                responses.thenAccept(this::registerMemberIds);
            } catch (Exception e) {
                this.logger.warn("Failed to request the identifier of the members {} of channel [{}]", unknownMembers,
                    getId(), e);
            }
        }
    }

    private void registerMemberIds(RspList<String> responses)
    {
        for (Map.Entry<Address, Rsp<String>> response : responses.entrySet()) {
            Rsp<String> rsp = response.getValue();
            if (rsp.wasReceived() && rsp.getValue() != null) {
                registerMemberId(response.getKey(), rsp.getValue());
            }
        }
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
        // Update the members based on the view we were given (and not the current channel view, which may already
        // have moved on, in which case we will be called again for it): members which left are removed and members
        // whose id was received before the view (JGroups starts accepting messages from a new member slightly before
        // it exposes the new view) are added
        updateMembers(view, true, true);

        // Ask their id to the members we don't know yet
        requestMissingIds(view);
    }

    /**
     * Update the members based on the current view of the channel, generally because the id of a member was received.
     *
     * @param notifyChanges true if listeners should be notified about the changes
     */
    private void updateMembers(boolean notifyChanges)
    {
        // The current view of the channel might be slightly outdated compared to the ids we already received so
        // we don't want to forget any id here, it will be done when the new view is received
        updateMembers(this.jchannel.getView(), notifyChanges, false);
    }

    /**
     * @param view the view to use to update the members
     * @param notifyChanges true if listeners should be notified about the changes
     * @param forgetLeftMembers true if the ids of the members which are not part of the view should be forgotten
     */
    private synchronized void updateMembers(View view, boolean notifyChanges, boolean forgetLeftMembers)
    {
        if (this.membersIdMap == null) {
            // Not ready
            return;
        }

        // Get the current JGroups members
        List<Address> jmembers = view.getMembers();

        // Create the member's map
        Map<String, JGroupsNetworkMember> previousMembers = this.members;
        Map<String, JGroupsNetworkMember> newMembers = new LinkedHashMap<>();
        JGroupsNetworkMember currentLeader = this.leader;
        JGroupsNetworkMember newLeader = null;
        for (Address jmember : jmembers) {
            String remoteId = this.membersIdMap.get(jmember);
            JGroupsNetworkMember member = new JGroupsNetworkMember(this, remoteId, jmember);

            // Every member must agree on the leader, which is the oldest member of the view (the JGroups coordinator),
            // even before its id is received. The leader is updated (and listeners notified) again when that id is
            // received.
            if (newLeader == null) {
                newLeader = member;
            }

            if (remoteId != null) {
                newMembers.put(remoteId, member);
            }
            // Otherwise the id of this member is not known yet: ignore it for now, it will be added (and listeners
            // notified) when its id is received, instead of producing a spurious member with a null id which would
            // later be reported as having left
        }
        this.leader = newLeader;
        this.members = Collections.unmodifiableMap(newMembers);

        if (notifyChanges) {
            // Notify listeners about changes made
            notifyChanges(currentLeader, previousMembers);
        }

        if (forgetLeftMembers) {
            // Forget the ids of the members which are not part of the view anymore (each JGroups address is unique
            // to a connection so the map would otherwise grow forever as other members restart). Done last so that
            // the ids of the members which left remain available while the changes are computed.
            this.membersIdMap.keySet().retainAll(jmembers);
        }
    }

    private void notifyChanges(JGroupsNetworkMember previousLeader, Map<String, JGroupsNetworkMember> previousMembers)
    {
        // Check and notify if the leader changed, including when the id of the leader is received after it became the
        // leader (both the address and the id are compared)
        if (!Objects.equals(previousLeader, this.leader)) {
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
                this.notifier.notifychannelMemberLeft(this, entry.getValue());
            }
        }
    }

    @Override
    public Collection<NetworkMember> getMembers()
    {
        return (Collection) this.members.values();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The leader is the oldest member of the JGroups view (its coordinator). Its identifier is {@code null} (and it's
     * not part of {@link #getMembers()}) while it's not known yet by the current instance.
     */
    @Override
    public NetworkMember getLeader()
    {
        return this.leader;
    }
}
