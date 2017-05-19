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
package org.xwiki.notifications.internal.page;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationDisplayer;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.page.PageNotificationEventDescriptor;
import org.xwiki.notifications.page.PageNotificationEventDescriptorContainer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.template.TemplateManager;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.XWikiContext;

/**
 * Implement a {@link NotificationDisplayer} for the event type
 * {@link org.xwiki.notifications.page.PageNotificationEvent}.
 *
 * @version $Id$
 * @since 9.4RC1
 */
@Component
@Singleton
@Named(DefaultPageNotificationDisplayer.EVENT_TYPE)
public class DefaultPageNotificationDisplayer implements NotificationDisplayer
{
    /**
     * Event handled by the displayer.
     */
    public static final String EVENT_TYPE = "org.xwiki.notifications.page.PageNotificationEvent";

    private static final String EVENT_BINDING_NAME = "event";

    @Inject
    private PageNotificationEventDescriptorContainer pageNotificationEventDescriptorContainer;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private TemplateManager templateManager;

    @Inject
    private VelocityManager velocityManager;

    @Inject
    private AuthorExecutor authorExecutor;

    private class VelocityTemplateStringEvaluator implements Callable<String>
    {
        private String content;

        private Provider<XWikiContext> contextProvider;

        /**
         * Constructs a {@link VelocityTemplateStringEvaluator}.
         *
         * @param contextProvider a reference to the XWikiContext provider component
         * @param content the content of the template that will be evaluated
         */
        VelocityTemplateStringEvaluator(Provider<XWikiContext> contextProvider, String content)
        {
            this.contextProvider = contextProvider;
            this.content = content;
        }

        @Override
        public String call() throws Exception
        {
            return contextProvider.get().getWiki().evaluateVelocity(content, "page-notification");
        }
    }

    @Override
    public Block renderNotification(CompositeEvent eventNotification) throws NotificationException
    {
        try {
            velocityManager.getCurrentVelocityContext().put(EVENT_BINDING_NAME, eventNotification);

            PageNotificationEventDescriptor eventDescriptor =
                    pageNotificationEventDescriptorContainer.getDescriptorByType(
                            eventNotification.getEvents().get(0).getType());

            // If we have no template defined, use the default one
            if (eventDescriptor.getNotificationTemplate().isEmpty()) {
                return templateManager.execute("notification/default.vm");
            }

            return new RawBlock(executeTemplate(eventDescriptor.getAuthorReference(),
                    eventDescriptor.getNotificationTemplate()), Syntax.HTML_5_0);
        } catch (Exception e) {
            throw new NotificationException("Unable to render notification template.", e);
        } finally {
            velocityManager.getCurrentVelocityContext().remove(EVENT_BINDING_NAME);
        }
    }

    private String executeTemplate(DocumentReference userReference, String templateContent)
    {

        try {
            return this.authorExecutor.call(
                    new VelocityTemplateStringEvaluator(this.contextProvider, templateContent),
                    userReference);
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public List<String> getSupportedEvents()
    {
        List<String> supportedEvents = new ArrayList<>();

        for (PageNotificationEventDescriptor descriptor: pageNotificationEventDescriptorContainer.getDescriptorList()) {
            supportedEvents.add(descriptor.getEventType());
        }

        return supportedEvents;
    }
}
