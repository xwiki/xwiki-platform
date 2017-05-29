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
package org.xwiki.notifications.internal.page;

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.page.PageNotificationEvent;
import org.xwiki.notifications.page.PageNotificationEventDescriptor;
import org.xwiki.stability.Unstable;

/**
 * Send a {@link PageNotificationEvent} when a custom event is triggered.
 *
 * @version $Id$
 * @since 9.5RC1
 */
@Role
@Unstable
public interface PageNotificationEventDescriptorManager
{
    /**
     * Update the object descriptorList.
     * @param descriptorList the new list of descriptors to apply
     */
    void updateDescriptorList(List<PageNotificationEventDescriptor> descriptorList);

    /**
     * @return the list of event descriptors
     */
    List<PageNotificationEventDescriptor> getDescriptors();

    /**
     * Find a descriptor corresponding to the given type.
     *
     * @param type Type of the descriptor
     * @return the descriptor that corresponds to the type
     * @throws NotificationException if the descriptor could not be found
     */
    PageNotificationEventDescriptor getDescriptorByType(String type) throws NotificationException;
}
