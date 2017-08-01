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
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.notifiers.email.NotificationEmailRenderer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.internal.transformation.MutableRenderingContext;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.ExternalServletURLFactory;
import com.xpn.xwiki.web.XWikiURLFactory;

/**
 * Helper for NotificationEmailRenderer that use the Template Manager to render notifications.
 *
 * @version $Id$
 * @since 9.5RC1
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

    @Inject
    protected Provider<XWikiContext> contextProvider;

    @Inject
    protected RenderingContext renderingContext;

    /**
     * Execute a template.
     *
     * @param event composite event to render
     * @param templatePath path of the template to use (with a %s that the method will replace by the event type)
     * @param syntax syntax of the template and of the output
     * @return the rendered template
     * @throws NotificationException if something wrong happens
     */
    protected Block executeTemplate(CompositeEvent event, String templatePath, Syntax syntax)
            throws NotificationException
    {
        XWikiContext context = contextProvider.get();
        XWikiURLFactory originalURLFactory = context.getURLFactory();
        try {
            // Bind the event to some variable in the velocity context
            velocityManager.getCurrentVelocityContext().put(EVENT_BINDING_NAME, event);
            // Use the external URL factory to generate full URLs
            context.setURLFactory(new ExternalServletURLFactory(context));
            // Set the given syntax in the rendering context
            if (renderingContext instanceof MutableRenderingContext) {
                ((MutableRenderingContext) renderingContext).push(null, null, syntax, null,
                        false, syntax);
            }
            // Generate the full template name
            String templateName = String.format(templatePath, event.getType().replaceAll("\\/", "."));
            // Get the template
            Template template = templateManager.getTemplate(templateName);
            // Render the template or fallback to the default one
            return template != null ? templateManager.execute(template)
                    : templateManager.execute(String.format(templatePath, "default"));
        } catch (Exception e) {
            throw new NotificationException("Failed to render the notification.", e);
        } finally {
            // Cleaning the rendering context
            if (renderingContext instanceof MutableRenderingContext) {
                ((MutableRenderingContext) renderingContext).pop();
            }
            // Cleaning the URL factory
            context.setURLFactory(originalURLFactory);
            // Cleaning the velocity context
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
