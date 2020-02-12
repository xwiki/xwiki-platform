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

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.script.ScriptContext;

import org.xwiki.component.annotation.Component;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.notifiers.NotificationDisplayer;
import org.xwiki.notifications.NotificationException;
import org.xwiki.rendering.block.Block;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;

/**
 * Default implementation for (@link {@link NotificationDisplayer}. The displayer try to execute
 * a template called "notifications/${eventType}.vm" and fallback to "notification/default.vm".
 *
 * @version $Id$
 * @since 9.2RC1
 */
@Component
@Singleton
public class DefaultNotificationDisplayer implements NotificationDisplayer
{
    private static final String EVENT_BINDING_NAME = "event";

    @Inject
    private TemplateManager templateManager;

    @Inject
    private ScriptContextManager scriptContextManager;

    @Override
    public Block renderNotification(CompositeEvent eventNotification) throws NotificationException
    {
        ScriptContext scriptContext = this.scriptContextManager.getScriptContext();
        try {
            scriptContext.setAttribute(EVENT_BINDING_NAME, eventNotification, ScriptContext.ENGINE_SCOPE);

            String templateName = String.format("notification/%s.vm",
                    eventNotification.getType().replaceAll("\\/", "."));
            Template template = templateManager.getTemplate(templateName);

            return (template != null) ? templateManager.execute(template)
                    : templateManager.execute("notification/default.vm");

        } catch (Exception e) {
            throw new NotificationException("Failed to render the notification.", e);
        } finally {
            scriptContext.removeAttribute(EVENT_BINDING_NAME, ScriptContext.ENGINE_SCOPE);
        }
    }

    @Override
    public List<String> getSupportedEvents()
    {
        // The default notification displayer is called manually if no other displayer has been found so we don't need
        // to create a list of supported events.
        return Collections.emptyList();
    }
}
