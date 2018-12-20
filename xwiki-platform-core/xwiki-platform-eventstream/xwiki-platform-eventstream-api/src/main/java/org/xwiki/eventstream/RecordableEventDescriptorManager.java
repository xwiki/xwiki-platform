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
package org.xwiki.eventstream;

import java.util.List;

import org.xwiki.component.annotation.Role;

/**
 * Get all RecordableEventDescriptors that are present in the wiki.
 *
 * @version $Id$
 * @since 9.2RC1
 */
@Role
public interface RecordableEventDescriptorManager
{
    /**
     * @param allWikis load the descriptors from all the wikis of the farm if true
     * @return all the available implementation of RecordableEventDescriptor
     * @throws EventStreamException if an error occurs
     * @since 9.5.1
     * @since 9.6RC1
     */
    List<RecordableEventDescriptor> getRecordableEventDescriptors(boolean allWikis) throws EventStreamException;

    /**
     * @param eventType the type of the event
     * @param allWikis load the descriptors from all the wikis of the farm if true
     * @return the corresponding RecordableEventDescriptor or null if no one matches
     * @throws EventStreamException if an error happens
     * @since 9.10RC1
     */
    RecordableEventDescriptor getDescriptorForEventType(String eventType, boolean allWikis)
            throws EventStreamException;
}
