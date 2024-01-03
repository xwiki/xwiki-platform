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
package org.xwiki.notifications.notifiers.internal;

import java.lang.reflect.Type;
import java.util.List;

import javax.inject.Inject;
import javax.script.ScriptContext;

import org.xwiki.component.annotation.Component;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.notifiers.NotificationDisplayer;
import org.xwiki.rendering.block.Block;
import org.xwiki.template.Template;

import com.xpn.xwiki.objects.BaseObject;

/**
 * This class is meant to be instanciated and then registered to the Component Manager by the
 * {@link WikiNotificationDisplayerComponentBuilder} component every time a document containing a
 * NotificationDisplayerClass is added, updated or deleted.
 *
 * @version $Id$
 * @since 9.5RC1
 */
@Component(roles = WikiNotificationDisplayer.class)
public class WikiNotificationDisplayer extends AbstractWikiNotificationRenderer implements NotificationDisplayer
{
    @Inject
    private NotificationDisplayer notificationDisplayer;

    private Template notificationTemplate;

    private List<String> supportedEvents;

    @Override
    public void initialize(BaseObject baseObject) throws NotificationException
    {
        super.initialize(baseObject);

        this.supportedEvents = List.of(this.eventType);
        this.notificationTemplate =
            extractTemplate(baseObject, WikiNotificationDisplayerDocumentInitializer.NOTIFICATION_TEMPLATE);
    }

    @Override
    public Block renderNotification(CompositeEvent eventNotification) throws NotificationException
    {
        // Save the old value in the context that refers to EVENT_BINDING_NAME
        Object oldContextAttribute = scriptContextManager.getCurrentScriptContext().getAttribute(EVENT_BINDING_NAME,
                ScriptContext.ENGINE_SCOPE);

        try {
            // Allow the template to access the event during its execution
            scriptContextManager.getCurrentScriptContext().setAttribute(EVENT_BINDING_NAME, eventNotification,
                    ScriptContext.ENGINE_SCOPE);

            // If we have no template defined, fallback on the default displayer
            if (this.notificationTemplate == null) {
                return this.notificationDisplayer.renderNotification(eventNotification);
            }

            return templateManager.execute(notificationTemplate);

        } catch (Exception e) {
            throw new NotificationException(
                    String.format("Unable to render notification template for the [%s].", this.eventType), e);
        } finally {
            // Restore the old object associated with EVENT_BINDING_NAME
            scriptContextManager.getCurrentScriptContext().setAttribute(EVENT_BINDING_NAME, oldContextAttribute,
                    ScriptContext.ENGINE_SCOPE);
        }
    }

    @Override
    public List<String> getSupportedEvents()
    {
        return this.supportedEvents;
    }

    @Override
    public Type getRoleType()
    {
        return NotificationDisplayer.class;
    }
}
