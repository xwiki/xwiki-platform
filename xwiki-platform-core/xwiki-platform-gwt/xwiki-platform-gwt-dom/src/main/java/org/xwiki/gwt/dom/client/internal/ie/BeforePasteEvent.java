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
package org.xwiki.gwt.dom.client.internal.ie;

import com.google.gwt.event.dom.client.DomEvent;

/**
 * Represents a native BeforePaste event.
 * 
 * @version $Id$
 * @since 5.0M2
 */
public class BeforePasteEvent extends DomEvent<BeforePasteHandler>
{
    /**
     * Event type for BeforePaste events. Represents the meta-data associated with this event.
     */
    private static final Type<BeforePasteHandler> TYPE = new Type<BeforePasteHandler>("beforepaste",
        new BeforePasteEvent());

    /**
     * Protected constructor, use
     * {@link DomEvent#fireNativeEvent(com.google.gwt.dom.client.NativeEvent, com.google.gwt.event.shared.HasHandlers)}
     * to fire BeforePaste events.
     */
    protected BeforePasteEvent()
    {
    }

    /**
     * Gets the event type associated with BeforePaste events.
     * 
     * @return the handler type
     */
    public static Type<BeforePasteHandler> getType()
    {
        return TYPE;
    }

    @Override
    public final Type<BeforePasteHandler> getAssociatedType()
    {
        return TYPE;
    }

    @Override
    protected void dispatch(BeforePasteHandler handler)
    {
        handler.onBeforePaste(this);
    }
}
