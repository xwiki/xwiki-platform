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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.job.GroupedJobInitializer;
import org.xwiki.job.JobGroupPath;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.CompositeEventStatus;
import org.xwiki.notifications.CompositeEventStatusManager;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.sources.NotificationParameters;
import org.xwiki.notifications.sources.ParametrizedNotificationManager;
import org.xwiki.rendering.RenderingException;
import org.xwiki.rendering.async.internal.AsyncRenderer;
import org.xwiki.rendering.async.internal.AsyncRendererResult;

/**
 * An async implementation of Notification rendering.
 *
 * @since 12.2
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
    private CompositeEventStatusManager compositeEventStatusManager;

    @Inject
    private ParametrizedNotificationManager notificationManager;

    @Inject
    private EntityReferenceSerializer<String> documentReferenceSerializer;

    @Inject
    @Named("AsyncNotificationRenderer")
    private GroupedJobInitializer jobInitializer;

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
        // We need to replace the / from the cachekey so it won't appear encoded in the URL
        // since it not necessarily supported, in particular for Tomcat standard configuration.
        return Arrays.asList("notifications", queryType, this.cacheKey.replaceAll("/", "_"));
    }

    @Override
    public AsyncRendererResult render(boolean async, boolean cached) throws RenderingException
    {
        Object fromCache =
            this.notificationCacheManager.getFromCache(this.cacheKey, this.configuration.isCount(), true);

        NotificationParameters notificationParameters = this.configuration.getNotificationParameters();
        List<CompositeEvent> events = null;
        Integer count = null;
        if (fromCache == null) {
            try {
                events = this.notificationManager.getEvents(notificationParameters);
                count = events.size();
                this.notificationCacheManager.setInCache(this.cacheKey, new ArrayList<>(events),
                    this.configuration.isCount(),
                    true);
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
            stringResult = this.htmlNotificationRenderer.render(count);
        } else {
            try {
                boolean loadMore = events.size() == notificationParameters.expectedCount;
                List<CompositeEventStatus> compositeEventStatuses = null;
                // We cannot compute the read status if the user is not available.
                String userId = null;
                if (notificationParameters.user != null) {
                    userId = documentReferenceSerializer.serialize(notificationParameters.user);
                }

                if (userId != null) {
                    compositeEventStatuses =
                        this.compositeEventStatusManager.getCompositeEventStatuses(events, userId);
                }
                stringResult = this.htmlNotificationRenderer.render(events, compositeEventStatuses, loadMore);
            } catch (Exception e) {
                throw new RenderingException("Error while retrieving the event status", e);
            }

        }
        return new AsyncRendererResult(stringResult);
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

    @Override
    public JobGroupPath getJobGroupPath()
    {
        return this.jobInitializer.getId();
    }
}
