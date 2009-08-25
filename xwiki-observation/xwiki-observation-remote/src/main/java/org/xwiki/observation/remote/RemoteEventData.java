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
package org.xwiki.observation.remote;

import java.io.Serializable;

/**
 * Represents a remote event with all its datas.
 * 
 * @version $Id$
 * @since 2.0M3
 */
public class RemoteEventData implements Serializable
{
    /**
     * The version identifier for this Serializable class. Increment only if the <i>serialized</i> form of the class
     * changes.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The event type.
     */
    private Serializable event;

    /**
     * The event source.
     */
    private Serializable source;

    /**
     * The event data.
     */
    private Serializable data;

    /**
     * @return the event type.
     */
    public Serializable getEvent()
    {
        return this.event;
    }

    /**
     * @param event the event type.
     */
    public void setEvent(Serializable event)
    {
        this.event = event;
    }

    /**
     * @return the event source.
     */
    public Serializable getSource()
    {
        return this.source;
    }

    /**
     * @param source the event source.
     */
    public void setSource(Serializable source)
    {
        this.source = source;
    }

    /**
     * @return the event data.
     */
    public Serializable getData()
    {
        return this.data;
    }

    /**
     * @param data the event data.
     */
    public void setData(Serializable data)
    {
        this.data = data;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return "event: [" + getEvent() + "], source: [" + getSource() + "], data: [" + getData() + "]";
    }
}
