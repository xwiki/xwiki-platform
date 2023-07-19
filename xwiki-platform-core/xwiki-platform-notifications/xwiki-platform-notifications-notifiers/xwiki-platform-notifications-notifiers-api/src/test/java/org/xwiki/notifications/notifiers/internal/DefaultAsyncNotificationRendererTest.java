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

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.job.GroupedJobInitializer;
import org.xwiki.job.JobGroupPath;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.CompositeEventStatus;
import org.xwiki.notifications.CompositeEventStatusManager;
import org.xwiki.notifications.sources.NotificationParameters;
import org.xwiki.notifications.sources.ParametrizedNotificationManager;
import org.xwiki.rendering.async.internal.AsyncRendererResult;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultAsyncNotificationRenderer}.
 */
@ComponentTest
class DefaultAsyncNotificationRendererTest
{
    private static final String CACHE_KEY = "mykey";
    private static final DocumentReference USER_DOC_REFERENCE = new DocumentReference("xwiki", "XWiki", "Foo");
    private static final String USER_SERIALIZED_REFERENCE = "xwiki:XWiki.Foo";

    @InjectMockComponents
    private DefaultAsyncNotificationRenderer asyncNotificationRenderer;

    @MockComponent
    private DefaultNotificationCacheManager notificationCacheManager;

    @MockComponent
    private InternalHtmlNotificationRenderer htmlNotificationRenderer;

    @MockComponent
    private CompositeEventStatusManager compositeEventStatusManager;

    @MockComponent
    private ParametrizedNotificationManager notificationManager;

    @MockComponent
    private EntityReferenceSerializer<String> documentReferenceSerializer;

    @MockComponent
    @Named("AsyncNotificationRenderer")
    private GroupedJobInitializer jobInitializer;

    private NotificationParameters notificationParameters;

    @BeforeEach
    public void setup()
    {
        this.notificationParameters = new NotificationParameters();
        this.notificationParameters.user = USER_DOC_REFERENCE;
        when(notificationCacheManager.createCacheKey(notificationParameters)).thenReturn(CACHE_KEY);
        when(this.documentReferenceSerializer.serialize(USER_DOC_REFERENCE)).thenReturn(USER_SERIALIZED_REFERENCE);
    }

    @Test
    void initialize()
    {
        this.asyncNotificationRenderer.initialize(
            new NotificationAsyncRendererConfiguration(notificationParameters, false));
        verify(notificationCacheManager).createCacheKey(notificationParameters);
    }

    @Test
    void getId()
    {
        this.asyncNotificationRenderer.initialize(
            new NotificationAsyncRendererConfiguration(notificationParameters, false));
        assertEquals(Arrays.asList("notifications", "display", CACHE_KEY), this.asyncNotificationRenderer.getId());

        this.asyncNotificationRenderer.initialize(
            new NotificationAsyncRendererConfiguration(notificationParameters, true));
        assertEquals(Arrays.asList("notifications", "count", CACHE_KEY), this.asyncNotificationRenderer.getId());
    }

