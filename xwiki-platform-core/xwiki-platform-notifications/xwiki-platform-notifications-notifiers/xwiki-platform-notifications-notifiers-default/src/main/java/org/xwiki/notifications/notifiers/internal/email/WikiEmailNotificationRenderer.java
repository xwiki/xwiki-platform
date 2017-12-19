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
package org.xwiki.notifications.notifiers.internal.email;

import java.lang.reflect.Type;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.notifiers.email.NotificationEmailRenderer;
import org.xwiki.notifications.notifiers.internal.AbstractWikiNotificationRenderer;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;

import com.xpn.xwiki.objects.BaseObject;

/**
 * This class is meant to be instanciated and then registered to the Component Manager by the
 * {@link WikiEmailNotificationRendererComponentBuilder} component every time a document containing a
 * NotificationEmailRendererClass is added, updated or deleted.
 *
 * @version $Id$
 * @since 9.11.1
 */
public class WikiEmailNotificationRenderer extends AbstractWikiNotificationRenderer
        implements NotificationEmailRenderer
{
    private EmailTemplateRenderer emailTemplateRenderer;

    private Template htmlTemplate;

    private Template plainTextTemplate;

    private Template emailSubjectTemplate;

    /**
     * Constructs a new {@link WikiEmailNotificationRenderer}.
     *
     * @param authorReference the author reference of the document
     * @param templateManager the {@link TemplateManager} to use
     * @param scriptContextManager the {@link ScriptContextManager} to use
     * @param componentManager the {@link ComponentManager} to use
     * @param baseObject the XObject which has the required properties to instantiate the component
     * @throws NotificationException if the properties of the given BaseObject could not be loaded
     */
    public WikiEmailNotificationRenderer(DocumentReference authorReference, TemplateManager templateManager,
            ScriptContextManager scriptContextManager, ComponentManager componentManager, BaseObject baseObject)
            throws NotificationException
    {
        super(authorReference, templateManager, scriptContextManager, componentManager, baseObject);
        try {
            emailTemplateRenderer = componentManager.getInstance(EmailTemplateRenderer.class);
        } catch (ComponentLookupException e) {
            throw new NotificationException("Failed to create a new instance of WikiEmailNotificationRenderer.", e);
        }
        this.htmlTemplate = extractTemplate(baseObject,
                WikiEmailNotificationRendererDocumentInitializer.HTML_TEMPLATE);
        this.plainTextTemplate = extractTemplate(baseObject,
                WikiEmailNotificationRendererDocumentInitializer.PLAIN_TEXT_TEMPLATE);
        this.emailSubjectTemplate = extractTemplate(baseObject,
                WikiEmailNotificationRendererDocumentInitializer.EMAIL_SUBJECT_TEMPLATE);
    }

    @Override
    public String renderHTML(CompositeEvent compositeEvent, String userId) throws NotificationException
    {
        if (this.htmlTemplate != null) {
            return emailTemplateRenderer.renderHTML(
                    emailTemplateRenderer.executeTemplate(compositeEvent, userId, this.htmlTemplate, Syntax.XHTML_1_0)
            );
        }
        // Fallback to the default renderer
        return null;
    }

    @Override
    public String renderPlainText(CompositeEvent compositeEvent, String userId) throws NotificationException
    {
        if (this.plainTextTemplate != null) {
            return emailTemplateRenderer.renderPlainText(
                    emailTemplateRenderer.executeTemplate(compositeEvent, userId, this.plainTextTemplate,
                            Syntax.PLAIN_1_0)
            );
        }
        // Fallback to the default renderer
        return null;
    }

    @Override
    public String generateEmailSubject(CompositeEvent compositeEvent, String userId)
            throws NotificationException
    {
        if (this.emailSubjectTemplate != null) {
            return emailTemplateRenderer.renderPlainText(
                    emailTemplateRenderer.executeTemplate(compositeEvent, userId, this.emailSubjectTemplate,
                            Syntax.PLAIN_1_0)
            );
        }
        // Fallback to the default renderer
        return null;
    }

    @Override
    public Type getRoleType()
    {
        return NotificationEmailRenderer.class;
    }
}
