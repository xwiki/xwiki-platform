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
 * Represents a native paste event.
 * 
 * @version $Id$
 */
public class PasteEvent extends DomEvent<PasteHandler>
{
    /**
     * Event type for paste events. Represents the meta-data associated with this event.
     */
    private static final Type<PasteHandler> TYPE = new Type<PasteHandler>("paste", new PasteEvent());

    /**
     * Protected constructor, use
     * {@link DomEvent#fireNativeEvent(com.google.gwt.dom.client.NativeEvent, com.google.gwt.event.shared.HasHandlers)}
     * to fire paste events.
     */
    protected PasteEvent()
    {
    }

    /**
     * Gets the event type associated with paste events.
     * 
     * @return the handler type
     */
    public static Type<PasteHandler> getType()
    {
        return TYPE;
    }

    @Override
    public final Type<PasteHandler> getAssociatedType()
    {
        return TYPE;
    }

    @Override
    protected void dispatch(PasteHandler handler)
    {
        handler.onPaste(this);
    }
}
