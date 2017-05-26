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

import java.util.List;
import java.util.Map;

import org.xwiki.eventstream.RecordableEventDescriptor;
import org.xwiki.eventstream.RecordableEventDescriptorContainer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

/**
 * Represent a notification type described in a wiki page.
 *
 * @version $Id$
 * @since 9.5RC1
 */
@Unstable
public class PageNotificationEventDescriptor implements RecordableEventDescriptor
{
    private String applicationName;

    private String eventType;

    private String eventPrettyName;

    private String eventIcon;

    private List<String> eventTriggers;

    private String objectType;

    private String validationExpression;

    private String notificationTemplate;

    private DocumentReference authorReference;

    private RecordableEventDescriptorContainer recordableEventDescriptorContainer;

    /**
     * Constructs a new PageNotificationEventDescriptor.
     * We use a map in order to pass less arguments to the PageNotificationEventDescriptor constructor.
     * See CheckStyle ParameterNumber rule.
     *
     * @param parameters A map of the objects parameters
     * @param eventTriggerList A list of the events that should trigger the notification
     * @param authorReference A reference to the XObject author (used in template rendering in order to avoid
     * privilege escalation)
     */
    public PageNotificationEventDescriptor(
            Map<String, String> parameters,
            List<String> eventTriggerList,
            DocumentReference authorReference)
    {
        this.applicationName = parameters.get("applicationName");
        this.eventType = parameters.get("eventType");
        this.eventPrettyName = parameters.get("eventPrettyName");
        this.eventIcon = parameters.get("eventIcon");
        this.eventTriggers = eventTriggerList;
        this.objectType = parameters.get("objectType");
        this.validationExpression = parameters.get("validationExpression");
        this.notificationTemplate = parameters.get("notificationTemplate");
        this.authorReference = authorReference;
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
        return this.eventType;
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
     * @return a list of event canonical names that should trigger the notification
     */
    public List<String> getEventTriggers()
    {
        return this.eventTriggers;
    }

    /**
     * The event validation expression should be written in XWiki syntax. If this expression contains `true` when
     * evaluated, then the notification can be triggered. Note that if the validation is blank, it is not taken into
     * account when triggering the notification.
     *
     * @return the event validation expression
     */
    public String getValidationExpression()
    {
        return this.validationExpression;
    }

    /**
     * @return the author of the XObject
     */
    public DocumentReference getAuthorReference()
    {
        return this.authorReference;
    }

    /**
     * @return the event type specified in the PageNotificationEventDescriptorClass XObject
     */
    @Override
    public String getEventType()
    {
        return this.eventType;
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
