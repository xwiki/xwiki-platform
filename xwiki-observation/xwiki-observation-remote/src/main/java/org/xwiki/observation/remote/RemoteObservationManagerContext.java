package org.xwiki.observation.remote;

import org.xwiki.component.annotation.ComponentRole;

/**
 * Provide informations about the event in the current thread.
 * 
 * @version $Id$
 * @since 2.0M3
 */
@ComponentRole
public interface RemoteObservationManagerContext
{
    /**
     * Set the remote state to true.
     */
    void pushRemoteState();

    /**
     * Set the remote state to false.
     */
    void popRemoteState();

    /**
     * @return indicate if the event in the current thread is a remote event
     */
    boolean isRemoteState();
}
