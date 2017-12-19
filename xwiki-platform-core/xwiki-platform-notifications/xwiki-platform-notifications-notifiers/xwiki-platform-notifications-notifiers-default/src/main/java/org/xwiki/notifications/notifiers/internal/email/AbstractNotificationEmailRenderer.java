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

import javax.inject.Inject;

import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.notifiers.email.NotificationEmailRenderer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;

/**
 * Helper for NotificationEmailRenderer that use the Template Manager to render notifications.
 *
 * @version $Id$
 * @since 9.5RC1
 */
public abstract class AbstractNotificationEmailRenderer implements NotificationEmailRenderer
{
    @Inject
    protected EmailTemplateRenderer emailTemplateRenderer;

    @Inject
    protected TemplateManager templateManager;

    /**
     * Execute a template.
     *
     * @param event composite event to render
     * @param userId id of the user who will receive the email
     * @param templatePath path of the template to use (with a %s that the method will replace by the event type)
     * @param syntax syntax of the template and of the output
     * @return the rendered template
     * @throws NotificationException if something wrong happens
     */
    protected Block executeTemplate(CompositeEvent event, String userId, String templatePath, Syntax syntax)
            throws NotificationException
    {
        // Generate the full template name
        String templateName = String.format(templatePath, event.getType().replaceAll("\\/", "."));
        // Get the template
        Template template = templateManager.getTemplate(templateName);
        if (template == null) {
            template = templateManager.getTemplate(String.format(templatePath, "default"));
        }
        return emailTemplateRenderer.executeTemplate(event, userId, template, syntax);
    }

    protected String renderHTML(Block block)
    {
        return emailTemplateRenderer.renderHTML(block);
    }

    protected String renderPlainText(Block block)
    {
        return emailTemplateRenderer.renderPlainText(block);
    }

}
