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
 * An interface for registering timer listeners.
 * 
 * @version $Id$
 */
public interface SourcesTimerEvents
{
    /**
     * Registers a timer listener for the underlying timer.
     * 
     * @param listener The timer listener to be added to the list of registered listeners.
     */
    void addTimerListener(TimerListener listener);

    /**
     * Unregister a timer listener from the underlying timer.
     * 
     * @param listener The timer listener to be removed from the list of registered listeners.
     */
    void removeTimerListener(TimerListener listener);
}
