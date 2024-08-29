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
import java.util.Map;

import javax.inject.Inject;

import org.xwiki.component.annotation.Component;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.notifiers.email.EmailTemplateRenderer;
import org.xwiki.notifications.notifiers.email.NotificationEmailRenderer;
import org.xwiki.notifications.notifiers.internal.AbstractWikiNotificationRenderer;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.template.Template;

import com.xpn.xwiki.objects.BaseObject;

/**
 * This class is meant to be instanciated and then registered to the Component Manager by the
 * {@link WikiEmailNotificationRendererComponentBuilder} component every time a document containing a
 * NotificationEmailRendererClass is added, updated or deleted.
 *
 * @version $Id$
 * @since 9.11.1
 */
@Component(roles = WikiEmailNotificationRenderer.class)
public class WikiEmailNotificationRenderer extends AbstractWikiNotificationRenderer implements NotificationEmailRenderer
{
    @Inject
    private EmailTemplateRenderer emailTemplateRenderer;

    private Template htmlTemplate;

    private Template plainTextTemplate;

    private Template emailSubjectTemplate;

    /**
     * Constructs a new {@link WikiEmailNotificationRenderer}.
     *
     * @param baseObject the XObject which has the required properties to instantiate the component
     * @throws NotificationException if the properties of the given BaseObject could not be loaded
     */
    @Override
    public void initialize(BaseObject baseObject) throws NotificationException
    {
        super.initialize(baseObject);

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
                    emailTemplateRenderer.executeTemplate(compositeEvent, userId, this.htmlTemplate,
                        Syntax.XHTML_1_0, Map.of())
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
                        Syntax.PLAIN_1_0, Map.of())
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
                        Syntax.PLAIN_1_0, Map.of())
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
