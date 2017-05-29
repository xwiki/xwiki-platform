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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.RecordableEventDescriptorContainer;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.page.PageNotificationEventDescriptor;

/**
 * Default implementation of {@link PageNotificationEventDescriptorManager}.
 *
 * @version $Id$
 * @since 9.5RC1
 */
@Component
@Singleton
public class DefaultPageNotificationEventDescriptorManager implements
        PageNotificationEventDescriptorManager
{
    private List<PageNotificationEventDescriptor> descriptorList = new ArrayList<>();

    @Inject
    private RecordableEventDescriptorContainer recordableEventDescriptorContainer;

    @Override
    public void updateDescriptorList(List<PageNotificationEventDescriptor> descriptorList)
    {
        // Remove the «old» descriptors from their RecordableEventDescriptorContainer …
        Iterator<PageNotificationEventDescriptor> it = this.descriptorList.iterator();

        while (it.hasNext()) {
            it.next().unRegister();
            it.remove();
        }

        // … and register the new descriptors
        for (PageNotificationEventDescriptor descriptor : descriptorList) {
            descriptor.register(this.recordableEventDescriptorContainer);
        }

        this.descriptorList = descriptorList;
    }

    @Override
    public List<PageNotificationEventDescriptor> getDescriptors()
    {
        return this.descriptorList;
    }

    @Override
    public PageNotificationEventDescriptor getDescriptorByType(String type) throws NotificationException
    {
        for (PageNotificationEventDescriptor element : this.descriptorList) {
            if (element.getEventType().equals(type)) {
                return element;
            }
        }

        throw new NotificationException("Unable to find a descriptor matching the given type.");
    }
}
