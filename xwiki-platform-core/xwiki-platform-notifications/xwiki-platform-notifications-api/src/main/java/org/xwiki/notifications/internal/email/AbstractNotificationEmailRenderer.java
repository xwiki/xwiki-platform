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
package org.xwiki.notifications.internal.email;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.email.NotificationEmailRenderer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;
import org.xwiki.velocity.VelocityManager;

/**
 * @version $Id$
 */
public abstract class AbstractNotificationEmailRenderer implements NotificationEmailRenderer
{
    protected static final String EVENT_BINDING_NAME = "event";

    @Inject
    // In 2017, it's safer to use XHTML 1.0 for emails because the emails clients have a very unequal HTML support
    @Named("xhtml/1.0")
    protected BlockRenderer htmlBlockRenderer;

    @Inject
    @Named("plain/1.0")
    protected BlockRenderer plainTextBlockRenderer;

    @Inject
    protected TemplateManager templateManager;

    @Inject
    protected VelocityManager velocityManager;

    protected Block executeTemplate(CompositeEvent event, String templatePath) throws NotificationException
    {
        try {
            velocityManager.getCurrentVelocityContext().put(EVENT_BINDING_NAME, event);

            String templateName = String.format(templatePath, event.getType().replaceAll("\\/", "."));
            Template template = templateManager.getTemplate(templateName);
            return template != null ? templateManager.execute(template)
                    : templateManager.execute(String.format(templatePath, "default"));
        } catch (Exception e) {
            throw new NotificationException("Failed to render the notification.", e);
        } finally {
            velocityManager.getCurrentVelocityContext().remove(EVENT_BINDING_NAME);
        }
    }

    protected String renderHTML(Block block)
    {
        WikiPrinter printer = new DefaultWikiPrinter();
        htmlBlockRenderer.render(block, printer);
        return printer.toString();
    }

    protected String renderPlainText(Block block)
    {
        // TODO: this does not work at all (templates enforce HTML syntax I guess)
        WikiPrinter printer = new DefaultWikiPrinter();
        plainTextBlockRenderer.render(block, printer);
        return printer.toString();
    }

}
