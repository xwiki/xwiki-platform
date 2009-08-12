package org.xwiki.observation.remote.internal;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.observation.remote.RemoteObservationManagerContext;

/**
 * Manager context properties specific to remote events.
 * 
 * @version $Id$
 * @since 2.0M3
 */
@Component
public class DefaultRemoteObservationManagerContext implements RemoteObservationManagerContext
{
    /**
     * The name of the properties containing the sate indicating if the current generated events are remote of local
     * events.
     */
    private static final String REMOTESTATE = "observation.remote.remotestate";

    /**
     * Used to store remote observation manager context properties.
     */
    @Requirement
    private Execution execution;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.remote.RemoteObservationManagerContext#isRemoteState()
     */
    public boolean isRemoteState()
    {
        ExecutionContext context = this.execution.getContext();

        return context != null && context.getProperty(REMOTESTATE) == Boolean.TRUE;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.remote.RemoteObservationManagerContext#pushRemoteState()
     */
    public void pushRemoteState()
    {
        this.execution.getContext().setProperty(REMOTESTATE, Boolean.TRUE);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.remote.RemoteObservationManagerContext#popRemoteState()
     */
    public void popRemoteState()
    {
        this.execution.getContext().setProperty(REMOTESTATE, Boolean.FALSE);
    }
}
