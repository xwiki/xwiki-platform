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
package org.xwiki.netflux.internal.event;

import java.io.Serializable;

import org.xwiki.netflux.internal.EntityChange;
import org.xwiki.observation.event.Event;

/**
 * An event triggered when last author with script right changed for a channel.
 * <p>
 * The event also send the following parameters:
 * </p>
 * <ul>
 * <li>source: the {@link EntityChange} describing the change.</li>
 * </ul>
 * 
 * @version $Id$
 * @since 17.10.0
 */
public class EntityChannelScriptAuthorChangeEvent implements Event, Serializable
{
    private static final long serialVersionUID = 1L;

    private final String channel;

    /**
     * Listener to all {@link EntityChannelScriptAuthorChangeEvent} events.
     */
    public EntityChannelScriptAuthorChangeEvent()
    {
        this(null);
    }

    /**
     * @param channel the identifier of the channel
     */
    public EntityChannelScriptAuthorChangeEvent(String channel)
    {
        this.channel = channel;
    }

    /**
     * @return the identifier of the channel
     */
    public String getChannel()
    {
        return this.channel;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent instanceof EntityChannelScriptAuthorChangeEvent;
    }
}
