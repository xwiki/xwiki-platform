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
package org.xwiki.netflux.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import jakarta.inject.Inject;

import org.xwiki.component.annotation.Component;
import org.xwiki.netflux.internal.event.EntityChannelCreatedEvent;
import org.xwiki.observation.event.AbstractRemoteEventListener;
import org.xwiki.observation.event.Event;

/**
 * Add channels created on other cluster members.
 * 
 * @version $Id$
 * @since 17.10.1
 * @since 18.0.0RC1
 */
@Component
@Named(EntityChannelsListener.NAME)
@Singleton
public class EntityChannelsListener extends AbstractRemoteEventListener
{
    /**
     * The name of this event listener (and its component hint at the same time).
     */
    public static final String NAME = "org.xwiki.netflux.internal.EntityChannelsListener";

    @Inject
    private InternalEntityChannelStore channelStore;

    /**
     * Setup the listener.
     */
    public EntityChannelsListener()
    {
        super(NAME, new EntityChannelCreatedEvent());
    }

    @Override
    public void processRemoteEvent(Event event, Object source, Object data)
    {
        if (event instanceof EntityChannelCreatedEvent channelEvent) {
            this.channelStore.createChannel(channelEvent.getChannelKey(), channelEvent.getEntityReference(),
                channelEvent.getPath());
        }
    }
}
