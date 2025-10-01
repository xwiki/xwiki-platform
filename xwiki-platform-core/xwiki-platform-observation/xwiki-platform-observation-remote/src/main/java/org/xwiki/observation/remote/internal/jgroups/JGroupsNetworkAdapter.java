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

import java.text.MessageFormat;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import javax.inject.Provider;

import org.jgroups.Message;
import org.jgroups.ObjectMessage;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.observation.remote.NetworkAdapter;
import org.xwiki.observation.remote.RemoteEventData;
import org.xwiki.observation.remote.RemoteEventException;

/**
 * JGroups based implementation of {@link NetworkAdapter}.
 *
 * @version $Id$
 * @since 2.0RC1
 */
@Component
@Named("jgroups")
@Singleton
public class JGroupsNetworkAdapter implements NetworkAdapter, Disposable
{
    @Inject
    private JGroupsNetworkChannels channels;

    @Inject
    private Provider<JGroupsNetworkChannel> channelProvider;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    @Override
    public void send(RemoteEventData remoteEvent)
    {
        this.logger.debug("Send JGroups remote event [{}]", remoteEvent.toString());

        // Send the message to the whole group
        Message message = new ObjectMessage(null, remoteEvent);

        // Send message to JGroups channels
        for (Map.Entry<String, JGroupsNetworkChannel> entry : this.channels.getChannels().entrySet()) {
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
        if (this.channels.getChannels().containsKey(channelId)) {
            throw new RemoteEventException(MessageFormat.format("Channel [{0}] already started", channelId));
        }

        try {
            // Create and start the channel
            JGroupsNetworkChannel channel = this.channelProvider.get();
            channel.start(channelId);
        } catch (Exception e) {
            throw new RemoteEventException("Failed to create channel [" + channelId + "]", e);
        }

        this.logger.info("Channel [{}] started", channelId);
    }

    private void stopChannel(JGroupsNetworkChannel channel)
    {
        channel.stop();

        this.logger.info("Channel [{}] stopped", channel.getId());
    }

    @Override
    public void stopChannel(String channelId) throws RemoteEventException
    {
        JGroupsNetworkChannel channel = this.channels.getChannels().remove(channelId);

        if (channel == null) {
            throw new RemoteEventException(MessageFormat.format("Channel [{0}] is not started", channelId));
        }

        stopChannel(channel);
    }

    private void internalStopAll()
    {
        for (Map.Entry<String, JGroupsNetworkChannel> channelEntry : this.channels.getChannels().entrySet()) {
            stopChannel(channelEntry.getValue());
        }

        this.channels.getChannels().clear();

        this.logger.info("All channels stopped");
    }

    @Override
    public void stopAllChannels() throws RemoteEventException
    {
        internalStopAll();
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        internalStopAll();
    }
}
