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

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.sources.ParametrizedNotificationManager;
import org.xwiki.rendering.RenderingException;
import org.xwiki.rendering.async.internal.AsyncRenderer;
import org.xwiki.rendering.async.internal.AsyncRendererResult;

/**
 * An async implementation of Notification rendering.
 *
 * @since 12.2RC1
 * @version $Id$
 */
@Component(roles = DefaultAsyncNotificationRenderer.class)
@InstantiationStrategy(value = ComponentInstantiationStrategy.PER_LOOKUP)
public class DefaultAsyncNotificationRenderer implements AsyncRenderer
{
    private NotificationAsyncRendererConfiguration configuration;

    @Inject
    private DefaultNotificationCacheManager notificationCacheManager;

    @Inject
    private InternalHtmlNotificationRenderer htmlNotificationRenderer;

    @Inject
    private ParametrizedNotificationManager notificationManager;

    private String cacheKey;

    /**
     * Initialize the render with the given configuration.
     * The cache key will be immediately computed.
     * @param configuration the actual configuration to be used in that renderer.
     */
    public void initialize(NotificationAsyncRendererConfiguration configuration)
    {
        this.configuration = configuration;

        // Avoid to compute several times the cache key.
        this.cacheKey = this.notificationCacheManager.createCacheKey(configuration.getNotificationParameters());
    }

    @Override
    public List<String> getId()
    {
        String queryType;
        if (this.configuration.isCount()) {
            queryType = "count";
        } else {
            queryType = "display";
        }
        return Arrays.asList("notifications", queryType, this.cacheKey);
    }

    @Override
    public AsyncRendererResult render(boolean async, boolean cached) throws RenderingException
    {
        Object fromCache =
            this.notificationCacheManager.getFromCache(this.cacheKey, this.configuration.isCount());

        List<CompositeEvent> events = null;
        Integer count = null;
        if (fromCache == null) {
            try {
                events = this.notificationManager.getEvents(this.configuration.getNotificationParameters());
                count = events.size();
                this.notificationCacheManager.setInCache(this.cacheKey, events, this.configuration.isCount());
            } catch (NotificationException e) {
                throw new RenderingException("Error while retrieving the notification", e);
            }
        } else {
            if (this.configuration.isCount()) {
                count = (Integer) fromCache;
            } else {
                events = (List<CompositeEvent>) fromCache;
            }
        }
        String stringResult;
        if (this.configuration.isCount()) {
            String displayCount = String.valueOf(count);
            // if the count is actually max, we display it with a "+" to inform that we might have more.
            if (count >= this.configuration.getNotificationParameters().expectedCount) {
                displayCount = String.format("%s+", count);
            }
            stringResult = getCountResult(displayCount);
        } else {
            stringResult = this.htmlNotificationRenderer.render(events);
        }
        return new AsyncRendererResult(stringResult);
    }

    private String getCountResult(String count)
    {
        return String.format("<span class=\"notifications-count badge\">%s</span>", count);
    }

    /**
     * @return {@code true}: this implementation is dedicated to async.
     */
    @Override
    public boolean isAsyncAllowed()
    {
        return true;
    }

    /**
     * @return {@code false}: we don't use the async cache rendering mechanism.
     */
    @Override
    public boolean isCacheAllowed()
    {
        return false;
    }
}
