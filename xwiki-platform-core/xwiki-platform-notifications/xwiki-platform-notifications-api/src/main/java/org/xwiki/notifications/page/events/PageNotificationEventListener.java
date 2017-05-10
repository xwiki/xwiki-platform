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

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.page.PageNotificationEvent;
import org.xwiki.notifications.page.PageNotificationEventDescriptor;
import org.xwiki.notifications.page.PageNotificationEventDescriptorContainer;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.AllEvent;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Default implementation of {@link PageNotificationEventDescriptorContainer}.
 *
 * @version $Id$
 * @since 9.4RC1
 */
@Component
@Singleton
@Named(PageNotificationEventListener.NAME)
public class PageNotificationEventListener extends AbstractEventListener
{
    /**
     * The listener name.
     */
    public static final String NAME = "Page Notification Event Listener";

    @Inject
    private ObservationManager observationManager;

    @Inject
    private PageNotificationEventDescriptorContainer pageNotificationEventDescriptorContainer;

    /**
     * Constructs a PageNotificationEventListener.
     */
    public PageNotificationEventListener()
    {
        super(NAME, AllEvent.ALLEVENT);
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // Filter the event descriptors concerned by the event, then create the concerned events
        for (PageNotificationEventDescriptor descriptor : pageNotificationEventDescriptorContainer.getDescriptorList())
        {
            // If the event is expected by our descriptor
            if (descriptor.getEventCanonicalName().equals(event.getClass().getCanonicalName())) {
                XWikiDocument document = (XWikiDocument) source;
                Map<DocumentReference, List<BaseObject>> documentXObjects = document.getXObjects();
                /*  We can’t create a DocumentReference when only using descriptor.objectType, so we will have to
                    iterate through the map */
                for (DocumentReference documentReference : documentXObjects.keySet())  {
                    if (documentReference.toString().equals(descriptor.getObjectType())) {
                        observationManager.notify(
                                new PageNotificationEvent(descriptor),
                                "org.xwiki.platform:xwiki-platform-notifications-api", document);
                    }
                }
            }
        }
    }
}
