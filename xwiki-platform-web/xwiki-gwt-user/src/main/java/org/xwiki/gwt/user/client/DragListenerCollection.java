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
package org.xwiki.gwt.user.client;

import java.util.ArrayList;

import com.google.gwt.user.client.ui.Widget;

/**
 * A collection of drag listeners.
 * 
 * @version $Id$
 */
public class DragListenerCollection extends ArrayList<DragListener>
{
    /**
     * Field required by all {@link java.io.Serializable} classes.
     */
    private static final long serialVersionUID = -5010881321934321154L;

    /**
     * Notifies all the listeners in this collection of the DragStart event.
     * 
     * @param sender The object which generated the event.
     * @param x the horizontal coordinate of the mouse when the event was fired.
     * @param y the vertical coordinate of the mouse when the event was fired.
     */
    public void fireDragStart(Widget sender, int x, int y)
    {
        for (DragListener listener : this) {
            listener.onDragStart(sender, x, y);
        }
    }

    /**
     * Notifies all the listeners in this collection of the Drag event.
     * 
     * @param sender The object which generated the event.
     * @param x the horizontal coordinate of the mouse when the event was fired.
     * @param y the vertical coordinate of the mouse when the event was fired.
     */
    public void fireDrag(Widget sender, int x, int y)
    {
        for (DragListener listener : this) {
            listener.onDrag(sender, x, y);
        }
    }

    /**
     * Notifies all the listeners in this collection of the DragEnd event.
     * 
     * @param sender The object which generated the event.
     * @param x the horizontal coordinate of the mouse when the event was fired.
     * @param y the vertical coordinate of the mouse when the event was fired.
     */
    public void fireDragEnd(Widget sender, int x, int y)
    {
        for (DragListener listener : this) {
            listener.onDragEnd(sender, x, y);
        }
    }
}
