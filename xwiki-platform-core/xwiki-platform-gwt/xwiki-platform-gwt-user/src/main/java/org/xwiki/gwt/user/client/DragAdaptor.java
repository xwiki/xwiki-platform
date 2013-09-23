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

import com.google.gwt.event.dom.client.HasMouseDownHandlers;
import com.google.gwt.event.dom.client.HasMouseMoveHandlers;
import com.google.gwt.event.dom.client.HasMouseUpHandlers;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Widget;

/**
 * Adapts mouse events to drag events.
 * 
 * @version $Id$
 */
public class DragAdaptor implements SourcesDragEvents, MouseDownHandler, MouseMoveHandler, MouseUpHandler
{
    /**
     * The number of pixels the mouse has to move before the drag starts, after the mouse button was pressed.
     */
    private static final int DELTA = 3;

    /**
     * The collection of registered drag listeners.
     */
    private final DragListenerCollection dragListeners = new DragListenerCollection();

    /**
     * The widget whose mouse events will adapted to drag events.
     */
    private final Widget adaptee;

    /**
     * Flag specifying whether the adaptee is being dragged.
     */
    private boolean dragging;

    /**
     * Specifies if the user holds the mouse button down.
     */
    private boolean mouseDown;

    /**
     * The horizontal coordinate when mouse down was fired.
     */
    private int xMouseDown;

    /**
     * The vertical coordinate when mouse up was fired.
     */
    private int yMouseDown;

    /**
     * Creates a new drag adaptor for the given source of mouse events.
     * 
     * @param adaptee {@link #adaptee}
     */
    public DragAdaptor(Widget adaptee)
    {
        this.adaptee = adaptee;
        ((HasMouseDownHandlers) adaptee).addMouseDownHandler(this);
        ((HasMouseMoveHandlers) adaptee).addMouseMoveHandler(this);
        ((HasMouseUpHandlers) adaptee).addMouseUpHandler(this);
    }

    @Override
    public void addDragListener(DragListener listener)
    {
        dragListeners.add(listener);
    }

    @Override
    public void removeDragListener(DragListener listener)
    {
        dragListeners.remove(listener);
    }

    @Override
    public void onMouseDown(MouseDownEvent event)
    {
        if (event.getSource() == adaptee) {
            mouseDown = true;
            DOM.setCapture(adaptee.getElement());
            xMouseDown = event.getX();
            yMouseDown = event.getY();
        }
    }

    @Override
    public void onMouseMove(MouseMoveEvent event)
    {
        if (event.getSource() == adaptee) {
            int x = event.getX();
            int y = event.getY();
            if (dragging) {
                dragListeners.fireDrag(adaptee, x, y);
            } else if (mouseDown && (Math.abs(x - xMouseDown) > DELTA || Math.abs(y - yMouseDown) > DELTA)) {
                dragging = true;
                dragListeners.fireDragStart(adaptee, x, y);
            }
        }
    }

    @Override
    public void onMouseUp(MouseUpEvent event)
    {
        if (event.getSource() == adaptee) {
            mouseDown = false;
            DOM.releaseCapture(adaptee.getElement());
            if (dragging) {
                dragging = false;
                dragListeners.fireDragEnd(adaptee, event.getX(), event.getY());
            }
        }
    }
}
