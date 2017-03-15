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
package org.xwiki.platform.blog.internal;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.eventstream.Event;
import org.xwiki.notifications.NotificationDisplayer;
import org.xwiki.notifications.NotificationException;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.XWikiContext;

/**
 * @version $Id$
 */
@Component
@Singleton
@Named(BlogNotificationDisplayer.EVENT_TYPE)
public class BlogNotificationDisplayer implements NotificationDisplayer, Initializable
{
    public static final String EVENT_TYPE = "org.xwiki.platform.blog.events.BlogPostPublishedEvent";

    private static final List<String> EVENTS = Arrays.asList(EVENT_TYPE);

    private static final String TEMPLATE_NAME = String.format("notification/%s.vm", EVENT_TYPE);

    @Inject
    private TemplateManager templateManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private VelocityManager velocityManager;

    private String defaultTemplate;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            defaultTemplate = IOUtils.toString(getClass().getResourceAsStream("/templates/notification.vm"));
        } catch (IOException e) {
            throw new InitializationException("Failed to initialize the Blog Extension.", e);
        }
    }

    @Override
    public Block renderNotification(Event eventNotification) throws NotificationException
    {
        try {
            velocityManager.getCurrentVelocityContext().put("event", eventNotification);

            Template template = templateManager.getTemplate(TEMPLATE_NAME);
            if (template != null) {
                return templateManager.executeNoException(TEMPLATE_NAME);
            }

            return new RawBlock(executeDefaultTemplate(), Syntax.HTML_5_0);
        } finally {
            velocityManager.getCurrentVelocityContext().remove("event");
        }
    }

    private String executeDefaultTemplate()
    {
        return contextProvider.get().getWiki().evaluateVelocity(defaultTemplate, "blog-notification");
    }

    @Override
    public List<String> getSupportedEvents()
    {
        return EVENTS;
    }
}
