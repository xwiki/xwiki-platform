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
package org.xwiki.notifications.filters;

import java.util.Set;

import org.xwiki.component.annotation.Role;
import org.xwiki.notifications.NotificationException;
import org.xwiki.rendering.block.Block;

/**
 * This interface can be used to display a given notification filter with a corresponding preference.
 *
 * @version $Id$
 * @since 9.7RC1
 */
@Role
public interface NotificationFilterDisplayer
{
    /**
     * Using the given {@link NotificationFilter} and an associated {@link NotificationFilterPreference}, display
     * the filter.
     *
     * @param filter the filter that should be displayed
     * @param preference the preferences that should be used for rendering the block
     * @return the rendered form of the given filter
     * @throws NotificationException if error occurs
     */
    Block display(NotificationFilter filter, NotificationFilterPreference preference)
            throws NotificationException;

    /**
     * @return a set of {@link NotificationFilter} names that are supported by the displayer.
     */
    Set<String> getSupportedFilters();
}
