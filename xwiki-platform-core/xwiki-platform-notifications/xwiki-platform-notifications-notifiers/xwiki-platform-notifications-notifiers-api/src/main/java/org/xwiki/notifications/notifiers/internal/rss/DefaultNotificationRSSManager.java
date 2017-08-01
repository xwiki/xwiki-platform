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
package org.xwiki.notifications.notifiers.internal.rss;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;

import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.notifiers.internal.ModelBridge;
import org.xwiki.notifications.notifiers.rss.NotificationRSSManager;
import org.xwiki.notifications.notifiers.rss.NotificationRSSRenderer;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndFeedImpl;

/**
 * This is the default implementation of {@link NotificationRSSManager}.
 *
 * @version $Id$
 * @since 9.6RC1
 */
@Component
@Singleton
public class DefaultNotificationRSSManager implements NotificationRSSManager
{
    @Inject
    private ContextualLocalizationManager contextualLocalizationManager;

    @Inject
    private ModelBridge modelBridge;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private NotificationRSSRenderer defaultNotificationRSSRenderer;

    @Inject
    private Logger logger;

    @Override
    public SyndFeed renderFeed(List<CompositeEvent> events)
    {
        SyndFeed feed = new SyndFeedImpl();

        // Define the general properties of the rss
        feed.setFeedType("rss_2.0");
        feed.setTitle(this.contextualLocalizationManager.getTranslationPlain("notifications.rss.feedTitle"));

        // Set the RSS feed link to the service generating the feed
        feed.setLink(this.modelBridge.getDocumentURL(
                new DocumentReference(wikiDescriptorManager.getCurrentWikiId(),
                        Arrays.asList("XWiki", "Notifications", "Code"), "NotificationRSSService"),
                "get", "outputSyntax=plain"));

        // Set the feed description
        feed.setDescription(this.contextualLocalizationManager.getTranslationPlain(
                "notifications.rss.feedDescription"));

        // Add every given CompositeEvent entry to the rss
        List<SyndEntry> entries = new ArrayList<>();
        for (CompositeEvent event : events) {
            try {
                NotificationRSSRenderer renderer = this.getRenderer(event);
                if (renderer != null) {
                    entries.add(renderer.renderNotification(event));
                } else {
                    entries.add(defaultNotificationRSSRenderer.renderNotification(event));
                }
            } catch (NotificationException e) {
                this.logger.warn("Unable to render RSS entry for CompositeEvent [{}] : [{}]",
                                event, ExceptionUtils.getRootCauseMessage(e));
            }
        }
        feed.setEntries(entries);

        return feed;
    }

    /**
     * Get the renderer that is registered in the Component Manager and that corresponds to the given event type.
     * @param event the event to use
     * @return the corresponding {@link NotificationRSSRenderer} or null if no renderer could be found
     */
    protected NotificationRSSRenderer getRenderer(CompositeEvent event)
    {
        if (this.componentManager.hasComponent(NotificationRSSRenderer.class, event.getType())) {
            try {
                return this.componentManager.getInstance(NotificationRSSRenderer.class, event.getType());
            } catch (ComponentLookupException e) {
                    /* If we get this exception, then it means that the component manager is broken as we’ve already
                       checked if an instance of a component with the given hint is available. */
                this.logger.error("Unable to fetch NotificationRSSRenderer component with hint [{}]",
                        event.getType(), e);
            }
        }
        return null;
    }
}
