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
package org.xwiki.notifications.internal;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import javax.script.ScriptContext;

import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.component.wiki.WikiComponentScope;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationDisplayer;
import org.xwiki.notifications.NotificationException;
import org.xwiki.rendering.block.Block;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;
import org.xwiki.text.StringUtils;

import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;

/**
 * This class is meant to be instanciated and then registered to the Component Manager by the
 * {@link NotificationDisplayerComponentBuilder} component every time a document containing a
 * NotificationDisplayerClass is added, updated or deleted.
 *
 * @version $Id$
 * @since 9.5RC1
 */
public class NotificationDisplayerComponent implements WikiComponent, NotificationDisplayer
{
    private static final String EVENT_BINDING_NAME = "event";

    private TemplateManager templateManager;

    private ScriptContextManager scriptContextManager;

    private ComponentManager componentManager;

    private BaseObjectReference objectReference;

    private DocumentReference authorReference;

    private String notificationTemplate;

    private String eventType;

    /**
     * Constructs a new {@link NotificationDisplayerComponent}.
     *
     * @param objectReference the reference to the template XObject
     * @param authorReference the author reference of the document
     * @param templateManager the {@link TemplateManager} to use
     * @param scriptContextManager the {@link ScriptContextManager} to use
     * @param componentManager the {@link ComponentManager} to use
     * @param baseObject the XObject which has the required properties to instantiate the component
     * @throws NotificationException if the properties of the given BaseObject could not be loaded
     */
    public NotificationDisplayerComponent(BaseObjectReference objectReference, DocumentReference authorReference,
            TemplateManager templateManager, ScriptContextManager scriptContextManager,
            ComponentManager componentManager, BaseObject baseObject) throws NotificationException
    {
        this.objectReference = objectReference;
        this.authorReference = authorReference;
        this.templateManager = templateManager;
        this.scriptContextManager = scriptContextManager;
        this.componentManager = componentManager;
        this.setProperties(baseObject);
    }

    /**
     * Set the object attributes by extracting their values from the given BaseObject.
     *
     * @param baseObject the XObject that should contain the parameters
     * @throws NotificationException if an error occured while extracting the parameters from the base object
     */
    private void setProperties(BaseObject baseObject) throws NotificationException
    {
        try {
            this.notificationTemplate = baseObject.getStringValue(
                    NotificationDisplayerDocumentInitializer.NOTIFICATION_TEMPLATE);
            this.eventType = baseObject.getStringValue(NotificationDisplayerDocumentInitializer.EVENT_TYPE);
        } catch (Exception e) {
            throw new NotificationException(
                    String.format("Unable to extract the parameters from the [%s] NotificationDisplayerClass.",
                            baseObject), e);
        }
    }

    @Override
    public Block renderNotification(CompositeEvent eventNotification) throws NotificationException
    {
        try {
            // Allow the template to access the event during its execution
            scriptContextManager.getCurrentScriptContext().setAttribute(EVENT_BINDING_NAME, eventNotification,
                    ScriptContext.ENGINE_SCOPE);

            // If we have no template defined, fallback on the default displayer
            if (StringUtils.isBlank(this.notificationTemplate)) {
                return ((NotificationDisplayer) this.componentManager.getInstance(NotificationDisplayer.class))
                        .renderNotification(eventNotification);
            }

            // Render the template
            Template customTemplate = templateManager.createStringTemplate(
                    this.notificationTemplate,
                    this.getAuthorReference());

            return templateManager.getXDOM(customTemplate);

        } catch (Exception e) {
            throw new NotificationException(
                    String.format("Unable to render notification template for the [%s].", this.eventType), e);
        } finally {
            // Unbind the event from the current script context
            scriptContextManager.getCurrentScriptContext().removeAttribute(EVENT_BINDING_NAME,
                    ScriptContext.ENGINE_SCOPE);
        }
    }

    @Override
    public List<String> getSupportedEvents()
    {
        return Arrays.asList(this.eventType);
    }

    @Override
    public DocumentReference getDocumentReference()
    {
        return (DocumentReference) this.objectReference.getParent();
    }

    @Override
    public EntityReference getEntityReference()
    {
        return this.objectReference;
    }

    @Override
    public DocumentReference getAuthorReference()
    {
        return this.authorReference;
    }

    @Override
    public Type getRoleType()
    {
        return NotificationDisplayer.class;
    }

    @Override
    public String getRoleHint()
    {
        return this.eventType;
    }

    @Override
    public WikiComponentScope getScope()
    {
        return WikiComponentScope.WIKI;
    }
}
