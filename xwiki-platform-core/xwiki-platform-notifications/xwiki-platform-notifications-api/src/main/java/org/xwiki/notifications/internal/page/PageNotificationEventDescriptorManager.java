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

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.eventstream.RecordableEventDescriptorContainer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.internal.ModelBridge;
import org.xwiki.notifications.page.PageNotificationEventDescriptor;
import org.xwiki.query.Query;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;

/**
 * Send a {@link org.xwiki.notifications.page.PageNotificationEvent} when a custom event is triggered.
 *
 * @version $Id$
 * @since 9.5RC1
 */
@Component(roles = PageNotificationEventDescriptorManager.class)
@Singleton
public class PageNotificationEventDescriptorManager
{
    @Inject
    private RecordableEventDescriptorContainer recordableEventDescriptorContainer;

    @Inject
    private QueryManager queryManager;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private DocumentReferenceResolver documentReferenceResolver;

    @Inject
    private ModelBridge modelBridge;

    @Inject
    private Logger logger;

    private List<PageNotificationEventDescriptor> descriptors = new ArrayList<>();

    /**
     * Update the object descriptorList.
     * @param descriptorList the new list of descriptors to apply
     */
    public void updateDescriptorList(List<PageNotificationEventDescriptor> descriptorList)
    {
        // Remove the «old» descriptors from their RecordableEventDescriptorContainer …
        Iterator<PageNotificationEventDescriptor> it = this.descriptors.iterator();

        while (it.hasNext()) {
            it.next().unRegister();
            it.remove();
        }

        // … and register the new descriptors
        for (PageNotificationEventDescriptor descriptor : descriptorList) {
            descriptor.register(this.recordableEventDescriptorContainer);
        }

        this.descriptors = descriptorList;
    }

    /**
     * @return the list of event descriptors
     */
    public List<PageNotificationEventDescriptor> getDescriptors()
    {
        return this.descriptors;
    }

    /**
     * Find a descriptor corresponding to the given type.
     *
     * @param type Type of the descriptor
     * @return the descriptor that corresponds to the type
     * @throws NotificationException if the descriptor could not be found
     */
    public PageNotificationEventDescriptor getDescriptorByType(String type) throws NotificationException
    {
        for (PageNotificationEventDescriptor element : this.descriptors) {
            if (element.getEventType().equals(type)) {
                return element;
            }
        }

        throw new NotificationException("Unable to find a descriptor matching the given type.");
    }

    /**
     * Fetch every registered PageNotificationEventDescriptorClass XObjects in the wiki, then update the
     * {@link PageNotificationEventDescriptorManager}.
     */
    public void updateDescriptors()
    {
        List<PageNotificationEventDescriptor> descriptorList = new ArrayList<>();

        // Fetch every PageNotificationEventDescriptors in the farm
        try {
            final List<String> newDescriptors = (List<String>) (List) this.queryManager.createQuery(
                    "from doc.object(XWiki.Notifications.Code.PageNotificationEventDescriptorClass)"
                            + " as document",
                    Query.XWQL)
                    .addFilter(componentManager.<QueryFilter>getInstance(QueryFilter.class, "unique"))
                    .execute();

            for (String descriptor: newDescriptors) {
                DocumentReference document = documentReferenceResolver.resolve(descriptor);
                descriptorList.add(modelBridge.getPageNotificationEventDescriptor(document));
            }
            this.updateDescriptorList(descriptorList);
        } catch (Exception e) {
            logger.warn(String.format(
                    "Unable to update the list of in-page notifications. Exception : %s\nStacktrace : %s",
                    e.getMessage(),
                    e.getStackTrace()));
        }
    }
}
