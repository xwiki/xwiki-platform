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

/**
 * An interface for registering drag event listeners.
 * 
 * @version $Id$
 */
public interface SourcesDragEvents
{
    /**
     * Registers a drag listener for the underlying event source.
     * 
     * @param listener The drag listener to be added to the list of registered listeners.
     */
    void addDragListener(DragListener listener);

    /**
     * Unregister a drag listener from the underlying event source.
     * 
     * @param listener The drag listener to be removed from the list of registered listeners.
     */
    void removeDragListener(DragListener listener);
}
