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
import org.xwiki.model.reference.RegexEntityReference;
import org.xwiki.notifications.internal.page.PageNotificationEventDescriptorManager;
import org.xwiki.notifications.page.PageNotificationEventDescriptor;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.internal.event.XObjectAddedEvent;
import com.xpn.xwiki.internal.event.XObjectDeletedEvent;
import com.xpn.xwiki.internal.event.XObjectUpdatedEvent;
import com.xpn.xwiki.objects.BaseObjectReference;

/**
 * This listener triggers {@link PageNotificationEventDescriptorManager} when needed in order to refresh the
 * {@link org.xwiki.notifications.page.PageNotificationEvent} registered by the {@link PageNotificationEventDescriptor}.
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

    /**
     * The reference to match the XObject PageNotificationEventDescriptorClass.
     */
    private static final RegexEntityReference PAGE_NOTIFICATION_EVENT_DESCRIPTOR_CLASS_REFERENCE =
            BaseObjectReference.any("XWiki.Notifications.Code.PageNotificationEventDescriptorClass");


    @Inject
    private PageNotificationEventDescriptorManager pageNotificationEventDescriptorManager;

    /**
     * Constructs a DefaultPageNotificationEventUpdater.
     */
    public PageNotificationEventListenerUpdater()
    {
        super(NAME,
                new ApplicationReadyEvent(),
                new XObjectAddedEvent(PAGE_NOTIFICATION_EVENT_DESCRIPTOR_CLASS_REFERENCE),
                new XObjectDeletedEvent(PAGE_NOTIFICATION_EVENT_DESCRIPTOR_CLASS_REFERENCE),
                new XObjectUpdatedEvent(PAGE_NOTIFICATION_EVENT_DESCRIPTOR_CLASS_REFERENCE));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        pageNotificationEventDescriptorManager.updateDescriptors();
    }
}
