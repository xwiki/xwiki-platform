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
package org.xwiki.notifications.page.events;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.notifications.page.PageNotificationEventDescriptor;
import org.xwiki.notifications.page.PageNotificationEventUpdater;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.internal.event.XObjectAddedEvent;
import com.xpn.xwiki.internal.event.XObjectDeletedEvent;
import com.xpn.xwiki.internal.event.XObjectEvent;
import com.xpn.xwiki.internal.event.XObjectUpdatedEvent;
import com.xpn.xwiki.objects.BaseObjectReference;

/**
 * This listener triggers {@link PageNotificationEventUpdater} when needed in order to refresh the
 * {@link PageNotificationEvent} registered by the {@link PageNotificationEventDescriptor}.
 *
 * @version $Id$
 * @since 9.5RC1
 */
@Component
@Singleton
@Named(PageNotificationEventListenerUpdater.NAME)
public class PageNotificationEventListenerUpdater extends AbstractEventListener
{
    /**
     * The listener name.
     */
    public static final String NAME = "Page Notification Event Listener Updater";

    @Inject
    private PageNotificationEventUpdater pageNotificationEventUpdater;

    /**
     * Constructs a DefaultPageNotificationEventUpdater.
     */
    public PageNotificationEventListenerUpdater()
    {
        super(NAME,
                new ApplicationReadyEvent(),
                new XObjectAddedEvent(),
                new XObjectDeletedEvent(),
                new XObjectUpdatedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof ApplicationReadyEvent) {
            pageNotificationEventUpdater.updateDescriptors();
        } else if (event instanceof XObjectAddedEvent
                || event instanceof XObjectDeletedEvent
                || event instanceof XObjectUpdatedEvent) {
            // Extract the BaseObjectReference to be able to inspect the XClassReference.
            BaseObjectReference objectReference = getBaseObjectReference((XObjectEvent) event);
            DocumentReference objectClassReference = objectReference.getXClassReference();

            // Only interested in PageNotificationEventDescriptorClass XObjects
            if (objectClassReference.getName().equals("PageNotificationEventDescriptorClass"))
            {
                pageNotificationEventUpdater.updateDescriptors();
            }
        }
    }

    /**
     * @param objectEvent the event involving an object
     * @return the {@link BaseObjectReference} of the object corresponding to the object event
     */
    private BaseObjectReference getBaseObjectReference(XObjectEvent objectEvent)
    {
        EntityReference objectReference = objectEvent.getReference();
        BaseObjectReference baseObjectReference = null;
        if (objectReference instanceof BaseObjectReference) {
            baseObjectReference = (BaseObjectReference) objectEvent.getReference();
        } else {
            baseObjectReference = new BaseObjectReference(objectEvent.getReference());
        }
        return baseObjectReference;
    }
}