    @Test
    void render() throws Exception
    {
        List<CompositeEvent> compositeEventList = List.of(
            mock(CompositeEvent.class),
            mock(CompositeEvent.class)
        );
        when(this.notificationManager.getEvents(notificationParameters)).thenReturn(compositeEventList);

        List<CompositeEventStatus> compositeEventStatusList = Arrays.asList(
            mock(CompositeEventStatus.class),
            mock(CompositeEventStatus.class)
        );
        when(this.compositeEventStatusManager.getCompositeEventStatuses(compositeEventList, USER_SERIALIZED_REFERENCE))
            .thenReturn(compositeEventStatusList);
        notificationParameters.expectedCount = 2;
        when(this.htmlNotificationRenderer.render(compositeEventList, compositeEventStatusList, true))
            .thenReturn("Expected result!");

        // Test1: get 2 events without cache
        this.asyncNotificationRenderer.initialize(
            new NotificationAsyncRendererConfiguration(notificationParameters, false));
        assertEquals(new AsyncRendererResult("Expected result!"), this.asyncNotificationRenderer.render(false, false));
        verify(this.notificationCacheManager).getFromCache(CACHE_KEY, false, true);
        verify(this.notificationCacheManager).setInCache(CACHE_KEY, new ArrayList<>(compositeEventList), false, true);

        // Test2: get count of 2 events, without cache
        when(this.htmlNotificationRenderer.render(2)).thenReturn("Expected count result!");
        this.asyncNotificationRenderer.initialize(
            new NotificationAsyncRendererConfiguration(notificationParameters, true));
        assertEquals(new AsyncRendererResult("Expected count result!"),
            this.asyncNotificationRenderer.render(true, true));
        verify(this.notificationCacheManager).getFromCache(CACHE_KEY, true, true);
        verify(this.notificationCacheManager).setInCache(CACHE_KEY, new ArrayList<>(compositeEventList), true, true);

        // Test3: get 1 event with cache
        compositeEventList = List.of(
            mock(CompositeEvent.class)
        );

        compositeEventStatusList = Arrays.asList(
            mock(CompositeEventStatus.class)
        );
        when(this.compositeEventStatusManager.getCompositeEventStatuses(compositeEventList, USER_SERIALIZED_REFERENCE))
            .thenReturn(compositeEventStatusList);
        when(this.notificationCacheManager.getFromCache(CACHE_KEY, false, true)).thenReturn(compositeEventList);

        when(this.htmlNotificationRenderer.render(compositeEventList, compositeEventStatusList, false))
            .thenReturn("Expected cache result!");
        this.asyncNotificationRenderer.initialize(
            new NotificationAsyncRendererConfiguration(notificationParameters, false));
        assertEquals(new AsyncRendererResult("Expected cache result!"),
            this.asyncNotificationRenderer.render(true, false));
        verify(this.notificationCacheManager, never()).setInCache(CACHE_KEY,
            new ArrayList<>(compositeEventList), false, true);

        when(this.notificationCacheManager.getFromCache(CACHE_KEY, true, true)).thenReturn(1);
        when(this.htmlNotificationRenderer.render(1))
            .thenReturn("Expected count cache result!");
        this.asyncNotificationRenderer.initialize(
            new NotificationAsyncRendererConfiguration(notificationParameters, true));
        assertEquals(new AsyncRendererResult("Expected count cache result!"),
            this.asyncNotificationRenderer.render(false, true));
        verify(this.notificationCacheManager, never()).setInCache(CACHE_KEY,
            new ArrayList<>(compositeEventList), false, true);
    }

    @Test
    void renderUserNull() throws Exception
    {
        notificationParameters.user = null;
        List<CompositeEvent> compositeEventList = List.of(
            mock(CompositeEvent.class),
            mock(CompositeEvent.class)
        );

        when(this.notificationManager.getEvents(notificationParameters)).thenReturn(compositeEventList);
        notificationParameters.expectedCount = 2;
        when(this.htmlNotificationRenderer.render(compositeEventList, null, true))
            .thenReturn("Expected result!");
        this.asyncNotificationRenderer.initialize(
            new NotificationAsyncRendererConfiguration(notificationParameters, false));
        assertEquals(new AsyncRendererResult("Expected result!"), this.asyncNotificationRenderer.render(false, false));
        verify(this.notificationCacheManager).getFromCache(CACHE_KEY, false, true);
        verify(this.notificationCacheManager).setInCache(CACHE_KEY, new ArrayList<>(compositeEventList), false, true);
        verify(this.compositeEventStatusManager, never()).getCompositeEventStatuses(any(), any());
    }

    @Test
    void getJobGroupPath()
    {
        JobGroupPath jobGroupPath = new JobGroupPath(Arrays.asList("Something", "Foo", "Bar"));
        when(this.jobInitializer.getId()).thenReturn(jobGroupPath);
        assertEquals(jobGroupPath, this.asyncNotificationRenderer.getJobGroupPath());
    }
}
