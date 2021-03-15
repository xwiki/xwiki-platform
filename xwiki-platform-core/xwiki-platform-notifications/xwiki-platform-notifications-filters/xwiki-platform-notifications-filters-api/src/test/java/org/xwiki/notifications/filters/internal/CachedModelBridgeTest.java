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
package org.xwiki.notifications.filters.internal;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 * @since 10.5RC1
 * @since 10.4
 * @since 9.11.5
 */
@ComponentTest
public class CachedModelBridgeTest
{
    @InjectMockComponents
    private CachedModelBridge cachedModelBridge;

    @MockComponent
    private ModelBridge modelBridge;

    private Cache cache;

    private Map<String, Object> executionContextProperties;

    private final DocumentReference user = new DocumentReference("xwiki", "XWiki", "User");
    private static final String SERIALIZED_USER = "xwiki:XWiki.User";

    private final WikiReference wikiReference = new WikiReference("foo");
    private final String SERIALIZED_WIKI = "foo";

    @BeforeComponent
    void beforeComponent(MockitoComponentManager componentManager) throws Exception
    {
        Execution execution = componentManager.registerMockComponent(Execution.class);
        EntityReferenceSerializer<String> serializer = componentManager
            .registerMockComponent(EntityReferenceSerializer.TYPE_STRING);
        CacheManager cacheManager = componentManager.registerMockComponent(CacheManager.class);
        CacheFactory factory = mock(CacheFactory.class);
        when(cacheManager.getCacheFactory()).thenReturn(factory);
        this.cache = mock(Cache.class);
        when(factory.newCache(any(CacheConfiguration.class))).thenReturn(cache);
        when(serializer.serialize(user)).thenReturn(SERIALIZED_USER);
        when(serializer.serialize(wikiReference)).thenReturn(SERIALIZED_WIKI);

        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(execution.getContext()).thenReturn(executionContext);
        this.executionContextProperties = new HashMap<>();
        when(executionContext.getProperties()).thenReturn(executionContextProperties);
        executionContextProperties.put("property1", new Object());
        executionContextProperties.put("userToggleableFilterPreference_property3", new Object());
        doAnswer(invocationOnMock -> {
            String property = invocationOnMock.getArgument(0);
            executionContextProperties.remove(property);
            return null;
        }).when(executionContext).removeProperty(anyString());

    }

    private void verifyClearCache(String serializedReference) throws Exception
    {
        assertTrue(executionContextProperties.containsKey("property1"));
        assertFalse(executionContextProperties.containsKey("userToggleableFilterPreference_property3"));

        verify(cache).remove(serializedReference);
    }

    @Test
    void setStartDateForUser() throws Exception
    {
        Date date = new Date();
        this.cachedModelBridge.setStartDateForUser(user, date);

        verify(modelBridge).setStartDateForUser(user, date);
        verifyClearCache(SERIALIZED_USER);
    }

    @Test
    void setFilterPreferenceEnabledForUser() throws Exception
    {
        this.cachedModelBridge.setFilterPreferenceEnabled(user, "filter1", true);
        verify(modelBridge).setFilterPreferenceEnabled(user, "filter1", true);
        verifyClearCache(SERIALIZED_USER);
    }

    @Test
    void setFilterPreferenceEnabledForWiki() throws Exception
    {
        this.cachedModelBridge.setFilterPreferenceEnabled(wikiReference, "filter2", false);
        verify(modelBridge).setFilterPreferenceEnabled(wikiReference, "filter2", false);
        verifyClearCache(SERIALIZED_WIKI);
    }

    @Test
    void saveFilterPreferencesForUser() throws Exception
    {
        List<NotificationFilterPreference> filterPreferenceList = Arrays.asList(
            mock(NotificationFilterPreference.class),
            mock(NotificationFilterPreference.class)
        );
        this.cachedModelBridge.saveFilterPreferences(user, filterPreferenceList);
        verify(modelBridge).saveFilterPreferences(user, filterPreferenceList);
        verifyClearCache(SERIALIZED_USER);
    }

    @Test
    void saveFilterPreferencesForWiki() throws Exception
    {
        List<NotificationFilterPreference> filterPreferenceList = Arrays.asList(
            mock(NotificationFilterPreference.class),
            mock(NotificationFilterPreference.class)
        );
        this.cachedModelBridge.saveFilterPreferences(wikiReference, filterPreferenceList);
        verify(modelBridge).saveFilterPreferences(wikiReference, filterPreferenceList);
        verifyClearCache(SERIALIZED_WIKI);
    }
}
