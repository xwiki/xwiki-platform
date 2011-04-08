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
package org.xwiki.gwt.user.client.ui.wizard;

import java.util.ArrayList;

import org.xwiki.gwt.user.client.ui.wizard.NavigationListener.NavigationDirection;


/**
 * A collection of {@link NavigationListener} to be used by {@link SourcesNavigationEvents} to manage the list of
 * listeners.
 * 
 * @version $Id$
 */
public class NavigationListenerCollection extends ArrayList<NavigationListener>
{
    /**
     * Field required by all {@link java.io.Serializable} classes.
     */
    private static final long serialVersionUID = 3437895315610766686L;

    /**
     * Fires a navigation event for all listeners in the collection.
     * 
     * @param direction the direction of the fired navigation event
     */
    public void fireNavigationEvent(NavigationDirection direction)
    {
        for (NavigationListener listener : this) {
            listener.onDirection(direction);
        }
    }
}
