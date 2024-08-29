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
package org.xwiki.notifications.notifiers.script;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.job.JobException;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.notifiers.NotificationRenderer;
import org.xwiki.notifications.notifiers.internal.DefaultAsyncNotificationRenderer;
import org.xwiki.notifications.notifiers.internal.NotificationAsyncRendererConfiguration;
import org.xwiki.notifications.notifiers.rss.NotificationRSSManager;
import org.xwiki.notifications.sources.NotificationParameters;
import org.xwiki.notifications.sources.ParametrizedNotificationManager;
import org.xwiki.notifications.sources.internal.DefaultNotificationParametersFactory;
import org.xwiki.rendering.RenderingException;
import org.xwiki.rendering.async.internal.AsyncRendererExecutor;
import org.xwiki.rendering.async.internal.AsyncRendererExecutorResponse;
import org.xwiki.rendering.async.internal.AsyncRendererResult;
import org.xwiki.rendering.block.Block;
import org.xwiki.script.service.ScriptService;

import com.rometools.rome.io.SyndFeedOutput;

/**
 * Script service for the notification notifiers.
 *
 * @since 9.7RC1
 * @version $Id$
 */
@Component
@Named("notification.notifiers")
@Singleton
public class NotificationNotifiersScriptService implements ScriptService
{
    @Inject
    private NotificationRenderer notificationRenderer;

    @Inject
    private NotificationRSSManager notificationRSSManager;

    @Inject
    private ParametrizedNotificationManager notificationManager;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private AsyncRendererExecutor asyncRendererExecutor;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManager;

    @Inject
    private DefaultNotificationParametersFactory parametersFactory;

    /**
     * Generate a rendering Block for a given event to display as notification.
     * @param event the event to render
     * @return a rendering block ready to display the event
     *
     * @throws NotificationException if an error happens
     */
    public Block render(CompositeEvent event) throws NotificationException
    {
        return notificationRenderer.render(event);
    }

    /**
     * Get the RSS notifications feed of the given user.
     *
     * @param entryNumber number of entries to get
     * @return the notifications RSS feed
     * @throws NotificationException if an error occurs
     *
     * @since 10.1RC1
     */
    public String getFeed(int entryNumber) throws NotificationException
    {
        String userId = entityReferenceSerializer.serialize(documentAccessBridge.getCurrentUserReference());
        return this.getFeed(userId, entryNumber);
    }

    /**
     * Get the RSS notifications feed of the given user.
     *
     * @param userId id of the user
     * @param entryNumber number of entries to get
     * @return the notifications RSS feed
     * @throws NotificationException if an error occurs
     *
     * @since 10.1RC1
     */
    public String getFeed(String userId, int entryNumber) throws NotificationException
    {
        SyndFeedOutput output = new SyndFeedOutput();
        NotificationParameters parametersForUserAndCount =
            this.parametersFactory.getParametersForUserAndCount(userId, entryNumber);
        List<CompositeEvent> events = this.notificationManager.getEvents(parametersForUserAndCount);
        try {
            return output.outputString(this.notificationRSSManager.renderFeed(events));
        } catch (Exception e) {
            throw new NotificationException("Unable to render RSS feed", e);
        }
    }

    /**
     * Compute the HTML fragment for the async placehoder.
     * @param response the async response containing ID informations for the placeholder
     * @param inline if {@code true} returns a span else a div.
     * @return a string containing the HTML for the placeholder.
     */
    private String computeAsyncPlaceholder(AsyncRendererExecutorResponse response, boolean inline)
    {
        String commonPart = String.format("class=\"xwiki-async\" data-xwiki-async-id=\"%s\" "
            + "data-xwiki-async-client-id=\"%s\"", response.getJobIdHTTPPath(), response.getAsyncClientId());

        String result;
        if (inline) {
            result = String.format("<span %s></span>", commonPart);
        } else {
            result = String.format("<div %s></div>", commonPart);
        }
        return result;
    }

    /**
     * Request asynchronously the notifications for the given parameters to retrieve their number.
     * This will return a piece of HTML with a placeholder for the asynchronous answer, which will be automatically
     * filled with the number once the request is done.
     * @param parameters the {@link NotificationParameters} used to make the request.
     *                   You can retrieve it by using
     * @param forcePlaceHolder {@code true} if the script service should always return a placeholder, even when the data
     *                          is immediately available (useful in case of AJAX request).
     * {@link org.xwiki.notifications.sources.script.NotificationSourcesScriptService#getNotificationParameters(Map)}.
     * @return the HTML of an asynchronous placeholder.
     * @throws NotificationException in case of error during the request.
     * @since 12.2
     */
    public String getNotificationCount(NotificationParameters parameters, boolean forcePlaceHolder)
        throws NotificationException
    {
        NotificationAsyncRendererConfiguration configuration =
            new NotificationAsyncRendererConfiguration(parameters, true);

        configuration.setPlaceHolderForced(forcePlaceHolder);
        return this.getAsyncNotification(configuration);
    }

    /**
     * Request asynchronously the notifications for the given parameters to display them.
     * This will return a piece of HTML with a placeholder for the asynchronous answer, which will be automatically
     * filled with the notifications once the request is done.
     * @param parameters the {@link NotificationParameters} used to make the request.
     *                   You can retrieve it by using
     * @param forcePlaceHolder {@code true} if the script service should always return a placeholder, even when the data
     *                         is immediately available (useful in case of AJAX request).
     * {@link org.xwiki.notifications.sources.script.NotificationSourcesScriptService#getNotificationParameters(Map)}.
     * @return the HTML of an asynchronous placeholder.
     * @throws NotificationException in case of error during the request.
     * @since 12.2
     */
    public String getNotifications(NotificationParameters parameters, boolean forcePlaceHolder)
        throws NotificationException
    {
        NotificationAsyncRendererConfiguration configuration =
            new NotificationAsyncRendererConfiguration(parameters, false);

        configuration.setPlaceHolderForced(forcePlaceHolder);
        return this.getAsyncNotification(configuration);
    }

    /**
     * Common code to perform the asynchronous request for
     * {@link #getNotificationCount(NotificationParameters, boolean)} and
     * {@link #getNotifications(NotificationParameters, boolean)}.
     * @param configuration the actual asynchronous configuration to use for the request.
     * @return the HTML of an asynchronous placeholder.
     * @throws NotificationException in case of error during the request.
     */
    private String getAsyncNotification(NotificationAsyncRendererConfiguration configuration)
        throws NotificationException
    {
        verifyParameterMaxCount(configuration.getNotificationParameters());
        try {
            DefaultAsyncNotificationRenderer asyncNotificationRenderer =
                this.componentManager.get().getInstance(DefaultAsyncNotificationRenderer.class);
            asyncNotificationRenderer.initialize(configuration);
            AsyncRendererExecutorResponse response =
                this.asyncRendererExecutor.render(asyncNotificationRenderer, configuration);
            AsyncRendererResult result = response.getStatus().getResult();
            if (result != null && !configuration.isPlaceHolderForced()) {
                return result.getResult();
            } else {
                return computeAsyncPlaceholder(response, configuration.isCount());
            }
        } catch (ComponentLookupException | JobException | RenderingException e) {
            throw new NotificationException("Unable to retrieve notifications", e);
        }
    }

    private void verifyParameterMaxCount(NotificationParameters parameters)
    {
        if (parameters.expectedCount == -1) {
            parameters.expectedCount = 20;
        }
    }
}
