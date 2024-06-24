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
package org.xwiki.notifications.filters.event;

import org.xwiki.observation.event.AbstractCancelableEvent;
import org.xwiki.stability.Unstable;

/**
 * Event triggered when a user filter is about to be deleted, the event is sent with the following data:
 * <ul>
 *     <li>source: the reference of the deleted document explaining the cleaning of the filters</li>
 *     <li>data: the actual {@code Set} of {@link org.xwiki.notifications.filters.NotificationFilterPreference} about
 *     to be deleted
 *     </li>
 * </ul>
 *
 * This event is cancellable: canceling it means the data won't be deleted.
 *
 * @version $Id$
 * @since 15.10.2
 * @since 16.0.0RC1
 */
@Unstable
public class CleaningFilterEvent extends AbstractCancelableEvent
{
    /**
     * The version identifier for this Serializable class. Increment only if the <i>serialized</i> form of the class
     * changes.
     */
    private static final long serialVersionUID = 1L;
}
