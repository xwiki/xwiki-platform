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
package org.xwiki.notifications.page;

import org.xwiki.eventstream.RecordableEventDescriptor;
import org.xwiki.eventstream.RecordableEventDescriptorContainer;
import org.xwiki.stability.Unstable;

/**
 * Represent a custom user-defined notification.
 *
 * @version $Id$
 * @since 9.4RC1
 */
@Unstable
public class PageNotificationEventDescriptor implements RecordableEventDescriptor
{
    private String applicationName;

    private String eventId;

    private String eventPrettyName;

    private String eventIcon;

    private String eventCanonicalName;

    private String objectType;

    private String notificationTemplate;

    private RecordableEventDescriptorContainer recordableEventDescriptorContainer;

    /**
     * Constructs a new PageNotificationEventDescriptor.
     *
     * @param applicationName Name of the application
     * @param eventId Event identifier
     * @param eventPrettyName Displayed name of the event
     * @param eventIcon Name of the icon used for the event
     * @param eventCanonicalName Class path of the listener to use
     * @param objectType Class path of the object to use
     * @param notificationTemplate Custom notification template
     */
    public PageNotificationEventDescriptor(
            String applicationName,
            String eventId,
            String eventPrettyName,
            String eventIcon,
            String eventCanonicalName,
            String objectType,
            String notificationTemplate) {
        this.applicationName = applicationName;
        this.eventId = eventId;
        this.eventPrettyName = eventPrettyName;
        this.eventIcon = eventIcon;
        this.eventCanonicalName = eventCanonicalName;
        this.objectType = objectType;
        this.notificationTemplate = notificationTemplate;
    }

    /**
     * Register the {@link PageNotificationEventDescriptor} to the {@link RecordableEventDescriptorContainer}.
     *
     * @param recordableEventDescriptorContainer Reference to the current {@link RecordableEventDescriptorContainer}
     * component
     */
    public void register(RecordableEventDescriptorContainer recordableEventDescriptorContainer) {
        this.recordableEventDescriptorContainer = recordableEventDescriptorContainer;
        this.recordableEventDescriptorContainer.addRecordableEventDescriptor(this);
    }

    /**
     * Unload the {@link PageNotificationEventDescriptor} from the {@link RecordableEventDescriptorContainer}.
     */
    public void unRegister() {
        this.recordableEventDescriptorContainer.deleteRecordableEventDescriptor(this);
    }

    /**
     * @return the custom event name
     */
    public String getEventName()
    {
        return this.eventId;
    }

    /**
     * @return the object that should be listened
     */
    public String getObjectType()
    {
        return this.objectType;
    }

    /**
     * @return the notification template
     */
    public String getNotificationTemplate()
    {
        return this.notificationTemplate;
    }

    /**
     * @return the event canonical name
     */
    public String getEventCanonicalName()
    {
        return this.eventCanonicalName;
    }

    @Override
    public String getEventType()
    {
        return this.eventId;
    }

    @Override
    public String getApplicationName()
    {
        return this.applicationName;
    }

    @Override
    public String getDescription()
    {
        return this.eventPrettyName;
    }

    @Override
    public String getApplicationIcon()
    {
        return this.eventIcon;
    }
}
