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
package com.xpn.xwiki.wysiwyg.client.util;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.MouseListener;
import com.google.gwt.user.client.ui.SourcesMouseEvents;
import com.google.gwt.user.client.ui.Widget;

/**
 * Adapts {@link SourcesMouseEvents} to {@link SourcesDragEvents}.
 * 
 * @version $Id$
 */
public class DragAdaptor implements SourcesDragEvents, MouseListener
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
        ((SourcesMouseEvents) adaptee).addMouseListener(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see SourcesDragEvents#addDragListener(DragListener)
     */
    public void addDragListener(DragListener listener)
    {
        dragListeners.add(listener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see SourcesDragEvents#removeDragListener(DragListener)
     */
    public void removeDragListener(DragListener listener)
    {
        dragListeners.remove(listener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see MouseListener#onMouseDown(Widget, int, int)
     */
    public void onMouseDown(Widget sender, int x, int y)
    {
        if (sender == adaptee) {
            mouseDown = true;
            DOM.setCapture(adaptee.getElement());
            xMouseDown = x;
            yMouseDown = y;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see MouseListener#onMouseEnter(Widget)
     */
    public void onMouseEnter(Widget sender)
    {
        // ignore
    }

    /**
     * {@inheritDoc}
     * 
     * @see MouseListener#onMouseLeave(Widget)
     */
    public void onMouseLeave(Widget sender)
    {
        // ignore
    }

    /**
     * {@inheritDoc}
     * 
     * @see MouseListener#onMouseMove(Widget, int, int)
     */
    public void onMouseMove(Widget sender, int x, int y)
    {
        if (sender == adaptee) {
            if (dragging) {
                dragListeners.fireDrag(adaptee, x, y);
            } else if (mouseDown && (Math.abs(x - xMouseDown) > DELTA || Math.abs(y - yMouseDown) > DELTA)) {
                dragging = true;
                dragListeners.fireDragStart(adaptee, x, y);
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see MouseListener#onMouseUp(Widget, int, int)
     */
    public void onMouseUp(Widget sender, int x, int y)
    {
        if (sender == adaptee) {
            mouseDown = false;
            DOM.releaseCapture(adaptee.getElement());
            if (dragging) {
                dragging = false;
                dragListeners.fireDragEnd(adaptee, x, y);
            }
        }
    }
}
