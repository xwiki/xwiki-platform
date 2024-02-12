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
package org.xwiki.notifications.notifiers.email;

import java.util.Map;

import org.xwiki.component.annotation.Role;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.stability.Unstable;
import org.xwiki.template.Template;

/**
 * Helper for rendering template for email notifications.
 *
 * @version $Id$
 * @since 16.1.0RC1
 */
@Role
@Unstable
public interface EmailTemplateRenderer
{
    /**
     * Execute a template for email notification. Two bindings are provided by default: {@code event} is bound to the
     * given {@link CompositeEvent} and {@code emailUser} is bound to the given userId.
     *
     * @param event composite event to render
     * @param userId id of the user who will receive the email
     * @param template the template to use
     * @param syntax syntax of the template and of the output
     * @param customBindings the custom bindings to use in the template
     * @return the rendered template
     * @throws NotificationException if something wrong happens
     */
    Block executeTemplate(CompositeEvent event, String userId, Template template, Syntax syntax,
        Map<String, Object> customBindings) throws NotificationException;

    /**
     * Render a block to HTML syntax.
     *
     * @param block block to render
     * @return the HTML rendered version of the block
     */
    String renderHTML(Block block);

    /**
     * Render a block to plain text syntax.
     *
     * @param block block to render
     * @return the plain text rendered version of the block
     */
    String renderPlainText(Block block);
}
