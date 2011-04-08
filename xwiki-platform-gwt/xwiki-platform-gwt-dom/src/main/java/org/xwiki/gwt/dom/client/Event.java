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

/**
 * DOM Event.
 * 
 * @version $Id$
 */
public class Event extends com.google.gwt.user.client.Event
{
    /**
     * Default constructor. Needs to be protected because all instances are created from JavaScript.
     */
    protected Event()
    {
        super();
    }

    /**
     * @return true if this event was cancelled by calling {@link #xPreventDefault()}.
     */
    public final native boolean isCancelled()
    /*-{
        return !!this.__cancelled;
    }-*/;

    /**
     * Sets this event's cancelled state.
     * 
     * @param cancelled specifies if this event should be cancelled or not.
     */
    protected final native void setCancelled(boolean cancelled)
    /*-{
        this.__cancelled = cancelled;
    }-*/;

    /**
     * Cancel this event.
     */
    public final void xPreventDefault()
    {
        preventDefault();
        setCancelled(true);
    }
}
