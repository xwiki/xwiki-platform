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

import com.google.gwt.user.client.ui.Widget;

/**
 * Interface for listening to drag events.
 * 
 * @version $Id$
 */
public interface DragListener
{
    /**
     * Fired on an element when a drag is started. The user is requesting to drag the element the DragStart event is
     * fired at (the sender of the notification). During this event, a listener would set information such the drag data
     * and image to be associated with the drag.
     * 
     * @param sender The object the user is requesting to drag.
     * @param x the horizontal coordinate of the mouse when the event was fired.
     * @param y the vertical coordinate of the mouse when the event was fired.
     */
    void onDragStart(Widget sender, int x, int y);

    /**
     * This event is fired at the source of the drag, that is, the element where DragStart was fired, during the drag
     * operation.
     * 
     * @param sender The object being dragged.
     * @param x the horizontal coordinate of the mouse when the event was fired.
     * @param y the vertical coordinate of the mouse when the event was fired.
     */
    void onDrag(Widget sender, int x, int y);

    /**
     * The source of the drag will receive a DragEnd event when the drag operation is complete, whether it was
     * successful or not.
     * 
     * @param sender The object the the user has stopped dragging.
     * @param x the horizontal coordinate of the mouse when the event was fired.
     * @param y the vertical coordinate of the mouse when the event was fired.
     */
    void onDragEnd(Widget sender, int x, int y);
}
