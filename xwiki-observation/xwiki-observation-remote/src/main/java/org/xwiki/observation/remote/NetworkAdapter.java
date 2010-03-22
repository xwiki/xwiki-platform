package org.xwiki.observation.remote;

import org.xwiki.component.annotation.ComponentRole;

/**
 * Handle all the actual communication with the network.
 * <p>
 * It's the entry point of the chosen implementation for the actual event distribution.
 * 
 * @version $Id$
 * @since 2.0RC1
 */
@ComponentRole
public interface NetworkAdapter
{
    /**
     * Send serializable event to the network depending of the implementation.
     * 
     * @param remoteEvent the serializable event to send
     */
    void send(RemoteEventData remoteEvent);

    /**
     * Stop a running channel.
     * 
     * @param channelId the identifier of the channel to stop
     * @throws RemoteEventException error when trying to stop a running channel
     */
    void stopChannel(String channelId) throws RemoteEventException;

    /**
     * Start a channel.
     * 
     * @param channelId the identifier of the channel to start
     * @throws RemoteEventException error when trying to start a channel
     */
    void startChannel(String channelId) throws RemoteEventException;

    /**
     * Stop all running channels.
     * 
     * @throws RemoteEventException error when trying to stop a running channel
     * @since 2.3M1
     */
    void stopAllChannels() throws RemoteEventException;
}
