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
import java.util.Arrays;
import java.util.List;

import javax.script.ScriptContext;

import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.notifiers.NotificationDisplayer;
import org.xwiki.rendering.block.Block;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;

import com.xpn.xwiki.objects.BaseObject;

/**
 * This class is meant to be instanciated and then registered to the Component Manager by the
 * {@link WikiNotificationDisplayerComponentBuilder} component every time a document containing a
 * NotificationDisplayerClass is added, updated or deleted.
 *
 * @version $Id$
 * @since 9.5RC1
 */
public class WikiNotificationDisplayer extends AbstractWikiNotificationRenderer implements NotificationDisplayer
{
    private Template notificationTemplate;

    private List<String> supportedEvents;

    /**
     * Constructs a new {@link WikiNotificationDisplayer}.
     *
     * @param authorReference the author reference of the document
     * @param templateManager the {@link TemplateManager} to use
     * @param scriptContextManager the {@link ScriptContextManager} to use
     * @param componentManager the {@link ComponentManager} to use
     * @param baseObject the XObject which has the required properties to instantiate the component
     * @throws NotificationException if the properties of the given BaseObject could not be loaded
     */
    public WikiNotificationDisplayer(DocumentReference authorReference, TemplateManager templateManager,
            ScriptContextManager scriptContextManager, ComponentManager componentManager, BaseObject baseObject)
            throws NotificationException
    {
        super(authorReference, templateManager, scriptContextManager, componentManager, baseObject);
        this.supportedEvents = Arrays.asList(this.eventType);
        this.notificationTemplate = extractTemplate(baseObject,
                WikiNotificationDisplayerDocumentInitializer.NOTIFICATION_TEMPLATE);
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
                return ((NotificationDisplayer) this.componentManager.getInstance(NotificationDisplayer.class))
                        .renderNotification(eventNotification);
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
