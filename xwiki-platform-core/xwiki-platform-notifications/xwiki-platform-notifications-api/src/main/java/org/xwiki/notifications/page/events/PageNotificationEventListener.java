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
import javax.script.ScriptContext;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.notifications.internal.page.PageNotificationEventDescriptorManager;
import org.xwiki.notifications.page.PageNotificationEvent;
import org.xwiki.notifications.page.PageNotificationEventDescriptor;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.AllEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Default implementation of {@link PageNotificationEventDescriptorManager}.
 *
 * @version $Id$
 * @since 9.5RC1
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

    /**
     * The binding name of the event when a validation expression is rendered.
     */
    public static final String EVENT_BINDING_NAME = "event";

    @Inject
    private ObservationManager observationManager;

    @Inject
    private PageNotificationEventDescriptorManager pageNotificationEventDescriptorManager;

    @Inject
    private TemplateManager templateManager;

    @Inject
    @Named("plain/1.0")
    private BlockRenderer plainRenderer;

    @Inject
    private DocumentReferenceResolver documentReferenceResolver;

    @Inject
    private ScriptContextManager scriptContextManager;

    @Inject
    private Logger logger;

    /**
     * Constructs a {@link PageNotificationEventListener}.
     */
    public PageNotificationEventListener()
    {
        super(NAME, AllEvent.ALLEVENT);
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // Filter the event descriptors concerned by the event, then create the concerned events
        for (PageNotificationEventDescriptor descriptor : pageNotificationEventDescriptorManager.getDescriptors())
        {
            // If the event is expected by our descriptor
            if (descriptor.getEventTriggers().contains(event.getClass().getCanonicalName())
                    && checkXObjectCondition(descriptor, source)
                    && this.evaluateVelocityTemplate(
                            event, descriptor.getAuthorReference(), descriptor.getValidationExpression())) {
                observationManager.notify(
                        new PageNotificationEvent(descriptor),
                        "org.xwiki.platform:xwiki-platform-notifications-api",
                        source);
            }
        }
    }

    /**
     * Ensure that the given the source matches what the descriptor needs.
     * If the source is an instance of XWikiDocument, will check if the document contains the XObject specified in
     * the descriptor.
     *
     * @param descriptor the event descriptor
     * @param source the event source
     * @return
     */
    private boolean checkXObjectCondition(PageNotificationEventDescriptor descriptor, Object source) {
        if (StringUtils.isBlank(descriptor.getObjectType())) {
            // If no XObject is specified in the Object Type field, we don’t need to check the source.
            return true;
        } else if (source instanceof XWikiDocument) {
            XWikiDocument document = (XWikiDocument) source;
            Map<DocumentReference, List<BaseObject>> documentXObjects = document.getXObjects();

            LocalDocumentReference localXObjectReference =
                    documentReferenceResolver.resolve(descriptor.getObjectType()).getLocalDocumentReference();

            /*  We can’t create a DocumentReference when only using descriptor.objectType, so we will have to
                    iterate through the map */
            for (DocumentReference documentReference : documentXObjects.keySet())  {
                if (documentReference.getLocalDocumentReference().equals(localXObjectReference)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Evaluate the given velocity template and return a boolean.
     *
     * @param event the event that should be bound to the script context
     * @param userReference a user reference used to build context
     * @param templateContent the velocity template that should be evaluated
     * @return true if the template evaluation returned «true» or if the template is empty
     */
    private boolean evaluateVelocityTemplate(Event event, DocumentReference userReference, String templateContent)
    {
        try {
            // We don’t need to evaluate the template if it’s empty
            if (StringUtils.isBlank(templateContent)) {
                return true;
            }

            scriptContextManager.getCurrentScriptContext().setAttribute(
                    EVENT_BINDING_NAME,
                    event,
                    ScriptContext.ENGINE_SCOPE);

            Template customTemplate = templateManager.createStringTemplate(templateContent, userReference);
            XDOM templateXDOM = templateManager.execute(customTemplate);

            WikiPrinter printer = new DefaultWikiPrinter();
            plainRenderer.render(templateXDOM, printer);

            scriptContextManager.getCurrentScriptContext().removeAttribute(
                    EVENT_BINDING_NAME,
                    ScriptContext.ENGINE_SCOPE);

            return printer.toString().trim().equals("true");
        } catch (Exception e) {
            logger.warn(String.format(
                    "Unable to render a notification validation template. Error : %s\nStack trace : %s",
                    e.getMessage(),
                    e.getStackTrace()));

            scriptContextManager.getCurrentScriptContext().removeAttribute(
                    EVENT_BINDING_NAME,
                    ScriptContext.ENGINE_SCOPE);

            return false;
        }
    }
}
