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
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationDisplayer;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.page.PageNotificationEventDescriptor;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;
import org.xwiki.template.helpers.StringTemplate;
import org.xwiki.velocity.VelocityManager;

/**
 * Implement a {@link NotificationDisplayer} for the event type
 * {@link org.xwiki.notifications.page.PageNotificationEvent}.
 *
 * @version $Id$
 * @since 9.5RC1
 */
@Component
@Singleton
@Named(DefaultPageNotificationDisplayer.EVENT_TYPE)
public class DefaultPageNotificationDisplayer implements NotificationDisplayer
{
    /**
     * Event handled by the displayer.
     */
    public static final String EVENT_TYPE = "org.xwiki.notifications.page.PageNotificationEvent";

    private static final String EVENT_BINDING_NAME = "event";

    @Inject
    private PageNotificationEventDescriptorManager pageNotificationEventDescriptorManager;

    @Inject
    private TemplateManager templateManager;

    @Inject
    private VelocityManager velocityManager;

    @Override
    public Block renderNotification(CompositeEvent eventNotification) throws NotificationException
    {
        try {
            velocityManager.getCurrentVelocityContext().put(EVENT_BINDING_NAME, eventNotification);

            PageNotificationEventDescriptor eventDescriptor =
                    pageNotificationEventDescriptorManager.getDescriptorByType(
                            eventNotification.getEvents().get(0).getType());

            // If we have no template defined, use the default one
            if (StringUtils.isBlank(eventDescriptor.getNotificationTemplate())) {
                // Try to render a template using the event name
                String templateName = String.format("notification/%s.vm",
                        eventDescriptor.getEventName().replaceAll("\\/", "."));
                Template template = templateManager.getTemplate(templateName);

                // If we can’t find such template, render the default notification template.
                return (template != null)
                        ? templateManager.executeNoException(template)
                        : templateManager.executeNoException("notification/default.vm");
            }

            Template customTemplate = new StringTemplate(
                    eventDescriptor.getNotificationTemplate(),
                    Syntax.XWIKI_2_1,
                    Syntax.PLAIN_1_0,
                    eventDescriptor.getAuthorReference());

            return templateManager.getXDOM(customTemplate);

        } catch (Exception e) {
            throw new NotificationException("Unable to render notification template.", e);
        } finally {
            velocityManager.getCurrentVelocityContext().remove(EVENT_BINDING_NAME);
        }
    }

    @Override
    public List<String> getSupportedEvents()
    {
        List<String> supportedEvents = new ArrayList<>();

        for (PageNotificationEventDescriptor descriptor: pageNotificationEventDescriptorManager.getDescriptors()) {
            supportedEvents.add(descriptor.getEventType());
        }

        return supportedEvents;
    }
}
