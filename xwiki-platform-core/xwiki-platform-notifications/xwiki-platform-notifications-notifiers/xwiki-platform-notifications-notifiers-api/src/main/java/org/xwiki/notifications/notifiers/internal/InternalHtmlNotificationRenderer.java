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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.notifiers.NotificationRenderer;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.util.ErrorBlockGenerator;

/**
 * Render {@link org.xwiki.notifications.CompositeEvent} to HTML5 by using the right displayers first.
 *
 * @since 12.2RC1
 * @version $Id$
 */
@Component(roles = InternalHtmlNotificationRenderer.class)
@Singleton
public class InternalHtmlNotificationRenderer
{
    @Inject
    private NotificationRenderer notificationRenderer;

    @Inject
    private ErrorBlockGenerator errorBlockGenerator;

    @Inject
    @Named("html/5.0")
    private BlockRenderer htmlRenderer;

    /**
     * Use the appropriate {@link NotificationRenderer} to render first the event to block, and then render it
     * to HTML5.
     * @param compositeEvent the event to render in HTML.
     * @return a string containing the HTML to display.
     * @throws Exception in case of error during some rendering operation.
     */
    public String render(CompositeEvent compositeEvent) throws Exception
    {
        WikiPrinter printer = new DefaultWikiPrinter();
        htmlRenderer.render(notificationRenderer.render(compositeEvent), printer);
        return printer.toString();
    }

    /**
     * Helper to render a list of composite events by using {@link #render(CompositeEvent)}.
     * In case of exception during the rendering an {@link ErrorBlockGenerator} is used to render the error.
     *
     * @param compositeEvents the events to render
     * @return a string contaning the HTML to display.
     */
    public String render(List<CompositeEvent> compositeEvents)
    {
        StringBuilder stringBuilder = new StringBuilder();
        for (CompositeEvent compositeEvent : compositeEvents) {
            try {
                stringBuilder.append(this.render(compositeEvent));
            } catch (Exception e) {
                WikiPrinter printer = new DefaultWikiPrinter();
                htmlRenderer.render(
                    errorBlockGenerator.generateErrorBlocks("Error while rendering notification", e, false), printer);
                stringBuilder.append(printer.toString());
            }
        }
        return stringBuilder.toString();
    }
}
