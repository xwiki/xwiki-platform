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
package org.xwiki.observation.remote.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
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
@Singleton
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
    @Inject
    private Execution execution;

    @Override
    public boolean isRemoteState()
    {
        ExecutionContext context = this.execution.getContext();

        return context != null && context.getProperty(REMOTESTATE) == Boolean.TRUE;
    }

    @Override
    public void pushRemoteState()
    {
        this.execution.getContext().setProperty(REMOTESTATE, Boolean.TRUE);
    }

    @Override
    public void popRemoteState()
    {
        this.execution.getContext().setProperty(REMOTESTATE, Boolean.FALSE);
    }
}
