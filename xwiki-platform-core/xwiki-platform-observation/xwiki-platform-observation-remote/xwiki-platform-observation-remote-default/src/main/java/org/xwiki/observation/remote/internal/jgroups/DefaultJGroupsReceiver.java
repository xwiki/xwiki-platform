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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jgroups.BytesMessage;
import org.jgroups.Message;
import org.jgroups.blocks.cs.ReceiverAdapter;
import org.slf4j.Logger;
import org.xwiki.classloader.ClassLoaderManager;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.observation.remote.RemoteEventData;
import org.xwiki.observation.remote.RemoteObservationManager;
import org.xwiki.observation.remote.jgroups.JGroupsReceiver;

/**
 * Default implementation of JGroupsReceiver. Receive remote events and send them as is to
 * {@link RemoteObservationManager} to be converted and injected as local events.
 *
 * @version $Id$
 * @since 2.0M3
 */
@Component
@Singleton
public class DefaultJGroupsReceiver extends ReceiverAdapter implements JGroupsReceiver
{
    /**
     * Used to send events for conversion.
     */
    private RemoteObservationManager remoteObservationManager;

    /**
     * Used to lookup {@link RemoteObservationManager}. To avoid cross-dependency issues.
     */
    @Inject
    private ComponentManager componentManager;

    @Inject
    private ClassLoaderManager classLoaderManager;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * @return the RemoteObservationManager
     */
    public RemoteObservationManager getRemoteObservationManager()
    {
        if (this.remoteObservationManager == null) {
            try {
                this.remoteObservationManager = this.componentManager.getInstance(RemoteObservationManager.class);
            } catch (ComponentLookupException e) {
                this.logger.error("Failed to lookup the Remote Observation Manager.", e);
            }
        }

        return this.remoteObservationManager;
    }

    @Override
    public void receive(Message msg)
    {
        if (msg instanceof BytesMessage) {
            RemoteEventData remoteEvent = (RemoteEventData) ((BytesMessage) msg)
                .getObject(this.classLoaderManager.getURLClassLoader(null, false));

            this.logger.debug("Received JGroups remote event [{}]", remoteEvent);

            getRemoteObservationManager().notify(remoteEvent);
        }
    }
}
