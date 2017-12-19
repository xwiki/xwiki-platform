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
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
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
 * Helper to render email notifications templates.
 *
 * @version $Id$
 * @since 9.11.1
 * @since 10.0RC1
 */
@Component(roles = EmailTemplateRenderer.class)
@Singleton
public class EmailTemplateRenderer
{
    private static final String EVENT_BINDING_NAME = "event";

    private static final String USER_BINDING_NAME = "emailUser";

    @Inject
    // In 2017, it's safer to use XHTML 1.0 for emails because the emails clients have a very unequal HTML support
    @Named("xhtml/1.0")
    private BlockRenderer htmlBlockRenderer;

    @Inject
    @Named("plain/1.0")
    private BlockRenderer plainTextBlockRenderer;

    @Inject
    private TemplateManager templateManager;

    @Inject
    private VelocityManager velocityManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private RenderingContext renderingContext;

    /**
     * Execute a template.
     *
     * @param event composite event to render
     * @param userId id of the user who will receive the email
     * @param template the template to use
     * @param syntax syntax of the template and of the output
     * @return the rendered template
     * @throws NotificationException if something wrong happens
     */
    public Block executeTemplate(CompositeEvent event, String userId, Template template, Syntax syntax)
            throws NotificationException
    {
        XWikiContext context = contextProvider.get();
        XWikiURLFactory originalURLFactory = context.getURLFactory();
        try {
            // Bind the event to some variable in the velocity context
            velocityManager.getCurrentVelocityContext().put(EVENT_BINDING_NAME, event);
            velocityManager.getCurrentVelocityContext().put(USER_BINDING_NAME, userId);
            // Use the external URL factory to generate full URLs
            context.setURLFactory(new ExternalServletURLFactory(context));
            // Set the given syntax in the rendering context
            if (renderingContext instanceof MutableRenderingContext) {
                ((MutableRenderingContext) renderingContext).push(null, null, syntax, null,
                        false, syntax);
            }
            // Render the template or fallback to the default one
            return templateManager.execute(template);
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
            velocityManager.getCurrentVelocityContext().remove(USER_BINDING_NAME);
        }
    }

    /**
     * Render a block to HTML syntax.
     * @param block block to render
     * @return the HTML rendered version of the block
     */
    public String renderHTML(Block block)
    {
        WikiPrinter printer = new DefaultWikiPrinter();
        htmlBlockRenderer.render(block, printer);
        return printer.toString();
    }

    /**
     * Render a block to plain text syntax.
     * @param block block to render
     * @return the plain text rendered version of the block
     */
    public String renderPlainText(Block block)
    {
        // TODO: this does not work at all (templates enforce HTML syntax I guess)
        WikiPrinter printer = new DefaultWikiPrinter();
        plainTextBlockRenderer.render(block, printer);
        return printer.toString();
    }
}
