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
 *
 */
package org.xwiki.observation.remote.internal.jgroups;

import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.View;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.observation.remote.RemoteEventData;
import org.xwiki.observation.remote.RemoteObservationManager;
import org.xwiki.observation.remote.jgroups.JGroupsReceiver;

/**
 * Default implementation of JGroupsReceiver. Receive remote event and sent them as is to
 * {@link RemoteObservationManager} to be converted and injected as local events.
 * 
 * @version $Id$
 * @since 2.0M3
 */
@Component
public class DefaultJGroupsReceiver extends AbstractLogEnabled implements JGroupsReceiver
{
    /**
     * Used to send events for conversion.
     */
    private RemoteObservationManager remoteObservationManager;

    /**
     * Used to lookup for {@link RemoteObservationManager}. To avoid cross dependency issues.
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * The address of the member.
     */
    private Address address;

    /**
     * @return the RemoteObservationManager
     */
    public RemoteObservationManager getRemoteObservationManager()
    {
        if (this.remoteObservationManager == null) {
            try {
                this.remoteObservationManager = componentManager.lookup(RemoteObservationManager.class);
            } catch (ComponentLookupException e) {
                getLogger().error("Failed to lookup RemoteObservationManager componenent.", e);
            }
        }

        return remoteObservationManager;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jgroups.MessageListener#getState()
     */
    public byte[] getState()
    {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jgroups.MessageListener#receive(org.jgroups.Message)
     */
    public void receive(Message msg)
    {
        if (this.address == null || !this.address.equals(msg.getSrc())) {
            RemoteEventData remoteEvent = (RemoteEventData) msg.getObject();

            getLogger().debug("Received JGroups remote event [" + remoteEvent + "]");

            getRemoteObservationManager().notify(remoteEvent);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jgroups.MessageListener#setState(byte[])
     */
    public void setState(byte[] state)
    {

    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jgroups.MembershipListener#block()
     */
    public void block()
    {

    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jgroups.MembershipListener#suspect(org.jgroups.Address)
     */
    public void suspect(Address suspectedMbr)
    {

    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jgroups.MembershipListener#viewAccepted(org.jgroups.View)
     */
    public void viewAccepted(View newView)
    {

    }
}
