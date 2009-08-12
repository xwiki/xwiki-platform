package org.xwiki.observation.remote;

import org.xwiki.component.annotation.ComponentRole;

/**
 * Provide apis to manage the event network interface.
 * 
 * @version $Id$
 * @since 2.0M3
 */
@ComponentRole
public interface RemoteObservationManager
{
    /**
     * Send a event in the different network channels.
     * <p>
     * This method is not supposed to be used directly for a new event unless the user specifically want to bypass or
     * emulate {@link org.xwiki.observation.ObservationManager}.
     * 
     * @param event the event
     */
    void notify(LocalEventData event);

    /**
     * Inject a remote event in the local {@link org.xwiki.observation.ObservationManager}.
     * <p>
     * This method is not supposed to be used directly for a new event unless the user specifically want to bypass or
     * emulate network.
     * 
     * @param event the event
     */
    void notify(RemoteEventData event);

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
}
