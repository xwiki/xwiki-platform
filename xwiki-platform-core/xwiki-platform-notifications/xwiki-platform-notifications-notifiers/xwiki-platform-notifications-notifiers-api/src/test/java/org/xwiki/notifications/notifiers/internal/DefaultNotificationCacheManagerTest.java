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
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.LRUCacheConfiguration;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.NotificationConfiguration;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.sources.NotificationParameters;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultNotificationCacheManager}.
 *
 * @version $Id$
 */
@ComponentTest
public class DefaultNotificationCacheManagerTest
{
    @InjectMockComponents
    private DefaultNotificationCacheManager defaultNotificationCacheManager;

    @MockComponent
    private NotificationConfiguration configuration;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    private Cache longEventCache;
    private Cache longCountCache;
    private Cache longCompositeEventCountCache;
    private Cache longCompositeEventCache;

    @BeforeComponent
    public void setupComponents(MockitoComponentManager componentManager) throws Exception
    {
        this.longEventCache = mock(Cache.class);
        this.longCountCache = mock(Cache.class);
        this.longCompositeEventCountCache = mock(Cache.class);
        this.longCompositeEventCache = mock(Cache.class);

        when(this.configuration.isRestCacheEnabled()).thenReturn(true);
        CacheManager cacheManager = componentManager.registerMockComponent(CacheManager.class);

        when(cacheManager.createNewCache(any())).thenAnswer(invocationOnMock -> {
            LRUCacheConfiguration lruCacheConfiguration =  (LRUCacheConfiguration) invocationOnMock.getArguments()[0];
            if ("notification.rest.longCache.events".equals(lruCacheConfiguration.getConfigurationId())) {
                return longEventCache;
            } else if ("notification.rest.longCache.count".equals(lruCacheConfiguration.getConfigurationId())) {
                return longCountCache;
            } else if ("notification.rest.longCache.events.composite"
                .equals(lruCacheConfiguration.getConfigurationId())) {
                return longCompositeEventCache;
            } else if ("notification.rest.longCache.count.composite"
                .equals(lruCacheConfiguration.getConfigurationId())) {
                return longCompositeEventCountCache;
            }
            return null;
        });
    }

    @Test
    public void getFromCache()
    {
        this.defaultNotificationCacheManager.getFromCache("anykey", true, false);
        verify(this.longCountCache).get("anykey");
        verify(this.longEventCache, never()).get("anykey");

        this.defaultNotificationCacheManager.getFromCache("anotherkey", false, false);
        verify(this.longEventCache).get("anotherkey");
        verify(this.longCountCache, never()).get("anotherkey");

        // 2 for the method getFromCache + 1 for the initialize
        verify(this.configuration, times(3)).isRestCacheEnabled();
    }

    @Test
    public void setInCache()
    {
        List<Object> events = Arrays.asList(mock(Event.class), mock(Event.class),
            mock(Event.class));
        this.defaultNotificationCacheManager.setInCache("mykey", events, false, false);
        verify(this.longEventCache).set("mykey", events);
        verify(this.longCountCache, never()).set("mykey", events);

        this.defaultNotificationCacheManager.setInCache("anotherkey", events, true, false);
        verify(this.longEventCache, never()).set("anotherkey", 3);
        verify(this.longCountCache).set("anotherkey", 3);

        // 2 for the method setInCache + 1 for the initialize
        verify(this.configuration, times(3)).isRestCacheEnabled();
    }

    @Test
    public void createCacheKey()
    {
        DocumentReference userReference = new DocumentReference("xwiki", "XWiki", "Foobar");
        when(this.entityReferenceSerializer.serialize(userReference)).thenReturn("xwiki:XWiki.Foobar");

        DocumentReference userReference2 = new DocumentReference("xwiki", "XWiki", "Another");
        when(this.entityReferenceSerializer.serialize(userReference2)).thenReturn("xwiki:XWiki.another");

        NotificationParameters notificationParameters = new NotificationParameters();
        notificationParameters.format = NotificationFormat.ALERT;
        notificationParameters.endDate = new Date(42);
        notificationParameters.blackList = Collections.singletonList("foo");
        notificationParameters.fromDate = new Date();
        notificationParameters.filters = Collections.singletonList(mock(NotificationFilter.class));
        notificationParameters.user = userReference;
        notificationParameters.expectedCount = 22;
        notificationParameters.onlyUnread = true;

        int hashCode = notificationParameters.hashCode();
        assertEquals("5ALERT/18xwiki:XWiki.Foobar/42/true/22/true/" + hashCode,
            this.defaultNotificationCacheManager.createCacheKey(notificationParameters));

        NotificationParameters notificationParameters2 = new NotificationParameters();
        notificationParameters2.format = NotificationFormat.ALERT;
        notificationParameters2.endDate = new Date(42);
        notificationParameters2.user = userReference;
        notificationParameters2.expectedCount = 22;
        notificationParameters2.onlyUnread = true;

        hashCode = notificationParameters2.hashCode();
        assertEquals("5ALERT/18xwiki:XWiki.Foobar/42/true/22/true/" + hashCode,
            this.defaultNotificationCacheManager.createCacheKey(notificationParameters2));

        assertNotEquals(notificationParameters, notificationParameters2);
        assertNotEquals(this.defaultNotificationCacheManager.createCacheKey(notificationParameters),
            this.defaultNotificationCacheManager.createCacheKey(notificationParameters2));

        notificationParameters = new NotificationParameters();
        notificationParameters.format = NotificationFormat.EMAIL;
        notificationParameters.endDate = new Date(84);
        notificationParameters.user = userReference2;
        notificationParameters.expectedCount = 444;
        notificationParameters.onlyUnread = false;

        hashCode = notificationParameters.hashCode();
        assertEquals("5EMAIL/19xwiki:XWiki.another/84/true/444/false/" + hashCode,
            this.defaultNotificationCacheManager.createCacheKey(notificationParameters));

        notificationParameters = new NotificationParameters();
        notificationParameters.format = NotificationFormat.EMAIL;
        notificationParameters.endDate = new Date(84);
        notificationParameters.endDateIncluded = false;
        notificationParameters.user = userReference2;
        notificationParameters.expectedCount = 444;
        notificationParameters.onlyUnread = false;

        hashCode = notificationParameters.hashCode();
        assertEquals("5EMAIL/19xwiki:XWiki.another/84/false/444/false/" + hashCode,
            this.defaultNotificationCacheManager.createCacheKey(notificationParameters));

        notificationParameters = new NotificationParameters();
        notificationParameters.format = NotificationFormat.EMAIL;
        notificationParameters.user = userReference2;
        notificationParameters.expectedCount = 444;
        notificationParameters.onlyUnread = false;

        hashCode = notificationParameters.hashCode();
        assertEquals("5EMAIL/19xwiki:XWiki.another/444/false/" + hashCode,
            this.defaultNotificationCacheManager.createCacheKey(notificationParameters));

        notificationParameters = new NotificationParameters();
        notificationParameters.format = NotificationFormat.EMAIL;
        notificationParameters.user = userReference2;

        hashCode = notificationParameters.hashCode();
        assertEquals("5EMAIL/19xwiki:XWiki.another/0/null/" + hashCode,
            this.defaultNotificationCacheManager.createCacheKey(notificationParameters));
    }
}
