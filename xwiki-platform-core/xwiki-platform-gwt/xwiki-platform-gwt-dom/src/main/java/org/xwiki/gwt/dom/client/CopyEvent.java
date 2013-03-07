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
package org.xwiki.gwt.dom.client;

import com.google.gwt.event.dom.client.DomEvent;

/**
 * Represents a native copy event.
 * 
 * @version $Id$
 * @since 5.0M2
 */
public class CopyEvent extends DomEvent<CopyHandler>
{
    /**
     * Event type for copy events. Represents the meta-data associated with this event.
     */
    private static final Type<CopyHandler> TYPE = new Type<CopyHandler>("copy", new CopyEvent());

    /**
     * Protected constructor, use
     * {@link DomEvent#fireNativeEvent(com.google.gwt.dom.client.NativeEvent, com.google.gwt.event.shared.HasHandlers)}
     * to fire copy events.
     */
    protected CopyEvent()
    {
    }

    /**
     * Gets the event type associated with copy events.
     * 
     * @return the handler type
     */
    public static Type<CopyHandler> getType()
    {
        return TYPE;
    }

    @Override
    public final Type<CopyHandler> getAssociatedType()
    {
        return TYPE;
    }

    @Override
    protected void dispatch(CopyHandler handler)
    {
        handler.onCopy(this);
    }
}
